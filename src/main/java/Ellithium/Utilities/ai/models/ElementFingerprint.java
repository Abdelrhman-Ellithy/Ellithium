package Ellithium.Utilities.ai.models;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Captures a "fingerprint" snapshot of a successfully-found WebElement.
 *
 * <p>When an element is found successfully, its fingerprint is stored as a baseline.
 * When a locator breaks later, the stored fingerprint is compared against current
 * DOM candidates using weighted attribute scoring — enabling deterministic,
 * LLM-free healing for the majority of locator failures.</p>
 *
 * <p>Compatible with both Selenium WebDriver and Appium (Android/iOS).
 * All attributes use standard WebElement API. Parent tag extraction via
 * JavascriptExecutor is attempted gracefully and skipped on unsupported drivers.</p>
 */
public class ElementFingerprint {

    // ── Identity ──
    private String locatorKey;           // Original By.toString(), e.g. "By.id: user" (the broken one)
    private String healedLocatorKey;     // Reconstructed best locator, e.g. "By.id: username" (the real one)

    // ── Core Attributes (always available on Selenium + Appium) ──
    private String tagName;          // "input", "button", "a", "select"
    private String id;               // element.getAttribute("id")
    private String name;             // element.getAttribute("name")
    private String type;             // element.getAttribute("type")
    private String className;        // element.getAttribute("class")
    private String text;             // element.getText() — truncated to 100 chars
    private String ariaLabel;        // element.getAttribute("aria-label")
    private String placeholder;      // element.getAttribute("placeholder")
    private String href;             // element.getAttribute("href")
    private String value;            // element.getAttribute("value")
    private String role;             // element.getAttribute("role")
    private String dataTestId;       // element.getAttribute("data-testid")

    // ── Structural (JS-dependent, gracefully skipped for Appium) ──
    private String parentTag;        // parent element's tag name
    private int childIndex;          // index among siblings

    // ── Metadata ──
    private long lastSeenEpoch;      // System.currentTimeMillis()
    private String pageUrl;          // driver.getCurrentUrl()

    /** No-arg constructor for Gson deserialization. */
    public ElementFingerprint() {}

    /**
     * Captures a fingerprint from a live WebElement.
     *
     * @param driver     The WebDriver (Selenium or Appium)
     * @param locator    The By locator that was used to find the element
     * @param element    The successfully-found WebElement
     * @return A fully-populated ElementFingerprint
     */
    public static ElementFingerprint capture(WebDriver driver, By locator, WebElement element) {
        // Suppress the Selenium event listener to avoid flooding the test report
        // with 13+ attribute reads per element during fingerprint capture
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            return captureInternal(driver, locator, element);
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
    }

    private static ElementFingerprint captureInternal(WebDriver driver, By locator, WebElement element) {
        ElementFingerprint fp = new ElementFingerprint();
        fp.locatorKey = locator.toString();
        fp.lastSeenEpoch = System.currentTimeMillis();

        // Page URL — safe for both Selenium and Appium
        try {
            fp.pageUrl = driver.getCurrentUrl();
        } catch (Exception ignored) {
            fp.pageUrl = null;
        }

        // Core attributes via standard WebElement API (works on all drivers)
        fp.tagName = safeGetTag(element);
        fp.id = safeGetAttribute(element, "id");
        fp.name = safeGetAttribute(element, "name");
        fp.type = safeGetAttribute(element, "type");
        fp.className = safeGetAttribute(element, "class");
        fp.ariaLabel = safeGetAttribute(element, "aria-label");
        fp.placeholder = safeGetAttribute(element, "placeholder");
        fp.href = safeGetAttribute(element, "href");
        fp.value = safeGetAttribute(element, "value");
        fp.role = safeGetAttribute(element, "role");
        fp.dataTestId = safeGetAttribute(element, "data-testid");

        // Reconstruct the healed locator from the element's actual attributes
        // so the baseline JSON shows what the element was healed TO
        By reconstructed = reconstructLocator(element);
        fp.healedLocatorKey = (reconstructed != null) ? reconstructed.toString() : null;

        // Text — truncated to 100 chars to avoid bloating the baseline file
        try {
            String rawText = element.getText();
            fp.text = (rawText != null && rawText.length() > 100)
                    ? rawText.substring(0, 100) : rawText;
        } catch (Exception ignored) {
            fp.text = null;
        }

        // Structural: parent tag + child index via JS (gracefully skipped if unavailable)
        fp.parentTag = null;
        fp.childIndex = -1;
        if (driver instanceof JavascriptExecutor) {
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                Object parentTagResult = js.executeScript(
                        "return arguments[0].parentElement ? arguments[0].parentElement.tagName.toLowerCase() : null;",
                        element);
                if (parentTagResult != null) {
                    fp.parentTag = parentTagResult.toString();
                }
                Object childIndexResult = js.executeScript(
                        "var el = arguments[0]; var parent = el.parentElement;"
                                + "if (!parent) return -1;"
                                + "return Array.from(parent.children).indexOf(el);",
                        element);
                if (childIndexResult instanceof Number) {
                    fp.childIndex = ((Number) childIndexResult).intValue();
                }
            } catch (Exception ignored) {
                // JS not supported (some Appium contexts) — skip gracefully
            }
        }

