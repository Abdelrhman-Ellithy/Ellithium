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
    ScreenRecorderActions recorderActions;
    @BeforeClass
    public void Setup() throws Exception {
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
        recorderActions=new ScreenRecorderActions<>(driver);
        recorderActions.startRecording("TestRecord");
    }
    @AfterClass
    public void tareDown() throws Exception {
        recorderActions.stopRecording();
        DriverFactory.quitDriver();
    }
}
