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
        
        if (!config.validateEmailConfiguration()) {
            Reporter.log("Email notification cancelled due to incomplete configuration", LogLevel.ERROR);
            return false;
        }
        
        try {
            String smtpHost = config.getSmtpHost();
            String smtpPort = config.getSmtpPort();
            String username = config.getSmtpUsername();
            String password = config.getSmtpPassword();
            String fromEmail = config.getFromEmail();
            String toEmail = config.getToEmail();
            
            Properties mailProps = new Properties();
            mailProps.put("mail.smtp.auth", "true");
            mailProps.put("mail.smtp.starttls.enable", "true");
            mailProps.put("mail.smtp.host", smtpHost);
            mailProps.put("mail.smtp.port", smtpPort);
            mailProps.put("mail.mime.charset", "UTF-8");
            mailProps.put("mail.mime.encoding", "UTF-8");
            mailProps.put("mail.smtp.allow8bitmime", "true");
            mailProps.put("mail.smtp.allowutf8", "true");
            
            Session session = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            Message message = new MimeMessage(session);
            
            // Set proper encoding for all email components
            message.setFrom(createEncodedInternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, createEncodedInternetAddresses(toEmail));
            message.setSubject(encodeEmailSubject(subject));
            
            if (isHtml) {
                // For HTML content, ensure proper encoding
                String encodedHtmlBody = encodeHtmlContent(body);
                message.setContent(encodedHtmlBody, "text/html; charset=UTF-8");
            } else {
                // For plain text, ensure proper newline encoding
                String encodedTextBody = encodeTextContent(body);
                message.setText(encodedTextBody);
            }
            
            Transport.send(message);
            
            Reporter.log("Email notification sent successfully to " + EmailObfuscator.obfuscate(toEmail), LogLevel.INFO_GREEN);
            return true;
            
        } catch (Exception e) {
            Reporter.log("Failed to send email notification: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Creates a properly encoded InternetAddress for the from field.
     * @param email The email address to encode
     * @return Properly encoded InternetAddress
     * @throws Exception if address creation fails
     */
    private InternetAddress createEncodedInternetAddress(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        
        // Clean and validate email address
        String cleanEmail = email.trim();
        if (!isValidEmailFormat(cleanEmail)) {
            throw new IllegalArgumentException("Invalid email format: " + cleanEmail);
        }
        
        return new InternetAddress(cleanEmail);
    }
    
    /**
     * Creates properly encoded InternetAddresses for recipients.
     * @param emails Comma-separated email addresses
     * @return Array of properly encoded InternetAddresses
     * @throws Exception if address creation fails
     */
    private InternetAddress[] createEncodedInternetAddresses(String emails) throws Exception {
        if (emails == null || emails.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient emails cannot be null or empty");
        }
        
        String[] emailArray = emails.split(",");
        InternetAddress[] addresses = new InternetAddress[emailArray.length];
        
        for (int i = 0; i < emailArray.length; i++) {
            addresses[i] = createEncodedInternetAddress(emailArray[i].trim());
        }
        
        return addresses;
    }
    
    /**
     * Validates email address format.
     * @param email The email address to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null) return false;
        
        // Basic email validation pattern
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }
    
    /**
     * Encodes email subject for proper transmission.
     * @param subject The subject to encode
     * @return Properly encoded subject
     */
    private String encodeEmailSubject(String subject) {
        if (subject == null) {
            return "";
        }
        
        // Ensure proper encoding and handle special characters
        return subject
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .trim();
    }
    
    /**
     * Encodes HTML content for proper email transmission.
     * @param htmlContent The HTML content to encode
     * @return Properly encoded HTML content
     */
    private String encodeHtmlContent(String htmlContent) {
        if (htmlContent == null) {
            return "";
        }
        
        // Ensure proper HTML encoding and newline handling
        return htmlContent
            .replace("\n", "\r\n")
            .replace("\r\r\n", "\r\n")
            .trim();
    }
    
    /**
     * Encodes plain text content for proper email transmission.
     * @param textContent The text content to encode
     * @return Properly encoded text content
     */
    private String encodeTextContent(String textContent) {
        if (textContent == null) {
            return "";
        }
        
        // Ensure proper newline encoding for email transmission
        return textContent
            .replace("\n", "\r\n")
            .replace("\r\r\n", "\r\n")
            .trim();
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
        
        if (!config.validateSlackConfiguration()) {
            Reporter.log("Slack notification cancelled due to incomplete configuration", LogLevel.ERROR);
            return false;
        }
        
        try {
            String webhookUrl = config.getSlackWebhookUrl();
            String channel = config.getSlackChannel();
            String username = config.getSlackUsername();
            
            Slack slack = Slack.getInstance();
            
            String payload = buildSlackPayload(message, channel, username);
            
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
        
        if (config.isEmailEnabled()) {
            emailSent = sendEmail(subject, htmlMessage, true);
        }
        
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
