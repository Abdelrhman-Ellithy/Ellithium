package AutoEllithiumSphere.com;

import AutoEllithiumSphere.Utilities.logsUtils;
import org.testng.*;
import static AutoEllithiumSphere.Utilities.Colors.*;

public class CustomTestNGListener implements IAlterSuiteListener, IAnnotationTransformer,
        IExecutionListener, ISuiteListener, IInvokedMethodListener, ITestListener {
    private long timeStartMills;
    private long timeFinishMills;

    @Override
    public void onTestStart(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
            logsUtils.info(BLUE + "[START] Test " + result.getName() + " [STARTED]" + RESET);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
            logsUtils.info(GREEN + "[PASSED] Test " + result.getName() + " [PASSED]" + RESET);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
            logsUtils.info(RED + "[FAILED] Test " + result.getName() + " [FAILED]" + RESET);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (!(result.getName().equals("runScenario"))) {
            logsUtils.info(YELLOW + "[SKIPPED] Test " + result.getName() + " [SKIPPED]" + RESET);
        }
    }

    @Override
    public void onStart(ITestContext context) {
        logsUtils.info(CYAN + "[START] " + context.getName() + " [TESTS STARTED]" + RESET);
    }

    @Override
    public void onFinish(ITestContext context) {
        logsUtils.info(PURPLE + "[ALL TESTS COMPLETED]: " + context.getName() + RESET);
    }

    @Override
    public void onExecutionStart() {
        timeStartMills = System.currentTimeMillis();
        logsUtils.info(BLUE + "--------------------------------------------------------" + RESET);
        logsUtils.info(CYAN + "------- AutoEllithiumSphere  Engine Setup  -------------" + RESET);
        logsUtils.info(BLUE + "--------------------------------------------------------" + RESET);
    }

    @Override
    public void onExecutionFinish() {
        timeFinishMills = System.currentTimeMillis();
        logsUtils.info(BLUE + "-----------------------------------------------------" + RESET);
        logsUtils.info(CYAN + "------- AutoEllithiumSphere  Engine TearDown  -------" + RESET);
        logsUtils.info(BLUE + "-----------------------------------------------------" + RESET);
        long totalMills, totalSeconds, totalMinutes, totalExecutionTime;
        totalExecutionTime = (timeFinishMills - timeStartMills);
        totalMills = totalExecutionTime % 1000;
        totalSeconds = (totalExecutionTime / 1000) % 60;
        totalMinutes = (totalExecutionTime / 60000) % 60;
        logsUtils.info(CYAN + "\n\nTotal Execution Time is: " + totalMinutes + " Min " + totalSeconds + " Sec " + totalMills + " Mills\n\n" + RESET);
        AllureHelper.allureOpen();
    }
}