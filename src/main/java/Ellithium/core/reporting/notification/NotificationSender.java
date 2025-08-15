package Ellithium.core.reporting.notification;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import com.slack.api.Slack;
import com.slack.api.webhook.WebhookResponse;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Handles sending notifications via email and Slack.
 * Follows proper object-oriented design with dependency injection.
 */
public class NotificationSender {
    
    private final NotificationConfig config;
    
    /**
     * Constructor that accepts NotificationConfig dependency.
     * @param config The notification configuration
     */
    public NotificationSender(NotificationConfig config) {
        this.config = config;
    }
    
    /**
     * Default constructor that uses the singleton NotificationConfig instance.
     */
    public NotificationSender() {
        this(NotificationConfig.getInstance());
    }
    
    /**
     * Sends an email notification with test results.
     * @param subject The email subject
     * @param body The email body (can be plain text or HTML)
     * @return true if email was sent successfully
     */
    public boolean sendEmail(String subject, String body) {
        return sendEmail(subject, body, false);
    }
    
    /**
     * Sends an email notification with test results.
     * @param subject The email subject
     * @param body The email body
     * @param isHtml true if the body contains HTML content
     * @return true if email was sent successfully
     */
    public boolean sendEmail(String subject, String body, boolean isHtml) {
        if (!config.isEmailEnabled()) {
            Reporter.log("Email notifications are disabled", LogLevel.INFO_BLUE);
            return false;
        }
        
        // Validate email configuration before attempting to send
        if (!config.validateEmailConfiguration()) {
            Reporter.log("Email notification cancelled due to incomplete configuration", LogLevel.ERROR);
            return false;
        }
        
        try {
            // Get email configuration using proper getters
            String smtpHost = config.getSmtpHost();
            String smtpPort = config.getSmtpPort();
            String username = config.getSmtpUsername();
            String password = config.getSmtpPassword();
            String fromEmail = config.getFromEmail();
            String toEmail = config.getToEmail();
            
            // Set up mail properties
            Properties mailProps = new Properties();
            mailProps.put("mail.smtp.auth", "true");
            mailProps.put("mail.smtp.starttls.enable", "true");
            mailProps.put("mail.smtp.host", smtpHost);
            mailProps.put("mail.smtp.port", smtpPort);
            
            // Create session
            Session session = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            
            // Set content type based on whether it's HTML or plain text
            if (isHtml) {
                message.setContent(body, "text/html; charset=UTF-8");
            } else {
                message.setText(body);
            }
            
            // Send message
            Transport.send(message);
            
            Reporter.log("Email notification sent successfully to " + EmailObfuscator.obfuscate(toEmail), LogLevel.INFO_GREEN);
            return true;
            
        } catch (Exception e) {
            Reporter.log("Failed to send email notification: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Sends a Slack notification with test results.
     * @param message The Slack message
     * @return true if Slack message was sent successfully
     */
    public boolean sendSlackMessage(String message) {
        if (!config.isSlackEnabled()) {
            Reporter.log("Slack notifications are disabled", LogLevel.INFO_BLUE);
            return false;
        }
        
        // Validate Slack configuration before attempting to send
        if (!config.validateSlackConfiguration()) {
            Reporter.log("Slack notification cancelled due to incomplete configuration", LogLevel.ERROR);
            return false;
        }
        
        try {
            String webhookUrl = config.getSlackWebhookUrl();
            String channel = config.getSlackChannel();
            String username = config.getSlackUsername();
            
            // Create Slack client
            Slack slack = Slack.getInstance();
            
            // Build the message payload
            String payload = buildSlackPayload(message, channel, username);
            
            // Send the message
            WebhookResponse response = slack.send(webhookUrl, payload);
            
            if (response.getCode() == 200) {
                Reporter.log("Slack notification sent successfully to channel " + channel, LogLevel.INFO_GREEN);
                return true;
            } else {
                Reporter.log("Failed to send Slack notification. Response code: " + response.getCode(), LogLevel.ERROR);
                return false;
            }
            
        } catch (Exception e) {
            Reporter.log("Failed to send Slack notification: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Builds the Slack message payload.
     * @param message The message text
     * @param channel The channel to post to
     * @param username The username to post as
     * @return The JSON payload string
     */
    private String buildSlackPayload(String message, String channel, String username) {
        StringBuilder payload = new StringBuilder();
        payload.append("{");
        payload.append("\"channel\": \"").append(channel).append("\",");
        payload.append("\"username\": \"").append(username).append("\",");
        payload.append("\"text\": \"").append(escapeJsonString(message)).append("\",");
        payload.append("\"icon_emoji\": \":robot_face:\"");
        payload.append("}");
        return payload.toString();
    }
    
    /**
     * Escapes special characters in JSON strings.
     * @param input The input string
     * @return The escaped string
     */
    private String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Sends both email and Slack notifications.
     * @param subject The email subject
     * @param message The message content for Slack
     * @param htmlMessage The HTML message content for email
     * @return true if at least one notification was sent successfully
     */
    public boolean sendNotifications(String subject, String message, String htmlMessage) {
        boolean emailSent = false;
        boolean slackSent = false;
        
        // Send email notification with HTML content
        if (config.isEmailEnabled()) {
            emailSent = sendEmail(subject, htmlMessage, true);
        }
        
        // Send Slack notification with plain text
        if (config.isSlackEnabled()) {
            slackSent = sendSlackMessage(message);
        }
        
        return emailSent || slackSent;
    }
    
    /**
     * Sends both email and Slack notifications (backward compatibility).
     * @param subject The email subject
     * @param message The message content
     * @return true if at least one notification was sent successfully
     */
    public boolean sendNotifications(String subject, String message) {
        return sendNotifications(subject, message, message);
    }
    
    /**
     * Gets the notification configuration.
     * @return The NotificationConfig instance
     */
    public NotificationConfig getConfig() {
        return config;
    }
}
