package Ellithium.core.driver;

import org.openqa.selenium.Capabilities;

/**
 * Configuration class for local WebDriver instances.
 * This class implements DriverConfigBuilder and manages various browser configurations.
 * Default values:
 * - HeadlessMode: false (Not supported with Safari)
 * - PageLoadStrategy: Normal
 * - PrivateMode: false
 * - SandboxMode: Sandbox (Not supported with Safari)
 * - WebSecurityMode: true
 * - LocalDriverType: Chrome
 */
public class LocalDriverConfig implements DriverConfigBuilder {


    private PrivateMode privateMode;
    private SandboxMode sandboxMode;
    private WebSecurityMode webSecurityMode;
    private PageLoadStrategyMode pageLoadStrategyMode;
    private HeadlessMode headlessMode;
    private LocalDriverType localDriverType;
    private Capabilities capabilities;


    /**
     * Constructs a LocalDriverConfig with all available configuration options.
     *
     * @param localDriverType Type of local driver to be used
     * @param headlessMode Headless mode setting
     * @param privateMode Browser private mode setting
     * @param pageLoadStrategyMode Page load strategy to be used
     * @param webSecurityMode Web security mode setting
     * @param sandboxMode Browser sandbox mode setting
     */
    public LocalDriverConfig(LocalDriverType localDriverType, HeadlessMode headlessMode, 
            PrivateMode privateMode,  PageLoadStrategyMode pageLoadStrategyMode,
            WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        setLocalDriverType(localDriverType);
        setHeadlessMode(headlessMode);
        setPrivateMode(privateMode);
        setPageLoadStrategy(pageLoadStrategyMode);
        setWebSecurityMode(webSecurityMode);
        setSandboxMode(sandboxMode);
    }

    /**
     * Constructs a LocalDriverConfig with all available configuration options including capabilities.
     */
    public LocalDriverConfig(LocalDriverType localDriverType, Capabilities capabilities,
            HeadlessMode headlessMode, PrivateMode privateMode,  PageLoadStrategyMode pageLoadStrategyMode,
            WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        setLocalDriverType(localDriverType);
        setCapabilities(capabilities);
        setHeadlessMode(headlessMode);
        setPrivateMode(privateMode);
        setPageLoadStrategy(pageLoadStrategyMode);
        setWebSecurityMode(webSecurityMode);
        setSandboxMode(sandboxMode);
    }

    /**
     * Constructs a LocalDriverConfig with sandbox mode, web security mode, page load strategy, headless mode, and local driver type.
     *
     * @param localDriverType Type of local driver to be used
     * @param headlessMode Headless mode setting
     * @param pageLoadStrategyMode Page load strategy to be used
     * @param webSecurityMode Web security mode setting
     * @param sandboxMode Browser sandbox mode setting
     */
    public LocalDriverConfig(LocalDriverType localDriverType, HeadlessMode headlessMode, 
             PageLoadStrategyMode pageLoadStrategyMode, WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        setLocalDriverType(localDriverType);
        setHeadlessMode(headlessMode);
        setPageLoadStrategy(pageLoadStrategyMode);
        setWebSecurityMode(webSecurityMode);
        setSandboxMode(sandboxMode);
    }

    /**
     * Constructs a LocalDriverConfig with web security mode, page load strategy, headless mode, and local driver type.
     *
     * @param localDriverType Type of local driver to be used
     * @param headlessMode Headless mode setting
     * @param pageLoadStrategyMode Page load strategy to be used
     * @param webSecurityMode Web security mode setting
     */
    public LocalDriverConfig(LocalDriverType localDriverType, HeadlessMode headlessMode, 
             PageLoadStrategyMode pageLoadStrategyMode, WebSecurityMode webSecurityMode) {
        setLocalDriverType(localDriverType);
        setHeadlessMode(headlessMode);
        setPageLoadStrategy(pageLoadStrategyMode);
        setWebSecurityMode(webSecurityMode);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
    }

