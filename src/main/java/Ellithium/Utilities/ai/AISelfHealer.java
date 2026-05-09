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

/**
 * Orchestrator responsible for handling failed locators, querying the AI provider
 * for a fix, and applying the AST modification to permanently heal the source code.
 *
 * <p>Thread-safe: uses ThreadLocal for per-thread provider/strategy configuration,
 * consistent with Ellithium's DriverFactory pattern.</p>
 *
 * <p>This class is invoked ONLY when a locator fails. During normal execution,
 * it has zero overhead (zero tokens, zero latency).</p>
 */
public class AISelfHealer {

    private static final ThreadLocal<LLMProvider> llmProviderThread = new ThreadLocal<>();
    private static final ThreadLocal<HealingStrategy> strategyThread = new ThreadLocal<>();

    // Global defaults (set once at startup, used as fallback if thread-local is not set)
    private static volatile LLMProvider globalProvider;
    private static volatile HealingStrategy globalStrategy = HealingStrategy.DISABLED;
    private static volatile double confidenceThreshold = 0.85;

    // Runtime locator cache: avoids re-calling the AI for the same broken locator
    private static final java.util.concurrent.ConcurrentHashMap<String, By> healedLocatorCache
            = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Initializes the self-healer globally. Called once at framework startup.
     *
     * @param provider  The AI provider implementation
     * @param strategy  The healing strategy
     * @param threshold Minimum confidence score (0.0–1.0) to allow automatic healing
     */
    public static void initialize(LLMProvider provider, HealingStrategy strategy, double threshold) {
        globalProvider = provider;
        globalStrategy = strategy;
        confidenceThreshold = threshold;
        Reporter.log("AI Self-Healing initialized | Strategy: " + strategy.name()
                + " | Model: " + provider.getModelName()
                + " | Confidence Threshold: " + threshold, LogLevel.INFO_YELLOW);
    }

    /**
     * Initializes the self-healer globally with default confidence threshold (0.85).
     *
     * @param provider The AI provider implementation
     * @param strategy The healing strategy
     */
    public static void initialize(LLMProvider provider, HealingStrategy strategy) {
        initialize(provider, strategy, 0.85);
    }

    /**
     * Overrides the provider and strategy for the current thread only.
     * Useful for parallel execution with different AI configurations per thread.
     *
     * @param provider The AI provider for this thread
     * @param strategy The healing strategy for this thread
     */
    public static void initializeForThread(LLMProvider provider, HealingStrategy strategy) {
        llmProviderThread.set(provider);
        strategyThread.set(strategy);
    }

    /**
     * Returns the effective strategy for the current thread.
     */
    private static HealingStrategy getEffectiveStrategy() {
        HealingStrategy threadStrategy = strategyThread.get();
        return threadStrategy != null ? threadStrategy : globalStrategy;
    }

    /**
     * Returns the effective LLM provider for the current thread.
     */
    public static LLMProvider getEffectiveProvider() {
        LLMProvider threadProvider = llmProviderThread.get();
        return threadProvider != null ? threadProvider : globalProvider;
    }

