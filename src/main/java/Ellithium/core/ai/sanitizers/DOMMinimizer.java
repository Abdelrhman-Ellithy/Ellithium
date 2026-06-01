package Ellithium.core.ai.sanitizers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimizes DOM/XML page source before sending to an LLM.
 *
 * <p>Uses a multi-phase strategy:
 * <ol>
 *   <li><b>Phase 1 – Noise removal:</b> Strips script, style, SVG, noscript, comments,
 *       inline styles, event handlers, and excessively long data attributes.</li>
 *   <li><b>Phase 2 – Whitespace compression.</b></li>
 *   <li><b>Phase 3 – Semantic grouping:</b> Groups interactive elements by their
 *       enclosing semantic landmark (&lt;header&gt;, &lt;nav&gt;, &lt;main&gt;,
 *       &lt;form&gt;, &lt;aside&gt;, &lt;footer&gt;, &lt;section&gt;, &lt;dialog&gt;).
 *       This gives the LLM spatial awareness — it knows which form an input
 *       belongs to and which region a button is in.</li>
 *   <li><b>Phase 4 – Structural context:</b> Fills remaining token budget with
 *       page-level structural context (headings, titles, etc.).</li>
 * </ol>
 * </p>
 */
public class DOMMinimizer {

    /**
     * Returns the best possible DOM representation for an LLM prompt.
     *
     * <p>Strategy:</p>
     * <ol>
     *   <li>Try {@link AccessibilityTreeExtractor#extractTree(org.openqa.selenium.WebDriver)}
     *       — works on Chrome, Firefox, Safari, Edge, Appium WebView via JavaScript injection</li>
     *   <li>Fall back to {@link #minimize(String)} on raw page source for native mobile or
     *       when JS injection fails</li>
     * </ol>
     *
     * @param driver The WebDriver instance (any browser)
     * @return Compact DOM representation suitable for LLM context
     */
    public static String getOptimalDOMRepresentation(org.openqa.selenium.WebDriver driver) {
        // Try Accessibility Tree first (10-50x smaller, semantically richer)
        String axTree = AccessibilityTreeExtractor.extractTree(driver);
        if (axTree != null && !axTree.isBlank()) {
            return axTree;
        }

        // Fallback: regex-based HTML minimization
        Reporter.log("DOMMinimizer: AX tree unavailable, falling back to HTML minimization", LogLevel.DEBUG);
        String rawSource = driver.getPageSource();
        return minimize(rawSource);
    }

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
     * Interactive element regex — matches input, button, select, textarea, a, label, form.
     */
    private static final Pattern INTERACTIVE_ELEMENT = Pattern.compile(
            "<(input|button|select|textarea|a|label)(\\s[^>]*)?>(?:[\\s\\S]*?</\\1>)?",
            Pattern.CASE_INSENSITIVE);

    /**
     * Semantic landmark tags that provide spatial context.
     * Each interactive element is grouped under its nearest enclosing landmark.
     */
    private static final Pattern LANDMARK_OPEN = Pattern.compile(
            "<(header|nav|main|aside|footer|section|form|dialog)(\\s[^>]*)?>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LANDMARK_CLOSE = Pattern.compile(
            "</(header|nav|main|aside|footer|section|form|dialog)>",
            Pattern.CASE_INSENSITIVE);

    /**
     * Extracts key attributes from a landmark opening tag for display.
     * Captures id, class, name, aria-label, role.
     */
    private static final Pattern ATTR_EXTRACT = Pattern.compile(
            "\\s(id|class|name|aria-label|role|action|method)\\s*=\\s*\"([^\"]*)\"",
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

        // Phase 4: Semantic grouping — group interactive elements by landmark region
        String groupedOutput = buildSemanticGroups(cleaned);

        // Phase 5: Fill remaining budget with page-level structural context
        int remainingBudget = MAX_OUTPUT_LENGTH - groupedOutput.length();
        String structuralContext = "";
        if (remainingBudget > 500) {
            structuralContext = extractStructuralContext(cleaned, remainingBudget);
        }

        String result = groupedOutput + structuralContext;

        // Final safety cap
        if (result.length() > MAX_OUTPUT_LENGTH) {
            result = result.substring(0, MAX_OUTPUT_LENGTH) + "\n<!-- DOM truncated -->";
        }

        Reporter.log("DOM minimized: " + originalLength + " \u2192 " + result.length() + " chars ("
                + String.format("%.1f", (1.0 - (double) result.length() / originalLength) * 100)
                + "% reduction)",
                LogLevel.INFO_BLUE);

        return result;
    }

