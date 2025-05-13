package Ellithium.core.driver;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.execution.listener.appiumListener;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.execution.listener.seleniumListener;
import Ellithium.Utilities.helpers.PropertyHelper;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v136.log.Log;
import org.openqa.selenium.devtools.v136.network.Network;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import java.net.URL;
import java.util.Optional;
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
 *   <li>MobileDriverConfig - For mobile device automation</li>
 * </ul>
 */
public class DriverFactory {
    private static ThreadLocal<WebDriver> WebDriverThread = new ThreadLocal<>();
    private static ThreadLocal<AndroidDriver> AndroidDriverThread = new ThreadLocal<>();
    private static ThreadLocal<IOSDriver> IOSDriverThread = new ThreadLocal<>();

    /**
     * Creates a new WebDriver instance using local driver configuration.
     *
     * @param localDriverConfig Configuration for local browser instance
     * @param <T> Type of WebDriver to be returned
     * @return Configured WebDriver instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNewDriver(LocalDriverConfig localDriverConfig) {
        ConfigContext.setCapabilities(localDriverConfig.getCapabilities());
        ConfigContext.setConfig(
                localDriverConfig.getLocalDriverType(),localDriverConfig.getHeadlessMode()
                ,localDriverConfig.getPageLoadStrategy(),localDriverConfig.getPrivateMode(),
                localDriverConfig.getSandboxMode(),localDriverConfig.getWebSecurityMode()
        );
        webSetUp();
        return (T) WebDriverThread.get();
    }

    /**
     * Creates a new WebDriver instance using remote driver configuration.
     *
     * @param remoteDriverConfig Configuration for remote browser instance
     * @param <T> Type of WebDriver to be returned
     * @return Configured WebDriver instance
     */
    public static <T> T getNewDriver(RemoteDriverConfig remoteDriverConfig) {
        ConfigContext.setConfig(
                remoteDriverConfig.getRemoteDriverType(),remoteDriverConfig.getHeadlessMode(),
                remoteDriverConfig.getPageLoadStrategy(),remoteDriverConfig.getPrivateMode(),
                remoteDriverConfig.getSandboxMode(),remoteDriverConfig.getWebSecurityMode());
        ConfigContext.setCapabilities(remoteDriverConfig.getCapabilities());
        ConfigContext.setRemoteAddress(remoteDriverConfig.getRemoteAddress());
        webSetUp();
        return (T)WebDriverThread.get();
    }

    /**
     * Creates a new mobile driver instance using mobile driver configuration.
     *
     * @param mobileDriverConfig Configuration for mobile device
     * @param <T> Type of mobile driver to be returned (AndroidDriver or IOSDriver)
     * @return Configured mobile driver instance
     */
    public static <T> T getNewDriver(MobileDriverConfig mobileDriverConfig) {
        ConfigContext.setDriverType(mobileDriverConfig.getDriverType());
        ConfigContext.setRemoteAddress(mobileDriverConfig.getRemoteAddress());
        ConfigContext.setCapabilities(mobileDriverConfig.getCapabilities());
        return mobileSetup((MobileDriverType) mobileDriverConfig.getDriverType(),mobileDriverConfig.getRemoteAddress(),mobileDriverConfig.getCapabilities());
    }

    /**
     * Factory method that creates appropriate driver based on configuration type.
     *
     * @param driverConfigBuilder Configuration builder instance
     * @param <T> Type of driver to be returned
     * @return Configured driver instance
     */
    public static <T> T getNewDriver(DriverConfigBuilder driverConfigBuilder) {
        if(driverConfigBuilder instanceof LocalDriverConfig localDriverConfig){
            return getNewDriver(localDriverConfig);
        } else if (driverConfigBuilder instanceof RemoteDriverConfig remoteDriverConfig) {
            return getNewDriver(remoteDriverConfig);
        }
        else {
            return getNewDriver((MobileDriverConfig) driverConfigBuilder);
        }
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
    @SuppressWarnings("unchecked")
    public static <T> T getNewLocalDriver(LocalDriverType driverType, HeadlessMode headlessMode, 
            PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,
            WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        ConfigContext.setConfig(driverType,headlessMode,pageLoadStrategyMode,privateMode,sandboxMode,webSecurityMode);
        webSetUp();
        return (T) WebDriverThread.get();
    }

    @SuppressWarnings("unchecked")
    public static <T > T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode) {
        return getNewLocalDriver(driverType,headlessMode,privateMode,pageLoadStrategyMode,webSecurityMode,SandboxMode.Sandbox);
    }

