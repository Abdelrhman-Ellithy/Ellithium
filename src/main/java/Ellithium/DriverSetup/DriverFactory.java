package Ellithium.DriverSetup;
import Ellithium.Internal.ConfigContext;
import Ellithium.Internal.LogLevel;
import Ellithium.Internal.Reporter;
import Ellithium.Internal.SeleniumListener;
import Ellithium.Utilities.PropertyHelper;
import Ellithium.Utilities.logsUtils;
import Ellithium.Utilities.Colors;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.log.Log;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;

import java.io.File;
import java.time.Duration;
import static Ellithium.Utilities.Colors.*;

public class DriverFactory {
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final String configPath="src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config";
    private static boolean defaultTimeoutGotFlag=false;
    private static int defaultTimeout= 5;

    public static WebDriver getNewDriver() {
        setUp();
        return driver.get();  // Get WebDriver for the current thread
    }
    public static WebDriver getCurrentDriver() {
        return driver.get();  // Get WebDriver for the current thread
    }
    private static void setUp() {
        String browserName = ConfigContext.getBrowserName();  // Default to Chrome if not set
        String headlessMode = ConfigContext.getHeadlessMode(); // Default to false if not set
        String PageLoadStrategy=ConfigContext.getPageLoadStrategy(); // Default to normal if not set
        String PrivateMode=ConfigContext.getPrivateMode();
        String SandboxMode=ConfigContext.getSandboxMode();
        String WebSecurityMode=ConfigContext.getWebSecurityMode();
        WebDriver localDriver = DriverSetUp.setupLocalDriver(browserName, headlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
        String loggerExtensiveTraceModeFlag=PropertyHelper.getDataFromProperties(configPath, "loggerExtensiveTraceMode");
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
                default:
                    break;
            }
        }
        if(!defaultTimeoutGotFlag){
            initTimeout();
        }
        localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(defaultTimeout));
        driver.set(getDecoratedDriver(localDriver));  // Set WebDriver for this thread
        if(driver!=null){
            Reporter.log("WebDriver Created", LogLevel.INFO_GREEN);
        }
        else {
            Reporter.log("WebDriver Creation Failed",LogLevel.INFO_RED);
        }
    }
    private static WebDriver getDecoratedDriver(WebDriver driver){
        return new EventFiringDecorator<>(org.openqa.selenium.WebDriver.class, new SeleniumListener()).decorate(driver);
    }
    private static void initTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(configPath, "defaultDriverWaitTimeout");
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
