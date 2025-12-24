package Ellithium.core.driver;

import org.openqa.selenium.Capabilities;

public interface DriverConfigBuilder {
    public DriverConfigBuilder setCapabilities(Capabilities capabilities);
    public DriverConfigBuilder setDriverType(DriverType driverType);

    public Capabilities getCapabilities();
    public DriverType getDriverType();
}
