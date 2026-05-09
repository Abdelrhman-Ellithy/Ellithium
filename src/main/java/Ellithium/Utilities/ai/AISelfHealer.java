package Ellithium.Utilities.ai;

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
    private static LLMProvider getEffectiveProvider() {
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
        String llmResponse = provider.ask(systemPrompt, userPrompt);

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

        // HEAL_AND_CONTINUE or HEAL_AND_NOTIFY: rewrite the source file
        SourceLocation sourceLocation = resolveSourceLocation(stackTrace);
        if (sourceLocation != null) {
            boolean written = JavaSourceModifier.updateLocatorValue(
                    sourceLocation.filePath, sourceLocation.fieldName, result.getNewLocatorExpression());
            if (written) {
                Reporter.log("AI Self-Healing: POM file updated successfully", LogLevel.INFO_GREEN);
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
                return driver.findElement(newLocator);
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
     * Resolves the POM source file path and field name from the stack trace.
     */
    private static SourceLocation resolveSourceLocation(StackTraceElement[] stackTrace) {
        // Walk the stack to find the first frame from the user's Page Object (not framework code)
        for (StackTraceElement frame : stackTrace) {
            String className = frame.getClassName();
            if (!className.startsWith("Ellithium.")
                    && !className.startsWith("org.openqa.selenium")
                    && !className.startsWith("java.")
                    && !className.startsWith("sun.")) {
                // This is likely the user's POM class
                String filePath = "src/main/java/" + className.replace('.', '/') + ".java";
                // Field name resolution requires additional AST analysis
                return new SourceLocation(filePath, null);
            }
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
