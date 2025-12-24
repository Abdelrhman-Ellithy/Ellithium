package Ellithium.core.driver;

import Ellithium.core.logging.Logger;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for mobile WebDriver instances.
 * This class implements DriverConfigBuilder and manages mobile driver configurations
 * with a fluent builder pattern for easy configuration.
 *
 * <p>Usage Example:
 * <pre>
 * MobileDriverConfig config = new MobileDriverConfig()
 *     .setDriverType(MobileDriverType.Android)
 *     .setDeviceName("Pixel 7")
 *     .setPlatformVersion("13.0")
 *     .setApp("/path/to/app.apk")
 *     .setAutomationName("UiAutomator2")
 *     .setAutoGrantPermissions(true)
 *     .setNewCommandTimeout(Duration.ofSeconds(300));
 *
 * AndroidDriver driver = DriverFactory.getNewDriver(config);
 * </pre>
 */
public class MobileDriverConfig implements DriverConfigBuilder {
    private MobileDriverType driverType;
    private URL remoteAddress;
    private MutableCapabilities internalCapabilities;
    private static final String DEFAULT_URL = "http://127.0.0.1:4723";

    /**
     * Default constructor for MobileDriverConfig.
     * Sets default URL to <a href="http://127.0.0.1:4723">...</a>
     * Initializes with empty capabilities.
     */
    public MobileDriverConfig() {
        this.internalCapabilities = new DesiredCapabilities();
        try {
            setRemoteAddress(new URL(DEFAULT_URL));
        } catch (MalformedURLException e) {
            Logger.logException(e);
        }
    }

    /**
     * Constructs a MobileDriverConfig with driver type.
     *
     * @param driverType Type of mobile driver (Android or iOS)
     */
    public MobileDriverConfig(MobileDriverType driverType) {
        this();
        setDriverType(driverType);
    }

    /**
     * Constructs a MobileDriverConfig with driver type and remote address.
     *
     * @param driverType Type of mobile driver (Android or iOS)
     * @param remoteAddress Appium server URL
     */
    public MobileDriverConfig(MobileDriverType driverType, URL remoteAddress) {
        this(driverType);
        setRemoteAddress(remoteAddress);
    }

    /**
     * Constructs a MobileDriverConfig with all main parameters.
     *
     * @param driverType Type of mobile driver (Android or iOS)
     * @param capabilities Device and app-specific capabilities
     * @param remoteAddress Appium server URL
     */
    public MobileDriverConfig(MobileDriverType driverType, Capabilities capabilities, URL remoteAddress) {
        this();
        setDriverType(driverType);
        setCapabilities(capabilities);
        setRemoteAddress(remoteAddress);
    }

    // ========================================================================================
    // BUILDER PATTERN METHODS - BASIC CONFIGURATION
    // ========================================================================================

    /**
     * Sets the driver type (Android or iOS).
     *
     * @param driverType The mobile driver type
     * @return This config instance for method chaining
     */
    @Override
    public MobileDriverConfig setDriverType(DriverType driverType) {
        this.driverType = (MobileDriverType) driverType;
        if (this.driverType != null) {
            internalCapabilities.setCapability("platformName", this.driverType.getPlatformName());
            // Set default automation name if not already set
            if (internalCapabilities.getCapability("appium:automationName") == null) {
                internalCapabilities.setCapability("appium:automationName", this.driverType.getDefaultAutomationName());
            }
        }
        return this;
    }

