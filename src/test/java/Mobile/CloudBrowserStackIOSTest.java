package Mobile;

import Ellithium.Utilities.helpers.CloudAppUploader;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.core.driver.CloudMobileDriverConfig;
import Ellithium.core.driver.CloudProviderType;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.MobileDriverType;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

public class CloudBrowserStackIOSTest {
    private String username, accessKey;
    private ThreadLocal<String>appUrl=new ThreadLocal<>();
    private AndroidDriver driver;
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
    public void testBrowserStackIOS() {
        new ScreenRecorderActions<>(driver).captureScreenshot("test browserstack ios app");
    }
    @BeforeMethod
    public void setUp() throws Exception {
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
