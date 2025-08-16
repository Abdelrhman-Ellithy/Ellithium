package Ellithium.core.reporting.notification;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import java.util.Properties;

/**
 * Configuration class for notification settings.
 * Loads notification properties from config.properties file.
 * Implements graceful error handling to prevent test execution blocking.
 */
public class NotificationConfig {
    
    private static final String CONFIG_FILE = "src/main/resources/properties/config.properties";
    private static NotificationConfig instance;
    private Properties properties;
    private boolean propertiesLoaded = false;
    
    private static final String NOTIFICATION_ENABLED = "notification.enabled";
    private static final String EMAIL_ENABLED = "notification.email.enabled";
    private static final String SLACK_ENABLED = "notification.slack.enabled";
    
    private static final String EMAIL_SMTP_HOST = "notification.email.smtp.host";
    private static final String EMAIL_SMTP_PORT = "notification.email.smtp.port";
    private static final String EMAIL_SMTP_USERNAME = "notification.email.smtp.username";
    private static final String EMAIL_SMTP_PASSWORD = "notification.email.smtp.password";
    private static final String EMAIL_FROM = "notification.email.from";
    private static final String EMAIL_TO = "notification.email.to";
    private static final String EMAIL_SUBJECT_PREFIX = "notification.email.subject.prefix";
    
    private static final String SLACK_WEBHOOK_URL = "notification.slack.webhook.url";
    private static final String SLACK_CHANNEL = "notification.slack.channel";
    private static final String SLACK_USERNAME = "notification.slack.username";
    
    private static final String FAILURE_THRESHOLD = "notification.failure.threshold";
    private static final String SEND_ON_FAILURE = "notification.send.on.failure";
    private static final String SEND_ON_COMPLETION = "notification.send.on.completion";
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private NotificationConfig() {
        loadProperties();
    }
    
