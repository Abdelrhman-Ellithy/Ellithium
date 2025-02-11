package Ellithium.core.execution.Analyzer;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
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
    public static void initRetryCount(){
        try {
            String countStr= PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(),"retryCountOnFailure");
            ConfigContext.setRetryCount(Integer.parseInt(countStr));
        }catch (Exception e){
            Reporter.log("You Need to Add \"retryCountOnFailure\" Key on you config.properties File", LogLevel.ERROR);
        }
    }
}