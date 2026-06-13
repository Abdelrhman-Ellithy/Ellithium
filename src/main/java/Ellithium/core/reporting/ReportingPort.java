package Ellithium.core.reporting;

import Ellithium.core.logging.LogLevel;

public interface ReportingPort {
    void log(String message, LogLevel level);
    void log(String message, LogLevel level, String additionalParameter);
    void logReportOnly(String message, LogLevel level);
}
