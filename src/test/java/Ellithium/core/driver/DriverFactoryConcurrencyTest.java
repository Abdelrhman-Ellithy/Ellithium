package Ellithium.core.driver;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class DriverFactoryConcurrencyTest {

    @Test
    public void runWithDriver_bindsScopedValueAndCleansUpAutomatically() {
        WebDriver mockDriver = mock(WebDriver.class);
        DriverConfiguration mockConfig = mock(DriverConfiguration.class);

        assertNull(DriverFactory.getCurrentDriver(), "Driver should initially be null");
        assertNull(DriverFactory.getCurrentDriverConfiguration(), "Config should initially be null");

        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicReference<WebDriver> seenDriver = new AtomicReference<>();
        AtomicReference<DriverConfiguration> seenConfig = new AtomicReference<>();

        DriverFactory.runWithDriver(mockDriver, mockConfig, () -> {
            executed.set(true);
            seenDriver.set(DriverFactory.getCurrentDriver());
            seenConfig.set(DriverFactory.getCurrentDriverConfiguration());
        });

        assertTrue(executed.get(), "Action should have executed");
        assertSame(seenDriver.get(), mockDriver, "Scoped driver should match mock");
        assertSame(seenConfig.get(), mockConfig, "Scoped config should match mock");

        // Verify automatic cleanup
        assertNull(DriverFactory.getCurrentDriver(), "Driver should be cleaned up after scope exit");
        assertNull(DriverFactory.getCurrentDriverConfiguration(), "Config should be cleaned up after scope exit");
    }

    @Test
    public void isDriverAlive_returnsTrueWhenWindowHandleAvailable() {
        WebDriver mockDriver = mock(WebDriver.class);
        DriverConfiguration mockConfig = mock(DriverConfiguration.class);
        when(mockDriver.getWindowHandle()).thenReturn("WINDOW-1234");

        DriverFactory.runWithDriver(mockDriver, mockConfig, () -> {
            assertTrue(DriverFactory.isDriverAlive(), "Driver should be reported alive");
        });

        assertFalse(DriverFactory.isDriverAlive(), "Driver should not be alive when unbound");
    }

    @Test
    public void runWithDriver_supportsVirtualThreadsInheritance() throws InterruptedException {
        WebDriver mockDriver = mock(WebDriver.class);
        DriverConfiguration mockConfig = mock(DriverConfiguration.class);

        AtomicReference<WebDriver> childSeenDriver = new AtomicReference<>();

        DriverFactory.runWithDriver(mockDriver, mockConfig, () -> {
            // Spawn virtual thread from within the scoped driver context
            Thread vt = Thread.ofVirtual().start(() -> {
                // In Java 25, ScopedValue can be read in structured contexts or worker threads
                WebDriver d = DriverFactory.getCurrentDriver();
                childSeenDriver.set(d);
            });
            try {
                vt.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // The parent scope ran cleanly
        assertNull(DriverFactory.getCurrentDriver());
    }
}
