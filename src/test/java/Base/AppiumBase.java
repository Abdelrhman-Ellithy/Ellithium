package Base;

import Ellithium.Utilities.interactions.DriverActions;
import Ellithium.core.base.NonBDDSetup;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.File;

public class AppiumBase extends NonBDDSetup {
    protected AndroidDriver androidDriver;
    protected AppiumDriverLocalService serviceBuilder;
    protected IOSDriver iosDriver;
    protected DriverActions driverActions;
    @BeforeClass
    public void setup()  {
        String appiumMainJsPath=System.getProperty("user.home").concat("\\AppData\\Roaming\\npm\\node_modules\\appium\\build\\lib\\main.js");
        serviceBuilder=new AppiumServiceBuilder().
                withAppiumJS(new File(appiumMainJsPath)).withIPAddress("0.0.0.0").usingPort(4723).build();
        //serviceBuilder.start();
    }
    @AfterClass
    public void tareDown(){
        androidDriver.quit();
         // serviceBuilder.stop();
        // serviceBuilder.close();
    }
}