    @SuppressWarnings("unchecked")
    public static <T > T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewLocalDriver(driverType,headlessMode,privateMode,pageLoadStrategyMode,WebSecurityMode.SecureMode);
    }

    @SuppressWarnings("unchecked")
    public static <T > T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode) {
        return getNewLocalDriver(driverType,headlessMode,privateMode,PageLoadStrategyMode.Normal);
    }

    @SuppressWarnings("unchecked")
    public static <T > T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode) {
        return getNewLocalDriver(driverType,headlessMode,PrivateMode.False);
    }

    @SuppressWarnings("unchecked")
    public static <T> T  getNewLocalDriver(LocalDriverType driverType) {
        return getNewLocalDriver(driverType,HeadlessMode.False);
    }

    @SuppressWarnings("unchecked")
    public static <T > T getNewMobileDriver(MobileDriverType driverType, URL remoteAddress, Capabilities capabilities) {
        ConfigContext.setDriverType(driverType);
        ConfigContext.setRemoteAddress(remoteAddress);
        ConfigContext.setCapabilities(capabilities);
        return mobileSetup(driverType,remoteAddress,capabilities);
    }

    // Remote web Driver Section
    @SuppressWarnings("unchecked")
    public static <T > T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        ConfigContext.setConfig(driverType,headlessMode,pageLoadStrategyMode,privateMode,sandboxMode,webSecurityMode);
        ConfigContext.setCapabilities(capabilities);
        ConfigContext.setRemoteAddress(remoteAddress);
        webSetUp();
        return (T)WebDriverThread.get();
    }

    @SuppressWarnings("unchecked")
    public static <T > T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode, WebSecurityMode webSecurityMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,pageLoadStrategyMode,webSecurityMode,SandboxMode.Sandbox);
    }

    @SuppressWarnings("unchecked")
    public static <T > T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,pageLoadStrategyMode,WebSecurityMode.SecureMode);
    }

    @SuppressWarnings("unchecked")
    public static <T > T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode, PageLoadStrategyMode.Normal);
    }

    @SuppressWarnings("unchecked")
    public static <T > T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,PrivateMode.False);
    }

    @SuppressWarnings("unchecked")
    public static <T > T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,HeadlessMode.False);
    }

    /**
     * Gets the current driver instance for the executing thread.
     *
     * @param <T> Type of driver to be returned
     * @return Current driver instance or null if no driver exists
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCurrentDriver() {
        var driverType=ConfigContext.getDriverType();
        if(driverType!=null){
            if (driverType.equals(MobileDriverType.Android)) {
                return (T) AndroidDriverThread.get();
            } else if (driverType.equals(IOS)) {
                return (T) IOSDriverThread.get();
            } else if (driverType instanceof LocalDriverType || driverType instanceof RemoteDriverType ) {
                return (T) WebDriverThread.get();
            }
        }
        return null;
    }

    /**
     * Quits the current driver instance and closes all associated windows.
     */
    public static void quitDriver() {
        var driverType=ConfigContext.getDriverType();
        if (driverType!=null){
            if (driverType.equals(MobileDriverType.Android)) {
                AndroidDriver localDriver = AndroidDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            } else if (driverType.equals(IOS)) {
                IOSDriver localDriver = IOSDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            } else if (driverType instanceof LocalDriverType || driverType instanceof RemoteDriverType ) {
                var localDriver = WebDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            }
        }
    }

    /**
     * Removes the current driver instance from thread local storage.
     */
    public static void removeDriver() {
        var driverType=ConfigContext.getDriverType();
        if(driverType!=null) {
            if (driverType.equals(MobileDriverType.Android)) {
                AndroidDriverThread.remove();
            } else if (driverType.equals(IOS)) {
                IOSDriverThread.remove();
            } else if (driverType instanceof LocalDriverType || driverType instanceof RemoteDriverType ) {
                WebDriverThread.remove();
            }
        }
    }

    /**
     * Sets up a web driver instance with specified configuration.
     * Handles both local and remote web driver setup.
     * 
     * @throws IllegalStateException if driver creation fails
     */
    private static void webSetUp() {
        var driverType = ConfigContext.getDriverType();
        var headlessMode = ConfigContext.getHeadlessMode();
        var PageLoadStrategy=ConfigContext.getPageLoadStrategy();
        var PrivateMode=ConfigContext.getPrivateMode();
        var SandboxMode=ConfigContext.getSandboxMode();
        var WebSecurityMode=ConfigContext.getWebSecurityMode();
        WebDriver localDriver;
        if (driverType instanceof RemoteDriverType) {
            var capabilities = ConfigContext.getCapabilities();
            var remoteAddress = ConfigContext.getRemoteAddress();
            localDriver = BrowserSetUp.setupRemoteDriver(driverType, remoteAddress, capabilities, headlessMode, PageLoadStrategy, PrivateMode, SandboxMode, WebSecurityMode);
        } else {
            localDriver = BrowserSetUp.setupLocalDriver(driverType, headlessMode, PageLoadStrategy, PrivateMode, SandboxMode, WebSecurityMode);
        }
        String loggerExtensiveTraceModeFlag = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "loggerExtensiveTraceMode");
        if (loggerExtensiveTraceModeFlag.equalsIgnoreCase("true")) {
            DevTools devTools;
            if (driverType.equals(LocalDriverType.Edge)) {
                devTools = ((EdgeDriver) localDriver).getDevTools();
                logDevTools(devTools);
            } else if (driverType.equals(LocalDriverType.Chrome)) {
                devTools = ((ChromeDriver) localDriver).getDevTools();
                logDevTools(devTools);
            }
        }
        WebDriverThread.set(getDecoratedWebDriver(localDriver));
        if (WebDriverThread != null) {
            Reporter.log("Driver Created", LogLevel.INFO_GREEN);
        } else {
            Reporter.log("Driver Creation Failed", LogLevel.INFO_RED);
        }
    }

    /**
     * Sets up a mobile driver instance with specified configuration.
     *
     * @param driverType Type of mobile driver (Android or iOS)
     * @param remoteAddress URL of the Appium server
     * @param capabilities Desired capabilities for the mobile driver
     * @param <T> Type of mobile driver to be returned
     * @return Configured mobile driver instance
     * @throws IllegalArgumentException if invalid driver type is specified
     */
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
            default -> {
                throw new IllegalArgumentException("Wrong Driver Initialization: " + ConfigContext.getDriverType()+ "visit: https://github.com/Abdelrhman-Ellithy/Ellithium to know how the correct way");
            }
        }
        Reporter.log("Driver Creation Failed",LogLevel.INFO_RED);
        return null;
    }

    /**
     * Creates a decorated WebDriver instance with event listening capabilities.
     *
     * @param driver Base WebDriver instance to be decorated
     * @return Decorated WebDriver instance
     */
    @SuppressWarnings("unchecked")
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

    /**
     * Configures DevTools logging for Chrome and Edge browsers.
     *
     * @param devTools DevTools instance to be configured
     */
    private static void logDevTools(DevTools devTools){
//                devTools.createSession();
//                devTools.send(Log.enable());
//                devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
//                devTools.addListener(Network.requestWillBeSent(), request -> {
//                    String type = request.getType().toString().toLowerCase();
//                    if (type.contains("xhr") || type.contains("fetch")) {
//                            networkRequests(request);
//                    }
//                });
//                devTools.addListener(Network.responseReceived(), response -> {
//                    String type = response.getType().toString().toLowerCase();
//                    if (type.contains("xhr") || type.contains("fetch"))  {
//                            networkResponses(response);
//                    }
//                });
    }

