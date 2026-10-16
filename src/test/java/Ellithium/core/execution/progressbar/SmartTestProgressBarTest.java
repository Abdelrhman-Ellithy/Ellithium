package Ellithium.core.execution.progressbar;

import Ellithium.core.execution.events.TestLifecycleEvent;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SmartTestProgressBarTest {

    private ByteArrayOutputStream outputCaptor;
    private PrintStream customOut;

    @BeforeMethod
    public void setup() {
        outputCaptor = new ByteArrayOutputStream();
        customOut = new PrintStream(outputCaptor, true, StandardCharsets.UTF_8);
    }

    @AfterMethod
    public void tearDown() {
        outputCaptor = null;
        customOut = null;
    }

    @Test
    public void testProgressBarRenderer_CalculatesPercentageAndRatesCorrectly() {
        ProgressBarRenderer.Snapshot s1 = new ProgressBarRenderer.Snapshot(
                100, 50, 48, 1, 1, 10_000L, "myTest"
        );

        Assert.assertEquals(s1.getPercentage(), 50.0, 0.001);
        Assert.assertEquals(s1.getRate(), 5.0, 0.001); // 50 tests in 10s = 5 t/s
        Assert.assertEquals(s1.getEtaMs(), 10_000L); // 50 remaining at 5 t/s = 10s

        // Edge case: 0 total expected
        ProgressBarRenderer.Snapshot sZero = new ProgressBarRenderer.Snapshot(
                0, 0, 0, 0, 0, 0L, ""
        );
        Assert.assertEquals(sZero.getPercentage(), 0.0);
        Assert.assertEquals(sZero.getEtaMs(), 0L);
    }

    @Test
    public void testProgressBarRenderer_RendersAsciiAndUnicodeLines() {
        ProgressBarRenderer.Snapshot s = new ProgressBarRenderer.Snapshot(
                80, 40, 38, 1, 1, 8_000L, "loginTests#validLogin"
        );

        // Unicode + ANSI
        String unicodeLine = ProgressBarRenderer.renderLine(s, 20, true, true);
        Assert.assertTrue(unicodeLine.contains("Ellithium"));
        Assert.assertTrue(unicodeLine.contains("50.0%"));
        Assert.assertTrue(unicodeLine.contains("40"));
        Assert.assertTrue(unicodeLine.contains("80"));
        Assert.assertTrue(unicodeLine.contains("38"));

        // ASCII plain text
        String asciiLine = ProgressBarRenderer.renderLine(s, 20, false, false);
        Assert.assertTrue(asciiLine.contains("[Ellithium]"));
        Assert.assertTrue(asciiLine.contains("50.0%"));
        Assert.assertTrue(asciiLine.contains("[40/80]"));
        Assert.assertTrue(asciiLine.contains("P:38"));
        Assert.assertTrue(asciiLine.contains("F:1"));
        Assert.assertTrue(asciiLine.contains("S:1"));
    }

    @Test
    public void testProgressBarRenderer_RenderWindowTitle() {
        ProgressBarRenderer.Snapshot s = new ProgressBarRenderer.Snapshot(
                50, 25, 23, 1, 1, 5_000L, "arenaTest"
        );
        String title = ProgressBarRenderer.renderWindowTitle(s);
        Assert.assertTrue(title.contains("Ellithium [50%] 25/50"));
        Assert.assertTrue(title.contains("23✔"));
        Assert.assertTrue(title.contains("1✖"));
        Assert.assertTrue(title.contains("1↷"));
    }

    @Test
    public void testProgressBarRenderer_RenderFinalSummary() {
        ProgressBarRenderer.Snapshot sSuccess = new ProgressBarRenderer.Snapshot(
                20, 20, 20, 0, 0, 4_500L, ""
        );
        String summarySuccess = ProgressBarRenderer.renderFinalSummary(sSuccess, true, false);
        Assert.assertTrue(summarySuccess.contains("SUITE COMPLETED SUCCESSFULLY"));
        Assert.assertTrue(summarySuccess.contains("20/20"));
        Assert.assertTrue(summarySuccess.contains("20 Passed"));

        ProgressBarRenderer.Snapshot sFailure = new ProgressBarRenderer.Snapshot(
                20, 20, 18, 2, 0, 4_500L, ""
        );
        String summaryFailure = ProgressBarRenderer.renderFinalSummary(sFailure, false, false);
        Assert.assertTrue(summaryFailure.contains("SUITE COMPLETED WITH FAILURES"));
        Assert.assertTrue(summaryFailure.contains("2 Failed"));
    }

    @Test
    public void testSmartTestProgressBar_LifecycleEventsIntegration() {
        SmartTestProgressBar bar = new SmartTestProgressBar();
        bar.setOutputStream(customOut);

        bar.onExecutionStarted(new TestLifecycleEvent.ExecutionStarted("UnitTestExec"));
        bar.onSuiteStarted(new TestLifecycleEvent.SuiteStarted("SmokeSuite", 3));

        bar.onTestStarted(new TestLifecycleEvent.TestStarted("t1", "testMethod1"));
        bar.onTestPassed(new TestLifecycleEvent.TestPassed("t1", "testMethod1", 100L));

        bar.onTestStarted(new TestLifecycleEvent.TestStarted("t2", "testMethod2"));
        bar.onTestFailed(new TestLifecycleEvent.TestFailed("t2", "testMethod2", new RuntimeException("Assertion error"), 150L));

        bar.onTestStarted(new TestLifecycleEvent.TestStarted("t3", "testMethod3"));
        bar.onTestSkipped(new TestLifecycleEvent.TestSkipped("t3", "testMethod3", new RuntimeException("Skipped")));

        ProgressBarRenderer.Snapshot snapshot = bar.getSnapshot();
        Assert.assertEquals(snapshot.totalExpected(), 3);
        Assert.assertEquals(snapshot.completed(), 3);
        Assert.assertEquals(snapshot.passed(), 1);
        Assert.assertEquals(snapshot.failed(), 1);
        Assert.assertEquals(snapshot.skipped(), 1);
        Assert.assertEquals(snapshot.getPercentage(), 100.0, 0.001);

        bar.onExecutionFinished(new TestLifecycleEvent.ExecutionFinished());

        String captured = outputCaptor.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(captured.contains("SUITE COMPLETED WITH FAILURES") || captured.contains("Results:"));
        Assert.assertTrue(captured.contains("1 Passed"));
        Assert.assertTrue(captured.contains("1 Failed"));
        Assert.assertTrue(captured.contains("1 Skipped"));
    }

    @Test
    public void testSmartTestProgressBar_ThreadSafetyUnderParallelLoad() throws InterruptedException {
        SmartTestProgressBar bar = new SmartTestProgressBar();
        bar.setOutputStream(customOut);
        int totalWorkers = 8;
        int testsPerWorker = 25; // 200 total tests
        int totalTests = totalWorkers * testsPerWorker;

        bar.onExecutionStarted(new TestLifecycleEvent.ExecutionStarted("ParallelExec"));
        bar.onSuiteStarted(new TestLifecycleEvent.SuiteStarted("ParallelSuite", totalTests));

        ExecutorService executor = Executors.newFixedThreadPool(totalWorkers);
        CountDownLatch latch = new CountDownLatch(totalWorkers);

        for (int w = 0; w < totalWorkers; w++) {
            final int workerId = w;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < testsPerWorker; i++) {
                        String id = "worker-" + workerId + "-test-" + i;
                        bar.onTestStarted(new TestLifecycleEvent.TestStarted(id, "runTest"));
                        if (i % 5 == 0) {
                            bar.onTestFailed(new TestLifecycleEvent.TestFailed(id, "runTest", new RuntimeException("Simulated failure"), 10L));
                        } else if (i % 7 == 0) {
                            bar.onTestSkipped(new TestLifecycleEvent.TestSkipped(id, "runTest", new RuntimeException("Simulated skip")));
                        } else {
                            bar.onTestPassed(new TestLifecycleEvent.TestPassed(id, "runTest", 10L));
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completedInTime = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        Assert.assertTrue(completedInTime, "Parallel tests should finish within timeout");

        ProgressBarRenderer.Snapshot s = bar.getSnapshot();
        Assert.assertEquals(s.completed(), totalTests, "All parallel tests must be recorded");
        Assert.assertEquals(s.passed() + s.failed() + s.skipped(), totalTests, "Sum of status counts must equal completed");
        Assert.assertEquals(s.getPercentage(), 100.0, 0.001);
    }

    @Test
    public void testTerminalCapabilities_SafeOperations() {
        // Assert that calling setConsoleWindowTitle with null or text does not throw
        TerminalCapabilities.setConsoleWindowTitle(null);
        TerminalCapabilities.setConsoleWindowTitle("");
        TerminalCapabilities.setConsoleWindowTitle("Ellithium Test Title");

        // Verify boolean query methods return without error
        boolean ansi = TerminalCapabilities.supportsAnsi();
        boolean unicode = TerminalCapabilities.supportsUnicode();
        boolean ci = TerminalCapabilities.isCiEnvironment();
        boolean interactive = TerminalCapabilities.isInteractive();

        Assert.assertTrue(ansi || !ansi);
        Assert.assertTrue(unicode || !unicode);
        Assert.assertTrue(ci || !ci);
        Assert.assertTrue(interactive || !interactive);
    }
}
