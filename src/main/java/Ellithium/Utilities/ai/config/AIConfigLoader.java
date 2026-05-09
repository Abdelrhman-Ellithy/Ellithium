package Ellithium.Utilities.ai.config;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;

/**
 * Reads AI-related configuration from Ellithium's standard {@code config.properties} file.
 * Follows the same pattern as {@code WaitManager.initializeTimeoutAndPolling()}.
 *
 * <p>Expected properties:</p>
 * <pre>
 * ai.healing.strategy=HEAL_AND_NOTIFY
 * ai.healing.confidenceThreshold=0.85
 * ai.llm.provider=openai
 * ai.llm.apiKey=${AI_API_KEY}
 * ai.llm.model=gpt-4o
 * ai.llm.baseUrl=https://api.openai.com/v1
 * ai.execution.mode=LOCAL
 * </pre>
 */
public class AIConfigLoader {

    private static HealingStrategy healingStrategy = HealingStrategy.DISABLED;
    private static double confidenceThreshold = 0.85;
    private static String llmProviderName = "";
    private static String llmApiKey = "";
    private static String llmModel = "";
    private static String llmBaseUrl = "";
    private static ExecutionMode executionMode = ExecutionMode.LOCAL;
    private static boolean visionRcaEnabled = false;
    private static boolean initialized = false;

    /**
     * Execution mode controls whether AI writes files directly or queues changes for review.
     */
    public enum ExecutionMode {
        /** Write healed files directly to disk (for local development) */
        LOCAL,
        /** Queue changes in memory and generate a report at suite end (for CI/CD) */
        CI
    }

    /**
     * Loads AI configuration from config.properties.
     * Safe to call multiple times — will only initialize once.
     */
    public static void initialize() {
        if (initialized) return;
        try {
            String configPath = ConfigContext.getConfigFilePath();

            String strategy = PropertyHelper.getDataFromProperties(configPath, "ai.healing.strategy");
            if (strategy != null && !strategy.isEmpty()) {
                try {
                    healingStrategy = HealingStrategy.valueOf(strategy.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    Logger.warn("Invalid ai.healing.strategy: " + strategy + ". Using DISABLED.");
                }
            }

            String threshold = PropertyHelper.getDataFromProperties(configPath, "ai.healing.confidenceThreshold");
            if (threshold != null && !threshold.isEmpty()) {
                try {
                    confidenceThreshold = Double.parseDouble(threshold.trim());
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid ai.healing.confidenceThreshold: " + threshold + ". Using 0.85.");
                }
            }

            llmProviderName = getPropertyOrDefault(configPath, "ai.llm.provider", "");
            llmApiKey = getPropertyOrDefault(configPath, "ai.llm.apiKey", "");
            llmModel = getPropertyOrDefault(configPath, "ai.llm.model", "");
            llmBaseUrl = getPropertyOrDefault(configPath, "ai.llm.baseUrl", "");

            String mode = PropertyHelper.getDataFromProperties(configPath, "ai.execution.mode");
            if (mode != null && !mode.isEmpty()) {
                try {
                    executionMode = ExecutionMode.valueOf(mode.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    Logger.warn("Invalid ai.execution.mode: " + mode + ". Using LOCAL.");
                }
            }

            String rca = PropertyHelper.getDataFromProperties(configPath, "ai.vision.rca.enabled");
            if (rca != null && !rca.isEmpty()) {
                visionRcaEnabled = Boolean.parseBoolean(rca.trim());
            }

            initialized = true;
            Reporter.log("AI Config loaded | Strategy: " + healingStrategy
                    + " | Mode: " + executionMode
                    + " | Vision RCA: " + (visionRcaEnabled ? "ENABLED" : "DISABLED")
                    + " | Provider: " + llmProviderName, LogLevel.INFO_YELLOW);

        } catch (Exception e) {
            Logger.warn("Could not load AI config from config.properties. AI features disabled. Error: " + e.getMessage());
        }
    }

    private static String getPropertyOrDefault(String configPath, String key, String defaultValue) {
        String value = PropertyHelper.getDataFromProperties(configPath, key);
        return (value != null && !value.isEmpty()) ? value.trim() : defaultValue;
    }

    public static HealingStrategy getHealingStrategy() { return healingStrategy; }
    public static double getConfidenceThreshold() { return confidenceThreshold; }
    public static String getLlmProviderName() { return llmProviderName; }
    public static String getLlmApiKey() { return llmApiKey; }
    public static String getLlmModel() { return llmModel; }
    public static String getLlmBaseUrl() { return llmBaseUrl; }
    public static ExecutionMode getExecutionMode() { return executionMode; }
    public static boolean isCI() { return executionMode == ExecutionMode.CI; }
    public static boolean isVisionRcaEnabled() { return visionRcaEnabled; }
}
