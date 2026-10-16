package Ellithium.core.reporting.notification;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.internet.InternetAddress;

import static org.mockito.Mockito.*;

public class NotificationSenderTest {

    private NotificationConfig mockConfig;
    private NotificationSender sender;

    @BeforeMethod
    public void setUp() {
        mockConfig = mock(NotificationConfig.class);
        sender = new NotificationSender(mockConfig);
    }

    @Test
    public void testSendEmail_whenDisabled_returnsFalse() {
        when(mockConfig.isEmailEnabled()).thenReturn(false);

        boolean result = sender.sendEmail("Subject", "Body", false);
        Assert.assertFalse(result);
        verify(mockConfig, never()).validateEmailConfiguration();
    }

    @Test
    public void testSendEmail_whenEnabledButIncompleteConfig_returnsFalse() {
        when(mockConfig.isEmailEnabled()).thenReturn(true);
        when(mockConfig.validateEmailConfiguration()).thenReturn(false);

        boolean result = sender.sendEmail("Subject", "Body", false);
        Assert.assertFalse(result);
        verify(mockConfig).validateEmailConfiguration();
    }

    @Test
    public void testSendEmail_whenInvalidFromEmail_returnsFalse() {
        when(mockConfig.isEmailEnabled()).thenReturn(true);
        when(mockConfig.validateEmailConfiguration()).thenReturn(true);
        when(mockConfig.getSmtpHost()).thenReturn("smtp.example.com");
        when(mockConfig.getSmtpPort()).thenReturn("587");
        when(mockConfig.getFromEmail()).thenReturn("invalid-email-address");

        boolean result = sender.sendEmail("Subject", "Body", false);
        Assert.assertFalse(result);
    }

    @Test
    public void testSendSlackMessage_whenDisabled_returnsFalse() {
        when(mockConfig.isSlackEnabled()).thenReturn(false);

        boolean result = sender.sendSlackMessage("Hello Slack");
        Assert.assertFalse(result);
        verify(mockConfig, never()).validateSlackConfiguration();
    }

    @Test
    public void testSendSlackMessage_whenEnabledButIncompleteConfig_returnsFalse() {
        when(mockConfig.isSlackEnabled()).thenReturn(true);
        when(mockConfig.validateSlackConfiguration()).thenReturn(false);

        boolean result = sender.sendSlackMessage("Hello Slack");
        Assert.assertFalse(result);
        verify(mockConfig).validateSlackConfiguration();
    }

    @Test
    public void testSendNotifications_bothDisabled_returnsFalse() {
        when(mockConfig.isEmailEnabled()).thenReturn(false);
        when(mockConfig.isSlackEnabled()).thenReturn(false);

        boolean result = sender.sendNotifications("Subject", "Body", "Slack");
        Assert.assertFalse(result);
    }

    @Test
    public void testIsValidEmailFormat() {
        Assert.assertTrue(NotificationSender.isValidEmailFormat("test@example.com"));
        Assert.assertTrue(NotificationSender.isValidEmailFormat("user.name+tag@sub.domain.org"));

        Assert.assertFalse(NotificationSender.isValidEmailFormat(null));
        Assert.assertFalse(NotificationSender.isValidEmailFormat(""));
        Assert.assertFalse(NotificationSender.isValidEmailFormat("not_an_email"));
        Assert.assertFalse(NotificationSender.isValidEmailFormat("user@"));
        Assert.assertFalse(NotificationSender.isValidEmailFormat("@domain.com"));
        Assert.assertFalse(NotificationSender.isValidEmailFormat("user @domain.com"));
        Assert.assertFalse(NotificationSender.isValidEmailFormat("user@domain"));
    }

    @Test
    public void testEncodeEmailSubject_preventsCrlfInjection() {
        Assert.assertEquals(NotificationSender.encodeEmailSubject(null), "");

        String raw = "Build Failed\r\nBcc: victim@example.com\t[Critical]";
        String sanitized = NotificationSender.encodeEmailSubject(raw);
        Assert.assertFalse(sanitized.contains("\r"));
        Assert.assertFalse(sanitized.contains("\n"));
        Assert.assertFalse(sanitized.contains("\t"));
        Assert.assertEquals(sanitized, "Build Failed Bcc: victim@example.com [Critical]");
    }

    @Test
    public void testEncodeHtmlAndTextContent() {
        Assert.assertEquals(NotificationSender.encodeHtmlContent(null), "");
        Assert.assertEquals(NotificationSender.encodeTextContent(null), "");

        String content = "Line 1\nLine 2\r\r\nLine 3";
        String encodedHtml = NotificationSender.encodeHtmlContent(content);
        Assert.assertTrue(encodedHtml.contains("\r\n"));
        Assert.assertFalse(encodedHtml.contains("\r\r\n"));

        String encodedText = NotificationSender.encodeTextContent(content);
        Assert.assertTrue(encodedText.contains("\r\n"));
        Assert.assertFalse(encodedText.contains("\r\r\n"));
    }

    @Test
    public void testBuildSlackPayload_andEscapeJson() {
        Assert.assertEquals(NotificationSender.escapeJson(null), "");
        Assert.assertEquals(NotificationSender.escapeJson("Hello \"World\" \\ path"), "Hello \\\"World\\\" \\\\ path");

        String payload = sender.buildSlackPayload("Test failed: \"Login\"", "#general", "bot");
        Assert.assertTrue(payload.contains("\"channel\":\"#general\""));
        Assert.assertTrue(payload.contains("\"username\":\"bot\""));
        Assert.assertTrue(payload.contains("\"text\":\"Test failed: \\\"Login\\\"\""));
        Assert.assertTrue(payload.contains("\"icon_emoji\":\":robot_face:\""));
    }

    @Test
    public void testCreateEncodedInternetAddresses_handlesTrailingCommas() {
        Assert.assertNull(sender.createEncodedInternetAddresses(null));
        Assert.assertNull(sender.createEncodedInternetAddresses("   "));

        // Handles trailing and double commas safely
        InternetAddress[] addrs = sender.createEncodedInternetAddresses("dev1@example.com, , dev2@example.com, ");
        Assert.assertNotNull(addrs);
        Assert.assertEquals(addrs.length, 2);
        Assert.assertEquals(addrs[0].getAddress(), "dev1@example.com");
        Assert.assertEquals(addrs[1].getAddress(), "dev2@example.com");

        // Invalid address inside list returns null
        InternetAddress[] invalid = sender.createEncodedInternetAddresses("dev1@example.com, not-an-email");
        Assert.assertNull(invalid);
    }
}
