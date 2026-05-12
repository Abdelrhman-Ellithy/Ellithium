package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.config.HealingStrategy;
import Ellithium.Utilities.ai.models.HealingResult;
import Ellithium.Utilities.ai.provider.LLMProvider;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrator for AI-driven locator self-healing.
 *
 * <p>When a locator fails, this class collects rich context (method name, action type,
 * data being sent, call-site source code, minimized DOM), queries the LLM for a fix,
 * validates the fix against the live DOM, caches it, modifies the source file, and
 * generates a healing report.</p>
 *
 * <p>Thread-safe: uses ThreadLocal for per-thread provider/strategy/cache.</p>
 * <p>Zero overhead on successful runs — the catch block is never entered.</p>
 */
public class AISelfHealer {

    private static final ThreadLocal<LLMProvider> llmProviderThread = new ThreadLocal<>();
    private static final ThreadLocal<HealingStrategy> strategyThread = new ThreadLocal<>();

    private static volatile LLMProvider globalProvider;
    private static volatile HealingStrategy globalStrategy = HealingStrategy.DISABLED;
    private static volatile double confidenceThreshold = 0.85;

    // Runtime locator cache: ThreadLocal ensures isolated cache per parallel test thread
    public static class CachedLocator {
        public final By newLocator;
        public final String originalField;
        public CachedLocator(By newLocator, String originalField) {
            this.newLocator = newLocator;
            this.originalField = originalField;
        }
    }
    public static final ThreadLocal<java.util.concurrent.ConcurrentHashMap<String, CachedLocator>> healedLocatorCacheThread
            = ThreadLocal.withInitial(java.util.concurrent.ConcurrentHashMap::new);

    // ──────────────────────── Initialization ────────────────────────

    public static void initialize(LLMProvider provider, HealingStrategy strategy, double threshold) {
        globalProvider = provider;
        globalStrategy = strategy;
        confidenceThreshold = threshold;
        Reporter.log("AI Self-Healing initialized | Strategy: " + strategy.name()
                + " | Model: " + provider.getModelName()
                + " | Confidence Threshold: " + threshold, LogLevel.INFO_YELLOW);
    }

    public static void initialize(LLMProvider provider, HealingStrategy strategy) {
        initialize(provider, strategy, 0.85);
    }

    public static void initializeForThread(LLMProvider provider, HealingStrategy strategy) {
        llmProviderThread.set(provider);
        strategyThread.set(strategy);
    }

    private static HealingStrategy getEffectiveStrategy() {
        HealingStrategy ts = strategyThread.get();
        return ts != null ? ts : globalStrategy;
    }

    public static LLMProvider getEffectiveProvider() {
        LLMProvider tp = llmProviderThread.get();
        return tp != null ? tp : globalProvider;
    }

    // ──────────────────────── Rich Context DTO ────────────────────────

    /**
     * Captures all contextual information needed by the LLM to heal a broken locator.
     */
    private static class HealingContext {
        String brokenLocatorStr;    // e.g. "By.id: test"
        String byMethod;           // e.g. "id", "cssSelector", "tagName"
        String byValue;            // e.g. "test", "lpl", "" (empty)
        String pageClassName;      // e.g. "Pages.LoginPage"
        String methodName;         // e.g. "setUserName"
        String actionType;         // e.g. "sendData", "clickOnElement"
        String callSiteSource;     // 3-5 lines of source around call site
        String minimizedDom;       // minimized DOM snapshot
        String filePath;           // e.g. "src/test/java/Pages/LoginPage.java"
        String fieldName;          // e.g. "usernameField" or null for inline
        int lineNumber;            // stack frame line number
        boolean isMobile;
    }

    // ──────────────────────── Source Location DTO ────────────────────────

    private static class SourceLocation {
        final String filePath;
        final String fieldName;
        final String className;
        final String methodName;
        final int lineNumber;

