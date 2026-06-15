package Ellithium.core.reporting.notification;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestResultCollectorManagerTest {

    @Test
    public void getInstance_returnsSameInstance() {
        TestResultCollectorManager a = TestResultCollectorManager.getInstance();
        TestResultCollectorManager b = TestResultCollectorManager.getInstance();
        Assert.assertSame(a, b);
    }

    @Test
    public void getTestResultCollector_returnsNonNull() {
        Assert.assertNotNull(TestResultCollectorManager.getInstance().getTestResultCollector());
    }

    @Test
    public void getTestResultCollector_implementsTestResultCollector() {
        TestResultCollector collector = TestResultCollectorManager.getInstance().getTestResultCollector();
        Assert.assertTrue(collector instanceof NotificationIntegrationHandler);
    }

    @Test
    public void sendExecutionCompletionNotifications_doesNotThrow() {
        // With no notification config loaded this should gracefully return false (no real SMTP)
        boolean result = TestResultCollectorManager.getInstance().sendExecutionCompletionNotifications();
        Assert.assertFalse(result);
    }
}
