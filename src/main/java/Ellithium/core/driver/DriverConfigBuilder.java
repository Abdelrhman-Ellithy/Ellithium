package Ellithium.core.driver;

import org.openqa.selenium.Capabilities;

public interface DriverConfigBuilder {
    public void setCapabilities(Capabilities capabilities);
    public void setDriverType(DriverType driverType);


    public Capabilities getCapabilities();
    public DriverType getDriverType();
}
