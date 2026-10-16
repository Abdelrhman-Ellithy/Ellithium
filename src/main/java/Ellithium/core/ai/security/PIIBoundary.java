package Ellithium.core.ai.security;

import Ellithium.core.ai.sanitizers.DataScrubber;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.regex.Pattern;

/**
 * PII and privacy boundary for AI/LLM interactions.
 * Enforces credential redaction (passwords, tokens, API keys, private keys)
 * and prompt injection neutralization on all untrusted data before sending to external LLMs.
 */
public final class PIIBoundary {

    private PIIBoundary() {}

    // Credential Patterns
    private static final Pattern BASIC_AUTH = Pattern.compile(
            "(?i)Authorization\\s*:\\s*Basic\\s+[A-Za-z0-9+/=]+");

    private static final Pattern BEARER_AUTH = Pattern.compile(
            "(?i)Authorization\\s*:\\s*Bearer\\s+[A-Za-z0-9\\-_.~+/=]+");

    private static final Pattern PRIVATE_KEY = Pattern.compile(
            "-----BEGIN [A-Z\\s]+PRIVATE KEY-----[\\s\\S]*?-----END [A-Z\\s]+PRIVATE KEY-----");

    private static final Pattern DB_URI_PASSWORD = Pattern.compile(
            "(?i)((?:postgres|mysql|oracle|sqlserver|mongodb|redis):\\/\\/[^:\\s/]+:)([^@\\s]+)(@)");

    private static final Pattern AWS_KEY = Pattern.compile(
            "\\bAKIA[0-9A-Z]{16}\\b");

    private static final Pattern API_KEY_SECRET = Pattern.compile(
            "(?i)\\b(?:[a-z0-9_-]*(?:api[_-]?key|secret|client[_-]?secret|access[_-]?token|app[_-]?secret|auth[_-]?token|bearer[_-]?token))\\s*[:=]\\s*[\"']?([A-Za-z0-9\\-_.~]{8,})[\"']?");

    private static final Pattern PASSWORD_KEY_VALUE = Pattern.compile(
            "(?i)\\b(?:[a-z0-9_-]*(?:password|passwd|pwd|pass))\\s*[:=]\\s*[\"']?([^\"'\\s,;}{]+)[\"']?");

    private static final Pattern PASSWORD_SUFFIX_TOKEN = Pattern.compile(
            "(?i)\\b((?:password|passwd|pwd|secret)[_-])([A-Za-z0-9]{8,})\\b");

    // Prompt Injection Patterns & LLM Delimiters
    private static final Pattern SPECIAL_TOKENS = Pattern.compile(
            "(?i)<\\|im_start\\|>|<\\|im_end\\|>|<\\|system\\|>|<\\|user\\|>|<\\|assistant\\|>|<\\|endoftext\\|>|<\\|fim_prefix\\|>|<\\|fim_suffix\\|>|<\\|fim_middle\\|>|<system>|<\\/system>");

    private static final Pattern INSTRUCTION_TAGS = Pattern.compile(
            "(?i)\\[INST\\]|\\[\\/INST\\]|<<SYS>>|<<\\/SYS>>|\\[SYS\\]|\\[\\/SYS\\]|```(?:system|instructions)");

    private static final Pattern TOOL_TAGS = Pattern.compile(
            "(?i)<\\/?(?:tool|tool_call|function_calls|functions)?>");

    private static final Pattern SYSTEM_HEADER = Pattern.compile(
            "(?i)(?:^|\\n)\\s*(?:system|assistant|human|user)\\s*:");

    private static final Pattern INJECTION_DIRECTIVES = Pattern.compile(
            "(?i)\\b(?:ignore\\s+(?:all\\s+)?(?:previous|prior|above)\\s+instructions?" +
            "|forget\\s+(?:all\\s+)?(?:previous|prior|above)?\\s*(?:context|instructions?|training)" +
            "|you\\s+are\\s+now\\s+(?:a|an|unconstrained)" +
            "|act\\s+as\\s+(?:a|an|if|though)" +
            "|disregard\\s+(?:all\\s+)?(?:previous|prior|the\\s+above|all\\s+safety\\s+checks?)" +
            "|new\\s+(?:system\\s+)?prompt" +
            "|print\\s+(?:your\\s+)?(?:system\\s+)?(?:prompt|instructions)" +
            "|reveal\\s+(?:your\\s+)?(?:system\\s+)?(?:prompt|instructions)" +
            "|developer\\s+mode\\s+(?:enabled|on)" +
            "|jailbreak|dan\\s+mode|do\\s+anything\\s+now|bypass\\s+rules?|override\\s+system\\s+prompt" +
            "|override\\s+(?:previous|all)?\\s*instructions?|bypass\\s+safety\\s+checks?)\\b");

    /**
     * Sanitizes raw DOM content by scrubbing credentials and neutralizing prompt injections.
     */
    public static String sanitizeDom(String dom) {
        if (dom == null || dom.isBlank()) return dom;
        String neutralized = neutralizeInjection(dom);
        return redactCredentials(neutralized);
    }

