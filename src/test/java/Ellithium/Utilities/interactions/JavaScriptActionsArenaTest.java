package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Real-browser integration tests for {@link JavaScriptActions} using the Ellithium Test Arena.
 *
 * <p>Runs primarily against {@code forms.html} and {@code mouse.html}.
 * Complements (does not replace) {@link JavaScriptActionsTest}.
 */
public class JavaScriptActionsArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetPage() {
        goTo("forms.html");
    }

    // ── 1. javascriptClick — bypasses overlay ─────────────────────────────────

    @Test(groups = "arena")
    public void javascriptClick_withTimeout_clicksCoveredButton() {
        goTo("state.html");
        jsActions.javascriptClick(By.id("covered-btn"), SHORT, POLL);
        String status = elementActions.getText(By.id("covered-result"), SHORT, POLL);
        Assert.assertTrue(status.contains("✅") || status.contains("clicked"),
                "covered-btn-status must confirm JS click; got: " + status);
    }

    @Test(groups = "arena")
    public void javascriptClick_noWaitOverload_clicksVisibleButton() {
        waitActions.waitForElementPresence(By.id("set-value-js-btn"), SHORT, POLL, true);
        jsActions.javascriptClick(By.id("set-value-js-btn"));
        String result = elementActions.getText(By.id("select-status"), SHORT, POLL);
        Assert.assertTrue(result.contains("executed") || result.contains("setElementValue"),
                "JS-click status must confirm the JS setter ran; got: " + result);
    }

    // ── 2. setElementValueUsingJS ─────────────────────────────────────────────

    @Test(groups = "arena")
    public void setElementValueUsingJS_setsReadonlyFieldValue() {
        // readonly-value-field is readonly — sendData won't work, JS setter must be used
        waitActions.waitForElementPresence(By.id("js-set-value-target"), SHORT, POLL, true);
        jsActions.setElementValueUsingJS(By.id("js-set-value-target"), "ArenaJsValue");
        String value = elementActions.getPropertyValue(
                By.id("js-set-value-target"), "value", SHORT, POLL);
        Assert.assertEquals(value, "ArenaJsValue",
                "setElementValueUsingJS must override readonly field value");
    }

    @Test(groups = "arena")
    public void setElementValueUsingJS_overwritesExistingValue() {
        waitActions.waitForElementPresence(By.id("js-set-value-target"), SHORT, POLL);
        elementActions.sendData(By.id("js-set-value-target"), "OldValue", SHORT, POLL);
        jsActions.setElementValueUsingJS(By.id("js-set-value-target"), "NewJsValue");
        String value = elementActions.getPropertyValue(
                By.id("js-set-value-target"), "value", SHORT, POLL);
        Assert.assertEquals(value, "NewJsValue",
                "setElementValueUsingJS must overwrite existing input value");
    }

    // ── 3. scrollByOffset ─────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void scrollByOffset_scrollsDownPageAndExposesBelowFoldElement() {
        goTo("mouse.html");
        // Before scroll, scroll-target-btn may not be in viewport
        jsActions.scrollByOffset(0, 800);
        // After scroll, Ellithium clickOnElement can click it
        elementActions.clickOnElement(By.id("scroll-target-btn"), SHORT, POLL);
        String status = elementActions.getText(By.id("scroll-status"), SHORT, POLL);
        Assert.assertTrue(status.contains("✅") || status.contains("clicked"),
                "scroll-target-status must confirm click after scroll; got: " + status);
    }

    @Test(groups = "arena")
    public void scrollByOffset_negativeOffset_scrollsUp() {
        goTo("mouse.html");
        jsActions.scrollByOffset(0, 1000);
        jsActions.scrollByOffset(0, -500);
        // Page should still be functional — confirm via Ellithium
        Assert.assertTrue(
                elementActions.isElementPresent(By.id("hover-menu-trigger"), SHORT, POLL),
                "hover-menu-trigger must be present after scroll-up");
    }

    // ── 4. scrollToElement ────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void scrollToElement_bringsTargetIntoViewport() {
        goTo("mouse.html");
        jsActions.scrollToElement(By.id("scroll-target-btn"));
        // After scroll, element is in viewport — Ellithium waitForElementToBeClickable succeeds
        waitActions.waitForElementToBeClickable(By.id("scroll-target-btn"), SHORT, POLL);
        Assert.assertTrue(
                elementActions.isElementClickable(By.id("scroll-target-btn"), SHORT, POLL),
                "scroll-target-btn must be clickable after scrollToElement");
    }

    // ── 5. uploadFileUsingJS ──────────────────────────────────────────────────

    @Test(groups = "arena", expectedExceptions = IllegalArgumentException.class)
    public void uploadFileUsingJS_throwsIllegalArgument_forNonExistentFile() {
        jsActions.uploadFileUsingJS(By.id("file-upload"), "/nonexistent/path/file.txt", SHORT, POLL);
    }

    @Test(groups = "arena")
    public void uploadFileUsingJS_uploadsRealTempFile_verifiedByInputValue() throws IOException {
        // Create a real temp file so the path-validation passes
        File tmp = File.createTempFile("arena-upload-", ".txt");
        tmp.deleteOnExit();
        Files.writeString(tmp.toPath(), "Ellithium arena upload test");

        jsActions.uploadFileUsingJS(By.id("file-upload"), tmp.getAbsolutePath(), SHORT, POLL);
        // The file input value (browser-sanitised) ends with the filename
        String value = elementActions.getPropertyValue(By.id("file-upload"), "value", SHORT, POLL);
        Assert.assertTrue(value.contains("arena-upload-") || value.contains(tmp.getName()),
                "File input value must contain uploaded filename; got: " + value);
    }

    // ── 6. End-to-end: setElementValueUsingJS on range slider ─────────────────

    @Test(groups = "arena")
    public void setElementValueUsingJS_setsRangeSliderValue() {
        waitActions.waitForElementPresence(By.id("timeout-slider"), SHORT, POLL, true);
        jsActions.setElementValueUsingJS(By.id("timeout-slider"), "75");
        String value = elementActions.getPropertyValue(
                By.id("timeout-slider"), "value", SHORT, POLL);
        Assert.assertEquals(value, "75",
                "Range slider value must be 75 after JS setter");
    }

    // ── 7. setElementValueUsingJS on date input ───────────────────────────────

    @Test(groups = "arena")
    public void setElementValueUsingJS_setsDateInputValue() {
        waitActions.waitForElementPresence(By.id("date-input"), SHORT, POLL);
        jsActions.setElementValueUsingJS(By.id("date-input"), "2024-12-31");
        String value = elementActions.getPropertyValue(
                By.id("date-input"), "value", SHORT, POLL);
        Assert.assertEquals(value, "2024-12-31",
                "Date input value must be 2024-12-31 after JS setter");
    }
}
