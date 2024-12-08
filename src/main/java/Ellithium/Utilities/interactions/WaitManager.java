package Ellithium.Utilities.interactions;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.logsUtils;
import Ellithium.core.reporting.Reporter;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;

public class WaitManager {
    private  static int defaultTimeout= 5;
    private  static int defaultImplicitWait= 10;
    private  static int defaultPollingTime=200;
    private static boolean defaultImplicitTimeoutGotFlag=false;

    public static int getDefaultTimeout() {
        return defaultTimeout;
    }
    public static int getDefaultPollingTime() {
        return defaultPollingTime;
    }
    public static int getDefaultImplicitWait() {
        return defaultImplicitWait;
    }
    private  static boolean defaultTimeoutGotFlag=false;
    private  static boolean defaultPollingTimeGotFlag=false;
    public static void initializeTimeoutAndPolling() {
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
        if(!defaultImplicitTimeoutGotFlag){
            initImplicitTimeout();
            defaultImplicitTimeoutGotFlag = true;
            Reporter.log("Initialize default Implicit Wait for the Driver ", LogLevel.INFO_GREEN);
        }
    }
    private static int parseProperty(String value, int defaultValue, String propertyName) {
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logsUtils.warn("Invalid value for " + propertyName + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    // Initialize default timeout from properties file
    private static void initTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultElementWaitTimeout");
            defaultTimeout = parseProperty(timeout, 5, "defaultElementWaitTimeout");
        } catch (Exception e) {
            logsUtils.logException(e);
            defaultTimeout = 5;  // Assign default if exception occurs
        }
    }
    // Initialize default polling time from properties file
    private static void initPolling() {
        try {
            String polling = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultElementPollingTime");
            defaultPollingTime = parseProperty(polling, 5, "defaultElementPollingTime");
        } catch (Exception e) {
            logsUtils.logException(e);
            defaultPollingTime = 5;  // Assign default if exception occurs
        }
    }
    private static void initImplicitTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultDriverWaitTimeout");
            defaultTimeout = Integer.parseInt(timeout);
        } catch (Exception e) {
            logsUtils.logException(e);
        }
    }
}
