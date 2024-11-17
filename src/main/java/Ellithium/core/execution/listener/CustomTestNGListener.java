package Ellithium.core.execution.listener;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.execution.Analyzer.RetryAnalyzer;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.reporting.internal.AllureHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.logging.logsUtils;
import org.testng.*;
import org.testng.annotations.ITestAnnotation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static Ellithium.core.reporting.internal.Colors.*;
import static org.testng.ITestResult.FAILURE;

public class CustomTestNGListener extends TestListenerAdapter implements IAlterSuiteListener,
        IAnnotationTransformer, IExecutionListener, ISuiteListener, IInvokedMethodListener, ITestListener {
    private long timeStartMills;
    @Override
    public void onTestStart(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
                logsUtils.info(BLUE + "[START] TESTCASE " + result.getName() + " [STARTED]" + RESET);
        }
    }
    @Override
    public void onTestFailure(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
                logsUtils.info(RED + "[FAILED] TESTCASE " + result.getName() + " [FAILED]" + RESET);
        }
    }
    @Override
    public void onTestSuccess(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
                logsUtils.info(BLUE + "[PASSED] TESTCASE " +result.getName()+" [PASSED]" + RESET);
            }
    }
    @Override
    public void onTestSkipped(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
                logsUtils.info(YELLOW + "[SKIPPED] TESTCASE " +result.getName()+" [SKIPPED]" + RESET);
        }
    }
    @Override
    public void onStart(ITestContext context) {
        logsUtils.info(PURPLE + "[ALL TESTS STARTED]: " + context.getName().toUpperCase() + " [ALL TESTS STARTED]" + RESET);
    }
    @Override
    public void onFinish(ITestContext context) {
        logsUtils.info(PURPLE + "[ALL TESTS COMPLETED]: " + context.getName().toUpperCase()+ " [ALL TESTS COMPLETED]" + RESET);
    }
    @Override
    public void onStart(ISuite suite) {
        logsUtils.info(PINK + "[SUITE STARTED]: " + suite.getName().toUpperCase() + " [SUITE STARTED]" + RESET);
    }
    @Override
    public void onFinish(ISuite suite) {
        logsUtils.info(PINK + "[SUITE FINISHED]: " + suite.getName().toUpperCase()+ " [SUITE FINISHED]" + RESET);
    }
    @Override
    public void onExecutionStart() {
        GeneralHandler.StartRoutine();
        logsUtils.info(BLUE + "---------------------------------------------" + RESET);
        logsUtils.info(CYAN + "------- Ellithium  Engine Setup -------------" + RESET);
        logsUtils.info(BLUE + "---------------------------------------------" + RESET);
        AllureHelper.deleteAllureResultsDir();
        timeStartMills = System.currentTimeMillis();
        ConfigContext.setOnExecution(true);
    }
    @Override
    public void onExecutionFinish() {
        long timeFinishMills;
        ConfigContext.setOnExecution(false);
        logsUtils.info(BLUE + "------------------------------------------" + RESET);
        logsUtils.info(CYAN + "------- Ellithium  Engine TearDown -------" + RESET);
        logsUtils.info(BLUE + "------------------------------------------" + RESET);
        timeFinishMills = System.currentTimeMillis();
        long totalExecutionTime = (timeFinishMills - timeStartMills);
        long totalMills = totalExecutionTime % 1000;
        long totalSeconds = (totalExecutionTime / 1000) % 60;
        long totalMinutes = (totalExecutionTime / 60000) % 60;
        logsUtils.info(CYAN + "Total Execution Time is: " + totalMinutes + " Min " + totalSeconds + " Sec " + totalMills + " Mills" + RESET);
        AllureHelper.allureOpen();
    }
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if(testResult.getStatus()==FAILURE){
        if(method.isTestMethod() && (!testResult.getName().equals("runScenario")) && (DriverFactory.getCurrentDriver()!=null)){
            String driverName=ConfigContext.getValue(ConfigContext.getDriverType());
            File screenShot=GeneralHandler.testFailed(driverName,testResult.getName());
                Reporter.attachScreenshotToReport(screenShot, screenShot.getName(),
                        driverName,testResult.getName());
            }
        }
    }
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}