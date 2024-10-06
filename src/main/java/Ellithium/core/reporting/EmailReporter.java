package Ellithium.core.reporting;

import Ellithium.core.logging.LogLevel;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EmailReporter {
    private final String smtpHost;
    private final String smtpPort;
    private final String username;
    private final String password;
    private final String recipientEmail;
    private final String subject;
    private final String reportFilePath;  // Path to the HTML report

    public EmailReporter(String smtpHost, String smtpPort, String username, String password,
                         String recipientEmail, String subject, String reportFilePath) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.reportFilePath = reportFilePath;
    }

    public void sendReport() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);  // Set SMTP server
        props.put("mail.smtp.port", smtpPort);  // e.g., "587"
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");  // Use STARTTLS
        props.put("mail.debug", "true");  // Enable debug output
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);

            MimeBodyPart messageBodyPart = new MimeBodyPart();

            // Read HTML content from file
            String htmlReportContent = new String(Files.readAllBytes(Paths.get(reportFilePath)));
            messageBodyPart.setContent(htmlReportContent, "text/html; charset=UTF-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);

            Transport.send(message);
            Reporter.log("HTML Report Sent Successfully to ", LogLevel.INFO_GREEN, recipientEmail);

        } catch (Exception e) {
            Reporter.log("Failed to Send HTML Report via Email to: " + recipientEmail, LogLevel.ERROR);
            Reporter.log(e.getCause().toString(), LogLevel.ERROR);
        }
    }
}