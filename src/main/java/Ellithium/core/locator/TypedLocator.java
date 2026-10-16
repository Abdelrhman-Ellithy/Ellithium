package Ellithium.core.locator;

import Ellithium.Utilities.helpers.Locators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Structured immutable locator record carrying compile-time strategy classification,
 * raw selector values, transparent Shadow DOM / iframe host hierarchy, and optional
 * reporting metadata.
 *
 * <p>Implements {@link Supplier<By>} for direct interoperability with Selenium and Ellithium actions.
 */
public record TypedLocator(
        LocatorType type,
        String value,
        By by,
        String hostSelector,
        String friendlyName
) implements Supplier<By> {

    /**
     * Compact canonical constructor ensuring non-null types and eager By instantiation.
     */
    public TypedLocator {
        Objects.requireNonNull(type, "LocatorType must not be null");
        Objects.requireNonNull(value, "Locator value must not be null");
        if (by == null) {
            by = buildBy(type, value, hostSelector);
        }
    }

    private static By buildBy(LocatorType type, String value, String hostSelector) {
        return switch (type) {
            case ID -> By.id(value);
            case CSS -> By.cssSelector(value);
            case XPATH -> By.xpath(value);
            case NAME -> By.name(value);
            case CLASS_NAME -> Locators.className(value);
            case TAG_NAME -> By.tagName(value);
            case LINK_TEXT -> By.linkText(value);
            case PARTIAL_LINK_TEXT -> By.partialLinkText(value);
            case SHADOW -> {
                if (hostSelector != null && !hostSelector.isBlank()) {
                    yield By.cssSelector(hostSelector.trim() + " >>> " + value.trim());
                }
                yield By.cssSelector(value);
            }
            case IFRAME -> By.cssSelector(value);
            case ACCESSIBILITY_ID -> AppiumBy.accessibilityId(value);
        };
    }

    /**
     * Unwraps this typed locator into a standard Selenium {@link By}.
     *
     * @return compiled By instance
     */
    public By toBy() {
        return by;
    }

    @Override
    public By get() {
        return by;
    }

    /**
     * Whether this locator represents a Shadow DOM pierced element.
     */
    public boolean isShadow() {
        return type == LocatorType.SHADOW || (type != LocatorType.IFRAME && hostSelector != null && !hostSelector.isBlank());
    }

    /**
     * Whether this locator targets an iframe-hosted element.
     */
    public boolean isIframe() {
        return type == LocatorType.IFRAME;
    }

    /**
     * Creates a copy of this TypedLocator with a descriptive friendly name for Allure reporting and logs.
     *
     * @param friendlyName human-readable element name
     * @return updated TypedLocator
     */
    public TypedLocator withFriendlyName(String friendlyName) {
        return new TypedLocator(type, value, by, hostSelector, friendlyName);
    }

    /**
     * Creates a copy of this TypedLocator with an attached shadow or iframe host selector.
     *
     * @param host CSS selector of the host element
     * @return updated TypedLocator
     */
    public TypedLocator withHostSelector(String host) {
        return new TypedLocator(type, value, null, host, friendlyName);
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    public static TypedLocator id(String id) {
        return new TypedLocator(LocatorType.ID, id, null, null, null);
    }

    public static TypedLocator id(String id, String friendlyName) {
        return new TypedLocator(LocatorType.ID, id, null, null, friendlyName);
    }

    public static TypedLocator css(String css) {
        return new TypedLocator(LocatorType.CSS, css, null, null, null);
    }

    public static TypedLocator css(String css, String friendlyName) {
        return new TypedLocator(LocatorType.CSS, css, null, null, friendlyName);
    }

    public static TypedLocator xpath(String xpath) {
        return new TypedLocator(LocatorType.XPATH, xpath, null, null, null);
    }

    public static TypedLocator xpath(String xpath, String friendlyName) {
        return new TypedLocator(LocatorType.XPATH, xpath, null, null, friendlyName);
    }

    public static TypedLocator name(String name) {
        return new TypedLocator(LocatorType.NAME, name, null, null, null);
    }

    public static TypedLocator name(String name, String friendlyName) {
        return new TypedLocator(LocatorType.NAME, name, null, null, friendlyName);
    }

    public static TypedLocator className(String className) {
        return new TypedLocator(LocatorType.CLASS_NAME, className, null, null, null);
    }

    public static TypedLocator className(String className, String friendlyName) {
        return new TypedLocator(LocatorType.CLASS_NAME, className, null, null, friendlyName);
    }

    public static TypedLocator tagName(String tagName) {
        return new TypedLocator(LocatorType.TAG_NAME, tagName, null, null, null);
    }

    public static TypedLocator tagName(String tagName, String friendlyName) {
        return new TypedLocator(LocatorType.TAG_NAME, tagName, null, null, friendlyName);
    }

    public static TypedLocator linkText(String text) {
        return new TypedLocator(LocatorType.LINK_TEXT, text, null, null, null);
    }

    public static TypedLocator linkText(String text, String friendlyName) {
        return new TypedLocator(LocatorType.LINK_TEXT, text, null, null, friendlyName);
    }

    public static TypedLocator partialLinkText(String text) {
        return new TypedLocator(LocatorType.PARTIAL_LINK_TEXT, text, null, null, null);
    }

    public static TypedLocator partialLinkText(String text, String friendlyName) {
        return new TypedLocator(LocatorType.PARTIAL_LINK_TEXT, text, null, null, friendlyName);
    }

    public static TypedLocator shadow(String hostSelector, String innerCss) {
        return new TypedLocator(LocatorType.SHADOW, innerCss, null, hostSelector, null);
    }

    public static TypedLocator shadow(String hostSelector, String innerCss, String friendlyName) {
        return new TypedLocator(LocatorType.SHADOW, innerCss, null, hostSelector, friendlyName);
    }

    public static TypedLocator iframe(String iframeHost, String innerCss) {
        return new TypedLocator(LocatorType.IFRAME, innerCss, null, iframeHost, null);
    }

    public static TypedLocator iframe(String iframeHost, String innerCss, String friendlyName) {
        return new TypedLocator(LocatorType.IFRAME, innerCss, null, iframeHost, friendlyName);
    }

    public static TypedLocator accessibilityId(String accessibilityId) {
        return new TypedLocator(LocatorType.ACCESSIBILITY_ID, accessibilityId, null, null, null);
    }

    public static TypedLocator accessibilityId(String accessibilityId, String friendlyName) {
        return new TypedLocator(LocatorType.ACCESSIBILITY_ID, accessibilityId, null, null, friendlyName);
    }

    /**
     * Resolves a {@link TypedLocator} from an existing Selenium {@link By} object.
     *
     * @param by source By instance
     * @return structured TypedLocator representation
     */
    public static TypedLocator fromBy(By by) {
        if (by == null) throw new IllegalArgumentException("by must not be null");
        String str = by.toString();
        int colon = str.indexOf(':');
        if (colon > 0) {
            String prefix = str.substring(0, colon).trim().toLowerCase();
            String val = str.substring(colon + 1).trim();
            if (prefix.contains("xpath")) {
                return new TypedLocator(LocatorType.XPATH, val, by, null, null);
            } else if (prefix.contains("css")) {
                if (val.contains(">>>")) {
                    String[] parts = val.split(">>>", 2);
                    return new TypedLocator(LocatorType.SHADOW, parts[1].trim(), by, parts[0].trim(), null);
                }
                return new TypedLocator(LocatorType.CSS, val, by, null, null);
            } else if (prefix.contains("id")) {
                return new TypedLocator(LocatorType.ID, val, by, null, null);
            } else if (prefix.contains("classname") || prefix.contains("class")) {
                return new TypedLocator(LocatorType.CLASS_NAME, val, by, null, null);
            } else if (prefix.contains("tagname") || prefix.contains("tag")) {
                return new TypedLocator(LocatorType.TAG_NAME, val, by, null, null);
            } else if (prefix.contains("name")) {
                return new TypedLocator(LocatorType.NAME, val, by, null, null);
            } else if (prefix.contains("partiallinktext")) {
                return new TypedLocator(LocatorType.PARTIAL_LINK_TEXT, val, by, null, null);
            } else if (prefix.contains("linktext")) {
                return new TypedLocator(LocatorType.LINK_TEXT, val, by, null, null);
            } else if (prefix.contains("accessibilityid")) {
                return new TypedLocator(LocatorType.ACCESSIBILITY_ID, val, by, null, null);
            }
        }
        return new TypedLocator(LocatorType.CSS, str, by, null, null);
    }

    /**
     * Resolves a {@link TypedLocator} from a string representation (e.g. {@code "id=username"} or {@code "//button"}).
     *
     * @param locatorStr string representation
     * @return structured TypedLocator
     */
    public static TypedLocator fromString(String locatorStr) {
        By by = Locators.fromString(locatorStr);
        return fromBy(by);
    }
}
