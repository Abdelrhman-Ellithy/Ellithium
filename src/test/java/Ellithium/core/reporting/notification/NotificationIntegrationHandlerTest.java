package Ellithium.core.reporting.notification;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

    // ── null guard ────────────────────────────────────────────────────────

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
