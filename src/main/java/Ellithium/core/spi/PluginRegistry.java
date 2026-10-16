package Ellithium.core.spi;

import Ellithium.core.driver.DriverConfiguration;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import org.openqa.selenium.WebDriver;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized, zero-reflection plugin coordinator for Ellithium.
 * Automatically discovers framework plugins via {@link ServiceLoader} SPI
 * and dispatches test, driver, and reporting lifecycle events.
 * <p>
 * Zero runtime reflection or dynamic bytecode generation is utilized, ensuring
 * seamless AOT compilation with GraalVM Native Image.
 */
public final class PluginRegistry {

    private static final PluginRegistry INSTANCE = new PluginRegistry();

    private final List<TestLifecyclePlugin> testLifecyclePlugins = new CopyOnWriteArrayList<>();
    private final List<DriverLifecyclePlugin> driverLifecyclePlugins = new CopyOnWriteArrayList<>();
    private final List<ReportingPlugin> reportingPlugins = new CopyOnWriteArrayList<>();
    private final List<EllithiumPlugin> allPlugins = new CopyOnWriteArrayList<>();

    private volatile boolean initialized = false;

    private PluginRegistry() {
        discoverAndLoadPlugins();
    }

    public static PluginRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Discovers and registers all plugins declared in META-INF/services via {@link ServiceLoader}.
     */
    public synchronized void discoverAndLoadPlugins() {
        testLifecyclePlugins.clear();
        driverLifecyclePlugins.clear();
        reportingPlugins.clear();
        allPlugins.clear();

        loadSpi(TestLifecyclePlugin.class, testLifecyclePlugins);
        loadSpi(DriverLifecyclePlugin.class, driverLifecyclePlugins);
        loadSpi(ReportingPlugin.class, reportingPlugins);

        // Sort each collection according to order()
        sortPlugins(testLifecyclePlugins);
        sortPlugins(driverLifecyclePlugins);
        sortPlugins(reportingPlugins);

        for (EllithiumPlugin p : allPlugins) {
            try {
                if (p.isEnabled()) {
                    p.onInitialize();
                }
            } catch (Exception e) {
                Logger.error("Error initializing plugin: " + p.getName() + " -> " + e.getMessage());
            }
        }
        initialized = true;
    }

