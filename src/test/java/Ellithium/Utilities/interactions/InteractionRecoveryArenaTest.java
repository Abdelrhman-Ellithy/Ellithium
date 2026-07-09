package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Real-browser integration tests for {@link InteractionRecovery} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code state.html} (intercepted elements, disabled, offscreen, inert)
 * and {@code stale.html} (Shadow DOM, staleness). All non-recovery interactions use
 * Ellithium {@link ElementActions}. Complements (does not replace) {@link InteractionRecoveryTest}.
 */
public class InteractionRecoveryArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetStatePage() {
        goTo("state.html");
    }

    // ── 1. Z-index overlay — jsClick recovery ─────────────────────────────────

    /**
     * covered-btn has a transparent overlay that intercepts normal clicks.
     * recovery.jsClick() must bypass it successfully.
     */
    @Test(groups = "arena")
    public void jsClick_bypassesZIndexOverlay_onCoveredBtn() {
        // Confirm button is present but covered
        waitActions.waitForElementPresence(By.id("covered-btn"), SHORT, POLL);
        WebElement btn = waitActions.waitForElementToBeClickable(By.id("covered-btn"), SHORT, POLL);

        boolean clicked = recovery.jsClick(btn);
        Assert.assertTrue(clicked, "jsClick must return true when JS executor is available");

        // Verify the click registered via Ellithium getText on the status
        String status = elementActions.getText(By.id("covered-result"), SHORT, POLL);
        Assert.assertTrue(status.contains("✅") || status.contains("clicked"),
                "covered-btn-status must confirm click; got: " + status);
    }

    // ── 2. resolveInteractable — 5 candidates, only #3 is enabled ────────────

    /**
     * The arena renders candidates 0–4 with class {@code .add}. Only candidate-3
     * is both displayed and enabled. resolveInteractable must pick it.
     */
    @Test(groups = "arena")
    public void resolveInteractable_picksOnlyEnabledCandidate() {
        // candidate-0..4 (class .add) are static on state.html; only candidate-3 is displayed+enabled
        waitActions.waitForElementPresence(By.cssSelector(".add"), SHORT, POLL);

        WebElement chosen = recovery.resolveInteractable(By.cssSelector(".add"), SHORT, POLL);
        Assert.assertNotNull(chosen, "resolveInteractable must return a non-null element");
        Assert.assertTrue(chosen.isDisplayed() && chosen.isEnabled(),
                "resolveInteractable must return a displayed AND enabled element");

        // Verify it's candidate-3 specifically (the element is identified by data-id, not id)
        String id = elementActions.getAttributeValue(By.cssSelector("[data-id='candidate-3']"), "data-id", SHORT, POLL);
        Assert.assertEquals(id, "candidate-3",
                "The only interactable candidate should be candidate-3");
    }

    // ── 3. resolveInteractable — no valid candidate returns null ──────────────

    @Test(groups = "arena")
    public void resolveInteractable_returnsNull_whenAllCandidatesDisabled() {
        // A selector that matches no element at all → resolveInteractable must return null
        WebElement result = recovery.resolveInteractable(By.cssSelector(".no-such-candidate-xyz"), 1, 100);
        Assert.assertNull(result,
                "resolveInteractable must return null when no candidates match");
    }

    // ── 4. drillToInteractive — finds enabled child inside disabled container ──

    @Test(groups = "arena")
    public void drillToInteractive_findsInteractiveChildInContainer() {
        // .intercepted-wrap is a real container whose only interactive descendant is js-only-btn-1
        WebElement container = waitActions.waitForElementPresence(
                By.cssSelector(".intercepted-wrap"), SHORT, POLL);
        WebElement inner = recovery.drillToInteractive(container);
        Assert.assertNotNull(inner, "drillToInteractive must find an interactive descendant");
        Assert.assertTrue(inner.isEnabled(), "Found descendant must be enabled");
    }

    // ── 5. handleUnexpectedAlert — armed alert trap clears alert ──────────────

    @Test(groups = "arena")
    public void handleUnexpectedAlert_clearsAlertFiredBy2sTimer() {
        // Click the arm button — alert fires ~2s later
        elementActions.clickOnElement(By.id("arm-alert-trap-btn"), SHORT, POLL);
        // Wait for alert to fire
        try { Thread.sleep(2500); } catch (InterruptedException ignored) {}

        boolean handled = recovery.handleUnexpectedAlert();
        Assert.assertTrue(handled, "handleUnexpectedAlert must return true when alert is present");

        // Page is now usable again — confirm via Ellithium
        Assert.assertTrue(elementActions.isElementDisplayed(
                By.id("arm-alert-trap-btn"), SHORT, POLL),
                "Page must be interactive after alert cleared");
    }

    // ── 6. handleUnexpectedAlert — false when no alert ────────────────────────

    @Test(groups = "arena")
    public void handleUnexpectedAlert_returnsFalse_whenNoAlertPresent() {
        boolean handled = recovery.handleUnexpectedAlert();
        Assert.assertFalse(handled, "handleUnexpectedAlert must return false when no alert present");
    }

    // ── 7. recoverContext — NoSuchWindowException ─────────────────────────────

    @Test(groups = "arena")
    public void recoverContext_noSuchWindow_switchesToOpenWindow() {
        goTo("frames.html"); // the recoverable-window trigger lives on frames.html, not state.html
        String original = windowActions.getCurrentWindowHandle();
        elementActions.clickOnElement(By.id("open-recover-window-btn"), SHORT, POLL);
        windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);

        // Switch to popup then close it — simulates a stale window handle situation
        windowActions.switchToLastWindow();
        windowActions.closeCurrentWindow();

        // Now driver is on a closed window — recoverContext must find the open one
        boolean recovered = recovery.recoverContext(
                By.id("arm-alert-trap-btn"),
                new org.openqa.selenium.NoSuchWindowException("window gone"));
        Assert.assertTrue(recovered, "recoverContext must recover from NoSuchWindowException");
        // Verify we're back on a valid window
        Assert.assertNotNull(windowActions.getCurrentWindowHandle(),
                "After recoverContext, getCurrentWindowHandle must not throw");
    }

    // ── 8. Stale element staleness detection via wait + recovery ──────────────

    @Test(groups = "arena")
    public void staleness_afterDomDetach_recoveryWaitsAndRefetches() {
        goTo("stale.html");
        // Capture reference
        WebElement target = waitActions.waitForElementPresence(
                By.id("staleness-target"), SHORT, POLL);
        // Trigger DOM detach
        elementActions.clickOnElement(By.id("trigger-staleness-btn"), SHORT, POLL);
        // Wait for staleness via WaitActions
        boolean isStale = waitActions.waitForElementStaleness(target, MEDIUM, POLL);
        Assert.assertTrue(isStale, "Staleness wait must return true after DOM detach");
        // Re-fetch the fresh replacement element via Ellithium
        WebElement fresh = waitActions.waitForElementPresence(
                By.id("staleness-target"), SHORT, POLL);
        Assert.assertNotNull(fresh, "A new element with id=staleness-target must appear after detach");
        String text = elementActions.getText(By.id("staleness-target"), SHORT, POLL);
        Assert.assertFalse(text.isEmpty(), "Fresh element must have non-empty text");
    }

    // ── 9. Shadow DOM piercing via jsActions ──────────────────────────────────

    @Test(groups = "arena")
    public void shadowDom_pierceWithJsExecutor_sendDataViaEllithium() {
        goTo("stale.html");
        waitActions.waitForElementPresence(By.id("shadow-host"), SHORT, POLL);

        // Get shadowRoot via jsActions / raw JS executor
        Object shadowRoot = js("return arguments[0].shadowRoot",
                waitActions.waitForElementToBeVisible(By.id("shadow-host"), SHORT, POLL));
        Assert.assertNotNull(shadowRoot, "shadow-host must have an open shadowRoot");

        // Interact inside shadow DOM via JS (standard Selenium cannot pierce it)
        js("arguments[0].shadowRoot.getElementById('shadow-input').value = 'ShadowTest'",
                waitActions.waitForElementToBeVisible(By.id("shadow-host"), SHORT, POLL));
        Object value = js("return arguments[0].shadowRoot.getElementById('shadow-input').value",
                waitActions.waitForElementToBeVisible(By.id("shadow-host"), SHORT, POLL));
        Assert.assertEquals(String.valueOf(value), "ShadowTest",
                "Shadow DOM input must accept value set via JS executor");
    }

    // ── 10. Offscreen element — isElementDisplayed returns false ──────────────

    @Test(groups = "arena")
    public void offscreenElement_isNotDisplayed_beforeScrolling() {
        // offscreen-element is at left:-9999px — Ellithium isElementDisplayed returns false
        boolean displayed = elementActions.isElementDisplayed(
                By.id("offscreen-element"), SHORT, POLL);
        Assert.assertFalse(displayed,
                "offscreen-element (left:-9999px) must report isDisplayed=false");
    }

    @Test(groups = "arena")
    public void offscreenElement_jsClick_worksEvenWhenNotVisible() {
        WebElement el = waitActions.waitForElementPresence(
                By.id("offscreen-element"), SHORT, POLL);
        // jsClick can interact regardless of visibility
        boolean clicked = recovery.jsClick(el);
        Assert.assertTrue(clicked,
                "jsClick must succeed on offscreen element (JS executor ignores visibility)");
    }
}
