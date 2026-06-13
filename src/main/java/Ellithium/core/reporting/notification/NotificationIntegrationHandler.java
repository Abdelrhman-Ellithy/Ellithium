package Ellithium.core.reporting.notification;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles integration between the notification system and test frameworks.
 * Manages test result collection and notification sending at execution completion.
 * Implements graceful error handling to prevent test execution blocking.
 */
public class NotificationIntegrationHandler implements TestResultCollector {

    private NotificationConfig config;
    private NotificationSender sender;

    private final AtomicLong totalTestsExecuted = new AtomicLong(0);
    private final AtomicLong passedTestsExecuted = new AtomicLong(0);
    private final AtomicLong failedTestsExecuted = new AtomicLong(0);
    private final AtomicLong skippedTestsExecuted = new AtomicLong(0);
    private final Set<ITestResult> allFailedResults = ConcurrentHashMap.newKeySet();
    private volatile long executionStartTime = 0;

    /**
     * Default constructor that uses lazy initialization.
     * No heavy operations are performed until notifications are actually needed.
     */
    public NotificationIntegrationHandler() {
        this.config = null;
        this.sender = null;
    }

    /**
     * Checks if notifications are enabled without loading the full configuration.
     * This provides early exit for performance optimization.
     * @return true if notifications are enabled, false otherwise
     */
    public boolean isNotificationSystemEnabled() {
        try {
            return NotificationConfig.isNotificationEnabledQuick();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Lazy initialization of notification system.
     * Only initializes when notifications are actually enabled.
     */
    private void ensureNotificationSystemInitialized() {
        if (config == null || sender == null) {
            if (isNotificationSystemEnabled()) {
                this.config = NotificationConfig.getInstance();
                this.sender = new NotificationSender();
                initializeTestResultCollection();
            }
        }
    }

    @Override
    public void initializeTestResultCollection() {
        try {
            executionStartTime = System.currentTimeMillis();
            totalTestsExecuted.set(0);
            passedTestsExecuted.set(0);
            failedTestsExecuted.set(0);
            skippedTestsExecuted.set(0);
            allFailedResults.clear();
            Reporter.log("Test result collection system initialized", LogLevel.INFO_BLUE);
        } catch (Exception e) {
            Reporter.log("Failed to initialize test result collection: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    @Override
    public void collectTestResults(ITestContext context) {
        if (!isNotificationSystemEnabled()) {
            return;
        }
        try {
            ensureNotificationSystemInitialized();

            int testngTests = 0;
            int testngPassed = 0;
            int testngFailed = 0;
            int testngSkipped = 0;

            for (ITestResult result : context.getPassedTests().getAllResults()) {
                if (!shouldExcludeFromTestNGCounting(result)) {
                    testngPassed++;
                    testngTests++;
                }
            }
            for (ITestResult result : context.getFailedTests().getAllResults()) {
                if (!shouldExcludeFromTestNGCounting(result)) {
                    testngFailed++;
                    testngTests++;
                    allFailedResults.add(result);
                }
            }
            for (ITestResult result : context.getSkippedTests().getAllResults()) {
                if (!shouldExcludeFromTestNGCounting(result)) {
                    testngSkipped++;
                    testngTests++;
                }
            }

            totalTestsExecuted.addAndGet(testngTests);
            passedTestsExecuted.addAndGet(testngPassed);
            failedTestsExecuted.addAndGet(testngFailed);
            skippedTestsExecuted.addAndGet(testngSkipped);

            Reporter.log("Collected TestNG test results from context: " + context.getName() +
                       " (TestNG Tests: " + testngTests + ", Passed: " + testngPassed +
                       ", Failed: " + testngFailed + ", Skipped: " + testngSkipped + ")", LogLevel.INFO_BLUE);
        } catch (Exception e) {
            Reporter.log("Failed to collect test results from context: " + context.getName() + " - " + e.getMessage(), LogLevel.ERROR);
        }
    }

    @Override
    public void collectCucumberTestResult(String scenarioName, String status, long executionTime) {
        if (!isNotificationSystemEnabled()) {
            return;
        }
        try {
            ensureNotificationSystemInitialized();
            totalTestsExecuted.incrementAndGet();
            switch (status.toLowerCase()) {
                case "passed"  -> passedTestsExecuted.incrementAndGet();
                case "failed"  -> failedTestsExecuted.incrementAndGet();
                case "skipped" -> skippedTestsExecuted.incrementAndGet();
                default -> Reporter.log("Unknown Cucumber test status: " + status + " for scenario: " + scenarioName, LogLevel.WARN);
            }
            Reporter.log("Collected Cucumber test result: " + scenarioName + " - " + status, LogLevel.DEBUG);
        } catch (Exception e) {
            Reporter.log("Failed to collect Cucumber test result: " + scenarioName + " - " + e.getMessage(), LogLevel.ERROR);
        }
    }

    private Set<ITestResult> getAllFailedResults() {
        return new HashSet<>(allFailedResults);
    }

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
        if (!isNotificationSystemEnabled()) {
            return false;
        }
        try {
            ensureNotificationSystemInitialized();

            if (config == null || !config.isNotificationEnabled()) {
                Reporter.log("Notifications are disabled. Skipping notification sending.", LogLevel.INFO_BLUE);
                return false;
            }
            if (totalTestsExecuted.get() == 0) {
                Reporter.log("No tests executed. Skipping notification sending.", LogLevel.INFO_BLUE);
                return false;
            }

            boolean shouldSendNotification = false;
            String notificationReason = "";

            if (failedTestsExecuted.get() > 0 && config.shouldSendOnFailure()) {
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
                totalTestsExecuted.get(),
                passedTestsExecuted.get(),
                failedTestsExecuted.get(),
                skippedTestsExecuted.get(),
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

    @Override
    public boolean isCucumberTest(ITestResult result) {
        return shouldExcludeFromTestNGCounting(result);
    }

    private boolean isCucumberTestByName(ITestResult result) {
        String testName = result.getName();
        return testName != null && testName.equals("runScenario");
    }

    private boolean isCucumberTestByClass(ITestResult result) {
        if (result.getTestClass() == null) {
            return false;
        }
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

    private double getFailureRate() {
        long total = totalTestsExecuted.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) failedTestsExecuted.get() / total * 100;
    }

    private boolean exceedsFailureThreshold() {
        if (config == null) {
            return false;
        }
        return getFailureRate() > config.getFailureThreshold();
    }
}
