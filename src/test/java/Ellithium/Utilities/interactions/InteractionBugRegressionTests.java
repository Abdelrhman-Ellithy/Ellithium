package Ellithium.Utilities.interactions;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;
import static org.mockito.Mockito.*;

public class InteractionBugRegressionTests {

    // ── WindowActions.closePopupWindow ──────────────────────────────────────

    @Test
    public void closePopupWindow_switchesToRemainingHandle() {
        WebDriver driver = mock(WebDriver.class);
        WebDriver.TargetLocator locator = mock(WebDriver.TargetLocator.class);
        WebDriver.Options options = mock(WebDriver.Options.class);
        WebDriver.Timeouts timeouts = mock(WebDriver.Timeouts.class);

        String mainHandle  = "main-handle";
        String popupHandle = "popup-handle";

        Set<String> both = new LinkedHashSet<>();
        both.add(mainHandle);
        both.add(popupHandle);

        when(driver.getWindowHandles()).thenReturn(both);
        when(driver.getWindowHandle()).thenReturn(popupHandle);
        when(driver.switchTo()).thenReturn(locator);
        when(driver.manage()).thenReturn(options);
        when(options.timeouts()).thenReturn(timeouts);

        WindowActions<WebDriver> wa = new WindowActions<>(driver);
        wa.closePopupWindow();

        verify(driver).close();
        verify(locator).window(mainHandle);
    }

    @Test
    public void closePopupWindow_noRemainingHandles_doesNotSwitchOrThrow() {
        WebDriver driver = mock(WebDriver.class);
        WebDriver.Options options = mock(WebDriver.Options.class);
        WebDriver.Timeouts timeouts = mock(WebDriver.Timeouts.class);

        Set<String> singleHandle = new LinkedHashSet<>();
        singleHandle.add("only-handle");

        when(driver.getWindowHandles()).thenReturn(singleHandle);
        when(driver.getWindowHandle()).thenReturn("only-handle");
        when(driver.manage()).thenReturn(options);
        when(options.timeouts()).thenReturn(timeouts);

        WindowActions<WebDriver> wa = new WindowActions<>(driver);
        wa.closePopupWindow();

        verify(driver).close();
        verify(driver, never()).switchTo();
    }

    // ── DriverActions.mobileActions ─────────────────────────────────────────

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void mobileActions_throwsOnPlainWebDriver() {
        WebDriver driver = mock(WebDriver.class);
        WebDriver.Options options = mock(WebDriver.Options.class);
        WebDriver.Timeouts timeouts = mock(WebDriver.Timeouts.class);
        when(driver.manage()).thenReturn(options);
        when(options.timeouts()).thenReturn(timeouts);

        DriverActions<WebDriver> da = new DriverActions<>(driver);
        da.mobileActions();
    }

    @Test
    public void mobileActions_succeeds_onAppiumDriver() {
        AppiumDriver appiumDriver = mock(AppiumDriver.class);
        WebDriver.Options options = mock(WebDriver.Options.class);
        WebDriver.Timeouts timeouts = mock(WebDriver.Timeouts.class);
        when(appiumDriver.manage()).thenReturn(options);
        when(options.timeouts()).thenReturn(timeouts);

        DriverActions<AppiumDriver> da = new DriverActions<>(appiumDriver);
        assertNotNull(da.mobileActions(), "mobileActions() must return a non-null MobileActions for AppiumDriver");
    }
}
