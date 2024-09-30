package Ellithium.Internal;
import Ellithium.DriverSetup.DriverFactory;
import Ellithium.Utilities.PropertyHelper;
import Ellithium.Utilities.logsUtils;
import org.openqa.selenium.WebDriver;
import org.testng.*;

import java.io.IOException;

import static Ellithium.Utilities.Colors.*;
public class CustomTestNGListener implements IAlterSuiteListener, IAnnotationTransformer,
        IExecutionListener, ISuiteListener, IInvokedMethodListener, ITestListener {
    private long timeStartMills;
    private long timeFinishMills;
    @Override
    public void onTestStart(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
            String name = result.getName();
            WebDriver driver = DriverFactory.getCurrentDriver();
            if (driver != null) {
                String browserName = ConfigContext.getBrowserName().toUpperCase();
                logsUtils.info(BLUE + "[START] TESTCASE " + browserName + "-" + name + " [STARTED]" + RESET);
            } else {
                logsUtils.info(BLUE + "[START] TESTCASE " + name + " [STARTED]" + RESET);
            }
        }
    }
    @Override
    public void onTestFailure(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
            String name = result.getName();
            WebDriver driver = DriverFactory.getCurrentDriver();
            if (driver != null) {
                String browserName = ConfigContext.getBrowserName().toUpperCase();
                logsUtils.info(RED + "[FAILED] TESTCASE " + browserName + "-" + name + " [FAILED]" + RESET);
                ConfigContext.setLastScreenShot(GeneralHandler.testFailed(driver,browserName,name));
                ConfigContext.setLastUIFailed(true);
            } else {
                logsUtils.info(RED + "[FAILED] TESTCASE " + name + " [FAILED]" + RESET);
            }
        }
    }
    @Override
    public void onTestSuccess(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
            String name=result.getName();
            if(DriverFactory.getCurrentDriver()!=null){
                String browserName=ConfigContext.getBrowserName().toUpperCase();
                logsUtils.info(GREEN + "[PASSED] TESTCASE "+browserName+"-"+name+" [PASSED]" + RESET);
            }
            else{
                logsUtils.info(BLUE + "[PASSED] TESTCASE " +name+" [PASSED]" + RESET);
            }
        }
    }
    @Override
    public void onTestSkipped(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
            String name=result.getName();
            if(DriverFactory.getCurrentDriver()!=null){
                String browserName=ConfigContext.getBrowserName().toUpperCase();
                logsUtils.info(YELLOW + "[SKIPPED] TESTCASE " +browserName+"-"+name+" [SKIPPED]" + RESET);
            }
            else{
                logsUtils.info(YELLOW + "[SKIPPED] TESTCASE " +name+" [SKIPPED]" + RESET);
            }
        }
    }

    @Override
    public void onStart(ITestContext context) {
        logsUtils.info(CYAN + "[ALL TESTS STARTED]: " + context.getName().toUpperCase() + " [ALL TESTS STARTED]" + RESET);
    }

    @Override
    public void onFinish(ITestContext context) {
        logsUtils.info(PURPLE + "[ALL TESTS COMPLETED]: " + context.getName().toUpperCase()+ " [ALL TESTS COMPLETED]" + RESET);
    }
    @Override
    public void onExecutionStart() {
        GeneralHandler.solveVersion();
        timeStartMills = System.currentTimeMillis();
        logsUtils.info(BLUE + "----------------------------------------------" + RESET);
        logsUtils.info(CYAN + "------- Ellithium  Engine Setup  -------------" + RESET);
        logsUtils.info(BLUE + "----------------------------------------------" + RESET);
        AllureHelper.deleteAllureResultsDir();
    }

    @Override
    public void onExecutionFinish() {
        timeFinishMills = System.currentTimeMillis();
        logsUtils.info(BLUE + "-------------------------------------------" + RESET);
        logsUtils.info(CYAN + "------- Ellithium  Engine TearDown  -------" + RESET);
        logsUtils.info(BLUE + "-------------------------------------------" + RESET);
        long totalExecutionTime = (timeFinishMills - timeStartMills);
        long totalMills = totalExecutionTime % 1000;
        long totalSeconds = (totalExecutionTime / 1000) % 60;
        long totalMinutes = (totalExecutionTime / 60000) % 60;
        logsUtils.info(CYAN + "\nTotal Execution Time is: " + totalMinutes + " Min " + totalSeconds + " Sec " + totalMills + " Mills\n" + RESET);
        GeneralHandler.attachAndOpen();
    }
}