package Ellithium.core.reporting.notification;

import org.testng.ITestResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * Collects and formats test results for notifications.
 */
public class TestResultSummary {
    
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int skippedTests;
    private long executionTime;
    private Date executionDate;
    private Set<ITestResult> failedTestResults;
    
    /**
     * Creates a test result summary from overall execution results.
     * @param totalTests Total number of tests executed
     * @param passedTests Number of passed tests
     * @param failedTests Number of failed tests
     * @param skippedTests Number of skipped tests
     * @param executionTime Total execution time in milliseconds
     * @param failedTestResults Set of failed test results
     */
    public TestResultSummary(int totalTests, int passedTests, int failedTests, int skippedTests, 
                           long executionTime, Set<ITestResult> failedTestResults) {
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.skippedTests = skippedTests;
        this.executionTime = executionTime;
        this.executionDate = new Date();
        this.failedTestResults = failedTestResults;
    }
    
    /**
     * Calculates the failure rate percentage.
     * @return Failure rate percentage
     */
    public double getFailureRate() {
        if (totalTests == 0) return 0.0;
        return (double) failedTests / totalTests * 100;
    }
    
    /**
     * Checks if any tests failed.
     * @return true if any tests failed
     */
    public boolean hasFailures() {
        return failedTests > 0;
    }
    
    /**
     * Checks if the failure rate exceeds the threshold.
     * @param threshold The failure threshold percentage
     * @return true if failure rate exceeds threshold
     */
    public boolean exceedsFailureThreshold(int threshold) {
        return getFailureRate() > threshold;
    }
    
    /**
     * Formats the execution time as a readable string.
     * @return Formatted execution time
     */
    public String getFormattedExecutionTime() {
        long seconds = executionTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Generates a summary message for notifications.
     * @return Formatted summary message
     */
    public String generateSummaryMessage() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        StringBuilder message = new StringBuilder();
        message.append("🚀 *Ellithium Test Execution Summary*\n\n");
        message.append("*Date:* ").append(dateFormat.format(executionDate)).append("\n");
        message.append("*Duration:* ").append(getFormattedExecutionTime()).append("\n\n");
        
        message.append("*Test Results:*\n");
        message.append("✅ Passed: ").append(passedTests).append("\n");
        message.append("❌ Failed: ").append(failedTests).append("\n");
        message.append("⏭️ Skipped: ").append(skippedTests).append("\n");
        message.append("📊 Total: ").append(totalTests).append("\n");
        message.append("📈 Success Rate: ").append(String.format("%.1f%%", (100 - getFailureRate()))).append("\n\n");
        
        if (hasFailures()) {
            message.append("*Failed Tests:*\n");
            for (ITestResult failedTest : failedTestResults) {
                String testName = failedTest.getName();
                String className = failedTest.getTestClass().getName();
                Throwable throwable = failedTest.getThrowable();
                String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";
                
                message.append("• ").append(testName).append(" (").append(className).append(")\n");
                message.append("  Error: ").append(errorMessage).append("\n\n");
            }
        }
        
        return message.toString();
    }
    
    /**
     * Generates an email subject line.
     * @return Email subject
     */
    public String generateEmailSubject() {
        String prefix = NotificationConfig.getProperty(NotificationConfig.EMAIL_SUBJECT_PREFIX);
        String status = hasFailures() ? "FAILED" : "PASSED";
        return prefix + " - Test Execution - " + status + " (" + passedTests + "/" + totalTests + ")";
    }
}
