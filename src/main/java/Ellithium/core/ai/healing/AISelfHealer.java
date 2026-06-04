package Ellithium.core.ai.healing;

import Ellithium.core.ai.reporting.AIHealingReporter;
import Ellithium.Utilities.ai.LLMProvider;
import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.HealingStrategy;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealingResult;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AISelfHealer {

    private static final ThreadLocal<LLMProvider> llmProviderThread = new ThreadLocal<>();
    private static final ThreadLocal<HealingStrategy> strategyThread = new ThreadLocal<>();

    private static volatile LLMProvider globalProvider;
    private static volatile HealingStrategy globalStrategy = HealingStrategy.DISABLED;
    private static volatile double confidenceThreshold = 0.85;

    public static class CachedLocator {
        public final By newLocator;
        public final String originalField;
        public CachedLocator(By newLocator, String originalField) {
            this.newLocator = newLocator;
            this.originalField = originalField;
        }
    }
    private static final ConcurrentHashMap<String, CachedLocator> globalHealedCache = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, java.util.concurrent.CompletableFuture<By>> inFlight =
            new ConcurrentHashMap<>();

    private static String cacheKey(WebDriver driver, By brokenLocator) {
        return pageContext(driver) + "##" + brokenLocator.toString();
    }

    private static String pageContext(WebDriver driver) {
        try {
            if (driver instanceof AppiumDriver) {
                Object pkg = ((AppiumDriver) driver).getCapabilities().getCapability("appPackage");
                return pkg != null ? pkg.toString() : "mobile";
            }
            String url = driver.getCurrentUrl();
            if (url == null) return "";
            int q = url.indexOf('?');
            if (q < 0) return url;
            int hash = url.indexOf('#', q);
            return hash >= 0 ? url.substring(0, q) + url.substring(hash) : url.substring(0, q);
        } catch (Exception e) {
            return "";
        }
    }

    public static By getCachedHealedLocator(WebDriver driver, By brokenLocator) {
        CachedLocator cached = globalHealedCache.get(cacheKey(driver, brokenLocator));
        return cached != null ? cached.newLocator : null;
    }

    public static void resetForSuite() {
        globalHealedCache.clear();
        inFlight.clear();
        pendingPatches.clear();
    }

    private static final ConcurrentLinkedQueue<SourcePatch> pendingPatches = new ConcurrentLinkedQueue<>();
    private static final SourcePatchQueue sourcePatchQueue = new SourcePatchQueue(pendingPatches);

    /** Represents a deferred source code patch with heal provenance for gating + conflict resolution. */
    public static class SourcePatch {
        public final String filePath;
        public final String fieldName;
        public final String byMethod;
        public final String byValue;
        public final String newLocatorExpression;
        public final double confidence;
        public final int    tier;
        public SourcePatch(String filePath, String fieldName, String byMethod, String byValue,
                           String newLocatorExpression, double confidence, int tier) {
            this.filePath = filePath;
            this.fieldName = fieldName;
            this.byMethod = byMethod;
            this.byValue = byValue;
            this.newLocatorExpression = newLocatorExpression;
            this.confidence = confidence;
            this.tier = tier;
        }
    }

    private static final ThreadLocal<Double> LAST_HEAL_CONFIDENCE = ThreadLocal.withInitial(() -> 0.0);

    /** Confidence (0.0–1.0) of the most recent accepted Tier 4 heal on this thread. */
    public static double getLastHealConfidence() { return LAST_HEAL_CONFIDENCE.get(); }

    // ──────────────────────── Public Source Patching API ────────────────────────

    public static void queueSourcePatch(By brokenLocator, WebElement healedElement,
                                        StackTraceElement[] stackTrace, double confidence, int tier) {
        try {
            HealingStrategy strategy = getEffectiveStrategy();
            if (strategy != HealingStrategy.HEAL_AND_NOTIFY) return;

            if (confidence < AIConfigLoader.getHealingStoreThreshold()) {
                Reporter.log(String.format("Source patch skipped: Tier %d heal confidence %.2f below "
                        + "store threshold %.2f — source left untouched", tier, confidence,
                        AIConfigLoader.getHealingStoreThreshold()), LogLevel.DEBUG);
                return;
            }

            HealingContextBuilder.SourceLocation srcLoc = HealingContextBuilder.resolveSourceLocation(stackTrace);
            if (srcLoc == null || srcLoc.filePath == null) return;

            HealingContextBuilder.HealingContext tempCtx = new HealingContextBuilder.HealingContext();
            HealingContextBuilder.parseByLocator(brokenLocator.toString(), tempCtx);
            if (tempCtx.byMethod == null) return;

            By healedBy = ElementFingerprint.reconstructLocator(healedElement);
            if (healedBy == null) return;
            String javaExpression = byToJavaExpression(healedBy);
            if (javaExpression == null) return;

            sourcePatchQueue.queue(new SourcePatch(
                    srcLoc.filePath, srcLoc.fieldName,
                    tempCtx.byMethod, tempCtx.byValue, javaExpression, confidence, tier));

            Reporter.log("Source patch queued: By." + tempCtx.byMethod + "(\"" + tempCtx.byValue + "\") → "
                    + javaExpression + " in " + srcLoc.filePath
                    + " (tier " + tier + ", conf " + String.format("%.2f", confidence) + ")", LogLevel.DEBUG);
        } catch (Exception e) {
            // Source patching must never crash the test
        }
    }

    public static String byToJavaExpression(By locator) {
        String str = locator.toString();
        boolean appium = locator instanceof AppiumBy || str.startsWith("AppiumBy.");
        java.util.regex.Pattern p = appium
                ? java.util.regex.Pattern.compile("AppiumBy\\.([a-zA-Z]+):\\s*(.*)")
                : java.util.regex.Pattern.compile("^By\\.([a-zA-Z]+):\\s*(.*)");
        java.util.regex.Matcher m = p.matcher(str);
        if (!m.find()) return null;
        String method = m.group(1);
        String value = escapeJava(m.group(2).trim());
        return (appium ? "AppiumBy." : "By.") + method + "(\"" + value + "\")";
    }

    private static String escapeJava(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // ──────────────────────── Initialization ────────────────────────

    public static void initialize(LLMProvider provider, HealingStrategy strategy, double threshold) {
        globalProvider = provider;
        globalStrategy = strategy;
        confidenceThreshold = threshold;
        Reporter.log("AI Self-Healing initialized | Strategy: " + strategy.name()
                + " | Model: " + provider.getModelName()
                + " | Confidence Threshold: " + threshold, LogLevel.INFO_YELLOW);
    }

    public static void initialize(LLMProvider provider, HealingStrategy strategy) {
        initialize(provider, strategy, 0.85);
    }

    public static void initializeForThread(LLMProvider provider, HealingStrategy strategy) {
        llmProviderThread.set(provider);
        strategyThread.set(strategy);
    }

    private static HealingStrategy getEffectiveStrategy() {
        HealingStrategy ts = strategyThread.get();
        return ts != null ? ts : globalStrategy;
    }

    public static LLMProvider getEffectiveProvider() {
        LLMProvider tp = llmProviderThread.get();
        return tp != null ? tp : globalProvider;
    }

    // ──────────────────────── Main Entry Points ────────────────────────

    public static WebElement attemptHeal(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace) {
        if (getEffectiveStrategy() == HealingStrategy.DISABLED || getEffectiveProvider() == null) {
            return null;
        }
        LAST_HEAL_CONFIDENCE.set(0.0);

        Reporter.log("[TIER 3] AI Self-Healing triggered for locator: " + brokenLocator.toString(), LogLevel.INFO_YELLOW);

        By newLocator = healLocator(driver, brokenLocator, stackTrace);

        if (newLocator != null) {
            CachedLocator cached = globalHealedCache.get(cacheKey(driver, brokenLocator));
            if (cached != null) {
                Reporter.log("AI Self-Healing (cached): reusing healed locator " + cached.newLocator
                        + " for field '" + cached.originalField + "' (original: " + brokenLocator + ")", LogLevel.INFO_YELLOW);
            }
            try {
                WebElement found = driver.findElement(newLocator);
                BaselineStore.capture(driver, brokenLocator, found, getLastHealConfidence(), 3);
                return found;
            } catch (Exception e) {
                Reporter.log("AI Self-Healing: Healed locator also failed: " + e.getMessage(), LogLevel.ERROR);
                globalHealedCache.remove(cacheKey(driver, brokenLocator));
            }
        }
        return null;
    }

    public static By healLocator(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace) {
        HealingStrategy strategy = getEffectiveStrategy();
        LLMProvider provider = getEffectiveProvider();
        if (strategy == HealingStrategy.DISABLED || provider == null) return null;

        String cacheKey = cacheKey(driver, brokenLocator);
        CachedLocator cached = globalHealedCache.get(cacheKey);
        if (cached != null) return cached.newLocator;

        java.util.concurrent.CompletableFuture<By> mine = new java.util.concurrent.CompletableFuture<>();
        java.util.concurrent.CompletableFuture<By> existing = inFlight.putIfAbsent(cacheKey, mine);
        if (existing != null) {
            try {
                return existing.get(AIConfigLoader.getLlmHealMaxWaitMs(),
                        java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                return null;
            }
        }
        try {
            By result = healLocatorInternal(driver, brokenLocator, stackTrace, strategy, provider);
            mine.complete(result);
            return result;
        } catch (RuntimeException re) {
            mine.complete(null);
            Reporter.log("AI Self-Healing: Tier 4 heal aborted by unexpected error — falling through: "
                    + re.getMessage(), LogLevel.WARN);
            return null;
        } finally {
            inFlight.remove(cacheKey, mine);
        }
    }

    private static By healLocatorInternal(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace,
                                           HealingStrategy strategy, LLMProvider provider) {

        HealingContextBuilder.HealingContext ctx =
                HealingContextBuilder.build(driver, brokenLocator, stackTrace, provider, strategy);

        String systemPrompt = HealingPromptBuilder.buildSystemPrompt(ctx.isMobile);
        String userPrompt = HealingPromptBuilder.buildUserPrompt(ctx);

        String llmResponse;
        if (ctx.screenshot != null && provider.supportsVision()) {
            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;
            llmResponse = queryLLMWithVisionRetry(provider, combinedPrompt, ctx.screenshot, 3);
        } else {
            llmResponse = queryLLMWithRetry(provider, systemPrompt, userPrompt, 3);
        }
        if (llmResponse == null || llmResponse.isBlank()) {
            Reporter.log("AI Self-Healing: LLM returned no response after retries", LogLevel.ERROR);
            return null;
        }

        List<HealingResult> candidates = HealingResponseParser.parseMultiCandidateResponse(llmResponse);
        if (candidates.isEmpty()) {
            Reporter.log("AI Self-Healing: Failed to parse any candidates from LLM response", LogLevel.ERROR);
            return null;
        }

        ElementFingerprint baseline = BaselineStore.getBaseline(brokenLocator.toString());
        By acceptedLocator = null;
        HealingResult acceptedResult = null;

        for (HealingResult candidate : candidates) {
            if (!candidate.isConfidentEnough(confidenceThreshold)) {
                Reporter.log("AI Candidate skipped (confidence " + String.format("%.2f", candidate.getConfidence())
                        + " < " + confidenceThreshold + "): " + candidate.getNewLocatorExpression(), LogLevel.INFO_BLUE);
                continue;
            }

            if (strategy == HealingStrategy.SUGGEST_ONLY) {
                Reporter.log("[AI Suggestion] " + candidate.getNewLocatorExpression()
                        + " | Confidence: " + String.format("%.2f", candidate.getConfidence())
                        + " | Reason: " + candidate.getReasoning(), LogLevel.INFO_BLUE);
                continue;
            }

            By candidateLocator = HealingResponseParser.parseByFromExpression(candidate.getNewLocatorExpression());
            if (candidateLocator == null) continue;

            WebElement foundEl;
            try {
                foundEl = driver.findElement(candidateLocator);
            } catch (Exception e) {
                Reporter.log("AI Candidate validation failed (not found): " + candidate.getNewLocatorExpression(), LogLevel.INFO_BLUE);
                continue;
            }

            if (baseline != null) {
                double matchScore = baseline.scoreSimilarity(foundEl);
                if (matchScore < AIConfigLoader.getTier3BaselineMatchFloor()) {
                    if (matchScore == 0.0 && candidate.getConfidence() >= AIConfigLoader.getTier3StaleBaselineConfidenceFloor()
                            && HealingResponseParser.isStableLocatorStrategy(candidateLocator)) {
                        Reporter.log("[TIER 3] Stale baseline (score=0.0); accepting stable-strategy LLM heal "
                                + "(confidence=" + String.format("%.2f", candidate.getConfidence()) + "): "
                                + candidate.getNewLocatorExpression(), LogLevel.WARN);
                    } else {
                        Reporter.log("AI Candidate rejected (baseline mismatch, score=" + String.format("%.2f", matchScore)
                                + "): " + candidate.getNewLocatorExpression(), LogLevel.INFO_BLUE);
                        continue;
                    }
                } else {
                    String baselineTag = baseline.getTagName();
                    if (baselineTag != null && !baselineTag.isBlank()) {
                        try {
                            String candidateTag = foundEl.getTagName();
                            if (candidateTag != null && !candidateTag.equalsIgnoreCase(baselineTag)) {
                                Reporter.log("AI Candidate rejected (tag mismatch: expected <" + baselineTag
                                        + "> but found <" + candidateTag + ">): "
                                        + candidate.getNewLocatorExpression(), LogLevel.INFO_BLUE);
                                continue;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            acceptedLocator = candidateLocator;
            acceptedResult = candidate;
            break;
        }

        if (acceptedLocator == null || acceptedResult == null) {
            if (strategy == HealingStrategy.SUGGEST_ONLY) return null;
            Reporter.log("AI Self-Healing: No candidate passed validation", LogLevel.ERROR);
            return null;
        }

        Reporter.log("AI Healing Accepted: " + acceptedResult.toString(), LogLevel.INFO_GREEN);

        LAST_HEAL_CONFIDENCE.set(acceptedResult.getConfidence());

        String fieldLabel = ctx.fieldName != null ? ctx.fieldName : ctx.methodName;
        globalHealedCache.put(cacheKey(driver, brokenLocator),
                new CachedLocator(acceptedLocator, fieldLabel != null ? fieldLabel : "unknown"));

        AIHealingReporter.queueChange(
                ctx.filePath != null ? ctx.filePath : "unknown",
                brokenLocator.toString(),
                acceptedResult,
                ctx.pageClassName,
                ctx.methodName,
                ctx.actionType,
                ctx.lineNumber);

        if (ctx.filePath != null
                && acceptedResult.getConfidence() >= AIConfigLoader.getHealingStoreThreshold()) {
            sourcePatchQueue.queue(new SourcePatch(
                    ctx.filePath, ctx.fieldName, ctx.byMethod, ctx.byValue,
                    acceptedResult.getNewLocatorExpression(), acceptedResult.getConfidence(), 3));
        }

        if (strategy == HealingStrategy.HEAL_AND_NOTIFY) {
            Reporter.log("[AI HEALED & NOTIFIED] Locator healed: " + brokenLocator
                    + " → " + acceptedResult.getNewLocatorExpression()
                    + " | File: " + (ctx.filePath != null ? ctx.filePath : "unknown"), LogLevel.INFO_YELLOW);
        }

        return acceptedLocator;
    }

    // ──────────────────────── LLM Retry Logic ────────────────────────

    private static String queryLLMWithRetry(LLMProvider provider, String systemPrompt, String userPrompt, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = provider.ask(systemPrompt, userPrompt);
                if (response != null && !response.isBlank()) return response;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    Reporter.log("AI Self-Healing: LLM failed after " + maxRetries + " attempts: " + e.getMessage(), LogLevel.ERROR);
                    return null;
                }
                long waitMs = backoffMs(attempt);
                Reporter.log("AI Self-Healing: LLM attempt " + attempt + " failed, retrying in " + waitMs + "ms...", LogLevel.WARN);
                try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
            }
        }
        return null;
    }

    private static long backoffMs(int attempt) {
        long base = AIConfigLoader.getLlmRetryInitialBackoffMs();
        long max  = AIConfigLoader.getLlmRetryMaxBackoffMs();
        long mult = 1L << Math.min(attempt - 1, 16);
        return Math.min(max, base * mult);
    }

    private static String queryLLMWithVisionRetry(LLMProvider provider, String prompt, byte[] screenshot, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = provider.askWithVision(prompt, screenshot);
                if (response != null && !response.isBlank()) return response;
            } catch (UnsupportedOperationException e) {
                Reporter.log("AI Self-Healing: Vision not supported at runtime, falling back to text-only", LogLevel.WARN);
                return queryLLMWithRetry(provider, "", prompt, maxRetries);
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    Reporter.log("AI Self-Healing: Vision LLM failed after " + maxRetries + " attempts: " + e.getMessage(), LogLevel.ERROR);
                    return null;
                }
                long waitMs = backoffMs(attempt);
                Reporter.log("AI Self-Healing: Vision LLM attempt " + attempt + " failed, retrying in " + waitMs + "ms...", LogLevel.WARN);
                try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
            }
        }
        return null;
    }

    // ──────────────────────── Deferred Patch Application ────────────────────────

    public static Map<String, SourcePatch> resolvePatchConflicts(Iterable<SourcePatch> patches) {
        return sourcePatchQueue.resolvePatchConflicts(patches);
    }

    public static void applyDeferredPatches() {
        sourcePatchQueue.apply();
    }

    // ──────────────────────── Cleanup ────────────────────────

    public static void cleanup() {
        llmProviderThread.remove();
        strategyThread.remove();
        if (!HealingContextBuilder.TIER4_PREP_POOL.isShutdown()) {
            HealingContextBuilder.TIER4_PREP_POOL.shutdown();
        }
    }

    /** Returns the global healed cache (for use by BaseActions findWebElements). */
    public static ConcurrentHashMap<String, CachedLocator> getGlobalHealedCache() {
        return globalHealedCache;
    }
}
