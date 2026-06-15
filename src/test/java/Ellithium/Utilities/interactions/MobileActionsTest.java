package Ellithium.Utilities.interactions;

import io.appium.java_client.AppiumDriver;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

public class MobileActionsTest {

    private AppiumDriver driver;
    private MobileActions<AppiumDriver> actions;
    private RemoteWebElement element;
    private static final By LOCATOR = By.id("el");

    @BeforeMethod
    public void setUp() {
        driver = Mockito.mock(AppiumDriver.class,
                Mockito.withSettings().extraInterfaces(
                        org.openqa.selenium.JavascriptExecutor.class,
                        org.openqa.selenium.TakesScreenshot.class));
        element = Mockito.mock(RemoteWebElement.class);
        Mockito.when(element.getId()).thenReturn("element-id-001");
        actions = new MobileActions<>(driver);

        Mockito.when(driver.findElements(LOCATOR)).thenReturn(List.of(element));
        Mockito.when(driver.findElement(LOCATOR)).thenReturn(element);
        Mockito.when(element.isDisplayed()).thenReturn(true);
        Mockito.when(element.isEnabled()).thenReturn(true);
        Mockito.when(element.getTagName()).thenReturn("XCUITypeButton");

        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenReturn(new java.util.HashMap<String, Object>());
    }

    // ── classifyGestureFailure via tap(x, y) which uses performGesture ───

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void tap_coordinate_unsupportedCommand_throwsUnsupportedOperation() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new UnsupportedCommandException("unknown command: mobile: tapGesture"));
        actions.tap(0.5, 0.5);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void tap_coordinate_notImplementedMessage_throwsUnsupportedOperation() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new RuntimeException("is not implemented for this driver"));
        actions.tap(0.5, 0.5);
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void tap_coordinate_timeoutException_rethrowsAsTimeout() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new TimeoutException("timed out waiting"));
        actions.tap(0.5, 0.5);
    }

    @Test(expectedExceptions = InvalidElementStateException.class)
    public void tap_coordinate_invalidElementState_rethrowsTyped() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new InvalidElementStateException("element is not interactable"));
        actions.tap(0.5, 0.5);
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void tap_coordinate_noSuchElement_rethrowsTyped() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new NoSuchElementException("element not found"));
        actions.tap(0.5, 0.5);
    }

    // ── performOnElement stale retry via tap(By) ─────────────────────────

    @Test
    public void tap_byLocator_stale_retriesOnce_thenSucceeds() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new StaleElementReferenceException("stale"))
                .thenReturn(new java.util.HashMap<>());

        // Should not throw — retry must succeed
        try {
            actions.tap(LOCATOR);
        } catch (StaleElementReferenceException e) {
            Assert.fail("Should have retried once and succeeded");
        }
    }

    @Test(expectedExceptions = StaleElementReferenceException.class)
    public void tap_byLocator_stale_twoConsecutiveTimes_throws() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new StaleElementReferenceException("stale 1"))
                .thenThrow(new StaleElementReferenceException("stale 2"));

        actions.tap(LOCATOR);
    }

    // ── isUnsupportedCommand message patterns ─────────────────────────────

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void tap_unknownCommandMessage_classifiedAsUnsupported() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new RuntimeException("unknown command: tap"));
        actions.tap(0.5, 0.5);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void tap_didNotMatchMessage_classifiedAsUnsupported() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;
        Mockito.when(js.executeScript(Mockito.anyString(), Mockito.any()))
                .thenThrow(new RuntimeException("did not match a supported gesture"));
        actions.tap(0.5, 0.5);
    }

    // ── validatePlatform ──────────────────────────────────────────────────

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void androidOnlyMethod_onPlainAppiumDriver_throwsUnsupported() {
        // scrollToElementBySelector calls validatePlatform(false, true) — Android required
        // plain AppiumDriver is not AndroidDriver, so it must throw UnsupportedOperationException
        actions.scrollToElementBySelector("#cell-id");
    }
}
