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
import io.qameta.allure.model.StepResult;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import java.net.URL;
import java.util.UUID;
import static Ellithium.core.driver.MobileDriverType.IOS;
import static io.appium.java_client.proxy.Helpers.createProxy;
public class DriverFactory {
    private static ThreadLocal<WebDriver> WebDriverThread = new ThreadLocal<>();
    private static ThreadLocal<AndroidDriver> AndroidDriverThread = new ThreadLocal<>();
    private static ThreadLocal<IOSDriver> IOSDriverThread = new ThreadLocal<>();
    @SuppressWarnings("unchecked")
    public static <T > T getNewDriver(LocalDriverConfig localDriverConfig){
        ConfigContext.setCapabilities(localDriverConfig.getCapabilities());
        ConfigContext.setConfig(
                localDriverConfig.getLocalDriverType(),localDriverConfig.getHeadlessMode()
                ,localDriverConfig.getPageLoadStrategy(),localDriverConfig.getPrivateMode(),
                localDriverConfig.getSandboxMode(),localDriverConfig.getWebSecurityMode()
        );
        webSetUp();
        return (T) WebDriverThread.get();
    }
    public static <T > T getNewDriver(RemoteDriverConfig remoteDriverConfig) {
        ConfigContext.setConfig(
                remoteDriverConfig.getRemoteDriverType(),remoteDriverConfig.getHeadlessMode(),
                remoteDriverConfig.getPageLoadStrategy(),remoteDriverConfig.getPrivateMode(),
                remoteDriverConfig.getSandboxMode(),remoteDriverConfig.getWebSecurityMode());
        ConfigContext.setCapabilities(remoteDriverConfig.getCapabilities());
        ConfigContext.setRemoteAddress(remoteDriverConfig.getRemoteAddress());
        webSetUp();
        return (T)WebDriverThread.get();
    }
    public static <T > T getNewDriver(MobileDriverConfig mobileDriverConfig) {
        ConfigContext.setDriverType(mobileDriverConfig.getDriverType());
        ConfigContext.setRemoteAddress(mobileDriverConfig.getRemoteAddress());
        ConfigContext.setCapabilities(mobileDriverConfig.getCapabilities());
        return mobileSetup((MobileDriverType) mobileDriverConfig.getDriverType(),mobileDriverConfig.getRemoteAddress(),mobileDriverConfig.getCapabilities());
    }
    public static <T > T getNewDriver(DriverConfigBuilder driverConfigBuilder) {
        if(driverConfigBuilder instanceof LocalDriverConfig localDriverConfig){
            return getNewDriver(localDriverConfig);
        } else if (driverConfigBuilder instanceof RemoteDriverConfig remoteDriverConfig) {
            return getNewDriver(remoteDriverConfig);
        }
        else {
            return getNewDriver((MobileDriverConfig) driverConfigBuilder);
        }
    }
    @SuppressWarnings("unchecked")
    public static <T > T getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
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
    private static  <T > T mobileSetup(MobileDriverType driverType, URL remoteAddress, Capabilities capabilities){
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

    @SuppressWarnings("unchecked")
    private static WebDriver getDecoratedWebDriver(WebDriver driver){
        return new EventFiringDecorator<>(org.openqa.selenium.WebDriver.class, new seleniumListener()).decorate(driver);
    }
    private static AndroidDriver getDecoratedAndroidDriver(URL remoteAddress, Capabilities capabilities){
        return createProxy(
                AndroidDriver.class,
                new Object[] {remoteAddress,capabilities},
                new Class[] {URL.class,Capabilities.class},
                new appiumListener()
        );
    }
    private static IOSDriver getDecoratedIOSDriver(URL remoteAddress, Capabilities capabilities){
        return createProxy(
                IOSDriver.class,
                new Object[] {remoteAddress,capabilities},
                new Class[] {URL.class,Capabilities.class},
                new appiumListener()
        );
    }
    private static void logDevTools(DevTools devTools){
//        devTools.createSession();
//        devTools.send(Log.enable());
//        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
//        devTools.addListener(Network.requestWillBeSent(), request -> {
//            String type = request.getType().toString().toLowerCase();
//            if (type.contains("xhr") || type.contains("fetch")) {
//                    networkRequests(request);
//            }
//        });
//        devTools.addListener(Network.responseReceived(), response -> {
//            String type = response.getType().toString().toLowerCase();
//            if (type.contains("xhr") || type.contains("fetch"))  {
//                    networkResponses(response);
//            }
//        });
    }
    private static void networkRequests(org.openqa.selenium.devtools.v134.network.model.RequestWillBeSent request) {
        String stepUuid = UUID.randomUUID().toString();
        Allure.getLifecycle().startStep(stepUuid, new StepResult().setName("Captured Network Requests Sent"));
        try {
            Reporter.logReportOnly("Request URL: " + request.getRequest().getUrl(), LogLevel.INFO_GREEN);
            Reporter.logReportOnly("Request Method: " + request.getRequest().getMethod(), LogLevel.INFO_GREEN);
            Reporter.logReportOnly("Request Headers: " + request.getRequest().getHeaders(), LogLevel.INFO_GREEN);
        } finally {
            Allure.getLifecycle().stopStep(stepUuid);
        }
    }

    private static void networkResponses(org.openqa.selenium.devtools.v134.network.model.ResponseReceived response) {
        String stepUuid = UUID.randomUUID().toString();
        Allure.getLifecycle().startStep(stepUuid, new StepResult().setName("Captured Network Responses Received"));
        try {
            int status = response.getResponse().getStatus();
            Reporter.logReportOnly("Response Time: " + response.getResponse().getResponseTime(), LogLevel.INFO_GREEN);
            Reporter.logReportOnly("Status Code: " + status, LogLevel.INFO_GREEN);
        } finally {
            Allure.getLifecycle().stopStep(stepUuid);
        }
    }

}