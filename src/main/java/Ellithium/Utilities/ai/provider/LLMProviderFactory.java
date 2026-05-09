package Ellithium.Utilities.ai.provider;

import Ellithium.Utilities.ai.config.AIConfigLoader;
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

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            Reporter.log("AI API Key is missing or default! AI self-healing will be disabled.", LogLevel.WARN);
            return null;
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
}
