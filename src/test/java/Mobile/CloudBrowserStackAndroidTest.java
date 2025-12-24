package Mobile;

import Ellithium.Utilities.helpers.CloudAppUploader;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.Utilities.interactions.Sleep;
import Ellithium.core.driver.CloudMobileDriverConfig;
import Ellithium.core.driver.CloudProviderType;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.MobileDriverType;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

public class CloudBrowserStackAndroidTest {
    private String username, accessKey;
    private ThreadLocal<String>appUrl=new ThreadLocal<>();
    AndroidDriver driver;
    @Test
    public void testBrowserStackAndroid() {
        new ScreenRecorderActions<>(driver).captureScreenshot("test browserstack ios app");
    }
    @BeforeMethod
    public void setUp() throws Exception {
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
                .setAutomationName("UiAutomator2")
                .setVideoRecording(true);
        driver=DriverFactory.getNewDriver(config);
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