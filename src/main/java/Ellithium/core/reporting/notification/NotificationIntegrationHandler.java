package Ellithium.core.reporting.notification;

import Ellithium.core.logging.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Method;

/**
 * Handles integration between the notification system and test frameworks.
 * Manages test result collection and notification sending at execution completion.
 * Follows proper object-oriented design with dependency injection.
 * Implements TestResultCollector interface for framework-agnostic result collection.
 */
public class NotificationIntegrationHandler implements TestResultCollector {
    
    private final NotificationConfig config;
    private final NotificationSender sender;
    
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
        int testngTests = 0;
        int testngPassed = 0;
        int testngFailed = 0;
        int testngSkipped = 0;
        for (ITestResult result : context.getPassedTests().getAllResults()) {
            if (!isCucumberTest(result)) {
                testngPassed++;
                testngTests++;
            }
        }
        
        for (ITestResult result : context.getFailedTests().getAllResults()) {
            if (!isCucumberTest(result)) {
                testngFailed++;
                testngTests++;
                allFailedResults.add(result);
            }
        }
        
        for (ITestResult result : context.getSkippedTests().getAllResults()) {
            if (!isCucumberTest(result)) {
                testngSkipped++;
                testngTests++;
            }
        }
        
        totalTestsExecuted += testngTests;
        passedTestsExecuted += testngPassed;
        failedTestsExecuted += testngFailed;
        skippedTestsExecuted += testngSkipped;
        
        Logger.info("Collected TestNG test results from context: " + context.getName() + 
                   " (TestNG Tests: " + testngTests + ", Passed: " + testngPassed + 
                   ", Failed: " + testngFailed + ", Skipped: " + testngSkipped + ")");
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
    
    @Override
    public boolean isCucumberTest(ITestResult result) {
        if (result == null) {
            return false;
        }
        String testName = result.getName();
        if (testName == null) {
            return false;
        }
        if ("runScenario".equals(testName)) {
            return true;
        }
        Class<?> testClass = result.getTestClass().getRealClass();
        if (testClass != null) {
            String className = testClass.getName();
            if (className.toLowerCase().contains("cucumber") || 
                className.toLowerCase().contains("feature") ||
                className.toLowerCase().contains("stepdef")) {
                return true;
            }
        }
        Method testMethod = result.getMethod().getConstructorOrMethod().getMethod();
        if (testMethod != null) {
            if (testMethod.isAnnotationPresent(io.cucumber.java.en.Given.class) ||
                testMethod.isAnnotationPresent(io.cucumber.java.en.When.class) ||
                testMethod.isAnnotationPresent(io.cucumber.java.en.Then.class) ||
                testMethod.isAnnotationPresent(io.cucumber.java.en.And.class) ||
                testMethod.isAnnotationPresent(io.cucumber.java.en.But.class)) {
                return true;
            }
        }
        
        return false;
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
            long totalExecutionTime = System.currentTimeMillis() - executionStartTime;
            
            Logger.info("Preparing execution completion notification for " + totalTestsExecuted + " tests");
            
            TestResultSummary summary = new TestResultSummary(
                totalTestsExecuted, 
                passedTestsExecuted, 
                failedTestsExecuted, 
                skippedTestsExecuted, 
                totalExecutionTime, 
                allFailedResults
            );
            
            boolean shouldSendNotification = false;
            String notificationReason = "";
            
            if (summary.hasFailures() && config.shouldSendOnFailure()) {
                shouldSendNotification = true;
                notificationReason = "Test failures detected";
            }
            
            if (summary.exceedsFailureThreshold(config.getFailureThreshold())) {
                shouldSendNotification = true;
                notificationReason = "Failure rate (" + String.format("%.1f%%", summary.getFailureRate()) + 
                                  "%) exceeds threshold (" + config.getFailureThreshold() + "%)";
            }
            
            if (config.shouldSendOnCompletion()) {
                shouldSendNotification = true;
                notificationReason = "Test execution completed";
            }
            
            if (shouldSendNotification) {
                Logger.info("Sending execution completion notification: " + notificationReason);
                
                String subject = summary.generateEmailSubject();
                String message = summary.generateSummaryMessage();
                String htmlMessage = summary.generateHtmlEmailBody();
                
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
