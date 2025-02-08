package Ellithium.core.execution.listener;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class APIListener implements Filter {
    
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "token", "authorization", "credit_card", "ssn", "email",
            "phone", "address", "pin", "secret", "key", "access_token", "refresh_token"
    ));

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{10,}");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}");

    private String obfuscateData(String input) {
        if (input == null) return null;
        
        // Obfuscate JSON content
        if (input.trim().startsWith("{")) {
            try {
                JSONObject json = new JSONObject(input);
                return obfuscateJson(json).toString();
            } catch (Exception e) {
                // Not valid JSON, continue with regular obfuscation
            }
        }

        // Obfuscate patterns
        String result = input;
        result = EMAIL_PATTERN.matcher(result).replaceAll("****@****.***");
        result = PHONE_PATTERN.matcher(result).replaceAll("**********");
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("****-****-****-****");
        
        return result;
    }

    private JSONObject obfuscateJson(JSONObject json) {
        JSONObject result = new JSONObject(json.toString());
        for (String key : result.keySet()) {
            if (SENSITIVE_FIELDS.contains(key.toLowerCase())) {
                result.put(key, "********");
            } else if (result.get(key) instanceof JSONObject) {
                result.put(key, obfuscateJson((JSONObject) result.get(key)));
            }
        }
        return result;
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        Response response = ctx.next(requestSpec, responseSpec);
        try {
            Reporter.log("Request Method: ", LogLevel.INFO_BLUE, requestSpec.getMethod());
            Reporter.log("Request URI: ", LogLevel.INFO_BLUE, requestSpec.getURI());
            Reporter.log("Request Headers: ", LogLevel.INFO_BLUE,
                    obfuscateData(requestSpec.getHeaders() != null ? requestSpec.getHeaders().toString() : "No Headers"));
            Reporter.log("Request Parameters: ", LogLevel.INFO_BLUE,
                    obfuscateData(requestSpec.getQueryParams() != null ? requestSpec.getQueryParams().toString() : "No Query Params"));
            Reporter.log("Request Base Path: ", LogLevel.INFO_BLUE,
                    requestSpec.getBasePath() != null ? requestSpec.getBasePath() : "No Base Path");
            Reporter.log("Request Proxy Specification: ", LogLevel.INFO_BLUE,
                    requestSpec.getProxySpecification() != null ? requestSpec.getProxySpecification().toString() : "No Proxy");
            Reporter.log("Request ContentType: ", LogLevel.INFO_BLUE,
                    requestSpec.getContentType() != null ? requestSpec.getContentType() : "No Content Type");
            Reporter.log("Request Cookies: ", LogLevel.INFO_BLUE,
                    obfuscateData(requestSpec.getCookies() != null ? requestSpec.getCookies().toString() : "No Cookies"));
            Reporter.log("Request Params: ", LogLevel.INFO_BLUE,
                    obfuscateData(requestSpec.getRequestParams() != null ? requestSpec.getRequestParams().toString() : "No Request Params"));
            Reporter.log("Request Body: ", LogLevel.INFO_BLUE,
                    obfuscateData(requestSpec.getBody() != null ? requestSpec.getBody().toString() : "No Body"));
            
            Reporter.log("Response Status Code: ", LogLevel.INFO_BLUE, String.valueOf(response.getStatusCode()));
            Reporter.log("Response ContentType: ", LogLevel.INFO_BLUE, response.getContentType());
            Reporter.log("Response Status Line: ", LogLevel.INFO_BLUE, response.getStatusLine());
            Reporter.log("Detailed Response Cookies: ", LogLevel.INFO_BLUE, 
                    obfuscateData(response.detailedCookies().toString()));
            Reporter.log("Response Content-Length: ", LogLevel.INFO_BLUE, response.getHeader("Content-Length"));
            Reporter.log("Response Body: ", LogLevel.INFO_BLUE,
                    obfuscateData(response.getBody() != null ? response.getBody().asString() : "No Body"));
            Reporter.log("Response Cookies: ", LogLevel.INFO_BLUE,
                    obfuscateData(response.getCookies() != null ? response.getCookies().toString() : "No Cookies"));
            Reporter.log("Response Headers: ", LogLevel.INFO_BLUE,
                    obfuscateData(response.getHeaders() != null ? response.getHeaders().toString() : "No Headers"));
            Reporter.log("Response Time (ms): ", LogLevel.INFO_BLUE, String.valueOf(response.getTime()));
            
        } catch (Exception e) {
            Reporter.log("Error in API Listener", LogLevel.ERROR);
        }

        return response;
    }
}