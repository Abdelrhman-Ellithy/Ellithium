package Ellithium.core.ai.config;

import Ellithium.Utilities.ai.HealingStrategy;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;

/**
 * Reads AI-related configuration from Ellithium's standard {@code ai-config.properties} file.
 */
public class AIConfigLoader {

    private static HealingStrategy healingStrategy    = HealingStrategy.DISABLED;
    private static double confidenceThreshold         = 0.70;
    private static double healingStoreThreshold       = 0.85;
    private static int    maxCandidates               = 3;
    private static String llmProviderName             = "";
    private static String llmApiKey                   = "";
    private static String llmModel                    = "";
    private static String llmBaseUrl                  = "";
    private static String llmProviderClass            = "";
    private static ExecutionMode executionMode        = ExecutionMode.LOCAL;
    private static boolean visionRcaEnabled           = false;
    private static double  onnxSimilarityThreshold    = 0.60;
    private static int     onnxMaxCandidates          = 15;
    private static double  tier3BaselineMatchFloor    = 0.40;
    private static int     onnxHardCandidateLimit     = 300;
    private static int     baselineTtlDays            = 30;
    private static double  semanticFallbackScore      = 0.65;
    private static boolean visionAllowMobile          = false;
    private static boolean visionAllowWeb             = false;
    private static int     llmHealMaxWaitMs           = 15_000;
    private static int     llmMaxRetries              = 3;
    private static int     telemetryMaxRecords        = 100_000;
    private static boolean tier3Enabled               = true;
    private static int     ciHealAlertThreshold       = -1;
    private static int     baselineMaxLocators        = 0;

    private static volatile boolean initialized = false;

    public enum ExecutionMode {
        LOCAL,
        CI
    }