    /**
     * Attempts to heal a broken locator.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Capture a minimized DOM snapshot (via {@link Ellithium.Utilities.ai.sanitizers.DOMMinimizer})</li>
     *   <li>Scrub PII (via {@link Ellithium.Utilities.ai.sanitizers.DataScrubber})</li>
     *   <li>Query the LLM with the broken locator + DOM context</li>
     *   <li>Parse the response into a {@link HealingResult} with confidence score</li>
     *   <li>If confidence &gt;= threshold AND strategy allows, rewrite the POM file</li>
     *   <li>Return the healed WebElement, or null if healing was not performed</li>
     * </ol>
     *
     * @param driver        Current WebDriver instance
     * @param brokenLocator The original 'By' locator that failed
     * @param stackTrace    The stack trace (used to locate the source .java file and field name)
     * @return The found WebElement if healed successfully, or null
     */
    public static WebElement attemptHeal(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace) {
        HealingStrategy strategy = getEffectiveStrategy();
        LLMProvider provider = getEffectiveProvider();

        if (strategy == HealingStrategy.DISABLED || provider == null) {
            return null;
        }

        Reporter.log("AI Self-Healing triggered for locator: " + brokenLocator.toString(), LogLevel.INFO_YELLOW);

        // Fast path: check runtime cache first
        By cached = healedLocatorCache.get(brokenLocator.toString());
        if (cached != null) {
            try {
                WebElement el = driver.findElement(cached);
                Reporter.log("AI Self-Healing (cached): reusing healed locator " + cached, LogLevel.INFO_YELLOW);
                return el;
            } catch (Exception ignored) {
                healedLocatorCache.remove(brokenLocator.toString());
            }
        }

        // Step 1: Detect Mobile Context
        boolean isMobile = driver instanceof AppiumDriver;

        // Step 2: Capture and minimize DOM
        String rawDom = driver.getPageSource();
        String minimizedDom = Ellithium.Utilities.ai.sanitizers.DOMMinimizer.minimize(rawDom);

        // Step 3: Scrub PII
        String safeDom = Ellithium.Utilities.ai.sanitizers.DataScrubber.scrub(minimizedDom);

        // Step 4: Build prompt and query LLM
        String systemPrompt = buildSystemPrompt(isMobile);
        String userPrompt = buildUserPrompt(brokenLocator.toString(), safeDom);
        String llmResponse;
        try {
            llmResponse = provider.ask(systemPrompt, userPrompt);
        } catch (Exception e) {
            Reporter.log("AI Self-Healing: LLM Provider failed - " + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        // Step 5: Parse response into HealingResult
        HealingResult result = parseHealingResponse(llmResponse);
        if (result == null) {
            Reporter.log("AI Self-Healing: Failed to parse LLM response", LogLevel.ERROR);
            return null;
        }

        Reporter.log("AI Healing Result: " + result.toString(), LogLevel.INFO_BLUE);

        // Step 5: Check confidence threshold
        if (!result.isConfidentEnough(confidenceThreshold)) {
            Reporter.log("AI Healing confidence (" + String.format("%.2f", result.getConfidence())
                    + ") is below threshold (" + confidenceThreshold
                    + "). Forcing SUGGEST_ONLY mode.", LogLevel.INFO_YELLOW);
            Reporter.log("[AI Suggestion] " + result.getNewLocatorExpression()
                    + " | Reason: " + result.getReasoning(), LogLevel.INFO_BLUE);
            return null;
        }

        // Step 6: Apply strategy
        if (strategy == HealingStrategy.SUGGEST_ONLY) {
            Reporter.log("[AI Suggestion] " + result.getNewLocatorExpression()
                    + " | Confidence: " + String.format("%.2f", result.getConfidence())
                    + " | Reason: " + result.getReasoning(), LogLevel.INFO_BLUE);
            return null;
        }

        // HEAL_AND_CONTINUE or HEAL_AND_NOTIFY
        SourceLocation sourceLocation = resolveSourceLocation(stackTrace);
        
        if (AIConfigLoader.isCI()) {
            // CI Mode: Do not modify source files directly. Queue for the report.
            String filePath = sourceLocation != null ? sourceLocation.filePath : "unknown file";
            AIHealingReporter.queueChange(filePath, brokenLocator.toString(), result);
        } else {
            // LOCAL Mode: Rewrite the source file
            if (sourceLocation != null) {
                boolean written = JavaSourceModifier.updateLocatorValue(
                        sourceLocation.filePath, sourceLocation.fieldName, result.getNewLocatorExpression());
                if (written) {
                    Reporter.log("AI Self-Healing: POM file updated successfully", LogLevel.INFO_GREEN);
                }
            }
        }

        if (strategy == HealingStrategy.HEAL_AND_NOTIFY) {
            Reporter.log("[AI HEALED & NOTIFIED] Locator healed: " + brokenLocator
                    + " → " + result.getNewLocatorExpression()
                    + " | File: " + (sourceLocation != null ? sourceLocation.filePath : "unknown"), LogLevel.INFO_YELLOW);
        }

        // Step 7: Try to find the element with the new locator
        try {
            By newLocator = parseByFromExpression(result.getNewLocatorExpression());
            if (newLocator != null) {
                WebElement el = driver.findElement(newLocator);
                // Cache the healed locator for future calls
                healedLocatorCache.put(brokenLocator.toString(), newLocator);
                return el;
            }
        } catch (Exception e) {
            Reporter.log("AI Self-Healing: New locator also failed: " + e.getMessage(), LogLevel.ERROR);
        }

        return null;
    }

    /**
     * Builds the system prompt that teaches the LLM Ellithium's conventions.
     */
    private static String buildSystemPrompt(boolean isMobile) {
        String prompt = "You are an expert Selenium and Appium test automation engineer. "
                + "You are given a broken locator and a " + (isMobile ? "Mobile XML tree" : "HTML DOM snippet") + ". "
                + "Your job is to find the correct new locator for the same UI element. "
                + "Respond ONLY in this JSON format: "
                + "{\"locator\": \"By.id(\\\"...\\\")\", \"confidence\": 0.95, \"reasoning\": \"...\"} ";
        
        if (isMobile) {
            prompt += "Use AppiumBy.accessibilityId, AppiumBy.androidUIAutomator, AppiumBy.iOSClassChain, By.id, or By.xpath. ";
        } else {
            prompt += "Use By.cssSelector, By.id, By.xpath, By.name, or By.className. ";
        }
        
        prompt += "If the element genuinely does not exist, set confidence to 0.0.";
        return prompt;
    }

    /**
     * Builds the user prompt with the broken locator and minimized DOM.
     */
    private static String buildUserPrompt(String brokenLocator, String domSnippet) {
        return "The following locator has failed:\n"
                + brokenLocator + "\n\n"
                + "Here is the current (minimized) DOM:\n"
                + domSnippet;
    }

    /**
     * Parses the LLM's JSON response into a HealingResult.
     */
    private static HealingResult parseHealingResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        try {
            // Simple JSON extraction — production should use Gson
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

    /**
     * Resolves the POM source file path AND the exact field name from the stack trace.
     * Uses the exact line number from the stack frame to read the source file and
     * scan upward from the call site to find the By field declaration that holds the broken locator.
     */
    private static SourceLocation resolveSourceLocation(StackTraceElement[] stackTrace) {
        for (StackTraceElement frame : stackTrace) {
            String className = frame.getClassName();
            // Skip framework, Selenium, and JDK internals
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

            // Derive file path from class name (try test directory first, then main)
            String classFilePart = className.replace('.', '/') + ".java";
            String resolvedPath = null;
            for (String root : new String[]{"src/test/java/", "src/main/java/"}) {
                String candidate = root + classFilePart;
                if (new java.io.File(candidate).exists()) {
                    resolvedPath = candidate;
                    break;
                }
            }
            if (resolvedPath == null) {
                continue; // file not found in either location
            }

            // Read the source file and scan upward from the stack frame's line number
            // to find the By field declaration that was being used
            int callSiteLine = frame.getLineNumber();
            String fieldName = resolveFieldNameFromSource(resolvedPath, callSiteLine);

            Reporter.log("AI Healer: Located source at " + resolvedPath
                    + ":" + callSiteLine
                    + (fieldName != null ? " → field '" + fieldName + "'" : ""), LogLevel.INFO_BLUE);
            return new SourceLocation(resolvedPath, fieldName);
        }
        return null;
    }

    /**
     * Reads the source file around the given line number to find the By field
     * declaration that the call at that line is using.
     *
     * Strategy:
     * 1. Read the file line at callSiteLine — it may directly contain a By.xxx call inline.
     * 2. If not, look at the token on the call site line that matches a field reference
     *    and scan upward through the file to find "private ... By fieldName = ..." or
     *    "By fieldName = ..." to extract fieldName.
     */
    private static String resolveFieldNameFromSource(String filePath, int callSiteLine) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(
                    java.nio.file.Paths.get(filePath));

            if (callSiteLine < 1 || callSiteLine > lines.size()) return null;

            // Line at the call site (0-indexed)
            String callLine = lines.get(callSiteLine - 1).trim();

            // Pattern: look for a simple variable reference like "someField" or "this.someField"
            // We try to extract the identifier used as locator argument
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?:this\\.)?([a-zA-Z_][a-zA-Z0-9_]*)")
                    .matcher(callLine);
            java.util.List<String> candidates = new java.util.ArrayList<>();
            while (m.find()) candidates.add(m.group(1));

            // Walk upward from call site looking for "By <candidate>" declarations
            for (int i = callSiteLine - 2; i >= 0; i--) {
                String srcLine = lines.get(i).trim();
                // Match field declarations like: private final By sortBtn = By.cssSelector(...);
                java.util.regex.Matcher fieldMatcher = java.util.regex.Pattern
                        .compile("(?:private|protected|public)?\\s*(?:final\\s+)?By\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=")
                        .matcher(srcLine);
                if (fieldMatcher.find()) {
                    String foundField = fieldMatcher.group(1);
                    // Prefer a candidate that matches a field referenced on the call site
                    if (candidates.contains(foundField)) {
                        return foundField;
                    }
                    // Otherwise, return the first matching field within reasonable range
                    if (callSiteLine - i <= 50) {
                        return foundField;
                    }
                }
            }
        } catch (Exception e) {
            Reporter.log("AI Healer: Could not read source file for field resolution: " + e.getMessage(), LogLevel.WARN);
        }
        return null;
    }

