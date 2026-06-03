package Ellithium.Utilities.ai;

import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

public class LLMProviderFactory {
    
    /**
     * Creates and returns the appropriate LLM provider based on ai-config.properties.
     * Supports specific providers (Gemini, Anthropic) and defaults to an 
     * OpenAI-compatible format which supports Qwen, DeepSeek, Groq, ChatGPT, and local LLMs.
     */
    public static LLMProvider createProvider() {
        String providerName = AIConfigLoader.getLlmProviderName().trim().toLowerCase();
        String baseUrl = AIConfigLoader.getLlmBaseUrl();
        String apiKey = AIConfigLoader.getLlmApiKey();
        String model = AIConfigLoader.getLlmModel();

        if (apiKey == null || apiKey.isEmpty()) {
            Reporter.log("AI API Key is missing or default! AI self-healing will be disabled.", LogLevel.WARN);
            return null;
        }

        String providerClass = AIConfigLoader.getLlmProviderClass();
        if (providerClass != null && !providerClass.isEmpty()) {
            LLMProvider custom = instantiate(providerClass, baseUrl, apiKey, model);
            if (custom != null) return custom;
        }

        switch (providerName) {
            case "gemini":
            case "google":
                return new GeminiProvider(baseUrl, apiKey, model);
            case "claude":
            case "anthropic":
                return new AnthropicProvider(baseUrl, apiKey, model);
            case "openai":
            case "chatgpt":
            case "groq":
            case "grok":
            case "qwen":
            case "deepseek":
            case "local":
            default:
                // Universal fallback for all models that follow the standard OpenAI API structure
                return new OpenAICompatibleProvider(baseUrl, apiKey, model);
        }
    }

    private static LLMProvider instantiate(String className, String baseUrl, String apiKey, String model) {
        try {
            Class<?> cls = Class.forName(className);
            Object instance = cls.getConstructor(String.class, String.class, String.class)
                    .newInstance(baseUrl, apiKey, model);
            if (instance instanceof LLMProvider provider) return provider;
            Reporter.log("ai.llm.providerClass " + className
                    + " does not implement LLMProvider — falling back to built-in providers", LogLevel.WARN);
        } catch (Exception e) {
            Reporter.log("Failed to load custom ai.llm.providerClass " + className
                    + ": " + e.getMessage() + " — falling back to built-in providers", LogLevel.WARN);
        }
        return null;
    }
}
