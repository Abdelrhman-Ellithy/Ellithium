package Ellithium.Utilities.interactions;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

public class DriverActionsTest {

    private WebDriver mockDriver;
    private DriverActions<WebDriver> driverActions;

    @BeforeMethod
    public void setup() {
        mockDriver = mock(WebDriver.class);
        driverActions = new DriverActions<>(mockDriver);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*driver must not be null.*")
    public void constructor_nullDriver_throwsNullPointerException() {
        new DriverActions<>(null);
    }

    @Test
    public void jsActions_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.JSActions());
    }

    @Test
    public void sleep_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.sleep());
    }

    @Test
    public void cookies_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.cookies());
    }

    @Test
    public void alerts_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.alerts());
    }

    @Test
    public void frames_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.frames());
    }

    @Test
    public void windows_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.windows());
    }

    @Test
    public void waits_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.waits());
    }

    @Test
    public void elements_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.elements());
    }

    @Test
    public void select_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.select());
    }

    @Test
    public void navigation_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.navigation());
    }

    @Test
    public void mouse_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.mouse());
    }

    @Test
    public void network_returnsNonNullInstance() {
        Assert.assertNotNull(driverActions.network());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = ".*mobileActions\\(\\) requires an AppiumDriver.*")
    public void mobileActions_plainWebDriver_throwsUnsupportedOperationException() {
        driverActions.mobileActions();
    }

    @Test
    public void mobileActions_appiumDriver_returnsNonNullInstance() {
        AppiumDriver mockAppiumDriver = mock(AppiumDriver.class);
        DriverActions<AppiumDriver> appiumActions = new DriverActions<>(mockAppiumDriver);
        Assert.assertNotNull(appiumActions.mobileActions());
    }
}
