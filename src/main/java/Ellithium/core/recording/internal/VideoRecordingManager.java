package Ellithium.core.recording.internal;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.driver.DriverConfiguration;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages video recording for test execution in parallel environments.
 * Handles both TestNG and Cucumber test scenarios with thread-safe operations.
 * Uses unique recording IDs to properly correlate start/stop operations across parallel tests.
 */
public class VideoRecordingManager {

    /**
     * Holds recording context for a single test execution
     */
    private static class RecordingContext {
        private final ScreenRecorderActions<WebDriver> recorder;
        private final String testName;
        private final WebDriver driver;
        private final long threadId;
        private final long startTime;

        public RecordingContext(ScreenRecorderActions<WebDriver> recorder, String testName,
                                WebDriver driver, long threadId) {
            this.recorder = recorder;
            this.testName = testName;
            this.driver = driver;
            this.threadId = threadId;
            this.startTime = System.currentTimeMillis();
        }

        public ScreenRecorderActions<WebDriver> getRecorder() { return recorder; }
        public String getTestName() { return testName; }
        public WebDriver getDriver() { return driver; }
        public long getThreadId() { return threadId; }
        public long getStartTime() { return startTime; }
    }

    /**
     * Maps unique recording ID to recording context
     * Key: Recording ID (UUID), Value: RecordingContext
     */
    private static final Map<String, RecordingContext> recordingContextMap = new ConcurrentHashMap<>();

    /**
     * Maps thread ID to current active recording ID
     * Key: Thread ID, Value: Recording ID
     * This allows us to get the current recording for a thread
     */
    private static final Map<Long, String> threadToRecordingMap = new ConcurrentHashMap<>();

    /**
     * Maps test identifier to recording ID for tracking
     * Key: Test unique identifier, Value: Recording ID
     */
    private static final Map<String, String> testToRecordingMap = new ConcurrentHashMap<>();

    /**
     * Configuration keys for properties file
     */
    private static final String RECORD_GUI_EXECUTION_KEY = "recordGUITestExecution";
    private static final String ATTACH_RECORDED_EXECUTION_KEY = "attachRecordedGUITestExecutionToReport";
    private static final String ATTACH_RECORDED_EXECUTION_ON_FAILURE_KEY = "attachRecordedGUITestExecutionToReportOnlyOnFailure";

    /**
     * Private constructor to prevent instantiation
     */
    private VideoRecordingManager() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Checks if video recording is enabled in configuration.
     * @return true if recording is enabled, false otherwise
     */
    public static boolean isRecordingEnabled() {
        String configPath = ConfigContext.getConfigFilePath();
        if (configPath == null || !PropertyHelper.keyExists(configPath, RECORD_GUI_EXECUTION_KEY)) {
            Reporter.log("Recording configuration key not found, defaulting to false", LogLevel.WARN);
            return false;
        }
        String value = PropertyHelper.getDataFromProperties(configPath, RECORD_GUI_EXECUTION_KEY);
        return Boolean.parseBoolean(value);
    }

    /**
     * Checks if recorded videos should be attached to the report.
     * @return true if attachment is enabled, false otherwise
     */
    public static boolean isAttachmentEnabled() {
        String configPath = ConfigContext.getConfigFilePath();
        if (configPath == null || !PropertyHelper.keyExists(configPath, ATTACH_RECORDED_EXECUTION_KEY)) {
            Reporter.log("Attachment configuration key not found, defaulting to true", LogLevel.WARN);
            return true;
        }
        String value = PropertyHelper.getDataFromProperties(configPath, ATTACH_RECORDED_EXECUTION_KEY);
        return Boolean.parseBoolean(value);
    }
    /**
     * Checks if recorded videos should be attached to the report on failure only.
     * @return true if attachment is enabled, false otherwise
     */
    public static boolean isAttachmentOnFailureOnlyEnabled() {
        String configPath = ConfigContext.getConfigFilePath();
        if (configPath == null || !PropertyHelper.keyExists(configPath, ATTACH_RECORDED_EXECUTION_ON_FAILURE_KEY)) {
            Reporter.log("Attachment on Failure configuration key not found, defaulting to true", LogLevel.WARN);
            return true;
        }
        String value = PropertyHelper.getDataFromProperties(configPath, ATTACH_RECORDED_EXECUTION_ON_FAILURE_KEY);
        return Boolean.parseBoolean(value);
    }

