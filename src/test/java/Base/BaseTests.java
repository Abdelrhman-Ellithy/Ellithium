package Base;

import Ellithium.core.driver.*;
import Pages.HomPage;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
public class BaseTests {
   protected WebDriver driver;
   protected HomPage home;
    @BeforeClass
    public void Setup(){
        DriverConfigBuilder driverConfig=new LocalDriverConfig(LocalDriverType.Chrome,
                HeadlessMode.False, PrivateMode.False,
                PageLoadStrategyMode.Normal,
                WebSecurityMode.SecureMode,
                SandboxMode.Sandbox);
        driver=DriverFactory.getNewDriver(driverConfig);
//        driver= DriverFactory.getNewLocalDriver(LocalDriverType.Chrome,
//                HeadlessMode.False, PrivateMode.False,
//                PageLoadStrategyMode.Normal,
//                WebSecurityMode.SecureMode,
//                SandboxMode.Sandbox);
        home=new HomPage(driver);
    }
    @AfterClass
    public void tareDown(){
        DriverFactory.quitDriver();
    }
}
