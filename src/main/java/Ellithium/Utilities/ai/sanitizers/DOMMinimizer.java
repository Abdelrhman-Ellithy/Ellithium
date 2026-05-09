package Ellithium.Utilities.ai.sanitizers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.regex.Pattern;

/**
 * Minimizes DOM/XML page source before sending to an LLM.
 * Reduces token consumption from ~100K to ~5K tokens by stripping
 * non-essential tags, attributes, and hidden elements.
 *
 * <p>Critical for cost control: without this, each healing attempt
 * could consume 50,000–200,000 tokens ($0.50–$2.00 per call).</p>
 */
public class DOMMinimizer {

    // Tags that are completely useless for locator healing
    private static final Pattern SCRIPT_TAG = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_TAG = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_TAG = Pattern.compile("<svg[^>]*>[\\s\\S]*?</svg>", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOSCRIPT_TAG = Pattern.compile("<noscript[^>]*>[\\s\\S]*?</noscript>", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT = Pattern.compile("<!--[\\s\\S]*?-->", Pattern.CASE_INSENSITIVE);

    // Hidden elements are irrelevant for locator analysis
    private static final Pattern HIDDEN_DISPLAY = Pattern.compile("<[^>]*display\\s*:\\s*none[^>]*>[\\s\\S]*?</[^>]+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIDDEN_ATTR = Pattern.compile("<[^>]*\\shidden[^>]*>[\\s\\S]*?</[^>]+>", Pattern.CASE_INSENSITIVE);

    // Inline styles and event handlers add noise
    private static final Pattern STYLE_ATTR = Pattern.compile("\\s+style\\s*=\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_ATTR = Pattern.compile("\\s+on\\w+\\s*=\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);

    // Data attributes with long encoded values (Base64, tokens, etc.)
    private static final Pattern LONG_DATA_ATTR = Pattern.compile("\\s+data-[\\w-]+\\s*=\\s*\"[^\"]{100,}\"", Pattern.CASE_INSENSITIVE);

    // Excessive whitespace
    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s{2,}");
    private static final Pattern EMPTY_LINES = Pattern.compile("\\n\\s*\\n");

    /** Maximum size of the minimized DOM in characters */
    private static final int MAX_OUTPUT_LENGTH = 15_000;

    /**
     * Minimizes the given HTML/XML page source for LLM consumption.
     *
     * @param rawSource The full page source from {@code driver.getPageSource()}
     * @return A minimized version suitable for LLM context
     */
    public static String minimize(String rawSource) {
        if (rawSource == null || rawSource.isEmpty()) {
            return "";
        }

        int originalLength = rawSource.length();
        String result = rawSource;

        // Phase 1: Strip completely useless tags
        result = SCRIPT_TAG.matcher(result).replaceAll("");
        result = STYLE_TAG.matcher(result).replaceAll("");
        result = SVG_TAG.matcher(result).replaceAll("");
        result = NOSCRIPT_TAG.matcher(result).replaceAll("");
        result = COMMENT.matcher(result).replaceAll("");

        // Phase 2: Strip hidden elements
        result = HIDDEN_DISPLAY.matcher(result).replaceAll("");
        result = HIDDEN_ATTR.matcher(result).replaceAll("");

        // Phase 3: Strip noisy attributes (keep id, class, name, type, placeholder, aria-*, role, href, value)
        result = STYLE_ATTR.matcher(result).replaceAll("");
        result = EVENT_ATTR.matcher(result).replaceAll("");
        result = LONG_DATA_ATTR.matcher(result).replaceAll("");

        // Phase 4: Compress whitespace
        result = MULTI_WHITESPACE.matcher(result).replaceAll(" ");
        result = EMPTY_LINES.matcher(result).replaceAll("\n");
        result = result.trim();

        // Phase 5: Truncate if still too large
        if (result.length() > MAX_OUTPUT_LENGTH) {
            result = result.substring(0, MAX_OUTPUT_LENGTH) + "\n<!-- DOM truncated -->";
        }

        Reporter.log("DOM minimized: " + originalLength + " → " + result.length() + " chars ("
                + String.format("%.1f", (1.0 - (double) result.length() / originalLength) * 100) + "% reduction)",
                LogLevel.INFO_BLUE);

        return result;
    }
}
