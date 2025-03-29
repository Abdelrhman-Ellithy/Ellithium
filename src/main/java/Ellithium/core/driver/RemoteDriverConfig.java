package Ellithium.core.driver;

import org.openqa.selenium.Capabilities;
import java.net.URL;

/**
 * Configuration class for remote WebDriver instances.
 * This class implements DriverConfigBuilder and manages remote browser configurations.
 * Default values:
 * - HeadlessMode: false (Not supported with Safari)
 * - PageLoadStrategy: Normal
 * - PrivateMode: false
 * - SandboxMode: Sandbox (Not supported with Safari)
 * - WebSecurityMode: true
 * - RemoteDriverType: Chrome
 */
public class RemoteDriverConfig implements DriverConfigBuilder {

    private RemoteDriverType driverType;
    private URL remoteAddress;
    private Capabilities capabilities;
    private PrivateMode privateMode;
    private SandboxMode sandboxMode;
    private WebSecurityMode webSecurityMode;
    private PageLoadStrategyMode pageLoadStrategyMode;
    private HeadlessMode headlessMode;

    /**
     * Default constructor for RemoteDriverConfig.
     * Sets all configurations to their default values:
     * - HeadlessMode: false
     * - PageLoadStrategy: Normal
     * - PrivateMode: false
     * - SandboxMode: Sandbox
     * - WebSecurityMode: true
     * - RemoteDriverType: Chrome
     * Note: remoteAddress and capabilities must be set separately as they are mandatory
     */
    public RemoteDriverConfig() {
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
        setDriverType(RemoteDriverType.REMOTE_Chrome);
    }

    /**
     * Constructs a RemoteDriverConfig with mandatory parameters.
     *
     * @param driverType Type of remote driver
     * @param remoteAddress URL of the remote server
     * @param capabilities WebDriver capabilities
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a RemoteDriverConfig with all configuration options.
     *
     * @param driverType Type of remote driver to be used
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities
     * @param headlessMode Browser headless mode configuration
     * @param privateMode Browser private/incognito mode configuration
     * @param pageLoadStrategyMode Strategy for handling page load events
     * @param webSecurityMode Browser web security policy configuration
     * @param sandboxMode Browser sandbox security configuration
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities,
            HeadlessMode headlessMode, PrivateMode privateMode, PageLoadStrategyMode pageLoadStrategyMode,
            WebSecurityMode webSecurityMode, SandboxMode sandboxMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(headlessMode);
        setPrivateMode(privateMode);
        setPageLoadStrategy(pageLoadStrategyMode);
        setWebSecurityMode(webSecurityMode);
        setSandboxMode(sandboxMode);
    }

    /**
     * Constructs a RemoteDriverConfig with a private mode configuration.
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param privateMode Browser private/incognito mode configuration
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
            PrivateMode privateMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(privateMode);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a RemoteDriverConfig with a sandbox mode configuration.
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param sandboxMode Browser sandbox security configuration
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
            SandboxMode sandboxMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(sandboxMode);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a RemoteDriverConfig with a web security mode configuration.
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param webSecurityMode Browser web security policy configuration
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
            WebSecurityMode webSecurityMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(webSecurityMode);
    }

    /**
     * Constructs a RemoteDriverConfig with a page load strategy configuration.
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param pageLoadStrategyMode Strategy for handling page load events (normal, eager, or none)
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
           PageLoadStrategyMode pageLoadStrategyMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(pageLoadStrategyMode);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a RemoteDriverConfig with a headless mode configuration.
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param headlessMode Browser headless mode configuration for running without GUI
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
            HeadlessMode headlessMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(headlessMode);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs a RemoteDriverConfig with private and sandbox mode configurations.
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param privateMode Browser private/incognito mode configuration
     * @param sandboxMode Browser sandbox security configuration
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
            PrivateMode privateMode, SandboxMode sandboxMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(privateMode);
        setSandboxMode(sandboxMode);
        setWebSecurityMode(WebSecurityMode.SecureMode);
    }

    /**
     * Constructs with mandatory parameters plus private mode and web security mode
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param privateMode Browser private/incognito mode configuration
     * @param webSecurityMode Browser web security policy configuration
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
            PrivateMode privateMode, WebSecurityMode webSecurityMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(privateMode);
        setSandboxMode(SandboxMode.Sandbox);
        setWebSecurityMode(webSecurityMode);
    }

    /**
     * Constructs with mandatory parameters plus sandbox mode and web security mode
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param sandboxMode Browser sandbox security configuration
     * @param webSecurityMode Browser web security policy configuration
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
            SandboxMode sandboxMode, WebSecurityMode webSecurityMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(PageLoadStrategyMode.Normal);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(sandboxMode);
        setWebSecurityMode(webSecurityMode);
    }

    /**
     * Constructs with mandatory parameters plus sandbox mode, web security mode, and page load strategy
     *
     * @param driverType Type of remote driver to be used (e.g., Chrome, Firefox)
     * @param remoteAddress URL of the remote WebDriver server
     * @param capabilities Browser-specific capabilities and settings
     * @param sandboxMode Browser sandbox security configuration
     * @param webSecurityMode Browser web security policy configuration
     * @param pageLoadStrategyMode Strategy for handling page load events (normal, eager, or none)
     */
    public RemoteDriverConfig(RemoteDriverType driverType, URL remoteAddress, Capabilities capabilities, 
            SandboxMode sandboxMode, WebSecurityMode webSecurityMode,PageLoadStrategyMode pageLoadStrategyMode) {
        setDriverType(driverType);
        setRemoteAddress(remoteAddress);
        setCapabilities(capabilities);
        setHeadlessMode(HeadlessMode.False);
        setPageLoadStrategy(pageLoadStrategyMode);
        setPrivateMode(PrivateMode.False);
        setSandboxMode(sandboxMode);
        setWebSecurityMode(webSecurityMode);
    }

