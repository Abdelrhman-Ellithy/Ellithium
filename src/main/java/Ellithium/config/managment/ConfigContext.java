package Ellithium.config.managment;

import Ellithium.core.driver.*;
import org.openqa.selenium.Capabilities;

import java.io.File;
import java.net.URL;
import java.util.Map;

public class ConfigContext {

    private static DriverType driverType =null;
    private static HeadlessMode headlessMode;
    private static PageLoadStrategyMode pageLoadStrategy;
    private static PrivateMode privateMode;
    private static SandboxMode sandboxMode;
    private static WebSecurityMode webSecurityMode;
    private static URL remoteAddress;
    private static Capabilities capabilities;
    private static final Map<DriverType, String> DRIVER_TYPE_STRING_MAP;
    private static final Map<HeadlessMode, String> HEADLESS_MODE_STRING_MAP;
    private static final Map<WebSecurityMode, String> WEB_SECURITY_MODE_STRING_MAP;
    private static final Map<PrivateMode, String> PRIVATE_MODE_STRING_MAP;
    private static final Map<PageLoadStrategyMode, String> PAGE_LOAD_STRATEGY_MODE_STRING_MAP;
    private static final Map<SandboxMode, String> SANDBOX_MODE_STRING_MAP;
    private static final String allureVersion="2.30.0";
    private static final String EllithuiumVersion="2.0.3";

    public static String getAllureVersion() {
        return allureVersion;
    }
    public static String getEllithuiumVersion() {
        return EllithuiumVersion;
    }

    static {
        // DriverType to String mapping
        DRIVER_TYPE_STRING_MAP = Map.ofEntries(
                Map.entry(LocalDriverType.Chrome, "Chrome"),
                Map.entry(LocalDriverType.Edge, "Edge"),
                Map.entry(LocalDriverType.Safari, "Safari"),
                Map.entry(LocalDriverType.FireFox, "Firefox"),
                Map.entry(RemoteDriverType.REMOTE_Chrome, "Remote Chrome"),
                Map.entry(RemoteDriverType.REMOTE_Edge, "Remote Edge"),
                Map.entry(RemoteDriverType.REMOTE_Safari, "Remote Safari"),
                Map.entry(RemoteDriverType.REMOTE_FireFox, "Remote Firefox"),
                Map.entry(MobileDriverType.Android, "Android"),
                Map.entry(MobileDriverType.IOS, "iOS")
        );
        // HeadlessMode to String mapping
        HEADLESS_MODE_STRING_MAP = Map.ofEntries(
                Map.entry(HeadlessMode.True, "Headless Mode Enabled"),
                Map.entry(HeadlessMode.False, "Headless Mode Disabled")
        );

        // WebSecurityMode to String mapping
        WEB_SECURITY_MODE_STRING_MAP = Map.ofEntries(
                Map.entry(WebSecurityMode.AllowUnsecure, "Allow Unsecure Content"),
                Map.entry(WebSecurityMode.SecureMode, "Secure Mode")
        );

        // PrivateMode to String mapping
        PRIVATE_MODE_STRING_MAP = Map.ofEntries(
                Map.entry(PrivateMode.True, "Private Mode Enabled"),
                Map.entry(PrivateMode.False, "Private Mode Disabled")
        );

        // PageLoadStrategyMode to String mapping
        PAGE_LOAD_STRATEGY_MODE_STRING_MAP = Map.ofEntries(
                Map.entry(PageLoadStrategyMode.Normal, "Normal Page Load"),
                Map.entry(PageLoadStrategyMode.Eager, "Eager Page Load")
        );

        // SandboxMode to String mapping
        SANDBOX_MODE_STRING_MAP = Map.ofEntries(
                Map.entry(SandboxMode.NoSandboxMode, "No Sandbox Mode"),
                Map.entry(SandboxMode.Sandbox, "Sandbox Mode Enabled")
        );
    }
    public static URL getRemoteAddress() {
        return remoteAddress;
    }

    public static void setRemoteAddress(URL remoteAddress) {
        ConfigContext.remoteAddress = remoteAddress;
    }

    public static Capabilities getCapabilities() {
        return capabilities;
    }

    public static void setCapabilities(Capabilities capabilities) {
        ConfigContext.capabilities = capabilities;
    }
    public static String getValue(DriverType key) {
        return DRIVER_TYPE_STRING_MAP.get(key);
    }

    public static String getValue(HeadlessMode key) {
        return HEADLESS_MODE_STRING_MAP.get(key);
    }

    public static String getValue(WebSecurityMode key) {
        return WEB_SECURITY_MODE_STRING_MAP.get(key);
    }

    public static String getValue(PrivateMode key) {
        return PRIVATE_MODE_STRING_MAP.get(key);
    }

    public static String getValue(PageLoadStrategyMode key) {
        return PAGE_LOAD_STRATEGY_MODE_STRING_MAP.get(key);
    }

    public static String getValue(SandboxMode key) {
        return SANDBOX_MODE_STRING_MAP.get(key);
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
    public static void setConfig(DriverType driverType, HeadlessMode headlessMode, PageLoadStrategyMode pageLoadStrategy,
                                 PrivateMode privateMode, SandboxMode sandboxMode, WebSecurityMode webSecurityMode)
    {
        ConfigContext.driverType = driverType;
        ConfigContext.headlessMode = headlessMode;
        ConfigContext.pageLoadStrategy = pageLoadStrategy;
        ConfigContext.privateMode = privateMode;
        ConfigContext.sandboxMode = sandboxMode;
        ConfigContext.webSecurityMode = webSecurityMode;
    }

    // Static getters to retrieve configuration values
    public static DriverType getDriverType() {
        return driverType;
    }
    public static HeadlessMode getHeadlessMode() {
        return headlessMode;
    }

    public static PageLoadStrategyMode getPageLoadStrategy() {
        return pageLoadStrategy;
    }

    public static PrivateMode getPrivateMode() {
        return privateMode;
    }

    public static SandboxMode getSandboxMode() {
        return sandboxMode;
    }

    public static WebSecurityMode getWebSecurityMode() {
        return webSecurityMode;
    }
    public static void setDriverType(DriverType driverType) {
        ConfigContext.driverType = driverType;
    }
    public static void setHeadlessMode(HeadlessMode headlessMode) {
        ConfigContext.headlessMode = headlessMode;
    }

    public static void setPageLoadStrategy(PageLoadStrategyMode pageLoadStrategy) {
        ConfigContext.pageLoadStrategy = pageLoadStrategy;
    }

    public static void setPrivateMode(PrivateMode privateMode) {
        ConfigContext.privateMode = privateMode;
    }

    public static void setSandboxMode(SandboxMode sandboxMode) {
        ConfigContext.sandboxMode = sandboxMode;
    }
    public static void setWebSecurityMode(WebSecurityMode webSecurityMode) {
        ConfigContext.webSecurityMode = webSecurityMode;
    }
}
