package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.models.ElementFingerprint;
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

    /**
     * In-memory store: locatorKey → immutable ordered list of up to MAX_HISTORY fingerprints.
     * Newest fingerprint is always LAST in the list.
     * The list itself is replaced atomically via ConcurrentHashMap.compute().
     */
    private static final ConcurrentHashMap<String, List<ElementFingerprint>> baselines =
            new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    /**
     * Per-thread score of the most recent Tier 1 heal. Set immediately before
     * {@link #tryAlgorithmicHeal} returns a non-null element and read by the caller
     * to attach heal provenance (confidence) to the gated capture / source patch.
     */
    private static final ThreadLocal<Double> LAST_HEAL_SCORE = ThreadLocal.withInitial(() -> 0.0);

    /** Score (0.0–1.0) of the most recent Tier 1 heal on this thread. */
    public static double getLastHealScore() { return LAST_HEAL_SCORE.get(); }

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
        return history.get(history.size() - 1);
    }

    /**
     * Returns the full fingerprint history (up to MAX_HISTORY entries) for a locator key.
     * Newest last. Returns an empty list if no baseline exists.
     */
    public static List<ElementFingerprint> getAllBaselines(String locatorKey) {
        ensureLoaded();
        List<ElementFingerprint> history = baselines.get(locatorKey);
        return history != null ? history : List.of();
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
    public static WebElement tryAlgorithmicHeal(WebDriver driver, By brokenLocator) {
        LAST_HEAL_SCORE.set(0.0);   // reset per attempt — never leak a prior heal's score to the gate
        ensureLoaded();
        List<ElementFingerprint> history = baselines.get(brokenLocator.toString());
        ElementFingerprint baseline = (history != null && !history.isEmpty())
                ? history.get(history.size() - 1) : null;

        if (baseline == null) {
            Reporter.log("BaselineStore: No baseline for " + brokenLocator
                    + " — skipping Tier 1", LogLevel.DEBUG);
            return null;
        }

        Reporter.log("BaselineStore: Baseline found (" + history.size() + " history entries) for "
                + brokenLocator + " — Tier 1 healing", LogLevel.DEBUG);

        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            // ── Step A: Mutation pre-pass (O(1), no DOM scan) ──
            WebElement mutationMatch = LocatorMutationEngine.tryMutations(brokenLocator, driver, baseline);
            if (mutationMatch != null) {
                Ellithium.core.execution.listener.seleniumListener.resumeLogging();
                acceptHeal(driver, brokenLocator, mutationMatch, 0.92, "[TIER 1 - Mutation]", null);
                LAST_HEAL_SCORE.set(0.92);
                return mutationMatch;
            }

            // ── Step B: Attribute pre-search (direct targeted lookups) ──
            WebElement attrMatch = tryAttributePreSearch(driver, baseline, history);
            if (attrMatch != null) {
                Ellithium.core.execution.listener.seleniumListener.resumeLogging();
                double score = scoreBestHistory(attrMatch, history);
                acceptHeal(driver, brokenLocator, attrMatch, score, "[TIER 1 - AttrSearch]", null);
                LAST_HEAL_SCORE.set(score);
                return attrMatch;
            }

            // ── Step C: Tag-narrowed DOM scan with visibility filter ──
            ScoredCandidate best = findBestMatch(driver, baseline, history);

            Ellithium.core.execution.listener.seleniumListener.resumeLogging();

            if (best == null) {
                Reporter.log("BaselineStore: Tier 1 found no candidates in DOM", LogLevel.DEBUG);
                HealingTelemetryStore.record(1, brokenLocator.toString(), null, 0.0, false);
                return null;
            }

            if (best.score >= 0.60) {
                Reporter.log("BaselineStore: Tier 1 MATCH — score=" + String.format("%.2f", best.score)
                        + " | " + best.reasoning + " | locator=" + best.reconstructedLocator,
                        LogLevel.INFO_GREEN);
                acceptHeal(driver, brokenLocator, best.element, best.score,
                        "[TIER 1 - Algorithmic] " + best.reasoning, best.reconstructedLocator);
                LAST_HEAL_SCORE.set(best.score);
                return best.element;
            } else {
                Reporter.log("BaselineStore: Tier 1 best score=" + String.format("%.2f", best.score)
                        + " (below 0.60) — falling through to Tier 2", LogLevel.DEBUG);
                HealingTelemetryStore.record(1, brokenLocator.toString(), null, best.score, false);
                return null;
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
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
        // Priority 1: data-testid (most stable)
        WebElement found = tryDirectLookup(driver,
                baseline.getDataTestId() != null
                        ? By.cssSelector("[data-testid='" + baseline.getDataTestId() + "']") : null,
                history, 0.85);
        if (found != null) return found;

        found = tryDirectLookup(driver,
                baseline.getDataTestId() != null
                        ? By.cssSelector("[data-testid*='" + baseline.getDataTestId() + "']") : null,
                history, 0.80);
        if (found != null) return found;

        // Priority 2: aria-label
        found = tryDirectLookup(driver,
                baseline.getAriaLabel() != null
                        ? By.cssSelector("[aria-label='" + escapeAttr(baseline.getAriaLabel()) + "']") : null,
                history, 0.80);
        if (found != null) return found;

        // Priority 3: placeholder (inputs only)
        found = tryDirectLookup(driver,
                baseline.getPlaceholder() != null
                        ? By.cssSelector("[placeholder='" + escapeAttr(baseline.getPlaceholder()) + "']") : null,
                history, 0.80);
        if (found != null) return found;

        // Priority 4: name
        found = tryDirectLookup(driver,
                baseline.getName() != null ? By.name(baseline.getName()) : null,
                history, 0.75);
        return found;
    }

    private static WebElement tryDirectLookup(WebDriver driver, By locator,
                                               List<ElementFingerprint> history, double threshold) {
        if (locator == null) return null;
        try {
            List<WebElement> results = driver.findElements(locator);
            if (results.size() == 1) {
                WebElement candidate = results.get(0);
                if (candidate.isDisplayed() && scoreBestHistory(candidate, history) >= threshold) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ──────────────────────── Tag-Narrowed DOM Scan (T1-D) ────────────────────────

    /**
     * Scans DOM for the best-matching element. Uses tag-narrowed candidate collection
     * (when tagName is known), visibility filter, and early termination at score >= 0.90.
     * Scores each candidate against ALL history fingerprints and takes the maximum.
     */
    public static ScoredCandidate findBestMatch(WebDriver driver, ElementFingerprint baseline,
                                                List<ElementFingerprint> history) {
        List<WebElement> candidates = collectCandidates(driver, baseline);
        if (candidates.isEmpty()) return null;

        // Batch-fetch structural context (parent/sibling/position) for ALL candidates in ONE JS
        // round-trip, so structural scoring is cheap and layout-stable matches get boosted.
        List<ElementFingerprint.StructuralContext> structural = fetchStructuralContexts(driver, candidates);

        ScoredCandidate best = null;

        for (int i = 0; i < candidates.size(); i++) {
            WebElement candidate = candidates.get(i);
            try {
                // Visibility filter — non-visible elements are almost never the right candidate
                if (!candidate.isDisplayed()) continue;

                // Score against ALL history fingerprints, take max (T1-E), with structural context
                double score = scoreBestHistory(candidate, structural.get(i), history);

                if (best == null || score > best.score) {
                    By locator = ElementFingerprint.reconstructLocator(candidate);
                    if (locator != null) {
                        String reasoning = buildMatchReasoning(baseline, candidate);
                        best = new ScoredCandidate(candidate, locator, score, reasoning);

                        // Early termination — no point scanning further
                        if (score >= 0.90) break;
                    }
                }
            } catch (Exception ignored) {
                // Skip stale/detached elements
            }
        }

        return best;
    }

    // Backward-compatible overload for callers that only have a single baseline
    public static ScoredCandidate findBestMatch(WebDriver driver, ElementFingerprint baseline) {
        return findBestMatch(driver, baseline, List.of(baseline));
    }

    /**
     * Collects candidate WebElements. Prefers tag-narrowed collection when tagName is known,
     * falling back to the broad interactive-elements selector.
     */
    private static List<WebElement> collectCandidates(WebDriver driver, ElementFingerprint baseline) {
        // Tag-narrowed first (T1-D): 10-30x fewer candidates on complex pages
        if (isNonBlank(baseline.getTagName())) {
            try {
                List<WebElement> tagCandidates = driver.findElements(By.tagName(baseline.getTagName()));
                if (!tagCandidates.isEmpty()) return tagCandidates;
            } catch (Exception ignored) {}
        }

        // Broad interactive-element fallback
        try {
            return driver.findElements(By.cssSelector(
                    "input, button, select, textarea, a, form, label, "
                    + "[role='button'], [role='link'], [role='textbox'], [role='checkbox'], "
                    + "[role='radio'], [role='tab'], [role='menuitem'], [data-testid]"));
        } catch (Exception e) {
            try {
                return driver.findElements(By.xpath(
                        "//*[self::input or self::button or self::select or self::textarea "
                        + "or self::a or self::form or self::label]"));
            } catch (Exception ex) {
                Reporter.log("BaselineStore: Failed to query DOM: " + ex.getMessage(), LogLevel.WARN);
                return List.of();
            }
        }
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

    /**
     * Fetches parent tag, child index, and previous/next sibling tags for every candidate in a
     * SINGLE {@code executeScript} round-trip (one network call regardless of candidate count).
     * Returns a list aligned by index with {@code candidates}; entries are {@code null} when the
     * driver has no JS (e.g. Appium native) or the call fails — structural scoring is then skipped.
     */
    private static List<ElementFingerprint.StructuralContext> fetchStructuralContexts(
            WebDriver driver, List<WebElement> candidates) {
        List<ElementFingerprint.StructuralContext> out = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) out.add(null);
        if (!(driver instanceof org.openqa.selenium.JavascriptExecutor) || candidates.isEmpty()) return out;
        try {
            Object res = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "return arguments[0].map(function(el){"
                            + " if(!el) return null;"
                            + " var p=el.parentElement,"
                            + "     prev=el.previousElementSibling, next=el.nextElementSibling;"
                            + " return [p?p.tagName.toLowerCase():null,"
                            + "  p?Array.prototype.indexOf.call(p.children,el):-1,"
                            + "  prev?prev.tagName.toLowerCase():null,"
                            + "  next?next.tagName.toLowerCase():null];"
                            + "});", candidates);
            if (res instanceof List<?> rows) {
                for (int i = 0; i < rows.size() && i < out.size(); i++) {
                    if (rows.get(i) instanceof List<?> r && r.size() == 4) {
                        String pt = r.get(0) != null ? r.get(0).toString() : null;
                        int    ci = r.get(1) instanceof Number n ? n.intValue() : -1;
                        String ps = r.get(2) != null ? r.get(2).toString() : null;
                        String ns = r.get(3) != null ? r.get(3).toString() : null;
                        out.set(i, new ElementFingerprint.StructuralContext(pt, ci, ps, ns));
                    }
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    // ──────────────────────── Accept Heal Helper ────────────────────────

    private static void acceptHeal(WebDriver driver, By brokenLocator, WebElement healed,
                                    double score, String tierLabel, By reconstructedLocator) {
        By bestLocator = reconstructedLocator != null
                ? reconstructedLocator
                : ElementFingerprint.reconstructLocator(healed);
        String locatorStr = bestLocator != null ? bestLocator.toString() : brokenLocator.toString();

        // Update baseline with fresh fingerprint — only when the heal clears the store threshold,
        // so a low-confidence Tier 1 match cannot persist a wrong fingerprint for future runs.
        try {
            if (bestLocator != null && score >= AIConfigLoader.getHealingStoreThreshold()) {
                ElementFingerprint updatedFp = ElementFingerprint.capture(driver, bestLocator, healed);
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
        } catch (Exception ignored) {}

        AIHealingReporter.queueChange(
                "algorithmic-baseline", brokenLocator.toString(),
                new Ellithium.Utilities.ai.models.HealingResult(locatorStr, score, tierLabel),
                null, null, null, 0);

        HealingTelemetryStore.record(1, brokenLocator.toString(), locatorStr, score, true);
    }

    // ──────────────────────── Reasoning ────────────────────────

    private static String buildMatchReasoning(ElementFingerprint baseline, WebElement candidate) {
        StringBuilder sb = new StringBuilder("Matched by:");
        try {
            String cid = candidate.getAttribute("id");
            if (cid != null && cid.equals(baseline.getId())) sb.append(" id='").append(cid).append("'");
            String cn = candidate.getAttribute("name");
            if (cn != null && cn.equals(baseline.getName())) sb.append(" name='").append(cn).append("'");
            String cal = candidate.getAttribute("aria-label");
            if (cal != null && cal.equals(baseline.getAriaLabel())) sb.append(" aria-label='").append(cal).append("'");
            String cdt = candidate.getAttribute("data-testid");
            if (cdt != null && cdt.equals(baseline.getDataTestId())) sb.append(" data-testid='").append(cdt).append("'");
            String cph = candidate.getAttribute("placeholder");
            if (cph != null && cph.equals(baseline.getPlaceholder())) sb.append(" placeholder='").append(cph).append("'");
            String ct = candidate.getTagName();
            if (ct != null && ct.equalsIgnoreCase(baseline.getTagName())) sb.append(" tag='").append(ct).append("'");
            String txt = candidate.getText();
            if (txt != null && baseline.getText() != null && txt.contains(baseline.getText())) sb.append(" text(partial)");
        } catch (Exception ignored) {}
        return sb.toString();
    }

    // ──────────────────────── Result Model ────────────────────────

    public static class ScoredCandidate {
        public final WebElement element;
        public final By reconstructedLocator;
        public final double score;
        public final String reasoning;

        public ScoredCandidate(WebElement element, By reconstructedLocator, double score, String reasoning) {
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
                    Reporter.log("BaselineStore: Loaded " + list.size()
                            + " baselines (legacy format) from disk", LogLevel.INFO_BLUE);
                }
            } else if (root.isJsonObject()) {
                // ── Current format: Map<String, List<ElementFingerprint>> ──
                Type mapType = new TypeToken<Map<String, List<ElementFingerprint>>>() {}.getType();
                Map<String, List<ElementFingerprint>> map = GSON.fromJson(root, mapType);
                if (map != null) {
                    int total = 0;
                    for (Map.Entry<String, List<ElementFingerprint>> entry : map.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            baselines.put(entry.getKey(), List.copyOf(entry.getValue()));
                            total += entry.getValue().size();
                        }
                    }
                    Reporter.log("BaselineStore: Loaded " + baselines.size()
                            + " locators (" + total + " fingerprints) from disk", LogLevel.INFO_BLUE);
                }
            }
        } catch (Exception e) {
            Reporter.log("BaselineStore: Failed to load baselines (non-fatal): " + e.getMessage(), LogLevel.WARN);
        }
    }

    // Single shared daemon writer (was: a NEW Thread per capture → hundreds of threads + O(N^2)
    // re-serialization across a suite). Captures now schedule one debounced, coalesced write.
    private static final java.util.concurrent.ScheduledExecutorService SAVE_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "baseline-store-save");
                t.setDaemon(true);
                return t;
            });
    private static final java.util.concurrent.atomic.AtomicBoolean saveScheduled =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final long SAVE_DEBOUNCE_MS = 1000;

    /**
     * Schedules a single coalesced disk write. Many capture() calls within the debounce window
     * collapse into ONE serialization on a shared daemon thread (final state is always persisted by
     * {@link #flush()} at suite end). Replaces the previous thread-per-capture full re-serialization.
     */
    private static void saveToDiskAsync() {
        if (saveScheduled.compareAndSet(false, true)) {
            try {
                SAVE_EXECUTOR.schedule(() -> {
                    saveScheduled.set(false);
                    writeToDisk();
                }, SAVE_DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                saveScheduled.set(false);   // executor rejected (shutdown) — leave to flush()
            }
        }
    }

    /** Serializes the baseline map to disk under LOCK. Shared by the debounced saver and flush(). */
    private static void writeToDisk() {
        synchronized (LOCK) {
            try {
                Path path = Paths.get(BASELINE_FILE);
                Files.createDirectories(path.getParent());
                Map<String, List<ElementFingerprint>> out = new LinkedHashMap<>(baselines);
                try (Writer writer = new FileWriter(path.toFile())) {
                    GSON.toJson(out, writer);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void flush() {
        synchronized (LOCK) {
            try {
                Path path = Paths.get(BASELINE_FILE);
                Files.createDirectories(path.getParent());
                Map<String, List<ElementFingerprint>> out = new LinkedHashMap<>(baselines);
                try (Writer writer = new FileWriter(path.toFile())) {
                    GSON.toJson(out, writer);
                }
                int total = baselines.values().stream().mapToInt(List::size).sum();
                Reporter.log("BaselineStore: Flushed " + baselines.size()
                        + " locators (" + total + " fingerprints) to disk", LogLevel.INFO_GREEN);
            } catch (Exception e) {
                Reporter.log("BaselineStore: Failed to flush: " + e.getMessage(), LogLevel.ERROR);
            }
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

    private static String escapeAttr(String value) {
        if (value == null) return "";
        return value.replace("'", "\\'");
    }
}
