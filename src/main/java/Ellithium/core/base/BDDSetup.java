package Ellithium.core.base;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.execution.listener.CustomTestNGListener;
import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.annotations.*;
@Listeners({CustomTestNGListener.class, AllureTestNg.class})
@CucumberOptions(
        glue = {"Ellithium.core.execution.listener"},
        plugin = {
                "pretty",
                "html:Test-Output/Reports/Cucumber/cucumber.html",
                "json:Test-Output/Reports/Cucumber/cucumber.json",
                "Ellithium.core.execution.listener.CucumberListener"
        }
)
public class BDDSetup extends AbstractTestNGCucumberTests {
        @Override
        @DataProvider(parallel = true)
        public Object[][] scenarios() {
                        return super.scenarios();
        }
}