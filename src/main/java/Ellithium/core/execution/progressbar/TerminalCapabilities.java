package Ellithium.core.execution.progressbar;

import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects terminal capabilities (dynamic terminal window columns, TTY interactivity, ANSI escape codes,
 * Unicode rendering, CI environments) and provides OS console window title update mechanisms.
 */
public final class TerminalCapabilities {

    private static final String OSC_TITLE_PREFIX = "\u001B]0;";
    private static final String OSC_TITLE_SUFFIX = "\u0007";
    private static final Pattern WINDOWS_COLUMNS_PATTERN = Pattern.compile("(?i)columns:\\s*(\\d+)");

    private static volatile int cachedTerminalWidth = -1;
    private static volatile long lastWidthCheck = 0L;

    private TerminalCapabilities() {}

    /**
     * Dynamically detects the current terminal window column width.
     * Queries the OS console device with caching to ensure zero latency.
     */
    public static int getTerminalWidth() {
        // 1. Explicit system property override
        String sys = System.getProperty("terminal.width");
        if (sys == null || sys.isBlank()) {
            sys = System.getProperty(ProgressBarConfig.KEY_BAR_WIDTH);
        }
        if (sys != null && !sys.isBlank()) {
            try {
                int w = Integer.parseInt(sys.trim());
                if (w >= 40 && w <= 500) return w;
            } catch (Exception ignored) {}
        }

        // 2. Environment variable COLUMNS
        String envCols = System.getenv("COLUMNS");
        if (envCols != null && !envCols.isBlank()) {
            try {
                int cols = Integer.parseInt(envCols.trim());
                if (cols >= 40 && cols <= 500) return cols;
            } catch (Exception ignored) {}
        }

        // 3. Cached detected width (valid for 5 seconds to respond to window resizing)
        long now = System.currentTimeMillis();
        if (cachedTerminalWidth > 0 && (now - lastWidthCheck < 5000L)) {
            return cachedTerminalWidth;
        }

        // 4. Query OS console dimensions
        int detected = detectOsConsoleWidth();
        cachedTerminalWidth = detected;
        lastWidthCheck = now;
        return detected;
    }

    private static int detectOsConsoleWidth() {
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "mode con");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = WINDOWS_COLUMNS_PATTERN.matcher(line);
                        if (matcher.find()) {
                            int cols = Integer.parseInt(matcher.group(1));
                            if (cols >= 40 && cols <= 500) {
                                return cols;
                            }
                        }
                    }
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder("sh", "-c", "tput cols 2>/dev/null || echo 100");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        int cols = Integer.parseInt(line.trim());
                        if (cols >= 40 && cols <= 500) {
                            return cols;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 100; // Sensible modern default width
    }

    /**
     * Checks if current process is running in a recognized continuous integration environment.
     */
    public static boolean isCiEnvironment() {
        return System.getenv("CI") != null
                || System.getenv("GITHUB_ACTIONS") != null
                || System.getenv("GITLAB_CI") != null
                || System.getenv("JENKINS_URL") != null
                || System.getenv("TRAVIS") != null
                || System.getenv("CIRCLECI") != null
                || System.getenv("TEAMCITY_VERSION") != null;
    }

    /**
     * Checks if standard output is connected to an interactive terminal/console.
     */
    public static boolean isInteractive() {
        if (isCiEnvironment()) {
            return false;
        }
        if (System.console() != null) {
            return true;
        }
        String wtSession = System.getenv("WT_SESSION");
        String term = System.getenv("TERM");
        if (wtSession != null && !wtSession.isBlank()) {
            return true;
        }
        return term != null && !term.isBlank() && !"dumb".equalsIgnoreCase(term);
    }

    /**
     * Checks if the console supports ANSI escape sequences.
     */
    public static boolean supportsAnsi() {
        String term = System.getenv("TERM");
        if ("dumb".equalsIgnoreCase(term)) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return System.getenv("WT_SESSION") != null
                    || System.getenv("ANSICON") != null
                    || "xterm-256color".equalsIgnoreCase(term)
                    || System.console() != null;
        }
        return true;
    }

    /**
     * Checks if the console supports Unicode characters for smooth blocks ('█' and '░').
     * Defaults to true on modern terminals unless explicitly disabled.
     */
    public static boolean supportsUnicode() {
        String sys = System.getProperty(ProgressBarConfig.KEY_UNICODE);
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys.trim());
        }
        String term = System.getenv("TERM");
        if ("dumb".equalsIgnoreCase(term)) {
            return false;
        }
        return true;
    }

    /**
     * Updates the OS console window or tab title using the standard ANSI OSC sequence.
     * Works on Windows Terminal, CMD, PowerShell, macOS Terminal, iTerm2, and modern Linux shells.
     *
     * @param title the window title text
     */
    public static void setConsoleWindowTitle(String title) {
        if (title == null || title.isBlank() || !ProgressBarConfig.isWindowTitleEnabled()) {
            return;
        }
        try {
            PrintStream out = System.out;
            out.print(OSC_TITLE_PREFIX + title + OSC_TITLE_SUFFIX);
            out.flush();
        } catch (Throwable ignored) {
            // Never let window title update failure interrupt test execution
        }
    }
}
