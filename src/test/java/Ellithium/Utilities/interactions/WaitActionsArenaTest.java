package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Real-browser integration tests for {@link WaitActions} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code waits.html}. All interactions use Ellithium action wrappers.
 * Complements (does not replace) the existing {@link WaitActionsTest} mock suite.
 */
public class WaitActionsArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetPage() {
        goTo("waits.html");
    }

    // ── 1. Presence after delayed spinner injection ───────────────────────────

    /**
     * Spinner blocks DOM injection for 5 seconds.
     * waitForElementPresence must survive past that threshold.
     */
    @Test(groups = "arena")
    public void waitForElementPresence_appearsAfter5sSpinner() {
        elementActions.clickOnElement(By.id("trigger-spinner-btn"), SHORT, POLL);
        WebElement form = waitActions.waitForElementPresence(By.id("injected-form"), LONG, POLL);
        Assert.assertNotNull(form, "injected-form should be present after spinner completes");
    }

    /**
     * Edge case: timeout shorter than injection delay → TimeoutException.
     */
    @Test(groups = "arena", expectedExceptions = TimeoutException.class)
    public void waitForElementPresence_timesOut_whenTimeoutShorterThanInjectionDelay() {
        elementActions.clickOnElement(By.id("trigger-spinner-btn"), SHORT, POLL);
        waitActions.waitForElementPresence(By.id("injected-form"), 2, POLL);
    }

    // ── 2. Visibility inside injected form ────────────────────────────────────

    @Test(groups = "arena")
    public void waitForElementToBeVisible_insideInjectedForm() {
        elementActions.clickOnElement(By.id("trigger-spinner-btn"), SHORT, POLL);
        waitActions.waitForElementPresence(By.id("injected-form"), LONG, POLL);
        WebElement field = waitActions.waitForElementToBeVisible(By.id("injected-first-name"), SHORT, POLL);
        Assert.assertTrue(elementActions.isElementDisplayed(By.id("injected-first-name"), SHORT, POLL));
    }

    // ── 3. Clickable after spinner clears ────────────────────────────────────

    @Test(groups = "arena")
    public void waitForElementToBeClickable_afterSpinnerDismisses() {
        elementActions.clickOnElement(By.id("trigger-spinner-btn"), SHORT, POLL);
        waitActions.waitForElementPresence(By.id("injected-form"), LONG, POLL);
        WebElement submitBtn = waitActions.waitForElementToBeClickable(By.id("injected-submit-btn"), SHORT, POLL);
        Assert.assertTrue(elementActions.isElementEnabled(By.id("injected-submit-btn"), SHORT, POLL));
    }

    // ── 4. Fill injected form end-to-end with Ellithium sendData ─────────────

    @Test(groups = "arena")
    public void sendData_intoInjectedFormFields_thenSubmit() {
        elementActions.clickOnElement(By.id("trigger-spinner-btn"), SHORT, POLL);
        waitActions.waitForElementPresence(By.id("injected-form"), LONG, POLL);
        waitActions.waitForElementToBeVisible(By.id("injected-first-name"), SHORT, POLL);

        elementActions.sendData(By.id("injected-first-name"), "Arena",    SHORT, POLL);
        elementActions.sendData(By.id("injected-last-name"),  "Tester",   SHORT, POLL);
        elementActions.sendData(By.id("injected-email"),      "arena@test.io", SHORT, POLL);
        elementActions.clickOnElement(By.id("injected-submit-btn"), SHORT, POLL);

        // After submit, form confirmation should appear
        waitActions.waitForElementToBeVisible(By.id("injected-result"), SHORT, POLL);
        String result = elementActions.getText(By.id("injected-result"), SHORT, POLL);
        Assert.assertTrue(result.contains("Arena") || result.contains("✅") || result.contains("submitted"),
                "Form result should confirm submission; got: " + result);
    }

    // ── 5. Counter polling — waitForTextToBePresentInElement ─────────────────

    /**
     * Counter counts 0→10 then sets text "DONE". Must wait through all ticks.
     */
    @Test(groups = "arena")
    public void waitForTextToBePresentInElement_counterReachesDone() {
        elementActions.clickOnElement(By.id("start-counter-btn"), SHORT, POLL);
        WebElement counter = waitActions.waitForTextToBePresentInElement(
                By.id("polling-counter"), "DONE", LONG, POLL);
        Assert.assertNotNull(counter);
        String text = elementActions.getText(By.id("polling-counter"), SHORT, POLL);
        Assert.assertTrue(text.contains("DONE"), "Counter must reach DONE; got: " + text);
    }

    // ── 6. Attribute wait — lazy image src ────────────────────────────────────

    @Test(groups = "arena")
    public void waitForElementAttributeContains_lazyImageSrcPopulated() {
        elementActions.clickOnElement(By.id("trigger-lazy-btn"), SHORT, POLL);
        boolean result = waitActions.waitForElementAttributeContains(
                By.id("lazy-image"), "src", "data:", MEDIUM, POLL);
        Assert.assertTrue(result, "lazy-image src should contain 'data:' after load");
    }

    @Test(groups = "arena")
    public void waitForElementAttributeToBe_dataStateBecomesActive() {
        elementActions.clickOnElement(By.id("set-attr-ready-btn"), SHORT, POLL);
        boolean result = waitActions.waitForElementAttributeToBe(
                By.id("attr-target-el"), "data-state", "ready", SHORT, POLL);
        Assert.assertTrue(result, "data-state attribute must become 'ready' after click");
        // Confirm via Ellithium getAttributeValue
        String state = elementActions.getAttributeValue(By.id("attr-target-el"), "data-state", SHORT, POLL);
        Assert.assertEquals(state, "ready");
    }

    // ── 7. Dynamic item count threshold ──────────────────────────────────────

    @Test(groups = "arena")
    public void waitForNumberOfElementsToBeMoreThan_dynamicItemsAdded() {
        elementActions.clickOnElement(By.id("add-items-btn"), SHORT, POLL);
        boolean result = waitActions.waitForNumberOfElementsToBeMoreThan(
                By.cssSelector("#dyn-items-container .delayed-pill"), 2, MEDIUM, POLL);
        Assert.assertTrue(result, "Should have more than 2 dynamic items after start");
    }

    @Test(groups = "arena")
    public void waitForNumberOfElementsToBeLessThan_initialItemCount() {
        boolean result = waitActions.waitForNumberOfElementsToBeLessThan(
                By.cssSelector("#dyn-items-container .delayed-pill"), 5, SHORT, POLL);
        Assert.assertTrue(result, "Initially less than 5 items should be present");
    }

    // ── 8. Staggered element visibility ──────────────────────────────────────

    @Test(groups = "arena")
    public void waitForVisibilityOfAllElements_staggeredReveal() {
        elementActions.clickOnElement(By.id("reveal-all-btn"), SHORT, POLL);
        List<WebElement> all = waitActions.waitForVisibilityOfAllElements(
                By.cssSelector("[id^='reveal-el']"), LONG, POLL);
        Assert.assertTrue(all.size() >= 3,
                "At least 3 staggered elements should be visible; got " + all.size());
    }

    // ── 9. Checkbox selection state ───────────────────────────────────────────

    @Test(groups = "arena")
    public void waitForElementSelectionStateToBe_autoToggleCheckbox() {
        elementActions.clickOnElement(By.id("start-toggle-cb-btn"), SHORT, POLL);
        boolean result = waitActions.waitForElementSelectionStateToBe(
                By.id("auto-toggle-checkbox"), true, MEDIUM, POLL);
        Assert.assertTrue(result, "auto-toggle-cb should become selected after 3s");
        Assert.assertTrue(elementActions.isElementSelected(By.id("auto-toggle-checkbox"), SHORT, POLL));
    }

    // ── 10. Button unlocks after timer ────────────────────────────────────────

    @Test(groups = "arena")
    public void waitForElementToBeEnabled_buttonUnlocksAfterDelay() {
        // Confirm initially disabled
        Assert.assertFalse(elementActions.isElementEnabled(By.id("enable-me-btn"), SHORT, POLL),
                "enable-me-btn should start disabled");
        elementActions.clickOnElement(By.id("trigger-enable-btn"), SHORT, POLL);
        boolean result = waitActions.waitForElementToBeEnabled(By.id("enable-me-btn"), MEDIUM, POLL);
        Assert.assertTrue(result, "enable-me-btn should become enabled after 5s timer");
        Assert.assertTrue(elementActions.isElementEnabled(By.id("enable-me-btn"), SHORT, POLL));
    }

    // ── 11. Staleness ─────────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void waitForElementStaleness_targetDetachesAfter3s() {
        goTo("stale.html"); // staleness-target + trigger-staleness-btn live on stale.html, not waits.html
        // We need a reference to wait against — get it via waitForElementPresence
        WebElement target = waitActions.waitForElementPresence(By.id("staleness-target"), SHORT, POLL);
        elementActions.clickOnElement(By.id("trigger-staleness-btn"), SHORT, POLL);
        boolean stale = waitActions.waitForElementStaleness(target, MEDIUM, POLL);
        Assert.assertTrue(stale, "staleness-target should become stale after DOM detach");
    }

    // ── 12. Title and URL waits ───────────────────────────────────────────────

    @Test(groups = "arena")
    public void waitForTitleContains_currentPageHasWaitInTitle() {
        boolean result = waitActions.waitForTitleContains("Wait", SHORT, POLL);
        Assert.assertTrue(result, "waits.html title should contain 'Wait'");
    }

    @Test(groups = "arena")
    public void waitForUrlContains_currentUrlContainsWaitsHtml() {
        boolean result = waitActions.waitForUrlContains("waits.html", SHORT, POLL);
        Assert.assertTrue(result, "URL should contain 'waits.html'");
    }

    // ── 13. Disappear — spinner overlay ──────────────────────────────────────

    @Test(groups = "arena")
    public void waitForElementToDisappear_spinnerVanishesAfterInjection() {
        elementActions.clickOnElement(By.id("trigger-spinner-btn"), SHORT, POLL);
        // Spinner should disappear once injection completes
        waitActions.waitForElementToDisappear(By.id("enable-spinner"), LONG, POLL);
        Assert.assertFalse(elementActions.isElementDisplayed(By.id("enable-spinner"), SHORT, POLL),
                "Spinner should be gone after injection");
    }
}
