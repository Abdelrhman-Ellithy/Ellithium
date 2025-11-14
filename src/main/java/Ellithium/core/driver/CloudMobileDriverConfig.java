package Ellithium.core.driver;

import Ellithium.core.logging.Logger;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for cloud-based mobile testing providers.
 * Supports BrowserStack, Sauce Labs, LambdaTest, and custom cloud providers.
 *
 * <p>This class extends MobileDriverConfig and adds cloud-specific capabilities
 * such as authentication, build names, project names, and provider-specific options.
 *
 * <p>Usage Example:
 * <pre>
 * CloudMobileDriverConfig config = new CloudMobileDriverConfig()
 *     .setCloudProvider(CloudProviderType.BROWSERSTACK)
 *     .setUsername("your_username")
 *     .setAccessKey("your_access_key")
 *     .setDriverType(MobileDriverType.Android)
 *     .setDeviceName("Google Pixel 7")
 *     .setPlatformVersion("13.0")
 *     .setApp("bs://app_id")
 *     .setProjectName("My Project")
 *     .setBuildName("Build 1.0");
 *
 * AndroidDriver driver = DriverFactory.getNewDriver(config);
 * </pre>
 */
public class CloudMobileDriverConfig extends MobileDriverConfig {
    private CloudProviderType cloudProvider;
    private String username;
    private String accessKey;
    private String projectName;
    private String buildName;
    private String testName;
    private String customHost;
    private Map<String, Object> providerOptions;
    private MutableCapabilities capabilities;

    /**
     * Default constructor initializing with LOCAL provider.
     */
    public CloudMobileDriverConfig() {
        super();
        this.cloudProvider = CloudProviderType.LOCAL;
        this.providerOptions = new HashMap<>();
        this.capabilities = new MutableCapabilities();
    }

    /**
     * Constructor with cloud provider.
     *
     * @param cloudProvider The cloud testing provider
     */
    public CloudMobileDriverConfig(CloudProviderType cloudProvider) {
        this();
        setCloudProvider(cloudProvider);
    }

    /**
     * Constructor with cloud provider and authentication.
     *
     * @param cloudProvider The cloud testing provider
     * @param username The username for authentication
     * @param accessKey The access key for authentication
     */
    public CloudMobileDriverConfig(CloudProviderType cloudProvider, String username, String accessKey) {
        this(cloudProvider);
        setUsername(username);
        setAccessKey(accessKey);
    }

    /**
     * Constructor with full configuration.
     *
     * @param cloudProvider The cloud testing provider
     * @param username The username for authentication
     * @param accessKey The access key for authentication
     * @param driverType The mobile driver type (Android/iOS)
     */
    public CloudMobileDriverConfig(CloudProviderType cloudProvider, String username, String accessKey,
                                   MobileDriverType driverType) {
        this(cloudProvider, username, accessKey);
        setDriverType(driverType);
    }

    // ========================================================================================
    // BUILDER PATTERN METHODS
    // ========================================================================================