    /**
     * Sanitizes call-site source code before inclusion in LLM prompts.
     */
    public static String sanitizeSource(String source) {
        if (source == null || source.isBlank()) return source;
        String neutralized = neutralizeInjection(source);
        return redactCredentials(neutralized);
    }

    /**
     * Sanitizes general text attributes or queries.
     */
    public static String sanitizeGeneral(String text) {
        if (text == null || text.isBlank()) return text;
        String neutralized = neutralizeInjection(text);
        return redactCredentials(neutralized);
    }

    /**
     * Sanitizes failure context (exception messages, stack traces, diagnostics)
     * by redacting credentials, tokens, passwords, and neutralizing prompt injections.
     */
    public static String sanitizeFailureContext(String failureContext) {
        if (failureContext == null || failureContext.isBlank()) return failureContext;
        String neutralized = neutralizeInjection(failureContext);
        return redactCredentials(neutralized);
    }

    /**
     * Redacts all sensitive credentials, keys, passwords, and tokens.
     */
    public static String redactCredentials(String text) {
        if (text == null || text.isBlank()) return text;
        String result = text;

        if (BASIC_AUTH.matcher(result).find()) {
            result = BASIC_AUTH.matcher(result).replaceAll("Authorization: Basic [REDACTED_BASIC_AUTH]");
        }
        if (BEARER_AUTH.matcher(result).find()) {
            result = BEARER_AUTH.matcher(result).replaceAll("Authorization: Bearer [REDACTED_BEARER_TOKEN]");
        }
        if (PRIVATE_KEY.matcher(result).find()) {
            result = PRIVATE_KEY.matcher(result).replaceAll("[REDACTED_PRIVATE_KEY]");
        }
        if (DB_URI_PASSWORD.matcher(result).find()) {
            result = DB_URI_PASSWORD.matcher(result).replaceAll("$1[REDACTED_PASSWORD]$3");
        }
        if (AWS_KEY.matcher(result).find()) {
            result = AWS_KEY.matcher(result).replaceAll("[REDACTED_AWS_KEY]");
        }
        if (API_KEY_SECRET.matcher(result).find()) {
            result = API_KEY_SECRET.matcher(result).replaceAll("apiKey=[REDACTED_KEY]");
        }
        if (PASSWORD_KEY_VALUE.matcher(result).find()) {
            result = PASSWORD_KEY_VALUE.matcher(result).replaceAll("password=[REDACTED_PASSWORD]");
        }
        if (PASSWORD_SUFFIX_TOKEN.matcher(result).find()) {
            result = PASSWORD_SUFFIX_TOKEN.matcher(result).replaceAll("$1[REDACTED_PASSWORD]");
        }

        // Apply DataScrubber for standard PII, email, passwords, tokens
        return DataScrubber.scrub(result);
    }

    /**
     * Neutralizes prompt injection patterns and special model delimiters.
     * Normalizes Unicode homoglyphs and removes zero-width characters to prevent filter evasion.
     */
    public static String neutralizeInjection(String text) {
        if (text == null || text.isBlank()) return text;
        // Strip zero-width and invisible control characters
        String result = text.replaceAll("[\\u200B-\\u200D\\uFEFF\\u00AD]", "");
        // Normalize Unicode compatibility forms (NFKC)
        result = java.text.Normalizer.normalize(result, java.text.Normalizer.Form.NFKC);
        // Normalize common Latin-lookalike homoglyphs
        result = normalizeHomoglyphs(result);

        if (SPECIAL_TOKENS.matcher(result).find()) {
            result = SPECIAL_TOKENS.matcher(result).replaceAll("[SANITIZED_TAG]");
        }
        if (INSTRUCTION_TAGS.matcher(result).find()) {
            result = INSTRUCTION_TAGS.matcher(result).replaceAll("[SANITIZED_INST]");
        }
        if (TOOL_TAGS.matcher(result).find()) {
            result = TOOL_TAGS.matcher(result).replaceAll("[SANITIZED_TOOL]");
        }
        if (SYSTEM_HEADER.matcher(result).find()) {
            result = SYSTEM_HEADER.matcher(result).replaceAll("\n[header]:");
        }
        if (INJECTION_DIRECTIVES.matcher(result).find()) {
            result = INJECTION_DIRECTIVES.matcher(result).replaceAll("[SANITIZED_DIRECTIVE]");
        }

        return result;
    }

    private static String normalizeHomoglyphs(String text) {
        if (text == null) return null;
        return text.replace('\u0430', 'a')
                .replace('\u0435', 'e')
                .replace('\u043E', 'o')
                .replace('\u0440', 'p')
                .replace('\u0441', 'c')
                .replace('\u0443', 'y')
                .replace('\u0445', 'x')
                .replace('\u0410', 'A')
                .replace('\u0415', 'E')
                .replace('\u041E', 'O')
                .replace('\u0420', 'P')
                .replace('\u0421', 'C')
                .replace('\u0423', 'Y')
                .replace('\u0425', 'X');
    }
}
