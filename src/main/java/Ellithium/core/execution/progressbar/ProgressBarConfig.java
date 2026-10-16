package Ellithium.core.execution.progressbar;

import Ellithium.config.management.ConfigContext;
import Ellithium.Utilities.helpers.PropertyHelper;

/**
 * Configuration for the modern CLI test progress bar and window title updates.
 * Supports system property overrides and configuration files.
 */
public final class ProgressBarConfig {

    public static final String KEY_ENABLED = "ellithium.progressbar.enabled";
    public static final String KEY_WINDOW_TITLE_ENABLED = "ellithium.progressbar.windowTitle.enabled";
    public static final String KEY_BAR_WIDTH = "ellithium.progressbar.width";
    public static final String KEY_THROTTLE_MS = "ellithium.progressbar.throttleMs";
    public static final String KEY_UNICODE = "ellithium.progressbar.unicode";
    public static final String KEY_CI_MODE = "ellithium.progressbar.ciMode";

    private static final int DEFAULT_WIDTH = 28;
    private static final long DEFAULT_THROTTLE_MS = 50L;

    private ProgressBarConfig() {}

    /**
     * Checks if the test progress bar is enabled.
     * Defaults to true unless explicitly disabled.
     */
    public static boolean isEnabled() {
        String sys = System.getProperty(KEY_ENABLED);
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys);
        }
        String configPath = ConfigContext.getConfigFilePath();
        if (configPath != null && PropertyHelper.keyExists(configPath, KEY_ENABLED)) {
            return Boolean.parseBoolean(PropertyHelper.getDataFromProperties(configPath, KEY_ENABLED));
        }
        return true;
    }

    /**
     * Checks if real-time terminal/OS console window title updates are enabled.
     * Defaults to true unless explicitly disabled.
     */
    public static boolean isWindowTitleEnabled() {
        String sys = System.getProperty(KEY_WINDOW_TITLE_ENABLED);
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys);
        }
        String configPath = ConfigContext.getConfigFilePath();
        if (configPath != null && PropertyHelper.keyExists(configPath, KEY_WINDOW_TITLE_ENABLED)) {
            return Boolean.parseBoolean(PropertyHelper.getDataFromProperties(configPath, KEY_WINDOW_TITLE_ENABLED));
        }
        return true;
    }

    /**
     * Gets the width (in character blocks) of the visual progress bar.
     */
    public static int getBarWidth() {
        String sys = System.getProperty(KEY_BAR_WIDTH);
        if (sys != null && !sys.isBlank()) {
            try {
                int w = Integer.parseInt(sys.trim());
                if (w >= 10 && w <= 100) return w;
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_WIDTH;
    }

    /**
     * Gets the throttle interval in milliseconds to prevent console tearing during parallel runs.
     */
    public static long getThrottleMs() {
        String sys = System.getProperty(KEY_THROTTLE_MS);
        if (sys != null && !sys.isBlank()) {
            try {
                long t = Long.parseLong(sys.trim());
                if (t >= 0 && t <= 2000) return t;
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_THROTTLE_MS;
    }

    /**
     * Checks if Unicode block characters are enabled.
     */
    public static boolean isUnicodeEnabled() {
        String sys = System.getProperty(KEY_UNICODE);
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys);
        }
        return TerminalCapabilities.supportsUnicode();
    }

    /**
     * Checks if running in CI mode (milestone lines instead of in-place carriage returns).
     */
    public static boolean isCiMode() {
        String sys = System.getProperty(KEY_CI_MODE);
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys);
        }
        return TerminalCapabilities.isCiEnvironment();
    }
}