    // Getters and Setters
    /**
     * Gets the remote driver type.
     * Default is CHROME.
     * 
     * @return The current remote driver type configuration or default CHROME if not set
     */
    public RemoteDriverType getRemoteDriverType() {
        return driverType != null ? driverType : RemoteDriverType.REMOTE_Chrome;
    }

    /**
     * Sets the remote driver type.
     * 
     * @param driverType The remote driver type to configure
     */
    public void setRemoteDriverType(RemoteDriverType driverType) {
        this.driverType = driverType;
    }

    /**
     * Gets the remote server address.
     * 
     * @return The URL of the remote server
     */
    public URL getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Sets the remote server address.
     * 
     * @param remoteAddress The URL of the remote server to connect to
     */
    public void setRemoteAddress(URL remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * Gets the private mode setting.
     * Default is FALSE.
     * 
     * @return The current private mode configuration or default FALSE if not set
     */
    public PrivateMode getPrivateMode() {
        return privateMode != null ? privateMode : PrivateMode.False;
    }

    /**
     * Sets the private mode setting.
     * 
     * @param privateMode The private mode configuration to set
     */
    public void setPrivateMode(PrivateMode privateMode) {
        this.privateMode = privateMode;
    }

    /**
     * Gets the sandbox mode setting.
     * Default is SANDBOX.
     * 
     * @return The current sandbox mode configuration or default SANDBOX if not set
     */
    public SandboxMode getSandboxMode() {
        return sandboxMode != null ? sandboxMode : SandboxMode.Sandbox;
    }

    /**
     * Sets the sandbox mode setting.
     * 
     * @param sandboxMode The sandbox mode configuration to set
     */
    public void setSandboxMode(SandboxMode sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    /**
     * Gets the web security mode setting.
     * Default is SecureMode.
     * 
     * @return The current web security mode configuration or default SecureMode if not set
     */
    public WebSecurityMode getWebSecurityMode() {
        return webSecurityMode != null ? webSecurityMode : WebSecurityMode.SecureMode;
    }

    /**
     * Sets the web security mode setting.
     * 
     * @param webSecurityMode The web security mode configuration to set
     */
    public void setWebSecurityMode(WebSecurityMode webSecurityMode) {
        this.webSecurityMode = webSecurityMode;
    }

    /**
     * Gets the page load strategy.
     * Default is NORMAL.
     * 
     * @return The current page load strategy configuration or default NORMAL if not set
     */
    public PageLoadStrategyMode getPageLoadStrategy() {
        return pageLoadStrategyMode != null ? pageLoadStrategyMode : PageLoadStrategyMode.Normal;
    }

    /**
     * Sets the page load strategy.
     * 
     * @param pageLoadStrategyMode The page load strategy configuration to set
     */
    public void setPageLoadStrategy(PageLoadStrategyMode pageLoadStrategyMode) {
        this.pageLoadStrategyMode = pageLoadStrategyMode;
    }

    /**
     * Gets the headless mode setting.
     * Default is FALSE.
     * 
     * @return The current headless mode configuration or default FALSE if not set
     */
    public HeadlessMode getHeadlessMode() {
        return headlessMode != null ? headlessMode : HeadlessMode.False;
    }

    /**
     * Sets the headless mode setting.
     * 
     * @param headlessMode The headless mode configuration to set
     */
    public void setHeadlessMode(HeadlessMode headlessMode) {
        this.headlessMode = headlessMode;
    }

    /**
     * Sets the WebDriver capabilities.
     * 
     * @param capabilities The WebDriver capabilities to configure
     */
    @Override
    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Sets the driver type.
     * 
     * @param driverType The driver type to configure, must be instance of RemoteDriverType
     */
    @Override
    public void setDriverType(DriverType driverType) {
        this.driverType = (RemoteDriverType) driverType;
    }

    /**
     * Gets the WebDriver capabilities.
     * 
     * @return The current WebDriver capabilities configuration
     */
    @Override
    public Capabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Gets the driver type.
     * 
     * @return The current driver type configuration
     */
    @Override
    public DriverType getDriverType() {
        return driverType;
    }
}