package Ellithium.core.driver;

import Ellithium.Utilities.interactions.WaitManager;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.execution.listener.appiumListener;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.execution.listener.seleniumListener;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.core.logging.Logger;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.log.Log;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import java.net.URL;
import static Ellithium.core.driver.MobileDriverType.IOS;
import static Ellithium.core.reporting.internal.Colors.*;
import static io.appium.java_client.proxy.Helpers.createProxy;

public class DriverFactory {
    private static ThreadLocal<RemoteWebDriver> RemoteWebDriverThreadLocal= new ThreadLocal<>();;
    private static ThreadLocal<WebDriver> WebDriverThread = new ThreadLocal<>();
    private static ThreadLocal<AndroidDriver> AndroidDriverThread = new ThreadLocal<>();
    private static ThreadLocal<IOSDriver> IOSDriverThread = new ThreadLocal<>();

    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        ConfigContext.setConfig(driverType,headlessMode,pageLoadStrategyMode,privateMode,sandboxMode,webSecurityMode);
        webSetUp();
        return (T) WebDriverThread.get();
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode) {
        return getNewLocalDriver(driverType,headlessMode,privateMode,pageLoadStrategyMode,webSecurityMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewLocalDriver(driverType,headlessMode,privateMode,pageLoadStrategyMode,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode) {
        return getNewLocalDriver(driverType,headlessMode,privateMode,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewLocalDriver(LocalDriverType driverType,HeadlessMode headlessMode) {
        return getNewLocalDriver(driverType,headlessMode,PrivateMode.False,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewLocalDriver(LocalDriverType driverType) {
        return getNewLocalDriver(driverType,HeadlessMode.False,PrivateMode.False,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends AppiumDriver> T getNewMobileDriver(MobileDriverType driverType, URL remoteAddress, Capabilities capabilities) {
        ConfigContext.setDriverType(driverType);
        ConfigContext.setRemoteAddress(remoteAddress);
        ConfigContext.setCapabilities(capabilities);
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

    // Remote web Driver Section
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode, WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        ConfigContext.setConfig(driverType,headlessMode,pageLoadStrategyMode,privateMode,sandboxMode,webSecurityMode);
        ConfigContext.setCapabilities(capabilities);
        ConfigContext.setRemoteAddress(remoteAddress);
        webSetUp();
        return (T)RemoteWebDriverThreadLocal.get();
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode, WebSecurityMode webSecurityMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,pageLoadStrategyMode,webSecurityMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,pageLoadStrategyMode,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNgetNewRemoteDriverewDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,headlessMode,PrivateMode.False,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewRemoteDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities) {
        return getNewRemoteDriver(driverType,remoteAddress,capabilities,HeadlessMode.False,PrivateMode.False,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T getCurrentDriver() {
        var driverType=ConfigContext.getDriverType();
        if(driverType!=null){
            if (driverType.equals(MobileDriverType.Android)) {
                return (T) AndroidDriverThread.get();
            } else if (driverType.equals(IOS)) {
                return (T) IOSDriverThread.get();
            } else if (driverType.equals(LocalDriverType.Chrome) || ConfigContext.getDriverType().equals(LocalDriverType.Edge) || ConfigContext.getDriverType().equals(LocalDriverType.FireFox) || ConfigContext.getDriverType().equals(LocalDriverType.Safari)) {
                return (T) WebDriverThread.get();
            } else if (driverType.equals(RemoteDriverType.REMOTE_Edge) || ConfigContext.getDriverType().equals(RemoteDriverType.REMOTE_Safari) || ConfigContext.getDriverType().equals(RemoteDriverType.REMOTE_FireFox) || ConfigContext.getDriverType().equals(RemoteDriverType.REMOTE_Chrome)) {
                return (T) RemoteWebDriverThreadLocal.get();
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
            } else if (driverType.equals(LocalDriverType.Chrome) || ConfigContext.getDriverType().equals(LocalDriverType.Edge) || ConfigContext.getDriverType().equals(LocalDriverType.FireFox) || ConfigContext.getDriverType().equals(LocalDriverType.Safari)) {
                WebDriver localDriver = WebDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            } else if (driverType.equals(RemoteDriverType.REMOTE_Edge) || ConfigContext.getDriverType().equals(RemoteDriverType.REMOTE_Safari) || ConfigContext.getDriverType().equals(RemoteDriverType.REMOTE_FireFox) || ConfigContext.getDriverType().equals(RemoteDriverType.REMOTE_Chrome)) {
                RemoteWebDriver localDriver = RemoteWebDriverThreadLocal.get();
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
            } else if (driverType.equals(LocalDriverType.Chrome) || driverType.equals(LocalDriverType.Edge) || driverType.equals(LocalDriverType.FireFox) || driverType.equals(LocalDriverType.Safari)) {
                WebDriverThread.remove();
            } else if (driverType.equals(RemoteDriverType.REMOTE_Edge) || driverType.equals(RemoteDriverType.REMOTE_Safari) || driverType.equals(RemoteDriverType.REMOTE_FireFox) || driverType.equals(RemoteDriverType.REMOTE_Chrome)) {
                RemoteWebDriverThreadLocal.remove();
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
        if (driverType.equals(RemoteDriverType.REMOTE_Edge) || driverType.equals(RemoteDriverType.REMOTE_Safari) || driverType.equals(RemoteDriverType.REMOTE_FireFox) || driverType.equals(RemoteDriverType.REMOTE_Chrome)) {
            var capabilities = ConfigContext.getCapabilities();
            var remoteAddress = ConfigContext.getRemoteAddress();
            var localDriver = BrowserSetUp.setupRemoteDriver(driverType, remoteAddress, capabilities, headlessMode, PageLoadStrategy, PrivateMode, SandboxMode, WebSecurityMode);
            RemoteWebDriverThreadLocal.set(getDecoratedWebDriver(localDriver));
            if (RemoteWebDriverThreadLocal != null) {
                Reporter.log("Driver Created", LogLevel.INFO_GREEN);
            } else {
                Reporter.log("Driver Creation Failed", LogLevel.INFO_RED);
            }
        } else {
            var localDriver = BrowserSetUp.setupLocalDriver(driverType, headlessMode, PageLoadStrategy, PrivateMode, SandboxMode, WebSecurityMode);
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
    }
    @SuppressWarnings("unchecked")
    private static WebDriver getDecoratedWebDriver(WebDriver driver){
        return new EventFiringDecorator<>(org.openqa.selenium.WebDriver.class, new seleniumListener()).decorate(driver);
    }
    @SuppressWarnings("unchecked")
    private static RemoteWebDriver getDecoratedWebDriver(RemoteWebDriver driver){
        return new EventFiringDecorator<>(org.openqa.selenium.remote.RemoteWebDriver.class, new seleniumListener()).decorate(driver);
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
        devTools.createSession();
        devTools.send(Log.enable());
        devTools.addListener(Log.entryAdded(), logEntry -> {
            Logger.info(PURPLE+"Level: "+logEntry.getLevel()+RESET);
            Logger.info(PINK+"Text: "+logEntry.getText()+RESET);
            Logger.info((BROWN+"URL: "+logEntry.getUrl())+RESET);
            Logger.info((YELLOW+"StackTrace: "+logEntry.getStackTrace())+RESET);
        });
    }
}