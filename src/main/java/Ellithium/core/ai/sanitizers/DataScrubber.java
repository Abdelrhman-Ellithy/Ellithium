package Ellithium.core.ai.sanitizers;

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

    // Password input values: value="..." inside type="password" inputs (type before value)
    private static final Pattern PASSWORD_VALUE = Pattern.compile(
            "(<input[^>]*type\\s*=\\s*\"password\"[^>]*value\\s*=\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE);

    // Password input values where value precedes the type="password" attribute (reversed order)
    private static final Pattern PASSWORD_VALUE_REV = Pattern.compile(
            "(<input[^>]*value\\s*=\\s*\")([^\"]*)(\"[^>]*type\\s*=\\s*\"password\")",
            Pattern.CASE_INSENSITIVE);

    // Credit-card numbers (13–16 digits, optional space/dash grouping)
    private static final Pattern CREDIT_CARD = Pattern.compile(
            "(?<!\\d)(?:\\d[ -]?){13,16}(?!\\d)");

    // US Social Security numbers
    private static final Pattern SSN = Pattern.compile(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b");

    // Phone numbers (US-style, optional country code / separators)
    private static final Pattern PHONE = Pattern.compile(
            "\\b(?:\\+?1[ .-]?)?\\(?\\d{3}\\)?[ .-]?\\d{3}[ .-]?\\d{4}\\b");

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

    // IBAN — 2-letter country code + 2 check digits + up to 30 alphanumeric chars (optional spaces)
    private static final Pattern IBAN = Pattern.compile(
            "\\b[A-Z]{2}\\d{2}(?:\\s?[A-Z0-9]{4}){2,7}\\b");

    // International phone — E.164 (+CC up to 15 digits) and common national formats
    private static final Pattern INTL_PHONE = Pattern.compile(
            "\\+(?:[1-9]\\d{0,2})[\\s.\\-]?\\(?\\d{1,4}\\)?[\\s.\\-]?\\d{1,4}[\\s.\\-]?\\d{1,9}");

    // Prompt-injection control phrases embedded in DOM content
    private static final Pattern PROMPT_INJECTION = Pattern.compile(
            "(?i)\\b(?:ignore\\s+(?:all\\s+)?(?:previous|prior|above)\\s+instructions?" +
            "|forget\\s+(?:your\\s+)?(?:context|instructions?|training)" +
            "|you\\s+are\\s+now\\s+(?:a|an)" +
            "|act\\s+as\\s+(?:a|an|if|though)" +
            "|disregard\\s+(?:all\\s+)?(?:previous|prior|the\\s+above)" +
            "|new\\s+(?:system\\s+)?prompt" +
            "|print\\s+(?:your\\s+)?(?:system\\s+)?(?:prompt|instructions)" +
            "|reveal\\s+(?:your\\s+)?(?:system\\s+)?(?:prompt|instructions)" +
            "|jailbreak|dan\\s+mode|do\\s+anything\\s+now)\\b");

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

        // Mask password input values (type before value, and value before type)
        if (PASSWORD_VALUE.matcher(result).find()) {
            result = PASSWORD_VALUE.matcher(result).replaceAll("$1[MASKED]$3");
            maskedCount++;
        }
        if (PASSWORD_VALUE_REV.matcher(result).find()) {
            result = PASSWORD_VALUE_REV.matcher(result).replaceAll("$1[MASKED]$3");
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

        // Mask credit-card numbers
        if (CREDIT_CARD.matcher(result).find()) {
            result = CREDIT_CARD.matcher(result).replaceAll("[MASKED_CC]");
            maskedCount++;
        }

        // Mask SSNs
        if (SSN.matcher(result).find()) {
            result = SSN.matcher(result).replaceAll("[MASKED_SSN]");
            maskedCount++;
        }

        // Mask phone numbers (US-style)
        if (PHONE.matcher(result).find()) {
            result = PHONE.matcher(result).replaceAll("[MASKED_PHONE]");
            maskedCount++;
        }

        // Mask international phone numbers (E.164 / +CC format)
        if (INTL_PHONE.matcher(result).find()) {
            result = INTL_PHONE.matcher(result).replaceAll("[MASKED_PHONE]");
            maskedCount++;
        }

        // Mask IBAN bank account numbers
        if (IBAN.matcher(result).find()) {
            result = IBAN.matcher(result).replaceAll("[MASKED_IBAN]");
            maskedCount++;
        }

        // Strip prompt-injection control phrases
        if (PROMPT_INJECTION.matcher(result).find()) {
            result = PROMPT_INJECTION.matcher(result).replaceAll("[REMOVED]");
            maskedCount++;
        }

        if (maskedCount > 0) {
            Reporter.log("DataScrubber: masked " + maskedCount + " sensitive data categories", LogLevel.DEBUG);
        }

        return result;
    }
}
