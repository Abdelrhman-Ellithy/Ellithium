package Ellithium.core.ai.healing;

import Ellithium.core.ai.DriverProfile;
import Ellithium.core.ai.healing.SemanticLocatorResolver;
import Ellithium.core.ai.dom.CandidateAttributeBatcher;
import Ellithium.core.ai.locators.LocatorTechniques;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the strongest locator for an element a healing tier has already identified, using rich
 * construction techniques (attr-equals, text + attribute {@code and}-xpath, role+text, tag+attribute
 * combos, progressive multi-class CSS, ancestor-scoped {@code nth-of-type} path) — DOM-validated so
 * invalid candidates are dropped and the best valid one is kept.
 *
 * <p>Healing-owned and independent of {@code core.ai.codegen}; shares only the neutral
 * {@link LocatorTechniques} primitives.</p>
 *
 * <h3>Round-trip budget (NFR)</h3>
 * <ul>
 *   <li>Web/webview common case: <b>1</b> round-trip (batched attribute read) — a strong, stable
 *       identity (data-testid / id / name) is emitted immediately, no validation needed.</li>
 *   <li>Web/webview hard case: <b>2</b> round-trips — the batched read plus ONE validation script
 *       that checks ALL candidates (≤10) and computes the nth-of-type path in the same call.</li>
 *   <li>Mobile native: a fast path for resource-id / accessibility-id (no validation), else a
 *       hard-capped (≤4) early-exiting {@code findElements} probe.</li>
 * </ul>
 */
public final class HealedLocatorBuilder {

    private HealedLocatorBuilder() {}

    private static final int    MAX_CANDIDATES = 15;
    private static final int    MOBILE_PROBE_CAP = 4;
    private static final int    MAX_TEXT_LEN = 80;

    private static final double W_ARIA = 0.80, W_TEXT_ATTR = 0.79, W_TEXT = 0.78, W_ROLE_TEXT = 0.78,
            W_TAG_ATTR = 0.72, W_TAG_CLASS2 = 0.68, W_TAG_CLASS1 = 0.62, W_CLASS1 = 0.58, W_PATH = 0.10;

    /** A candidate locator plus the raw kind/selector needed to validate it in one JS call. */
    static final class Candidate {
        final By by; final String kind; final String sel; final String tier; final double weight;
        Candidate(By by, String kind, String sel, String tier, double weight) {
            this.by = by; this.kind = kind; this.sel = sel; this.tier = tier; this.weight = weight;
        }
    }

    /** 0 = invalid (no match / target absent), 1 = unique, n = target found among n. */
    @FunctionalInterface
    interface MatchProbe { int targetMatches(By by); }

    // ──────────────────────── Public API ────────────────────────

    /** Best locator for an already-identified element, or {@code null} (caller falls back). */
    public static By build(WebDriver driver, WebElement target,
                           Ellithium.core.ai.models.ElementFingerprint baseline) {
        if (driver == null || target == null) return null;
        try {
            if (DriverProfile.detect(driver).isNativeMobile()) {
                return buildMobileNative(driver, target);
            }
            return buildWeb(driver, target);
        } catch (Exception e) {
            return null;
        }
    }

    // ──────────────────────── Web / WebView ────────────────────────

    private static By buildWeb(WebDriver driver, WebElement target) {
        List<Map<String, Object>> batch = CandidateAttributeBatcher.fetch(driver, List.of(target));
        if (batch == null || batch.isEmpty() || batch.get(0) == null) return null;
        Map<String, Object> attrs = batch.get(0);

        By fast = fastPathLocator(attrs);   // 0 validation round-trips
        if (fast != null) return fast;

        List<Candidate> candidates = buildWebCandidates(attrs);
        if (candidates.isEmpty()) return null;

        if (!(driver instanceof JavascriptExecutor js)) return null;
        Map<By, Integer> counts = validateBatch(js, target, candidates);   // 1 validation round-trip
        if (counts.isEmpty()) return null;
        return rankAndPick(candidates, by -> counts.getOrDefault(by, 0));
    }

    /** Strong, stable identity that does not need validation. Pure. */
    static By fastPathLocator(Map<String, Object> attrs) {
        // Priority 1: any non-dynamic data-* attribute (covers data-testid, data-test, and any
        // custom attr the app uses — e.g. data-automation-id, data-ftid, data-qa-selector).
        Object dm = attrs.get("dataAttrs");
        if (dm instanceof Map<?, ?> dataMap) {
            for (Map.Entry<?, ?> e : dataMap.entrySet()) {
                String n = e.getKey() != null ? e.getKey().toString() : null;
                String v = e.getValue() != null ? e.getValue().toString() : null;
                if (present(n) && present(v) && !LocatorTechniques.looksDynamic(v)) {
                    return By.cssSelector("[" + n + "='" + cssEsc(v) + "']");
                }
            }
        }
        // Fallback for when dataAttrs map is absent (older batch or non-batch path)
        for (String a : new String[]{"data-testid", "data-test"}) {
            String v = str(attrs.get(a));
            if (present(v) && !LocatorTechniques.looksDynamic(v)) {
                return By.cssSelector("[" + a + "='" + cssEsc(v) + "']");
            }
        }
        String id = str(attrs.get("id"));
        if (present(id) && !LocatorTechniques.looksDynamic(id)) return By.id(id);
        String name = str(attrs.get("name"));
        if (present(name) && !LocatorTechniques.looksDynamic(name)) return By.name(name);
        return null;
    }

