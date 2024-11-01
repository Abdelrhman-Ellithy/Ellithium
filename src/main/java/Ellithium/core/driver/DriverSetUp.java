package Ellithium.core.driver;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

public class DriverSetUp {

    // Private method to configure browser options and return the WebDriver instance
    public static WebDriver setupLocalDriver(String browserName, String headlessMode, String pageLoadStrategy, String privateMode, String sandboxMode, String webSecurityMode) {
        switch (browserName.toLowerCase()) {
            case "chrome":
                ChromeOptions chromeOptions = configureChromeOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                return new ChromeDriver(chromeOptions);
            case "firefox":
                FirefoxOptions firefoxOptions = configureFirefoxOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                return new FirefoxDriver(firefoxOptions);
            case "edge":
                EdgeOptions edgeOptions = configureEdgeOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                return new EdgeDriver(edgeOptions);
            case "safari":
                SafariOptions safariOptions = configureSafariOptions(pageLoadStrategy, privateMode);
                return new SafariDriver(safariOptions);
            default:
                throw new IllegalArgumentException("Invalid browser: " + browserName);
        }
    }
    // Configure Chrome options
    // The Options commented to enable Developer tools
    private static ChromeOptions configureChromeOptions(String headlessMode, String pageLoadStrategy, String privateMode, String sandboxMode, String webSecurityMode) {
        ChromeOptions chromeOptions = new ChromeOptions();
        if ("true".equalsIgnoreCase(headlessMode)) {
            chromeOptions.addArguments("--headless");
        }
        if ("eager".equalsIgnoreCase(pageLoadStrategy)) {
            chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if ("true".equalsIgnoreCase(privateMode)) {
            chromeOptions.addArguments("--incognito");
        }
        if ("nosandbox".equalsIgnoreCase(sandboxMode)) {
            chromeOptions.addArguments("--no-sandbox");
        }
        if ("false".equalsIgnoreCase(webSecurityMode)) {
            chromeOptions.addArguments("--disable-web-security");
            chromeOptions.addArguments("--allow-running-insecure-content");
        }
        // Other common options
        chromeOptions.addArguments(
                  "--disable-dev-shm-usage"
                , "--disable-search-engine-choice-screen"
                , "--remote-allow-origins=*"
                , "--enable-automation"
                , "--disable-background-timer-throttling"
                , "--disable-backgrounding-occluded-windows"
                , "--disable-features=OptimizationGuideModelDownloading,OptimizationHintsFetching,OptimizationTargetPrediction,OptimizationHints,CalculateNativeWinOcclusion,AutofillServerCommunication,MediaRouter,Translate,AvoidUnnecessaryBeforeUnloadCheckSync,CertificateTransparencyComponentUpdater,OptimizationHints,DialMediaRouteProvider,GlobalMediaControls,ImprovedCookieControls,LazyFrameLoading,InterestFeedContentSuggestions"
                , "--disable-hang-monitor"
                , "--disable-domain-reliability"
                , "--disable-renderer-backgrounding"
                , "--metrics-recording-only"
                , "--no-first-run"
                , "--no-default-browser-check"
               // , "--silent-debugger-extension-api"
              //  , "--disable-extensions"
                , "--disable-component-extensions-with-background-pages"
                , "--disable-ipc-flooding-protection"
                , "--disable-background-networking"
                , "--mute-audio"
                , "--disable-breakpad"
                , "--ignore-certificate-errors"
                , "--disable-device-discovery-notifications"
                , "--force-color-profile=srgb"
                , "--hide-scrollbars"
                , "--host-resolver-rules"
                , "--no-pings"
                , "--disable-sync"
                , "--disable-field-trial-config"
                , "--enable-features=NetworkService"
                , "--enable-features=NetworkServiceInProcess"
                , "--enable-use-zoom-for-dsf"
                , "--log-net-log"
                , "--net-log-capture-mode"
                , "--disable-client-side-phishing-detection"
                , "--disable-default-apps"
                ,"--disable-software-rasterizer"
               // ,"--single-process"
                ,"--disable-infobars"
                ,"--window-size=1920,1080"
                ,"--disable-notifications"
                ,"--disable-background-networking"
                ,"--disable-translate"
                ,"--disable-sync-preferences"
                ,"--dns-prefetch-disable"
                ,"--disable-blink-features=AutomationControlled"
                ,"--disable-pinch"
                ,"--disable-background-tasks"
                ,"--disable-component-update"
                ,"--enable-logging"
                ,"--disable-plugins"
                ,"--ash-disable-system-sounds"

        );
        Reporter.log(  "Chrome Options Configured" , LogLevel.INFO_GREEN);
        return chromeOptions;
    }

    // Configure Firefox options
    private static FirefoxOptions configureFirefoxOptions(String headlessMode, String pageLoadStrategy, String privateMode, String sandboxMode, String webSecurityMode) {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        if ("true".equalsIgnoreCase(headlessMode)) {
            firefoxOptions.addArguments("--headless");
        }
        if ("eager".equalsIgnoreCase(pageLoadStrategy)) {
            firefoxOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if ("true".equalsIgnoreCase(privateMode)) {
            firefoxOptions.addArguments("--private");
        }
        if ("false".equalsIgnoreCase(sandboxMode)) {
            firefoxOptions.addArguments("--no-sandbox");
        }
        if ("false".equalsIgnoreCase(webSecurityMode)) {
            firefoxOptions.addPreference("security.mixed_content.block_active_content", false);
        }
        // Other common options
        firefoxOptions.addArguments(
                "--disable-dev-shm-usage",  // Reduce memory usage in environments with low shared memory
                "--enable-automation",  // Enable automation control
                "--disable-background-timer-throttling",  // Disable throttling for background tabs
                "--disable-backgrounding-occluded-windows",  // Prevent backgrounding of occluded windows
                "--disable-hang-monitor",  // Disable hang monitor
                "--disable-domain-reliability",  // Disable domain reliability monitoring
                "--metrics-recording-only",  // Record metrics only (for debugging)
                "--no-first-run",  // Skip first run setup
                "--no-default-browser-check",  // Avoid default browser prompt
              //  "--disable-extensions",  // Disable all extensions
              //  "--disable-background-networking",  // Disable background networking
                "--mute-audio",  // Mute audio
                "--ignore-certificate-errors",  // Ignore SSL certificate errors
           //     "--force-color-profile=srgb",  // Use sRGB color profile
                "--hide-scrollbars",  // Hide scrollbars
                "--disable-sync",  // Disable Firefox sync
                "--disable-client-side-phishing-detection",  // Disable phishing detection (specific to Chrome but similar behavior in Firefox)
                "--disable-default-apps",  // Prevent loading of default apps (not entirely applicable to Firefox, but similar logic)
                "--disable-notifications",  // Disable notifications
                "--window-size=1920,1080",  // Set window size
                "--disable-plugins"  // Disable plugins (extensions can be disabled with similar options)
               // "--single-process"  // Use single-process mode
        );
        Reporter.log(  "Firefox Options Configured", LogLevel.INFO_GREEN);
        return firefoxOptions;
    }

    // Configure Edge options
    private static EdgeOptions configureEdgeOptions(String headlessMode, String pageLoadStrategy, String privateMode, String sandboxMode, String webSecurityMode) {
        EdgeOptions edgeOptions = new EdgeOptions();
        if ("true".equalsIgnoreCase(headlessMode)) {
            edgeOptions.addArguments("--headless");
        }
        if ("eager".equalsIgnoreCase(pageLoadStrategy)) {
            edgeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if ("true".equalsIgnoreCase(privateMode)) {
            edgeOptions.addArguments("--inPrivate");
        }
        if ("false".equalsIgnoreCase(sandboxMode)) {
            edgeOptions.addArguments("--no-sandbox");
        }
        if ("false".equalsIgnoreCase(webSecurityMode)) {
            edgeOptions.addArguments("--disable-web-security");
            edgeOptions.addArguments("--allow-running-insecure-content");
        }
        // Other common options
        edgeOptions.addArguments(
                "--disable-dev-shm-usage",  // Reduce memory usage
                "--disable-search-engine-choice-screen",  // Edge equivalent to Chrome's search engine choice
                "--remote-allow-origins=*",  // Allow cross-origin requests
                "--enable-automation",  // Enable automation control
                "--disable-background-timer-throttling",  // Optimize background tab performance
                "--disable-backgrounding-occluded-windows",  // Avoid backgrounding windows not in focus
                "--disable-hang-monitor",  // Disable hang monitor
                "--disable-domain-reliability",  // Disable domain reliability checks
                "--disable-renderer-backgrounding",  // Prevent renderer from backgrounding
                "--metrics-recording-only",  // Only record metrics
                "--no-first-run",  // Skip the first run experience
                "--no-default-browser-check",  // Prevent the default browser check
               // "--silent-debugger-extension-api",  // Silent extensions for debugging
              //  "--disable-extensions",  // Disable all extensions
                "--disable-component-extensions-with-background-pages",  // Disable extensions with background pages
                "--disable-ipc-flooding-protection",  // Disable IPC flooding protection
                "--disable-background-networking",  // Disable background network connections
                "--mute-audio",  // Mute audio
                "--disable-breakpad",  // Disable crash reporting
                "--ignore-certificate-errors",  // Ignore SSL certificate errors
                "--disable-device-discovery-notifications",  // Disable device discovery notifications
                "--force-color-profile=srgb",  // Force sRGB color profile
                "--hide-scrollbars",  // Hide scrollbars
                "--no-pings",  // Disable ping requests
                "--disable-sync",  // Disable syncing with Microsoft account
                "--disable-features=Translate",  // Disable the translate feature
                "--enable-features=NetworkService",  // Enable network service feature
                "--enable-features=NetworkServiceInProcess",  // Enable network service in process
                "--enable-use-zoom-for-dsf",  // Enable zooming for display scaling
                "--disable-client-side-phishing-detection",  // Disable phishing detection
                "--disable-default-apps",  // Disable default apps
                "--disable-software-rasterizer",  // Disable software-based rendering
               // "--single-process",  // Use single process for the browser
                "--disable-infobars",  // Disable infobars
                "--window-size=1920,1080",  // Set window size
                "--disable-notifications",  // Disable notifications
                "--dns-prefetch-disable",  // Disable DNS prefetching
                "--disable-blink-features=AutomationControlled",  // Hide automation control features
                "--disable-pinch",  // Disable pinch to zoom
                "--disable-background-tasks",  // Disable background tasks
                "--disable-component-update",  // Disable component updates
                "--enable-logging",  // Enable logging
                "--disable-plugins",  // Disable plugins
                "--ash-disable-system-sounds"  // Disable system sounds
        );
        Reporter.log( "Edge Options Configured", LogLevel.INFO_GREEN);
        return edgeOptions;
    }
    // Configure Safari options
    private static SafariOptions configureSafariOptions(String pageLoadStrategy, String privateMode) {
        SafariOptions safariOptions = new SafariOptions();
        if ("eager".equalsIgnoreCase(pageLoadStrategy)) {
            safariOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if ("true".equalsIgnoreCase(privateMode)) {
            safariOptions.setUseTechnologyPreview(true); // Safari doesn't have "private" mode via arguments, but this simulates it
        }
        Reporter.log( "Safari Options Configured", LogLevel.INFO_GREEN);
        return safariOptions;
    }

}