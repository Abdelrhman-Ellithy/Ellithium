package Ellithium.Utilities.ai;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
            JsonObject payload = new JsonObject();

            // Gemini system instruction format
            JsonObject systemInstruction = new JsonObject();
            JsonArray sysParts = new JsonArray();
            JsonObject sysPart = new JsonObject();
            sysPart.addProperty("text", systemPrompt);
            sysParts.add(sysPart);
            systemInstruction.add("parts", sysParts);
            payload.add("systemInstruction", systemInstruction);

            JsonArray contents = new JsonArray();
            JsonObject userContent = new JsonObject();
            userContent.addProperty("role", "user");
            JsonArray userParts = new JsonArray();
            JsonObject userPart = new JsonObject();
            userPart.addProperty("text", userPrompt);
            userParts.add(userPart);
            userContent.add("parts", userParts);
            contents.add(userContent);
            payload.add("contents", contents);

            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.0);
            payload.add("generationConfig", generationConfig);

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
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                if (jsonResponse.has("candidates") && jsonResponse.get("candidates").isJsonArray()) {
                    JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                    if (!candidates.isEmpty()) {
                        JsonObject candidate = candidates.get(0).isJsonObject() ? candidates.get(0).getAsJsonObject() : null;
                        JsonObject content = candidate != null && candidate.has("content") && candidate.get("content").isJsonObject()
                                ? candidate.getAsJsonObject("content") : null;
                        JsonArray parts = content != null && content.has("parts") && content.get("parts").isJsonArray()
                                ? content.getAsJsonArray("parts") : null;
                        if (parts != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < parts.size(); i++) {
                                JsonElement partElem = parts.get(i);
                                if (partElem != null && partElem.isJsonObject()) {
                                    JsonObject part = partElem.getAsJsonObject();
                                    if (part.has("text") && !part.get("text").isJsonNull()) {
                                        sb.append(part.get("text").getAsString());
                                    }
                                }
                            }
                            if (!sb.isEmpty()) return stripMarkdownFences(sb.toString());
                        }
                        String finishReason = candidate != null && candidate.has("finishReason") && !candidate.get("finishReason").isJsonNull()
                                ? candidate.get("finishReason").getAsString() : "";
                        if (!finishReason.isEmpty() && !"STOP".equalsIgnoreCase(finishReason)) {
                            Reporter.log("Gemini API returned no text (finishReason=" + finishReason + ") — switch to DEBUG for full response", LogLevel.ERROR);
                            Reporter.log("Gemini API body: " + response.body(), LogLevel.DEBUG);
                            return null;
                        }
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
            JsonObject payload = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject userContent = new JsonObject();
            userContent.addProperty("role", "user");

            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);
            parts.add(textPart);

            String base64Image = Base64.getEncoder().encodeToString(screenshot);
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mimeType", "image/png");
            inlineData.addProperty("data", base64Image);
            JsonObject inlinePart = new JsonObject();
            inlinePart.add("inlineData", inlineData);
            parts.add(inlinePart);

            userContent.add("parts", parts);
            contents.add(userContent);
            payload.add("contents", contents);

            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.0);
            payload.add("generationConfig", generationConfig);

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
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                if (jsonResponse.has("candidates") && jsonResponse.get("candidates").isJsonArray()) {
                    JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                    if (!candidates.isEmpty()) {
                        JsonObject candidate = candidates.get(0).isJsonObject() ? candidates.get(0).getAsJsonObject() : null;
                        JsonObject content = candidate != null && candidate.has("content") && candidate.get("content").isJsonObject()
                                ? candidate.getAsJsonObject("content") : null;
                        JsonArray respParts = content != null && content.has("parts") && content.get("parts").isJsonArray()
                                ? content.getAsJsonArray("parts") : null;
                        if (respParts != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < respParts.size(); i++) {
                                JsonElement partElem = respParts.get(i);
                                if (partElem != null && partElem.isJsonObject()) {
                                    JsonObject part = partElem.getAsJsonObject();
                                    if (part.has("text") && !part.get("text").isJsonNull()) {
                                        sb.append(part.get("text").getAsString());
                                    }
                                }
                            }
                            if (!sb.isEmpty()) return stripMarkdownFences(sb.toString());
                        }
                        String finishReason = candidate != null && candidate.has("finishReason") && !candidate.get("finishReason").isJsonNull()
                                ? candidate.get("finishReason").getAsString() : "";
                        if (!finishReason.isEmpty() && !"STOP".equalsIgnoreCase(finishReason)) {
                            Reporter.log("Gemini Vision API returned no text (finishReason=" + finishReason + ") — switch to DEBUG for full response", LogLevel.ERROR);
                            Reporter.log("Gemini Vision API body: " + response.body(), LogLevel.DEBUG);
                            return null;
                        }
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