    /** Rich candidate set (pure, no driver). Capped at {@link #MAX_CANDIDATES}. */
    static List<Candidate> buildWebCandidates(Map<String, Object> attrs) {
        List<Candidate> out = new ArrayList<>();
        if (attrs == null) return out;
        String tag  = str(attrs.get("tag"));
        String text = str(attrs.get("text"));
        String role = str(attrs.get("role"));

        String aria = str(attrs.get("aria-label"));
        if (present(aria)) {
            addCss(out, "[aria-label='" + cssEsc(aria) + "']", "aria-label", W_ARIA);
        }

        if (present(text) && text.length() <= MAX_TEXT_LEN) {
            String lit = SemanticLocatorResolver.xpathLiteral(text);
            if (present(role)) {
                addXpath(out, "//*[@role='" + role + "' and normalize-space(.)=" + lit + "]", "role-text", W_ROLE_TEXT);
            }
            String base = present(tag) ? tag : "*";
            for (String a : new String[]{"data-testid", "name", "type", "placeholder", "title"}) {
                String v = str(attrs.get(a));
                if (present(v)) {
                    addXpath(out, "//" + base + "[normalize-space(.)=" + lit + " and @" + a + "='" + cssEsc(v) + "']",
                            "text-attr", W_TEXT_ATTR);
                }
            }
            if (isClickableTag(tag)) {
                addXpath(out, "//" + tag + "[normalize-space(.)=" + lit + "]", "text", W_TEXT);
            }
        }

        if (present(tag)) {
            for (String a : new String[]{"placeholder", "title", "type"}) {
                String v = str(attrs.get(a));
                if (present(v)) addCss(out, tag + "[" + a + "='" + cssEsc(v) + "']", "tag-attr", W_TAG_ATTR);
            }
        }

        List<String> classes = stableClasses(str(attrs.get("class")));
        if (present(tag) && !classes.isEmpty()) {
            addCss(out, tag + "." + classes.get(0), "tag-class", W_TAG_CLASS1);
            if (classes.size() >= 2) {
                addCss(out, tag + "." + classes.get(0) + "." + classes.get(1), "tag-class2", W_TAG_CLASS2);
            }
            addCss(out, "." + classes.get(0), "class", W_CLASS1);
        }

        return out.size() > MAX_CANDIDATES ? out.subList(0, MAX_CANDIDATES) : out;
    }

    /** Validate ALL candidates + the nth-of-type path in ONE executeScript round-trip. */
    private static Map<By, Integer> validateBatch(JavascriptExecutor js, WebElement target,
                                                  List<Candidate> candidates) {
        Map<By, Integer> result = new IdentityHashMap<>();
        List<Map<String, String>> descriptors = new ArrayList<>(candidates.size());
        for (Candidate c : candidates) descriptors.add(Map.of("kind", c.kind, "sel", c.sel));
        try {
            Object raw = js.executeScript(VALIDATE_SCRIPT, target, descriptors);
            if (!(raw instanceof Map<?, ?> m)) return result;
            Object resObj = m.get("results");
            if (resObj instanceof List<?> rows) {
                for (int i = 0; i < candidates.size() && i < rows.size(); i++) {
                    result.put(candidates.get(i).by, matchOf(rows.get(i)));
                }
            }
            String pathSel = str(m.get("pathSel"));
            int pathHit = matchOf(m.get("pathInfo"));
            if (present(pathSel) && pathHit > 0) {
                Candidate pathCand = new Candidate(By.cssSelector(pathSel), "css", pathSel, "css-path", W_PATH);
                candidates.add(pathCand);
                result.put(pathCand.by, pathHit);
            }
        } catch (Exception ignored) {}
        return result;
    }

    /** {count,hit} row → targetMatches (hit ? count : 0). */
    private static int matchOf(Object row) {
        if (!(row instanceof Map<?, ?> r)) return 0;
        Object hit = r.get("hit");
        if (!Boolean.TRUE.equals(hit)) return 0;
        Object count = r.get("count");
        return (count instanceof Number n) ? Math.max(1, n.intValue()) : 1;
    }

