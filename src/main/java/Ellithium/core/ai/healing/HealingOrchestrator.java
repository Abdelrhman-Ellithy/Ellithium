package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.models.HealingResult;
import Ellithium.core.ai.reporting.AIHealingReporter;
import Ellithium.core.ai.spi.ElementHealingPort;
import Ellithium.core.ai.spi.HealingTier;
import Ellithium.core.ai.spi.Tier1AlgorithmicHealer;
import Ellithium.core.ai.spi.Tier2EnsembleHealer;
import Ellithium.core.ai.spi.Tier3LLMHealer;
import Ellithium.core.ai.HealingTelemetryStore;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class HealingOrchestrator implements ElementHealingPort {

    private static final java.nio.file.Path KILL_SWITCH_PATH =
            Paths.get(System.getProperty("user.home"), ".ellithium", "ai-disable");

    private static final HealingOrchestrator INSTANCE = new HealingOrchestrator(loadTiers());

    private static List<HealingTier> loadTiers() {
        List<HealingTier> tiers = new java.util.ArrayList<>(List.of(
                new Tier1AlgorithmicHealer(), new Tier2EnsembleHealer(), new Tier3LLMHealer()));
        try {
            for (HealingTier ext : java.util.ServiceLoader.load(HealingTier.class)) {
                tiers.add(ext);
            }
        } catch (Exception ignored) {}
        return tiers;
    }

    private final List<HealingTier> tiers;

    HealingOrchestrator(List<HealingTier> tiers) {
        this.tiers = tiers.stream().sorted(Comparator.comparingInt(HealingTier::order)).toList();
    }

    public static HealingOrchestrator get() {
        return INSTANCE;
    }

    @Override
    public By getCachedLocator(WebDriver driver, By brokenLocator) {
        return AISelfHealer.getCachedHealedLocator(driver, brokenLocator);
    }

    public static boolean isHealingGloballyEnabled() {
        return !Files.exists(KILL_SWITCH_PATH);
    }

    public HealOutcome heal(HealingRequest request) {
        if (!isHealingGloballyEnabled()) {
            Reporter.log("[AI] Kill switch active (~/.ellithium/ai-disable exists) — all healing disabled",
                    LogLevel.WARN);
            HealingTelemetryStore.record(0, request.brokenLocator().toString(), null, 0.0, false);
            return null;
        }
        for (HealingTier tier : tiers) {
            if (!tier.isAvailable()) continue;

            HealOutcome raw;
            try {
                raw = tier.heal(request);
            } catch (Throwable e) {
                // Re-interrupt so executor-shutdown signals are not silently swallowed
                // by this catch(Throwable) — without this, parallel-suite shutdown hangs.
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                Reporter.log("[TIER " + tier.order() + "] heal raised " + e.getClass().getSimpleName()
                        + " — falling through", LogLevel.WARN);
                continue;
            }
            if (raw == null || raw.element() == null) continue;

            WebElement resolved = resolveInteractiveElement(
                    raw.element(), request.actionType(), "TIER " + tier.order(), request.driver());
            if (resolved == null) continue;

            By locator = (resolved == raw.element() && raw.reconstructedLocator() != null)
                    ? raw.reconstructedLocator()
                    : reconstructBest(request.driver(), resolved, request.baseline());

            AISelfHealer.cacheHealedLocator(request.driver(), request.brokenLocator(),
                    locator, raw.score(), request.fieldName());

            if (!tier.persistsOwnHeal()) {
                BaselineStore.capture(request.driver(), request.brokenLocator(), resolved,
                        raw.score(), tier.order());

                HealingContextBuilder.SourceLocation srcLoc =
                        HealingContextBuilder.resolveSourceLocation(request.stackTrace());

                if (locator != null) {
                    AISelfHealer.queueSourcePatch(request.brokenLocator(), locator,
                            srcLoc, raw.score(), tier.order());
                }

                String expr = locator != null ? AISelfHealer.byToJavaExpression(locator) : null;
                if (expr == null && locator != null) expr = locator.toString();
                AIHealingReporter.queueChange(
                        srcLoc != null && srcLoc.filePath != null ? srcLoc.filePath : "unknown",
                        request.brokenLocator().toString(),
                        new HealingResult(expr != null ? expr : "unknown",
                                raw.score(), "[TIER " + tier.order() + "]"),
                        srcLoc != null ? srcLoc.className : null,
                        srcLoc != null ? srcLoc.methodName : null,
                        request.actionType(),
                        srcLoc != null ? srcLoc.lineNumber : 0);
            }
            WebElement guarded = guardStaleHeal(request.driver(), resolved, request.baseline(), locator);
            if (guarded == null) continue;
            return new HealOutcome(guarded, locator, raw.score(), tier.order());
        }
        Reporter.log("[AI] All healing tiers exhausted for " + request.brokenLocator()
                + ". Known limitations: elements inside <iframe> (switch frame context before the"
                + " action) and inside Shadow DOM roots (use CSS ::part() or pierce selector)"
                + " are not reachable by standard WebDriver and will not heal.", LogLevel.DEBUG);
        return null;
    }

    private static final Set<String> INTERACTIVE_TAGS =
            Set.of("button", "a", "input", "select", "textarea", "option");

    private static final Set<String> INTERACTIVE_ROLES =
            Set.of("button", "link", "menuitem", "menuitemcheckbox",
                    "menuitemradio", "tab", "option", "checkbox", "radio");

    private static final String[] INNER_INTERACTIVE_SELECTORS = {
            "button[type='submit']",
            "input[type='submit']",
            "button",
            "input[type='button']",
            "a"
    };

    private static boolean isClickLikeAction(String actionType) {
        if (actionType == null || actionType.equals("unknown")) return false;
        String lower = actionType.toLowerCase();
        return lower.contains("click") || lower.contains("tap")
                || lower.contains("press") || lower.contains("hover");
    }

    private static WebElement resolveInteractiveElement(WebElement healed, String actionType,
                                                        String tierLabel, WebDriver driver) {
        if (healed == null || !isClickLikeAction(actionType)) return healed;
        try {
            // Batch getTagName + getAttribute("role") into one JS round-trip instead of two.
            String tag, role;
            try {
                Object[] res = (Object[]) ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("var e=arguments[0]; return [e.tagName.toLowerCase(), e.getAttribute('role')];", healed);
                if (res != null && res.length >= 2) {
                    tag  = res[0] != null ? res[0].toString() : healed.getTagName().toLowerCase();
                    role = res[1] != null ? res[1].toString() : null;
                } else {
                    tag  = healed.getTagName().toLowerCase();
                    role = healed.getAttribute("role");
                }
            } catch (Exception jse) {
                tag  = healed.getTagName().toLowerCase();
                role = healed.getAttribute("role");
            }
            if (INTERACTIVE_TAGS.contains(tag)) return healed;

            if (role != null && INTERACTIVE_ROLES.contains(role)) return healed;

            for (String selector : INNER_INTERACTIVE_SELECTORS) {
                try {
                    WebElement inner = healed.findElement(By.cssSelector(selector));
                    if (inner.isDisplayed()) {
                        Reporter.log("[" + tierLabel + "] Resolved container <" + tag
                                + "> → inner interactive <" + inner.getTagName() + ">", LogLevel.INFO_YELLOW);
                        return inner;
                    }
                } catch (NoSuchElementException ignored) {}
            }

            Reporter.log("[" + tierLabel + "] Healed element is container <" + tag
                    + "> with no interactive child for click action — skipping", LogLevel.WARN);
            return null;
        } catch (Exception ex) {
            return healed;
        }
    }

    /**
     * Reconstructs the strongest DOM-validated locator for a healed element via
     * {@link HealedLocatorBuilder}; falls back to the lightweight fingerprint reconstruction.
     */
    private static By reconstructBest(WebDriver driver, WebElement healed,
                                      ElementFingerprint baseline) {
        By best = HealedLocatorBuilder.build(driver, healed, baseline);
        return best != null ? best : ElementFingerprint.reconstructLocator(healed);
    }

    private static WebElement guardStaleHeal(WebDriver driver, WebElement healed,
                                             ElementFingerprint baseline, By fallback) {
        if (healed == null) return null;
        try {
            healed.isEnabled();
            return healed;
        } catch (org.openqa.selenium.StaleElementReferenceException stale) {
            if (fallback != null) {
                try {
                    List<WebElement> candidates = driver.findElements(fallback);
                    if (candidates.size() == 1) {
                        Reporter.log("Healed element went stale — re-resolved uniquely via "
                                + fallback, LogLevel.INFO_YELLOW);
                        return candidates.get(0);
                    }
                    if (!candidates.isEmpty()) {
                        // Multiple matches: pick the best-scoring candidate against the baseline.
                        // No threshold gate here — this is stale recovery, not a fresh heal;
                        // the locator was already validated. Any re-found element is better than stale.
                        if (baseline != null) {
                            WebElement best = null;
                            double bestScore = -1;
                            for (WebElement c : candidates) {
                                try {
                                    double s = baseline.scoreSimilarity(c);
                                    if (s > bestScore) { bestScore = s; best = c; }
                                } catch (Exception ignored) {}
                            }
                            if (best != null) {
                                Reporter.log("Healed element went stale — re-resolved via fingerprint score "
                                        + String.format("%.2f", bestScore) + " using " + fallback,
                                        LogLevel.INFO_YELLOW);
                                return best;
                            }
                        }
                        return candidates.get(0);
                    }
                } catch (Exception ignored) {}
            }
            return null;
        } catch (Exception other) {
            return healed;
        }
    }
}
