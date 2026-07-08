package Ellithium.core.reporting;

import Ellithium.config.management.ConfigContext;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.internal.Colors;
import Ellithium.core.logging.LogLevel;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private record PendingStep(String uuid, long start) {}
    private static final ThreadLocal<PendingStep> PENDING_STEP = new ThreadLocal<>();

    /**
     * Logs a message with specified log level and additional parameter.
     * @param message The main message to log
     * @param logLevel The logging level
     * @param additionalParameter Additional context or data
     */
    public static void log(String message, LogLevel logLevel, String additionalParameter) {
        String coloredMessage = logMap.get(logLevel) + message + additionalParameter + Colors.RESET;
        logByLevel(logLevel, coloredMessage);
        if (ConfigContext.isOnExecution() && shouldAttachToReport(logLevel)) {
            long now = System.currentTimeMillis();
            closePendingStep(now);
            openPendingStep(message + additionalParameter, logLevel, now);
        }
    }

    /**
     * As {@link #log(String, LogLevel, String)}, but records the step with an explicit start
     * time, so its reported duration is the real elapsed time since {@code startMillis}.
     */
    public static void log(String message, LogLevel logLevel, String additionalParameter, long startMillis) {
        String coloredMessage = logMap.get(logLevel) + message + additionalParameter + Colors.RESET;
        logByLevel(logLevel, coloredMessage);
        if (ConfigContext.isOnExecution() && shouldAttachToReport(logLevel)) {
            closePendingStep(startMillis);
            String uuid = UUID.randomUUID().toString();
            StepResult result = new StepResult()
                    .setName(message + additionalParameter)
                    .setStatus(allureStatusMap.get(logLevel));
            // AllureLifecycle#startStep unconditionally overwrites StepResult.start with "now" —
            // any pre-set value is silently discarded — so the real start time must be restored
            // afterward via updateStep, which applies the given mutation directly with no such override.
            Allure.getLifecycle().startStep(uuid, result);
            Allure.getLifecycle().updateStep(uuid, sr -> sr.setStart(startMillis));
            Allure.getLifecycle().stopStep(uuid);
        }
    }

    private static void openPendingStep(String name, LogLevel logLevel, long start) {
        String uuid = UUID.randomUUID().toString();
        StepResult result = new StepResult().setName(name).setStatus(allureStatusMap.get(logLevel));
        Allure.getLifecycle().startStep(uuid, result);
        Allure.getLifecycle().updateStep(uuid, sr -> sr.setStart(start));
        PENDING_STEP.set(new PendingStep(uuid, start));
    }

    private static void closePendingStep(long stopAt) {
        PendingStep pending = PENDING_STEP.get();
        if (pending == null) return;
        PENDING_STEP.remove();
        Allure.getLifecycle().updateStep(pending.uuid(), sr -> sr.setStop(Math.max(stopAt, pending.start())));
        Allure.getLifecycle().stopStep(pending.uuid());
    }

    /**
     * Closes any step left open by {@link #log(String, LogLevel, String)} on the calling thread,
     * without opening a new one. Call at fixture/test/step boundaries.
     */
    public static void flushPendingStep() {
        if (ConfigContext.isOnExecution()) {
            closePendingStep(System.currentTimeMillis());
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

    private static final org.apache.logging.log4j.Logger LOG4J = org.apache.logging.log4j.LogManager.getLogger(Reporter.class);

    /**
     * Checks whether a log message at the given level should be attached to the Allure report.
     * Maps Ellithium LogLevel to Log4j2 Level and checks against the effective logger threshold.
     * DEBUG/TRACE messages are excluded from the report when the logger is at INFO or above.
     */
    private static boolean shouldAttachToReport(LogLevel logLevel) {
        return switch (logLevel) {
            case TRACE -> LOG4J.isTraceEnabled();
            case DEBUG -> LOG4J.isDebugEnabled();
            // INFO variants, WARN, ERROR always show in the report
            default -> true;
        };
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
     * @param description description for the test attachment
     */
    public static void attachScreenshotToReport(File screenshot, String name, String description ){
        try (FileInputStream fis = new FileInputStream(screenshot)) {
            // Without this, the attachment lands on whatever step log() last left open/pending
            // (this call has no log() of its own to open a step for it), instead of at the
            // current test/fixture level.
            flushPendingStep();
            Allure.addAttachment(name + (description != null && !description.isEmpty() ? " - " + description : ""), "image/png", fis, ".png");
        }catch (IOException e) {
            Logger.logException(e);
        }
    }

    /**
     * Attaches any file (image, video, log, json...) to the Allure test report.
     * Automatically detects the MIME type and file extension.
     *
     * @param file File to attach
     * @param name Name of the attachment in the report
     */
    public static void attachFileToReport(File file, String name) {
        if (file == null || !file.exists()) {
            log("Attachment failed: file does not exist -> " + file, LogLevel.ERROR);
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream"; // fallback
            }
            String fileName = file.getName();
            String extension = "";
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex != -1) {
                extension = fileName.substring(dotIndex); // includes the dot
            }
            flushPendingStep();
            Allure.addAttachment(name, mimeType, fis, extension);
        } catch (IOException e) {
            Logger.logException(e);
        }
    }
    /**
     * Attaches a screenshot to the test report using a file path.
     * * @param screenshotPath Relative or absolute path to the screenshot file
     * @param name Screenshot name in the report
     * @param description Description for the test attachment
     */
    public static void attachScreenshotToReport(String screenshotPath, String name, String description) {
        if (screenshotPath == null || screenshotPath.isEmpty()) {
            log("Screenshot attachment failed: path is null or empty.", LogLevel.ERROR);
            return;
        }
        attachScreenshotToReport(new File(screenshotPath), name, description);
    }

    /**
     * Attaches any file (image, video, log, json...) to the Allure test report using a file path.
     * Automatically detects the MIME type and file extension.
     *
     * @param filePath Relative or absolute path to the file
     * @param name Name of the attachment in the report
     */
    public static void attachFileToReport(String filePath, String name) {
        if (filePath == null || filePath.isEmpty()) {
            log("File attachment failed: path is null or empty.", LogLevel.ERROR);
            return;
        }
        attachFileToReport(new File(filePath), name);
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
