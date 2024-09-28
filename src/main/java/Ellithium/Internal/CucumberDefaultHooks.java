package Ellithium.Internal;
import static Ellithium.Internal.GeneralHandler.attachScreenshotToReport;
import static Ellithium.Utilities.Colors.*;
import static Ellithium.Internal.GeneralHandler.testFailed;

import Ellithium.DriverSetup.DriverFactory;
import Ellithium.Utilities.PropertyHelper;
import Ellithium.Utilities.logsUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;

import java.io.File;

public class CucumberDefaultHooks {
    private static final String configPath="src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config";
    private String browserName;
    @Before
    public void ScenarioStart(Scenario scenario) {
        browserName = ConfigContext.getBrowserName().toUpperCase();
        Allure.getLifecycle().updateTestCase(testResult -> testResult.setName(scenario.getName()));
        logsUtils.info(CYAN + "[START] " + browserName.toUpperCase() + BLUE + " Scenario " + scenario.getName() + " [START]\n" + RESET);
    }
    @After
    public void ScenarioEnd(Scenario scenario) {
        switch (scenario.getStatus()) {
            case FAILED:
                File screenShot=testFailed(DriverFactory.getCurrentDriver(),browserName,scenario.getName());
                attachScreenshotToReport(screenShot,screenShot.getName(),browserName,scenario.getName());
                logsUtils.info(RED + ' ' + browserName.toUpperCase() + "[FAILED] Scenario " + scenario.getName() + " [FAILED]" + RESET);
                break;
            case PASSED:
                logsUtils.info(GREEN + ' ' + browserName + "[PASSED] Scenario " + scenario.getName() + " [PASSED]" + RESET);
                break;
            case SKIPPED:
                logsUtils.info(YELLOW + ' ' + browserName + "[SKIPPED] Scenario " + scenario.getName() + " [SKIPPED]" + RESET);
                break;
        }
        String closeFlag= PropertyHelper.getDataFromProperties(configPath, "closeDriverAfterBDDScenario");
        if(closeFlag.equalsIgnoreCase("true")){
            DriverFactory.quitDriver();
        }else {
            DriverFactory.removeDriver();
        }
    }

}