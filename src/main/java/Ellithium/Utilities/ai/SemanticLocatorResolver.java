package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.models.ElementFingerprint;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.support.locators.RelativeLocator;

import java.util.ArrayList;
import java.util.List;

/**
 * Tier 1.5 — Semantic Strategy Search.
 *
 * <p>When a locator fails and baseline fingerprinting can't heal it,
 * this resolver uses the <b>semantic meaning</b> of the element (extracted
 * from method name, field name, and action type) to generate and try
 * dozens of locator strategies algorithmically — zero LLM cost.</p>
 *
 * <p>For example, if a method called {@code setUserEmail()} has a broken
 * {@code By.id("email")} locator, this resolver will:</p>
 * <ol>
 *   <li>Extract the semantic name: {@code "Email"}</li>
 *   <li>Detect the element category from the action: {@code sendData → INPUT}</li>
 *   <li>Generate ~30 XPath/CSS strategies targeting inputs with "Email" in
 *       their placeholder, aria-label, id, name, or nearby text</li>
 *   <li>Try each strategy against the live DOM</li>
 *   <li>Cross-validate against the stored baseline fingerprint</li>
 * </ol>
 *
 * <p>Includes Selenium 4 {@link RelativeLocator} strategies for web drivers
 * (skipped for Appium since RelativeLocator requires a Chromium backend).</p>
 */
public class SemanticLocatorResolver {

    // ──────────────────────── Element Categories ────────────────────────

    /**
     * The type of UI element we're searching for, inferred from the Ellithium
     * action method (sendData, clickOnElement, etc.).
     */
    public enum ElementCategory {
        INPUT,      // sendData, type, enterText → input, textarea
        CLICKABLE,  // click, tap, press → button, a, role=button
        SELECT,     // selectDropdown → select
        READABLE,   // getText, getTitle → any visible text-bearing element
        ANY         // fallback
    }

    // ──────────────────────── Main Entry Point ────────────────────────

    /**
     * Attempts to find the broken element using semantic strategies.
     *
     * @param driver     The WebDriver instance
     * @param methodName The POM method name (e.g., "setUserEmail")
     * @param fieldName  The locator field name (e.g., "emailField"), may be null
     * @param actionType The Ellithium action (e.g., "sendData", "clickOnElement")
     * @param locatorValue The broken locator's original value (e.g., "email_input")
     * @param baseline   The stored fingerprint for cross-validation, may be null
     * @return The matched WebElement, or null if no confident match was found
     */
    public static WebElement trySemanticHeal(WebDriver driver,
                                             String methodName, String fieldName,
                                             String actionType, String locatorValue,
                                             ElementFingerprint baseline) {
        // Extract semantic names from context
        List<String> semanticNames = SemanticNameExtractor.extract(methodName, fieldName, locatorValue);

        if (semanticNames.isEmpty()) {
            Reporter.log("Tier 1.5: No semantic names could be extracted — skipping", LogLevel.DEBUG);
            return null;
        }

        ElementCategory category = categorizeAction(actionType);
        boolean isMobile = driver instanceof AppiumDriver;

        Reporter.log("Tier 1.5: Semantic search — names=" + semanticNames
                + " category=" + category + " mobile=" + isMobile, LogLevel.DEBUG);

        // Build all locator strategies for this category + semantic names
        List<LocatorAttempt> attempts = buildStrategies(semanticNames, category, isMobile, driver);

        // Suppress selenium listener to avoid logging every findElement attempt
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            for (LocatorAttempt attempt : attempts) {
                try {
                    WebElement found = driver.findElement(attempt.locator);

                    // Cross-validate against baseline if available
                    if (baseline != null) {
                        double similarity = baseline.scoreSimilarity(found);
                        if (similarity < 0.25) {
                            continue; // Wrong element — skip
                        }
                    }

                    // Re-enable logging for the success message
                    Ellithium.core.execution.listener.seleniumListener.resumeLogging();

                    // Reconstruct the simplest possible locator from the matched element
                    By cleanLocator = ElementFingerprint.reconstructLocator(found);
                    String cleanLocatorStr = (cleanLocator != null) ? cleanLocator.toString() : attempt.locator.toString();

                    Reporter.log("Tier 1.5: MATCH via " + attempt.description
                            + " | locator=" + cleanLocatorStr, LogLevel.INFO_GREEN);

                    // Queue for healing report (BaselineStore capture is done by caller in BaseActions)
                    AIHealingReporter.queueChange(
                            "semantic-strategy", cleanLocatorStr,
                            new Ellithium.Utilities.ai.models.HealingResult(
                                    cleanLocatorStr, 0.85,
                                    "[TIER 1.5 - Semantic] " + attempt.description),
                            null, methodName, actionType, 0);

                    return found;
                } catch (NoSuchElementException | InvalidSelectorException ignored) {
                    // Strategy didn't match — try next
                }
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }

        Reporter.log("Tier 1.5: No match found across " + attempts.size()
                + " strategies — falling through to Tier 2 (LLM)", LogLevel.DEBUG);
        return null;
    }

