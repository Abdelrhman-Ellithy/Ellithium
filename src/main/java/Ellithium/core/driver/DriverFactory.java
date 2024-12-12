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
import java.time.Duration;
import static Ellithium.core.reporting.internal.Colors.*;
import static io.appium.java_client.proxy.Helpers.createProxy;

public class DriverFactory {
    private static ThreadLocal<RemoteWebDriver> RemoteWebDriverThreadLocal= new ThreadLocal<>();;
    private static ThreadLocal<WebDriver> WebDriverThread = new ThreadLocal<>();
    private static ThreadLocal<AndroidDriver> AndroidDriverThread = new ThreadLocal<>();
    private static ThreadLocal<IOSDriver> IOSDriverThread = new ThreadLocal<>();

    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T getNewDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        ConfigContext.setConfig(driverType,headlessMode,pageLoadStrategyMode,privateMode,sandboxMode,webSecurityMode);
        webSetUp();
        return (T) WebDriverThread.get();
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode) {
        return getNewDriver(driverType,headlessMode,privateMode,pageLoadStrategyMode,webSecurityMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewDriver(driverType,headlessMode,privateMode,pageLoadStrategyMode,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(LocalDriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode) {
        return getNewDriver(driverType,headlessMode,privateMode,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(LocalDriverType driverType,HeadlessMode headlessMode) {
        return getNewDriver(driverType,headlessMode,PrivateMode.True,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(LocalDriverType driverType) {
        return getNewDriver(driverType,HeadlessMode.False,PrivateMode.True,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends AppiumDriver> T getNewDriver(MobileDriverType driverType, URL remoteAddress, Capabilities capabilities) {
        ConfigContext.setDriverType(driverType);
        ConfigContext.setRemoteAddress(remoteAddress);
        ConfigContext.setCapabilities(capabilities);
        switch (driverType){
            case MobileDriverType.IOS -> {
                IOSDriver localDriver=getDecoratedIOSDriver(remoteAddress, capabilities);
                localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(WaitManager.getDefaultImplicitWait()));
                IOSDriverThread.set(localDriver);
                if(IOSDriverThread!=null){
                    Reporter.log("Driver Created", LogLevel.INFO_GREEN);
                    return (T)IOSDriverThread.get();
                }
            }
            case MobileDriverType.Android -> {
                AndroidDriver localDriver=getDecoratedAndroidDriver(remoteAddress, capabilities);
                localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(WaitManager.getDefaultImplicitWait()));
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
    public static <T extends RemoteWebDriver> T getNewDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode, WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        ConfigContext.setConfig(driverType,headlessMode,pageLoadStrategyMode,privateMode,sandboxMode,webSecurityMode);
        ConfigContext.setCapabilities(capabilities);
        ConfigContext.setRemoteAddress(remoteAddress);
        webSetUp();
        return (T)RemoteWebDriverThreadLocal.get();
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode, WebSecurityMode webSecurityMode) {
        return getNewDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,pageLoadStrategyMode,webSecurityMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,pageLoadStrategyMode,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PrivateMode privateMode) {
        return getNewDriver(driverType,remoteAddress,capabilities,headlessMode,privateMode,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode) {
        return getNewDriver(driverType,remoteAddress,capabilities,headlessMode,PrivateMode.True,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends RemoteWebDriver> T getNewDriver(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities) {
        return getNewDriver(driverType,remoteAddress,capabilities,HeadlessMode.False,PrivateMode.True,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T getCurrentDriver() {
        switch (ConfigContext.getDriverType()){
            case MobileDriverType.Android -> {
                return (T)AndroidDriverThread.get();
            }
            case MobileDriverType.IOS -> {
                return (T)IOSDriverThread.get();
            }
            case LocalDriverType.Chrome, LocalDriverType.Edge, LocalDriverType.FireFox, LocalDriverType.Safari  ->{
                return (T)WebDriverThread.get();
            }
            case RemoteDriverType.REMOTE_Edge, RemoteDriverType.REMOTE_Safari, RemoteDriverType.REMOTE_FireFox, RemoteDriverType.REMOTE_Chrome ->{
                return (T)RemoteWebDriverThreadLocal.get();
            }
            default -> {
                return null;
            }
        }
    }
    public static void quitDriver() {
        switch (ConfigContext.getDriverType()){
            case MobileDriverType.Android -> {
                AndroidDriver localDriver = AndroidDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            }
            case MobileDriverType.IOS -> {
                IOSDriver localDriver = IOSDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            }
            case LocalDriverType.Chrome, LocalDriverType.Edge, LocalDriverType.FireFox, LocalDriverType.Safari ->{
                WebDriver localDriver = WebDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            }
            case RemoteDriverType.REMOTE_Edge, RemoteDriverType.REMOTE_Safari, RemoteDriverType.REMOTE_FireFox, RemoteDriverType.REMOTE_Chrome ->{
                RemoteWebDriver localDriver = RemoteWebDriverThreadLocal.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            }
            default ->{

            }
        }
    }
    public static void removeDriver() {
        switch (ConfigContext.getDriverType()){
            case MobileDriverType.Android -> {
                AndroidDriverThread.remove();
            }
            case MobileDriverType.IOS -> {
                IOSDriverThread.remove();
            }
            case LocalDriverType.Chrome, LocalDriverType.Edge, LocalDriverType.FireFox, LocalDriverType.Safari ->{
                WebDriverThread.remove();
            }
            case  RemoteDriverType.REMOTE_Edge, RemoteDriverType.REMOTE_Safari, RemoteDriverType.REMOTE_FireFox, RemoteDriverType.REMOTE_Chrome ->{
                RemoteWebDriverThreadLocal.remove();
            }
            default ->{

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
        switch (driverType){
            case RemoteDriverType.REMOTE_Edge, RemoteDriverType.REMOTE_Safari,
                 RemoteDriverType.REMOTE_FireFox, RemoteDriverType.REMOTE_Chrome  ->{
                var capabilities=ConfigContext.getCapabilities();
                var remoteAddress=ConfigContext.getRemoteAddress();
                var localDriver=BrowserSetUp.setupRemoteDriver(driverType,remoteAddress,capabilities, headlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
                localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(WaitManager.getDefaultImplicitWait()));
                RemoteWebDriverThreadLocal.set(getDecoratedWebDriver(localDriver));
                if(RemoteWebDriverThreadLocal!=null){
                    Reporter.log("Driver Created", LogLevel.INFO_GREEN);
                }
                else {
                    Reporter.log("Driver Creation Failed",LogLevel.INFO_RED);
                }
            }
            default-> {
               var localDriver= BrowserSetUp.setupLocalDriver(driverType, headlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
                String loggerExtensiveTraceModeFlag=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "loggerExtensiveTraceMode");
                if (loggerExtensiveTraceModeFlag.equalsIgnoreCase("true")){
                    DevTools devTools;
                    switch (driverType){
                        case LocalDriverType.Edge->{
                            devTools=((EdgeDriver)localDriver).getDevTools();
                            logDevTools(devTools);
                        }
                        case LocalDriverType.Chrome->{
                            devTools=((ChromeDriver)localDriver).getDevTools();
                            logDevTools(devTools);
                        }
                        default ->{}
                    }
                }
                localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(WaitManager.getDefaultImplicitWait()));
                WebDriverThread.set(getDecoratedWebDriver(localDriver));
                if(WebDriverThread!=null){
                    Reporter.log("Driver Created", LogLevel.INFO_GREEN);
                }
                else {
                    Reporter.log("Driver Creation Failed",LogLevel.INFO_RED);
                }
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
        return      createProxy(
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
