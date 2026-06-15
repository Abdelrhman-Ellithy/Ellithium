package Ellithium.Utilities.interactions;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
}
