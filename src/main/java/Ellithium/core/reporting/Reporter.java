package Ellithium.core.reporting;

import Ellithium.config.management.ConfigContext;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.internal.Colors;
import Ellithium.core.logging.LogLevel;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AttachmentOptions;
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

    private record PendingStep(AllureExternalKey key, long start) {}
    private static final ThreadLocal<PendingStep> PENDING_STEP = new ThreadLocal<>();

    /**
     * Sets the current execution stage in the Allure report (e.g. "Setup", "Action", "Verification", "AI Self-Healing").
     *
     * @param stageName The name of the test execution stage
     */
    public static void stage(String stageName) {
        if (stageName != null && !stageName.isBlank()) {
            try {
                flushPendingStep();
                Allure.stage(stageName);
            } catch (Exception ignored) {
                // Ignore if called outside an active Allure test context
            }
        }
    }

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
     * time, logging it as a sub-step under any currently open step without closing the parent step.
     */
    public static void log(String message, LogLevel logLevel, String additionalParameter, long startMillis) {
        logSubStep(message, logLevel, additionalParameter, startMillis);
    }

    /**
     * Logs a sub-step to the console/logger and attaches it as a sub-step under the currently
     * active step (e.g. pending action step, Cucumber step, or @Step) without closing the parent step.
     */
    public static void logSubStep(String message, LogLevel logLevel, String additionalParameter, long startMillis) {
        String coloredMessage = logMap.get(logLevel) + message + additionalParameter + Colors.RESET;
        logByLevel(logLevel, coloredMessage);
        if (ConfigContext.isOnExecution() && shouldAttachToReport(logLevel)) {
            AllureExternalKey key = AllureExternalKey.random(StepResult.class);
            StepResult result = new StepResult()
                    .setName(message + additionalParameter)
                    .setStatus(allureStatusMap.get(logLevel));
            Allure.getLifecycle().startStep(key, result);
            if (startMillis > 0) {
                Allure.getLifecycle().updateStep(key, sr -> sr.setStart(startMillis));
            }
            Allure.getLifecycle().stopStep();
        }
    }

    public static void logSubStep(String message, LogLevel logLevel, String additionalParameter) {
        logSubStep(message, logLevel, additionalParameter, 0);
    }

    public static void logSubStep(String message, LogLevel logLevel) {
        logSubStep(message, logLevel, "", 0);
    }

    private static void openPendingStep(String name, LogLevel logLevel, long start) {
        AllureExternalKey key = AllureExternalKey.random(StepResult.class);
        StepResult result = new StepResult().setName(name).setStatus(allureStatusMap.get(logLevel));
        Allure.getLifecycle().startStep(key, result);
        Allure.getLifecycle().updateStep(key, sr -> sr.setStart(start));
        PENDING_STEP.set(new PendingStep(key, start));
    }

    private static void closePendingStep(long stopAt) {
        PendingStep pending = PENDING_STEP.get();
        if (pending == null) return;
        PENDING_STEP.remove();
        try {
            Allure.getLifecycle().updateStep(pending.key(), sr -> sr.setStop(Math.max(stopAt, pending.start())));
            Allure.getLifecycle().stopStep();
        } catch (Exception e) {
            Logger.debug("Could not stop pending Allure step: " + e.getMessage());
        }
    }

    /**
     * Closes any step left open by {@link #log(String, LogLevel, String)} on the calling thread,
     * without opening a new one. Call at fixture/test/step boundaries.
     */
    public static void flushPendingStep() {
        closePendingStep(System.currentTimeMillis());
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
            flushPendingStep();
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
        if (screenshot == null || !screenshot.exists()) {
            log("Screenshot attachment failed: file does not exist -> " + screenshot, LogLevel.ERROR);
            return;
        }
        try (FileInputStream fis = new FileInputStream(screenshot)) {
            // Without this, the attachment lands on whatever step log() last left open/pending
            // (this call has no log() of its own to open a step for it), instead of at the
            // current test/fixture level.
            flushPendingStep();
            String attachmentName = (name != null && !name.isBlank()) ? name : screenshot.getName();
            Allure.attachment(attachmentName + (description != null && !description.isEmpty() ? " - " + description : ""), "image/png", fis, AttachmentOptions.withFileExtension(".png"));
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
            String attachmentName = (name != null && !name.isBlank()) ? name : fileName;
            Allure.attachment(attachmentName, mimeType, fis, AttachmentOptions.withFileExtension(extension));
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
     * Asynchronously attaches any file (e.g. video recordings) to the Allure test report.
     * Prevents I/O blocking during large file attachments.
     *
     * @param file File to attach
     * @param name Name of the attachment in the report
     * @return CompletableFuture that completes when the file is attached
     */
    public static java.util.concurrent.CompletableFuture<Void> attachFileToReportAsync(File file, String name) {
        if (file == null || !file.exists()) {
            log("Attachment failed: file does not exist -> " + file, LogLevel.ERROR);
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        try {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            String fileName = file.getName();
            String extension = "";
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex != -1) {
                extension = fileName.substring(dotIndex);
            }
            flushPendingStep();
            String attachmentName = (name != null && !name.isBlank()) ? name : fileName;
            java.util.concurrent.CompletableFuture<FileInputStream> streamFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return new FileInputStream(file);
                } catch (IOException e) {
                    throw new java.util.concurrent.CompletionException(e);
                }
            });
            return Allure.attachmentAsync(attachmentName, mimeType, streamFuture, AttachmentOptions.withFileExtension(extension));
        } catch (Exception e) {
            Logger.logException(e);
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Asynchronously attaches any file to the Allure test report using a file path.
     *
     * @param filePath Relative or absolute path to the file
     * @param name Name of the attachment in the report
     * @return CompletableFuture that completes when the file is attached
     */
    public static java.util.concurrent.CompletableFuture<Void> attachFileToReportAsync(String filePath, String name) {
        if (filePath == null || filePath.isEmpty()) {
            log("File attachment failed: path is null or empty.", LogLevel.ERROR);
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        return attachFileToReportAsync(new File(filePath), name);
    }

    /**
     * Sets the test case name in the report.
     * @param Name Test case name
     */
    public static void setTestCaseName(String Name) {
        Allure.getLifecycle().updateTest(testResult -> testResult.setName(Name));
    }

    /**
     * Sets the test case description in the report.
     * @param description Test case description
     */
    public static void setTestCaseDescription(String description) {
        Allure.getLifecycle().updateTest(testResult -> testResult.setDescriptionHtml(description));
    }

    /**
     * Updates the status of a test step.
     * @param uuid Step identifier
     * @param status New status
     */
    public static void setStepStatus(String uuid, Status status) {
        if (uuid != null) {
            Allure.getLifecycle().updateStep(AllureExternalKey.of(StepResult.class, uuid), stepResult -> stepResult.setStatus(status));
        } else {
            Allure.getLifecycle().updateStep(stepResult -> stepResult.setStatus(status));
        }
    }

    /**
     * Updates the status of a test step.
     * @param key Step key
     * @param status New status
     */
    public static void setStepStatus(AllureExternalKey key, Status status) {
        if (key != null) {
            Allure.getLifecycle().updateStep(key, stepResult -> stepResult.setStatus(status));
        } else {
            Allure.getLifecycle().updateStep(stepResult -> stepResult.setStatus(status));
        }
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
    public static void addParams(List<Parameter> parameters) {
        Allure.getLifecycle().updateTest(testResult -> {
            testResult.setParameters(parameters);
        });
    }
}
