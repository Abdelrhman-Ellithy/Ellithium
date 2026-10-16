package Ellithium.Utilities.cloud;

import Ellithium.core.driver.CloudProviderType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;

public class CloudAppUploaderTest {

    @Test
    public void testUploadApp_credentialValidation() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CloudAppUploader.uploadApp(CloudProviderType.BROWSERSTACK, null, "key", "app.apk");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CloudAppUploader.uploadApp(CloudProviderType.BROWSERSTACK, "user", "   ", "app.apk");
        });
    }

    @Test
    public void testUploadApp_nonExistentFileThrows() {
        Assert.assertThrows(IOException.class, () -> {
            CloudAppUploader.uploadApp(CloudProviderType.BROWSERSTACK, "user", "key", "non_existent_file.apk");
        });
    }

    @Test
    public void testUploadApp_unsupportedExtensionThrows() throws IOException {
        File tempTxt = File.createTempFile("bad_app_", ".txt");
        try {
            Assert.assertThrows(IOException.class, () -> {
                CloudAppUploader.uploadApp(CloudProviderType.BROWSERSTACK, "user", "key", tempTxt.getAbsolutePath());
            });
        } finally {
            tempTxt.delete();
        }
    }

    @Test
    public void testDeleteApp_credentialValidation() {
        boolean result = CloudAppUploader.deleteApp(CloudProviderType.BROWSERSTACK, "", "key", "app-123");
        Assert.assertFalse(result);
    }

    @Test
    public void testExtractAppId_viaReflection() throws Exception {
        Method extractMethod = CloudAppUploader.class.getDeclaredMethod("extractAppId", CloudProviderType.class, String.class);
        extractMethod.setAccessible(true);

        // BrowserStack
        String bsJson = "{\"app_url\": \"bs://1234567890\"}";
        String bsId = (String) extractMethod.invoke(null, CloudProviderType.BROWSERSTACK, bsJson);
        Assert.assertEquals(bsId, "bs://1234567890");

        // SauceLabs
        String sauceJson = "{\"item\": {\"id\": \"sauce-storage-id\"}}";
        String sauceId = (String) extractMethod.invoke(null, CloudProviderType.SAUCE_LABS, sauceJson);
        Assert.assertEquals(sauceId, "storage:filename=sauce-storage-id");

        // LambdaTest raw id
        String ltRawJson = "{\"app_id\": \"app-999\"}";
        String ltRawId = (String) extractMethod.invoke(null, CloudProviderType.LAMBDATEST, ltRawJson);
        Assert.assertEquals(ltRawId, "lt://app-999");

        // LambdaTest already-prefixed id
        String ltPrefixedJson = "{\"app_id\": \"lt://app-999\"}";
        String ltPrefixedId = (String) extractMethod.invoke(null, CloudProviderType.LAMBDATEST, ltPrefixedJson);
        Assert.assertEquals(ltPrefixedId, "lt://app-999");
    }
}
