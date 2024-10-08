package Ellithium.core.execution.listener;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.logging.logsUtils;
import Ellithium.core.reporting.Reporter;
import io.cucumber.plugin.event.*;
import io.qameta.allure.Allure;
import io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm;
import io.qameta.allure.model.Parameter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static Ellithium.config.managment.GeneralHandler.testFailed;
import static Ellithium.core.reporting.internal.Colors.*;
public class CucumberListener extends AllureCucumber7Jvm {
    private static String ScenarioName;
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::testStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class,this::stepFinishedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class,this::testFinishedHandler);
        super.setEventPublisher(publisher);
    }
    private void testStartedHandler(TestCaseStarted event) {
        ScenarioName = event.getTestCase().getName();
        logsUtils.info(CYAN + "[START] "  + BLUE + " Scenario " + ScenarioName + " [START]" + RESET);
    }
    private void testFinishedHandler(TestCaseFinished event){
        boolean flagFailed=false;
        ScenarioName=event.getTestCase().getName();
        String uuid=UUID.randomUUID().toString();
        File screenShot=null;
        switch (event.getResult().getStatus()){
            case PASSED -> {
                logsUtils.info(GREEN  +"[PASSED] Scenario " + ScenarioName+ " [PASSED]" + RESET);
            }
            case FAILED -> {
                flagFailed=true;
                screenShot = testFailed(ConfigContext.getBrowserName(), ScenarioName);
                logsUtils.info(RED + "[FAILED] Scenario " + ScenarioName + " [FAILED]" + RESET);
            }
            case SKIPPED -> {
                logsUtils.info(YELLOW + "[SKIPPED] Scenario " + ScenarioName + " [SKIPPED]" + RESET);
            }
        }
        String closeFlag= PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "closeDriverAfterBDDScenario");
        if(closeFlag.equalsIgnoreCase("true")){
            uuid=UUID.randomUUID().toString();
            Allure.getLifecycle().startStep(uuid, new io.qameta.allure.model.StepResult().setName("Automatic Driver Quit "));
            Allure.getLifecycle().updateStep(uuid, stepResult -> {
                stepResult.setStatus(io.qameta.allure.model.Status.PASSED);
                if(closeFlag.equalsIgnoreCase("true")){
                    DriverFactory.quitDriver();
                }else {
                    DriverFactory.removeDriver();
                }
            });
            Allure.getLifecycle().stopStep(uuid);
        }
        else {
            DriverFactory.removeDriver();
        }
        uuid=UUID.randomUUID().toString();
        Allure.getLifecycle().startStep(uuid, new io.qameta.allure.model.StepResult().setName("Attachments"));
        if(flagFailed && screenShot!=null ) {
            File finalScreenShot = screenShot;
            Allure.getLifecycle().updateStep(uuid, stepResult -> {
                Reporter.attachScreenshotToReport(finalScreenShot, finalScreenShot.getName(), ConfigContext.getBrowserName(), ScenarioName);
                GeneralHandler.AttachLogs();
                stepResult.setStatus(io.qameta.allure.model.Status.PASSED);
            });
        }
        else {
            Allure.getLifecycle().updateStep(uuid, stepResult -> {
                GeneralHandler.AttachLogs();
                stepResult.setStatus(io.qameta.allure.model.Status.PASSED);
            });
        }
        Allure.getLifecycle().stopStep(uuid);
        Reporter.addParams(GeneralHandler.getParameters());
    }
    private void stepFinishedHandler( TestStepFinished event) {
        var result=event.getResult().getStatus();
        if (event.getTestStep() instanceof PickleStepTestStep){
            if (result== Status.FAILED){
                Reporter.setStepStatus(event.getTestStep().getId().toString(),io.qameta.allure.model.Status.FAILED);
            }
        }
    }
}