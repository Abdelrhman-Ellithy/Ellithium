package Ellithium.core.execution.listener;
import static Ellithium.config.managment.GeneralHandler.attachScreenshotToReport;
import static Ellithium.core.reporting.internal.Colors.*;
import static Ellithium.config.managment.GeneralHandler.testFailed;

import Ellithium.config.managment.ConfigContext;
import Ellithium.core.driver.DriverFactory;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.core.logging.logsUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;

import java.io.File;

public class CucumberDefaultHooks {
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
        String closeFlag= PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "closeDriverAfterBDDScenario");
        if(closeFlag.equalsIgnoreCase("true")){
            DriverFactory.quitDriver();
        }else {
            DriverFactory.removeDriver();
        }
    }

}