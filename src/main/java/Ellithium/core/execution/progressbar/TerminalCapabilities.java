package Ellithium.core.execution.progressbar;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Detects terminal capabilities (TTY interactivity, ANSI escape codes, Unicode rendering, CI environments)
 * and provides window title update mechanisms.
 */
public final class TerminalCapabilities {

    private static final String OSC_TITLE_PREFIX = "\u001B]0;";
    private static final String OSC_TITLE_SUFFIX = "\u0007";

    private TerminalCapabilities() {}

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
        // System.console() is non-null when running attached to an interactive terminal
        if (System.console() != null) {
            return true;
        }
        // In some IDEs or shells (e.g. Windows Terminal, ConEmu, Mintty), WT_SESSION or TERM is set
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
        // Windows Terminal and modern Windows 10/11 shells natively support ANSI
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
     * Checks if the console supports UTF-8 / Unicode characters for smooth blocks.
     */
    public static boolean supportsUnicode() {
        String stdoutEncoding = System.getProperty("stdout.encoding");
        if (stdoutEncoding != null && stdoutEncoding.toUpperCase().contains("UTF")) {
            return true;
        }
        String nativeEncoding = System.getProperty("native.encoding");
        if (nativeEncoding != null && nativeEncoding.toUpperCase().contains("UTF")) {
            return true;
        }
        Charset def = Charset.defaultCharset();
        if (StandardCharsets.UTF_8.equals(def)) {
            return true;
        }
        // Modern Windows Terminal and Linux/macOS terminals support UTF-8 by default
        String wtSession = System.getenv("WT_SESSION");
        return wtSession != null && !wtSession.isBlank();
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
