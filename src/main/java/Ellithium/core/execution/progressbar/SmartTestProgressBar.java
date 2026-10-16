package Ellithium.core.execution.progressbar;

import Ellithium.core.execution.events.TestLifecycleEvent;
import Ellithium.core.execution.events.TestLifecycleSubscriber;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smart CLI test progress bar and live status engine.
 *
 * <p>Provides:
 * <ul>
 *   <li>High-resolution Unicode/ANSI progress bar with sub-block rendering</li>
 *   <li>Dynamic live pass/fail/skip counts and execution throughput (tests/sec)</li>
 *   <li>Accurate dynamic ETA calculation based on rolling test execution speed</li>
 *   <li>Real-time OS console window and tab title updates (via ANSI OSC)</li>
 *   <li>Intelligent dual-mode output: smooth in-place {@code \r} updates on interactive TTYs,
 *       and structured milestone progress logs in CI environments</li>
 *   <li>Thread-safe concurrent execution tracking across parallel worker threads</li>
 * </ul>
 */
public final class SmartTestProgressBar implements TestLifecycleSubscriber {

    private static final SmartTestProgressBar INSTANCE = new SmartTestProgressBar();

    private final AtomicInteger totalExpected = new AtomicInteger(0);
    private final AtomicInteger completed = new AtomicInteger(0);
    private final AtomicInteger passed = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    private final ConcurrentHashMap<Long, String> activeTests = new ConcurrentHashMap<>();

    private volatile long startTimeMs = System.currentTimeMillis();
    private volatile long lastRenderMs = 0L;
    private volatile int lastMilestonePercent = -1;
    private volatile PrintStream outStream = System.out;

    SmartTestProgressBar() {}

    public static SmartTestProgressBar getInstance() {
        return INSTANCE;
    }

    /**
     * Overrides output stream (primarily for unit testing and verification).
     */
    public void setOutputStream(PrintStream out) {
        this.outStream = (out != null) ? out : System.out;
    }

    /**
     * Resets all internal state and counters (useful between test runs or unit tests).
     */
    public synchronized void reset() {
        totalExpected.set(0);
        completed.set(0);
        passed.set(0);
        failed.set(0);
        skipped.set(0);
        isFinished.set(false);
        activeTests.clear();
        startTimeMs = System.currentTimeMillis();
        lastRenderMs = 0L;
        lastMilestonePercent = -1;
    }

    /**
     * Sets or expands the expected total test count.
     */
    public void setExpectedTotalTests(int count) {
        if (count > 0) {
            totalExpected.updateAndGet(prev -> Math.max(prev, count));
            render(true);
        }
    }

    /**
     * Captures a consistent point-in-time snapshot of the execution state.
     */
    public ProgressBarRenderer.Snapshot getSnapshot() {
        int comp = completed.get();
        int tot = totalExpected.get();
        if (comp > tot && tot > 0) {
            tot = comp;
        }

        String active = "";
        if (!activeTests.isEmpty()) {
            active = activeTests.values().iterator().next();
        }

        long elapsed = Math.max(0, System.currentTimeMillis() - startTimeMs);
        return new ProgressBarRenderer.Snapshot(
                tot,
                comp,
                passed.get(),
                failed.get(),
                skipped.get(),
                elapsed,
                active
        );
    }

    // ── TestLifecycleSubscriber Callbacks ──────────────────────────────────────

    @Override
    public void onExecutionStarted(TestLifecycleEvent.ExecutionStarted event) {
        reset();
        startTimeMs = event.timestamp();
        TerminalCapabilities.setConsoleWindowTitle("Ellithium [0%] Starting Test Suite...");
    }

    @Override
    public void onSuiteStarted(TestLifecycleEvent.SuiteStarted event) {
        if (event.totalTests() > 0) {
            totalExpected.updateAndGet(prev -> prev + event.totalTests());
        }
        render(true);
    }

    @Override
    public void onTestStarted(TestLifecycleEvent.TestStarted event) {
        if (isFinished.get()) return;
        long tid = Thread.currentThread().threadId();
        String name = (event.testName() != null) ? event.testName() : event.testIdentifier();
        if (name != null) {
            activeTests.put(tid, name);
        }
        if (ProgressBarConfig.isInPlaceMode()) {
            render(false);
        } else {
            // Update OS console window title without polluting stdout logs
            TerminalCapabilities.setConsoleWindowTitle(ProgressBarRenderer.renderWindowTitle(getSnapshot()));
        }
    }

    @Override
    public void onTestPassed(TestLifecycleEvent.TestPassed event) {
        if (isFinished.get()) return;
        passed.incrementAndGet();
        completed.incrementAndGet();
        activeTests.remove(Thread.currentThread().threadId());
        render(false);
    }

