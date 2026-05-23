package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.models.ElementFingerprint;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Builds a canonical semantic query string for embedding-based similarity search (Tier 3)
 * and enriched LLM prompts (Tier 4).
 *
 * <p>Query quality is the single biggest driver of embedding accuracy — a well-constructed
 * query from all available context beats a better model with a naive locator-string query.
 * Assembly order (highest signal first):
 * <pre>
 *   [expanded_action_type] [de-camelCased locator value] [de-camelCased method name]
 *   [locator hint annotation] [last known text] [last known id] [last known aria-label]
 *   [last known placeholder] [last known data-testid] [tag name]
 * </pre>
 *
 * <p>Example: {@code clickOnElement}, {@code By.id("loginSubmitBtn")}, method={@code clickLoginButton},
 * last-text={@code "Sign in"} → {@code "click press button login submit login button sign in"}</p>
 */
public class SemanticQueryBuilder {

    // Splits camelCase and PascalCase at boundaries; also splits on - and _
    private static final Pattern CAMEL_SPLIT =
            Pattern.compile("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|[-_]");

    // Parses By.toString() to extract the locator value: "By.id: loginBtn" → "loginBtn"
    private static final Pattern BY_VALUE = Pattern.compile("By\\.[a-zA-Z]+:\\s*(.*)");

    // Action type → expanded descriptive tokens (broadens the semantic field for embedding)
    private static final Map<String, String> ACTION_EXPANSIONS = new LinkedHashMap<>();

    static {
        ACTION_EXPANSIONS.put("senddata",          "type input enter text");
        ACTION_EXPANSIONS.put("settext",            "type input enter text");
        ACTION_EXPANSIONS.put("clickonelement",     "click press button");
        ACTION_EXPANSIONS.put("click",              "click press button");
        ACTION_EXPANSIONS.put("tap",                "tap click press");
        ACTION_EXPANSIONS.put("doubletap",          "double tap click");
        ACTION_EXPANSIONS.put("doubleclick",        "double click press");
        ACTION_EXPANSIONS.put("rightclick",         "right click context menu");
        ACTION_EXPANSIONS.put("longpress",          "long press hold");
        ACTION_EXPANSIONS.put("selectdropdown",     "select dropdown option choose");
        ACTION_EXPANSIONS.put("selectdropdownby",   "select dropdown option choose");
        ACTION_EXPANSIONS.put("gettext",            "read text label value");
        ACTION_EXPANSIONS.put("getattributevalue",  "read attribute property");
        ACTION_EXPANSIONS.put("iselementdisplayed", "check visible display");
        ACTION_EXPANSIONS.put("iselementpresent",   "check exists present");
        ACTION_EXPANSIONS.put("iselementclickable", "check clickable enabled");
        ACTION_EXPANSIONS.put("iselementselected",  "check selected state");
        ACTION_EXPANSIONS.put("uploadfile",         "upload file input attach");
        ACTION_EXPANSIONS.put("uploadmultiplefiles","upload file input attach multiple");
        ACTION_EXPANSIONS.put("hoverelement",       "hover mouse over");
        ACTION_EXPANSIONS.put("hoveroverelement",   "hover mouse over");
        ACTION_EXPANSIONS.put("hoverandclick",      "hover mouse click");
        ACTION_EXPANSIONS.put("clearelement",       "clear input reset");
        ACTION_EXPANSIONS.put("movetoelement",      "scroll move element");
        ACTION_EXPANSIONS.put("scrollintoelement",  "scroll into view");
        ACTION_EXPANSIONS.put("dragandrop",         "drag drop move");
        ACTION_EXPANSIONS.put("draganddropbyoffset","drag drop offset");
        ACTION_EXPANSIONS.put("swipe",              "swipe scroll gesture");
        ACTION_EXPANSIONS.put("scroll",             "scroll swipe");
        ACTION_EXPANSIONS.put("movesliderto",       "slider range move");
        ACTION_EXPANSIONS.put("movesliderbyoffset", "slider range offset");
        ACTION_EXPANSIONS.put("javascriptclick",    "click javascript button");
        ACTION_EXPANSIONS.put("waitforvisibility",  "wait visible appear");
        ACTION_EXPANSIONS.put("waitforelement",     "wait element appear");
    }

    // ──────────────────────── Public API ────────────────────────

    /**
     * Builds a canonical query from full heal context and a stored fingerprint.
     *
     * @param actionType   The Ellithium action type (e.g., "sendData", "clickOnElement")
     * @param locatorValue The broken locator's raw value (e.g., "loginBtn" or "By.id: loginBtn")
     * @param methodName   The caller POM method name (e.g., "clickLoginButton"), may be null
     * @param fingerprint  Stored baseline fingerprint for last-known attribute context, may be null
     * @return Single-space lowercase query string ready for tokenizer input
     */
    public static String buildFromContext(String actionType, String locatorValue,
                                          String methodName, ElementFingerprint fingerprint) {
        String lastText        = fingerprint != null ? fingerprint.getText()        : null;
        String lastId          = fingerprint != null ? fingerprint.getId()          : null;
        String lastAriaLabel   = fingerprint != null ? fingerprint.getAriaLabel()   : null;
        String lastPlaceholder = fingerprint != null ? fingerprint.getPlaceholder() : null;
        String lastDataTestId  = fingerprint != null ? fingerprint.getDataTestId()  : null;
        String lastTagName     = fingerprint != null ? fingerprint.getTagName()     : null;

        // READABLE intent (getText/verify…) describes WHICH element to read, not its rendered content.
        // Injecting the last-known text makes the embedding match on lexical overlap with page copy
        // (e.g. method "getLoginMessage" + last text "Login Page" wrongly pulled toward the <h2>
        // instead of the flash message). Drop raw last-text for READABLE; keep structural signals.
        boolean isReadable = SemanticLocatorResolver.ElementCategory.READABLE
                == SemanticLocatorResolver.categorizeAction(actionType);
        if (isReadable) lastText = null;

        return build(actionType, locatorValue, methodName, null,
                lastText, lastId, lastAriaLabel, lastPlaceholder, lastDataTestId, lastTagName);
    }

