package Ellithium.core.driver;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import java.net.URL;
import java.util.List;

import static Ellithium.core.driver.LocalDriverType.*;
import static Ellithium.core.driver.RemoteDriverType.*;

public class BrowserSetUp {

    public static WebDriver setupLocalDriver(DriverType driverType, HeadlessMode headlessMode, PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode, SandboxMode sandboxMode, WebSecurityMode webSecurityMode) {
        var capabilities=ConfigContext.getCapabilities();
        switch (driverType) {
            case Chrome -> {
                ChromeOptions chromeOptions = configureChromeOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                if (capabilities != null) chromeOptions=chromeOptions.merge(capabilities);
                return new ChromeDriver(chromeOptions);
            }
            case FireFox -> {
                FirefoxOptions firefoxOptions = configureFirefoxOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                if (capabilities != null) firefoxOptions=firefoxOptions.merge(capabilities);
                return new FirefoxDriver(firefoxOptions);
            }
            case Edge -> {
                EdgeOptions edgeOptions = configureEdgeOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                if (capabilities != null) edgeOptions=edgeOptions.merge(capabilities);
                return new EdgeDriver(edgeOptions);
            }
            case Safari -> {
                SafariOptions safariOptions = configureSafariOptions(pageLoadStrategy, privateMode);
                if (capabilities != null) safariOptions=safariOptions.merge(capabilities);
                return new SafariDriver(safariOptions);
            }
            default -> {
                return null;
            }
        }
    }
    public static RemoteWebDriver setupRemoteDriver(DriverType driverType, URL remoteAddress, Capabilities capabilities, HeadlessMode headlessMode, PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode, SandboxMode sandboxMode, WebSecurityMode webSecurityMode) {
        RemoteWebDriver driver;
        switch (driverType) {
            case REMOTE_Chrome -> {
                ChromeOptions chromeOptions = configureChromeOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                capabilities.merge(chromeOptions);
                driver = new RemoteWebDriver(remoteAddress, capabilities);
            }
            case REMOTE_FireFox -> {
                FirefoxOptions firefoxOptions = configureFirefoxOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                capabilities.merge(firefoxOptions);
                driver = new RemoteWebDriver(remoteAddress, capabilities);
            }
            case REMOTE_Edge -> {
                EdgeOptions edgeOptions = configureEdgeOptions(headlessMode, pageLoadStrategy, privateMode, sandboxMode, webSecurityMode);
                capabilities.merge(edgeOptions);
                driver = new RemoteWebDriver(remoteAddress, edgeOptions);
            }
            case REMOTE_Safari -> {
                SafariOptions safariOptions = configureSafariOptions(pageLoadStrategy, privateMode);
                capabilities.merge(safariOptions);
                driver = new RemoteWebDriver(remoteAddress, safariOptions);
            }
            default -> {
                return null;
            }
        }
        driver.setFileDetector(new LocalFileDetector());
        return driver;
    }
    private static ChromeOptions configureChromeOptions(HeadlessMode headlessMode, PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode, SandboxMode sandboxMode, WebSecurityMode webSecurityMode) {
        ChromeOptions chromeOptions = new ChromeOptions();
        boolean bidi=true;
        if (headlessMode==HeadlessMode.True) {
            chromeOptions.addArguments("--headless");
        }
        if (pageLoadStrategy==PageLoadStrategyMode.Eager) {
            chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if (privateMode==PrivateMode.True) {
            chromeOptions.addArguments("--incognito");
            bidi=false;
        }
        if (sandboxMode==SandboxMode.NoSandboxMode) {
            chromeOptions.addArguments("--no-sandbox");
        }
        if (webSecurityMode==WebSecurityMode.AllowUnsecure) {
            chromeOptions.addArguments("--disable-web-security");
            chromeOptions.addArguments("--allow-running-insecure-content");
        }
        chromeOptions.setCapability("webSocketUrl", bidi);
        // Other common options
        chromeOptions.addArguments(
                  "--disable-dev-shm-usage"
                , "--disable-search-engine-choice-screen"
                , "--remote-allow-origins=*"
                , "--disable-automation"
                , "--disable-background-timer-throttling"
                , "--disable-backgrounding-occluded-windows"
                , "--disable-features=OptimizationGuideModelDownloading,OptimizationHintsFetching,OptimizationTargetPrediction,OptimizationHints,CalculateNativeWinOcclusion,AutofillServerCommunication,MediaRouter,Translate,AvoidUnnecessaryBeforeUnloadCheckSync,CertificateTransparencyComponentUpdater,OptimizationHints,DialMediaRouteProvider,GlobalMediaControls,ImprovedCookieControls,LazyFrameLoading,InterestFeedContentSuggestions"
                , "--disable-hang-monitor"
                , "--disable-domain-reliability"
                , "--disable-renderer-backgrounding"
                , "--metrics-recording-only"
                , "--no-first-run"
                , "--no-default-browser-check"
                , "--silent-debugger-extension-api"
                , "--disable-extensions"
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
        chromeOptions.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        Reporter.log(  "Chrome Options Configured" , LogLevel.INFO_GREEN);
        Reporter.logReportOnly(chromeOptions.asMap().toString(),LogLevel.INFO_BLUE);
        return chromeOptions;
    }
    // Configure Firefox options
    private static FirefoxOptions configureFirefoxOptions(HeadlessMode headlessMode, PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode, SandboxMode sandboxMode, WebSecurityMode webSecurityMode) {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        boolean bidi=true;
        if (headlessMode==HeadlessMode.True) {
            firefoxOptions.addArguments("--headless");
        }
        if (pageLoadStrategy==PageLoadStrategyMode.Eager) {
            firefoxOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if (privateMode==PrivateMode.True) {
            firefoxOptions.addArguments("--private");
            bidi=false;
        }
        if (sandboxMode==SandboxMode.NoSandboxMode) {
            firefoxOptions.addArguments("--no-sandbox");
        }
        if (webSecurityMode==WebSecurityMode.AllowUnsecure) {
            firefoxOptions.addPreference("security.mixed_content.block_active_content", false);
        }
        firefoxOptions.setCapability("webSocketUrl", bidi);
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
                "--mute-audio",  // Mute audio
                "--ignore-certificate-errors",  // Ignore SSL certificate errors
                "--hide-scrollbars",  // Hide scrollbars
                "--disable-sync",  // Disable Firefox sync
                "--disable-client-side-phishing-detection",  // Disable phishing detection (specific to Chrome but similar behavior in Firefox)
                "--disable-default-apps",  // Prevent loading of default apps (not entirely applicable to Firefox, but similar logic)
                "--disable-notifications",  // Disable notifications
                "--window-size=1920,1080",  // Set window size
                "--disable-plugins"  // Disable plugins (extensions can be disabled with similar options)
        );
        firefoxOptions.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        Reporter.log(  "Firefox Options Configured", LogLevel.INFO_GREEN);
        Reporter.logReportOnly(firefoxOptions.asMap().toString(),LogLevel.INFO_BLUE);
        return firefoxOptions;
    }
    // Configure Edge options
    private static EdgeOptions configureEdgeOptions(HeadlessMode headlessMode, PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode, SandboxMode sandboxMode, WebSecurityMode webSecurityMode) {
        EdgeOptions edgeOptions = new EdgeOptions();
        boolean bidi=true;
        if (headlessMode==HeadlessMode.True) {
            edgeOptions.addArguments("--headless");
        }
        if (pageLoadStrategy==PageLoadStrategyMode.Eager) {
            edgeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if (privateMode==PrivateMode.True) {
            edgeOptions.addArguments("--inPrivate");
            bidi=false;
        }
        if (sandboxMode==SandboxMode.NoSandboxMode) {
            edgeOptions.addArguments("--no-sandbox");
        }
        if (webSecurityMode==WebSecurityMode.AllowUnsecure) {
            edgeOptions.addArguments("--disable-web-security");
            edgeOptions.addArguments("--allow-running-insecure-content");
        }
        edgeOptions.setCapability("webSocketUrl", bidi);
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
        edgeOptions.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        Reporter.log( "Edge Options Configured", LogLevel.INFO_GREEN);
        Reporter.logReportOnly(edgeOptions.asMap().toString(),LogLevel.INFO_BLUE);
        return edgeOptions;
    }
    // Configure Safari options
    private static SafariOptions configureSafariOptions(PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode) {
        SafariOptions safariOptions = new SafariOptions();
        if (pageLoadStrategy==PageLoadStrategyMode.Eager) {
            safariOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if (privateMode==PrivateMode.True) {
            safariOptions.setUseTechnologyPreview(true); // Safari doesn't have "private" mode via arguments, but this simulates it
        }
        safariOptions.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        Reporter.log( "Safari Options Configured", LogLevel.INFO_GREEN);
        Reporter.logReportOnly(safariOptions.asMap().toString(),LogLevel.INFO_BLUE);
        return safariOptions;
    }
}