    /**
     * Starts recording for the current test/scenario.
     * Creates a unique recording ID to track this specific recording session.
     * @param testName Name of the test or scenario
     * @param testIdentifier Unique identifier for the test (e.g., method name + params hash)
     * @return Recording ID that must be used to stop the recording, null if recording failed
     */
    public static String startRecording(String testName, String testIdentifier) {
        if (!isRecordingEnabled()) {
            Reporter.log("Video recording is disabled in configuration", LogLevel.DEBUG);
            return null;
        }
        WebDriver driver = DriverFactory.getCurrentDriver();
        if (driver == null) {
            Reporter.log("No active driver found, skipping video recording", LogLevel.WARN);
            return null;
        }
        long threadId = Thread.currentThread().threadId();
        String existingRecordingId = threadToRecordingMap.get(threadId);
        if (existingRecordingId != null) {
            RecordingContext existingContext = recordingContextMap.get(existingRecordingId);
            if (existingContext != null) {
                Reporter.log("Thread " + threadId + " already has active recording for: " +
                        existingContext.getTestName() + ". Stopping previous recording.", LogLevel.WARN);
                stopRecordingById(existingRecordingId, "INTERRUPTED");
            }
        }
        String existingTestRecordingId = testToRecordingMap.get(testIdentifier);
        if (existingTestRecordingId != null) {
            Reporter.log("Test " + testIdentifier + " already has an active recording. Stopping previous recording.",
                    LogLevel.WARN);
            stopRecordingById(existingTestRecordingId, "DUPLICATE");
        }
        try {
            String recordingId = UUID.randomUUID().toString();
            ScreenRecorderActions<WebDriver> recorder = new ScreenRecorderActions<>(driver);
            DriverConfiguration currentDriverConfiguration=DriverFactory.getCurrentDriverConfiguration();
            String recordingName = sanitizeFileName(testName) + "_" + "_"+currentDriverConfiguration.getDriverType().getName().toUpperCase()+
                    TestDataGenerator.getTimeStamp() + "_" + recordingId.substring(0, 8);
            RecordingContext context = new RecordingContext(recorder, testName, driver, threadId);
            recordingContextMap.put(recordingId, context);
            threadToRecordingMap.put(threadId, recordingId);
            testToRecordingMap.put(testIdentifier, recordingId);
            recorder.startRecording(recordingName);
            Reporter.log("Started video recording [] for: " + testName +" - " +currentDriverConfiguration.getDriverType().getName().toUpperCase()+ " on thread: " + threadId, LogLevel.INFO_BLUE);
            return recordingId;
        } catch (Exception e) {
            Reporter.log("Failed to start video recording: " + e.getMessage(), LogLevel.ERROR);
            Logger.logException(e);
            return null;
        }
    }

