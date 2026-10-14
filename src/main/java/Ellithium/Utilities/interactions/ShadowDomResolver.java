package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchShadowRootException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance, automatic Shadow DOM resolution engine for Ellithium.
 *
 * <p>Enables the framework to transparently pierce and locate elements within open Shadow DOM
 * boundaries across standard element interactions, waits, and assertions without requiring explicit
 * shadow root chaining or manual shadow actions.</p>
 *
 * <p><b>Architecture:</b>
 * <ol>
 *   <li><b>Tier 1 (Native Selenium 4 SearchContext):</b> Inspects shadow host elements and retrieves
 *       {@link SearchContext} via {@code host.getShadowRoot()} using 100% pure native Selenium W3C
 *       endpoints (Zero JavaScript). Supports direct, compound ({@code >>>}), and parent-child selectors.</li>
 *   <li><b>Tier 2 (Deep Recursive JS Piercing Fallback):</b> Invoked when native Selenium cannot resolve
 *       the locator (e.g. XPath queries, text predicates, link text, or deeply nested shadow trees).
 *       A recursive DOM script traverses {@code document} and all open {@code shadowRoot} instances.</li>
 * </ol>
 * </p>
 *
 * <p><b>Zero Overhead:</b> This engine is only invoked as a fallback when standard document-level
 * resolution fails (e.g. on {@code NoSuchElementException} or wait timeouts). Successful standard DOM
 * queries experience 0ms overhead.</p>
 */
public final class ShadowDomResolver {

    private static final Pattern BY_TOSTRING =
            Pattern.compile("By\\.([a-zA-Z]+):\\s*(.*)", Pattern.DOTALL);

    static final String DEEP_JS_SCRIPT =
            "var sel = arguments[0];" +
            "var strategy = arguments[1];" +
            "var matchAll = arguments[2];" +
            "var out = [];" +
            "function checkRoot(root) {" +
            "  if (!root) return;" +
            "  try {" +
            "    if (strategy === 'css') {" +
            "      var els = root.querySelectorAll(sel);" +
            "      for (var i = 0; i < els.length; i++) {" +
            "        out.push(els[i]);" +
            "        if (!matchAll) return;" +
            "      }" +
            "    } else if (strategy === 'xpath') {" +
            "      var q = sel;" +
            "      if (root.nodeType === 11 && q.indexOf('//') === 0) q = '.' + q;" +
            "      var found = false;" +
            "      try {" +
            "        var snap = document.evaluate(q, root, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);" +
            "        if (snap && snap.snapshotLength > 0) {" +
            "          for (var j = 0; j < snap.snapshotLength; j++) {" +
            "            out.push(snap.snapshotItem(j));" +
            "            if (!matchAll) return;" +
            "          }" +
            "          found = true;" +
            "        }" +
            "      } catch(e) {}" +
            "      if (!found) {" +
            "        var all = root.querySelectorAll('*');" +
            "        for (var k = 0; k < all.length; k++) {" +
            "          var el = all[k];" +
            "          if (q.indexOf('text()') !== -1) {" +
            "            var tm = q.match(/text\\(\\)\\s*=\\s*['\"]([^'\"]+)['\"]/);" +
            "            if (tm && el.textContent.trim() === tm[1]) {" +
            "              out.push(el);" +
            "              if (!matchAll) return;" +
            "            }" +
            "            var tmc = q.match(/contains\\s*\\(\\s*text\\(\\)\\s*,\\s*['\"]([^'\"]+)['\"]\\s*\\)/);" +
            "            if (tmc && el.textContent.indexOf(tmc[1]) !== -1) {" +
            "              out.push(el);" +
            "              if (!matchAll) return;" +
            "            }" +
            "          }" +
            "        }" +
            "      }" +
            "    } else if (strategy === 'linkText') {" +
            "      var links = root.querySelectorAll('a');" +
            "      for (var l = 0; l < links.length; l++) {" +
            "        if (links[l].textContent.trim() === sel) {" +
            "          out.push(links[l]);" +
            "          if (!matchAll) return;" +
            "        }" +
            "      }" +
            "    } else if (strategy === 'partialLinkText') {" +
            "      var plinks = root.querySelectorAll('a');" +
            "      for (var m = 0; m < plinks.length; m++) {" +
            "        if (plinks[m].textContent.indexOf(sel) !== -1) {" +
            "          out.push(plinks[m]);" +
            "          if (!matchAll) return;" +
            "        }" +
            "      }" +
            "    }" +
            "  } catch(err) {}" +
            "}" +
            "function walk(node) {" +
            "  if (!node) return;" +
            "  if (node !== document) {" +
            "    checkRoot(node);" +
            "    if (!matchAll && out.length > 0) return;" +
            "  }" +
            "  var children = [];" +
            "  try { children = node.querySelectorAll('*'); } catch(e) {}" +
            "  for (var n = 0; n < children.length; n++) {" +
            "    if (children[n].shadowRoot) {" +
            "      walk(children[n].shadowRoot);" +
            "      if (!matchAll && out.length > 0) return;" +
            "    }" +
            "  }" +
            "}" +
            "walk(document);" +
            "return matchAll ? out : (out.length > 0 ? out[0] : null);";

