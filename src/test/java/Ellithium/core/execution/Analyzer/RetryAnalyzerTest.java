package Ellithium.core.execution.Analyzer;

import Ellithium.config.management.ConfigContext;
import org.mockito.Mockito;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.Assert;

public class RetryAnalyzerTest {

    private RetryAnalyzer analyzer;
    private ITestResult result;

    @BeforeMethod
    public void setUp() {
        analyzer = new RetryAnalyzer();
        result = Mockito.mock(ITestResult.class);
        ITestNGMethod method = Mockito.mock(ITestNGMethod.class);
        Mockito.when(result.getMethod()).thenReturn(method);
        Mockito.when(method.getMethodName()).thenReturn("someTest");
    }

    @Test
    public void retry_whenTestPasses_returnsFalse() {
        ConfigContext.setRetryCount(3);
        Mockito.when(result.isSuccess()).thenReturn(true);
        Assert.assertFalse(analyzer.retry(result));
    }

    @Test
    public void retry_firstFailure_belowMax_returnsTrue() {
        ConfigContext.setRetryCount(2);
        Mockito.when(result.isSuccess()).thenReturn(false);
        Assert.assertTrue(analyzer.retry(result));
    }

    @Test
    public void retry_exhaustedRetries_returnsFalse() {
        ConfigContext.setRetryCount(2);
        Mockito.when(result.isSuccess()).thenReturn(false);
        analyzer.retry(result); // attempt 1
        analyzer.retry(result); // attempt 2
        Assert.assertFalse(analyzer.retry(result)); // attempt 3 — at max
    }

    @Test
    public void retry_zeroMaxCount_failedTest_returnsFalse() {
        ConfigContext.setRetryCount(0);
        Mockito.when(result.isSuccess()).thenReturn(false);
        Assert.assertFalse(analyzer.retry(result));
    }

    @Test
    public void retry_successSetsStatusSuccess() {
        ConfigContext.setRetryCount(1);
        Mockito.when(result.isSuccess()).thenReturn(true);
        analyzer.retry(result);
        Mockito.verify(result).setStatus(ITestResult.SUCCESS);
    }

    @Test
    public void retry_failureSetsStatusFailure() {
        ConfigContext.setRetryCount(2);
        Mockito.when(result.isSuccess()).thenReturn(false);
        analyzer.retry(result);
        Mockito.verify(result).setStatus(ITestResult.FAILURE);
    }

    @Test
    public void retry_eachRetryUsesIndependentCounter() {
        ConfigContext.setRetryCount(1);
        Mockito.when(result.isSuccess()).thenReturn(false);
        RetryAnalyzer a1 = new RetryAnalyzer();
        RetryAnalyzer a2 = new RetryAnalyzer();
        Assert.assertTrue(a1.retry(result));
        Assert.assertFalse(a1.retry(result));
        Assert.assertTrue(a2.retry(result)); // independent counter
    }
}
