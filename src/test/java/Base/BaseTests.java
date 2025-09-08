package Base;

import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.core.driver.*;
import Pages.HomPage;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class BaseTests {
   protected WebDriver driver;
   protected HomPage home;
    @BeforeClass
    public void Setup()  {
        LocalDriverConfig driverConfig=new LocalDriverConfig(LocalDriverType.Chrome,
                HeadlessMode.False, PrivateMode.False,
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
