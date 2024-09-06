package AutoEllithiumSphere.Utilities;

import org.apache.logging.log4j.LogManager;

public class logsUtils {
    // Path for storing log files
    public static String LOGS_PATH = "Test-Output/Logs";

    // Log a TRACE level message
    public static void trace(String message) {
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString())
                .trace(message);
    }
    // Log a DEBUG level message
    public static void debug(String message) {
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString())
                .debug(message);
    }
    // Log an INFO level message
    public static void info(String message) {
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString())
                .info(message);
    }
    // Log a WARN level message
    public static void warn(String message) {
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString())
                .warn(message);
    }
    // Log an ERROR level message
    public static void error(String message) {
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString())
                .error(message);
    }
    // Log a FATAL level message
    public static void fatal(String message) {
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString())
                .fatal(message);
    }
    // Log an exception with ERROR level message
    public static void logException(Exception e) {
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString())
                .error("Exception occurred: ", e);
    }

}