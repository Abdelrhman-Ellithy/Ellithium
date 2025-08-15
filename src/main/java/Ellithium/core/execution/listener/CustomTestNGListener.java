package Ellithium.core.execution.listener;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.execution.Analyzer.RetryAnalyzer;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.reporting.internal.AllureHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.reporting.notification.NotificationIntegrationHandler;
import Ellithium.core.logging.Logger;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.*;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Listeners;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

import static Ellithium.core.reporting.internal.Colors.*;
import static org.testng.ITestResult.FAILURE;
@Listeners({AllureTestNg.class})
public class CustomTestNGListener extends TestListenerAdapter implements IAlterSuiteListener,
        IAnnotationTransformer, IExecutionListener, ISuiteListener, IInvokedMethodListener, ITestListener {
    private long timeStartMills;
    private NotificationIntegrationHandler notificationHandler;
    
    /**
     * Constructor initializes the notification handler.
     */
    public CustomTestNGListener() {
        this.notificationHandler = new NotificationIntegrationHandler();
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
                Logger.clearCurrentExecutionLogs();
                Logger.info(BLUE + "[START] TESTCASE " + result.getName() + " [STARTED]" + RESET);
        }
    }
    @Override
    public void onTestFailure(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
                Logger.info(RED + "[FAILED] TESTCASE " + result.getName() + " [FAILED]" + RESET);
        }
    }
    @Override
    public void onTestSuccess(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
                Logger.info(GREEN + "[PASSED] TESTCASE " +result.getName()+" [PASSED]" + RESET);
            }
    }
    @Override
    public void onTestSkipped(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
                Logger.info(YELLOW + "[SKIPPED] TESTCASE " +result.getName()+" [SKIPPED]" + RESET);
        }
    }
    @Override
    public void onStart(ITestContext context) {
        Logger.info(PURPLE + "[ALL TESTS STARTED]: " + context.getName().toUpperCase() + " [ALL TESTS STARTED]" + RESET);
    }
    @Override
    public void onFinish(ITestContext context) {
        Logger.info(PURPLE + "[ALL TESTS COMPLETED]: " + context.getName().toUpperCase()+ " [ALL TESTS COMPLETED]" + RESET);
        
        // Collect test results for overall execution summary
        notificationHandler.collectTestResults(context);
    }
    @Override
    public void onStart(ISuite suite) {
        Logger.info(PINK + "[SUITE STARTED]: " + suite.getName().toUpperCase() + " [SUITE STARTED]" + RESET);
    }
    @Override
    public void onFinish(ISuite suite) {
        Logger.info(PINK + "[SUITE FINISHED]: " + suite.getName().toUpperCase()+ " [SUITE FINISHED]" + RESET);
    }
    @Override
    public void onExecutionStart() {
        GeneralHandler.StartRoutine();
        Logger.info(BLUE + "---------------------------------------------" + RESET);
        Logger.info(CYAN + "------- Ellithium Engine Setup --------------" + RESET);
        Logger.info(BLUE + "---------------------------------------------" + RESET);
        AllureHelper.deleteAllureResultsDir();
        timeStartMills = System.currentTimeMillis();
        ConfigContext.setOnExecution(true);
        
        // Initialize notification integration system
        notificationHandler.initializeNotificationSystem();
    }
    @Override
    public void onExecutionFinish() {
        ConfigContext.setOnExecution(false);
        long timeFinishMills = System.currentTimeMillis();
        long totalExecutionTime = (timeFinishMills - timeStartMills);
        long totalMills = totalExecutionTime % 1000;
        long totalSeconds = (totalExecutionTime / 1000) % 60;
        long totalMinutes = (totalExecutionTime / 60000) % 60;
        Reporter.log( "Total Execution Time is: " + totalMinutes + " Min " , LogLevel.INFO_BLUE, totalSeconds + " Sec " + totalMills + " Mills" );
        Logger.info(BLUE + "------------------------------------------" + RESET);
        Logger.info(CYAN + "------- Ellithium Engine TearDown --------" + RESET);
        Logger.info(BLUE + "------------------------------------------" + RESET);
        AllureHelper.allureOpen();
        notificationHandler.sendExecutionCompletionNotifications();
    }
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if(method.isTestMethod() && (!testResult.getName().equals("runScenario")) && (DriverFactory.getCurrentDriver()!=null)){
            if(testResult.getStatus()==FAILURE){
            String driverName=ConfigContext.getValue(ConfigContext.getDriverType());
            File screenShot=GeneralHandler.testFailed(driverName,testResult.getName());
                String description=driverName.toUpperCase() + "-" + driverName +" "+ testResult.getName();
                Reporter.attachScreenshotToReport(screenShot, Objects.requireNonNull(screenShot).getName(), description);
            }
        }
        if (method.isTestMethod()){
            GeneralHandler.addAttachments();
            Reporter.addParams(GeneralHandler.getParameters());
        }
    }
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}