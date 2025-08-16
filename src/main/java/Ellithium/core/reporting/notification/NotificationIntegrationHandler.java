package Ellithium.core.reporting.notification;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Method;

/**
 * Handles integration between the notification system and test frameworks.
 * Manages test result collection and notification sending at execution completion.
 * Implements graceful error handling to prevent test execution blocking.
 */
public class NotificationIntegrationHandler implements TestResultCollector {
    
    private NotificationConfig config;
    private NotificationSender sender;
    
    private long totalTestsExecuted = 0;
    private long passedTestsExecuted = 0;
    private long failedTestsExecuted = 0;
    private long skippedTestsExecuted = 0;
    private Set<ITestResult> allFailedResults = new HashSet<>();
    private long executionStartTime = 0;
    
    /**
     * Default constructor that uses lazy initialization.
     * No heavy operations are performed until notifications are actually needed.
     */
    public NotificationIntegrationHandler() {
        // Lazy initialization - don't create instances until needed
        this.config = null;
        this.sender = null;
    }
    
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
     * Checks if notifications are enabled without loading the full configuration.
     * This provides early exit for performance optimization.
     * @return true if notifications are enabled, false otherwise
     */
    public boolean isNotificationSystemEnabled() {
        try {
            // Quick check without full initialization
            return NotificationConfig.isNotificationEnabledQuick();
        } catch (Exception e) {
            // If we can't check, assume disabled for safety
            return false;
        }
    }
    
    /**
     * Lazy initialization of notification system.
     * Only initializes when notifications are actually enabled.
     */
    private void ensureNotificationSystemInitialized() {
        if (config == null || sender == null) {
            // Only initialize if not already done and notifications are enabled
            if (isNotificationSystemEnabled()) {
                this.config = NotificationConfig.getInstance();
                this.sender = new NotificationSender();
                // Initialize the system
                initializeTestResultCollection();
            }
        }
    }
    
    @Override
    public void initializeTestResultCollection() {
        try {
            executionStartTime = System.currentTimeMillis();
            totalTestsExecuted = 0;
            passedTestsExecuted = 0;
            failedTestsExecuted = 0;
            skippedTestsExecuted = 0;
            allFailedResults.clear();
            
            Reporter.log("Test result collection system initialized", LogLevel.INFO_BLUE);
        } catch (Exception e) {
            Reporter.log("Failed to initialize test result collection: " + e.getMessage(), LogLevel.ERROR);
        }
    }
    
    @Override
    public void collectTestResults(ITestContext context) {
        // Early exit if notifications are disabled
        if (!isNotificationSystemEnabled()) {
            return;
        }
        
        try {
            // Ensure system is initialized
            ensureNotificationSystemInitialized();
            
            int testngTests = 0;
            int testngPassed = 0;
            int testngFailed = 0;
            int testngSkipped = 0;
            
            // Process passed tests
            for (ITestResult result : context.getPassedTests().getAllResults()) {
                if (!shouldExcludeFromTestNGCounting(result)) {
                    testngPassed++;
                    testngTests++;
                }
            }
            
            // Process failed tests
            for (ITestResult result : context.getFailedTests().getAllResults()) {
                if (!shouldExcludeFromTestNGCounting(result)) {
                    testngFailed++;
                    testngTests++;
                    allFailedResults.add(result);
                }
            }
            
            // Process skipped tests
            for (ITestResult result : context.getSkippedTests().getAllResults()) {
                if (!shouldExcludeFromTestNGCounting(result)) {
                    testngSkipped++;
                    testngTests++;
                }
            }
            
            // Update totals
            totalTestsExecuted += testngTests;
            passedTestsExecuted += testngPassed;
            failedTestsExecuted += testngFailed;
            skippedTestsExecuted += testngSkipped;
            
            Reporter.log("Collected TestNG test results from context: " + context.getName() + 
                       " (TestNG Tests: " + testngTests + ", Passed: " + testngPassed + 
                       ", Failed: " + testngFailed + ", Skipped: " + testngSkipped + ")", LogLevel.INFO_BLUE);
        } catch (Exception e) {
            Reporter.log("Failed to collect test results from context: " + context.getName() + " - " + e.getMessage(), LogLevel.ERROR);
        }
    }
    
    @Override
    public void collectCucumberTestResult(String scenarioName, String status, long executionTime) {
        // Early exit if notifications are disabled
        if (!isNotificationSystemEnabled()) {
            return;
        }
        
        try {
            // Ensure system is initialized
            ensureNotificationSystemInitialized();
            
            totalTestsExecuted++;
            
            switch (status.toLowerCase()) {
                case "passed":
                    passedTestsExecuted++;
                    break;
                case "failed":
                    failedTestsExecuted++;
                    break;
                case "skipped":
                    skippedTestsExecuted++;
                    break;
                default:
                    Reporter.log("Unknown Cucumber test status: " + status + " for scenario: " + scenarioName, LogLevel.WARN);
                    break;
            }
            
            Reporter.log("Collected Cucumber test result: " + scenarioName + " - " + status, LogLevel.DEBUG);
        } catch (Exception e) {
            Reporter.log("Failed to collect Cucumber test result: " + scenarioName + " - " + e.getMessage(), LogLevel.ERROR);
        }
    }
    
    /**
     * Gets the set of all failed test results.
     * @return Set of failed test results
     */
    private Set<ITestResult> getAllFailedResults() {
        return new HashSet<>(allFailedResults);
    }
    
