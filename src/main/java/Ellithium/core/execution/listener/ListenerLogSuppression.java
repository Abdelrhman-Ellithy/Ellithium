package Ellithium.core.execution.listener;

/**
 * Shared, thread-local logging-suppression switch honored by BOTH the web
 * ({@link seleniumListener}) and mobile ({@link appiumListener}) driver listeners.
 *
 * <p>Internal AI operations (fingerprint capture, DOM minimization, candidate probing)
 * issue many low-level WebDriver/Appium calls that would otherwise flood the report.
 * They bracket that work with {@link #suppress()} / {@link #resume()} so the listeners
 * stay quiet. Reentrant: pair every {@code suppress()} with a {@code resume()}.</p>
 */
public final class ListenerLogSuppression {

    private ListenerLogSuppression() {}

    private static final ThreadLocal<Integer> SUPPRESS_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static void suppress() { SUPPRESS_DEPTH.set(SUPPRESS_DEPTH.get() + 1); }

    public static void resume() { SUPPRESS_DEPTH.set(Math.max(0, SUPPRESS_DEPTH.get() - 1)); }

    public static boolean isSuppressed() { return SUPPRESS_DEPTH.get() > 0; }
}
