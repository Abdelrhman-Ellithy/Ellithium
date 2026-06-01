package Ellithium.core.ai;

import Ellithium.core.ai.reporting.AIHealingReporter;
import Ellithium.core.ai.scoring.SemanticNameExtractor;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealingResult;
import Ellithium.core.ai.scoring.LocatorMutationEngine;
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

    // Graded f2 strategy weights by tier (gold = most stable test attrs … iron = structural/relational).
    private static final double F2_GOLD = 1.00, F2_SILVER = 0.75, F2_BRONZE = 0.50, F2_IRON = 0.30;

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
    /**
     * Fans out ALL semantic strategies and returns every matching live element with its tier weight
     * (gold=1.0, silver=0.75, bronze=0.5, iron=0.3). Side-effect-free — the ensemble's job to commit
     * the winner. The mutation pre-pass is included as a synthetic high-confidence hit (weight 0.95).
     */
    public static List<Ellithium.core.ai.models.SemanticHit> findSemanticHits(WebDriver driver,
                                                                               String methodName, String fieldName,
                                                                               String actionType, String locatorValue,
                                                                               ElementFingerprint baseline) {
        List<Ellithium.core.ai.models.SemanticHit> hits = new ArrayList<>();
        collectMutationHits(hits, locatorValue, driver, baseline);

        ElementCategory category = categorizeAction(actionType);
        boolean isMobile = driver instanceof AppiumDriver;
        String semanticMethodName = (category == ElementCategory.READABLE) ? null : methodName;
        List<String> semanticNames = SemanticNameExtractor.extract(semanticMethodName, fieldName, locatorValue);
        if (semanticNames.isEmpty()) return hits;

        TieredAttempts tiered = buildTieredStrategies(semanticNames, category, isMobile, driver, baseline);

        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            java.util.IdentityHashMap<WebElement, Double> best = new java.util.IdentityHashMap<>();
            collectHits(driver, tiered.gold, F2_GOLD, best);
            if (best.isEmpty()) collectHits(driver, tiered.silver, F2_SILVER, best);
            if (best.isEmpty()) collectHits(driver, tiered.bronze, F2_BRONZE, best);
            if (best.isEmpty()) collectHits(driver, tiered.iron,   F2_IRON,   best);
            for (java.util.Map.Entry<WebElement, Double> e : best.entrySet()) {
                hits.add(new Ellithium.core.ai.models.SemanticHit(e.getKey(), e.getValue(), "strategy"));
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
        return hits;
    }

    public static List<Ellithium.core.ai.models.SemanticHit> findExactHits(WebDriver driver,
                                                                           String methodName, String fieldName,
                                                                           String actionType, String locatorValue,
                                                                           ElementFingerprint baseline) {
        List<Ellithium.core.ai.models.SemanticHit> hits = new ArrayList<>();
        collectMutationHits(hits, locatorValue, driver, baseline);

        ElementCategory category = categorizeAction(actionType);
        boolean isMobile = driver instanceof AppiumDriver;
        String semanticMethodName = (category == ElementCategory.READABLE) ? null : methodName;
        List<String> semanticNames = SemanticNameExtractor.extract(semanticMethodName, fieldName, locatorValue);
        if (semanticNames.isEmpty()) return hits;

        TieredAttempts tiered = buildTieredStrategies(semanticNames, category, isMobile, driver, baseline);
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            java.util.IdentityHashMap<WebElement, Double> best = new java.util.IdentityHashMap<>();
            collectHits(driver, tiered.gold, F2_GOLD, best);
            if (best.isEmpty()) collectHits(driver, tiered.silver, F2_SILVER, best);
            for (java.util.Map.Entry<WebElement, Double> e : best.entrySet()) {
                hits.add(new Ellithium.core.ai.models.SemanticHit(e.getKey(), e.getValue(), "exact"));
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
        return hits;
    }

    /** Mutation pre-pass shared by findSemanticHits and findExactHits. */
    private static void collectMutationHits(List<Ellithium.core.ai.models.SemanticHit> hits,
                                            String locatorValue, WebDriver driver,
                                            ElementFingerprint baseline) {
        if (locatorValue != null && !locatorValue.isBlank()) {
            By brokenBy = rebuildLocator(locatorValue);
            if (brokenBy != null) {
                WebElement mutationMatch = LocatorMutationEngine.tryMutations(brokenBy, driver, baseline);
                if (mutationMatch != null) {
                    hits.add(new Ellithium.core.ai.models.SemanticHit(mutationMatch, 0.95, "mutation"));
                }
            }
        }
    }

    /** Clears the strategy cache. Call between suites to avoid stale strategies from a different app. */
    public static void resetCache() {
        STRATEGY_CACHE.clear();
    }

    private static void collectHits(WebDriver driver, List<LocatorAttempt> attempts, double weight,
                                    java.util.IdentityHashMap<WebElement, Double> best) {
        for (LocatorAttempt attempt : attempts) {
            try {
                List<WebElement> found = driver.findElements(attempt.locator);
                for (WebElement el : found) {
                    Double prev = best.get(el);
                    if (prev == null || prev < weight) best.put(el, weight);
                }
                if (!best.isEmpty()) return;   // first productive strategy in this tier wins (priority order)
            } catch (Exception ignored) {}
        }
    }

    public static double strategyWeightForAttrs(java.util.Map<String, Object> attrs, List<String> names) {
        if (attrs == null || names == null || names.isEmpty()) return Double.NaN;
        String id   = lc(attrs.get("id")),   nm    = lc(attrs.get("name")),  testid = lc(attrs.get("data-testid"));
        String aria = lc(attrs.get("aria-label")), accId = lc(attrs.get("accessibility-id"));
        String cdesc = lc(attrs.get("content-desc")), resId = lc(attrs.get("resource-id"));
        String allattrs = lc(attrs.get("allattrs")), text = lc(attrs.get("text"));

        double best = Double.NaN;
        for (String raw : names) {
            if (raw == null || raw.isBlank()) continue;
            String n = raw.toLowerCase();
            if (eq(id, n) || eq(nm, n) || eq(testid, n) || eq(aria, n)
                    || eq(accId, n) || eq(cdesc, n) || suffixEq(resId, n) || eq(text, n)) {
                return F2_GOLD;
            }
            if (has(text, n) || has(allattrs, n)) {
                best = nanMax(best, F2_SILVER);
            }
        }
        return best;
    }

    private static String lc(Object o) { return o == null ? null : o.toString().toLowerCase(); }
    private static boolean eq(String a, String n) { return a != null && a.equals(n); }
    private static boolean has(String a, String n) { return a != null && !a.isEmpty() && a.contains(n); }
    private static boolean suffixEq(String resId, String n) {
        if (resId == null) return false;
        int i = resId.lastIndexOf('/');
        return (i >= 0 ? resId.substring(i + 1) : resId).equals(n);
    }
    private static double nanMax(double a, double b) { return Double.isNaN(a) ? b : Math.max(a, b); }

    /**
     * Standalone Tier-2 heal — picks the highest-tier match, cross-validates against the baseline,
     * commits telemetry. Used as a fallback path when the ensemble is disabled.
     */
    public static WebElement trySemanticHeal(WebDriver driver,
                                             String methodName, String fieldName,
                                             String actionType, String locatorValue,
                                             ElementFingerprint baseline) {
        List<Ellithium.core.ai.models.SemanticHit> hits = findSemanticHits(driver, methodName, fieldName, actionType, locatorValue, baseline);
        if (hits.isEmpty()) {
            HealingTelemetryStore.record(2, locatorValue, null, 0.0, false);
            return null;
        }
        double cvThreshold;
        if (baseline == null) cvThreshold = 0.0;
        else if (baseline.getId() != null || baseline.getDataTestId() != null) cvThreshold = 0.35;
        else cvThreshold = 0.25;

        hits.sort((a, b) -> Double.compare(b.tierWeight, a.tierWeight));
        for (Ellithium.core.ai.models.SemanticHit hit : hits) {
            try {
                if (baseline != null && baseline.scoreSimilarity(hit.element) < cvThreshold) continue;
                By cleanLocator = ElementFingerprint.reconstructLocator(hit.element);
                String cleanLocatorStr = cleanLocator != null ? cleanLocator.toString() : "";
                Reporter.log("Tier 2: MATCH via " + hit.strategyDescription
                        + " | locator=" + cleanLocatorStr, LogLevel.INFO_GREEN);
                AIHealingReporter.queueChange("semantic-strategy", cleanLocatorStr,
                        new HealingResult(cleanLocatorStr, 0.85, "[TIER 2 - Semantic] " + hit.strategyDescription),
                        null, methodName, actionType, 0);
                HealingTelemetryStore.record(2, locatorValue, cleanLocatorStr, 0.85, true);
                return hit.element;
            } catch (Exception ignored) {}
        }
        HealingTelemetryStore.record(2, locatorValue, null, 0.0, false);
        return null;
    }

    /**
     * Generates all applicable locator strategies for the given semantic names
     * and element category.
     */
    static final class TieredAttempts {
        final List<LocatorAttempt> gold;
        final List<LocatorAttempt> silver;
        final List<LocatorAttempt> bronze;
        final List<LocatorAttempt> iron;
        TieredAttempts(List<LocatorAttempt> gold, List<LocatorAttempt> silver,
                       List<LocatorAttempt> bronze, List<LocatorAttempt> iron) {
            this.gold = gold; this.silver = silver; this.bronze = bronze; this.iron = iron;
        }
    }

    private static final int STRATEGY_CACHE_MAX = 1000;
    private static final java.util.Map<String, TieredAttempts> STRATEGY_CACHE =
            java.util.Collections.synchronizedMap(
                    new java.util.LinkedHashMap<>(STRATEGY_CACHE_MAX, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(java.util.Map.Entry<String, TieredAttempts> e) {
                            return size() > STRATEGY_CACHE_MAX;
                        }
                    });

    private static TieredAttempts buildTieredStrategies(List<String> names,
                                                        ElementCategory category,
                                                        boolean isMobile,
                                                        WebDriver driver,
                                                        ElementFingerprint baseline) {
        String parentTag = (baseline != null) ? baseline.getParentTag() : null;
        String scopePrefix = (parentTag != null) ? "//" + parentTag + "/" : "//";
        String cacheKey = String.join(",", names) + "|"
                + category + "|" + isMobile + "|" + parentTag;
        TieredAttempts cached = STRATEGY_CACHE.get(cacheKey);
        if (cached != null) return cached;
        TieredAttempts built = buildTieredUncached(names, category, isMobile, driver, scopePrefix);
        STRATEGY_CACHE.put(cacheKey, built);
        return built;
    }

    /**
     * Flattens the tiered strategies into ONE ordered, deduplicated list (gold → silver → bronze
     * → iron), preserving priority. Kept as a primitive for callers that want a single ranked
     * list to iterate (the ensemble uses the tiered form directly for graded f2 weights).
     */
    static List<LocatorAttempt> buildStrategies(List<String> names,
                                                ElementCategory category,
                                                boolean isMobile,
                                                WebDriver driver,
                                                ElementFingerprint baseline) {
        TieredAttempts t = buildTieredStrategies(names, category, isMobile, driver, baseline);
        List<LocatorAttempt> ordered = new ArrayList<>(
                t.gold.size() + t.silver.size() + t.bronze.size() + t.iron.size());
        ordered.addAll(t.gold);
        ordered.addAll(t.silver);
        ordered.addAll(t.bronze);
        ordered.addAll(t.iron);
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        List<LocatorAttempt> deduped = new ArrayList<>();
        for (LocatorAttempt a : ordered) {
            if (seen.add(a.locator.toString())) deduped.add(a);
        }
        return deduped;
    }

    private static TieredAttempts buildTieredUncached(List<String> names,
                                                      ElementCategory category,
                                                      boolean isMobile,
                                                      WebDriver driver,
                                                      String scopePrefix) {

        List<LocatorAttempt> gold   = new ArrayList<>();
        List<LocatorAttempt> silver = new ArrayList<>();
        List<LocatorAttempt> bronze = new ArrayList<>();
        List<LocatorAttempt> iron   = new ArrayList<>();

        for (String name : names) {
            if (name.isBlank()) continue;
            String lower = name.toLowerCase();

            addTestAttrStrategies(gold, lower);
            if (isMobile) addMobileGoldStrategies(gold, name, lower);

            addSilverStrategies(silver, name, lower, scopePrefix);

            switch (category) {
                case INPUT:
                    addInputStrategies(bronze, name, lower, scopePrefix, driver);
                    if (!isMobile) addRelativeInputStrategies(iron, name, driver);
                    addNeighborhoodInputStrategies(iron, name);
                    break;
                case CLICKABLE:
                    addClickableStrategies(bronze, name, lower, scopePrefix);
                    break;
                case SELECT:
                    addSelectStrategies(bronze, name, lower, scopePrefix);
                    break;
                case READABLE:
                    addReadableStrategies(bronze, name, lower);
                    addNeighborhoodReadableStrategies(iron, name);
                    break;
                case ANY:
                    addBronzeUniversalStrategies(bronze, name, lower, scopePrefix);
                    break;
            }

            if (category != ElementCategory.ANY) {
                addBronzeUniversalStrategies(bronze, name, lower, scopePrefix);
            }
        }
        return new TieredAttempts(gold, silver, bronze, iron);
    }

    private static void addMobileGoldStrategies(List<LocatorAttempt> out, String name, String lower) {
        try {
            out.add(attempt(io.appium.java_client.AppiumBy.accessibilityId(name),
                    "AppiumBy.accessibilityId('" + name + "')"));
            out.add(attempt(io.appium.java_client.AppiumBy.accessibilityId(lower),
                    "AppiumBy.accessibilityId('" + lower + "')"));
            out.add(attempt(io.appium.java_client.AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceIdMatches(\".*" + lower + ".*\")"),
                    "uiautomator resourceIdMatches " + lower));
            out.add(attempt(io.appium.java_client.AppiumBy.iOSClassChain(
                            "**/*[`name CONTAINS[c] '" + lower + "'`]"),
                    "iosClassChain name CONTAINS " + lower));
        } catch (Exception ignored) {}
    }

    private static void addNeighborhoodInputStrategies(List<LocatorAttempt> out, String name) {
        out.add(attempt(By.xpath(precedingSibling("input", name)),
                "input preceding text '" + name + "'"));
        out.add(attempt(By.xpath(precedingSibling("textarea", name)),
                "textarea preceding text '" + name + "'"));
    }

    private static void addNeighborhoodReadableStrategies(List<LocatorAttempt> out, String name) {
        out.add(attempt(By.xpath(precedingSibling("span", name)),
                "span preceding text '" + name + "'"));
        out.add(attempt(By.xpath(precedingSibling("div", name)),
                "div preceding text '" + name + "'"));
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
        out.add(attempt(By.xpath(scopePrefix + "input[@placeholder=" + xpathLiteral(name)  + "]"), "input[placeholder='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@placeholder=" + xpathLiteral(lower) + "]"), "input[placeholder='" + lower + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@name=" + xpathLiteral(name)  + "]"), "input[name='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "input[@name=" + xpathLiteral(lower) + "]"), "input[name='" + lower + "'] exact"));

        out.add(attempt(By.xpath(multiCaseXpath("input", "placeholder", name, scopePrefix)), "input[placeholder~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "aria-label",  name, scopePrefix)), "input[aria-label~='"  + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "id",          name, scopePrefix)), "input[id~='"          + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("input", "name",        name, scopePrefix)), "input[name~='"        + name + "']"));

        out.add(attempt(By.xpath(multiCaseXpath("textarea", "placeholder", name, scopePrefix)), "textarea[placeholder~='" + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("textarea", "aria-label",  name, scopePrefix)), "textarea[aria-label~='"  + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("textarea", "id",          name, scopePrefix)), "textarea[id~='"          + name + "']"));

        out.add(attempt(
                By.xpath("//label[contains(normalize-space(text())," + xpathLiteral(name) + ")]/following::input[1]"),
                "label[text~='" + name + "']/following::input"));
        out.add(attempt(
                By.xpath("//label[contains(normalize-space(text())," + xpathLiteral(lower) + ")]/following::input[1]"),
                "label[text~='" + lower + "']/following::input"));
        out.add(attempt(
                By.xpath("(//label[contains(normalize-space(text())," + xpathLiteral(name) + ")]//input)[1]"),
                "label[text~='" + name + "']//input"));

        out.add(attempt(By.xpath(childOfTextContainer("input", name)),   "input inside container with text '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("input", name)),        "first input after text '" + name + "'"));
        out.add(attempt(By.xpath(precedingSibling("input", name)),        "first input before text '" + name + "'"));
    }

    private static void addRelativeInputStrategies(List<LocatorAttempt> out, String name, WebDriver driver) {
        try {
            By textAnchor = By.xpath("//*[contains(normalize-space(text())," + xpathLiteral(name) + ")]");
            out.add(attempt(RelativeLocator.with(By.tagName("input")).near(textAnchor, 150),   "input near text '" + name + "' (RelativeLocator)"));
            out.add(attempt(RelativeLocator.with(By.tagName("input")).toRightOf(textAnchor),   "input right-of text '" + name + "' (RelativeLocator)"));
            out.add(attempt(RelativeLocator.with(By.tagName("input")).below(textAnchor),       "input below text '" + name + "' (RelativeLocator)"));
        } catch (Exception ignored) {}
    }

    private static void addClickableStrategies(List<LocatorAttempt> out, String name, String lower,
                                                String scopePrefix) {
        out.add(attempt(By.xpath(scopePrefix + "button[normalize-space(text())=" + xpathLiteral(name)  + "]"), "button[text='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[normalize-space(text())=" + xpathLiteral(lower) + "]"), "button[text='" + lower + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[@id=" + xpathLiteral(name)  + "]"),  "button[id='" + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "button[@id=" + xpathLiteral(lower) + "]"),  "button[id='" + lower + "'] exact"));

        out.add(attempt(By.xpath(textContains("button", name, scopePrefix)), "button with text '" + name + "'"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "id",         name, scopePrefix)), "button[id~='"        + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "name",       name, scopePrefix)), "button[name~='"      + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "aria-label", name, scopePrefix)), "button[aria-label~='"+ name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "title",      name, scopePrefix)), "button[title~='"     + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("button", "value",      name, scopePrefix)), "button[value~='"     + name + "']"));

        out.add(attempt(By.xpath(scopePrefix + "a[normalize-space(text())=" + xpathLiteral(name)  + "]"), "a[text='" + name  + "'] exact"));
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

        out.add(attempt(By.xpath("//*[normalize-space(text())=" + xpathLiteral(name)
                + " and (@role='button' or @type='submit' or self::button or self::a)]"),
                "exact text clickable '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("button", name)), "first button after text '" + name + "'"));
        out.add(attempt(By.xpath(followingSibling("a",      name)), "first link after text '"   + name + "'"));
    }

    private static void addSelectStrategies(List<LocatorAttempt> out, String name, String lower,
                                             String scopePrefix) {
        out.add(attempt(By.xpath(scopePrefix + "select[@id="   + xpathLiteral(name)  + "]"), "select[id='"   + name  + "'] exact"));
        out.add(attempt(By.xpath(scopePrefix + "select[@name=" + xpathLiteral(name)  + "]"), "select[name='" + name  + "'] exact"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "id",         name, scopePrefix)), "select[id~='"         + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "name",       name, scopePrefix)), "select[name~='"       + name + "']"));
        out.add(attempt(By.xpath(multiCaseXpath("select", "aria-label", name, scopePrefix)), "select[aria-label~='" + name + "']"));
        out.add(attempt(By.xpath("//label[contains(normalize-space(text())," + xpathLiteral(name) + ")]/following::select[1]"),
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

        out.add(attempt(By.xpath("//*[normalize-space(text())=" + xpathLiteral(name)  + "]"),           "exact text='" + name  + "'"));
        out.add(attempt(By.xpath("//*[contains(normalize-space(text())," + xpathLiteral(name)  + ")]"), "text contains '" + name  + "'"));
        out.add(attempt(By.xpath("//*[contains(normalize-space(text())," + xpathLiteral(lower) + ")]"), "text contains '" + lower + "'"));
    }

    private static void addBronzeUniversalStrategies(List<LocatorAttempt> out, String name, String lower,
                                                      String scopePrefix) {
        out.add(attempt(By.xpath("//*[contains(@aria-label," + xpathLiteral(name)  + ")]"), "*[aria-label*='" + name  + "']"));
        out.add(attempt(By.xpath("//*[contains(@aria-label," + xpathLiteral(lower) + ")]"), "*[aria-label*='" + lower + "']"));
        out.add(attempt(By.xpath("//*[contains(@title,"      + xpathLiteral(name)  + ")]"), "*[title*='"      + name  + "']"));
        out.add(attempt(By.xpath("//*[contains(@title,"      + xpathLiteral(lower) + ")]"), "*[title*='"      + lower + "']"));
    }

    /**
     * Generates an XPath that checks an attribute against multiple case variants.
     * scopePrefix: "//" for global, "//form/" for parent-scoped (T2-F).
     */
    private static String multiCaseXpath(String tag, String attr, String name, String scopePrefix) {
        if (name == null || name.isEmpty()) return "//" + tag + "[@" + attr + "]";
        String lower = name.toLowerCase();
        String upper = name.toUpperCase();
        String capitalized = Character.toUpperCase(name.charAt(0))
                + (name.length() > 1 ? name.substring(1).toLowerCase() : "");
        String base = scopePrefix.endsWith("/") ? scopePrefix + tag : scopePrefix + "/" + tag;
        return base + "[contains(@" + attr + "," + xpathLiteral(name)        + ")"
                +     " or contains(@" + attr + "," + xpathLiteral(lower)      + ")"
                +     " or contains(@" + attr + "," + xpathLiteral(upper)      + ")"
                +     " or contains(@" + attr + "," + xpathLiteral(capitalized) + ")]";
    }

    /**
     * Generates XPath to find an element by text content with case variants.
     * scopePrefix: "//" for global, "//form/" for parent-scoped (T2-F).
     */
    private static String textContains(String tag, String name, String scopePrefix) {
        String lower = name.toLowerCase();
        String base = scopePrefix.endsWith("/") ? scopePrefix + tag : scopePrefix + "/" + tag;
        return base + "[contains(normalize-space(text())," + xpathLiteral(name)  + ")"
                +     " or contains(normalize-space(text())," + xpathLiteral(lower) + ")]";
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
        return "(//*[contains(normalize-space(text())," + xpathLiteral(name) + ")]/" + tag + ")[1]";
    }

    /**
     * Generates XPath for the first element of a tag type that follows text with the name.
     * Example: (//*[contains(text(),'Email')]/following::input)[1]
     */
    private static String followingSibling(String tag, String name) {
        return "(//*[contains(normalize-space(text())," + xpathLiteral(name) + ")]/following::" + tag + ")[1]";
    }

    /**
     * Generates XPath for the first element of a tag type that precedes text with the name.
     */
    private static String precedingSibling(String tag, String name) {
        return "(//*[contains(normalize-space(text())," + xpathLiteral(name) + ")]/preceding::" + tag + ")[1]";
    }

    /**
     * Generates XPath for input[type=X][attr contains name].
     */
    private static String inputTypeWithAttr(String type, String attr, String name) {
        String lower = name.toLowerCase();
        return "//input[@type='" + type + "' and (contains(@" + attr + "," + xpathLiteral(name) + ")"
                + " or contains(@" + attr + "," + xpathLiteral(lower) + "))]";
    }

    /**
     * Generates XPath for role-based elements with matching text content.
     */
    private static String roleWithText(String role, String name) {
        String lower = name.toLowerCase();
        return "//*[@role='" + role + "' and (contains(normalize-space(text())," + xpathLiteral(name) + ")"
                + " or contains(normalize-space(text())," + xpathLiteral(lower) + "))]";
    }

    /**
     * Returns an XPath string literal safe for any input. Standard XPath 1.0 has no escape syntax for
     * the quote character — when the input contains both {@code '} and {@code "} it must be split via
     * {@code concat()}. Wrapping every name-concat call through this prevents both injection (a
     * baseline or i18n placeholder containing {@code '} could break out of the predicate and select an
     * attacker-chosen node) and the everyday correctness bug of unescaped apostrophes.
     */
    public static String xpathLiteral(String s) {
        if (s == null) return "''";
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = s.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",\"'\",");
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Maps Ellithium action method names to element categories.
     * These are the REAL method names from Ellithium's interaction classes:
     * ElementActions, SelectActions, MouseActions, JavaScriptActions, MobileActions.
     */
    public static ElementCategory categorizeAction(String actionType) {
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
