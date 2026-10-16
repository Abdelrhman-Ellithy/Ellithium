package Ellithium.Utilities.interactions;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.management.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
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
    static int parseProperty(String value, int defaultValue, String propertyName) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                int parsed = Integer.parseInt(value.trim());
                if (parsed > 0) {
                    return parsed;
                }
                Logger.warn("Non-positive value for " + propertyName + ": " + value + ". Using default: " + defaultValue);
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
        return getFluentWaitMillis(driver, timeoutInSeconds * 1000L, pollingEveryInMillis);
    }

    /**
     * Millisecond-granularity FluentWait. {@link FluentWait} timeout is a {@link Duration}, so a
     * sub-second budget (e.g. a per-attempt slice of a divided total) is honored exactly without
     * collapsing to whole seconds.
     * Automatically suppresses repetitive listener logging during the condition polling loop.
     */
    public static <T> FluentWait<T> getFluentWaitMillis(T driver, long timeoutMillis, int pollingEveryInMillis) {
        long effectiveTimeout = timeoutMillis > 0 ? timeoutMillis : 1000L;
        int effectivePolling = pollingEveryInMillis > 0 ? pollingEveryInMillis : DEFAULT_POLLING_MS;
        if (effectivePolling > effectiveTimeout) {
            effectivePolling = (int) Math.max(1, effectiveTimeout);
        }
        return new PollingFluentWait<>(driver)
                .withTimeout(Duration.ofMillis(effectiveTimeout))
                .pollingEvery(Duration.ofMillis(effectivePolling))
                .ignoreAll(expectedExceptions);
    }

    private static class PollingFluentWait<T> extends FluentWait<T> {
        public PollingFluentWait(T input) {
            super(input);
        }

        @Override
        public <V> V until(java.util.function.Function<? super T, ? extends V> isTrue) {
            Ellithium.core.execution.listener.ListenerLogSuppression.suppress();
            try {
                return super.until(isTrue);
            } finally {
                Ellithium.core.execution.listener.ListenerLogSuppression.resume();
            }
        }
    }

}
