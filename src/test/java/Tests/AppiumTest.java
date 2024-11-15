package Tests;

import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.driver.DriverFactory;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.testng.annotations.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class AppiumTest extends NonBDDSetup {
    @Test
    public void Test1() throws MalformedURLException, InterruptedException {
        String appiumMainJsPath=System.getProperty("user.home").concat("\\AppData\\Roaming\\npm\\node_modules\\appium\\build\\lib\\main.js");
        AppiumDriverLocalService serviceBuilder=new AppiumServiceBuilder().
                withAppiumJS(new File(appiumMainJsPath)).withIPAddress("0.0.0.0").usingPort(4723).build();
        //serviceBuilder.start();
        UiAutomator2Options options=new UiAutomator2Options();
        options.setDeviceName("Xiaomi Redmi Note 8");
        options.setApp("C:\\Users\\lenovo\\Documents\\college\\base.apk");
        AndroidDriver androidDriver= DriverFactory.getAndroidDriver(new URL("http://0.0.0.0:4723"),options);
        Thread.sleep(2000);
        androidDriver.findElement(AppiumBy.accessibilityId("English - الإنجليزية ")).click();
        Thread.sleep(2000);
        androidDriver.quit();
        serviceBuilder.stop();
    }
}
