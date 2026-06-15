package Ellithium.Utilities.ai;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LLMProviderFactoryTest {

    @Test
    public void gemini_selectedByName() {
        LLMProvider p = LLMProviderFactory.createProvider("gemini", "", "key", "model");
        Assert.assertNotNull(p);
        Assert.assertTrue(p instanceof GeminiProvider);
    }

    @Test
    public void google_alias_mapsToGemini() {
        Assert.assertTrue(LLMProviderFactory.createProvider("google", "", "key", "model") instanceof GeminiProvider);
    }

    @Test
    public void claude_alias_mapsToAnthropic() {
        Assert.assertTrue(LLMProviderFactory.createProvider("claude", "", "key", "model") instanceof AnthropicProvider);
    }

    @Test
    public void anthropic_alias_mapsToAnthropic() {
        Assert.assertTrue(LLMProviderFactory.createProvider("anthropic", "", "key", "model") instanceof AnthropicProvider);
    }

    @Test
    public void openai_alias_mapsToOpenAICompatible() {
        Assert.assertTrue(LLMProviderFactory.createProvider("openai", "", "key", "model") instanceof OpenAICompatibleProvider);
    }

    @Test
    public void groq_alias_mapsToOpenAICompatible() {
        Assert.assertTrue(LLMProviderFactory.createProvider("groq", "", "key", "model") instanceof OpenAICompatibleProvider);
    }

    @Test
    public void deepseek_alias_mapsToOpenAICompatible() {
        Assert.assertTrue(LLMProviderFactory.createProvider("deepseek", "", "key", "model") instanceof OpenAICompatibleProvider);
    }

    @Test
    public void unknown_provider_fallsBackToOpenAICompatible() {
        Assert.assertTrue(LLMProviderFactory.createProvider("someunknown", "", "key", "model") instanceof OpenAICompatibleProvider);
    }

    @Test
    public void emptyApiKey_returnsNull() {
        Assert.assertNull(LLMProviderFactory.createProvider("gemini", "", "", "model"));
    }

    @Test
    public void nullApiKey_returnsNull() {
        Assert.assertNull(LLMProviderFactory.createProvider("gemini", "", null, "model"));
    }

    @Test
    public void localProvider_emptyKey_usesFallbackPlaceholder() {
        LLMProvider p = LLMProviderFactory.createProvider("local", "", "", "model");
        Assert.assertNotNull(p, "local must use placeholder bearer token and return a provider");
        Assert.assertTrue(p instanceof OpenAICompatibleProvider);
    }

    @Test
    public void invalidProviderClass_fallsBackToBuiltin() {
        LLMProvider p = LLMProviderFactory.createProvider("openai", "", "key", "model",
                "com.example.NonExistentProvider");
        Assert.assertNotNull(p);
        Assert.assertTrue(p instanceof OpenAICompatibleProvider);
    }

    @Test
    public void providerNameIsCaseInsensitive() {
        Assert.assertTrue(LLMProviderFactory.createProvider("GEMINI", "", "key", "model") instanceof GeminiProvider);
        Assert.assertTrue(LLMProviderFactory.createProvider("Anthropic", "", "key", "model") instanceof AnthropicProvider);
    }
}