    private ShadowDomResolver() {}

    /**
     * Checks if the driver supports Shadow DOM inspection.
     *
     * @param driver the current WebDriver instance
     * @return true if Shadow DOM resolution is possible, false otherwise
     */
    public static boolean isSupported(WebDriver driver) {
        if (driver == null) return false;
        return !InteractionRecovery.isNativeMobileContext(driver);
    }

    /**
     * Checks if the driver supports JavaScript execution for fallback piercing.
     */
    public static boolean isJsSupported(WebDriver driver) {
        return driver instanceof JavascriptExecutor;
    }

    /**
     * Finds the first element matching {@code locator} across all open Shadow DOM roots.
     * Uses pure native Selenium 4 SearchContext first, and falls back to deep recursive JS piercing.
     *
     * @param driver the WebDriver instance
     * @param locator the element locator
     * @return the resolved {@link WebElement}, or {@code null} if not found
     */
    public static WebElement findElement(WebDriver driver, By locator) {
        if (!isSupported(driver) || locator == null) return null;
        By normalized = BaseActions.normalizeLocator(locator);
        String css = toCssSelector(normalized);

        // Tier 1: Try Native Selenium 4 SearchContext first (Zero JS)
        if (css != null && !css.isEmpty()) {
            List<WebElement> nativeHits = findNativeShadowElements(driver, css, true);
            if (!nativeHits.isEmpty()) {
                WebElement el = nativeHits.get(0);
                Reporter.log("[SHADOW DOM] Resolved element via native Selenium SearchContext: " + normalized, LogLevel.INFO_GREEN);
                return el;
            }
        }

        // Tier 2: Deep Recursive JS Piercing Fallback
        if (isJsSupported(driver)) {
            WebElement deepHit = findDeepJsElement(driver, normalized);
            if (deepHit != null) {
                Reporter.log("[SHADOW DOM] Resolved element via deep JS piercing fallback: " + normalized, LogLevel.INFO_GREEN);
                return deepHit;
            }
        }

        return null;
    }

