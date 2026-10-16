package Ellithium.core.execution.fixtures;

import Ellithium.Utilities.interactions.*;
import Ellithium.core.driver.*;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

import java.lang.reflect.Method;

/**
 * TestNG DataProvider supplying declarative driver fixtures and parameter injection
 * for methods annotated with {@link WithDriver}.
 */
public final class DriverFixtureProvider {

    public static final String FIXTURE_DATA_PROVIDER = "ellithiumDriverFixture";

    private DriverFixtureProvider() {}

    @DataProvider(name = FIXTURE_DATA_PROVIDER)
    public static Object[][] provideDriverFixture(Method method, ITestContext context) {
        WithDriver annotation = resolveAnnotation(method);
        if (annotation == null) {
            return new Object[0][0];
        }

        LocalDriverConfig config = createConfig(annotation);
        WebDriver driver = DriverFactory.getNewDriver(config);

        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] params = new Object[paramTypes.length];
        DriverActions<WebDriver> actions = null;

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> pt = paramTypes[i];
            if (WebDriver.class.isAssignableFrom(pt)) {
                params[i] = driver;
            } else if (DriverActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions;
            } else if (ElementActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.elements();
            } else if (WaitActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.waits();
            } else if (NavigationActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.navigation();
            } else if (MouseActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.mouse();
            } else if (JavaScriptActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.JSActions();
            } else if (AlertActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.alerts();
            } else if (SelectActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.select();
            } else if (WindowActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.windows();
            } else if (FrameActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.frames();
            } else if (CookieActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.cookies();
            } else if (NetworkActions.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.network();
            } else if (Sleep.class.isAssignableFrom(pt)) {
                if (actions == null) actions = new DriverActions<>(driver);
                params[i] = actions.sleep();
            } else if (LocalDriverConfig.class.isAssignableFrom(pt)) {
                params[i] = config;
            } else if (ITestContext.class.isAssignableFrom(pt)) {
                params[i] = context;
            } else {
                params[i] = null;
            }
        }

        return new Object[][] { params };
    }

    /**
     * Resolves {@link WithDriver} from method level, falling back to class level.
     *
     * @param method method under test
     * @return resolved annotation or null
     */
    public static WithDriver resolveAnnotation(Method method) {
        if (method == null) return null;
        if (method.isAnnotationPresent(WithDriver.class)) {
            return method.getAnnotation(WithDriver.class);
        }
        if (method.getDeclaringClass() != null && method.getDeclaringClass().isAnnotationPresent(WithDriver.class)) {
            return method.getDeclaringClass().getAnnotation(WithDriver.class);
        }
        return null;
    }

    /**
     * Converts a {@link WithDriver} annotation to a fully configured {@link LocalDriverConfig}.
     *
     * @param annotation the fixture annotation
     * @return populated driver config
     */
    public static LocalDriverConfig createConfig(WithDriver annotation) {
        return new LocalDriverConfig(
                annotation.type(),
                annotation.headless() ? HeadlessMode.True : HeadlessMode.False,
                annotation.incognito() ? PrivateMode.True : PrivateMode.False,
                annotation.pageLoadStrategy(),
                annotation.webSecurity(),
                annotation.sandbox()
        );
    }
}