        return fp;
    }

    /**
     * Computes a similarity score (0.0–1.0) between this stored baseline and a live candidate.
     *
     * <p>Dynamic scoring — only attributes that are non-null in the STORED baseline
     * contribute to the denominator, so a fingerprint without data-testid is not
     * penalized by a 30-point gap. Scoring table:</p>
     * <ul>
     *   <li>data-testid exact: 30 pts</li>
     *   <li>id exact: 25 pts | token-Jaccard ≥ 0.5: 12 pts</li>
     *   <li>name exact: 20 pts | token-Jaccard ≥ 0.5: 10 pts</li>
     *   <li>aria-label exact: 15 pts | contains: 8 pts</li>
     *   <li>placeholder exact: 15 pts | contains: 8 pts</li>
     *   <li>text exact: 12 pts | partial: 7 pts</li>
     *   <li>type exact: 8 pts</li>
     *   <li>role exact: 5 pts</li>
     *   <li>tagName exact: 5 pts</li>
     *   <li>className Jaccard ≥ 0.40: 5 pts</li>
     *   <li>parentTag structural: 3 pts</li>
     *   <li>childIndex structural: 2 pts</li>
     * </ul>
     *
     * @param candidate A live WebElement from the current DOM
     * @return Similarity score between 0.0 and 1.0
     */
    public double scoreSimilarity(WebElement candidate) {
        int score = 0;
        int dynamicMax = 0;

        // data-testid: 30 pts (most stable test attribute)
        if (isNonBlank(this.dataTestId)) {
            dynamicMax += 30;
            if (this.dataTestId.equals(safeGetAttribute(candidate, "data-testid"))) score += 30;
        }

        // id: 25 pts exact, 12 pts token-Jaccard ≥ 0.5
        if (isNonBlank(this.id)) {
            dynamicMax += 25;
            String cid = safeGetAttribute(candidate, "id");
            if (this.id.equals(cid)) {
                score += 25;
            } else if (isNonBlank(cid)) {
                double j = tokenJaccard(this.id, cid);
                if (j >= 0.5) score += 12;
            }
        }

        // name: 20 pts exact, 10 pts token-Jaccard ≥ 0.5
        if (isNonBlank(this.name)) {
            dynamicMax += 20;
            String cname = safeGetAttribute(candidate, "name");
            if (this.name.equals(cname)) {
                score += 20;
            } else if (isNonBlank(cname)) {
                double j = tokenJaccard(this.name, cname);
                if (j >= 0.5) score += 10;
            }
        }

        // aria-label: 15 pts exact, 8 pts contains
        if (isNonBlank(this.ariaLabel)) {
            dynamicMax += 15;
            String cal = safeGetAttribute(candidate, "aria-label");
            if (this.ariaLabel.equals(cal)) {
                score += 15;
            } else if (isNonBlank(cal) &&
                    (cal.contains(this.ariaLabel) || this.ariaLabel.contains(cal))) {
                score += 8;
            }
        }

        // placeholder: 15 pts exact, 8 pts contains
        if (isNonBlank(this.placeholder)) {
            dynamicMax += 15;
            String cph = safeGetAttribute(candidate, "placeholder");
            if (this.placeholder.equals(cph)) {
                score += 15;
            } else if (isNonBlank(cph) &&
                    (cph.contains(this.placeholder) || this.placeholder.contains(cph))) {
                score += 8;
            }
        }

        // text: 12 pts exact, 7 pts partial
        if (isNonBlank(this.text)) {
            dynamicMax += 12;
            String ct = safeGetText(candidate);
            if (isNonBlank(ct)) {
                if (this.text.equals(ct)) {
                    score += 12;
                } else if (ct.contains(this.text) || this.text.contains(ct)) {
                    score += 7;
                }
            }
        }

        // type: 8 pts
        if (isNonBlank(this.type)) {
            dynamicMax += 8;
            if (this.type.equals(safeGetAttribute(candidate, "type"))) score += 8;
        }

        // role: 5 pts
        if (isNonBlank(this.role)) {
            dynamicMax += 5;
            if (this.role.equals(safeGetAttribute(candidate, "role"))) score += 5;
        }

        // tagName: 5 pts
        if (isNonBlank(this.tagName)) {
            dynamicMax += 5;
            if (this.tagName.equalsIgnoreCase(safeGetTag(candidate))) score += 5;
        }

        // className Jaccard ≥ 0.40: 5 pts (fixed — no more break-logic bug)
        if (isNonBlank(this.className)) {
            dynamicMax += 5;
            String ccls = safeGetAttribute(candidate, "class");
            if (isNonBlank(ccls) && classJaccard(this.className, ccls) >= 0.40) {
                score += 5;
            }
        }

        // parentTag structural: 3 pts
        if (isNonBlank(this.parentTag)) {
            dynamicMax += 3;
            // parentTag is not queryable from WebElement directly; skip if unavailable
            // (only BaselineStore can enrich this via JS; leave as bonus when both known)
        }

        // childIndex structural: 2 pts
        if (this.childIndex >= 0) {
            dynamicMax += 2;
            // Same note — enriched by BaselineStore JS execution, not queryable here
        }

        if (dynamicMax == 0) return 0.0;
        return Math.min(1.0, score / (double) dynamicMax);
    }

    /**
     * Token-level Jaccard similarity between two identifier strings.
     * Splits on camelCase, hyphen, underscore — then computes |intersection|/|union|.
     * "login-btn" vs "loginBtn" → tokens {"login","btn"} vs {"login","btn"} → 1.0
     */
    private static double tokenJaccard(String a, String b) {
        java.util.Set<String> ta = tokenSet(a);
        java.util.Set<String> tb = tokenSet(b);
        if (ta.isEmpty() && tb.isEmpty()) return 1.0;
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        java.util.Set<String> intersection = new java.util.HashSet<>(ta);
        intersection.retainAll(tb);
        java.util.Set<String> union = new java.util.HashSet<>(ta);
        union.addAll(tb);
        return intersection.size() / (double) union.size();
    }

    private static java.util.Set<String> tokenSet(String s) {
        java.util.Set<String> tokens = new java.util.HashSet<>();
        for (String p : s.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|[-_\\s]+")) {
            String t = p.strip().toLowerCase();
            if (!t.isEmpty()) tokens.add(t);
        }
        return tokens;
    }

    /**
     * CSS class set Jaccard similarity.
     * Splits both class strings on whitespace and computes |intersection|/|union|.
     */
    private static double classJaccard(String a, String b) {
        java.util.Set<String> sa = new java.util.HashSet<>(java.util.Arrays.asList(a.split("\\s+")));
        java.util.Set<String> sb = new java.util.HashSet<>(java.util.Arrays.asList(b.split("\\s+")));
        sa.remove(""); sb.remove("");
        if (sa.isEmpty() && sb.isEmpty()) return 1.0;
        if (sa.isEmpty() || sb.isEmpty()) return 0.0;
        java.util.Set<String> intersection = new java.util.HashSet<>(sa);
        intersection.retainAll(sb);
        java.util.Set<String> union = new java.util.HashSet<>(sa);
        union.addAll(sb);
        return intersection.size() / (double) union.size();
    }

    // ──────────────────── Utility Methods ────────────────────

    private static String safeGetAttribute(WebElement element, String attribute) {
        try {
            String val = element.getAttribute(attribute);
            return (val != null && !val.isEmpty()) ? val : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeGetTag(WebElement element) {
        try {
            return element.getTagName().toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeGetText(WebElement element) {
        try {
            String t = element.getText();
            return (t != null && t.length() > 100) ? t.substring(0, 100) : t;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Reconstructs the best possible By locator for a given WebElement.
     * Priority: id > name > data-testid > css class > xpath.
     *
     * @param element The element to build a locator for
     * @return A By locator that can reach this element
     */
    public static By reconstructLocator(WebElement element) {
        String id = safeGetAttribute(element, "id");
        if (isNonBlank(id)) return By.id(id);

        String name = safeGetAttribute(element, "name");
        if (isNonBlank(name)) return By.name(name);

        String dataTestId = safeGetAttribute(element, "data-testid");
        if (isNonBlank(dataTestId)) return By.cssSelector("[data-testid='" + dataTestId + "']");

        String ariaLabel = safeGetAttribute(element, "aria-label");
        if (isNonBlank(ariaLabel)) return By.cssSelector("[aria-label='" + ariaLabel + "']");

        // Fallback: tag + class combination
        String tag = safeGetTag(element);
        String cls = safeGetAttribute(element, "class");
        if (isNonBlank(tag) && isNonBlank(cls)) {
            String firstClass = cls.split("\\s+")[0];
            return By.cssSelector(tag + "." + firstClass);
        }

        // Last resort: tag name only
        if (isNonBlank(tag)) return By.tagName(tag);

        return null;
    }

    // ──────────────────── Getters ────────────────────

    public String getLocatorKey() { return locatorKey; }
    public String getHealedLocatorKey() { return healedLocatorKey; }
    public String getTagName() { return tagName; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getClassName() { return className; }
    public String getText() { return text; }
    public String getAriaLabel() { return ariaLabel; }
    public String getPlaceholder() { return placeholder; }
    public String getHref() { return href; }
    public String getValue() { return value; }
    public String getRole() { return role; }
    public String getDataTestId() { return dataTestId; }
    public String getParentTag() { return parentTag; }
    public int getChildIndex() { return childIndex; }
    public long getLastSeenEpoch() { return lastSeenEpoch; }
    public String getPageUrl() { return pageUrl; }

    @Override
    public String toString() {
        return "ElementFingerprint{" +
                "locatorKey='" + locatorKey + '\'' +
                ", healedLocatorKey='" + healedLocatorKey + '\'' +
                ", tag='" + tagName + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ariaLabel='" + ariaLabel + '\'' +
                ", text='" + (text != null ? text.substring(0, Math.min(30, text.length())) : "null") + '\'' +
                '}';
    }
}
