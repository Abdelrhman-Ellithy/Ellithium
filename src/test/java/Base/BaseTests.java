package Base;

import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.driver.*;
import Pages.HomPage;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;

public class BaseTests extends NonBDDSetup {
   protected WebDriver driver;
   protected HomPage home;
    @Parameters({"BrowserName"})
    @BeforeClass
    public void Setup(String BrowserName){
        DriverType type;
        switch(BrowserName.toLowerCase()){
            case "edge"->type=DriverType.Edge;
            case "firefox"->type=DriverType.FireFox;
            default -> {type=DriverType.Chrome;}
        }
        driver= DriverFactory.getNewLocalWebDriver(type, HeadlessMode.False, PrivateMode.True, PageLoadStrategyMode.Normal,WebSecurityMode.SecureMode,SandboxMode.Sandbox);
        home=new HomPage(driver);
    }
    @AfterClass
    public void tareDown(){
        DriverFactory.quitDriver();;
    }
}
