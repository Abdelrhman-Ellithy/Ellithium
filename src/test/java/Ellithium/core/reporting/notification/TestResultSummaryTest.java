package Ellithium.core.reporting.notification;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;

public class TestResultSummaryTest {

    // ── Rate calculations ─────────────────────────────────────────────────

    @Test
    public void getSuccessRate_allPassed_returns100() {
        TestResultSummary s = summary(10, 10, 0, 0, 1000, Collections.emptySet());
        Assert.assertEquals(s.getSuccessRate(), 100.0, 0.001);
    }

    @Test
    public void getFailureRate_allFailed_returns100() {
        TestResultSummary s = summary(10, 0, 10, 0, 1000, Set.of(failedResult("t1")));
        Assert.assertEquals(s.getFailureRate(), 100.0, 0.001);
    }

    @Test
    public void getSuccessRate_halfPassed_returns50() {
        TestResultSummary s = summary(10, 5, 5, 0, 500, Set.of(failedResult("t1")));
        Assert.assertEquals(s.getSuccessRate(), 50.0, 0.001);
    }

    @Test
    public void getSuccessRate_zeroTotal_returnsZero() {
        TestResultSummary s = summary(0, 0, 0, 0, 0, Collections.emptySet());
        Assert.assertEquals(s.getSuccessRate(), 0.0, 0.001);
    }

    @Test
    public void getFailureRate_zeroTotal_returnsZero() {
        TestResultSummary s = summary(0, 0, 0, 0, 0, Collections.emptySet());
        Assert.assertEquals(s.getFailureRate(), 0.0, 0.001);
    }

    // ── hasFailures ───────────────────────────────────────────────────────

    @Test
    public void hasFailures_withFailedResultsSet_returnsTrue() {
        TestResultSummary s = summary(5, 4, 1, 0, 1000, Set.of(failedResult("bad")));
        Assert.assertTrue(s.hasFailures());
    }

    @Test
    public void hasFailures_noFailures_returnsFalse() {
        TestResultSummary s = summary(5, 5, 0, 0, 1000, Collections.emptySet());
        Assert.assertFalse(s.hasFailures());
    }

    @Test
    public void hasFailures_failedCountButEmptySet_returnsFalse() {
        TestResultSummary s = summary(5, 4, 1, 0, 1000, Collections.emptySet());
        Assert.assertFalse(s.hasFailures());
    }

    // ── getFormattedExecutionTime ─────────────────────────────────────────

    @Test
    public void formattedTime_seconds_only() {
        TestResultSummary s = summary(1, 1, 0, 0, 45_000, Collections.emptySet());
        Assert.assertEquals(s.getFormattedExecutionTime(), "45s");
    }

    @Test
    public void formattedTime_minutes_and_seconds() {
        TestResultSummary s = summary(1, 1, 0, 0, 125_000, Collections.emptySet());
        Assert.assertEquals(s.getFormattedExecutionTime(), "2m 5s");
    }

    @Test
    public void formattedTime_hours_minutes_seconds() {
        TestResultSummary s = summary(1, 1, 0, 0, 3_661_000, Collections.emptySet());
        Assert.assertEquals(s.getFormattedExecutionTime(), "1h 1m 1s");
    }

    // ── getters ───────────────────────────────────────────────────────────

    @Test
    public void getters_returnConstructedValues() {
        TestResultSummary s = summary(10, 7, 2, 1, 5000, Collections.emptySet());
        Assert.assertEquals(s.getTotalTests(), 10);
        Assert.assertEquals(s.getPassedTests(), 7);
        Assert.assertEquals(s.getFailedTests(), 2);
        Assert.assertEquals(s.getSkippedTests(), 1);
    }

    // ── generateSummaryMessage ────────────────────────────────────────────

    @Test
    public void generateSummaryMessage_containsPassedCount() {
        TestResultSummary s = summary(10, 8, 2, 0, 1000, Set.of(failedResult("x")));
        String msg = s.generateSummaryMessage();
        Assert.assertTrue(msg.contains("8"));
        Assert.assertTrue(msg.contains("2"));
    }

    // ── generateHtmlEmailBody ─────────────────────────────────────────────

    @Test
    public void generateHtmlEmailBody_containsDoctype() {
        TestResultSummary s = summary(5, 5, 0, 0, 1000, Collections.emptySet());
        Assert.assertTrue(s.generateHtmlEmailBody().contains("<!DOCTYPE html>"));
    }

    @Test
    public void generateHtmlEmailBody_passedStatus_showsPASSED() {
        TestResultSummary s = summary(5, 5, 0, 0, 1000, Collections.emptySet());
        Assert.assertTrue(s.generateHtmlEmailBody().contains("PASSED"));
    }

    @Test
    public void generateHtmlEmailBody_failedStatus_showsFAILED() {
        TestResultSummary s = summary(5, 4, 1, 0, 1000, Set.of(failedResult("bad")));
        Assert.assertTrue(s.generateHtmlEmailBody().contains("FAILED"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static TestResultSummary summary(long total, long passed, long failed,
                                             long skipped, long time,
                                             Set<ITestResult> failedResults) {
        return new TestResultSummary(total, passed, failed, skipped, time, failedResults);
    }

    private static ITestResult failedResult(String name) {
        ITestResult r = Mockito.mock(ITestResult.class);
        Mockito.when(r.getName()).thenReturn(name);
        org.testng.ITestClass tc = Mockito.mock(org.testng.ITestClass.class);
        Mockito.when(tc.getName()).thenReturn("SomeClass");
        Mockito.when(r.getTestClass()).thenReturn(tc);
        Mockito.when(r.getThrowable()).thenReturn(new RuntimeException("test failed"));
        return r;
    }
}
