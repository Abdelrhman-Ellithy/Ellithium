package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Real-browser integration tests for {@link MouseActions} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code mouse.html}. Pre/post checks use Ellithium {@link ElementActions}.
 * Complements (does not replace) {@link MouseActionsTest}.
 */
public class MouseActionsArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetPage() {
        goTo("mouse.html");
    }

    // ── 1. Hover — sub-menu becomes visible ───────────────────────────────────

    @Test(groups = "arena")
    public void hoverOverElement_revealsHiddenSubMenu() {
        // Pre-condition: sub-menu is hidden
        Assert.assertFalse(elementActions.isElementDisplayed(By.id("hover-submenu"), SHORT, POLL),
                "hover-submenu must be hidden before hover");

        mouseActions.hoverOverElement(By.id("hover-menu-trigger"), SHORT);

        Assert.assertTrue(elementActions.isElementDisplayed(By.id("hover-submenu"), SHORT, POLL),
                "hover-submenu must be visible after hoverOverElement");
    }

    /**
     * Edge case: sub-menu link clicked only after hover makes it reachable.
     */
    @Test(groups = "arena")
    public void hoverThenClickSubMenuLink_registersInteraction() {
        mouseActions.hoverOverElement(By.id("hover-menu-trigger"), SHORT);
        elementActions.clickOnElement(By.id("hover-smoke"), SHORT, POLL);
        String status = elementActions.getText(By.id("hover-status"), SHORT, POLL);
        Assert.assertTrue(status.contains("Smoke") || status.contains("hover") || status.contains("✅"),
                "hover-status must reflect the sub-link click; got: " + status);
    }

    // ── 2. Double-click gate ──────────────────────────────────────────────────

    @Test(groups = "arena")
    public void singleClick_doesNotActivateDblClickTarget() {
        String classBefore = elementActions.getAttributeValue(
                By.id("dbl-click-target"), "class", SHORT, POLL);
        // Single click via Ellithium
        elementActions.clickOnElement(By.id("dbl-click-target"), SHORT, POLL);
        String classAfter = elementActions.getAttributeValue(
                By.id("dbl-click-target"), "class", SHORT, POLL);
        Assert.assertEquals(classAfter, classBefore,
                "Single click must NOT activate dbl-click-target");
    }

    @Test(groups = "arena")
    public void doubleClick_activatesDblClickTarget() {
        mouseActions.doubleClick(By.id("dbl-click-target"), SHORT, POLL);
        waitActions.waitForElementAttributeContains(By.id("dbl-click-target"), "class", "activated", SHORT, POLL);
        String classAfter = elementActions.getAttributeValue(
                By.id("dbl-click-target"), "class", SHORT, POLL);
        Assert.assertTrue(classAfter.contains("activated"),
                "dbl-click-target should gain 'activated' class after doubleClick; class: " + classAfter);
    }

    // ── 3. Right-click — custom context menu ─────────────────────────────────

    @Test(groups = "arena")
    public void rightClick_opensCustomContextMenu() {
        Assert.assertFalse(elementActions.isElementDisplayed(By.id("custom-ctx-menu"), SHORT, POLL),
                "Context menu must be hidden before right-click");

        mouseActions.rightClick(By.id("right-click-target"), SHORT, POLL);

        Assert.assertTrue(elementActions.isElementDisplayed(By.id("custom-ctx-menu"), SHORT, POLL),
                "Custom context menu must appear after rightClick");
    }

    @Test(groups = "arena")
    public void rightClick_contextMenu_dismissedByClickingElsewhere() {
        mouseActions.rightClick(By.id("right-click-target"), SHORT, POLL);
        waitActions.waitForElementToBeVisible(By.id("custom-ctx-menu"), SHORT, POLL);
        // Click elsewhere with Ellithium — context menu should collapse
        elementActions.clickOnElement(By.id("dbl-click-target"), SHORT, POLL);
        waitActions.waitForElementToDisappear(By.id("custom-ctx-menu"), SHORT, POLL);
        Assert.assertFalse(elementActions.isElementDisplayed(By.id("custom-ctx-menu"), SHORT, POLL),
                "Context menu should be gone after clicking elsewhere");
    }

    // ── 4. Drag-and-drop ──────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void dragAndDrop_sourceDropsOntoTargetA() {
        mouseActions.dragAndDrop(By.id("drag-source"), By.id("drop-target-a"));

        // The page marks a successful drop by adding the 'dropped' class to the zone.
        waitActions.waitForElementAttributeContains(By.id("drop-target-a"), "class", "dropped", SHORT, POLL);
        String classAfter = elementActions.getAttributeValue(
                By.id("drop-target-a"), "class", SHORT, POLL);
        Assert.assertTrue(classAfter.contains("dropped"),
                "drop-target-a must gain the 'dropped' class after dragAndDrop; class=" + classAfter);
    }

    @Test(groups = "arena")
    public void dragAndDrop_multipleSourcesOnDifferentTargets() {
        mouseActions.dragAndDrop(By.id("drag-source"),   By.id("drop-target-a"));
        waitActions.waitForElementAttributeContains(By.id("drop-target-a"), "class", "dropped", SHORT, POLL);
        mouseActions.dragAndDrop(By.id("drag-source-2"), By.id("drop-target-b"));
        waitActions.waitForElementAttributeContains(By.id("drop-target-b"), "class", "dropped", SHORT, POLL);

        String classA = elementActions.getAttributeValue(By.id("drop-target-a"), "class", SHORT, POLL);
        String classB = elementActions.getAttributeValue(By.id("drop-target-b"), "class", SHORT, POLL);
        Assert.assertTrue(classA.contains("dropped"), "drop-target-a should show a drop; class=" + classA);
        Assert.assertTrue(classB.contains("dropped"), "drop-target-b should show a drop; class=" + classB);
    }

    // ── 5. Scroll into view then Ellithium click ──────────────────────────────

    @Test(groups = "arena")
    public void scrollIntoView_thenEllithiumClick_scrollTargetButton() {
        // Scroll via jsActions, then Ellithium clickOnElement auto-waits
        jsActions.scrollByOffset(0, 600);
        elementActions.clickOnElement(By.id("scroll-target-btn"), SHORT, POLL);
        String status = elementActions.getText(By.id("scroll-status"), SHORT, POLL);
        Assert.assertTrue(status.contains("✅") || status.contains("clicked"),
                "Scroll target status must confirm click; got: " + status);
    }

    // ── 6. Click-and-hold ─────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void clickAndHold_2Seconds_activatesHoldTarget() {
        // Use raw Actions for hold — mouseActions wraps hoverOverElement but
        // the standard hold pattern needs clickAndHold+pause+release
        new Actions(driver)
                .clickAndHold(waitActions.waitForElementToBeClickable(By.id("hold-target"), SHORT, POLL))
                .pause(Duration.ofMillis(2200))
                .release()
                .perform();

        String holdStatus = elementActions.getText(By.id("hold-status"), SHORT, POLL);
        Assert.assertTrue(holdStatus.contains("✅") || holdStatus.contains("held"),
                "hold-status must confirm successful 2s hold; got: " + holdStatus);
    }

    // ── 7. Tooltip — only visible on hover ────────────────────────────────────

    @Test(groups = "arena")
    public void tooltip_hiddenBeforeHover_visibleAfterHover() {
        Assert.assertFalse(elementActions.isElementDisplayed(By.id("tooltip-popup"), SHORT, POLL),
                "Tooltip must be hidden before hover");
        mouseActions.hoverOverElement(By.id("tooltip-trigger"), SHORT);
        Assert.assertTrue(elementActions.isElementDisplayed(By.id("tooltip-popup"), SHORT, POLL),
                "Tooltip must appear after hoverOverElement");
    }

    // ── 8. keyboard input via Ellithium sendData ──────────────────────────────

    @Test(groups = "arena")
    public void sendData_intoKeyboardInput_logsKeypresses() {
        elementActions.sendData(By.id("keyboard-input"), "Ellithium", SHORT, POLL);
        String value = elementActions.getPropertyValue(By.id("keyboard-input"), "value", SHORT, POLL);
        Assert.assertEquals(value, "Ellithium",
                "Keyboard input field should contain the typed text");
        // Verify log area received input events
        String logText = elementActions.getText(By.id("key-log"), SHORT, POLL);
        Assert.assertFalse(logText.isEmpty(), "Key log should have recorded keypress events");
    }
}
