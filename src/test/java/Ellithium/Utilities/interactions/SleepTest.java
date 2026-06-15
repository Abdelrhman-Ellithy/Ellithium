package Ellithium.Utilities.interactions;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SleepTest {

    @Test
    public void sleepMillis_negativeValue_returnsImmediately() {
        long start = System.currentTimeMillis();
        Sleep.sleepMillis(-500);
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertTrue(elapsed < 200, "Negative sleep must not block, elapsed: " + elapsed);
    }

    @Test
    public void sleepMillis_zero_returnsImmediately() {
        long start = System.currentTimeMillis();
        Sleep.sleepMillis(0);
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertTrue(elapsed < 200, "Zero sleep must not block meaningfully, elapsed: " + elapsed);
    }

    @Test
    public void sleepMillis_smallDuration_sleepsAtLeastThatLong() {
        long millis = 150;
        long start  = System.currentTimeMillis();
        Sleep.sleepMillis(millis);
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertTrue(elapsed >= millis - 20,
                "Sleep should last at least ~" + millis + "ms, elapsed: " + elapsed);
    }

    @Test
    public void sleepSeconds_oneSecond_sleepsAtLeastOneSecond() {
        long start = System.currentTimeMillis();
        Sleep.sleepSeconds(1);
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertTrue(elapsed >= 950, "sleepSeconds(1) should sleep ~1000ms, elapsed: " + elapsed);
    }
}
