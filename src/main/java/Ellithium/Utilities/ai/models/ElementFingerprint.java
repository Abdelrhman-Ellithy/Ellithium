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
     * Computes a similarity score (0.0 to 1.0) between this stored baseline
     * and a live candidate WebElement.
     *
     * <p>Weighted scoring:</p>
     * <ul>
     *   <li>id match: 30 points</li>
     *   <li>name match: 20 points</li>
     *   <li>aria-label match: 15 points</li>
     *   <li>text match (fuzzy): 15 points</li>
     *   <li>type match: 10 points</li>
     *   <li>tagName match: 5 points</li>
     *   <li>className partial overlap: 5 points</li>
     * </ul>
     * <p>Total possible = 100. Returned as 0.0–1.0 by dividing by 100.</p>
     *
     * @param candidate A live WebElement from the current DOM
     * @return Similarity score between 0.0 and 1.0
     */
    public double scoreSimilarity(WebElement candidate) {
        int score = 0;

        // id: 30 points
        if (isNonBlank(this.id) && this.id.equals(safeGetAttribute(candidate, "id"))) {
            score += 30;
        }

        // name: 20 points
        if (isNonBlank(this.name) && this.name.equals(safeGetAttribute(candidate, "name"))) {
            score += 20;
        }

        // aria-label: 15 points
        if (isNonBlank(this.ariaLabel) && this.ariaLabel.equals(safeGetAttribute(candidate, "aria-label"))) {
            score += 15;
        }

        // text: 15 points (fuzzy — contains match)
        if (isNonBlank(this.text)) {
            String candidateText = safeGetText(candidate);
            if (isNonBlank(candidateText)) {
                if (this.text.equals(candidateText)) {
                    score += 15;
                } else if (candidateText.contains(this.text) || this.text.contains(candidateText)) {
                    score += 10; // partial text match
                }
            }
        }

        // type: 10 points
        if (isNonBlank(this.type) && this.type.equals(safeGetAttribute(candidate, "type"))) {
            score += 10;
        }

        // tagName: 5 points
        if (isNonBlank(this.tagName) && this.tagName.equalsIgnoreCase(safeGetTag(candidate))) {
            score += 5;
        }

        // className partial overlap: 5 points
        if (isNonBlank(this.className)) {
            String candidateClass = safeGetAttribute(candidate, "class");
            if (isNonBlank(candidateClass)) {
                // Check if any CSS class overlaps
                String[] baselineClasses = this.className.split("\\s+");
                String[] candidateClasses = candidateClass.split("\\s+");
                for (String bc : baselineClasses) {
                    for (String cc : candidateClasses) {
                        if (bc.equals(cc) && !bc.isEmpty()) {
                            score += 5;
                            // Only award once
                            break;
                        }
                    }
                    if (score % 5 == 0 && score > 0) break; // already awarded
                }
            }
        }

        return score / 100.0;
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
