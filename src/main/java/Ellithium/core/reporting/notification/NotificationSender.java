package Ellithium.core.reporting.notification;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.slack.api.Slack;
import com.slack.api.webhook.WebhookResponse;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Handles sending notifications via email and Slack.
 */
public class NotificationSender {
    
    /**
     * Sends an email notification with test results.
     * @param subject The email subject
     * @param body The email body
     * @return true if email was sent successfully
     */
    public static boolean sendEmail(String subject, String body) {
        if (!NotificationConfig.isEmailEnabled()) {
            Reporter.log("Email notifications are disabled", LogLevel.INFO_BLUE);
            return false;
        }
        
        // Validate email configuration before attempting to send
        if (!NotificationConfig.validateEmailConfiguration()) {
            Reporter.log("Email notification cancelled due to incomplete configuration", LogLevel.ERROR);
            return false;
        }
        
        try {
            // Get email configuration
            String smtpHost = NotificationConfig.getProperty(NotificationConfig.EMAIL_SMTP_HOST);
            String smtpPort = NotificationConfig.getProperty(NotificationConfig.EMAIL_SMTP_PORT);
            String username = NotificationConfig.getProperty(NotificationConfig.EMAIL_SMTP_USERNAME);
            String password = NotificationConfig.getProperty(NotificationConfig.EMAIL_SMTP_PASSWORD);
            String fromEmail = NotificationConfig.getProperty(NotificationConfig.EMAIL_FROM);
            String toEmail = NotificationConfig.getProperty(NotificationConfig.EMAIL_TO);
            
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
            message.setText(body);
            
            // Send message
            Transport.send(message);
            
            Reporter.log("Email notification sent successfully to " + toEmail, LogLevel.INFO_GREEN);
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
    public static boolean sendSlackMessage(String message) {
        if (!NotificationConfig.isSlackEnabled()) {
            Reporter.log("Slack notifications are disabled", LogLevel.INFO_BLUE);
            return false;
        }
        
        // Validate Slack configuration before attempting to send
        if (!NotificationConfig.validateSlackConfiguration()) {
            Reporter.log("Slack notification cancelled due to incomplete configuration", LogLevel.ERROR);
            return false;
        }
        
        try {
            String webhookUrl = NotificationConfig.getProperty(NotificationConfig.SLACK_WEBHOOK_URL);
            String channel = NotificationConfig.getProperty(NotificationConfig.SLACK_CHANNEL);
            String username = NotificationConfig.getProperty(NotificationConfig.SLACK_USERNAME);
            
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
    private static String buildSlackPayload(String message, String channel, String username) {
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
    private static String escapeJsonString(String input) {
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
     * @param message The message content
     * @return true if at least one notification was sent successfully
     */
    public static boolean sendNotifications(String subject, String message) {
        boolean emailSent = false;
        boolean slackSent = false;
        
        // Send email notification
        if (NotificationConfig.isEmailEnabled()) {
            emailSent = sendEmail(subject, message);
        }
        
        // Send Slack notification
        if (NotificationConfig.isSlackEnabled()) {
            slackSent = sendSlackMessage(message);
        }
        
        return emailSent || slackSent;
    }
}
