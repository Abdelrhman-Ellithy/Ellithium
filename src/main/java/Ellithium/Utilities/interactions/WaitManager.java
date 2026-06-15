package Ellithium.Utilities.interactions;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.management.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumFluentWait;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.ArrayList;

public class WaitManager <T extends WebDriver>{
    private  static volatile int defaultTimeout;
    private  static volatile int defaultPollingTime;

    public static int getDefaultTimeout() {
        return defaultTimeout;
    }
    public static int getDefaultPollingTime() {
        return defaultPollingTime;
    }
    private  static volatile boolean defaultTimeoutGotFlag=false;
    private  static volatile boolean defaultPollingTimeGotFlag=false;
    private static final Object INIT_LOCK = new Object();

    public static void initializeTimeoutAndPolling() {
        synchronized (INIT_LOCK) {
            try {
                if (!defaultTimeoutGotFlag) {
                    initTimeout();
                    defaultTimeoutGotFlag = true;
                    Reporter.log("Initialize default Timeout for Element ", LogLevel.INFO_GREEN);
                }
                if (!defaultPollingTimeGotFlag) {
                    initPolling();
                    defaultPollingTimeGotFlag = true;
                    Reporter.log("Initialize default Polling Time for Element ", LogLevel.INFO_GREEN);
                }
            } catch (Exception e) {
                Logger.logException(e);
            }
        }
    }
    private static int parseProperty(String value, int defaultValue, String propertyName) {
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Logger.warn("Invalid value for " + propertyName + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    private static final int DEFAULT_TIMEOUT_SECONDS  = 10;
    private static final int DEFAULT_POLLING_MS        = 50;

    private static void initTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultElementWaitTimeout");
            defaultTimeout = parseProperty(timeout, DEFAULT_TIMEOUT_SECONDS, "defaultElementWaitTimeout");
        } catch (Exception e) {
            Logger.logException(e);
            defaultTimeout = DEFAULT_TIMEOUT_SECONDS;
        }
    }
    private static void initPolling() {
        try {
            String polling = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultElementPollingTime");
            defaultPollingTime = parseProperty(polling, DEFAULT_POLLING_MS, "defaultElementPollingTime");
        } catch (Exception e) {
            Logger.logException(e);
            defaultPollingTime = DEFAULT_POLLING_MS;
        }
    }
    private static final ArrayList<Class<? extends Exception>> expectedExceptions = new ArrayList<>();
    static {
        expectedExceptions.add(org.openqa.selenium.NoSuchElementException.class);
        expectedExceptions.add(org.openqa.selenium.StaleElementReferenceException.class);
        expectedExceptions.add(org.openqa.selenium.JavascriptException.class);
        expectedExceptions.add(org.openqa.selenium.ElementClickInterceptedException.class);
        expectedExceptions.add(org.openqa.selenium.ElementNotInteractableException.class);
        expectedExceptions.add(org.openqa.selenium.InvalidElementStateException.class);
        expectedExceptions.add(org.openqa.selenium.interactions.MoveTargetOutOfBoundsException.class);
        expectedExceptions.add(NoAlertPresentException.class);
    }

    public static <T> FluentWait<T> getFluentWait(T driver, int timeoutInSeconds, int pollingEveryInMillis) {
        if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
            return  new AppiumFluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(timeoutInSeconds))
                    .pollingEvery(Duration.ofMillis(pollingEveryInMillis))
                    .ignoreAll(expectedExceptions);
        } else {
            return  new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(timeoutInSeconds))
                    .pollingEvery(Duration.ofMillis(pollingEveryInMillis))
                    .ignoreAll(expectedExceptions);
        }
    }

}
