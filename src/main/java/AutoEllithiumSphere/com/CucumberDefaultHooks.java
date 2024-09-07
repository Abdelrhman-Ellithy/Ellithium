package AutoEllithiumSphere.com;
import static AutoEllithiumSphere.com.Colors.*;

import AutoEllithiumSphere.DriverSetup.DriverSetUp;
import AutoEllithiumSphere.Utilities.logsUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import com.google.common.io.Files;
public class CucumberDefaultHooks {
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private String browserName;

    @Before
    public void setUp(Scenario scenario) {

        browserName = System.getProperty("BrowserName", "Chrome").toLowerCase();  // Default to Chrome if not set
        String headlessMode = System.getProperty("HeadlessMode", "false").toLowerCase(); // Default to false if not set
        String PageLoadStrategy=System.getProperty("PageLoadStrategy", "normal").toLowerCase(); // Default to normal if not set
        String PrivateMode=System.getProperty("PrivateMode", "true").toLowerCase();
        String SandboxMode=System.getProperty("SandboxMode","Sandbox").toLowerCase();
        String WebSecurityMode=System.getProperty("WebSecurityMode","True").toLowerCase();
        logsUtils.info(CYAN + " [START] " + browserName + BLUE + " Scenario " + scenario.getName() + " [START]\n" + RESET);
        WebDriver localDriver = DriverSetUp.setupLocalDriver(browserName, headlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
        localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.set(localDriver);  // Set WebDriver for this thread
    }
    @After
    public void tearDown(Scenario scenario) {
        WebDriver localDriver = driver.get();
        if (localDriver != null) {
            TakesScreenshot camera = (TakesScreenshot) localDriver;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);

            switch (scenario.getStatus()) {
                case FAILED:
                    try {
                        String browserName=System.getProperty("BrowserName");
                        logsUtils.info(RED+' '+browserName + " [FAILED] Scenario " + scenario.getName() + " [FAILED]" + RESET);
                        Files.move(screenshot, new File("Test-Output/ScreenShots/Failed/" +browserName+ scenario.getName() + ".png"));
                    } catch (IOException e) {
                        logsUtils.logException(e);
                    }
                    break;
                case PASSED:
                    logsUtils.info(GREEN +' '+browserName+ " [PASSED] Scenario " + scenario.getName() + " [PASSED]" + RESET);
                    break;
                case SKIPPED:
                    logsUtils.info(YELLOW+' '+browserName + " [SKIPPED] Scenario " + scenario.getName() + " [SKIPPED]" + RESET);
                    break;
            }
             localDriver.quit();
            driver.remove();  // Remove WebDriver instance for this thread
        }
    }
    public static WebDriver getDriver() {
        return driver.get();  // Get WebDriver for the current thread
    }
}