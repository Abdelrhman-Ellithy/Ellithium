package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ShadowDomResolverTest {

    interface TestDriver extends WebDriver, JavascriptExecutor {}

    private TestDriver driver;

    @BeforeMethod
    public void setup() {
        driver = mock(TestDriver.class);
    }

    @Test
    public void toCssSelector_convertsStandardLocators() {
        assertEquals(ShadowDomResolver.toCssSelector(By.cssSelector("button.submit")), "button.submit");
        assertEquals(ShadowDomResolver.toCssSelector(By.id("username")), "#username");
        assertEquals(ShadowDomResolver.toCssSelector(By.className("active")), ".active");
        assertEquals(ShadowDomResolver.toCssSelector(By.tagName("input")), "input");
        assertEquals(ShadowDomResolver.toCssSelector(By.name("email")), "[name='email']");
    }

    @Test
    public void toCssSelector_convertsSimpleXPaths() {
        assertEquals(ShadowDomResolver.toCssSelector(By.xpath("//button[@id='btn']")), "button#btn");
        assertEquals(ShadowDomResolver.toCssSelector(By.xpath("//*[@id='login']")), "#login");
        assertEquals(ShadowDomResolver.toCssSelector(By.xpath("//div[@class='card']")), "div.card");
        assertEquals(ShadowDomResolver.toCssSelector(By.xpath("//input[@name='q']")), "input[name='q']");
        assertEquals(ShadowDomResolver.toCssSelector(By.xpath("//span[contains(@class, 'icon')]")), "span[class*='icon']");
        assertEquals(ShadowDomResolver.toCssSelector(By.xpath("//h1")), "h1");
    }

    @Test
    public void findElement_usesNativeSeleniumFirstWithoutJs() {
        WebElement host = mock(WebElement.class);
        SearchContext shadow = mock(SearchContext.class);
        WebElement shadowButton = mock(WebElement.class);

        when(driver.findElements(By.cssSelector("*"))).thenReturn(List.of(host));
        when(host.getShadowRoot()).thenReturn(shadow);
        when(shadow.findElements(By.cssSelector("button.submit"))).thenReturn(List.of(shadowButton));

        WebElement result = ShadowDomResolver.findElement(driver, By.cssSelector("button.submit"));

        assertNotNull(result);
        assertSame(result, shadowButton);
        // Verify ZERO javascript execution occurred!
        verify(driver, never()).executeScript(anyString(), any());
    }

    @Test
    public void findElement_supportsChainedArrowSelectorNatively() {
        WebElement host = mock(WebElement.class);
        SearchContext shadow = mock(SearchContext.class);
        WebElement target = mock(WebElement.class);

        when(driver.findElement(By.cssSelector("user-card"))).thenReturn(host);
        when(host.getShadowRoot()).thenReturn(shadow);
        when(shadow.findElements(By.cssSelector("span.name"))).thenReturn(List.of(target));

        WebElement result = ShadowDomResolver.findElement(driver, By.cssSelector("user-card >>> span.name"));

        assertNotNull(result);
        assertSame(result, target);
        verify(driver, never()).executeScript(anyString(), any());
    }

    @Test
    public void findElement_fallsBackToDeepJsWhenNativeFails() {
        // Native scan returns no shadow root
        when(driver.findElements(By.cssSelector("*"))).thenReturn(Collections.emptyList());

        WebElement deepElement = mock(WebElement.class);
        when(driver.executeScript(eq(ShadowDomResolver.DEEP_JS_SCRIPT), anyString(), anyString(), eq(false)))
                .thenReturn(deepElement);

        WebElement result = ShadowDomResolver.findElement(driver, By.xpath("//div[@custom='val']//button"));

        assertNotNull(result);
        assertSame(result, deepElement);
        verify(driver).executeScript(eq(ShadowDomResolver.DEEP_JS_SCRIPT), eq("//div[@custom='val']//button"), eq("xpath"), eq(false));
    }

    @Test
    public void findElements_returnsAllMatchesViaNativeThenJs() {
        WebElement host = mock(WebElement.class);
        SearchContext shadow = mock(SearchContext.class);
        WebElement el1 = mock(WebElement.class);
        WebElement el2 = mock(WebElement.class);

        when(driver.findElements(By.cssSelector("*"))).thenReturn(List.of(host));
        when(host.getShadowRoot()).thenReturn(shadow);
        when(shadow.findElements(By.cssSelector(".item"))).thenReturn(List.of(el1, el2));

        List<WebElement> results = ShadowDomResolver.findElements(driver, By.cssSelector(".item"));

        assertEquals(results.size(), 2);
        assertTrue(results.contains(el1));
        assertTrue(results.contains(el2));
        verify(driver, never()).executeScript(anyString(), any());
    }

    @Test
    public void baseActions_transparentlyFindsShadowElementWhenDocumentFindFails() {
        BaseActions<TestDriver> actions = new BaseActions<>(driver);
        By locator = By.cssSelector("custom-tag button");

        // Standard find fails because element is inside Shadow DOM
        when(driver.findElement(locator)).thenThrow(new NoSuchElementException("Not in document"));

        // Native shadow resolution finds it
        WebElement host = mock(WebElement.class);
        SearchContext shadow = mock(SearchContext.class);
        WebElement shadowButton = mock(WebElement.class);

        when(driver.findElements(By.cssSelector("custom-tag"))).thenReturn(List.of(host));
        when(host.getShadowRoot()).thenReturn(shadow);
        when(shadow.findElements(By.cssSelector("button"))).thenReturn(List.of(shadowButton));

        WebElement resolved = actions.findWebElement(locator);

        assertNotNull(resolved);
        assertSame(resolved, shadowButton);
    }
}
