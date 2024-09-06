package AutoEllithiumSphere.DriverSetup;
import AutoEllithiumSphere.com.CustomTestNGListener;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.annotations.*;

@Listeners({AllureTestNg.class})
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"UI.stepDefinitions","AutoEllithiumSphere.com", "AutoEllithiumSphere.DriverSetup"},
        plugin = {
                "pretty",
                "html:Test-Output/Reports/Cucumber/cucumber.html",
                "json:Test-Output/Reports/Cucumber/cucumber.json",
        },
        monochrome = true,
        tags = "@Run"
)
public class SETUP extends AbstractTestNGCucumberTests {
        @Parameters({"BrowserName","HeadlessMode","PageLoadStrategy"})
        @BeforeClass(alwaysRun = true)
        protected void setUp(@Optional("Chrome") String BrowserName, @Optional("false") String HeadlessMode,@Optional("Normal") String PageLoadStrategy) {
                System.setProperty("BrowserName", BrowserName.toLowerCase());  // Set browser name for the current test run
                System.setProperty("HeadlessMode",HeadlessMode.toLowerCase());
                System.setProperty("PageLoadStrategy",PageLoadStrategy.toLowerCase());
        }
        @AfterClass(alwaysRun = true)
        protected void tearDown() {
                System.clearProperty("BrowserName");  // Clean up system property after test run
                System.clearProperty("HeadlessMode");   // Clean up system property after test run
                System.clearProperty("PageLoadStrategy");   // Clean up system property after test run
        }
        @Override
        @DataProvider(parallel = true) // always false
        public Object[][] scenarios() {
                return super.scenarios();
        }

}