//    /**
//     * Logs network request information to Allure report.
//     *
//     * @param request Network request information
//     */
//    @Step("Captured Network Request")
//    private static void networkRequests(org.openqa.selenium.devtools.v134.network.model.RequestWillBeSent request) {
//        Allure.step("Captured Network Requests Sent", () -> {
//            Reporter.logReportOnly("Request URL: " + request.getRequest().getUrl(), LogLevel.INFO_GREEN);
//            Reporter.logReportOnly("Request Method: " + request.getRequest().getMethod(), LogLevel.INFO_GREEN);
//            Reporter.logReportOnly("Request Headers: " + request.getRequest().getHeaders(), LogLevel.INFO_GREEN);
//        });
//    }
//
//    /**
//     * Logs network response information to Allure report.
//     *
//     * @param response Network response information
//     */
//    @Step("Captured Network Response")
//    private static void networkResponses(org.openqa.selenium.devtools.v134.network.model.ResponseReceived response) {
//        Allure.step("Captured Network Responses Received", () -> {
//            int status = response.getResponse().getStatus();
//            Reporter.logReportOnly("Response Time: " + response.getResponse().getResponseTime(), LogLevel.INFO_GREEN);
//            Reporter.logReportOnly("Status Code: " + status, LogLevel.INFO_GREEN);
//        });
//    }
}