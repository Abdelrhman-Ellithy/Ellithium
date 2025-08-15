package Ellithium.core.reporting.notification;

import Ellithium.core.logging.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles integration between the notification system and test frameworks.
 * Manages test result collection and notification sending at execution completion.
 * Follows proper object-oriented design with dependency injection.
 * Implements TestResultCollector interface for framework-agnostic result collection.
 */
public class NotificationIntegrationHandler implements TestResultCollector {
    
    private final NotificationConfig config;
    private final NotificationSender sender;
    
    // Instance variables to collect overall execution results
    private int totalTestsExecuted = 0;
    private int passedTestsExecuted = 0;
    private int failedTestsExecuted = 0;
    private int skippedTestsExecuted = 0;
    private Set<ITestResult> allFailedResults = new HashSet<>();
    private long executionStartTime = 0;
    
    /**
     * Constructor that accepts dependencies.
     * @param config The notification configuration
     * @param sender The notification sender
     */
    public NotificationIntegrationHandler(NotificationConfig config, NotificationSender sender) {
        this.config = config;
        this.sender = sender;
    }
    
    /**
     * Default constructor that uses singleton instances.
     */
    public NotificationIntegrationHandler() {
        this(NotificationConfig.getInstance(), new NotificationSender());
    }
    
    @Override
    public void initializeTestResultCollection() {
        executionStartTime = System.currentTimeMillis();
        totalTestsExecuted = 0;
        passedTestsExecuted = 0;
        failedTestsExecuted = 0;
        skippedTestsExecuted = 0;
        allFailedResults.clear();
        
        Logger.info("Test result collection system initialized");
    }
    
    @Override
    public void collectTestResults(ITestContext context) {
        totalTestsExecuted += context.getPassedTests().size() + context.getFailedTests().size() + context.getSkippedTests().size();
        passedTestsExecuted += context.getPassedTests().size();
        failedTestsExecuted += context.getFailedTests().size();
        skippedTestsExecuted += context.getSkippedTests().size();
        allFailedResults.addAll(context.getFailedTests().getAllResults());
        
        Logger.info("Collected TestNG test results from context: " + context.getName() + 
                   " (Total: " + totalTestsExecuted + ", Passed: " + passedTestsExecuted + 
                   ", Failed: " + failedTestsExecuted + ", Skipped: " + skippedTestsExecuted + ")");
    }
    
    @Override
    public void collectCucumberTestResult(String scenarioName, String status, long executionTime) {
        totalTestsExecuted++;
        
        switch (status.toUpperCase()) {
            case "PASSED":
                passedTestsExecuted++;
                break;
            case "FAILED":
                failedTestsExecuted++;
                // For Cucumber, we can't easily get ITestResult, so we'll track count only
                break;
            case "SKIPPED":
                skippedTestsExecuted++;
                break;
            default:
                Logger.warn("Unknown Cucumber test status: " + status);
                break;
        }
        
        Logger.info("Collected Cucumber test result: " + scenarioName + " - " + status + 
                   " (Total: " + totalTestsExecuted + ", Passed: " + passedTestsExecuted + 
                   ", Failed: " + failedTestsExecuted + ", Skipped: " + skippedTestsExecuted + ")");
    }
    
    /**
     * Sends test completion notifications based on overall execution results.
     * This method should be called at the end of all test execution.
     */
    public void sendExecutionCompletionNotifications() {
        if (!config.isNotificationEnabled()) {
            Logger.info("Notifications are disabled. Skipping notification sending.");
            return;
        }
        
        try {
            // Calculate total execution time
            long totalExecutionTime = System.currentTimeMillis() - executionStartTime;
            
            Logger.info("Preparing execution completion notification for " + totalTestsExecuted + " tests");
            
            // Create test result summary from overall execution data
            TestResultSummary summary = new TestResultSummary(
                totalTestsExecuted, 
                passedTestsExecuted, 
                failedTestsExecuted, 
                skippedTestsExecuted, 
                totalExecutionTime, 
                allFailedResults
            );
            
            // Determine if notifications should be sent
            boolean shouldSendNotification = false;
            String notificationReason = "";
            
            // Check if any tests failed
            if (summary.hasFailures() && config.shouldSendOnFailure()) {
                shouldSendNotification = true;
                notificationReason = "Test failures detected";
            }
            
            // Check if failure rate exceeds threshold
            if (summary.exceedsFailureThreshold(config.getFailureThreshold())) {
                shouldSendNotification = true;
                notificationReason = "Failure rate (" + String.format("%.1f%%", summary.getFailureRate()) + 
                                  "%) exceeds threshold (" + config.getFailureThreshold() + "%)";
            }
            
            // Check if notifications should be sent on completion
            if (config.shouldSendOnCompletion()) {
                shouldSendNotification = true;
                notificationReason = "Test execution completed";
            }
            
            if (shouldSendNotification) {
                Logger.info("Sending execution completion notification: " + notificationReason);
                
                // Generate notification content
                String subject = summary.generateEmailSubject();
                String message = summary.generateSummaryMessage();
                String htmlMessage = summary.generateHtmlEmailBody();
                
                // Send notifications
                boolean notificationSent = sender.sendNotifications(subject, message, htmlMessage);
                
                if (notificationSent) {
                    Logger.info("Execution completion notification sent successfully");
                } else {
                    Logger.info("Failed to send execution completion notification");
                }
            } else {
                Logger.info("No notification criteria met. Skipping execution completion notification.");
            }
            
        } catch (Exception e) {
            Logger.error("Error sending execution completion notifications: " + e.getMessage());
            // Don't let notification errors break the test execution
        }
    }
    
    @Override
    public int getTotalTestsExecuted() {
        return totalTestsExecuted;
    }
    
    @Override
    public int getPassedTestsExecuted() {
        return passedTestsExecuted;
    }
    
    @Override
    public int getFailedTestsExecuted() {
        return failedTestsExecuted;
    }
    
    @Override
    public int getSkippedTestsExecuted() {
        return skippedTestsExecuted;
    }
    
    /**
     * Gets the notification configuration.
     * @return The NotificationConfig instance
     */
    public NotificationConfig getConfig() {
        return config;
    }
    
    /**
     * Gets the notification sender.
     * @return The NotificationSender instance
     */
    public NotificationSender getSender() {
        return sender;
    }
    
    /**
     * Gets the set of all failed test results.
     * @return Set of failed test results
     */
    public Set<ITestResult> getAllFailedResults() {
        return new HashSet<>(allFailedResults);
    }
    
    /**
     * Gets the execution start time.
     * @return Execution start time in milliseconds
     */
    public long getExecutionStartTime() {
        return executionStartTime;
    }
}
