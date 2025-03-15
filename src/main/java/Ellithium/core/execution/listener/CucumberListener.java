package Ellithium.core.execution.listener;

import Ellithium.config.managment.ConfigContext;
import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import io.cucumber.plugin.event.*;
import io.qameta.allure.Allure;
import io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm;
import java.io.File;
import static Ellithium.config.managment.GeneralHandler.testFailed;
import static Ellithium.core.reporting.internal.Colors.*;
public class CucumberListener extends AllureCucumber7Jvm {
    private static String ScenarioName;
    private static File failedScreenShot;
    private static Boolean paramAdded;
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::testStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class,this::stepFinishedHandler);
        publisher.registerHandlerFor(TestStepStarted.class,this::stepStartedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class,this::testFinishedHandler);
        super.setEventPublisher(publisher);
    }
    private void testStartedHandler(TestCaseStarted event) {
        ScenarioName = event.getTestCase().getName();
        Logger.info(CYAN + "[START] "  + BLUE + " Scenario " + ScenarioName + " [START]" + RESET);
        paramAdded=false;
    }
    private void testFinishedHandler(TestCaseFinished event){
        ScenarioName=event.getTestCase().getName();
        switch (event.getResult().getStatus()){
            case PASSED -> {
                Logger.info(GREEN  +"[PASSED] Scenario " + ScenarioName+ " [PASSED]" + RESET);
            }
            case FAILED -> {
                Logger.info(RED + "[FAILED] Scenario " + ScenarioName + " [FAILED]" + RESET);
            }
            case SKIPPED -> {
                Logger.info(YELLOW + "[SKIPPED] Scenario " + ScenarioName + " [SKIPPED]" + RESET);
            }
        }
        paramAdded=false;

    }
    private void stepFinishedHandler( TestStepFinished event) {
        var result=event.getResult().getStatus();
        if (event.getTestStep() instanceof PickleStepTestStep){
                if(DriverFactory.getCurrentDriver()!=null&&(result== Status.FAILED)){
                Reporter.setStepStatus(event.getTestStep().getId().toString(),io.qameta.allure.model.Status.FAILED);
                    failedScreenShot= testFailed(ConfigContext.getValue(ConfigContext.getDriverType()), ScenarioName);
                        Allure.getLifecycle().updateTestCase(stepResult -> {
                            if(failedScreenShot!=null ) {
                                String description=ConfigContext.getValue(ConfigContext.getDriverType()).toUpperCase() + "-" + ScenarioName + " FAILED";
                                Reporter.attachScreenshotToReport(failedScreenShot, failedScreenShot.getName(), description);
                                failedScreenShot=null;
                            }
                            GeneralHandler.addAttachments();
                            stepResult.setStatus(io.qameta.allure.model.Status.FAILED);
                        });
                    }
                    else {
                        Allure.getLifecycle().updateTestCase(stepResult -> {
                            GeneralHandler.addAttachments();
                            stepResult.setStatus(io.qameta.allure.model.Status.PASSED);
                        });
                    }
            }
        if(!paramAdded){
            Reporter.addParams(GeneralHandler.getParameters());
            paramAdded=true;
        }
    }
    private void stepStartedHandler(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep){
            GeneralHandler.clearTestLogFile();
        }
    }
}