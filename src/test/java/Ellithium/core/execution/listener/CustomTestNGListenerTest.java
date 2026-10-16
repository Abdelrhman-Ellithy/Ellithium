package Ellithium.core.execution.listener;

import Ellithium.core.ai.HealingTelemetryStore;
import Ellithium.core.execution.Analyzer.RetryAnalyzer;
import Ellithium.core.execution.context.TestContext;
import org.mockito.Mockito;
import org.testng.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.annotations.ITestAnnotation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;

public class CustomTestNGListenerTest {

    private CustomTestNGListener listener;

    @BeforeMethod
    public void setUp() {
        listener = new CustomTestNGListener();
    }

    @AfterMethod
    public void tearDown() {
        HealingTelemetryStore.clearCurrentTest();
        System.clearProperty("ellithium.suite.name");
    }

    @Test
    public void testTransform_setsRetryAnalyzer() {
        ITestAnnotation annotation = mock(ITestAnnotation.class);
        listener.transform(annotation, null, null, null);
        verify(annotation).setRetryAnalyzer(RetryAnalyzer.class);
    }

    @Test
    public void testOnExecutionStartAndFinish() {
        listener.onExecutionStart();
        Assert.assertTrue(Ellithium.config.management.ConfigContext.isOnExecution());

        listener.onExecutionFinish();
        Assert.assertFalse(Ellithium.config.management.ConfigContext.isOnExecution());
    }

    @Test
    public void testOnStartAndFinishTestContext() {
        ITestContext context = (ITestContext) java.lang.reflect.Proxy.newProxyInstance(
                ITestContext.class.getClassLoader(),
                new Class<?>[]{ITestContext.class},
                (proxy, method, args) -> "getName".equals(method.getName()) ? "UnitContext" : null
        );

        listener.onStart(context);
        listener.onFinish(context);
    }

    @Test
    public void testHookableRun_bindsScopedValueTestContext() {
        IHookCallBack callBack = mock(IHookCallBack.class);
        ITestResult result = mock(ITestResult.class);
        when(result.getName()).thenReturn("myTestMethod");

        IClass mockClass = mock(IClass.class);
        when(result.getTestClass()).thenReturn(mockClass);
        when(mockClass.getName()).thenReturn("com.example.TestClass");

        AtomicBoolean verifiedInsideScope = new AtomicBoolean(false);

        doAnswer(inv -> {
            Assert.assertNotNull(TestContext.current());
            Assert.assertEquals(TestContext.testName(), "myTestMethod");
            Assert.assertTrue(TestContext.testId().contains("myTestMethod"));
            verifiedInsideScope.set(true);
            return null;
        }).when(callBack).runTestMethod(result);

        listener.run(callBack, result);
        Assert.assertTrue(verifiedInsideScope.get());
    }

    @Test
    public void testTestLifecycleCallbacks() {
        ITestResult result = mock(ITestResult.class);
        when(result.getName()).thenReturn("sampleTest");
        IClass mockClass = mock(IClass.class);
        when(result.getTestClass()).thenReturn(mockClass);
        when(mockClass.getName()).thenReturn("com.example.SampleTest");

        // onTestStart
        listener.onTestStart(result);
        Assert.assertEquals(org.apache.logging.log4j.ThreadContext.get("testIdentifier"), "com.example.SampleTest.sampleTest");
        Assert.assertEquals(org.apache.logging.log4j.ThreadContext.get("testName"), "sampleTest");

        // onTestSuccess
        listener.onTestSuccess(result);
        Assert.assertNull(org.apache.logging.log4j.ThreadContext.get("testIdentifier"));
        Assert.assertEquals(org.apache.logging.log4j.ThreadContext.get("testName"), "Ellithium");

        // onTestSkipped
        listener.onTestStart(result);
        listener.onTestSkipped(result);
        Assert.assertNull(org.apache.logging.log4j.ThreadContext.get("testIdentifier"));

        // onTestFailure
        listener.onTestStart(result);
        listener.onTestFailure(result);
        Assert.assertNull(org.apache.logging.log4j.ThreadContext.get("testIdentifier"));
    }

    @Test
    public void testMethodInterceptor() {
        IMethodInstance m1 = mock(IMethodInstance.class);
        ITestNGMethod ngm1 = mock(ITestNGMethod.class);
        when(m1.getMethod()).thenReturn(ngm1);
        when(ngm1.getMethodName()).thenReturn("testA");

        IMethodInstance m2 = mock(IMethodInstance.class);
        ITestNGMethod ngm2 = mock(ITestNGMethod.class);
        when(m2.getMethod()).thenReturn(ngm2);
        when(ngm2.getMethodName()).thenReturn("testB");

        List<IMethodInstance> list = List.of(m1, m2);
        List<IMethodInstance> result = listener.intercept(list, mock(ITestContext.class));
        Assert.assertEquals(result.size(), 2);

        // Null / single item fast-path
        Assert.assertNull(listener.intercept(null, null));
        Assert.assertEquals(listener.intercept(List.of(m1), null).size(), 1);
    }
}
