package Ellithium.core.execution.listener;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class APIListener implements Filter {
    
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "token", "authorization", "credit_card", "ssn", "email",
            "phone", "address", "pin", "secret", "key", "access_token", "refresh_token",
            "api_key", "apikey", "auth", "credential", "private", "secure", "security",
            "jwt", "session", "certificate", "passcode", "pwd", "social_security",
            "card_number", "cvv", "cvc", "ccv", "account", "routing", "passport",
            "license", "bearer", "basic", "username", "user", "login", "pass"
    ));

    public static Set<String> getSensitiveFields() {
        return java.util.Collections.unmodifiableSet(SENSITIVE_FIELDS);
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+\\d{1,3}[- ]?)?\\d{10,}");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\d{3}-?\\d{2}-?\\d{4}");
    private static final Pattern AUTH_PATTERN = Pattern.compile("(?i)(Bearer|Basic|Digest|OAuth)\\s+[a-zA-Z0-9+/=._-]+");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");

    String obfuscateData(String input) {
        if (input == null) return null;
        
        // Obfuscate JSON content
        if (input.trim().startsWith("{") || input.trim().startsWith("[")) {
            try {
                if (input.trim().startsWith("{")) {
                    JsonObject json = JsonParser.parseString(input).getAsJsonObject();
                    return obfuscateJson(json).toString();
                } else {
                    // Handle JSON arrays
                    JsonArray jsonArray = JsonParser.parseString(input).getAsJsonArray();
                    return obfuscateJsonArray(jsonArray).toString();
                }
            } catch (Exception e) {
                // Not valid JSON, continue with regular obfuscation
            }
        }

        // Obfuscate patterns
        String result = input;
        result = EMAIL_PATTERN.matcher(result).replaceAll("****@****.***");
        result = PHONE_PATTERN.matcher(result).replaceAll("****-***-****");
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("****-****-****-****");
        result = SSN_PATTERN.matcher(result).replaceAll("***-**-****");
        result = AUTH_PATTERN.matcher(result).replaceAll("$1 ********");
        result = IP_PATTERN.matcher(result).replaceAll("***.***.***.***");
        
        return result;
    }

    /**
     * As {@link #obfuscateData}, additionally masking sensitive query-string parameter VALUES
     * (e.g. {@code ?api_key=...}, {@code ?token=...}) by parameter NAME — {@code obfuscateData}'s
     * generic patterns don't recognize a raw {@code key=value} pair the way {@link #obfuscateJson}
     * recognizes a JSON key, so an API key or token passed as a query parameter (common with
     * key-based REST auth) would otherwise reach the report unredacted.
     */
    String obfuscateUri(String uri) {
        if (uri == null) return null;
        int q = uri.indexOf('?');
        if (q < 0) return obfuscateData(uri);

        String base = uri.substring(0, q);
        String query = uri.substring(q + 1);
        StringBuilder masked = new StringBuilder();
        for (String param : query.split("&")) {
            if (!masked.isEmpty()) masked.append('&');
            int eq = param.indexOf('=');
            if (eq < 0) {
                masked.append(param);
                continue;
            }
            String key = param.substring(0, eq);
            String value = param.substring(eq + 1);
            boolean sensitive = SENSITIVE_FIELDS.contains(key.toLowerCase());
            masked.append(key).append('=').append(sensitive ? "********" : obfuscateData(value));
        }
        return obfuscateData(base) + "?" + masked;
    }

    JsonObject obfuscateJson(JsonObject json) {
        JsonObject result = new JsonObject();
        for (String key : json.keySet()) {
            JsonElement value = json.get(key);
            if (SENSITIVE_FIELDS.contains(key.toLowerCase())) {
                result.addProperty(key, "********");
            } else if (value != null && value.isJsonObject()) {
                result.add(key, obfuscateJson(value.getAsJsonObject()));
            } else if (value != null && value.isJsonArray()) {
                result.add(key, obfuscateJsonArray(value.getAsJsonArray()));
            } else if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                result.addProperty(key, obfuscateData(value.getAsString()));
            } else {
                result.add(key, value);
            }
        }
        return result;
    }

    JsonArray obfuscateJsonArray(JsonArray jsonArray) {
        JsonArray result = new JsonArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement value = jsonArray.get(i);
            if (value != null && value.isJsonObject()) {
                result.add(obfuscateJson(value.getAsJsonObject()));
            } else if (value != null && value.isJsonArray()) {
                result.add(obfuscateJsonArray(value.getAsJsonArray()));
            } else if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                result.add(obfuscateData(value.getAsString()));
            } else {
                result.add(value);
            }
        }
        return result;
    }

    String handleCookies(Object cookies) {
        if (cookies == null) return "No Cookies";
        
        JsonObject json = new JsonObject();
        if (cookies instanceof Map) {
            Map<String, String> cookieMap = (Map<String, String>) cookies;
            if (cookieMap.isEmpty()) return "No Cookies";
            for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
                json.addProperty(entry.getKey(), entry.getValue());
            }
        } else if (cookies instanceof Cookies cookieObj) {
            if (cookieObj.asList().isEmpty()) return "No Cookies";
            for (Cookie cookie : cookieObj.asList()) {
                json.addProperty(cookie.getName(), cookie.getValue());
            }
        }
        return obfuscateJson(json).toString();
    }

    JsonObject headersToJson(Headers headers) {
        JsonObject json = new JsonObject();
        for (Header header : headers.asList()) {
            json.addProperty(header.getName(), header.getValue());
        }
        return obfuscateJson(json);
    }

    JsonObject mapToJson(Map<String, ?> map) {
        if (map == null || map.isEmpty()) return new JsonObject();
        JsonObject json = new JsonObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            json.addProperty(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
        }
        return obfuscateJson(json);
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        Response response = ctx.next(requestSpec, responseSpec);

        // Request logging
        try {
            Reporter.log("Request Method: ", LogLevel.INFO_BLUE, requestSpec.getMethod());
            Reporter.log("Request URI: ", LogLevel.INFO_BLUE, obfuscateUri(requestSpec.getURI()));
        } catch (Exception e) {
            Reporter.log("Error logging request basic info: " + e.getMessage(), LogLevel.ERROR);
        }

        try {
            Reporter.log("Request Headers: ", LogLevel.INFO_BLUE,
                    requestSpec.getHeaders() != null 
                      ? headersToJson(requestSpec.getHeaders()).toString() 
                      : "No Headers");
        } catch (Exception e) {
            Reporter.log("Error logging request headers: " + e.getMessage(), LogLevel.ERROR);
        }

        try {
            Reporter.log("Request Parameters: ", LogLevel.INFO_BLUE,
                    !requestSpec.getQueryParams().isEmpty()
                      ? mapToJson(requestSpec.getQueryParams()).toString() 
                      : "No Query Params");
            Reporter.log("Request Base Path: ", LogLevel.INFO_BLUE,
                    requestSpec.getBasePath() != null ? requestSpec.getBasePath() : "No Base Path");
            Reporter.log("Request Proxy Specification: ", LogLevel.INFO_BLUE,
                    requestSpec.getProxySpecification() != null ? requestSpec.getProxySpecification().toString() : "No Proxy");
            Reporter.log("Request ContentType: ", LogLevel.INFO_BLUE,
                    requestSpec.getContentType() != null ? requestSpec.getContentType() : "No Content Type");
        } catch (Exception e) {
            Reporter.log("Error logging request parameters: " + e.getMessage(), LogLevel.ERROR);
        }

        try {
            Reporter.log("Request Cookies: ", LogLevel.INFO_BLUE,
                    handleCookies(requestSpec.getCookies()));
        } catch (Exception e) {
            Reporter.log("Error logging request cookies: " + e.getMessage(), LogLevel.ERROR);
        }

        try {
            Reporter.log("Request Body: ", LogLevel.INFO_BLUE,
                    obfuscateData(requestSpec.getBody() != null ? requestSpec.getBody().toString() : "No Body"));
        } catch (Exception e) {
            Reporter.log("Error logging request body: " + e.getMessage(), LogLevel.ERROR);
        }

        // Response logging
        try {
            Reporter.log("Response Status Code: ", LogLevel.INFO_BLUE, String.valueOf(response.getStatusCode()));
            Reporter.log("Response ContentType: ", LogLevel.INFO_BLUE, response.getContentType());
            Reporter.log("Response Status Line: ", LogLevel.INFO_BLUE, response.getStatusLine());
        } catch (Exception e) {
            Reporter.log("Error logging response basic info: " + e.getMessage(), LogLevel.ERROR);
        }

        try {
            Reporter.log("Response Cookies: ", LogLevel.INFO_BLUE,
                    handleCookies(response.detailedCookies()));
        } catch (Exception e) {
            Reporter.log("Error logging response cookies: " + e.getMessage(), LogLevel.ERROR);
        }

        try {
            Reporter.log("Response Headers: ", LogLevel.INFO_BLUE,
                    response.getHeaders() != null 
                      ? headersToJson(response.getHeaders()).toString() 
                      : "No Headers");
        } catch (Exception e) {
            Reporter.log("Error logging response headers: " + e.getMessage(), LogLevel.ERROR);
        }

        try {
            Reporter.log("Response Body: ", LogLevel.INFO_BLUE,
                    obfuscateData(response.getBody() != null ? response.getBody().asString() : "No Body"));
        } catch (Exception e) {
            Reporter.log("Error logging response body: " + e.getMessage(), LogLevel.ERROR);
        }

        try {
            Reporter.log("Response Time (ms): ", LogLevel.INFO_BLUE, String.valueOf(response.getTime()));
        } catch (Exception e) {
            Reporter.log("Error logging response time: " + e.getMessage(), LogLevel.ERROR);
        }
        return response;
    }
}