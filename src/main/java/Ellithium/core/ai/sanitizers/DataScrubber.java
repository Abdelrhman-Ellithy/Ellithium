package Ellithium.Utilities.ai.sanitizers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.regex.Pattern;

/**
 * Scrubs Personally Identifiable Information (PII), credentials, and sensitive data
 * from DOM/text content before sending it to external LLM APIs.
 *
 * <p>This is a critical security gate. Without it, sending raw DOM to
 * OpenAI/Anthropic/etc. can leak passwords, session tokens, API keys,
 * and user data — a compliance and security violation.</p>
 */
public class DataScrubber {

    // Email addresses
    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // Password input values: value="..." inside type="password" inputs
    private static final Pattern PASSWORD_VALUE = Pattern.compile(
            "(<input[^>]*type\\s*=\\s*\"password\"[^>]*value\\s*=\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE);

    // Bearer tokens and API keys in attributes
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(Bearer\\s+)[A-Za-z0-9\\-._~+/]+=*",
            Pattern.CASE_INSENSITIVE);

    // Common API key patterns in attribute values
    private static final Pattern API_KEY_ATTR = Pattern.compile(
            "((?:api[_-]?key|token|secret|authorization|x-api-key)\\s*[=:]\\s*\")([^\"]{8,})(\")",
            Pattern.CASE_INSENSITIVE);

    // Session IDs and CSRF tokens in hidden inputs
    private static final Pattern HIDDEN_TOKEN = Pattern.compile(
            "(<input[^>]*type\\s*=\\s*\"hidden\"[^>]*value\\s*=\\s*\")([^\"]{16,})(\")",
            Pattern.CASE_INSENSITIVE);

    // JWT tokens (header.payload.signature)
    private static final Pattern JWT = Pattern.compile(
            "eyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]+");

    // Cookie values in set-cookie or cookie headers visible in DOM
    private static final Pattern COOKIE_VALUE = Pattern.compile(
            "((?:session|sid|token|auth)[a-zA-Z0-9_-]*\\s*=\\s*)([^;\\s\"]{8,})",
            Pattern.CASE_INSENSITIVE);

    /**
     * Scrubs sensitive data from the given text content.
     *
     * @param content The raw text/DOM content to sanitize
     * @return Sanitized content with PII and credentials masked
     */
    public static String scrub(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String result = content;
        int maskedCount = 0;

        // Mask emails
        if (EMAIL.matcher(result).find()) {
            result = EMAIL.matcher(result).replaceAll("***@***.***");
            maskedCount++;
        }

        // Mask password input values
        if (PASSWORD_VALUE.matcher(result).find()) {
            result = PASSWORD_VALUE.matcher(result).replaceAll("$1[MASKED]$3");
            maskedCount++;
        }

        // Mask bearer tokens
        if (BEARER_TOKEN.matcher(result).find()) {
            result = BEARER_TOKEN.matcher(result).replaceAll("$1[MASKED_TOKEN]");
            maskedCount++;
        }

        // Mask API keys in attributes
        if (API_KEY_ATTR.matcher(result).find()) {
            result = API_KEY_ATTR.matcher(result).replaceAll("$1[MASKED_KEY]$3");
            maskedCount++;
        }

        // Mask hidden input tokens (CSRF, session)
        if (HIDDEN_TOKEN.matcher(result).find()) {
            result = HIDDEN_TOKEN.matcher(result).replaceAll("$1[MASKED_TOKEN]$3");
            maskedCount++;
        }

        // Mask JWT tokens
        if (JWT.matcher(result).find()) {
            result = JWT.matcher(result).replaceAll("[MASKED_JWT]");
            maskedCount++;
        }

        // Mask cookie/session values
        if (COOKIE_VALUE.matcher(result).find()) {
            result = COOKIE_VALUE.matcher(result).replaceAll("$1[MASKED]");
            maskedCount++;
        }

        if (maskedCount > 0) {
            Reporter.log("DataScrubber: masked " + maskedCount + " categories of sensitive data", LogLevel.INFO_BLUE);
        }

        return result;
    }
}
