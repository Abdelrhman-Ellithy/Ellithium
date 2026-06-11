package Ellithium.core.ai.reporting;

import Ellithium.core.ai.HealingTelemetryStore;
import Ellithium.core.ai.healing.AISelfHealer;
import Ellithium.core.ai.healing.BaselineStore;
import Ellithium.core.ai.models.HealingResult;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Aggregates AI healing events during a test run and generates a Markdown report.
 *
 * <p>The report is ALWAYS generated at suite end, regardless of LOCAL or CI mode.
 * In LOCAL mode it serves as an audit trail; in CI mode it's the primary output
 * since source files are not modified directly.</p>
 */
public class AIHealingReporter {

    private static final ConcurrentLinkedQueue<HealedLocatorEntry> queuedChanges = new ConcurrentLinkedQueue<>();

    /**
     * Queues a healing event with full context for the report.
     */
    public static void queueChange(String filePath, String brokenLocator, HealingResult result,
                                   String pageClassName, String methodName, String actionType, int lineNumber) {
        queuedChanges.add(new HealedLocatorEntry(filePath, brokenLocator, result,
                pageClassName, methodName, actionType, lineNumber));
    }

    /**
     * Generates the healing-report.md file.
     * Call this in the test suite teardown.
     */
    public static void generateReport() {
        HealingTelemetryStore.logConsoleSummary();   // CI-visible, runs even when nothing was patched
        if (queuedChanges.isEmpty()) {
            AISelfHealer.applyDeferredPatches();
            HealingTelemetryStore.flush();
            BaselineStore.flush();
            return;
        }

        File reportDir = new File("Test-Output", "Reports");
        if (!reportDir.exists() && !reportDir.mkdirs()) {
            Reporter.log("Failed to create Reports directory for AI Healing Report", LogLevel.ERROR);
            // Still persist learned state even if the report dir can't be created.
            HealingTelemetryStore.flush();
            BaselineStore.flush();
            return;
        }

        File reportFile = new File(reportDir, "healing-report.md");
        try (FileWriter writer = new FileWriter(reportFile, true)) {
            writer.write("\n---\n\n## Run: "
                    + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n");
            writer.write("The following locators failed during execution and were healed by the AI Engine.\n\n");
            writer.write("| # | File | Method | Action | Broken Locator | Healed Locator | Confidence |\n");
            writer.write("|---|------|--------|--------|----------------|----------------|------------|\n");

            int index = 1;
            for (HealedLocatorEntry entry : queuedChanges) {
                writer.write("| " + index++
                        + " | `" + (entry.filePath != null ? entry.filePath : "unknown") + "`"
                        + " | " + (entry.methodName != null ? entry.methodName : "-")
                        + " | " + (entry.actionType != null ? entry.actionType : "-")
                        + " | `" + entry.brokenLocator + "`"
                        + " | `" + entry.result.getNewLocatorExpression() + "`"
                        + " | " + String.format("%.2f", entry.result.getConfidence())
                        + " |\n");
            }

            writer.write("\n## Detailed Reasoning\n\n");
            index = 1;
            for (HealedLocatorEntry entry : queuedChanges) {
                writer.write("### " + index++ + ". " + entry.brokenLocator + "\n");
                if (entry.pageClassName != null) writer.write("- **Class:** `" + entry.pageClassName + "`\n");
                if (entry.methodName != null) writer.write("- **Method:** `" + entry.methodName + "`\n");
                if (entry.lineNumber > 0) writer.write("- **Line:** " + entry.lineNumber + "\n");
                writer.write("- **Healed to:** `" + entry.result.getNewLocatorExpression() + "`\n");
                if (entry.result.getReasoning() != null && !entry.result.getReasoning().isEmpty()) {
                    writer.write("- **Reasoning:** " + entry.result.getReasoning() + "\n");
                }
                writer.write("\n---\n\n");
            }

            Reporter.log("AI Healing Report generated at: " + reportFile.getAbsolutePath(), LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Failed to write AI Healing Report: " + e.getMessage(), LogLevel.ERROR);
        }

        // Apply deferred source patches (LOCAL mode only — safe since test execution is complete)
        AISelfHealer.applyDeferredPatches();

        // Flush structured telemetry to JSON before baseline flush
        HealingTelemetryStore.flush();

        // Flush baseline fingerprints to disk
        BaselineStore.flush();
    }

    private static class HealedLocatorEntry {
        final String filePath;
        final String brokenLocator;
        final HealingResult result;
        final String pageClassName;
        final String methodName;
        final String actionType;
        final int lineNumber;

        HealedLocatorEntry(String filePath, String brokenLocator, HealingResult result,
                          String pageClassName, String methodName, String actionType, int lineNumber) {
            this.filePath = filePath;
            this.brokenLocator = brokenLocator;
            this.result = result;
            this.pageClassName = pageClassName;
            this.methodName = methodName;
            this.actionType = actionType;
            this.lineNumber = lineNumber;
        }
    }
}