        SourceLocation(String filePath, String fieldName, String className, String methodName, int lineNumber) {
            this.filePath = filePath;
            this.fieldName = fieldName;
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }
    }

    // ──────────────────────── Main Entry Points ────────────────────────

    /**
     * Primary entry point: attempts to heal a broken locator and return the found element.
     */
    public static WebElement attemptHeal(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace) {
        if (getEffectiveStrategy() == HealingStrategy.DISABLED || getEffectiveProvider() == null) {
            return null;
        }

        Reporter.log("AI Self-Healing triggered for locator: " + brokenLocator.toString(), LogLevel.INFO_YELLOW);

        By newLocator = healLocator(driver, brokenLocator, stackTrace);

        if (newLocator != null) {
            CachedLocator cached = healedLocatorCacheThread.get().get(brokenLocator.toString());
            if (cached != null) {
                Reporter.log("AI Self-Healing (cached): reusing healed locator " + cached.newLocator
                        + " for field '" + cached.originalField + "' (original: " + brokenLocator + ")", LogLevel.INFO_YELLOW);
            }
            try {
                return driver.findElement(newLocator);
            } catch (Exception e) {
                Reporter.log("AI Self-Healing: Healed locator also failed: " + e.getMessage(), LogLevel.ERROR);
                healedLocatorCacheThread.get().remove(brokenLocator.toString());
            }
        }
        return null;
    }

