package Ellithium.core.execution.events;

import Ellithium.core.logging.Logger;
import Ellithium.core.recording.ScreenRecordingLifecycleSubscriber;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Isolated, reactive thread-safe event bus for test lifecycle notifications.
 * Decouples test framework listeners (TestNG, Cucumber) from media capture,
 * reporting, and telemetry subscribers.
 */
public final class TestLifecycleEventBus {

    private static final TestLifecycleEventBus INSTANCE = new TestLifecycleEventBus();

    private final List<TestLifecycleSubscriber> subscribers = new CopyOnWriteArrayList<>();

    private TestLifecycleEventBus() {
        // Register default subscribers
        register(new ScreenRecordingLifecycleSubscriber());
    }

    public static TestLifecycleEventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a subscriber to receive test lifecycle events.
     * @param subscriber subscriber implementation
     */
    public void register(TestLifecycleSubscriber subscriber) {
        if (subscriber != null && !subscribers.contains(subscriber)) {
            subscribers.add(subscriber);
        }
    }

    /**
     * Unregisters a subscriber.
     * @param subscriber subscriber implementation
     */
    public void unregister(TestLifecycleSubscriber subscriber) {
        if (subscriber != null) {
            subscribers.remove(subscriber);
        }
    }

    /**
     * Resets subscribers to the default configuration (useful in unit testing).
     */
    public void resetToDefault() {
        subscribers.clear();
        subscribers.add(new ScreenRecordingLifecycleSubscriber());
    }

    /**
     * Returns an unmodifiable list of currently registered subscribers.
     * @return current subscribers
     */
    public List<TestLifecycleSubscriber> getSubscribers() {
        return Collections.unmodifiableList(subscribers);
    }

    /**
     * Publishes a lifecycle event to all registered subscribers.
     * Guarantees exception isolation so a failing subscriber never interrupts the test pipeline.
     * @param event the lifecycle event to publish
     */
    public void publish(TestLifecycleEvent event) {
        if (event == null) return;

        for (TestLifecycleSubscriber subscriber : subscribers) {
            try {
                switch (event) {
                    case TestLifecycleEvent.ExecutionStarted e -> subscriber.onExecutionStarted(e);
                    case TestLifecycleEvent.ExecutionFinished e -> subscriber.onExecutionFinished(e);
                    case TestLifecycleEvent.SuiteStarted e -> subscriber.onSuiteStarted(e);
                    case TestLifecycleEvent.SuiteFinished e -> subscriber.onSuiteFinished(e);
                    case TestLifecycleEvent.TestStarted e -> subscriber.onTestStarted(e);
                    case TestLifecycleEvent.TestPassed e -> subscriber.onTestPassed(e);
                    case TestLifecycleEvent.TestFailed e -> subscriber.onTestFailed(e);
                    case TestLifecycleEvent.TestSkipped e -> subscriber.onTestSkipped(e);
                }
            } catch (Throwable t) {
                Logger.warn("Error in TestLifecycleSubscriber [" + subscriber.getClass().getSimpleName() + "]: " + t.getMessage());
            }
        }
    }

    public void publishExecutionStarted(String executionName) {
        publish(new TestLifecycleEvent.ExecutionStarted(executionName));
    }

    public void publishExecutionFinished() {
        publish(new TestLifecycleEvent.ExecutionFinished());
    }

    public void publishSuiteStarted(String suiteName) {
        publish(new TestLifecycleEvent.SuiteStarted(suiteName));
    }

    public void publishSuiteFinished(String suiteName) {
        publish(new TestLifecycleEvent.SuiteFinished(suiteName));
    }

    public void publishTestStarted(String testIdentifier, String testName) {
        publish(new TestLifecycleEvent.TestStarted(testIdentifier, testName));
    }

    public void publishTestPassed(String testIdentifier, String testName, long durationMs) {
        publish(new TestLifecycleEvent.TestPassed(testIdentifier, testName, durationMs));
    }

    public void publishTestFailed(String testIdentifier, String testName, Throwable throwable, long durationMs) {
        publish(new TestLifecycleEvent.TestFailed(testIdentifier, testName, throwable, durationMs));
    }

    public void publishTestSkipped(String testIdentifier, String testName, Throwable throwable) {
        publish(new TestLifecycleEvent.TestSkipped(testIdentifier, testName, throwable));
    }
}
