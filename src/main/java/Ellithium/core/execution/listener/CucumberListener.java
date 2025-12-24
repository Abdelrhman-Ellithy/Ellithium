package Ellithium.core.execution.listener;

import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.logging.Logger;
import Ellithium.core.recording.internal.VideoRecordingManager;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.reporting.notification.TestResultCollectorManager;
import io.cucumber.plugin.event.*;
import io.qameta.allure.Allure;
import io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Ellithium.config.managment.GeneralHandler.testFailed;
import static Ellithium.core.reporting.internal.Colors.*;

/**
 * Cucumber listener that integrates with the Ellithium framework.
 * Extends AllureCucumber7Jvm for Allure reporting integration.
 * Handles video recording starting from first step (when driver is available).
 */
public class CucumberListener extends AllureCucumber7Jvm {

    /**
     * Thread-local storage for scenario context
     */
    private static final ThreadLocal<ScenarioContext> scenarioContext = new ThreadLocal<>();

    /**
     * Maps scenario URI+Line to Recording ID for tracking across parallel execution
     */
    private static final Map<String, String> scenarioToRecordingId = new ConcurrentHashMap<>();

    /**
     * Inner class to hold scenario-specific context
     */
    private static class ScenarioContext {
        private String scenarioName;
        private String scenarioId;
        private String recordingId;
        private File failedScreenShot;
        private Boolean paramAdded;
        private Long scenarioStartTime;
        private String scenarioStatus;
        private int totalSteps;
        private int currentStepIndex;
        private boolean recordingStarted;

