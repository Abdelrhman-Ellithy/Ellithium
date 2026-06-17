package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class SelectActionsTest {

    private WebDriver mockDriver;
    private WebElement mockSelect;
    private SelectActions<WebDriver> selectActions;

    private static final By LOCATOR  = By.id("dropdown");
    private static final int TIMEOUT  = 5;
    private static final int POLLING  = 100;

    @BeforeMethod
    public void setup() {
        mockDriver  = mock(WebDriver.class);
        mockSelect  = mock(WebElement.class);
        when(mockDriver.findElement(any())).thenReturn(mockSelect);
        when(mockSelect.isDisplayed()).thenReturn(true);
        when(mockSelect.isEnabled()).thenReturn(true);
        when(mockSelect.getTagName()).thenReturn("select");
        // Selenium 4.44.0 Select.assertSelectIsVisible() calls getCssValue(); null NPEs in ImmutableCollections.SetN
        when(mockSelect.getCssValue(anyString())).thenReturn("visible");
        selectActions = new SelectActions<>(mockDriver);
    }

    // ── getDropdownSelectedOptions ────────────────────────────────────────────

    @Test
    public void getDropdownSelectedOptions_returnsOnlySelectedOptionTexts() {
        WebElement opt1 = option("Alpha", true);
        WebElement opt2 = option("Beta", false);
        WebElement opt3 = option("Gamma", true);
        when(mockSelect.findElements(By.tagName("option"))).thenReturn(List.of(opt1, opt2, opt3));

        List<String> result = selectActions.getDropdownSelectedOptions(LOCATOR, TIMEOUT, POLLING);
        Assert.assertEquals(result, List.of("Alpha", "Gamma"));
    }

    @Test
    public void getDropdownSelectedOptions_emptyWhenNothingSelected() {
        WebElement opt1 = option("X", false);
        when(mockSelect.findElements(By.tagName("option"))).thenReturn(List.of(opt1));

        List<String> result = selectActions.getDropdownSelectedOptions(LOCATOR, TIMEOUT, POLLING);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getDropdownSelectedOptions_defaultOverload_delegatesThrough() {
        WebElement opt = option("Only", true);
        when(mockSelect.findElements(By.tagName("option"))).thenReturn(List.of(opt));

        List<String> result = selectActions.getDropdownSelectedOptions(LOCATOR);
        Assert.assertEquals(result, List.of("Only"));
    }

    // ── selectDropdownByIndex ─────────────────────────────────────────────────

    @Test
    public void selectDropdownByIndex_clicksOptionAtGivenIndex() {
        WebElement opt0 = option("First", false);
        when(opt0.getAttribute("index")).thenReturn("0");
        when(mockSelect.findElements(By.tagName("option"))).thenReturn(List.of(opt0));
        when(mockSelect.getDomAttribute("multiple")).thenReturn(null);

        selectActions.selectDropdownByIndex(LOCATOR, 0, TIMEOUT, POLLING);
        verify(opt0).click();
    }

    // ── deselectAll ───────────────────────────────────────────────────────────

    @Test
    public void deselectAll_clicksSelectedOptionsInMultiSelect() {
        WebElement opt1 = option("A", true);
        WebElement opt2 = option("B", false);
        when(mockSelect.findElements(By.tagName("option"))).thenReturn(List.of(opt1, opt2));
        when(mockSelect.getDomAttribute("multiple")).thenReturn("multiple");

        selectActions.deselectAll(LOCATOR, TIMEOUT, POLLING);
        verify(opt1).click();
        verify(opt2, never()).click();
    }

    @Test
    public void deselectAll_defaultOverload_delegatesThrough() {
        WebElement opt = option("X", true);
        when(mockSelect.findElements(By.tagName("option"))).thenReturn(List.of(opt));
        when(mockSelect.getDomAttribute("multiple")).thenReturn("true");

        selectActions.deselectAll(LOCATOR);
        verify(opt).click();
    }

    // ── selectDropdownByText (delegation verification) ────────────────────────

    @Test
    public void selectDropdownByText_invokesSelectByVisibleText_onFoundElement() {
        // Selenium 4.44.0: Select.isMultiple() calls getDomAttribute("multiple"), not getAttribute
        when(mockSelect.getDomAttribute("multiple")).thenReturn(null);
        try {
            selectActions.selectDropdownByText(LOCATOR, "NonExistent", TIMEOUT, POLLING);
            Assert.fail("Expected NoSuchElementException from Select.selectByVisibleText");
        } catch (org.openqa.selenium.NoSuchElementException expected) {
            // confirms selectByVisibleText was called and searched for the option
        }
        verify(mockDriver, atLeastOnce()).findElement(LOCATOR);
    }

    @Test
    public void selectDropdownByValue_invokesSelectByValue_onFoundElement() {
        when(mockSelect.getDomAttribute("multiple")).thenReturn(null);
        try {
            selectActions.selectDropdownByValue(LOCATOR, "nonexistent-value", TIMEOUT, POLLING);
            Assert.fail("Expected NoSuchElementException from Select.selectByValue");
        } catch (org.openqa.selenium.NoSuchElementException expected) {
            // confirms selectByValue was called and searched for the option
        }
        verify(mockDriver, atLeastOnce()).findElement(LOCATOR);
    }

    // ── deselectDropdownByIndex ───────────────────────────────────────────────

    @Test
    public void deselectDropdownByIndex_clicksSelectedOptionAtIndex() {
        WebElement opt0 = option("Item", true);
        when(opt0.getAttribute("index")).thenReturn("0");
        when(mockSelect.findElements(By.tagName("option"))).thenReturn(List.of(opt0));
        when(mockSelect.getDomAttribute("multiple")).thenReturn("multiple");

        selectActions.deselectDropdownByIndex(LOCATOR, 0, TIMEOUT, POLLING);
        verify(opt0).click();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static WebElement option(String text, boolean selected) {
        WebElement opt = mock(WebElement.class);
        when(opt.getText()).thenReturn(text);
        when(opt.isSelected()).thenReturn(selected);
        when(opt.isEnabled()).thenReturn(true);
        when(opt.getAttribute("value")).thenReturn(text.toLowerCase());
        return opt;
    }
}
