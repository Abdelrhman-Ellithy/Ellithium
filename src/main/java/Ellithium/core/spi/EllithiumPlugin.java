package Ellithium.core.spi;

/**
 * Base contract for all pluggable framework extensions in Ellithium.
 * Plugins are discovered via standard Java {@link java.util.ServiceLoader} SPI
 * or registered programmatically with {@link PluginRegistry}.
 * <p>
 * Zero reflection is required at runtime, ensuring complete compatibility with
 * Ahead-Of-Time (AOT) GraalVM native images and sub-millisecond cold starts.
 */
public interface EllithiumPlugin {

    /**
     * @return Human-readable name identifying this plugin.
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Execution order for plugins of the same type. Lower numerical values execute first.
     * Default order is 0.
     *
     * @return priority order
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Determines whether this plugin is active.
     *
     * @return true if enabled, false to bypass invocation
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Lifecycle callback invoked when the plugin registry is initialized or when
     * this plugin is registered.
     */
    default void onInitialize() {
        // default no-op
    }

    /**
     * Lifecycle callback invoked during framework teardown or when the plugin registry is shut down.
     */
    default void onShutdown() {
        // default no-op
    }
}
