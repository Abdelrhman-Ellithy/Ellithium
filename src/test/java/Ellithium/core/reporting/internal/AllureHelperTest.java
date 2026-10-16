package Ellithium.core.reporting.internal;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class AllureHelperTest {

    @AfterMethod
    public void tearDown() {
        System.clearProperty("allure.report.name");
        System.clearProperty("report.name");
        System.clearProperty("ellithium.suite.name");
    }

    @Test
    public void testSanitizeFileName_nullAndBlank() {
        Assert.assertEquals(AllureHelper.sanitizeFileName(null), "Report");
        Assert.assertEquals(AllureHelper.sanitizeFileName(""), "Report");
        Assert.assertEquals(AllureHelper.sanitizeFileName("   "), "Report");
    }

    @Test
    public void testSanitizeFileName_replacesForbiddenCharacters() {
        String input = "test/report:name*with?bad\"chars<and>pipes|";
        String sanitized = AllureHelper.sanitizeFileName(input);
        Assert.assertFalse(sanitized.contains("/"));
        Assert.assertFalse(sanitized.contains(":"));
        Assert.assertFalse(sanitized.contains("*"));
        Assert.assertFalse(sanitized.contains("?"));
        Assert.assertFalse(sanitized.contains("\""));
        Assert.assertFalse(sanitized.contains("<"));
        Assert.assertFalse(sanitized.contains(">"));
        Assert.assertFalse(sanitized.contains("|"));
    }

    @Test
    public void testSanitizeFileName_trimsTrailingDotsAndSpaces() {
        String input = "my_report...   ";
        String sanitized = AllureHelper.sanitizeFileName(input);
        Assert.assertEquals(sanitized, "my_report");
    }

    @Test
    public void testResolveReportName_fromSystemProperty() {
        System.setProperty("allure.report.name", "SmokeSuite-{date}");
        String name = AllureHelper.resolveReportName("non_existent.properties");
        Assert.assertTrue(name.startsWith("SmokeSuite-"));
        Assert.assertFalse(name.contains("{date}"));
    }

    @Test
    public void testResolveReportName_stripsEllithiumPrefix() {
        System.setProperty("allure.report.name", "ellithium-RegressionTest");
        String name = AllureHelper.resolveReportName("non_existent.properties");
        Assert.assertEquals(name, "RegressionTest");
    }

    @Test
    public void testResolveReportName_interpolatesSuite() {
        System.setProperty("ellithium.suite.name", "CheckoutSuite");
        System.setProperty("allure.report.name", "{suite}-Run");
        String name = AllureHelper.resolveReportName("non_existent.properties");
        Assert.assertEquals(name, "CheckoutSuite-Run");
    }

    @Test
    public void testDeleteAllureResultsDir_doesNotThrow() {
        AllureHelper.deleteAllureResultsDir();
    }

    @Test
    public void testAddEnvironmentDetailsToReport_doesNotThrow() {
        AllureHelper.addEnvironmentDetailsToReport();
    }
}
