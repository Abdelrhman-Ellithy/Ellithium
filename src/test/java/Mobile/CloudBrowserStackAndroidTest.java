package Mobile;

import Ellithium.Utilities.helpers.CloudAppUploader;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.core.driver.CloudMobileDriverConfig;
import Ellithium.core.driver.CloudProviderType;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.MobileDriverType;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

public class CloudBrowserStackTest {
    private String username, accessKey;
    private ThreadLocal<String>appUrl=new ThreadLocal<>();
    @BeforeClass
    public void setup() {
        String testDataFile="src/test/resources/TestData/MobileApps/browserstack.json";
        boolean testDataFileExists=new File(testDataFile).exists();
        if (testDataFileExists){
            username= JsonHelper.getJsonKeyValue(testDataFile,"username");
            accessKey= JsonHelper.getJsonKeyValue(testDataFile,"accessKey");
        }
        else {
            username=System.getProperty("browserstackUser");
            accessKey=System.getProperty("browserstackAccessKey");
        }
    }
    @Test
    public void testBrowserStackAndroid()  throws Exception {
        appUrl.set(CloudAppUploader.uploadApp(
                CloudProviderType.BROWSERSTACK,
                username,
                accessKey,
                "src/test/resources/TestData/MobileApps/app-debug.apk",
                "my-app-v1.0"
        ));
        CloudMobileDriverConfig config = new CloudMobileDriverConfig()
                .setCloudProvider(CloudProviderType.BROWSERSTACK)
                .setUsername(username)
                .setAccessKey(accessKey)
                .setDriverType(MobileDriverType.Android)
                .setDeviceName("Samsung Galaxy S22")
                .setPlatformVersion("12.0")
                .setApp(appUrl.get())
                .setProjectName("My Mobile Project")
                .setBuildName("Build 1.0")
                .setTestName("Android Login Test")
                .setRealDevice(true)
                .setAutomationName("UiAutomator2");
        DriverFactory.getNewDriver(config);
        new ScreenRecorderActions<>(DriverFactory.getCurrentDriver()).captureScreenshot("test browserstack android app");
    }
    @Test
    public void testBrowserStackIOS() throws Exception {
        appUrl.set(CloudAppUploader.uploadApp(
                CloudProviderType.BROWSERSTACK,
                username,
                accessKey,
                "src/test/resources/TestData/MobileApps/BStackSampleApp.ipa",
                "my-app-v1.0"
        ));
        CloudMobileDriverConfig config = new CloudMobileDriverConfig()
                .setCloudProvider(CloudProviderType.BROWSERSTACK)
                .setUsername(username)
                .setAccessKey(accessKey)
                .setDriverType(MobileDriverType.IOS)
                .setDeviceName("iPhone 14 Pro")
                .setPlatformVersion("16.0")
                .setApp(appUrl.get())
                .setProjectName("My Mobile Project")
                .setBuildName("Build 1.0")
                .setTestName("iOS Login Test")
                .setRealDevice(true)
                .setAutomationName("XCUITest")
                .setDeviceOrientation("portrait");
        DriverFactory.getNewDriver(config);
        new ScreenRecorderActions<>(DriverFactory.getCurrentDriver()).captureScreenshot("test browserstack ios app");
    }
    @AfterMethod
    public void tareDown(){
        DriverFactory.quitDriver();
        CloudAppUploader.deleteApp(CloudProviderType.BROWSERSTACK,
                    username,
                    accessKey,
                    appUrl.get()
            );
        appUrl.remove();
        }
}