package Ellithium.Utilities.interactions;

import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class MouseActionsTest {

    private WebDriver driver;
    private MouseActions<WebDriver> mouse;
    private RemoteWebElement element;

    private static final By LOCATOR = By.id("target");

    @BeforeMethod
    public void setUp() {
        driver = Mockito.mock(WebDriver.class,
                Mockito.withSettings().extraInterfaces(
                        org.openqa.selenium.JavascriptExecutor.class,
                        org.openqa.selenium.TakesScreenshot.class,
                        org.openqa.selenium.interactions.Interactive.class));
        element = Mockito.mock(RemoteWebElement.class);
        Mockito.when(element.getId()).thenReturn("element-id-001");
        mouse = new MouseActions<>(driver);

        WebDriver.TargetLocator tl = Mockito.mock(WebDriver.TargetLocator.class);
        Mockito.when(driver.switchTo()).thenReturn(tl);
        Mockito.when(driver.findElement(LOCATOR)).thenReturn(element);
        Mockito.when(element.isDisplayed()).thenReturn(true);
        Mockito.when(element.isEnabled()).thenReturn(true);
        Mockito.when(element.getTagName()).thenReturn("button");
        stubCapture();
    }

    @Test
    public void hoverOverElement_invokesDriver() {
        mouse.hoverOverElement(LOCATOR, 5);
        Mockito.verify(driver, Mockito.atLeastOnce()).findElement(LOCATOR);
    }

    @Test
    public void dragAndDrop_invokesDriver() {
        By target = By.id("target2");
        RemoteWebElement targetEl = Mockito.mock(RemoteWebElement.class);
        Mockito.when(targetEl.getId()).thenReturn("element-id-002");
        Mockito.when(driver.findElement(target)).thenReturn(targetEl);
        Mockito.when(targetEl.isDisplayed()).thenReturn(true);
        Mockito.when(targetEl.isEnabled()).thenReturn(true);
        Mockito.when(targetEl.getTagName()).thenReturn("div");
        stubCapture(target, targetEl);

        mouse.dragAndDrop(LOCATOR, target);

        Mockito.verify(driver, Mockito.atLeastOnce()).findElement(LOCATOR);
        Mockito.verify(driver, Mockito.atLeastOnce()).findElement(target);
    }

    @Test
    public void rightClick_invokesDriver() {
        mouse.rightClick(LOCATOR, 5, 500);
        Mockito.verify(driver, Mockito.atLeastOnce()).findElement(LOCATOR);
    }

    @Test
    public void doubleClick_invokesDriver() {
        mouse.doubleClick(LOCATOR, 5, 500);
        Mockito.verify(driver, Mockito.atLeastOnce()).findElement(LOCATOR);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void stubCapture() {
        stubCapture(LOCATOR, element);
    }

    private void stubCapture(By locator, RemoteWebElement el) {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.eq(el)))
                .thenReturn(new java.util.HashMap<String, Object>());
        Mockito.when(js.executeScript(Mockito.startsWith("return (function"), Mockito.any()))
                .thenReturn("[]");
        Mockito.when(js.executeScript(Mockito.contains("querySelectorAll"), Mockito.any()))
                .thenReturn(java.util.Collections.emptyList());
    }
}
