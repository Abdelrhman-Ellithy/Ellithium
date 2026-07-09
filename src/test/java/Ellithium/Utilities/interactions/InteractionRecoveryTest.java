package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class InteractionRecoveryTest {

    private static final By LOCATOR = By.cssSelector(".add");

    private WebDriver driver;
    private InteractionRecovery recovery;

    @BeforeMethod
    public void setup() {
        driver = mock(WebDriver.class);
        recovery = new InteractionRecovery(driver);
    }

    private WebElement element(boolean displayed, boolean enabled) {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(displayed);
        when(el.isEnabled()).thenReturn(enabled);
        return el;
    }

    @Test
    public void resolveInteractable_picksFirstDisplayedAndEnabled() {
        WebElement disabled = element(true, false);
        WebElement good = element(true, true);
        when(driver.findElements(LOCATOR)).thenReturn(List.of(disabled, good));

        WebElement chosen = recovery.resolveInteractable(LOCATOR, 1, 100);

        assertSame(chosen, good);
    }

    @Test
    public void resolveInteractable_nullWhenNoInteractableTargetExists() {
        WebElement dead = element(false, false);
        when(dead.findElements(any(By.class))).thenReturn(List.of());
        when(dead.findElement(any(By.class))).thenThrow(new org.openqa.selenium.NoSuchElementException("none"));
        when(driver.findElements(LOCATOR)).thenReturn(List.of(dead));

        assertNull(recovery.resolveInteractable(LOCATOR, 1, 100));
    }

    @Test
    public void drillToInteractive_findsInteractiveDescendant() {
        WebElement container = element(true, false);
        WebElement innerButton = element(true, true);
        when(container.findElements(any(By.class))).thenReturn(List.of(innerButton));

        assertSame(recovery.drillToInteractive(container), innerButton);
    }

    @Test
    public void isPlainClick_trueOnlyForPlainClickActions() {
        assertTrue(Ellithium.core.ai.dom.InteractiveElements.isPlainClick("clickOnElement"));
        assertTrue(Ellithium.core.ai.dom.InteractiveElements.isPlainClick("clickOnMultipleElements"));
        assertFalse(Ellithium.core.ai.dom.InteractiveElements.isPlainClick("doubleClick"));
        assertFalse(Ellithium.core.ai.dom.InteractiveElements.isPlainClick("javascriptClick"));
        assertFalse(Ellithium.core.ai.dom.InteractiveElements.isPlainClick("hoverOverElement"));
        assertFalse(Ellithium.core.ai.dom.InteractiveElements.isPlainClick(null));
    }

    @Test
    public void jsClick_falseWhenDriverCannotExecuteJavascript() {
        WebElement el = element(true, true);
        assertFalse(recovery.jsClick(el));
        assertFalse(recovery.jsClick(null));
    }

    @Test
    public void jsClick_executesScriptWhenJavascriptCapable() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        InteractionRecovery jsRecovery = new InteractionRecovery(jsDriver);
        WebElement el = element(true, true);

        assertTrue(jsRecovery.jsClick(el));
        verify((JavascriptExecutor) jsDriver).executeScript(contains("click"), eq(el));
    }

    @Test
    public void handleUnexpectedAlert_acceptsBlockingAlert() {
        org.openqa.selenium.WebDriver.TargetLocator switchTo = mock(org.openqa.selenium.WebDriver.TargetLocator.class);
        org.openqa.selenium.Alert alert = mock(org.openqa.selenium.Alert.class);
        when(driver.switchTo()).thenReturn(switchTo);
        when(switchTo.alert()).thenReturn(alert);
        when(alert.getText()).thenReturn("Leave page?");

        assertTrue(recovery.handleUnexpectedAlert());
        verify(alert).accept();
    }

    @Test
    public void handleUnexpectedAlert_falseWhenNoAlertPresent() {
        org.openqa.selenium.WebDriver.TargetLocator switchTo = mock(org.openqa.selenium.WebDriver.TargetLocator.class);
        when(driver.switchTo()).thenReturn(switchTo);
        when(switchTo.alert()).thenThrow(new org.openqa.selenium.NoAlertPresentException());

        assertFalse(recovery.handleUnexpectedAlert());
    }

    @Test
    public void recoverContext_switchesToOpenWindow_whenCurrentWindowGone() {
        org.openqa.selenium.WebDriver.TargetLocator switchTo = mock(org.openqa.selenium.WebDriver.TargetLocator.class);
        when(driver.getWindowHandles()).thenReturn(new java.util.LinkedHashSet<>(List.of("w-open")));
        when(driver.getWindowHandle()).thenThrow(new org.openqa.selenium.NoSuchWindowException("gone"));
        when(driver.switchTo()).thenReturn(switchTo);

        assertTrue(recovery.recoverContext(LOCATOR, new org.openqa.selenium.NoSuchWindowException("gone")));
        verify(switchTo).window("w-open");
    }

    @Test
    public void recoverContext_switchesToFrameContainingElement() {
        org.openqa.selenium.WebDriver.TargetLocator switchTo = mock(org.openqa.selenium.WebDriver.TargetLocator.class);
        WebElement frame = mock(WebElement.class);
        WebElement found = element(true, true);
        when(driver.switchTo()).thenReturn(switchTo);
        when(switchTo.defaultContent()).thenReturn(driver);
        when(switchTo.parentFrame()).thenReturn(driver);
        when(switchTo.frame(anyInt())).thenReturn(driver);
        when(driver.findElements(By.cssSelector("iframe, frame"))).thenReturn(List.of(frame));
        when(driver.findElements(LOCATOR)).thenReturn(List.of(), List.of(found));

        assertTrue(recovery.recoverContext(LOCATOR, new org.openqa.selenium.NoSuchFrameException("gone")));
        verify(switchTo).frame(0);
    }

    @Test
    public void recoverContext_falseWhenNoFrameContainsElement() {
        org.openqa.selenium.WebDriver.TargetLocator switchTo = mock(org.openqa.selenium.WebDriver.TargetLocator.class);
        when(driver.switchTo()).thenReturn(switchTo);
        when(switchTo.defaultContent()).thenReturn(driver);
        when(driver.findElements(By.cssSelector("iframe, frame"))).thenReturn(List.of());
        when(driver.findElements(LOCATOR)).thenReturn(List.of());

        assertFalse(recovery.recoverContext(LOCATOR, new org.openqa.selenium.NoSuchFrameException("gone")));
    }
}
