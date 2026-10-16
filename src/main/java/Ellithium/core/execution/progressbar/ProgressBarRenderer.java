package Ellithium.core.execution.progressbar;

import java.util.Locale;

/**
 * High-performance, ANSI/Unicode renderer for the Ellithium test execution progress bar,
 * status line, executive completion banner, and OS console window titles.
 */
public final class ProgressBarRenderer {

    // ANSI Color Sequences
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String RED = "\u001B[91m";
    public static final String GREEN = "\u001B[92m";
    public static final String YELLOW = "\u001B[93m";
    public static final String BLUE = "\u001B[94m";
    public static final String MAGENTA = "\u001B[95m";
    public static final String CYAN = "\u001B[96m";
    public static final String WHITE_BOLD = "\u001B[1;97m";
    public static final String GRAY = "\u001B[90m";
    public static final String CLEAR_LINE = "\u001B[2K\r";

    // Unicode block characters for fractional high-resolution rendering
    private static final char UNICODE_FULL_BLOCK = '█';
    private static final char UNICODE_EMPTY_BLOCK = '░';
    private static final char[] UNICODE_FRACTIONS = { ' ', '▏', '▎', '▍', '▌', '▋', '▊', '▉' };

    // ASCII Fallbacks
    private static final char ASCII_FULL_BLOCK = '=';
    private static final char ASCII_HEAD_BLOCK = '>';
    private static final char ASCII_EMPTY_BLOCK = '-';

    private ProgressBarRenderer() {}

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
     * Renders the complete, real-time terminal progress line.
     */
    public static String renderLine(Snapshot s, int width, boolean unicode, boolean ansi) {
        StringBuilder sb = new StringBuilder(256);

        if (ansi) {
            sb.append(CYAN).append(BOLD).append("Ellithium").append(RESET).append(" ");
        } else {
            sb.append("[Ellithium] ");
        }

        // 1. Visual Progress Bar
        renderBar(sb, s.getPercentage(), width, unicode, ansi);

        // 2. Percentage
        if (ansi) {
            sb.append(String.format(Locale.US, " %s%5.1f%%%s ", WHITE_BOLD, s.getPercentage(), RESET));
        } else {
            sb.append(String.format(Locale.US, " %5.1f%% ", s.getPercentage()));
        }

        // 3. Counts [completed/total]
        if (ansi) {
            sb.append(GRAY).append("[").append(RESET);
            sb.append(BOLD).append(s.completed()).append(RESET);
            sb.append(GRAY).append("/").append(s.totalExpected() > 0 ? s.totalExpected() : "?").append("]").append(RESET);
        } else {
            sb.append("[").append(s.completed()).append("/").append(s.totalExpected() > 0 ? s.totalExpected() : "?").append("]");
        }

        // 4. Badges (Passed, Failed, Skipped)
        sb.append(" | ");
        if (ansi) {
            sb.append(GREEN).append(unicode ? "✔ " : "P:").append(s.passed()).append(RESET).append(" ");
            if (s.failed() > 0) {
                sb.append(RED).append(unicode ? "✖ " : "F:").append(s.failed()).append(RESET).append(" ");
            }
            if (s.skipped() > 0) {
                sb.append(YELLOW).append(unicode ? "↷ " : "S:").append(s.skipped()).append(RESET).append(" ");
            }
        } else {
            sb.append("P:").append(s.passed()).append(" ");
            if (s.failed() > 0) sb.append("F:").append(s.failed()).append(" ");
            if (s.skipped() > 0) sb.append("S:").append(s.skipped()).append(" ");
        }

        // 5. Elapsed & ETA
        sb.append("| ");
        sb.append(formatDuration(s.elapsedMs()));
        long eta = s.getEtaMs();
        if (eta > 0) {
            sb.append(ansi ? GRAY : "").append(" (ETA ").append(formatDuration(eta)).append(")").append(ansi ? RESET : "");
        }

        // 6. Throughput rate
        double rate = s.getRate();
        if (rate > 0.05) {
            sb.append(String.format(Locale.US, " | %.1f t/s", rate));
        }

        // 7. Active running test
        if (s.activeTest() != null && !s.activeTest().isBlank()) {
            sb.append(" | ");
            if (ansi) sb.append(YELLOW);
            sb.append(truncate(s.activeTest(), 30));
            if (ansi) sb.append(RESET);
        }

        return sb.toString();
    }