    public static synchronized void initialize() {
        if (initialized) return;
        try {
            String p = ConfigContext.getAiFilePath();

            healingStrategy       = parseEnum(p, "ai.healing.strategy", HealingStrategy.class, healingStrategy);
            confidenceThreshold   = clamp01("ai.healing.confidenceThreshold",
                    parseDouble(p, "ai.healing.confidenceThreshold", confidenceThreshold));
            healingStoreThreshold = clamp01("ai.healing.storeThreshold",
                    parseDouble(p, "ai.healing.storeThreshold", healingStoreThreshold));

            int mc = parseInt(p, "ai.healing.maxCandidates", maxCandidates);
            if (mc >= 1 && mc <= 10) maxCandidates = mc;
            else Logger.warn("ai.healing.maxCandidates must be between 1-10. Using " + maxCandidates + ".");

            llmProviderName  = getPropertyOrDefault(p, "ai.llm.provider", "");
            String rawKey    = getPropertyOrDefault(p, "ai.llm.apiKey", "");
            if (rawKey.contains("${")) {
                Logger.warn("ai.llm.apiKey contains an unresolved placeholder '" + rawKey
                        + "' — set the corresponding environment variable. LLM healing disabled.");
                rawKey = "";
            }
            llmApiKey        = rawKey;
            llmModel         = getPropertyOrDefault(p, "ai.llm.model", "");
            llmBaseUrl       = getPropertyOrDefault(p, "ai.llm.baseUrl", "");
            llmProviderClass = getPropertyOrDefault(p, "ai.llm.providerClass", "");

            executionMode    = parseEnum(p, "ai.execution.mode", ExecutionMode.class, executionMode);
            visionRcaEnabled = parseBool(p, "ai.vision.rca.enabled", visionRcaEnabled);

            onnxSimilarityThreshold     = parseDouble(p, "ai.onnx.similarityThreshold", onnxSimilarityThreshold);
            onnxMaxCandidates           = parseInt(p, "ai.onnx.maxCandidates", onnxMaxCandidates);
            onnxHardCandidateLimit      = parseInt(p, "ai.onnx.hardCandidateLimit", onnxHardCandidateLimit);
            tier3BaselineMatchFloor     = parseDouble(p, "ai.healing.tier3BaselineMatchFloor", tier3BaselineMatchFloor);
            visionAllowMobile           = parseBool(p, "ai.vision.allowMobile", visionAllowMobile);
            visionAllowWeb              = parseBool(p, "ai.vision.allowWeb", visionAllowWeb);
            baselineTtlDays             = parseInt(p, "ai.healing.baselineTtlDays", baselineTtlDays);
            semanticFallbackScore       = parseDouble(p, "ai.healing.semanticFallbackScore", semanticFallbackScore);
            llmHealMaxWaitMs            = parseInt(p, "ai.llm.healMaxWaitMs", llmHealMaxWaitMs);
            llmMaxRetries               = parseInt(p, "ai.llm.maxRetries", llmMaxRetries);
            telemetryMaxRecords         = parseInt(p, "ai.telemetry.maxRecords", telemetryMaxRecords);
            tier3Enabled                = parseBool(p, "ai.tier3.enabled", tier3Enabled);
            ciHealAlertThreshold        = parseInt(p, "ai.healing.ciAlertThreshold", ciHealAlertThreshold);
            baselineMaxLocators         = parseInt(p, "ai.healing.baselineMaxLocators", baselineMaxLocators);

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
        String value = raw(configPath, key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private static String raw(String configPath, String key) {
        String value = PropertyHelper.getDataFromProperties(configPath, key);
        return (value != null) ? resolveEnvironmentVariables(value).trim() : null;
    }

    private static double parseDouble(String configPath, String key, double def) {
        String v = raw(configPath, key);
        if (v == null || v.isEmpty()) return def;
        try { return Double.parseDouble(v); }
        catch (NumberFormatException e) { Logger.warn("Invalid " + key + ": " + v + ". Using " + def + "."); return def; }
    }

    private static int parseInt(String configPath, String key, int def) {
        String v = raw(configPath, key);
        if (v == null || v.isEmpty()) return def;
        try { return Integer.parseInt(v); }
        catch (NumberFormatException e) { Logger.warn("Invalid " + key + ": " + v + ". Using " + def + "."); return def; }
    }

    private static boolean parseBool(String configPath, String key, boolean def) {
        String v = raw(configPath, key);
        return (v == null || v.isEmpty()) ? def : Boolean.parseBoolean(v);
    }

    private static double clamp01(String key, double v) {
        if (v < 0.0 || v > 1.0) {
            Logger.warn(key + "=" + v + " is outside [0.0, 1.0] — clamped.");
            return Math.max(0.0, Math.min(1.0, v));
        }
        return v;
    }

    private static <E extends Enum<E>> E parseEnum(String configPath, String key, Class<E> type, E def) {
        String v = raw(configPath, key);
        if (v == null || v.isEmpty()) return def;
        try { return Enum.valueOf(type, v.toUpperCase()); }
        catch (IllegalArgumentException e) { Logger.warn("Invalid " + key + ": " + v + ". Using " + def + "."); return def; }
    }

    private static String resolveEnvironmentVariables(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.contains("${") && value.contains("}")) {
            String resolvedValue = value;
            int startIndex = 0;
            while ((startIndex = resolvedValue.indexOf("${", startIndex)) != -1) {
                int endIndex = resolvedValue.indexOf("}", startIndex);
                if (endIndex == -1) break;
                String placeholder = resolvedValue.substring(startIndex, endIndex + 1);
                String envVarName  = resolvedValue.substring(startIndex + 2, endIndex);
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

    public static HealingStrategy getHealingStrategy()          { return healingStrategy; }
    public static double getConfidenceThreshold()               { return confidenceThreshold; }
    public static double getHealingStoreThreshold()             { return healingStoreThreshold; }
    public static String getLlmProviderName()                   { return llmProviderName; }
    public static String getLlmApiKey()                         { return llmApiKey; }
    public static String getLlmModel()                          { return llmModel; }
    public static String getLlmBaseUrl()                        { return llmBaseUrl; }
    public static String getLlmProviderClass()                  { return llmProviderClass; }
    public static ExecutionMode getExecutionMode()              { return executionMode; }
    public static boolean isCI()                                { return executionMode == ExecutionMode.CI; }
    public static boolean isVisionRcaEnabled()                  { return visionRcaEnabled; }
    public static int    getMaxCandidates()                     { return maxCandidates; }
    public static double getOnnxSimilarityThreshold()           { return onnxSimilarityThreshold; }
    public static int    getOnnxMaxCandidates()                 { return onnxMaxCandidates; }
    public static int    getOnnxHardCandidateLimit()            { return onnxHardCandidateLimit; }
    public static boolean isVisionAllowedOnMobile()             { return visionAllowMobile; }
    public static boolean isVisionAllowedOnWeb()                { return visionAllowWeb; }
    public static int    getBaselineTtlDays()                   { return baselineTtlDays; }
    public static double getSemanticFallbackScore()             { return semanticFallbackScore; }
    public static int    getLlmHealMaxWaitMs()                  { return llmHealMaxWaitMs; }
    public static int    getLlmMaxRetries()                     { return llmMaxRetries; }
    public static int    getTelemetryMaxRecords()               { return telemetryMaxRecords; }
    public static double getTier3BaselineMatchFloor()           { return tier3BaselineMatchFloor; }
    public static boolean isTier3Enabled()                      { return tier3Enabled; }
    public static int    getCiHealAlertThreshold()              { return ciHealAlertThreshold; }
    public static int    getBaselineMaxLocators()               { return baselineMaxLocators; }
}
