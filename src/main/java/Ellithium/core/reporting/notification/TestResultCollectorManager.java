package Ellithium.core.reporting.notification;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

/**
 * Singleton manager for TestResultCollector instance.
 * Ensures shared state between different listeners and prevents test execution blocking.
 */
public class TestResultCollectorManager {
    
    private static TestResultCollectorManager instance;
    private final NotificationIntegrationHandler testResultCollector;
    
    private TestResultCollectorManager() {
        this.testResultCollector = new NotificationIntegrationHandler();
    }
    
    /**
     * Gets the singleton instance of TestResultCollectorManager.
     * @return The singleton instance
     */
    public static synchronized TestResultCollectorManager getInstance() {
        if (instance == null) {
            instance = new TestResultCollectorManager();
        }
        return instance;
    }
    
    /**
     * Gets the TestResultCollector instance.
     * @return The TestResultCollector instance
     */
    public TestResultCollector getTestResultCollector() {
        return testResultCollector;
    }
    
    /**
     * Sends execution completion notifications.
     * @return true if notifications were sent successfully
     */
    public boolean sendExecutionCompletionNotifications() {
        try {
            return testResultCollector.sendExecutionCompletionNotifications();
        } catch (Exception e) {
            Reporter.log("Failed to send execution completion notifications: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
}