    /**
     * Converts a By expression string like "By.cssSelector(\"#login\")" into an actual By object.
     */
    private static By parseByFromExpression(String expression) {
        try {
            if (expression.startsWith("By.id(")) {
                String value = extractValue(expression);
                return By.id(value);
            } else if (expression.startsWith("By.cssSelector(")) {
                String value = extractValue(expression);
                return By.cssSelector(value);
            } else if (expression.startsWith("By.xpath(")) {
                String value = extractValue(expression);
                return By.xpath(value);
            } else if (expression.startsWith("By.name(")) {
                String value = extractValue(expression);
                return By.name(value);
            } else if (expression.startsWith("By.className(")) {
                String value = extractValue(expression);
                return By.className(value);
            } else if (expression.startsWith("By.linkText(")) {
                String value = extractValue(expression);
                return By.linkText(value);
            } else if (expression.startsWith("By.partialLinkText(")) {
                String value = extractValue(expression);
                return By.partialLinkText(value);
            } else if (expression.startsWith("By.tagName(")) {
                String value = extractValue(expression);
                return By.tagName(value);
            } else if (expression.startsWith("AppiumBy.accessibilityId(")) {
                String value = extractValue(expression);
                return AppiumBy.accessibilityId(value);
            } else if (expression.startsWith("AppiumBy.androidUIAutomator(")) {
                String value = extractValue(expression);
                return AppiumBy.androidUIAutomator(value);
            } else if (expression.startsWith("AppiumBy.androidViewTag(")) {
                String value = extractValue(expression);
                return AppiumBy.androidViewTag(value);
            } else if (expression.startsWith("AppiumBy.androidDataMatcher(")) {
                String value = extractValue(expression);
                return AppiumBy.androidDataMatcher(value);
            } else if (expression.startsWith("AppiumBy.iOSClassChain(")) {
                String value = extractValue(expression);
                return AppiumBy.iOSClassChain(value);
            } else if (expression.startsWith("AppiumBy.iOSNsPredicateString(")) {
                String value = extractValue(expression);
                return AppiumBy.iOSNsPredicateString(value);
            } else if (expression.startsWith("AppiumBy.image(")) {
                String value = extractValue(expression);
                return AppiumBy.image(value);
            } else if (expression.startsWith("AppiumBy.custom(")) {
                String value = extractValue(expression);
                return AppiumBy.custom(value);
            }
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

    /**
     * Internal DTO for holding resolved source file location.
     */
    private static class SourceLocation {
        final String filePath;
        final String fieldName;

        SourceLocation(String filePath, String fieldName) {
            this.filePath = filePath;
            this.fieldName = fieldName;
        }
    }

    /**
     * Cleans up ThreadLocal references. Call this in test teardown.
     */
    public static void cleanup() {
        llmProviderThread.remove();
        strategyThread.remove();
    }
}
