package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.config.HealingStrategy;
import Ellithium.Utilities.ai.models.ElementFingerprint;
import Ellithium.Utilities.ai.models.HealingResult;
import Ellithium.Utilities.ai.provider.LLMProvider;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    // Global runtime locator cache: shared across threads, persisted at suite end
    public static class CachedLocator {
        public final By newLocator;
        public final String originalField;
        public CachedLocator(By newLocator, String originalField) {
            this.newLocator = newLocator;
            this.originalField = originalField;
        }
    }
    private static final ConcurrentHashMap<String, CachedLocator> globalHealedCache = new ConcurrentHashMap<>();

    // Deferred source patches — queued during test, applied at suite end
    private static final ConcurrentLinkedQueue<SourcePatch> pendingPatches = new ConcurrentLinkedQueue<>();

    /** Represents a deferred source code patch. */
    public static class SourcePatch {
        public final String filePath;
        public final String fieldName;
        public final String byMethod;
        public final String byValue;
        public final String newLocatorExpression;
        public SourcePatch(String filePath, String fieldName, String byMethod, String byValue, String newLocatorExpression) {
            this.filePath = filePath;
            this.fieldName = fieldName;
            this.byMethod = byMethod;
            this.byValue = byValue;
            this.newLocatorExpression = newLocatorExpression;
        }
    }

    // ──────────────────────── Public Source Patching API ────────────────────────

    /**
     * Queues a source patch from Tier 1 or Tier 1.5 healing.
     * Called by {@link BaseActions} after a successful heal so the POM source file
     * gets corrected at suite end (if strategy is HEAL_AND_NOTIFY).
     *
     * @param brokenLocator  The original broken By locator
     * @param healedElement  The element that was found by healing
     * @param stackTrace     The call stack for source location resolution
     */
    public static void queueSourcePatch(By brokenLocator, WebElement healedElement, StackTraceElement[] stackTrace) {
        try {
            // Only patch source when HEAL_AND_NOTIFY is active
            HealingStrategy strategy = getEffectiveStrategy();
            if (strategy != HealingStrategy.HEAL_AND_NOTIFY) return;

            // Resolve source location (file path, field name)
            SourceLocation srcLoc = resolveSourceLocation(stackTrace);
            if (srcLoc == null || srcLoc.filePath == null) return;

            // Parse broken locator to get byMethod + byValue
            HealingContext tempCtx = new HealingContext();
            parseByLocator(brokenLocator.toString(), tempCtx);
            if (tempCtx.byMethod == null) return;

            // Reconstruct cleanest locator from healed element and convert to Java source
            By healedBy = Ellithium.Utilities.ai.models.ElementFingerprint.reconstructLocator(healedElement);
            if (healedBy == null) return;
            String javaExpression = byToJavaExpression(healedBy);
            if (javaExpression == null) return;

            pendingPatches.add(new SourcePatch(
                    srcLoc.filePath, srcLoc.fieldName,
                    tempCtx.byMethod, tempCtx.byValue, javaExpression));

            Reporter.log("Source patch queued: By." + tempCtx.byMethod + "(\"" + tempCtx.byValue + "\") → "
                    + javaExpression + " in " + srcLoc.filePath, LogLevel.DEBUG);
        } catch (Exception e) {
            // Source patching must never crash the test
        }
    }

    /**
     * Converts a By locator to a Java source code expression.
     * E.g., {@code By.id: username} → {@code By.id("username")}
     */
    public static String byToJavaExpression(By locator) {
        String str = locator.toString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("By\\.([a-zA-Z]+):\\s*(.*)").matcher(str);
        if (!m.find()) return null;
        String method = m.group(1);
        String value = m.group(2).trim();
        return "By." + method + "(\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\")";
    }

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
        byte[] screenshot;         // PNG screenshot for vision-capable LLMs (null if not captured)
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
     * Uses a global shared cache for cross-thread healing reuse.
     */
    public static WebElement attemptHeal(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace) {
        if (getEffectiveStrategy() == HealingStrategy.DISABLED || getEffectiveProvider() == null) {
            return null;
        }

        Reporter.log("[TIER 2] AI Self-Healing triggered for locator: " + brokenLocator.toString(), LogLevel.INFO_YELLOW);

        By newLocator = healLocator(driver, brokenLocator, stackTrace);

        if (newLocator != null) {
            CachedLocator cached = globalHealedCache.get(brokenLocator.toString());
            if (cached != null) {
                Reporter.log("AI Self-Healing (cached): reusing healed locator " + cached.newLocator
                        + " for field '" + cached.originalField + "' (original: " + brokenLocator + ")", LogLevel.INFO_YELLOW);
            }
            try {
                WebElement found = driver.findElement(newLocator);
                // Capture the healed element as new baseline
                BaselineStore.capture(driver, brokenLocator, found);
                return found;
            } catch (Exception e) {
                Reporter.log("AI Self-Healing: Healed locator also failed: " + e.getMessage(), LogLevel.ERROR);
                globalHealedCache.remove(brokenLocator.toString());
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

        // Fast path: global cache hit
        CachedLocator cached = globalHealedCache.get(brokenLocator.toString());
        if (cached != null) return cached.newLocator;

        // ── Step 1-2: Collect rich context ──
        HealingContext ctx = buildHealingContext(driver, brokenLocator, stackTrace);

        // ── Step 3: Build prompts ──
        String systemPrompt = buildSystemPrompt(ctx.isMobile);
        String userPrompt = buildUserPrompt(ctx);

        // ── Step 4: Query LLM with retry + exponential backoff ──
        // Use vision (screenshot) if available and provider supports it
        String llmResponse;
        if (ctx.screenshot != null && provider.supportsVision()) {
            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;
            llmResponse = queryLLMWithVisionRetry(provider, combinedPrompt, ctx.screenshot, 3);
        } else {
            llmResponse = queryLLMWithRetry(provider, systemPrompt, userPrompt, 3);
        }
        if (llmResponse == null || llmResponse.isBlank()) {
            Reporter.log("AI Self-Healing: LLM returned no response after retries", LogLevel.ERROR);
            return null;
        }

        // ── Step 5: Parse multi-candidate response ──
        List<HealingResult> candidates = parseMultiCandidateResponse(llmResponse);
        if (candidates.isEmpty()) {
            Reporter.log("AI Self-Healing: Failed to parse any candidates from LLM response", LogLevel.ERROR);
            return null;
        }

        // ── Step 6: Validate candidates against baseline + live DOM ──
        ElementFingerprint baseline = BaselineStore.getBaseline(brokenLocator.toString());
        By acceptedLocator = null;
        HealingResult acceptedResult = null;

        for (HealingResult candidate : candidates) {
            if (!candidate.isConfidentEnough(confidenceThreshold)) {
                Reporter.log("AI Candidate skipped (confidence " + String.format("%.2f", candidate.getConfidence())
                        + " < " + confidenceThreshold + "): " + candidate.getNewLocatorExpression(), LogLevel.INFO_BLUE);
                continue;
            }

            if (strategy == HealingStrategy.SUGGEST_ONLY) {
                Reporter.log("[AI Suggestion] " + candidate.getNewLocatorExpression()
                        + " | Confidence: " + String.format("%.2f", candidate.getConfidence())
                        + " | Reason: " + candidate.getReasoning(), LogLevel.INFO_BLUE);
                continue;
            }

            By candidateLocator = parseByFromExpression(candidate.getNewLocatorExpression());
            if (candidateLocator == null) continue;

            // Validate: element must exist
            WebElement foundEl;
            try {
                foundEl = driver.findElement(candidateLocator);
            } catch (Exception e) {
                Reporter.log("AI Candidate validation failed (not found): " + candidate.getNewLocatorExpression(), LogLevel.INFO_BLUE);
                continue;
            }

            // Cross-validate against baseline fingerprint if available
            if (baseline != null) {
                double matchScore = baseline.scoreSimilarity(foundEl);
                if (matchScore < 0.40) {
                    Reporter.log("AI Candidate rejected (baseline mismatch, score=" + String.format("%.2f", matchScore)
                            + "): " + candidate.getNewLocatorExpression(), LogLevel.INFO_BLUE);
                    continue;
                }

                // Tag-type cross-validation: if baseline says <input> but candidate is <button>, reject
                String baselineTag = baseline.getTagName();
                if (baselineTag != null && !baselineTag.isBlank()) {
                    try {
                        String candidateTag = foundEl.getTagName();
                        if (candidateTag != null && !candidateTag.equalsIgnoreCase(baselineTag)) {
                            Reporter.log("AI Candidate rejected (tag mismatch: expected <" + baselineTag
                                    + "> but found <" + candidateTag + ">): "
                                    + candidate.getNewLocatorExpression(), LogLevel.INFO_BLUE);
                            continue;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Candidate accepted!
            acceptedLocator = candidateLocator;
            acceptedResult = candidate;
            break;
        }

        if (acceptedLocator == null || acceptedResult == null) {
            if (strategy == HealingStrategy.SUGGEST_ONLY) return null;
            Reporter.log("AI Self-Healing: No candidate passed validation", LogLevel.ERROR);
            return null;
        }

        Reporter.log("AI Healing Accepted: " + acceptedResult.toString(), LogLevel.INFO_GREEN);

        // ── Step 7: Cache + Deferred Source Patch + Report ──
        String fieldLabel = ctx.fieldName != null ? ctx.fieldName : ctx.methodName;
        globalHealedCache.put(brokenLocator.toString(),
                new CachedLocator(acceptedLocator, fieldLabel != null ? fieldLabel : "unknown"));

        // ALWAYS queue for report
        AIHealingReporter.queueChange(
                ctx.filePath != null ? ctx.filePath : "unknown",
                brokenLocator.toString(),
                acceptedResult,
                ctx.pageClassName,
                ctx.methodName,
                ctx.actionType,
                ctx.lineNumber);

        // Deferred source modification — queue patch for suite end (never mid-test)
        if (ctx.filePath != null) {
            pendingPatches.add(new SourcePatch(
                    ctx.filePath, ctx.fieldName, ctx.byMethod, ctx.byValue,
                    acceptedResult.getNewLocatorExpression()));
        }

        if (strategy == HealingStrategy.HEAL_AND_NOTIFY) {
            Reporter.log("[AI HEALED & NOTIFIED] Locator healed: " + brokenLocator
                    + " → " + acceptedResult.getNewLocatorExpression()
                    + " | File: " + (ctx.filePath != null ? ctx.filePath : "unknown"), LogLevel.INFO_YELLOW);
        }

        return acceptedLocator;
    }

    // ──────────────────────── LLM Retry Logic ────────────────────────

    /**
     * Queries the LLM with exponential backoff retry.
     */
    private static String queryLLMWithRetry(LLMProvider provider, String systemPrompt, String userPrompt, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = provider.ask(systemPrompt, userPrompt);
                if (response != null && !response.isBlank()) return response;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    Reporter.log("AI Self-Healing: LLM failed after " + maxRetries + " attempts: " + e.getMessage(), LogLevel.ERROR);
                    return null;
                }
                long waitMs = (long) Math.pow(2, attempt) * 500; // 1s, 2s, 4s
                Reporter.log("AI Self-Healing: LLM attempt " + attempt + " failed, retrying in " + waitMs + "ms...", LogLevel.WARN);
                try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
            }
        }
        return null;
    }

    /**
     * Queries the LLM with vision support (screenshot) and exponential backoff retry.
     * Falls back to text-only if the provider doesn't actually support vision at runtime.
     */
    private static String queryLLMWithVisionRetry(LLMProvider provider, String prompt, byte[] screenshot, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = provider.askWithVision(prompt, screenshot);
                if (response != null && !response.isBlank()) return response;
            } catch (UnsupportedOperationException e) {
                // Provider lied about vision support — fall back to text-only
                Reporter.log("AI Self-Healing: Vision not supported at runtime, falling back to text-only", LogLevel.WARN);
                return queryLLMWithRetry(provider, "", prompt, maxRetries);
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    Reporter.log("AI Self-Healing: Vision LLM failed after " + maxRetries + " attempts: " + e.getMessage(), LogLevel.ERROR);
                    return null;
                }
                long waitMs = (long) Math.pow(2, attempt) * 500;
                Reporter.log("AI Self-Healing: Vision LLM attempt " + attempt + " failed, retrying in " + waitMs + "ms...", LogLevel.WARN);
                try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
            }
        }
        return null;
    }

    // ──────────────────────── Deferred Patch Application ────────────────────────

    /**
     * Applies all deferred source patches. Called at suite end by {@link AIHealingReporter#generateReport()}.
     * Only applies in LOCAL mode — CI mode leaves source files untouched.
     */
    public static void applyDeferredPatches() {
        if (AIConfigLoader.isCI() || pendingPatches.isEmpty()) return;

        java.util.LinkedHashMap<String, SourcePatch> uniquePatches = new java.util.LinkedHashMap<>();
        for (SourcePatch patch : pendingPatches) {
            String key = patch.filePath + "|" + patch.byMethod + "|" + patch.byValue;
            uniquePatches.putIfAbsent(key, patch);
        }
        pendingPatches.clear();

        Reporter.log("AI Self-Healing: Applying " + uniquePatches.size() + " source patches...", LogLevel.INFO_YELLOW);
        int applied = 0;
        for (SourcePatch patch : uniquePatches.values()) {
            boolean written = false;
            if (patch.fieldName != null) {
                written = JavaSourceModifier.updateLocatorValue(patch.filePath, patch.fieldName, patch.newLocatorExpression);
            } else if (patch.byMethod != null) {
                written = JavaSourceModifier.updateLocatorByOldValue(patch.filePath, patch.byMethod, patch.byValue, patch.newLocatorExpression);
            }
            if (written) applied++;
        }
        Reporter.log("AI Self-Healing: " + applied + "/" + uniquePatches.size() + " source patches applied", LogLevel.INFO_GREEN);
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

        // Capture DOM using best available method (AX tree → HTML fallback)
        String optimizedDom = Ellithium.Utilities.ai.sanitizers.DOMMinimizer.getOptimalDOMRepresentation(driver);
        ctx.minimizedDom = Ellithium.Utilities.ai.sanitizers.DataScrubber.scrub(optimizedDom);

        // Capture screenshot for vision-capable LLMs (GPT-4o, Gemini Pro Vision, etc.)
        LLMProvider provider = getEffectiveProvider();
        if (provider != null && provider.supportsVision()) {
            try {
                if (driver instanceof org.openqa.selenium.TakesScreenshot screenshotDriver) {
                    ctx.screenshot = screenshotDriver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                    Reporter.log("Screenshot captured for visual healing (" + ctx.screenshot.length + " bytes)", LogLevel.INFO_BLUE);
                }
            } catch (Exception e) {
                Reporter.log("Failed to capture screenshot for visual healing: " + e.getMessage(), LogLevel.WARN);
            }
        }

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
        sb.append("## Reference Documentation:\n");
        sb.append("- GitHub: https://github.com/Abdelrhman-Ellithy/Ellithium\n");
        sb.append("- Website: https://abdelrhman-ellithy.github.io/ellithium.github.io/\n\n");
        sb.append("CRITICAL RULES:\n");
        sb.append("1. PRIORITY 1: Verify the SYNTAX of the original locator. If the locator has a syntax error (e.g., malformed XPath, invalid CSS pseudo-classes, or typos in attributes), your first goal must be to FIX the syntax while strictly preserving the original intent and locator type, rather than suggesting a completely different locator strategy.\n");
        sb.append("2. The METHOD NAME is a STRONG HINT for the element's purpose ");
        sb.append("(e.g., setUserName → username input, clickLoginBtn → login/submit button)\n");
        sb.append("3. The ACTION TYPE tells you what kind of element to look for based on Ellithium's DriverActions subclasses:\n");
        sb.append("4. Use Ellithium's API Structure:\n");
        sb.append("   - .elements() -> ElementActions (sendData, clickOnElement, getText, clearElement, isElementDisplayed)\n");
        sb.append("   - .waits() -> WaitActions (waitForElementToBeVisible, waitForElementToBeClickable)\n");
        sb.append("   - .select() -> SelectActions (selectDropdownByText, selectDropdownByIndex)\n");
        sb.append("   - .mouse() -> MouseActions (hoverOverElement, doubleClick)\n");
        sb.append("   - .mobileActions() -> MobileActions (swipe, longPress, pinch, tap)\n");
        sb.append("   - ElementActions (sendData → input/textarea, clickOnElement → button/link/clickable, getText, clearElement)\n");
        sb.append("   - SelectActions (selectDropdownByText, selectDropdownByIndex → select)\n");
        sb.append("   - WaitActions (waitForElementToBeVisible, waitForElementToBeClickable)\n");
        sb.append("   - MouseActions (hoverOverElement, doubleClick)\n");
        sb.append("   - MobileActions (swipe, longPress, pinch, tap)\n");
        sb.append("4. If the broken locator value is empty or nonsensical, use the method name as PRIMARY signal\n");
        sb.append("5. Prefer stable locators: id > name > data-testid > css > xpath\n");
        int maxCandidates = AIConfigLoader.getMaxCandidates();
        sb.append("6. Respond ONLY in JSON with your TOP ").append(maxCandidates).append(" candidates ranked by confidence (highest first):\n");
        sb.append("{\"candidates\": [\n");
        sb.append("  {\"locator\": \"By.id(\\\"...\\\")\", \"confidence\": 0.95, \"reasoning\": \"...\"},\n");
        sb.append("  {\"locator\": \"By.cssSelector(\\\"...\\\")\", \"confidence\": 0.88, \"reasoning\": \"...\"},\n");
        sb.append("  ...\n");
        sb.append("]}\n");
        sb.append("Return up to ").append(maxCandidates).append(" candidates. If only one is viable, return a single-element array. ");
        sb.append("Also accept legacy single-object format: {\"locator\": ..., \"confidence\": ..., \"reasoning\": ...}\n");
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

    /**
     * Parses LLM response into a list of healing candidates.
     * Handles both multi-candidate format ({"candidates": [...]}) and
     * legacy single-object format ({"locator": ..., "confidence": ...}).
     * Lenient: strips markdown fences and extra whitespace.
     */
    static List<HealingResult> parseMultiCandidateResponse(String response) {
        List<HealingResult> results = new ArrayList<>();
        if (response == null || response.trim().isEmpty()) return results;

        // Strip markdown code fences if present
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
        }

        try {
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(cleaned);

            if (root.isJsonObject()) {
                com.google.gson.JsonObject obj = root.getAsJsonObject();

                // Multi-candidate format: {"candidates": [...]}
                if (obj.has("candidates") && obj.get("candidates").isJsonArray()) {
                    com.google.gson.JsonArray arr = obj.getAsJsonArray("candidates");
                    for (com.google.gson.JsonElement el : arr) {
                        HealingResult r = parseSingleCandidate(el.getAsJsonObject());
                        if (r != null) results.add(r);
                    }
                } else if (obj.has("locator")) {
                    // Legacy single-object format
                    HealingResult r = parseSingleCandidate(obj);
                    if (r != null) results.add(r);
                }
            } else if (root.isJsonArray()) {
                // Bare array format: [{...}, {...}]
                for (com.google.gson.JsonElement el : root.getAsJsonArray()) {
                    HealingResult r = parseSingleCandidate(el.getAsJsonObject());
                    if (r != null) results.add(r);
                }
            }
        } catch (Exception e) {
            Reporter.log("Failed to parse AI healing response: " + e.getMessage(), LogLevel.ERROR);
        }
        return results;
    }

    private static HealingResult parseSingleCandidate(com.google.gson.JsonObject json) {
        try {
            String locator = json.get("locator").getAsString();
            double confidence = json.has("confidence") ? json.get("confidence").getAsDouble() : 0.5;
            String reasoning = json.has("reasoning") ? json.get("reasoning").getAsString() : "";
            return new HealingResult(locator, confidence, reasoning);
        } catch (Exception e) {
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
    }

    /** Returns the global healed cache (for use by BaseActions findWebElements). */
    public static ConcurrentHashMap<String, CachedLocator> getGlobalHealedCache() {
        return globalHealedCache;
    }
}
