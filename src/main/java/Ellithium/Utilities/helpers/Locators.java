package Ellithium.Utilities.helpers;

import org.openqa.selenium.By;

/**
 * Safe {@link By} factories for common Selenium construction-time pitfalls. {@code By.className}
 * throws {@code InvalidArgumentException} synchronously when given a value containing whitespace
 * (a multi-class string) — that failure happens inside Selenium's own constructor, before the
 * locator object exists, so Ellithium's healing cascade (which only ever sees an already-constructed
 * {@code By}) can never intercept it. Use these factories in place of the raw {@code By} methods to
 * avoid the construction-time exception entirely.
 */
public final class Locators {

    private Locators() {}

    /**
     * Builds a class-name locator, tolerating a space-separated multi-class value.
     * A single class name delegates to {@link By#className(String)} unchanged; a value containing
     * whitespace (e.g. {@code "btn primary"}) is rebuilt as the equivalent dot-joined CSS selector
     * ({@code .btn.primary}) instead of letting {@code By.className} throw.
     *
     * @param value one class name, or several separated by whitespace
     * @return a {@code By} that resolves the same set of elements a valid {@code By.className} would
     */
    public static By className(String value) {
        if (value == null) return By.className(value);
        String trimmed = value.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length <= 1) return By.className(trimmed);
        StringBuilder css = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            css.append('.').append(escapeCssIdent(part));
        }
        return By.cssSelector(css.toString());
    }

    /**
     * Escapes characters CSS would otherwise parse as selector syntax rather than literal class-name
     * characters — utility-class frameworks (Tailwind's {@code hover:bg-red-500}, {@code w-[100px]})
     * routinely produce classes containing {@code :}/{@code [}/{@code ]}, which are meaningless as
     * literal characters to CSS unless backslash-escaped.
     */
    private static String escapeCssIdent(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                out.append(c);
            } else {
                out.append('\\').append(c);
            }
        }
        return out.toString();
    }

    private static boolean isXPath(String selector) {
        return selector.startsWith("/") || selector.startsWith("(") || selector.startsWith("./") || selector.startsWith("../");
    }

    /**
     * Parses a string representation of a locator into a Selenium {@link By} object.
     * Supports prefix format (e.g. {@code "id=username"}, {@code "xpath=//button"},
     * {@code "css=.submit-btn"}) as well as raw XPath and CSS strings.
     *
     * @param locatorStr The locator string to parse
     * @return Resolved {@link By} locator
     */
    public static By fromString(String locatorStr) {
        if (locatorStr == null || locatorStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Locator string cannot be null or empty");
        }
        String trimmed = locatorStr.trim();
        if (isXPath(trimmed)) {
            return By.xpath(trimmed);
        }
        int eq = trimmed.indexOf('=');
        if (eq > 0) {
            String strategy = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            return switch (strategy) {
                case "id" -> By.id(value);
                case "name" -> By.name(value);
                case "class", "classname" -> className(value);
                case "css", "cssselector" -> By.cssSelector(value);
                case "xpath" -> By.xpath(value);
                case "tag", "tagname" -> By.tagName(value);
                case "link", "linktext" -> By.linkText(value);
                case "partiallink", "partiallinktext" -> By.partialLinkText(value);
                default -> isXPath(trimmed)
                        ? By.xpath(trimmed)
                        : By.cssSelector(trimmed);
            };
        }
        return By.cssSelector(trimmed);
    }

    /**
     * Converts a Selenium {@link By} to a structured {@link Ellithium.core.locator.TypedLocator}.
     *
     * @param by source By locator
     * @return structured TypedLocator
     */
    public static Ellithium.core.locator.TypedLocator typed(By by) {
        return Ellithium.core.locator.TypedLocator.fromBy(by);
    }

    /**
     * Parses a string representation into a structured {@link Ellithium.core.locator.TypedLocator}.
     *
     * @param locatorStr string locator representation
     * @return structured TypedLocator
     */
    public static Ellithium.core.locator.TypedLocator typed(String locatorStr) {
        return Ellithium.core.locator.TypedLocator.fromString(locatorStr);
    }
}