    // ──────────────────────── Semantic Grouping ────────────────────────

    /**
     * Groups interactive elements by their enclosing semantic landmark.
     * Output format:
     * <pre>
     * --- &lt;nav id="main-nav"&gt; ---
     * &lt;a href="/login"&gt;Login&lt;/a&gt;
     * &lt;a href="/signup"&gt;Sign Up&lt;/a&gt;
     * --- &lt;form id="search" action="/search"&gt; ---
     * &lt;input name="q" placeholder="Search..."&gt;
     * &lt;button type="submit"&gt;Go&lt;/button&gt;
     * --- Ungrouped ---
     * &lt;button class="modal-close"&gt;×&lt;/button&gt;
     * </pre>
     */
    private static String buildSemanticGroups(String cleaned) {
        // Step 1: Find all landmark regions with their character ranges
        List<LandmarkRegion> landmarks = findLandmarkRegions(cleaned);

        // Step 2: Find all interactive elements
        List<InteractiveElement> elements = findInteractiveElements(cleaned);

        // Step 3: Assign each element to its deepest enclosing landmark
        Map<String, List<String>> grouped = new LinkedHashMap<>(); // preserves insertion order
        List<String> ungrouped = new ArrayList<>();

        for (InteractiveElement elem : elements) {
            LandmarkRegion deepest = findDeepestEnclosingLandmark(landmarks, elem.startPos, elem.endPos);
            if (deepest != null) {
                grouped.computeIfAbsent(deepest.label, k -> new ArrayList<>()).add(elem.html);
            } else {
                ungrouped.add(elem.html);
            }
        }

        // Step 4: Format output
        StringBuilder sb = new StringBuilder();
        int totalElements = 0;

        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            sb.append("--- ").append(entry.getKey()).append(" ---\n");
            for (String html : entry.getValue()) {
                sb.append(html).append("\n");
                totalElements++;
            }
        }

        if (!ungrouped.isEmpty()) {
            sb.append("--- Ungrouped ---\n");
            for (String html : ungrouped) {
                sb.append(html).append("\n");
                totalElements++;
            }
        }

        sb.insert(0, "<!-- INTERACTIVE ELEMENTS (" + totalElements + " found, grouped by region) -->\n");
        sb.append("<!-- END INTERACTIVE ELEMENTS -->\n");