    /**
     * Constructs a LocalDriverConfig with page load strategy, headless mode, and local driver type.
     *
     * @param localDriverType Type of local driver to be used
     * @param headlessMode Headless mode setting
     * @param pageLoadStrategyMode Page load strategy to be used
     */
    public LocalDriverConfig(LocalDriverType localDriverType, HeadlessMode headlessMode, 
             PageLoadStrategyMode pageLoadStrategyMode) {
        setLocalDriverType(localDriverType);
        setHeadlessMode(headlessMode);
        setPageLoadStrategy(pageLoadStrategyMode);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a LocalDriverConfig with headless mode and local driver type.
     *
     * @param localDriverType Type of local driver to be used
     * @param headlessMode Headless mode setting
     */
    public LocalDriverConfig(LocalDriverType localDriverType, HeadlessMode headlessMode) {
        setLocalDriverType(localDriverType);
        setHeadlessMode(headlessMode);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a LocalDriverConfig with local driver type.
     *
     * @param localDriverType Type of local driver to be used
     */
    public LocalDriverConfig(LocalDriverType localDriverType) {
        setLocalDriverType(localDriverType);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a LocalDriverConfig with local driver type and capabilities.
     *
     * @param localDriverType Type of local driver to be used
     * @param capabilities WebDriver capabilities
     */
    public LocalDriverConfig(LocalDriverType localDriverType, Capabilities capabilities) {
        setLocalDriverType(localDriverType);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a LocalDriverConfig with capabilities and headless mode.
     */
    public LocalDriverConfig(LocalDriverType localDriverType, Capabilities capabilities, 
            HeadlessMode headlessMode) {
        setLocalDriverType(localDriverType);
        setCapabilities(capabilities);
        setHeadlessMode(headlessMode);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a LocalDriverConfig with capabilities and page load strategy.
     */
    public LocalDriverConfig(LocalDriverType localDriverType, Capabilities capabilities, 
             PageLoadStrategyMode pageLoadStrategyMode) {
        setLocalDriverType(localDriverType);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(pageLoadStrategyMode);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a LocalDriverConfig with capabilities and private mode.
     */
    public LocalDriverConfig(LocalDriverType localDriverType, Capabilities capabilities, 
            PrivateMode privateMode) {
        setLocalDriverType(localDriverType);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(privateMode);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Default constructor for LocalDriverConfig.
     * Sets all configurations to their default values:
     * - HeadlessMode: false
     * - PageLoadStrategy: Normal
     * - PrivateMode: false
     * - SandboxMode: Sandbox
     * - WebSecurityMode: true
     * - LocalDriverType: Chrome
     */
    public LocalDriverConfig() {
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
        setLocalDriverType(LocalDriverType.Chrome);
    }

    /**
     * Constructs a LocalDriverConfig with private mode and local driver type.
     *
     * @param privateMode Browser private mode setting
     * @param localDriverType Type of local driver to be used
     */
    public LocalDriverConfig(PrivateMode privateMode, LocalDriverType localDriverType) {
        setPrivateMode(privateMode);
        setLocalDriverType(localDriverType);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a LocalDriverConfig with sandbox mode and local driver type.
     *
     * @param sandboxMode Browser sandbox mode setting
     * @param localDriverType Type of local driver to be used
     */
    public LocalDriverConfig(SandboxMode sandboxMode, LocalDriverType localDriverType) {
        setSandboxMode(sandboxMode);
        setLocalDriverType(localDriverType);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a LocalDriverConfig with web security mode and local driver type.
     *
     * @param webSecurityMode Web security mode setting
     * @param localDriverType Type of local driver to be used
     */
    public LocalDriverConfig(WebSecurityMode webSecurityMode, LocalDriverType localDriverType) {
        setWebSecurityMode(webSecurityMode);
        setLocalDriverType(localDriverType);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
    }

    /**
     * Constructs a LocalDriverConfig with page load strategy and local driver type.
     *
     * @param pageLoadStrategyMode Page load strategy to be used
     * @param localDriverType Type of local driver to be used
     */
    public LocalDriverConfig(PageLoadStrategyMode pageLoadStrategyMode, LocalDriverType localDriverType) {
        setPageLoadStrategy(pageLoadStrategyMode);
        setLocalDriverType(localDriverType);
        setHeadlessMode(HeadlessMode.False);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Gets the private mode setting.
     * Default is FALSE.
     *
     * @return The current private mode setting
     */
    public PrivateMode getPrivateMode() {
        return privateMode != null ? privateMode : PrivateMode.False;
    }

    /**
     * Sets the private mode setting.
     *
     * @param privateMode The private mode to set
     */
    public void setPrivateMode(PrivateMode privateMode) {
        this.privateMode = privateMode;
    }

    /**
     * Gets the sandbox mode setting.
     * Default is SANDBOX.
     *
     * @return The current sandbox mode setting
     */
    public SandboxMode getSandboxMode() {
        return sandboxMode != null ? sandboxMode : SandboxMode.Sandbox;
    }

    /**
     * Sets the sandbox mode setting.
     *
     * @param sandboxMode The sandbox mode to set
     */
    public void setSandboxMode(SandboxMode sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    /**
     * Gets the web security mode setting.
     * Default is TRUE.
     *
     * @return The current web security mode setting
     */
    public WebSecurityMode getWebSecurityMode() {
        return webSecurityMode != null ? webSecurityMode : WebSecurityMode.SecureMode;
    }

    /**
     * Sets the web security mode setting.
     *
     * @param webSecurityMode The web security mode to set
     */
    public void setWebSecurityMode(WebSecurityMode webSecurityMode) {
        this.webSecurityMode = webSecurityMode;
    }

    /**
     * Gets the page load strategy.
     * Default is NORMAL.
     *
     * @return The current page load strategy
     */
    public PageLoadStrategyMode getPageLoadStrategy() {
        return pageLoadStrategyMode != null ? pageLoadStrategyMode : PageLoadStrategyMode.Normal;
    }

    /**
     * Sets the page load strategy.
     *
     * @param pageLoadStrategyMode The page load strategy to set
     */
    public void setPageLoadStrategy( PageLoadStrategyMode pageLoadStrategyMode) {
        this.pageLoadStrategyMode = pageLoadStrategyMode;
    }

    /**
     * Gets the headless mode setting.
     * Default is FALSE.
     *
     * @return The current headless mode setting
     */
    public HeadlessMode getHeadlessMode() {
        return headlessMode != null ? headlessMode : HeadlessMode.False;
    }

    /**
     * Sets the headless mode setting.
     *
     * @param headlessMode The headless mode to set
     */
    public void setHeadlessMode(HeadlessMode headlessMode) {
        this.headlessMode = headlessMode;
    }

    /**
     * Gets the local driver type.
     * Default is CHROME.
     *
     * @return The current local driver type
     */
    public LocalDriverType getLocalDriverType() {
        return localDriverType != null ? localDriverType : LocalDriverType.Chrome;
    }

    /**
     * Sets the local driver type.
     *
     * @param localDriverType The local driver type to set
     */
    public void setLocalDriverType(LocalDriverType localDriverType) {
        this.localDriverType = localDriverType;
    }

    /**
     * Sets the WebDriver capabilities.
     *
     * @param capabilities The capabilities to set
     */
    @Override
    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Sets the driver type.
     *
     * @param driverType The driver type to set
     */
    @Override
    public LocalDriverConfig setDriverType(DriverType driverType) {
        this.localDriverType = (LocalDriverType)driverType;
        return this;
    }

    /**
     * Gets the WebDriver capabilities.
     *
     * @return The current capabilities
     */
    @Override
    public Capabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Gets the driver type.
     *
     * @return The current driver type
     */
    @Override
    public DriverType getDriverType() {
        return null;
    }
}
