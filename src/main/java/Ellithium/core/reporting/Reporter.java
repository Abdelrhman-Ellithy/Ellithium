package Ellithium.core.reporting;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.reporting.internal.Colors;
import Ellithium.core.logging.LogLevel;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.apache.xmlbeans.impl.xb.xsdschema.All;

import static Ellithium.core.logging.logsUtils.*;

public class Reporter {
    public static void log(String message, LogLevel logLevel, String additionalParameter){
        switch(logLevel){
            case INFO_BLUE:
               info(Colors.BLUE+message+ additionalParameter+ Colors.RESET);
               if (ConfigContext.isOnExecution()){
                   Allure.step(message + additionalParameter, Status.PASSED);
               }
                break;
            case INFO_GREEN:
                info(Colors.GREEN+message+ additionalParameter+ Colors.RESET);
                if (ConfigContext.isOnExecution()) {
                    Allure.step(message + additionalParameter, Status.PASSED);
                }
                break;
            case INFO_RED:
                info(Colors.RED+message+ additionalParameter+ Colors.RESET);
                if (ConfigContext.isOnExecution()) {
                    Allure.step(message + additionalParameter, Status.FAILED);
                }
                break;
            case INFO_YELLOW:
                info(Colors.YELLOW+message+ additionalParameter+ Colors.RESET);
                if (ConfigContext.isOnExecution()) {
                    Allure.step(message + additionalParameter, Status.SKIPPED);
                }
                break;
            case ERROR:
                error(Colors.RED+message+ additionalParameter+ Colors.RESET);
                if (ConfigContext.isOnExecution()) {
                    Allure.step(message + additionalParameter, Status.FAILED);
                }
                break;
            case TRACE:
                trace(Colors.BLUE+message+ additionalParameter+ Colors.RESET);
                if (ConfigContext.isOnExecution()) {
                    Allure.step(message + additionalParameter, Status.PASSED);
                }
                break;
            case WARN:
                warn(Colors.PINK+message+ additionalParameter+ Colors.RESET);
                if (ConfigContext.isOnExecution()) {
                    Allure.step(message + additionalParameter, Status.PASSED);
                }
                break;
            case DEBUG:
                debug(Colors.YELLOW+message+ additionalParameter+ Colors.RESET);
                if (ConfigContext.isOnExecution()) {
                    Allure.step(message + additionalParameter, Status.PASSED);
                }
            default: break;
        }
    }
    public static void log(String message, LogLevel logLevel){
            log(message,logLevel,"");
    }

}
