package Ellithium.core.execution.Analyzer;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import Ellithium.core.reporting.Reporter;
public class RetryAnalyzer implements IRetryAnalyzer {
    private int counter = 0;
    @Override
    public boolean retry(ITestResult iTestResult) {
        if (!iTestResult.isSuccess()) {
            int maxCount=ConfigContext.getRetryCount();
            if (counter < maxCount) {
                counter++;
                iTestResult.setStatus(ITestResult.FAILURE);
                Reporter.log("Retry #" + counter +
                        " for test: " + iTestResult.getMethod().getMethodName() +
                        ", on thread: " + Thread.currentThread().getName(),
                        LogLevel.ERROR);
                return true;
            }
        } else {
            iTestResult.setStatus(ITestResult.SUCCESS);
        }
        return false;
    }
}