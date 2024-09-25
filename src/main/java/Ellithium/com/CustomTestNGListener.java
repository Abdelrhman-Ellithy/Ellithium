package Ellithium.com;

import Ellithium.Utilities.Colors;
import Ellithium.Utilities.PropertyHelper;
import Ellithium.Utilities.logsUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import org.testng.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import static Ellithium.Utilities.Colors.*;

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
        logsUtils.info(CYAN + "[ALL TESTS STARTED]: " + context.getName().toUpperCase() + " [ALL TESTS STARTED]" + RESET);
    }

    @Override
    public void onFinish(ITestContext context) {
        logsUtils.info(PURPLE + "[ALL TESTS COMPLETED]: " + context.getName().toUpperCase()+ " [ALL TESTS COMPLETED]" + RESET);
    }
    @Override
    public void onExecutionStart() {
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
        // Construct the log file path
        String logFilePath = PropertyHelper.getDataFromProperties(
                "src" + File.separator + "main" + File.separator + "resources" + File.separator +
                        "properties" + File.separator + "default" + File.separator + "log4j2",
                "property.basePath"
        );
        logFilePath = logFilePath.concat(File.separator).concat(
                PropertyHelper.getDataFromProperties(
                        "src" + File.separator + "main" + File.separator + "resources" + File.separator +
                                "properties" + File.separator + "default" + File.separator + "log4j2",
                        "property.fileName"
                )
        );

        // Check if the log file exists before attaching it
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            logsUtils.error("Log file not found at: " + logFilePath);
            Allure.step("Log file not found: " + logFilePath, Status.FAILED);
            return;
        }
        // Attach the log file to Allure report
        try (FileInputStream fis = new FileInputStream(logFile)) {
            // Generate a UUID for the custom test result
            String uuid = UUID.randomUUID().toString();

            // Create a custom TestResult for the whole test execution
            TestResult result = new TestResult()
                    .setUuid(uuid)  // Set the generated UUID
                    .setName("Full Execution Log")
                    .setStatus(Status.PASSED)
                    .setDescription("This is the execution log for the entire test run.");
            Allure.getLifecycle().scheduleTestCase(result);  // Start this "virtual" test case
            Allure.getLifecycle().startTestCase(uuid);
            // Attach the log file
            Allure.addAttachment("Execution Log File", "text/plain", fis, ".log");
            Allure.getLifecycle().stopTestCase(uuid);
            Allure.getLifecycle().writeTestCase(uuid);  // Write the test case in the Allure report
            logsUtils.info("Log file successfully attached to the Allure report.");
        } catch (IOException e) {
            logsUtils.logException(e);
            Allure.step("Failed to attach log file: " + e.getMessage(), Status.FAILED);
        }
        // Optionally, perform additional Allure-related actions after the execution
        AllureHelper.allureOpen();
    }
}