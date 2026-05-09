package Ellithium.Utilities.ai.provider;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiProvider implements LLMProvider {
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    public GeminiProvider(String baseUrl, String apiKey, String model) {
        this.baseUrl = (baseUrl == null || baseUrl.isEmpty()) ? "https://generativelanguage.googleapis.com/v1beta/" : baseUrl;
        this.apiKey = apiKey;
        this.model = (model == null || model.isEmpty()) ? "gemini-1.5-flash" : model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String ask(String prompt) {
        return ask("You are an expert Automation QA Engineer.", prompt);
    }

    @Override
    public String ask(String systemPrompt, String userPrompt) {
        try {
            JSONObject payload = new JSONObject();

            // Gemini system instruction format
            JSONObject systemInstruction = new JSONObject();
            systemInstruction.put("parts", new JSONArray().put(new JSONObject().put("text", systemPrompt)));
            payload.put("systemInstruction", systemInstruction);

            JSONArray contents = new JSONArray();
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            userContent.put("parts", new JSONArray().put(new JSONObject().put("text", userPrompt)));
            contents.put(userContent);
            payload.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.0);
            payload.put("generationConfig", generationConfig);

            String url = baseUrl;
            if (!url.endsWith("/")) url += "/";
            // Check if model name already starts with models/
            String modelPath = model.startsWith("models/") ? model : "models/" + model;
            url += modelPath + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray candidates = jsonResponse.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                     return candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                }
                Reporter.log("Gemini API Empty Response: " + response.body(), LogLevel.ERROR);
                return null;
            } else {
                Reporter.log("Gemini API Error: " + response.statusCode() + " - " + response.body(), LogLevel.ERROR);
                return null;
            }
        } catch (Exception e) {
            Reporter.log("Failed to communicate with Gemini API: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    @Override
    public String getModelName() {
        return model;
    }
}
