package Ellithium.core.reporting.notification;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;

import java.util.Properties;

/**
 * Configuration class for notification settings.
 * Loads notification properties from config.properties file.
 * Follows singleton pattern for configuration management.
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
     * @return The NotificationConfig instance
     */
    public static synchronized NotificationConfig getInstance() {
        if (instance == null) {
            instance = new NotificationConfig();
        }
        return instance;
    }
    
    /**
     * Loads notification properties from config file.
     */
    private void loadProperties() {
        try {
            properties = PropertyHelper.getAllProperties(CONFIG_FILE);
            propertiesLoaded = true;
            Logger.info("Notification properties loaded successfully");
        } catch (Exception e) {
            propertiesLoaded = false;
            Logger.error("Failed to load notification properties: " + e.getMessage());
            Logger.error("Notification system will be disabled due to configuration loading failure");
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
                    Logger.warn("Environment variable '" + envVarName + "' not found. Using placeholder as-is.");
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
     * Checks if properties were loaded successfully.
     * @return true if properties are loaded
     */
    public boolean arePropertiesLoaded() {
        return propertiesLoaded;
    }
    
    /**
     * Gets a property value as string with environment variable resolution.
     * @param key The property key
     * @return The property value with environment variables resolved
     */
    public String getProperty(String key) {
        if (!propertiesLoaded || properties == null) {
            Logger.warn("Attempting to get property '" + key + "' but properties are not loaded");
            return "";
        }
        
        String rawValue = properties.getProperty(key, "");
        return resolveEnvironmentVariables(rawValue);
    }
    
    /**
     * Gets a property value as boolean.
     * @param key The property key
     * @return The property value as boolean
     */
    public boolean getBooleanProperty(String key) {
        String value = getProperty(key);
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Gets a property value as integer.
     * @param key The property key
     * @return The property value as integer
     */
    public int getIntegerProperty(String key) {
        String value = getProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Logger.warn("Failed to parse integer property '" + key + "' with value '" + value + "': " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Checks if notifications are enabled.
     * @return true if notifications are enabled
     */
    public boolean isNotificationEnabled() {
        return getBooleanProperty(NOTIFICATION_ENABLED);
    }
    
    /**
     * Checks if email notifications are enabled.
     * @return true if email notifications are enabled
     */
    public boolean isEmailEnabled() {
        return getBooleanProperty(EMAIL_ENABLED);
    }
    
    /**
     * Checks if Slack notifications are enabled.
     * @return true if Slack notifications are enabled
     */
    public boolean isSlackEnabled() {
        return getBooleanProperty(SLACK_ENABLED);
    }
    
    /**
     * Gets the SMTP host for email configuration.
     * @return SMTP host
     */
    public String getSmtpHost() {
        return getProperty(EMAIL_SMTP_HOST);
    }
    
    /**
     * Gets the SMTP port for email configuration.
     * @return SMTP port
     */
    public String getSmtpPort() {
        return getProperty(EMAIL_SMTP_PORT);
    }
    
    /**
     * Gets the SMTP username for email configuration.
     * @return SMTP username
     */
    public String getSmtpUsername() {
        return getProperty(EMAIL_SMTP_USERNAME);
    }
    
    /**
     * Gets the SMTP password for email configuration.
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
        return getIntegerProperty(FAILURE_THRESHOLD);
    }
    
    /**
     * Checks if notifications should be sent on failure.
     * @return true if notifications should be sent on failure
     */
    public boolean shouldSendOnFailure() {
        return getBooleanProperty(SEND_ON_FAILURE);
    }
    
    /**
     * Checks if notifications should be sent on completion.
     * @return true if notifications should be sent on completion
     */
    public boolean shouldSendOnCompletion() {
        return getBooleanProperty(SEND_ON_COMPLETION);
    }
    
    /**
     * Validates email configuration.
     * @return true if email configuration is complete
     */
    public boolean validateEmailConfiguration() {
        if (!isEmailEnabled()) {
            return false;
        }
        
        String smtpHost = getSmtpHost();
        String smtpPort = getSmtpPort();
        String username = getSmtpUsername();
        String password = getSmtpPassword();
        String fromEmail = getFromEmail();
        String toEmail = getToEmail();
        
        if (smtpHost.isEmpty() || smtpPort.isEmpty() || username.isEmpty() || 
            password.isEmpty() || fromEmail.isEmpty() || toEmail.isEmpty()) {
            Logger.error("Email configuration incomplete. Missing required properties.");
            Logger.error("SMTP Host: " + (smtpHost.isEmpty() ? "MISSING" : "SET"));
            Logger.error("SMTP Port: " + (smtpPort.isEmpty() ? "MISSING" : "SET"));
            Logger.error("Username: " + (username.isEmpty() ? "MISSING" : EmailObfuscator.obfuscate(username)));
            Logger.error("Password: " + (password.isEmpty() ? "MISSING" : "SET"));
            Logger.error("From Email: " + (fromEmail.isEmpty() ? "MISSING" : EmailObfuscator.obfuscate(fromEmail)));
            Logger.error("To Email: " + (toEmail.isEmpty() ? "MISSING" : EmailObfuscator.obfuscate(toEmail)));
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates Slack configuration.
     * @return true if Slack configuration is complete
     */
    public boolean validateSlackConfiguration() {
        if (!isSlackEnabled()) {
            return false;
        }
        
        String webhookUrl = getSlackWebhookUrl();
        String channel = getSlackChannel();
        String username = getSlackUsername();
        
        if (webhookUrl.isEmpty() || channel.isEmpty() || username.isEmpty()) {
            Logger.error("Slack configuration incomplete. Missing required properties.");
            Logger.error("Webhook URL: " + (webhookUrl.isEmpty() ? "MISSING" : "SET"));
            Logger.error("Channel: " + (channel.isEmpty() ? "MISSING" : "SET"));
            Logger.error("Username: " + (username.isEmpty() ? "MISSING" : "SET"));
            return false;
        }
        
        return true;
    }
}
