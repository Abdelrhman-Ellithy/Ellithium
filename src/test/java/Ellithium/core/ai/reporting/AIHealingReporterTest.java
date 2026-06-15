package Ellithium.core.ai.reporting;

import Ellithium.core.ai.models.HealingResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AIHealingReporterTest {

    @Test
    public void queueChange_doesNotThrow() {
        HealingResult result = new HealingResult("By.id(\"new-btn\")", 0.91, "Exact id match");
        AIHealingReporter.queueChange(
                "src/test/java/pages/LoginPage.java",
                "By.id(\"old-btn\")",
                result,
                "LoginPage",
                "clickLogin",
                "click",
                42);
        // No assertion needed — absence of exception is the contract
    }

    @Test
    public void queueChange_withNullFilePath_doesNotThrow() {
        HealingResult result = new HealingResult("By.cssSelector(\".btn\")", 0.8, "class match");
        // Should not throw even with null file path
        AIHealingReporter.queueChange(null, "By.id(\"x\")", result, null, null, null, 0);
    }

    @Test
    public void generateReport_withNoQueuedChanges_doesNotThrow() {
        // Empty queue — generateReport should flush telemetry + baseline and return without creating a report file
        AIHealingReporter.generateReport();
    }
}
