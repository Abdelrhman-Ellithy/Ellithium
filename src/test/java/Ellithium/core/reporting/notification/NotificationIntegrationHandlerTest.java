package Ellithium.core.reporting.notification;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NotificationIntegrationHandlerTest {

    private NotificationIntegrationHandler handler;

    @BeforeMethod
    public void setUp() {
        handler = new NotificationIntegrationHandler();
    }

    // ── helper inner classes ───────────────────────────────────────────────

    static class PlainTest {
        public void someMethod() {}
    }

    static class EllithiumAIFeaturesTest {
        public void someMethod() {}
    }

    // ── isCucumberTestByClass — superclass walk ───────────────────────────

    @Test
    public void isCucumberTest_plainClass_returnsFalse() {
        ITestResult result = mockResult("someNonCucumberTest", PlainTest.class);
        Assert.assertFalse(handler.isCucumberTest(result));
    }

    @Test
    public void isCucumberTest_classWithFeatureInName_returnsFalse() {
        ITestResult result = mockResult("someNonCucumberTest", EllithiumAIFeaturesTest.class);
        Assert.assertFalse(handler.isCucumberTest(result),
                "Class name containing 'feature' must not be misclassified as Cucumber");
    }

    // ── isCucumberTestByName ──────────────────────────────────────────────

    @Test
    public void isCucumberTest_runScenarioExactName_returnsTrue() {
        ITestResult result = mockResult("runScenario", PlainTest.class);
        Assert.assertTrue(handler.isCucumberTest(result));
    }

    @Test
    public void isCucumberTest_runScenarioPrefixed_doesNotMatch() {
        ITestResult result = mockResult("runScenario_extended", PlainTest.class);
        Assert.assertFalse(handler.isCucumberTest(result));
    }

    // ── null guards ────────────────────────────────────────────────────────

    @Test
    public void isCucumberTest_nullTestResult_doesNotThrow() {
        Assert.assertFalse(handler.isCucumberTest(null));
        Assert.assertFalse(handler.shouldExcludeFromTestNGCounting(null));
    }

    @Test
    public void isCucumberTest_nullTestClass_doesNotThrow() {
        ITestResult result = Mockito.mock(ITestResult.class);
        Mockito.when(result.getName()).thenReturn("someTest");
        Mockito.when(result.getTestClass()).thenReturn(null);

        ITestNGMethod method = Mockito.mock(ITestNGMethod.class);
        Mockito.when(result.getMethod()).thenReturn(method);
        Mockito.when(method.getConstructorOrMethod()).thenReturn(null);

        Assert.assertFalse(handler.isCucumberTest(result));
    }

    @Test
    public void collectTestResults_nullContext_doesNotThrow() {
        handler.collectTestResults(null);
    }

    @Test
    public void collectCucumberTestResult_nullParameters_doesNotThrow() {
        handler.collectCucumberTestResult(null, null, 100);
        handler.collectCucumberTestResult("   ", "passed", 200);
    }

    @Test
    public void testGenerateEmailSubject_passedAndFailedOutcomes() {
        // Outcome 1: All passed
        TestResultSummary passedSummary = new TestResultSummary(
                10, 10, 0, 0, 5000,
                Set.of(), List.of(), "Smoke Suite"
        );
        String subjectPassed = handler.generateEmailSubject(passedSummary);
        Assert.assertTrue(subjectPassed.contains("PASSED"));
        Assert.assertTrue(subjectPassed.contains("10 passed, 0 failed"));
        Assert.assertTrue(subjectPassed.contains("[Smoke Suite]"));

        // Outcome 2: With failures and skips
        TestResultSummary failedSummary = new TestResultSummary(
                10, 7, 2, 1, 8000,
                Set.of(), List.of(), "Regression Suite"
        );
        String subjectFailed = handler.generateEmailSubject(failedSummary);
        Assert.assertTrue(subjectFailed.contains("FAILED"));
        Assert.assertTrue(subjectFailed.contains("2 failed, 7 passed, 1 skipped of 10"));
        Assert.assertTrue(subjectFailed.contains("[Regression Suite]"));
    }

    @Test
    public void testGetFailureRate_defaultZero() {
        Assert.assertEquals(handler.getFailureRate(), 0.0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ITestResult mockResult(String testName, Class<?> clazz) {
        ITestResult result = Mockito.mock(ITestResult.class);
        Mockito.when(result.getName()).thenReturn(testName);

        ITestClass testClass = Mockito.mock(ITestClass.class);
        Mockito.doReturn(clazz).when(testClass).getRealClass();
        Mockito.when(result.getTestClass()).thenReturn(testClass);

        ITestNGMethod method = Mockito.mock(ITestNGMethod.class);
        Mockito.when(result.getMethod()).thenReturn(method);
        Mockito.when(method.getConstructorOrMethod()).thenReturn(null);

        return result;
    }
}
