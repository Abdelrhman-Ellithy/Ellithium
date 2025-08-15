package Ellithium.core.execution.listener;

import Ellithium.config.managment.ConfigContext;
import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.reporting.notification.TestResultCollectorManager;
import io.cucumber.plugin.event.*;
import io.qameta.allure.Allure;
import io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm;
import java.io.File;
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
    
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::testStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, this::stepFinishedHandler);
        publisher.registerHandlerFor(TestStepStarted.class, this::stepStartedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class, this::testFinishedHandler);
        super.setEventPublisher(publisher);
    }
    
    private void testStartedHandler(TestCaseStarted event) {
        scenarioName = event.getTestCase().getName();
        scenarioStartTime = System.currentTimeMillis();
        Logger.info(CYAN + "[START] " + BLUE + " Scenario " + scenarioName + " [START]" + RESET);
        paramAdded = false;
    }
    
    private void testFinishedHandler(TestCaseFinished event) {
        scenarioName = event.getTestCase().getName();
        long scenarioExecutionTime = System.currentTimeMillis() - scenarioStartTime;
        String status = event.getResult().getStatus().name();
        
        // Log the scenario result
        switch (event.getResult().getStatus()) {
            case PASSED -> Logger.info(GREEN + "[PASSED] Scenario " + scenarioName + " [PASSED]" + RESET);
            case FAILED -> Logger.info(RED + "[FAILED] Scenario " + scenarioName + " [FAILED]" + RESET);
            case SKIPPED -> Logger.info(YELLOW + "[SKIPPED] Scenario " + scenarioName + " [SKIPPED]" + RESET);
        }
        
        // Collect test result for notification system
        // This ensures Cucumber scenarios are counted only once
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
    
    private void stepStartedHandler(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            Logger.clearCurrentExecutionLogs();
        }
    }
}