    /**
     * Sets the cloud provider type.
     *
     * @param cloudProvider The cloud testing provider
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setCloudProvider(CloudProviderType cloudProvider) {
        this.cloudProvider = cloudProvider;
        updateRemoteAddress();
        return this;
    }

    /**
     * Sets the username for authentication.
     *
     * @param username The username
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setUsername(String username) {
        this.username = username;
        updateRemoteAddress();
        return this;
    }

    /**
     * Sets the access key for authentication.
     *
     * @param accessKey The access key
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setAccessKey(String accessKey) {
        this.accessKey = accessKey;
        updateRemoteAddress();
        return this;
    }

    /**
     * Sets the project name for test organization.
     *
     * @param projectName The project name
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setProjectName(String projectName) {
        this.projectName = projectName;
        addToProviderOptions("project", projectName);
        return this;
    }

    /**
     * Sets the build name for test organization.
     *
     * @param buildName The build name
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setBuildName(String buildName) {
        this.buildName = buildName;
        addToProviderOptions("build", buildName);
        return this;
    }

    /**
     * Sets the test name.
     *
     * @param testName The test name
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setTestName(String testName) {
        this.testName = testName;
        addToProviderOptions("name", testName);
        return this;
    }

    /**
     * Sets a custom host instead of the default provider host.
     * Useful for custom Sauce Labs data centers or enterprise BrowserStack instances.
     *
     * @param customHost The custom host (e.g., "ondemand.eu-central-1.saucelabs.com")
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setCustomHost(String customHost) {
        this.customHost = customHost;
        updateRemoteAddress();
        return this;
    }

    /**
     * Sets the device name for testing.
     *
     * @param deviceName The device name (e.g., "Google Pixel 7", "iPhone 14 Pro")
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setDeviceName(String deviceName) {
        capabilities.setCapability("deviceName", deviceName);
        // BrowserStack uses 'device' instead of 'deviceName'
        if (cloudProvider == CloudProviderType.BROWSERSTACK) {
            capabilities.setCapability("device", deviceName);
        }
        return this;
    }

    /**
     * Sets the platform version.
     *
     * @param platformVersion The OS version (e.g., "13.0", "16.0")
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setPlatformVersion(String platformVersion) {
        capabilities.setCapability("platformVersion", platformVersion);
        // BrowserStack uses 'osVersion'
        if (cloudProvider == CloudProviderType.BROWSERSTACK) {
            capabilities.setCapability("osVersion", platformVersion);
        }
        return this;
    }

    /**
     * Sets the app location.
     * Can be a cloud storage ID (e.g., "bs://app_id" for BrowserStack)
     * or a public URL.
     *
     * @param app The app location
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setApp(String app) {
        capabilities.setCapability("app", app);
        return this;
    }

    /**
     * Sets the automation name (e.g., "UiAutomator2", "XCUITest").
     *
     * @param automationName The automation name
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setAutomationName(String automationName) {
        capabilities.setCapability("automationName", automationName);
        return this;
    }

    /**
     * Enables or disables local testing (for apps accessing local/internal resources).
     *
     * @param enableLocal true to enable local testing
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setLocalTesting(boolean enableLocal) {
        switch (cloudProvider) {
            case BROWSERSTACK -> addToProviderOptions("browserstackLocal", enableLocal);
            case SAUCE_LABS -> addToProviderOptions("tunnelIdentifier", enableLocal ? "default" : null);
            case LAMBDATEST -> addToProviderOptions("tunnel", enableLocal);
        }
        return this;
    }

    /**
     * Sets the network profile for testing (e.g., "4g-lte", "3g", "edge").
     *
     * @param networkProfile The network profile name
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setNetworkProfile(String networkProfile) {
        addToProviderOptions("networkProfile", networkProfile);
        return this;
    }

    /**
     * Sets device orientation.
     *
     * @param orientation The orientation ("portrait" or "landscape")
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setDeviceOrientation(String orientation) {
        capabilities.setCapability("deviceOrientation", orientation);
        return this;
    }

    /**
     * Sets geolocation for testing.
     *
     * @param geoLocation The country code (e.g., "US", "UK", "FR")
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setGeoLocation(String geoLocation) {
        addToProviderOptions("geoLocation", geoLocation);
        return this;
    }

    /**
     * Sets the Appium version to use.
     *
     * @param appiumVersion The Appium version (e.g., "2.0.0", "1.22.3")
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setAppiumVersion(String appiumVersion) {
        addToProviderOptions("appiumVersion", appiumVersion);
        return this;
    }

    /**
     * Enables video recording of test execution.
     *
     * @param enableVideo true to enable video recording
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setVideoRecording(boolean enableVideo) {
        switch (cloudProvider) {
            case BROWSERSTACK -> addToProviderOptions("video", enableVideo);
            case SAUCE_LABS -> addToProviderOptions("recordVideo", enableVideo);
            case LAMBDATEST -> addToProviderOptions("video", enableVideo);
        }
        return this;
    }

    /**
     * Enables screenshot capturing during test execution.
     *
     * @param enableScreenshots true to enable screenshots
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setScreenshots(boolean enableScreenshots) {
        addToProviderOptions("debug", enableScreenshots);
        return this;
    }

    /**
     * Sets whether to test on a real device or emulator/simulator.
     *
     * @param isRealDevice true for real device, false for emulator/simulator
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig setRealDevice(boolean isRealDevice) {
        switch (cloudProvider) {
            case BROWSERSTACK -> capabilities.setCapability("realMobile", isRealDevice);
            case LAMBDATEST -> addToProviderOptions("isRealMobile", isRealDevice);
        }
        return this;
    }

    /**
     * Adds a custom provider-specific option.
     *
     * @param key The option key
     * @param value The option value
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig addProviderOption(String key, Object value) {
        addToProviderOptions(key, value);
        return this;
    }

    /**
     * Adds a custom capability.
     *
     * @param key The capability key
     * @param value The capability value
     * @return This config instance for method chaining
     */
    public CloudMobileDriverConfig addCapability(String key, Object value) {
        capabilities.setCapability(key, value);
        return this;
    }

