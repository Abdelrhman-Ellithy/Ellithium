package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.models.TraceabilityRecord;
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
            } catch (IOException e) {
                Reporter.log("Failed to read traceability mappings: " + e.getMessage(), LogLevel.ERROR);
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
            records.removeIf(r -> r.getSource().getTestId().equals(record.getSource().getTestId())
                    && r.getSource().getSourceFile().equals(record.getSource().getSourceFile()));

            records.add(record);

            try (FileWriter writer = new FileWriter(MAPPING_FILE_PATH)) {
                gson.toJson(records, writer);
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
            return loadAllRecordsInternal().stream()
                    .anyMatch(r -> r.getSource().getTestId().equals(testId)
                            && r.getSource().getSourceFile().equals(sourceFile));
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
        } catch (IOException e) {
            Reporter.log("Failed to read traceability mappings: " + e.getMessage(), LogLevel.ERROR);
            return new ArrayList<>();
        }
    }
}
