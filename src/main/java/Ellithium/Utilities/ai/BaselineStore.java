package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.models.ElementFingerprint;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages persistence and lookup of {@link ElementFingerprint} baselines.
 *
 * <p><b>Tier 1 Healing:</b> When a locator fails, this store is checked FIRST.
 * If a baseline fingerprint exists for the broken locator, it scans the current DOM
 * for the best-matching element using a weighted attribute scoring algorithm.
 * This is instant, free, and deterministic — no LLM call needed.</p>
 *
 * <p><b>Persistence:</b> Baselines are stored in {@code Test-Output/healing-baselines.json}.
 * The file is loaded lazily on first access and saved after every capture.</p>
 *
 * <p>Thread-safe: All operations are synchronized on an internal lock.</p>
 *
 * <p>Compatible with Selenium WebDriver and Appium (Android/iOS).</p>
 */
public class BaselineStore {

    private static final String BASELINE_FILE = "Test-Output" + File.separator + "healing-baselines.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();

    /** In-memory store: locatorKey → fingerprint. Loaded lazily from disk. */
    private static final ConcurrentHashMap<String, ElementFingerprint> baselines = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    // ──────────────────────── Capture ────────────────────────

    /**
     * Captures and stores a fingerprint for a successfully-found element.
     * Called silently after every successful {@code findWebElement()} — zero overhead
     * since fingerprinting only reads attributes (no DOM traversal).
     *
     * @param driver  The WebDriver (Selenium or Appium)
     * @param locator The By locator that successfully found the element
     * @param element The found WebElement
     */
    public static void capture(WebDriver driver, By locator, WebElement element) {
        try {
            ensureLoaded();
            ElementFingerprint fp = ElementFingerprint.capture(driver, locator, element);
            baselines.put(fp.getLocatorKey(), fp);
            saveToDiskAsync();
        } catch (Exception e) {
            // Fingerprinting must NEVER crash the test — silently swallow
            Reporter.log("BaselineStore: capture failed (non-fatal): " + e.getMessage(), LogLevel.WARN);
        }
    }

    // ──────────────────────── Lookup ────────────────────────

    /**
     * Retrieves the stored fingerprint for a given locator key.
     *
     * @param locatorKey The {@code By.toString()} of the broken locator
     * @return The stored fingerprint, or null if no baseline exists
     */
    public static ElementFingerprint getBaseline(String locatorKey) {
        ensureLoaded();
        return baselines.get(locatorKey);
    }

    // ──────────────────────── Tier 1 Algorithmic Healing ────────────────────────

    /**
     * Attempts to heal a broken locator using stored baseline fingerprints
     * and weighted attribute scoring against the current DOM.
     *
     * <p>This is the Tier 1 healing path — fast, free, deterministic.
     * Called BEFORE the LLM-based Tier 2.</p>
     *
     * @param driver        The WebDriver
     * @param brokenLocator The locator that failed
     * @return The healed WebElement, or null if no confident match was found
     */
    public static WebElement tryAlgorithmicHeal(WebDriver driver, By brokenLocator) {
        ensureLoaded();
        ElementFingerprint baseline = baselines.get(brokenLocator.toString());
        if (baseline == null) {
            Reporter.log("BaselineStore: No baseline found for " + brokenLocator
                    + " — skipping Tier 1 (algorithmic healing)", LogLevel.DEBUG);
            return null;
        }

        Reporter.log("BaselineStore: Baseline found for " + brokenLocator
                + " — attempting Tier 1 algorithmic match", LogLevel.DEBUG);

        // Suppress listener logging during DOM scanning — prevents 30+ attribute logs per element
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        ScoredCandidate best;
        try {
            best = findBestMatch(driver, baseline);
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }

        if (best == null) {
            Reporter.log("BaselineStore: Tier 1 found no candidates in the DOM", LogLevel.DEBUG);
            return null;
        }

        if (best.score >= 0.60) {
            Reporter.log("BaselineStore: Tier 1 MATCH — score=" + String.format("%.2f", best.score)
                    + " | " + best.reasoning + " | locator=" + best.reconstructedLocator,
                    LogLevel.INFO_GREEN);

            // Update the baseline with the new successful fingerprint
            try {
                ElementFingerprint updatedFp = ElementFingerprint.capture(driver, best.reconstructedLocator, best.element);
                baselines.put(brokenLocator.toString(), updatedFp);
                saveToDiskAsync();
            } catch (Exception ignored) {}

            // Queue for healing report
            AIHealingReporter.queueChange(
                    "algorithmic-baseline", brokenLocator.toString(),
                    new Ellithium.Utilities.ai.models.HealingResult(
                            best.reconstructedLocator.toString(), best.score,
                            "[TIER 1 - Algorithmic] " + best.reasoning),
                    null, null, null, 0);

            return best.element;
        } else {
            Reporter.log("BaselineStore: Tier 1 best score=" + String.format("%.2f", best.score)
                    + " (below 0.60 threshold) — falling through to Tier 1.5",
                    LogLevel.DEBUG);
            return null;
        }
    }

    // ──────────────────────── Algorithmic Matching ────────────────────────

