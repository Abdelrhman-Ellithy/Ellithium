package Mobile;

import Ellithium.Utilities.cloud.CloudAppUploader;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.Utilities.interactions.DriverActions;
import Ellithium.core.driver.CloudMobileDriverConfig;
import Ellithium.core.driver.CloudProviderType;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.MobileDriverType;
import io.appium.java_client.android.AndroidDriver;

import org.openqa.selenium.By;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

public class CloudBrowserStackAndroidTest {
    private String username, accessKey;
    private ThreadLocal<String> appUrl = new ThreadLocal<>();
    AndroidDriver driver;

    @Test
    public void testBrowserStackAndroid() {
        new ScreenRecorderActions<>(driver).captureScreenshot("test browserstack ios app");
        DriverActions driverActions = new DriverActions(driver);
        driverActions.elements().clickOnElement(By.xpath("(//android.widget.EditText)[1]"));
        driverActions.elements().sendData(By.xpath("(//android.widget.EditText)[1]"), "testuser");
        driverActions.elements().clickOnElement(By.xpath("(//android.widget.EditText)[2]"));
        driverActions.elements().sendData(By.xpath("(//android.widget.EditText)[2]"), "test");
        driverActions.elements().isElementDisplayed(By.xpath("//android.widget.TextView[@text='Hello World!']"));
        driverActions.elements().isElementDisplayed(By.xpath("//android.widget.TextView[@text='AddNumber']"));
        driverActions.elements().clickOnElement(By.xpath("//android.widget.Button[@text='ADD']"));
    }

    @BeforeMethod
    public void setUp() throws Exception {
        String testDataFile = "src/test/resources/TestData/MobileApps/browserstack.json";
        if (new File(testDataFile).exists()) {
            username = JsonHelper.getJsonKeyValue(testDataFile, "username");
            accessKey = JsonHelper.getJsonKeyValue(testDataFile, "accessKey");
        } else {
            username = System.getProperty("browserstackUser");
            accessKey = System.getProperty("browserstackAccessKey");
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
                .setProjectName("My project test")
                .setBuildName("Build 1.0")
                .setTestName("Android Login Test")
                .setRealDevice(true)
                .setAutomationName("UiAutomator2")
                .setVideoRecording(true);
        driver = DriverFactory.getNewDriver(config);
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
        String uploadedApp = appUrl.get();
        if (uploadedApp != null) {
            CloudAppUploader.deleteApp(CloudProviderType.BROWSERSTACK, username, accessKey, uploadedApp);
        }
        appUrl.remove();
    }
}
