package Ellithium.core.ai;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persists and indexes hard-negative healing candidates — candidates that scored highly
 * enough to heal, but resulted in subsequent test failures.
 *
 * <p>Hard-negative records are stored in {@code Test-Output/hard-negatives.json} and consumed
 * by {@link Ellithium.core.ai.healing.ModelCalibrationRunner} to calibrate threshold boundaries
 * contrastively, preventing repeat false heals.</p>
 */
public final class HardNegativeStore {

    public static final String OUTPUT_FILE = "Test-Output" + File.separator + "hard-negatives.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record HardNegativeRecord(
            String testId,
            int tier,
            String brokenLocator,
            String falseHealedLocator,
            double score,
            String actionType,
            String timestamp
    ) {}

    private static final CopyOnWriteArrayList<HardNegativeRecord> records = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<String, Set<String>> knownFalseLocators = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private HardNegativeStore() {}

    /**
     * Initializes the store by loading historical hard negatives from disk if present.
     */
    public static synchronized void initialize() {
        if (loaded) return;
        loadFromDisk();
        loaded = true;
    }

    /**
     * Records a hard-negative healing event.
     */
    public static void record(String testId, int tier, String brokenLocator,
                              String falseHealedLocator, double score, String actionType) {
        if (brokenLocator == null || falseHealedLocator == null) return;
        initialize();

        HardNegativeRecord record = new HardNegativeRecord(
                testId,
                tier,
                brokenLocator,
                falseHealedLocator,
                score,
                actionType,
                Instant.now().toString()
        );

        records.add(record);
        knownFalseLocators.computeIfAbsent(brokenLocator, k -> ConcurrentHashMap.newKeySet())
                .add(falseHealedLocator);
        Reporter.log("[HARD-NEGATIVE] Recorded suspect false heal: " + brokenLocator + " -> " + falseHealedLocator
                + " (score: " + String.format(Locale.ROOT, "%.2f", score) + ")", LogLevel.WARN);
    }

    /**
     * Checks if a candidate locator is a known hard-negative for the given broken locator.
     */
    public static boolean isHardNegative(String brokenLocator, String candidateLocator) {
        if (brokenLocator == null || candidateLocator == null) return false;
        initialize();
        Set<String> bad = knownFalseLocators.get(brokenLocator);
        return bad != null && bad.contains(candidateLocator);
    }

    /**
     * Returns a snapshot of all recorded hard negatives.
     */
    public static List<HardNegativeRecord> getAllRecords() {
        initialize();
        return new ArrayList<>(records);
    }

    /**
     * Flushes recorded hard negatives to disk.
     */
    public static synchronized void flush() {
        if (records.isEmpty()) return;
        try {
            File file = new File(OUTPUT_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(records, writer);
            }
            Reporter.log("[HARD-NEGATIVE] Persisted " + records.size() + " hard negative(s) to " + OUTPUT_FILE,
                    LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("[HARD-NEGATIVE] Failed to persist hard negatives: " + e.getMessage(), LogLevel.WARN);
        }
    }

    /**
     * Loads hard negatives from disk if available.
     */
    private static void loadFromDisk() {
        File file = new File(OUTPUT_FILE);
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<HardNegativeRecord>>() {}.getType();
            List<HardNegativeRecord> loadedList = GSON.fromJson(reader, listType);
            if (loadedList != null) {
                for (HardNegativeRecord r : loadedList) {
                    if (r != null && r.brokenLocator() != null && r.falseHealedLocator() != null) {
                        records.add(r);
                        knownFalseLocators.computeIfAbsent(r.brokenLocator(), k -> ConcurrentHashMap.newKeySet())
                                .add(r.falseHealedLocator());
                    }
                }
            }
        } catch (Exception e) {
            Reporter.log("[HARD-NEGATIVE] Could not load prior hard negatives: " + e.getMessage(), LogLevel.DEBUG);
        }
    }

    /**
     * Clears in-memory records (primarily for testing).
     */
    public static synchronized void clear() {
        records.clear();
        knownFalseLocators.clear();
        loaded = false;
    }
}
