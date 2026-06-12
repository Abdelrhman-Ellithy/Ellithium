package Ellithium.core.ai;

import Ellithium.core.ai.scoring.SemanticNameExtractor;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.scoring.LocatorMutationEngine;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.support.locators.RelativeLocator;

import java.util.ArrayList;
import java.util.List;

/**
 * Tier 2 — Semantic Strategy Search.
 *
 * <p>When a locator fails and baseline fingerprinting can't heal it,
 * this resolver uses the <b>semantic meaning</b> of the element (extracted
 * from method name, field name, and action type) to generate and try
 * dozens of locator strategies algorithmically — zero LLM cost.</p>
 *
 * <p>For example, if a method called {@code setUserEmail()} has a broken
 * {@code By.id("email")} locator, this resolver will:</p>
 * <ol>
 *   <li>Extract the semantic name: {@code "Email"}</li>
 *   <li>Detect the element category from the action: {@code sendData → INPUT}</li>
 *   <li>Generate ~30 XPath/CSS strategies targeting inputs with "Email" in
 *       their placeholder, aria-label, id, name, or nearby text</li>
 *   <li>Try each strategy against the live DOM</li>
 *   <li>Cross-validate against the stored baseline fingerprint</li>
 * </ol>
 *
 * <p>Includes Selenium 4 {@link RelativeLocator} strategies for web drivers
 * (skipped for Appium since RelativeLocator requires a Chromium backend).</p>
 */
public class SemanticLocatorResolver {

    // Graded f2 strategy weights by tier (gold = most stable test attrs … iron = structural/relational).
    private static final double F2_GOLD = 1.00, F2_SILVER = 0.75, F2_BRONZE = 0.50, F2_IRON = 0.30;

    /**
     * The type of UI element we're searching for, inferred from the Ellithium
     * action method (sendData, clickOnElement, etc.).
     */
    public enum ElementCategory {
        INPUT,      // sendData, type, enterText → input, textarea
        CLICKABLE,  // click, tap, press → button, a, role=button
        SELECT,     // selectDropdown → select
        READABLE,   // getText, getTitle → any visible text-bearing element
        ANY         // fallback
    }

