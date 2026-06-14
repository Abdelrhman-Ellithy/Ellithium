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
            JSONObject payload = new JSONObject();
            payload.put("model", model);
            payload.put("max_tokens", 8192);
            payload.put("temperature", 0.0);
            payload.put("system", systemPrompt);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
            payload.put("messages", messages);

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
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray contentArr = jsonResponse.optJSONArray("content");
                if (contentArr != null && contentArr.length() > 0) {
                    String rawText = contentArr.getJSONObject(0).getString("text");
                    return stripMarkdownFences(rawText);
                }
                Reporter.log("Anthropic API returned empty content array (HTTP 200) — switch to DEBUG for full response", LogLevel.ERROR);
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
