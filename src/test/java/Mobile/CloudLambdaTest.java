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
import org.testng.annotations.Test;

import java.io.File;

public class CloudLambdaTest {
    private String username, accessKey;
    private ThreadLocal<String>appUrl=new ThreadLocal<>();
    @BeforeClass
    public void setup() {
        String testDataFile="src/test/resources/TestData/MobileApps/browserstack.json";
        boolean testDataFileExists=new File(testDataFile).exists();
        if (testDataFileExists){
            username= JsonHelper.getJsonKeyValue(testDataFile,"lambdaUser");
            accessKey= JsonHelper.getJsonKeyValue(testDataFile,"lambdaPass");
        }
        else {
            username=System.getProperty("lambdaUser");
            accessKey=System.getProperty("lambdaPass");
        }
    }
    @Test
    public void LambdaTestAndroid() throws Exception {
        appUrl.set( CloudAppUploader.uploadApp(
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
        AndroidDriver driver = DriverFactory.getNewDriver(config);
        new ScreenRecorderActions<>(driver).captureScreenshot("test lambda android app");
    }
    @AfterMethod
    public void tareDown(){
        DriverFactory.quitDriver();
        CloudAppUploader.deleteApp(CloudProviderType.LAMBDATEST,
                username,
                accessKey,
                appUrl.get()
        );
        appUrl.remove();
    }
}
