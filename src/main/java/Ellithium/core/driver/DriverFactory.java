package Ellithium.core.driver;

import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.execution.listener.DriverListener;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.core.logging.logsUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.log.Log;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import java.time.Duration;
import static Ellithium.core.reporting.internal.Colors.*;

public class DriverFactory {
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static boolean defaultTimeoutGotFlag=false;
    private static int defaultTimeout= 5;
    public static WebDriver getNewDriver() {
        setUp();
        return driver.get();
    }
    public static WebDriver getCurrentDriver() {
        return driver.get();
    }
    private static void setUp() {
        String browserName = ConfigContext.getBrowserName();
        String headlessMode = ConfigContext.getHeadlessMode();
        String PageLoadStrategy=ConfigContext.getPageLoadStrategy();
        String PrivateMode=ConfigContext.getPrivateMode();
        String SandboxMode=ConfigContext.getSandboxMode();
        String WebSecurityMode=ConfigContext.getWebSecurityMode();
        WebDriver localDriver = DriverSetUp.setupLocalDriver(browserName, headlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
        String loggerExtensiveTraceModeFlag=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "loggerExtensiveTraceMode");
        if (loggerExtensiveTraceModeFlag.equalsIgnoreCase("true")){
            DevTools devTools;
            switch (browserName.toLowerCase()){
                case "edge" :
                    devTools=((EdgeDriver)localDriver).getDevTools();
                    logDevTools(devTools);
                    break;
                case "chrome":
                    devTools=((ChromeDriver)localDriver).getDevTools();
                    logDevTools(devTools);
                    break;
                default: break;
            }
        }
        if(!defaultTimeoutGotFlag){
            initTimeout();
        }
        localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(defaultTimeout));
        driver.set(getDecoratedDriver(localDriver));
        if(driver!=null){
            Reporter.log("WebDriver Created", LogLevel.INFO_GREEN);
        }
        else {
            Reporter.log("WebDriver Creation Failed",LogLevel.INFO_RED);
        }
    }
    private static WebDriver getDecoratedDriver(WebDriver driver){
        return new EventFiringDecorator<>(org.openqa.selenium.WebDriver.class, new DriverListener()).decorate(driver);
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
    public static void quitDriver() {
        WebDriver localDriver = driver.get();
        if (localDriver != null) {
            localDriver.quit();
        }
        removeDriver();
    }
    public static void removeDriver() {
        driver.remove();
    }
}
