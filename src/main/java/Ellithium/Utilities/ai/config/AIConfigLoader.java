package Ellithium.Utilities.ai.config;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;

/**
 * Reads AI-related configuration from Ellithium's standard {@code ai-config.properties} file.
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
    private static double confidenceThreshold = 0.70;
    private static int maxCandidates = 3;
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
            String configPath = ConfigContext.getAiFilePath();

            String strategyRaw = PropertyHelper.getDataFromProperties(configPath, "ai.healing.strategy");
            String strategy = strategyRaw != null ? resolveEnvironmentVariables(strategyRaw) : null;
            if (strategy != null && !strategy.isEmpty()) {
                try {
                    healingStrategy = HealingStrategy.valueOf(strategy.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    Logger.warn("Invalid ai.healing.strategy: " + strategy + ". Using DISABLED.");
                }
            }

            String thresholdRaw = PropertyHelper.getDataFromProperties(configPath, "ai.healing.confidenceThreshold");
            String threshold = thresholdRaw != null ? resolveEnvironmentVariables(thresholdRaw) : null;
            if (threshold != null && !threshold.isEmpty()) {
                try {
                    confidenceThreshold = Double.parseDouble(threshold.trim());
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid ai.healing.confidenceThreshold: " + threshold + ". Using 0.70.");
                }
            }

            String maxCandidatesRaw = PropertyHelper.getDataFromProperties(configPath, "ai.healing.maxCandidates");
            String maxCandidatesStr = maxCandidatesRaw != null ? resolveEnvironmentVariables(maxCandidatesRaw) : null;
            if (maxCandidatesStr != null && !maxCandidatesStr.isEmpty()) {
                try {
                    int parsed = Integer.parseInt(maxCandidatesStr.trim());
                    if (parsed >= 1 && parsed <= 10) {
                        maxCandidates = parsed;
                    } else {
                        Logger.warn("ai.healing.maxCandidates must be between 1-10. Using default: 3.");
                    }
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid ai.healing.maxCandidates: " + maxCandidatesStr + ". Using 3.");
                }
            }

            llmProviderName = getPropertyOrDefault(configPath, "ai.llm.provider", "");
            llmApiKey = getPropertyOrDefault(configPath, "ai.llm.apiKey", "");
            llmModel = getPropertyOrDefault(configPath, "ai.llm.model", "");
            llmBaseUrl = getPropertyOrDefault(configPath, "ai.llm.baseUrl", "");

            String modeRaw = PropertyHelper.getDataFromProperties(configPath, "ai.execution.mode");
            String mode = modeRaw != null ? resolveEnvironmentVariables(modeRaw) : null;
            if (mode != null && !mode.isEmpty()) {
                try {
                    executionMode = ExecutionMode.valueOf(mode.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    Logger.warn("Invalid ai.execution.mode: " + mode + ". Using LOCAL.");
                }
            }

            String rcaRaw = PropertyHelper.getDataFromProperties(configPath, "ai.vision.rca.enabled");
            String rca = rcaRaw != null ? resolveEnvironmentVariables(rcaRaw) : null;
            if (rca != null && !rca.isEmpty()) {
                visionRcaEnabled = Boolean.parseBoolean(rca.trim());
            }

            initialized = true;
            Reporter.log("AI Config loaded | Strategy: " + healingStrategy
                    + " | Mode: " + executionMode
                    + " | Vision RCA: " + (visionRcaEnabled ? "ENABLED" : "DISABLED")
                    + " | Provider: " + llmProviderName, LogLevel.INFO_YELLOW);

        } catch (Exception e) {
            Logger.warn("Could not load AI config from ai-config.properties. AI features disabled. Error: " + e.getMessage());
        }
    }

    private static String getPropertyOrDefault(String configPath, String key, String defaultValue) {
        String value = PropertyHelper.getDataFromProperties(configPath, key);
        if (value != null && !value.isEmpty()) {
            return resolveEnvironmentVariables(value.trim());
        }
        return defaultValue;
    }

    /**
     * Resolves environment variables in a property value.
     * Replaces ${ENV_VAR} with actual environment variable values.
     */
    private static String resolveEnvironmentVariables(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (value.contains("${") && value.contains("}")) {
            String resolvedValue = value;
            int startIndex = 0;
            while ((startIndex = resolvedValue.indexOf("${", startIndex)) != -1) {
                int endIndex = resolvedValue.indexOf("}", startIndex);
                if (endIndex == -1) break;

                String placeholder = resolvedValue.substring(startIndex, endIndex + 1);
                String envVarName = resolvedValue.substring(startIndex + 2, endIndex);

                String envVarValue = System.getenv(envVarName);
                if (envVarValue == null) {
                    Logger.warn("Environment variable '" + envVarName + "' not found. Using placeholder as-is.");
                    envVarValue = placeholder;
                }

                resolvedValue = resolvedValue.replace(placeholder, envVarValue);
                startIndex += envVarValue.length();
            }
            return resolvedValue;
        }
        return value;
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
    public static int getMaxCandidates() { return maxCandidates; }
}
