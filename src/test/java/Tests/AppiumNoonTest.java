package Tests;

import Base.AppiumBase;
import Ellithium.Utilities.interactions.actions;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class AppiumNoonTest extends AppiumBase {
    // com.noon.buyerapp/.MainActivity
    @Test(description = "Just testing the Ability to run tests on mobile, I made it fails to check the Attached Screenshot")
    public void SmokeMobileTest() throws MalformedURLException, InterruptedException {
        UiAutomator2Options options=new UiAutomator2Options();
        options.setDeviceName("Xiaomi Redmi Note 8");
        options.setAppActivity("com.noon.buyerapp.MainActivity");
        options.setAppPackage("com.noon.buyerapp");
        options.setCapability("noReset", true);
        options.setCapability("fullReset", false);
        androidDriver= DriverFactory.getNewDriver(DriverType.Android,new URL("http://0.0.0.0:4723"),options);
        actions.clickOnElement(androidDriver,AppiumBy.accessibilityId("English - الإنجليزية "),5,200);
        Thread.sleep(2000);
        Assert.assertTrue(false);
    }
}
