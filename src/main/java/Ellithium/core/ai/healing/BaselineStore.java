package Ellithium.core.ai.healing;

import Ellithium.core.ai.reporting.AIHealingReporter;
import Ellithium.core.ai.HealingTelemetryStore;
import Ellithium.core.ai.scoring.LocatorMutationEngine;
import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.models.HealingResult;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tier 1 — Algorithmic locator healing via stored element fingerprints.
 *
 * <h3>Multi-fingerprint history</h3>
 * Each locator key retains the last {@value #MAX_HISTORY} fingerprints (ring buffer).
 * On heal, the candidate is scored against ALL stored fingerprints; the maximum is used.
 * This handles elements that have oscillated through multiple states across test runs.
 *
 * <h3>Healing cascade within Tier 1</h3>
 * <ol>
 *   <li><b>Mutation pre-pass</b> — O(1) broken-locator mutations via
 *       {@link LocatorMutationEngine} (covers convention renames, attribute swaps)</li>
 *   <li><b>Attribute pre-search</b> — direct lookups on the most stable stored
 *       attributes before any broad DOM scan</li>
 *   <li><b>Tag-narrowed, visibility-filtered DOM scan</b> — scans only elements of
 *       the same tag type, skipping hidden/disabled elements</li>
 * </ol>
 */
public class BaselineStore {

    private static final String BASELINE_FILE = "Test-Output" + File.separator + "healing-baselines.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();
    static final int MAX_HISTORY = 3;

    private static final double PRE_SEARCH_EXACT_THRESHOLD  = 0.85;
    private static final double PRE_SEARCH_FUZZY_THRESHOLD  = 0.80;
    private static final double PRE_SEARCH_NAME_THRESHOLD   = 0.75;

    /**
     * In-memory store: locatorKey → immutable ordered list of up to MAX_HISTORY fingerprints.
     * Newest fingerprint is always LAST in the list.
     * The list itself is replaced atomically via ConcurrentHashMap.compute().
     */
    private static final ConcurrentHashMap<String, List<ElementFingerprint>> baselines =
            new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    // ──────────────────────── Capture ────────────────────────

    /**
     * Captures the genuine element found directly by its locator (ground truth, always trusted).
     */
    public static void capture(WebDriver driver, By locator, WebElement element) {
        capture(driver, locator, element, 1.0, 0);
    }

    /**
     * Captures and stores a fingerprint for an element, gated by heal confidence.
     *
     * <p>A heal whose confidence is below {@code ai.healing.storeThreshold} is used for the
     * current action but NOT persisted — this prevents a low-confidence guess (e.g. a Tier 3
     * cosine of 0.74 picking the wrong heading) from poisoning the baseline store.</p>
     *
     * @param confidence heal confidence 0.0–1.0 (1.0 = element found directly, not healed)
     * @param tier       originating tier (0 = direct find, 1/3/4 = heal tiers) — for logging
     */
    public static void capture(WebDriver driver, By locator, WebElement element,
                               double confidence, int tier) {
        try {
            ensureLoaded();
            if (tier > 0 && confidence < AIConfigLoader.getHealingStoreThreshold()) {
                Reporter.log(String.format(
                        "BaselineStore: Tier %d heal used (confidence=%.2f) but NOT persisted "
                        + "(below store threshold %.2f) — baseline left untouched",
                        tier, confidence, AIConfigLoader.getHealingStoreThreshold()), LogLevel.DEBUG);
                return;
            }
            ElementFingerprint fp = ElementFingerprint.capture(driver, locator, element);
            if (tier > 0 && fp.computeDynamicMax() < 15) {
                Reporter.log(String.format(
                        "BaselineStore: Tier %d heal has tag-only signal (dynamicMax<15) — skipping persist",
                        tier), LogLevel.DEBUG);
                return;
            }
            baselines.compute(fp.getLocatorKey(), (k, existing) -> {
                List<ElementFingerprint> updated = new ArrayList<>();
                if (existing != null && !existing.isEmpty()) {
                    int start = Math.max(0, existing.size() - (MAX_HISTORY - 1));
                    updated.addAll(existing.subList(start, existing.size()));
                }
                updated.add(fp);
                return List.copyOf(updated);
            });
            saveToDiskAsync();
        } catch (Exception e) {
            Reporter.log("BaselineStore: capture failed (non-fatal): " + e.getMessage(), LogLevel.WARN);
        }
    }

    // ──────────────────────── Lookup ────────────────────────

    /**
     * Returns the most recent fingerprint for a locator key, or null if absent.
     */
    public static ElementFingerprint getBaseline(String locatorKey) {
        ensureLoaded();
        List<ElementFingerprint> history = baselines.get(locatorKey);
        if (history == null || history.isEmpty()) return null;
        return history.getLast();
    }

    /**
     * Returns the full fingerprint history (up to MAX_HISTORY entries) for a locator key.
     * Newest last. Returns an empty list if no baseline exists.
     */
    static List<ElementFingerprint> getAllBaselines(String locatorKey) {
        ensureLoaded();
        List<ElementFingerprint> history = baselines.get(locatorKey);
        return history != null ? history : List.of();
    }

    /**
     * Removes a stored baseline entry by its locator key and schedules a disk flush.
     * Use this to prune a stale or deliberately changed locator so it stops being healed
     * to the old element. Returns true if an entry was found and removed.
     */
    static boolean removeKey(String locatorKey) {
        ensureLoaded();
        boolean removed = baselines.remove(locatorKey) != null;
        if (removed) saveToDiskAsync();
        return removed;
    }

    // ──────────────────────── Tier 1 Algorithmic Healing ────────────────────────

    /**
     * Attempts to heal a broken locator using stored baseline fingerprints.
     *
     * <p>Three-step cascade:
     * <ol>
     *   <li>Mutation pre-pass via {@link LocatorMutationEngine}</li>
     *   <li>Attribute-targeted direct lookups (data-testid, aria-label, placeholder, name)</li>
     *   <li>Tag-narrowed, visibility-filtered full DOM scan</li>
     * </ol>
     */
    public static HealOutcome tryAlgorithmicHeal(WebDriver driver, By brokenLocator,
                                                  StackTraceElement[] stackTrace, String actionType) {
        ensureLoaded();
        List<ElementFingerprint> history = baselines.get(brokenLocator.toString());
        ElementFingerprint baseline = (history != null && !history.isEmpty())
                ? history.getLast() : null;

        String category = Ellithium.core.ai.HealingTelemetryStore.categoryForAction(actionType);

        if (baseline == null) {
            Reporter.log("BaselineStore: No baseline for " + brokenLocator
                    + " — skipping Tier 1", LogLevel.DEBUG);
            HealingTelemetryStore.record(1, brokenLocator.toString(), null, 0.0, false, null, category);
            return null;
        }

        Reporter.log("BaselineStore: Baseline found (" + history.size() + " history entries) for "
                + brokenLocator + " — Tier 1 healing", LogLevel.DEBUG);

        boolean switchedFrame = baseline.enterIframeContext(driver);
        if (switchedFrame) {
            Reporter.log("BaselineStore: element is inside iframe — switched frame context for Tier 1 heal",
                    LogLevel.DEBUG);
        }
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            WebElement mutationMatch = LocatorMutationEngine.tryMutations(brokenLocator, driver, baseline);
            if (mutationMatch != null) {
                Ellithium.core.execution.listener.seleniumListener.resumeLogging();
                acceptHeal(driver, brokenLocator, mutationMatch, 0.92, "[TIER 1 - Mutation]", null, stackTrace, category);
                return HealOutcome.of(mutationMatch, 0.92, 1);
            }

            WebElement attrMatch = tryAttributePreSearch(driver, baseline, history);
            if (attrMatch != null) {
                Ellithium.core.execution.listener.seleniumListener.resumeLogging();
                double score = scoreBestHistory(attrMatch, history);
                acceptHeal(driver, brokenLocator, attrMatch, score, "[TIER 1 - AttrSearch]", null, stackTrace, category);
                return HealOutcome.of(attrMatch, score, 1);
            }

            ScoredCandidate best = findBestMatch(driver, baseline, history);

            Ellithium.core.execution.listener.seleniumListener.resumeLogging();

            if (best == null) {
                Reporter.log("BaselineStore: Tier 1 found no candidates in DOM", LogLevel.DEBUG);
                HealingTelemetryStore.record(1, brokenLocator.toString(), null, 0.0, false, null, category);
                return null;
            }

            double acceptBar = AIConfigLoader.getHealingStoreThreshold();
            if (best.score >= acceptBar) {
                Reporter.log("BaselineStore: Tier 1 MATCH — score=" + String.format("%.2f", best.score)
                        + " | " + best.reasoning + " | locator=" + best.reconstructedLocator,
                        LogLevel.INFO_GREEN);
                acceptHeal(driver, brokenLocator, best.element, best.score,
                        "[TIER 1 - Algorithmic] " + best.reasoning, best.reconstructedLocator, stackTrace, category);
                return HealOutcome.of(best.element, best.reconstructedLocator, best.score, 1);
            } else {
                Reporter.log("BaselineStore: Tier 1 best score=" + String.format("%.2f", best.score)
                        + " (below " + String.format("%.2f", acceptBar) + ") — falling through", LogLevel.DEBUG);
                HealingTelemetryStore.record(1, brokenLocator.toString(), null, best.score, false, null, category);
                return null;
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
            if (switchedFrame) {
                try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
            }
        }
    }

    // ──────────────────────── Attribute Pre-Search (T1-C) ────────────────────────

    /**
     * Tries direct attribute lookups using the most stable stored attributes before
     * committing to a full DOM scan. Returns immediately if exactly one element is
     * found and scores >= the lookup threshold against any history fingerprint.
     */
    private static WebElement tryAttributePreSearch(WebDriver driver, ElementFingerprint baseline,
                                                     List<ElementFingerprint> history) {
        // Priority 1: stable test attributes — named + any custom data-* attrs the app uses
        for (String[] pair : namedTestAttrs(baseline)) {
            WebElement found = tryDirectLookup(driver,
                    By.cssSelector("[" + pair[0] + "='" + escapeAttr(pair[1]) + "']"), history, PRE_SEARCH_EXACT_THRESHOLD);
            if (found != null) return found;
        }
        if (baseline.getDataTestId() != null) {
            // contains-match (*=) is intentional: tolerates value-suffix drift (e.g. "submit" still
            // matches "submit-v2") without falling through to a full DOM scan.
            WebElement found = tryDirectLookup(driver,
                    By.cssSelector("[data-testid*='" + baseline.getDataTestId() + "']"), history, PRE_SEARCH_FUZZY_THRESHOLD);
            if (found != null) return found;
        }

        // Adaptive: any custom data-* attribute stored in the fingerprint (exact match)
        java.util.Map<String, String> custom = baseline.getCustomDataAttrs();
        if (custom != null) {
            for (java.util.Map.Entry<String, String> e : custom.entrySet()) {
                WebElement found = tryDirectLookup(driver,
                        By.cssSelector("[" + e.getKey() + "='" + escapeAttr(e.getValue()) + "']"),
                        history, PRE_SEARCH_EXACT_THRESHOLD);
                if (found != null) return found;
            }
        }

        // Priority 2: aria-label
        WebElement found = tryDirectLookup(driver,
                baseline.getAriaLabel() != null
                        ? By.cssSelector("[aria-label='" + escapeAttr(baseline.getAriaLabel()) + "']") : null,
                history, PRE_SEARCH_FUZZY_THRESHOLD);
        if (found != null) return found;

        // Priority 3: placeholder
        found = tryDirectLookup(driver,
                baseline.getPlaceholder() != null
                        ? By.cssSelector("[placeholder='" + escapeAttr(baseline.getPlaceholder()) + "']") : null,
                history, PRE_SEARCH_FUZZY_THRESHOLD);
        if (found != null) return found;

        // Priority 4: name
        return tryDirectLookup(driver,
                baseline.getName() != null ? By.name(baseline.getName()) : null,
                history, PRE_SEARCH_NAME_THRESHOLD);
    }

    private static java.util.List<String[]> namedTestAttrs(ElementFingerprint b) {
        java.util.List<String[]> pairs = new java.util.ArrayList<>();
        if (b.getDataTestId() != null) pairs.add(new String[]{"data-testid", b.getDataTestId()});
        if (b.getDataTest()   != null) pairs.add(new String[]{"data-test",   b.getDataTest()});
        if (b.getDataCy()     != null) pairs.add(new String[]{"data-cy",     b.getDataCy()});
        if (b.getDataQa()     != null) pairs.add(new String[]{"data-qa",     b.getDataQa()});
        return pairs;
    }

    private static WebElement tryDirectLookup(WebDriver driver, By locator,
                                               List<ElementFingerprint> history, double threshold) {
        if (locator == null) return null;
        try {
            List<WebElement> results = driver.findElements(locator);
            if (results.size() == 1) {
                WebElement candidate = results.get(0);
                // Batch-fetch all attributes in ONE executeScript (was D×~17 per-element RTs).
                // Falls back to live getAttribute chain only when JS is unavailable (Appium native).
                java.util.List<java.util.Map<String, Object>> batch =
                        Ellithium.core.ai.dom.CandidateAttributeBatcher.fetch(driver, java.util.List.of(candidate));
                if (batch != null && !batch.isEmpty() && batch.get(0) != null) {
                    java.util.Map<String, Object> attrs = batch.get(0);
                    if (Boolean.FALSE.equals(attrs.get("visible"))) return null;
                    if (scoreBestHistory(attrs, structuralFrom(attrs), history) >= threshold) return candidate;
                } else if (batch != null) {
                    // batch ran but element[0] was stale — retry the locator once for a fresh reference
                    List<WebElement> retried = driver.findElements(locator);
                    if (retried.size() == 1) {
                        WebElement fresh = retried.get(0);
                        java.util.List<java.util.Map<String, Object>> rb =
                                Ellithium.core.ai.dom.CandidateAttributeBatcher.fetch(driver, java.util.List.of(fresh));
                        if (rb != null && !rb.isEmpty() && rb.get(0) != null) {
                            java.util.Map<String, Object> ra = rb.get(0);
                            if (!Boolean.FALSE.equals(ra.get("visible"))
                                    && scoreBestHistory(ra, structuralFrom(ra), history) >= threshold)
                                return fresh;
                        } else if (fresh.isDisplayed() && scoreBestHistory(fresh, history) >= threshold) {
                            return fresh;
                        }
                    }
                } else if (candidate.isDisplayed() && scoreBestHistory(candidate, history) >= threshold) {
                    return candidate;
                }
            }
        } catch (Exception e) {
            Reporter.log("BaselineStore: tryDirectLookup skipped candidate: " + e.getClass().getSimpleName(), LogLevel.DEBUG);
        }
        return null;
    }

    // ──────────────────────── Tag-Narrowed DOM Scan (T1-D) ────────────────────────

    /**
     * Scans DOM for the best-matching element. Uses tag-narrowed candidate collection
     * (when tagName is known), visibility filter, and early termination at score >= 0.90.
     * Scores each candidate against ALL history fingerprints and takes the maximum.
     */
    static ScoredCandidate findBestMatch(WebDriver driver, ElementFingerprint baseline,
                                         List<ElementFingerprint> history) {
        List<WebElement> candidates = collectCandidates(driver, baseline);
        if (candidates.isEmpty()) return null;

        List<Map<String, Object>> attrsBatch =
                Ellithium.core.ai.dom.CandidateAttributeBatcher.fetch(driver, candidates);

        // Batch failed (Appium native / blocked JS): each candidate now costs ~12 WebDriver
        // round-trips, so cap the scan hard to keep the heal bounded.
        int scanLimit = (attrsBatch == null)
                ? Math.min(candidates.size(), T1_FALLBACK_SCAN_LIMIT)
                : candidates.size();

        WebElement bestEl = null;
        Map<String, Object> bestAttrs = null;
        double bestScore = -1.0;

        for (int i = 0; i < scanLimit; i++) {
            WebElement candidate = candidates.get(i);
            try {
                Map<String, Object> attrs = (attrsBatch != null && i < attrsBatch.size()) ? attrsBatch.get(i) : null;
                // Element was stale when the batch JS ran — skip; don't cascade to isDisplayed().
                if (attrsBatch != null && attrs == null) continue;
                if (attrs != null) {
                    if (Boolean.FALSE.equals(attrs.get("visible"))) continue;
                } else {
                    if (!candidate.isDisplayed()) continue;
                }

                double score = (attrs != null)
                        ? scoreBestHistory(attrs, structuralFrom(attrs), history)
                        : scoreBestHistory(candidate, null, history);

                if (bestEl == null || score > bestScore) {
                    bestEl = candidate;
                    bestAttrs = attrs;
                    bestScore = score;
                    if (score >= 0.90) break;
                }
            } catch (Exception e) {
                Reporter.log("BaselineStore: skipped candidate during scoring: " + e.getClass().getSimpleName(), LogLevel.DEBUG);
            }
        }

        if (bestEl == null) return null;
        By locator = HealedLocatorBuilder.build(driver, bestEl, baseline);
        if (locator == null) locator = ElementFingerprint.reconstructLocator(bestEl);
        if (locator == null) return null;
        return new ScoredCandidate(bestEl, locator, bestScore, buildMatchReasoning(baseline, bestAttrs, bestEl));
    }

    private static ElementFingerprint.StructuralContext structuralFrom(Map<String, Object> attrs) {
        if (attrs == null) return null;
        Object pt = attrs.get("parent-tag");
        if (pt == null && attrs.get("child-index") == null) return null;
        int ci = attrs.get("child-index") instanceof Number n ? n.intValue() : -1;
        return new ElementFingerprint.StructuralContext(
                pt != null ? pt.toString() : null, ci,
                asStr(attrs.get("prev-sib")), asStr(attrs.get("next-sib")));
    }

    private static String asStr(Object o) { return o != null ? o.toString() : null; }

    private static double scoreBestHistory(Map<String, Object> attrs,
                                           ElementFingerprint.StructuralContext sc,
                                           List<ElementFingerprint> history) {
        double max = 0.0;
        for (ElementFingerprint fp : history) {
            try {
                double s = fp.scoreSimilarity(attrs, sc);
                if (s > max) max = s;
            } catch (Exception ignored) {}
        }
        return max;
    }

    // Backward-compatible overload for callers that only have a single baseline
    static ScoredCandidate findBestMatch(WebDriver driver, ElementFingerprint baseline) {
        return findBestMatch(driver, baseline, List.of(baseline));
    }

    /**
     * Collects candidate WebElements. Prefers tag-narrowed collection when tagName is known,
     * falling back to the broad interactive-elements selector.
     */
    private static final int T1_HARD_CANDIDATE_LIMIT = 500;
    private static final int T1_FALLBACK_SCAN_LIMIT = 15;

    private static final String SHADOW_CANDIDATE_SELECTOR =
            "input,button,select,textarea,a,label,[role='button'],[role='link'],[role='textbox'],"
            + "[role='checkbox'],[role='radio'],[role='tab'],[role='menuitem'],[data-testid]";

    private static final String SHADOW_DOM_SCRIPT =
            "var sel=arguments[0],lim=arguments[1],out=[];"
            + "function walk(root){"
            + " if(out.length>=lim)return;"
            + " var els=root.querySelectorAll(sel);"
            + " for(var i=0;i<els.length&&out.length<lim;i++) out.push(els[i]);"
            + " var all=root.querySelectorAll('*');"
            + " for(var j=0;j<all.length&&out.length<lim;j++) if(all[j].shadowRoot) walk(all[j].shadowRoot);"
            + "}"
            + "walk(document); return out;";

    private static List<WebElement> collectCandidates(WebDriver driver, ElementFingerprint baseline) {
        java.util.LinkedHashSet<WebElement> seen = new java.util.LinkedHashSet<>();

        if (isNonBlank(baseline.getTagName())) {
            try {
                driver.findElements(By.tagName(baseline.getTagName())).forEach(seen::add);
            } catch (Exception ignored) {}
        }

        if (seen.isEmpty()) {
            try {
                driver.findElements(By.cssSelector(
                        "input, button, select, textarea, a, form, label, "
                        + "[role='button'], [role='link'], [role='textbox'], [role='checkbox'], "
                        + "[role='radio'], [role='tab'], [role='menuitem'], [data-testid]"))
                        .forEach(seen::add);
            } catch (Exception e) {
                try {
                    driver.findElements(By.xpath(
                            "//*[self::input or self::button or self::select or self::textarea "
                            + "or self::a or self::form or self::label]")).forEach(seen::add);
                } catch (Exception ex) {
                    Reporter.log("BaselineStore: Failed to query DOM: " + ex.getMessage(), LogLevel.WARN);
                }
            }
        }

        if (seen.size() < T1_HARD_CANDIDATE_LIMIT && driver instanceof org.openqa.selenium.JavascriptExecutor js) {
            try {
                Object res = js.executeScript(SHADOW_DOM_SCRIPT, SHADOW_CANDIDATE_SELECTOR,
                        T1_HARD_CANDIDATE_LIMIT - seen.size());
                if (res instanceof List<?> rows) {
                    for (Object o : rows) if (o instanceof WebElement w) seen.add(w);
                }
            } catch (Exception ignored) {}
        }

        if (seen.size() > T1_HARD_CANDIDATE_LIMIT) {
            Reporter.log("BaselineStore: capped " + seen.size() + " candidates to "
                    + T1_HARD_CANDIDATE_LIMIT + " (heavy DOM)", LogLevel.DEBUG);
            List<WebElement> list = new ArrayList<>(seen);
            return list.subList(0, T1_HARD_CANDIDATE_LIMIT);
        }
        return new ArrayList<>(seen);
    }

    /** Scores a candidate against ALL history fingerprints (no structural context). */
    private static double scoreBestHistory(WebElement candidate, List<ElementFingerprint> history) {
        return scoreBestHistory(candidate, null, history);
    }

    /**
     * Scores a candidate against ALL history fingerprints and returns the maximum score, optionally
     * using the candidate's structural context (parent/sibling/position) to reward layout matches.
     */
    private static double scoreBestHistory(WebElement candidate,
                                           ElementFingerprint.StructuralContext sc,
                                           List<ElementFingerprint> history) {
        double max = 0.0;
        for (ElementFingerprint fp : history) {
            try {
                double s = fp.scoreSimilarity(candidate, sc);
                if (s > max) max = s;
            } catch (Exception ignored) {}
        }
        return max;
    }

    // ──────────────────────── Accept Heal Helper ────────────────────────

    private static void acceptHeal(WebDriver driver, By brokenLocator, WebElement healed,
                                    double score, String tierLabel, By reconstructedLocator,
                                    StackTraceElement[] stackTrace, String category) {
        By built = HealedLocatorBuilder.build(driver, healed, null);
        By bestLocator = built != null ? built
                : (reconstructedLocator != null ? reconstructedLocator
                        : ElementFingerprint.reconstructLocator(healed));
        String locatorStr = bestLocator != null ? bestLocator.toString() : brokenLocator.toString();

        // Update baseline with fresh fingerprint — only when the heal clears the store threshold,
        // so a low-confidence Tier 1 match cannot persist a wrong fingerprint for future runs.
        try {
            if (bestLocator != null && score >= AIConfigLoader.getHealingStoreThreshold()) {
                // Key the fingerprint by the BROKEN locator (the lookup key) so fp.locatorKey
                // matches the map key — previously captured under bestLocator causing key mismatch.
                ElementFingerprint updatedFp = ElementFingerprint.capture(driver, brokenLocator, healed);
                if (updatedFp.computeDynamicMax() >= 15) {
                    baselines.compute(brokenLocator.toString(), (k, existing) -> {
                        List<ElementFingerprint> updated = new ArrayList<>();
                        if (existing != null && !existing.isEmpty()) {
                            int start = Math.max(0, existing.size() - (MAX_HISTORY - 1));
                            updated.addAll(existing.subList(start, existing.size()));
                        }
                        updated.add(updatedFp);
                        return List.copyOf(updated);
                    });
                    saveToDiskAsync();
                }
            }
        } catch (Exception ignored) {}

        AIHealingReporter.queueChange(
                "algorithmic-baseline", brokenLocator.toString(),
                new HealingResult(locatorStr, score, tierLabel),
                null, null, null, 0);

        HealingTelemetryStore.record(1, brokenLocator.toString(), locatorStr, score, true, null, category);

        if (stackTrace != null && bestLocator != null) {
            AISelfHealer.queueSourcePatch(brokenLocator, bestLocator, stackTrace, score, 1);
        }
    }

    // ──────────────────────── Reasoning ────────────────────────

    private static String buildMatchReasoning(ElementFingerprint baseline,
                                               Map<String, Object> attrs, WebElement candidate) {
        StringBuilder sb = new StringBuilder("Matched by:");
        try {
            String cid    = attrs != null ? asStr(attrs.get("id"))          : candidate.getAttribute("id");
            String cn     = attrs != null ? asStr(attrs.get("name"))        : candidate.getAttribute("name");
            String cal    = attrs != null ? asStr(attrs.get("aria-label"))  : candidate.getAttribute("aria-label");
            String cdt    = attrs != null ? asStr(attrs.get("data-testid")) : candidate.getAttribute("data-testid");
            String cph    = attrs != null ? asStr(attrs.get("placeholder")) : candidate.getAttribute("placeholder");
            String ct     = attrs != null ? asStr(attrs.get("tag"))         : candidate.getTagName();
            String txt    = attrs != null ? asStr(attrs.get("text"))        : candidate.getText();
            if (cid  != null && cid.equals(baseline.getId()))               sb.append(" id='").append(cid).append("'");
            if (cn   != null && cn.equals(baseline.getName()))              sb.append(" name='").append(cn).append("'");
            if (cal  != null && cal.equals(baseline.getAriaLabel()))        sb.append(" aria-label='").append(cal).append("'");
            if (cdt  != null && cdt.equals(baseline.getDataTestId()))       sb.append(" data-testid='").append(cdt).append("'");
            if (cph  != null && cph.equals(baseline.getPlaceholder()))      sb.append(" placeholder='").append(cph).append("'");
            if (ct   != null && ct.equalsIgnoreCase(baseline.getTagName())) sb.append(" tag='").append(ct).append("'");
            if (txt  != null && baseline.getText() != null && txt.contains(baseline.getText())) sb.append(" text(partial)");
        } catch (Exception ignored) {}
        return sb.toString();
    }

    // ──────────────────────── Result Model ────────────────────────

    static class ScoredCandidate {
        final WebElement element;
        final By reconstructedLocator;
        final double score;
        final String reasoning;

        ScoredCandidate(WebElement element, By reconstructedLocator, double score, String reasoning) {
            this.element = element;
            this.reconstructedLocator = reconstructedLocator;
            this.score = score;
            this.reasoning = reasoning;
        }
    }

    // ──────────────────────── Persistence ────────────────────────

    private static void ensureLoaded() {
        if (loaded) return;
        synchronized (LOCK) {
            if (loaded) return;
            loadFromDisk();
            loaded = true;
        }
    }

    public static void preWarmAsync() {
        if (loaded) return;
        Thread.ofVirtual().name("ellithium-baseline-prewarm").start(BaselineStore::ensureLoaded);
    }

    private static List<ElementFingerprint> pruneStale(List<ElementFingerprint> fps) {
        int ttlDays = AIConfigLoader.getBaselineTtlDays();
        if (ttlDays <= 0) return fps;   // 0/negative disables expiry
        long cutoff = System.currentTimeMillis() - ttlDays * 86_400_000L;
        List<ElementFingerprint> fresh = new ArrayList<>(fps.size());
        for (ElementFingerprint fp : fps) {
            if (fp.getLastSeenEpoch() <= 0 || fp.getLastSeenEpoch() >= cutoff) fresh.add(fp);
        }
        return fresh;
    }

    private static void loadFromDisk() {
        Path path = Paths.get(BASELINE_FILE);
        if (!Files.exists(path)) return;
        try (Reader reader = new FileReader(path.toFile())) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root.isJsonArray()) {
                // ── Backward compat: old flat List<ElementFingerprint> format ──
                Type listType = new TypeToken<List<ElementFingerprint>>() {}.getType();
                List<ElementFingerprint> list = GSON.fromJson(root, listType);
                if (list != null) {
                    for (ElementFingerprint fp : list) {
                        if (fp.getLocatorKey() != null) {
                            baselines.put(fp.getLocatorKey(), List.of(fp));
                        }
                    }
                    Reporter.log("BaselineStore: Loaded " + list.size() + " baselines", LogLevel.DEBUG);
                }
            } else if (root.isJsonObject()) {
                // ── Current format: Map<String, List<ElementFingerprint>> ──
                Type mapType = new TypeToken<Map<String, List<ElementFingerprint>>>() {}.getType();
                Map<String, List<ElementFingerprint>> map = GSON.fromJson(root, mapType);
                if (map != null) {
                    int evicted = 0;
                    for (Map.Entry<String, List<ElementFingerprint>> entry : map.entrySet()) {
                        if (entry.getKey() == null || entry.getValue() == null) continue;
                        List<ElementFingerprint> fresh = pruneStale(entry.getValue());
                        evicted += entry.getValue().size() - fresh.size();
                        if (fresh.isEmpty()) continue;
                        baselines.put(entry.getKey(), List.copyOf(fresh));
                    }
                    Reporter.log("BaselineStore: " + baselines.size() + " locators loaded"
                            + (evicted > 0 ? " (" + evicted + " expired pruned)" : ""), LogLevel.INFO_BLUE);
                }
            }
        } catch (Exception e) {
            Reporter.log("BaselineStore: Failed to load baselines (non-fatal): " + e.getMessage(), LogLevel.WARN);
        }
        pruneByCount();
    }

    private static void pruneByCount() {
        int max = AIConfigLoader.getBaselineMaxLocators();
        if (max <= 0 || baselines.size() <= max) return;
        int toEvict = baselines.size() - max;
        baselines.entrySet().stream()
                .sorted(java.util.Comparator.comparingLong(e ->
                        e.getValue().stream()
                                .mapToLong(ElementFingerprint::getLastSeenEpoch)
                                .max().orElse(0L)))
                .limit(toEvict)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(baselines::remove);
        Reporter.log("BaselineStore: count-pruned " + toEvict + " oldest locators (max=" + max + ")", LogLevel.DEBUG);
    }

    // Single shared daemon writer (was: a NEW Thread per capture → hundreds of threads + O(N^2)
    // re-serialization across a suite). Captures now schedule one debounced, coalesced write.
    private static volatile java.util.concurrent.ScheduledExecutorService SAVE_EXECUTOR =
            newSaveExecutor();
    private static final java.util.concurrent.atomic.AtomicBoolean saveScheduled =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final long SAVE_DEBOUNCE_MS = 1000;

    private static java.util.concurrent.ScheduledExecutorService newSaveExecutor() {
        return java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r ->
                Thread.ofPlatform().daemon(true).name("baseline-store-save").unstarted(r));
    }

    static {
        Runtime.getRuntime().addShutdownHook(
                Thread.ofPlatform().daemon(false).name("baseline-store-shutdown").unstarted(
                        () -> SAVE_EXECUTOR.shutdown()));
    }

    /**
     * Schedules a single coalesced disk write. Many capture() calls within the debounce window
     * collapse into ONE serialization on a shared daemon thread (final state is always persisted by
     * {@link #flush()} at suite end). Replaces the previous thread-per-capture full re-serialization.
     */
    private static void saveToDiskAsync() {
        if (saveScheduled.compareAndSet(false, true)) {
            try {
                java.util.concurrent.ScheduledExecutorService exec = SAVE_EXECUTOR;
                if (exec.isShutdown()) {
                    synchronized (LOCK) {
                        if (SAVE_EXECUTOR.isShutdown()) {
                            SAVE_EXECUTOR = newSaveExecutor();
                        }
                        exec = SAVE_EXECUTOR;
                    }
                }
                exec.schedule(() -> {
                    saveScheduled.set(false);
                    writeToDisk();
                }, SAVE_DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                saveScheduled.set(false);
            }
        }
    }

    /** Serializes the baseline map to disk under LOCK. Shared by the debounced saver and flush(). */
    private static void writeToDisk() {
        synchronized (LOCK) {
            try {
                persist(new LinkedHashMap<>(baselines));
            } catch (Exception ignored) {}
        }
    }

    private static void persist(Map<String, List<ElementFingerprint>> out) throws IOException {
        Path path = Paths.get(BASELINE_FILE);
        Files.createDirectories(path.getParent());
        Path lockPath = path.getParent().resolve("healing-baselines.lock");
        try (java.nio.channels.FileChannel lockChannel = java.nio.channels.FileChannel.open(lockPath,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE)) {
            lockChannel.lock();
            Path tmp = Files.createTempFile(path.getParent(), "healing-baselines", ".tmp");
            try {
                try (Writer writer = new FileWriter(tmp.toFile())) {
                    GSON.toJson(out, writer);
                }
                try {
                    Files.move(tmp, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
    }

    public static void flush() {
        synchronized (LOCK) {
            try {
                persist(new LinkedHashMap<>(baselines));
                Reporter.log("BaselineStore: flushed " + baselines.size() + " locators", LogLevel.DEBUG);
            } catch (Exception e) {
                Reporter.log("BaselineStore: Failed to flush: " + e.getMessage(), LogLevel.ERROR);
            }
            SAVE_EXECUTOR.shutdown();
            saveScheduled.set(false);
        }
    }

    public static void clear() {
        baselines.clear();
        loaded = false;
    }

    // ──────────────────────── Helpers ────────────────────────

    private static boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Escapes a value for use inside a CSS attribute selector (e.g. {@code [attr='value']}).
     * Handles {@code '}, {@code "}, {@code \}, {@code ]}, and control characters — preventing
     * selector structure alteration when attribute values come from live (potentially
     * attacker-influenced) DOM content.
     */
    static String escapeAttr(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length() + 4);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\'' -> sb.append("\\'");
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case ']'  -> sb.append("\\]");
                default   -> { if (c >= 0x20) sb.append(c); } // strip control chars
            }
        }
        return sb.toString();
    }

}
  