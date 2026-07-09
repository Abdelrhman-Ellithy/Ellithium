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
}