    /**
     * Sets the device name for testing.
     *
     * @param deviceName The device name (e.g., "Pixel 7", "iPhone 14 Pro")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setDeviceName(String deviceName) {
        internalCapabilities.setCapability("appium:deviceName", deviceName);
        return this;
    }

    /**
     * Sets the platform version.
     *
     * @param platformVersion The OS version (e.g., "13.0", "16.0")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setPlatformVersion(String platformVersion) {
        internalCapabilities.setCapability("appium:platformVersion", platformVersion);
        return this;
    }

    /**
     * Sets the app location.
     * Can be a local path or a remote URL.
     *
     * @param app The app location (e.g., "/path/to/app.apk" or "<a href="http://example.com/app.apk">...</a>")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setApp(String app) {
        internalCapabilities.setCapability("appium:app", app);
        return this;
    }

    /**
     * Sets the automation name explicitly.
     *
     * @param automationName The automation name (e.g., "UiAutomator2", "XCUITest", "Espresso")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setAutomationName(String automationName) {
        internalCapabilities.setCapability("appium:automationName", automationName);
        return this;
    }

    /**
     * Sets the device UDID for connecting to a specific device.
     *
     * @param udid The device UDID
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setUdid(String udid) {
        internalCapabilities.setCapability("appium:udid", udid);
        return this;
    }

    /**
     * Sets the remote server address.
     *
     * @param remoteAddress The URL of the Appium server
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setRemoteAddress(URL remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    /**
     * Sets the remote server address from a string.
     *
     * @param remoteAddress The URL string of the Appium server
     * @return This config instance for method chaining
     * @throws IllegalArgumentException if the URL is malformed
     */
    public MobileDriverConfig setRemoteAddress(String remoteAddress) {
        try {
            this.remoteAddress = new URL(remoteAddress);
        } catch (MalformedURLException e) {
            Logger.logException(e);
            throw new IllegalArgumentException("Invalid URL: " + remoteAddress, e);
        }
        return this;
    }

    // ========================================================================================
    // BUILDER PATTERN METHODS - APP CONFIGURATION
    // ========================================================================================

