package AutoEllithiumSphere.DriverSetup;

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
    public static WebDriver setupLocalDriver(String browserName, String headlessMode, String PageLoadStrategy) {
        switch (browserName) {
            case "chrome":
                ChromeOptions chromeOptions = configureChromeOptions(headlessMode, PageLoadStrategy);
                return new ChromeDriver(chromeOptions);
            case "firefox":
                FirefoxOptions firefoxOptions = configureFirefoxOptions(headlessMode, PageLoadStrategy);
                return new FirefoxDriver(firefoxOptions);
            case "edge":
                EdgeOptions edgeOptions = configureEdgeOptions(headlessMode,PageLoadStrategy);
                return new EdgeDriver(edgeOptions);
            default:
                throw new IllegalArgumentException("Invalid browser: " + browserName);
        }
    }

    // Configure Chrome options
    private static ChromeOptions configureChromeOptions(String headlessMode,String pageLoadStrategy) {
        ChromeOptions chromeOptions = new ChromeOptions();
        if ("true".equals(headlessMode)) {
            chromeOptions.addArguments("--headless");
        }
        if ("eager".equals(pageLoadStrategy)){
            chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        chromeOptions.addArguments("--incognito");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--allow-running-insecure-content");
       // chromeOptions.addArguments("--disable-search-engine-choice-screen");
       // chromeOptions.addArguments("--disable-features=OptimizationGuideModelDownloading,OptimizationHintsFetching,OptimizationTargetPrediction,OptimizationHints");
        return chromeOptions;
    }

    // Configure Firefox options
    private static FirefoxOptions configureFirefoxOptions(String headlessMode,String pageLoadStrategy) {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        if ("true".equals(headlessMode)) {
            firefoxOptions.addArguments("--headless");
        }
        if ("eager".equals(pageLoadStrategy)){
            firefoxOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        firefoxOptions.addArguments("--private");  // Firefox private mode
        firefoxOptions.addArguments("--no-sandbox");  // Equivalent to Chrome's no-sandbox
        firefoxOptions.addArguments("--disable-dev-shm-usage");  // Solve resource problems
        return firefoxOptions;
    }

    // Configure Edge options
    private static EdgeOptions configureEdgeOptions(String headlessMode,String pageLoadStrategy) {
        EdgeOptions edgeOptions = new EdgeOptions();
        if ("true".equals(headlessMode)) {
            edgeOptions.addArguments("--headless");
        }
        if ("eager".equals(pageLoadStrategy)){
            edgeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }
        edgeOptions.addArguments("--inprivate");  // Edge inprivate mode
        edgeOptions.addArguments("--no-sandbox");  // Equivalent to Chrome's no-sandbox
        edgeOptions.addArguments("--disable-dev-shm-usage");  // Solve resource problems
        return edgeOptions;
    }
}
