package Ellithium.Utilities.ai;

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

    // ──────────────────────── Public API ────────────────────────

    /**
     * Records a single heal attempt.
     *
     * @param tier          1 = Algorithmic, 2 = Semantic, 3 = ONNX Embedding (future), 4 = LLM
     * @param brokenLocator The locator that failed (e.g., "By.id: loginBtn")
     * @param healedLocator The healed locator string, or null if healing failed
     * @param score         Similarity / confidence score (0.0–1.0), or 0.0 on failure
     * @param success       Whether the heal attempt produced a usable element
     */
    public static void record(int tier, String brokenLocator, String healedLocator,
                               double score, boolean success) {
        records.add(new TelemetryRecord(tier, brokenLocator, healedLocator, score, success));
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
        return records.size();
    }

    /**
     * Writes all records to {@code Test-Output/healing-telemetry.json}.
     * Called by {@link AIHealingReporter#generateReport()} at suite end.
     * Non-fatal: logs a warning on write failure.
     */
    public static void flush() {
        if (records.isEmpty()) return;

        List<TelemetryRecord> snapshot = new ArrayList<>(records);
        File outputFile = new File(OUTPUT_FILE);

        try {
            outputFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(outputFile)) {
                GSON.toJson(new TelemetryOutput(snapshot), writer);
            }
            Reporter.log("HealingTelemetryStore: Flushed " + snapshot.size()
                    + " telemetry records to " + OUTPUT_FILE, LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("HealingTelemetryStore: Failed to flush telemetry (non-fatal): "
                    + e.getMessage(), LogLevel.WARN);
        }
    }

    /** Clears all in-memory records (for testing/reset). */
    public static void clear() {
        records.clear();
    }

    // ──────────────────────── Data Model ────────────────────────

    public static class TelemetryRecord {
        public final int tier;
        public final String brokenLocator;
        public final String healedLocator;
        public final double score;
        public final boolean success;
        public final String threadName;
        public final String timestamp;

        TelemetryRecord(int tier, String brokenLocator, String healedLocator,
                        double score, boolean success) {
            this.tier = tier;
            this.brokenLocator = brokenLocator;
            this.healedLocator = healedLocator;
            this.score = score;
            this.success = success;
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
            this.tier1 = new TierSummary(1, records);
            this.tier2 = new TierSummary(2, records);
            this.tier3 = new TierSummary(3, records);
            this.tier4 = new TierSummary(4, records);
        }
    }

    private static class TierSummary {
        final int tier;
        final long attempts;
        final long successes;
        final double avgScore;

        TierSummary(int tier, List<TelemetryRecord> records) {
            this.tier = tier;
            List<TelemetryRecord> tierRecs = records.stream()
                    .filter(r -> r.tier == tier).collect(Collectors.toList());
            this.attempts = tierRecs.size();
            this.successes = tierRecs.stream().filter(r -> r.success).count();
            this.avgScore = tierRecs.isEmpty() ? 0.0
                    : tierRecs.stream().mapToDouble(r -> r.score).average().orElse(0.0);
        }
    }
}
