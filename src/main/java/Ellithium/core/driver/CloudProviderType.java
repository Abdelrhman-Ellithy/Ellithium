package Ellithium.core.driver;

/**
 * Enumeration of supported cloud device testing providers.
 * Each provider has specific endpoints and authentication requirements.
 *
 * <p>Supported Providers:
 * <ul>
 *   <li>BrowserStack - <a href="https://www.browserstack.com/">...</a></li>
 *   <li>Sauce Labs - <a href="https://saucelabs.com/">...</a></li>
 *   <li>LambdaTest - <a href="https://www.lambdatest.com/">...</a></li>
 *   <li>Local - For local Appium server</li>
 * </ul>
 */
public enum CloudProviderType {
    /**
     * BrowserStack cloud testing platform.
     * Default hub URL: <a href="https://hub-cloud.browserstack.com/wd/hub">...</a>
     */
    BROWSERSTACK("hub-cloud.browserstack.com", "/wd/hub", true),

    /**
     * Sauce Labs cloud testing platform.
     * Default hub URLs:
     * - US West: ondemand.us-west-1.saucelabs.com
     * - US East: ondemand.us-east-4.saucelabs.com
     * - EU Central: ondemand.eu-central-1.saucelabs.com
     */
    SAUCE_LABS("ondemand.us-west-1.saucelabs.com", "/wd/hub", true),

    /**
     * LambdaTest cloud testing platform.
     * Default hub URL: <a href="https://mobile-hub.lambdatest.com/wd/hub">...</a>
     */
    LAMBDATEST("mobile-hub.lambdatest.com", "/wd/hub", true),

    /**
     * Local Appium server.
     * Default URL: <a href="http://127.0.0.1:4723">...</a>
     */
    LOCAL("127.0.0.1:4723", "", false);

    private final String defaultHost;
    private final String hubPath;
    private final boolean requiresAuth;

    /**
     * Constructor for CloudProviderType.
     *
     * @param defaultHost The default host for the provider
     * @param hubPath The hub endpoint path
     * @param requiresAuth Whether the provider requires authentication
     */
    CloudProviderType(String defaultHost, String hubPath, boolean requiresAuth) {
        this.defaultHost = defaultHost;
        this.hubPath = hubPath;
        this.requiresAuth = requiresAuth;
    }

    /**
     * Gets the default host for this provider.
     *
     * @return The default host string
     */
    public String getDefaultHost() {
        return defaultHost;
    }

    /**
     * Gets the hub endpoint path for this provider.
     *
     * @return The hub path string
     */
    public String getHubPath() {
        return hubPath;
    }

    /**
     * Checks if this provider requires authentication.
     *
     * @return true if authentication is required, false otherwise
     */
    public boolean requiresAuth() {
        return requiresAuth;
    }

    /**
     * Constructs the full hub URL with authentication if required.
     *
     * @param username The username for authentication (ignored for LOCAL)
     * @param accessKey The access key for authentication (ignored for LOCAL)
     * @return The complete hub URL
     */
    public String getHubUrl(String username, String accessKey) {
        if (requiresAuth && username != null && accessKey != null) {
            return "https://" + username + ":" + accessKey + "@" + defaultHost + hubPath;
        } else if (this == LOCAL) {
            return "http://" + defaultHost;
        } else {
            return "https://" + defaultHost + hubPath;
        }
    }

    /**
     * Constructs the full hub URL with custom host and authentication.
     *
     * @param customHost The custom host to use instead of default
     * @param username The username for authentication
     * @param accessKey The access key for authentication
     * @return The complete hub URL
     */
    public String getHubUrl(String customHost, String username, String accessKey) {
        String host = (customHost != null && !customHost.isEmpty()) ? customHost : defaultHost;

        if (requiresAuth && username != null && accessKey != null) {
            return "https://" + username + ":" + accessKey + "@" + host + hubPath;
        } else if (this == LOCAL) {
            return "http://" + host;
        } else {
            return "https://" + host + hubPath;
        }
    }
}