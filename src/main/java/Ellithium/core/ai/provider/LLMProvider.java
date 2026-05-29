package Ellithium.core.ai.provider;

/**
 * A generic interface for interacting with any Language Model (LLM).
 * This allows the framework to be provider-agnostic, supporting models
 * from OpenAI, Anthropic, Google, local open-source models (via Ollama),
 * Chinese open-source models (DeepSeek, Qwen), or custom internal endpoints.
 *
 * <p>Implementations must provide at least the basic {@link #ask(String)} method.
 * System prompt and vision capabilities have default implementations that
 * providers can override when supported.</p>
 */
public interface LLMProvider {

    /**
     * Sends a user prompt to the LLM and returns the generated response.
     *
     * @param prompt The user's instructions/context for the LLM
     * @return The AI-generated text response
     */
    String ask(String prompt);

    /**
     * Sends a system prompt and a user prompt to the LLM.
     * The system prompt defines the AI's persona and output format rules.
     * The user prompt contains the actual task (e.g., DOM + broken locator).
     *
     * @param systemPrompt Instructions defining the LLM's behavior and output format
     * @param userPrompt   The actual task/question for the LLM
     * @return The AI-generated text response
     */
    default String ask(String systemPrompt, String userPrompt) {
        // Default: concatenate prompts. Providers should override for proper role separation.
        return ask(systemPrompt + "\n\n" + userPrompt);
    }

    /**
     * Sends a prompt along with a screenshot image for vision-capable models.
     * Used for visual RCA (Root Cause Analysis) and layout validation.
     *
     * @param prompt     The text prompt describing what to analyze
     * @param screenshot The screenshot image as a byte array (PNG/JPEG)
     * @return The AI-generated text response
     * @throws UnsupportedOperationException if the provider does not support vision
     */
    default String askWithVision(String prompt, byte[] screenshot) {
        throw new UnsupportedOperationException(
                "Vision is not supported by this LLM provider: " + getModelName());
    }

    /**
     * Identifies the name or version of the underlying model.
     *
     * @return The model identifier (e.g., "gpt-4o", "llama-3-8b", "deepseek-v2")
     */
    String getModelName();

    /**
     * Checks if this provider supports vision (multimodal) input.
     *
     * @return true if {@link #askWithVision(String, byte[])} is implemented
     */
    default boolean supportsVision() {
        return false;
    }
}
