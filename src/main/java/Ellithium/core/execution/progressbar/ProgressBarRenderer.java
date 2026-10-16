package Ellithium.core.execution.progressbar;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Dynamic, responsive ANSI/Unicode renderer for the Ellithium test execution progress bar,
 * status line, executive completion banner, and OS console window titles.
 *
 * <p>Dynamically adapts to the current terminal window column size and guarantees
 * zero character corruption ('?' glyph degradation) on non-UTF-8 console code pages.
 */
public final class ProgressBarRenderer {

    // ANSI Color Sequences
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[91m";
    public static final String GREEN = "\u001B[92m";
    public static final String YELLOW = "\u001B[93m";
    public static final String CYAN = "\u001B[96m";
    public static final String WHITE_BOLD = "\u001B[1;97m";
    public static final String GRAY = "\u001B[90m";
    public static final String CLEAR_LINE = "\u001B[2K\r";

    private static final Pattern ANSI_STRIPPER = Pattern.compile("\\u001B\\[[;\\d]*m");

    // Universal block characters (present in IBM CP437, CP850, and UTF-8)
    private static final char BLOCK_FULL = '█';
    private static final char BLOCK_EMPTY = '░';

    // ASCII fallback characters
    private static final char ASCII_FULL = '=';
    private static final char ASCII_HEAD = '>';
    private static final char ASCII_EMPTY = '-';

    private static final java.time.format.DateTimeFormatter TIMESTAMP_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ProgressBarRenderer() {}

    /**
     * Formats standard Log4j-aligned prefix: "yyyy-MM-dd HH:mm:ss INFO  [Ellithium] - "
     */
    public static String formatLogPrefix(boolean ansi, int width) {
        String ts = java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        if (width >= 90) {
            if (ansi) {
                return GRAY + ts + RESET + " " + CYAN + BOLD + "INFO " + RESET + " " + CYAN + "[Ellithium]" + RESET + " - ";
            } else {
                return ts + " INFO  [Ellithium] - ";
            }
        } else if (width >= 65) {
            String shortTs = ts.substring(11);
            if (ansi) {
                return GRAY + shortTs + RESET + " " + CYAN + BOLD + "INFO " + RESET + " " + CYAN + "[Ellithium]" + RESET + " - ";
            } else {
                return shortTs + " INFO  [Ellithium] - ";
            }
        } else {
            if (ansi) {
                return CYAN + BOLD + "[Ellithium]" + RESET + " - ";
            } else {
                return "[Ellithium] - ";
            }
        }
    }

    /**
     * Snapshot record capturing an atomic point-in-time view of test execution progress.
     */
    public record Snapshot(
            int totalExpected,
            int completed,
            int passed,
            int failed,
            int skipped,
            long elapsedMs,
            String activeTest
    ) {
        public double getPercentage() {
            if (totalExpected <= 0) return (completed > 0) ? 100.0 : 0.0;
            return Math.min(100.0, (completed * 100.0) / totalExpected);
        }

        public double getRate() {
            if (elapsedMs <= 0) return 0.0;
            return (completed * 1000.0) / elapsedMs;
        }

        public long getEtaMs() {
            if (completed <= 0 || totalExpected <= completed || elapsedMs <= 0) return 0L;
            double avgTimePerTest = (double) elapsedMs / completed;
            int remaining = totalExpected - completed;
            return Math.round(avgTimePerTest * remaining);
        }
    }

    /**
     * Renders the complete, real-time terminal progress line dynamically scaled to terminal width.
     *
     * @param s             snapshot state
     * @param terminalWidth current columns width of terminal window
     * @param unicode       whether block characters are supported
     * @param ansi          whether ANSI colors are supported
     * @return formatted single-line progress string
     */
    public static String renderLine(Snapshot s, int terminalWidth, boolean unicode, boolean ansi) {
        int width = Math.max(40, terminalWidth);
        StringBuilder sb = new StringBuilder(256);

        // 1. Prefix: Standard Log4j-aligned timestamp and category header
        sb.append(formatLogPrefix(ansi, width));

        // 2. Dynamic column allocation based on terminal width
        int barLength;
        boolean verboseBadges;
        boolean showEta;
        boolean showRate;

        if (width < 85) {
            barLength = 8;
            verboseBadges = false;
            showEta = false;
            showRate = false;
        } else if (width < 105) {
            barLength = 10;
            verboseBadges = false;
            showEta = false;
            showRate = false;
        } else if (width < 125) {
            barLength = 12;
            verboseBadges = false;
            showEta = true;
            showRate = false;
        } else if (width < 145) {
            barLength = 16;
            verboseBadges = true;
            showEta = true;
            showRate = true;
        } else {
            barLength = 22;
            verboseBadges = true;
            showEta = true;
            showRate = true;
        }

        // 3. Visual Progress Bar
        renderBar(sb, s.getPercentage(), barLength, unicode, ansi);

        // 4. Percentage: " 50.0% "
        if (ansi) {
            sb.append(String.format(Locale.US, " %s%5.1f%%%s ", WHITE_BOLD, s.getPercentage(), RESET));
        } else {
            sb.append(String.format(Locale.US, " %5.1f%% ", s.getPercentage()));
        }

        // 5. Counts: "[completed/total]"
        String totalStr = s.totalExpected() > 0 ? String.valueOf(s.totalExpected()) : "?";
        if (ansi) {
            sb.append(GRAY).append("[").append(RESET);
            sb.append(BOLD).append(s.completed()).append(RESET);
            sb.append(GRAY).append("/").append(totalStr).append("]").append(RESET);
        } else {
            sb.append("[").append(s.completed()).append("/").append(totalStr).append("]");
        }

        // 6. Status Badges: Universal safe labels (Zero '?' character degradation)
        sb.append(" | ");
        if (verboseBadges) {
            if (ansi) {
                sb.append(GREEN).append("PASSED: ").append(s.passed()).append(RESET);
                if (s.failed() > 0) {
                    sb.append(" ").append(RED).append("FAILED: ").append(s.failed()).append(RESET);
                }
                if (s.skipped() > 0) {
                    sb.append(" ").append(YELLOW).append("SKIPPED: ").append(s.skipped()).append(RESET);
                }
            } else {
                sb.append("PASSED: ").append(s.passed());
                if (s.failed() > 0) sb.append(" FAILED: ").append(s.failed());
                if (s.skipped() > 0) sb.append(" SKIPPED: ").append(s.skipped());
            }
        } else {
            // Compact safe badges (P:X, F:Y, S:Z - pure ASCII, zero '?')
            if (ansi) {
                sb.append(GREEN).append("P:").append(s.passed()).append(RESET);
                if (s.failed() > 0) {
                    sb.append(" ").append(RED).append("F:").append(s.failed()).append(RESET);
                }
                if (s.skipped() > 0) {
                    sb.append(" ").append(YELLOW).append("S:").append(s.skipped()).append(RESET);
                }
            } else {
                sb.append("P:").append(s.passed());
                if (s.failed() > 0) sb.append(" F:").append(s.failed());
                if (s.skipped() > 0) sb.append(" S:").append(s.skipped());
            }
        }

        // 7. Elapsed & ETA
        sb.append(" | ").append(formatDuration(s.elapsedMs()));
        if (showEta) {
            long eta = s.getEtaMs();
            if (eta > 0) {
                sb.append(ansi ? GRAY : "").append(" (ETA ").append(formatDuration(eta)).append(")").append(ansi ? RESET : "");
            }
        }

        // 8. Throughput rate
        if (showRate) {
            double rate = s.getRate();
            if (rate > 0.05) {
                sb.append(String.format(Locale.US, " | %.1f t/s", rate));
            }
        }

        // 9. Dynamic active test name (truncated responsively to fit terminal window without wrapping)
        if (s.activeTest() != null && !s.activeTest().isBlank()) {
            int visibleSoFar = getVisibleLength(sb.toString());
            int remainingSpace = width - visibleSoFar - 3; // leave buffer to prevent overflow
            if (remainingSpace >= 12) {
                sb.append(" | ");
                if (ansi) sb.append(YELLOW);
                sb.append(truncate(s.activeTest(), remainingSpace - 3));
                if (ansi) sb.append(RESET);
            }
        }

        String result = sb.toString();
        if (getVisibleLength(result) > width) {
            result = truncate(result, width);
        }
        return result;
    }

    /**
     * Renders the graphical progress bar portion.
     */
    private static void renderBar(StringBuilder sb, double percentage, int width, boolean unicode, boolean ansi) {
        sb.append(ansi ? GRAY : "").append("[").append(ansi ? RESET : "");

        double fillRatio = Math.max(0.0, Math.min(1.0, percentage / 100.0));
        int fullBlocks = (int) Math.round(fillRatio * width);

        if (ansi) {
            sb.append(percentage >= 100.0 ? GREEN : CYAN);
        }

        if (unicode) {
            for (int i = 0; i < fullBlocks; i++) {
                sb.append(BLOCK_FULL);
            }
            if (ansi) sb.append(GRAY);
            for (int i = fullBlocks; i < width; i++) {
                sb.append(BLOCK_EMPTY);
            }
        } else {
            for (int i = 0; i < fullBlocks; i++) {
                sb.append(ASCII_FULL);
            }
            if (fullBlocks < width && fullBlocks > 0) {
                sb.deleteCharAt(sb.length() - 1);
                sb.append(ASCII_HEAD);
            }
            if (ansi) sb.append(GRAY);
            for (int i = fullBlocks; i < width; i++) {
                sb.append(ASCII_EMPTY);
            }
        }

        if (ansi) sb.append(RESET).append(GRAY);
        sb.append("]");
        if (ansi) sb.append(RESET);
    }

    /**
     * Formats safe, clean text for updating the OS console window or tab title.
     * Free of non-ASCII symbols so it never degrades to '?' on Windows.
     */
    public static String renderWindowTitle(Snapshot s) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(String.format(Locale.US, "Ellithium [%.0f%%] %d/%s",
                s.getPercentage(), s.completed(), (s.totalExpected() > 0 ? String.valueOf(s.totalExpected()) : "?")));

        sb.append(" (").append(s.passed()).append(" PASS");
        if (s.failed() > 0) sb.append(" ").append(s.failed()).append(" FAIL");
        if (s.skipped() > 0) sb.append(" ").append(s.skipped()).append(" SKIP");
        sb.append(")");

        long eta = s.getEtaMs();
        if (eta > 0) {
            sb.append(" - ETA: ").append(formatDuration(eta));
        }
        return sb.toString();
    }

