package Ellithium.core.ai.scoring;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts semantic element names from Java method names, field names, and locator values.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code "setUserEmail"} → {@code ["Email", "email", "EMAIL", "User Email"]}</li>
 *   <li>{@code "clickLoginBtn"} → {@code ["Login", "login", "LOGIN"]}</li>
 *   <li>{@code "passwordField"} → {@code ["password", "Password", "PASSWORD"]}</li>
 *   <li>{@code "By.id: user_name"} → {@code ["user_name", "user name", "User Name"]}</li>
 * </ul>
 */
public class SemanticNameExtractor {

    // Action prefixes to strip from method names (ordered by length, longest first)
    // These match actual Ellithium interaction method names
    private static final String[] ACTION_PREFIXES = {
            "selectDropdownByTextForMultipleElements",
            "selectDropdownBy", "deselectDropdownBy",
            "waitForVisibilityAnd", "waitForElementTo", "waitForElement",
            "scrollIntoView", "scrollToElement",
            "clickOnMultipleElements", "clickOnElement",
            "uploadMultipleFiles", "uploadFile",
            "setElementValueUsingJS", "javascriptClick", "uploadFileUsingJS",
            "hoverOverElement", "hoverAndClick",
            "moveSliderTo", "moveSliderByOffset",
            "dragAndDropByOffset", "dragAndDrop",
            "getAttributeValue", "getPropertyValue",
            "isAttributeContains", "isElementClickable",
            "isElementDisplayed", "isElementPresent",
            "isElementSelected", "isElementEnabled",
            "isTextContains",
            "clearElement", "deselectAll",
            "doubleClick", "rightClick",
            "doubleTap", "longPress", "twoFingerTap",
            "sendData", "getText",
            "hover", "swipe", "scroll", "pinch", "fling", "drag",
            "click", "tap", "get", "set", "is"
    };

    // UI suffixes to strip from field/method names
    private static final String[] UI_SUFFIXES = {
            "Element", "Locator", "Button", "Field", "Input",
            "TextBox", "Checkbox", "Radio", "Selector", "Link",
            "Dropdown", "Select", "Label", "Area", "Icon",
            "Btn", "Txt", "Lbl", "Chk", "Lnk", "Drp", "Sel"
    };

    // Pattern to split camelCase: "UserName" → ["User", "Name"]
    private static final Pattern CAMEL_SPLIT = Pattern.compile("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");

    // State and utility modifier tokens that describe transient UI state rather than element identity
    public static final Set<String> STATE_AND_UTILITY_MODIFIERS = Set.of(
            "active", "disabled", "enabled", "selected", "checked", "focused",
            "open", "closed", "show", "hide", "hidden", "visible", "collapsed",
            "expanded", "current", "valid", "invalid", "loading", "busy",
            "success", "danger", "warning", "info", "primary", "secondary",
            "item", "btn", "true", "false"
    );

    // Generic UI terms that dilute distinctive semantic matching across candidates
    public static final Set<String> GENERIC_UI_TERMS = Set.of(
            "btn", "button", "radio", "option", "field", "input", "select", "dropdown",
            "checkbox", "label", "area", "icon", "txt", "lbl", "chk", "lnk", "drp", "sel",
            "link", "item", "box", "form", "container", "wrapper", "element", "locator",
            "group", "row", "col", "cell", "table", "div", "span", "nav", "menu", "header",
            "footer", "content", "view", "section", "main", "app", "root", "page", "modal",
            "dialog", "popup", "pane", "panel", "card", "list", "bar",
            "id", "name", "class", "type", "value", "role", "for", "href", "src", "style",
            "title", "placeholder", "data", "aria", "test", "testid", "xpath", "css",
            "selector", "by", "contains", "text", "normalize", "space",
            "action", "target", "method", "tag", "tagname", "attr", "attribute"
    );

    public static boolean isStateOrUtilityModifier(String token) {
        if (token == null || token.isBlank()) return true;
        return STATE_AND_UTILITY_MODIFIERS.contains(token.toLowerCase(java.util.Locale.ROOT).trim());
    }

    public static boolean isGenericOrModifier(String token) {
        if (token == null || token.isBlank()) return true;
        String lower = token.toLowerCase(java.util.Locale.ROOT).trim();
        return GENERIC_UI_TERMS.contains(lower) || isStateOrUtilityModifier(lower);
    }

    /**
     * Extracts distinctive non-generic keywords from a locator (e.g. "firefox" from "radio-option-firefox").
     */
    public static Set<String> extractDistinctiveTokens(By locator) {
        if (locator == null) return Set.of();
        String locatorStr = locator.toString();
        int colonIdx = locatorStr.indexOf(':');
        String val = colonIdx >= 0 ? locatorStr.substring(colonIdx + 1).trim() : locatorStr;
        return extractDistinctiveTokens(val);
    }