    // ──────────────────────── Strategy Building ────────────────────────

    /**
     * Generates all applicable locator strategies for the given semantic names
     * and element category.
     */
    private static List<LocatorAttempt> buildStrategies(List<String> names,
                                                        ElementCategory category,
                                                        boolean isMobile,
                                                        WebDriver driver) {
        List<LocatorAttempt> strategies = new ArrayList<>();

        for (String name : names) {
            if (name.isBlank()) continue;
            String lower = name.toLowerCase();

            switch (category) {
                case INPUT:
                    addInputStrategies(strategies, name, lower);
                    if (!isMobile) addRelativeInputStrategies(strategies, name, driver);
                    break;
                case CLICKABLE:
                    addClickableStrategies(strategies, name, lower);
                    break;
                case SELECT:
                    addSelectStrategies(strategies, name, lower);
                    break;
                case READABLE:
                    addReadableStrategies(strategies, name, lower);
                    break;
                case ANY:
                    addUniversalStrategies(strategies, name, lower);
                    break;
            }

            // Universal strategies apply to all categories
            addUniversalStrategies(strategies, name, lower);
        }

        return strategies;
    }

    // ──────────────────────── INPUT Strategies ────────────────────────

    private static void addInputStrategies(List<LocatorAttempt> out, String name, String lower) {
        // Input by attribute match
        out.add(attempt(
                By.xpath(multiCaseXpath("input", "placeholder", name)),
                "input[placeholder~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("input", "aria-label", name)),
                "input[aria-label~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("input", "id", name)),
                "input[id~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("input", "name", name)),
                "input[name~='" + name + "']"));

        // Textarea variants
        out.add(attempt(
                By.xpath(multiCaseXpath("textarea", "placeholder", name)),
                "textarea[placeholder~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("textarea", "aria-label", name)),
                "textarea[aria-label~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("textarea", "id", name)),
                "textarea[id~='" + name + "']"));

        // Input as child of a label or container with matching text
        out.add(attempt(
                By.xpath(childOfTextContainer("input", name)),
                "input inside element with text '" + name + "'"));

        // Input following a label/text with the semantic name
        out.add(attempt(
                By.xpath(followingSibling("input", name)),
                "first input after text '" + name + "'"));

        // Input preceding a label/text with the semantic name
        out.add(attempt(
                By.xpath(precedingSibling("input", name)),
                "first input before text '" + name + "'"));
    }

    /**
     * Adds Selenium 4 RelativeLocator strategies for input elements.
     * These are spatial strategies — "find input to the right of label 'Email'".
     * Only works on web (Chromium-based drivers), skipped for Appium.
     */
    private static void addRelativeInputStrategies(List<LocatorAttempt> out, String name, WebDriver driver) {
        try {
            // Find any label/text element containing the semantic name
            By textAnchor = By.xpath("//*[contains(normalize-space(text()),'" + name + "')]");
            // Look for an input element spatially near that text
            out.add(attempt(
                    RelativeLocator.with(By.tagName("input")).near(textAnchor, 150),
                    "input near text '" + name + "' (RelativeLocator)"));
            out.add(attempt(
                    RelativeLocator.with(By.tagName("input")).toRightOf(textAnchor),
                    "input right-of text '" + name + "' (RelativeLocator)"));
            out.add(attempt(
                    RelativeLocator.with(By.tagName("input")).below(textAnchor),
                    "input below text '" + name + "' (RelativeLocator)"));
        } catch (Exception ignored) {
            // RelativeLocator may fail on non-Chromium browsers — silently skip
        }
    }

    // ──────────────────────── CLICKABLE Strategies ────────────────────────

