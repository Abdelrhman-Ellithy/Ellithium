package Ellithium.core.reporting.notification;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;

import java.util.Properties;

/**
 * Configuration class for notification settings.
 * Loads notification properties from config.properties file.
 */
public class NotificationConfig {
    
    private static final String CONFIG_FILE = "src/main/resources/properties/config.properties";
    private static Properties properties;
    private static boolean propertiesLoaded = false;
    
    // Notification enable/disable flags
    public static final String NOTIFICATION_ENABLED = "notification.enabled";
    public static final String EMAIL_ENABLED = "notification.email.enabled";
    public static final String SLACK_ENABLED = "notification.slack.enabled";
    
    // Email configuration
    public static final String EMAIL_SMTP_HOST = "notification.email.smtp.host";
    public static final String EMAIL_SMTP_PORT = "notification.email.smtp.port";
    public static final String EMAIL_SMTP_USERNAME = "notification.email.smtp.username";
    public static final String EMAIL_SMTP_PASSWORD = "notification.email.smtp.password";
    public static final String EMAIL_FROM = "notification.email.from";
    public static final String EMAIL_TO = "notification.email.to";
    public static final String EMAIL_SUBJECT_PREFIX = "notification.email.subject.prefix";
    
    // Slack configuration
    public static final String SLACK_WEBHOOK_URL = "notification.slack.webhook.url";
    public static final String SLACK_CHANNEL = "notification.slack.channel";
    public static final String SLACK_USERNAME = "notification.slack.username";
    
    // Trigger configuration
    public static final String FAILURE_THRESHOLD = "notification.failure.threshold";
    public static final String SEND_ON_FAILURE = "notification.send.on.failure";
    public static final String SEND_ON_COMPLETION = "notification.send.on.completion";
    
    static {
        loadProperties();
    }
    
    /**
     * Loads notification properties from config file.
     */
    private static void loadProperties() {
        try {
            Logger.info("Loading notification properties from: " + CONFIG_FILE);
            properties = PropertyHelper.getAllProperties(CONFIG_FILE);
            propertiesLoaded = true;
            Logger.info("Notification properties loaded successfully. Properties count: " + properties.size());
        } catch (Exception e) {
            propertiesLoaded = false;
            Logger.error("Failed to load notification properties: " + e.getMessage());
            Logger.error("Notification system will be disabled due to configuration loading failure");
        }
    }
    
    /**
     * Checks if properties were loaded successfully.
     * @return true if properties are loaded
     */
    public static boolean arePropertiesLoaded() {
        return propertiesLoaded;
    }
    
    /**
     * Gets a property value as string.
     * @param key The property key
     * @return The property value
     */
    public static String getProperty(String key) {
        if (!propertiesLoaded || properties == null) {
            Logger.warn("Attempting to get property '" + key + "' but properties are not loaded");
            return "";
        }
        return properties.getProperty(key, "");
    }
    
    /**
     * Gets a property value as boolean.
     * @param key The property key
     * @return The property value as boolean
     */
    public static boolean getBooleanProperty(String key) {
        String value = getProperty(key);
        boolean result = Boolean.parseBoolean(value);
        Logger.info("Boolean property '" + key + "' = '" + value + "' -> " + result);
        return result;
    }
    
    /**
     * Gets a property value as integer.
     * @param key The property key
     * @return The property value as integer
     */
    public static int getIntProperty(String key) {
        String value = getProperty(key);
        try {
            int result = Integer.parseInt(value);
            return result;
        } catch (NumberFormatException e) {
            Logger.warn("Failed to parse int property '" + key + "' = '" + value + "', returning 0");
            return 0;
        }
    }
    
    /**
     * Checks if notifications are enabled.
     * @return true if notifications are enabled
     */
    public static boolean isNotificationEnabled() {
        boolean propertiesLoaded = arePropertiesLoaded();
        boolean globalEnabled = getBooleanProperty(NOTIFICATION_ENABLED);
        
        Logger.info("Checking if notifications are enabled:");
        Logger.info("  - Properties loaded: " + propertiesLoaded);
        Logger.info("  - Global enabled: " + globalEnabled);
        Logger.info("  - Final result: " + (propertiesLoaded && globalEnabled));
        
        return propertiesLoaded && globalEnabled;
    }
    
    /**
     * Checks if email notifications are enabled.
     * @return true if email notifications are enabled
     */
    public static boolean isEmailEnabled() {
        return isNotificationEnabled() && getBooleanProperty(EMAIL_ENABLED);
    }
    
    /**
     * Checks if Slack notifications are enabled.
     * @return true if Slack notifications are enabled
     */
    public static boolean isSlackEnabled() {
        return isNotificationEnabled() && getBooleanProperty(SLACK_ENABLED);
    }
    
    /**
     * Gets the failure threshold percentage.
     * @return The failure threshold percentage
     */
    public static int getFailureThreshold() {
        return getIntProperty(FAILURE_THRESHOLD);
    }
    
    /**
     * Checks if notifications should be sent on test failure.
     * @return true if notifications should be sent on failure
     */
    public static boolean shouldSendOnFailure() {
        return getBooleanProperty(SEND_ON_FAILURE);
    }
    
    /**
     * Checks if notifications should be sent on test completion.
     * @return true if notifications should be sent on completion
     */
    public static boolean shouldSendOnCompletion() {
        return getBooleanProperty(SEND_ON_COMPLETION);
    }
    
    /**
     * Validates email configuration and logs any issues.
     * @return true if email configuration is valid
     */
    public static boolean validateEmailConfiguration() {
        if (!isEmailEnabled()) {
            return false;
        }
        
        String username = getProperty(EMAIL_SMTP_USERNAME);
        String password = getProperty(EMAIL_SMTP_PASSWORD);
        String fromEmail = getProperty(EMAIL_FROM);
        String toEmail = getProperty(EMAIL_TO);
        
        if (username.isEmpty()) {
            Reporter.log("Email configuration incomplete: SMTP username is missing", LogLevel.ERROR);
            return false;
        }
        if (password.isEmpty()) {
            Reporter.log("Email configuration incomplete: SMTP password is missing", LogLevel.ERROR);
            return false;
        }
        if (fromEmail.isEmpty()) {
            Reporter.log("Email configuration incomplete: From email address is missing", LogLevel.ERROR);
            return false;
        }
        if (toEmail.isEmpty()) {
            Reporter.log("Email configuration incomplete: To email address is missing", LogLevel.ERROR);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates Slack configuration and logs any issues.
     * @return true if Slack configuration is valid
     */
    public static boolean validateSlackConfiguration() {
        if (!isSlackEnabled()) {
            return false;
        }
        
        String webhookUrl = getProperty(SLACK_WEBHOOK_URL);
        
        if (webhookUrl.isEmpty()) {
            Reporter.log("Slack configuration incomplete: Webhook URL is missing", LogLevel.ERROR);
            return false;
        }
        
        return true;
    }
}
