package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Comprehensive real-browser integration tests for {@link ElementActions}.
 *
 * <p>This is the primary arena test class and exercises every public method
 * of {@link ElementActions} against real browser mechanics:
 *
 * <ul>
 *   <li>{@code sendData()} — text, Keys, stale-retry, disabled, readonly</li>
 *   <li>{@code clickOnElement()} — normal, intercepted, multiple elements</li>
 *   <li>{@code getText()} / {@code getTextFromMultipleElements()}</li>
 *   <li>{@code getAttributeValue()} / {@code getPropertyValue()}</li>
 *   <li>{@code getAttributeFromMultipleElements()} / {@code getPropertyFromMultipleElements()}</li>
 *   <li>{@code uploadFile()} — sendKeys path, validation</li>
 *   <li>{@code isElementPresent()} / {@code isElementDisplayed()} / {@code isElementEnabled()}</li>
 *   <li>{@code isElementSelected()} / {@code isElementClickable()}</li>
 *   <li>{@code clearElement()}</li>
 *   <li>{@code scrollIntoView()}</li>
 *   <li>{@code isTextContains()} / {@code isTextEqual()}</li>
 *   <li>{@code isAttributeContains()}</li>
 *   <li>{@code clickOnMultipleElements()}</li>
 * </ul>
 *
 * <p>Runs across multiple arena pages depending on the target element.
 */
public class ElementActionsArenaTest extends ArenaBaseTest {

    // ═══════════════════════════════════════════════════════════════════════
    // sendData — forms.html
    // ═══════════════════════════════════════════════════════════════════════

    @BeforeMethod
    public void resetPage() {
        goTo("forms.html");
    }

    // ── sendData(String) — normal visible input ────────────────────────────────

    @Test(groups = "arena")
    public void sendData_text_typesIntoVisibleTextField() {
        elementActions.sendData(By.id("js-set-value-target"), "HelloArena", SHORT, POLL);
        String value = elementActions.getPropertyValue(By.id("js-set-value-target"), "value", SHORT, POLL);
        Assert.assertEquals(value, "HelloArena",
                "sendData must set input value to 'HelloArena'");
    }

    @Test(groups = "arena")
    public void sendData_text_replacesExistingContent() {
        elementActions.sendData(By.id("js-set-value-target"), "First",  SHORT, POLL);
        elementActions.sendData(By.id("js-set-value-target"), "Second", SHORT, POLL);
        String value = elementActions.getPropertyValue(By.id("js-set-value-target"), "value", SHORT, POLL);
        Assert.assertEquals(value, "Second",
                "sendData must clear and replace existing input content");
    }

    @Test(groups = "arena")
    public void sendData_textarea_multilineInput() {
        String multiLine = "Line1\nLine2\nLine3";
        elementActions.sendData(By.id("textarea-input"), multiLine, SHORT, POLL);
        String value = elementActions.getPropertyValue(By.id("textarea-input"), "value", SHORT, POLL);
        Assert.assertTrue(value.contains("Line1") && value.contains("Line3"),
                "Textarea must contain all lines; got: " + value);
    }

    @Test(groups = "arena")
    public void sendData_emptyString_clearsField() {
        elementActions.sendData(By.id("js-set-value-target"), "SomeText",  SHORT, POLL);
        elementActions.sendData(By.id("js-set-value-target"), "", SHORT, POLL);
        String value = elementActions.getPropertyValue(By.id("js-set-value-target"), "value", SHORT, POLL);
        Assert.assertEquals(value, "",
                "sendData with empty string must produce empty input value");
    }

    // ── sendData(Keys) — keyboard special keys ────────────────────────────────

    @Test(groups = "arena")
    public void sendData_keys_tabMovesToNextField() {
        elementActions.sendData(By.id("js-set-value-target"), "Value1", SHORT, POLL);
        elementActions.sendData(By.id("js-set-value-target"), Keys.TAB, SHORT, POLL);
        // Focus should shift — confirm the second field is now focused (active element)
        Object tagName = js("return document.activeElement.id");
        Assert.assertNotEquals(String.valueOf(tagName), "js-set-value-target",
                "Tab key must move focus away from the field");
    }