    /**
     * Scans the current DOM for interactive elements and scores each against
     * the stored baseline fingerprint.
     *
     * @param driver   The WebDriver
     * @param baseline The stored fingerprint of the element we're looking for
     * @return The highest-scoring candidate, or null if no candidates found
     */
    public static ScoredCandidate findBestMatch(WebDriver driver, ElementFingerprint baseline) {
        List<WebElement> candidates;
        try {
            // Get all interactive elements — works on both Selenium and Appium
            candidates = driver.findElements(By.cssSelector(
                    "input, button, select, textarea, a, form, label, "
                    + "[role='button'], [role='link'], [role='textbox'], [role='checkbox'], "
                    + "[role='radio'], [role='tab'], [role='menuitem'], [data-testid]"));
        } catch (Exception e) {
            // CSS selector not supported (some Appium drivers) — fallback to XPath
            try {
                candidates = driver.findElements(By.xpath(
                        "//*[self::input or self::button or self::select or self::textarea "
                        + "or self::a or self::form or self::label]"));
            } catch (Exception ex) {
                Reporter.log("BaselineStore: Failed to query DOM for candidates: " + ex.getMessage(), LogLevel.WARN);
                return null;
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        ScoredCandidate best = null;

        for (WebElement candidate : candidates) {
            try {
                double score = baseline.scoreSimilarity(candidate);

                if (best == null || score > best.score) {
                    By locator = ElementFingerprint.reconstructLocator(candidate);
                    if (locator != null) {
                        String reasoning = buildMatchReasoning(baseline, candidate);
                        best = new ScoredCandidate(candidate, locator, score, reasoning);
                    }
                }
            } catch (Exception ignored) {
                // Skip stale/detached elements
            }
        }

        return best;
    }

    /**
     * Builds a human-readable explanation of why a candidate matched.
     */
    private static String buildMatchReasoning(ElementFingerprint baseline, WebElement candidate) {
        StringBuilder sb = new StringBuilder("Matched by:");
        try {
            String candidateId = candidate.getAttribute("id");
            if (candidateId != null && candidateId.equals(baseline.getId())) {
                sb.append(" id='").append(candidateId).append("'");
            }
            String candidateName = candidate.getAttribute("name");
            if (candidateName != null && candidateName.equals(baseline.getName())) {
                sb.append(" name='").append(candidateName).append("'");
            }
            String candidateAriaLabel = candidate.getAttribute("aria-label");
            if (candidateAriaLabel != null && candidateAriaLabel.equals(baseline.getAriaLabel())) {
                sb.append(" aria-label='").append(candidateAriaLabel).append("'");
            }
            String candidateTag = candidate.getTagName();
            if (candidateTag != null && candidateTag.equalsIgnoreCase(baseline.getTagName())) {
                sb.append(" tag='").append(candidateTag).append("'");
            }
            String candidateText = candidate.getText();
            if (candidateText != null && baseline.getText() != null
                    && candidateText.contains(baseline.getText())) {
                sb.append(" text(partial)");
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    // ──────────────────────── Result Model ────────────────────────

    /**
     * A candidate element with its similarity score and reasoning.
     */
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

    /**
     * Loads baselines from disk into memory. Called lazily on first access.
     */
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
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = new FileReader(path.toFile())) {
            Type listType = new TypeToken<ArrayList<ElementFingerprint>>() {}.getType();
            List<ElementFingerprint> list = GSON.fromJson(reader, listType);
            if (list != null) {
                for (ElementFingerprint fp : list) {
                    if (fp.getLocatorKey() != null) {
                        baselines.put(fp.getLocatorKey(), fp);
                    }
                }
                Reporter.log("BaselineStore: Loaded " + list.size() + " baselines from disk", LogLevel.INFO_BLUE);
            }
        } catch (Exception e) {
            Reporter.log("BaselineStore: Failed to load baselines (non-fatal): " + e.getMessage(), LogLevel.WARN);
        }
    }

    /**
     * Saves baselines to disk asynchronously to avoid blocking the test thread.
     */
    private static void saveToDiskAsync() {
        // Use a daemon thread for non-blocking I/O
        Thread saveThread = new Thread(() -> {
            synchronized (LOCK) {
                try {
                    Path path = Paths.get(BASELINE_FILE);
                    Files.createDirectories(path.getParent());
                    List<ElementFingerprint> list = new ArrayList<>(baselines.values());
                    try (Writer writer = new FileWriter(path.toFile())) {
                        GSON.toJson(list, writer);
                    }
                } catch (Exception e) {
                    // Persistence failure must never crash the test
                }
            }
        }, "baseline-store-save");
        saveThread.setDaemon(true);
        saveThread.start();
    }

    /**
     * Forces a synchronous save. Called at suite end.
     */
    public static void flush() {
        synchronized (LOCK) {
            try {
                Path path = Paths.get(BASELINE_FILE);
                Files.createDirectories(path.getParent());
                List<ElementFingerprint> list = new ArrayList<>(baselines.values());
                try (Writer writer = new FileWriter(path.toFile())) {
                    GSON.toJson(list, writer);
                }
                Reporter.log("BaselineStore: Flushed " + list.size() + " baselines to disk", LogLevel.INFO_GREEN);
            } catch (Exception e) {
                Reporter.log("BaselineStore: Failed to flush baselines: " + e.getMessage(), LogLevel.ERROR);
            }
        }
    }

    /**
     * Clears all in-memory baselines. Used for testing.
     */
    public static void clear() {
        baselines.clear();
        loaded = false;
    }
}
