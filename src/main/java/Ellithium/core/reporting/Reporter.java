package Ellithium.core.reporting;

import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.internal.Colors;
import Ellithium.core.logging.LogLevel;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import static Ellithium.core.logging.LogLevel.*;
import static Ellithium.core.logging.Logger.*;

/**
 * Handles test reporting and logging functionality with Allure integration.
 */
public class Reporter {
    private static final Map<LogLevel, String> logMap = Map.ofEntries(
            Map.entry(INFO_BLUE, Colors.BLUE),
            Map.entry(INFO_GREEN, Colors.GREEN),
            Map.entry(INFO_RED, Colors.RED),
            Map.entry(INFO_YELLOW, Colors.YELLOW),
            Map.entry(ERROR, Colors.RED),
            Map.entry(TRACE, Colors.BLUE),
            Map.entry(WARN, Colors.PINK),
            Map.entry(DEBUG, Colors.YELLOW)
    );
    private static final Map<LogLevel, Status> allureStatusMap = Map.ofEntries(
            Map.entry(INFO_BLUE, Status.PASSED),
            Map.entry(INFO_GREEN, Status.PASSED),
            Map.entry(INFO_RED, Status.FAILED),
            Map.entry(INFO_YELLOW, Status.SKIPPED),
            Map.entry(ERROR, Status.FAILED),
            Map.entry(TRACE, Status.PASSED),
            Map.entry(WARN, Status.PASSED),
            Map.entry(DEBUG, Status.PASSED)
    );

    /**
     * Logs a message with specified log level and additional parameter.
     * @param message The main message to log
     * @param logLevel The logging level
     * @param additionalParameter Additional context or data
     */
    public static void log(String message, LogLevel logLevel, String additionalParameter) {
        String coloredMessage = logMap.get(logLevel) + message + additionalParameter + Colors.RESET;
        logByLevel(logLevel, coloredMessage);
        if (ConfigContext.isOnExecution()) {
            Allure.step(message + additionalParameter, allureStatusMap.get(logLevel));
        }
    }

    /**
     * Handles logging based on log level.
     * @param logLevel The logging level
     * @param message The message to log
     */
    private static void logByLevel(LogLevel logLevel, String message) {
        switch (logLevel) {
            case INFO_BLUE, INFO_GREEN, INFO_RED, INFO_YELLOW -> info(message);
            case ERROR -> error(message);
            case TRACE -> trace(message);
            case WARN -> warn(message);
            case DEBUG -> debug(message);
            default ->  {}
        }
    }

    /**
     * Logs only to the report without console output.
     * @param message The message to log
     * @param logLevel The logging level
     * @param additionalParameter Additional context or data
     */
    public static void logReportOnly(String message, LogLevel logLevel, String additionalParameter) {
        if (ConfigContext.isOnExecution()) {
            Allure.step(message + additionalParameter, allureStatusMap.get(logLevel));
        }
    }

    /**
     * Logs only to the report without additional parameter.
     * @param message The message to log
     * @param logLevel The logging level
     */
    public static void logReportOnly(String message, LogLevel logLevel){
        logReportOnly(message,logLevel,"");
    }

    /**
     * Logs a message without additional parameter.
     * @param message The message to log
     * @param logLevel The logging level
     */
    public static void log(String message, LogLevel logLevel){
            log(message,logLevel,"");
    }

    /**
     * Attaches a screenshot to the test report.
     * @param screenshot The screenshot file
     * @param name Screenshot name in the report
     * @param browserName Browser used for test
     * @param testName Name of the test
     */
    public static void attachScreenshotToReport(File screenshot, String name, String browserName, String testName){
        try (FileInputStream fis = new FileInputStream(screenshot)) {
            Allure.description(browserName.toUpperCase() + "-" + testName + " FAILED");
            Allure.addAttachment(name, "image/png", fis, ".png");
        }catch (IOException e) {
            Logger.logException(e);
        }
    }

    /**
     * Sets the test case name in the report.
     * @param Name Test case name
     */
    public static void setTestCaseName(String Name) {
        Allure.getLifecycle().updateTestCase(testResult -> testResult.setName(Name));
    }

    /**
     * Sets the test case description in the report.
     * @param description Test case description
     */
    public static void setTestCaseDescription(String description) {
        Allure.getLifecycle().updateTestCase(testResult -> testResult.setDescriptionHtml(description));
    }

    /**
     * Updates the status of a test step.
     * @param uuid Step identifier
     * @param status New status
     */
    public static void setStepStatus(String uuid,Status status) {
        Allure.getLifecycle().updateStep(uuid, stepResult -> stepResult.setStatus(status));
    }

    /**
     * Sets the name of the current test step.
     * @param name Step name
     */
    public static void setStepName(String name) {
        Allure.getLifecycle().updateStep(stepResult -> stepResult.setName(name));
    }

    /**
     * Sets the name of a test hook.
     * @param name Hook name
     */
    public static void setHookName(String name) {
        Allure.getLifecycle().updateFixture(fixtureResult -> fixtureResult.setName(name));
    }

    /**
     * Adds parameters to the test case.
     * @param parameters List of parameters to add
     */
    public static void addParams(List<Parameter>parameters) {
        Allure.getLifecycle().updateTestCase(testResult ->{
            testResult.setParameters(parameters);
        });
    }
}
