package Ellithium.core.reporting;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.logsUtils;
import Ellithium.core.reporting.internal.Colors;
import Ellithium.core.logging.LogLevel;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import org.apache.xmlbeans.impl.xb.xsdschema.All;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

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
    public static void logReportOnly(String message, LogLevel logLevel, String additionalParameter){
        if (ConfigContext.isOnExecution()) {
            switch (logLevel) {
                case INFO_BLUE, INFO_GREEN, TRACE, WARN, DEBUG:
                    Allure.step(message + additionalParameter, Status.PASSED);
                    break;
                case INFO_RED, ERROR:
                        Allure.step(message + additionalParameter, Status.FAILED);
                    break;
                case INFO_YELLOW:
                        Allure.step(message + additionalParameter, Status.SKIPPED);
                    break;
                default:
                    break;
            }
        }
    }
    public static void logReportOnly(String message, LogLevel logLevel){
        logReportOnly(message,logLevel,"");
    }
    public static void log(String message, LogLevel logLevel){
            log(message,logLevel,"");
    }

    public static void attachScreenshotToReport(File screenshot, String name, String browserName, String testName){
        try (FileInputStream fis = new FileInputStream(screenshot)) {
            Allure.description(browserName.toUpperCase() + "-" + testName + " FAILED");
            Allure.addAttachment(name, "image/png", fis, ".png");
        }catch (IOException e) {
            logsUtils.logException(e);
        }
    }
    public static void setTestCaseName(String Name) {
        Allure.getLifecycle().updateTestCase(testResult -> testResult.setName(Name));
    }
    public static void setTestCaseDescription(String description) {
        Allure.getLifecycle().updateTestCase(testResult -> testResult.setDescriptionHtml(description));
    }
    public static void setStepStatus(String uuid,Status status) {
        Allure.getLifecycle().updateStep(uuid, stepResult -> stepResult.setStatus(status));
    }
    public static void setStepName(String name) {
        Allure.getLifecycle().updateStep(stepResult -> stepResult.setName(name));
    }
    public static void setHookName(String name) {
        Allure.getLifecycle().updateFixture(fixtureResult -> fixtureResult.setName(name));
    }
    public static void addParams(List<Parameter>parameters) {
        Allure.getLifecycle().updateTestCase(testResult ->{
            testResult.setParameters(parameters);
        });
    }
}
