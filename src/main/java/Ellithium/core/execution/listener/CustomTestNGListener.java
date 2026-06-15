package Ellithium.core.execution.listener;
import Ellithium.core.ai.healing.EnsembleHealer;
import Ellithium.core.ai.healing.AISelfHealer;
import Ellithium.core.driver.DriverConfiguration;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.HeadlessMode;
import org.openqa.selenium.WebDriver;
import Ellithium.core.execution.Analyzer.RetryAnalyzer;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.recording.internal.VideoRecordingManager;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.reporting.internal.AllureHelper;
import Ellithium.core.ai.reporting.AIHealingReporter;
import Ellithium.core.ai.HealingTelemetryStore;
import Ellithium.core.execution.context.TestContext;
import Ellithium.core.execution.context.TestContextData;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.config.management.ConfigContext;
import Ellithium.config.management.GeneralHandler;
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
        IAnnotationTransformer, IExecutionListener, ISuiteListener, IInvokedMethodListener, ITestListener,
        IHookable {
    private long timeStartMills;
    private TestResultCollector testResultCollector;

    /**
     * Maps ITestResult to Recording ID
     * This allows us to correlate test results with their recordings
     */
    private static final Map<String, String> testResultToRecordingId = new ConcurrentHashMap<>();

    /**
     * Per-class driver snapshot captured after @BeforeClass completes.
     * Used to re-adopt the driver onto worker threads that TestNG dispatches via
     * dependsOnMethods — those threads never ran @BeforeClass and therefore have
     * null ThreadLocals in DriverFactory, even though the browser is running fine.
     */
    private final Map<String, WebDriver>           classDriverMap = new ConcurrentHashMap<>();
    private final Map<String, DriverConfiguration> classConfigMap = new ConcurrentHashMap<>();


    /**
     * Constructor initializes the test result collector.
     */
    public CustomTestNGListener() {
        this.testResultCollector = TestResultCollectorManager.getInstance().getTestResultCollector();
    }
    
    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        TestContextData data = new TestContextData(
                getTestIdentifier(testResult), testResult.getName(), resolveBrowser());
        ScopedValue.where(TestContext.CURRENT, data)
                .run(() -> callBack.runTestMethod(testResult));
    }

    private String resolveBrowser() {
        try {
            DriverConfiguration cfg = DriverFactory.getCurrentDriverConfiguration();
            if (cfg != null && cfg.getDriverType() != null) return cfg.getDriverType().getName();
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void onTestStart(ITestResult result) {
        // Attribute every heal on this thread to this test, so the false-heal detector (R9) can flag a
        // heal that was USED in a test that later FAILS. Safe for BDD + non-BDD (id needs no driver).
        HealingTelemetryStore.setCurrentTest(getTestIdentifier(result));
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
        // False-heal detector (R9): flag any heal USED in this now-failed test as a wrong-heal suspect.
        HealingTelemetryStore.markTestFailed(getTestIdentifier(result));
        HealingTelemetryStore.clearCurrentTest();
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.info(RED + "[FAILED] TESTCASE " + result.getName() + " [FAILED]" + RESET);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        HealingTelemetryStore.clearCurrentTest();
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.info(GREEN + "[PASSED] TESTCASE " +result.getName()+" [PASSED]" + RESET);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        HealingTelemetryStore.clearCurrentTest();
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
        try {
            testResultCollector.collectTestResults(context);
        } catch (Throwable t) {
            Logger.info("Notification result collection skipped: " + t.getClass().getSimpleName());
        }
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
        int active = ScreenRecorderActions.pendingCompilations();
        try {
            if (active > 0) {
            Logger.info("Background video compilation in progress (" + active + " videos)");
            Logger.info("Videos will finish compiling before process exits");
        } else {
            Logger.info("All videos compiled");
        }   
        } catch (Exception e) {
            Logger.logException(e);
        }
        finally {
            AISelfHealer.cleanup();
            EnsembleHealer.shutdown();
            AIHealingReporter.generateReport();
            AllureHelper.allureOpen();
            TestResultCollectorManager.getInstance().sendExecutionCompletionNotifications();
        }
    }
    
    /**
     * Restores the driver ThreadLocal on borrowed threads before any method body runs.
     *
     * TestNG's dependsOnMethods can dispatch a test method (or @BeforeMethod/@AfterMethod) to a
     * thread-pool thread that never executed @BeforeClass for that class. Because DriverFactory
     * stores the driver in ThreadLocals, those threads see null even though the browser is alive.
     * We capture the driver snapshot after @BeforeClass (in afterInvocation below) and re-adopt
     * it here so that all framework features — recording, screenshots, AI healing — work
     * transparently without any user boilerplate.
     */
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (DriverFactory.getCurrentDriver() == null) {
            String className = testResult.getTestClass().getRealClass().getName();
            WebDriver savedDriver = classDriverMap.get(className);
            DriverConfiguration savedConfig = classConfigMap.get(className);
            if (savedDriver != null && savedConfig != null) {
                DriverFactory.adoptCurrentThread(savedDriver, savedConfig);
            }
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        // Capture the driver right after @BeforeClass so we can restore it on worker threads.
        if (method.getTestMethod().isBeforeClassConfiguration()) {
            WebDriver current = DriverFactory.getCurrentDriver();
            DriverConfiguration config = DriverFactory.getCurrentDriverConfiguration();
            if (current != null) {
                String className = testResult.getTestClass().getRealClass().getName();
                classDriverMap.put(className, current);
                classConfigMap.put(className, config);
            }
        } else if (method.getTestMethod().isAfterClassConfiguration()) {
            // @AfterClass has quit the driver — remove the stale entry so recycled threads
            // from the pool don't accidentally adopt the dead browser.
            String className = testResult.getTestClass().getRealClass().getName();
            classDriverMap.remove(className);
            classConfigMap.remove(className);
        }

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
        Ellithium.core.driver.DriverConfiguration cfg = DriverFactory.getCurrentDriverConfiguration();
        name.append(cfg != null && cfg.getDriverType() != null ? cfg.getDriverType().getName().toUpperCase() : "UNKNOWN");
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