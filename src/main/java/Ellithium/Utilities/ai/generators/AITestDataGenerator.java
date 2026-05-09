package Ellithium.Utilities.ai.generators;

import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.provider.LLMProvider;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses the AI Engine to dynamically generate test data (fuzzing payloads)
 * for API endpoints or Database boundary testing.
 * 
 * <p>Returns edge cases based on a schema description.</p>
 */
public class AITestDataGenerator {

    private static final String FUZZING_PROMPT = 
            "You are an expert security and QA engineer. " +
            "Your task is to generate edge-case and boundary-value test data payloads based on the provided schema. " +
            "Include SQL injection attempts, XSS payloads, massive string lengths, negative numbers, nulls, and boundary dates where applicable. " +
            "Respond ONLY with a valid JSON array of objects representing the payloads. Do not include any explanations.";

    /**
     * Generates a list of fuzzing JSON payloads based on an input schema.
     * 
     * @param schemaDescription A description of the API schema (e.g., "User registration: username (string), age (int), email (string)")
     * @param count             Number of payloads to generate
     * @param provider          The LLM provider
     * @return A list of JSON payload strings
     */
    public static List<String> generateFuzzingData(String schemaDescription, int count, LLMProvider provider) {
        List<String> payloads = new ArrayList<>();
        
        if (provider == null) {
            Reporter.log("AITestDataGenerator skipped: No LLM Provider configured.", LogLevel.ERROR);
            return payloads;
        }

        if (count <= 0 || schemaDescription == null || schemaDescription.trim().isEmpty()) {
            Reporter.log("AITestDataGenerator skipped: Invalid schema or count.", LogLevel.WARN);
            return payloads;
        }

        try {
            Reporter.log("Generating " + count + " edge-case test data payloads via AI...", LogLevel.INFO_BLUE);
            
            String userPrompt = "Schema:\n" + schemaDescription + "\n\nGenerate exactly " + count + " diverse JSON payloads in a single JSON array format.";
            String llmResponse = provider.ask(FUZZING_PROMPT, userPrompt);
            
            // The LLM should return a JSON array
            JsonArray jsonArray = JsonParser.parseString(llmResponse).getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                payloads.add(jsonArray.get(i).getAsJsonObject().toString());
            }
            
            Reporter.log("Successfully generated " + payloads.size() + " payloads.", LogLevel.INFO_GREEN);
            
        } catch (Exception e) {
            Reporter.log("AITestDataGenerator failed: " + e.getMessage(), LogLevel.ERROR);
        }
        
        return payloads;
    }
}
