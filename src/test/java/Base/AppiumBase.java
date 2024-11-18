package Base;

import Ellithium.Utilities.interactions.actions;
import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class AppiumBase extends NonBDDSetup {
    protected AndroidDriver androidDriver;
    protected AppiumDriverLocalService serviceBuilder;
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
