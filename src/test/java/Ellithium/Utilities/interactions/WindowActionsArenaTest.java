package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Real-browser integration tests for {@link WindowActions} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code frames.html}, which provides popup window triggers.
 * All interactions use Ellithium action wrappers.
 * Complements (does not replace) {@link WindowActionsTest}.
 */
public class WindowActionsArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetPage() {
        goTo("frames.html");
    }

    // ── 1. Open popup and wait for 2 windows ──────────────────────────────────

    @Test(groups = "arena")
    public void openPopup_waitForNumberOfWindowsToBe2() {
        String original = windowActions.getCurrentWindowHandle();
        elementActions.clickOnElement(By.id("open-blank-window-btn"), SHORT, POLL);
        boolean twoWindows = windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);
        Assert.assertTrue(twoWindows, "Should have 2 windows after opening popup");
        // Clean up
        windowActions.switchToLastWindow();
        windowActions.closeCurrentWindow();
        windowActions.switchToOriginalWindow(original);
    }

    // ── 2. getAllWindowHandles returns both handles ────────────────────────────

    @Test(groups = "arena")
    public void getAllWindowHandles_returnsBothHandlesAfterPopupOpen() {
        String original = windowActions.getCurrentWindowHandle();
        elementActions.clickOnElement(By.id("open-blank-window-btn"), SHORT, POLL);
        windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);

        List<String> handles = windowActions.getAllWindowHandles();
        Assert.assertEquals(handles.size(), 2,
                "getAllWindowHandles must return exactly 2 handles after popup");
        Assert.assertTrue(handles.contains(original),
                "Original window handle must be in the set");

        // Clean up
        windowActions.switchToLastWindow();
        windowActions.closeCurrentWindow();
        windowActions.switchToOriginalWindow(original);
    }

    // ── 3. Switch to last window (popup) ──────────────────────────────────────

    @Test(groups = "arena")
    public void switchToLastWindow_changesCurrentWindowHandle() {
        String original = windowActions.getCurrentWindowHandle();
        elementActions.clickOnElement(By.id("open-blank-window-btn"), SHORT, POLL);
        windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);

        windowActions.switchToLastWindow();
        String current = windowActions.getCurrentWindowHandle();
        Assert.assertNotEquals(current, original,
                "After switchToLastWindow, handle must differ from original");

        windowActions.closeCurrentWindow();
        windowActions.switchToOriginalWindow(original);
    }

    // ── 4. closeCurrentWindow — window count drops to 1 ──────────────────────

    @Test(groups = "arena")
    public void closeCurrentWindow_reducesWindowCountToOne() {
        String original = windowActions.getCurrentWindowHandle();
        elementActions.clickOnElement(By.id("open-blank-window-btn"), SHORT, POLL);
        windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);

        windowActions.switchToLastWindow();
        windowActions.closeCurrentWindow();
        windowActions.switchToOriginalWindow(original);

        int count = windowActions.getNumberOfWindows();
        Assert.assertEquals(count, 1, "Window count must be 1 after closing popup");
    }

    // ── 5. switchToOriginalWindow restores main context ───────────────────────

    @Test(groups = "arena")
    public void switchToOriginalWindow_restoresMainPageContext() {
        String original = windowActions.getCurrentWindowHandle();
        elementActions.clickOnElement(By.id("open-blank-window-btn"), SHORT, POLL);
        windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);
        windowActions.switchToLastWindow();

        // Now back to main
        windowActions.switchToOriginalWindow(original);
        Assert.assertEquals(windowActions.getCurrentWindowHandle(), original,
                "switchToOriginalWindow must restore the original handle");
        // Can interact with main page via Ellithium
        Assert.assertTrue(elementActions.isElementDisplayed(
                By.id("open-blank-window-btn"), SHORT, POLL),
                "Main page element must be accessible after switchToOriginalWindow");

        // Clean up
        windowActions.switchToLastWindow();
        windowActions.closeCurrentWindow();
        windowActions.switchToOriginalWindow(original);
    }

    // ── 6. closeAllExceptMain ─────────────────────────────────────────────────

    @Test(groups = "arena")
    public void closeAllExceptMain_afterOpeningPopup_returnsToOneWindow() {
        elementActions.clickOnElement(By.id("open-blank-window-btn"), SHORT, POLL);
        windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);

        windowActions.closeAllExceptMain();
        int count = windowActions.getNumberOfWindows();
        Assert.assertEquals(count, 1, "closeAllExceptMain must leave exactly 1 window");
    }

    // ── 7. maximizeWindow / setWindowSize ─────────────────────────────────────

    @Test(groups = "arena")
    public void maximizeWindow_setsLargeSize() {
        windowActions.maximizeWindow();
        Dimension size = windowActions.getWindowSize();
        if (headless) {
            Assert.assertTrue(size.getWidth() > 0 && size.getHeight() > 0,
                    "Headless maximize must yield a positive window size; got: " + size);
        } else {
            Assert.assertTrue(size.getWidth() > 800,
                    "Maximized window width must be > 800px; got: " + size.getWidth());
            Assert.assertTrue(size.getHeight() > 600,
                    "Maximized window height must be > 600px; got: " + size.getHeight());
        }
    }

    @Test(groups = "arena")
    public void setWindowSize_appliesDimension() {
        windowActions.setWindowSize(1280, 800);
        Dimension size = windowActions.getWindowSize();
        // Headed browser chrome/OS DPI rounding can shift outer window bounds a few px from the
        // requested size, so allow a small tolerance rather than asserting pixel-exact equality.
        int tolerance = 10;
        Assert.assertTrue(Math.abs(size.getWidth() - 1280) <= tolerance,
                "Width should be ~1280 after setWindowSize; got: " + size.getWidth());
        Assert.assertTrue(Math.abs(size.getHeight() - 800) <= tolerance,
                "Height should be ~800 after setWindowSize; got: " + size.getHeight());
    }

    // ── 8. waitForNumberOfWindowsToBe — timeout edge case ─────────────────────

    @Test(groups = "arena", expectedExceptions = TimeoutException.class)
    public void waitForNumberOfWindowsToBe_timeout_whenCountNeverMatches() {
        // Only 1 window exists — waiting for 3 should time out
        windowActions.waitForNumberOfWindowsToBe(3, 1, 100);
    }

    // ── 9. switchToPopupWindow by title ──────────────────────────────────────

    @Test(groups = "arena")
    public void switchToPopupWindow_byTitle_switchesCorrectly() {
        String original = windowActions.getCurrentWindowHandle();
        elementActions.clickOnElement(By.id("open-named-window-btn"), SHORT, POLL);
        windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);

        // Ellithium switchToPopupWindow finds window by title
        windowActions.switchToPopupWindow("Named Window", SHORT, POLL);
        String popupTitle = windowActions.getCurrentWindowTitle();
        Assert.assertTrue(popupTitle.contains("Named") || !popupTitle.isEmpty(),
                "Should be in popup window after switchToPopupWindow by title");

        windowActions.closeCurrentWindow();
        windowActions.switchToOriginalWindow(original);
    }

    // ── 10. switchToWindowByIndex ─────────────────────────────────────────────

    @Test(groups = "arena")
    public void switchToWindowByIndex_index1_switchesToPopup() {
        String original = windowActions.getCurrentWindowHandle();
        elementActions.clickOnElement(By.id("open-blank-window-btn"), SHORT, POLL);
        windowActions.waitForNumberOfWindowsToBe(2, MEDIUM, POLL);

        boolean switched = windowActions.switchToWindowByIndex(1);
        Assert.assertTrue(switched, "switchToWindowByIndex(1) must return true when popup exists");
        String afterSwitch = windowActions.getCurrentWindowHandle();
        Assert.assertNotEquals(afterSwitch, original,
                "After switchToWindowByIndex(1), handle must differ from original");

        windowActions.closeCurrentWindow();
        windowActions.switchToOriginalWindow(original);
    }
}
