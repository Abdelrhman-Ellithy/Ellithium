package Ellithium.Utilities.ai;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

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
                .connectTimeout(Duration.ofSeconds(60))
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
            url += modelPath + ":generateContent";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray candidates = jsonResponse.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject candidate = candidates.optJSONObject(0);
                    JSONObject content = candidate != null ? candidate.optJSONObject("content") : null;
                    JSONArray parts = content != null ? content.optJSONArray("parts") : null;
                    if (parts != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < parts.length(); i++) {
                            JSONObject part = parts.optJSONObject(i);
                            if (part != null && part.has("text")) sb.append(part.optString("text"));
                        }
                        if (!sb.isEmpty()) return stripMarkdownFences(sb.toString());
                    }
                    String finishReason = candidate != null ? candidate.optString("finishReason", "") : "";
                    if (!finishReason.isEmpty() && !"STOP".equalsIgnoreCase(finishReason)) {
                        Reporter.log("Gemini API returned no text (finishReason=" + finishReason + ") — switch to DEBUG for full response", LogLevel.ERROR);
                        Reporter.log("Gemini API body: " + response.body(), LogLevel.DEBUG);
                        return null;
                    }
                }
                Reporter.log("Gemini API returned empty response (HTTP 200) — switch to DEBUG for full response", LogLevel.ERROR);
                Reporter.log("Gemini API empty body: " + response.body(), LogLevel.DEBUG);
                return null;
            } else {
                Reporter.log("Gemini API Error: HTTP " + response.statusCode() + " — switch to DEBUG for full response", LogLevel.ERROR);
                Reporter.log("Gemini API error body: " + response.body(), LogLevel.DEBUG);
                return null;
            }
        } catch (java.net.http.HttpTimeoutException e) {
            Reporter.log("Gemini API Error: request timed out", LogLevel.ERROR);
            return null;
        } catch (Exception e) {
            Reporter.log("Failed to communicate with Gemini API: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    @Override
    public String askWithVision(String prompt, byte[] screenshot) {
        try {
            JSONObject payload = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");

            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", prompt));

            String base64Image = Base64.getEncoder().encodeToString(screenshot);
            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/png");
            inlineData.put("data", base64Image);
            parts.put(new JSONObject().put("inlineData", inlineData));

            userContent.put("parts", parts);
            contents.put(userContent);
            payload.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.0);
            payload.put("generationConfig", generationConfig);

            String url = baseUrl;
            if (!url.endsWith("/")) url += "/";
            String modelPath = model.startsWith("models/") ? model : "models/" + model;
            url += modelPath + ":generateContent";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray candidates = jsonResponse.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject candidate = candidates.optJSONObject(0);
                    JSONObject content = candidate != null ? candidate.optJSONObject("content") : null;
                    JSONArray respParts = content != null ? content.optJSONArray("parts") : null;
                    if (respParts != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < respParts.length(); i++) {
                            JSONObject part = respParts.optJSONObject(i);
                            if (part != null && part.has("text")) sb.append(part.optString("text"));
                        }
                        if (!sb.isEmpty()) return stripMarkdownFences(sb.toString());
                    }
                    String finishReason = candidate != null ? candidate.optString("finishReason", "") : "";
                    if (!finishReason.isEmpty() && !"STOP".equalsIgnoreCase(finishReason)) {
                        Reporter.log("Gemini Vision API returned no text (finishReason=" + finishReason + ") — switch to DEBUG for full response", LogLevel.ERROR);
                        Reporter.log("Gemini Vision API body: " + response.body(), LogLevel.DEBUG);
                        return null;
                    }
                }
                Reporter.log("Gemini Vision API returned empty response (HTTP 200) — switch to DEBUG for full response", LogLevel.ERROR);
                Reporter.log("Gemini Vision API empty body: " + response.body(), LogLevel.DEBUG);
                return null;
            } else {
                Reporter.log("Gemini Vision API Error: HTTP " + response.statusCode() + " — switch to DEBUG for full response", LogLevel.ERROR);
                Reporter.log("Gemini Vision API error body: " + response.body(), LogLevel.DEBUG);
                return null;
            }
        } catch (java.net.http.HttpTimeoutException e) {
            Reporter.log("Gemini Vision API Error: request timed out", LogLevel.ERROR);
            return null;
        } catch (Exception e) {
            Reporter.log("Failed to communicate with Gemini API: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    @Override
    public boolean supportsVision() {
        return true;
    }

    @Override
    public String getModelName() {
        return model;
    }

    /**
     * Strips markdown code fences (```json ... ```) that Gemini sometimes wraps its JSON responses in.
     */
    private String stripMarkdownFences(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
            }
        }
        return trimmed;
    }
}
