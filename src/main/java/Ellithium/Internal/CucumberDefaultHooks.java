package Ellithium.com;
import static Ellithium.Utilities.Colors.*;
import static Ellithium.com.GeneralHandler.testFailed;

import Ellithium.DriverSetup.DriverFactory;
import Ellithium.DriverSetup.DriverSetUp;
import Ellithium.Utilities.PropertyHelper;
import Ellithium.Utilities.logsUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
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
    private static final String configPath="src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config";
    private String browserName;
    @Before
    public void setUp(Scenario scenario) {
        browserName = System.getProperty("BrowserName", "Chrome").toUpperCase();
        Allure.getLifecycle().updateTestCase(testResult -> testResult.setName(scenario.getName()));
        logsUtils.info(CYAN + "[START] " + browserName.toUpperCase() + BLUE + " Scenario " + scenario.getName() + " [START]\n" + RESET);
    }
    @After
    public void tearDown(Scenario scenario) {
        switch (scenario.getStatus()) {
            case FAILED:
                testFailed(DriverFactory.getCurrentDriver(),browserName,scenario.getName());
                logsUtils.info(RED + ' ' + browserName.toUpperCase() + "[FAILED] Scenario " + scenario.getName() + " [FAILED]" + RESET);
                break;
            case PASSED:
                logsUtils.info(GREEN + ' ' + browserName + "[PASSED] Scenario " + scenario.getName() + " [PASSED]" + RESET);
                break;
            case SKIPPED:
                logsUtils.info(YELLOW + ' ' + browserName + "[SKIPPED] Scenario " + scenario.getName() + " [SKIPPED]" + RESET);
                break;
        }
        String closeFlag= PropertyHelper.getDataFromProperties(configPath, "closeDriverAfterScenario");
        if(closeFlag.equalsIgnoreCase("true")){
            DriverFactory.quitDriver();
        }else {
            DriverFactory.removeDriver();
        }
    }

}