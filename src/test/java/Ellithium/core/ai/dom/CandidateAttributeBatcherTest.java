package Ellithium.core.ai.dom;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

public class CandidateAttributeBatcherTest {

    private WebElement createMockElement() {
        return (WebElement) Proxy.newProxyInstance(
                WebElement.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> null
        );
    }

    private WebDriver createMockDriver(boolean isJsExecutor, Object jsResult, boolean throwJsException) {
        Class<?>[] interfaces = isJsExecutor
                ? new Class<?>[]{WebDriver.class, JavascriptExecutor.class}
                : new Class<?>[]{WebDriver.class};

        return (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(),
                interfaces,
                (proxy, method, args) -> {
                    if ("executeScript".equals(method.getName())) {
                        if (throwJsException) {
                            throw new RuntimeException("Simulated JS error");
                        }
                        return jsResult;
                    }
                    return null;
                }
        );
    }

    @Test
    public void testFetch_nullOrEmptyCandidates_returnsNull() {
        WebDriver driver = createMockDriver(true, List.of(), false);
        Assert.assertNull(CandidateAttributeBatcher.fetch(driver, null));
        Assert.assertNull(CandidateAttributeBatcher.fetch(driver, List.of()));
    }

    @Test
    public void testFetch_nonJavascriptExecutor_returnsNull() {
        WebDriver driver = createMockDriver(false, null, false);
        List<WebElement> candidates = List.of(createMockElement());
        Assert.assertNull(CandidateAttributeBatcher.fetch(driver, candidates));
    }

    @Test
    public void testFetch_jsException_returnsNull() {
        WebDriver driver = createMockDriver(true, null, true);
        List<WebElement> candidates = List.of(createMockElement());
        Assert.assertNull(CandidateAttributeBatcher.fetch(driver, candidates));
    }

    @Test
    public void testFetch_successfulJsExecution_returnsMappedList() {
        Map<String, Object> row1 = Map.of("id", "btn1", "tag", "button", "visible", true);
        Map<String, Object> row2 = Map.of("id", "btn2", "tag", "a", "visible", false);
        List<?> jsResult = List.of(row1, row2);

        WebDriver driver = createMockDriver(true, jsResult, false);
        List<WebElement> candidates = List.of(createMockElement(), createMockElement());

        List<Map<String, Object>> result = CandidateAttributeBatcher.fetch(driver, candidates);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get(0).get("id"), "btn1");
        Assert.assertEquals(result.get(0).get("tag"), "button");
        Assert.assertEquals(result.get(1).get("id"), "btn2");
    }

    @Test
    public void testFetch_nonListJsReturn_returnsNull() {
        WebDriver driver = createMockDriver(true, "UnexpectedStringResult", false);
        List<WebElement> candidates = List.of(createMockElement());

        Assert.assertNull(CandidateAttributeBatcher.fetch(driver, candidates));
    }
}