    /**
     * Gets the singleton instance of NotificationConfig.
     * @return The singleton instance
     */
    public static NotificationConfig getInstance() {
        if (instance == null) {
            synchronized (NotificationConfig.class) {
                if (instance == null) {
                    instance = new NotificationConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * Quick check if notifications are enabled without full configuration loading.
     * This method provides early exit for performance optimization.
     * @return true if notifications are enabled, false otherwise
     */
    public static boolean isNotificationEnabledQuick() {
        try {
            // Use PropertyHelper to get just the notification.enabled property
            String enabled = Ellithium.Utilities.helpers.PropertyHelper.getAllProperties("src/main/resources/properties/config.properties").getProperty("notification.enabled");
            return enabled != null && Boolean.parseBoolean(enabled);
        } catch (Exception e) {
            // If we can't check, assume disabled for safety
            return false;
        }
    }
    
    /**
     * Loads notification properties from config file.
     */
    private void loadProperties() {
        try {
            properties = PropertyHelper.getAllProperties(CONFIG_FILE);
            propertiesLoaded = true;
            Reporter.log("Notification properties loaded successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            propertiesLoaded = false;
            Reporter.log("Failed to load notification properties: " + e.getMessage(), LogLevel.ERROR);
            Reporter.log("Notification system will be disabled due to configuration loading failure", LogLevel.ERROR);
        }
    }
    
    /**
     * Resolves environment variables in a property value.
     * Replaces ${ENV_VAR} with actual environment variable values.
     * @param value The property value that may contain environment variable placeholders
     * @return The resolved value with environment variables substituted
     */
    private String resolveEnvironmentVariables(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (value.contains("${") && value.contains("}")) {
            String resolvedValue = value;
            
            int startIndex = 0;
            while ((startIndex = resolvedValue.indexOf("${", startIndex)) != -1) {
                int endIndex = resolvedValue.indexOf("}", startIndex);
                if (endIndex == -1) break;
                
                String placeholder = resolvedValue.substring(startIndex, endIndex + 1);
                String envVarName = resolvedValue.substring(startIndex + 2, endIndex);
                
                String envVarValue = System.getenv(envVarName);
                if (envVarValue == null) {
                    Reporter.log("Environment variable '" + envVarName + "' not found. Using placeholder as-is.", LogLevel.WARN);
                    envVarValue = placeholder;
                }
                
                resolvedValue = resolvedValue.replace(placeholder, envVarValue);
                startIndex += envVarValue.length();
            }
            
            return resolvedValue;
        }
        
        return value;
    }
    
    /**
     * Gets a property value with environment variable resolution.
     * @param key The property key
     * @return The resolved property value, or null if not found
     */
    private String getProperty(String key) {
        try {
            if (!propertiesLoaded || properties == null) {
                return null;
            }
            
            String value = properties.getProperty(key);
            if (value != null) {
                return resolveEnvironmentVariables(value);
            }
            
            return null;
        } catch (Exception e) {
            Reporter.log("Failed to get property '" + key + "': " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }
    
    /**
     * Checks if notifications are enabled.
     * @return true if notifications are enabled
     */
    public boolean isNotificationEnabled() {
        try {
            String value = getProperty(NOTIFICATION_ENABLED);
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            Reporter.log("Failed to check if notifications are enabled: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Checks if email notifications are enabled.
     * @return true if email notifications are enabled
     */
    public boolean isEmailEnabled() {
        try {
            String value = getProperty(EMAIL_ENABLED);
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            Reporter.log("Failed to check if email notifications are enabled: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Checks if Slack notifications are enabled.
     * @return true if Slack notifications are enabled
     */
    public boolean isSlackEnabled() {
        try {
            String value = getProperty(SLACK_ENABLED);
            return  Boolean.parseBoolean(value);
        } catch (Exception e) {
            Reporter.log("Failed to check if Slack notifications are enabled: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Gets the SMTP host.
     * @return SMTP host
     */
    public String getSmtpHost() {
        return getProperty(EMAIL_SMTP_HOST);
    }
    
    /**
     * Gets the SMTP port.
     * @return SMTP port
     */
    public String getSmtpPort() {
        return getProperty(EMAIL_SMTP_PORT);
    }
    
    /**
     * Gets the SMTP username.
     * @return SMTP username
     */
    public String getSmtpUsername() {
        return getProperty(EMAIL_SMTP_USERNAME);
    }
    
    /**
     * Gets the SMTP password.
     * @return SMTP password
     */
    public String getSmtpPassword() {
        return getProperty(EMAIL_SMTP_PASSWORD);
    }
    
    /**
     * Gets the from email address.
     * @return From email address
     */
    public String getFromEmail() {
        return getProperty(EMAIL_FROM);
    }
    
    /**
     * Gets the to email address.
     * @return To email address
     */
    public String getToEmail() {
        return getProperty(EMAIL_TO);
    }
    
    /**
     * Gets the email subject prefix.
     * @return Email subject prefix
     */
    public String getEmailSubjectPrefix() {
        return getProperty(EMAIL_SUBJECT_PREFIX);
    }
    
    /**
     * Gets the Slack webhook URL.
     * @return Slack webhook URL
     */
    public String getSlackWebhookUrl() {
        return getProperty(SLACK_WEBHOOK_URL);
    }
    
    /**
     * Gets the Slack channel.
     * @return Slack channel
     */
    public String getSlackChannel() {
        return getProperty(SLACK_CHANNEL);
    }
    
    /**
     * Gets the Slack username.
     * @return Slack username
     */
    public String getSlackUsername() {
        return getProperty(SLACK_USERNAME);
    }
    
    /**
     * Gets the failure threshold percentage.
     * @return Failure threshold percentage
     */
    public int getFailureThreshold() {
        try {
            String value = getProperty(FAILURE_THRESHOLD);
            return value != null ? Integer.parseInt(value) : 20;
        } catch (Exception e) {
            Reporter.log("Failed to get failure threshold: " + e.getMessage(), LogLevel.ERROR);
            return 20;
        }
    }
    
    /**
     * Checks if notifications should be sent on test failure.
     * @return true if notifications should be sent on failure
     */
    public boolean shouldSendOnFailure() {
        try {
            String value = getProperty(SEND_ON_FAILURE);
            return  Boolean.parseBoolean(value);
        } catch (Exception e) {
            Reporter.log("Failed to check if should send on failure: " + e.getMessage(), LogLevel.ERROR);
            return true;
        }
    }
    
    /**
     * Checks if notifications should be sent on test completion.
     * @return true if notifications should be sent on completion
     */
    public boolean shouldSendOnCompletion() {
        try {
            String value = getProperty(SEND_ON_COMPLETION);
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            Reporter.log("Failed to check if should send on completion: " + e.getMessage(), LogLevel.ERROR);
            return true;
        }
    }
    
    /**
     * Validates email configuration.
     * @return true if email configuration is valid
     */
    public boolean validateEmailConfiguration() {
        try {
            if (!isEmailEnabled()) {
                return false;
            }
            
            String smtpHost = getSmtpHost();
            String smtpPort = getSmtpPort();
            String username = getSmtpUsername();
            String password = getSmtpPassword();
            String fromEmail = getFromEmail();
            String toEmail = getToEmail();
            
            if (smtpHost == null || smtpHost.trim().isEmpty()) {
                Reporter.log("SMTP host is missing from email configuration", LogLevel.ERROR);
                return false;
            }
            
            if (smtpPort == null || smtpPort.trim().isEmpty()) {
                Reporter.log("SMTP port is missing from email configuration", LogLevel.ERROR);
                return false;
            }
            
            if (username == null || username.trim().isEmpty()) {
                Reporter.log("SMTP username is missing from email configuration", LogLevel.ERROR);
                return false;
            }
            
            if (password == null || password.trim().isEmpty()) {
                Reporter.log("SMTP password is missing from email configuration", LogLevel.ERROR);
                return false;
            }
            
            if (fromEmail == null || fromEmail.trim().isEmpty()) {
                Reporter.log("From email is missing from email configuration", LogLevel.ERROR);
                return false;
            }
            
            if (toEmail == null || toEmail.trim().isEmpty()) {
                Reporter.log("To email is missing from email configuration", LogLevel.ERROR);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Reporter.log("Failed to validate email configuration: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Validates Slack configuration.
     * @return true if Slack configuration is valid
     */
    public boolean validateSlackConfiguration() {
        try {
            if (!isSlackEnabled()) {
                return false;
            }
            
            String webhookUrl = getSlackWebhookUrl();
            String channel = getSlackChannel();
            String username = getSlackUsername();
            
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                Reporter.log("Slack webhook URL is missing from Slack configuration", LogLevel.ERROR);
                return false;
            }
            
            if (channel == null || channel.trim().isEmpty()) {
                Reporter.log("Slack channel is missing from Slack configuration", LogLevel.ERROR);
                return false;
            }
            
            if (username == null || username.trim().isEmpty()) {
                Reporter.log("Slack username is missing from Slack configuration", LogLevel.ERROR);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Reporter.log("Failed to validate Slack configuration: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
}
