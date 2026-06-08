package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.spi.HealingTier;
import Ellithium.core.ai.spi.Tier1AlgorithmicHealer;
import Ellithium.core.ai.spi.Tier2EnsembleHealer;
import Ellithium.core.ai.spi.Tier3LLMHealer;
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

public final class HealingOrchestrator {

    private static final java.nio.file.Path KILL_SWITCH_PATH =
            Paths.get(System.getProperty("user.home"), ".ellithium", "ai-disable");

    private static final HealingOrchestrator INSTANCE = new HealingOrchestrator(List.of(
            new Tier1AlgorithmicHealer(), new Tier2EnsembleHealer(), new Tier3LLMHealer()));

    private final List<HealingTier> tiers;

    HealingOrchestrator(List<HealingTier> tiers) {
        this.tiers = tiers.stream().sorted(Comparator.comparingInt(HealingTier::order)).toList();
    }

    public static HealingOrchestrator get() {
        return INSTANCE;
    }

    public HealOutcome heal(HealingRequest request) {
        if (Files.exists(KILL_SWITCH_PATH)) {
            Reporter.log("[AI] Kill switch active (~/.ellithium/ai-disable exists) — all healing disabled",
                    LogLevel.WARN);
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
                    raw.element(), request.actionType(), "TIER " + tier.order());
            if (resolved == null) continue;

            if (!tier.persistsOwnHeal()) {
                BaselineStore.capture(request.driver(), request.brokenLocator(), resolved,
                        raw.score(), tier.order());
                AISelfHealer.queueSourcePatch(request.brokenLocator(), resolved,
                        request.stackTrace(), raw.score(), tier.order());
            }

            By locator = (resolved == raw.element() && raw.reconstructedLocator() != null)
                    ? raw.reconstructedLocator()
                    : reconstructBest(request.driver(), resolved, request.baseline());
            WebElement guarded = guardStaleHeal(request.driver(), resolved, request.baseline(), locator);
            return new HealOutcome(guarded, locator, raw.score(), tier.order());
        }
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
                                                        String tierLabel) {
        if (healed == null || !isClickLikeAction(actionType)) return healed;
        try {
            String tag = healed.getTagName().toLowerCase();
            if (INTERACTIVE_TAGS.contains(tag)) return healed;

            String role = healed.getAttribute("role");
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
                    // Multiple matches: re-score against baseline to pick the right one
                    if (baseline != null && !candidates.isEmpty()) {
                        WebElement best = null;
                        double bestScore = -1;
                        for (WebElement c : candidates) {
                            try {
                                double s = baseline.scoreSimilarity(c);
                                if (s > bestScore) { bestScore = s; best = c; }
                            } catch (Exception ignored) {}
                        }
                        if (best != null && bestScore >= 0.5) {
                            Reporter.log("Healed element went stale — re-resolved via fingerprint score "
                                    + String.format("%.2f", bestScore) + " using " + fallback,
                                    LogLevel.INFO_YELLOW);
                            return best;
                        }
                    }
                } catch (Exception ignored) {}
            }
            return healed;
        } catch (Exception other) {
            return healed;
        }
    }
}
