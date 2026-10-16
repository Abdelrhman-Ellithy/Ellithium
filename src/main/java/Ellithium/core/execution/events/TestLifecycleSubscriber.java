package Ellithium.core.execution.events;

/**
 * Subscriber interface for handling reactive test lifecycle events.
 * Implementations can selectively override methods of interest.
 */
public interface TestLifecycleSubscriber {

    default void onExecutionStarted(TestLifecycleEvent.ExecutionStarted event) {}

    default void onExecutionFinished(TestLifecycleEvent.ExecutionFinished event) {}

    default void onSuiteStarted(TestLifecycleEvent.SuiteStarted event) {}

    default void onSuiteFinished(TestLifecycleEvent.SuiteFinished event) {}

    default void onTestStarted(TestLifecycleEvent.TestStarted event) {}

    default void onTestPassed(TestLifecycleEvent.TestPassed event) {}

    default void onTestFailed(TestLifecycleEvent.TestFailed event) {}

    default void onTestSkipped(TestLifecycleEvent.TestSkipped event) {}
}