    @Override
    public void onTestFailed(TestLifecycleEvent.TestFailed event) {
        if (isFinished.get()) return;
        failed.incrementAndGet();
        completed.incrementAndGet();
        activeTests.remove(Thread.currentThread().threadId());
        render(false);
    }

    @Override
    public void onTestSkipped(TestLifecycleEvent.TestSkipped event) {
        if (isFinished.get()) return;
        skipped.incrementAndGet();
        completed.incrementAndGet();
        activeTests.remove(Thread.currentThread().threadId());
        render(false);
    }

    @Override
    public void onSuiteFinished(TestLifecycleEvent.SuiteFinished event) {
        render(true);
    }

    @Override
    public void onExecutionFinished(TestLifecycleEvent.ExecutionFinished event) {
        finish();
    }

    // ── Rendering & Terminal Updates ──────────────────────────────────────────

    /**
     * Renders the progress bar and updates the console window title.
     *
     * @param force true to bypass throttle and force immediate display
     */
    public synchronized void render(boolean force) {
        if (!ProgressBarConfig.isEnabled() || isFinished.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        long throttle = ProgressBarConfig.getThrottleMs();
        if (!force && (now - lastRenderMs < throttle)) {
            return;
        }
        lastRenderMs = now;

        ProgressBarRenderer.Snapshot snapshot = getSnapshot();
        boolean unicode = ProgressBarConfig.isUnicodeEnabled();
        boolean ansi = TerminalCapabilities.supportsAnsi();
        int terminalWidth = TerminalCapabilities.getTerminalWidth();

        // 1. Update OS Console Window Title
        String windowTitle = ProgressBarRenderer.renderWindowTitle(snapshot);
        TerminalCapabilities.setConsoleWindowTitle(windowTitle);

        // 2. Render to Terminal
        boolean ciMode = ProgressBarConfig.isCiMode();
        boolean inPlace = ProgressBarConfig.isInPlaceMode() && TerminalCapabilities.isInteractive() && !ciMode;

        if (inPlace) {
            String line = ProgressBarRenderer.renderLine(snapshot, terminalWidth, unicode, ansi);
            outStream.print("\r" + ProgressBarRenderer.CLEAR_LINE + line);
            outStream.flush();
        } else if (ciMode) {
            // In CI or non-interactive stdout: emit milestones to keep logs clean
            int currentPercent = (int) Math.floor(snapshot.getPercentage() / 10.0) * 10;
            if (force || currentPercent > lastMilestonePercent) {
                lastMilestonePercent = currentPercent;
                String line = ProgressBarRenderer.renderLine(snapshot, terminalWidth, unicode, false);
                outStream.println(line);
                outStream.flush();
            }
        } else {
            // Standard LINE mode: print clean, non-interleaved progress line on test completion or suite milestone
            String line = ProgressBarRenderer.renderLine(snapshot, terminalWidth, unicode, ansi);
            outStream.println(line);
            outStream.flush();
        }
    }

    /**
     * Finalizes the progress display, prints the executive summary box, and updates window title.
     */
    public synchronized void finish() {
        if (!isFinished.compareAndSet(false, true)) {
            return;
        }

        ProgressBarRenderer.Snapshot snapshot = getSnapshot();
        boolean unicode = ProgressBarConfig.isUnicodeEnabled();
        boolean ansi = TerminalCapabilities.supportsAnsi();

        // If in-place mode was used, clear the in-place line before printing summary
        boolean ciMode = ProgressBarConfig.isCiMode();
        boolean inPlace = ProgressBarConfig.isInPlaceMode() && TerminalCapabilities.isInteractive() && !ciMode;
        if (inPlace) {
            outStream.print("\r" + ProgressBarRenderer.CLEAR_LINE);
            outStream.flush();
        }

        // Print final executive summary banner
        String summary = ProgressBarRenderer.renderFinalSummary(snapshot, unicode, ansi);
        outStream.println("\n" + summary);
        outStream.flush();

        // Set final completion window title
        String completionTitle = String.format("Ellithium [100%%] Complete (%d/%d) - %d Passed, %d Failed, %d Skipped (%s)",
                snapshot.completed(),
                snapshot.totalExpected() > 0 ? snapshot.totalExpected() : snapshot.completed(),
                snapshot.passed(),
                snapshot.failed(),
                snapshot.skipped(),
                ProgressBarRenderer.formatDuration(snapshot.elapsedMs()));
        TerminalCapabilities.setConsoleWindowTitle(completionTitle);
    }
}
