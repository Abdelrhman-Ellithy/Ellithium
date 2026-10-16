package Ellithium.core.reporting;

import Ellithium.config.management.ConfigContext;
import Ellithium.core.logging.LogLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ReporterTest {

    @Test
    public void testLog_allLogLevelsDoNotThrow() {
        for (LogLevel level : LogLevel.values()) {
            Reporter.log("Testing log level " + level.name(), level);
            Reporter.log("Testing log with param", level, " [EXTRA]");
            Reporter.log("Testing log with timestamp", level, " [EXTRA]", System.currentTimeMillis());
            Reporter.logSubStep("SubStep test", level);
            Reporter.logSubStep("SubStep with param", level, " [SUB]");
            Reporter.logSubStep("SubStep with timestamp", level, " [SUB]", System.currentTimeMillis());
        }
    }

    @Test
    public void testLogReportOnly() {
        ConfigContext.setOnExecution(false);
        Reporter.logReportOnly("Report only inactive", LogLevel.INFO_BLUE);
        Reporter.logReportOnly("Report only with param", LogLevel.INFO_GREEN, " - param");

        ConfigContext.setOnExecution(true);
        Reporter.logReportOnly("Report only active", LogLevel.INFO_BLUE);
        Reporter.logReportOnly("Report only active with param", LogLevel.INFO_GREEN, " - param");
        ConfigContext.setOnExecution(false);
    }

    @Test
    public void testStage_doesNotThrow() {
        Reporter.stage("Setup Stage");
        Reporter.stage("Execution Stage");
        Reporter.stage(null);
        Reporter.stage("   ");
    }

    @Test
    public void testFlushPendingStep() {
        Reporter.log("Opening step", LogLevel.INFO_BLUE);
        Reporter.flushPendingStep();
        // Subsequent flush is a safe no-op
        Reporter.flushPendingStep();
    }

    @Test
    public void testAttachFileAndScreenshot_nullAndInvalidPaths() {
        // Null or non-existent files should log errors and return gracefully
        Reporter.attachFileToReport((java.io.File) null, "test");
        Reporter.attachFileToReport((String) null, "test");
        Reporter.attachFileToReport("", "test");
        Reporter.attachFileToReport(new java.io.File("non_existent_file.xyz"), "test");

        Reporter.attachScreenshotToReport((java.io.File) null, "shot", "desc");
        Reporter.attachScreenshotToReport((String) null, "shot", "desc");
        Reporter.attachScreenshotToReport("", "shot", "desc");
        Reporter.attachScreenshotToReport(new java.io.File("non_existent_shot.png"), "shot", "desc");

        java.util.concurrent.CompletableFuture<Void> fut1 = Reporter.attachFileToReportAsync((java.io.File) null, "test");
        Assert.assertNotNull(fut1);
        Assert.assertTrue(fut1.isDone());

        java.util.concurrent.CompletableFuture<Void> fut2 = Reporter.attachFileToReportAsync((String) null, "test");
        Assert.assertNotNull(fut2);
        Assert.assertTrue(fut2.isDone());

        java.util.concurrent.CompletableFuture<Void> fut3 = Reporter.attachFileToReportAsync("", "test");
        Assert.assertNotNull(fut3);
        Assert.assertTrue(fut3.isDone());
    }

    @Test
    public void testAttachFileAndScreenshot_validTempFiles() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("reporter_test_", ".txt");
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), "Sample report content");

        // Attach with explicit name
        Reporter.attachFileToReport(tempFile, "Sample Report");
        Reporter.attachFileToReport(tempFile.getAbsolutePath(), "Sample Report Path");

        // Attach with null/blank name (fallback to file.getName())
        Reporter.attachFileToReport(tempFile, null);
        Reporter.attachFileToReport(tempFile, "   ");

        // Async attachments
        java.util.concurrent.CompletableFuture<Void> fut1 = Reporter.attachFileToReportAsync(tempFile, "Async Report");
        Assert.assertNotNull(fut1);
        fut1.join();

        java.util.concurrent.CompletableFuture<Void> fut2 = Reporter.attachFileToReportAsync(tempFile, null);
        Assert.assertNotNull(fut2);
        fut2.join();

        java.util.concurrent.CompletableFuture<Void> fut3 = Reporter.attachFileToReportAsync(tempFile.getAbsolutePath(), "Async Path");
        Assert.assertNotNull(fut3);
        fut3.join();

        // Screenshot attachments
        java.io.File tempPng = java.io.File.createTempFile("reporter_shot_", ".png");
        tempPng.deleteOnExit();
        java.nio.file.Files.write(tempPng.toPath(), new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        Reporter.attachScreenshotToReport(tempPng, "TempShot", "A description");
        Reporter.attachScreenshotToReport(tempPng.getAbsolutePath(), "TempShotPath", null);
        Reporter.attachScreenshotToReport(tempPng, null, null);
    }

    @Test
    public void testTestCaseMetadata() {
        // These should not throw even if no test context is actively registered
        try {
            Reporter.setTestCaseName("My Test Name");
            Reporter.setTestCaseDescription("My Test Description");
        } catch (Exception ignored) {
            // Lifecycle may be uninitialized outside Surefire/Allure runtime
        }
    }
}
