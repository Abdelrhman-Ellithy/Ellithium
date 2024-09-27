package Ellithium.Internal;
public class ConfigContext {

    // Static fields to store the configuration
    private static String browserName;
    private static String headlessMode;
    private static String pageLoadStrategy;
    private static String privateMode;
    private static String sandboxMode;
    private static String webSecurityMode;

    // Static method to set configuration
    public static void setConfig(String browserName, String headlessMode, String pageLoadStrategy,
                                 String privateMode, String sandboxMode, String webSecurityMode) {
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
}
