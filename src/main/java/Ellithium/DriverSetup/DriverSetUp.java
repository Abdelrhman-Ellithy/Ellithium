package Ellithium.DriverSetup;

import Ellithium.Utilities.Colors;
import Ellithium.Utilities.logsUtils;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

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
            default:
                throw new IllegalArgumentException("Invalid browser: " + browserName);
        }
    }

    // Configure Chrome options
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
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--ignore-certificate-errors");
        logsUtils.info(Colors.GREEN+ "Chrome Options Configured"+Colors.RESET);
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
        firefoxOptions.addArguments("--disable-dev-shm-usage");
        logsUtils.info(Colors.GREEN+ "Firefox Options Configured"+Colors.RESET);
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
            edgeOptions.addArguments("--inprivate");
        }
        if ("false".equalsIgnoreCase(sandboxMode)) {
            edgeOptions.addArguments("--no-sandbox");
        }
        if ("false".equalsIgnoreCase(webSecurityMode)) {
            edgeOptions.addArguments("--disable-web-security");
            edgeOptions.addArguments("--allow-running-insecure-content");
        }
        // Other common options
        edgeOptions.addArguments("--disable-dev-shm-usage");
        logsUtils.info(Colors.GREEN+ "Edge Options Configured"+Colors.RESET);
        return edgeOptions;
    }
}