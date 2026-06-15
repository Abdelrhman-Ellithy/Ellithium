package Ellithium.Utilities.helpers;

import Ellithium.core.driver.CloudProviderType;

import java.io.IOException;

/** @deprecated Use {@link Ellithium.Utilities.cloud.CloudAppUploader} instead. */
@Deprecated(forRemoval = true)
public class CloudAppUploader {

    @Deprecated(forRemoval = true)
    public static String uploadApp(CloudProviderType provider, String username,
                                   String accessKey, String appFilePath) throws IOException {
        return Ellithium.Utilities.cloud.CloudAppUploader.uploadApp(provider, username, accessKey, appFilePath);
    }

    @Deprecated(forRemoval = true)
    public static String uploadApp(CloudProviderType provider, String username,
                                   String accessKey, String appFilePath, String customId) throws IOException {
        return Ellithium.Utilities.cloud.CloudAppUploader.uploadApp(provider, username, accessKey, appFilePath, customId);
    }

    @Deprecated(forRemoval = true)
    public static String uploadApp(CloudProviderType provider, String username,
                                   String accessKey, byte[] appBytes, String fileName) throws IOException {
        return Ellithium.Utilities.cloud.CloudAppUploader.uploadApp(provider, username, accessKey, appBytes, fileName);
    }

    @Deprecated(forRemoval = true)
    public static boolean deleteApp(CloudProviderType provider, String username,
                                    String accessKey, String appId) {
        return Ellithium.Utilities.cloud.CloudAppUploader.deleteApp(provider, username, accessKey, appId);
    }
}
