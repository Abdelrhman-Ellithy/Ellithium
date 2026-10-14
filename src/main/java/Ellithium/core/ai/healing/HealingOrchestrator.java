package Ellithium.core.ai.healing;

import Ellithium.Utilities.ai.HealingStrategy;
import Ellithium.core.ai.config.AIConfigLoader;
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
import Ellithium.core.ai.dom.InteractiveElements;
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

public final class HealingOrchestrator implements ElementHealingPort {

    private static final java.nio.file.Path KILL_SWITCH_PATH =
            Paths.get(System.getProperty("user.home"), ".ellithium", "ai-disable");

    private static final HealingOrchestrator INSTANCE = new HealingOrchestrator(loadTiers());

    private static List<HealingTier> loadTiers() {
        List<HealingTier> tiers = new java.util.ArrayList<>(List.of(
                new Tier1AlgorithmicHealer(), new Tier2EnsembleHealer(), new Tier3LLMHealer()));
        try {
            for (HealingTier ext : java.util.ServiceLoader.load(HealingTier.class)) {
                if (ext.apiVersion() != 1) {
                    Reporter.log("[HEALING-SPI] Rejected external tier " + ext.getClass().getName()
                            + " — apiVersion=" + ext.apiVersion() + " (expected 1); "
                            + "update the implementation to match the current HealingTier contract",
                            LogLevel.WARN);
                    continue;
                }
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
        HealingStrategy strategy = AIConfigLoader.getHealingStrategy();
        if (strategy == HealingStrategy.DISABLED) {
            return null;
        }
        if (!isHealingGloballyEnabled()) {
            Reporter.log("[AI] Kill switch active (~/.ellithium/ai-disable exists) — all healing disabled",
                    LogLevel.WARN);
            HealingTelemetryStore.record(0, request.brokenLocator().toString(), null, 0.0, false);
            return null;
        }

        for (HealingTier tier : tiers) {
            if (!tier.isAvailable()) continue;

            HealOutcome outcome = processTier(tier, request, strategy);
            if (outcome != null) {
                return strategy == HealingStrategy.SUGGEST_ONLY ? null : outcome;
            }
        }
        Reporter.log("[AI] All healing tiers exhausted for " + request.brokenLocator()
                + ". Known limitations: elements inside <iframe> (switch frame context before the"
                + " action) and inside Shadow DOM roots (use CSS ::part() or pierce selector)"
                + " are not reachable by standard WebDriver and will not heal.", LogLevel.DEBUG);
        return null;
    }

    private HealOutcome processTier(HealingTier tier, HealingRequest request, HealingStrategy strategy) {
        HealingTelemetryStore.markAttemptStart();
        HealOutcome raw = executeTierSafe(tier, request);
        if (raw == null || raw.element() == null) return null;

        double rawScore = raw.score();
        if (Double.isNaN(rawScore) || rawScore <= 0.0) return null;
        if (tier.order() > 1 && rawScore < AIConfigLoader.getConfidenceThreshold()) return null;

        WebElement resolved = resolveInteractiveElement(
                raw.element(), request.actionType(), "TIER " + tier.order(), request.driver());
        if (resolved == null) return null;

        By locator = (resolved == raw.element() && raw.reconstructedLocator() != null)
                ? raw.reconstructedLocator()
                : reconstructBest(request.driver(), resolved, request.baseline());

        if (locator != null && locator.equals(request.brokenLocator())) return null;

        WebElement guarded = guardStaleHeal(request.driver(), resolved, request.baseline(), locator);
        if (guarded == null) return null;

        persistAndRecordHeal(tier, request, locator, guarded, raw.score());

        if (strategy == HealingStrategy.SUGGEST_ONLY) {
            Reporter.log("[AI SUGGESTION] Locator " + request.brokenLocator() + " could be healed to: "
                    + locator + " (score " + String.format("%.2f", raw.score()) + " via TIER " + tier.order() + ")",
                    LogLevel.INFO_YELLOW);
        }
        return new HealOutcome(guarded, locator, raw.score(), tier.order());
    }

    private static HealOutcome executeTierSafe(HealingTier tier, HealingRequest request) {
        HealOutcome raw = null;
        try {
            raw = tier.heal(request);
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Reporter.log("[TIER " + tier.order() + "] heal raised " + e.getClass().getSimpleName()
                    + " — falling through", LogLevel.WARN);
            Reporter.log("[TIER " + tier.order() + "] cause: "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()),
                    LogLevel.DEBUG);
            if (!HealingTelemetryStore.wasRecordedSinceMark()) {
                HealingTelemetryStore.record(tier.order(), request.brokenLocator().toString(),
                        null, 0.0, false, null, null, "backstop-exception");
            }
            return null;
        }

        if (!HealingTelemetryStore.wasRecordedSinceMark()) {
            HealingTelemetryStore.record(tier.order(), request.brokenLocator().toString(), null, 0.0,
                    raw != null && raw.element() != null, null, null, "backstop");
        }
        return raw;
    }

    private static void persistAndRecordHeal(HealingTier tier, HealingRequest request, By locator,
                                              WebElement guarded, double score) {
        AISelfHealer.cacheHealedLocator(request.driver(), request.brokenLocator(),
                locator, score, request.fieldName());

        if (!tier.persistsOwnHeal()) {
            BaselineStore.capture(request.driver(), request.brokenLocator(), guarded,
                    score, tier.order());

            HealingContextBuilder.SourceLocation srcLoc =
                    HealingContextBuilder.resolveSourceLocation(request.stackTrace());

            if (locator != null) {
                AISelfHealer.queueSourcePatch(request.brokenLocator(), locator,
                        srcLoc, score, tier.order());
            }

            String expr = locator != null ? AISelfHealer.byToJavaExpression(locator) : null;
            if (expr == null && locator != null) expr = locator.toString();
            AIHealingReporter.queueChange(
                    srcLoc != null && srcLoc.filePath != null ? srcLoc.filePath : "unknown",
                    request.brokenLocator().toString(),
                    new HealingResult(expr != null ? expr : "unknown",
                            score, "[TIER " + tier.order() + "]"),
                    srcLoc != null ? srcLoc.className : null,
                    srcLoc != null ? srcLoc.methodName : null,
                    request.actionType(),
                    srcLoc != null ? srcLoc.lineNumber : 0);
        }
    }

    private static WebElement resolveInteractiveElement(WebElement healed, String actionType,
                                                        String tierLabel, WebDriver driver) {
        if (healed == null) return null;
        if (InteractiveElements.isTextInputAction(actionType)) {
            return resolveTextInputElement(healed, actionType, tierLabel, driver);
        }
        if (!InteractiveElements.isClickLikeAction(actionType)) return healed;
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
            if (InteractiveElements.TAGS.contains(tag)) return healed;

            if (role != null && InteractiveElements.ROLES.contains(role)) return healed;

            for (String selector : InteractiveElements.INNER_SELECTORS) {
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

    private static final java.util.Set<String> NON_TEXT_INPUT_TYPES = java.util.Set.of(
            "hidden", "button", "submit", "checkbox", "radio", "image");

    private record TextAttributes(String tag, String role, String type, String contentEditable) {}

    private static TextAttributes extractTextAttributes(WebElement healed, WebDriver driver) {
        try {
            Object[] res = (Object[]) ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("var e=arguments[0]; return [e.tagName.toLowerCase(), e.getAttribute('role'), e.getAttribute('type'), e.getAttribute('contenteditable')];", healed);
            if (res != null && res.length >= 4) {
                return new TextAttributes(
                        res[0] != null ? res[0].toString() : healed.getTagName().toLowerCase(),
                        res[1] != null ? res[1].toString() : null,
                        res[2] != null ? res[2].toString() : null,
                        res[3] != null ? res[3].toString() : null
                );
            }
        } catch (Exception ignored) {}
        return new TextAttributes(
                healed.getTagName().toLowerCase(),
                healed.getAttribute("role"),
                healed.getAttribute("type"),
                healed.getAttribute("contenteditable")
        );
    }

    private static boolean isTextInputTarget(TextAttributes attrs) {
        boolean isInputTag = InteractiveElements.TEXT_INPUT_TAGS.contains(attrs.tag());
        boolean isNonTextType = attrs.type() != null && NON_TEXT_INPUT_TYPES.contains(attrs.type().toLowerCase(java.util.Locale.ROOT));
        boolean isEditable = "true".equalsIgnoreCase(attrs.contentEditable());
        boolean isRoleText = attrs.role() != null && InteractiveElements.TEXT_INPUT_ROLES.contains(attrs.role().toLowerCase(java.util.Locale.ROOT));
        return (isInputTag && !isNonTextType) || isEditable || isRoleText;
    }

    private static WebElement findInnerEditable(WebElement healed, String tag, String tierLabel) {
        for (String selector : InteractiveElements.INNER_TEXT_SELECTORS) {
            try {
                WebElement inner = healed.findElement(By.cssSelector(selector));
                if (inner.isDisplayed() && inner.isEnabled()) {
                    Reporter.log("[" + tierLabel + "] Resolved container <" + tag
                            + "> → inner editable <" + inner.getTagName() + ">", LogLevel.INFO_YELLOW);
                    return inner;
                }
            } catch (NoSuchElementException ignored) {}
        }
        Reporter.log("[" + tierLabel + "] Healed element is container <" + tag
                + "> with no editable child for text input action — skipping", LogLevel.WARN);
        return null;
    }

    private static WebElement resolveTextInputElement(WebElement healed, String actionType,
                                                      String tierLabel, WebDriver driver) {
        try {
            TextAttributes attrs = extractTextAttributes(healed, driver);
            if (isTextInputTarget(attrs)) {
                return healed;
            }
            return findInnerEditable(healed, attrs.tag(), tierLabel);
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
