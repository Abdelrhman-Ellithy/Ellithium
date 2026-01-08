package Ellithium.core.driver;

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
import static Ellithium.core.driver.LocalDriverType.*;
import static Ellithium.core.driver.RemoteDriverType.*;
import static Ellithium.core.recording.internal.VideoRecordingManager.isRecordingEnabled;

public class BrowserSetUp {

    public static WebDriver setupLocalDriver(DriverType driverType,Capabilities capabilities, HeadlessMode headlessMode, PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode, SandboxMode sandboxMode, WebSecurityMode webSecurityMode) {
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
        ChromeOptions options = new ChromeOptions();
        if (headlessMode==HeadlessMode.True) {
            options.addArguments("--headless");
        }
        if (pageLoadStrategy==PageLoadStrategyMode.Eager) {
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if (privateMode==PrivateMode.True) {
            options.addArguments("--incognito");
        }
        if (sandboxMode==SandboxMode.NoSandboxMode) {
            options.addArguments("--no-sandbox");
        }
        if (webSecurityMode==WebSecurityMode.AllowUnsecure) {
            options.addArguments("--disable-web-security");
            options.addArguments("--allow-running-insecure-content");
        }
        // Window and Display Settings
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--force-color-profile=srgb");
        options.addArguments("--hide-scrollbars");

        // Security and Privacy
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--remote-allow-origins=*");

        // Automation Detection
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-automation");

        // Browser UI
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        options.addArguments("--disable-search-engine-choice-screen");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-default-apps");

        // Performance Optimizations (SAFE)
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--disable-pinch");
        options.addArguments("--mute-audio");
        options.addArguments("--dns-prefetch-disable");

        // Stability
        options.addArguments("--disable-hang-monitor");
        options.addArguments("--disable-ipc-flooding-protection");
        options.addArguments("--silent-debugger-extension-api");

        // Privacy Features (SAFE)
        options.addArguments("--disable-client-side-phishing-detection");
        options.addArguments("--disable-sync");
        options.addArguments("--disable-sync-preferences");
        options.addArguments("--disable-translate");
        options.addArguments("--no-pings");

        // Disable Heavy Features
        options.addArguments("--disable-features=OptimizationGuideModelDownloading," +
                "OptimizationHintsFetching,OptimizationTargetPrediction,OptimizationHints," +
                "CalculateNativeWinOcclusion,AutofillServerCommunication,MediaRouter," +
                "Translate,AvoidUnnecessaryBeforeUnloadCheckSync,CertificateTransparencyComponentUpdater," +
                "DialMediaRouteProvider,GlobalMediaControls,ImprovedCookieControls," +
                "LazyFrameLoading,InterestFeedContentSuggestions");

        // Network Features (Careful - some affect CDP)
        options.addArguments("--disable-background-networking");  // Only once!
        options.addArguments("--disable-domain-reliability");

        // Background Tasks
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-background-tasks");

        // Component Updates
        options.addArguments("--disable-component-update");
        options.addArguments("--disable-field-trial-config");

        // Miscellaneous
        options.addArguments("--metrics-recording-only");
        options.addArguments("--host-resolver-rules");
        options.addArguments("--disable-device-discovery-notifications");
        options.addArguments("--ash-disable-system-sounds");
        options.addArguments("--disable-plugins");
        options.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        // ====================================================================
        // REQUIRED FOR CDP
        // ====================================================================
        options.addArguments("--enable-features=NetworkService");
        options.addArguments("--enable-features=NetworkServiceInProcess");
        options.addArguments("--enable-use-zoom-for-dsf");
        options.setCapability("unhandledPromptBehavior", "ignore");
        options.setCapability("webSocketUrl", true);
        Reporter.log(  "Chrome Options Configured" , LogLevel.INFO_GREEN);
        boolean isRecordingEnabled = isRecordingEnabled();

        if (!isRecordingEnabled) {
            // These can interfere with CDP but are safe when not recording
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--enable-logging");
            options.addArguments("--log-net-log");
            options.addArguments("--net-log-capture-mode");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-component-extensions-with-background-pages");
        }
        addCapabilitiesToParam(options);
        return options;
    }
    // Configure Firefox options
    private static FirefoxOptions configureFirefoxOptions(
            HeadlessMode headlessMode,
            PageLoadStrategyMode pageLoadStrategy,
            PrivateMode privateMode,
            SandboxMode sandboxMode,
            WebSecurityMode webSecurityMode) {

        FirefoxOptions options = new FirefoxOptions();
        if (headlessMode == HeadlessMode.True) {
            options.addArguments("--headless");
        }
        if (pageLoadStrategy == PageLoadStrategyMode.Eager) {
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        // Private mode (use preferences, not disabling BiDi)
        if (privateMode == PrivateMode.True) {
            options.addPreference("browser.privatebrowsing.autostart", true);
        }

        if (sandboxMode == SandboxMode.NoSandboxMode) {
            // Firefox doesn't have a single --no-sandbox flag like Chromium; only use if you know what you're doing
            // (left intentionally blank or use environment-specific service config)
        }
        if (webSecurityMode == WebSecurityMode.AllowUnsecure) {
            options.addPreference("security.mixed_content.block_active_content", false);
            options.addPreference("security.mixed_content.block_display_content", false);
            options.setAcceptInsecureCerts(true); // acceptInsecureCerts capability
        }

        /* ---------- Performance & Noise Reduction ---------- */
        options.addPreference("browser.aboutConfig.showWarning", false);
        options.addPreference("browser.tabs.warnOnClose", false);
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("services.sync.enabled", false);

        /* ---------- Telemetry ---------- */
        options.addPreference("toolkit.telemetry.enabled", false);
        options.addPreference("toolkit.telemetry.unified", false);
        options.addPreference("datareporting.healthreport.uploadEnabled", false);

        /* ---------- Media ---------- */
        options.addPreference("media.volume_scale", 0); // MUST be int

        /* ---------- Automation ---------- */
        options.addPreference("dom.webdriver.enabled", true);

        /* ---------- Alerts ---------- */
        options.setCapability(
                CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR,
                UnexpectedAlertBehaviour.IGNORE
        );

        Reporter.log("Firefox Options Configured", LogLevel.INFO_GREEN);
        addCapabilitiesToParam(options);
        return options;
    }
    // Configure Edge options
    private static EdgeOptions configureEdgeOptions(
            HeadlessMode headlessMode,
            PageLoadStrategyMode pageLoadStrategy,
            PrivateMode privateMode,
            SandboxMode sandboxMode,
            WebSecurityMode webSecurityMode) {

        EdgeOptions options = new EdgeOptions();
        if (headlessMode == HeadlessMode.True) {
            options.addArguments("--headless");
        }

        if (pageLoadStrategy == PageLoadStrategyMode.Eager) {
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }

        if (privateMode == PrivateMode.True) {
            options.addArguments("--inPrivate");
        }
        if (sandboxMode == SandboxMode.NoSandboxMode) {
            options.addArguments("--no-sandbox");
        }
        if (webSecurityMode == WebSecurityMode.AllowUnsecure) {
            options.addArguments("--disable-web-security");
            options.addArguments("--allow-running-insecure-content");
        }
        // =========================================================
        // Window & Display
        // =========================================================
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--force-color-profile=srgb");
        options.addArguments("--hide-scrollbars");

        // =========================================================
        // Security & Privacy
        // =========================================================
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--remote-allow-origins=*");

        // =========================================================
        // Automation Detection
        // =========================================================
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-automation");

        // =========================================================
        // Browser UI
        // =========================================================
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        options.addArguments("--disable-search-engine-choice-screen");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-default-apps");

        // =========================================================
        // Performance (SAFE)
        // =========================================================
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--disable-pinch");
        options.addArguments("--mute-audio");
        options.addArguments("--dns-prefetch-disable");

        // =========================================================
        // Stability
        // =========================================================
        options.addArguments("--disable-hang-monitor");
        options.addArguments("--silent-debugger-extension-api");

        // =========================================================
        // Privacy
        // =========================================================
        options.addArguments("--disable-client-side-phishing-detection");
        options.addArguments("--disable-sync");
        options.addArguments("--disable-sync-preferences");
        options.addArguments("--disable-translate");
        options.addArguments("--no-pings");

        // =========================================================
        // Background / Network
        // =========================================================
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-background-tasks");

        // =========================================================
        // Component Updates
        // =========================================================
        options.addArguments("--disable-component-update");
        options.addArguments("--disable-field-trial-config");

        // =========================================================
        // Misc
        // =========================================================
        options.addArguments("--metrics-recording-only");
        options.addArguments("--disable-device-discovery-notifications");
        options.addArguments("--ash-disable-system-sounds");

        // =========================================================
        // REQUIRED FOR CDP / BiDi
        // =========================================================
        options.addArguments("--enable-features=NetworkService");
        options.addArguments("--enable-features=NetworkServiceInProcess");
        options.addArguments("--enable-use-zoom-for-dsf");
        options.setCapability("webSocketUrl", true);
        options.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);

        // =========================================================
        // CONDITIONAL (NOT recording)
        // =========================================================
        boolean isRecordingEnabled = isRecordingEnabled();
        if (!isRecordingEnabled) {
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--enable-logging");
            options.addArguments("--log-net-log");
            options.addArguments("--net-log-capture-mode");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-component-extensions-with-background-pages");
        }

        Reporter.log("Edge Options Configured", LogLevel.INFO_GREEN);
        addCapabilitiesToParam(options);
        return options;
    }
    // Configure Safari options
    private static SafariOptions configureSafariOptions(PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode) {
        SafariOptions safariOptions = new SafariOptions();
        if (pageLoadStrategy==PageLoadStrategyMode.Eager) {
            safariOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        if (privateMode == PrivateMode.True) {
            Reporter.log(
                    "Safari does not support Private/Incognito mode via WebDriver. Ignoring PrivateMode.",
                    LogLevel.WARN
            );
        }
        safariOptions.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        Reporter.log( "Safari Options Configured", LogLevel.INFO_GREEN);
        addCapabilitiesToParam(safariOptions);
        return safariOptions;
    }
    private static void addCapabilitiesToParam(Capabilities capabilities){
        String convertedCaps=capabilities.asMap().toString();
        Reporter.logReportOnly("Configured Optimized Capabilities: "+convertedCaps,LogLevel.INFO_BLUE);
    }
}