    private static final String VALIDATE_SCRIPT =
            LocatorTechniques.STRUCTURAL_PATH_FN
            + "var el=arguments[0], cands=arguments[1];"
            + "function info(kind,sel){var nodes=[];try{"
            + " if(kind==='xpath'){var r=document.evaluate(sel,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);"
            + "   for(var i=0;i<r.snapshotLength;i++) nodes.push(r.snapshotItem(i));}"
            + " else {nodes=Array.prototype.slice.call(document.querySelectorAll(sel));}"
            + "}catch(e){return {'count':-1,'hit':false};}"
            + " var hit=false; for(var j=0;j<nodes.length;j++){ if(nodes[j]===el){hit=true;break;} }"
            + " return {'count':nodes.length,'hit':hit};}"
            + "var out=[]; for(var k=0;k<cands.length;k++){ out.push(info(cands[k].kind,cands[k].sel)); }"
            + "var pathSel=''; try{ pathSel=__ellPath(el); }catch(e){}"
            + "var pathInfo = pathSel ? info('css',pathSel) : null;"
            + "return {'results':out,'pathSel':pathSel,'pathInfo':pathInfo};";

    // ──────────────────────── Ranking (pure, testable) ────────────────────────

    /** Drops invalid candidates; scores valid ones by weight × validity × brevity; returns the best By. */
    static By rankAndPick(List<Candidate> candidates, MatchProbe probe) {
        By best = null;
        double bestScore = -1.0;
        for (Candidate c : candidates) {
            int tm = probe.targetMatches(c.by);
            if (tm <= 0) continue;                       // invalid → dropped
            double validity = (tm == 1) ? 1.0 : 1.0 / tm;
            double brevity  = 1.0 / (1.0 + c.sel.length() / 120.0);
            double score    = c.weight * validity * (0.9 + 0.1 * brevity);
            if (score > bestScore) { bestScore = score; best = c.by; }
        }
        return best;
    }

    // ──────────────────────── Mobile native ────────────────────────

    private static By buildMobileNative(WebDriver driver, WebElement target) {
        String resId = attr(target, "resource-id");
        if (present(resId)) return AppiumBy.id(resId);                 // fast path
        String acc = attr(target, "accessibility-id");
        if (!present(acc)) acc = attr(target, "content-desc");
        if (present(acc)) return AppiumBy.accessibilityId(acc);         // fast path

        String rawTag = rawTag(target);
        String text   = attr(target, "text");
        List<Candidate> cands = new ArrayList<>();
        if (present(text)) {
            cands.add(new Candidate(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + text + "\")"),
                    "appium", text, "ui-text", W_TEXT));
            String base = present(rawTag) ? rawTag : "*";
            cands.add(new Candidate(By.xpath("//" + base + "[@text=" + SemanticLocatorResolver.xpathLiteral(text) + "]"),
                    "xpath", text, "xpath-text", W_TEXT_ATTR));
        }
        if (present(rawTag)) {
            cands.add(new Candidate(By.xpath("//" + rawTag), "xpath", rawTag, "xpath-class", W_TAG_CLASS1));
        }
        List<Candidate> capped = cands.size() > MOBILE_PROBE_CAP ? cands.subList(0, MOBILE_PROBE_CAP) : cands;
        return rankAndPick(capped, by -> nativeMatch(driver, by, target));
    }

    private static int nativeMatch(WebDriver driver, By by, WebElement target) {
        try {
            List<WebElement> found = driver.findElements(by);
            if (found.isEmpty() || !found.contains(target)) return 0;
            return found.size();
        } catch (Exception e) {
            return 0;
        }
    }

    // ──────────────────────── Helpers ────────────────────────

    private static void addCss(List<Candidate> out, String sel, String tier, double weight) {
        out.add(new Candidate(By.cssSelector(sel), "css", sel, tier, weight));
    }

    private static void addXpath(List<Candidate> out, String xp, String tier, double weight) {
        out.add(new Candidate(By.xpath(xp), "xpath", xp, tier, weight));
    }

    private static List<String> stableClasses(String cls) {
        List<String> out = new ArrayList<>();
        if (!present(cls)) return out;
        for (String c : cls.trim().split("\\s+")) {
            if (LocatorTechniques.isCssSafeIdentifier(c) && !LocatorTechniques.looksDynamic(c)) out.add(c);
            if (out.size() >= 2) break;
        }
        return out;
    }

    private static boolean isClickableTag(String tag) {
        return "a".equals(tag) || "button".equals(tag) || "label".equals(tag) || "summary".equals(tag);
    }

    private static String cssEsc(String v) { return v.replace("'", "\\'"); }

    private static String attr(WebElement el, String name) {
        try {
            String v = el.getAttribute(name);
            return present(v) ? v : null;
        } catch (Exception e) { return null; }
    }

    private static String rawTag(WebElement el) {
        try { return el.getTagName(); } catch (Exception e) { return null; }
    }

    private static String str(Object o) { return o != null ? o.toString() : null; }
    private static boolean present(String s) { return s != null && !s.isBlank(); }
}
