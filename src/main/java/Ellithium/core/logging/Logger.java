package Ellithium.core.logging;

import org.apache.logging.log4j.LogManager;

public class Logger {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(Logger.class);

    // Log a TRACE level message
    public static void trace(String message) {
        logger.trace(message);
    }

    // Log a DEBUG level message
    public static void debug(String message) {
        logger.debug(message);
    }

    // Log an INFO level message
    public static void info(String message) {
        logger.info(message);
    }

    // Log a WARN level message
    public static void warn(String message) {
        logger.warn(message);
    }

    // Log an ERROR level message
    public static void error(String message) {
        logger.error(message);
    }

    // Log a FATAL level message
    public static void fatal(String message) {
        logger.fatal(message);
    }

    // Log an exception with ERROR level message
    public static void logException(Exception e) {
        logger.error("Exception occurred: ", e);
    }
}