    private <T extends EllithiumPlugin> void loadSpi(Class<T> serviceClass, List<T> targetList) {
        try {
            ServiceLoader<T> loader = ServiceLoader.load(serviceClass, PluginRegistry.class.getClassLoader());
            for (T plugin : loader) {
                if (!targetList.contains(plugin)) {
                    targetList.add(plugin);
                    if (!allPlugins.contains(plugin)) {
                        allPlugins.add(plugin);
                    }
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to load SPI services for " + serviceClass.getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Programmatically registers a plugin instance.
     *
     * @param plugin the plugin to register
     */
    public synchronized void registerPlugin(EllithiumPlugin plugin) {
        if (plugin == null) {
            return;
        }
        if (!allPlugins.contains(plugin)) {
            allPlugins.add(plugin);
        }
        if (plugin instanceof TestLifecyclePlugin t && !testLifecyclePlugins.contains(t)) {
            testLifecyclePlugins.add(t);
            sortPlugins(testLifecyclePlugins);
        }
        if (plugin instanceof DriverLifecyclePlugin d && !driverLifecyclePlugins.contains(d)) {
            driverLifecyclePlugins.add(d);
            sortPlugins(driverLifecyclePlugins);
        }
        if (plugin instanceof ReportingPlugin r && !reportingPlugins.contains(r)) {
            reportingPlugins.add(r);
            sortPlugins(reportingPlugins);
        }
        try {
            if (plugin.isEnabled()) {
                plugin.onInitialize();
            }
        } catch (Exception e) {
            Logger.error("Failed to initialize registered plugin " + plugin.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Unregisters a previously registered plugin.
     *
     * @param plugin the plugin to remove
     */
    public synchronized void unregisterPlugin(EllithiumPlugin plugin) {
        if (plugin == null) {
            return;
        }
        try {
            plugin.onShutdown();
        } catch (Exception ignored) {
        }
        allPlugins.remove(plugin);
        if (plugin instanceof TestLifecyclePlugin t) {
            testLifecyclePlugins.remove(t);
        }
        if (plugin instanceof DriverLifecyclePlugin d) {
            driverLifecyclePlugins.remove(d);
        }
        if (plugin instanceof ReportingPlugin r) {
            reportingPlugins.remove(r);
        }
    }

    /**
     * Clears all registered plugins and reinitializes with defaults.
     */
    public synchronized void reset() {
        for (EllithiumPlugin p : allPlugins) {
            try {
                p.onShutdown();
            } catch (Exception ignored) {
            }
        }
        testLifecyclePlugins.clear();
        driverLifecyclePlugins.clear();
        reportingPlugins.clear();
        allPlugins.clear();
        initialized = false;
        discoverAndLoadPlugins();
    }

    public List<TestLifecyclePlugin> getTestLifecyclePlugins() {
        return Collections.unmodifiableList(testLifecyclePlugins);
    }

    public List<DriverLifecyclePlugin> getDriverLifecyclePlugins() {
        return Collections.unmodifiableList(driverLifecyclePlugins);
    }

    public List<ReportingPlugin> getReportingPlugins() {
        return Collections.unmodifiableList(reportingPlugins);
    }

    public List<EllithiumPlugin> getAllPlugins() {
        return Collections.unmodifiableList(allPlugins);
    }

    // --- Dispatch Methods ---

    public void dispatchSuiteStart(String suiteName) {
        for (TestLifecyclePlugin p : testLifecyclePlugins) {
            safeExec(p, () -> p.onSuiteStart(suiteName));
        }
    }

    public void dispatchSuiteFinish(String suiteName) {
        for (TestLifecyclePlugin p : testLifecyclePlugins) {
            safeExec(p, () -> p.onSuiteFinish(suiteName));
        }
    }

    public void dispatchTestStart(String testId, String testName) {
        for (TestLifecyclePlugin p : testLifecyclePlugins) {
            safeExec(p, () -> p.onTestStart(testId, testName));
        }
    }

    public void dispatchTestSuccess(String testId, String testName, long durationMs) {
        for (TestLifecyclePlugin p : testLifecyclePlugins) {
            safeExec(p, () -> p.onTestSuccess(testId, testName, durationMs));
        }
    }

    public void dispatchTestFailure(String testId, String testName, Throwable cause, long durationMs) {
        for (TestLifecyclePlugin p : testLifecyclePlugins) {
            safeExec(p, () -> p.onTestFailure(testId, testName, cause, durationMs));
        }
    }

    public void dispatchTestSkipped(String testId, String testName, Throwable cause) {
        for (TestLifecyclePlugin p : testLifecyclePlugins) {
            safeExec(p, () -> p.onTestSkipped(testId, testName, cause));
        }
    }

    public void dispatchDriverCreated(WebDriver driver, DriverConfiguration config) {
        for (DriverLifecyclePlugin p : driverLifecyclePlugins) {
            safeExec(p, () -> p.onDriverCreated(driver, config));
        }
    }

    public void dispatchDriverQuit(WebDriver driver) {
        for (DriverLifecyclePlugin p : driverLifecyclePlugins) {
            safeExec(p, () -> p.onDriverQuit(driver));
        }
    }

    public void dispatchLogStep(String stepName, LogLevel level, String details) {
        for (ReportingPlugin p : reportingPlugins) {
            safeExec(p, () -> p.logStep(stepName, level, details));
        }
    }

    public void dispatchAttachArtifact(String name, String mimeType, byte[] data) {
        for (ReportingPlugin p : reportingPlugins) {
            safeExec(p, () -> p.attachArtifact(name, mimeType, data));
        }
    }

    public void dispatchFlush() {
        for (ReportingPlugin p : reportingPlugins) {
            safeExec(p, p::flush);
        }
    }

    public synchronized void shutdown() {
        for (EllithiumPlugin p : allPlugins) {
            try {
                p.onShutdown();
            } catch (Exception e) {
                Logger.error("Error shutting down plugin " + p.getName() + ": " + e.getMessage());
            }
        }
    }

    private <T extends EllithiumPlugin> void sortPlugins(List<T> list) {
        list.sort(Comparator.comparingInt(EllithiumPlugin::getOrder));
    }

    private void safeExec(EllithiumPlugin plugin, Runnable action) {
        if (plugin != null && plugin.isEnabled()) {
            try {
                action.run();
            } catch (Throwable t) {
                Logger.error("Plugin " + plugin.getName() + " threw an unhandled exception: " + t.getMessage());
            }
        }
    }
}
