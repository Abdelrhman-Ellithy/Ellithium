package Mobile;

import Ellithium.Utilities.cloud.CloudAppUploader;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.Utilities.interactions.DriverActions;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
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

public class CloudLambdaTest {
    private String username, accessKey;
    private ThreadLocal<String> appUrl = new ThreadLocal<>();
    private AndroidDriver driver;

    @BeforeMethod
    public void setUp() throws Exception {
        String testDataFile = "src/test/resources/TestData/MobileApps/browserstack.json";
        if (new File(testDataFile).exists()) {
            username = JsonHelper.getJsonKeyValue(testDataFile, "lambdaUser");
            accessKey = JsonHelper.getJsonKeyValue(testDataFile, "lambdaPass");
        } else {
            username = System.getProperty("lambdaUser");
            accessKey = System.getProperty("lambdaPass");
        }
        appUrl.set(CloudAppUploader.uploadApp(
                CloudProviderType.LAMBDATEST,
                username,
                accessKey,
                "src/test/resources/TestData/MobileApps/app-debug.apk"
        ));
        CloudMobileDriverConfig config = new CloudMobileDriverConfig()
                .setCloudProvider(CloudProviderType.LAMBDATEST)
                .setUsername(username)
                .setAccessKey(accessKey)
                .setDriverType(MobileDriverType.Android)
                .setDeviceName("Galaxy A12")
                .setPlatformVersion("11")
                .setApp(appUrl.get())
                .setRealDevice(true)
                .setAutomationName("UiAutomator2");
        driver = DriverFactory.getNewDriver(config);
    }

    @Test
    public void LambdaTestAndroid() {
        new ScreenRecorderActions<>(driver).captureScreenshot("test lambda android app");
        DriverActions driverActions = new DriverActions(driver);
        driverActions.elements().clickOnElement(By.xpath("(//android.widget.EditText)[1]"));
        driverActions.elements().sendData(By.xpath("(//android.widget.EditText)[1]"), "testuser");
        driverActions.elements().clickOnElement(By.xpath("(//android.widget.EditText)[2]"));
        driverActions.elements().sendData(By.xpath("(//android.widget.EditText)[2]"), "test");
        driverActions.elements().isElementDisplayed(By.xpath("//android.widget.TextView[@text='Hello World!']"));
        driverActions.elements().isElementDisplayed(By.xpath("//android.widget.TextView[@text='AddNumber']"));
        driverActions.elements().clickOnElement(By.xpath("//android.widget.Button[@text='ADD']"));
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
        String uploadedApp = appUrl.get();
        if (uploadedApp != null) {
            CloudAppUploader.deleteApp(CloudProviderType.LAMBDATEST, username, accessKey, uploadedApp);
        }
        appUrl.remove();
    }
}
