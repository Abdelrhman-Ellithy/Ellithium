package Ellithium.core.driver;

/**
 * Enumeration of supported mobile driver types.
 * Defines the mobile platforms and their associated automation frameworks.
 *
 * <p>Supported Platforms:
 * <ul>
 *   <li>Android - Using UiAutomator2</li>
 *   <li>iOS - Using XCUITest</li>
 * </ul>
 */
public enum MobileDriverType implements DriverType {
    /**
     * Android mobile platform.
     * Default automation framework: UiAutomator2
     * Platform name: Android
     */
    Android("Android", "UiAutomator2"),

    /**
     * iOS mobile platform.
     * Default automation framework: XCUITest
     * Platform name: iOS
     */
    IOS("iOS", "XCUITest");

    private final String platformName;
    private final String defaultAutomationName;

    /**
     * Constructor for MobileDriverType.
     *
     * @param platformName The platform name used in capabilities
     * @param defaultAutomationName The default automation framework name
     */
    MobileDriverType(String platformName, String defaultAutomationName) {
        this.platformName = platformName;
        this.defaultAutomationName = defaultAutomationName;
    }

    /**
     * Gets the platform name for this driver type.
     *
     * @return The platform name string (e.g., "Android", "iOS")
     */
    public String getPlatformName() {
        return platformName;
    }

    /**
     * Gets the platform name for this driver type.
     *
     * @return The platform name string (e.g., "Android", "iOS")
     */
    public String getName() {
        return platformName;
    }
    /**
     * Gets the default automation framework name for this driver type.
     *
     * @return The default automation name (e.g., "UiAutomator2", "XCUITest")
     */
    public String getDefaultAutomationName() {
        return defaultAutomationName;
    }

    /**
     * Checks if this is an Android driver type.
     *
     * @return true if this is Android, false otherwise
     */
    public boolean isAndroid() {
        return this == Android;
    }

    /**
     * Checks if this is an iOS driver type.
     *
     * @return true if this is iOS, false otherwise
     */
    public boolean isIOS() {
        return this == IOS;
    }
}