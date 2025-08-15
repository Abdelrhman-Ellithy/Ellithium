package Ellithium.core.reporting.notification;

import Ellithium.core.logging.Logger;

/**
 * Manager class that provides a singleton instance of TestResultCollector.
 * This ensures that both TestNG and Cucumber listeners share the same test result collection state.
 * Follows the Singleton pattern for managing shared resources.
 */
public class TestResultCollectorManager {
    
    private static TestResultCollectorManager instance;
    private final NotificationIntegrationHandler testResultCollector;
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private TestResultCollectorManager() {
        this.testResultCollector = new NotificationIntegrationHandler();
    }
    
    /**
     * Gets the singleton instance of TestResultCollectorManager.
     * @return The TestResultCollectorManager instance
     */
    public static synchronized TestResultCollectorManager getInstance() {
        if (instance == null) {
            instance = new TestResultCollectorManager();
        }
        return instance;
    }
    
    /**
     * Gets the shared test result collector instance.
     * @return The TestResultCollector instance
     */
    public TestResultCollector getTestResultCollector() {
        return testResultCollector;
    }
    
    /**
     * Delegates to the underlying NotificationIntegrationHandler to send execution completion notifications.
     * This method is called at the end of test execution to send notifications.
     */
    public void sendExecutionCompletionNotifications() {
        try {
            testResultCollector.sendExecutionCompletionNotifications();
        } catch (Exception e) {
            Logger.error("Error in TestResultCollectorManager while sending notifications: " + e.getMessage());
        }
    }
    
    /**
     * Gets the notification configuration.
     * @return The NotificationConfig instance
     */
    public NotificationConfig getNotificationConfig() {
        return testResultCollector.getConfig();
    }
    
    /**
     * Gets the notification sender.
     * @return The NotificationSender instance
     */
    public NotificationSender getNotificationSender() {
        return testResultCollector.getSender();
    }
}