    /**
     * Finds all elements matching {@code locator} across all open Shadow DOM roots.
     * Uses pure native Selenium 4 SearchContext first, and falls back to deep recursive JS piercing.
     *
     * @param driver the WebDriver instance
     * @param locator the element locator
     * @return list of matching {@link WebElement} instances (never null, empty if none found)
     */
    public static List<WebElement> findElements(WebDriver driver, By locator) {
        if (!isSupported(driver) || locator == null) return Collections.emptyList();
        By normalized = BaseActions.normalizeLocator(locator);
        String css = toCssSelector(normalized);

        // Tier 1: Try Native Selenium 4 SearchContext first (Zero JS)
        if (css != null && !css.isEmpty()) {
            List<WebElement> nativeHits = findNativeShadowElements(driver, css, false);
            if (!nativeHits.isEmpty()) {
                Reporter.log("[SHADOW DOM] Found " + nativeHits.size() + " element(s) via native Selenium SearchContext: " + normalized, LogLevel.INFO_GREEN);
                return nativeHits;
            }
        }

        // Tier 2: Deep Recursive JS Piercing Fallback
        if (isJsSupported(driver)) {
            List<WebElement> deepHits = findDeepJsElements(driver, normalized);
            if (!deepHits.isEmpty()) {
                Reporter.log("[SHADOW DOM] Found " + deepHits.size() + " element(s) via deep JS piercing fallback: " + normalized, LogLevel.INFO_GREEN);
                return deepHits;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Waits for an element inside Shadow DOM to become present and visible within the timeout budget.
     *
     * @param driver the WebDriver instance
     * @param locator the element locator
     * @param timeoutMillis maximum wait duration in milliseconds
     * @param pollingEveryMs polling interval in milliseconds
     * @return the visible {@link WebElement}, or {@code null} if timeout expires
     */
    public static WebElement findVisibleElement(WebDriver driver, By locator, long timeoutMillis, int pollingEveryMs) {
        if (!isSupported(driver) || locator == null) return null;
        long deadline = System.currentTimeMillis() + Math.max(50L, timeoutMillis);
        int poll = Math.max(50, pollingEveryMs);

        while (System.currentTimeMillis() < deadline) {
            try {
                WebElement el = findElement(driver, locator);
                if (el != null && isDisplayedSafe(el)) {
                    return el;
                }
            } catch (Exception ignored) {}
            try {
                Thread.sleep(poll);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    /**
     * Pure native Selenium 4 SearchContext resolver without any JavaScript execution.
     */
    static List<WebElement> findNativeShadowElements(WebDriver driver, String cssSelector, boolean firstOnly) {
        if (driver == null || cssSelector == null || cssSelector.isEmpty()) return Collections.emptyList();
        List<WebElement> results = new ArrayList<>();

        // Strategy A: Chained shadow selector using '>>>' (e.g. "my-host >>> button.edit")
        if (cssSelector.contains(">>>")) {
            String[] segments = cssSelector.split("\\s*>>>\\s*");
            try {
                WebElement host = driver.findElement(By.cssSelector(segments[0].trim()));
                SearchContext currentContext = host.getShadowRoot();
                for (int i = 1; i < segments.length; i++) {
                    String part = segments[i].trim();
                    if (i == segments.length - 1) {
                        List<WebElement> found = currentContext.findElements(By.cssSelector(part));
                        if (!found.isEmpty()) {
                            results.addAll(found);
                            return results;
                        }
                    } else {
                        WebElement nextHost = currentContext.findElement(By.cssSelector(part));
                        currentContext = nextHost.getShadowRoot();
                    }
                }
            } catch (Exception ignored) {}
        }

        // Strategy B: Space-separated host-child selector (e.g. "my-card button")
        int firstSpace = cssSelector.indexOf(' ');
        if (firstSpace > 0) {
            String potentialHost = cssSelector.substring(0, firstSpace).trim();
            String innerTarget = cssSelector.substring(firstSpace + 1).trim();
            if (!potentialHost.isEmpty() && !innerTarget.isEmpty()) {
                try {
                    List<WebElement> hosts = driver.findElements(By.cssSelector(potentialHost));
                    for (WebElement host : hosts) {
                        try {
                            SearchContext shadow = host.getShadowRoot();
                            if (shadow != null) {
                                List<WebElement> found = shadow.findElements(By.cssSelector(innerTarget));
                                if (!found.isEmpty()) {
                                    results.addAll(found);
                                    if (firstOnly) return results;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }

        // Strategy C: Pure native scan of candidate elements in current DOM using host.getShadowRoot()
        if (!cssSelector.contains(">>>")) {
            try {
                By cssBy = By.cssSelector(cssSelector);
                List<WebElement> candidates = driver.findElements(By.cssSelector("*"));
                int checked = 0;
                final int maxNativeChecks = 150; // Bound pure native calls to prevent wire latency

                for (WebElement candidate : candidates) {
                    if (checked++ >= maxNativeChecks) break;
                    try {
                        SearchContext shadow = candidate.getShadowRoot();
                        if (shadow != null) {
                            List<WebElement> found = shadow.findElements(cssBy);
                            if (!found.isEmpty()) {
                                results.addAll(found);
                                if (firstOnly) return results;
                            }
                        }
                    } catch (NoSuchShadowRootException | UnsupportedOperationException ignored) {
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        return results;
    }

    static WebElement findDeepJsElement(WebDriver driver, By locator) {
        if (!(driver instanceof JavascriptExecutor js)) return null;
        LocatorSpec spec = parseLocator(locator);
        try {
            Object res = js.executeScript(DEEP_JS_SCRIPT, spec.query(), spec.strategy(), false);
            return (res instanceof WebElement we) ? we : null;
        } catch (Exception e) {
            return null;
        }
    }

    static List<WebElement> findDeepJsElements(WebDriver driver, By locator) {
        if (!(driver instanceof JavascriptExecutor js)) return Collections.emptyList();
        LocatorSpec spec = parseLocator(locator);
        try {
            Object res = js.executeScript(DEEP_JS_SCRIPT, spec.query(), spec.strategy(), true);
            if (res instanceof List<?> list) {
                List<WebElement> result = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item instanceof WebElement we) result.add(we);
                }
                return result;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static boolean isDisplayedSafe(WebElement el) {
        try {
            return el.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public static String toCssSelector(By locator) {
        if (locator == null) return null;
        String s = locator.toString();
        int colon = s.indexOf(':');
        if (colon < 0) return null;
        String strategy = s.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        String value = s.substring(colon + 1).trim();

        if (strategy.contains("cssselector")) {
            return value;
        } else if (strategy.contains("id")) {
            return "#" + escapeCssIdent(value);
        } else if (strategy.contains("classname")) {
            return "." + escapeCssIdent(value);
        } else if (strategy.contains("tagname")) {
            return value;
        } else if (strategy.contains("name")) {
            return "[name='" + escapeQuotes(value) + "']";
        } else if (strategy.contains("xpath")) {
            return convertSimpleXpathToCss(value);
        }
        return null;
    }

    private static String escapeCssIdent(String val) {
        if (val == null) return "";
        StringBuilder sb = new StringBuilder(val.length());
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('\\').append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeQuotes(String val) {
        return val.replace("'", "\\'");
    }

    static String convertSimpleXpathToCss(String xpath) {
        if (xpath == null || xpath.isEmpty()) return null;
        String trimmed = xpath.trim();
        if (trimmed.startsWith(".//")) trimmed = trimmed.substring(3);
        else if (trimmed.startsWith("//")) trimmed = trimmed.substring(2);
        else if (trimmed.startsWith("./")) trimmed = trimmed.substring(2);
        else if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);

        if (trimmed.matches("^[a-zA-Z0-9*_-]+$")) {
            return "*".equals(trimmed) ? null : trimmed;
        }
        Matcher mId = Pattern.compile("^([a-zA-Z0-9*_-]*)\\[@id=['\"]([^'\"]+)['\"]\\]$").matcher(trimmed);
        if (mId.matches()) {
            String tag = "*".equals(mId.group(1)) ? "" : mId.group(1);
            return tag + "#" + escapeCssIdent(mId.group(2));
        }
        Matcher mClass = Pattern.compile("^([a-zA-Z0-9*_-]*)\\[@class=['\"]([^'\"]+)['\"]\\]$").matcher(trimmed);
        if (mClass.matches()) {
            String tag = "*".equals(mClass.group(1)) ? "" : mClass.group(1);
            return tag + "." + escapeCssIdent(mClass.group(2));
        }
        Matcher mAttr = Pattern.compile("^([a-zA-Z0-9*_-]*)\\[@([a-zA-Z0-9_-]+)=['\"]([^'\"]+)['\"]\\]$").matcher(trimmed);
        if (mAttr.matches()) {
            String tag = "*".equals(mAttr.group(1)) ? "" : mAttr.group(1);
            return tag + "[" + mAttr.group(2) + "='" + escapeQuotes(mAttr.group(3)) + "']";
        }
        Matcher mContains = Pattern.compile("^([a-zA-Z0-9*_-]*)\\[contains\\(@([a-zA-Z0-9_-]+),\\s*['\"]([^'\"]+)['\"]\\)\\]$").matcher(trimmed);
        if (mContains.matches()) {
            String tag = "*".equals(mContains.group(1)) ? "" : mContains.group(1);
            return tag + "[" + mContains.group(2) + "*='" + escapeQuotes(mContains.group(3)) + "']";
        }
        return null;
    }

    private static LocatorSpec parseLocator(By locator) {
        String s = locator.toString();
        Matcher m = BY_TOSTRING.matcher(s);
        if (!m.matches()) return new LocatorSpec("css", s);
        String method = m.group(1).trim().toLowerCase(Locale.ROOT);
        String value = m.group(2).trim();

        if (method.contains("xpath")) {
            return new LocatorSpec("xpath", value);
        } else if (method.contains("linktext") && !method.contains("partial")) {
            return new LocatorSpec("linkText", value);
        } else if (method.contains("partiallinktext")) {
            return new LocatorSpec("partialLinkText", value);
        } else {
            String css = toCssSelector(locator);
            return new LocatorSpec("css", css != null ? css : value);
        }
    }

    record LocatorSpec(String strategy, String query) {}
}
