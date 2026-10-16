package Ellithium.core.ai.sanitizers;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;

public class AccessibilityTreeExtractorTest {

    private WebDriver createMockDriver(boolean isJsExecutor, Object scriptResult, boolean throwException) {
        Class<?>[] interfaces = isJsExecutor
                ? new Class<?>[]{WebDriver.class, JavascriptExecutor.class}
                : new Class<?>[]{WebDriver.class};

        return (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(),
                interfaces,
                (proxy, method, args) -> {
                    if ("executeScript".equals(method.getName())) {
                        if (throwException) {
                            throw new RuntimeException("Simulated script execution failure");
                        }
                        return scriptResult;
                    }
                    return null;
                }
        );
    }

    @Test
    public void testExtractTree_nonJavascriptExecutor_returnsNull() {
        WebDriver driver = createMockDriver(false, null, false);
        Assert.assertNull(AccessibilityTreeExtractor.extractTree(driver));
    }

    @Test
    public void testExtractTree_scriptReturnsNull_returnsNull() {
        WebDriver driver = createMockDriver(true, null, false);
        Assert.assertNull(AccessibilityTreeExtractor.extractTree(driver));
    }

    @Test
    public void testExtractTree_scriptThrowsException_returnsNull() {
        WebDriver driver = createMockDriver(true, null, true);
        Assert.assertNull(AccessibilityTreeExtractor.extractTree(driver));
    }

    @Test
    public void testExtractTree_validShortTree_returnsExactString() {
        String expectedTree = "Page: Home\nURL: http://example.com\n\n[button] \"Submit\" id=submit-btn";
        WebDriver driver = createMockDriver(true, expectedTree, false);

        String result = AccessibilityTreeExtractor.extractTree(driver);
        Assert.assertNotNull(result);
        Assert.assertEquals(result, expectedTree);
    }

    @Test
    public void testExtractTree_veryLongTree_truncatesAt8000Chars() {
        StringBuilder longTree = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longTree.append("[item] \"Item ").append(i).append("\" id=item-").append(i).append("\n");
        }
        Assert.assertTrue(longTree.length() > 8000);

        WebDriver driver = createMockDriver(true, longTree.toString(), false);
        String result = AccessibilityTreeExtractor.extractTree(driver);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("<!-- AX tree truncated at 8000 chars -->"));
        Assert.assertTrue(result.startsWith(longTree.substring(0, 8000)));
    }
}
