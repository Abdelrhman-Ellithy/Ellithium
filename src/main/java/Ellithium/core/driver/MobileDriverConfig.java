package Ellithium.core.driver;

import org.openqa.selenium.Capabilities;

import java.net.URL;

/**
 * Configuration class for mobile WebDriver instances.
 * This class implements DriverConfigBuilder and manages mobile driver configurations.
 * Required parameters:
 * - MobileDriverType: Type of mobile driver (iOS or Android)
 * - Capabilities: Device and app-specific capabilities
 * - RemoteAddress: Appium server URL
 */
public class MobileDriverConfig implements DriverConfigBuilder {
    private MobileDriverType driverType;
    private Capabilities capabilities;
    private URL remoteAddress;
    private static final String DEFAULT_URL = "http://127.0.0.1:4723";

    /**
     * Default constructor for MobileDriverConfig.
     * Sets default URL to http://127.0.0.1:4723
     * Note: driverType and capabilities must be set before use as they are mandatory.
     */
    public MobileDriverConfig() {
        try {
            setRemoteAddress(new URL(DEFAULT_URL));
        } catch (Exception e) {
            throw new RuntimeException("Error creating default URL", e);
        }
    }

    /**
     * Constructs a MobileDriverConfig with all mandatory parameters.
     *
     * @param driverType Type of mobile driver (iOS or Android)
     * @param capabilities Device and app-specific capabilities
     * @param remoteAddress Appium server URL
     */
    public MobileDriverConfig(MobileDriverType driverType, Capabilities capabilities, URL remoteAddress) {
        setDriverType(driverType);
        setCapabilities(capabilities);
        setRemoteAddress(remoteAddress);
    }

    /**
     * Constructs a MobileDriverConfig with driver type and capabilities.
     *
     * @param driverType Type of mobile driver (iOS or Android)
     * @param capabilities Device and app-specific capabilities
     */
    public MobileDriverConfig(MobileDriverType driverType, Capabilities capabilities) {
        setDriverType(driverType);
        setCapabilities(capabilities);
    }

    /**
     * Constructs a MobileDriverConfig with driver type and remote address.
     *
     * @param driverType Type of mobile driver (iOS or Android)
     * @param remoteAddress Appium server URL
     */
    public MobileDriverConfig(MobileDriverType driverType, URL remoteAddress) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
    }

    /**
     * Sets the WebDriver capabilities.
     *
     * @param capabilities The WebDriver capabilities to configure
     */
    @Override
    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Sets the driver type.
     *
     * @param driverType The driver type to configure, must be instance of RemoteDriverType
     */
    @Override
    public void setDriverType(DriverType driverType) {
        this.driverType = (MobileDriverType) driverType;
    }

    /**
     * Gets the WebDriver capabilities.
     *
     * @return The current WebDriver capabilities configuration
     */
    @Override
    public Capabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Gets the driver type.
     *
     * @return The current driver type configuration
     */
    @Override
    public DriverType getDriverType() {
        return driverType;
    }

    /**
     * Gets the remote server address.
     * Default is http://127.0.0.1:4723
     *
     * @return The URL of the remote server, defaults to http://127.0.0.1:4723 if not set
     */
    public URL getRemoteAddress() {
        try {
            return remoteAddress != null ? remoteAddress : new URL(DEFAULT_URL);
        } catch (Exception e) {
            throw new RuntimeException("Error creating default URL", e);
        }
    }

    /**
     * Sets the remote server address.
     *
     * @param remoteAddress The URL of the remote server to connect to
     */
    public void setRemoteAddress(URL remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
}