    /**
     * Sets the app package name (Android only).
     *
     * @param appPackage The app package name (e.g., "com.example.app")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setAppPackage(String appPackage) {
        internalCapabilities.setCapability("appium:appPackage", appPackage);
        return this;
    }

    /**
     * Sets the app activity name (Android only).
     *
     * @param appActivity The app activity name (e.g., ".MainActivity")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setAppActivity(String appActivity) {
        internalCapabilities.setCapability("appium:appActivity", appActivity);
        return this;
    }

    /**
     * Sets the bundle identifier (iOS only).
     *
     * @param bundleId The bundle identifier (e.g., "com.example.app")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setBundleId(String bundleId) {
        internalCapabilities.setCapability("appium:bundleId", bundleId);
        return this;
    }

    /**
     * Sets whether to install the app before the session.
     *
     * @param noReset true to not reset app state between sessions
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setNoReset(boolean noReset) {
        internalCapabilities.setCapability("appium:noReset", noReset);
        return this;
    }

    /**
     * Sets whether to perform a full reset (reinstall app).
     *
     * @param fullReset true to perform full reset
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setFullReset(boolean fullReset) {
        internalCapabilities.setCapability("appium:fullReset", fullReset);
        return this;
    }

    /**
     * Sets whether to automatically grant app permissions (Android only).
     *
     * @param autoGrant true to automatically grant permissions
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setAutoGrantPermissions(boolean autoGrant) {
        internalCapabilities.setCapability("appium:autoGrantPermissions", autoGrant);
        return this;
    }

    // ========================================================================================
    // BUILDER PATTERN METHODS - SESSION CONFIGURATION
    // ========================================================================================

    /**
     * Sets the new command timeout.
     *
     * @param timeout The timeout duration
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setNewCommandTimeout(Duration timeout) {
        internalCapabilities.setCapability("appium:newCommandTimeout", timeout.getSeconds());
        return this;
    }

    /**
     * Sets the new command timeout in seconds.
     *
     * @param seconds The timeout in seconds
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setNewCommandTimeout(long seconds) {
        internalCapabilities.setCapability("appium:newCommandTimeout", seconds);
        return this;
    }

    /**
     * Sets device orientation.
     *
     * @param orientation The orientation ("PORTRAIT" or "LANDSCAPE")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setDeviceOrientation(String orientation) {
        internalCapabilities.setCapability("appium:orientation", orientation.toUpperCase());
        return this;
    }

    /**
     * Sets the language for the session.
     *
     * @param language The language code (e.g., "en", "es", "fr")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setLanguage(String language) {
        internalCapabilities.setCapability("appium:language", language);
        return this;
    }

    /**
     * Sets the locale for the session.
     *
     * @param locale The locale code (e.g., "US", "GB", "FR")
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setLocale(String locale) {
        internalCapabilities.setCapability("appium:locale", locale);
        return this;
    }

    // ========================================================================================
    // BUILDER PATTERN METHODS - ANDROID SPECIFIC
    // ========================================================================================

    /**
     * Sets the system port for UiAutomator2 (Android only).
     *
     * @param port The system port number
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setSystemPort(int port) {
        internalCapabilities.setCapability("appium:systemPort", port);
        return this;
    }

    /**
     * Sets whether to use Unicode keyboard (Android only).
     *
     * @param useUnicode true to use Unicode keyboard
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setUnicodeKeyboard(boolean useUnicode) {
        internalCapabilities.setCapability("appium:unicodeKeyboard", useUnicode);
        return this;
    }

    /**
     * Sets whether to reset keyboard after test (Android only).
     *
     * @param reset true to reset keyboard
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setResetKeyboard(boolean reset) {
        internalCapabilities.setCapability("appium:resetKeyboard", reset);
        return this;
    }

    /**
     * Sets the Chrome driver executable path (Android only, for hybrid apps).
     *
     * @param path The path to chromedriver executable
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setChromedriverExecutable(String path) {
        internalCapabilities.setCapability("appium:chromedriverExecutable", path);
        return this;
    }

    /**
     * Sets whether to skip device initialization (Android only).
     *
     * @param skip true to skip device initialization
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setSkipDeviceInitialization(boolean skip) {
        internalCapabilities.setCapability("appium:skipDeviceInitialization", skip);
        return this;
    }

    // ========================================================================================
    // BUILDER PATTERN METHODS - IOS SPECIFIC
    // ========================================================================================

    /**
     * Sets whether to show XCode logs (iOS only).
     *
     * @param show true to show XCode logs
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setShowXcodeLog(boolean show) {
        internalCapabilities.setCapability("appium:showXcodeLog", show);
        return this;
    }

    /**
     * Sets the WDA local port (iOS only).
     *
     * @param port The WDA local port number
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setWdaLocalPort(int port) {
        internalCapabilities.setCapability("appium:wdaLocalPort", port);
        return this;
    }

    /**
     * Sets whether to use new WDA (iOS only).
     *
     * @param useNew true to use new WDA
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setUseNewWDA(boolean useNew) {
        internalCapabilities.setCapability("appium:useNewWDA", useNew);
        return this;
    }

    /**
     * Sets the XCUITest WDA launch timeout (iOS only).
     *
     * @param timeout The timeout duration
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setWdaLaunchTimeout(Duration timeout) {
        internalCapabilities.setCapability("appium:wdaLaunchTimeout", timeout.toMillis());
        return this;
    }

    /**
     * Sets whether to accept alerts automatically (iOS only).
     *
     * @param autoAccept true to auto-accept alerts
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setAutoAcceptAlerts(boolean autoAccept) {
        internalCapabilities.setCapability("appium:autoAcceptAlerts", autoAccept);
        return this;
    }

    /**
     * Sets whether to dismiss alerts automatically (iOS only).
     *
     * @param autoDismiss true to auto-dismiss alerts
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setAutoDismissAlerts(boolean autoDismiss) {
        internalCapabilities.setCapability("appium:autoDismissAlerts", autoDismiss);
        return this;
    }

    // ========================================================================================
    // BUILDER PATTERN METHODS - ADVANCED OPTIONS
    // ========================================================================================

    /**
     * Sets whether to print page source on find failure for debugging.
     *
     * @param print true to print page source on failure
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setPrintPageSourceOnFindFailure(boolean print) {
        internalCapabilities.setCapability("appium:printPageSourceOnFindFailure", print);
        return this;
    }

    /**
     * Sets whether to clear system files after session.
     *
     * @param clear true to clear system files
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setClearSystemFiles(boolean clear) {
        internalCapabilities.setCapability("appium:clearSystemFiles", clear);
        return this;
    }

    /**
     * Sets custom event timings.
     *
     * @param enable true to enable event timings
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setEventTimings(boolean enable) {
        internalCapabilities.setCapability("appium:eventTimings", enable);
        return this;
    }

    /**
     * Sets whether to enable performance logging.
     *
     * @param enable true to enable performance logging
     * @return This config instance for method chaining
     */
    public MobileDriverConfig setEnablePerformanceLogging(boolean enable) {
        internalCapabilities.setCapability("appium:enablePerformanceLogging", enable);
        return this;
    }

