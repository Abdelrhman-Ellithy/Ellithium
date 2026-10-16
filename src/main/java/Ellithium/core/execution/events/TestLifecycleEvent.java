package Ellithium.core.execution.events;

import java.lang.reflect.Method;

/**
 * Sealed hierarchy of test execution lifecycle events for the Ellithium framework.
 * Enables reactive, decoupled event dispatching across recording, reporting,
 * and telemetry subsystems.
 */
public sealed interface TestLifecycleEvent permits
        TestLifecycleEvent.ExecutionStarted,
        TestLifecycleEvent.ExecutionFinished,
        TestLifecycleEvent.SuiteStarted,
        TestLifecycleEvent.SuiteFinished,
        TestLifecycleEvent.TestStarted,
        TestLifecycleEvent.TestPassed,
        TestLifecycleEvent.TestFailed,
        TestLifecycleEvent.TestSkipped {

    record ExecutionStarted(String executionName, long timestamp) implements TestLifecycleEvent {
        public ExecutionStarted(String executionName) {
            this(executionName, System.currentTimeMillis());
        }
    }

    record ExecutionFinished(long timestamp) implements TestLifecycleEvent {
        public ExecutionFinished() {
            this(System.currentTimeMillis());
        }
    }

    record SuiteStarted(String suiteName, long timestamp) implements TestLifecycleEvent {
        public SuiteStarted(String suiteName) {
            this(suiteName, System.currentTimeMillis());
        }
    }

    record SuiteFinished(String suiteName, long timestamp) implements TestLifecycleEvent {
        public SuiteFinished(String suiteName) {
            this(suiteName, System.currentTimeMillis());
        }
    }

    record TestStarted(String testIdentifier, String testName, Method testMethod, long timestamp) implements TestLifecycleEvent {
        public TestStarted(String testIdentifier, String testName) {
            this(testIdentifier, testName, null, System.currentTimeMillis());
        }
    }

    record TestPassed(String testIdentifier, String testName, long durationMs, long timestamp) implements TestLifecycleEvent {
        public TestPassed(String testIdentifier, String testName, long durationMs) {
            this(testIdentifier, testName, durationMs, System.currentTimeMillis());
        }
    }

    record TestFailed(String testIdentifier, String testName, Throwable throwable, long durationMs, long timestamp) implements TestLifecycleEvent {
        public TestFailed(String testIdentifier, String testName, Throwable throwable, long durationMs) {
            this(testIdentifier, testName, throwable, durationMs, System.currentTimeMillis());
        }
    }

    record TestSkipped(String testIdentifier, String testName, Throwable throwable, long timestamp) implements TestLifecycleEvent {
        public TestSkipped(String testIdentifier, String testName, Throwable throwable) {
            this(testIdentifier, testName, throwable, System.currentTimeMillis());
        }
    }
}
