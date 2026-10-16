package Ellithium.core.execution.listener;

import Ellithium.core.ai.healing.EnsembleHealer;
import Ellithium.core.ai.healing.AISelfHealer;
import Ellithium.core.driver.DriverConfiguration;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.driver.PrivateMode;
import Ellithium.core.execution.fixtures.WithDriver;
import Ellithium.core.execution.fixtures.DriverFixtureProvider;
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
import Ellithium.core.execution.events.TestLifecycleEventBus;
import io.qameta.allure.testng.AllureTestNg;
import org.apache.logging.log4j.ThreadContext;
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

@Listeners({ AllureTestNg.class })
public class CustomTestNGListener extends TestListenerAdapter implements IAlterSuiteListener,
        IAnnotationTransformer, IExecutionListener, ISuiteListener, IInvokedMethodListener, ITestListener,
        IHookable, IMethodInterceptor {
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
    private final Map<String, WebDriver> classDriverMap = new ConcurrentHashMap<>();
    private final Map<String, DriverConfiguration> classConfigMap = new ConcurrentHashMap<>();

    /**
     * Constructor initializes the test result collector.
     */
    public CustomTestNGListener() {
        this.testResultCollector = TestResultCollectorManager.getInstance().getTestResultCollector();
    }

    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        Reporter.flushPendingStep();
        TestContextData data = new TestContextData(
                getTestIdentifier(testResult), testResult.getName(), resolveBrowser());
        ScopedValue.where(TestContext.CURRENT, data)
                .run(() -> {
                    try {
                        callBack.runTestMethod(testResult);
                    } finally {
                        Reporter.flushPendingStep();
                    }
                });
    }

    private String resolveBrowser() {
        try {
            DriverConfiguration cfg = DriverFactory.getCurrentDriverConfiguration();
            if (cfg != null && cfg.getDriverType() != null)
                return cfg.getDriverType().getName();
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void onTestStart(ITestResult result) {
        Reporter.flushPendingStep();
        // Attribute every heal on this thread to this test, so the false-heal detector
        // (R9) can flag a
        // heal that was USED in a test that later FAILS. Safe for BDD + non-BDD (id
        // needs no driver).
        String testIdentifier = getTestIdentifier(result);
        String testName = result.getName();
        HealingTelemetryStore.setCurrentTest(testIdentifier);
        ThreadContext.put("testIdentifier", testIdentifier);
        ThreadContext.put("testName", testName);
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.clearLogsForTest(testIdentifier);
            Logger.info(BLUE + "[START] TESTCASE " + testName + " [STARTED]" + RESET);
            String recordingId = VideoRecordingManager.prepareRecording(getTestName(result), testIdentifier);
            if (recordingId != null) {
                testResultToRecordingId.put(getResultKey(result), recordingId);
            }
        }
        Ellithium.core.spi.PluginRegistry.getInstance().dispatchTestStart(testIdentifier, testName);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Reporter.flushPendingStep();
        // False-heal detector (R9): flag any heal USED in this now-failed test as a
        // wrong-heal suspect.
        String testIdentifier = getTestIdentifier(result);
        int suspectHeals = HealingTelemetryStore.markTestFailed(testIdentifier);
        HealingTelemetryStore.clearCurrentTest();
        if (suspectHeals > 0) {
            String details = HealingTelemetryStore.getSuspectWrongHealDetails(testIdentifier);
            Logger.warn(RED + "[FALSE-HEAL DETECTED] Test " + result.getName() + " failed after using "
                    + suspectHeals + " AI heal(s): " + details + RESET);
            if (Ellithium.core.ai.config.AIConfigLoader.isFailOnSuspectHeal()) {
                Throwable original = result.getThrowable();
                String msg = "[AI FALSE-HEAL] Test failed after using " + suspectHeals + " healed locator(s). Suspect heals: " + details;
                AssertionError falseHealError = new AssertionError(msg, original);
                result.setThrowable(falseHealError);
            }
        }
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.info(RED + "[FAILED] TESTCASE " + result.getName() + " [FAILED]" + RESET);
        }
        ThreadContext.put("testName", "Ellithium");
        ThreadContext.remove("testIdentifier");
        long durationFailed = Math.max(0, result.getEndMillis() - result.getStartMillis());
        TestLifecycleEventBus.getInstance().publishTestFailed(testIdentifier, result.getName(), result.getThrowable(), durationFailed);
        Ellithium.core.spi.PluginRegistry.getInstance().dispatchTestFailure(
                testIdentifier, result.getName(), result.getThrowable(),
                durationFailed);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        Reporter.flushPendingStep();
        HealingTelemetryStore.clearCurrentTest();
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.info(GREEN + "[PASSED] TESTCASE " + result.getName() + " [PASSED]" + RESET);
        }
        ThreadContext.put("testName", "Ellithium");
        ThreadContext.remove("testIdentifier");
        long durationPassed = Math.max(0, result.getEndMillis() - result.getStartMillis());
        TestLifecycleEventBus.getInstance().publishTestPassed(getTestIdentifier(result), result.getName(), durationPassed);
        Ellithium.core.spi.PluginRegistry.getInstance().dispatchTestSuccess(
                getTestIdentifier(result), result.getName(),
                durationPassed);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Reporter.flushPendingStep();
        HealingTelemetryStore.clearCurrentTest();
        if (!testResultCollector.isCucumberTest(result)) {
            Logger.info(YELLOW + "[SKIPPED] TESTCASE " + result.getName() + " [SKIPPED]" + RESET);
        }
        ThreadContext.put("testName", "Ellithium");
        ThreadContext.remove("testIdentifier");
        TestLifecycleEventBus.getInstance().publishTestSkipped(getTestIdentifier(result), result.getName(), result.getThrowable());
        Ellithium.core.spi.PluginRegistry.getInstance().dispatchTestSkipped(
                getTestIdentifier(result), result.getName(), result.getThrowable());
    }

    @Override
    public void onStart(ITestContext context) {
        Logger.info(
                PURPLE + "[ALL TESTS STARTED]: " + context.getName().toUpperCase() + " [ALL TESTS STARTED]" + RESET);
        if (context.getAllTestMethods() != null && context.getAllTestMethods().length > 0) {
            Ellithium.core.execution.progressbar.SmartTestProgressBar.getInstance().setExpectedTotalTests(context.getAllTestMethods().length);
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        Logger.info(PURPLE + "[ALL TESTS COMPLETED]: " + context.getName().toUpperCase() + " [ALL TESTS COMPLETED]"
                + RESET);
        try {
            testResultCollector.collectTestResults(context);
        } catch (Throwable t) {
            Logger.debug("Notification result collection skipped: " + t.getClass().getSimpleName());
        }
        testResultToRecordingId.clear();
    }

    @Override
    public void onStart(ISuite suite) {
        String existing = System.getProperty("ellithium.suite.name");
        if (existing == null || existing.isBlank() || "UnknownSuite".equalsIgnoreCase(existing)) {
            System.setProperty("ellithium.suite.name", suite.getName());
        }
        Logger.info(PINK + "[SUITE STARTED]: " + suite.getName().toUpperCase() + " [SUITE STARTED]" + RESET);
        int totalMethods = (suite.getAllMethods() != null) ? suite.getAllMethods().size() : 0;
        TestLifecycleEventBus.getInstance().publishSuiteStarted(suite.getName(), totalMethods);
    }

    @Override
    public void onFinish(ISuite suite) {
        Logger.info(PINK + "[SUITE FINISHED]: " + suite.getName().toUpperCase() + " [SUITE FINISHED]" + RESET);
        TestLifecycleEventBus.getInstance().publishSuiteFinished(suite.getName());
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
            Logger.info(CYAN + "Video Attachment: "
                    + (VideoRecordingManager.isAttachmentEnabled() ? "ENABLED" : "DISABLED") + RESET);
        } else {
            Logger.info(YELLOW + "Video Recording: DISABLED" + RESET);
        }
        timeStartMills = System.currentTimeMillis();
        ConfigContext.setOnExecution(true);
        testResultCollector.initializeTestResultCollection();
        TestLifecycleEventBus.getInstance().publishExecutionStarted("EllithiumExecution");
        Ellithium.core.spi.PluginRegistry.getInstance().dispatchSuiteStart("EllithiumExecution");
    }

    @Override
    public void onExecutionFinish() {
        ConfigContext.setOnExecution(false);
        TestLifecycleEventBus.getInstance().publishExecutionFinished();
        long timeFinishMills = System.currentTimeMillis();
        long totalExecutionTime = (timeFinishMills - timeStartMills);
        long totalMills = totalExecutionTime % 1000;
        long totalSeconds = (totalExecutionTime / 1000) % 60;
        long totalMinutes = (totalExecutionTime / 60000) % 60;
        Reporter.log("Total Execution Time is: " + totalMinutes + " Min ", LogLevel.INFO_BLUE,
                totalSeconds + " Sec " + totalMills + " Mills");
        Logger.info(BLUE + "------------------------------------------" + RESET);
        Logger.info(CYAN + "------- Ellithium Engine TearDown --------" + RESET);
        Logger.info(BLUE + "------------------------------------------" + RESET);
        int active = ScreenRecorderActions.pendingCompilations();
        try {
            if (active > 0) {
                Logger.info("Awaiting background video compilation (" + active + " videos in progress)...");
                boolean drained = ScreenRecorderActions.awaitPendingCompilations(60, java.util.concurrent.TimeUnit.SECONDS);
                if (drained) {
                    Logger.info("All videos compiled");
                } else {
                    Logger.warn("Some background video compilations did not finish before timeout");
                }
            } else {
                Logger.info("All videos compiled");
            }
        } catch (Exception e) {
            Logger.logException(e);
        } finally {
            AISelfHealer.cleanup();
            EnsembleHealer.shutdown();
            AIHealingReporter.generateReport();
            Ellithium.core.reporting.internal.AllureEnvironmentManager.writeEnvironmentMetadata();
            AllureHelper.allureOpen();
            TestResultCollectorManager.getInstance().sendExecutionCompletionNotifications();
            Ellithium.core.spi.PluginRegistry.getInstance().dispatchSuiteFinish("EllithiumExecution");
            Ellithium.core.spi.PluginRegistry.getInstance().shutdown();
        }
    }

    /**
     * Restores the driver ThreadLocal on borrowed threads before any method body
     * runs.
     *
     * TestNG's dependsOnMethods can dispatch a test method
     * (or @BeforeMethod/@AfterMethod) to a
     * thread-pool thread that never executed @BeforeClass for that class. Because
     * DriverFactory
     * stores the driver in ThreadLocals, those threads see null even though the
     * browser is alive.
     * We capture the driver snapshot after @BeforeClass (in afterInvocation below)
     * and re-adopt
     * it here so that all framework features — recording, screenshots, AI healing —
     * work
     * transparently without any user boilerplate.
     */
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        Reporter.flushPendingStep();
        if (testResult != null) {
            if (method.isTestMethod() || (method.getTestMethod() != null && method.getTestMethod().isBeforeMethodConfiguration())) {
                String testIdentifier = getTestIdentifier(testResult);
                String testName = testResult.getName();
                ThreadContext.put("testIdentifier", testIdentifier);
                ThreadContext.put("testName", testName);
            } else if (method.isConfigurationMethod()) {
                String className = testResult.getTestClass() != null
                        ? testResult.getTestClass().getRealClass().getSimpleName()
                        : "Ellithium";
                String methodName = method.getTestMethod() != null ? method.getTestMethod().getMethodName() : "config";
                String target = className + "." + methodName;
                ThreadContext.put("testName", target);
                ThreadContext.put("testIdentifier", target);
            }
        }
        if (DriverFactory.getCurrentDriver() == null && testResult != null && testResult.getTestClass() != null) {
            String className = testResult.getTestClass().getRealClass().getName();
            WebDriver savedDriver = classDriverMap.get(className);
            DriverConfiguration savedConfig = classConfigMap.get(className);
            if (savedDriver != null && savedConfig != null) {
                DriverFactory.adoptCurrentThread(savedDriver, savedConfig);
            }
        }
        if (method.isTestMethod()) {
            Method javaMethod = method.getTestMethod() != null && method.getTestMethod().getConstructorOrMethod() != null
                    ? method.getTestMethod().getConstructorOrMethod().getMethod()
                    : null;
            if (javaMethod != null) {
                WithDriver withDriver = DriverFixtureProvider.resolveAnnotation(javaMethod);
                if (withDriver != null) {
                    if (DriverFactory.getCurrentDriver() == null && testResult != null && testResult.getParameters() != null) {
                        for (Object param : testResult.getParameters()) {
                            if (param instanceof WebDriver wd) {
                                DriverConfiguration cfg = new DriverConfiguration(
                                        withDriver.type(),
                                        withDriver.headless() ? HeadlessMode.True : HeadlessMode.False,
                                        withDriver.pageLoadStrategy(),
                                        withDriver.incognito() ? PrivateMode.True : PrivateMode.False,
                                        withDriver.sandbox(),
                                        withDriver.webSecurity(),
                                        null,
                                        false
                                );
                                DriverFactory.adoptCurrentThread(wd, cfg);
                                break;
                            }
                        }
                    }
                    if (DriverFactory.getCurrentDriver() == null) {
                        DriverFactory.getNewDriver(DriverFixtureProvider.createConfig(withDriver));
                    }
                }
            }
            String testName = testResult != null ? testResult.getName() : "";
            if (HealingTelemetryStore.hasHealsForTest(testName)) {
                io.qameta.allure.Allure.label("flaky", "true");
                io.qameta.allure.Allure.label("ai-healed", "true");
            }
            ensureRecordingStarted(testResult);
        }
    }

    private void ensureRecordingStarted(ITestResult testResult) {
        if (testResult == null || testResultCollector.isCucumberTest(testResult)) return;
        String testIdentifier = getTestIdentifier(testResult);
        String testName = getTestName(testResult);
        TestLifecycleEventBus.getInstance().publishTestStarted(testIdentifier, testName);
        String recordingId = VideoRecordingManager.getRecordingIdForTest(testIdentifier);
        if (recordingId != null) {
            testResultToRecordingId.put(getResultKey(testResult), recordingId);
        }
    }

    @Override
    public java.util.List<IMethodInstance> intercept(java.util.List<IMethodInstance> methods, ITestContext context) {
        if (methods == null || methods.size() <= 1) {
            return methods;
        }
        // Dynamic re-ordering: Prioritize tests that previously healed or had failure telemetry
        java.util.List<IMethodInstance> sorted = new java.util.ArrayList<>(methods);
        sorted.sort((m1, m2) -> {
            boolean m1Healed = m1 != null && m1.getMethod() != null && HealingTelemetryStore.hasHealsForTest(m1.getMethod().getMethodName());
            boolean m2Healed = m2 != null && m2.getMethod() != null && HealingTelemetryStore.hasHealsForTest(m2.getMethod().getMethodName());
            if (m1Healed && !m2Healed) return -1;
            if (!m1Healed && m2Healed) return 1;
            return 0;
        });
        return sorted;
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        Reporter.flushPendingStep();
        // Capture the driver right after @BeforeClass so we can restore it on worker
        // threads.
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
            ThreadContext.put("testName", "Ellithium");
            ThreadContext.remove("testIdentifier");
        }

        if (method.isTestMethod()) {
            boolean driverExecution = (DriverFactory.getCurrentDriver() != null);
            DriverConfiguration currentDriverConfiguration = DriverFactory.getCurrentDriverConfiguration();
            if ((testResult.getStatus() == FAILURE) && driverExecution) {
                Reporter.setStepStatus(method.getTestMethod().getMethodName(), io.qameta.allure.model.Status.FAILED);
                File failedScreenShot = GeneralHandler.testFailed(currentDriverConfiguration.getDriverType().getName(),
                        method.getTestMethod().getMethodName());
                if (failedScreenShot != null) {
                    String description = currentDriverConfiguration.getDriverType().getName().toUpperCase() + "-"
                            + method.getTestMethod().getMethodName() + " FAILED";
                    Reporter.attachScreenshotToReport(failedScreenShot, failedScreenShot.getName(), description);
                }
            }
            Reporter.addParams(GeneralHandler.getParameters());
            stopRecordingForTest(testResult, getStatus(testResult.getStatus()));
            Reporter.flushPendingStep();
            GeneralHandler.addAttachments();
            Method javaMethod = method.getTestMethod() != null && method.getTestMethod().getConstructorOrMethod() != null
                    ? method.getTestMethod().getConstructorOrMethod().getMethod()
                    : null;
            if (javaMethod != null) {
                WithDriver withDriver = DriverFixtureProvider.resolveAnnotation(javaMethod);
                if (withDriver != null && withDriver.autoQuit() && DriverFactory.getCurrentDriver() != null) {
                    try {
                        DriverFactory.quitDriver();
                    } catch (Throwable t) {
                        Logger.warn("Auto-quit driver fixture failed: " + t.getMessage());
                    }
                }
            }
        }
        if (method.isConfigurationMethod()) {
            ThreadContext.put("testName", "Ellithium");
            ThreadContext.remove("testIdentifier");
        }
        Reporter.flushPendingStep();
    }

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
        if (testMethod != null) {
            WithDriver withDriver = DriverFixtureProvider.resolveAnnotation(testMethod);
            if (withDriver != null && testMethod.getParameterCount() > 0) {
                if (annotation.getDataProvider() == null || annotation.getDataProvider().isEmpty()) {
                    annotation.setDataProvider(DriverFixtureProvider.FIXTURE_DATA_PROVIDER);
                    annotation.setDataProviderClass(DriverFixtureProvider.class);
                }
            }
        }
    }

    /**
     * Stops recording for a specific test result.
     * Uses the recording ID that was stored when the test started.
     * 
     * @param result TestNG test result
     * @param status Test status (PASSED, FAILED, SKIPPED)
     */
    private void stopRecordingForTest(ITestResult result, String status) {
        try {
            String resultKey = getResultKey(result);
            String recordingId = testResultToRecordingId.remove(resultKey);
            String videoPath = null;
            if (recordingId != null) {
                videoPath = VideoRecordingManager.stopRecordingById(recordingId, status);
            } else {
                String testIdentifier = getTestIdentifier(result);
                videoPath = VideoRecordingManager.stopRecordingForTest(testIdentifier, status);
                if (videoPath == null) {
                    videoPath = VideoRecordingManager.stopRecordingForCurrentThread(status);
                }
            }
            if (videoPath != null) {
                Logger.info(GREEN + "Video recording stopped successfully for: " + result.getName() + RESET);
            }
        } catch (Exception e) {
            Logger.warn(RED + "Failed to stop video recording: " + e.getMessage() + RESET);
            Logger.logException(e);
        }
    }

    /**
     * Gets a user-friendly test name for display.
     * 
     * @param result TestNG test result
     * @return Test name
     */
    private String getTestName(ITestResult result) {
        StringBuilder name = new StringBuilder(result.getName());
        name.append("_");
        Ellithium.core.driver.DriverConfiguration cfg = DriverFactory.getCurrentDriverConfiguration();
        name.append(
                cfg != null && cfg.getDriverType() != null ? cfg.getDriverType().getName().toUpperCase() : "UNKNOWN");
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            name.append("_params");
            for (int i = 0; i < Math.min(parameters.length, 3); i++) {
                name.append("_").append(sanitizeParam(parameters[i]));
            }
        }
        return name.toString();
    }

    private String getStatus(int status) {
        switch (status) {
            case FAILURE -> {
                return "FAILED";
            }
            case SKIP -> {
                return "SKIPPED";
            }
            default -> {
                return "PASSED";
            }
        }
    }

    /**
     * Generates a unique test identifier that's consistent across retries.
     * This is used as the key to track recordings for specific tests.
     * 
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
     * 
     * @param result TestNG test result
     * @return Unique result key
     */
    private String getResultKey(ITestResult result) {
        return getTestIdentifier(result) + "_" + System.identityHashCode(result);
    }

    /**
     * Sanitizes parameter value for use in file names.
     * 
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