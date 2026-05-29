package Ellithium.Utilities.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
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
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