    /**
     * Attempts to find the broken element using semantic strategies.
     *
     * @param driver     The WebDriver instance
     * @param methodName The POM method name (e.g., "setUserEmail")
     * @param fieldName  The locator field name (e.g., "emailField"), may be null
     * @param actionType The Ellithium action (e.g., "sendData", "clickOnElement")
     * @param locatorValue The broken locator's original value (e.g., "email_input")
     * @param baseline   The stored fingerprint for cross-validation, may be null
     * @return The matched WebElement, or null if no confident match was found
     */
    /**
     * Fans out ALL semantic strategies and returns every matching live element with its tier weight
     * (gold=1.0, silver=0.75, bronze=0.5, iron=0.3). Side-effect-free — the ensemble's job to commit
     * the winner. The mutation pre-pass is included as a synthetic high-confidence hit (weight 0.95).
     */
    public static List<Ellithium.core.ai.models.SemanticHit> findSemanticHits(WebDriver driver,
                                                                               String methodName, String fieldName,
                                                                               String actionType, String locatorValue,
                                                                               ElementFingerprint baseline) {
        List<Ellithium.core.ai.models.SemanticHit> hits = new ArrayList<>();
        collectMutationHits(hits, locatorValue, driver, baseline);

        ElementCategory category = categorizeAction(actionType);
        boolean isMobile = driver instanceof AppiumDriver;
        String semanticMethodName = (category == ElementCategory.READABLE) ? null : methodName;
        List<String> semanticNames = SemanticNameExtractor.extract(semanticMethodName, fieldName, locatorValue);
        if (semanticNames.isEmpty()) return hits;

        TieredAttempts tiered = buildTieredStrategies(semanticNames, category, isMobile, driver, baseline);

        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            java.util.IdentityHashMap<WebElement, Double> best = new java.util.IdentityHashMap<>();
            // Always run gold AND silver. Batch all CSS-batchable strategies into 1 JS call;
            // XPath/RelativeLocator/Appium strategies fall back to individual findElements.
            if (driver instanceof JavascriptExecutor) {
                collectHitsBatched(driver, tiered.gold, F2_GOLD, tiered.silver, F2_SILVER, best);
            } else {
                collectHits(driver, tiered.gold,   F2_GOLD,   best);
                collectHits(driver, tiered.silver, F2_SILVER, best);
            }
            if (best.isEmpty()) collectHitsBatchedXPath(driver, tiered.bronze, F2_BRONZE, best);
            if (best.isEmpty()) collectHitsBatchedXPath(driver, tiered.iron,   F2_IRON,   best);
            for (java.util.Map.Entry<WebElement, Double> e : best.entrySet()) {
                hits.add(new Ellithium.core.ai.models.SemanticHit(e.getKey(), e.getValue(), "strategy"));
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
        return hits;
    }

    /** Mutation pre-pass shared by findSemanticHits and findExactHits. */
    private static void collectMutationHits(List<Ellithium.core.ai.models.SemanticHit> hits,
                                            String locatorValue, WebDriver driver,
                                            ElementFingerprint baseline) {
        if (locatorValue != null && !locatorValue.isBlank()) {
            By brokenBy = rebuildLocator(locatorValue);
            if (brokenBy != null) {
                WebElement mutationMatch = LocatorMutationEngine.tryMutations(brokenBy, driver, baseline);
                if (mutationMatch != null) {
                    hits.add(new Ellithium.core.ai.models.SemanticHit(mutationMatch, 0.95, "mutation"));
                }
            }
        }
    }

    /** Clears the strategy caches. Call between suites to avoid stale strategies from a different app. */
    public static void resetCache() {
        STRATEGY_CACHE.clear();
        FLAT_STRATEGY_CACHE.clear();
    }

    private static void collectHits(WebDriver driver, List<LocatorAttempt> attempts, double weight,
                                    java.util.IdentityHashMap<WebElement, Double> best) {
        for (LocatorAttempt attempt : attempts) {
            try {
                List<WebElement> found = driver.findElements(attempt.locator);
                for (WebElement el : found) {
                    Double prev = best.get(el);
                    if (prev == null || prev < weight) best.put(el, weight);
                }
            } catch (Exception ignored) {}
        }
    }

    private static final String XPATH_BATCH_SCRIPT =
            "return arguments[0].map(function(expr){"
            + " try{"
            + "  var r=[], snap=document.evaluate(expr,document,null,5,null);"
            + "  var n; while((n=snap.iterateNext())!=null) r.push(n);"
            + "  return r;"
            + " } catch(e){ return []; }"
            + "});";

    private static void collectHitsBatchedXPath(WebDriver driver, List<LocatorAttempt> attempts,
                                                double weight,
                                                java.util.IdentityHashMap<WebElement, Double> best) {
        if (attempts.isEmpty()) return;
        List<String> xpaths = new ArrayList<>();
        List<LocatorAttempt> nonXpath = new ArrayList<>();
        for (LocatorAttempt a : attempts) {
            String s = a.locator.toString();
            if (s.startsWith("By.xpath: ")) xpaths.add(s.substring(10));
            else nonXpath.add(a);
        }
        if (!xpaths.isEmpty() && driver instanceof JavascriptExecutor js) {
            boolean batched = false;
            try {
                Object res = js.executeScript(XPATH_BATCH_SCRIPT, (Object) xpaths.toArray(new String[0]));
                if (res instanceof List<?> rows) {
                    batched = true;
                    for (Object row : rows) {
                        if (!(row instanceof List<?> els)) continue;
                        for (Object o : els) {
                            if (o instanceof WebElement el) {
                                Double prev = best.get(el);
                                if (prev == null || prev < weight) best.put(el, weight);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (!batched) {
                for (String xpath : xpaths) {
                    try {
                        for (WebElement el : driver.findElements(By.xpath(xpath))) {
                            Double prev = best.get(el);
                            if (prev == null || prev < weight) best.put(el, weight);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } else {
            for (String xpath : xpaths) {
                try {
                    for (WebElement el : driver.findElements(By.xpath(xpath))) {
                        Double prev = best.get(el);
                        if (prev == null || prev < weight) best.put(el, weight);
                    }
                } catch (Exception ignored) {}
            }
        }
        if (!nonXpath.isEmpty()) collectHits(driver, nonXpath, weight, best);
    }

    public static double strategyWeightForAttrs(java.util.Map<String, Object> attrs, List<String> names) {
        if (attrs == null || names == null || names.isEmpty()) return Double.NaN;
        String id   = lc(attrs.get("id")),   nm    = lc(attrs.get("name")),  testid = lc(attrs.get("data-testid"));
        String aria = lc(attrs.get("aria-label")), accId = lc(attrs.get("accessibility-id"));
        String cdesc = lc(attrs.get("content-desc")), resId = lc(attrs.get("resource-id"));
        String allattrs = lc(attrs.get("allattrs")), text = lc(attrs.get("text"));

        double best = Double.NaN;
        for (String raw : names) {
            if (raw == null || raw.isBlank()) continue;
            String n = raw.toLowerCase();
            // Gold: stable attribute exact match, but only when the value is not auto-generated.
            if ((eq(id, n) && !looksDynamic(id))
                    || (eq(nm, n) && !looksDynamic(nm))
                    || (eq(testid, n) && !looksDynamic(testid))
                    || eq(aria, n)
                    || eq(accId, n)
                    || eq(cdesc, n)
                    || suffixEq(resId, n)) {
                return F2_GOLD;
            }
            // Auto-generated values matched exactly are capped at silver.
            if (eq(id, n) || eq(nm, n) || eq(testid, n)) {
                best = nanMax(best, F2_SILVER);
            }
            if (eq(text, n) || has(text, n) || has(allattrs, n)) {
                best = nanMax(best, F2_SILVER);
            }
        }
        return best;
    }

    /**
     * Returns true when an attribute value looks auto-generated (UUID, pure numeric, framework
     * auto-id, short random hex, or numeric-suffix pattern) and is therefore unreliable as a
     * gold-confidence locator signal.
     */
    static boolean looksDynamic(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.matches("\\{?[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\}?")) return true;
        if (value.matches("[0-9]+")) return true;
        if (value.matches(":[a-zA-Z][a-zA-Z0-9]{0,4}:")) return true;
        if (value.length() <= 12 && value.matches("[0-9a-f]{4,12}")) return true;
        if (value.matches(".*[-_][0-9]+")) return true;
        return false;
    }

    private static String lc(Object o) { return o == null ? null : o.toString().toLowerCase(); }
    private static boolean eq(String a, String n) { return a != null && a.equals(n); }
    private static boolean has(String a, String n) { return a != null && !a.isEmpty() && a.contains(n); }
    private static boolean suffixEq(String resId, String n) {
        if (resId == null) return false;
        int i = resId.lastIndexOf('/');
        return (i >= 0 ? resId.substring(i + 1) : resId).equals(n);
    }
    private static double nanMax(double a, double b) { return Double.isNaN(a) ? b : Math.max(a, b); }

    /**
     * Standalone Tier-2 heal — used as a fallback path when the ensemble model is unavailable.
     *
     * <p>Iterates the full flat strategy list (gold → silver → bronze → iron) and cross-validates
     * each candidate against the baseline fingerprint as it goes. Unlike {@link #findSemanticHits}
     * which stops at the first productive tier, this continues past a tier whose DOM hits all fail
     * the similarity gate — so a silver or bronze match is still reachable even when gold strategies
     * find elements that don't resemble the stored baseline.
     */
    public static WebElement trySemanticHeal(WebDriver driver,
                                             String methodName, String fieldName,
                                             String actionType, String locatorValue,
                                             ElementFingerprint baseline) {
        boolean switchedFrame = (baseline != null) && baseline.enterIframeContext(driver);
        try {
        return trySemanticHealInContext(driver, methodName, fieldName, actionType, locatorValue, baseline);
        } finally {
            if (switchedFrame) {
                try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
            }
        }
    }

    private static WebElement trySemanticHealInContext(WebDriver driver,
                                                       String methodName, String fieldName,
                                                       String actionType, String locatorValue,
                                                       ElementFingerprint baseline) {
        List<Ellithium.core.ai.models.SemanticHit> mutationHits = new ArrayList<>();
        collectMutationHits(mutationHits, locatorValue, driver, baseline);
        if (!mutationHits.isEmpty()) {
            WebElement el = mutationHits.get(0).element;
            By loc = ElementFingerprint.reconstructLocator(el);
            String locStr = loc != null ? loc.toString() : "";
            HealingTelemetryStore.record(2, locatorValue, locStr, 0.95, true);
            return el;
        }

        ElementCategory category = categorizeAction(actionType);
        boolean isMobile = driver instanceof AppiumDriver;
        String semanticMethodName = (category == ElementCategory.READABLE) ? null : methodName;
        List<String> semanticNames = SemanticNameExtractor.extract(semanticMethodName, fieldName, locatorValue);
        if (semanticNames.isEmpty()) {
            HealingTelemetryStore.record(2, locatorValue, null, 0.0, false);
            return null;
        }

        double cvThreshold;
        if (baseline == null) cvThreshold = 0.0;
        else if (baseline.hasStrongIdentity()) cvThreshold = 0.35;
        else cvThreshold = 0.55;

        List<LocatorAttempt> strategies = buildStrategies(semanticNames, category, isMobile, driver, baseline);

        // Light DOM first (the common case): collect candidates and pick the BEST scorer, not the
        // first above threshold — first-match heals to the wrong cell on grids/tables/card lists.
        java.util.LinkedHashMap<WebElement, String> candidateDesc = collectFromStrategies(driver, strategies);
        Scored best = candidateDesc.isEmpty() ? null : pickBest(driver, candidateDesc, baseline, cvThreshold);

        // Shadow-DOM fallthrough only: web components hide their internals behind shadow roots that
        // plain findElements cannot pierce. We pay the extra JS round-trip ONLY when the light DOM
        // produced no acceptable match, so the happy path keeps its original cost.
        if (best == null) {
            java.util.LinkedHashMap<WebElement, String> shadow = new java.util.LinkedHashMap<>();
            collectShadowCandidates(driver, strategies, shadow);
            if (!shadow.isEmpty()) best = pickBest(driver, shadow, baseline, cvThreshold);
        }

        if (best == null) {
            HealingTelemetryStore.record(2, locatorValue, null, 0.0, false);
            return null;
        }

        By cleanLocator = ElementFingerprint.reconstructLocator(best.element);
        String cleanLocatorStr = cleanLocator != null ? cleanLocator.toString() : "";
        String scoreStr = Double.isNaN(best.score) ? "unscored" : String.format("%.2f", best.score);
        Reporter.log(String.format("[TIER 2] healed via %s: %s (score %s)",
                best.desc, cleanLocatorStr, scoreStr), LogLevel.INFO_GREEN);
        HealingTelemetryStore.record(2, locatorValue, cleanLocatorStr,
                Double.isNaN(best.score) ? 0.0 : best.score, true);
        return best.element;
    }

    /** Result of cross-validating a candidate pool: the winning element, its score, and its source description. */
    private record Scored(WebElement element, double score, String desc) {}

    /** Collects candidates from every strategy in the current browsing context (deduped, DOM order preserved). */
    private static java.util.LinkedHashMap<WebElement, String> collectFromStrategies(
            WebDriver driver, List<LocatorAttempt> strategies) {
        java.util.LinkedHashMap<WebElement, String> candidateDesc = new java.util.LinkedHashMap<>();
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            for (LocatorAttempt attempt : strategies) {
                try {
                    for (WebElement el : driver.findElements(attempt.locator)) {
                        candidateDesc.putIfAbsent(el, attempt.description);
                    }
                } catch (Exception ignored) {}
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
        return candidateDesc;
    }

    /**
     * Cross-validates a candidate pool with ONE batched attribute read (0 extra WebDriver round-trips)
     * and returns the HIGHEST-scoring candidate above {@code cvThreshold}, early-exiting at a
     * near-perfect 0.90 (same bar as Tier 1). When no baseline is available to rank against, falls
     * back to the original first-visible-match short-circuit.
     */
    private static final String VISIBILITY_BATCH_SCRIPT =
            "return arguments[0].map(function(el){"
            + "try{var r=el.getBoundingClientRect(),cs=getComputedStyle(el);"
            + "return !!(el.offsetParent!==null&&r.width>0&&r.height>0"
            + "&&cs.visibility!=='hidden'&&cs.display!=='none');}catch(e){return false;}"
            + "});";

    private static boolean[] fetchVisibilityFallback(WebDriver driver, List<WebElement> candidates) {
        if (!(driver instanceof JavascriptExecutor js)) return null;
        try {
            Object res = js.executeScript(VISIBILITY_BATCH_SCRIPT, candidates);
            if (res instanceof List<?> rows) {
                boolean[] out = new boolean[candidates.size()];
                for (int i = 0; i < rows.size() && i < out.length; i++)
                    out[i] = Boolean.TRUE.equals(rows.get(i));
                return out;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Scored pickBest(WebDriver driver,
                                   java.util.LinkedHashMap<WebElement, String> candidateDesc,
                                   ElementFingerprint baseline, double cvThreshold) {
        List<WebElement> candidateList = new ArrayList<>(candidateDesc.keySet());
        java.util.List<java.util.Map<String, Object>> batch =
                Ellithium.core.ai.dom.CandidateAttributeBatcher.fetch(driver, candidateList);
        boolean scoring = (baseline != null && cvThreshold > 0.0);

        // When the full attribute batch failed, try a lightweight visibility-only JS batch (1 round-trip
        // for all candidates). Falls back to per-element isDisplayed() only when JS is unavailable
        // (Appium native context, non-JS driver).
        boolean[] visFallback = (batch == null) ? fetchVisibilityFallback(driver, candidateList) : null;

        WebElement bestEl = null;
        String bestDesc = null;
        double bestScore = -1.0;
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            for (int i = 0; i < candidateList.size(); i++) {
                WebElement el = candidateList.get(i);
                java.util.Map<String, Object> attrs = (batch != null && i < batch.size()) ? batch.get(i) : null;
                try {
                    if (attrs != null) {
                        if (Boolean.FALSE.equals(attrs.get("visible"))) continue;
                    } else if (visFallback != null) {
                        if (!visFallback[i]) continue;
                    } else if (!el.isDisplayed()) {
                        continue;
                    }
                    if (!scoring) {
                        // No baseline to rank by — return NaN score so callers can distinguish
                        // "scored heal" from "first-visible-match with no baseline to validate against".
                        return new Scored(el, Double.NaN, candidateDesc.getOrDefault(el, "strategy"));
                    }
                    double score = (attrs != null)
                            ? baseline.scoreSimilarity(attrs, structuralFrom(attrs))
                            : baseline.scoreSimilarity(el);
                    if (score < cvThreshold) continue;
                    if (score > bestScore) {
                        bestScore = score;
                        bestEl = el;
                        bestDesc = candidateDesc.getOrDefault(el, "strategy");
                        if (score >= 0.90) break;   // near-perfect: stop scanning (pure-Java early exit)
                    }
                } catch (Exception ignored) {}
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
        return bestEl == null ? null : new Scored(bestEl, bestScore, bestDesc);
    }

    // Walks the document + every open shadow root once, collecting matches for a (comma-joined)
    // selector. ONE executeScript serves all CSS strategies — no per-strategy round-trip.
    private static final String SHADOW_QUERY_SCRIPT =
            "var sel=arguments[0],lim=arguments[1],out=[];"
            + "function walk(root){"
            + " if(out.length>=lim)return;"
            + " try{var m=root.querySelectorAll(sel);"
            + "  for(var i=0;i<m.length&&out.length<lim;i++) out.push(m[i]);}catch(e){}"
            + " var all=root.querySelectorAll('*');"
            + " for(var j=0;j<all.length&&out.length<lim;j++) if(all[j].shadowRoot) walk(all[j].shadowRoot);"
            + "}"
            + "walk(document); return out;";

    private static final int SHADOW_MATCH_LIMIT = 50;

    /**
     * Pierces open shadow roots for the CSS-expressible strategies. All selectors are comma-joined
     * into a single {@code querySelectorAll} run inside every shadow root, so this costs exactly ONE
     * WebDriver round-trip regardless of strategy count. XPath / relative / link-text strategies are
     * skipped — CSS is the only selector form that can cross a shadow boundary.
     */
    private static void collectShadowCandidates(WebDriver driver, List<LocatorAttempt> strategies,
                                                java.util.LinkedHashMap<WebElement, String> candidateDesc) {
        if (!(driver instanceof JavascriptExecutor js)) return;
        java.util.LinkedHashSet<String> selectors = new java.util.LinkedHashSet<>();
        for (LocatorAttempt attempt : strategies) {
            String css = toCssSelector(attempt.locator);
            if (css != null) selectors.add(css);
        }
        if (selectors.isEmpty()) return;
        String combined = String.join(",", selectors);
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            Object res = js.executeScript(SHADOW_QUERY_SCRIPT, combined, SHADOW_MATCH_LIMIT);
            if (res instanceof List<?> rows) {
                for (Object o : rows) {
                    if (o instanceof WebElement w) candidateDesc.putIfAbsent(w, "shadow-DOM strategy");
                }
            }
        } catch (Exception ignored) {
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
    }

    /**
     * Renders a {@link By} as a CSS selector for shadow-root querying, or {@code null} when the
     * strategy cannot cross a shadow boundary (XPath, relative, link-text). Id/class/name are emitted
     * as attribute selectors to sidestep CSS identifier escaping.
     */
    private static String toCssSelector(By by) {
        String s = by.toString();
        int idx = s.indexOf(": ");
        if (idx < 0) return null;
        String prefix = s.substring(0, idx);
        String value = s.substring(idx + 2);
        return switch (prefix) {
            case "By.cssSelector" -> value;
            case "By.tagName" -> value;
            case "By.id" -> "[id='" + cssAttrValue(value) + "']";
            case "By.className" -> "[class~='" + cssAttrValue(value) + "']";
            case "By.name" -> "[name='" + cssAttrValue(value) + "']";
            default -> null;
        };
    }

    private static String cssAttrValue(String v) {
        return v.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static ElementFingerprint.StructuralContext structuralFrom(java.util.Map<String, Object> attrs) {
        Object pt = attrs.get("parent-tag");
        Object ci = attrs.get("child-index");
        if (pt == null && ci == null) return null;
        int childIdx = ci instanceof Number n ? n.intValue() : -1;
        String prevSib = attrs.get("prev-sib") instanceof String s ? s : null;
        String nextSib = attrs.get("next-sib") instanceof String s ? s : null;
        return new ElementFingerprint.StructuralContext(
                pt != null ? pt.toString() : null, childIdx, prevSib, nextSib);
    }

    /**
     * Generates all applicable locator strategies for the given semantic names
     * and element category.
     */
    static final class TieredAttempts {
        final List<LocatorAttempt> gold;
        final List<LocatorAttempt> silver;
        final List<LocatorAttempt> bronze;
        final List<LocatorAttempt> iron;
        TieredAttempts(List<LocatorAttempt> gold, List<LocatorAttempt> silver,
                       List<LocatorAttempt> bronze, List<LocatorAttempt> iron) {
            this.gold = gold; this.silver = silver; this.bronze = bronze; this.iron = iron;
        }
    }

    private static final int STRATEGY_CACHE_MAX = 1000;
    private static final java.util.Map<String, TieredAttempts> STRATEGY_CACHE =
            java.util.Collections.synchronizedMap(
                    new java.util.LinkedHashMap<>(STRATEGY_CACHE_MAX, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(java.util.Map.Entry<String, TieredAttempts> e) {
                            return size() > STRATEGY_CACHE_MAX;
                        }
                    });

    private static final java.util.Map<String, List<LocatorAttempt>> FLAT_STRATEGY_CACHE =
            java.util.Collections.synchronizedMap(
                    new java.util.LinkedHashMap<>(STRATEGY_CACHE_MAX, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(java.util.Map.Entry<String, List<LocatorAttempt>> e) {
                            return size() > STRATEGY_CACHE_MAX;
                        }
                    });

    private static TieredAttempts buildTieredStrategies(List<String> names,
                                                        ElementCategory category,
                                                        boolean isMobile,
                                                        WebDriver driver,
                                                        ElementFingerprint baseline) {
        String parentTag = (baseline != null) ? baseline.getParentTag() : null;
        String scopePrefix = (parentTag != null) ? "//" + parentTag + "/" : "//";
        String cacheKey = String.join(",", names) + "|"
                + category + "|" + isMobile + "|" + parentTag;
        TieredAttempts cached = STRATEGY_CACHE.get(cacheKey);
        if (cached != null) return cached;
        TieredAttempts built = buildTieredUncached(names, category, isMobile, driver, scopePrefix);
        STRATEGY_CACHE.put(cacheKey, built);
        return built;
    }

    /**
     * Flattens the tiered strategies into ONE ordered, deduplicated list (gold → silver → bronze
     * → iron), preserving priority. Used by {@link #trySemanticHeal} for sequential
     * try-and-cross-validate iteration: unlike the tiered pass in {@link #findSemanticHits} which
     * stops at the first productive tier, iterating this flat list allows cross-validation to
     * continue past a tier whose DOM hits all fail the fingerprint similarity gate.
     */
    static List<LocatorAttempt> buildStrategies(List<String> names,
                                                ElementCategory category,
                                                boolean isMobile,
                                                WebDriver driver,
                                                ElementFingerprint baseline) {
        String parentTag = (baseline != null) ? baseline.getParentTag() : null;
        String cacheKey = String.join(",", names) + "|" + category + "|" + isMobile + "|" + parentTag;
        List<LocatorAttempt> cached = FLAT_STRATEGY_CACHE.get(cacheKey);
        if (cached != null) return cached;

        TieredAttempts t = buildTieredStrategies(names, category, isMobile, driver, baseline);
        List<LocatorAttempt> ordered = new ArrayList<>(
                t.gold.size() + t.silver.size() + t.bronze.size() + t.iron.size());
        ordered.addAll(t.gold);
        ordered.addAll(t.silver);
        ordered.addAll(t.bronze);
        ordered.addAll(t.iron);
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        List<LocatorAttempt> deduped = new ArrayList<>();
        for (LocatorAttempt a : ordered) {
            if (seen.add(a.locator.toString())) deduped.add(a);
        }
        List<LocatorAttempt> result = java.util.Collections.unmodifiableList(deduped);
        FLAT_STRATEGY_CACHE.put(cacheKey, result);
        return result;
    }

    private static TieredAttempts buildTieredUncached(List<String> names,
                                                      ElementCategory category,
                                                      boolean isMobile,
                                                      WebDriver driver,
                                                      String scopePrefix) {

        List<LocatorAttempt> gold   = new ArrayList<>();
        List<LocatorAttempt> silver = new ArrayList<>();
        List<LocatorAttempt> bronze = new ArrayList<>();
        List<LocatorAttempt> iron   = new ArrayList<>();

        for (String name : names) {
            if (name.isBlank()) continue;
            String lower = name.toLowerCase();

            addTestAttrStrategies(gold, lower);
            if (isMobile) addMobileGoldStrategies(gold, name, lower);

            addSilverStrategies(silver, name, lower, scopePrefix);

            switch (category) {
                case INPUT:
                    addInputStrategies(bronze, name, lower, scopePrefix, driver);
                    if (!isMobile) addRelativeInputStrategies(iron, name, driver);
                    addNeighborhoodInputStrategies(iron, name);
                    break;
                case CLICKABLE:
                    addClickableStrategies(bronze, name, lower, scopePrefix);
                    break;
                case SELECT:
                    addSelectStrategies(bronze, name, lower, scopePrefix);
                    break;
                case READABLE:
                    addReadableStrategies(bronze, name, lower);
                    addNeighborhoodReadableStrategies(iron, name);
                    break;
                case ANY:
                    addBronzeUniversalStrategies(bronze, name, lower, scopePrefix);
                    break;
            }

            if (category != ElementCategory.ANY) {
                addBronzeUniversalStrategies(bronze, name, lower, scopePrefix);
            }
        }
        return new TieredAttempts(gold, silver, bronze, iron);
    }

    private static void addMobileGoldStrategies(List<LocatorAttempt> out, String name, String lower) {
        try {
            out.add(attempt(io.appium.java_client.AppiumBy.accessibilityId(name),
                    "AppiumBy.accessibilityId('" + name + "')"));
            out.add(attempt(io.appium.java_client.AppiumBy.accessibilityId(lower),
                    "AppiumBy.accessibilityId('" + lower + "')"));
            out.add(attempt(io.appium.java_client.AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceIdMatches(\".*" + lower + ".*\")"),
                    "uiautomator resourceIdMatches " + lower));
            out.add(attempt(io.appium.java_client.AppiumBy.iOSClassChain(
                            "**/*[`name CONTAINS[c] '" + lower + "'`]"),
                    "iosClassChain name CONTAINS " + lower));
        } catch (Exception ignored) {}
    }

    private static void addNeighborhoodInputStrategies(List<LocatorAttempt> out, String name) {
        out.add(attempt(By.xpath(precedingSibling("input", name)),
                "input preceding text '" + name + "'"));
        out.add(attempt(By.xpath(precedingSibling("textarea", name)),
                "textarea preceding text '" + name + "'"));
    }

    private static void addNeighborhoodReadableStrategies(List<LocatorAttempt> out, String name) {
        out.add(attempt(By.xpath(precedingSibling("span", name)),
                "span preceding text '" + name + "'"));
        out.add(attempt(By.xpath(precedingSibling("div", name)),
                "div preceding text '" + name + "'"));
    }

    private static void addTestAttrStrategies(List<LocatorAttempt> out, String lower) {
        out.add(attempt(By.cssSelector("[data-testid='"  + lower + "']"), "[data-testid='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-testid*='" + lower + "']"), "[data-testid*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-test='"    + lower + "']"), "[data-test='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-test*='"   + lower + "']"), "[data-test*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-qa='"      + lower + "']"), "[data-qa='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-qa*='"     + lower + "']"), "[data-qa*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-cy='"            + lower + "']"), "[data-cy='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-cy*='"           + lower + "']"), "[data-cy*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-e2e='"           + lower + "']"), "[data-e2e='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-e2e*='"          + lower + "']"), "[data-e2e*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-automation='"    + lower + "']"), "[data-automation='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-automation*='"   + lower + "']"), "[data-automation*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-automation-id='" + lower + "']"), "[data-automation-id='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-automation-id*='"+ lower + "']"), "[data-automation-id*='" + lower + "']"));
    }

    private static void addSilverStrategies(List<LocatorAttempt> out, String name, String lower, String scopePrefix) {
        out.add(attempt(By.id(name),  "id='" + name + "' (exact)"));
        out.add(attempt(By.id(lower), "id='" + lower + "' (exact lower)"));
        out.add(attempt(By.cssSelector("[aria-label='" + name  + "']"), "aria-label='" + name + "'"));
        out.add(attempt(By.cssSelector("[aria-label='" + lower + "']"), "aria-label='" + lower + "'"));
        out.add(attempt(By.cssSelector("[formcontrolname='" + lower + "']"), "formcontrolname='" + lower + "'"));
        out.add(attempt(By.cssSelector("[formcontrolname*='" + lower + "']"), "formcontrolname*='" + lower + "'"));
        out.add(attempt(By.cssSelector("[ng-model*='" + lower + "']"), "ng-model*='" + lower + "'"));
    }

    private static void addInputStrategies(List<LocatorAttempt> out, String name, String lower,
                                            String scopePrefix, WebDriver driver) {
        out.add(attempt(By.xpath(scopePrefix + "input[@placeholder=" + xpathLiteral(name)  + "]"), "input[placeholder='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@placeholder=" + xpathLiteral(lower) + "]"), "input[placeholder='" + lower + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@name=" + xpathLiteral(name)  + "]"), "input[name='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@name=" + xpathLiteral(lower) + "]"), "input[name='" + lower + "'] exact"));

        out.add(attempt(By.xpath(multiCaseXpath("input", "placeholder", name, scopePrefix)), "input[placeholder~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "aria-label",  name, scopePrefix)), "input[aria-label~='"  + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "id",          name, scopePrefix)), "input[id~='"          + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "name",        name, scopePrefix)), "input[name~='"        + name + "']"));

        out.add(attempt(By.xpath(multiCaseXpath("textarea", "placeholder", name, scopePrefix)), "textarea[placeholder~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("textarea", "aria-label",  name, scopePrefix)), "textarea[aria-label~='"  + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("textarea", "id",          name, scopePrefix)), "textarea[id~='"          + name + "']"));

        out.add(attempt(
                By.xpath("//label[contains(normalize-space(text())," + xpathLiteral(name) + ")]/following::input[1]"),
                "label[text~='" + name + "']/following::input"));
        out.add(attempt(
                By.xpath("//label[contains(normalize-space(text())," + xpathLiteral(lower) + ")]/following::input[1]"),
                "label[text~='" + lower + "']/following::input"));
        out.add(attempt(
                By.xpath("(//label[contains(normalize-space(text())," + xpathLiteral(name) + ")]//input)[1]"),
                "label[text~='" + name + "']//input"));

        out.add(attempt(By.xpath(childOfTextContainer("input", name)),   "input inside container with text '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("input", name)),        "first input after text '" + name + "'"));
        out.add(attempt(By.xpath(precedingSibling("input", name)),        "first input before text '" + name + "'"));
    }

    private static void addRelativeInputStrategies(List<LocatorAttempt> out, String name, WebDriver driver) {
        try {
            By textAnchor = By.xpath("//*[contains(normalize-space(text())," + xpathLiteral(name) + ")]");
            out.add(attempt(RelativeLocator.with(By.tagName("input")).near(textAnchor, 150),   "input near text '" + name + "' (RelativeLocator)"));
            out.add(attempt(RelativeLocator.with(By.tagName("input")).toRightOf(textAnchor),   "input right-of text '" + name + "' (RelativeLocator)"));
            out.add(attempt(RelativeLocator.with(By.tagName("input")).below(textAnchor),       "input below text '" + name + "' (RelativeLocator)"));
        } catch (Exception ignored) {}
    }

    private static void addClickableStrategies(List<LocatorAttempt> out, String name, String lower,
                                                String scopePrefix) {
        out.add(attempt(By.xpath(scopePrefix + "button[normalize-space(text())=" + xpathLiteral(name)  + "]"), "button[text='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[normalize-space(text())=" + xpathLiteral(lower) + "]"), "button[text='" + lower + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[@id=" + xpathLiteral(name)  + "]"),  "button[id='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[@id=" + xpathLiteral(lower) + "]"),  "button[id='" + lower + "'] exact"));

        out.add(attempt(By.xpath(textContains("button", name, scopePrefix)), "button with text '" + name + "'"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "id",         name, scopePrefix)), "button[id~='"        + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "name",       name, scopePrefix)), "button[name~='"      + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "aria-label", name, scopePrefix)), "button[aria-label~='"+ name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "title",      name, scopePrefix)), "button[title~='"     + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "value",      name, scopePrefix)), "button[value~='"     + name + "']"));

        out.add(attempt(By.xpath(scopePrefix + "a[normalize-space(text())=" + xpathLiteral(name)  + "]"), "a[text='" + name  + "'] exact"));
        out.add(attempt(By.xpath(textContains("a", name, scopePrefix)), "link with text '" + name + "'"));
        out.add(attempt(By.xpath(multiCaseXpath("a", "id",         name, scopePrefix)), "a[id~='"        + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("a", "aria-label", name, scopePrefix)), "a[aria-label~='"+ name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("a", "title",      name, scopePrefix)), "a[title~='"     + name + "']"));

        out.add(attempt(By.xpath(inputTypeWithAttr("submit", "value", name)), "input[type=submit][value~='" + name + "']"));
        out.add(attempt(By.xpath(inputTypeWithAttr("button", "value", name)), "input[type=button][value~='" + name + "']"));
        out.add(attempt(By.xpath(inputTypeWithAttr("image",  "alt",   name)), "input[type=image][alt~='"   + name + "']"));

        out.add(attempt(By.xpath(roleWithText("button",   name)), "[role=button] with text '"   + name + "'"));
        out.add(attempt(By.xpath(roleWithText("link",     name)), "[role=link] with text '"     + name + "'"));
        out.add(attempt(By.xpath(roleWithText("tab",      name)), "[role=tab] with text '"      + name + "'"));
        out.add(attempt(By.xpath(roleWithText("menuitem", name)), "[role=menuitem] with text '" + name + "'"));

        out.add(attempt(By.xpath("//*[normalize-space(text())=" + xpathLiteral(name)
                + " and (@role='button' or @type='submit' or self::button or self::a)]"),
                "exact text clickable '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("button", name)), "first button after text '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("a",      name)), "first link after text '"   + name + "'"));
    }

    private static void addSelectStrategies(List<LocatorAttempt> out, String name, String lower,
                                             String scopePrefix) {
        out.add(attempt(By.xpath(scopePrefix + "select[@id="   + xpathLiteral(name)  + "]"), "select[id='"   + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "select[@name=" + xpathLiteral(name)  + "]"), "select[name='" + name  + "'] exact"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "id",         name, scopePrefix)), "select[id~='"         + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "name",       name, scopePrefix)), "select[name~='"       + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "aria-label", name, scopePrefix)), "select[aria-label~='" + name + "']"));
        out.add(attempt(By.xpath("//label[contains(normalize-space(text())," + xpathLiteral(name) + ")]/following::select[1]"),
                "select after label '" + name + "'"));
        out.add(attempt(By.xpath(childOfTextContainer("select", name)), "select inside element with text '" + name + "'"));
    }

    private static void addReadableStrategies(List<LocatorAttempt> out, String name, String lower) {
        out.add(attempt(By.xpath(multiCaseXpath("span",    "id", name, "//")), "span[id~='"    + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("div",     "id", name, "//")), "div[id~='"     + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("p",       "id", name, "//")), "p[id~='"       + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("section", "id", name, "//")), "section[id~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("article", "id", name, "//")), "article[id~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("h1",      "id", name, "//")), "h1[id~='"      + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("h2",      "id", name, "//")), "h2[id~='"      + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("h3",      "id", name, "//")), "h3[id~='"      + name + "']"));

        out.add(attempt(By.cssSelector("[aria-label='"  + name  + "']"), "aria-label='" + name  + "' exact"));
        out.add(attempt(By.cssSelector("[aria-label='"  + lower + "']"), "aria-label='" + lower + "' exact"));
        out.add(attempt(By.cssSelector("[aria-label*='" + lower + "']"), "aria-label*='" + lower + "'"));
        out.add(attempt(By.cssSelector("[title*='"      + lower + "']"), "title*='" + lower + "'"));
        out.add(attempt(By.cssSelector("[role='" + lower + "']"), "role='" + lower + "'"));

        out.add(attempt(By.cssSelector("[class*='" + lower + "']"),                              "class*='" + lower + "'"));
        out.add(attempt(By.xpath(multiCaseXpath("div",  "class", name, "//")), "div[class~='"  + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("span", "class", name, "//")), "span[class~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("p",    "class", name, "//")), "p[class~='"    + name + "']"));

        out.add(attempt(By.xpath("//*[normalize-space(text())=" + xpathLiteral(name)  + "]"),           "exact text='" + name  + "'"));
        out.add(attempt(By.xpath("//*[contains(normalize-space(text())," + xpathLiteral(name)  + ")]"), "text contains '" + name  + "'"));
        out.add(attempt(By.xpath("//*[contains(normalize-space(text())," + xpathLiteral(lower) + ")]"), "text contains '" + lower + "'"));
    }

    private static void addBronzeUniversalStrategies(List<LocatorAttempt> out, String name, String lower,
                                                      String scopePrefix) {
        out.add(attempt(By.xpath("//*[contains(@aria-label," + xpathLiteral(name)  + ")]"), "*[aria-label*='" + name  + "']"));
        out.add(attempt(By.xpath("//*[contains(@aria-label," + xpathLiteral(lower) + ")]"), "*[aria-label*='" + lower + "']"));
        out.add(attempt(By.xpath("//*[contains(@title,"      + xpathLiteral(name)  + ")]"), "*[title*='"      + name  + "']"));
        out.add(attempt(By.xpath("//*[contains(@title,"      + xpathLiteral(lower) + ")]"), "*[title*='"      + lower + "']"));
    }

    /**
     * Generates an XPath that checks an attribute against multiple case variants.
     * scopePrefix: "//" for global, "//form/" for parent-scoped (T2-F).
     */
    private static String multiCaseXpath(String tag, String attr, String name, String scopePrefix) {
        if (name == null || name.isEmpty()) return "//" + tag + "[@" + attr + "]";
        String lower = name.toLowerCase();
        String upper = name.toUpperCase();
        String capitalized = Character.toUpperCase(name.charAt(0))
                + (name.length() > 1 ? name.substring(1).toLowerCase() : "");
        String base = scopePrefix.endsWith("/") ? scopePrefix + tag : scopePrefix + "/" + tag;
        return base + "[contains(@" + attr + "," + xpathLiteral(name)        + ")"
                +     " or contains(@" + attr + "," + xpathLiteral(lower)      + ")"
                +     " or contains(@" + attr + "," + xpathLiteral(upper)      + ")"
                +     " or contains(@" + attr + "," + xpathLiteral(capitalized) + ")]";
    }

    /**
     * Generates XPath to find an element by text content with case variants.
     * scopePrefix: "//" for global, "//form/" for parent-scoped (T2-F).
     */
    private static String textContains(String tag, String name, String scopePrefix) {
        String lower = name.toLowerCase();
        String base = scopePrefix.endsWith("/") ? scopePrefix + tag : scopePrefix + "/" + tag;
        return base + "[contains(normalize-space(text())," + xpathLiteral(name)  + ")"
                +     " or contains(normalize-space(text())," + xpathLiteral(lower) + ")]";
    }

    /**
     * Reconstructs a By locator from a locator value string produced by By.toString().
     * "By.id: loginBtn" → By.id("loginBtn"). Returns null if the format is not recognised.
     */
    private static By rebuildLocator(String locatorValue) {
        if (locatorValue == null || locatorValue.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("By\\.([a-zA-Z]+):\\s*(.*)")
                .matcher(locatorValue.trim());
        if (!m.find()) return null;
        String method = m.group(1);
        String value  = m.group(2).trim();
        if (value.isBlank()) return null;
        return switch (method) {
            case "id"          -> By.id(value);
            case "name"        -> By.name(value);
            case "cssSelector" -> By.cssSelector(value);
            case "xpath"       -> By.xpath(value);
            case "className"   -> By.className(value);
            case "tagName"     -> By.tagName(value);
            case "linkText"    -> By.linkText(value);
            case "partialLinkText" -> By.partialLinkText(value);
            default            -> null;
        };
    }

    /**
     * Generates XPath for element as child of a container with matching text.
     * Example: (//*[contains(text(),'Email')]/input)[1]
     */
    private static String childOfTextContainer(String tag, String name) {
        return "(//*[contains(normalize-space(text())," + xpathLiteral(name) + ")]/" + tag + ")[1]";
    }

    /**
     * Generates XPath for the first element of a tag type that follows text with the name.
     * Example: (//*[contains(text(),'Email')]/following::input)[1]
     */
    private static String followingSibling(String tag, String name) {
        return "(//*[contains(normalize-space(text())," + xpathLiteral(name) + ")]/following::" + tag + ")[1]";
    }

    /**
     * Generates XPath for the first element of a tag type that precedes text with the name.
     */
    private static String precedingSibling(String tag, String name) {
        return "(//*[contains(normalize-space(text())," + xpathLiteral(name) + ")]/preceding::" + tag + ")[1]";
    }

    /**
     * Generates XPath for input[type=X][attr contains name].
     */
    private static String inputTypeWithAttr(String type, String attr, String name) {
        String lower = name.toLowerCase();
        return "//input[@type='" + type + "' and (contains(@" + attr + "," + xpathLiteral(name) + ")"
                + " or contains(@" + attr + "," + xpathLiteral(lower) + "))]";
    }

    /**
     * Generates XPath for role-based elements with matching text content.
     */
    private static String roleWithText(String role, String name) {
        String lower = name.toLowerCase();
        return "//*[@role='" + role + "' and (contains(normalize-space(text())," + xpathLiteral(name) + ")"
                + " or contains(normalize-space(text())," + xpathLiteral(lower) + "))]";
    }

    /**
     * Returns an XPath string literal safe for any input. Standard XPath 1.0 has no escape syntax for
     * the quote character — when the input contains both {@code '} and {@code "} it must be split via
     * {@code concat()}. Wrapping every name-concat call through this prevents both injection (a
     * baseline or i18n placeholder containing {@code '} could break out of the predicate and select an
     * attacker-chosen node) and the everyday correctness bug of unescaped apostrophes.
     */
    public static String xpathLiteral(String s) {
        if (s == null) return "''";
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = s.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",\"'\",");
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Maps Ellithium action method names to element categories.
     * These are the REAL method names from Ellithium's interaction classes:
     * ElementActions, SelectActions, MouseActions, JavaScriptActions, MobileActions.
     */
    public static ElementCategory categorizeAction(String actionType) {
        if (actionType == null || actionType.equals("unknown")) return ElementCategory.ANY;

        return switch (actionType) {
            case "sendData", "clearElement",
                 "setElementValueUsingJS",
                 "uploadFile", "uploadMultipleFiles", "uploadFileUsingJS"
                    -> ElementCategory.INPUT;

            case "clickOnElement", "clickOnMultipleElements",
                 "hoverOverElement", "hoverAndClick", "doubleClick", "rightClick",
                 "dragAndDrop", "dragAndDropByOffset",
                 "javascriptClick",
                 "tap", "doubleTap", "longPress", "twoFingerTap"
                    -> ElementCategory.CLICKABLE;

            case "selectDropdownByText", "selectDropdownByValue", "selectDropdownByIndex",
                 "deselectAll", "deselectDropdownByText", "deselectDropdownByValue",
                 "deselectDropdownByIndex", "selectDropdownByTextForMultipleElements"
                    -> ElementCategory.SELECT;

            case "getText", "getTextFromMultipleElements",
                 "getAttributeValue", "getAttributeFromMultipleElements", "getPropertyValue",
                 "getDropdownSelectedOptions",
                 "isTextContains", "isAttributeContains",
                 "isElementPresent", "isElementDisplayed", "isElementEnabled",
                 "isElementSelected", "isElementClickable"
                    -> ElementCategory.READABLE;

            case "scrollIntoView", "scrollToElement",
                 "moveSliderTo", "moveSliderByOffset",
                 "swipe", "scroll", "scrollToElementBySelector",
                 "drag", "pinch", "fling"
                    -> ElementCategory.ANY;

            default -> ElementCategory.ANY;
        };
    }

    private static final String CSS_BATCH_FIND_SCRIPT =
            "return arguments[0].map(function(sel){"
            + " try{ return Array.from(document.querySelectorAll(sel)); }"
            + " catch(e){ return []; }"
            + "});";

    private static void collectHitsBatched(WebDriver driver,
                                            List<LocatorAttempt> gold, double goldW,
                                            List<LocatorAttempt> silver, double silverW,
                                            java.util.IdentityHashMap<WebElement, Double> best) {
        List<String> cssSels   = new ArrayList<>();
        List<Double>  cssWts   = new ArrayList<>();
        List<LocatorAttempt> nonCssGold   = new ArrayList<>();
        List<LocatorAttempt> nonCssSilver = new ArrayList<>();

        for (LocatorAttempt a : gold) {
            String sel = toCssSelector(a.locator);
            if (sel != null) { cssSels.add(sel); cssWts.add(goldW); }
            else nonCssGold.add(a);
        }
        for (LocatorAttempt a : silver) {
            String sel = toCssSelector(a.locator);
            if (sel != null) { cssSels.add(sel); cssWts.add(silverW); }
            else nonCssSilver.add(a);
        }

        if (!cssSels.isEmpty()) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            boolean batchSucceeded = false;
            try {
                Object res = js.executeScript(CSS_BATCH_FIND_SCRIPT,
                        (Object) cssSels.toArray(new String[0]));
                if (res instanceof List<?> rows) {
                    batchSucceeded = true;
                    for (int i = 0; i < rows.size() && i < cssWts.size(); i++) {
                        Object row = rows.get(i);
                        if (!(row instanceof List<?> els)) continue;
                        double w = cssWts.get(i);
                        for (Object o : els) {
                            if (o instanceof WebElement el) {
                                Double prev = best.get(el);
                                if (prev == null || prev < w) best.put(el, w);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (!batchSucceeded) {
                for (int i = 0; i < cssSels.size(); i++) {
                    try {
                        List<WebElement> found = driver.findElements(
                                By.cssSelector(cssSels.get(i)));
                        double w = cssWts.get(i);
                        for (WebElement el : found) {
                            Double prev = best.get(el);
                            if (prev == null || prev < w) best.put(el, w);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (!nonCssGold.isEmpty())   collectHits(driver, nonCssGold,   goldW,   best);
        if (!nonCssSilver.isEmpty()) collectHits(driver, nonCssSilver, silverW, best);
    }

    private static LocatorAttempt attempt(By locator, String description) {
        return new LocatorAttempt(locator, description);
    }

    private static class LocatorAttempt {
        final By locator;
        final String description;

        LocatorAttempt(By locator, String description) {
            this.locator = locator;
            this.description = description;
        }
    }
}
