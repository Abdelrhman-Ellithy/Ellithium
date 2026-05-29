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
 * Tier 2 — Semantic Strategy Search.
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
    /** Side-effect-free version used by the Tier 2⊕3 ensemble: same matching logic, but does NOT
     *  queue an AIHealingReporter entry or record HealingTelemetryStore — those are the caller's
     *  responsibility once the ENSEMBLE decides this element is the final winner. */
    public static WebElement peekSemanticMatch(WebDriver driver,
                                               String methodName, String fieldName,
                                               String actionType, String locatorValue,
                                               ElementFingerprint baseline) {
        return trySemanticHeal(driver, methodName, fieldName, actionType, locatorValue, baseline, false);
    }

    public static WebElement trySemanticHeal(WebDriver driver,
                                             String methodName, String fieldName,
                                             String actionType, String locatorValue,
                                             ElementFingerprint baseline) {
        return trySemanticHeal(driver, methodName, fieldName, actionType, locatorValue, baseline, true);
    }

    private static WebElement trySemanticHeal(WebDriver driver,
                                              String methodName, String fieldName,
                                              String actionType, String locatorValue,
                                              ElementFingerprint baseline,
                                              boolean recordSideEffects) {
        if (locatorValue != null && !locatorValue.isBlank()) {
            By brokenBy = rebuildLocator(locatorValue);
            if (brokenBy != null) {
                WebElement mutationMatch = LocatorMutationEngine.tryMutations(brokenBy, driver, baseline);
                if (mutationMatch != null) {
                    if (recordSideEffects) {
                        By cleanLocator = ElementFingerprint.reconstructLocator(mutationMatch);
                        String cleanLocatorStr = cleanLocator != null ? cleanLocator.toString() : brokenBy.toString();
                        Reporter.log("Tier 2 [Mutation]: MATCH via mutation pre-pass | locator=" + cleanLocatorStr, LogLevel.INFO_GREEN);
                        AIHealingReporter.queueChange("semantic-mutation", cleanLocatorStr,
                                new Ellithium.Utilities.ai.models.HealingResult(cleanLocatorStr, 0.90, "[TIER 2 - Mutation]"),
                                null, methodName, actionType, 0);
                        HealingTelemetryStore.record(2, locatorValue, cleanLocatorStr, 0.90, true);
                    }
                    return mutationMatch;
                }
            }
        }

        double cvThreshold;
        if (baseline == null) {
            cvThreshold = 0.0;
        } else if (baseline.getId() != null || baseline.getDataTestId() != null) {
            cvThreshold = 0.35;
        } else {
            cvThreshold = 0.25;
        }

        ElementCategory category = categorizeAction(actionType);
        boolean isMobile = driver instanceof AppiumDriver;

        String semanticMethodName = (category == ElementCategory.READABLE) ? null : methodName;
        List<String> semanticNames = SemanticNameExtractor.extract(semanticMethodName, fieldName, locatorValue);

        if (semanticNames.isEmpty()) {
            Reporter.log("Tier 2: No semantic names could be extracted — skipping", LogLevel.DEBUG);
            return null;
        }

        Reporter.log("Tier 2: Semantic search — names=" + semanticNames
                + " category=" + category + " mobile=" + isMobile + " cvThreshold=" + cvThreshold, LogLevel.DEBUG);

        List<LocatorAttempt> attempts = buildStrategies(semanticNames, category, isMobile, driver, baseline);

        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            for (LocatorAttempt attempt : attempts) {
                try {
                    WebElement found = driver.findElement(attempt.locator);

                    if (category == ElementCategory.READABLE) {
                        try { if (!found.isDisplayed()) continue; } catch (Exception ignored2) {}
                    }

                    if (baseline != null) {
                        double similarity = baseline.scoreSimilarity(found);
                        if (similarity < cvThreshold) {
                            continue; // Wrong element — skip
                        }
                    }

                    if (recordSideEffects) {
                        Ellithium.core.execution.listener.seleniumListener.resumeLogging();

                        By cleanLocator = ElementFingerprint.reconstructLocator(found);
                        String cleanLocatorStr = (cleanLocator != null) ? cleanLocator.toString() : attempt.locator.toString();

                        Reporter.log("Tier 2: MATCH via " + attempt.description
                                + " | locator=" + cleanLocatorStr, LogLevel.INFO_GREEN);

                        AIHealingReporter.queueChange(
                                "semantic-strategy", cleanLocatorStr,
                                new Ellithium.Utilities.ai.models.HealingResult(
                                        cleanLocatorStr, 0.85,
                                        "[TIER 2 - Semantic] " + attempt.description),
                                null, methodName, actionType, 0);

                        String brokenStr = (baseline != null) ? baseline.getLocatorKey() : attempt.locator.toString();
                        HealingTelemetryStore.record(2, brokenStr, cleanLocatorStr, 0.85, true);
                    }
                    return found;
                } catch (NoSuchElementException | InvalidSelectorException ignored) {
                }
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }

        if (recordSideEffects) {
            Reporter.log("Tier 2: No match found across " + attempts.size()
                    + " strategies — falling through to Tier 4 (LLM)", LogLevel.DEBUG);
            HealingTelemetryStore.record(2, locatorValue, null, 0.0, false);
        }
        return null;
    }

    /**
     * Generates all applicable locator strategies for the given semantic names
     * and element category.
     */
    private static List<LocatorAttempt> buildStrategies(List<String> names,
                                                        ElementCategory category,
                                                        boolean isMobile,
                                                        WebDriver driver,
                                                        ElementFingerprint baseline) {
        String scopePrefix = (baseline != null && baseline.getParentTag() != null)
                ? "//" + baseline.getParentTag() + "/"
                : "//";

        List<LocatorAttempt> gold   = new ArrayList<>();  // data-testid/cy/e2e/automation attrs
        List<LocatorAttempt> silver = new ArrayList<>();  // exact id/name/aria-label
        List<LocatorAttempt> bronze = new ArrayList<>();  // contains/semantic inference
        List<LocatorAttempt> iron   = new ArrayList<>();  // structural/relational

        for (String name : names) {
            if (name.isBlank()) continue;
            String lower = name.toLowerCase();

            addTestAttrStrategies(gold, lower);

            addSilverStrategies(silver, name, lower, scopePrefix);

            switch (category) {
                case INPUT:
                    addInputStrategies(bronze, name, lower, scopePrefix, driver);
                    if (!isMobile) addRelativeInputStrategies(iron, name, driver);
                    break;
                case CLICKABLE:
                    addClickableStrategies(bronze, name, lower, scopePrefix);
                    break;
                case SELECT:
                    addSelectStrategies(bronze, name, lower, scopePrefix);
                    break;
                case READABLE:
                    addReadableStrategies(bronze, name, lower);
                    break;
                case ANY:
                    addBronzeUniversalStrategies(bronze, name, lower, scopePrefix);
                    break;
            }

            if (category != ElementCategory.ANY) {
                addBronzeUniversalStrategies(bronze, name, lower, scopePrefix);
            }
        }

        List<LocatorAttempt> ordered = new ArrayList<>();
        ordered.addAll(gold);
        ordered.addAll(silver);
        ordered.addAll(bronze);
        ordered.addAll(iron);

        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        List<LocatorAttempt> deduped = new ArrayList<>();
        for (LocatorAttempt a : ordered) {
            if (seen.add(a.locator.toString())) deduped.add(a);
        }

        return deduped;
    }

    private static void addTestAttrStrategies(List<LocatorAttempt> out, String lower) {
        out.add(attempt(By.cssSelector("[data-testid='"  + lower + "']"), "[data-testid='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-testid*='" + lower + "']"), "[data-testid*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-test='"    + lower + "']"), "[data-test='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-test*='"   + lower + "']"), "[data-test*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-qa='"      + lower + "']"), "[data-qa='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-qa*='"     + lower + "']"), "[data-qa*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-cy='"            + lower + "']"), "[data-cy='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-cy*='"           + lower + "']"), "[data-cy*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-e2e='"           + lower + "']"), "[data-e2e='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-e2e*='"          + lower + "']"), "[data-e2e*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-automation='"    + lower + "']"), "[data-automation='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-automation*='"   + lower + "']"), "[data-automation*='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-automation-id='" + lower + "']"), "[data-automation-id='" + lower + "']"));
        out.add(attempt(By.cssSelector("[data-automation-id*='"+ lower + "']"), "[data-automation-id*='" + lower + "']"));
    }

    private static void addSilverStrategies(List<LocatorAttempt> out, String name, String lower, String scopePrefix) {
        out.add(attempt(By.id(name),  "id='" + name + "' (exact)"));
        out.add(attempt(By.id(lower), "id='" + lower + "' (exact lower)"));
        out.add(attempt(By.cssSelector("[aria-label='" + name  + "']"), "aria-label='" + name + "'"));
        out.add(attempt(By.cssSelector("[aria-label='" + lower + "']"), "aria-label='" + lower + "'"));
        out.add(attempt(By.cssSelector("[formcontrolname='" + lower + "']"), "formcontrolname='" + lower + "'"));
        out.add(attempt(By.cssSelector("[formcontrolname*='" + lower + "']"), "formcontrolname*='" + lower + "'"));
        out.add(attempt(By.cssSelector("[ng-model*='" + lower + "']"), "ng-model*='" + lower + "'"));
    }

    private static void addInputStrategies(List<LocatorAttempt> out, String name, String lower,
                                            String scopePrefix, WebDriver driver) {
        out.add(attempt(By.xpath(scopePrefix + "input[@placeholder='" + name  + "']"), "input[placeholder='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@placeholder='" + lower + "']"), "input[placeholder='" + lower + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@name='" + name  + "']"), "input[name='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@name='" + lower + "']"), "input[name='" + lower + "'] exact"));

        out.add(attempt(By.xpath(multiCaseXpath("input", "placeholder", name, scopePrefix)), "input[placeholder~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "aria-label",  name, scopePrefix)), "input[aria-label~='"  + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "id",          name, scopePrefix)), "input[id~='"          + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "name",        name, scopePrefix)), "input[name~='"        + name + "']"));

        out.add(attempt(By.xpath(multiCaseXpath("textarea", "placeholder", name, scopePrefix)), "textarea[placeholder~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("textarea", "aria-label",  name, scopePrefix)), "textarea[aria-label~='"  + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("textarea", "id",          name, scopePrefix)), "textarea[id~='"          + name + "']"));

        out.add(attempt(
                By.xpath("//label[contains(normalize-space(text()),'" + name + "')]/following::input[1]"),
                "label[text~='" + name + "']/following::input"));
        out.add(attempt(
                By.xpath("//label[contains(normalize-space(text()),'" + lower + "')]/following::input[1]"),
                "label[text~='" + lower + "']/following::input"));
        out.add(attempt(
                By.xpath("(//label[contains(normalize-space(text()),'" + name + "')]//input)[1]"),
                "label[text~='" + name + "']//input"));

        out.add(attempt(By.xpath(childOfTextContainer("input", name)),   "input inside container with text '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("input", name)),        "first input after text '" + name + "'"));
        out.add(attempt(By.xpath(precedingSibling("input", name)),        "first input before text '" + name + "'"));
    }

    private static void addRelativeInputStrategies(List<LocatorAttempt> out, String name, WebDriver driver) {
        try {
            By textAnchor = By.xpath("//*[contains(normalize-space(text()),'" + name + "')]");
            out.add(attempt(RelativeLocator.with(By.tagName("input")).near(textAnchor, 150),   "input near text '" + name + "' (RelativeLocator)"));
            out.add(attempt(RelativeLocator.with(By.tagName("input")).toRightOf(textAnchor),   "input right-of text '" + name + "' (RelativeLocator)"));
            out.add(attempt(RelativeLocator.with(By.tagName("input")).below(textAnchor),       "input below text '" + name + "' (RelativeLocator)"));
        } catch (Exception ignored) {}
    }

    private static void addClickableStrategies(List<LocatorAttempt> out, String name, String lower,
                                                String scopePrefix) {
        out.add(attempt(By.xpath(scopePrefix + "button[normalize-space(text())='" + name  + "']"), "button[text='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[normalize-space(text())='" + lower + "']"), "button[text='" + lower + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[@id='" + name  + "']"),  "button[id='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[@id='" + lower + "']"),  "button[id='" + lower + "'] exact"));

        out.add(attempt(By.xpath(textContains("button", name, scopePrefix)), "button with text '" + name + "'"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "id",         name, scopePrefix)), "button[id~='"        + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "name",       name, scopePrefix)), "button[name~='"      + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "aria-label", name, scopePrefix)), "button[aria-label~='"+ name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "title",      name, scopePrefix)), "button[title~='"     + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "value",      name, scopePrefix)), "button[value~='"     + name + "']"));

        out.add(attempt(By.xpath(scopePrefix + "a[normalize-space(text())='" + name  + "']"), "a[text='" + name  + "'] exact"));
        out.add(attempt(By.xpath(textContains("a", name, scopePrefix)), "link with text '" + name + "'"));
        out.add(attempt(By.xpath(multiCaseXpath("a", "id",         name, scopePrefix)), "a[id~='"        + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("a", "aria-label", name, scopePrefix)), "a[aria-label~='"+ name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("a", "title",      name, scopePrefix)), "a[title~='"     + name + "']"));

        out.add(attempt(By.xpath(inputTypeWithAttr("submit", "value", name)), "input[type=submit][value~='" + name + "']"));
        out.add(attempt(By.xpath(inputTypeWithAttr("button", "value", name)), "input[type=button][value~='" + name + "']"));
        out.add(attempt(By.xpath(inputTypeWithAttr("image",  "alt",   name)), "input[type=image][alt~='"   + name + "']"));

        out.add(attempt(By.xpath(roleWithText("button",   name)), "[role=button] with text '"   + name + "'"));
        out.add(attempt(By.xpath(roleWithText("link",     name)), "[role=link] with text '"     + name + "'"));
        out.add(attempt(By.xpath(roleWithText("tab",      name)), "[role=tab] with text '"      + name + "'"));
        out.add(attempt(By.xpath(roleWithText("menuitem", name)), "[role=menuitem] with text '" + name + "'"));

        out.add(attempt(By.xpath("//*[normalize-space(text())='" + name
                + "' and (@role='button' or @type='submit' or self::button or self::a)]"),
                "exact text clickable '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("button", name)), "first button after text '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("a",      name)), "first link after text '"   + name + "'"));
    }

    private static void addSelectStrategies(List<LocatorAttempt> out, String name, String lower,
                                             String scopePrefix) {
        out.add(attempt(By.xpath(scopePrefix + "select[@id='"   + name  + "']"), "select[id='"   + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "select[@name='" + name  + "']"), "select[name='" + name  + "'] exact"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "id",         name, scopePrefix)), "select[id~='"         + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "name",       name, scopePrefix)), "select[name~='"       + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "aria-label", name, scopePrefix)), "select[aria-label~='" + name + "']"));
        out.add(attempt(By.xpath("//label[contains(normalize-space(text()),'" + name + "')]/following::select[1]"),
                "select after label '" + name + "'"));
        out.add(attempt(By.xpath(childOfTextContainer("select", name)), "select inside element with text '" + name + "'"));
    }

    private static void addReadableStrategies(List<LocatorAttempt> out, String name, String lower) {
        out.add(attempt(By.xpath(multiCaseXpath("span",    "id", name, "//")), "span[id~='"    + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("div",     "id", name, "//")), "div[id~='"     + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("p",       "id", name, "//")), "p[id~='"       + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("section", "id", name, "//")), "section[id~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("article", "id", name, "//")), "article[id~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("h1",      "id", name, "//")), "h1[id~='"      + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("h2",      "id", name, "//")), "h2[id~='"      + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("h3",      "id", name, "//")), "h3[id~='"      + name + "']"));

        out.add(attempt(By.cssSelector("[aria-label='"  + name  + "']"), "aria-label='" + name  + "' exact"));
        out.add(attempt(By.cssSelector("[aria-label='"  + lower + "']"), "aria-label='" + lower + "' exact"));
        out.add(attempt(By.cssSelector("[aria-label*='" + lower + "']"), "aria-label*='" + lower + "'"));
        out.add(attempt(By.cssSelector("[title*='"      + lower + "']"), "title*='" + lower + "'"));
        out.add(attempt(By.cssSelector("[role='" + lower + "']"), "role='" + lower + "'"));

        out.add(attempt(By.cssSelector("[class*='" + lower + "']"),                              "class*='" + lower + "'"));
        out.add(attempt(By.xpath(multiCaseXpath("div",  "class", name, "//")), "div[class~='"  + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("span", "class", name, "//")), "span[class~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("p",    "class", name, "//")), "p[class~='"    + name + "']"));

        out.add(attempt(By.xpath("//*[normalize-space(text())='" + name  + "']"),           "exact text='" + name  + "'"));
        out.add(attempt(By.xpath("//*[contains(normalize-space(text()),'" + name  + "')]"), "text contains '" + name  + "'"));
        out.add(attempt(By.xpath("//*[contains(normalize-space(text()),'" + lower + "')]"), "text contains '" + lower + "'"));
    }

    private static void addBronzeUniversalStrategies(List<LocatorAttempt> out, String name, String lower,
                                                      String scopePrefix) {
        out.add(attempt(By.xpath("//*[contains(@aria-label,'" + name  + "')]"), "*[aria-label*='" + name  + "']"));
        out.add(attempt(By.xpath("//*[contains(@aria-label,'" + lower + "')]"), "*[aria-label*='" + lower + "']"));
        out.add(attempt(By.xpath("//*[contains(@title,'"      + name  + "')]"), "*[title*='"      + name  + "']"));
        out.add(attempt(By.xpath("//*[contains(@title,'"      + lower + "')]"), "*[title*='"      + lower + "']"));
    }

    /**
     * Generates an XPath that checks an attribute against multiple case variants.
     * scopePrefix: "//" for global, "//form/" for parent-scoped (T2-F).
     */
    private static String multiCaseXpath(String tag, String attr, String name, String scopePrefix) {
        String lower = name.toLowerCase();
        String upper = name.toUpperCase();
        String capitalized = Character.toUpperCase(name.charAt(0))
                + (name.length() > 1 ? name.substring(1).toLowerCase() : "");
        String base = scopePrefix.endsWith("/") ? scopePrefix + tag : scopePrefix + "/" + tag;
        return base + "[contains(@" + attr + ",'" + name       + "')"
                +     " or contains(@" + attr + ",'" + lower      + "')"
                +     " or contains(@" + attr + ",'" + upper      + "')"
                +     " or contains(@" + attr + ",'" + capitalized + "')]";
    }

    /**
     * Generates XPath to find an element by text content with case variants.
     * scopePrefix: "//" for global, "//form/" for parent-scoped (T2-F).
     */
    private static String textContains(String tag, String name, String scopePrefix) {
        String lower = name.toLowerCase();
        String base = scopePrefix.endsWith("/") ? scopePrefix + tag : scopePrefix + "/" + tag;
        return base + "[contains(normalize-space(text()),'" + name  + "')"
                +     " or contains(normalize-space(text()),'" + lower + "')]";
    }

    /**
     * Reconstructs a By locator from a locator value string produced by By.toString().
     * "By.id: loginBtn" → By.id("loginBtn"). Returns null if the format is not recognised.
     */
    private static By rebuildLocator(String locatorValue) {
        if (locatorValue == null || locatorValue.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("By\\.([a-zA-Z]+):\\s*(.*)")
                .matcher(locatorValue.trim());
        if (!m.find()) return null;
        String method = m.group(1);
        String value  = m.group(2).trim();
        if (value.isBlank()) return null;
        return switch (method) {
            case "id"          -> By.id(value);
            case "name"        -> By.name(value);
            case "cssSelector" -> By.cssSelector(value);
            case "xpath"       -> By.xpath(value);
            case "className"   -> By.className(value);
            case "tagName"     -> By.tagName(value);
            case "linkText"    -> By.linkText(value);
            case "partialLinkText" -> By.partialLinkText(value);
            default            -> null;
        };
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

    /**
     * Maps Ellithium action method names to element categories.
     * These are the REAL method names from Ellithium's interaction classes:
     * ElementActions, SelectActions, MouseActions, JavaScriptActions, MobileActions.
     */
    static ElementCategory categorizeAction(String actionType) {
        if (actionType == null || actionType.equals("unknown")) return ElementCategory.ANY;

        return switch (actionType) {
            case "sendData", "clearElement",
                 "setElementValueUsingJS",
                 "uploadFile", "uploadMultipleFiles", "uploadFileUsingJS"
                    -> ElementCategory.INPUT;

            case "clickOnElement", "clickOnMultipleElements",
                 "hoverOverElement", "hoverAndClick", "doubleClick", "rightClick",
                 "dragAndDrop", "dragAndDropByOffset",
                 "javascriptClick",
                 "tap", "doubleTap", "longPress", "twoFingerTap"
                    -> ElementCategory.CLICKABLE;

            case "selectDropdownByText", "selectDropdownByValue", "selectDropdownByIndex",
                 "deselectAll", "deselectDropdownByText", "deselectDropdownByValue",
                 "deselectDropdownByIndex", "selectDropdownByTextForMultipleElements"
                    -> ElementCategory.SELECT;

            case "getText", "getTextFromMultipleElements",
                 "getAttributeValue", "getAttributeFromMultipleElements", "getPropertyValue",
                 "getDropdownSelectedOptions",
                 "isTextContains", "isAttributeContains",
                 "isElementPresent", "isElementDisplayed", "isElementEnabled",
                 "isElementSelected", "isElementClickable"
                    -> ElementCategory.READABLE;

            case "scrollIntoView", "scrollToElement",
                 "moveSliderTo", "moveSliderByOffset",
                 "swipe", "scroll", "scrollToElementBySelector",
                 "drag", "pinch", "fling"
                    -> ElementCategory.ANY;

            default -> ElementCategory.ANY;
        };
    }

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
