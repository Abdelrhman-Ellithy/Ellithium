package Ellithium.core.spi;

import Ellithium.core.logging.LogLevel;

/**
 * Service Provider Interface (SPI) for reporting backends and artifact sinks.
 */
public interface ReportingPlugin extends EllithiumPlugin {

    /**
     * Dispatched when an action or log step is executed.
     *
     * @param stepName name/summary of the step
     * @param level severity level of the log
     * @param details detailed message or parameter payload
     */
    default void logStep(String stepName, LogLevel level, String details) {
        // default no-op
    }

    /**
     * Dispatched when an artifact (screenshot, video, network dump, JSON telemetry) is generated.
     *
     * @param name artifact name
     * @param mimeType MIME type of artifact (e.g. image/png, video/mp4, application/json)
     * @param data binary payload
     */
    default void attachArtifact(String name, String mimeType, byte[] data) {
        // default no-op
    }

    /**
     * Flushes buffered logs and attachments.
     */
    default void flush() {
        // default no-op
    }
}
