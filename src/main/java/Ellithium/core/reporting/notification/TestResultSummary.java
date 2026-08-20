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

    private final long totalTests;
    private final long passedTests;
    private final long failedTests;
    private final long skippedTests;
    private final long executionTime;
    private final Date executionDate;
    private final Set<ITestResult> failedTestResults;

    /**
     * Creates a test result summary from overall execution results.
     * @param totalTests Total number of tests executed
     * @param passedTests Number of passed tests
     * @param failedTests Number of failed tests
     * @param skippedTests Number of skipped tests
     * @param executionTime Total execution time in milliseconds
     * @param failedTestResults Set of failed test results
     */
    public TestResultSummary(long totalTests, long passedTests, long failedTests, long skippedTests,
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
        return failedTests > 0 && failedTestResults != null && !failedTestResults.isEmpty();
    }

    /**
     * Formats the execution time as a readable string.
     * @return Formatted execution time
     */
    public String getFormattedExecutionTime() {
        try {
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
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Generates a summary message for notifications.
     * @return Formatted summary message
     */
    public String generateSummaryMessage() {
        try {
            StringBuilder message = new StringBuilder();
            message.append("🚀 *Ellithium Test Execution Summary*").append(System.lineSeparator()).append(System.lineSeparator());
            message.append("*Date:* ").append(getFormattedExecutionDate()).append(System.lineSeparator());
            message.append("*Duration:* ").append(getFormattedExecutionTime()).append(System.lineSeparator()).append(System.lineSeparator());

            message.append("*Test Results:*").append(System.lineSeparator());
            message.append("✅ Passed: ").append(passedTests).append(System.lineSeparator());
            message.append("❌ Failed: ").append(failedTests).append(System.lineSeparator());
            message.append("⏭️ Skipped: ").append(skippedTests).append(System.lineSeparator());
            message.append("📊 Total: ").append(totalTests).append(System.lineSeparator());
            message.append("📈 Success Rate: ").append(String.format("%.1f%%", getSuccessRate())).append(System.lineSeparator()).append(System.lineSeparator());

            if (hasFailures()) {
                message.append("*Failed Tests:*").append(System.lineSeparator());
                appendFailedTestsToMessage(message);
            }

            return message.toString();
        } catch (Exception e) {
            return "Error generating summary message: " + e.getMessage();
        }
    }

    private void appendFailedTestsToMessage(StringBuilder message) {
        try {
            for (ITestResult failedTest : failedTestResults) {
                String testName = failedTest.getName();
                String className = failedTest.getTestClass() != null ? failedTest.getTestClass().getName() : "Unknown Class";
                Throwable throwable = failedTest.getThrowable();
                String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";

                message.append("• ").append(testName).append(" (").append(className).append(")").append(System.lineSeparator());
                message.append("  Error: ").append(errorMessage).append(System.lineSeparator()).append(System.lineSeparator());
            }
        } catch (Exception e) {
            message.append("Error processing failed tests: ").append(e.getMessage());
        }
    }

    /**
     * Web-safe font stack, inlined on every element below rather than left to a stylesheet.
     * Outlook desktop/MS365 (Word rendering engine) and many corporate mail security gateways
     * strip or ignore {@code <head><style>} content, so anything that only lives there is not
     * guaranteed to reach the reader.
     */
    private static final String FONT_FAMILY = "'Segoe UI', Arial, Helvetica, sans-serif";

    /**
     * Generates rich HTML-formatted email content using a table-based layout with every color,
     * font, and spacing inlined on the element itself, so it renders consistently across email
     * clients that strip {@code <style>} blocks or ignore modern CSS. In particular:
     * <ul>
     *   <li>No CSS custom properties ({@code var(--x)}) — unsupported by Outlook's Word engine,
     *       which silently drops the whole declaration (this is what was making the status badge
     *       and other accent colors disappear in Outlook/MS365).</li>
     *   <li>No div-based layout ({@code max-width}, {@code margin:auto}, flex/grid) — layout is
     *       done with {@code <table>} elements, which every mail client renders consistently.</li>
     *   <li>Every gradient has a solid {@code background-color} fallback declared alongside it,
     *       since Outlook does not paint CSS gradients at all.</li>
     *   <li>Colors are set via both a {@code style} attribute and a legacy {@code bgcolor}
     *       attribute, so the base look survives even when a security gateway strips the
     *       {@code style} attribute itself.</li>
     * </ul>
     * @return HTML-formatted email body
     */
    public String generateHtmlEmailBody() {
        try {
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
            html.append("<tr><td style=\"padding:28px 24px; font-family:").append(FONT_FAMILY).append("; color:#333333;\">");
            appendSummaryTable(html);
            appendProgressSection(html);
            appendExecutionInfoSection(html);
            appendFailedTestsSection(html);
            html.append("</td></tr>");

            // Close HTML structure
            appendHtmlBodyEnd(html);

            return html.toString();
        } catch (Exception e) {
            return "<!DOCTYPE html><html><body><h1>Error generating HTML report</h1><p>" + escapeHtml(e.getMessage()) + "</p></body></html>";
        }
    }

    private void appendHtmlHeader(StringBuilder html) {
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"en\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">");
        html.append("<meta name=\"color-scheme\" content=\"light\">");
        html.append("<meta name=\"supported-color-schemes\" content=\"light\">");
        html.append("<title>Ellithium Test Execution Report</title>");
    }

    /**
     * Minimal progressive-enhancement styling only — a mobile-width tweak for clients that honor
     * {@code <style>} blocks. Every color/spacing/font that actually matters is already inlined on
     * its element by the append* methods below, so clients/gateways that strip this block entirely
     * still render a correct, readable email.
     */
    private void appendHtmlStyles(StringBuilder html) {
        html.append("<style>@media (max-width:620px){.ellithium-wrap{width:100% !important;}}</style>");
        html.append("</head>");
    }

    private void appendHtmlBodyStart(StringBuilder html) {
        html.append("<body style=\"margin:0; padding:0; background-color:#f4f5f7;\">");
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f4f5f7\" style=\"background-color:#f4f5f7;\">");
        html.append("<tr><td align=\"center\" style=\"padding:24px 12px;\">");
        html.append("<table role=\"presentation\" class=\"ellithium-wrap\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\" bgcolor=\"#ffffff\" style=\"width:600px; max-width:600px; background-color:#ffffff;\">");
    }

    private void appendHeaderSection(StringBuilder html, String status, String statusColor, String statusIcon) {
        html.append("<tr><td align=\"center\" bgcolor=\"#5b3fa0\" style=\"background-color:#5b3fa0; background-image:linear-gradient(135deg,#667eea 0%,#764ba2 100%); padding:32px 24px; font-family:")
            .append(FONT_FAMILY).append(";\">");
        html.append("<div style=\"font-size:26px; line-height:32px; font-weight:600; color:#ffffff;\">🚀 Ellithium Test Execution Report</div>");
        html.append("<div style=\"font-size:14px; color:#eef0fb; margin-top:6px;\">Automated Test Results Summary</div>");
        html.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:16px auto 0;\"><tr>");
        html.append("<td bgcolor=\"").append(statusColor).append("\" style=\"background-color:").append(statusColor)
            .append("; padding:8px 18px; border-radius:20px; font-family:").append(FONT_FAMILY)
            .append("; font-size:14px; font-weight:bold; color:#ffffff;\">").append(statusIcon).append(" ").append(status).append("</td>");
        html.append("</tr></table>");
        html.append("</td></tr>");
    }

    private void appendSummaryTable(StringBuilder html) {
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse; margin-bottom:24px;\">");
        html.append("<tr>");
        appendSummaryHeaderCell(html, "Result");
        appendSummaryHeaderCell(html, "Count");
        appendSummaryHeaderCell(html, "Percentage");
        html.append("</tr>");

        appendSummaryRow(html, "✅ Passed Tests", passedTests, getSuccessRate(), "#28a745");
        appendSummaryRow(html, "❌ Failed Tests", failedTests, getFailureRate(), "#dc3545");
        appendSummaryRow(html, "⏭️ Skipped Tests", skippedTests,
                totalTests > 0 ? (double) skippedTests / totalTests * 100 : 0.0, "#b8860b");
        appendSummaryRow(html, "📊 Total Tests", totalTests, 100.0, "#0d6efd");

        html.append("</table>");
    }

    private void appendSummaryHeaderCell(StringBuilder html, String label) {
        html.append("<th align=\"center\" bgcolor=\"#495057\" style=\"background-color:#495057; color:#ffffff; padding:12px; font-family:")
            .append(FONT_FAMILY).append("; font-size:12px; text-transform:uppercase; letter-spacing:1px;\">").append(label).append("</th>");
    }

    private void appendSummaryRow(StringBuilder html, String label, long count, double percentage, String color) {
        html.append("<tr>");
        html.append("<td align=\"center\" style=\"padding:16px; border-bottom:1px solid #e9ecef; font-family:")
            .append(FONT_FAMILY).append(";\">").append(label).append("</td>");
        html.append("<td align=\"center\" style=\"padding:16px; border-bottom:1px solid #e9ecef; font-family:")
            .append(FONT_FAMILY).append("; color:").append(color).append("; font-weight:bold; font-size:22px;\">").append(count).append("</td>");
        html.append("<td align=\"center\" style=\"padding:16px; border-bottom:1px solid #e9ecef; font-family:")
            .append(FONT_FAMILY).append(";\">").append(String.format("%.1f%%", percentage)).append("</td>");
        html.append("</tr>");
    }

    private void appendProgressSection(StringBuilder html) {
        html.append("<div style=\"font-size:16px; font-weight:600; color:#495057; font-family:")
            .append(FONT_FAMILY).append("; margin:24px 0 8px;\">📈 Success Rate Progress</div>");

        // Outlook's Word engine ignores width percentages on divs, but it does honor
        // <td width="%">, so the bar is built from table cells rather than a CSS-width div.
        int filledPercent = (int) Math.round(Math.max(0.0, Math.min(100.0, getSuccessRate())));
        String fillColor = hasFailures() ? "#dc3545" : "#28a745";

        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;\">");
        html.append("<tr style=\"line-height:18px; font-size:1px;\">");
        if (filledPercent > 0) {
            html.append("<td width=\"").append(filledPercent).append("%\" bgcolor=\"").append(fillColor)
                .append("\" style=\"background-color:").append(fillColor).append("; line-height:18px; font-size:1px;\">&nbsp;</td>");
        }
        if (filledPercent < 100) {
            html.append("<td width=\"").append(100 - filledPercent)
                .append("%\" bgcolor=\"#e9ecef\" style=\"background-color:#e9ecef; line-height:18px; font-size:1px;\">&nbsp;</td>");
        }
        html.append("</tr></table>");

        html.append("<div style=\"text-align:center; font-weight:600; color:#495057; font-family:")
            .append(FONT_FAMILY).append("; margin-top:8px;\">")
            .append(String.format("%.1f%% Success Rate (%d/%d tests passed)", getSuccessRate(), passedTests, totalTests))
            .append("</div>");
    }

    private void appendExecutionInfoSection(StringBuilder html) {
        try {
            html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#e9ecef\" style=\"background-color:#e9ecef; margin-top:24px;\">");
            html.append("<tr><td style=\"padding:20px; font-family:").append(FONT_FAMILY).append(";\">");
            html.append("<div style=\"font-size:16px; font-weight:600; color:#495057; margin-bottom:12px;\">📊 Execution Details</div>");
            html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");

            appendInfoRow(html, "Execution Date:", getFormattedExecutionDate());
            appendInfoRow(html, "Total Duration:", getFormattedExecutionTime());
            appendInfoRow(html, "Report Type:", "Allure Reports");

            String status = hasFailures() ? "FAILED" : "PASSED";
            String statusColor = hasFailures() ? "#dc3545" : "#28a745";
            html.append("<tr><td style=\"padding:6px 0; font-weight:600; color:#495057; width:40%;\">Execution Status:</td>")
                .append("<td style=\"padding:6px 0;\"><strong style=\"color:").append(statusColor).append(";\">").append(status).append("</strong></td></tr>");

            html.append("</table>");
            html.append("</td></tr></table>");
        } catch (Exception e) {
            html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#e9ecef\" style=\"background-color:#e9ecef; margin-top:24px;\">");
            html.append("<tr><td style=\"padding:20px; font-family:").append(FONT_FAMILY).append(";\">");
            html.append("<div style=\"font-size:16px; font-weight:600; color:#495057; margin-bottom:12px;\">📊 Execution Details</div>");
            html.append("<p>Error loading execution details: ").append(escapeHtml(e.getMessage())).append("</p>");
            html.append("</td></tr></table>");
        }
    }

    private void appendInfoRow(StringBuilder html, String label, String value) {
        html.append("<tr><td style=\"padding:6px 0; border-bottom:1px solid #dee2e6; font-weight:600; color:#495057; width:40%;\">")
            .append(escapeHtml(label)).append("</td><td style=\"padding:6px 0; border-bottom:1px solid #dee2e6; color:#6c757d;\">")
            .append(escapeHtml(value)).append("</td></tr>");
    }

    private void appendFailedTestsSection(StringBuilder html) {
        if (!hasFailures() || failedTestResults == null || failedTestResults.isEmpty()) {
            return;
        }
        try {
            html.append("<div style=\"font-size:16px; font-weight:600; color:#dc3545; font-family:")
                .append(FONT_FAMILY).append("; margin:28px 0 12px;\">❌ Failed Test Details</div>");
            html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;\">");
            html.append("<tr>");
            appendFailedTableHeaderCell(html, "Test Name");
            appendFailedTableHeaderCell(html, "Test Class");
            appendFailedTableHeaderCell(html, "Error Details");
            html.append("</tr>");

            for (ITestResult failedTest : failedTestResults) {
                String testName = failedTest.getName();
                String className = failedTest.getTestClass() != null ? failedTest.getTestClass().getName() : "Unknown Class";
                Throwable throwable = failedTest.getThrowable();
                String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";

                html.append("<tr>");
                html.append("<td style=\"padding:12px; border-bottom:1px solid #f5c6cb; font-family:").append(FONT_FAMILY)
                    .append("; font-weight:bold; color:#c53030; vertical-align:top;\">").append(escapeHtml(testName)).append("</td>");
                html.append("<td style=\"padding:12px; border-bottom:1px solid #f5c6cb; font-family:").append(FONT_FAMILY)
                    .append("; font-size:12px; color:#718096; vertical-align:top;\">").append(escapeHtml(className)).append("</td>");
                html.append("<td style=\"padding:12px; border-bottom:1px solid #f5c6cb; background-color:#fdf0f0; font-family:'Courier New',Courier,monospace; font-size:12px; color:#c53030; vertical-align:top;\">")
                    .append(escapeHtml(errorMessage)).append("</td>");
                html.append("</tr>");
            }

            html.append("</table>");
        } catch (Exception e) {
            html.append("<div style=\"font-family:").append(FONT_FAMILY).append(";\">Error loading failed test details: ")
                .append(escapeHtml(e.getMessage())).append("</div>");
        }
    }

    private void appendFailedTableHeaderCell(StringBuilder html, String label) {
        html.append("<th align=\"left\" bgcolor=\"#dc3545\" style=\"background-color:#dc3545; color:#ffffff; padding:12px; font-family:")
            .append(FONT_FAMILY).append("; font-size:13px;\">").append(label).append("</th>");
    }

    private void appendHtmlBodyEnd(StringBuilder html) {
        html.append("<tr><td align=\"center\" bgcolor=\"#f8f9fa\" style=\"background-color:#f8f9fa; padding:20px; font-family:")
            .append(FONT_FAMILY).append("; font-size:12px; color:#6c757d; border-top:1px solid #e9ecef;\">");
        html.append("Generated by Ellithium Test Automation Framework<br>For detailed reports, please check the Allure HTML report");
        html.append("</td></tr>");
        html.append("</table>");
        html.append("</td></tr>");
        html.append("</table>");
        html.append("</body>");
        html.append("</html>");
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    private String getFormattedExecutionDate() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.format(executionDate);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Getters for the fields
    public long getTotalTests() { return totalTests; }
    public long getPassedTests() { return passedTests; }
    public long getFailedTests() { return failedTests; }
    public long getSkippedTests() { return skippedTests; }
}
