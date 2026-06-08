package Ellithium.core.ai;

import Ellithium.core.ai.models.TraceabilityRecord;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the persistence of AI-generated mappings.
 * Acts as a Repository to save and retrieve TraceabilityRecords to a JSON file.
 *
 * <p>Thread-safe: all read/write operations are synchronized to prevent
 * data loss during parallel test execution.</p>
 */
public class TraceabilityManager {

    private static final String MAPPING_FILE_PATH = "ellithium-ai-mappings.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();

    /**
     * Retrieves all traceability records from the mapping file.
     *
     * @return List of TraceabilityRecord objects
     */
    public static List<TraceabilityRecord> loadAllRecords() {
        synchronized (LOCK) {
            File file = new File(MAPPING_FILE_PATH);
            if (!file.exists()) {
                return new ArrayList<>();
            }

            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<ArrayList<TraceabilityRecord>>() {}.getType();
                List<TraceabilityRecord> records = gson.fromJson(reader, listType);
                return records != null ? records : new ArrayList<>();
            } catch (Exception e) {
                Reporter.log("Failed to read traceability mappings (treating as empty): " + e.getMessage(), LogLevel.ERROR);
                return new ArrayList<>();
            }
        }
    }

    /**
     * Adds a new record to the mapping file. If a record with the same
     * test ID and source file already exists, it updates it.
     *
     * @param record The new traceability record to save
     */
    public static void saveRecord(TraceabilityRecord record) {
        synchronized (LOCK) {
            List<TraceabilityRecord> records = loadAllRecordsInternal();

            // Check if record exists, if so replace it
            records.removeIf(r -> sameKey(r, record.getSource().getTestId(), record.getSource().getSourceFile()));

            records.add(record);

            java.nio.file.Path target = java.nio.file.Paths.get(MAPPING_FILE_PATH);
            try {
                java.nio.file.Path tmp = java.nio.file.Files.createTempFile(
                        target.getParent() != null ? target.getParent() : java.nio.file.Paths.get("."),
                        "ellithium-ai-mappings", ".tmp");
                try {
                    try (FileWriter writer = new FileWriter(tmp.toFile())) {
                        gson.toJson(records, writer);
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
                Reporter.log("Traceability record saved for Test ID: " + record.getSource().getTestId(), LogLevel.INFO_GREEN);
            } catch (IOException e) {
                Reporter.log("Failed to save traceability mapping: " + e.getMessage(), LogLevel.ERROR);
            }
        }
    }

    /**
     * Checks if a test case has already been generated.
     *
     * @param testId     The test case ID
     * @param sourceFile The source file path
     * @return true if a mapping already exists for this test case
     */
    public static boolean isAlreadyGenerated(String testId, String sourceFile) {
        synchronized (LOCK) {
            return loadAllRecordsInternal().stream().anyMatch(r -> sameKey(r, testId, sourceFile));
        }
    }

    private static boolean sameKey(TraceabilityRecord r, String testId, String sourceFile) {
        if (r == null || r.getSource() == null) return false;
        return java.util.Objects.equals(r.getSource().getTestId(), testId)
                && java.util.Objects.equals(normalizeSourceFile(r.getSource().getSourceFile()),
                                            normalizeSourceFile(sourceFile));
    }

    private static String normalizeSourceFile(String path) {
        if (path == null) return null;
        try {
            java.nio.file.Path root = java.nio.file.Paths.get("").toAbsolutePath();
            java.nio.file.Path abs  = java.nio.file.Paths.get(path).toAbsolutePath().normalize();
            return root.relativize(abs).toString().replace('\\', '/');
        } catch (Exception e) {
            return path;
        }
    }

    /**
     * Internal read method — caller must already hold the LOCK.
     */
    private static List<TraceabilityRecord> loadAllRecordsInternal() {
        File file = new File(MAPPING_FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<TraceabilityRecord>>() {}.getType();
            List<TraceabilityRecord> records = gson.fromJson(reader, listType);
            return records != null ? records : new ArrayList<>();
        } catch (Exception e) {
            Reporter.log("Failed to read traceability mappings (treating as empty): " + e.getMessage(), LogLevel.ERROR);
            return new ArrayList<>();
        }
    }
}