    /**
     * Builds a canonical query from individual field values.
     *
     * @param actionType    The Ellithium action type (e.g., "sendData")
     * @param locatorValue  The broken locator value (e.g., "loginBtn" or full By.toString())
     * @param methodName    The caller POM method name, may be null
     * @param hint          A @LocatorHint annotation value, may be null
     * @param lastText      Last known element text, may be null
     * @param lastId        Last known element id attribute, may be null
     * @param lastAriaLabel Last known aria-label, may be null
     * @param lastPlaceholder Last known placeholder, may be null
     * @param lastDataTestId  Last known data-testid, may be null
     * @param lastTagName     Last known tag name, may be null
     * @return Single-space lowercase query string
     */
    public static String build(String actionType, String locatorValue, String methodName,
                                String hint,
                                String lastText, String lastId, String lastAriaLabel,
                                String lastPlaceholder, String lastDataTestId, String lastTagName) {
        List<String> tokens = new ArrayList<>();

        // 1. Action type expansion (highest signal — tells the model what interaction is expected)
        if (isPresent(actionType)) {
            tokens.add(expandAction(actionType));
        }

        // 2. De-camelCased locator value (strip By prefix if present)
        if (isPresent(locatorValue)) {
            tokens.add(deCamelCase(extractLocatorValue(locatorValue)));
        }

        // 3. De-camelCased method name (minus action prefix)
        if (isPresent(methodName)) {
            String stripped = SemanticNameExtractor.stripActionPrefix(methodName);
            stripped = SemanticNameExtractor.stripUISuffixes(stripped);
            if (!stripped.isBlank()) {
                tokens.add(deCamelCase(stripped));
            }
        }

        // 4. Hint annotation value (developer-provided — highest semantic clarity)
        if (isPresent(hint)) {
            tokens.add(hint.toLowerCase());
        }

        // 5. Last known element text (most visible user-facing signal)
        if (isPresent(lastText) && lastText.length() <= 100) {
            tokens.add(lastText.toLowerCase());
        }

        // 6. Last known id (structural signal)
        if (isPresent(lastId)) {
            tokens.add(deCamelCase(lastId));
        }

        // 7. Last known aria-label (accessibility label)
        if (isPresent(lastAriaLabel)) {
            tokens.add(lastAriaLabel.toLowerCase());
        }

        // 8. Last known placeholder (input hint text)
        if (isPresent(lastPlaceholder)) {
            tokens.add(lastPlaceholder.toLowerCase());
        }

        // 9. Last known data-testid
        if (isPresent(lastDataTestId)) {
            tokens.add(deCamelCase(lastDataTestId));
        }

        // 10. Tag name (structural context)
        if (isPresent(lastTagName)) {
            tokens.add(lastTagName.toLowerCase());
        }

        return tokens.stream()
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .collect(Collectors.joining(" "))
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    // ──────────────────────── Utilities ────────────────────────

    /**
     * Splits camelCase/PascalCase/kebab-case/snake_case identifier into space-separated
     * lowercase tokens. "loginSubmitBtn" → "login submit btn".
     * "data-testid" → "data testid". "user_name" → "user name".
     */
    public static String deCamelCase(String identifier) {
        if (identifier == null || identifier.isBlank()) return "";
        String[] parts = CAMEL_SPLIT.split(identifier);
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.joining(" "));
    }

    /**
     * Expands an action type to a broader set of descriptive tokens.
     * "sendData" → "type input enter text". Gracefully falls back to de-camelCased action name.
     */
    public static String expandAction(String actionType) {
        if (actionType == null || actionType.isBlank()) return "";
        String key = actionType.toLowerCase().replace("-", "").replace("_", "");
        // Exact match first
        if (ACTION_EXPANSIONS.containsKey(key)) {
            return ACTION_EXPANSIONS.get(key);
        }
        // Prefix match (for methods like "waitForVisibilityAndClick")
        for (Map.Entry<String, String> entry : ACTION_EXPANSIONS.entrySet()) {
            if (key.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        // Fallback: de-camelCase the action type itself
        return deCamelCase(actionType);
    }

    /**
     * Extracts the value portion from a By.toString() representation.
     * "By.id: loginBtn" → "loginBtn". "By.cssSelector: [data-testid='foo']" → "[data-testid='foo']".
     * If no By prefix is found, returns the input unchanged.
     */
    public static String extractLocatorValue(String locatorOrValue) {
        if (locatorOrValue == null) return "";
        java.util.regex.Matcher m = BY_VALUE.matcher(locatorOrValue.trim());
        return m.find() ? m.group(1).trim() : locatorOrValue.trim();
    }

    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }
}
