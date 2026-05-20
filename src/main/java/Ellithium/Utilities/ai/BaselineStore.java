package Ellithium.Utilities.ai;

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

    // ──────────────────────── Capture ────────────────────────

    /**
     * Captures and stores a fingerprint for a successfully-found element.
     * Maintains a ring buffer of the last {@value #MAX_HISTORY} fingerprints per locator key.
     */
    public static void capture(WebDriver driver, By locator, WebElement element) {
        try {
            ensureLoaded();
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
                return mutationMatch;
            }

            // ── Step B: Attribute pre-search (direct targeted lookups) ──
            WebElement attrMatch = tryAttributePreSearch(driver, baseline, history);
            if (attrMatch != null) {
                Ellithium.core.execution.listener.seleniumListener.resumeLogging();
                double score = scoreBestHistory(attrMatch, history);
                acceptHeal(driver, brokenLocator, attrMatch, score, "[TIER 1 - AttrSearch]", null);
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

        ScoredCandidate best = null;

        for (WebElement candidate : candidates) {
            try {
                // Visibility filter — non-visible elements are almost never the right candidate
                if (!candidate.isDisplayed()) continue;

                // Score against ALL history fingerprints, take max (T1-E)
                double score = scoreBestHistory(candidate, history);

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

    /**
     * Scores a candidate against ALL history fingerprints and returns the maximum score.
     */
    private static double scoreBestHistory(WebElement candidate, List<ElementFingerprint> history) {
        double max = 0.0;
        for (ElementFingerprint fp : history) {
            try {
                double s = fp.scoreSimilarity(candidate);
                if (s > max) max = s;
            } catch (Exception ignored) {}
        }
        return max;
    }

    // ──────────────────────── Accept Heal Helper ────────────────────────

    private static void acceptHeal(WebDriver driver, By brokenLocator, WebElement healed,
                                    double score, String tierLabel, By reconstructedLocator) {
        By bestLocator = reconstructedLocator != null
                ? reconstructedLocator
                : ElementFingerprint.reconstructLocator(healed);
        String locatorStr = bestLocator != null ? bestLocator.toString() : brokenLocator.toString();

        // Update baseline with fresh fingerprint
        try {
            if (bestLocator != null) {
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

    private static void saveToDiskAsync() {
        Thread t = new Thread(() -> {
            synchronized (LOCK) {
                try {
                    Path path = Paths.get(BASELINE_FILE);
                    Files.createDirectories(path.getParent());
                    // Serialize as ordered map for readability
                    Map<String, List<ElementFingerprint>> out = new LinkedHashMap<>(baselines);
                    try (Writer writer = new FileWriter(path.toFile())) {
                        GSON.toJson(out, writer);
                    }
                } catch (Exception ignored) {}
            }
        }, "baseline-store-save");
        t.setDaemon(true);
        t.start();
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