    @Test(groups = "arena")
    public void sendData_keys_enterSubmitsForm() {
        elementActions.sendData(By.id("form-name"), "SearchQuery", SHORT, POLL);
        elementActions.sendData(By.id("form-email"), "arena@test.io" + Keys.ENTER, SHORT, POLL);
        String result = elementActions.getText(By.id("form-result"), SHORT, POLL);
        Assert.assertFalse(result.isEmpty(),
                "ENTER key must trigger form action; got empty result");
    }

    // ── sendData — stale-retry (stale.html) ───────────────────────────────────

    @Test(groups = "arena")
    public void getText_retriesOnStaleElement_afterDomMutation() {
        goTo("stale.html");
        // Grab a reference to staleness-target first
        waitActions.waitForElementPresence(By.id("staleness-target"), SHORT, POLL);
        // Trigger DOM replacement: staleness-target is removed and a NEW element with the same id is
        // injected ~3s later, so any prior reference goes stale.
        elementActions.clickOnElement(By.id("trigger-staleness-btn"), SHORT, POLL);
        // getText auto-retries on StaleElementReferenceException and reads the fresh element
        // (re-injected ~3s after the click), so a LONG budget covers the replacement delay.
        String text = elementActions.getText(By.id("staleness-target"), LONG, POLL);
        Assert.assertFalse(text.isEmpty(),
                "getText must succeed on the re-injected element after DOM replacement via stale retry");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // clickOnElement — forms.html / state.html
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void clickOnElement_clicksCheckbox_togglesSelection() {
        By cb = By.id("screenshots-checkbox");
        boolean before = elementActions.isElementSelected(cb, SHORT, POLL);
        elementActions.clickOnElement(cb, SHORT, POLL);
        boolean after = elementActions.isElementSelected(cb, SHORT, POLL);
        Assert.assertNotEquals(after, before,
                "clickOnElement on checkbox must toggle selection state");
    }

    @Test(groups = "arena")
    public void clickOnElement_clicksRadioButton_selectsIt() {
        elementActions.clickOnElement(By.id("radio-firefox"), SHORT, POLL);
        Assert.assertTrue(elementActions.isElementSelected(By.id("radio-firefox"), SHORT, POLL),
                "Radio B must be selected after clickOnElement");
        Assert.assertFalse(elementActions.isElementSelected(By.id("radio-chrome"), SHORT, POLL),
                "Radio A must be deselected when Radio B is selected");
    }

    @Test(groups = "arena")
    public void clickOnElement_onSubmitButton_triggersFormAction() {
        elementActions.sendData(By.id("name"),  "Arena",        SHORT, POLL);
        elementActions.sendData(By.id("formEmail"), "a@arena.test", SHORT, POLL);
        elementActions.clickOnElement(By.id("send-form-button"), SHORT, POLL);
        waitActions.waitForElementToBeVisible(By.id("form-result"), SHORT, POLL);
        String result = elementActions.getText(By.id("result"), SHORT, POLL);
        Assert.assertTrue(result.contains("Arena") || result.contains("✅"),
                "contact-result must confirm form submission; got: " + result);
    }

    @Test(groups = "arena")
    public void clickOnElement_withTimeout_onDelayedButton() {
        goTo("waits.html");
        elementActions.clickOnElement(By.id("begin-enable-timer"), SHORT, POLL);
        elementActions.clickOnElement(By.id("enable-me-btn"), MEDIUM, POLL);
        String result = elementActions.getText(By.id("enable-status"), SHORT, POLL);
        Assert.assertTrue(result.contains("✅") || result.contains("clicked"),
                "enable-status must confirm click; got: " + result);
    }

    // ── clickOnMultipleElements ───────────────────────────────────────────────

    @Test(groups = "arena")
    public void clickOnMultipleElements_clicksAllMatchingCheckboxes() {
        // The 4 .cb-option labels each toggle a feature checkbox; clicking all of them must enable
        // all 4 and update cb-status from its initial "Nothing checked" state.
        elementActions.clickOnMultipleElements(By.cssSelector(".cb-option"), SHORT, POLL);
        waitActions.waitForTextToBePresentInElement(By.id("cb-status"), "Enabled", SHORT, POLL);
        String status = elementActions.getText(By.id("cb-status"), SHORT, POLL);
        Assert.assertTrue(status.contains("Enabled"),
                "clickOnMultipleElements must toggle the checkboxes (cb-status should report enabled features); got: " + status);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getText / getTextFromMultipleElements
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void getText_returnsElementInnerText() {
        // page-title is a static heading
        String text = elementActions.getText(By.id("page-title"), SHORT, POLL);
        Assert.assertFalse(text.isEmpty(), "getText must return non-empty heading text");
        Assert.assertTrue(text.length() > 3, "Heading text must have meaningful content");
    }

    @Test(groups = "arena")
    public void getText_returnsUpdatedText_afterDomMutation() {
        goTo("waits.html");
        elementActions.clickOnElement(By.id("start-counter"), SHORT, POLL);
        waitActions.waitForTextToBePresentInElement(By.id("polling-counter"), "DONE", LONG, POLL);
        String text = elementActions.getText(By.id("polling-counter"), SHORT, POLL);
        Assert.assertEquals(text, "DONE", "getText must return 'DONE' after counter completes");
    }

    @Test(groups = "arena")
    public void getTextFromMultipleElements_returnsAllTexts() {
        List<String> texts = elementActions.getTextFromMultipleElements(
                By.cssSelector(".card-title"), SHORT, POLL);
        Assert.assertFalse(texts.isEmpty(),
                "getTextFromMultipleElements must return non-empty list");
        texts.forEach(t -> Assert.assertFalse(t.isEmpty(),
                "Every element in the text list must be non-empty"));
    }

    @Test(groups = "arena")
    public void getTextFromMultipleElements_countMatchesRenderedElements() {
        List<String> texts = elementActions.getTextFromMultipleElements(
                By.cssSelector(".radio-option"), SHORT, POLL);
        int directCount = driver.findElements(By.cssSelector(".radio-option")).size();
        Assert.assertEquals(texts.size(), directCount,
                "getTextFromMultipleElements list size must match DOM element count");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getAttributeValue / getPropertyValue
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void getAttributeValue_returnsHrefAttribute() {
        String href = elementActions.getAttributeValue(By.cssSelector("a.nav-link"), "href", SHORT, POLL);
        Assert.assertFalse(href.isEmpty(), "href attribute must not be empty");
        Assert.assertTrue(href.contains("index.html") || href.contains("http"),
                "href must point to a page; got: " + href);
    }

    @Test(groups = "arena")
    public void getAttributeValue_returnsDataAttribute() {
        goTo("state.html");
        elementActions.clickOnElement(By.id("start-cycle"), SHORT, POLL);
        waitActions.waitForElementAttributeToBe(By.id("mutating-el"), "data-state", "active", LONG, POLL);
        String state = elementActions.getAttributeValue(
                By.id("mutating-el"), "data-state", SHORT, POLL);
        Assert.assertEquals(state, "active",
                "data-state attribute must be 'active' after click");
    }

    @Test(groups = "arena")
    public void getPropertyValue_returnsInputTypeProperty() {
        String type = elementActions.getPropertyValue(By.id("js-set-value-target"), "type", SHORT, POLL);
        Assert.assertEquals(type, "text", "Input type property must be 'text'");
    }

    @Test(groups = "arena")
    public void getPropertyValue_returnsCheckboxCheckedProperty() {
        By cb = By.id("screenshots-checkbox");
        // Initially unchecked
        String checked = elementActions.getPropertyValue(cb, "checked", SHORT, POLL);
        Assert.assertEquals(checked, "false", "Unchecked checkbox 'checked' property must be 'false'");
        // Click to check
        elementActions.clickOnElement(cb, SHORT, POLL);
        String checkedAfter = elementActions.getPropertyValue(cb, "checked", SHORT, POLL);
        Assert.assertEquals(checkedAfter, "true",
                "Checked checkbox 'checked' property must be 'true'");
    }

    // ── getAttributeFromMultipleElements / getPropertyFromMultipleElements ────

    @Test(groups = "arena")
    public void getAttributeFromMultipleElements_returnsAllHrefValues() {
        List<String> hrefs = elementActions.getAttributeFromMultipleElements(
                By.cssSelector(".nav-link"), "href", SHORT, POLL);
        Assert.assertFalse(hrefs.isEmpty(), "Must return at least one href value");
        hrefs.forEach(h -> Assert.assertFalse(h == null || h.isEmpty(),
                "Every nav-link href must be non-null and non-empty"));
    }

    @Test(groups = "arena")
    public void getPropertyFromMultipleElements_returnsAllTagNames() {
        List<String> tags = elementActions.getPropertyFromMultipleElements(
                By.cssSelector(".card-title"), "tagName", SHORT, POLL);
        Assert.assertFalse(tags.isEmpty(), "Must return tag names for all section titles");
        tags.forEach(t -> Assert.assertFalse(t.isEmpty(),
                "Every tagName property must be non-empty"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // isElementPresent / isElementDisplayed / isElementEnabled / isElementSelected
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Precision contract for a genuinely ambiguous broken locator: forms.html has TWO email inputs
     * (form-email and email-input), so By.id("email") is under-determined — no tier can know which
     * one is meant. Acceptable outcomes are an honest abstain (throw) or a heal into one of the two
     * real email inputs (Tier 2 shortlist differentiation); healing into any OTHER element is a
     * wrong-heal and fails. Exercises the Tier-1 tie → HealingHints → Tier-2 shortlist handoff.
     */
    @Test(groups = "arena")
    public void sendData_ambiguousLocator_neverHealsToArbitraryElement() {
        String marker = "AmbiguityProbe";
        boolean healed;
        try {
            elementActions.sendData(By.id("email"), marker, SHORT, POLL);
            healed = true;
        } catch (RuntimeException honestAbstain) {
            healed = false;
        }
        if (healed) {
            String formEmail  = elementActions.getPropertyValue(By.id("form-email"),  "value", SHORT, POLL);
            String emailInput = elementActions.getPropertyValue(By.id("email-input"), "value", SHORT, POLL);
            Assert.assertTrue(marker.equals(formEmail) || marker.equals(emailInput),
                    "an ambiguous 'email' heal may only land in one of the two real email inputs; "
                    + "form-email='" + formEmail + "' email-input='" + emailInput + "'");
        }
    }

    @Test(groups = "arena")
    public void isElementPresent_returnsTrueForExistingElement() {
        Assert.assertTrue(elementActions.isElementPresent(By.id("js-set-value-target"), SHORT, POLL),
                "isElementPresent must be true for a DOM element");
    }

    @Test(groups = "arena")
    public void isElementPresent_returnsFalseForNonExistingElement() {
        Assert.assertFalse(elementActions.isElementPresent(By.id("does-not-exist-xyz"), 1, 100),
                "isElementPresent must return false within 1s for non-existent element");
    }

    @Test(groups = "arena")
    public void isElementDisplayed_returnsTrueForVisibleElement() {
        Assert.assertTrue(elementActions.isElementDisplayed(By.id("js-set-value-target"), SHORT, POLL),
                "isElementDisplayed must be true for a visible input");
    }

    @Test(groups = "arena")
    public void isElementDisplayed_returnsFalseForOffscreenElement() {
        goTo("state.html");
        Assert.assertFalse(elementActions.isElementDisplayed(By.id("offscreen-element"), SHORT, POLL),
                "isElementDisplayed must be false for element at left:-9999px");
    }

    @Test(groups = "arena")
    public void isElementEnabled_returnsTrueForNormalInput() {
        Assert.assertTrue(elementActions.isElementEnabled(By.id("js-set-value-target"), SHORT, POLL),
                "isElementEnabled must be true for an enabled input");
    }

    @Test(groups = "arena")
    public void isElementEnabled_returnsFalseForDisabledInput() {
        goTo("state.html");
        Assert.assertFalse(elementActions.isElementEnabled(By.id("disabled-input"), SHORT, POLL),
                "isElementEnabled must be false for a disabled input");
    }

    @Test(groups = "arena")
    public void isElementEnabled_becomesTrueAfterTimerUnlocks() {
        goTo("waits.html");
        Assert.assertFalse(elementActions.isElementEnabled(By.id("enable-me-btn"), SHORT, POLL));
        elementActions.clickOnElement(By.id("trigger-enable-btn"), SHORT, POLL);
        Assert.assertTrue(elementActions.isElementEnabled(By.id("enable-me-btn"), MEDIUM, POLL),
                "isElementEnabled must become true after 5s timer unlocks the button");
    }

    @Test(groups = "arena")
    public void isElementSelected_returnsFalseForUncheckedCheckbox() {
        // agree-checkbox starts unchecked
        Assert.assertFalse(elementActions.isElementSelected(By.id("screenshots-checkbox"), SHORT, POLL),
                "Unchecked checkbox isElementSelected must be false");
    }

    @Test(groups = "arena")
    public void isElementSelected_returnsTrueAfterCheckboxClick() {
        elementActions.clickOnElement(By.id("screenshots-checkbox"), SHORT, POLL);
        Assert.assertTrue(elementActions.isElementSelected(By.id("screenshots-checkbox"), SHORT, POLL),
                "After clicking, isElementSelected must return true");
    }

    // ── isElementClickable ────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void isElementClickable_returnsTrueForEnabledVisibleButton() {
        Assert.assertTrue(elementActions.isElementClickable(
                By.id("form-submit-btn"), SHORT, POLL),
                "Enabled visible button must be clickable");
    }

    @Test(groups = "arena")
    public void isElementClickable_returnsFalseForDisabledButton() {
        goTo("state.html");
        Assert.assertFalse(elementActions.isElementClickable(
                By.id("disabled-input"), SHORT, POLL),
                "Disabled element must not be clickable");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // clearElement
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void clearElement_removesTextFromInput() {
        elementActions.sendData(By.id("js-set-value-target"), "ClearMe", SHORT, POLL);
        Assert.assertEquals(
                elementActions.getPropertyValue(By.id("js-set-value-target"), "value", SHORT, POLL),
                "ClearMe", "Pre-condition: field must contain 'ClearMe'");

        elementActions.clearElement(By.id("js-set-value-target"), SHORT, POLL);
        String after = elementActions.getPropertyValue(By.id("js-set-value-target"), "value", SHORT, POLL);
        Assert.assertEquals(after, "",
                "clearElement must leave field empty; got: '" + after + "'");
    }

    @Test(groups = "arena")
    public void clearElement_onTextarea_removesContent() {
        elementActions.sendData(By.id("textarea-input"), "Initial content", SHORT, POLL);
        elementActions.clearElement(By.id("textarea-input"), SHORT, POLL);
        String val = elementActions.getPropertyValue(By.id("textarea-input"), "value", SHORT, POLL);
        Assert.assertEquals(val, "", "clearElement must empty the textarea");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // scrollIntoView
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void scrollIntoView_bringsElementIntoViewport_thenClickable() {
        goTo("mouse.html");
        elementActions.scrollIntoView(By.id("scroll-target-btn"), SHORT, POLL);
        // After scroll, Ellithium click must succeed without separate scrollByOffset
        elementActions.clickOnElement(By.id("scroll-target-btn"), SHORT, POLL);
        String status = elementActions.getText(By.id("scroll-status"), SHORT, POLL);
        Assert.assertTrue(status.contains("✅") || status.contains("clicked"),
                "scroll-target-status must confirm click after scrollIntoView; got: " + status);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // isTextContains / isTextEqual / isAttributeContains
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void isTextContains_trueWhenSubstringPresent() {
        Assert.assertTrue(
                elementActions.isTextContains(By.id("page-title"), "Form", SHORT, POLL),
                "isTextContains must be true when heading contains 'Form'");
    }

    @Test(groups = "arena")
    public void isTextContains_falseWhenSubstringAbsent() {
        Assert.assertFalse(
                elementActions.isTextContains(By.id("page-title"), "NONEXISTENT_XYZ", SHORT, POLL),
                "isTextContains must be false for non-existent substring");
    }

    @Test(groups = "arena")
    public void isTextEqual_trueWhenTextMatchesExactly() {
        // native-select first option text is "-- Select Framework --"
        String label = elementActions.getText(By.id("multi-status"), SHORT, POLL);
        Assert.assertTrue(
                elementActions.isTextEqual(By.id("multi-status"), label, SHORT, POLL),
                "isTextEqual must be true for exact text match");
    }

    @Test(groups = "arena")
    public void isTextEqual_falseWhenTextDiffers() {
        Assert.assertFalse(
                elementActions.isTextEqual(By.id("page-title"), "COMPLETELY_WRONG_TEXT", SHORT, POLL),
                "isTextEqual must be false when text does not match");
    }

    @Test(groups = "arena")
    public void isAttributeContains_trueWhenAttrSubstringPresent() {
        // text-input has class "form-control" or similar
        Assert.assertTrue(
                elementActions.isAttributeContains(
                        By.id("js-set-value-target"), "type", "text", SHORT, POLL),
                "isAttributeContains must be true when type attribute contains 'text'");
    }

    @Test(groups = "arena")
    public void isAttributeContains_falseWhenAttrSubstringAbsent() {
        Assert.assertFalse(
                elementActions.isAttributeContains(
                        By.id("js-set-value-target"), "type", "password", SHORT, POLL),
                "isAttributeContains must be false when type attribute does not contain 'password'");
    }

    @Test(groups = "arena")
    public void isAttributeContains_onDataAttr_reflectsMutation() {
        goTo("state.html");
        // Before clicking
        Assert.assertFalse(
                elementActions.isAttributeContains(
                        By.id("mutating-el"), "data-state", "active", SHORT, POLL),
                "data-state should not contain 'active' before click");
        elementActions.clickOnElement(By.id("start-cycle"), SHORT, POLL);
        waitActions.waitForElementAttributeToBe(By.id("mutating-el"), "data-state", "active", LONG, POLL);
        Assert.assertTrue(
                elementActions.isAttributeContains(
                        By.id("mutating-el"), "data-state", "active", SHORT, POLL),
                "data-state must contain 'active' after click triggers the mutation");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // uploadFile — forms.html
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void uploadFile_setsFileInputPath_verifiedByPropertyValue() throws IOException {
        File tmp = File.createTempFile("arena-el-upload-", ".txt");
        tmp.deleteOnExit();
        Files.writeString(tmp.toPath(), "Ellithium ElementActions upload test");

        elementActions.uploadFile(By.id("file-upload"), tmp.getAbsolutePath(), SHORT, POLL);
        String value = elementActions.getPropertyValue(By.id("file-upload"), "value", SHORT, POLL);
        Assert.assertTrue(value.contains("arena-el-upload-") || value.contains(tmp.getName()),
                "File input value must contain uploaded filename after uploadFile; got: " + value);
    }

    @Test(groups = "arena", expectedExceptions = IllegalArgumentException.class)
    public void uploadFile_throwsIllegalArgument_forNonExistentFile() {
        elementActions.uploadFile(By.id("file-upload"), "/path/does/not/exist.pdf", SHORT, POLL);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Default timeout overloads — spot check
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void defaultOverloads_sendData_getText_workWithoutExplicitTimeout() {
        // These use WaitManager.getDefaultTimeout() internally
        elementActions.sendData(By.id("js-set-value-target"), "DefaultTimeout");
        String value = elementActions.getPropertyValue(By.id("js-set-value-target"), "value", SHORT, POLL);
        Assert.assertEquals(value, "DefaultTimeout");

        elementActions.clearElement(By.id("js-set-value-target"));
        String cleared = elementActions.getPropertyValue(By.id("js-set-value-target"), "value", SHORT, POLL);
        Assert.assertEquals(cleared, "");

        Assert.assertTrue(elementActions.isElementPresent(By.id("js-set-value-target")));
        Assert.assertTrue(elementActions.isElementDisplayed(By.id("js-set-value-target")));
        Assert.assertTrue(elementActions.isElementEnabled(By.id("js-set-value-target")));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Edge cases — stale, contenteditable, dynamic injection
    // ═══════════════════════════════════════════════════════════════════════

    @Test(groups = "arena")
    public void sendData_onContentEditableDiv_setsText() {
        elementActions.sendData(By.id("content-editable"), "ContentEditableText", SHORT, POLL);
        String text = elementActions.getText(By.id("content-editable"), SHORT, POLL);
        Assert.assertTrue(text.contains("ContentEditableText"),
                "sendData on contenteditable div must set the text; got: " + text);
    }

    @Test(groups = "arena")
    public void isElementPresent_returnsTrueForDynamicallyInjectedElement() {
        goTo("waits.html");
        // Before injection the placeholder is shown; the form is injected only after the 5s spinner.
        Assert.assertTrue(elementActions.isElementPresent(By.id("no-form-placeholder"), SHORT, POLL),
                "placeholder must be present before the spinner injects the form");
        elementActions.clickOnElement(By.id("trigger-spinner-btn"), SHORT, POLL);
        boolean present = elementActions.isElementPresent(By.id("injected-form"), LONG, POLL);
        Assert.assertTrue(present,
                "isElementPresent must be true once spinner completes injection");
    }

    @Test(groups = "arena")
    public void getText_onInjectedElement_returnsCorrectText() {
        goTo("waits.html");
        elementActions.clickOnElement(By.id("trigger-spinner-btn"), SHORT, POLL);
        // near-miss id → opt the wait into healing so it resolves the injected submit button
        waitActions.waitForElementPresence(By.id("injected-submit-btn"), LONG, POLL, true);
        String text = elementActions.getText(By.id("injected-submit-btn"), SHORT, POLL);
        Assert.assertFalse(text.isEmpty(), "getText on injected heading must return non-empty text");
    }

    @Test(groups = "arena")
    public void isElementDisplayed_onHiddenElement_returnsFalse_thenTrueAfterShow() {
        goTo("state.html");
        Assert.assertFalse(
                elementActions.isElementDisplayed(By.id("offscreen-element"), SHORT, POLL),
                "hidden-reveal-el must be hidden initially");
        elementActions.clickOnElement(By.id("reveal-offscreen-btn"), SHORT, POLL);
        Assert.assertTrue(
                elementActions.isElementDisplayed(By.id("offscreen-element"), SHORT, POLL),
                "hidden-reveal-el must be visible after show button click");
    }

    @Test(groups = "arena")
    public void isTextContains_onDynamicallyChangingCounter_detectsDone() {
        goTo("waits.html");
        elementActions.clickOnElement(By.id("start-counter-btn"), SHORT, POLL);
        waitActions.waitForTextToBePresentInElement(By.id("polling-counter"), "DONE", LONG, POLL);
        Assert.assertTrue(
                elementActions.isTextContains(By.id("polling-counter"), "DONE", SHORT, POLL),
                "isTextContains must detect 'DONE' in polling-counter after counter completes");
    }
}
