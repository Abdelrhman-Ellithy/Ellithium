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
    public void sendReport(){

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);

            MimeBodyPart messageBodyPart = new MimeBodyPart();

            String htmlReportContent = new String(Files.readAllBytes(Paths.get(reportFilePath)));

            // Set the HTML content
            messageBodyPart.setContent(htmlReportContent, "text/html");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);
            Transport.send(message);
            Reporter.log("HTML Report Sent Successfully to ", LogLevel.INFO_GREEN, recipientEmail);
        }catch (Exception e){
            Reporter.log("Failed to Send HTML Report via Email to: ", LogLevel.ERROR, recipientEmail);
        }
    }
}
