package Tests;

import Base.AppiumBase;
import Ellithium.Utilities.interactions.DriverActions;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import Ellithium.core.driver.MobileDriverType;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.Test;
import java.net.MalformedURLException;
import java.net.URL;

public class AppiumTest extends AppiumBase {
    @Test(description = "Just testing the Ability to run tests on mobile, I made it fails to check the Attached Screenshot")
    public void SmokeMobileTest() throws MalformedURLException, InterruptedException {
        UiAutomator2Options options=new UiAutomator2Options();
        options.setDeviceName("Xiaomi Redmi Note 8");
        options.setAppActivity("com.appyinnovate.e_invoice.MainActivity");
        options.setAppPackage("com.appyinnovate.e_invoice");
        androidDriver= DriverFactory.getNewMobileDriver(MobileDriverType.Android,new URL("http://0.0.0.0:4723"),options);
        driverActions=new DriverActions(androidDriver);
    }
}
