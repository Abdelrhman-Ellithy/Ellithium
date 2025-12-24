package Ellithium.config.managment;

import java.io.File;
public class ConfigContext {

    private static final String allureVersion="2.30.0";
    private static final String EllithuiumVersion="2.3.0";
    public static String getAllureVersion() {
        return allureVersion;
    }
    public static String getEllithuiumVersion() {
        return EllithuiumVersion;
    }
    public static int getRetryCount() {
        return retryCount;
    }

    public static void setRetryCount(int retryCount) {
        ConfigContext.retryCount = retryCount;
    }
    private static int retryCount;
    private static final String logFilePath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties"  + File.separator + "log4j2"+".properties";
    private static final String configFilePath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties"  + File.separator + "config"+".properties";
    private static final String allureFilePath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties"  + File.separator + "allure"+".properties";
    private final static String notificationFilePath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties"  + File.separator + "notifications"+".properties";
    private static final String basePropertyFolderPath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties" + File.separator;
    private static final String checkerFilePath="Test-Output" + File.separator + "UpdateChecker"+File.separator+"checker"+".json";
    private static final String failedScreenShotPath="Test-Output" + File.separator + "ScreenShots" + File.separator + "Failed";
    private static final String capturedScreenShotPath="Test-Output" + File.separator + "ScreenShots" + File.separator + "Captured";
    private static final String recordedExecutionsPath="Test-Output" + File.separator + "RecordedExecutions";
    private static final String checkerFolderPath="Test-Output" + File.separator + "UpdateChecker";
    public static String getCheckerFilePath() {
        return checkerFilePath;
    }
    public static String getFailedScreenShotPath() {
        return failedScreenShotPath;
    }
    public static String getCapturedScreenShotPath() {
        return capturedScreenShotPath;
    }
    public static String getRecordedExecutionsPath() {
        return recordedExecutionsPath;
    }
    public static String getCheckerFolderPath() {
        return checkerFolderPath;
    }
    public static String getNotificationFilePath() {
        return notificationFilePath;
    }
    public static String getEllithiumRepoPath() {
        return System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository"
                + File.separator + "io" + File.separator + "github" + File.separator+"abdelrhman-ellithy"+ File.separator + "ellithium";
    }
    public static String getBasePropertyFolderPath() {
        return basePropertyFolderPath;
    }
    public static String getConfigFilePath() {
        return configFilePath;
    }
    public static String getAllureFilePath() {
        return allureFilePath;
    }
    public static String getLogFilePath() {
        return logFilePath;
    }
    private static boolean onExecution=false;
    public static boolean isLoggingOn() {
        return isLoggingOn;
    }
    public static void setIsLoggingOn(boolean isLoggingOn) {
        ConfigContext.isLoggingOn = isLoggingOn;
    }
    private static boolean isLoggingOn=false;
    public static boolean isOnExecution() {
        return onExecution;
    }
    public static void setOnExecution(boolean onExecution) {
        ConfigContext.onExecution = onExecution;
    }
}
