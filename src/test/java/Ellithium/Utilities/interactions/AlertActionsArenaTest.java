package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Real-browser integration tests for {@link AlertActions} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code alerts.html}. All non-alert interactions use Ellithium
 * {@link ElementActions}. Complements (does not replace) {@link AlertActionsTest}.
 */
public class AlertActionsArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetPage() {
        goTo("alerts.html");
    }

    // ── 1. Simple alert — accept ──────────────────────────────────────────────

    @Test(groups = "arena")
    public void accept_dismissesSimpleAlert_pageRemainsInteractable() {
        elementActions.clickOnElement(By.id("trigger-alert-btn"), SHORT, POLL);
        alertActions.accept(SHORT, POLL);
        // After accept, page must be fully interactive — confirm via Ellithium check
        Assert.assertTrue(elementActions.isElementDisplayed(By.id("trigger-confirm-btn"), SHORT, POLL),
                "Page should remain accessible after alert dismiss");
    }

    // ── 2. getText — known alert text assertions ───────────────────────────────

    @Test(groups = "arena")
    public void getText_returnsExactKnownTextA() {
        elementActions.clickOnElement(By.id("alert-text-a-btn"), SHORT, POLL);
        String text = alertActions.getText(SHORT, POLL);
        alertActions.accept(SHORT, POLL);
        Assert.assertEquals(text, "ELLITHIUM_ALERT_A",
                "Alert A getText() must return the exact known string");
    }

    @Test(groups = "arena")
    public void getText_returnsExactKnownTextB() {
        elementActions.clickOnElement(By.id("alert-text-b-btn"), SHORT, POLL);
        String text = alertActions.getText(SHORT, POLL);
        alertActions.accept(SHORT, POLL);
        Assert.assertEquals(text, "ELLITHIUM_CONFIRM_B");
    }

    @Test(groups = "arena")
    public void getText_returnsExactKnownTextC() {
        elementActions.clickOnElement(By.id("alert-text-c-btn"), SHORT, POLL);
        String text = alertActions.getText(SHORT, POLL);
        alertActions.accept(SHORT, POLL);
        Assert.assertEquals(text, "ELLITHIUM_PROMPT_C");
    }

    // ── 3. Confirm — dismiss ──────────────────────────────────────────────────

    @Test(groups = "arena")
    public void dismiss_dismissesConfirmDialog_resultReflectsDismissal() {
        elementActions.clickOnElement(By.id("trigger-confirm-btn"), SHORT, POLL);
        alertActions.dismiss(SHORT, POLL);
        waitActions.waitForTextToBePresentInElement(By.id("confirm-result"), "Dismissed", SHORT, POLL);
        String result = elementActions.getText(By.id("confirm-result"), SHORT, POLL);
        Assert.assertTrue(result.contains("Dismissed") || result.contains("❌"),
                "confirm-result should show dismissal; got: " + result);
    }

    // ── 4. Prompt — sendData then accept ─────────────────────────────────────

    @Test(groups = "arena")
    public void sendData_thenAccept_submitsPromptInputAndVerifies() {
        elementActions.clickOnElement(By.id("trigger-prompt-btn"), SHORT, POLL);
        alertActions.sendData("ArenaTestInput", SHORT, POLL);
        alertActions.accept(SHORT, POLL);
        waitActions.waitForTextToBePresentInElement(By.id("prompt-result"), "ArenaTestInput", SHORT, POLL);
        String result = elementActions.getText(By.id("prompt-result"), SHORT, POLL);
        Assert.assertTrue(result.contains("ArenaTestInput"),
                "prompt-result should contain sent input; got: " + result);
    }

    // ── 5. Edge case: NoAlertPresentException before delayed alert fires ───────

    /**
     * Calling alertActions.getText with a 1-second timeout after clicking the
     * delayed-alert button (which fires after 3 seconds) must throw.
     */
    @Test(groups = "arena", expectedExceptions = {NoAlertPresentException.class, TimeoutException.class})
    public void getText_throwsWhenCalledBeforeDelayedAlertFires() {
        elementActions.clickOnElement(By.id("trigger-delayed-alert-btn"), SHORT, POLL);
        // 1s timeout < 3s delay — must fail
        alertActions.getText(1, 100);
    }

    // ── 6. Delayed alert — wait long enough ──────────────────────────────────

    @Test(groups = "arena")
    public void accept_waitsForDelayedConfirm_afterFullWaitPeriod() {
        elementActions.clickOnElement(By.id("trigger-delayed-alert-btn"), SHORT, POLL);
        // MEDIUM (10s) > 3s delay — must succeed
        String text = alertActions.getText(MEDIUM, POLL);
        Assert.assertTrue(text.contains("ELLITHIUM_DELAYED_CONFIRM"),
                "Delayed confirm text should contain known string; got: " + text);
        alertActions.accept(SHORT, POLL);
        waitActions.waitForTextToBePresentInElement(By.id("delayed-alert-status"), "✅", SHORT, POLL);
        String status = elementActions.getText(By.id("delayed-alert-status"), SHORT, POLL);
        Assert.assertTrue(status.contains("Accepted") || status.contains("✅"),
                "delayed-alert-status should reflect acceptance; got: " + status);
    }

    // ── 7. Delayed prompt — sendData after wait ───────────────────────────────

    @Test(groups = "arena")
    public void sendDataThenAccept_onDelayedPrompt_verifyResult() {
        elementActions.clickOnElement(By.id("trigger-delayed-prompt-btn"), SHORT, POLL);
        alertActions.sendData("DelayedInput", MEDIUM, POLL);
        alertActions.accept(SHORT, POLL);
        waitActions.waitForTextToBePresentInElement(By.id("delayed-prompt-status"), "✅", SHORT, POLL);
        String result = elementActions.getText(By.id("delayed-prompt-status"), SHORT, POLL);
        Assert.assertTrue(result.contains("DelayedInput") || result.contains("✅"),
                "delayed-prompt-status should reflect sent input; got: " + result);
    }

    // ── 8. DOM modal — input inside overlay, not a browser alert ─────────────

    @Test(groups = "arena")
    public void domModal_open_typeInside_confirm_verifyResult() {
        elementActions.clickOnElement(By.id("open-modal-btn"), SHORT, POLL);
        // Wait for modal to be visible via Ellithium
        Assert.assertTrue(elementActions.isElementDisplayed(By.id("custom-modal"), SHORT, POLL),
                "Custom modal must be visible after open");
        // Type inside the modal using Ellithium sendData (auto-waits for visibility)
        elementActions.sendData(By.id("modal-input"), "ArenaModalTest", SHORT, POLL);
        elementActions.clickOnElement(By.id("modal-confirm-btn"), SHORT, POLL);
        String result = elementActions.getText(By.id("modal-result"), SHORT, POLL);
        Assert.assertTrue(result.contains("ArenaModalTest"),
                "modal-result should contain typed value; got: " + result);
    }

    // ── 9. Close DOM modal via Escape key ────────────────────────────────────

    @Test(groups = "arena")
    public void domModal_closedByEscape_isNoLongerVisible() {
        elementActions.clickOnElement(By.id("open-modal-btn"), SHORT, POLL);
        Assert.assertTrue(elementActions.isElementDisplayed(By.id("custom-modal"), SHORT, POLL));
        // Send Escape via Ellithium sendData with Keys
        elementActions.sendData(By.id("modal-input"), org.openqa.selenium.Keys.ESCAPE, SHORT, POLL);
        waitActions.waitForElementToDisappear(By.cssSelector(".modal-overlay.open"), SHORT, POLL);
        Assert.assertFalse(
                elementActions.isElementDisplayed(By.id("custom-modal-overlay"), SHORT, POLL),
                "Modal overlay should be closed after Escape");
    }

    // ── 10. Unexpected alert recovery via InteractionRecovery ─────────────────

    @Test(groups = "arena")
    public void handleUnexpectedAlert_clearsBlueTriggerAlert() {
        // Arm the blur alert
        elementActions.clickOnElement(By.id("arm-blur-alert"), SHORT, POLL);
        // Blur via JS, not by clicking another button — every other button on this page fires its own alert.
        elementActions.sendData(By.id("blur-alert-input"), "x", SHORT, POLL);
        js("document.activeElement.blur();");
        try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        boolean handled = recovery.handleUnexpectedAlert();
        Assert.assertTrue(handled, "InteractionRecovery should handle the blur-triggered alert");
    }
}
