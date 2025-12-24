package Ellithium.core.driver;

import Ellithium.core.execution.listener.appiumListener;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.execution.listener.seleniumListener;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import java.net.URL;
import static Ellithium.core.driver.MobileDriverType.IOS;
import static io.appium.java_client.proxy.Helpers.createProxy;

/**
 * Factory class for creating and managing WebDriver instances.
 * This class provides thread-safe creation and management of different types of WebDriver instances
 * including local browsers, remote browsers, and mobile devices (Android and iOS).
 *
 * <p>The factory supports various configuration options through different driver configs:
 * <ul>
 *   <li>LocalDriverConfig - For local browser instances</li>
 *   <li>RemoteDriverConfig - For remote browser instances</li>
 *   <li>MobileDriverConfig - For mobile device automation (local Appium)</li>
 *   <li>CloudMobileDriverConfig - For cloud mobile testing (BrowserStack, Sauce Labs, LambdaTest)</li>
 * </ul>
 *
 * <p>Thread Safety: This factory maintains separate ThreadLocal instances for different driver types,
 * ensuring thread-safe operation in parallel test execution scenarios.
 *
 * <p>Usage Examples:
 * <pre>
 * // Local browser
 * WebDriver driver = DriverFactory.getNewDriver(new LocalDriverConfig()
 *     .setLocalDriverType(LocalDriverType.CHROME)
 *     .setHeadlessMode(HeadlessMode.True));
 *
 * // Cloud mobile
 * AndroidDriver driver = DriverFactory.getNewDriver(new CloudMobileDriverConfig()
 *     .setCloudProvider(CloudProviderType.BROWSERSTACK)
 *     .setUsername("user")
 *     .setAccessKey("key")
 *     .setDriverType(MobileDriverType.Android)
 *     .setDeviceName("Google Pixel 7"));
 * </pre>
 */
public class DriverFactory {
    private static ThreadLocal<WebDriver> WebDriverThread = new ThreadLocal<>();
    private static ThreadLocal<AndroidDriver> AndroidDriverThread = new ThreadLocal<>();
    private static ThreadLocal<IOSDriver> IOSDriverThread = new ThreadLocal<>();
    private static ThreadLocal<DriverConfiguration> driverConfigurationThread=new ThreadLocal<>();

    // ========================================================================================
    // MAIN FACTORY METHOD
    // ========================================================================================

    /**
     * Factory method that creates appropriate driver based on configuration type.
     * This is the primary entry point for driver creation using config objects.
     *
     * <p>Supported Configuration Types:
     * <ul>
     *   <li>LocalDriverConfig - Creates local browser instances</li>
     *   <li>RemoteDriverConfig - Creates remote browser instances</li>
     *   <li>CloudMobileDriverConfig - Creates cloud mobile device instances</li>
     *   <li>MobileDriverConfig - Creates local mobile device instances</li>
     * </ul>
     *
     * @param driverConfigBuilder Configuration builder instance
     * @param <T> Type of driver to be returned (WebDriver, AndroidDriver, or IOSDriver)
     * @return Configured driver instance
     * @throws IllegalArgumentException if unknown config type is provided
     */
    public static <T> T getNewDriver(DriverConfigBuilder driverConfigBuilder) {
        return switch (driverConfigBuilder) {
            case LocalDriverConfig localDriverConfig -> getNewDriver(localDriverConfig);
            case RemoteDriverConfig remoteDriverConfig -> getNewDriver(remoteDriverConfig);
            case CloudMobileDriverConfig cloudMobileConfig -> getNewDriver(cloudMobileConfig);
            case MobileDriverConfig mobileDriverConfig -> getNewDriver(mobileDriverConfig);
            case null, default ->
                    throw new IllegalArgumentException("Unknown driver config type: " + driverConfigBuilder.getClass().getName());
        };
    }

    // ========================================================================================
    // LOCAL WEB DRIVER SECTION
    // ========================================================================================

