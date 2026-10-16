package Ellithium.core.spi;

import Ellithium.core.driver.DriverConfiguration;
import Ellithium.core.driver.LocalDriverConfig;
import Ellithium.core.driver.LocalDriverType;
import Ellithium.core.logging.LogLevel;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;

public class PluginRegistryTest {

    private PluginRegistry registry;

    @BeforeMethod
    public void setUp() {
        registry = PluginRegistry.getInstance();
        registry.reset();
    }

    @AfterMethod
    public void tearDown() {
        registry.reset();
    }

    @Test
    public void testRegistrySingletonInstance() {
        PluginRegistry instance1 = PluginRegistry.getInstance();
        PluginRegistry instance2 = PluginRegistry.getInstance();
        Assert.assertNotNull(instance1);
        Assert.assertSame(instance1, instance2);
    }

    @Test
    public void testRegisterAndUnregisterTestLifecyclePlugin() {
        AtomicBoolean suiteStarted = new AtomicBoolean(false);
        TestLifecyclePlugin plugin = new TestLifecyclePlugin() {
            @Override
            public void onSuiteStart(String suiteName) {
                suiteStarted.set(true);
            }
        };

        registry.registerPlugin(plugin);
        Assert.assertTrue(registry.getTestLifecyclePlugins().contains(plugin));
        Assert.assertTrue(registry.getAllPlugins().contains(plugin));

        registry.dispatchSuiteStart("TestExecution");
        Assert.assertTrue(suiteStarted.get());

        suiteStarted.set(false);
        registry.unregisterPlugin(plugin);
        Assert.assertFalse(registry.getTestLifecyclePlugins().contains(plugin));
        Assert.assertFalse(registry.getAllPlugins().contains(plugin));

        registry.dispatchSuiteStart("TestExecution");
        Assert.assertFalse(suiteStarted.get());
    }

    @Test
    public void testPluginPriorityOrdering() {
        List<String> executionOrder = new ArrayList<>();

        TestLifecyclePlugin latePlugin = new TestLifecyclePlugin() {
            @Override
            public int getOrder() { return 10; }
            @Override
            public void onSuiteStart(String suiteName) { executionOrder.add("late"); }
        };

        TestLifecyclePlugin earlyPlugin = new TestLifecyclePlugin() {
            @Override
            public int getOrder() { return -5; }
            @Override
            public void onSuiteStart(String suiteName) { executionOrder.add("early"); }
        };

        TestLifecyclePlugin defaultPlugin = new TestLifecyclePlugin() {
            @Override
            public int getOrder() { return 0; }
            @Override
            public void onSuiteStart(String suiteName) { executionOrder.add("default"); }
        };

        registry.registerPlugin(latePlugin);
        registry.registerPlugin(earlyPlugin);
        registry.registerPlugin(defaultPlugin);

        registry.dispatchSuiteStart("OrderSuite");

        Assert.assertEquals(executionOrder, List.of("early", "default", "late"));
    }

    @Test
    public void testDisabledPluginIsIgnored() {
        AtomicBoolean called = new AtomicBoolean(false);
        TestLifecyclePlugin disabledPlugin = new TestLifecyclePlugin() {
            @Override
            public boolean isEnabled() { return false; }
            @Override
            public void onSuiteStart(String suiteName) { called.set(true); }
        };

        registry.registerPlugin(disabledPlugin);
        registry.dispatchSuiteStart("DisabledSuite");
        Assert.assertFalse(called.get());
    }

    @Test
    public void testPluginExceptionIsolation() {
        AtomicBoolean nextPluginExecuted = new AtomicBoolean(false);

        TestLifecyclePlugin failingPlugin = new TestLifecyclePlugin() {
            @Override
            public int getOrder() { return -1; }
            @Override
            public void onTestStart(String testId, String testName) {
                throw new RuntimeException("Deliberate plugin failure");
            }
        };

        TestLifecyclePlugin resilientPlugin = new TestLifecyclePlugin() {
            @Override
            public int getOrder() { return 1; }
            @Override
            public void onTestStart(String testId, String testName) {
                nextPluginExecuted.set(true);
            }
        };

        registry.registerPlugin(failingPlugin);
        registry.registerPlugin(resilientPlugin);

        // Dispatches without throwing exception
        registry.dispatchTestStart("test-1", "sampleTest");
        Assert.assertTrue(nextPluginExecuted.get(), "Subsequent plugins must execute even if one throws");
    }

