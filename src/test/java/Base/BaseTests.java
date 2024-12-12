package Base;

import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.driver.*;
import Pages.HomPage;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
public class BaseTests extends NonBDDSetup {
   protected WebDriver driver;
   protected HomPage home;
    @BeforeClass
    public void Setup(){
        driver= DriverFactory.getNewDriver(LocalDriverType.Chrome, HeadlessMode.False, PrivateMode.True, PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
        home=new HomPage(driver);
    }
    @AfterClass
    public void tareDown(){
        DriverFactory.quitDriver();
    }
}
