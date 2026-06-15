package Ellithium.core.ai;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Driver runtime profile — replaces scattered {@code driver instanceof AppiumDriver} checks across
 * the healing tiers. The ensemble, fingerprint, candidate provider, and source patcher all branch
 * on this so platform-specific code lives in one place per concern.
 *
 * <ul>
 *   <li>{@link #WEB} — Selenium against a real browser. CSS/RelativeLocator/JS-batch all work.</li>
 *   <li>{@link #MOBILE_WEBVIEW} — Appium driver currently in a WEBVIEW context. DOM JS works,
 *       CSS works; treat like {@link #WEB} for selectors but emit {@code AppiumBy} when source-
 *       patching (the driver type stays {@code AppiumDriver}).</li>
 *   <li>{@link #MOBILE_NATIVE} — Appium native (UiAutomator2 / XCUITest). No DOM, no CSS engine.
 *       Use {@code AppiumBy.accessibilityId} / {@code androidUIAutomator} / {@code iOSClassChain}
 *       and bound the candidate scan tighter (each {@code findElement} is ~150–400 ms).</li>
 * </ul>
 */
public enum DriverProfile {
    WEB,
    MOBILE_WEBVIEW,
    MOBILE_NATIVE;

    public boolean isMobile()      { return this == MOBILE_WEBVIEW || this == MOBILE_NATIVE; }
    public boolean isNativeMobile(){ return this == MOBILE_NATIVE; }
    public boolean supportsDomJs() { return this == WEB || this == MOBILE_WEBVIEW; }
    public boolean supportsCss()   { return this == WEB || this == MOBILE_WEBVIEW; }

    /**
     * Detects the driver's profile. WebView detection on Appium uses the active context name;
     * if the context query fails for any reason (some Appium versions throw) it falls back to
     * {@link #MOBILE_NATIVE}, which is the safer default (skip DOM JS).
     */
    public static DriverProfile detect(WebDriver driver) {
        if (!(driver instanceof AppiumDriver)) return WEB;
        if (driver instanceof io.appium.java_client.remote.SupportsContextSwitching ctxAware) {
            try {
                String ctx = ctxAware.getContext();
                if (ctx != null && ctx.toUpperCase().contains("WEBVIEW")) return MOBILE_WEBVIEW;
                if (ctx != null && ctx.toUpperCase().startsWith("NATIVE")) return MOBILE_NATIVE;
            } catch (Exception ignored) {}
        }
        if (driver instanceof JavascriptExecutor) {
            try {
                Object probe = ((JavascriptExecutor) driver)
                        .executeScript("return document && document.readyState ? 1 : 0;");
                if (probe instanceof Number n && n.intValue() > 0) return MOBILE_WEBVIEW;
            } catch (Exception ignored) {}
        }
        return MOBILE_NATIVE;
    }
}
