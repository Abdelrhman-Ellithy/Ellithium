package Base;

import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.core.driver.*;
import Ellithium.core.reporting.Reporter;
import Pages.HomPage;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
public class BaseTests {
   protected WebDriver driver;
   protected HomPage home;
    @BeforeClass
    public void Setup()  {
        LocalDriverConfig driverConfig=new LocalDriverConfig(LocalDriverType.Chrome,
                HeadlessMode.False, PrivateMode.True,
                PageLoadStrategyMode.Normal,
                WebSecurityMode.SecureMode,
                SandboxMode.Sandbox);
        driver=DriverFactory.getNewDriver(driverConfig);
        home=new HomPage(driver);
    }
    @AfterClass
    public void tareDown() {
        DriverFactory.quitDriver();
    }
}
