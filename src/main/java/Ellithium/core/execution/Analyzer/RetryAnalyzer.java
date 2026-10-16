package Ellithium.core.execution.Analyzer;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.management.ConfigContext;
import Ellithium.core.logging.LogLevel;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import Ellithium.core.reporting.Reporter;
import java.util.concurrent.atomic.AtomicInteger;
public class RetryAnalyzer implements IRetryAnalyzer {
    private final AtomicInteger counter = new AtomicInteger(0);
    @Override
    public boolean retry(ITestResult iTestResult) {
        if (!iTestResult.isSuccess()) {
            int maxCount=ConfigContext.getRetryCount();
            if (counter.get() < maxCount) {
                counter.incrementAndGet();
                iTestResult.setStatus(ITestResult.FAILURE);
                String methodName = (iTestResult.getMethod() != null) ? iTestResult.getMethod().getMethodName() : (iTestResult.getName() != null ? iTestResult.getName() : "unknownTest");
                Reporter.log("Retry #" + counter.get() +
                        " for test: " + methodName +
                        ", on thread: " + Thread.currentThread().getName(),
                        LogLevel.ERROR);
                return true;
            }
        } else {
            iTestResult.setStatus(ITestResult.SUCCESS);
        }
        return false;
    }
    public static void initRetryCount(){
        String countStr = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "retryCountOnFailure");
        if (countStr == null) {
            ConfigContext.setRetryCount(0);
            Reporter.log("You Need to Add \"retryCountOnFailure\" Key on you config.properties File", LogLevel.ERROR);
            return;
        }
        try {
            ConfigContext.setRetryCount(Integer.parseInt(countStr));
        } catch (NumberFormatException e) {
            ConfigContext.setRetryCount(0);
            Reporter.log("Invalid \"retryCountOnFailure\" value in config.properties: \"" + countStr
                    + "\" is not a number. Retries disabled (defaulting to 0).", LogLevel.ERROR);
        }
    }
}