        public ScenarioContext(String scenarioName, String scenarioId, int totalSteps) {
            this.scenarioName = scenarioName;
            this.scenarioId = scenarioId;
            this.totalSteps = totalSteps;
            this.paramAdded = false;
            this.scenarioStartTime = System.currentTimeMillis();
            this.scenarioStatus = "UNKNOWN";
            this.currentStepIndex = 0;
            this.recordingStarted = false;
        }
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::testStartedHandler);
        publisher.registerHandlerFor(TestStepStarted.class, this::stepStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, this::stepFinishedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class, this::testFinishedHandler);
        publisher.registerHandlerFor(TestRunFinished.class, this::testRunFinishedHandler);
        super.setEventPublisher(publisher);
    }

    /**
     * Handles scenario start - initializes context and counts steps
     */
    private void testStartedHandler(TestCaseStarted event) {
        String name = event.getTestCase().getName();
        String scenarioId = getScenarioIdentifier(event.getTestCase());
        List<TestStep> allSteps = event.getTestCase().getTestSteps();
        int totalSteps = (int) allSteps.stream()
                .filter(step -> step instanceof PickleStepTestStep)
                .count();
        ScenarioContext context = new ScenarioContext(name, scenarioId, totalSteps);
        scenarioContext.set(context);
        Logger.info(CYAN + "[START] " + BLUE + "Scenario " + name + " (Steps: " + totalSteps + ") [START]" + RESET);
    }

    /**
     * Handles step start - starts recording on first step and clears logs
     */
    private void stepStartedHandler(TestStepStarted event) {
        if (!(event.getTestStep() instanceof PickleStepTestStep)) {
            return;
        }
        ScenarioContext context = scenarioContext.get();
        if (context == null) {
            return;
        }
        Logger.clearCurrentExecutionLogs();
        context.currentStepIndex++;
        if (context.currentStepIndex == 1 && !context.recordingStarted) {
            startRecordingIfPossible(context);
        }
    }

    /**
     * Attempts to start recording if driver is available and conditions are met
     */
    private void startRecordingIfPossible(ScenarioContext context) {
        boolean driverExecution=(DriverFactory.getCurrentDriver() != null);
        boolean notHeadless=DriverFactory.getCurrentDriverConfiguration().getHeadlessMode()==HeadlessMode.False;
        boolean isNotMobileCloud=!DriverFactory.getCurrentDriverConfiguration().isMobileCloud();
        boolean shouldRecord=driverExecution && notHeadless&&isNotMobileCloud;
        if (shouldRecord) {
            try {
                String recordingId = VideoRecordingManager.startRecording(
                        context.scenarioName,
                        context.scenarioId
                );

                if (recordingId != null) {
                    context.recordingId = recordingId;
                    context.recordingStarted = true;
                    scenarioToRecordingId.put(context.scenarioId, recordingId);
                    Logger.info(GREEN + "Video recording started for scenario: " + context.scenarioName + RESET);
                }
            } catch (Exception e) {
                Logger.error("Failed to start video recording: " + e.getMessage());
                Logger.logException(e);
            }
        } else {
            if (!driverExecution) {
                Logger.debug("Video recording skipped: No active driver");
            } else {
                Logger.debug("Video recording skipped: Headless mode enabled");
            }
        }
    }

    /**
     * Handles step finish - captures screenshots on failure and stops recording on last step
     */
    private void stepFinishedHandler(TestStepFinished event) {
        if (!(event.getTestStep() instanceof PickleStepTestStep)) {
            return;
        }
        ScenarioContext context = scenarioContext.get();
        if (context == null) {
            return;
        }
        Status stepStatus = event.getResult().getStatus();
        boolean driverExecution = (DriverFactory.getCurrentDriver() != null);
        boolean notHeadless = DriverFactory.getCurrentDriverConfiguration().getHeadlessMode() == HeadlessMode.False;
        boolean shouldCapture = driverExecution && notHeadless;
        if (shouldCapture && stepStatus == Status.FAILED) {
            handleStepFailure(event, context);
        } else {
            if (stepStatus==Status.SKIPPED) {
                handleStepSuccess(io.qameta.allure.model.Status.SKIPPED);
            }
            else {
                handleStepSuccess(io.qameta.allure.model.Status.PASSED);
            }
        }
        if (!context.paramAdded) {
            Reporter.addParams(GeneralHandler.getParameters());
            context.paramAdded = true;
        }
        boolean isLastStep = context.currentStepIndex >= context.totalSteps;
        if (isLastStep && context.recordingStarted) {
            stopRecording(context);
        }
    }

    /**
     * Handles scenario finish - ensures recording is stopped and collects results
     */
    private void testFinishedHandler(TestCaseFinished event) {
        String scenarioName = event.getTestCase().getName();
        ScenarioContext context = scenarioContext.get();
        if (context == null) {
            Logger.warn("Scenario context is null in testFinishedHandler");
            return;
        }
        long scenarioExecutionTime = System.currentTimeMillis() - context.scenarioStartTime;
        String status = event.getResult().getStatus().name();
        context.scenarioStatus = status;
        switch (event.getResult().getStatus()) {
            case PASSED -> Logger.info(GREEN + "[PASSED] Scenario " + scenarioName + " [PASSED]" + RESET);
            case FAILED -> Logger.info(RED + "[FAILED] Scenario " + scenarioName + " [FAILED]" + RESET);
            case SKIPPED -> Logger.info(YELLOW + "[SKIPPED] Scenario " + scenarioName + " [SKIPPED]" + RESET);
            default -> Logger.info(YELLOW + "[" + status + "] Scenario " + scenarioName +" [" + status + "]" + RESET);
        }
        if (context.recordingStarted && context.recordingId != null) {
            Logger.debug("Recording still active in scenario finish, stopping now");
            stopRecording(context);
        }
        try {
            TestResultCollectorManager.getInstance().getTestResultCollector()
                    .collectCucumberTestResult(scenarioName, status, scenarioExecutionTime);
            Logger.debug("Cucumber scenario result collected: " + scenarioName + " - " + status);
        } catch (Exception e) {
            Logger.warn("Failed to collect Cucumber test result: " + e.getMessage());
        }
        scenarioContext.remove();
    }

    /**
     * Handles test run completion - final cleanup
     */
    private void testRunFinishedHandler(TestRunFinished event) {
        if (!scenarioToRecordingId.isEmpty()) {
            Logger.info(VideoRecordingManager.getDiagnosticInfo());
        }
        scenarioToRecordingId.clear();
        try {
            VideoRecordingManager.forceCleanupAll();
            Logger.info("Video recording resources cleaned up after test run completion");
        } catch (Exception e) {
            Logger.warn("Failed to cleanup video recording resources: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /**
     * Handles step failure - captures screenshot and updates Allure
     */
    private void handleStepFailure(TestStepFinished event, ScenarioContext context) {
        String driverName =DriverFactory.getCurrentDriverConfiguration().getDriverType().getName();
        Reporter.setStepStatus(event.getTestStep().getId().toString(),
                io.qameta.allure.model.Status.FAILED);
        context.failedScreenShot = testFailed(
                driverName,
                context.scenarioName
        );
        Allure.getLifecycle().updateTestCase(stepResult -> {
            if (context.failedScreenShot != null) {
                String description = driverName +
                        " - " + context.scenarioName + " - FAILED";
                Reporter.attachScreenshotToReport(
                        context.failedScreenShot,
                        context.failedScreenShot.getName(),
                        description
                );
                context.failedScreenShot = null;
            }
            GeneralHandler.addAttachments();
            stepResult.setStatus(io.qameta.allure.model.Status.FAILED);
        });
    }

    /**
     * Handles step success - updates Allure status
     */
    private void handleStepSuccess( io.qameta.allure.model.Status status) {
        Allure.getLifecycle().updateTestCase(stepResult -> {
            GeneralHandler.addAttachments();
            stepResult.setStatus(status);
        });
    }

    /**
     * Stops recording using VideoRecordingManager (which handles attachment automatically)
     */
    private void stopRecording(ScenarioContext context) {
        if (context.recordingId == null) {
            Logger.debug("No recording ID, skipping video stop");
            return;
        }
        try {
            String recordingStatus = context.scenarioStatus;
            if ("UNKNOWN".equals(recordingStatus)) {
                recordingStatus = "PASSED";
            }
            String videoPath = VideoRecordingManager.stopRecordingById(
                    context.recordingId,
                    recordingStatus
            );
            if (videoPath != null) {
                Logger.debug(GREEN + "Video recording stopped and attached for scenario: " + context.scenarioName + RESET);
            } else {
                Logger.warn(YELLOW+ "Failed to stop video recording (no video path returned)"+ RESET);
            }
            scenarioToRecordingId.remove(context.scenarioId);
            context.recordingStarted = false;
        } catch (Exception e) {
            Logger.error("Failed to stop recording: " + e.getMessage());
            Logger.logException(e);
            tryFallbackStop(context);
        }
    }

    /**
     * Tries fallback methods to stop recording if primary method fails
     */
    private void tryFallbackStop(ScenarioContext context) {
        try {
            Logger.debug("Trying fallback recording stop methods");
            String mappedRecordingId = scenarioToRecordingId.get(context.scenarioId);
            if (mappedRecordingId != null) {
                VideoRecordingManager.stopRecordingById(mappedRecordingId, context.scenarioStatus);
                scenarioToRecordingId.remove(context.scenarioId);
                Logger.info("Recording stopped using mapped ID fallback");
            } else {
                VideoRecordingManager.stopRecordingForCurrentThread(context.scenarioStatus);
                Logger.info("Recording stopped using thread fallback");
            }
        } catch (Exception e2) {
            Logger.error("All fallback methods failed: " + e2.getMessage());
        }
    }

    /**
     * Generates a unique identifier for a scenario.
     * Uses URI and line number to ensure uniqueness even with parallel execution.
     */
    private String getScenarioIdentifier(TestCase testCase) {
        String uri = testCase.getUri().toString();
        int line = testCase.hashCode();
        String name = testCase.getName().replaceAll("[^a-zA-Z0-9]", "_");
        return uri + ":" + line + ":" + name+":"+DriverFactory.getCurrentDriverConfiguration().getDriverType().getName().toUpperCase();
    }
}