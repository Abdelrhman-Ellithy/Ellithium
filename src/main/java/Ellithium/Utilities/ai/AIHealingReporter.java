package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.models.HealingResult;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Aggregates AI healing suggestions during a test run.
 * Critical for CI/CD environments where direct file modification is disabled.
 * Generates a comprehensive Markdown report at the end of the suite.
 */
public class AIHealingReporter {

    private static final ConcurrentLinkedQueue<HealedLocatorEntry> queuedChanges = new ConcurrentLinkedQueue<>();

    /**
     * Queues a healing suggestion for the report.
     */
    public static void queueChange(String filePath, String brokenLocator, HealingResult result) {
        queuedChanges.add(new HealedLocatorEntry(filePath, brokenLocator, result));
        Reporter.log("Healing suggestion queued for report: " + brokenLocator + " -> " + result.getNewLocatorExpression(), LogLevel.INFO_YELLOW);
    }

    /**
     * Generates the healing-report.md file.
     * Call this in the test suite teardown (e.g., GeneralHandler.onExecutionFinish or CustomTestNGListener.onExecutionFinish).
     */
    public static void generateReport() {
        if (queuedChanges.isEmpty()) {
            return;
        }

        File reportDir = new File("Test-Output", "Reports");
        if (!reportDir.exists() && !reportDir.mkdirs()) {
            Reporter.log("Failed to create Reports directory for AI Healing Report", LogLevel.ERROR);
            return;
        }

        File reportFile = new File(reportDir, "healing-report.md");
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("# Ellithium AI Healing Report\n\n");
            writer.write("Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n");
            writer.write("The following locators failed during execution and were successfully healed by the AI Engine. ");
            writer.write("Since the framework is running in CI mode, these changes were not written to the source code.\n\n");
            writer.write("## Suggested Fixes\n\n");

            for (HealedLocatorEntry entry : queuedChanges) {
                writer.write("### File: `" + entry.filePath + "`\n");
                writer.write("- **Broken Locator:** `" + entry.brokenLocator + "`\n");
                writer.write("- **Suggested Locator:** `" + entry.result.getNewLocatorExpression() + "`\n");
                writer.write("- **Confidence:** `" + String.format("%.2f", entry.result.getConfidence()) + "`\n");
                if (entry.result.getReasoning() != null && !entry.result.getReasoning().isEmpty()) {
                    writer.write("- **Reasoning:** " + entry.result.getReasoning() + "\n");
                }
                writer.write("\n---\n\n");
            }

            Reporter.log("AI Healing Report generated at: " + reportFile.getAbsolutePath(), LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Failed to write AI Healing Report: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    private static class HealedLocatorEntry {
        final String filePath;
        final String brokenLocator;
        final HealingResult result;

        HealedLocatorEntry(String filePath, String brokenLocator, HealingResult result) {
            this.filePath = filePath;
            this.brokenLocator = brokenLocator;
            this.result = result;
        }
    }
}
