package Ellithium.core.logging;

import Ellithium.config.management.ConfigContext;
import Ellithium.core.execution.context.TestContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.spi.ExtendedLogger;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static Ellithium.core.logging.LogLevel.*;
import static Ellithium.core.logging.LogLevel.DEBUG;

public class Logger {
    private static final String FQCN = Logger.class.getName();
    private static final String REPORTER_FQCN = "Ellithium.core.reporting.Reporter";

    private static final ConcurrentHashMap<String, List<String>> testLogsMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<List<String>> threadLogs = ThreadLocal.withInitial(ArrayList::new);
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\e\\[[;\\d]*m");

    private static final Map<LogLevel, String> LEVEL_STRING_MAP = Map.ofEntries(
            Map.entry(INFO_BLUE, "INFO"),
            Map.entry(ERROR, "ERROR"),
            Map.entry(TRACE, "TRACE"),
            Map.entry(WARN, "WARN"),
            Map.entry(DEBUG, "DEBUG")
    );

    private static boolean isFrameworkInternal(String className) {
        if (className == null) return false;
        if (className.endsWith("Test") || className.endsWith("Tests")
                || className.contains(".tests.") || className.contains(".pages.")) {
            return false;
        }
        return className.startsWith("Ellithium.core.logging.")
                || className.startsWith("Ellithium.core.reporting.")
                || className.startsWith("Ellithium.core.execution.listener.seleniumListener")
                || className.startsWith("Ellithium.Utilities.interactions.")
                || className.startsWith("org.openqa.selenium.");
    }

    private static String getFqcn() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String lastInternalClass = FQCN;
        for (int i = 1; i < Math.min(stack.length, 25); i++) {
            String cls = stack[i].getClassName();
            if (isFrameworkInternal(cls)) {
                lastInternalClass = cls;
            } else if (!lastInternalClass.equals(FQCN)) {
                return lastInternalClass;
            }
        }
        return lastInternalClass;
    }

    private static ExtendedLogger getLogger() {
        String testName = ThreadContext.get("testName");
        if (testName != null && !testName.isBlank() && !"Ellithium".equals(testName)) {
            return (ExtendedLogger) LogManager.getLogger(testName);
        }
        return (ExtendedLogger) LogManager.getLogger("Ellithium");
    }

    public static String stripAnsi(String message) {
        if (message == null) return null;
        return ANSI_PATTERN.matcher(message).replaceAll("");
    }

    public static String getCurrentTestIdentifier() {
        String testId = ThreadContext.get("testIdentifier");
        if (testId != null && !testId.isBlank()) {
            return testId;
        }
        testId = ThreadContext.get("testName");
        if (testId != null && !testId.isBlank()) {
            return testId;
        }
        return TestContext.testId();
    }

    public static String getCurrentTestName() {
        String testName = ThreadContext.get("testName");
        if (testName != null && !testName.isBlank()) {
            return testName;
        }
        return TestContext.testName();
    }

    private static void ensureTestNameContext() {
        if (ThreadContext.get("testName") == null) {
            String scopedName = TestContext.testName();
            ThreadContext.put("testName", (scopedName != null && !scopedName.isBlank()) ? scopedName : "Ellithium");
        }
    }

    // Log a TRACE level message
    public static void trace(String message) {
        if (ConfigContext.isLoggingOn()){
            ensureTestNameContext();
            getLogger().logIfEnabled(getFqcn(), Level.TRACE, null, (Object) message, null);
            logToCurrentExecution(LogLevel.TRACE, message);
        }
    }

    // Log a DEBUG level message
    public static void debug(String message) {
        if (ConfigContext.isLoggingOn()) {
            ensureTestNameContext();
            getLogger().logIfEnabled(getFqcn(), Level.DEBUG, null, (Object) message, null);
            logToCurrentExecution(LogLevel.DEBUG, message);
        }
    }

    // Log an INFO level message
    public static void info(String message) {
        if (ConfigContext.isLoggingOn()) {
            ensureTestNameContext();
            getLogger().logIfEnabled(getFqcn(), Level.INFO, null, (Object) message, null);
            logToCurrentExecution(LogLevel.INFO_BLUE, message);
        }
    }

    // Log a WARN level message
    public static void warn(String message) {
        if (ConfigContext.isLoggingOn()) {
            ensureTestNameContext();
            getLogger().logIfEnabled(getFqcn(), Level.WARN, null, (Object) message, null);
            logToCurrentExecution(WARN, message);
        }
    }

    // Log an ERROR level message
    public static void error(String message) {
        if (ConfigContext.isLoggingOn()) {
            ensureTestNameContext();
            getLogger().logIfEnabled(getFqcn(), Level.ERROR, null, (Object) message, null);
            logToCurrentExecution(ERROR, message);
        }
    }

    // Log an exception with ERROR level message
    public static void logException(Exception e) {
        if (ConfigContext.isLoggingOn()) {
            ensureTestNameContext();
            getLogger().logIfEnabled(getFqcn(), Level.ERROR, null, (Object) "Exception occurred: ", e);
            logToCurrentExecution(INFO_RED, e.getMessage());
        }
    }

    public static String getCurrentExecutionLogs() {
        String testId = getCurrentTestIdentifier();
        if (testId != null) {
            List<String> logs = testLogsMap.get(testId);
            if (logs != null && !logs.isEmpty()) {
                synchronized (logs) {
                    return String.join("\n", logs);
                }
            }
        }
        return String.join("\n", threadLogs.get());
    }

    public static String getLogsForTest(String testId) {
        if (testId != null) {
            List<String> logs = testLogsMap.get(testId);
            if (logs != null && !logs.isEmpty()) {
                synchronized (logs) {
                    return String.join("\n", logs);
                }
            }
        }
        return "";
    }

    public static void clearLogsForTest(String testId) {
        if (testId != null) {
            testLogsMap.remove(testId);
        }
    }

    public static void clearCurrentExecutionLogs() {
        String testId = getCurrentTestIdentifier();
        if (testId != null) {
            testLogsMap.remove(testId);
        }
        String testName = ThreadContext.get("testName");
        if (testName != null) {
            testLogsMap.remove(testName);
        }
        threadLogs.get().clear();
    }

    private static void logToCurrentExecution(LogLevel level, String message) {
        if (ConfigContext.isLoggingOn()) {
            String clean = stripAnsi(message);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-h-m-ssa").format(new Date());
            String entry = "[" + LEVEL_STRING_MAP.getOrDefault(level, "EXCEPTION") + "] - [" + timestamp + "] - " + clean;

            threadLogs.get().add(entry);

            String testId = getCurrentTestIdentifier();
            if (testId != null && !testId.isBlank()) {
                testLogsMap.computeIfAbsent(testId, k -> Collections.synchronizedList(new ArrayList<>())).add(entry);
            }
            String testName = ThreadContext.get("testName");
            if (testName != null && !testName.isBlank() && !testName.equals(testId)) {
                testLogsMap.computeIfAbsent(testName, k -> Collections.synchronizedList(new ArrayList<>())).add(entry);
            }
        }
    }
}