    private static void addClickableStrategies(List<LocatorAttempt> out, String name, String lower) {
        // Button by text content
        out.add(attempt(
                By.xpath(textContains("button", name)),
                "button with text '" + name + "'"));
        // Button by attributes
        out.add(attempt(
                By.xpath(multiCaseXpath("button", "id", name)),
                "button[id~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("button", "name", name)),
                "button[name~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("button", "aria-label", name)),
                "button[aria-label~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("button", "title", name)),
                "button[title~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("button", "value", name)),
                "button[value~='" + name + "']"));

        // Anchor/link by text and attributes
        out.add(attempt(
                By.xpath(textContains("a", name)),
                "link with text '" + name + "'"));
        out.add(attempt(
                By.xpath(multiCaseXpath("a", "id", name)),
                "a[id~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("a", "name", name)),
                "a[name~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("a", "title", name)),
                "a[title~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("a", "aria-label", name)),
                "a[aria-label~='" + name + "']"));

        // Submit/button-type inputs
        out.add(attempt(
                By.xpath(inputTypeWithAttr("submit", "value", name)),
                "input[type=submit][value~='" + name + "']"));
        out.add(attempt(
                By.xpath(inputTypeWithAttr("button", "value", name)),
                "input[type=button][value~='" + name + "']"));
        out.add(attempt(
                By.xpath(inputTypeWithAttr("image", "alt", name)),
                "input[type=image][alt~='" + name + "']"));

        // ARIA role-based clickables
        out.add(attempt(
                By.xpath(roleWithText("button", name)),
                "[role=button] with text '" + name + "'"));
        out.add(attempt(
                By.xpath(roleWithText("link", name)),
                "[role=link] with text '" + name + "'"));
        out.add(attempt(
                By.xpath(roleWithText("tab", name)),
                "[role=tab] with text '" + name + "'"));
        out.add(attempt(
                By.xpath(roleWithText("menuitem", name)),
                "[role=menuitem] with text '" + name + "'"));

        // Clickable element with exact text match
        out.add(attempt(
                By.xpath("//*[normalize-space(text())='" + name
                        + "' and (@role='button' or @type='submit' or self::button or self::a)]"),
                "exact text clickable '" + name + "'"));

        // Element followed by a button/link
        out.add(attempt(
                By.xpath(followingSibling("button", name)),
                "first button after text '" + name + "'"));
        out.add(attempt(
                By.xpath(followingSibling("a", name)),
                "first link after text '" + name + "'"));
    }

    // ──────────────────────── SELECT Strategies ────────────────────────

    private static void addSelectStrategies(List<LocatorAttempt> out, String name, String lower) {
        out.add(attempt(
                By.xpath(multiCaseXpath("select", "id", name)),
                "select[id~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("select", "name", name)),
                "select[name~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("select", "aria-label", name)),
                "select[aria-label~='" + name + "']"));
        // Select following a label with matching text
        out.add(attempt(
                By.xpath("//label[contains(normalize-space(text()),'" + name + "')]/following::select[1]"),
                "select after label '" + name + "'"));
        out.add(attempt(
                By.xpath(childOfTextContainer("select", name)),
                "select inside element with text '" + name + "'"));
    }

    // ──────────────────────── READABLE Strategies ────────────────────────

    private static void addReadableStrategies(List<LocatorAttempt> out, String name, String lower) {
        // Any element with matching text
        out.add(attempt(
                By.xpath("//*[normalize-space(text())='" + name + "']"),
                "element with exact text '" + name + "'"));
        out.add(attempt(
                By.xpath("//*[contains(normalize-space(text()),'" + name + "')]"),
                "element containing text '" + name + "'"));
        // Labeled outputs
        out.add(attempt(
                By.xpath(multiCaseXpath("span", "id", name)),
                "span[id~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("div", "id", name)),
                "div[id~='" + name + "']"));
        out.add(attempt(
                By.xpath(multiCaseXpath("p", "id", name)),
                "p[id~='" + name + "']"));
    }

    // ──────────────────────── UNIVERSAL Strategies ────────────────────────

    private static void addUniversalStrategies(List<LocatorAttempt> out, String name, String lower) {
        // data-testid — the gold standard for test automation
        out.add(attempt(
                By.cssSelector("[data-testid*='" + lower + "']"),
                "[data-testid*='" + lower + "']"));
        // data-test, data-qa (common variants)
        out.add(attempt(
                By.cssSelector("[data-test*='" + lower + "']"),
                "[data-test*='" + lower + "']"));
        out.add(attempt(
                By.cssSelector("[data-qa*='" + lower + "']"),
                "[data-qa*='" + lower + "']"));
        // aria-label on any element
        out.add(attempt(
                By.xpath("//*[contains(@aria-label,'" + name + "')]"),
                "*[aria-label*='" + name + "']"));
        // title attribute
        out.add(attempt(
                By.xpath("//*[contains(@title,'" + name + "')]"),
                "*[title*='" + name + "']"));
    }

    // ──────────────────────── XPath Generators ────────────────────────

    /**
     * Generates an XPath that checks an attribute against multiple case variants
     * of the semantic name using the contains() function.
     *
     * Example output for ("input", "placeholder", "Email"):
     * //input[contains(@placeholder,'Email') or contains(@placeholder,'email') or contains(@placeholder,'EMAIL')]
     */
    private static String multiCaseXpath(String tag, String attr, String name) {
        String lower = name.toLowerCase();
        String upper = name.toUpperCase();
        String capitalized = Character.toUpperCase(name.charAt(0))
                + (name.length() > 1 ? name.substring(1).toLowerCase() : "");

        return "//" + tag + "[contains(@" + attr + ",'" + name + "')"
                + " or contains(@" + attr + ",'" + lower + "')"
                + " or contains(@" + attr + ",'" + upper + "')"
                + " or contains(@" + attr + ",'" + capitalized + "')]";
    }

