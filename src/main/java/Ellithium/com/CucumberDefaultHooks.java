package Ellithium.com;
import static Ellithium.Utilities.Colors.*;
import Ellithium.DriverSetup.DriverSetUp;
import Ellithium.Utilities.Colors;
import Ellithium.Utilities.PropertyHelper;
import Ellithium.Utilities.TestDataGenerator;
import Ellithium.Utilities.logsUtils;
import io.cucumber.java.*;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.apache.xmlbeans.impl.xb.xsdschema.All;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import com.google.common.io.Files;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.log.Log;
import org.openqa.selenium.edge.EdgeDriver;

public class CucumberDefaultHooks {
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final String configPath="src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config";
    private String browserName;
    private static boolean defaultTimeoutGotFlag=false;
    private static int defaultTimeout= 5;

    @Before
    public void setUp(Scenario scenario) {
        browserName = System.getProperty("BrowserName", "Chrome").toLowerCase();  // Default to Chrome if not set
        String headlessMode = System.getProperty("HeadlessMode", "false").toLowerCase(); // Default to false if not set
        String PageLoadStrategy=System.getProperty("PageLoadStrategy", "normal").toLowerCase(); // Default to normal if not set
        String PrivateMode=System.getProperty("PrivateMode", "true").toLowerCase();
        String SandboxMode=System.getProperty("SandboxMode","Sandbox").toLowerCase();
        String WebSecurityMode=System.getProperty("WebSecurityMode","True").toLowerCase();
        Allure.getLifecycle().updateTestCase(testResult -> testResult.setName(scenario.getName()));
        logsUtils.info(CYAN + "[START] " + browserName.toUpperCase() + BLUE + " Scenario " + scenario.getName() + " [START]\n" + RESET);
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
        driver.set(localDriver);  // Set WebDriver for this thread
    }
    @After
    public void tearDown(Scenario scenario) {
        WebDriver localDriver = driver.get();
        if (localDriver != null) {
            switch (scenario.getStatus()) {
                case FAILED:
                    try {
                        logsUtils.info(RED + ' ' + browserName.toUpperCase() + "[FAILED] Scenario " + scenario.getName() + " [FAILED]" + RESET);
                        TakesScreenshot camera = (TakesScreenshot) localDriver;
                        File screenshot = camera.getScreenshotAs(OutputType.FILE);
                        File screenShotFile = new File("Test-Output/ScreenShots/Failed/" + browserName + scenario.getName()+"-"+ TestDataGenerator.getTimeStamp() + ".png");
                        Files.move(screenshot, screenShotFile);
                        if(screenshot.exists()){
                            screenshot.delete();
                        }
                        try (FileInputStream fis = new FileInputStream(screenShotFile)) {
                            Allure.addAttachment(browserName.toUpperCase() + "- Scenario " + scenario.getName(), "image/png", fis, ".png");
                        }
                        logsUtils.info(Colors.BLUE + "Screenshot captured: " + screenShotFile.getPath() + Colors.RESET);
                        Allure.step("Screenshot captured: " + screenShotFile.getPath(), Status.PASSED);
                    } catch (IOException e) {
                        logsUtils.logException(e);
                        Allure.step("Failed to capture screenshot: " + e.getMessage(), Status.FAILED);
                    }
                    break;
                case PASSED:
                    logsUtils.info(GREEN + ' ' + browserName + "[PASSED] Scenario " + scenario.getName() + " [PASSED]" + RESET);
                    break;
                case SKIPPED:
                    logsUtils.info(YELLOW + ' ' + browserName + "[SKIPPED] Scenario " + scenario.getName() + " [SKIPPED]" + RESET);
                    break;
            }
            String closeFlag= PropertyHelper.getDataFromProperties(configPath,
                    "closeDriverAfterScenario");
            if(closeFlag.equalsIgnoreCase("true")){
                localDriver.quit();
            }
            driver.remove();  // Remove WebDriver instance for this thread
        }
    }
    private static void logDevTools(DevTools devTools){
        devTools.createSession();
        devTools.send(Log.enable());
        devTools.addListener(Log.entryAdded(), logEntry -> {
            logsUtils.info(PURPLE+"Level: "+logEntry.getLevel()+RESET);
            logsUtils.info(BLUE+"Text: "+logEntry.getText()+RESET);
            logsUtils.info((YELLOW+"URL: "+logEntry.getUrl())+RESET);
            logsUtils.info((YELLOW+"StackTrace: "+logEntry.getStackTrace())+RESET);
        });
    }
    private static void initTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(configPath, "defaultDriverWaitTimeout");
            defaultTimeout = Integer.parseInt(timeout);
        } catch (Exception e) {
            logsUtils.logException(e);
        }
    }
    public static WebDriver getDriver() {
        return driver.get();  // Get WebDriver for the current thread
    }
}