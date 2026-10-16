package Ellithium.core.spi;

import Ellithium.core.driver.DriverConfiguration;
import org.openqa.selenium.WebDriver;

/**
 * Service Provider Interface (SPI) for plugins that monitor driver lifecycle transitions
 * (allocation, configuration, teardown).
 */
public interface DriverLifecyclePlugin extends EllithiumPlugin {

    /**
     * Invoked immediately after a new driver instance is created and registered to thread context.
     *
     * @param driver the newly created WebDriver instance
     * @param config the configuration applied to this driver
     */
    default void onDriverCreated(WebDriver driver, DriverConfiguration config) {
        // default no-op
    }

    /**
     * Invoked immediately prior to or upon driver termination.
     *
     * @param driver the WebDriver instance being torn down
     */
    default void onDriverQuit(WebDriver driver) {
        // default no-op
    }
}
