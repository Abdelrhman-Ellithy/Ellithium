package Mobile;

import Ellithium.Utilities.helpers.CloudAppUploader;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.core.driver.CloudMobileDriverConfig;
import Ellithium.core.driver.CloudProviderType;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.MobileDriverType;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.io.IOException;

public class CloudBrowserStackAndroidTest {
    private String username, accessKey,appUrl;
    private final String testDataFile="src/test/resources/TestData/MobileApps/browserstack.json";
    @BeforeMethod
    public void setup() throws IOException {
        username= JsonHelper.getJsonKeyValue(testDataFile,"username");
        accessKey= JsonHelper.getJsonKeyValue(testDataFile,"accessKey");
        appUrl = CloudAppUploader.uploadApp(
                CloudProviderType.BROWSERSTACK,
                username,
                accessKey,
                "src/test/resources/TestData/MobileApps/app-debug.apk",
                "my-app-v1.0"
        );
        Reporter.log(appUrl,LogLevel.INFO_BLUE);
    }
    @Test
    public void testBrowserStackAndroid() {
        CloudMobileDriverConfig config = new CloudMobileDriverConfig()
                .setCloudProvider(CloudProviderType.BROWSERSTACK)
                .setUsername(username)
                .setAccessKey(accessKey)
                .setDriverType(MobileDriverType.Android)
                .setDeviceName("Samsung Galaxy S22")
                .setPlatformVersion("12.0")
                .setApp(appUrl)
                .setProjectName("My Mobile Project")
                .setBuildName("Build 1.0")
                .setTestName("Android Login Test")
                .setRealDevice(true)
                .setAutomationName("UiAutomator2");
        AndroidDriver driver = DriverFactory.getNewDriver(config);
        new ScreenRecorderActions<>(driver).captureScreenshot("test app");
        DriverFactory.quitDriver();
    }
    @AfterMethod
    public void tareDown(){
        CloudAppUploader.deleteApp(CloudProviderType.BROWSERSTACK,
                    username,
                    accessKey,
                    appUrl
            );
        }
}