package Ellithium.core.execution.listener;

import Ellithium.config.managment.ConfigContext;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Ellithium.config.managment.GeneralHandler.testFailed;
import static Ellithium.core.reporting.internal.Colors.*;

/**
 * Cucumber listener that integrates with the Ellithium framework.
 * Extends AllureCucumber7Jvm for Allure reporting integration.
 * Collects test results for notification system integration.
 * This listener is responsible for counting Cucumber scenarios only.
 */
public class CucumberListener extends AllureCucumber7Jvm {
    private static String scenarioName;
    private static File failedScreenShot;
    private static Boolean paramAdded;
    private static long scenarioStartTime;

    /**
     * Thread-local storage for scenario context
     */
    private static final ThreadLocal<ScenarioContext> scenarioContext = new ThreadLocal<>();

    /**
     * Maps scenario URI+Line to Recording ID for tracking across parallel execution
     * Key: Scenario unique identifier (URI + Line), Value: Recording ID
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

        public ScenarioContext(String scenarioName, String scenarioId) {
            this.scenarioName = scenarioName;
            this.scenarioId = scenarioId;
            this.paramAdded = false;
            this.scenarioStartTime = System.currentTimeMillis();
        }
    }
    
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::testStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, this::stepFinishedHandler);
        publisher.registerHandlerFor(TestStepStarted.class, this::stepStartedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class, this::testFinishedHandler);
        super.setEventPublisher(publisher);
    }
    
    private void testStartedHandler(TestCaseStarted event) {
        String name = event.getTestCase().getName();
        String scenarioId = getScenarioIdentifier(event.getTestCase());
        ScenarioContext context = new ScenarioContext(name, scenarioId);
        scenarioContext.set(context);
        Logger.info(CYAN + "[START] " + BLUE + " Scenario " + scenarioName + " [START]" + RESET);
        paramAdded = false;
        boolean driverExecution=(DriverFactory.getCurrentDriver() != null);
        boolean headless= ConfigContext.getHeadlessMode() == HeadlessMode.False;
        if (driverExecution && headless) {
            try {
                String recordingId = VideoRecordingManager.startRecording(name, scenarioId);
                if (recordingId != null) {
                    context.recordingId = recordingId;
                    scenarioToRecordingId.put(scenarioId, recordingId);
                }
            } catch (Exception e) {
                Logger.logException(e);
            }
        }
    }
    
    private void testFinishedHandler(TestCaseFinished event) {
        scenarioName = event.getTestCase().getName();
        long scenarioExecutionTime = System.currentTimeMillis() - scenarioStartTime;
        String status = event.getResult().getStatus().name();
        switch (event.getResult().getStatus()) {
            case PASSED -> Logger.info(GREEN + "[PASSED] Scenario " + scenarioName + " [PASSED]" + RESET);
            case FAILED -> Logger.info(RED + "[FAILED] Scenario " + scenarioName + " [FAILED]" + RESET);
            case SKIPPED -> Logger.info(YELLOW + "[SKIPPED] Scenario " + scenarioName + " [SKIPPED]" + RESET);
        }
        try {
            TestResultCollectorManager.getInstance().getTestResultCollector()
                .collectCucumberTestResult(scenarioName, status, scenarioExecutionTime);
            Logger.info("Cucumber scenario result collected: " + scenarioName + " - " + status);
        } catch (Exception e) {
            Logger.warn("Failed to collect Cucumber test result for notification system: " + e.getMessage());
        }
        paramAdded = false;
    }
    
    private void stepFinishedHandler(TestStepFinished event) {
        var result = event.getResult().getStatus();
        if (event.getTestStep() instanceof PickleStepTestStep) {
            if (DriverFactory.getCurrentDriver() != null && (result == Status.FAILED)) {
                Reporter.setStepStatus(event.getTestStep().getId().toString(), io.qameta.allure.model.Status.FAILED);
                failedScreenShot = testFailed(ConfigContext.getValue(ConfigContext.getDriverType()), scenarioName);
                Allure.getLifecycle().updateTestCase(stepResult -> {
                    if (failedScreenShot != null) {
                        String description = ConfigContext.getValue(ConfigContext.getDriverType()).toUpperCase() + "-" + scenarioName + " FAILED";
                        Reporter.attachScreenshotToReport(failedScreenShot, failedScreenShot.getName(), description);
                        failedScreenShot = null;
                    }
                    GeneralHandler.addAttachments();
                    stepResult.setStatus(io.qameta.allure.model.Status.FAILED);
                });
            } else {
                Allure.getLifecycle().updateTestCase(stepResult -> {
                    GeneralHandler.addAttachments();
                    stepResult.setStatus(io.qameta.allure.model.Status.PASSED);
                });
            }
        }
        if (!paramAdded) {
            Reporter.addParams(GeneralHandler.getParameters());
            paramAdded = true;
        }
    }
    private void testRunFinishedHandler(TestRunFinished event) {
        // Print diagnostic info if there are orphaned recordings
        if (!scenarioToRecordingId.isEmpty()) {
            Logger.info(YELLOW + "Warning: Orphaned scenario recordings detected: " +
                    scenarioToRecordingId.size() + RESET);
            Logger.info(VideoRecordingManager.getDiagnosticInfo());
        }

        // Clean up any orphaned mappings
        scenarioToRecordingId.clear();

        // Force cleanup all video recording resources at the end of test run
        try {
            VideoRecordingManager.forceCleanupAll();
            Logger.info("Video recording resources cleaned up after test run completion");
        } catch (Exception e) {
            Logger.warn("Failed to cleanup video recording resources: " + e.getMessage());
        }
    }

    /**
     * Generates a unique identifier for a scenario.
     * Uses URI and line number to ensure uniqueness even with parallel execution.
     * @param testCase Cucumber test case
     * @return Unique scenario identifier
     */
    private String getScenarioIdentifier(TestCase testCase) {
        // Use URI + Line to create a unique identifier
        // This ensures the same scenario can be identified across multiple executions
        String uri = testCase.getUri().toString();
        int line = testCase.getLine();

        // Also include the scenario name for better readability
        String name = testCase.getName().replaceAll("[^a-zA-Z0-9]", "_");

        return uri + ":" + line + ":" + name;
    }
    private void stepStartedHandler(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            Logger.clearCurrentExecutionLogs();
        }
    }
}