    /**
     * Creates a new WebDriver instance using local driver configuration.
     *
     * @param localDriverConfig Configuration for local browser instance
     * @param <T> Type of WebDriver to be returned
     * @return Configured WebDriver instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNewDriver(LocalDriverConfig localDriverConfig) {
        DriverConfiguration driverConfiguration=new DriverConfiguration(
                localDriverConfig.getLocalDriverType(),
                localDriverConfig.getHeadlessMode(),
                localDriverConfig.getPageLoadStrategy(),
                localDriverConfig.getPrivateMode(),
                localDriverConfig.getSandboxMode(),
                localDriverConfig.getWebSecurityMode()
                ,localDriverConfig.getCapabilities(),
                false);
        driverConfigurationThread.set(driverConfiguration);
        webSetUp();
        return (T) WebDriverThread.get();
    }

    /**
     * Creates a new local WebDriver instance with detailed configuration options.
     *
     * @param driverType Browser type to be instantiated
     * @param headlessMode Whether to run browser in headless mode
     * @param privateMode Whether to run browser in private/incognito mode
     * @param pageLoadStrategyMode Strategy for handling page loads
     * @param webSecurityMode Security settings for the browser
     * @param sandboxMode Sandbox mode configuration
     * @param <T> Type of WebDriver to be returned
     * @return Configured local WebDriver instance
     */
    public static <T> T getNewLocalDriver(LocalDriverType driverType,
                                          HeadlessMode headlessMode,
                                          PrivateMode privateMode,
                                          PageLoadStrategyMode pageLoadStrategyMode,
                                          WebSecurityMode webSecurityMode,
                                          SandboxMode sandboxMode) {
        Capabilities emptyCaps = new MutableCapabilities();
        LocalDriverConfig localDriverConfig=new LocalDriverConfig(
                driverType,
                emptyCaps,
                headlessMode,
                privateMode,
                pageLoadStrategyMode,
                webSecurityMode,
                sandboxMode).setCapabilities(emptyCaps);
        return getNewDriver(localDriverConfig);
    }
    /**
     * Creates a new local WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * 
     * @param driverType Browser type to be instantiated
     * @param headlessMode Whether to run browser in headless mode
     * @param privateMode Whether to run browser in private/incognito mode
     * @param pageLoadStrategyMode Strategy for handling page loads
     * @param webSecurityMode Security settings for the browser
     * @param <T> Type of WebDriver to be returned
     * @return Configured local WebDriver instance
     */
    public static <T> T getNewLocalDriver(
            LocalDriverType driverType,
            HeadlessMode headlessMode,
            PrivateMode privateMode,
            PageLoadStrategyMode pageLoadStrategyMode,
            WebSecurityMode webSecurityMode) {
        return getNewLocalDriver(driverType, headlessMode, privateMode, pageLoadStrategyMode, webSecurityMode, SandboxMode.Sandbox);
    }

    /**
     * Creates a new local WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * sets WebSecurityMode to default value SecureMode
     * 
     * @param driverType Browser type to be instantiated
     * @param headlessMode Whether to run browser in headless mode
     * @param privateMode Whether to run browser in private/incognito mode
     * @param pageLoadStrategyMode Strategy for handling page loads
     * @param <T> Type of WebDriver to be returned
     * @return Configured local WebDriver instance
     */
    public static <T> T getNewLocalDriver(
            LocalDriverType driverType,
            HeadlessMode headlessMode,
            PrivateMode privateMode,
            PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewLocalDriver(driverType, headlessMode, privateMode, pageLoadStrategyMode, WebSecurityMode.SecureMode);
    }

    /**
     * Creates a new local WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * sets WebSecurityMode to default value SecureMode
     * sets PageLoadStrategyMode to default value Normal
     * 
     * @param driverType Browser type to be instantiated
     * @param headlessMode Whether to run browser in headless mode
     * @param privateMode Whether to run browser in private/incognito mode
     * @param <T> Type of WebDriver to be returned
     * @return Configured local WebDriver instance
     */
    public static <T> T getNewLocalDriver(
            LocalDriverType driverType,
            HeadlessMode headlessMode,
            PrivateMode privateMode) {
        return getNewLocalDriver(driverType,headlessMode,privateMode,PageLoadStrategyMode.Normal);
    }

