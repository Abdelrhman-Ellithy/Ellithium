package Ellithium.core.ai;

import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.spi.HealingTier;
import Ellithium.core.ai.spi.Tier1AlgorithmicHealer;
import Ellithium.core.ai.spi.Tier3EnsembleHealer;
import Ellithium.core.ai.spi.Tier4LLMHealer;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class HealingOrchestrator {

    private static final HealingOrchestrator INSTANCE = new HealingOrchestrator(List.of(
            new Tier1AlgorithmicHealer(), new Tier3EnsembleHealer(), new Tier4LLMHealer()));

    private final List<HealingTier> tiers;

    HealingOrchestrator(List<HealingTier> tiers) {
        this.tiers = tiers.stream().sorted(Comparator.comparingInt(HealingTier::order)).toList();
    }

    public static HealingOrchestrator get() {
        return INSTANCE;
    }

    public HealOutcome heal(HealingRequest request) {
        for (HealingTier tier : tiers) {
            if (!tier.isAvailable()) continue;

            HealOutcome raw;
            long tierStart = System.nanoTime();
            try {
                raw = tier.heal(request);
            } catch (Exception e) {
                Reporter.log("[TIER " + tier.order() + "] heal raised " + e.getClass().getSimpleName()
                        + " — falling through", LogLevel.WARN);
                continue;
            }
            Reporter.log("[PERF] TIER " + tier.order() + " heal() = "
                    + (System.nanoTime() - tierStart) / 1_000_000 + "ms"
                    + (raw != null && raw.element() != null ? " (hit)" : " (miss)"), LogLevel.INFO_BLUE);
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
                    : ElementFingerprint.reconstructLocator(resolved);
            WebElement guarded = guardStaleHeal(request.driver(), resolved);
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

    private static WebElement guardStaleHeal(WebDriver driver, WebElement healed) {
        if (healed == null) return null;
        By reconstructed = ElementFingerprint.reconstructLocator(healed);
        try {
            healed.isEnabled();
            return healed;
        } catch (org.openqa.selenium.StaleElementReferenceException stale) {
            if (reconstructed != null) {
                try {
                    WebElement refreshed = driver.findElement(reconstructed);
                    Reporter.log("Healed element went stale before use — re-resolved via "
                            + reconstructed, LogLevel.INFO_YELLOW);
                    return refreshed;
                } catch (Exception ignored) {}
            }
            return healed;
        } catch (Exception other) {
            return healed;
        }
    }
}
