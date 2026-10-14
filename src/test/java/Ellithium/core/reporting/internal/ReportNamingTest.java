package Ellithium.core.reporting.internal;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class ReportNamingTest {

    @AfterMethod
    public void cleanup() {
        System.clearProperty("allure.report.name");
        System.clearProperty("report.name");
        System.clearProperty("ellithium.suite.name");
    }

    private File createTempProperties(String reportName) throws IOException {
        File temp = Files.createTempFile("allure-test", ".properties").toFile();
        temp.deleteOnExit();
        Properties props = new Properties();
        if (reportName != null) {
            props.setProperty("allure.report.name", reportName);
        }
        try (FileWriter writer = new FileWriter(temp)) {
            props.store(writer, null);
        }
        return temp;
    }

    @Test
    public void testDefaultReportNameDoesNotContainEllithium() throws IOException {
        File props = createTempProperties(null);
        System.setProperty("ellithium.suite.name", "SmokeSuite");

        String name = AllureHelper.resolveReportName(props.getAbsolutePath());
        Assert.assertFalse(name.toLowerCase().contains("ellithium"), "Report name must not contain Ellithium: " + name);
        Assert.assertTrue(name.startsWith("SmokeSuite-"), "Report name should start with suite: " + name);
    }

    @Test
    public void testEllithiumPrefixIsAutomaticallyStripped() throws IOException {
        File props = createTempProperties("Ellithium-{suite}-{timestamp}");
        System.setProperty("ellithium.suite.name", "RegressionSuite");

        String name = AllureHelper.resolveReportName(props.getAbsolutePath());
        Assert.assertFalse(name.toLowerCase().contains("ellithium"), "Ellithium- prefix must be stripped: " + name);
        Assert.assertTrue(name.startsWith("RegressionSuite-"), "Report name should start with suite: " + name);
    }

    @Test
    public void testSystemPropertyOverridesFileConfig() throws IOException {
        File props = createTempProperties("FileSuite-{timestamp}");
        System.setProperty("allure.report.name", "CustomOverride-{date}");

        String name = AllureHelper.resolveReportName(props.getAbsolutePath());
        Assert.assertTrue(name.startsWith("CustomOverride-"), "System property should override file: " + name);
        Assert.assertFalse(name.contains("FileSuite"), "File config should be ignored when sys prop is present");
    }

    @Test
    public void testUnknownSuiteIsCleanedUpGracefully() throws IOException {
        File props = createTempProperties("{suite}-{timestamp}");
        System.setProperty("ellithium.suite.name", "UnknownSuite");

        String name = AllureHelper.resolveReportName(props.getAbsolutePath());
        Assert.assertFalse(name.contains("UnknownSuite"), "UnknownSuite placeholder must be omitted: " + name);
        Assert.assertFalse(name.startsWith("-"), "Name should not start with hyphen: " + name);
    }

    @Test
    public void testSanitizeFileNameWithSpacesAndIllegalChars() {
        Assert.assertEquals(AllureHelper.sanitizeFileName("Fuel Transportation Report"), "Fuel Transportation Report");
        Assert.assertEquals(AllureHelper.sanitizeFileName("Report: Test/Suite *?"), "Report_ Test_Suite __");
        Assert.assertEquals(AllureHelper.sanitizeFileName("Trailing Dots and Spaces...   "), "Trailing Dots and Spaces");
        Assert.assertEquals(AllureHelper.sanitizeFileName(null), "Report");
        Assert.assertEquals(AllureHelper.sanitizeFileName("   "), "Report");
        Assert.assertEquals(AllureHelper.sanitizeFileName("Invalid: <tag> | \"quotes\""), "Invalid_ _tag_ _ _quotes_");
    }

    @Test
    public void testFlushPendingStepSafety() {
        // Calling flushPendingStep when no step is active should be completely safe and not throw
        Ellithium.core.reporting.Reporter.flushPendingStep();
        Ellithium.core.reporting.Reporter.flushPendingStep();
    }
}