    /**
     * Stops recording using the recording ID.
     * This ensures we stop the correct recording even in complex parallel scenarios.
     * @param recordingId The unique recording ID returned by startRecording
     * @param testStatus Status of the test (PASSED, FAILED, SKIPPED, etc.)
     * @return Path to the saved video file, or null if recording failed
     */
    public static String stopRecordingById(String recordingId, String testStatus) {
        if (recordingId == null) {
            Reporter.log("Recording ID is null, cannot stop recording", LogLevel.WARN);
            return null;
        }
        RecordingContext context = recordingContextMap.get(recordingId);
        if (context == null) {
            Reporter.log("No recording context found for ID: " + recordingId, LogLevel.WARN);
            return null;
        }
        try {
            ScreenRecorderActions<WebDriver> recorder = context.getRecorder();
            String testName = context.getTestName();
            long duration = System.currentTimeMillis() - context.getStartTime();
            String videoPath = recorder.stopRecording();
            if (videoPath != null) {
                Reporter.log("Stopped video recording for: " + testName +" - " +DriverFactory.getCurrentDriverConfiguration().getDriverType().getName().toUpperCase()+ " (Duration: " + duration + "ms)", LogLevel.INFO_BLUE);
                Reporter.log("Video saved at: " + videoPath, LogLevel.INFO_GREEN);
                handleVideoAttachment(videoPath, testName, testStatus);
                return videoPath;
            } else {
                Reporter.log("Failed to save video recording for: " + testName, LogLevel.ERROR);
                return null;
            }
        } catch (Exception e) {
            Reporter.log("Error while stopping video recording [ID: " + recordingId + "]: " +
                    e.getMessage(), LogLevel.ERROR);
            Logger.logException(e);
            return null;
        } finally {
            cleanupRecording(recordingId);
        }
    }

    /**
     * Stops recording for the current thread.
     * Finds the active recording for this thread and stops it.
     * @param testStatus Status of the test (PASSED, FAILED, SKIPPED, etc.)
     * @return Path to the saved video file, or null if recording failed
     */
    public static String stopRecordingForCurrentThread(String testStatus) {
        long threadId = Thread.currentThread().threadId();
        String recordingId = threadToRecordingMap.get(threadId);
        if (recordingId == null) {
            Reporter.log("No active recording found for thread: " + threadId, LogLevel.DEBUG);
            return null;
        }
        return stopRecordingById(recordingId, testStatus);
    }

    /**
     * Stops recording for a specific test identifier.
     * Useful when you know the test identifier but not the recording ID.
     * @param testIdentifier Unique identifier for the test
     * @param testStatus Status of the test
     * @return Path to the saved video file, or null if recording failed
     */
    public static String stopRecordingForTest(String testIdentifier, String testStatus) {
        String recordingId = testToRecordingMap.get(testIdentifier);
        if (recordingId == null) {
            Reporter.log("No active recording found for test: " + testIdentifier, LogLevel.DEBUG);
            return null;
        }
        return stopRecordingById(recordingId, testStatus);
    }

    /**
     * Handles video attachment to the report based on configuration and test status.
     * @param videoPath Path to the video file
     * @param testName Name of the test
     * @param testStatus Status of the test
     */
    private static void handleVideoAttachment(String videoPath, String testName, String testStatus) {
        boolean isFailed = "FAILED".equalsIgnoreCase(testStatus);
        boolean shouldAttach=isAttachmentEnabled();
        if (isAttachmentOnFailureOnlyEnabled()){
            shouldAttach= (isAttachmentEnabled()&&isFailed);
        }
        if (shouldAttach) {
            try {
                String description = String.format("%s - %s - %s -Video Recording",DriverFactory.getCurrentDriverConfiguration().getDriverType().getName().toUpperCase(), testName, testStatus);
                Reporter.attachFileToReport(videoPath, testName + "_recording", description);
                Reporter.log("Video attached to report: " + testName, LogLevel.INFO_GREEN);
            } catch (Exception e) {
                Reporter.log("Failed to attach video to report: " + e.getMessage(), LogLevel.ERROR);
            }
        } else {
            deleteVideoFile(videoPath);
        }
    }

    /**
     * Deletes a video file from the file system.
     * @param videoPath Path to the video file
     */
    private static void deleteVideoFile(String videoPath) {
        try {
            File videoFile = new File(videoPath);
            if (videoFile.exists() && videoFile.delete()) {
                Reporter.log("Deleted video file: " + videoPath, LogLevel.DEBUG);
            }
        } catch (Exception e) {
            Reporter.log("Failed to delete video file: " + e.getMessage(), LogLevel.WARN);
        }
    }

