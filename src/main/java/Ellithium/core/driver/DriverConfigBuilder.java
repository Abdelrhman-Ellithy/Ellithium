package Ellithium.core.driver;

import org.openqa.selenium.Capabilities;

public interface DriverConfigBuilder {
    DriverConfigBuilder setCapabilities(Capabilities capabilities);
    DriverConfigBuilder setDriverType(DriverType driverType);

    Capabilities getCapabilities();
    DriverType getDriverType();

    default void validate() {}
}
