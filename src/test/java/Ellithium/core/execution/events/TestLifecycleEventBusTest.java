package Ellithium.core.execution.events;

import Ellithium.core.recording.ScreenRecordingLifecycleSubscriber;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TestLifecycleEventBusTest {

    private TestLifecycleEventBus eventBus;

    @BeforeMethod
    public void setUp() {
        eventBus = TestLifecycleEventBus.getInstance();
        eventBus.resetToDefault();
    }

    @AfterMethod
    public void tearDown() {
        eventBus.resetToDefault();
    }

    @Test
    public void testEventDispatchingToSubscribers() {
        List<String> eventLog = new ArrayList<>();

        TestLifecycleSubscriber testSubscriber = new TestLifecycleSubscriber() {
            @Override
            public void onExecutionStarted(TestLifecycleEvent.ExecutionStarted event) {
                if ("MasterExecution".equals(event.executionName())) {
                    eventLog.add("EXEC_START:" + event.executionName());
                }
            }

            @Override
            public void onSuiteStarted(TestLifecycleEvent.SuiteStarted event) {
                if ("SmokeSuite".equals(event.suiteName())) {
                    eventLog.add("SUITE_START:" + event.suiteName());
                }
            }

            @Override
            public void onTestStarted(TestLifecycleEvent.TestStarted event) {
                if (event.testIdentifier() != null && event.testIdentifier().startsWith("test-")) {
                    eventLog.add("TEST_START:" + event.testIdentifier());
                }
            }

            @Override
            public void onTestPassed(TestLifecycleEvent.TestPassed event) {
                if (event.testIdentifier() != null && event.testIdentifier().startsWith("test-")) {
                    eventLog.add("TEST_PASS:" + event.testIdentifier());
                }
            }

            @Override
            public void onTestFailed(TestLifecycleEvent.TestFailed event) {
                if (event.testIdentifier() != null && event.testIdentifier().startsWith("test-")) {
                    eventLog.add("TEST_FAIL:" + event.testIdentifier() + ":" + event.throwable().getMessage());
                }
            }

            @Override
            public void onTestSkipped(TestLifecycleEvent.TestSkipped event) {
                if (event.testIdentifier() != null && event.testIdentifier().startsWith("test-")) {
                    eventLog.add("TEST_SKIP:" + event.testIdentifier());
                }
            }

            @Override
            public void onSuiteFinished(TestLifecycleEvent.SuiteFinished event) {
                if ("SmokeSuite".equals(event.suiteName())) {
                    eventLog.add("SUITE_FINISH:" + event.suiteName());
                }
            }

            @Override
            public void onExecutionFinished(TestLifecycleEvent.ExecutionFinished event) {
                if (eventLog.contains("EXEC_START:MasterExecution") && !eventLog.contains("EXEC_FINISH")) {
                    eventLog.add("EXEC_FINISH");
                }
            }
        };

        eventBus.register(testSubscriber);

        eventBus.publishExecutionStarted("MasterExecution");
        eventBus.publishSuiteStarted("SmokeSuite");
        eventBus.publishTestStarted("test-1", "userLogin");
        eventBus.publishTestPassed("test-1", "userLogin", 150);
        eventBus.publishTestStarted("test-2", "checkoutFail");
        eventBus.publishTestFailed("test-2", "checkoutFail", new AssertionError("expected 200 but got 500"), 300);
        eventBus.publishTestStarted("test-3", "profileUpdate");
        eventBus.publishTestSkipped("test-3", "profileUpdate", new RuntimeException("skip reason"));
        eventBus.publishSuiteFinished("SmokeSuite");
        eventBus.publishExecutionFinished();
        eventBus.unregister(testSubscriber);

        Assert.assertEquals(eventLog.size(), 10);
        Assert.assertEquals(eventLog.get(0), "EXEC_START:MasterExecution");
        Assert.assertEquals(eventLog.get(1), "SUITE_START:SmokeSuite");
        Assert.assertEquals(eventLog.get(2), "TEST_START:test-1");
        Assert.assertEquals(eventLog.get(3), "TEST_PASS:test-1");
        Assert.assertEquals(eventLog.get(4), "TEST_START:test-2");
        Assert.assertEquals(eventLog.get(5), "TEST_FAIL:test-2:expected 200 but got 500");
        Assert.assertEquals(eventLog.get(6), "TEST_START:test-3");
        Assert.assertEquals(eventLog.get(7), "TEST_SKIP:test-3");
        Assert.assertEquals(eventLog.get(8), "SUITE_FINISH:SmokeSuite");
        Assert.assertEquals(eventLog.get(9), "EXEC_FINISH");
    }

    @Test
    public void testExceptionIsolation() {
        AtomicBoolean healthySubscriberInvoked = new AtomicBoolean(false);

        TestLifecycleSubscriber faultySubscriber = new TestLifecycleSubscriber() {
            @Override
            public void onTestStarted(TestLifecycleEvent.TestStarted event) {
                throw new RuntimeException("Deliberate subscriber failure");
            }
        };

        TestLifecycleSubscriber healthySubscriber = new TestLifecycleSubscriber() {
            @Override
            public void onTestStarted(TestLifecycleEvent.TestStarted event) {
                healthySubscriberInvoked.set(true);
            }
        };

        eventBus.register(faultySubscriber);
        eventBus.register(healthySubscriber);

        // Publication should not throw despite faulty subscriber
        eventBus.publishTestStarted("test-fault-isolation", "testMethod");

        Assert.assertTrue(healthySubscriberInvoked.get(),
                "Healthy subscriber must be invoked even if another subscriber throws an exception");
    }

    @Test
    public void testUnregisterSubscriber() {
        AtomicInteger invocationCount = new AtomicInteger(0);

        TestLifecycleSubscriber subscriber = new TestLifecycleSubscriber() {
            @Override
            public void onTestStarted(TestLifecycleEvent.TestStarted event) {
                invocationCount.incrementAndGet();
            }
        };

        eventBus.register(subscriber);
        eventBus.publishTestStarted("test-1", "test1");
        Assert.assertEquals(invocationCount.get(), 1);

        eventBus.unregister(subscriber);
        eventBus.publishTestStarted("test-2", "test2");
        Assert.assertEquals(invocationCount.get(), 1, "Unregistered subscriber must not receive subsequent events");
    }

    @Test
    public void testScreenRecordingLifecycleSubscriberDefaultRegistered() {
        boolean hasRecordingSubscriber = eventBus.getSubscribers().stream()
                .anyMatch(s -> s instanceof ScreenRecordingLifecycleSubscriber);
        Assert.assertTrue(hasRecordingSubscriber, "ScreenRecordingLifecycleSubscriber must be registered by default");
    }

    @Test
    public void testScreenRecordingLifecycleGracefulHandlingWithoutDriver() {
        ScreenRecordingLifecycleSubscriber subscriber = new ScreenRecordingLifecycleSubscriber();
        // None of these should throw NPE or crash when invoked without an active WebDriver
        subscriber.onTestStarted(new TestLifecycleEvent.TestStarted("test-offline", "testMethod"));
        subscriber.onTestPassed(new TestLifecycleEvent.TestPassed("test-offline", "testMethod", 100));
        subscriber.onTestFailed(new TestLifecycleEvent.TestFailed("test-offline", "testMethod", new RuntimeException("error"), 100));
        subscriber.onTestSkipped(new TestLifecycleEvent.TestSkipped("test-offline", "testMethod", null));
        subscriber.onSuiteFinished(new TestLifecycleEvent.SuiteFinished("OfflineSuite"));
        subscriber.onExecutionFinished(new TestLifecycleEvent.ExecutionFinished());
    }
}