    /**
     * Creates a new local WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * sets WebSecurityMode to default value SecureMode
     * sets PageLoadStrategyMode to default value Normal
     * sets PrivateMode to default value False
     * 
     * @param driverType Browser type to be instantiated
     * @param headlessMode Whether to run browser in headless mode
     * @param <T> Type of WebDriver to be returned
     * @return Configured local WebDriver instance
     */
    public static <T > T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode) {
        return getNewLocalDriver(driverType,headlessMode,PrivateMode.False);
    }

    /**
     * Creates a new local WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * sets WebSecurityMode to default value SecureMode
     * sets PageLoadStrategyMode to default value Normal
     * sets PrivateMode to default value False
     * sets HeadlessMode to default value False
     * 
     * @param driverType Browser type to be instantiated
     * @param <T> Type of WebDriver to be returned
     * @return Configured local WebDriver instance
     */
    public static <T> T  getNewLocalDriver(LocalDriverType driverType) {
        return getNewLocalDriver(driverType,HeadlessMode.False);
    }

    // ========================================================================================
    // REMOTE WEB DRIVER SECTION
    // ========================================================================================

    /**
     * Creates a new WebDriver instance using remote driver configuration.
     *
     * @param remoteDriverConfig Configuration for remote browser instance
     * @param <T> Type of WebDriver to be returned
     * @return Configured WebDriver instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNewDriver(RemoteDriverConfig remoteDriverConfig) {
        DriverConfiguration driverConfiguration=new DriverConfiguration(
                remoteDriverConfig.getDriverType(),
                remoteDriverConfig.getHeadlessMode(),
                remoteDriverConfig.getPageLoadStrategy(),
                remoteDriverConfig.getPrivateMode(),
                remoteDriverConfig.getSandboxMode(),
                remoteDriverConfig.getWebSecurityMode()
                ,remoteDriverConfig.getCapabilities(),
                false);
        driverConfigurationThread.set(driverConfiguration);
        webSetUp();
        return (T)WebDriverThread.get();
    }

    /**
     * Creates a new remote WebDriver instance with detailed configuration options.
     * @param <T> Type of WebDriver to be returned
     * @param driverType Browser type to be instantiated
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Desired capabilities for the remote WebDriver
     * @param headlessMode Whether to run browser in headless mode
     * @param privateMode Whether to run browser in private/incognito mode
     * @param pageLoadStrategyMode Strategy for handling page loads
     * @param webSecurityMode Security settings for the browser
     * @param sandboxMode Sandbox mode configuration
     * @return Configured remote WebDriver instance
     */
    public static <T> T getNewRemoteDriver(
            RemoteDriverType driverType,
            URL remoteAddress,
            Capabilities capabilities,
            HeadlessMode headlessMode,
            PrivateMode privateMode,
            PageLoadStrategyMode pageLoadStrategyMode,
            WebSecurityMode webSecurityMode,
            SandboxMode sandboxMode) {
        RemoteDriverConfig remoteDriverConfig=new RemoteDriverConfig(
                driverType, remoteAddress,
                capabilities, headlessMode,
                privateMode, pageLoadStrategyMode,
                webSecurityMode,sandboxMode);
        return getNewDriver(remoteDriverConfig);
    }

    /**
     * Creates a new remote WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * @param <T> Type of WebDriver to be returned
     * @param driverType Browser type to be instantiated
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Desired capabilities for the remote WebDriver
     * @param headlessMode Whether to run browser in headless mode
     * @param privateMode Whether to run browser in private/incognito mode
     * @param pageLoadStrategyMode Strategy for handling page loads
     * @param webSecurityMode Security settings for the browser
     * @return Configured remote WebDriver instance
     */
    public static <T> T getNewRemoteDriver(
            RemoteDriverType driverType,
            URL remoteAddress,
            Capabilities capabilities,
            HeadlessMode headlessMode,
            PrivateMode privateMode,
            PageLoadStrategyMode pageLoadStrategyMode,
            WebSecurityMode webSecurityMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,pageLoadStrategyMode,webSecurityMode,SandboxMode.Sandbox);
    }

    /**
     * Creates a new remote WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * sets WebSecurityMode to default value SecureMode
     * @param <T> Type of WebDriver to be returned
     * @param driverType Browser type to be instantiated
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Desired capabilities for the remote WebDriver
     * @param headlessMode Whether to run browser in headless mode
     * @param privateMode Whether to run browser in private/incognito mode
     * @param pageLoadStrategyMode Strategy for handling page loads
     * @return Configured remote WebDriver instance
     */
    public static <T> T getNewRemoteDriver(
            RemoteDriverType driverType,
            URL remoteAddress,
            Capabilities capabilities,
            HeadlessMode headlessMode,
            PrivateMode privateMode,
            PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,pageLoadStrategyMode,WebSecurityMode.SecureMode);
    }

    /**
     * Creates a new remote WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * sets WebSecurityMode to default value SecureMode
     * sets PageLoadStrategyMode to default value Normal
     * @param <T> Type of WebDriver to be returned
     * @param driverType Browser type to be instantiated
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Desired capabilities for the remote WebDriver
     * @param headlessMode Whether to run browser in headless mode
     * @param privateMode Whether to run browser in private/incognito mode
     * @return Configured remote WebDriver instance
     */
    public static <T> T getNewRemoteDriver(
            RemoteDriverType driverType,
            URL remoteAddress,
            Capabilities capabilities,
            HeadlessMode headlessMode,
            PrivateMode privateMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode, PageLoadStrategyMode.Normal);
    }


    /**
     * Creates a new remote WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * sets WebSecurityMode to default value SecureMode
     * sets PageLoadStrategyMode to default value Normal
     * sets PrivateMode to default value False
     * @param <T> Type of WebDriver to be returned
     * @param driverType Browser type to be instantiated
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Desired capabilities for the remote WebDriver
     * @param headlessMode Whether to run browser in headless mode
     * @return Configured remote WebDriver instance
     */
    public static <T> T getNewRemoteDriver(
            RemoteDriverType driverType,
            URL remoteAddress,
            Capabilities capabilities,
            HeadlessMode headlessMode)  {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,PrivateMode.False);
    }


    /**
     * Creates a new remote WebDriver instance with detailed configuration options.
     * sets SandboxMode to Sandbox by default
     * sets WebSecurityMode to default value SecureMode
     * sets PageLoadStrategyMode to default value Normal
     * sets PrivateMode to default value False
     * sets HeadlessMode to default value False
     * @param <T> Type of WebDriver to be returned
     * @param driverType Browser type to be instantiated
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Desired capabilities for the remote WebDriver
     * @return Configured remote WebDriver instance
     */
    public static <T> T getNewRemoteDriver(
            RemoteDriverType driverType,
            URL remoteAddress,
            Capabilities capabilities) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,HeadlessMode.False);
    }

    // ========================================================================================
    // MOBILE DRIVER SECTION - LOCAL APPIUM
    // ========================================================================================

    /**
     * Creates a new mobile driver instance using mobile driver configuration.
     *
     * @param mobileDriverConfig Configuration for mobile device
     * @param <T> Type of mobile driver to be returned (AndroidDriver or IOSDriver)
     * @return Configured mobile driver instance
     */
    public static <T> T getNewDriver(MobileDriverConfig mobileDriverConfig) {
        HeadlessMode mode=checkMobileHeadless(mobileDriverConfig.getCapabilities());
        DriverConfiguration driverConfiguration=new DriverConfiguration(
                mobileDriverConfig.getDriverType(),
                mode,
                mobileDriverConfig.getCapabilities(),
                false);
        driverConfigurationThread.set(driverConfiguration);
        return mobileSetup(
                (MobileDriverType) mobileDriverConfig.getDriverType(),
                mobileDriverConfig.getRemoteAddress(),
                mobileDriverConfig.getCapabilities()
        );
    }

    /**
     * Creates a new mobile driver instance with full configuration.
     * This is the most detailed mobile driver creation method for local Appium.
     *
     * @param driverType Type of mobile driver (Android or iOS)
     * @param remoteAddress URL of the Appium server
     * @param capabilities Desired capabilities for the mobile driver
     * @param <T> Type of mobile driver to be returned (AndroidDriver or IOSDriver)
     * @return Configured mobile driver instance
     */
    public static <T> T getNewMobileDriver(
            MobileDriverType driverType,
            URL remoteAddress,
            Capabilities capabilities) {
        MobileDriverConfig mobileDriverConfig =new MobileDriverConfig(driverType,capabilities,remoteAddress);
        return getNewDriver(mobileDriverConfig);
    }
    
    /**
     * Creates a new mobile driver instance with simplified configuration.
     * Uses default Appium server URL (<a href="http://127.0.0.1:4723">...</a>).
     *
     * @param driverType Type of mobile driver (Android or iOS)
     * @param capabilities Desired capabilities for the mobile driver
     * @param <T> Type of mobile driver to be returned (AndroidDriver or IOSDriver)
     * @return Configured mobile driver instance
     */
    public static <T> T getNewMobileDriver(
            MobileDriverType driverType,
            Capabilities capabilities) {
        MobileDriverConfig config = new MobileDriverConfig(driverType);
        config.setCapabilities(capabilities);
        return getNewDriver(config);
    }

    // ========================================================================================
    // MOBILE DRIVER SECTION - CLOUD PROVIDERS
    // ========================================================================================

    /**
     * Creates a new mobile driver instance using cloud mobile driver configuration.
     * Supports BrowserStack, Sauce Labs, LambdaTest, and other cloud providers.
     *
     * @param cloudMobileConfig Configuration for cloud mobile device
     * @param <T> Type of mobile driver to be returned (AndroidDriver or IOSDriver)
     * @return Configured mobile driver instance
     */
    public static <T> T getNewDriver(CloudMobileDriverConfig cloudMobileConfig) {
        cloudMobileConfig.validate();
        HeadlessMode mode=checkMobileHeadless(cloudMobileConfig.getCapabilities());
        DriverConfiguration driverConfiguration=new DriverConfiguration(
                cloudMobileConfig.getDriverType(),
                mode,
                cloudMobileConfig.getCapabilities(),
                true
        );
        driverConfigurationThread.set(driverConfiguration);
        Reporter.logReportOnly("Capabilities: "+cloudMobileConfig.getCapabilities().asMap().toString(),LogLevel.INFO_BLUE);
        Reporter.log("Creating driver: "+ ((MobileDriverType)cloudMobileConfig.getDriverType()).getPlatformName()+ " for " + cloudMobileConfig.getCloudProvider() +
                " cloud provider", LogLevel.INFO_BLUE);
        checkMobileHeadless(cloudMobileConfig.getCapabilities());
         return mobileSetup( (MobileDriverType) cloudMobileConfig.getDriverType(), cloudMobileConfig.getRemoteAddress(), cloudMobileConfig.getCapabilities());
    }

    /**
     * Creates a new cloud mobile driver with full configuration.
     * This is the most detailed cloud mobile driver creation method.
     *
     * @param provider The cloud provider type
     * @param username The username for authentication
     * @param accessKey The access key for authentication
     * @param driverType The mobile driver type (Android or iOS)
     * @param deviceName The device name to test on
     * @param platformVersion The platform version
     * @param app The app location/ID (e.g., "bs://app_id" for BrowserStack)
     * @param projectName The project name for organization
     * @param buildName The build name for organization
     * @param testName The test name
     * @param <T> Type of mobile driver to be returned (AndroidDriver or IOSDriver)
     * @return Configured mobile driver instance
     */
    public static <T> T getNewCloudMobileDriver(
            CloudProviderType provider,
            String username,
            String accessKey,
            MobileDriverType driverType,
            String deviceName,
            String platformVersion,
            String app,
            String projectName,
            String buildName,
            String testName) {
        CloudMobileDriverConfig config = new CloudMobileDriverConfig(provider, username, accessKey, driverType)
                .setDeviceName(deviceName)
                .setPlatformVersion(platformVersion)
                .setApp(app)
                .setProjectName(projectName)
                .setBuildName(buildName)
                .setTestName(testName);
        return getNewDriver(config);
    }

    /**
     * Creates a new cloud mobile driver with essential configuration.
     * Omits project, build, and test names for simpler setup.
     *
     * @param provider The cloud provider type
     * @param username The username for authentication
     * @param accessKey The access key for authentication
     * @param driverType The mobile driver type (Android or iOS)
     * @param deviceName The device name to test on
     * @param platformVersion The platform version
     * @param app The app location/ID (e.g., "bs://app_id" for BrowserStack)
     * @param <T> Type of mobile driver to be returned (AndroidDriver or IOSDriver)
     * @return Configured mobile driver instance
     */
    public static <T> T getNewCloudMobileDriver(
            CloudProviderType provider,
            String username,
            String accessKey,
            MobileDriverType driverType,
            String deviceName,
            String platformVersion,
            String app) {
        CloudMobileDriverConfig config = new CloudMobileDriverConfig(provider, username, accessKey, driverType)
                .setDeviceName(deviceName)
                .setPlatformVersion(platformVersion)
                .setApp(app);
        return getNewDriver(config);
    }

    /**
     * Creates a new cloud mobile driver with minimal configuration.
     * Uses the cloud provider's default settings for most options.
     *
     * @param provider The cloud provider type
     * @param username The username for authentication
     * @param accessKey The access key for authentication
     * @param driverType The mobile driver type (Android or iOS)
     * @param deviceName The device name to test on
     * @param app The app location/ID
     * @param <T> Type of mobile driver to be returned (AndroidDriver or IOSDriver)
     * @return Configured mobile driver instance
     */
    public static <T> T getNewCloudMobileDriver(
            CloudProviderType provider,
            String username,
            String accessKey,
            MobileDriverType driverType,
            String deviceName,
            String app) {
        CloudMobileDriverConfig config = new CloudMobileDriverConfig(provider, username, accessKey, driverType)
                .setDeviceName(deviceName)
                .setApp(app);
        return getNewDriver(config);
    }

    // ========================================================================================
    // DRIVER LIFECYCLE MANAGEMENT
    // ========================================================================================

    /**
     * Gets the current driver instance for the executing thread.
     *
     * @param <T> Type of driver to be returned
     * @return Current driver instance or null if no driver exists
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCurrentDriver() {
       DriverConfiguration currentDriverConfigurationThread=driverConfigurationThread.get();
       if (currentDriverConfigurationThread!=null){
           DriverType driverType=driverConfigurationThread.get().getDriverType();
           if(driverType!=null){
               if (driverType.equals(MobileDriverType.Android)) {
                   return (T) AndroidDriverThread.get();
               } else if (driverType.equals(IOS)) {
                   return (T) IOSDriverThread.get();
               } else if (driverType instanceof LocalDriverType || driverType instanceof RemoteDriverType ) {
                   return (T) WebDriverThread.get();
               }
           }
       }
        return null;
    }

    /**
     * Quits the current driver instance and closes all associated windows.
     */
    public static void quitDriver() {
        var driverType=driverConfigurationThread.get().getDriverType();
        if (driverType!=null){
            if (driverType==MobileDriverType.Android) {
                AndroidDriver localDriver = AndroidDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
            } else if (driverType==IOS) {
                IOSDriver localDriver = IOSDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
            } else if (driverType instanceof LocalDriverType || driverType instanceof RemoteDriverType ) {
                var localDriver = WebDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
            }
            removeDriver();
        }
    }

    /**
     * Removes the current driver instance from thread local storage.
     */
    public static void removeDriver() {
        DriverType driverType=driverConfigurationThread.get().getDriverType();
        if(driverType!=null) {
            if (driverType.equals(MobileDriverType.Android)) {
                AndroidDriverThread.remove();
            } else if (driverType.equals(IOS)) {
                IOSDriverThread.remove();
            } else if (driverType instanceof LocalDriverType || driverType instanceof RemoteDriverType ) {
                WebDriverThread.remove();
            }
            driverConfigurationThread.remove();
        }
    }

    /**
     * Gets the current driver configuration for the executing thread.
     *
     * @return Current driver configuration
     */
    public static DriverConfiguration getCurrentDriverConfiguration(){
        return driverConfigurationThread.get();
    }

    /**
     * Internal Method to remove the current driver configuration for the executing thread.
     * Automatically managed don't call it
     *
     */
    public static void removeCurrentDriverConfiguration(){
        driverConfigurationThread.remove();
    }

    // ========================================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================================

    /**
     * Sets up a web driver instance with specified configuration.
     * Handles both local and remote web driver setup.
     *
     * @throws IllegalStateException if driver creation fails
     */
    private static void webSetUp() {
        DriverConfiguration currentDriverConfig=getCurrentDriverConfiguration();
        DriverType driverType =currentDriverConfig.getDriverType();
        HeadlessMode headlessMode = currentDriverConfig.getHeadlessMode();
        PageLoadStrategyMode PageLoadStrategy=currentDriverConfig.getPageLoadStrategy();
        PrivateMode PrivateMode=currentDriverConfig.getPrivateMode();
        SandboxMode SandboxMode=currentDriverConfig.getSandboxMode();
        WebSecurityMode WebSecurityMode=currentDriverConfig.getWebSecurityMode();
        Capabilities capabilities=currentDriverConfig.getCapabilities();
        WebDriver localDriver;
        if (driverType instanceof RemoteDriverType) {
            var remoteAddress = currentDriverConfig.getRemoteAddress();
            localDriver = BrowserSetUp.setupRemoteDriver(driverType, remoteAddress, capabilities, headlessMode, PageLoadStrategy, PrivateMode, SandboxMode, WebSecurityMode);
        } else {
            localDriver = BrowserSetUp.setupLocalDriver(driverType, capabilities,headlessMode, PageLoadStrategy, PrivateMode, SandboxMode, WebSecurityMode);
        }
        WebDriverThread.set(getDecoratedWebDriver(localDriver));
        if (WebDriverThread != null) {
            Reporter.log("Driver Created", LogLevel.INFO_GREEN);
        } else {
            Reporter.log("Driver Creation Failed", LogLevel.ERROR);
        }
    }

    /**
     * Sets up a mobile driver instance with specified configuration.
     * KEY INSIGHT: This method works for BOTH local and cloud!
     * The only difference is the URL and capabilities passed in.
     * Local: <a href="http://127.0.0.1:4723">...</a> with basic capabilities
     * Cloud: <a href="https://user:key@hub.browserstack.com/wd/hub">...</a> with cloud capabilities
     * @param driverType Type of mobile driver (Android or iOS)
     * @param remoteAddress URL of the Appium server
     * @param capabilities Desired capabilities for the mobile driver
     * @param <T> Type of mobile driver to be returned
     * @return Configured mobile driver instance
     * @throws IllegalArgumentException if invalid driver type is specified
     */
    @SuppressWarnings("unchecked")
    private static <T> T mobileSetup(MobileDriverType driverType, URL remoteAddress, Capabilities capabilities){
        switch (driverType){
            case IOS -> {
                IOSDriver localDriver=getDecoratedIOSDriver(remoteAddress, capabilities);
                IOSDriverThread.set(localDriver);
                if(IOSDriverThread!=null){
                    Reporter.log("Driver Created", LogLevel.INFO_GREEN);
                    return (T)IOSDriverThread.get();
                }
            }
            case Android -> {
                AndroidDriver localDriver=getDecoratedAndroidDriver(remoteAddress, capabilities);
                AndroidDriverThread.set(localDriver);
                if(IOSDriverThread!=null){
                    Reporter.log("Driver Created", LogLevel.INFO_GREEN);
                    return (T)AndroidDriverThread.get();
                }
            }
            default -> throw new IllegalArgumentException("Wrong Driver Initialization: " + getCurrentDriverConfiguration().getDriverType()+ "visit: https://github.com/Abdelrhman-Ellithy/Ellithium to know how the correct way");
        }
        Reporter.log("Driver Creation Failed",LogLevel.ERROR);
        return null;
    }

    /**
     * Creates a decorated WebDriver instance with event listening capabilities.
     *
     * @param driver Base WebDriver instance to be decorated
     * @return Decorated WebDriver instance
     */
    private static WebDriver getDecoratedWebDriver(WebDriver driver){
        return new EventFiringDecorator<>(org.openqa.selenium.WebDriver.class, new seleniumListener()).decorate(driver);
    }

    /**
     * Creates a decorated AndroidDriver instance with event listening capabilities.
     *
     * @param remoteAddress Appium server URL
     * @param capabilities Desired capabilities for Android
     * @return Decorated AndroidDriver instance
     */
    private static AndroidDriver getDecoratedAndroidDriver(URL remoteAddress, Capabilities capabilities){
        return createProxy(
                AndroidDriver.class,
                new Object[] {remoteAddress,capabilities},
                new Class[] {URL.class,Capabilities.class},
                new appiumListener()
        );
    }

    /**
     * Creates a decorated IOSDriver instance with event listening capabilities.
     *
     * @param remoteAddress Appium server URL
     * @param capabilities Desired capabilities for iOS
     * @return Decorated IOSDriver instance
     */
    private static IOSDriver getDecoratedIOSDriver(URL remoteAddress, Capabilities capabilities){
        return createProxy(
                IOSDriver.class,
                new Object[] {remoteAddress,capabilities},
                new Class[] {URL.class,Capabilities.class},
                new appiumListener()
        );
    }
    private static HeadlessMode checkMobileHeadless(Capabilities capabilities){
        Object isHeadless=capabilities.getCapability("appium:isHeadless");
        if (isHeadless!=null){
            boolean headless=isHeadless.toString().equalsIgnoreCase("true");
            if (headless) return HeadlessMode.True;
        }
        return HeadlessMode.False;
    }
}