package Ellithium.DriverSetup;
import Ellithium.Internal.ConfigContext;
import Ellithium.Internal.CustomTestNGListener;
import Ellithium.Internal.GeneralHandler;
import Ellithium.Internal.SeleniumListener;
import Ellithium.Utilities.Colors;
import Ellithium.Utilities.PropertyHelper;
import Ellithium.Utilities.logsUtils;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.annotations.*;

@Listeners({CustomTestNGListener.class, AllureTestNg.class})
@CucumberOptions(
        glue = {"Ellithium.Internal", "Ellithium.DriverSetup"},
        plugin = {
                "pretty",
                "html:Test-Output/Reports/Cucumber/cucumber.html",
                "json:Test-Output/Reports/Cucumber/cucumber.json"
        }
)
public class BDDSetup extends AbstractTestNGCucumberTests {
        @Parameters({"BrowserName","HeadlessMode","PageLoadStrategy","PrivateMode","SandboxMode", "WebSecurityMode" })
        @BeforeTest(alwaysRun = true)
        protected void BrowserConfig(@Optional("Chrome") String BrowserName, @Optional("false") String HeadlessMode,@Optional("Normal") String PageLoadStrategy,@Optional("True") String PrivateMode,@Optional("Sandbox") String SandboxMode,@Optional("True") String WebSecurityMode) {
                if(GeneralHandler.getBDDMode()){
                        ConfigContext.setConfig(BrowserName,HeadlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
                }
                else{
                        logsUtils.error(Colors.RED+ "Invalid runMode Selection"+Colors.RESET);
                }
        }
        @Override
        @DataProvider(parallel = true) // always false
        public Object[][] scenarios() {
                if(GeneralHandler.getBDDMode()){
                        return super.scenarios();
                }
                else{
                        logsUtils.error(Colors.RED+ "Invalid runMode Selection"+Colors.RESET);
                        return null;
                }
        }
}