package Ellithium.config.management;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigContext {

    private static final String allureVersion      = "2.30.0";
    private static final String EllithuiumVersion  = "3.0.0-beta";

    public static String getAllureVersion()    { return allureVersion; }
    public static String getEllithuiumVersion(){ return EllithuiumVersion; }

    private static final AtomicInteger retryCount = new AtomicInteger(0);
    public static int  getRetryCount()           { return retryCount.get(); }
    public static void setRetryCount(int value)  { retryCount.set(value); }

    private static final String logFilePath =
            "src" + File.separator + "main" + File.separator + "resources" +
            File.separator + "properties" + File.separator + "log4j2.properties";
    private static final String configFilePath =
            "src" + File.separator + "main" + File.separator + "resources" +
            File.separator + "properties" + File.separator + "config.properties";
    private static final String allureFilePath =
            "src" + File.separator + "main" + File.separator + "resources" +
            File.separator + "properties" + File.separator + "allure.properties";
    private static final String aiFilePath =
            "src" + File.separator + "main" + File.separator + "resources" +
            File.separator + "properties" + File.separator + "ai-config.properties";
    private static final String notificationFilePath =
            "src" + File.separator + "main" + File.separator + "resources" +
            File.separator + "properties" + File.separator + "notifications.properties";
    private static final String basePropertyFolderPath =
            "src" + File.separator + "main" + File.separator + "resources" +
            File.separator + "properties" + File.separator;
    private static final String checkerFilePath =
            "Test-Output" + File.separator + "UpdateChecker" + File.separator + "checker.json";
    private static final String failedScreenShotPath =
            "Test-Output" + File.separator + "ScreenShots" + File.separator + "Failed";
    private static final String capturedScreenShotPath =
            "Test-Output" + File.separator + "ScreenShots" + File.separator + "Captured";
    private static final String recordedExecutionsPath =
            "Test-Output" + File.separator + "RecordedExecutions";
    private static final String checkerFolderPath =
            "Test-Output" + File.separator + "UpdateChecker";

    public static String getCheckerFilePath()        { return checkerFilePath; }
    public static String getFailedScreenShotPath()   { return failedScreenShotPath; }
    public static String getCapturedScreenShotPath() { return capturedScreenShotPath; }
    public static String getRecordedExecutionsPath() { return recordedExecutionsPath; }
    public static String getCheckerFolderPath()      { return checkerFolderPath; }
    public static String getNotificationFilePath()   { return notificationFilePath; }
    public static String getBasePropertyFolderPath() { return basePropertyFolderPath; }
    public static String getConfigFilePath()         { return configFilePath; }
    public static String getAllureFilePath()          { return allureFilePath; }
    public static String getAiFilePath()             { return aiFilePath; }
    public static String getLogFilePath()            { return logFilePath; }

    public static String getEllithiumRepoPath() {
        return System.getProperty("user.home") + File.separator + ".m2" + File.separator
                + "repository" + File.separator + "io" + File.separator + "github"
                + File.separator + "abdelrhman-ellithy" + File.separator + "ellithium";
    }

    private static volatile boolean onExecution = false;
    private static volatile boolean isLoggingOn  = false;

    public static boolean isOnExecution()               { return onExecution; }
    public static void    setOnExecution(boolean v)     { onExecution = v; }
    public static boolean isLoggingOn()                 { return isLoggingOn; }
    public static void    setIsLoggingOn(boolean v)     { isLoggingOn = v; }

    public static final class Paths {
        private Paths() {}
        public static String log()              { return logFilePath; }
        public static String config()           { return configFilePath; }
        public static String allure()           { return allureFilePath; }
        public static String ai()               { return aiFilePath; }
        public static String notifications()    { return notificationFilePath; }
        public static String baseProperties()   { return basePropertyFolderPath; }
        public static String checker()          { return checkerFilePath; }
        public static String failedScreenshots(){ return failedScreenShotPath; }
        public static String capturedScreenshots(){ return capturedScreenShotPath; }
        public static String recordedExecutions(){ return recordedExecutionsPath; }
        public static String checkerFolder()    { return checkerFolderPath; }
        public static String ellithiumRepo()    { return getEllithiumRepoPath(); }
    }

    public static final class State {
        private State() {}
        public static boolean onExecution()              { return ConfigContext.onExecution; }
        public static void    setOnExecution(boolean v)  { ConfigContext.onExecution = v; }
        public static boolean loggingOn()                { return ConfigContext.isLoggingOn; }
        public static void    setLoggingOn(boolean v)    { ConfigContext.isLoggingOn = v; }
        public static int     retryCount()               { return ConfigContext.retryCount.get(); }
        public static void    setRetryCount(int v)       { ConfigContext.retryCount.set(v); }
    }
}