        return sb.toString();
    }

    /**
     * Finds all semantic landmark regions in the cleaned DOM.
     * Handles nesting — a form inside a main section creates two regions.
     */
    private static List<LandmarkRegion> findLandmarkRegions(String cleaned) {
        List<LandmarkRegion> regions = new ArrayList<>();
        Deque<LandmarkRegion> stack = new ArrayDeque<>();

        // Find all opens
        Matcher openMatcher = LANDMARK_OPEN.matcher(cleaned);
        Matcher closeMatcher = LANDMARK_CLOSE.matcher(cleaned);

        // Collect all open/close events and sort by position
        List<TagEvent> events = new ArrayList<>();
        while (openMatcher.find()) {
            String tag = openMatcher.group(1).toLowerCase();
            String attrs = openMatcher.group(2);
            String label = formatLandmarkLabel(tag, attrs);
            events.add(new TagEvent(openMatcher.start(), true, tag, label));
        }
        while (closeMatcher.find()) {
            events.add(new TagEvent(closeMatcher.start(), false, closeMatcher.group(1).toLowerCase(), null));
        }
        events.sort(Comparator.comparingInt(e -> e.position));

        // Process events using a stack to handle nesting
        for (TagEvent event : events) {
            if (event.isOpen) {
                LandmarkRegion region = new LandmarkRegion();
                region.tag = event.tag;
                region.label = event.label;
                region.startPos = event.position;
                region.depth = stack.size();
                stack.push(region);
            } else {
                // Pop matching tag
                if (!stack.isEmpty() && stack.peek().tag.equals(event.tag)) {
                    LandmarkRegion completed = stack.pop();
                    completed.endPos = event.position;
                    regions.add(completed);
                }
            }
        }

        return regions;
    }

    /**
     * Finds all interactive elements with their positions.
     */
    private static List<InteractiveElement> findInteractiveElements(String cleaned) {
        List<InteractiveElement> elements = new ArrayList<>();
        Matcher matcher = INTERACTIVE_ELEMENT.matcher(cleaned);
        while (matcher.find()) {
            String html = matcher.group().trim();
            if (html.length() < 500) { // Skip data URIs / absurdly long inline content
                elements.add(new InteractiveElement(html, matcher.start(), matcher.end()));
            }
        }
        return elements;
    }

    /**
     * Finds the deepest (most specific) landmark that fully contains the element.
     */
    private static LandmarkRegion findDeepestEnclosingLandmark(List<LandmarkRegion> landmarks,
                                                               int elemStart, int elemEnd) {
        LandmarkRegion deepest = null;
        for (LandmarkRegion region : landmarks) {
            if (region.startPos <= elemStart && region.endPos >= elemEnd) {
                if (deepest == null || region.depth > deepest.depth) {
                    deepest = region;
                }
            }
        }
        return deepest;
    }

    /**
     * Formats a landmark tag + key attributes into a readable label.
     * Example: {@code <form id="login" action="/auth">} → {@code <form id="login" action="/auth">}
     */
    private static String formatLandmarkLabel(String tag, String rawAttrs) {
        if (rawAttrs == null || rawAttrs.isBlank()) return "<" + tag + ">";

        StringBuilder label = new StringBuilder("<").append(tag);
        Matcher attrMatcher = ATTR_EXTRACT.matcher(rawAttrs);
        while (attrMatcher.find()) {
            String attrName = attrMatcher.group(1);
            String attrValue = attrMatcher.group(2);
            // Truncate long class names
            if (attrName.equalsIgnoreCase("class") && attrValue.length() > 40) {
                attrValue = attrValue.substring(0, 37) + "...";
            }
            label.append(" ").append(attrName).append("=\"").append(attrValue).append("\"");
        }
        label.append(">");
        return label.toString();
    }

    // ──────────────────────── Structural Context ────────────────────────

    /**
     * Extracts page-level structural context: headings, title, meta info.
     * This gives the LLM page identity without wasting token budget on non-interactive elements.
     */
    private static String extractStructuralContext(String cleaned, int budget) {
        StringBuilder ctx = new StringBuilder("\n<!-- PAGE CONTEXT -->\n");

        // Extract title
        Matcher titleMatcher = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (titleMatcher.find()) {
            ctx.append("Page Title: ").append(titleMatcher.group(1).trim()).append("\n");
        }

        // Extract headings (h1-h3 only — deeper headings are usually noise)
        Matcher headingMatcher = Pattern.compile("<(h[1-3])[^>]*>([^<]+)</\\1>", Pattern.CASE_INSENSITIVE).matcher(cleaned);
        while (headingMatcher.find() && ctx.length() < budget - 200) {
            ctx.append(headingMatcher.group(1).toUpperCase()).append(": ")
               .append(headingMatcher.group(2).trim()).append("\n");
        }

        // If still within budget, add first chunk of cleaned DOM for structural context
        int remaining = budget - ctx.length();
        if (remaining > 500) {
            int contextEnd = Math.min(remaining, cleaned.length());
            ctx.append("\n<!-- STRUCTURAL CONTEXT (page start) -->\n");
            ctx.append(cleaned, 0, contextEnd);
        }

        return ctx.toString();
    }

    // ──────────────────────── Internal Models ────────────────────────

    private static class LandmarkRegion {
        String tag;
        String label;
        int startPos;
        int endPos;
        int depth;
    }

    private static class InteractiveElement {
        final String html;
        final int startPos;
        final int endPos;

        InteractiveElement(String html, int startPos, int endPos) {
            this.html = html;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }

    private static class TagEvent {
        final int position;
        final boolean isOpen;
        final String tag;
        final String label;

        TagEvent(int position, boolean isOpen, String tag, String label) {
            this.position = position;
            this.isOpen = isOpen;
            this.tag = tag;
            this.label = label;
        }
    }
}