    /**
     * Cleans up a specific recording from all tracking maps.
     * @param recordingId The recording ID to clean up
     */
    private static void cleanupRecording(String recordingId) {
        RecordingContext context = recordingContextMap.remove(recordingId);
        if (context != null) {
            threadToRecordingMap.remove(context.getThreadId(), recordingId);
            testToRecordingMap.entrySet().removeIf(entry -> entry.getValue().equals(recordingId));
            Reporter.log("Cleaned up recording context [ID: " + recordingId.substring(0, 8) + "]",
                    LogLevel.DEBUG);
        }
    }

    /**
     * Gets the current recorder instance for the calling thread.
     * @return ScreenRecorderActions instance or null if not found
     */
    public static ScreenRecorderActions<WebDriver> getCurrentRecorder() {
        long threadId = Thread.currentThread().threadId();
        String recordingId = threadToRecordingMap.get(threadId);
        if (recordingId != null) {
            RecordingContext context = recordingContextMap.get(recordingId);
            return context != null ? context.getRecorder() : null;
        }
        return null;
    }

    /**
     * Gets the recording ID for the current thread.
     * @return Recording ID or null if no active recording
     */
    public static String getCurrentRecordingId() {
        return threadToRecordingMap.get(Thread.currentThread().threadId());
    }
    /**
     * Gets the recording ID for a specific test.
     * @param testIdentifier Unique identifier for the test
     * @return Recording ID or null if no active recording
     */
    public static String getRecordingIdForTest(String testIdentifier) {
        return testToRecordingMap.get(testIdentifier);
    }

    /**
     * Checks if a recording is currently active for the current thread.
     * @return true if recording is active, false otherwise
     */
    public static boolean isRecordingActive() {
        return threadToRecordingMap.containsKey(Thread.currentThread().threadId());
    }

    /**
     * Checks if a specific test has an active recording.
     * @param testIdentifier Unique identifier for the test
     * @return true if recording is active, false otherwise
     */
    public static boolean isRecordingActiveForTest(String testIdentifier) {
        return testToRecordingMap.containsKey(testIdentifier);
    }

    /**
     * Gets diagnostic information about all active recordings.
     * Useful for debugging parallel execution issues.
     * @return Diagnostic string with recording information
     */
    public static String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("\n=== Video Recording Manager Diagnostic Info ===\n");
        info.append("Active Recordings: ").append(recordingContextMap.size()).append("\n");
        info.append("Thread Mappings: ").append(threadToRecordingMap.size()).append("\n");
        info.append("Test Mappings: ").append(testToRecordingMap.size()).append("\n");
        info.append("\nActive Recording Details:\n");
        recordingContextMap.forEach((id, context) -> info.append(" | Test: ").append(context.getTestName())
                .append(" | Thread: ").append(context.getThreadId())
                .append(" | Duration: ").append(System.currentTimeMillis() - context.getStartTime())
                .append("ms\n"));

        info.append("===============================================\n");
        return info.toString();
    }

    /**
     * Sanitizes a file name by removing invalid characters.
     * @param fileName Original file name
     * @return Sanitized file name
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "test_recording";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .trim();
    }
    /**
     * Force cleanup of all resources.
     * Should be called in emergency situations or test suite completion.
     */
    public static void forceCleanupAll() {
        int activeRecordings = recordingContextMap.size();
        recordingContextMap.keySet().forEach(recordingId -> {
            try {
                stopRecordingById(recordingId, "CLEANUP");
            } catch (Exception e) {
                Reporter.log("Failed to stop recording during cleanup [ID: " + recordingId + "]: " +
                        e.getMessage(), LogLevel.ERROR);
            }
        });
        recordingContextMap.clear();
        threadToRecordingMap.clear();
        testToRecordingMap.clear();
        Reporter.log("Force cleanup completed. Stopped " + activeRecordings +
                " active recordings.", LogLevel.INFO_BLUE);
    }
}