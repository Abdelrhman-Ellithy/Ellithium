package Ellithium.core.logging;

import Ellithium.Utilities.generators.TestDataGenerator;
import io.qameta.allure.model.Status;
import org.apache.logging.log4j.LogManager;

import java.text.SimpleDateFormat;
import java.util.*;

import static Ellithium.core.logging.LogLevel.*;
import static Ellithium.core.logging.LogLevel.DEBUG;

public class Logger {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(Logger.class);
    private static final List<String> logs= Collections.synchronizedList(new ArrayList<>());
    private static final Map<LogLevel, String> LEVEL_STRING_MAP = Map.ofEntries(
            Map.entry(INFO_BLUE, "INFO"),
            Map.entry(ERROR, "ERROR"),
            Map.entry(TRACE, "TRACE"),
            Map.entry(WARN, "WARN"),
            Map.entry(DEBUG, "DEBUG")
    );

    // Log a TRACE level message
    public static void trace(String message) {
        logger.trace(message);
        logToCurrentExecution(LogLevel.TRACE, message);
    }

    // Log a DEBUG level message
    public static void debug(String message) {
        logger.debug(message);
        logToCurrentExecution(LogLevel.DEBUG, message);
    }

    // Log an INFO level message
    public static void info(String message) {
        logger.info(message);
        logToCurrentExecution(LogLevel.INFO_BLUE, message);
    }

    // Log a WARN level message
    public static void warn(String message) {
        logger.warn(message);
        logToCurrentExecution(WARN, message);
    }

    // Log an ERROR level message
    public static void error(String message) {
        logger.error(message);
        logToCurrentExecution(ERROR, message);
    }

    // Log an exception with ERROR level message
    public static void logException(Exception e) {
        logger.error("Exception occurred: ", e);
        logToCurrentExecution(INFO_RED,e.getMessage());
    }
    public static String getCurrentExecutionLogs() {
        return String.join("\n", logs);
    }
    public static void clearCurrentExecutionLogs() {
        logs.clear();
    }
    private static void logToCurrentExecution(LogLevel level,String message){
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-h-m-ssa").format(new Date());
        logs.add("["+LEVEL_STRING_MAP.getOrDefault(level,"EXCEPTION")+"] - ["+timestamp+"] - " +message);
    }
}