    /**
     * Resolves the healed By locator without finding the element.
     * Used for list operations (findElements).
     */
    public static By healLocator(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace) {
        HealingStrategy strategy = getEffectiveStrategy();
        LLMProvider provider = getEffectiveProvider();
        if (strategy == HealingStrategy.DISABLED || provider == null) return null;

        // Fast path: cache hit
        CachedLocator cached = healedLocatorCacheThread.get().get(brokenLocator.toString());
        if (cached != null) return cached.newLocator;

        // ── Step 1-2: Collect rich context ──
        HealingContext ctx = buildHealingContext(driver, brokenLocator, stackTrace);

        // ── Step 3: Build prompts ──
        String systemPrompt = buildSystemPrompt(ctx.isMobile);
        String userPrompt = buildUserPrompt(ctx);

        // ── Step 4: Query LLM ──
        String llmResponse;
        try {
            llmResponse = provider.ask(systemPrompt, userPrompt);
        } catch (Exception e) {
            Reporter.log("AI Self-Healing: LLM Provider failed - " + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        // ── Step 5: Parse response ──
        HealingResult result = parseHealingResponse(llmResponse);
        if (result == null) {
            Reporter.log("AI Self-Healing: Failed to parse LLM response", LogLevel.ERROR);
            return null;
        }
        Reporter.log("AI Healing Result: " + result.toString(), LogLevel.INFO_BLUE);

        // ── Step 6: Check confidence ──
        if (!result.isConfidentEnough(confidenceThreshold)) {
            Reporter.log("AI Healing confidence (" + String.format("%.2f", result.getConfidence())
                    + ") below threshold (" + confidenceThreshold + "). SUGGEST_ONLY mode forced.", LogLevel.INFO_YELLOW);
            Reporter.log("[AI Suggestion] " + result.getNewLocatorExpression()
                    + " | Reason: " + result.getReasoning(), LogLevel.INFO_BLUE);
            return null;
        }

        if (strategy == HealingStrategy.SUGGEST_ONLY) {
            Reporter.log("[AI Suggestion] " + result.getNewLocatorExpression()
                    + " | Confidence: " + String.format("%.2f", result.getConfidence())
                    + " | Reason: " + result.getReasoning(), LogLevel.INFO_BLUE);
            return null;
        }

        // ── Step 7: Parse and validate the healed locator ──
        By newLocator = parseByFromExpression(result.getNewLocatorExpression());
        if (newLocator == null) {
            Reporter.log("AI Self-Healing: Failed to parse expression: " + result.getNewLocatorExpression(), LogLevel.ERROR);
            return null;
        }

        // Validate: actually try to find the element with the new locator
        try {
            driver.findElement(newLocator);
        } catch (Exception e) {
            Reporter.log("AI Self-Healing: Healed locator validation failed (element not found): "
                    + result.getNewLocatorExpression(), LogLevel.ERROR);
            return null;
        }

        // ── Step 8: Cache + Modify Source + Report ──
        String fieldLabel = ctx.fieldName != null ? ctx.fieldName : ctx.methodName;
        healedLocatorCacheThread.get().put(brokenLocator.toString(),
                new CachedLocator(newLocator, fieldLabel != null ? fieldLabel : "unknown"));

        // ALWAYS queue for report (both LOCAL and CI)
        AIHealingReporter.queueChange(
                ctx.filePath != null ? ctx.filePath : "unknown",
                brokenLocator.toString(),
                result,
                ctx.pageClassName,
                ctx.methodName,
                ctx.actionType,
                ctx.lineNumber);

        // Source modification (LOCAL mode only)
        if (!AIConfigLoader.isCI() && ctx.filePath != null) {
            boolean written = false;

            if (ctx.fieldName != null) {
                // Strategy A: field variable
                written = JavaSourceModifier.updateLocatorValue(
                        ctx.filePath, ctx.fieldName, result.getNewLocatorExpression());
            } else if (ctx.byMethod != null) {
                // Strategy B: inline By.xxx() — match by content
                written = JavaSourceModifier.updateLocatorByOldValue(
                        ctx.filePath, ctx.byMethod, ctx.byValue, result.getNewLocatorExpression());
            }

            if (written) {
                Reporter.log("AI Self-Healing: Source file updated successfully", LogLevel.INFO_GREEN);
            }
        }

        if (strategy == HealingStrategy.HEAL_AND_NOTIFY) {
            Reporter.log("[AI HEALED & NOTIFIED] Locator healed: " + brokenLocator
                    + " → " + result.getNewLocatorExpression()
                    + " | File: " + (ctx.filePath != null ? ctx.filePath : "unknown"), LogLevel.INFO_YELLOW);
        }

        return newLocator;
    }

    // ──────────────────────── Context Collection ────────────────────────

    /**
     * Collects all contextual information about the failing locator.
     */
    private static HealingContext buildHealingContext(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace) {
        HealingContext ctx = new HealingContext();
        ctx.brokenLocatorStr = brokenLocator.toString();
        ctx.isMobile = driver instanceof AppiumDriver;

        // Extract By method and value from the locator's toString()
        // Format: "By.id: test" or "By.cssSelector: #login" or "By.tagName: bt"
        parseByLocator(brokenLocator.toString(), ctx);

        // Resolve source location (POM class, method, file path, field name)
        SourceLocation srcLoc = resolveSourceLocation(stackTrace);
        if (srcLoc != null) {
            ctx.pageClassName = srcLoc.className;
            ctx.methodName = srcLoc.methodName;
            ctx.filePath = srcLoc.filePath;
            ctx.fieldName = srcLoc.fieldName;
            ctx.lineNumber = srcLoc.lineNumber;
        }

        // Extract action type from Ellithium interaction layer in the stack
        ctx.actionType = extractActionType(stackTrace);

        // Read source code around call site
        if (ctx.filePath != null && ctx.lineNumber > 0) {
            ctx.callSiteSource = readCallSiteSource(ctx.filePath, ctx.lineNumber);
        }

        // Capture and minimize DOM
        String rawDom = driver.getPageSource();
        Reporter.log("Page source retrieved", LogLevel.INFO_BLUE);
        String minimizedDom = Ellithium.Utilities.ai.sanitizers.DOMMinimizer.minimize(rawDom);
        ctx.minimizedDom = Ellithium.Utilities.ai.sanitizers.DataScrubber.scrub(minimizedDom);

        return ctx;
    }

    /**
     * Parses "By.id: test" into byMethod="id", byValue="test"
     */
    private static void parseByLocator(String locatorStr, HealingContext ctx) {
        // Pattern: "By.methodName: value" or "By.methodName: "
        Matcher m = Pattern.compile("By\\.([a-zA-Z]+):\\s*(.*)").matcher(locatorStr);
        if (m.find()) {
            ctx.byMethod = m.group(1);
            ctx.byValue = m.group(2).trim();
        }
    }

    /**
     * Walks the stack trace to find the Ellithium interaction method name.
     * Returns e.g. "sendData", "clickOnElement", "getText", "hover".
     */
    private static String extractActionType(StackTraceElement[] stackTrace) {
        for (StackTraceElement frame : stackTrace) {
            String cls = frame.getClassName();
            if (cls.startsWith("Ellithium.Utilities.interactions.")) {
                String method = frame.getMethodName();
                // Skip internal infrastructure methods
                if (!method.equals("findWebElement") && !method.equals("waitForVisibilityAndFindElement")
                        && !method.equals("getFluentWait") && !method.equals("findWebElements")
                        && !method.equals("waitForVisibilityAndFindElements")) {
                    return method;
                }
            }
        }
        return "unknown";
    }

    /**
     * Reads 5 lines around the call site from the source file.
     */
    private static String readCallSiteSource(String filePath, int lineNumber) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filePath));
            int start = Math.max(0, lineNumber - 3);
            int end = Math.min(lines.size(), lineNumber + 2);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(lines.get(i)).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ──────────────────────── Prompt Building ────────────────────────

