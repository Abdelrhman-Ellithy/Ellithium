package Tests;

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
public class AppiumTest extends NonBDDSetup {
    AndroidDriver androidDriver;
    AppiumDriverLocalService serviceBuilder;
    @BeforeClass
    public void setup() throws MalformedURLException {
        String appiumMainJsPath=System.getProperty("user.home").concat("\\AppData\\Roaming\\npm\\node_modules\\appium\\build\\lib\\main.js");
        serviceBuilder=new AppiumServiceBuilder().
                withAppiumJS(new File(appiumMainJsPath)).withIPAddress("0.0.0.0").usingPort(4723).build();
        //serviceBuilder.start();
        UiAutomator2Options options=new UiAutomator2Options();
        options.setDeviceName("Xiaomi Redmi Note 8");
        options.setApp("C:\\Users\\lenovo\\Documents\\college\\base.apk");
        androidDriver= DriverFactory.getNewDriver(DriverType.Android,new URL("http://0.0.0.0:4723"),options);
    }
    @Test(description = "Just testing the Ability to run tests on mobile, I made it fails to check the Attached Screenshot")
    public void SmokeMobileTest() {
        actions.clickOnElement(androidDriver,AppiumBy.accessibilityId("English - الإنجليزية "),5,200);
        Assert.assertTrue(false);
    }
    @AfterClass
    public void tareDown(){
        androidDriver.quit();
       // serviceBuilder.stop();
       // serviceBuilder.close();
    }
}
