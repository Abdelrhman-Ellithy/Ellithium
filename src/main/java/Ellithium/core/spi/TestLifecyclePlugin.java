package Ellithium.core.spi;

/**
 * Service Provider Interface (SPI) for plugins that listen to test execution lifecycle events.
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 */
public interface TestLifecyclePlugin extends EllithiumPlugin {

    /**
     * Invoked when a test suite begins execution.
     *
     * @param suiteName name of the suite
     */
    default void onSuiteStart(String suiteName) {
        // default no-op
    }

    /**
     * Invoked when a test suite completes execution.
     *
     * @param suiteName name of the suite
     */
    default void onSuiteFinish(String suiteName) {
        // default no-op
    }

    /**
     * Invoked before an individual test method starts.
     *
     * @param testId unique identifier of the test
     * @param testName name of the test method
     */
    default void onTestStart(String testId, String testName) {
        // default no-op
    }

    /**
     * Invoked when a test method succeeds.
     *
     * @param testId unique identifier of the test
     * @param testName name of the test method
     * @param durationMs execution time in milliseconds
     */
    default void onTestSuccess(String testId, String testName, long durationMs) {
        // default no-op
    }

    /**
     * Invoked when a test method fails.
     *
     * @param testId unique identifier of the test
     * @param testName name of the test method
     * @param cause throwable that caused failure
     * @param durationMs execution time in milliseconds
     */
    default void onTestFailure(String testId, String testName, Throwable cause, long durationMs) {
        // default no-op
    }

    /**
     * Invoked when a test method is skipped.
     *
     * @param testId unique identifier of the test
     * @param testName name of the test method
     * @param cause optional throwable indicating skip reason
     */
    default void onTestSkipped(String testId, String testName, Throwable cause) {
        // default no-op
    }
}