    @Test
    public void testFullTestLifecycleCallbacks() {
        AtomicInteger startCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);
        AtomicInteger suiteFinishCount = new AtomicInteger(0);

        TestLifecyclePlugin plugin = new TestLifecyclePlugin() {
            @Override
            public void onTestStart(String testId, String testName) { startCount.incrementAndGet(); }
            @Override
            public void onTestSuccess(String testId, String testName, long durationMs) { successCount.incrementAndGet(); }
            @Override
            public void onTestFailure(String testId, String testName, Throwable cause, long durationMs) { failCount.incrementAndGet(); }
            @Override
            public void onTestSkipped(String testId, String testName, Throwable cause) { skipCount.incrementAndGet(); }
            @Override
            public void onSuiteFinish(String suiteName) { suiteFinishCount.incrementAndGet(); }
        };

        registry.registerPlugin(plugin);

        registry.dispatchTestStart("1", "testA");
        registry.dispatchTestSuccess("1", "testA", 100);
        registry.dispatchTestStart("2", "testB");
        registry.dispatchTestFailure("2", "testB", new AssertionError("failed"), 150);
        registry.dispatchTestStart("3", "testC");
        registry.dispatchTestSkipped("3", "testC", null);
        registry.dispatchSuiteFinish("SuiteAll");

        Assert.assertEquals(startCount.get(), 3);
        Assert.assertEquals(successCount.get(), 1);
        Assert.assertEquals(failCount.get(), 1);
        Assert.assertEquals(skipCount.get(), 1);
        Assert.assertEquals(suiteFinishCount.get(), 1);
    }

    @Test
    public void testDriverLifecycleCallbacks() {
        AtomicBoolean driverCreated = new AtomicBoolean(false);
        AtomicBoolean driverQuit = new AtomicBoolean(false);

        DriverLifecyclePlugin plugin = new DriverLifecyclePlugin() {
            @Override
            public void onDriverCreated(WebDriver driver, DriverConfiguration config) {
                driverCreated.set(true);
            }
            @Override
            public void onDriverQuit(WebDriver driver) {
                driverQuit.set(true);
            }
        };

        registry.registerPlugin(plugin);
        Assert.assertTrue(registry.getDriverLifecyclePlugins().contains(plugin));

        WebDriver mockDriver = mock(WebDriver.class);
        DriverConfiguration cfg = new DriverConfiguration(LocalDriverType.Chrome, null, null, null, null, null, null, false);

        registry.dispatchDriverCreated(mockDriver, cfg);
        Assert.assertTrue(driverCreated.get());

        registry.dispatchDriverQuit(mockDriver);
        Assert.assertTrue(driverQuit.get());
    }

    @Test
    public void testReportingCallbacks() {
        AtomicBoolean stepLogged = new AtomicBoolean(false);
        AtomicBoolean artifactAttached = new AtomicBoolean(false);
        AtomicBoolean flushed = new AtomicBoolean(false);

        ReportingPlugin plugin = new ReportingPlugin() {
            @Override
            public void logStep(String stepName, LogLevel level, String details) {
                stepLogged.set(true);
            }
            @Override
            public void attachArtifact(String name, String mimeType, byte[] data) {
                artifactAttached.set(true);
            }
            @Override
            public void flush() {
                flushed.set(true);
            }
        };

        registry.registerPlugin(plugin);
        Assert.assertTrue(registry.getReportingPlugins().contains(plugin));

        registry.dispatchLogStep("Click button", LogLevel.INFO_GREEN, "id: btn");
        registry.dispatchAttachArtifact("log.txt", "text/plain", new byte[]{1, 2});
        registry.dispatchFlush();

        Assert.assertTrue(stepLogged.get());
        Assert.assertTrue(artifactAttached.get());
        Assert.assertTrue(flushed.get());
    }

    @Test
    public void testPluginLifecycleShutdown() {
        AtomicBoolean shutdownCalled = new AtomicBoolean(false);
        EllithiumPlugin plugin = new EllithiumPlugin() {
            @Override
            public void onShutdown() {
                shutdownCalled.set(true);
            }
        };

        registry.registerPlugin(plugin);
        registry.shutdown();
        Assert.assertTrue(shutdownCalled.get());
    }
}
