package Ellithium.Utilities.ai;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenAICompatibleProvider implements LLMProvider {
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    public OpenAICompatibleProvider(String baseUrl, String apiKey, String model) {
        // Default to OpenAI's public URL if baseUrl is empty
        this.baseUrl = (baseUrl == null || baseUrl.isEmpty()) ? "https://api.openai.com/v1" : baseUrl;
        this.apiKey = apiKey;
        this.model = (model == null || model.isEmpty()) ? "gpt-4o" : model;
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
            JsonObject payload = new JsonObject();
            payload.addProperty("model", model);
            payload.addProperty("temperature", 0.0);

            JsonArray messages = new JsonArray();
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userPrompt);
            messages.add(userMsg);

            payload.add("messages", messages);

            String url = baseUrl;
            if (!url.endsWith("/chat/completions")) {
                url = url.endsWith("/") ? url + "chat/completions" : url + "/chat/completions";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()) {
                    JsonArray choices = jsonResponse.getAsJsonArray("choices");
                    if (!choices.isEmpty()) {
                        JsonObject first = choices.get(0).isJsonObject() ? choices.get(0).getAsJsonObject() : null;
                        JsonObject message = first != null && first.has("message") && first.get("message").isJsonObject()
                                ? first.getAsJsonObject("message") : null;
                        String rawText = message != null && message.has("content") && !message.get("content").isJsonNull()
                                ? message.get("content").getAsString() : null;
                        if (rawText != null && !rawText.isEmpty()) {
                            return stripMarkdownFences(rawText);
                        }
                    }
                }
                Reporter.log("OpenAI API returned no message content (HTTP 200) — switch to DEBUG for full response", LogLevel.ERROR);
                Reporter.log("OpenAI API empty body: " + response.body(), LogLevel.DEBUG);
                return null;
            } else {
                Reporter.log("OpenAI API Error: HTTP " + response.statusCode() + " — switch to DEBUG for full response", LogLevel.ERROR);
                Reporter.log("OpenAI API error body: " + response.body(), LogLevel.DEBUG);
                return null;
            }
        } catch (Exception e) {
            Reporter.log("Failed to communicate with OpenAI-compatible API: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    private String stripMarkdownFences(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) trimmed = trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
        }
        return trimmed;
    }
}
