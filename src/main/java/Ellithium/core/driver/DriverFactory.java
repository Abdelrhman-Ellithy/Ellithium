package Ellithium.core.driver;

import Ellithium.config.managment.ConfigContext;
import Ellithium.core.execution.listener.appiumListener;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.execution.listener.seleniumListener;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.core.logging.logsUtils;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import net.bytebuddy.implementation.bytecode.Throw;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.log.Log;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;

import java.net.URL;
import java.time.Duration;
import static Ellithium.core.reporting.internal.Colors.*;
import static io.appium.java_client.proxy.Helpers.createProxy;

public class DriverFactory {
    private static ThreadLocal<WebDriver> WebDriverThread = new ThreadLocal<>();
    private static ThreadLocal<AndroidDriver> AndroidDriverThread = new ThreadLocal<>();
    private static ThreadLocal<IOSDriver> IOSDriverThread = new ThreadLocal<>();
    private static boolean defaultTimeoutGotFlag=false;
    private static int defaultTimeout= 5;

    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T getNewDriver(DriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        ConfigContext.setConfig(driverType,headlessMode,pageLoadStrategyMode,privateMode,sandboxMode,webSecurityMode);
        webSetUp();
        return (T) WebDriverThread.get();
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(DriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,WebSecurityMode webSecurityMode) {
        return getNewDriver(driverType,headlessMode,privateMode,pageLoadStrategyMode,webSecurityMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(DriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode) {
        return getNewDriver(driverType,headlessMode,privateMode,pageLoadStrategyMode,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(DriverType driverType,HeadlessMode headlessMode, PrivateMode privateMode) {
        return getNewDriver(driverType,headlessMode,privateMode,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(DriverType driverType,HeadlessMode headlessMode) {
        return getNewDriver(driverType,headlessMode,PrivateMode.True,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends WebDriver> T  getNewDriver(DriverType driverType) {
        return getNewDriver(driverType,HeadlessMode.False,PrivateMode.True,PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
    }
    @SuppressWarnings("unchecked")
    public static <T extends AppiumDriver> T getNewDriver(DriverType driverType, URL remoteAddress, Capabilities capabilities) {
        if(!defaultTimeoutGotFlag){
            initTimeout();
        }
        switch (driverType){
            case IOS -> {
                ConfigContext.setDriverType(DriverType.IOS);
                IOSDriver localDriver=getDecoratedIOSDriver(remoteAddress, capabilities);
                localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(defaultTimeout));
                IOSDriverThread.set(localDriver);
                if(IOSDriverThread!=null){
                    Reporter.log("Driver Created", LogLevel.INFO_GREEN);
                    return (T)IOSDriverThread.get();
                }
            }
            case Android -> {
                ConfigContext.setDriverType(DriverType.Android);
                AndroidDriver localDriver=getDecoratedAndroidDriver(remoteAddress, capabilities);
                localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(defaultTimeout));
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
    public static WebDriver getCurrentDriver() {
        switch (ConfigContext.getDriverType()){
            case Android -> {
                return AndroidDriverThread.get();
            }
            case IOS -> {
                return IOSDriverThread.get();
            }
            case Chrome, Edge,FireFox,Safari ->{
                return WebDriverThread.get();
            }
            default -> {
                return null;
            }
        }
    }
    public static void quitDriver() {
        switch (ConfigContext.getDriverType()){
            case Android -> {
                AndroidDriver localDriver = AndroidDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            }
            case IOS -> {
                IOSDriver localDriver = IOSDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            }
            case Chrome, Edge,FireFox,Safari ->{
                WebDriver localDriver = WebDriverThread.get();
                if (localDriver != null) {
                    localDriver.quit();
                }
                removeDriver();
            }
        }
    }
    public static void removeDriver() {
        switch (ConfigContext.getDriverType()){
            case Android -> {
                AndroidDriverThread.remove();
            }
            case IOS -> {
                IOSDriverThread.remove();
            }
            case Chrome, Edge,FireFox,Safari ->{
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
        WebDriver localDriver = BrowserSetUp.setupLocalDriver(driverType, headlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
        String loggerExtensiveTraceModeFlag=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "loggerExtensiveTraceMode");
        if (loggerExtensiveTraceModeFlag.equalsIgnoreCase("true")){
            DevTools devTools;
            switch (driverType){
                case Edge->{
                    devTools=((EdgeDriver)localDriver).getDevTools();
                    logDevTools(devTools);
                }
                case Chrome->{
                    devTools=((ChromeDriver)localDriver).getDevTools();
                    logDevTools(devTools);
                }
            }
        }
        if(!defaultTimeoutGotFlag){
            initTimeout();
        }
        localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(defaultTimeout));
        WebDriverThread.set(getDecoratedWebDriver(localDriver));
        if(WebDriverThread!=null){
            Reporter.log("Driver Created", LogLevel.INFO_GREEN);
        }
        else {
            Reporter.log("Driver Creation Failed",LogLevel.INFO_RED);
        }
    }
    private static WebDriver getDecoratedWebDriver(WebDriver driver){
        return new EventFiringDecorator<>(org.openqa.selenium.WebDriver.class, new seleniumListener()).decorate(driver);
    }

    private static AndroidDriver getDecoratedAndroidDriver(URL remoteAddress, Capabilities capabilities){
                            AndroidDriver decoratedDriver = createProxy(
                            AndroidDriver.class,
                            new Object[] {remoteAddress,capabilities},
                            new Class[] {URL.class,Capabilities.class},
                            new appiumListener()
                    );
         return decoratedDriver;
    }
    private static IOSDriver getDecoratedIOSDriver(URL remoteAddress, Capabilities capabilities){
        IOSDriver decoratedDriver = createProxy(
                IOSDriver.class,
                new Object[] {remoteAddress,capabilities},
                new Class[] {URL.class,Capabilities.class},
                new appiumListener()
        );
        return decoratedDriver;
    }
    private static void initTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultDriverWaitTimeout");
            defaultTimeout = Integer.parseInt(timeout);
        } catch (Exception e) {
            logsUtils.logException(e);
        }
    }
    private static void logDevTools(DevTools devTools){
        devTools.createSession();
        devTools.send(Log.enable());
        devTools.addListener(Log.entryAdded(), logEntry -> {
            logsUtils.info(PURPLE+"Level: "+logEntry.getLevel()+RESET);
            logsUtils.info(PINK+"Text: "+logEntry.getText()+RESET);
            logsUtils.info((BROWN+"URL: "+logEntry.getUrl())+RESET);
            logsUtils.info((YELLOW+"StackTrace: "+logEntry.getStackTrace())+RESET);
        });
    }
}