    // ========================================================================================
    // OVERRIDE METHODS
    // ========================================================================================

    @Override
    public CloudMobileDriverConfig setDriverType(DriverType driverType) {
        super.setDriverType(driverType);
        if (driverType == MobileDriverType.Android) {
            capabilities.setCapability("platformName", "Android");
        } else if (driverType == MobileDriverType.IOS) {
            capabilities.setCapability("platformName", "iOS");
        }
        return this;
    }

    @Override
    public Capabilities getCapabilities() {
        MutableCapabilities finalCapabilities = new MutableCapabilities(capabilities);
        if (!providerOptions.isEmpty()) {
            switch (cloudProvider) {
                case BROWSERSTACK -> finalCapabilities.setCapability("bstack:options", new HashMap<>(providerOptions));
                case SAUCE_LABS -> finalCapabilities.setCapability("sauce:options", new HashMap<>(providerOptions));
                case LAMBDATEST -> finalCapabilities.setCapability("lt:options", new HashMap<>(providerOptions));
            }
        }

        return finalCapabilities;
    }

    @Override
    public void setCapabilities(Capabilities capabilities) {
        if (capabilities instanceof MutableCapabilities) {
            this.capabilities = (MutableCapabilities) capabilities;
        } else {
            this.capabilities = new MutableCapabilities(capabilities);
        }
    }

    // ========================================================================================
    // GETTERS
    // ========================================================================================

    public CloudProviderType getCloudProvider() {
        return cloudProvider;
    }

    public String getUsername() {
        return username;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getTestName() {
        return testName;
    }

    public String getCustomHost() {
        return customHost;
    }

    public Map<String, Object> getProviderOptions() {
        return new HashMap<>(providerOptions);
    }

    // ========================================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================================

    /**
     * Updates the remote address based on current configuration.
     */
    private void updateRemoteAddress() {
        try {
            String urlString;
            if (customHost != null && !customHost.isEmpty()) {
                urlString = cloudProvider.getHubUrl(customHost, username, accessKey);
            } else {
                urlString = cloudProvider.getHubUrl(username, accessKey);
            }
            setRemoteAddress(new URL(urlString));
        } catch (MalformedURLException e) {
            Logger.logException(e);
        }
    }

    /**
     * Adds an option to provider-specific options map.
     *
     * @param key The option key
     * @param value The option value
     */
    private void addToProviderOptions(String key, Object value) {
        if (value != null) {
            providerOptions.put(key, value);
        }
    }

    /**
     * Validates the configuration before driver creation.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    public void validate() {
        if (cloudProvider.requiresAuth()) {
            if (username == null || username.isEmpty()) {
                throw new IllegalStateException("Username is required for " + cloudProvider + " provider");
            }
            if (accessKey == null || accessKey.isEmpty()) {
                throw new IllegalStateException("Access key is required for " + cloudProvider + " provider");
            }
        }

        if (getDriverType() == null) {
            throw new IllegalStateException("Driver type must be set (Android or iOS)");
        }
    }
}