package Base;

import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.driver.PrivateMode;
import Pages.HomPage;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class BaseTests extends NonBDDSetup {
   protected WebDriver driver;
   protected HomPage home;
    @BeforeClass
    public void Setup(){
        driver= DriverFactory.getNewDriver(DriverType.Chrome, HeadlessMode.False, PrivateMode.True);
        home=new HomPage(driver);
    }
    @AfterClass
    public void tareDown(){
        DriverFactory.quitDriver();;
    }
}
