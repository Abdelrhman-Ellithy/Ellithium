package Ellithium.core.ai;

import Ellithium.core.ai.reporting.AIHealingReporter;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Structured per-heal-attempt telemetry for all tiers.
 *
 * <p>Records tier, broken locator, healed locator, similarity score, success flag,
 * thread name, and ISO timestamp for every heal attempt. Flushed to JSON at suite end
 * by {@link AIHealingReporter#generateReport()}.</p>
 *
 * <p>Thread-safe: uses {@link ConcurrentLinkedQueue} for lock-free enqueue and
 * volatile-read snapshot for flush.</p>
 */
public class HealingTelemetryStore {

    private static final String OUTPUT_FILE =
            "Test-Output" + File.separator + "healing-telemetry.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ConcurrentLinkedQueue<TelemetryRecord> records =
            new ConcurrentLinkedQueue<>();
    private static final AtomicInteger recordCount = new AtomicInteger(0);

    private static final ThreadLocal<String> CURRENT_TEST = new ThreadLocal<>();

    /** Sets the test identifier for the current thread so subsequent heals are attributed to it. */
    public static void setCurrentTest(String testId) {
        if (testId == null) CURRENT_TEST.remove(); else CURRENT_TEST.set(testId);
    }

    /** Clears the current-thread test attribution (call at test end to avoid leaking across reuse). */
    public static void clearCurrentTest() {
        CURRENT_TEST.remove();
    }

    /**
     * Records a single heal attempt (legacy 5-arg form — no query/category context).
     *
     * @param tier          1 = Algorithmic, 2 = Semantic, 3 = ONNX Embedding, 4 = LLM
     * @param brokenLocator The locator that failed (e.g., "By.id: loginBtn")
     * @param healedLocator The healed locator string, or null if healing failed
     * @param score         Similarity / confidence score (0.0–1.0), or 0.0 on failure
     * @param success       Whether the heal attempt produced a usable element (used) vs fell through
     */
    public static void record(int tier, String brokenLocator, String healedLocator,
                               double score, boolean success) {
        record(tier, brokenLocator, healedLocator, score, success, null, null);
    }

    /**
     * Records a single heal attempt with full Tier-3 context (B2 instrumentation).
     *
     * @param query    The semantic query string served to the model (debug + fallthrough analysis)
     * @param category Element category for this action — READABLE / CLICKABLE / INPUT (per-class rates)
     */
    public static void record(int tier, String brokenLocator, String healedLocator,
                               double score, boolean success, String query, String category) {
        int max = Ellithium.core.ai.config.AIConfigLoader.getTelemetryMaxRecords();
        records.add(new TelemetryRecord(tier, brokenLocator, healedLocator, score, success,
                query, category, CURRENT_TEST.get()));
        int n = recordCount.incrementAndGet();
        if (max > 0 && n > max) {
            boolean dropped = false;
            while (n > max) {
                if (records.poll() != null) { n = recordCount.decrementAndGet(); dropped = true; }
                else break;
            }
            if (dropped) {
                Reporter.log("[TELEMETRY] Record limit (" + max + ") reached — oldest entries evicted. "
                        + "Increase ai.telemetry.maxRecords to retain full history.", LogLevel.WARN);
            }
        }
    }

    /**
     * False-heal detector (R9): a heal that was USED in a test that subsequently FAILED is a prime
     * suspect for a confident-wrong heal. Flags every used heal attributed to {@code testId} and logs
     * a warning. This is the only ground-truth precision signal available in production — offline
     * metrics can't see a heal that was structurally plausible but semantically wrong.
     *
     * @return the number of used heals flagged as suspect for this test
     */
    public static int markTestFailed(String testId) {
        if (testId == null) return 0;
        int flagged = 0;
        for (TelemetryRecord r : records) {
            if (r.success && testId.equals(r.testId) && !r.suspectWrongHeal) {
                r.suspectWrongHeal = true;
                flagged++;
            }
        }
        if (flagged > 0) {
            Reporter.log(String.format(
                    "[FALSE-HEAL?] %s failed after %d used heal(s) — see healing-telemetry.json",
                    testId, flagged), LogLevel.WARN);
        }
        return flagged;
    }

    /**
     * Returns all records for a specific tier (snapshot — safe to call from any thread).
     * Used by {@link ModelCalibrationRunner} for per-model threshold calibration.
     */
    public static List<TelemetryRecord> getRecordsForTier(int tier) {
        return records.stream()
                .filter(r -> r.tier == tier)
                .collect(Collectors.toList());
    }

    /**
     * Returns all records (snapshot).
     */
    public static List<TelemetryRecord> getAllRecords() {
        return new ArrayList<>(records);
    }

    /**
     * Returns the total number of recorded attempts.
     */
    public static int size() {
        return recordCount.get();
    }

    /**
     * Writes all records to {@code Test-Output/healing-telemetry.json}.
     * Called by {@link AIHealingReporter#generateReport()} at suite end.
     * Non-fatal: logs a warning on write failure.
     */
    public static void flush() {
        if (records.isEmpty()) return;

        List<TelemetryRecord> snapshot = new ArrayList<>(records);
        java.nio.file.Path target = java.nio.file.Paths.get(OUTPUT_FILE);

        try {
            java.nio.file.Files.createDirectories(target.getParent());
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile(target.getParent(), "healing-telemetry", ".tmp");
            try {
                try (FileWriter writer = new FileWriter(tmp.toFile())) {
                    GSON.toJson(new TelemetryOutput(snapshot), writer);
                }
                try {
                    java.nio.file.Files.move(tmp, target,
                            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    java.nio.file.Files.move(tmp, target,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                java.nio.file.Files.deleteIfExists(tmp);
            }
            Reporter.log("HealingTelemetryStore: Flushed " + snapshot.size()
                    + " telemetry records to " + OUTPUT_FILE, LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("HealingTelemetryStore: Failed to flush telemetry (non-fatal): "
                    + e.getMessage(), LogLevel.WARN);
        }
    }

    /**
     * Emits a one-block CI-visible summary (stdout via Reporter) so a team notices silent healing
     * without opening the JSON. Per-tier used/fell-through + suspect wrong-heals (B3).
     */
    public static void logConsoleSummary() {
        if (records.isEmpty()) return;
        List<TelemetryRecord> snap = new ArrayList<>(records);
        long used = snap.stream().filter(r -> r.success).count();
        long suspect = snap.stream().filter(r -> r.suspectWrongHeal).count();
        StringBuilder sb = new StringBuilder("\n──────── Ellithium AI Healing Summary ────────\n");
        sb.append(String.format("  attempts=%d  used=%d  fell-through=%d%n",
                snap.size(), used, snap.size() - used));
        for (int tier : new int[]{1, 2, 3, 4}) {
            TierSummary t = new TierSummary(tier, snap);
            if (t.attempts == 0) continue;
            sb.append(String.format("  Tier %d: used=%d  fell-through=%d  fallthrough=%.2f  avgScore=%.3f%n",
                    tier, t.used, t.fellThrough, t.fallthroughRate, t.avgScore));
        }
        if (suspect > 0) {
            sb.append(String.format("  ⚠ %d SUSPECT wrong-heal(s) — a used heal's test later FAILED. "
                    + "Review healing-telemetry.json.%n", suspect));
        }
        sb.append("──────────────────────────────────────────────");
        Reporter.log(sb.toString(), suspect > 0 ? LogLevel.WARN : LogLevel.INFO_GREEN);
    }

    /** Clears all in-memory records (for testing/reset). */
    public static void clear() {
        records.clear();
        recordCount.set(0);
    }

    public static class TelemetryRecord {
        public final int tier;
        public final String brokenLocator;
        public final String healedLocator;
        public final double score;
        public final boolean success;        // true = heal USED; false = fell through to the next tier
        public final String query;           // semantic query served to the model (Tier 3), may be null
        public final String category;        // READABLE / CLICKABLE / INPUT, may be null
        public final String testId;          // owning test (for the false-heal detector), may be null
        public volatile boolean suspectWrongHeal; // set by markTestFailed when the owning test failed
        public final String threadName;
        public final String timestamp;

        TelemetryRecord(int tier, String brokenLocator, String healedLocator, double score,
                        boolean success, String query, String category, String testId) {
            this.tier = tier;
            this.brokenLocator = brokenLocator;
            this.healedLocator = healedLocator;
            this.score = score;
            this.success = success;
            this.query = query;
            this.category = category;
            this.testId = testId;
            this.suspectWrongHeal = false;
            this.threadName = Thread.currentThread().getName();
            this.timestamp = Instant.now().toString();
        }
    }

    /** Wrapper that includes a summary header alongside the records array. */
    private static class TelemetryOutput {
        final String generatedAt = Instant.now().toString();
        final int totalRecords;
        final long successCount;
        final long failureCount;
        final long suspectWrongHeals;     // used heals whose owning test later failed (R9)
        final TierSummary tier1;
        final TierSummary tier2;
        final TierSummary tier3;
        final TierSummary tier4;
        final List<TelemetryRecord> records;

        TelemetryOutput(List<TelemetryRecord> records) {
            this.records = records;
            this.totalRecords = records.size();
            this.successCount = records.stream().filter(r -> r.success).count();
            this.failureCount = records.stream().filter(r -> !r.success).count();
            this.suspectWrongHeals = records.stream().filter(r -> r.suspectWrongHeal).count();
            this.tier1 = new TierSummary(1, records);
            this.tier2 = new TierSummary(2, records);
            this.tier3 = new TierSummary(3, records);
            this.tier4 = new TierSummary(4, records);
        }
    }

    private static class TierSummary {
        final int tier;
        final long attempts;
        final long used;             // heals used (success=true)
        final long fellThrough;      // attempts that fell through to the next tier (success=false)
        final double fallthroughRate;// fellThrough / attempts — the B0.5/B4 decision signal
        final long suspectWrongHeals;// used heals whose owning test later failed
        final double avgScore;

        TierSummary(int tier, List<TelemetryRecord> records) {
            this.tier = tier;
            List<TelemetryRecord> tierRecs = records.stream()
                    .filter(r -> r.tier == tier).collect(Collectors.toList());
            this.attempts = tierRecs.size();
            this.used = tierRecs.stream().filter(r -> r.success).count();
            this.fellThrough = tierRecs.stream().filter(r -> !r.success).count();
            this.fallthroughRate = tierRecs.isEmpty() ? 0.0 : (double) fellThrough / attempts;
            this.suspectWrongHeals = tierRecs.stream().filter(r -> r.suspectWrongHeal).count();
            this.avgScore = tierRecs.isEmpty() ? 0.0
                    : tierRecs.stream().mapToDouble(r -> r.score).average().orElse(0.0);
        }
    }
}
