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

public class AnthropicProvider implements LLMProvider {
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    public AnthropicProvider(String baseUrl, String apiKey, String model) {
        this.baseUrl = (baseUrl == null || baseUrl.isEmpty()) ? "https://api.anthropic.com/v1" : baseUrl;
        this.apiKey = apiKey;
        this.model = (model == null || model.isEmpty()) ? "claude-3-5-sonnet-latest" : model;
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
            payload.addProperty("max_tokens", 8192);
            payload.addProperty("temperature", 0.0);
            payload.addProperty("system", systemPrompt);

            JsonArray messages = new JsonArray();
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", userPrompt);
            messages.add(userMessage);
            payload.add("messages", messages);

            String url = baseUrl;
            if (!url.endsWith("/messages")) {
                url = url.endsWith("/") ? url + "messages" : url + "/messages";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                if (jsonResponse.has("content") && jsonResponse.get("content").isJsonArray()) {
                    JsonArray contentArr = jsonResponse.getAsJsonArray("content");
                    if (!contentArr.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < contentArr.size(); i++) {
                            JsonElement elem = contentArr.get(i);
                            if (elem != null && elem.isJsonObject()) {
                                JsonObject block = elem.getAsJsonObject();
                                if (block.has("type") && !block.get("type").isJsonNull()
                                        && "text".equals(block.get("type").getAsString())
                                        && block.has("text") && !block.get("text").isJsonNull()) {
                                    sb.append(block.get("text").getAsString());
                                }
                            }
                        }
                        if (!sb.isEmpty()) {
                            return stripMarkdownFences(sb.toString());
                        }
                    }
                }
                Reporter.log("Anthropic API returned no text block (HTTP 200) — switch to DEBUG for full response", LogLevel.ERROR);
                Reporter.log("Anthropic API empty body: " + response.body(), LogLevel.DEBUG);
                return null;
            } else {
                Reporter.log("Anthropic API Error: HTTP " + response.statusCode() + " — switch to DEBUG for full response", LogLevel.ERROR);
                Reporter.log("Anthropic API error body: " + response.body(), LogLevel.DEBUG);
                return null;
            }
        } catch (Exception e) {
            Reporter.log("Failed to communicate with Anthropic API: " + e.getMessage(), LogLevel.ERROR);
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