    /**
     * Adds a custom capability.
     *
     * @param key The capability key
     * @param value The capability value
     * @return This config instance for method chaining
     */
    public MobileDriverConfig addCapability(String key, Object value) {
        internalCapabilities.setCapability(key, value);
        return this;
    }

    /**
     * Adds multiple custom capabilities.
     *
     * @param capabilities Map of capability key-value pairs
     * @return This config instance for method chaining
     */
    public MobileDriverConfig addCapabilities(Map<String, Object> capabilities) {
        capabilities.forEach(internalCapabilities::setCapability);
        return this;
    }

    // ========================================================================================
    // INTERFACE IMPLEMENTATION METHODS
    // ========================================================================================

    /**
     * Sets the WebDriver capabilities.
     * This replaces all existing capabilities.
     *
     * @param capabilities The WebDriver capabilities to configure
     */
    @Override
    public MobileDriverConfig setCapabilities(Capabilities capabilities) {
        if (capabilities instanceof MutableCapabilities) {
            this.internalCapabilities = (MutableCapabilities) capabilities;
        } else {
            this.internalCapabilities = new MutableCapabilities(capabilities);
        }
        return this;
    }

    /**
     * Gets the WebDriver capabilities.
     * Returns platform-specific options (UiAutomator2Options for Android, XCUITestOptions for iOS).
     *
     * @return The current WebDriver capabilities configuration
     */
    @Override
    public Capabilities getCapabilities() {
        MutableCapabilities finalCapabilities;

        if (driverType == MobileDriverType.IOS) {
            finalCapabilities = new XCUITestOptions(internalCapabilities);
        } else {
            // Default to Android if not specified or explicitly Android
            finalCapabilities = new UiAutomator2Options(internalCapabilities);
        }

        // Ensure automation name is set
        if (finalCapabilities.getCapability("appium:automationName") == null && driverType != null) {
            finalCapabilities.setCapability("appium:automationName", driverType.getDefaultAutomationName());
        }

        return finalCapabilities;
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
     * Default is <a href="http://127.0.0.1:4723">...</a>
     *
     * @return The URL of the remote server
     */
    public URL getRemoteAddress() {
        if (remoteAddress != null) {
            return remoteAddress;
        }

        try {
            return new URL(DEFAULT_URL);
        } catch (MalformedURLException e) {
            Logger.logException(e);
            return null;
        }
    }

    /**
     * Gets the internal mutable capabilities.
     *
     * @return A copy of the internal capabilities map
     */
    public Map<String, Object> getInternalCapabilities() {
        Map<String, Object> capabilitiesMap = new HashMap<>();
        capabilitiesMap.putAll(internalCapabilities.asMap());
        return capabilitiesMap;
    }

    // ========================================================================================
    // VALIDATION
    // ========================================================================================

    /**
     * Validates the configuration before driver creation.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    public void validate() {
        if (driverType == null) {
            throw new IllegalStateException("Driver type must be set (Android or iOS)");
        }

        if (remoteAddress == null) {
            throw new IllegalStateException("Remote address must be set");
        }
    }
}