    /**
     * Calculates the total execution time.
     * @return Execution time in milliseconds
     */
    private long getTotalExecutionTime() {
        if (executionStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - executionStartTime;
    }
    
    /**
     * Sends execution completion notifications.
     * @return true if notifications were sent successfully
     */
    public boolean sendExecutionCompletionNotifications() {
        // Early exit if notifications are disabled
        if (!isNotificationSystemEnabled()) {
            return false;
        }
        
        try {
            // Ensure system is initialized
            ensureNotificationSystemInitialized();
            
            if (!config.isNotificationEnabled()) {
                Reporter.log("Notifications are disabled. Skipping notification sending.", LogLevel.INFO_BLUE);
                return false;
            }
            
            if (totalTestsExecuted == 0) {
                Reporter.log("No tests executed. Skipping notification sending.", LogLevel.INFO_BLUE);
                return false;
            }
            
            // Check if we should send notifications based on configuration
            boolean shouldSendNotification = false;
            String notificationReason = "";
            
            if (failedTestsExecuted > 0 && config.shouldSendOnFailure()) {
                shouldSendNotification = true;
                notificationReason = "Test failures detected";
            }
            
            if (exceedsFailureThreshold()) {
                shouldSendNotification = true;
                notificationReason = "Failure rate (" + String.format("%.1f%%", getFailureRate()) + 
                                  "%) exceeds threshold (" + config.getFailureThreshold() + "%)";
            }
            
            if (config.shouldSendOnCompletion()) {
                shouldSendNotification = true;
                notificationReason = "Test execution completed";
            }
            
            if (!shouldSendNotification) {
                Reporter.log("No notification criteria met. Skipping execution completion notification.", LogLevel.INFO_BLUE);
                return false;
            }
            
            Reporter.log("Sending execution completion notification: " + notificationReason, LogLevel.INFO_BLUE);
            
            TestResultSummary summary = new TestResultSummary(
                totalTestsExecuted,
                passedTestsExecuted,
                failedTestsExecuted,
                skippedTestsExecuted,
                getTotalExecutionTime(),
                getAllFailedResults()
            );
            
            String subject = generateEmailSubject(summary);
            String emailBody = summary.generateHtmlEmailBody();
            String slackMessage = summary.generateSummaryMessage();
            
            boolean notificationsSent = sender.sendNotifications(subject, emailBody, slackMessage);
            
            if (notificationsSent) {
                Reporter.log("Execution completion notifications sent successfully", LogLevel.INFO_GREEN);
            } else {
                Reporter.log("Failed to send execution completion notifications", LogLevel.ERROR);
            }
            
            return notificationsSent;
            
        } catch (Exception e) {
            Reporter.log("Failed to send execution completion notifications: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Generates an email subject line.
     * @param summary The test result summary
     * @return Email subject
     */
    private String generateEmailSubject(TestResultSummary summary) {
        try {
            String prefix = config.getEmailSubjectPrefix();
            String status = summary.getFailedTests() > 0 ? "FAILED" : "PASSED";
            return prefix + " - Test Execution - " + status + " (" + summary.getPassedTests() + "/" + summary.getTotalTests() + ")";
        } catch (Exception e) {
            Reporter.log("Failed to generate email subject: " + e.getMessage(), LogLevel.ERROR);
            return "Ellithium Test Results";
        }
    }
    
    /**
     * Determines if a test result should be excluded from TestNG counting.
     * This prevents double counting when both TestNG and Cucumber listeners are active.
     * @param result The test result to check
     * @return true if the test should be excluded from TestNG counting
     */
    public boolean shouldExcludeFromTestNGCounting(ITestResult result) {
        try {
            return isCucumberTestByName(result) || 
                   isCucumberTestByClass(result) || 
                   isCucumberTestByAnnotation(result);
        } catch (Exception e) {
            Reporter.log("Failed to determine if test should be excluded: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
    
    /**
     * Determines if a test result is from Cucumber (interface requirement).
     * @param result The test result to check
     * @return true if it's a Cucumber test, false otherwise
     */
    @Override
    public boolean isCucumberTest(ITestResult result) {
        return shouldExcludeFromTestNGCounting(result);
    }
    
    private boolean isCucumberTestByName(ITestResult result) {
        String testName = result.getName();
        return testName != null && testName.equals("runScenario");
    }
    
    private boolean isCucumberTestByClass(ITestResult result) {
        String className = result.getTestClass().getName();
        return className != null && (
            className.toLowerCase().contains("cucumber") ||
            className.toLowerCase().contains("feature") ||
            className.toLowerCase().contains("stepdef")
        );
    }
    
    private boolean isCucumberTestByAnnotation(ITestResult result) {
        try {
            Method method = result.getMethod().getConstructorOrMethod().getMethod();
            if (method != null) {
                return hasCucumberAnnotation(method);
            }
        } catch (Exception e) {
            Reporter.log("Failed to check Cucumber annotations: " + e.getMessage(), LogLevel.DEBUG);
        }
        return false;
    }
    
    private boolean hasCucumberAnnotation(Method method) {
        try {
            return method.isAnnotationPresent(io.cucumber.java.en.Given.class) ||
                   method.isAnnotationPresent(io.cucumber.java.en.When.class) ||
                   method.isAnnotationPresent(io.cucumber.java.en.Then.class) ||
                   method.isAnnotationPresent(io.cucumber.java.en.And.class) ||
                   method.isAnnotationPresent(io.cucumber.java.en.But.class);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculates the failure rate percentage.
     * @return Failure rate as a percentage
     */
    private double getFailureRate() {
        if (totalTestsExecuted == 0) {
            return 0.0;
        }
        return (double) failedTestsExecuted / totalTestsExecuted * 100;
    }
    
    /**
     * Checks if the failure rate exceeds the configured threshold.
     * @return true if failure rate exceeds threshold
     */
    private boolean exceedsFailureThreshold() {
        return getFailureRate() > config.getFailureThreshold();
    }
}
