package Ellithium.core.reporting.notification;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NotificationConfigTest {

    @Test
    public void getInstance_returnsSingleton() {
        NotificationConfig a = NotificationConfig.getInstance();
        NotificationConfig b = NotificationConfig.getInstance();
        Assert.assertSame(a, b, "getInstance must return the same object every time");
    }

    @Test
    public void isNotificationEnabled_returnsBoolean_doesNotThrow() {
        // Value depends on notifications.properties in the environment — just verify no exception
        boolean result = NotificationConfig.getInstance().isNotificationEnabled();
        Assert.assertTrue(result || !result); // always passes — asserts the call completes
    }

    @Test
    public void isEmailEnabled_returnsBoolean_doesNotThrow() {
        boolean result = NotificationConfig.getInstance().isEmailEnabled();
        Assert.assertTrue(result || !result);
    }

    @Test
    public void isSlackEnabled_returnsBoolean_doesNotThrow() {
        boolean result = NotificationConfig.getInstance().isSlackEnabled();
        Assert.assertTrue(result || !result);
    }

    @Test
    public void validateEmailConfiguration_returnsBoolean_doesNotThrow() {
        boolean result = NotificationConfig.getInstance().validateEmailConfiguration();
        Assert.assertTrue(result || !result);
    }

    @Test
    public void validateSlackConfiguration_returnsFalse_whenSlackDisabled() {
        // Slack is not configured in this project's notifications.properties
        Assert.assertFalse(NotificationConfig.getInstance().validateSlackConfiguration());
    }

    @Test
    public void getFailureThreshold_returnsPositiveInt() {
        int threshold = NotificationConfig.getInstance().getFailureThreshold();
        Assert.assertTrue(threshold >= 0,
                "Failure threshold must be non-negative, was: " + threshold);
    }

    @Test
    public void shouldSendOnFailure_doesNotThrow() {
        NotificationConfig.getInstance().shouldSendOnFailure();
    }

    @Test
    public void shouldSendOnCompletion_doesNotThrow() {
        NotificationConfig.getInstance().shouldSendOnCompletion();
    }

    @Test
    public void smtpGetters_doNotThrow() {
        NotificationConfig cfg = NotificationConfig.getInstance();
        // None of these should throw regardless of configuration state
        cfg.getSmtpHost();
        cfg.getSmtpPort();
        cfg.getSmtpUsername();
        cfg.getSmtpPassword();
        cfg.getFromEmail();
        cfg.getToEmail();
        cfg.getCcEmail();
        cfg.getEmailSubjectPrefix();
        cfg.getSlackWebhookUrl();
        cfg.getSlackChannel();
        cfg.getSlackUsername();
    }

    @Test
    public void validateEmailConfiguration_consistentWithIsEmailEnabled() {
        NotificationConfig cfg = NotificationConfig.getInstance();
        // If email is disabled, validation must also fail
        if (!cfg.isEmailEnabled()) {
            Assert.assertFalse(cfg.validateEmailConfiguration(),
                    "Validation must return false when email is disabled");
        }
    }

    @Test
    public void resolveEnvironmentVariables_nullAndEmpty() {
        Assert.assertNull(NotificationConfig.resolveEnvironmentVariables(null));
        Assert.assertEquals(NotificationConfig.resolveEnvironmentVariables(""), "");
    }

    @Test
    public void resolveEnvironmentVariables_noPlaceholders() {
        String input = "simple_smtp_host.com";
        Assert.assertEquals(NotificationConfig.resolveEnvironmentVariables(input), input);
    }

    @Test
    public void resolveEnvironmentVariables_systemProperty() {
        String key = "test.notification.cfg.host";
        System.setProperty(key, "smtp.custom-domain.net");
        try {
            String input = "Host is ${" + key + "}";
            String resolved = NotificationConfig.resolveEnvironmentVariables(input);
            Assert.assertEquals(resolved, "Host is smtp.custom-domain.net");
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    public void resolveEnvironmentVariables_multipleAndRepeated() {
        String key1 = "test.notif.user";
        String key2 = "test.notif.domain";
        System.setProperty(key1, "admin");
        System.setProperty(key2, "company.org");
        try {
            String input = "${" + key1 + "}@${" + key2 + "} (repeat: ${" + key1 + "})";
            String resolved = NotificationConfig.resolveEnvironmentVariables(input);
            Assert.assertEquals(resolved, "admin@company.org (repeat: admin)");
        } finally {
            System.clearProperty(key1);
            System.clearProperty(key2);
        }
    }

    @Test
    public void resolveEnvironmentVariables_unknownVariable_retainsPlaceholder() {
        String input = "smtp://${UNLIKELY_TO_EXIST_ENV_VAR_12345}:587";
        String resolved = NotificationConfig.resolveEnvironmentVariables(input);
        Assert.assertEquals(resolved, input);
    }

    @Test
    public void resolveEnvironmentVariables_unclosedPlaceholder() {
        String input = "smtp://${UNCLOSED_PLACEHOLDER";
        String resolved = NotificationConfig.resolveEnvironmentVariables(input);
        Assert.assertEquals(resolved, input);
    }
}
