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
        String unicodeLine = ProgressBarRenderer.renderLine(s, 120, true, true);
        Assert.assertTrue(unicodeLine.contains("Ellithium"));
        Assert.assertTrue(unicodeLine.contains("50.0%"));
        Assert.assertTrue(unicodeLine.contains("40"));
        Assert.assertTrue(unicodeLine.contains("80"));
        Assert.assertTrue(unicodeLine.contains("38"));
        Assert.assertTrue(unicodeLine.contains("█") || unicodeLine.contains("░"));

        // ASCII plain text (wide terminal >= 120 cols gets verbose badges)
        String asciiLineWide = ProgressBarRenderer.renderLine(s, 140, false, false);
        Assert.assertTrue(asciiLineWide.contains("[Ellithium]"));
        Assert.assertTrue(asciiLineWide.contains("50.0%"));
        Assert.assertTrue(asciiLineWide.contains("[40/80]"));
        Assert.assertTrue(asciiLineWide.contains("PASSED: 38"));
        Assert.assertTrue(asciiLineWide.contains("FAILED: 1"));
        Assert.assertTrue(asciiLineWide.contains("SKIPPED: 1"));

        // ASCII plain text (compact terminal < 100 cols gets compact badges)
        String asciiLineCompact = ProgressBarRenderer.renderLine(s, 80, false, false);
        Assert.assertTrue(asciiLineCompact.contains("[Ellithium]"));
        Assert.assertTrue(asciiLineCompact.contains("50.0%"));
        Assert.assertTrue(asciiLineCompact.contains("[40/80]"));
        Assert.assertTrue(asciiLineCompact.contains("P:38"));
        Assert.assertTrue(asciiLineCompact.contains("F:1"));
        Assert.assertTrue(asciiLineCompact.contains("S:1"));
    }

    @Test
    public void testProgressBarRenderer_RenderWindowTitle() {
        ProgressBarRenderer.Snapshot s = new ProgressBarRenderer.Snapshot(
                50, 25, 23, 1, 1, 5_000L, "arenaTest"
        );
        String title = ProgressBarRenderer.renderWindowTitle(s);
        Assert.assertTrue(title.contains("Ellithium [50%] 25/50"));
        Assert.assertTrue(title.contains("23 PASS"));
        Assert.assertTrue(title.contains("1 FAIL"));
        Assert.assertTrue(title.contains("1 SKIP"));
    }

    @Test
    public void testProgressBarRenderer_RenderFinalSummary() {
        ProgressBarRenderer.Snapshot sSuccess = new ProgressBarRenderer.Snapshot(
                20, 20, 20, 0, 0, 4_500L, ""
        );
        String summarySuccess = ProgressBarRenderer.renderFinalSummary(sSuccess, true, false);
        Assert.assertTrue(summarySuccess.contains("SUITE COMPLETED SUCCESSFULLY"));
        Assert.assertTrue(summarySuccess.contains("20/20"));
        Assert.assertFalse(summarySuccess.contains("Results:"));

        ProgressBarRenderer.Snapshot sFailure = new ProgressBarRenderer.Snapshot(
                20, 18, 16, 2, 0, 4_500L, ""
        );
        String summaryFailure = ProgressBarRenderer.renderFinalSummary(sFailure, false, false);
        Assert.assertTrue(summaryFailure.contains("SUITE COMPLETED WITH FAILURES"));
        Assert.assertTrue(summaryFailure.contains("18/20"));
        Assert.assertFalse(summaryFailure.contains("Results:"));
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

        String raw = outputCaptor.toString(StandardCharsets.UTF_8);
        String captured = raw.replaceAll("\\u001B\\[[;\\d]*m", "");
        Assert.assertTrue(captured.contains("SUITE COMPLETED WITH FAILURES"));
        Assert.assertFalse(captured.contains("Results:"));
    }

    @Test
    public void testSmartTestProgressBar_DeduplicatesDuplicateRenderCalls() {
        SmartTestProgressBar bar = new SmartTestProgressBar();
        bar.setOutputStream(customOut);

        bar.onExecutionStarted(new TestLifecycleEvent.ExecutionStarted("DeduplicationExec"));
        bar.onSuiteStarted(new TestLifecycleEvent.SuiteStarted("DeduplicationSuite", 2));

        // Test 1 passes
        bar.onTestStarted(new TestLifecycleEvent.TestStarted("d1", "dedupTest1"));
        bar.onTestPassed(new TestLifecycleEvent.TestPassed("d1", "dedupTest1", 50L));

        // Multiple calls to render(true) or render(false) without count change
        bar.render(false);
        bar.render(true);

        // Test 2 passes
        bar.onTestStarted(new TestLifecycleEvent.TestStarted("d2", "dedupTest2"));
        bar.onTestPassed(new TestLifecycleEvent.TestPassed("d2", "dedupTest2", 50L));

        // onSuiteFinished also calls render(true)
        bar.onSuiteFinished(new TestLifecycleEvent.SuiteFinished("DeduplicationSuite"));

        String raw = outputCaptor.toString(StandardCharsets.UTF_8);
        String captured = raw.replaceAll("\\u001B\\[[;\\d]*m", "");

        // Count occurrences of "[1/2]" and "[2/2]"
        int count1of2 = countOccurrences(captured, "[1/2]");
        int count2of2 = countOccurrences(captured, "[2/2]");

        Assert.assertEquals(count1of2, 1, "Progress line for [1/2] should be emitted exactly once");
        Assert.assertEquals(count2of2, 1, "Progress line for [2/2] should be emitted exactly once (no duplicate onSuiteFinished)");
    }

    private static int countOccurrences(String str, String sub) {
        if (str == null || sub == null || sub.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
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
    public void testProgressBarRenderer_LineNeverExceedsTerminalWidth() {
        ProgressBarRenderer.Snapshot s = new ProgressBarRenderer.Snapshot(
                100, 50, 48, 1, 1, 10_000L, "com.company.project.very.long.package.name.and.long.test.method.name.that.might.overflow.screen"
        );
        int terminalWidth = 80;
        String line = ProgressBarRenderer.renderLine(s, terminalWidth, false, false);
        int visibleLen = ProgressBarRenderer.getVisibleLength(line);
        Assert.assertTrue(visibleLen <= terminalWidth, "Rendered line visible length (" + visibleLen + ") must not exceed terminal width (" + terminalWidth + ")");
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
        int width = TerminalCapabilities.getTerminalWidth();

        Assert.assertTrue(width >= 40 && width <= 500, "Detected terminal width should be within valid bounds");
        Assert.assertTrue(ansi || !ansi);
        Assert.assertTrue(unicode || !unicode);
        Assert.assertTrue(ci || !ci);
        Assert.assertTrue(interactive || !interactive);
    }
}
