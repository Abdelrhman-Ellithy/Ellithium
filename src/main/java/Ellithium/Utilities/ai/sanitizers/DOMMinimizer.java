package Ellithium.Utilities.ai.sanitizers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimizes DOM/XML page source before sending to an LLM.
 *
 * <p>Uses a two-phase strategy:
 * <ol>
 *   <li><b>Phase A – Interactive extraction:</b> Extracts ALL interactive elements
 *       (input, button, select, textarea, a, form, label) from the ENTIRE cleaned DOM.
 *       This guarantees the broken locator's element is ALWAYS visible to the AI,
 *       regardless of its position in the DOM.</li>
 *   <li><b>Phase B – Structural context:</b> Fills remaining token budget with
 *       structural context from the top of the cleaned DOM (nav, section names, etc.).</li>
 * </ol>
 * </p>
 */
public class DOMMinimizer {

    // Tags that are completely useless for locator healing
    private static final Pattern SCRIPT_TAG     = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>",   Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_TAG      = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>",     Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_TAG        = Pattern.compile("<svg[^>]*>[\\s\\S]*?</svg>",         Pattern.CASE_INSENSITIVE);
    private static final Pattern NOSCRIPT_TAG   = Pattern.compile("<noscript[^>]*>[\\s\\S]*?</noscript>", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT        = Pattern.compile("<!--[\\s\\S]*?-->",                  Pattern.CASE_INSENSITIVE);

    // Attribute noise
    private static final Pattern STYLE_ATTR     = Pattern.compile("\\s+style\\s*=\\s*\"[^\"]*\"",       Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_ATTR     = Pattern.compile("\\s+on\\w+\\s*=\\s*\"[^\"]*\"",      Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG_DATA_ATTR = Pattern.compile("\\s+data-[\\w-]+\\s*=\\s*\"[^\"]{100,}\"", Pattern.CASE_INSENSITIVE);

    // Whitespace compression
    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s{2,}");
    private static final Pattern EMPTY_LINES      = Pattern.compile("\\n\\s*\\n");

    /** Maximum characters to send to the LLM */
    private static final int MAX_OUTPUT_LENGTH = 15_000;

    /**
     * Regex that matches interactive HTML elements across the entire DOM.
     * Captures both self-closing and paired tags.
     */
    private static final Pattern INTERACTIVE_ELEMENT = Pattern.compile(
            "<(input|button|select|textarea|a|form|label)(\\s[^>]*)?>(?:[\\s\\S]*?</\\1>)?",
            Pattern.CASE_INSENSITIVE);

    public static String minimize(String rawSource) {
        if (rawSource == null || rawSource.isEmpty()) {
            return "";
        }

        int originalLength = rawSource.length();
        String cleaned = rawSource;

        // Phase 1: Strip completely useless tags
        cleaned = SCRIPT_TAG.matcher(cleaned).replaceAll("");
        cleaned = STYLE_TAG.matcher(cleaned).replaceAll("");
        cleaned = SVG_TAG.matcher(cleaned).replaceAll("");
        cleaned = NOSCRIPT_TAG.matcher(cleaned).replaceAll("");
        cleaned = COMMENT.matcher(cleaned).replaceAll("");

        // Phase 2: Strip noisy attributes
        // (keeps id, class, name, type, placeholder, aria-*, role, href, value, data-qa, data-testid)
        cleaned = STYLE_ATTR.matcher(cleaned).replaceAll("");
        cleaned = EVENT_ATTR.matcher(cleaned).replaceAll("");
        cleaned = LONG_DATA_ATTR.matcher(cleaned).replaceAll("");

        // Phase 3: Compress whitespace
        cleaned = MULTI_WHITESPACE.matcher(cleaned).replaceAll(" ");
        cleaned = EMPTY_LINES.matcher(cleaned).replaceAll("\n");
        cleaned = cleaned.trim();

        // Phase 4: Extract ALL interactive elements from the entire cleaned DOM.
        // This is the critical step — the broken locator's element is somewhere in this list.
        StringBuilder interactiveSection = new StringBuilder();
        interactiveSection.append("<!-- INTERACTIVE ELEMENTS (full page scan) -->\n");

        Matcher iMatcher = INTERACTIVE_ELEMENT.matcher(cleaned);
        int interactiveCount = 0;
        while (iMatcher.find()) {
            String tag = iMatcher.group().trim();
            // Skip embedded data URIs or absurdly long inline content
            if (tag.length() < 500) {
                interactiveSection.append(tag).append("\n");
                interactiveCount++;
            }
        }
        interactiveSection.append("<!-- END INTERACTIVE ELEMENTS (")
                          .append(interactiveCount)
                          .append(" found) -->\n");

        String interactiveStr = interactiveSection.toString();
        int remainingBudget = MAX_OUTPUT_LENGTH - interactiveStr.length();

        // Phase 5: Fill remaining budget with structural context from the top of the DOM
        // (gives the AI page identity: navbar, section headings, etc.)
        String structuralContext = "";
        if (remainingBudget > 500) {
            int contextEnd = Math.min(remainingBudget, cleaned.length());
            structuralContext = "\n<!-- STRUCTURAL CONTEXT (page start) -->\n"
                    + cleaned.substring(0, contextEnd);
        }

        String result = interactiveStr + structuralContext;

        // Final safety cap
        if (result.length() > MAX_OUTPUT_LENGTH) {
            result = result.substring(0, MAX_OUTPUT_LENGTH) + "\n<!-- DOM truncated -->";
        }

        Reporter.log("DOM minimized: " + originalLength + " \u2192 " + result.length() + " chars ("
                + String.format("%.1f", (1.0 - (double) result.length() / originalLength) * 100)
                + "% reduction) | " + interactiveCount + " interactive elements extracted",
                LogLevel.INFO_BLUE);

        return result;
    }
}
