package Ellithium.Utilities.interactions;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebDriver;

/**
 * Provides a comprehensive, unified set of WebDriver interaction methods with built-in
 * explicit waits, automatic retries, enhanced logging, and cross-platform mobile gesture support.
 *
 * <p>This is the central facade for all high-level user interactions in the Ellithium framework.
 * It aggregates specialized action classes (JavaScript, Alerts, Frames, etc.) and provides
 * convenient factory methods to access them fluently.</p>
 *
 * <p>All operations are logged via {@link Ellithium.core.reporting.Reporter} with appropriate
 * {@link Ellithium.core.logging.LogLevel} coloring for easy test execution visibility.</p>
 *
 * <p><b>Thread Safety:</b> This class is not thread-safe. Each test thread should have its own instance.</p>
 *
 * <p><b>Mobile Support:</b>
 * <ul>
 *   <li>Android: Full access via {@link #androidActions()}</li>
 *   <li>iOS: Full access via {@link #iosActions()}</li>
 * </ul>
 * </p>
 *
 * @param <T> The specific WebDriver type (e.g., ChromeDriver, AndroidDriver, IOSDriver, RemoteWebDriver)
 * @author Ellithium Framework Team
 */
public class DriverActions<T extends WebDriver> extends BaseActions<T> {

    /**
     * Creates a new DriverActions instance wrapping the provided WebDriver.
     *
     * @param driver the WebDriver instance to be used for all actions
     * @throws IllegalArgumentException if driver is null
     */
    public DriverActions(T driver) {
        super(driver);
    }

    /**
     * Returns a fluent API for executing JavaScript commands safely with logging and error handling.
     *
     * @return a new {@link JavaScriptActions} instance bound to the current driver
     */
    public JavaScriptActions JSActions() {
        return new JavaScriptActions<>(driver);
    }

    /**
     * Provides utility methods for explicit and implicit waits, including sleep operations.
     *
     * @return a singleton {@link Sleep} utility instance
     */
    public Sleep sleep() {
        return new Sleep();
    }

    /**
     * Handles browser and native alert dialogs (accept, dismiss, get text, send keys).
     *
     * @return a new {@link AlertActions} instance
     */
    public AlertActions alerts() {
        return new AlertActions<>(driver);
    }

    /**
     * Provides methods for switching to and interacting with frames and iframes.
     *
     * @return a new {@link FrameActions} instance
     */
    public FrameActions frames() {
        return new FrameActions<>(driver);
    }

    /**
     * Manages browser window operations: maximize, resize, switch, close, etc.
     *
     * @return a new {@link WindowActions} instance
     */
    public WindowActions windows() {
        return new WindowActions<>(driver);
    }

    /**
     * Offers enhanced waiting mechanisms: explicit waits with custom conditions, fluent waits, etc.
     *
     * @return a new {@link WaitActions} instance
     */
    public WaitActions waits() {
        return new WaitActions<>(driver);
    }

    /**
     * Core element interaction API: click, send keys, get text, clear — with built-in retry and wait logic.
     *
     * @return a new {@link ElementActions} instance
     */
    public ElementActions elements() {
        return new ElementActions<>(driver);
    }

    /**
     * Handles HTML {@code <select>} dropdown elements using Selenium's Select support.
     *
     * @return a new {@link SelectActions} instance
     */
    public SelectActions select() {
        return new SelectActions<>(driver);
    }

    /**
     * Provides navigation actions: back, forward, refresh, to(URL).
     *
     * @return a new {@link NavigationActions} instance
     */
    public NavigationActions navigation() {
        return new NavigationActions<>(driver);
    }

    /**
     * Simulates mouse actions: hover, click-and-hold, drag-and-drop, context click.
     *
     * @return a new {@link MouseActions} instance
     */
    public MouseActions mouse() {
        return new MouseActions<>(driver);
    }

    /**
     * Returns Android-specific mobile gestures and actions.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>Android only — throws {@link ClassCastException} if driver is not {@link AndroidDriver}</li>
     * </ul>
     *
     * @return a new {@link AndroidActions} instance
     * @throws ClassCastException if the current driver is not an {@link AndroidDriver}
     * @deprecated Use {@link #mobileActions()} for cross-platform compatibility
     */
    @Deprecated
    public AndroidActions androidActions() {
        return new AndroidActions<>((AndroidDriver) driver);
    }

    /**
     * Returns iOS-specific mobile gestures and actions.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS only — throws {@link ClassCastException} if driver is not {@link IOSDriver}</li>
     * </ul>
     *
     * @return a new {@link IOSActions} instance
     * @throws ClassCastException if the current driver is not an {@link IOSDriver}
     * @deprecated Use {@link #mobileActions()} for cross-platform compatibility
     */
    @Deprecated
    public IOSActions iosActions() {
        return new IOSActions((IOSDriver) driver);
    }

    /**
     * Returns a unified, cross-platform API for mobile gestures and native actions.
     *
     * <p>Provides a single, consistent interface for performing touch gestures (tap, swipe, scroll,
     * pinch, drag, long press, etc.) that works identically on both Android and iOS devices.</p>
     *
     * <p>Automatically detects the underlying platform (Android or iOS) and executes the
     * appropriate Appium mobile command under the hood.</p>
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>Android: Full support via UIA upholsterer2 driver</li>
     *   <li>iOS: Full support via XCUITest driver</li>
     *   <li>Desktop browsers: Throws {@link ClassCastException}</li>
     * </ul>
     * </p>
     *
     * <p>Replaces the older platform-specific {@link #androidActions()} and {@link #iosActions()}
     * methods. Use this method for all new test development to ensure maximum script portability.</p>
     *
     * <p><b>Example usage:</b></p>
     * <pre>
     * driverActions.mobileActions().swipe(locator, "up");
     * driverActions.mobileActions().longPress(locator, 2.0);
     * driverActions.mobileActions().pinch(locator, 2.0, 1.5); // zoom in
     * </pre>
     *
     * @return a new {@link MobileActions} instance bound to the current {@link AppiumDriver}
     * @throws ClassCastException if the current driver is not an {@link io.appium.java_client.AppiumDriver}
     * @see MobileActions
     * @since 2.2.2 (unified mobile actions)
     */
    public MobileActions mobileActions() {
        return new MobileActions<>((AppiumDriver) driver);
    }
}