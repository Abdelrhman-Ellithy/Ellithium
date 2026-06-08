package Ellithium.core.ai.models;

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

    /** Max characters of element text retained in a fingerprint (was 100; raised for richer signal). */
    private static final int TEXT_CAP = 240;
    /** Max length of either side for the partial-contains text bonus to apply. */
    private static final int TEXT_PARTIAL_CAP = 50;

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
    private String dataTest;         // element.getAttribute("data-test")   (Cypress alternative)
    private String dataCy;           // element.getAttribute("data-cy")     (Cypress convention)
    private String dataQa;           // element.getAttribute("data-qa")     (QA convention)
    private String title;            // element.getAttribute("title")
    private String label;            // element.getAttribute("label")

    // ── Mobile primaries (Appium-only; null on web) ──
    private String resourceId;       // Android resource-id (e.g. "com.app:id/loginBtn")
    private String accessibilityId;  // Appium accessibility id (Android content-desc / iOS accessibility-id)
    private String contentDesc;      // Android content-desc (also used as accessibility-id source)

    // ── Adaptive data-* attributes (JS-dependent) ──
    // Captures every data-* attribute the element has, beyond the named ones above.
    // Keyed by attribute name; value is the attribute value. Null when absent or on Appium native.
    private java.util.Map<String, String> customDataAttrs;

    // ── Structural (JS-dependent, gracefully skipped for Appium) ──
    private String parentTag;        // parent element's tag name
    private int childIndex;          // index among siblings
    private String prevSiblingTag;   // previous element sibling's tag name
    private String nextSiblingTag;   // next element sibling's tag name

    // ── Metadata ──
    private long lastSeenEpoch;      // System.currentTimeMillis()
    private String pageUrl;          // driver.getCurrentUrl()

    // ── iframe context ──
    // XPath selectors (or index strings) of the iframe chain from the top document to this element,
    // innermost last. Null/empty means the element lives in the top document.
    // Example: ["iframe#authFrame", "iframe:nth-of-type(2)"]
    private java.util.List<String> iframeChain;

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
        fp.dataTest   = safeGetAttribute(element, "data-test");
        fp.dataCy     = safeGetAttribute(element, "data-cy");
        fp.dataQa     = safeGetAttribute(element, "data-qa");
        // title and label are captured inside the structural JS call below (0 extra RTs)
        fp.resourceId      = safeGetAttribute(element, "resource-id");
        fp.accessibilityId = safeGetAttribute(element, "accessibility-id");
        fp.contentDesc     = safeGetAttribute(element, "content-desc");
        if (!isNonBlank(fp.accessibilityId) && isNonBlank(fp.contentDesc)) {
            fp.accessibilityId = fp.contentDesc;
        }

        // Reconstruct the healed locator from the element's actual attributes
        // so the baseline JSON shows what the element was healed TO
        By reconstructed = reconstructLocator(element);
        fp.healedLocatorKey = (reconstructed != null) ? reconstructed.toString() : null;

        // Text — truncated to 240 chars (was 100) so longer titles/messages survive for scoring
        try {
            String rawText = element.getText();
            fp.text = (rawText != null && rawText.length() > TEXT_CAP)
                    ? rawText.substring(0, TEXT_CAP) : rawText;
        } catch (Exception ignored) {
            fp.text = null;
        }

        // Structural + adaptive data-* attrs — ONE JS call, zero extra round-trips.
        fp.parentTag = null;
        fp.childIndex = -1;
        fp.prevSiblingTag = null;
        fp.nextSiblingTag = null;
        fp.customDataAttrs = null;
        if (driver instanceof JavascriptExecutor) {
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                Object result = js.executeScript(
                        "var el=arguments[0], p=el.parentElement;"
                        + "var prev=el.previousElementSibling, next=el.nextElementSibling;"
                        + "var dm={}, at=el.attributes;"
                        + "for(var k=0;k<at.length;k++){"
                        + " var n=at[k].name, v=at[k].value;"
                        + " if(n.indexOf('data-')===0 && n!=='data-ellithium-pick' && v) dm[n]=v;"
                        + "}"
                        + "var ifc=[];"
                        + "try{ var w=window;"
                        + "  while(w!==w.top){"
                        + "    var fe=w.frameElement; if(!fe)break;"
                        + "    var fid=fe.getAttribute('id')||fe.getAttribute('name');"
                        + "    var sibs=fe.parentElement?Array.prototype.slice.call(fe.parentElement.querySelectorAll('iframe,frame')):[];"
                        + "    ifc.unshift(fid?fid:(':'+sibs.indexOf(fe)));"
                        + "    w=w.parent;"
                        + "  }"
                        + "}catch(e){}"
                        + "return [p?p.tagName.toLowerCase():null,"
                        + " p?Array.prototype.indexOf.call(p.children,el):-1,"
                        + " prev?prev.tagName.toLowerCase():null,"
                        + " next?next.tagName.toLowerCase():null,"
                        + " dm, ifc,"
                        + " el.getAttribute('title'), el.getAttribute('label')];",
                        element);
                if (result instanceof java.util.List<?> r && r.size() >= 4) {
                    if (r.get(0) != null) fp.parentTag = r.get(0).toString();
                    if (r.get(1) instanceof Number n) fp.childIndex = n.intValue();
                    if (r.get(2) != null) fp.prevSiblingTag = r.get(2).toString();
                    if (r.get(3) != null) fp.nextSiblingTag = r.get(3).toString();
                    if (r.size() >= 5 && r.get(4) instanceof java.util.Map<?, ?> dm && !dm.isEmpty()) {
                        java.util.Map<String, String> custom = new java.util.LinkedHashMap<>();
                        for (java.util.Map.Entry<?, ?> e : dm.entrySet()) {
                            if (e.getKey() == null || e.getValue() == null) continue;
                            String an = e.getKey().toString();
                            String av = e.getValue().toString();
                            if (av.isBlank()) continue;
                            if ("data-testid".equals(an) || "data-test".equals(an)
                                    || "data-cy".equals(an) || "data-qa".equals(an)) continue;
                            custom.put(an, av);
                        }
                        if (!custom.isEmpty()) fp.customDataAttrs = custom;
                    }
                    if (r.size() >= 6 && r.get(5) instanceof java.util.List<?> ifc && !ifc.isEmpty()) {
                        java.util.List<String> chain = new java.util.ArrayList<>();
                        for (Object item : ifc) if (item != null) chain.add(item.toString());
                        if (!chain.isEmpty()) fp.iframeChain = java.util.List.copyOf(chain);
                    }
                    // title and label piggyback on this single JS call — 0 extra WebDriver RTs
                    if (r.size() >= 7 && r.get(6) instanceof String t && !t.isBlank()) fp.title = t;
                    if (r.size() >= 8 && r.get(7) instanceof String l && !l.isBlank()) fp.label = l;
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
        return scoreSimilarity(candidate, null);
    }

    /**
     * Lightweight structural snapshot of a candidate (parent tag, sibling tags, child index),
     * gathered by the caller via JavascriptExecutor so {@link #scoreSimilarity(WebElement, StructuralContext)}
     * can reward parent/sibling/position matches — a strong, layout-stable signal that survives
     * id/class churn.
     */
    public static class StructuralContext {
        public final String parentTag;
        public final int childIndex;
        public final String prevSiblingTag;
        public final String nextSiblingTag;
        public StructuralContext(String parentTag, int childIndex,
                                 String prevSiblingTag, String nextSiblingTag) {
            this.parentTag = parentTag;
            this.childIndex = childIndex;
            this.prevSiblingTag = prevSiblingTag;
            this.nextSiblingTag = nextSiblingTag;
        }
    }

    /**
     * Similarity scoring with optional structural context. When {@code sc} is non-null, parent tag,
     * child index, and previous/next sibling tags contribute to BOTH numerator and denominator —
     * so a candidate sitting in the same DOM position as the baseline is boosted, and one in a
     * different position is discriminated against. When {@code sc} is null (no driver available),
     * structural terms are omitted entirely so a full attribute match still normalises to 1.0.
     */
    public double scoreSimilarity(WebElement candidate, StructuralContext sc) {
        int score = 0;
        int dynamicMax = 0;

        // resource-id (Appium): 30 pts exact, 12 pts suffix-match (com.app:id/btn vs ":id/btn")
        if (isNonBlank(this.resourceId)) {
            dynamicMax += 30;
            String crid = safeGetAttribute(candidate, "resource-id");
            if (this.resourceId.equals(crid)) score += 30;
            else if (isNonBlank(crid) && (crid.endsWith(suffixAfter(this.resourceId, '/'))
                    || this.resourceId.endsWith(suffixAfter(crid, '/')))) score += 12;
        }

        // accessibility-id (Appium): 28 pts exact
        if (isNonBlank(this.accessibilityId)) {
            dynamicMax += 28;
            String caid = safeGetAttribute(candidate, "accessibility-id");
            if (caid == null || caid.isBlank()) caid = safeGetAttribute(candidate, "content-desc");
            if (this.accessibilityId.equals(caid)) score += 28;
        }

        // content-desc (Android): 18 pts exact, 9 pts contains
        if (isNonBlank(this.contentDesc)) {
            dynamicMax += 18;
            String ccd = safeGetAttribute(candidate, "content-desc");
            if (this.contentDesc.equals(ccd)) score += 18;
            else if (isNonBlank(ccd) && (ccd.contains(this.contentDesc) || this.contentDesc.contains(ccd))) score += 9;
        }

        // data-testid / data-test / data-cy / data-qa: 30 pts each (stable test attributes)
        if (isNonBlank(this.dataTestId)) {
            dynamicMax += 30;
            if (this.dataTestId.equals(safeGetAttribute(candidate, "data-testid"))) score += 30;
        }
        if (isNonBlank(this.dataTest)) {
            dynamicMax += 30;
            if (this.dataTest.equals(safeGetAttribute(candidate, "data-test"))) score += 30;
        }
        if (isNonBlank(this.dataCy)) {
            dynamicMax += 30;
            if (this.dataCy.equals(safeGetAttribute(candidate, "data-cy"))) score += 30;
        }
        if (isNonBlank(this.dataQa)) {
            dynamicMax += 30;
            if (this.dataQa.equals(safeGetAttribute(candidate, "data-qa"))) score += 30;
        }
        // Adaptive data-* attributes: 30 pts each (any attribute the app uses beyond the named four)
        if (this.customDataAttrs != null) {
            for (java.util.Map.Entry<String, String> e : this.customDataAttrs.entrySet()) {
                dynamicMax += 30;
                if (e.getValue().equals(safeGetAttribute(candidate, e.getKey()))) score += 30;
            }
        }

        // id: 25 pts exact, 12 pts token-Jaccard ≥ 0.5
        if (isNonBlank(this.id)) {
            dynamicMax += 25;
            String cid = safeGetAttribute(candidate, "id");
            if (this.id.equals(cid)) {
                score += 25;
            } else if (fuzzyIdMatch(idTokens(), this.id, cid)) {
                score += 12;
            }
        }

        // name: 20 pts exact, 10 pts fuzzy (token-Jaccard ≥ 0.5 OR edit-ratio ≥ 0.82)
        if (isNonBlank(this.name)) {
            dynamicMax += 20;
            String cname = safeGetAttribute(candidate, "name");
            if (this.name.equals(cname)) {
                score += 20;
            } else if (fuzzyIdMatch(nameTokens(), this.name, cname)) {
                score += 10;
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

        // text: 12 pts exact, 7 pts partial (short strings only)
        if (isNonBlank(this.text)) {
            dynamicMax += 12;
            String ct = safeGetText(candidate);
            if (isNonBlank(ct)) {
                if (this.text.equals(ct)) {
                    score += 12;
                } else if (this.text.length() <= TEXT_PARTIAL_CAP && ct.length() <= TEXT_PARTIAL_CAP
                        && (ct.contains(this.text) || this.text.contains(ct))) {
                    score += 7;
                }
            }
        }

        // href: 20 pts exact, 10 pts contains (strong discriminator for anchors)
        if (isNonBlank(this.href)) {
            dynamicMax += 20;
            String ch = safeGetAttribute(candidate, "href");
            if (this.href.equals(ch)) score += 20;
            else if (isNonBlank(ch) && (ch.contains(this.href) || this.href.contains(ch))) score += 10;
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
            if (isNonBlank(ccls) && classJaccard(this.classTokens(), ccls) >= 0.40) {
                score += 5;
            }
        }

        // ── Structural scoring (only when the caller supplied a StructuralContext) ──

        if (sc != null) {
            if (isNonBlank(this.parentTag)) {
                dynamicMax += 3;
                if (this.parentTag.equalsIgnoreCase(sc.parentTag)) score += 3;
            }
            if (this.childIndex >= 0 && sc.childIndex >= 0) {
                dynamicMax += 2;
                if (this.childIndex == sc.childIndex) score += 2;
            }
            if (isNonBlank(this.prevSiblingTag)) {
                dynamicMax += 2;
                if (this.prevSiblingTag.equalsIgnoreCase(sc.prevSiblingTag)) score += 2;
            }
            if (isNonBlank(this.nextSiblingTag)) {
                dynamicMax += 2;
                if (this.nextSiblingTag.equalsIgnoreCase(sc.nextSiblingTag)) score += 2;
            }
        }

        if (dynamicMax == 0) return 0.0;
        return Math.min(1.0, score / (double) dynamicMax);
    }

    /**
     * Map-based similarity scoring — zero WebDriver round-trips. The {@code attrs} map is the
     * pre-fetched, batched per-candidate projection produced once by EnsembleHealer's
     * fetchCandidateAttributes JS. Uses the same field weights as {@link #scoreSimilarity(WebElement)};
     * structural fields are omitted (no DOM walk available from a plain map).
     */
    public double scoreSimilarity(java.util.Map<String, Object> attrs) {
        return scoreSimilarity(attrs, null);
    }

    public double scoreSimilarity(java.util.Map<String, Object> attrs, StructuralContext sc) {
        if (attrs == null) return 0.0;
        int score = 0;
        int dynamicMax = 0;

        if (isNonBlank(this.resourceId)) {
            dynamicMax += 30;
            String crid = asStr(attrs.get("resource-id"));
            if (this.resourceId.equals(crid)) score += 30;
            else if (isNonBlank(crid) && (crid.endsWith(suffixAfter(this.resourceId, '/'))
                    || this.resourceId.endsWith(suffixAfter(crid, '/')))) score += 12;
        }
        if (isNonBlank(this.accessibilityId)) {
            dynamicMax += 28;
            String caid = asStr(attrs.get("accessibility-id"));
            if (caid == null || caid.isBlank()) caid = asStr(attrs.get("content-desc"));
            if (this.accessibilityId.equals(caid)) score += 28;
        }
        if (isNonBlank(this.contentDesc)) {
            dynamicMax += 18;
            String ccd = asStr(attrs.get("content-desc"));
            if (this.contentDesc.equals(ccd)) score += 18;
            else if (isNonBlank(ccd) && (ccd.contains(this.contentDesc) || this.contentDesc.contains(ccd))) score += 9;
        }
        if (isNonBlank(this.dataTestId)) {
            dynamicMax += 30;
            if (this.dataTestId.equals(asStr(attrs.get("data-testid")))) score += 30;
        }
        if (isNonBlank(this.dataTest)) {
            dynamicMax += 30;
            if (this.dataTest.equals(asStr(attrs.get("data-test")))) score += 30;
        }
        if (isNonBlank(this.dataCy)) {
            dynamicMax += 30;
            if (this.dataCy.equals(asStr(attrs.get("data-cy")))) score += 30;
        }
        if (isNonBlank(this.dataQa)) {
            dynamicMax += 30;
            if (this.dataQa.equals(asStr(attrs.get("data-qa")))) score += 30;
        }
        if (this.customDataAttrs != null) {
            Object dm = attrs.get("dataAttrs");
            for (java.util.Map.Entry<String, String> e : this.customDataAttrs.entrySet()) {
                dynamicMax += 30;
                String cv = (dm instanceof java.util.Map<?, ?> dmap) ? asStr(dmap.get(e.getKey())) : null;
                if (e.getValue().equals(cv)) score += 30;
            }
        }
        if (isNonBlank(this.id)) {
            dynamicMax += 25;
            String cid = asStr(attrs.get("id"));
            if (this.id.equals(cid)) {
                score += 25;
            } else if (fuzzyIdMatch(idTokens(), this.id, cid)) {
                score += 12;
            }
        }
        if (isNonBlank(this.name)) {
            dynamicMax += 20;
            String cname = asStr(attrs.get("name"));
            if (this.name.equals(cname)) {
                score += 20;
            } else if (fuzzyIdMatch(nameTokens(), this.name, cname)) {
                score += 10;
            }
        }
        if (isNonBlank(this.ariaLabel)) {
            dynamicMax += 15;
            String cal = asStr(attrs.get("aria-label"));
            if (this.ariaLabel.equals(cal)) {
                score += 15;
            } else if (isNonBlank(cal) &&
                    (cal.contains(this.ariaLabel) || this.ariaLabel.contains(cal))) {
                score += 8;
            }
        }
        if (isNonBlank(this.placeholder)) {
            dynamicMax += 15;
            String cph = asStr(attrs.get("placeholder"));
            if (this.placeholder.equals(cph)) {
                score += 15;
            } else if (isNonBlank(cph) &&
                    (cph.contains(this.placeholder) || this.placeholder.contains(cph))) {
                score += 8;
            }
        }
        if (isNonBlank(this.text)) {
            dynamicMax += 12;
            String ct = asStr(attrs.get("text"));
            if (isNonBlank(ct)) {
                if (this.text.equals(ct)) {
                    score += 12;
                } else if (this.text.length() <= TEXT_PARTIAL_CAP && ct.length() <= TEXT_PARTIAL_CAP
                        && (ct.contains(this.text) || this.text.contains(ct))) {
                    score += 7;
                }
            }
        }
        if (isNonBlank(this.href)) {
            dynamicMax += 20;
            String ch = asStr(attrs.get("href"));
            if (this.href.equals(ch)) score += 20;
            else if (isNonBlank(ch) && (ch.contains(this.href) || this.href.contains(ch))) score += 10;
        }
        if (isNonBlank(this.type)) {
            dynamicMax += 8;
            if (this.type.equals(asStr(attrs.get("type")))) score += 8;
        }
        if (isNonBlank(this.role)) {
            dynamicMax += 5;
            if (this.role.equals(asStr(attrs.get("role")))) score += 5;
        }
        if (isNonBlank(this.tagName)) {
            dynamicMax += 5;
            String ctag = asStr(attrs.get("tag"));
            if (ctag != null && this.tagName.equalsIgnoreCase(ctag)) score += 5;
        }
        if (isNonBlank(this.className)) {
            dynamicMax += 5;
            String ccls = asStr(attrs.get("class"));
            if (isNonBlank(ccls) && classJaccard(this.classTokens(), ccls) >= 0.40) {
                score += 5;
            }
        }

        if (sc != null) {
            if (isNonBlank(this.parentTag)) {
                dynamicMax += 3;
                if (this.parentTag.equalsIgnoreCase(sc.parentTag)) score += 3;
            }
            if (this.childIndex >= 0 && sc.childIndex >= 0) {
                dynamicMax += 2;
                if (this.childIndex == sc.childIndex) score += 2;
            }
            if (isNonBlank(this.prevSiblingTag)) {
                dynamicMax += 2;
                if (this.prevSiblingTag.equalsIgnoreCase(sc.prevSiblingTag)) score += 2;
            }
            if (isNonBlank(this.nextSiblingTag)) {
                dynamicMax += 2;
                if (this.nextSiblingTag.equalsIgnoreCase(sc.nextSiblingTag)) score += 2;
            }
        }

        if (dynamicMax == 0) return 0.0;
        return Math.min(1.0, score / (double) dynamicMax);
    }

    private static String asStr(Object o) { return o != null ? o.toString() : null; }

    /**
     * Fuzzy identifier match for id/name drift. True when EITHER token-Jaccard ≥ 0.5 (rename across
     * naming conventions, e.g. {@code loginBtn}↔{@code login-btn}) OR normalized Levenshtein ratio
     * ≥ 0.82 (typo/char drift, e.g. {@code usrname}↔{@code username}, which token-Jaccard scores 0).
     */
    private static boolean fuzzyIdMatch(java.util.Set<String> selfTokens, String self, String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        if (jaccard(selfTokens, candidate) >= 0.5) return true;
        return levenshteinRatio(self.toLowerCase(), candidate.toLowerCase()) >= 0.82;
    }

    /** Similarity ratio in [0,1] = 1 - editDistance/maxLen. Bounded O(min·max); inputs are short ids. */
    private static double levenshteinRatio(String a, String b) {
        if (a == null || b == null) return 0.0;
        int la = a.length(), lb = b.length();
        if (la == 0 && lb == 0) return 1.0;
        if (la == 0 || lb == 0) return 0.0;
        int[] prev = new int[lb + 1];
        int[] cur  = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;
        for (int i = 1; i <= la; i++) {
            cur[0] = i;
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        int dist = prev[lb];
        return 1.0 - (double) dist / Math.max(la, lb);
    }

    /** Returns the substring after the last {@code sep}, or the input unchanged when absent.
     *  Used for Android resource-id suffix-match (e.g. {@code com.app:id/btn} → {@code btn}). */
    private static String suffixAfter(String s, char sep) {
        if (s == null) return "";
        int i = s.lastIndexOf(sep);
        return (i < 0) ? s : s.substring(i + 1);
    }

    // Precompiled once (was recompiled on every String.split call inside the per-candidate hot loop).
    private static final java.util.regex.Pattern TOKEN_SPLIT =
            java.util.regex.Pattern.compile("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|[-_\\s]+");
    private static final java.util.regex.Pattern WS_SPLIT = java.util.regex.Pattern.compile("\\s+");

    // Cached token sets for THIS baseline's id/name/class — constant across all candidates, so they
    // are computed once instead of N(candidates)×D(history) times. Transient: never serialized by Gson.
    private transient java.util.Set<String> idTokensCache;
    private transient java.util.Set<String> nameTokensCache;
    private transient java.util.Set<String> classTokensCache;

    private java.util.Set<String> idTokens() {
        if (idTokensCache == null) idTokensCache = tokenSet(this.id);
        return idTokensCache;
    }

    private java.util.Set<String> nameTokens() {
        if (nameTokensCache == null) nameTokensCache = tokenSet(this.name);
        return nameTokensCache;
    }

    private java.util.Set<String> classTokens() {
        if (classTokensCache == null) {
            classTokensCache = new java.util.HashSet<>();
            if (this.className != null) {
                for (String p : WS_SPLIT.split(this.className)) {
                    String t = p.strip();
                    if (!t.isEmpty()) classTokensCache.add(t);
                }
            }
        }
        return classTokensCache;
    }

    /**
     * Token-level Jaccard between this baseline's cached token set and a candidate string.
     * "login-btn" vs "loginBtn" → {"login","btn"} vs {"login","btn"} → 1.0
     */
    private static double jaccard(java.util.Set<String> ta, String b) {
        java.util.Set<String> tb = tokenSet(b);
        if (ta.isEmpty() && tb.isEmpty()) return 1.0;
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        int inter = 0;
        for (String t : ta) if (tb.contains(t)) inter++;
        return inter / (double) (ta.size() + tb.size() - inter);   // |∩| / |∪| without extra sets
    }

    private static java.util.Set<String> tokenSet(String s) {
        java.util.Set<String> tokens = new java.util.HashSet<>();
        if (s == null) return tokens;
        for (String p : TOKEN_SPLIT.split(s)) {
            String t = p.strip().toLowerCase();
            if (!t.isEmpty()) tokens.add(t);
        }
        return tokens;
    }

    /**
     * CSS class set Jaccard similarity. Baseline token set is pre-cached; only one HashSet is
     * allocated per call (for the candidate string), eliminating the 2-alloc hot-path cost.
     */
    private static double classJaccard(java.util.Set<String> baseline, String candidateClass) {
        java.util.Set<String> sb = new java.util.HashSet<>();
        for (String p : WS_SPLIT.split(candidateClass)) { String t = p.strip(); if (!t.isEmpty()) sb.add(t); }
        if (baseline.isEmpty() && sb.isEmpty()) return 1.0;
        if (baseline.isEmpty() || sb.isEmpty()) return 0.0;
        int inter = 0;
        for (String t : baseline) if (sb.contains(t)) inter++;
        return inter / (double) (baseline.size() + sb.size() - inter);
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

    /** Tag name with original case preserved — required for case-sensitive Appium widget classes. */
    private static String safeGetRawTag(WebElement element) {
        try {
            return element.getTagName();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeGetText(WebElement element) {
        try {
            String t = element.getText();
            return (t != null && t.length() > TEXT_CAP) ? t.substring(0, TEXT_CAP) : t;
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
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            String accId = safeGetAttribute(element, "accessibility-id");
            if (accId == null || accId.isBlank()) accId = safeGetAttribute(element, "content-desc");
            if (isNonBlank(accId)) return io.appium.java_client.AppiumBy.accessibilityId(accId);

            String resId = safeGetAttribute(element, "resource-id");
            if (isNonBlank(resId)) return io.appium.java_client.AppiumBy.id(resId);

            String id = safeGetAttribute(element, "id");
            if (isNonBlank(id)) return By.id(id);

            String name = safeGetAttribute(element, "name");
            if (isNonBlank(name)) return By.name(name);

            String dataTestId = safeGetAttribute(element, "data-testid");
            if (isNonBlank(dataTestId)) return By.cssSelector("[data-testid='" + dataTestId + "']");
            String dataTest = safeGetAttribute(element, "data-test");
            if (isNonBlank(dataTest)) return By.cssSelector("[data-test='" + dataTest + "']");
            String dataCy = safeGetAttribute(element, "data-cy");
            if (isNonBlank(dataCy)) return By.cssSelector("[data-cy='" + dataCy + "']");
            String dataQa = safeGetAttribute(element, "data-qa");
            if (isNonBlank(dataQa)) return By.cssSelector("[data-qa='" + dataQa + "']");

            String ariaLabel = safeGetAttribute(element, "aria-label");
            if (isNonBlank(ariaLabel)) return By.cssSelector("[aria-label='" + ariaLabel + "']");

            // Native widget classes are case-sensitive (android.widget.Button, XCUIElementTypeButton),
            // so read the RAW tag — never the lowercased web tag — and emit xpath-by-class
            // (valid on UiAutomator2/XCUITest; CSS is not supported there).
            String rawTag = safeGetRawTag(element);
            if (isNativeWidgetClass(rawTag)) return By.xpath("//" + rawTag);

            String tag = safeGetTag(element);
            String cls = safeGetAttribute(element, "class");
            if (isNonBlank(tag) && isNonBlank(cls)) {
                String firstClass = cls.split("\\s+")[0];
                if (isCssSafeIdentifier(firstClass)) return By.cssSelector(tag + "." + firstClass);
            }
            if (isNonBlank(tag)) return By.tagName(tag);
            return null;
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
    }

    /** Appium native widget class (android.widget.* / android.view.* / XCUIElementType*) — not a CSS tag. */
    static boolean isNativeWidgetClass(String tag) {
        if (!isNonBlank(tag)) return false;
        return tag.indexOf('.') >= 0 || tag.startsWith("XCUIElementType");
    }

    /** True when the token is a valid bare CSS class identifier (no :, /, ., spaces, etc.). */
    static boolean isCssSafeIdentifier(String token) {
        return isNonBlank(token) && token.matches("^-?[_a-zA-Z][_a-zA-Z0-9-]*$");
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
    public String getDataTest()   { return dataTest; }
    public String getDataCy()     { return dataCy; }
    public String getDataQa()     { return dataQa; }
    public String getTitle()      { return title; }
    public String getLabel()      { return label; }
    public java.util.Map<String, String> getCustomDataAttrs() { return customDataAttrs; }
    public String getResourceId() { return resourceId; }
    public String getAccessibilityId() { return accessibilityId; }
    public String getContentDesc() { return contentDesc; }

    /**
     * True when the baseline carries at least one strong-identity anchor (id / name / data-testid /
     * resource-id / accessibility-id). When false the fingerprint's only evidence is weak signals
     * (tag/class/role/text), so a match score normalises over a tiny denominator and a 0.60 is far
     * weaker proof than the same 0.60 backed by a stable id — callers should raise the accept bar.
     */
    public boolean hasStrongIdentity() {
        return isNonBlank(id) || isNonBlank(name) || isNonBlank(dataTestId)
                || isNonBlank(dataTest) || isNonBlank(dataCy) || isNonBlank(dataQa)
                || (customDataAttrs != null && !customDataAttrs.isEmpty())
                || isNonBlank(resourceId) || isNonBlank(accessibilityId);
    }
    public String getParentTag() { return parentTag; }
    public int getChildIndex() { return childIndex; }
    public String getPrevSiblingTag() { return prevSiblingTag; }
    public String getNextSiblingTag() { return nextSiblingTag; }
    public long getLastSeenEpoch() { return lastSeenEpoch; }
    public String getPageUrl() { return pageUrl; }
    public java.util.List<String> getIframeChain() {
        return iframeChain != null ? iframeChain : java.util.List.of();
    }
    public boolean isInsideIframe() { return iframeChain != null && !iframeChain.isEmpty(); }

    /**
     * Switches {@code driver} into the iframe chain recorded at capture time.
     * Returns {@code true} if any frame switch was made (caller must call
     * {@code driver.switchTo().defaultContent()} after the operation).
     * Returns {@code false} if the chain is empty (no switch needed, top frame).
     */
    public boolean enterIframeContext(org.openqa.selenium.WebDriver driver) {
        if (!isInsideIframe()) return false;
        try {
            for (String frame : iframeChain) {
                if (frame.startsWith(":")) {
                    int idx = Integer.parseInt(frame.substring(1));
                    driver.switchTo().frame(idx);
                } else {
                    driver.switchTo().frame(frame);
                }
            }
            return true;
        } catch (Exception e) {
            try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
            return false;
        }
    }

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
