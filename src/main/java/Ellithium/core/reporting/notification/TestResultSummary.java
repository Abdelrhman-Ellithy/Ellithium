package Ellithium.core.reporting.notification;

import org.testng.ITestResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * Collects and formats test results for notifications.
 * Generates rich HTML-formatted email content with professional styling.
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
     * Calculates the success rate percentage.
     * @return Success rate percentage
     */
    public double getSuccessRate() {
        if (totalTests == 0) return 0.0;
        return (double) passedTests / totalTests * 100;
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
        message.append("📈 Success Rate: ").append(String.format("%.1f%%", getSuccessRate())).append("\n\n");
        
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
     * Generates rich HTML-formatted email content with table format.
     * @return HTML-formatted email body
     */
    public String generateHtmlEmailBody() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String status = hasFailures() ? "FAILED" : "PASSED";
        String statusColor = hasFailures() ? "#dc3545" : "#28a745";
        String statusIcon = hasFailures() ? "❌" : "✅";
        
        StringBuilder html = new StringBuilder();
        
        // Build HTML structure
        appendHtmlHeader(html);
        appendHtmlStyles(html);
        appendHtmlBodyStart(html);
        
        // Build content sections
        appendHeaderSection(html, status, statusColor, statusIcon);
        appendSummaryTable(html);
        appendProgressSection(html);
        appendExecutionInfoSection(html, dateFormat);
        appendFailedTestsSection(html);
        
        // Close HTML structure
        appendHtmlBodyEnd(html);
        
        return html.toString();
    }
    
    private void appendHtmlHeader(StringBuilder html) {
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"en\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>Ellithium Test Execution Report</title>");
    }
    
    private void appendHtmlStyles(StringBuilder html) {
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f8f9fa; }");
        html.append(".container { max-width: 900px; margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 300; }");
        html.append(".header .subtitle { margin: 10px 0 0 0; font-size: 16px; opacity: 0.9; }");
        html.append(".status-badge { display: inline-block; background-color: var(--status-color); color: white; padding: 8px 16px; border-radius: 20px; font-weight: bold; font-size: 14px; margin-top: 10px; }");
        html.append(".content { padding: 30px; }");
        html.append(".summary-table { width: 100%; border-collapse: collapse; margin: 30px 0; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); }");
        html.append(".summary-table th { background-color: #495057; color: white; padding: 15px; text-align: center; font-weight: 600; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; }");
        html.append(".summary-table td { padding: 20px; text-align: center; border-bottom: 1px solid #e9ecef; font-size: 16px; }");
        html.append(".summary-table tr:last-child td { border-bottom: none; }");
        html.append(".summary-table .passed { color: #28a745; font-weight: bold; font-size: 24px; }");
        html.append(".summary-table .failed { color: #dc3545; font-weight: bold; font-size: 24px; }");
        html.append(".summary-table .skipped { color: #ffc107; font-weight: bold; font-size: 24px; }");
        html.append(".summary-table .total { color: #007bff; font-weight: bold; font-size: 24px; }");
        html.append(".summary-table .label { font-size: 12px; color: #6c757d; text-transform: uppercase; letter-spacing: 1px; margin-top: 5px; display: block; }");
        html.append(".execution-info { background-color: #e9ecef; border-radius: 8px; padding: 20px; margin: 30px 0; }");
        html.append(".execution-info h3 { margin: 0 0 15px 0; color: #495057; font-size: 18px; }");
        html.append(".execution-table { width: 100%; border-collapse: collapse; }");
        html.append(".execution-table td { padding: 12px 0; border-bottom: 1px solid #dee2e6; }");
        html.append(".execution-table td:first-child { font-weight: 600; color: #495057; width: 40%; }");
        html.append(".execution-table td:last-child { color: #6c757d; }");
        html.append(".execution-table tr:last-child td { border-bottom: none; }");
        html.append(".failed-tests { margin-top: 30px; }");
        html.append(".failed-tests h3 { color: #dc3545; margin-bottom: 20px; }");
        html.append(".failed-table { width: 100%; border-collapse: collapse; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); }");
        html.append(".failed-table th { background-color: #dc3545; color: white; padding: 15px; text-align: left; font-weight: 600; font-size: 14px; }");
        html.append(".failed-table td { padding: 15px; border-bottom: 1px solid #fed7d7; }");
        html.append(".failed-table tr:last-child td { border-bottom: none; }");
        html.append(".failed-table .test-name { font-weight: 600; color: #c53030; }");
        html.append(".failed-table .test-class { font-size: 12px; color: #718096; }");
        html.append(".failed-table .test-error { background-color: #fed7d7; border-radius: 4px; padding: 10px; font-family: 'Courier New', monospace; font-size: 12px; color: #c53030; margin-top: 5px; }");
        html.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 12px; border-top: 1px solid #dee2e6; }");
        html.append(".progress-section { margin: 30px 0; }");
        html.append(".progress-section h3 { margin-bottom: 15px; color: #495057; font-size: 18px; }");
        html.append(".progress-bar { background-color: #e9ecef; border-radius: 10px; height: 20px; overflow: hidden; margin: 10px 0; }");
        html.append(".progress-fill { height: 100%; background: linear-gradient(90deg, #28a745 0%, #20c997 100%); border-radius: 10px; transition: width 0.3s ease; }");
        html.append(".progress-fill.failed { background: linear-gradient(90deg, #dc3545 0%, #fd7e14 100%); }");
        html.append(".progress-text { text-align: center; margin-top: 10px; font-weight: 600; color: #495057; }");
        html.append("@media (max-width: 600px) { .summary-table, .execution-table, .failed-table { font-size: 12px; } .summary-table td, .summary-table th { padding: 10px 5px; } }");
        html.append("</style>");
        html.append("</head>");
    }
    
    private void appendHtmlBodyStart(StringBuilder html) {
        html.append("<body>");
        html.append("<div class=\"container\">");
    }
    
    private void appendHeaderSection(StringBuilder html, String status, String statusColor, String statusIcon) {
        html.append("<div class=\"header\">");
        html.append("<h1>🚀 Ellithium Test Execution Report</h1>");
        html.append("<div class=\"subtitle\">Automated Test Results Summary</div>");
        html.append("<div class=\"status-badge\" style=\"--status-color: ").append(statusColor).append(";\">").append(statusIcon).append(" ").append(status).append("</div>");
        html.append("</div>");
        html.append("<div class=\"content\">");
    }
    
    private void appendSummaryTable(StringBuilder html) {
        html.append("<table class=\"summary-table\">");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Test Results Summary</th>");
        html.append("<th>Count</th>");
        html.append("<th>Percentage</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");
        
        // Passed tests row
        html.append("<tr>");
        html.append("<td>✅ Passed Tests</td>");
        html.append("<td class=\"passed\">").append(passedTests).append("<span class=\"label\">Tests</span></td>");
        html.append("<td>").append(String.format("%.1f%%", getSuccessRate())).append("</td>");
        html.append("</tr>");
        
        // Failed tests row
        html.append("<tr>");
        html.append("<td>❌ Failed Tests</td>");
        html.append("<td class=\"failed\">").append(failedTests).append("<span class=\"label\">Tests</span></td>");
        html.append("<td>").append(String.format("%.1f%%", getFailureRate())).append("</td>");
        html.append("</tr>");
        
        // Skipped tests row
        html.append("<tr>");
        html.append("<td>⏭️ Skipped Tests</td>");
        html.append("<td class=\"skipped\">").append(skippedTests).append("<span class=\"label\">Tests</span></td>");
        html.append("<td>").append(String.format("%.1f%%", totalTests > 0 ? (double) skippedTests / totalTests * 100 : 0.0)).append("</td>");
        html.append("</tr>");
        
        // Total tests row
        html.append("<tr>");
        html.append("<td>📊 Total Tests</td>");
        html.append("<td class=\"total\">").append(totalTests).append("<span class=\"label\">Tests</span></td>");
        html.append("<td>100.0%</td>");
        html.append("</tr>");
        
        html.append("</tbody>");
        html.append("</table>");
    }
    
    private void appendProgressSection(StringBuilder html) {
        html.append("<div class=\"progress-section\">");
        html.append("<h3>📈 Success Rate Progress</h3>");
        html.append("<div class=\"progress-bar\">");
        String progressClass = hasFailures() ? "failed" : "";
        html.append("<div class=\"progress-fill ").append(progressClass).append("\" style=\"width: ").append(getSuccessRate()).append("%\"></div>");
        html.append("</div>");
        html.append("<div class=\"progress-text\">").append(String.format("%.1f%% Success Rate (%d/%d tests passed)", getSuccessRate(), passedTests, totalTests)).append("</div>");
        html.append("</div>");
    }
    
    private void appendExecutionInfoSection(StringBuilder html, SimpleDateFormat dateFormat) {
        html.append("<div class=\"execution-info\">");
        html.append("<h3>📊 Execution Details</h3>");
        html.append("<table class=\"execution-table\">");
        
        // Execution Date
        html.append("<tr>");
        html.append("<td>Execution Date:</td>");
        html.append("<td>").append(dateFormat.format(executionDate)).append("</td>");
        html.append("</tr>");
        
        // Total Duration
        html.append("<tr>");
        html.append("<td>Total Duration:</td>");
        html.append("<td>").append(getFormattedExecutionTime()).append("</td>");
        html.append("</tr>");
        
        // Report Type
        html.append("<tr>");
        html.append("<td>Report Type:</td>");
        html.append("<td>Allure Reports</td>");
        html.append("</tr>");
        
        // Execution Status
        String status = hasFailures() ? "FAILED" : "PASSED";
        String statusColor = hasFailures() ? "#dc3545" : "#28a745";
        html.append("<tr>");
        html.append("<td>Execution Status:</td>");
        html.append("<td><strong style=\"color: ").append(statusColor).append(";\">").append(status).append("</strong></td>");
        html.append("</tr>");
        
        html.append("</table>");
        html.append("</div>");
    }
    
    private void appendFailedTestsSection(StringBuilder html) {
        if (hasFailures() && !failedTestResults.isEmpty()) {
            html.append("<div class=\"failed-tests\">");
            html.append("<h3>❌ Failed Test Details</h3>");
            html.append("<table class=\"failed-table\">");
            html.append("<thead>");
            html.append("<tr>");
            html.append("<th>Test Name</th>");
            html.append("<th>Test Class</th>");
            html.append("<th>Error Details</th>");
            html.append("</tr>");
            html.append("</thead>");
            html.append("<tbody>");
            
            for (ITestResult failedTest : failedTestResults) {
                String testName = failedTest.getName();
                String className = failedTest.getTestClass() != null ? failedTest.getTestClass().getName() : "Unknown Class";
                Throwable throwable = failedTest.getThrowable();
                String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";
                
                html.append("<tr>");
                html.append("<td class=\"test-name\">").append(testName).append("</td>");
                html.append("<td class=\"test-class\">").append(className).append("</td>");
                html.append("<td><div class=\"test-error\">").append(errorMessage).append("</div></td>");
                html.append("</tr>");
            }
            
            html.append("</tbody>");
            html.append("</table>");
            html.append("</div>");
        }
    }
    
    private void appendHtmlBodyEnd(StringBuilder html) {
        html.append("</div>");
        html.append("<div class=\"footer\">");
        html.append("<p>Generated by Ellithium Test Automation Framework</p>");
        html.append("<p>For detailed reports, please check the Allure HTML report</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
    }
    
    /**
     * Generates an email subject line.
     * @return Email subject
     */
    public String generateEmailSubject() {
        NotificationConfig config = NotificationConfig.getInstance();
        String prefix = config.getEmailSubjectPrefix();
        String status = hasFailures() ? "FAILED" : "PASSED";
        return prefix + " - Test Execution - " + status + " (" + passedTests + "/" + totalTests + ")";
    }
}
