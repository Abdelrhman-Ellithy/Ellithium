package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Real-browser integration tests for {@link SelectActions} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code forms.html}. All pre/post interaction checks use Ellithium
 * {@link ElementActions}. Complements (does not replace) {@link SelectActionsTest}.
 */
public class SelectActionsArenaTest extends ArenaBaseTest {

    private static final By NATIVE_SELECT  = By.id("native-select");
    private static final By MULTI_SELECT   = By.id("multi-select");
    private static final By GROUPED_SELECT = By.id("native-select-grouped");

    @BeforeMethod
    public void resetPage() {
        goTo("forms.html");
    }

    // ── 1. selectDropdownByText ───────────────────────────────────────────────

    @Test(groups = "arena")
    public void selectDropdownByText_selectsSeleniumWebDriver() {
        // Ellithium SelectActions waits for element before acting
        selectActions.selectDropdownByText(NATIVE_SELECT, "Selenium WebDriver", SHORT, POLL);
        List<String> selected = selectActions.getDropdownSelectedOptions(NATIVE_SELECT, SHORT, POLL);
        Assert.assertTrue(selected.contains("Selenium WebDriver"),
                "Should select 'Selenium WebDriver'; got: " + selected);
    }

    @Test(groups = "arena")
    public void selectDropdownByText_changingSelection_deselectedPrevious() {
        selectActions.selectDropdownByText(NATIVE_SELECT, "Selenium WebDriver", SHORT, POLL);
        selectActions.selectDropdownByText(NATIVE_SELECT, "Playwright", SHORT, POLL);
        List<String> selected = selectActions.getDropdownSelectedOptions(NATIVE_SELECT, SHORT, POLL);
        Assert.assertFalse(selected.contains("Selenium WebDriver"),
                "Previous selection must be replaced");
        Assert.assertTrue(selected.contains("Playwright"),
                "New selection 'Playwright' must be present");
    }

    // ── 2. selectDropdownByValue ──────────────────────────────────────────────

    @Test(groups = "arena")
    public void selectDropdownByValue_selectsCorrectOption() {
        selectActions.selectDropdownByValue(NATIVE_SELECT, "playwright", SHORT, POLL);
        List<String> selected = selectActions.getDropdownSelectedOptions(NATIVE_SELECT, SHORT, POLL);
        Assert.assertTrue(selected.stream().anyMatch(s -> s.equalsIgnoreCase("Playwright")),
                "Value 'playwright' should map to option 'Playwright'; got: " + selected);
        // Double-confirm via Ellithium getPropertyValue on the select element
        String selectedValue = elementActions.getPropertyValue(NATIVE_SELECT, "value", SHORT, POLL);
        Assert.assertEquals(selectedValue, "playwright");
    }

    @Test(groups = "arena", expectedExceptions = NoSuchElementException.class)
    public void selectDropdownByValue_nonExistentValue_throwsNoSuchElement() {
        selectActions.selectDropdownByValue(NATIVE_SELECT, "value_xyz_nonexistent", SHORT, POLL);
    }

    // ── 3. selectDropdownByIndex ──────────────────────────────────────────────

    @Test(groups = "arena")
    public void selectDropdownByIndex_index0_selectsFirstOption() {
        selectActions.selectDropdownByIndex(NATIVE_SELECT, 0, SHORT, POLL);
        List<String> selected = selectActions.getDropdownSelectedOptions(NATIVE_SELECT, SHORT, POLL);
        Assert.assertFalse(selected.isEmpty(), "Index 0 selection must not be empty");
    }

    @Test(groups = "arena")
    public void selectDropdownByIndex_index2_selectsThirdOption() {
        selectActions.selectDropdownByIndex(NATIVE_SELECT, 2, SHORT, POLL);
        List<String> selected = selectActions.getDropdownSelectedOptions(NATIVE_SELECT, SHORT, POLL);
        Assert.assertEquals(selected.size(), 1,
                "Single-select should have exactly 1 selected option after index select");
    }

