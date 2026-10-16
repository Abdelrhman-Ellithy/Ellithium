package Ellithium.Utilities.interactions;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

public class WaitManagerTest {

    @BeforeClass
    public void init() {
        WaitManager.initializeTimeoutAndPolling();
    }

    @Test
    public void getDefaultTimeout_returnsNonNegativeValue() {
        int timeout = WaitManager.getDefaultTimeout();
        Assert.assertTrue(timeout >= 0, "Default timeout must be non-negative, got: " + timeout);
    }

    @Test
    public void getDefaultPollingTime_returnsPositiveValue() {
        int polling = WaitManager.getDefaultPollingTime();
        Assert.assertTrue(polling > 0, "Default polling time must be positive, got: " + polling);
    }

    @Test
    public void getDefaultPollingTime_isLessThanOneHour_inMilliseconds() {
        int polling = WaitManager.getDefaultPollingTime();
        Assert.assertTrue(polling < 3_600_000, "Polling interval must be < 1h in ms, got: " + polling);
    }

    @Test
    public void initializeTimeoutAndPolling_isIdempotent() {
        int timeoutBefore = WaitManager.getDefaultTimeout();
        int pollingBefore = WaitManager.getDefaultPollingTime();
        WaitManager.initializeTimeoutAndPolling();
        Assert.assertEquals(WaitManager.getDefaultTimeout(), timeoutBefore, "Repeated init must not change timeout");
        Assert.assertEquals(WaitManager.getDefaultPollingTime(), pollingBefore, "Repeated init must not change polling");
    }

    @Test
    public void testParseProperty_validAndInvalidCases() {
        // Valid positive integers
        Assert.assertEquals(WaitManager.parseProperty("15", 10, "timeout"), 15);
        Assert.assertEquals(WaitManager.parseProperty("  250  ", 50, "polling"), 250);

        // Non-positive integers should fall back to default
        Assert.assertEquals(WaitManager.parseProperty("0", 10, "timeout"), 10);
        Assert.assertEquals(WaitManager.parseProperty("-5", 50, "polling"), 50);

        // Malformed, blank, or null should fall back to default
        Assert.assertEquals(WaitManager.parseProperty("abc", 10, "timeout"), 10);
        Assert.assertEquals(WaitManager.parseProperty("", 10, "timeout"), 10);
        Assert.assertEquals(WaitManager.parseProperty("   ", 10, "timeout"), 10);
        Assert.assertEquals(WaitManager.parseProperty(null, 10, "timeout"), 10);
    }

    @Test
    public void testGetFluentWaitMillis_boundsCheck() {
        WebDriver driver = mock(WebDriver.class);

        // Non-positive values must not throw IllegalArgumentException
        FluentWait<WebDriver> wait1 = WaitManager.getFluentWaitMillis(driver, -100L, -50);
        Assert.assertNotNull(wait1);

        FluentWait<WebDriver> wait2 = WaitManager.getFluentWaitMillis(driver, 0L, 0);
        Assert.assertNotNull(wait2);

        // Polling interval larger than timeout should safely adapt
        FluentWait<WebDriver> wait3 = WaitManager.getFluentWaitMillis(driver, 10L, 500);
        Assert.assertNotNull(wait3);

        // Standard positive values
        FluentWait<WebDriver> wait4 = WaitManager.getFluentWait(driver, 5, 100);
        Assert.assertNotNull(wait4);
    }
}