    /**
     * Renders the graphical progress bar portion.
     */
    private static void renderBar(StringBuilder sb, double percentage, int width, boolean unicode, boolean ansi) {
        sb.append(ansi ? GRAY : "").append("[").append(ansi ? RESET : "");

        double fillRatio = Math.max(0.0, Math.min(1.0, percentage / 100.0));
        double exactBlocks = fillRatio * width;
        int fullBlocks = (int) Math.floor(exactBlocks);
        double remainder = exactBlocks - fullBlocks;

        if (ansi) {
            if (percentage >= 100.0) {
                sb.append(GREEN);
            } else {
                sb.append(CYAN);
            }
        }

        if (unicode) {
            for (int i = 0; i < fullBlocks; i++) {
                sb.append(UNICODE_FULL_BLOCK);
            }
            if (fullBlocks < width) {
                int fracIndex = (int) Math.round(remainder * (UNICODE_FRACTIONS.length - 1));
                if (fracIndex > 0) {
                    sb.append(UNICODE_FRACTIONS[fracIndex]);
                    fullBlocks++;
                }
            }
            if (ansi) sb.append(GRAY);
            for (int i = fullBlocks; i < width; i++) {
                sb.append(UNICODE_EMPTY_BLOCK);
            }
        } else {
            for (int i = 0; i < fullBlocks; i++) {
                sb.append(ASCII_FULL_BLOCK);
            }
            if (fullBlocks < width) {
                sb.append(ASCII_HEAD_BLOCK);
                fullBlocks++;
            }
            if (ansi) sb.append(GRAY);
            for (int i = fullBlocks; i < width; i++) {
                sb.append(ASCII_EMPTY_BLOCK);
            }
        }

        if (ansi) sb.append(RESET).append(GRAY);
        sb.append("]");
        if (ansi) sb.append(RESET);
    }

    /**
     * Formats concise, clean text for updating the OS console window or tab title.
     */
    public static String renderWindowTitle(Snapshot s) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(String.format(Locale.US, "Ellithium [%.0f%%] %d/%s",
                s.getPercentage(), s.completed(), (s.totalExpected() > 0 ? String.valueOf(s.totalExpected()) : "?")));

        sb.append(" (").append(s.passed()).append("✔");
        if (s.failed() > 0) sb.append(" ").append(s.failed()).append("✖");
        if (s.skipped() > 0) sb.append(" ").append(s.skipped()).append("↷");
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
        String bar = unicode ? "═══════════════════════════════════════════════════════════════════════"
                : "=======================================================================";
        String check = unicode ? "✔ " : "[PASS] ";
        String cross = unicode ? "✖ " : "[FAIL] ";

        StringBuilder sb = new StringBuilder(512);
        if (ansi) sb.append(CYAN);
        sb.append("[Ellithium] ").append(bar).append("\n");
        if (ansi) sb.append(RESET);

        if (s.failed() == 0) {
            if (ansi) sb.append(GREEN).append(BOLD);
            sb.append("[Ellithium] ").append(check).append("SUITE COMPLETED SUCCESSFULLY: ");
        } else {
            if (ansi) sb.append(RED).append(BOLD);
            sb.append("[Ellithium] ").append(cross).append("SUITE COMPLETED WITH FAILURES: ");
        }
        sb.append(s.completed()).append("/").append(s.totalExpected() > 0 ? s.totalExpected() : s.completed())
                .append(String.format(Locale.US, " tests executed (%.1f%%) in %s (%.1f tests/sec)\n",
                        s.getPercentage(), formatDuration(s.elapsedMs()), s.getRate()));
        if (ansi) sb.append(RESET);

        sb.append("[Ellithium] Results: ");
        if (ansi) sb.append(GREEN).append(BOLD);
        sb.append(s.passed()).append(" Passed").append(ansi ? RESET : "").append(" | ");
        if (s.failed() > 0) {
            if (ansi) sb.append(RED).append(BOLD);
            sb.append(s.failed()).append(" Failed").append(ansi ? RESET : "").append(" | ");
        }
        if (s.skipped() > 0) {
            if (ansi) sb.append(YELLOW);
            sb.append(s.skipped()).append(" Skipped").append(ansi ? RESET : "");
        } else {
            sb.append("0 Skipped");
        }
        sb.append("\n");

        if (ansi) sb.append(CYAN);
        sb.append("[Ellithium] ").append(bar);
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
     * Truncates long strings with ellipsis.
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