    private static String buildSystemPrompt(boolean isMobile) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Selenium/Appium test automation engineer performing locator healing.\n");
        sb.append("A test automation locator has failed. Analyze the context and the current DOM ");
        sb.append("to find the CORRECT element the test was trying to interact with.\n\n");
        sb.append("CRITICAL RULES:\n");
        sb.append("1. PRIORITY 1: Verify the SYNTAX of the original locator. If the locator has a syntax error (e.g., malformed XPath, invalid CSS pseudo-classes, or typos in attributes), your first goal must be to FIX the syntax while strictly preserving the original intent and locator type, rather than suggesting a completely different locator strategy.\n");
        sb.append("2. The METHOD NAME is a STRONG HINT for the element's purpose ");
        sb.append("(e.g., setUserName → username input, clickLoginBtn → login/submit button)\n");
        sb.append("3. The ACTION TYPE tells you what kind of element to look for ");
        sb.append("(sendData → input/textarea, clickOnElement → button/link/clickable)\n");
        sb.append("4. If the broken locator value is empty or nonsensical, use the method name as PRIMARY signal\n");
        sb.append("5. Prefer stable locators: id > name > data-testid > css > xpath\n");
        sb.append("6. Respond ONLY in JSON: {\"locator\": \"By.id(\\\"...\\\")\", \"confidence\": 0.95, \"reasoning\": \"...\"}\n");
        sb.append("7. If the element genuinely does not exist on the page, set confidence to 0.0\n\n");

