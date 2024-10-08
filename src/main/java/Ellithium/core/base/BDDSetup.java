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
        @Parameters({"BrowserName","HeadlessMode","PageLoadStrategy","PrivateMode","SandboxMode", "WebSecurityMode" })
        @BeforeTest(alwaysRun = true, description = "Test Engine start")
        protected void setUp(@Optional("Chrome") String BrowserName, @Optional("false") String HeadlessMode,@Optional("Normal") String PageLoadStrategy,@Optional("True") String PrivateMode,@Optional("Sandbox") String SandboxMode,@Optional("True") String WebSecurityMode) {
                if(GeneralHandler.getBDDMode()){
                        ConfigContext.setConfig(BrowserName,HeadlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
                }
                else{
                        Reporter.log("Invalid runMode Selection", LogLevel.ERROR);
                }
        }
        @Override
        @DataProvider(parallel = true) // always false
        public Object[][] scenarios() {
                if(GeneralHandler.getBDDMode()){
                        return super.scenarios();
                }
                else{
                        Reporter.log("Invalid runMode Selection", LogLevel.ERROR);
                        return null;
                }
        }
}