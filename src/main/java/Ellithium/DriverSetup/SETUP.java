package Ellithium.DriverSetup;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.annotations.*;

@Listeners({AllureTestNg.class})
@CucumberOptions(
        glue = {"Ellithium.com", "Ellithium.DriverSetup"},
        plugin = {
                "pretty",
                "html:Test-Output/Reports/Cucumber/cucumber.html",
                "json:Test-Output/Reports/Cucumber/cucumber.json",
        }
)
public class SETUP extends AbstractTestNGCucumberTests {
        @Parameters({"BrowserName","HeadlessMode","PageLoadStrategy","PrivateMode","SandboxMode", "WebSecurityMode" })
        @BeforeTest(alwaysRun = true)
        protected void setUp(@Optional("Chrome") String BrowserName, @Optional("false") String HeadlessMode,@Optional("Normal") String PageLoadStrategy,@Optional("True") String PrivateMode,@Optional("Sandbox") String SandboxMode,@Optional("True") String WebSecurityMode) {
                System.setProperty("BrowserName", BrowserName.toLowerCase());
                System.setProperty("HeadlessMode",HeadlessMode.toLowerCase());
                System.setProperty("PageLoadStrategy",PageLoadStrategy.toLowerCase());
                System.setProperty("PrivateMode",PrivateMode.toLowerCase());
                System.setProperty("SandboxMode", SandboxMode.toLowerCase());
                System.setProperty("WebSecurityMode",WebSecurityMode.toLowerCase());
        }
        @AfterTest(alwaysRun = true)
        protected void tearDown() {
                System.clearProperty("BrowserName");
                System.clearProperty("HeadlessMode");
                System.clearProperty("PageLoadStrategy");
                System.clearProperty("PrivateMode");
                System.clearProperty("SandboxMode");
                System.clearProperty("WebSecurityMode");
        }
        @Override
        @DataProvider(parallel = true) // always false
        public Object[][] scenarios() {
                return super.scenarios();
        }
}