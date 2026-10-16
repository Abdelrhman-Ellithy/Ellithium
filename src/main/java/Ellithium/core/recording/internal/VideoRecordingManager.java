package Ellithium.core.recording.internal;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.config.management.ConfigContext;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverConfiguration;
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
 * Uses unique recording IDs to properly correlate start/stop operations across
 * parallel tests.
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

        public ScreenRecorderActions<WebDriver> getRecorder() {
            return recorder;
        }

        public String getTestName() {
            return testName;
        }

        public WebDriver getDriver() {
            return driver;
        }

        public long getThreadId() {
            return threadId;
        }

        public long getStartTime() {
            return startTime;
        }
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
    private static final ThreadLocal<PendingRecording> pendingRecording = new ThreadLocal<>();

    private record PendingRecording(String testName, String testIdentifier) {
    }

    /**
     * Configuration keys for properties file
     */
    private static final String RECORD_GUI_EXECUTION_KEY = "recordGUITestExecution";
    private static final String ATTACH_RECORDED_EXECUTION_KEY = "attachRecordedGUITestExecutionToReport";
    private static final String GENERATE_REPORT_KEY = "allure.generate.report";
    private static final String ATTACH_RECORDED_EXECUTION_ON_FAILURE_KEY = "attachRecordedGUITestExecutionToReportOnlyOnFailure";

    /**
     * Private constructor to prevent instantiation
     */
    private VideoRecordingManager() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Checks if video recording is enabled in configuration.
     * 
     * @return true if recording is enabled, false otherwise
     */
    public static boolean isRecordingEnabled() {
        String sysProp = System.getProperty(RECORD_GUI_EXECUTION_KEY);
        if (sysProp != null && !sysProp.isBlank()) {
            return Boolean.parseBoolean(sysProp);
        }
        String configPath = ConfigContext.getConfigFilePath();
        if (configPath != null && PropertyHelper.keyExists(configPath, RECORD_GUI_EXECUTION_KEY)) {
            String value = PropertyHelper.getDataFromProperties(configPath, RECORD_GUI_EXECUTION_KEY);
            return Boolean.parseBoolean(value);
        }
        return true;
    }

    /**
     * Checks if recorded videos should be attached to the report.
     * Defaults to true unless explicitly disabled.
     * 
     * @return true if attachment is enabled, false otherwise
     */
    public static boolean isAttachmentEnabled() {
        String sysAttachment = System.getProperty(ATTACH_RECORDED_EXECUTION_KEY);
        if (sysAttachment != null && !sysAttachment.isBlank()) {
            return Boolean.parseBoolean(sysAttachment);
        }
        String configPath = ConfigContext.getConfigFilePath();
        if (configPath != null && PropertyHelper.keyExists(configPath, ATTACH_RECORDED_EXECUTION_KEY)) {
            String value = PropertyHelper.getDataFromProperties(configPath, ATTACH_RECORDED_EXECUTION_KEY);
            return Boolean.parseBoolean(value);
        }
        return true;
    }

    /**
     * Checks if recorded videos should be attached to the report on failure only.
     * Defaults to false (attaches for all tests) unless failure-only is explicitly enabled.
     * 
     * @return true if attachment is enabled for failure only, false otherwise
     */
    public static boolean isAttachmentOnFailureOnlyEnabled() {
        String sysProp = System.getProperty(ATTACH_RECORDED_EXECUTION_ON_FAILURE_KEY);
        if (sysProp != null && !sysProp.isBlank()) {
            return Boolean.parseBoolean(sysProp);
        }
        String configPath = ConfigContext.getConfigFilePath();
        if (configPath != null && PropertyHelper.keyExists(configPath, ATTACH_RECORDED_EXECUTION_ON_FAILURE_KEY)) {
            String value = PropertyHelper.getDataFromProperties(configPath, ATTACH_RECORDED_EXECUTION_ON_FAILURE_KEY);
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    /**
     * Starts recording for the current test/scenario.
     * Creates a unique recording ID to track this specific recording session.
     * 
     * @param testName       Name of the test or scenario
     * @param testIdentifier Unique identifier for the test (e.g., method name +
     *                       params hash)
     * @return Recording ID that must be used to stop the recording, null if
     *         recording failed
     */
    private static final Map<String, PendingRecording> pendingTestRecordings = new ConcurrentHashMap<>();

    /**
     * Starts recording for the current test/scenario.
     * Creates a unique recording ID to track this specific recording session.
     * 
     * @param testName       Name of the test or scenario
     * @param testIdentifier Unique identifier for the test (e.g., method name +
     *                       params hash)
     * @return Recording ID that must be used to stop the recording, null if
     *         recording failed
     */
    public static String startRecording(String testName, String testIdentifier) {
        if (!isRecordingEnabled()) {
            Logger.debug("Video recording is disabled in configuration");
            return null;
        }
        WebDriver driver = DriverFactory.getCurrentDriver();
        if (driver == null) {
            Logger.warn("No active driver found, skipping video recording");
            return null;
        }
        long threadId = Thread.currentThread().threadId();
        
        // If a recording is ALREADY active for this exact test, do NOT kill it! Return the existing recording ID.
        String existingTestRecordingId = testToRecordingMap.get(testIdentifier);
        if (existingTestRecordingId != null && recordingContextMap.containsKey(existingTestRecordingId)) {
            threadToRecordingMap.put(threadId, existingTestRecordingId);
            return existingTestRecordingId;
        }

        String existingRecordingId = threadToRecordingMap.get(threadId);
        if (existingRecordingId != null) {
            RecordingContext existingContext = recordingContextMap.get(existingRecordingId);
            if (existingContext != null) {
                if (existingContext.getTestName().equals(testName) || existingRecordingId.equals(existingTestRecordingId)) {
                    return existingRecordingId;
                }
                Logger.warn("Thread " + threadId + " already has active recording for: " +
                        existingContext.getTestName() + ". Stopping previous recording.");
                stopRecordingById(existingRecordingId, "INTERRUPTED");
            }
        }
        try {
            String recordingId = UUID.randomUUID().toString();
            ScreenRecorderActions<WebDriver> recorder = new ScreenRecorderActions<>(driver);
            String recordingName = sanitizeFileName(testName) + "_" +
                    TestDataGenerator.getTimeStamp() + "_" + recordingId.substring(0, 8);
            RecordingContext context = new RecordingContext(recorder, testName, driver, threadId);
            recordingContextMap.put(recordingId, context);
            threadToRecordingMap.put(threadId, recordingId);
            testToRecordingMap.put(testIdentifier, recordingId);
            recorder.startRecording(recordingName);
            Logger.info("Started video recording for: " + testName + " on thread: " + threadId);
            return recordingId;
        } catch (Exception e) {
            Logger.error("Failed to start video recording: " + e.getMessage());
            Logger.logException(e);
            return null;
        }
    }

    /** Registers a test for recording now or when its driver is created later. */
    public static String prepareRecording(String testName, String testIdentifier) {
        PendingRecording pending = new PendingRecording(testName, testIdentifier);
        pendingRecording.set(pending);
        pendingTestRecordings.put(testIdentifier, pending);
        startPendingRecordingIfPossible();
        return testToRecordingMap.get(testIdentifier);
    }

    /**
     * Called by DriverFactory after a driver is installed on the current thread.
     */
    public static void onDriverCreated() {
        startPendingRecordingIfPossible();
    }

    private static void startPendingRecordingIfPossible() {
        PendingRecording pending = pendingRecording.get();
        if (pending == null) {
            String testId = Logger.getCurrentTestIdentifier();
            if (testId != null) {
                pending = pendingTestRecordings.get(testId);
            }
        }
        if (pending == null || DriverFactory.getCurrentDriver() == null) {
            return;
        }
        DriverConfiguration configuration = DriverFactory.getCurrentDriverConfiguration();
        if (configuration == null || configuration.isMobileCloud()) {
            pendingRecording.remove();
            pendingTestRecordings.remove(pending.testIdentifier());
            return;
        }
        try {
            startRecording(pending.testName(), pending.testIdentifier());
        } finally {
            pendingRecording.remove();
            pendingTestRecordings.remove(pending.testIdentifier());
        }
    }

    /**
     * Stops recording using the recording ID.
     * This ensures we stop the correct recording even in complex parallel
     * scenarios.
     * 
     * @param recordingId The unique recording ID returned by startRecording
     * @param testStatus  Status of the test (PASSED, FAILED, SKIPPED, etc.)
     * @return Path to the saved video file, or null if recording failed
     */
    public static String stopRecordingById(String recordingId, String testStatus) {
        if (recordingId == null) {
            Logger.warn("Recording ID is null, cannot stop recording");
            return null;
        }
        RecordingContext context = recordingContextMap.get(recordingId);
        if (context == null) {
            Logger.warn("No recording context found for ID: " + recordingId);
            return null;
        }
        try {
            ScreenRecorderActions<WebDriver> recorder = context.getRecorder();
            String testName = context.getTestName();
            long duration = System.currentTimeMillis() - context.getStartTime();
            String videoPath = recorder.stopRecording();
            if (videoPath != null) {
                Logger.info("Stopped video recording for: " + testName + " - (Duration: " + duration + "ms)");
                handleVideoAttachment(videoPath, recorder.getLastCompilationFuture(), testName, testStatus);
                return videoPath;
            } else {
                Logger.error("Failed to save video recording for: " + testName);
                return null;
            }
        } catch (Exception e) {
            Logger.error("Error while stopping video recording [ID: " + recordingId + "]: " + e.getMessage());
            Logger.logException(e);
            return null;
        } finally {
            cleanupRecording(recordingId);
            pendingRecording.remove();
        }
    }

    /**
     * Stops recording for the current thread.
     * Finds the active recording for this thread and stops it.
     * 
     * @param testStatus Status of the test (PASSED, FAILED, SKIPPED, etc.)
     * @return Path to the saved video file, or null if recording failed
     */
    public static String stopRecordingForCurrentThread(String testStatus) {
        pendingRecording.remove();
        long threadId = Thread.currentThread().threadId();
        String recordingId = threadToRecordingMap.get(threadId);
        if (recordingId == null) {
            Logger.debug("No active recording found for thread: " + threadId);
            return null;
        }
        return stopRecordingById(recordingId, testStatus);
    }

    /**
     * Stops recording for a specific test identifier.
     * Useful when you know the test identifier but not the recording ID.
     * 
     * @param testIdentifier Unique identifier for the test
     * @param testStatus     Status of the test
     * @return Path to the saved video file, or null if recording failed
     */
    public static String stopRecordingForTest(String testIdentifier, String testStatus) {
        pendingRecording.remove();
        String recordingId = testToRecordingMap.get(testIdentifier);
        if (recordingId == null) {
            Logger.debug("No active recording found for test: " + testIdentifier);
            return null;
        }
        return stopRecordingById(recordingId, testStatus);
    }

    /**
     * Handles video attachment to the report based on configuration and test
     * status.
     * 
     * @param videoPath         Path to the video file
     * @param compilationFuture Future resolving to the compiled video File
     * @param testName          Name of the test
     * @param testStatus        Status of the test
     */
    private static void handleVideoAttachment(String videoPath,
                                              java.util.concurrent.CompletableFuture<File> compilationFuture,
                                              String testName, String testStatus) {
        boolean isFailed = "FAILED".equalsIgnoreCase(testStatus);
        boolean shouldAttach = isAttachmentEnabled();
        if (isAttachmentOnFailureOnlyEnabled()) {
            shouldAttach = (isAttachmentEnabled() && isFailed);
        }
        if (shouldAttach) {
            try {
                Reporter.attachVideoToReportAsync(compilationFuture, videoPath, testName + "_recording");
                Logger.info("Video attachment scheduled asynchronously for: " + testName);
            } catch (Exception e) {
                Logger.error("Failed to attach video to report: " + e.getMessage());
            }
        } else {
            Logger.info("Video recording saved at: " + videoPath + " (report attachment skipped per configuration)");
        }
    }

    /**
     * Deletes a video file from the file system.
     * 
     * @param videoPath Path to the video file
     */
    private static void deleteVideoFile(String videoPath) {
        try {
            File videoFile = new File(videoPath);
            if (videoFile.exists() && videoFile.delete()) {
                Logger.debug("Deleted video file: " + videoPath);
            }
        } catch (Exception e) {
            Logger.warn("Failed to delete video file: " + e.getMessage());
        }
    }

    /**
     * Cleans up a specific recording from all tracking maps.
     * 
     * @param recordingId The recording ID to clean up
     */
    private static void cleanupRecording(String recordingId) {
        RecordingContext context = recordingContextMap.remove(recordingId);
        if (context != null) {
            threadToRecordingMap.remove(context.getThreadId(), recordingId);
            testToRecordingMap.entrySet().removeIf(entry -> entry.getValue().equals(recordingId));
            Logger.debug("Cleaned up recording context [ID: " + recordingId.substring(0, 8) + "]");
        }
    }

    /**
     * Gets the current recorder instance for the calling thread.
     * 
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
     * 
     * @return Recording ID or null if no active recording
     */
    public static String getCurrentRecordingId() {
        return threadToRecordingMap.get(Thread.currentThread().threadId());
    }

    /**
     * Gets the recording ID for a specific test.
     * 
     * @param testIdentifier Unique identifier for the test
     * @return Recording ID or null if no active recording
     */
    public static String getRecordingIdForTest(String testIdentifier) {
        return testToRecordingMap.get(testIdentifier);
    }

    /**
     * Checks if a recording is currently active for the current thread.
     * 
     * @return true if recording is active, false otherwise
     */
    public static boolean isRecordingActive() {
        return threadToRecordingMap.containsKey(Thread.currentThread().threadId());
    }

    /**
     * Checks if a specific test has an active recording.
     * 
     * @param testIdentifier Unique identifier for the test
     * @return true if recording is active, false otherwise
     */
    public static boolean isRecordingActiveForTest(String testIdentifier) {
        return testToRecordingMap.containsKey(testIdentifier);
    }

    /**
     * Gets diagnostic information about all active recordings.
     * Useful for debugging parallel execution issues.
     * 
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
     * 
     * @param fileName Original file name
     * @return Sanitized file name
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "test_recording";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_{2,}", "_")
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
                Logger.error("Failed to stop recording during cleanup [ID: " + recordingId + "]: " +
                        e.getMessage());
            }
        });
        recordingContextMap.clear();
        threadToRecordingMap.clear();
        testToRecordingMap.clear();
        if (activeRecordings > 0) {
            Logger.info("Force cleanup completed. Stopped " + activeRecordings + " active recordings.");
        }
    }
}