    // ── 4. Multi-select ───────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void multiSelect_selectsMultipleOptionsSimultaneously() {
        selectActions.selectDropdownByText(MULTI_SELECT, "Java",   SHORT, POLL);
        selectActions.selectDropdownByText(MULTI_SELECT, "Python", SHORT, POLL);
        selectActions.selectDropdownByText(MULTI_SELECT, "Ruby", SHORT, POLL);
        List<String> selected = selectActions.getDropdownSelectedOptions(MULTI_SELECT, SHORT, POLL);
        Assert.assertTrue(selected.contains("Java"),   "Java should be selected");
        Assert.assertTrue(selected.contains("Python"), "Python should be selected");
        Assert.assertTrue(selected.contains("Ruby"), "Kotlin should be selected");
        Assert.assertTrue(selected.size() >= 3, "At least 3 options should be selected");
    }

    @Test(groups = "arena")
    public void deselectAll_afterMultiSelect_resultsInEmptySelection() {
        selectActions.selectDropdownByText(MULTI_SELECT, "Java",   SHORT, POLL);
        selectActions.selectDropdownByText(MULTI_SELECT, "Python", SHORT, POLL);
        // Verify pre-condition
        List<String> before = selectActions.getDropdownSelectedOptions(MULTI_SELECT, SHORT, POLL);
        Assert.assertTrue(before.size() >= 2, "Pre-condition: at least 2 selected");

        selectActions.deselectAll(MULTI_SELECT, SHORT, POLL);
        List<String> after = selectActions.getDropdownSelectedOptions(MULTI_SELECT, SHORT, POLL);
        Assert.assertTrue(after.isEmpty(),
                "After deselectAll, selection must be empty; got: " + after);
    }

    @Test(groups = "arena")
    public void deselectDropdownByIndex_deselects_specificIndex() {
        selectActions.selectDropdownByText(MULTI_SELECT, "Java",   SHORT, POLL);
        selectActions.selectDropdownByText(MULTI_SELECT, "Python", SHORT, POLL);
        // Deselect index 0 (Java)
        selectActions.deselectDropdownByIndex(MULTI_SELECT, 0, SHORT, POLL);
        List<String> after = selectActions.getDropdownSelectedOptions(MULTI_SELECT, SHORT, POLL);
        Assert.assertFalse(after.contains("Java"),
                "Java (index 0) should not be selected after deselectByIndex(0)");
        Assert.assertTrue(after.contains("Python"),
                "Python should remain selected after deselecting index 0");
    }

    // ── 5. Optgroup select ────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void selectDropdownByText_selectsOptionInsideOptgroup() {
        selectActions.selectDropdownByText(GROUPED_SELECT, "Chrome", SHORT, POLL);
        List<String> selected = selectActions.getDropdownSelectedOptions(GROUPED_SELECT, SHORT, POLL);
        Assert.assertTrue(selected.contains("Chrome"),
                "Should select 'Chrome' within optgroup; got: " + selected);
    }

    @Test(groups = "arena")
    public void selectDropdownByValue_selectsOptionInsideOptgroup_byValue() {
        selectActions.selectDropdownByValue(GROUPED_SELECT, "firefox", SHORT, POLL);
        String value = elementActions.getPropertyValue(GROUPED_SELECT, "value", SHORT, POLL);
        Assert.assertEquals(value, "firefox", "Grouped select should reflect 'firefox' value");
    }

    // ── 6. getDropdownSelectedOptions edge cases ──────────────────────────────

    @Test(groups = "arena")
    public void getDropdownSelectedOptions_defaultOverload_returnsSelection() {
        selectActions.selectDropdownByText(NATIVE_SELECT, "Selenium WebDriver", SHORT, POLL);
        List<String> result = selectActions.getDropdownSelectedOptions(NATIVE_SELECT);
        Assert.assertFalse(result.isEmpty(), "Default overload must return selected options");
        Assert.assertTrue(result.contains("Selenium WebDriver"));
    }

    @Test(groups = "arena")
    public void getDropdownSelectedOptions_afterDeselectAll_returnsEmptyList() {
        selectActions.deselectAll(MULTI_SELECT, SHORT, POLL);
        List<String> result = selectActions.getDropdownSelectedOptions(MULTI_SELECT, SHORT, POLL);
        Assert.assertTrue(result.isEmpty(),
                "After deselectAll on fresh load, result must be empty; got: " + result);
    }

    // ── 7. Custom div dropdown — manual Ellithium click pattern ───────────────

    /**
     * The custom div dropdown is NOT a {@code <select>}; SelectActions must NOT be used.
     * We click the trigger, wait for options via Ellithium waitForElementToBeVisible,
     * then click the desired option.
     */
    @Test(groups = "arena")
    public void customDivDropdown_ellithiumClickPattern_selectsOption() {
        // Ellithium clickOnElement — auto-waits for visibility + clickability
        elementActions.clickOnElement(By.id("custom-dd-trigger"), SHORT, POLL);

        Assert.assertTrue(elementActions.isElementDisplayed(By.id("dd-blink"), SHORT, POLL),
                "Custom dropdown option must be visible after trigger click");
        elementActions.clickOnElement(By.id("dd-blink"), SHORT, POLL);
        // Verify selection reflected in trigger text
        String triggerText = elementActions.getText(By.id("custom-dd-trigger"), SHORT, POLL);
        Assert.assertFalse(triggerText.isEmpty(),
                "Trigger button should show selected value; got empty");
    }
}
