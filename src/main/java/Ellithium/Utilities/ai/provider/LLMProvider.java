package Ellithium.Utilities.ai.provider;

/**
 * A generic interface for interacting with any Language Model (LLM).
 * This allows the framework to be provider-agnostic, supporting models
 * from OpenAI, Anthropic, Google, local open-source models (via Ollama),
 * or custom internal endpoints.
 */
public interface LLMProvider {

    /**
     * Sends a text prompt to the LLM and returns the generated response.
     *
     * @param prompt The complete instructions/context for the LLM
     * @return The AI-generated text response
     */
    String ask(String prompt);

    /**
     * Identifies the name or version of the underlying model (e.g., "gpt-4o", "llama-3-8b").
     *
     * @return The model identifier
     */
    String getModelName();
}