    /**
     * Generates XPath to find an element by text content with case variants.
     * Example: //button[contains(normalize-space(text()),'Login') or ...]
     */
    private static String textContains(String tag, String name) {
        String lower = name.toLowerCase();
        return "//" + tag + "[contains(normalize-space(text()),'" + name + "')"
                + " or contains(normalize-space(text()),'" + lower + "')]";
    }

    /**
     * Generates XPath for element as child of a container with matching text.
     * Example: (//*[contains(text(),'Email')]/input)[1]
     */
    private static String childOfTextContainer(String tag, String name) {
        return "(//*[contains(normalize-space(text()),'" + name + "')]/" + tag + ")[1]";
    }

    /**
     * Generates XPath for the first element of a tag type that follows text with the name.
     * Example: (//*[contains(text(),'Email')]/following::input)[1]
     */
    private static String followingSibling(String tag, String name) {
        return "(//*[contains(normalize-space(text()),'" + name + "')]/following::" + tag + ")[1]";
    }

    /**
     * Generates XPath for the first element of a tag type that precedes text with the name.
     */
    private static String precedingSibling(String tag, String name) {
        return "(//*[contains(normalize-space(text()),'" + name + "')]/preceding::" + tag + ")[1]";
    }

    /**
     * Generates XPath for input[type=X][attr contains name].
     */
    private static String inputTypeWithAttr(String type, String attr, String name) {
        String lower = name.toLowerCase();
        return "//input[@type='" + type + "' and (contains(@" + attr + ",'" + name + "')"
                + " or contains(@" + attr + ",'" + lower + "'))]";
    }

    /**
     * Generates XPath for role-based elements with matching text content.
     */
    private static String roleWithText(String role, String name) {
        String lower = name.toLowerCase();
        return "//*[@role='" + role + "' and (contains(normalize-space(text()),'" + name + "')"
                + " or contains(normalize-space(text()),'" + lower + "'))]";
    }

    // ──────────────────────── Action Categorization ────────────────────────

    /**
     * Maps Ellithium action method names to element categories.
     * These are the REAL method names from Ellithium's interaction classes:
     * ElementActions, SelectActions, MouseActions, JavaScriptActions, MobileActions.
     */
    static ElementCategory categorizeAction(String actionType) {
        if (actionType == null || actionType.equals("unknown")) return ElementCategory.ANY;

        return switch (actionType) {
            // ElementActions — text input
            case "sendData", "clearElement",
                 // JavaScriptActions — value setting
                 "setElementValueUsingJS",
                 // ElementActions — file upload (targets input[type=file])
                 "uploadFile", "uploadMultipleFiles", "uploadFileUsingJS"
                    -> ElementCategory.INPUT;

            // ElementActions — clicking
            case "clickOnElement", "clickOnMultipleElements",
                 // MouseActions — mouse interactions
                 "hoverOverElement", "hoverAndClick", "doubleClick", "rightClick",
                 "dragAndDrop", "dragAndDropByOffset",
                 // JavaScriptActions — JS click
                 "javascriptClick",
                 // MobileActions — touch interactions
                 "tap", "doubleTap", "longPress", "twoFingerTap"
                    -> ElementCategory.CLICKABLE;

            // SelectActions — dropdown operations
            case "selectDropdownByText", "selectDropdownByValue", "selectDropdownByIndex",
                 "deselectAll", "deselectDropdownByText", "deselectDropdownByValue",
                 "deselectDropdownByIndex", "selectDropdownByTextForMultipleElements"
                    -> ElementCategory.SELECT;

            // ElementActions — reading element state/text
            case "getText", "getAttributeValue", "getPropertyValue",
                 "isTextContains", "isAttributeContains",
                 // ElementActions — element state checks
                 "isElementPresent", "isElementDisplayed", "isElementEnabled",
                 "isElementSelected", "isElementClickable"
                    -> ElementCategory.READABLE;

            // ElementActions & MouseActions — scrolling / navigation (element could be anything)
            case "scrollIntoView", "scrollToElement",
                 "moveSliderTo", "moveSliderByOffset",
                 // MobileActions — gestures
                 "swipe", "scroll", "scrollToElementBySelector",
                 "drag", "pinch", "fling"
                    -> ElementCategory.ANY;

            default -> ElementCategory.ANY;
        };
    }

    // ──────────────────────── Internal Model ────────────────────────

    private static LocatorAttempt attempt(By locator, String description) {
        return new LocatorAttempt(locator, description);
    }

    private static class LocatorAttempt {
        final By locator;
        final String description;

        LocatorAttempt(By locator, String description) {
            this.locator = locator;
            this.description = description;
        }
    }
}
