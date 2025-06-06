package Base;

import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.core.driver.*;
import Pages.HomPage;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
public class BaseTests {
   protected WebDriver driver;
   protected HomPage home;
   ScreenRecorderActions screenRecorderActions;
    @BeforeClass
    public void Setup()  {
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
        screenRecorderActions=new ScreenRecorderActions<>(driver);
        screenRecorderActions.startRecording("Test Name");
    }
    @AfterClass
    public void tareDown() {
        screenRecorderActions.stopRecording();
        DriverFactory.quitDriver();
    }
}
