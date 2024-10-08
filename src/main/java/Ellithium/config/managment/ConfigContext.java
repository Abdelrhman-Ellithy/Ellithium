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

    private static final String basePropertyFolderPath="src" + File.separator + "main" + File.separator + "resources" + File.separator +
            "properties" + File.separator;

    private static final String checkerFilePath="Test-Output" + File.separator + "UpdateChecker"+File.separator+"checker";
    private static final String checkerFolderPath="Test-Output" + File.separator + "UpdateChecker";
    public static String getCheckerFilePath() {
        return checkerFilePath;
    }
    public static String getCheckerFolderPath() {
        return checkerFolderPath;
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
    public static boolean isOnExecution() {
        return onExecution;
    }
    public static void setOnExecution(boolean onExecution) {
        ConfigContext.onExecution = onExecution;
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
