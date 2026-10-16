package Ellithium.core.reporting.notification;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Handles sending email and Slack notifications for test results.
 * Implements graceful error handling to prevent test execution blocking.
 */
class NotificationSender {

    private final NotificationConfig config;

    /**
     * Constructor that accepts NotificationConfig dependency.
     * @param config The notification configuration
     */
    NotificationSender(NotificationConfig config) {
        this.config = config;
    }

    /**
     * Default constructor that uses the singleton NotificationConfig instance.
     */
    NotificationSender() {
        this(NotificationConfig.getInstance());
    }

    /**
     * Sends an email notification with test results.
     * @param subject The email subject
     * @param body The email body (can be plain text or HTML)
     * @param isHtml Whether the body is HTML content
     * @return true if email was sent successfully
     */
    public boolean sendEmail(String subject, String body, boolean isHtml) {
        if (!config.isEmailEnabled()) {
            Reporter.log("Email notifications are disabled", LogLevel.DEBUG);
            return false;
        }

        if (!config.validateEmailConfiguration()) {
            Reporter.log("Email notification skipped due to incomplete configuration", LogLevel.DEBUG);
            return false;
        }

        try {
            Properties mailProps = getProperties();

            Session session = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
                }
            });

            Message message = new MimeMessage(session);
            InternetAddress fromAddress = createEncodedInternetAddress(config.getFromEmail());
            if (fromAddress == null) {
                Reporter.log("Invalid from email address, cannot send email", LogLevel.ERROR);
                return false;
            }
            message.setFrom(fromAddress);
            message.setRecipients(Message.RecipientType.TO, createEncodedInternetAddresses(config.getToEmail()));

            String ccEmail = config.getCcEmail();
            boolean hasCc = ccEmail != null && !ccEmail.trim().isEmpty();
            if (hasCc) {
                InternetAddress[] ccAddresses = createEncodedInternetAddresses(ccEmail);
                if (ccAddresses != null) {
                    message.setRecipients(Message.RecipientType.CC, ccAddresses);
                } else {
                    Reporter.log("Skipping CC - one or more CC addresses are invalid: " + ccEmail, LogLevel.WARN);
                    hasCc = false;
                }
            }

            message.setSubject(encodeEmailSubject(subject));

            if (isHtml) {
                String encodedHtmlBody = encodeHtmlContent(body);
                message.setContent(encodedHtmlBody, "text/html; charset=UTF-8");
            } else {
                String encodedTextBody = encodeTextContent(body);
                message.setText(encodedTextBody);
            }

            Transport.send(message);

            String successLog = "Email notification sent successfully to " + EmailObfuscator.obfuscate(config.getToEmail());
            if (hasCc) {
                successLog += " (cc: " + EmailObfuscator.obfuscate(ccEmail) + ")";
            }
            Reporter.log(successLog, LogLevel.INFO_GREEN);
            return true;

        } catch (Exception e) {
            Reporter.log("Failed to send email notification: " + e.getMessage(), LogLevel.ERROR);
            Reporter.log("Email notification error details: " + getErrorDetails(e), LogLevel.DEBUG);
            return false;
        }
    }

    private Properties getProperties() {
        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.auth", "true");
        mailProps.put("mail.smtp.starttls.enable", "true");
        mailProps.put("mail.smtp.host", config.getSmtpHost());
        mailProps.put("mail.smtp.port", config.getSmtpPort());
        mailProps.put("mail.mime.charset", "UTF-8");
        mailProps.put("mail.mime.encoding", "UTF-8");
        mailProps.put("mail.smtp.allow8bitmime", "true");
        mailProps.put("mail.smtp.allowutf8", "true");
        return mailProps;
    }

    /**
     * Creates a properly encoded InternetAddress for the from field.
     * @param email The email address to encode
     * @return Properly encoded InternetAddress, or null if creation fails
     */
    private InternetAddress createEncodedInternetAddress(String email) {
        if (email == null || email.trim().isEmpty()) {
            Reporter.log("Email address cannot be null or empty", LogLevel.ERROR);
            return null;
        }

        String cleanEmail = email.trim();
        if (!isValidEmailFormat(cleanEmail)) {
            Reporter.log("Invalid email format: " + cleanEmail, LogLevel.ERROR);
            return null;
        }

        try {
            return new InternetAddress(cleanEmail);
        } catch (Exception e) {
            Reporter.log("Failed to create InternetAddress for: " + cleanEmail + " - " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Creates properly encoded InternetAddresses for recipients.
     * @param emails Comma-separated email addresses
     * @return Array of properly encoded InternetAddresses, or null if creation fails
     */
    InternetAddress[] createEncodedInternetAddresses(String emails) {
        if (emails == null || emails.trim().isEmpty()) {
            Reporter.log("Recipient emails cannot be null or empty", LogLevel.ERROR);
            return null;
        }

        try {
            String[] emailArray = emails.split(",");
            java.util.List<InternetAddress> addresses = new java.util.ArrayList<>();

            for (String raw : emailArray) {
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) continue;
                InternetAddress address = createEncodedInternetAddress(trimmed);
                if (address == null) {
                    Reporter.log("Failed to create address for recipient: " + trimmed, LogLevel.ERROR);
                    return null;
                }
                addresses.add(address);
            }

            if (addresses.isEmpty()) return null;
            return addresses.toArray(new InternetAddress[0]);
        } catch (Exception e) {
            Reporter.log("Failed to create recipient addresses: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Validates email address format.
     * @param email The email address to validate
     * @return true if valid format, false otherwise
     */
    static boolean isValidEmailFormat(String email) {
        if (email == null) return false;

        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }

    /**
     * Encodes email subject for proper transmission.
     * @param subject The subject to encode
     * @return Properly encoded subject
     */
    static String encodeEmailSubject(String subject) {
        if (subject == null) {
            return "";
        }

        return subject
            .replace("\r\n", " ")
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
    static String encodeHtmlContent(String htmlContent) {
        if (htmlContent == null) {
            return "";
        }

        return htmlContent
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n", "\r\n")
            .trim();
    }

    /**
     * Encodes plain text content for proper email transmission.
     * @param textContent The text content to encode
     * @return Properly encoded text content
     */
    static String encodeTextContent(String textContent) {
        if (textContent == null) {
            return "";
        }

        return textContent
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n", "\r\n")
            .trim();
    }

    /**
     * Sends a Slack notification with test results.
     * @param message The Slack message
     * @return true if Slack message was sent successfully
     */
    public boolean sendSlackMessage(String message) {
        if (!config.isSlackEnabled()) {
            Reporter.log("Slack notifications are disabled", LogLevel.DEBUG);
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

            String payload = buildSlackPayload(message, channel, username);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Reporter.log("Slack notification sent successfully to channel " + channel, LogLevel.INFO_GREEN);
                return true;
            } else {
                Reporter.log("Failed to send Slack notification. Response code: " + response.statusCode(), LogLevel.ERROR);
                return false;
            }

        } catch (Exception e) {
            Reporter.log("Failed to send Slack notification: " + e.getMessage(), LogLevel.ERROR);
            Reporter.log("Slack notification error details: " + getErrorDetails(e), LogLevel.DEBUG);
            return false;
        }
    }

    /**
     * Sends both email and Slack notifications.
     * @param subject The email subject
     * @param emailBody The email body content
     * @param slackMessage The Slack message content
     * @return true if at least one notification was sent successfully
     */
    public boolean sendNotifications(String subject, String emailBody, String slackMessage) {
        boolean emailSent = false;
        boolean slackSent = false;

        if (config.isEmailEnabled()) {
            emailSent = sendEmail(subject, emailBody, true);
        }

        if (config.isSlackEnabled()) {
            slackSent = sendSlackMessage(slackMessage);
        }

        return emailSent || slackSent;
    }

    /**
     * Builds the Slack payload for webhook requests.
     * @param message The message content
     * @param channel The target channel
     * @param username The bot username
     * @return JSON payload string
     */
    String buildSlackPayload(String message, String channel, String username) {
        return String.format(
            "{\"channel\":\"%s\",\"username\":\"%s\",\"text\":\"%s\",\"icon_emoji\":\":robot_face:\"}",
            escapeJson(channel), escapeJson(username), escapeJson(message)
        );
    }

    static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * Gets detailed error information for debugging.
     * @param exception The exception to analyze
     * @return Formatted error details string
     */
    private String getErrorDetails(Exception exception) {
        if (exception == null) {
            return "No exception details available";
        }

        StringBuilder details = new StringBuilder();
        details.append("Exception: ").append(exception.getClass().getSimpleName());
        details.append(", Message: ").append(exception.getMessage());

        if (exception.getCause() != null) {
            details.append(", Cause: ").append(exception.getCause().getMessage());
        }

        return details.toString();
    }
}
