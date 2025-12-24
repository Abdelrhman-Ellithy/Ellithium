package Ellithium.core.driver;

import Ellithium.core.logging.Logger;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import java.net.MalformedURLException;
import java.net.URL;
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
    private MutableCapabilities internalCapabilities;

    /**
     * Default constructor initializing with LOCAL provider.
     */
    public CloudMobileDriverConfig() {
        super();
        this.cloudProvider = CloudProviderType.LOCAL;
        this.providerOptions = new HashMap<>();
        this.internalCapabilities = new MutableCapabilities();

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
    // BUILDER PATTERN METHODS - CLOUD PROVIDER
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
        if (cloudProvider == CloudProviderType.BROWSERSTACK) {
            addToProviderOptions("projectName", projectName);
        } else {
            addToProviderOptions("project", projectName);
        }
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
        if (cloudProvider == CloudProviderType.BROWSERSTACK) {
            addToProviderOptions("buildName", buildName);
        } else {
            addToProviderOptions("build", buildName);
        }
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
        if (cloudProvider == CloudProviderType.BROWSERSTACK) {
            addToProviderOptions("sessionName", testName);
        } else {
            addToProviderOptions("name", testName);
        }
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

    // ========================================================================================
    // BUILDER PATTERN METHODS - DEVICE CONFIGURATION (OVERRIDE PARENT)
    // ========================================================================================

    /**
     * Sets the device name for testing.
     *
     * @param deviceName The device name (e.g., "Google Pixel 7", "iPhone 14 Pro")
     * @return This config instance for method chaining
     */
    @Override
    public CloudMobileDriverConfig setDeviceName(String deviceName) {
        if (cloudProvider == CloudProviderType.BROWSERSTACK || cloudProvider == CloudProviderType.LAMBDATEST) {
            addToProviderOptions("deviceName", deviceName);
        } else {
            internalCapabilities.setCapability("appium:deviceName", deviceName);
        }
        return this;
    }

    /**
     * Sets the platform version.
     *
     * @param platformVersion The OS version (e.g., "13.0", "16.0")
     * @return This config instance for method chaining
     */
    @Override
    public CloudMobileDriverConfig setPlatformVersion(String platformVersion) {
        if (cloudProvider == CloudProviderType.BROWSERSTACK) {
            addToProviderOptions("osVersion", platformVersion);
        } else if (cloudProvider == CloudProviderType.LAMBDATEST) {
            String majorVersion = platformVersion.contains(".") ? platformVersion.split("\\.")[0] : platformVersion;
            addToProviderOptions("platformVersion", majorVersion);
        } else {
            internalCapabilities.setCapability("appium:platformVersion", platformVersion);
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
    @Override
    public CloudMobileDriverConfig setApp(String app) {
        //capabilities.setCapability("app", app);
        internalCapabilities.setCapability("appium:app", app);
        return this;
    }

    /**
     * Sets the automation name (e.g., "UiAutomator2", "XCUITest").
     *
     * @param automationName The automation name
     * @return This config instance for method chaining
     */
    @Override
    public CloudMobileDriverConfig setAutomationName(String automationName) {
        internalCapabilities.setCapability("appium:automationName", automationName);
        return this;
    }

    /**
     * Sets device orientation.
     *
     * @param orientation The orientation ("portrait" or "landscape")
     * @return This config instance for method chaining
     */
    @Override
    public CloudMobileDriverConfig setDeviceOrientation(String orientation) {
        if (cloudProvider == CloudProviderType.BROWSERSTACK) {
            addToProviderOptions("deviceOrientation", orientation);
        } else {
            internalCapabilities.setCapability("appium:orientation", orientation);
        }
        return this;
    }


    // ========================================================================================
    // BUILDER PATTERN METHODS - CLOUD-SPECIFIC FEATURES
    // ========================================================================================

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
            case BROWSERSTACK, LAMBDATEST -> addToProviderOptions("video", enableVideo);
            case SAUCE_LABS -> addToProviderOptions("recordVideo", enableVideo);
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
            case BROWSERSTACK -> addToProviderOptions("realMobile", isRealDevice);
            case LAMBDATEST -> {
                providerOptions.put("w3c", true);
                addToProviderOptions("isRealMobile", isRealDevice);
            }
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
    @Override
    public CloudMobileDriverConfig addCapability(String key, Object value) {
        internalCapabilities.setCapability(key, value);
        return this;
    }

    // ========================================================================================
    // OVERRIDE METHODS
    // ========================================================================================

    @Override
    public CloudMobileDriverConfig setDriverType(DriverType driverType) {
        super.setDriverType(driverType);
        if (driverType == MobileDriverType.Android) {
            internalCapabilities.setCapability("platformName", "Android");
        } else if (driverType == MobileDriverType.IOS) {
            internalCapabilities.setCapability("platformName", "iOS");
        }
        return this;
    }

    @Override
    public Capabilities getCapabilities() {
        MutableCapabilities finalCapabilities;
        if (getDriverType() == MobileDriverType.IOS) {
            finalCapabilities = new XCUITestOptions(internalCapabilities);
        } else {
            // Default to Android if not specified or explicitly Android
            finalCapabilities = new UiAutomator2Options(internalCapabilities);
        }
        if (finalCapabilities.getCapability("appium:platformName") == null) {
            String platform = String.valueOf(finalCapabilities.getCapability("platformName"));
            finalCapabilities.setCapability("appium:automationName", platform.equalsIgnoreCase("ios") ? "XCUITest" : "UiAutomator2");
        }
        if (!providerOptions.isEmpty()) {
            switch (cloudProvider) {
                case BROWSERSTACK -> {
                    finalCapabilities.setCapability("bstack:options", new HashMap<>(providerOptions));
                }
                case SAUCE_LABS -> {
                    providerOptions.putIfAbsent("appiumVersion", "stable");
                    finalCapabilities.setCapability("sauce:options", new HashMap<>(providerOptions));
                }
                case LAMBDATEST -> {
                    providerOptions.putIfAbsent("w3c", true);
                    String platform= finalCapabilities.getCapability("platformName").toString();
                    finalCapabilities.setCapability("platformName",platform.toLowerCase());
                    finalCapabilities.setCapability("lt:options", new HashMap<>(providerOptions));
                }
            }
        }

        return finalCapabilities;
    }

    /**
     * Sets the Driver capabilities.
     * This replaces all existing capabilities.
     *
     * @param capabilities The Driver capabilities to configure
     */
    @Override
    public CloudMobileDriverConfig setCapabilities(Capabilities capabilities) {
        if (capabilities instanceof MutableCapabilities) {
            this.internalCapabilities = (MutableCapabilities) capabilities;
        } else {
            this.internalCapabilities = new MutableCapabilities(capabilities);
        }
        return this;
    }

    // ========================================================================================
    // GETTERS
    // ========================================================================================

    /**
     * Gets the cloud provider type.
     *
     * @return The cloud provider
     */
    public CloudProviderType getCloudProvider() {
        return cloudProvider;
    }

    /**
     * Gets the username for authentication.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the access key for authentication.
     *
     * @return The access key
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Gets the project name.
     *
     * @return The project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Gets the build name.
     *
     * @return The build name
     */
    public String getBuildName() {
        return buildName;
    }

    /**
     * Gets the test name.
     *
     * @return The test name
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Gets the custom host.
     *
     * @return The custom host or null if using default
     */
    public String getCustomHost() {
        return customHost;
    }

    /**
     * Gets a copy of the provider-specific options.
     *
     * @return A Map of provider specific options
     */    
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
    @Override
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