        if (isMobile) {
            sb.append("Use AppiumBy.accessibilityId, AppiumBy.androidUIAutomator, AppiumBy.iOSClassChain, By.id, or By.xpath.\n");
        } else {
            sb.append("Use By.id, By.cssSelector, By.xpath, By.name, or By.className.\n");
        }
        return sb.toString();
    }

    /**
     * Builds the full user prompt with all available context.
     */
    private static String buildUserPrompt(HealingContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("FAILED LOCATOR: ").append(ctx.brokenLocatorStr).append("\n\n");

        sb.append("CONTEXT:\n");
        if (ctx.pageClassName != null) sb.append("- Page Object Class: ").append(ctx.pageClassName).append("\n");
        if (ctx.methodName != null) sb.append("- Method: ").append(ctx.methodName).append("\n");
        if (ctx.actionType != null && !ctx.actionType.equals("unknown")) {
            sb.append("- Action: ").append(ctx.actionType);
            if (ctx.actionType.equals("sendData")) sb.append(" (text input into a field)");
            else if (ctx.actionType.equals("clickOnElement")) sb.append(" (clicking a button/link)");
            else if (ctx.actionType.equals("getText")) sb.append(" (reading text from element)");
            sb.append("\n");
        }
        if (ctx.callSiteSource != null) {
            sb.append("- Source code at call site:\n").append(ctx.callSiteSource).append("\n");
        }

        if (ctx.minimizedDom != null && !ctx.minimizedDom.isEmpty()) {
            sb.append("\nCURRENT DOM:\n").append(ctx.minimizedDom);
        }
        return sb.toString();
    }

    // ──────────────────────── Source Location Resolution ────────────────────────

    private static SourceLocation resolveSourceLocation(StackTraceElement[] stackTrace) {
        for (StackTraceElement frame : stackTrace) {
            String className = frame.getClassName();
            if (className.startsWith("Ellithium.")
                    || className.startsWith("org.openqa.selenium")
                    || className.startsWith("java.")
                    || className.startsWith("sun.")
                    || className.startsWith("io.cucumber")
                    || className.startsWith("io.qameta")
                    || className.startsWith("org.testng")
                    || className.startsWith("net.bytebuddy")) {
                continue;
            }

            String classFilePart = className.replace('.', '/') + ".java";
            String resolvedPath = null;
            for (String root : new String[]{"src/test/java/", "src/main/java/"}) {
                String candidate = root + classFilePart;
                if (new java.io.File(candidate).exists()) {
                    resolvedPath = candidate;
                    break;
                }
            }
            int callSiteLine = frame.getLineNumber();

            if (resolvedPath == null) {
                return new SourceLocation(null, null, className, frame.getMethodName(), callSiteLine);
            }

            String fieldName = resolveFieldNameFromSource(resolvedPath, callSiteLine);

            Reporter.log("AI Healer: Located source at " + resolvedPath
                    + ":" + callSiteLine
                    + (fieldName != null ? " → field '" + fieldName + "'" : " (inline locator)"), LogLevel.INFO_BLUE);
            return new SourceLocation(resolvedPath, fieldName, className, frame.getMethodName(), callSiteLine);
        }
        return null;
    }

    private static String resolveFieldNameFromSource(String filePath, int callSiteLine) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filePath));
            if (callSiteLine < 1 || callSiteLine > lines.size()) return null;

            String callLine = lines.get(callSiteLine - 1).trim();

            // Check if the call line contains an inline By.xxx() — if so, no field name
            if (callLine.matches(".*By\\.[a-zA-Z]+\\(.*") || callLine.matches(".*AppiumBy\\.[a-zA-Z]+\\(.*")) {
                // It's an inline locator — check if a variable is being passed instead
                // Look for patterns like someMethod(fieldName, ...) where fieldName is a By variable
                java.util.regex.Matcher m = Pattern.compile("(?:this\\.)?([a-zA-Z_][a-zA-Z0-9_]*)")
                        .matcher(callLine);
                java.util.List<String> candidates = new java.util.ArrayList<>();
                while (m.find()) candidates.add(m.group(1));

                // Walk upward looking for By field declarations
                for (int i = callSiteLine - 2; i >= 0; i--) {
                    String srcLine = lines.get(i).trim();
                    java.util.regex.Matcher fieldMatcher = Pattern
                            .compile("(?:private|protected|public)?\\s*(?:final\\s+)?By\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:=|;)")
                            .matcher(srcLine);
                    if (fieldMatcher.find()) {
                        String foundField = fieldMatcher.group(1);
                        if (candidates.contains(foundField)) {
                            return foundField;
                        }
                    }
                }
                // No field reference found — it's truly inline
                return null;
            }

            // Fallback: scan upward for By field declarations
            java.util.regex.Matcher m = Pattern.compile("(?:this\\.)?([a-zA-Z_][a-zA-Z0-9_]*)")
                    .matcher(callLine);
            java.util.List<String> candidates = new java.util.ArrayList<>();
            while (m.find()) candidates.add(m.group(1));

            for (int i = callSiteLine - 2; i >= 0; i--) {
                String srcLine = lines.get(i).trim();
                java.util.regex.Matcher fieldMatcher = Pattern
                        .compile("(?:private|protected|public)?\\s*(?:final\\s+)?By\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:=|;)")
                        .matcher(srcLine);
                if (fieldMatcher.find()) {
                    String foundField = fieldMatcher.group(1);
                    if (candidates.contains(foundField)) return foundField;
                }
            }
        } catch (Exception e) {
            Reporter.log("AI Healer: Could not read source for field resolution: " + e.getMessage(), LogLevel.WARN);
        }
        return null;
    }

    // ──────────────────────── Response Parsing ────────────────────────

    private static HealingResult parseHealingResponse(String response) {
        if (response == null || response.trim().isEmpty()) return null;
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
            String locator = json.get("locator").getAsString();
            double confidence = json.get("confidence").getAsDouble();
            String reasoning = json.has("reasoning") ? json.get("reasoning").getAsString() : "";
            return new HealingResult(locator, confidence, reasoning);
        } catch (Exception e) {
            Reporter.log("Failed to parse AI healing response: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    // ──────────────────────── By Expression Parsing ────────────────────────

    private static By parseByFromExpression(String expression) {
        try {
            if (expression.startsWith("By.id(")) return By.id(extractValue(expression));
            else if (expression.startsWith("By.cssSelector(")) return By.cssSelector(extractValue(expression));
            else if (expression.startsWith("By.xpath(")) return By.xpath(extractValue(expression));
            else if (expression.startsWith("By.name(")) return By.name(extractValue(expression));
            else if (expression.startsWith("By.className(")) return By.className(extractValue(expression));
            else if (expression.startsWith("By.linkText(")) return By.linkText(extractValue(expression));
            else if (expression.startsWith("By.partialLinkText(")) return By.partialLinkText(extractValue(expression));
            else if (expression.startsWith("By.tagName(")) return By.tagName(extractValue(expression));
            else if (expression.startsWith("AppiumBy.accessibilityId(")) return AppiumBy.accessibilityId(extractValue(expression));
            else if (expression.startsWith("AppiumBy.androidUIAutomator(")) return AppiumBy.androidUIAutomator(extractValue(expression));
            else if (expression.startsWith("AppiumBy.androidViewTag(")) return AppiumBy.androidViewTag(extractValue(expression));
            else if (expression.startsWith("AppiumBy.androidDataMatcher(")) return AppiumBy.androidDataMatcher(extractValue(expression));
            else if (expression.startsWith("AppiumBy.iOSClassChain(")) return AppiumBy.iOSClassChain(extractValue(expression));
            else if (expression.startsWith("AppiumBy.iOSNsPredicateString(")) return AppiumBy.iOSNsPredicateString(extractValue(expression));
            else if (expression.startsWith("AppiumBy.image(")) return AppiumBy.image(extractValue(expression));
            else if (expression.startsWith("AppiumBy.custom(")) return AppiumBy.custom(extractValue(expression));
        } catch (Exception e) {
            Reporter.log("Failed to parse By expression: " + expression, LogLevel.ERROR);
        }
        return null;
    }

    private static String extractValue(String expression) {
        int start = expression.indexOf('"') + 1;
        int end = expression.lastIndexOf('"');
        return expression.substring(start, end);
    }

    // ──────────────────────── Cleanup ────────────────────────

    public static void cleanup() {
        llmProviderThread.remove();
        strategyThread.remove();
        healedLocatorCacheThread.remove();
    }
}