    /**
     * Extracts distinctive non-generic keywords from locator text, stripping stop words and UI suffixes.
     */
    public static Set<String> extractDistinctiveTokens(String text) {
        if (text == null || text.isBlank()) return Set.of();
        String[] parts = text.split("[^a-zA-Z0-9]+");
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            String[] subParts = CAMEL_SPLIT.split(part);
            for (String sp : subParts) {
                String lower = sp.toLowerCase(java.util.Locale.ROOT).trim();
                if (lower.length() < 2) continue;
                if (isGenericOrModifier(lower)) continue;
                result.add(lower);
            }
        }
        return result;
    }

    private static final List<String> DISTINCTIVE_MATCH_KEYS = List.of(
            "id", "name", "data-testid", "data-test", "data-cy", "data-qa",
            "aria-label", "placeholder", "title", "value", "text", "class",
            "resource-id", "accessibility-id"
    );

    /**
     * Checks if a candidate element or its pre-fetched attributes match any distinctive keyword.
     */
    public static boolean candidateMatchesDistinctiveTokens(Map<String, Object> attrs, WebElement el, Set<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return false;
        if (attrs != null) {
            for (String token : tokens) {
                for (String key : DISTINCTIVE_MATCH_KEYS) {
                    Object val = attrs.get(key);
                    if (val != null) {
                        String s = val.toString().toLowerCase(java.util.Locale.ROOT);
                        if (matchesToken(s, token)) return true;
                    }
                }
            }
            return false;
        }
        if (el != null) {
            for (String token : tokens) {
                try {
                    for (String key : DISTINCTIVE_MATCH_KEYS) {
                        String val = "text".equals(key) ? el.getText() : el.getAttribute(key);
                        if (val != null && matchesToken(val.toLowerCase(java.util.Locale.ROOT), token)) {
                            return true;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static boolean matchesToken(String text, String token) {
        if (text == null || token == null) return false;
        if (token.length() >= 4) {
            return text.contains(token);
        }
        for (String word : text.split("[^a-zA-Z0-9]+")) {
            if (word.equalsIgnoreCase(token)) return true;
        }
        return false;
    }

    /**
     * Extracts semantic name candidates from all available context.
     *
     * @param methodName Method name (e.g., "setUserEmail"), may be null
     * @param fieldName  Field name (e.g., "emailField"), may be null
     * @param locatorValue The broken locator's value (e.g., "user_name"), may be null
     * @return Ordered set of semantic name candidates (most specific first)
     */
    public static List<String> extract(String methodName, String fieldName, String locatorValue) {
        Set<String> candidates = new LinkedHashSet<>(); // preserves insertion order, deduplicates

        // Priority 1: field name (most reliable — explicitly named by developer)
        if (fieldName != null && !fieldName.isBlank()) {
            addVariants(candidates, stripUISuffixes(fieldName));
        }

        // Priority 2: method name (strong signal — business-level naming)
        if (methodName != null && !methodName.isBlank()) {
            String stripped = stripActionPrefix(methodName);
            stripped = stripUISuffixes(stripped);
            if (!stripped.isEmpty()) {
                addVariants(candidates, stripped);
            }
        }

        // Priority 3: locator value (the actual broken locator content)
        if (locatorValue != null && !locatorValue.isBlank()) {
            // Handle underscore/hyphen-separated values: "user_name" → "user name"
            String normalized = locatorValue.replaceAll("[_\\-]", " ").trim();
            if (!normalized.isEmpty()) {
                candidates.add(normalized);
                candidates.add(locatorValue); // original form too
                // Capitalize each word: "user name" → "User Name"
                candidates.add(capitalizeWords(normalized));
            }
        }

        return new ArrayList<>(candidates);
    }

    /**
     * Strips known action prefixes from a method name.
     * "setUserEmail" → "UserEmail", "clickLoginBtn" → "LoginBtn"
     */
    static String stripActionPrefix(String name) {
        for (String prefix : ACTION_PREFIXES) {
            if (name.startsWith(prefix) && name.length() > prefix.length()) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }

    /**
     * Strips known UI element suffixes from a name.
     * "emailField" → "email", "loginButton" → "login", "submitBtn" → "submit"
     */
    static String stripUISuffixes(String name) {
        for (String suffix : UI_SUFFIXES) {
            if (name.endsWith(suffix) && name.length() > suffix.length()) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    /**
     * Adds case and split variants of a semantic name.
     */
    private static void addVariants(Set<String> candidates, String base) {
        if (base == null || base.isEmpty()) return;

        // Original
        candidates.add(base);
        // Lowercase
        candidates.add(base.toLowerCase());
        // Uppercase
        candidates.add(base.toUpperCase());
        // First letter uppercase, rest lowercase
        candidates.add(Character.toUpperCase(base.charAt(0)) + base.substring(1).toLowerCase());

        // CamelCase split: "UserName" → also try "User" and "Name" separately
        String[] parts = CAMEL_SPLIT.split(base);
        if (parts.length > 1) {
            // Join with space: "User Name"
            candidates.add(String.join(" ", parts));
            candidates.add(String.join(" ", parts).toLowerCase());
            // Each part individually
            for (String part : parts) {
                if (part.length() >= 3) { // skip very short fragments
                    candidates.add(part);
                    candidates.add(part.toLowerCase());
                }
            }
        }
    }

    private static String capitalizeWords(String input) {
        StringBuilder sb = new StringBuilder();
        for (String word : input.split("\\s+")) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
