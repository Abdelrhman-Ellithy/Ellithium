package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

public class WaitActionsTest {

    private WebDriver mockDriver;
    private WebElement mockElement;
    private WaitActions<WebDriver> waitActions;

    private static final By LOCATOR = By.id("elem");
    private static final int TIMEOUT  = 5;
    private static final int POLLING  = 100;

    @BeforeMethod
    public void setup() {
        mockDriver  = mock(WebDriver.class);
        mockElement = mock(WebElement.class);
        when(mockElement.isDisplayed()).thenReturn(true);
        waitActions = new WaitActions<>(mockDriver);
    }

    // ── presence ──────────────────────────────────────────────────────────────

    @Test
    public void waitForElementPresence_returnsPresentElement() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        WebElement result = waitActions.waitForElementPresence(LOCATOR, TIMEOUT, POLLING);
        Assert.assertEquals(result, mockElement);
    }

    @Test
    public void waitForElementPresence_defaultOverload_delegatesThrough() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        WebElement result = waitActions.waitForElementPresence(LOCATOR);
        Assert.assertEquals(result, mockElement);
    }

    // ── visibility ────────────────────────────────────────────────────────────

    @Test
    public void waitForElementToBeVisible_returnsVisibleElement() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        WebElement result = waitActions.waitForElementToBeVisible(LOCATOR, TIMEOUT, POLLING);
        Assert.assertEquals(result, mockElement);
    }

    // ── clickable ─────────────────────────────────────────────────────────────

    @Test
    public void waitForElementToBeClickable_returnsClickableElement() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.isEnabled()).thenReturn(true);
        WebElement result = waitActions.waitForElementToBeClickable(LOCATOR, TIMEOUT, POLLING);
        Assert.assertEquals(result, mockElement);
    }

    // ── disappear ─────────────────────────────────────────────────────────────

    @Test
    public void waitForElementToDisappear_succeedsWhenElementAbsent() {
        when(mockDriver.findElement(LOCATOR)).thenThrow(new NoSuchElementException("not found"));
        waitActions.waitForElementToDisappear(LOCATOR, TIMEOUT, POLLING);
    }

    @Test
    public void waitForElementToDisappear_defaultOverload_delegatesThrough() {
        when(mockDriver.findElement(LOCATOR)).thenThrow(new NoSuchElementException("not found"));
        waitActions.waitForElementToDisappear(LOCATOR);
    }

    // ── title ─────────────────────────────────────────────────────────────────

    @Test
    public void waitForTitleContains_returnsTrueWhenTitleMatches() {
        when(mockDriver.getTitle()).thenReturn("My Fancy Page");
        boolean result = waitActions.waitForTitleContains("Fancy", TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForTitleIs_returnsTrueWhenExactMatch() {
        when(mockDriver.getTitle()).thenReturn("Exact Title");
        boolean result = waitActions.waitForTitleIs("Exact Title", TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForTitleContains_defaultOverload_delegatesThrough() {
        when(mockDriver.getTitle()).thenReturn("Hello World");
        Assert.assertTrue(waitActions.waitForTitleContains("Hello"));
    }

    // ── url ───────────────────────────────────────────────────────────────────

    @Test
    public void waitForUrlContains_returnsTrueWhenUrlContainsPart() {
        when(mockDriver.getCurrentUrl()).thenReturn("https://example.com/dashboard");
        boolean result = waitActions.waitForUrlContains("dashboard", TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForUrlToBe_returnsTrueWhenExactUrlMatch() {
        when(mockDriver.getCurrentUrl()).thenReturn("https://example.com/login");
        boolean result = waitActions.waitForUrlToBe("https://example.com/login", TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    // ── attribute ─────────────────────────────────────────────────────────────

    @Test
    public void waitForElementAttributeToBe_returnsTrueWhenMatches() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.getAttribute("class")).thenReturn("btn-primary");
        boolean result = waitActions.waitForElementAttributeToBe(LOCATOR, "class", "btn-primary", TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForElementAttributeContains_returnsTrueWhenContains() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.getAttribute("class")).thenReturn("btn-primary active");
        boolean result = waitActions.waitForElementAttributeContains(LOCATOR, "class", "active", TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    // ── staleness ─────────────────────────────────────────────────────────────

    @Test
    public void waitForElementStaleness_byDirectElement_returnsTrueWhenStale() {
        when(mockElement.isEnabled()).thenThrow(new StaleElementReferenceException("stale"));
        boolean result = waitActions.waitForElementStaleness(mockElement, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForElementStaleness_byLocator_returnsTrueWhenStale() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.isEnabled()).thenThrow(new StaleElementReferenceException("stale"));
        boolean result = waitActions.waitForElementStaleness(LOCATOR, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    // ── text in element value ─────────────────────────────────────────────────

    @Test
    public void waitForTextToBePresentInElementValue_returnsTrueWhenMatches() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.getAttribute("value")).thenReturn("hello world");
        boolean result = waitActions.waitForTextToBePresentInElementValue(LOCATOR, "hello", TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    // ── window count ──────────────────────────────────────────────────────────

    @Test
    public void waitForNumberOfWindowsToBe_returnsTrueWhenMatchingCount() {
        when(mockDriver.getWindowHandles()).thenReturn(Set.of("window1", "window2"));
        boolean result = waitActions.waitForNumberOfWindowsToBe(2, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    // ── element count ─────────────────────────────────────────────────────────

    @Test
    public void waitForNumberOfElementsToBeMoreThan_returnsTrueWhenAboveThreshold() {
        WebElement e1 = mock(WebElement.class);
        WebElement e2 = mock(WebElement.class);
        when(mockDriver.findElements(LOCATOR)).thenReturn(List.of(e1, e2));
        boolean result = waitActions.waitForNumberOfElementsToBeMoreThan(LOCATOR, 1, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForNumberOfElementsToBeLessThan_returnsTrueWhenBelowThreshold() {
        when(mockDriver.findElements(LOCATOR)).thenReturn(List.of(mock(WebElement.class)));
        boolean result = waitActions.waitForNumberOfElementsToBeLessThan(LOCATOR, 3, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForNumberOfElementsToBe_returnsTrueWhenExactCount() {
        when(mockDriver.findElements(LOCATOR)).thenReturn(List.of(mock(WebElement.class), mock(WebElement.class)));
        boolean result = waitActions.waitForNumberOfElementsToBe(LOCATOR, 2, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    // ── visibility of all ─────────────────────────────────────────────────────

    @Test
    public void waitForVisibilityOfAllElements_returnsAllVisibleElements() {
        WebElement e1 = mock(WebElement.class);
        WebElement e2 = mock(WebElement.class);
        when(e1.isDisplayed()).thenReturn(true);
        when(e2.isDisplayed()).thenReturn(true);
        when(mockDriver.findElements(LOCATOR)).thenReturn(List.of(e1, e2));
        List<WebElement> results = waitActions.waitForVisibilityOfAllElements(LOCATOR, TIMEOUT, POLLING);
        Assert.assertEquals(results.size(), 2);
    }

    // ── selection state ───────────────────────────────────────────────────────

    @Test
    public void waitForElementToBeSelected_returnsTrueWhenSelected() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.isSelected()).thenReturn(true);
        boolean result = waitActions.waitForElementToBeSelected(LOCATOR, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForElementSelectionStateToBe_trueState_returnsTrueWhenSelected() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.isSelected()).thenReturn(true);
        boolean result = waitActions.waitForElementSelectionStateToBe(LOCATOR, true, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    @Test
    public void waitForElementSelectionStateToBe_falseState_returnsTrueWhenNotSelected() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.isSelected()).thenReturn(false);
        boolean result = waitActions.waitForElementSelectionStateToBe(LOCATOR, false, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    // ── enabled ───────────────────────────────────────────────────────────────

    @Test
    public void waitForElementToBeEnabled_returnsTrueWhenEnabled() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.isEnabled()).thenReturn(true);
        boolean result = waitActions.waitForElementToBeEnabled(LOCATOR, TIMEOUT, POLLING);
        Assert.assertTrue(result);
    }

    // ── text present in element ───────────────────────────────────────────────

    @Test
    public void waitForTextToBePresentInElement_returnsElementWithText() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockElement.getText()).thenReturn("Welcome to the page");
        WebElement result = waitActions.waitForTextToBePresentInElement(LOCATOR, "Welcome", TIMEOUT, POLLING);
        Assert.assertEquals(result, mockElement);
    }

    // ── frame wait ────────────────────────────────────────────────────────────

    @Test
    public void waitForFrameToBeAvailableAndSwitchToIt_switchesAndReturnsDriver() {
        TargetLocator mockTargetLocator = mock(TargetLocator.class);
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        when(mockDriver.switchTo()).thenReturn(mockTargetLocator);
        when(mockTargetLocator.frame(mockElement)).thenReturn(mockDriver);
        WebDriver result = waitActions.waitForFrameToBeAvailableAndSwitchToIt(LOCATOR, TIMEOUT, POLLING);
        Assert.assertEquals(result, mockDriver);
    }

    // ── clickable timeout fallback ─────────────────────────────────────────────

    @Test
    public void waitForElementToBeClickable_fallbackPath_usesDirectElementWhenLocatorTimesOut() {
        when(mockDriver.findElement(LOCATOR)).thenReturn(mockElement);
        // First call: isEnabled()=false → locator-based condition returns null → TimeoutException with timeout=0
        // Second call (fallback): isEnabled()=true → element-based condition satisfied
        when(mockElement.isEnabled()).thenReturn(false, true);
        WebElement result = waitActions.waitForElementToBeClickable(LOCATOR, 0, 100);
        Assert.assertEquals(result, mockElement);
    }
}
