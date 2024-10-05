package Ellithium.config.managment;

import java.io.File;

public class ConfigContext {

    // Static fields to store the configuration
    private static String browserName;
    private static String headlessMode;
    private static String pageLoadStrategy;
    private static String privateMode;
    private static String sandboxMode;
    private static String webSecurityMode;
    private static String reportPath;
    private static final String logFilePath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties"  + File.separator + "log4j2";

    private static final String configFilePath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties"  + File.separator + "config";
    private static final String allureFilePath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties"  + File.separator + "allure";
    private static final String emailFilePath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties" + File.separator + "email";

    private static final String basePropertyFolderPath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties" + File.separator;
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
    public static String getEmailFilePath() {
        return emailFilePath;
    }
    private static boolean lastUIFailed=false;
    private static File lastScreenShot;
    public static boolean isLastUIFailed() {
        return lastUIFailed;
    }

    public static void setLastUIFailed(boolean lastUIFailed) {
        ConfigContext.lastUIFailed = lastUIFailed;
    }

    public static File getLastScreenShot() {
        return lastScreenShot;
    }

    public static void setLastScreenShot(File lastScreenShot) {
        ConfigContext.lastScreenShot = lastScreenShot;
    }

    // Static method to set configuration
    public static void setConfig(String browserName, String headlessMode, String pageLoadStrategy,
                                 String privateMode, String sandboxMode, String webSecurityMode)
    {
        ConfigContext.browserName = browserName.toLowerCase();
        ConfigContext.headlessMode = headlessMode.toLowerCase();
        ConfigContext.pageLoadStrategy = pageLoadStrategy.toLowerCase();
        ConfigContext.privateMode = privateMode.toLowerCase();
        ConfigContext.sandboxMode = sandboxMode.toLowerCase();
        ConfigContext.webSecurityMode = webSecurityMode.toLowerCase();
    }

    // Static getters to retrieve configuration values
    public static String getBrowserName() {
        return browserName;
    }
    public static String getHeadlessMode() {
        return headlessMode;
    }

    public static String getPageLoadStrategy() {
        return pageLoadStrategy;
    }

    public static String getPrivateMode() {
        return privateMode;
    }

    public static String getSandboxMode() {
        return sandboxMode;
    }

    public static String getWebSecurityMode() {
        return webSecurityMode;
    }
    public static String getReportPath() {
        return reportPath;
    }
    public static void setReportPath(String reportPath) {
        ConfigContext.reportPath = reportPath;
    }
}
