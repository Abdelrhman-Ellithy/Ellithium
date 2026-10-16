package Ellithium.core.execution.fixtures;

import Ellithium.Utilities.interactions.*;
import Ellithium.core.driver.*;
import Ellithium.core.execution.listener.CustomTestNGListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class WithDriverFixtureTest {

    @WithDriver(
            type = LocalDriverType.FireFox,
            headless = true,
            incognito = true,
            pageLoadStrategy = PageLoadStrategyMode.Eager,
            webSecurity = WebSecurityMode.AllowUnsecure,
            sandbox = SandboxMode.NoSandboxMode,
            autoQuit = false
    )
    static class CustomAnnotatedClass {
        @Test
        public void defaultClassMethod(WebDriver driver, DriverActions<WebDriver> actions) {}

        @Test
        @WithDriver(type = LocalDriverType.Edge, headless = false, autoQuit = true)
        public void overrideMethod(ElementActions elements, WaitActions waits) {}

        @Test
        public void parameterlessMethod() {}
    }

    static class UnannotatedClass {
        @Test
        public void plainMethod(String name) {}
    }

    @Test
    public void testAnnotationResolution_MethodOverride() throws NoSuchMethodException {
        Method method = CustomAnnotatedClass.class.getMethod("overrideMethod", ElementActions.class, WaitActions.class);
        WithDriver annotation = DriverFixtureProvider.resolveAnnotation(method);

        assertNotNull(annotation, "Annotation should be resolved");
        assertEquals(annotation.type(), LocalDriverType.Edge);
        assertFalse(annotation.headless());
        assertTrue(annotation.autoQuit());
    }

    @Test
    public void testAnnotationResolution_ClassFallback() throws NoSuchMethodException {
        Method method = CustomAnnotatedClass.class.getMethod("defaultClassMethod", WebDriver.class, DriverActions.class);
        WithDriver annotation = DriverFixtureProvider.resolveAnnotation(method);

        assertNotNull(annotation, "Annotation should be resolved from class");
        assertEquals(annotation.type(), LocalDriverType.FireFox);
        assertTrue(annotation.headless());
        assertTrue(annotation.incognito());
        assertEquals(annotation.pageLoadStrategy(), PageLoadStrategyMode.Eager);
        assertEquals(annotation.webSecurity(), WebSecurityMode.AllowUnsecure);
        assertEquals(annotation.sandbox(), SandboxMode.NoSandboxMode);
        assertFalse(annotation.autoQuit());
    }

    @Test
    public void testAnnotationResolution_Unannotated() throws NoSuchMethodException {
        Method method = UnannotatedClass.class.getMethod("plainMethod", String.class);
        WithDriver annotation = DriverFixtureProvider.resolveAnnotation(method);

        assertNull(annotation, "Unannotated method on unannotated class must return null");
    }

    @Test
    public void testCreateConfig_CustomValues() throws NoSuchMethodException {
        Method method = CustomAnnotatedClass.class.getMethod("defaultClassMethod", WebDriver.class, DriverActions.class);
        WithDriver annotation = DriverFixtureProvider.resolveAnnotation(method);
        assertNotNull(annotation);

        LocalDriverConfig config = DriverFixtureProvider.createConfig(annotation);
        assertEquals(config.getLocalDriverType(), LocalDriverType.FireFox);
        assertEquals(config.getHeadlessMode(), HeadlessMode.True);
        assertEquals(config.getPrivateMode(), PrivateMode.True);
        assertEquals(config.getPageLoadStrategy(), PageLoadStrategyMode.Eager);
        assertEquals(config.getWebSecurityMode(), WebSecurityMode.AllowUnsecure);
        assertEquals(config.getSandboxMode(), SandboxMode.NoSandboxMode);
    }

    @Test
    public void testTransform_AnnotatedMethodWithParametersConfiguresDataProvider() throws NoSuchMethodException {
        CustomTestNGListener listener = new CustomTestNGListener();
        Map<String, Object> state = new HashMap<>();
        ITestAnnotation annotation = createMockAnnotation(state);

        Method methodWithParams = CustomAnnotatedClass.class.getMethod("overrideMethod", ElementActions.class, WaitActions.class);
        listener.transform(annotation, CustomAnnotatedClass.class, null, methodWithParams);

        assertEquals(state.get("dataProvider"), DriverFixtureProvider.FIXTURE_DATA_PROVIDER);
        assertEquals(state.get("dataProviderClass"), DriverFixtureProvider.class);
    }

    @Test
    public void testTransform_AnnotatedMethodWithoutParametersSkipsDataProvider() throws NoSuchMethodException {
        CustomTestNGListener listener = new CustomTestNGListener();
        Map<String, Object> state = new HashMap<>();
        ITestAnnotation annotation = createMockAnnotation(state);

        Method methodWithoutParams = CustomAnnotatedClass.class.getMethod("parameterlessMethod");
        listener.transform(annotation, CustomAnnotatedClass.class, null, methodWithoutParams);

        assertNull(state.get("dataProvider"), "DataProvider should not be set for parameterless methods");
        assertNull(state.get("dataProviderClass"));
    }

    @Test
    public void testTransform_UnannotatedMethodSkipsDataProvider() throws NoSuchMethodException {
        CustomTestNGListener listener = new CustomTestNGListener();
        Map<String, Object> state = new HashMap<>();
        ITestAnnotation annotation = createMockAnnotation(state);

        Method plainMethod = UnannotatedClass.class.getMethod("plainMethod", String.class);
        listener.transform(annotation, UnannotatedClass.class, null, plainMethod);

        assertNull(state.get("dataProvider"));
        assertNull(state.get("dataProviderClass"));
    }

    private static ITestAnnotation createMockAnnotation(Map<String, Object> state) {
        return (ITestAnnotation) Proxy.newProxyInstance(
                ITestAnnotation.class.getClassLoader(),
                new Class<?>[] { ITestAnnotation.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getDataProvider".equals(name)) {
                        return state.get("dataProvider");
                    } else if ("setDataProvider".equals(name)) {
                        state.put("dataProvider", args[0]);
                        return null;
                    } else if ("getDataProviderClass".equals(name)) {
                        return state.get("dataProviderClass");
                    } else if ("setDataProviderClass".equals(name)) {
                        state.put("dataProviderClass", args[0]);
                        return null;
                    } else if ("setRetryAnalyzer".equals(name)) {
                        state.put("retryAnalyzer", args[0]);
                        return null;
                    } else if ("getRetryAnalyzerClass".equals(name)) {
                        return state.get("retryAnalyzer");
                    }
                    Class<?> ret = method.getReturnType();
                    if (ret.equals(boolean.class)) return false;
                    if (ret.equals(int.class)) return 0;
                    if (ret.equals(long.class)) return 0L;
                    return null;
                }
        );
    }
}