    /**
     * Formats the final executive completion banner.
     */
    public static String renderFinalSummary(Snapshot s, boolean unicode, boolean ansi) {
        String bar = "=======================================================================";
        String prefix = formatLogPrefix(ansi, 100);

        StringBuilder sb = new StringBuilder(512);
        if (ansi) sb.append(CYAN);
        sb.append(prefix).append(bar).append("\n");
        if (ansi) sb.append(RESET);

        if (s.failed() == 0) {
            if (ansi) sb.append(GREEN).append(BOLD);
            sb.append(prefix).append("[PASS] SUITE COMPLETED SUCCESSFULLY: ");
        } else {
            if (ansi) sb.append(RED).append(BOLD);
            sb.append(prefix).append("[FAIL] SUITE COMPLETED WITH FAILURES: ");
        }
        sb.append(s.completed()).append("/").append(s.totalExpected() > 0 ? s.totalExpected() : s.completed())
                .append(String.format(Locale.US, " tests executed (%.1f%%) in %s (%.1f tests/sec)\n",
                        s.getPercentage(), formatDuration(s.elapsedMs()), s.getRate()));
        if (ansi) sb.append(RESET);

        if (ansi) sb.append(CYAN);
        sb.append(prefix).append(bar);
        if (ansi) sb.append(RESET);

        return sb.toString();
    }

    /**
     * Formats milliseconds into human-readable duration (e.g. 1m 23s or 4.5s).
     */
    public static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (millis % 1000) / 100;

        if (minutes > 0) {
            return String.format(Locale.US, "%dm %02ds", minutes, seconds);
        } else if (seconds >= 10) {
            return String.format(Locale.US, "%ds", seconds);
        } else {
            return String.format(Locale.US, "%d.%ds", seconds, tenths);
        }
    }

    /**
     * Calculates visible character length after stripping ANSI color codes.
     */
    public static int getVisibleLength(String text) {
        if (text == null) return 0;
        return ANSI_STRIPPER.matcher(text).replaceAll("").length();
    }

    /**
     * Truncates long strings with ellipsis.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return text.substring(0, Math.max(0, maxLength));
        return text.substring(0, maxLength - 3) + "...";
    }
}
