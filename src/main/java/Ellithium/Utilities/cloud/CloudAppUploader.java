package Ellithium.Utilities.cloud;

import Ellithium.core.driver.CloudProviderType;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

public class CloudAppUploader {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public static String uploadApp(CloudProviderType provider, String username,
                                   String accessKey, String appFilePath) throws IOException {
        return uploadApp(provider, username, accessKey, appFilePath, null);
    }

    public static String uploadApp(CloudProviderType provider, String username,
                                   String accessKey, String appFilePath, String customId) throws IOException {

        Reporter.log("Uploading app to " + provider + ": " + appFilePath, LogLevel.INFO_BLUE);

        File appFile = new File(appFilePath);
        if (!appFile.exists()) {
            throw new IOException("App file not found: " + appFilePath);
        }
        validateAppFile(appFile);
        String uploadUrl = getUploadUrl(provider);
        String authHeader = createAuthHeader(username, accessKey);

        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            byte[] multipartBody = createMultipartBody(provider, appFile, customId, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofMinutes(10))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String appId = extractAppId(provider, response.body());
                Reporter.log("App uploaded successfully. App ID/URL: " + appId, LogLevel.INFO_GREEN);
                return appId;
            } else {
                String errorMsg = "App upload failed. Status: " + response.statusCode() + ", Response: " + response.body();
                Reporter.log(errorMsg, LogLevel.ERROR);
                throw new IOException(errorMsg);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("App upload interrupted", e);
        } catch (Exception e) {
            Reporter.log("Failed to upload app: " + e.getMessage(), LogLevel.ERROR);
            Logger.logException(e);
            throw new IOException("App upload failed", e);
        }
    }

    public static String uploadApp(CloudProviderType provider, String username,
                                   String accessKey, byte[] appBytes, String fileName) throws IOException {
        Path tempFile = Files.createTempFile("app_upload_", fileName);
        try {
            Files.write(tempFile, appBytes);
            return uploadApp(provider, username, accessKey, tempFile.toString(), null);
        } finally {
            try { Files.deleteIfExists(tempFile); } catch (IOException e) { Logger.logException(e); }
        }
    }

    public static boolean deleteApp(CloudProviderType provider, String username,
                                    String accessKey, String appId) {
        Reporter.log("Attempting to delete app: " + appId + " from " + provider, LogLevel.INFO_BLUE);
        try {
            String authHeader = createAuthHeader(username, accessKey);
            String cleanId = appId.replace("lt://", "").replace("bs://", "").replace("storage:filename=", "");
            if (provider == CloudProviderType.LAMBDATEST) {
                String deleteUrl = "https://manual-api.lambdatest.com/app/delete";
                JSONObject bodyJson = new JSONObject();
                bodyJson.put("appIds", cleanId);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(deleteUrl))
                        .header("Authorization", authHeader)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .method("DELETE", HttpRequest.BodyPublishers.ofString(bodyJson.toString()))
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                return handleResponse(response);
            } else {
                String deleteUrl = getDeleteUrl(provider, appId);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(deleteUrl))
                        .header("Authorization", authHeader)
                        .timeout(Duration.ofSeconds(30))
                        .DELETE()
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                return handleResponse(response);
            }
        } catch (Exception e) {
            Reporter.log("Failed to delete app: " + e.getMessage(), LogLevel.ERROR);
            Logger.logException(e);
            return false;
        }
    }

    private static void validateAppFile(File appFile) throws IOException {
        String fileName = appFile.getName().toLowerCase();
        if (!fileName.endsWith(".apk") && !fileName.endsWith(".ipa") && !fileName.endsWith(".aab")) {
            throw new IOException("Unsupported app file type. Supported: .apk, .ipa, .aab");
        }
    }

    private static String getUploadUrl(CloudProviderType provider) {
        return switch (provider) {
            case BROWSERSTACK -> "https://api-cloud.browserstack.com/app-automate/upload";
            case SAUCE_LABS   -> "https://api.us-west-1.saucelabs.com/v1/storage/upload";
            case LAMBDATEST   -> "https://manual-api.lambdatest.com/app/upload/realDevice";
            default -> throw new IllegalArgumentException("Upload not supported for provider: " + provider);
        };
    }

    private static String createAuthHeader(String username, String accessKey) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + accessKey).getBytes());
    }

    private static byte[] createMultipartBody(CloudProviderType provider, File appFile,
                                               String customId, String boundary) throws IOException {
        String fileFieldName = (provider == CloudProviderType.LAMBDATEST) ? "appFile" : "file";

        StringBuilder header = new StringBuilder();
        header.append("--").append(boundary).append("\r\n");
        header.append("Content-Disposition: form-data; name=\"").append(fileFieldName)
              .append("\"; filename=\"").append(appFile.getName()).append("\"\r\n");
        header.append("Content-Type: application/octet-stream\r\n\r\n");

        byte[] fileBytes   = Files.readAllBytes(appFile.toPath());
        byte[] headerBytes = header.toString().getBytes();

        StringBuilder footer = new StringBuilder();
        footer.append("\r\n");
        if (customId != null && !customId.isEmpty()) {
            footer.append("--").append(boundary).append("\r\n");
            footer.append("Content-Disposition: form-data; name=\"custom_id\"\r\n\r\n");
            footer.append(customId).append("\r\n");
        }
        footer.append("--").append(boundary).append("--\r\n");
        byte[] footerBytes = footer.toString().getBytes();

        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes,   0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);
        return body;
    }

    private static String extractAppId(CloudProviderType provider, String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return switch (provider) {
                case BROWSERSTACK -> {
                    if (json.has("app_url"))   yield json.getString("app_url");
                    if (json.has("custom_id")) yield "bs://" + json.getString("custom_id");
                    yield json.toString();
                }
                case SAUCE_LABS -> {
                    if (json.has("item")) {
                        JSONObject item = json.getJSONObject("item");
                        if (item.has("id")) yield "storage:filename=" + item.getString("id");
                    }
                    yield json.toString();
                }
                case LAMBDATEST -> {
                    if (json.has("app_url")) yield json.getString("app_url");
                    if (json.has("app_id"))  yield "lt://" + json.getString("app_id");
                    yield json.toString();
                }
                default -> responseBody;
            };
        } catch (Exception e) {
            Logger.logException(e);
            return responseBody;
        }
    }

    private static String getDeleteUrl(CloudProviderType provider, String appId) {
        String cleanId = appId.replace("bs://", "").replace("lt://", "").replace("storage:filename=", "");
        return switch (provider) {
            case BROWSERSTACK -> "https://api-cloud.browserstack.com/app-automate/app/delete/" + cleanId;
            case SAUCE_LABS   -> "https://api.us-west-1.saucelabs.com/v1/storage/files/" + cleanId;
            default -> throw new IllegalArgumentException("Delete not supported for provider: " + provider);
        };
    }

    private static boolean handleResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            Reporter.log("App deleted successfully", LogLevel.INFO_GREEN);
            return true;
        }
        Reporter.log("App deletion failed. Status: " + response.statusCode()
                + " Body: " + response.body(), LogLevel.ERROR);
        return false;
    }
}
