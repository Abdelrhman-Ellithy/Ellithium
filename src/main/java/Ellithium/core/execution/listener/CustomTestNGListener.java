package Ellithium.core.execution.listener;
import Ellithium.core.driver.DriverConfiguration;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.execution.Analyzer.RetryAnalyzer;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.recording.internal.VideoRecordingManager;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.reporting.internal.AllureHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.reporting.notification.TestResultCollector;
import Ellithium.core.reporting.notification.TestResultCollectorManager;
import Ellithium.core.logging.Logger;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.*;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Listeners;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static Ellithium.core.reporting.internal.Colors.*;
import static org.testng.ITestResult.*;

@Listeners({AllureTestNg.class})
public class CustomTestNGListener extends TestListenerAdapter implements IAlterSuiteListener,
        IAnnotationTransformer, IExecutionListener, ISuiteListener, IInvokedMethodListener, ITestListener {
    private long timeStartMills;
    private TestResultCollector testResultCollector;

    /**
     * Maps ITestResult to Recording ID
     * This allows us to correlate test results with their recordings
     */
    private static final Map<String, String> testResultToRecordingId = new ConcurrentHashMap<>();


    /**
     * Constructor initializes the test result collector.
     */
    public CustomTestNGListener() {
        this.testResultCollector = TestResultCollectorManager.getInstance().getTestResultCollector();
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.clearCurrentExecutionLogs();
            Logger.info(BLUE + "[START] TESTCASE " + result.getName() + " [STARTED]" + RESET);
            boolean driverExecution=(DriverFactory.getCurrentDriver() != null);
            DriverConfiguration driverConfiguration=DriverFactory.getCurrentDriverConfiguration();
            boolean notHeadless= (driverConfiguration != (null)) && (driverConfiguration.getHeadlessMode() == HeadlessMode.False);
            boolean isNotMobileCloud= (driverConfiguration != (null)) && (!driverConfiguration.isMobileCloud());
            boolean shouldRecord=driverExecution && notHeadless&&isNotMobileCloud;
            if (shouldRecord) {
                try {
                    String testName = getTestName(result);
                    String testIdentifier = getTestIdentifier(result);
                    String recordingId = VideoRecordingManager.startRecording(testName, testIdentifier);
                    if (recordingId != null) {
                        String resultKey = getResultKey(result);
                        testResultToRecordingId.put(resultKey, recordingId);
                        Logger.info(CYAN + "Video Recording started for Test Case: " + testName + RESET);
                    }
                } catch (Exception e) {
                    Logger.info(RED + "Failed to start video recording: " + e.getMessage() + RESET);
                    Logger.logException(e);
                }
            }
            else {
                if (!driverExecution) {
                    Logger.debug("Video recording skipped: No active driver");
                }
                else if(!isNotMobileCloud){
                    Logger.debug("Video recording skipped: Recording not available when testing mobile on cloud, it's handled by provider");
                }
                else {
                    Logger.debug("Video recording skipped: Headless mode enabled");
                }
            }
        }
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.info(RED + "[FAILED] TESTCASE " + result.getName() + " [FAILED]" + RESET);
        }
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.info(GREEN + "[PASSED] TESTCASE " +result.getName()+" [PASSED]" + RESET);
        }
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        if (!testResultCollector.isCucumberTest(result)) {
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
        testResultCollector.collectTestResults(context);
        testResultToRecordingId.clear();
    }
    
    @Override
    public void onStart(ISuite suite) {
        Logger.info(PINK + "[SUITE STARTED]: " + suite.getName().toUpperCase() + " [SUITE STARTED]" + RESET);
    }
    
    @Override
    public void onFinish(ISuite suite) {
        Logger.info(PINK + "[SUITE FINISHED]: " + suite.getName().toUpperCase()+ " [SUITE FINISHED]" + RESET);
        VideoRecordingManager.forceCleanupAll();
    }
    
    @Override
    public void onExecutionStart() {
        GeneralHandler.StartRoutine();
        Logger.info(BLUE + "---------------------------------------------" + RESET);
        Logger.info(CYAN + "------- Ellithium Engine Setup --------------" + RESET);
        Logger.info(BLUE + "---------------------------------------------" + RESET);
        if (VideoRecordingManager.isRecordingEnabled()) {
            Logger.info(GREEN + "Video Recording: ENABLED" + RESET);
            Logger.info(CYAN + "Video Attachment: " + (VideoRecordingManager.isAttachmentEnabled() ? "ENABLED" : "DISABLED") + RESET);
        } else {
            Logger.info(YELLOW + "Video Recording: DISABLED" + RESET);
        }
        timeStartMills = System.currentTimeMillis();
        ConfigContext.setOnExecution(true);
        testResultCollector.initializeTestResultCollection();
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
        TestResultCollectorManager.getInstance().sendExecutionCompletionNotifications();
    }
    
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()){
            boolean driverExecution=(DriverFactory.getCurrentDriver() != null);
            DriverConfiguration currentDriverConfiguration=DriverFactory.getCurrentDriverConfiguration();
            if ((testResult.getStatus() == FAILURE) && driverExecution) {
                Reporter.setStepStatus(method.getTestMethod().getMethodName(), io.qameta.allure.model.Status.FAILED);
                File failedScreenShot = GeneralHandler.testFailed(currentDriverConfiguration.getDriverType().getName(), method.getTestMethod().getMethodName());
                if (failedScreenShot != null) {
                    String description = currentDriverConfiguration.getDriverType().getName().toUpperCase() + "-" + method.getTestMethod().getMethodName() + " FAILED";
                    Reporter.attachScreenshotToReport(failedScreenShot, failedScreenShot.getName(), description);
                }
            }
            boolean notHeadless= (currentDriverConfiguration != (null)) && (currentDriverConfiguration.getHeadlessMode() == HeadlessMode.False);
            boolean isNotMobileCloud= (currentDriverConfiguration != (null)) && (!currentDriverConfiguration.isMobileCloud());
            boolean shouldRecord=driverExecution && notHeadless&&isNotMobileCloud;
            Reporter.addParams(GeneralHandler.getParameters());
            if (shouldRecord){
                stopRecordingForTest(testResult, getStatus(testResult.getStatus()));
            }
            GeneralHandler.addAttachments();
        }
    }
    
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }

    /**
     * Stops recording for a specific test result.
     * Uses the recording ID that was stored when the test started.
     * @param result TestNG test result
     * @param status Test status (PASSED, FAILED, SKIPPED)
     */
    private void stopRecordingForTest(ITestResult result, String status) {
        try {
            String resultKey = getResultKey(result);
            String recordingId = testResultToRecordingId.get(resultKey);
            if (recordingId != null) {
                String videoPath = VideoRecordingManager.stopRecordingById(recordingId, status);
                if (videoPath != null) {
                    Logger.info(GREEN + "Video recording stopped successfully for: " +result.getName() + RESET);
                }
                testResultToRecordingId.remove(resultKey);
            } else {
                Logger.info(YELLOW + "No recording ID found for test, trying thread-based stop" + RESET);
                VideoRecordingManager.stopRecordingForCurrentThread(status);
            }
        } catch (Exception e) {
            Logger.warn(RED + "Failed to stop video recording: " + e.getMessage() + RESET);
            Logger.logException(e);
        }
    }

    /**
     * Gets a user-friendly test name for display.
     * @param result TestNG test result
     * @return Test name
     */
    private String getTestName(ITestResult result) {
        StringBuilder name = new StringBuilder(result.getName());
        name.append("_");
        name.append(DriverFactory.getCurrentDriverConfiguration().getDriverType().getName().toUpperCase());
        name.append("_");
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            name.append("_params");
            for (int i = 0; i < Math.min(parameters.length, 3); i++) {
                name.append("_").append(sanitizeParam(parameters[i]));
            }
        }
        return name.toString();
    }
    private String getStatus(int status){
        switch (status){
            case FAILURE -> {return "FAILED";}
            case SKIP -> {return "SKIPPED";}
            default -> {return "PASSED";}
        }
    }

    /**
     * Generates a unique test identifier that's consistent across retries.
     * This is used as the key to track recordings for specific tests.
     * @param result TestNG test result
     * @return Unique test identifier
     */
    private String getTestIdentifier(ITestResult result) {
        StringBuilder identifier = new StringBuilder();
        if (result.getTestClass() != null) {
            identifier.append(result.getTestClass().getName()).append(".");
        }
        identifier.append(result.getName());
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            identifier.append("_").append(Math.abs(java.util.Arrays.deepHashCode(parameters)));
        }
        return identifier.toString();
    }

    /**
     * Generates a unique key for an ITestResult instance.
     * This is used to map test results to their recording IDs.
     * @param result TestNG test result
     * @return Unique result key
     */
    private String getResultKey(ITestResult result) {
        return getTestIdentifier(result) + "_" + System.identityHashCode(result);
    }

    /**
     * Sanitizes parameter value for use in file names.
     * @param param Parameter object
     * @return Sanitized string representation
     */
    private String sanitizeParam(Object param) {
        if (param == null) {
            return "null";
        }
        String value = param.toString();
        if (value.length() > 20) {
            value = value.substring(0, 20);
        }
        return value.replaceAll("[^a-zA-Z0-9]", "_");
    }
}