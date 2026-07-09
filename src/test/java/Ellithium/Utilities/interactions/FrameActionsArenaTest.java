package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Real-browser integration tests for {@link FrameActions} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code frames.html}. The page injects a login iframe after 3 seconds
 * and provides nested iframe chains. All interactions use Ellithium action wrappers.
 * Complements (does not replace) {@link FrameActionsTest}.
 */
public class FrameActionsArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetPage() {
        goTo("frames.html");
    }

    // ── 1. Switch to delayed iframe before injection — NoSuchFrameException ────

    /**
     * The dynamic-iframe is injected 3 seconds after page load.
     * Attempting to switch to it within 1 second must fail.
     */
    @Test(groups = "arena", expectedExceptions = {NoSuchFrameException.class, TimeoutException.class})
    public void switchToFrame_beforeInjection_throwsNoSuchFrame() {
        // 1s timeout < 3s injection delay — must fail
        waitActions.waitForFrameToBeAvailableAndSwitchToIt(By.id("dynamic-iframe"), 1, 100);
    }

    // ── 2. Switch to delayed iframe after injection ────────────────────────────

    @Test(groups = "arena")
    public void switchToFrame_afterInjectionDelay_allowsInteraction() {
        // MEDIUM (10s) > 3s delay — succeeds
        waitActions.waitForFrameToBeAvailableAndSwitchToIt(
                By.id("dynamic-iframe"), MEDIUM, POLL);

        // Inside frame: interact via Ellithium
        elementActions.sendData(By.id("iframe-username"), "admin",  SHORT, POLL);
        elementActions.sendData(By.id("iframe-password"), "secret", SHORT, POLL);
        elementActions.clickOnElement(By.id("iframe-login-btn"), SHORT, POLL);

        // Verify login feedback inside the iframe
        waitActions.waitForElementToBeVisible(By.id("result-message"), SHORT, POLL);
        String result = elementActions.getText(By.id("result-message"), SHORT, POLL);
        Assert.assertTrue(result.contains("✅") || result.contains("admin"),
                "Login result inside iframe should confirm success; got: " + result);

        frameActions.switchToDefaultContent();
    }

    // ── 3. switchToDefaultContent after frame interaction ─────────────────────

    @Test(groups = "arena")
    public void switchToDefaultContent_allowsInteractionWithMainPage() {
        waitActions.waitForFrameToBeAvailableAndSwitchToIt(
                By.id("dynamic-iframe"), MEDIUM, POLL);
        frameActions.switchToDefaultContent();
        // Must be able to interact with main page again
        Assert.assertTrue(elementActions.isElementDisplayed(
                By.id("inject-nested-btn"), SHORT, POLL),
                "inject-nested-btn must be visible on main page after switchToDefaultContent");
    }

    // ── 4. Nested frame — 2-level switch ──────────────────────────────────────

    @Test(groups = "arena")
    public void nestedFrames_drillDown_andInteractWithDeepInput() {
        // Inject the nested frame structure
        elementActions.clickOnElement(By.id("inject-nested-btn"), SHORT, POLL);

        // Level 1 — wait for outer frame
        waitActions.waitForFrameToBeAvailableAndSwitchToIt(
                By.name("outer-frame"), SHORT, POLL);

        // Level 2 — switch into inner frame
        waitActions.waitForFrameToBeAvailableAndSwitchToIt(
                By.name("inner-frame"), SHORT, POLL);

        // Interact deep inside via Ellithium
        elementActions.sendData(By.id("deep-input"), "NestedFrameTest", SHORT, POLL);
        String value = elementActions.getPropertyValue(By.id("deep-input"), "value", SHORT, POLL);
        Assert.assertEquals(value, "NestedFrameTest",
                "deep-input inside nested frame must accept Ellithium sendData");

        frameActions.switchToDefaultContent();
    }

    // ── 5. switchToFrame by index ─────────────────────────────────────────────

    @Test(groups = "arena")
    public void switchToFrameByIndex_afterInjection_enterFrame() {
        frameActions.switchToFrameByIndex(0, MEDIUM, POLL);

        // Verify we are inside by finding a frame-only element
        Assert.assertTrue(elementActions.isElementDisplayed(By.id("iframe-username"), SHORT, POLL),
                "Should be inside iframe after switchToFrameByIndex(0)");

        frameActions.switchToDefaultContent();
    }

    // ── 6. switchToFrame by name ──────────────────────────────────────────────

    @Test(groups = "arena")
    public void switchToFrameByName_loginFrame_afterInjection() {
        frameActions.switchToFrameByNameOrID("login-frame", MEDIUM, POLL);

        Assert.assertTrue(elementActions.isElementDisplayed(By.id("iframe-username"), SHORT, POLL),
                "Should be inside login-frame after switchToIframe by name");

        frameActions.switchToDefaultContent();
    }

    // ── 7. Form submit inside iframe — data persists ───────────────────────────

    @Test(groups = "arena")
    public void submitFormInsideIframe_captureResultThenReturnToMain() {
        waitActions.waitForFrameToBeAvailableAndSwitchToIt(
                By.id("dynamic-iframe"), MEDIUM, POLL);

        elementActions.sendData(By.id("iframe-username"), "TestUser", SHORT, POLL);
        elementActions.sendData(By.id("iframe-password"), "TestPass", SHORT, POLL);
        elementActions.clickOnElement(By.id("iframe-login-btn"), SHORT, POLL);

        String msg = elementActions.getText(By.id("result-message"), SHORT, POLL);
        frameActions.switchToDefaultContent();

        // After returning, main page elements are accessible
        Assert.assertTrue(
                elementActions.isElementDisplayed(By.id("page-title"), SHORT, POLL),
                "Main page title element must be visible after frame exit");
        Assert.assertFalse(msg.isEmpty(), "iframe login result must not be empty");
    }

    // ── 8. Blob URL iframe — switch by id ────────────────────────────────────

    @Test(groups = "arena")
    public void blobUrlIframe_switchById_interactInsideFrame() {
        // The dynamic-iframe src is a blob: URL — we switch by element id, not src
        String src = elementActions.getAttributeValue(By.id("dynamic-iframe"), "src", MEDIUM, POLL);
        Assert.assertTrue(src.startsWith("blob:") || src.startsWith("data:") || !src.isEmpty(),
                "dynamic-iframe should have a non-empty src (blob: or data:); got: " + src);

        waitActions.waitForFrameToBeAvailableAndSwitchToIt(
                By.id("dynamic-iframe"), SHORT, POLL);
        boolean usernameVisible = elementActions.isElementDisplayed(
                By.id("iframe-username"), SHORT, POLL);
        frameActions.switchToDefaultContent();

        Assert.assertTrue(usernameVisible, "iframe-username must be visible inside blob URL iframe");
    }
}
