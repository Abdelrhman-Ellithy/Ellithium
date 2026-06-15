package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class JavaScriptActionsTest {

    private WebDriver mockDriver;
    private JavascriptExecutor mockJs;
    private JavaScriptActions<WebDriver> jsActions;

    @BeforeMethod
    public void setup() {
        mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        mockJs     = (JavascriptExecutor) mockDriver;
        jsActions  = new JavaScriptActions<>(mockDriver);
    }

    // ── scrollByOffset ────────────────────────────────────────────────────────

    @Test
    public void scrollByOffset_executesScrollScript() {
        jsActions.scrollByOffset(200, 400);
        verify(mockJs).executeScript("window.scrollBy(arguments[0], arguments[1]);", 200, 400);
    }

    @Test
    public void scrollByOffset_negativeOffsets_executesScript() {
        jsActions.scrollByOffset(-50, -100);
        verify(mockJs).executeScript("window.scrollBy(arguments[0], arguments[1]);", -50, -100);
    }

    @Test
    public void scrollByOffset_zeroOffset_executesScript() {
        jsActions.scrollByOffset(0, 0);
        verify(mockJs).executeScript("window.scrollBy(arguments[0], arguments[1]);", 0, 0);
    }

    // ── uploadFileUsingJS — validation gate (no driver needed) ────────────────

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadFileUsingJS_throwsIllegalArgument_forNonExistentFile() {
        By locator = By.id("upload");
        jsActions.uploadFileUsingJS(locator, "/this/path/does/not/exist.txt", 5, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadFileUsingJS_defaultTimeout_throwsIllegalArgument_forNonExistentFile() {
        jsActions.uploadFileUsingJS(By.id("upload"), "/this/path/does/not/exist.txt");
    }

    // ── setElementValueUsingJS ────────────────────────────────────────────────

    @Test
    public void setElementValueUsingJS_executesSetValueScript() {
        By locator = By.id("email");
        WebElement mockElement = mock(WebElement.class);
        when(mockDriver.findElement(locator)).thenReturn(mockElement);
        jsActions.setElementValueUsingJS(locator, "user@example.com");
        verify(mockJs).executeScript(
                eq("arguments[0].value = arguments[1];"),
                eq(mockElement),
                eq("user@example.com"));
    }

    // ── javascriptClick (no-wait overload) ───────────────────────────────────

    @Test
    public void javascriptClick_noWaitOverload_executesClickScript() {
        By locator = By.id("btn");
        WebElement mockElement = mock(WebElement.class);
        when(mockDriver.findElement(locator)).thenReturn(mockElement);
        jsActions.javascriptClick(locator);
        verify(mockJs).executeScript("arguments[0].click();", mockElement);
    }
}
