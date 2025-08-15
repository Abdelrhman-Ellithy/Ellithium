package Ellithium.core.reporting.notification;

import org.testng.ITestContext;
import org.testng.ITestResult;

/**
 * Common interface for collecting test results from different test frameworks.
 * This allows TestNG and Cucumber listeners to share the same result collection logic.
 */
public interface TestResultCollector {
    
    /**
     * Collects test results from a test execution context.
     * @param context The test context containing results
     */
    void collectTestResults(ITestContext context);
    
    /**
     * Collects test results from Cucumber test execution.
     * @param scenarioName The name of the scenario
     * @param status The test status (PASSED, FAILED, SKIPPED)
     * @param executionTime The execution time in milliseconds
     */
    void collectCucumberTestResult(String scenarioName, String status, long executionTime);
    
    /**
     * Initializes the test result collection system.
     */
    void initializeTestResultCollection();
    
    /**
     * Gets the total number of tests executed.
     * @return Total tests executed
     */
    int getTotalTestsExecuted();
    
    /**
     * Gets the number of passed tests.
     * @return Number of passed tests
     */
    int getPassedTestsExecuted();
    
    /**
     * Gets the number of failed tests.
     * @return Number of failed tests
     */
    int getFailedTestsExecuted();
    
    /**
     * Gets the number of skipped tests.
     * @return Number of skipped tests
     */
    int getSkippedTestsExecuted();
    
    /**
     * Checks if a test result is from Cucumber execution.
     * This prevents double counting when both TestNG and Cucumber listeners are active.
     * @param result The test result to check
     * @return true if the test is from Cucumber
     */
    boolean isCucumberTest(ITestResult result);
}
