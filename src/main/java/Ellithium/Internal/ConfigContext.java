package Ellithium.Internal;

public class Config {
    private static Config instance;
    private String browserName;
    private String headlessMode;
    private String pageLoadStrategy;
    private String privateMode;
    private String sandboxMode;
    private String webSecurityMode;
    private Config() {}

    // Singleton instance
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    // Setters
    public void setBrowserName(String browserName) { this.browserName = browserName; }
    public void setHeadlessMode(String headlessMode) { this.headlessMode = headlessMode; }
    public void setPageLoadStrategy(String pageLoadStrategy) { this.pageLoadStrategy = pageLoadStrategy; }
    public void setPrivateMode(String privateMode) { this.privateMode = privateMode; }
    public void setSandboxMode(String sandboxMode) { this.sandboxMode = sandboxMode; }
    public void setWebSecurityMode(String webSecurityMode) { this.webSecurityMode = webSecurityMode; }
    // Getters
    public String getBrowserName() { return browserName; }
    public String getHeadlessMode() { return headlessMode; }
    public String getPageLoadStrategy() { return pageLoadStrategy; }
    public String getPrivateMode() { return privateMode; }
    public String getSandboxMode() { return sandboxMode; }
    public String getWebSecurityMode() { return webSecurityMode; }
}
