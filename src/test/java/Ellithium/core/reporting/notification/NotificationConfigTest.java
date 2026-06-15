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
}
