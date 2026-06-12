package Ellithium.core.ai;

import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.scoring.ElementVectorCache;
import Ellithium.core.ai.scoring.SemanticNameExtractor;
import Ellithium.core.ai.scoring.SemanticQueryBuilder;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Tier 2 local embedding healer. See ai-context for architecture notes. */
public class EnsembleHealer {

    private static final String EXTERNAL_MODEL_DIR = System.getProperty("ellithium.ai.modelDir",
            System.getProperty("user.home") + "/.ellithium/ai-model");
    private static final String BGE_QUERY_PREFIX   =
            "Represent this sentence for searching relevant passages: ";
    private static final int MAX_SEQ_LEN     = 256;
    private static final int MAX_TEXT_CHARS  = 240;
    private static final int MAX_CLASS_CHARS = 200;
    private static final double EARLY_EXIT_COMBINED          = 0.95;
    private static final double COSINE_CORROBORATION_FLOOR   = 0.55;

    private static volatile boolean initialized = false;
    private static volatile boolean available   = false;
    private static final java.util.concurrent.atomic.AtomicInteger EMBED_IN_FLIGHT =
            new java.util.concurrent.atomic.AtomicInteger(0);

    private static Object ortEnvironment = null;
    private static Object tokenizer      = null;

    // ORT session pool — sized to min(availableProcessors, 4) so parallel suites don't queue.
    // Each session costs ~34 MB native heap; 4 sessions = 136 MB worst-case, acceptable for
    // CI agents with ≥ 512 MB heap. The old fixed-2 size caused 6/8 threads to wait 8 s each.
    private static final int SESSION_POOL_SIZE =
            Math.min(Runtime.getRuntime().availableProcessors(), 4);
    private static final java.util.concurrent.LinkedBlockingQueue<Object> SESSION_POOL =
            new java.util.concurrent.LinkedBlockingQueue<>(SESSION_POOL_SIZE);

    // ONNX inference is CPU-bound — it MUST run on bounded platform threads, never virtual threads
    // or the shared common ForkJoinPool. Sized to SESSION_POOL_SIZE so the offloaded query-embed
    // never contends for more sessions than exist.
    private static final java.util.concurrent.ExecutorService EMBED_POOL =
            java.util.concurrent.Executors.newFixedThreadPool(SESSION_POOL_SIZE,
                    Thread.ofPlatform().daemon(true).name("ellithium-onnx-embed", 0).factory());

    private static volatile java.util.concurrent.CompletableFuture<Void> INIT_FUTURE;

    public static synchronized void initializeAsync() {
        if (INIT_FUTURE != null || initialized) return;
        java.util.concurrent.CompletableFuture<Void> f = new java.util.concurrent.CompletableFuture<>();
        INIT_FUTURE = f;
        Thread.ofPlatform().daemon(true).name("ellithium-onnx-init").start(() -> {
            try { initialize(); } finally { f.complete(null); }
        });
    }

    static void awaitInit() {
        java.util.concurrent.CompletableFuture<Void> f = INIT_FUTURE;
        if (f == null && !initialized) {
            // Suite startup hook was skipped — kick off async init now and return unavailable
            // for this heal rather than paying model-load cost on the test thread.
            initializeAsync();
            return;
        }
        if (f != null && !f.isDone()) {
            try { f.get(30, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
    }

    private static Method mEncode, mGetIds, mGetMask, mGetTypes, mCreateTensor, mSessionRun, mOnnxGetValue;

    /**
     * Loads the ONNX model and tokenizer from JAR resources.
     * Called by GeneralHandler at suite startup. Safe to call multiple times.
     */
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        byte[] modelBytes = Ellithium.core.ai.crypto.ModelDecryptor.decryptModel();
        if (modelBytes == null) {
            Reporter.log("[ENSEMBLE] Model not found — Tier 2 unavailable", LogLevel.WARN);
            return;
        }

        try {
            byte[] tokenizerBytes = Ellithium.core.ai.crypto.ModelDecryptor.loadTokenizer();
            if (tokenizerBytes == null) {
                String extTok = EXTERNAL_MODEL_DIR + "/tokenizer.json";
                try (java.io.InputStream is = new java.io.FileInputStream(extTok)) {
                    tokenizerBytes = readAllBytes(is);
                } catch (Exception ignored) {}
            }
            if (tokenizerBytes == null) {
                Reporter.log("[ENSEMBLE] Tokenizer not found — Tier 2 unavailable", LogLevel.WARN);
                return;
            }

            initOrtSession(modelBytes);
            // Create SESSION_POOL_SIZE-1 additional sessions on a daemon thread so the pool
            // reaches its configured depth without blocking suite startup.
            final int extraSessions = SESSION_POOL_SIZE - 1;
            if (extraSessions > 0) {
                // Each extra session needs its own copy of modelBytes (zeroed after session creation).
                final byte[][] extraCopies = new byte[extraSessions][];
                for (int i = 0; i < extraSessions; i++) {
                    extraCopies[i] = java.util.Arrays.copyOf(modelBytes, modelBytes.length);
                }
                java.util.Arrays.fill(modelBytes, (byte) 0);
                Thread.ofPlatform().daemon(true).name("ellithium-onnx-pool-expand").start(() -> {
                    for (int i = 0; i < extraSessions; i++) {
                        byte[] copy = extraCopies[i];
                        try {
                            Class<?> optClass2 = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
                            Object opts2 = optClass2.getDeclaredConstructor().newInstance();
                            capOrtThreads(optClass2, opts2);
                            Object sess2 = ortEnvironment.getClass()
                                    .getMethod("createSession", byte[].class, optClass2)
                                    .invoke(ortEnvironment, copy, opts2);
                            java.util.Arrays.fill(copy, (byte) 0);
                            SESSION_POOL.offer(sess2);
                        } catch (Throwable t) {
                            java.util.Arrays.fill(copy, (byte) 0);
                            Reporter.log("[ENSEMBLE] Extra ORT session " + (i + 2) + " failed — pool running at "
                                    + SESSION_POOL.size() + "/" + SESSION_POOL_SIZE + ": "
                                    + t.getClass().getSimpleName(), LogLevel.WARN);
                        }
                    }
                });
            } else {
                java.util.Arrays.fill(modelBytes, (byte) 0);
            }
            initTokenizer(tokenizerBytes);
            available = true;
            // Warmup inference: forces ORT JIT compilation so the first real heal
            // doesn't pay a 2-5× latency penalty.
            try { embed("warmup click button input", false); } catch (Exception ignored) {}
            Reporter.log("[ENSEMBLE] Tier 2 ready", LogLevel.INFO_GREEN);

        } catch (ClassNotFoundException e) {
            Reporter.log("[ENSEMBLE] Required JARs not on classpath (onnxruntime / djl-tokenizers): "
                    + e.getMessage() + " — Tier 2 unavailable", LogLevel.WARN);
        } catch (Exception e) {
            Reporter.log("[ENSEMBLE] Init failed: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage() + " — Tier 2 unavailable", LogLevel.WARN);
        } catch (Throwable t) {
            Reporter.log("[ENSEMBLE] Init failed (native/JVM error): " + t.getClass().getSimpleName()
                    + ": " + t.getMessage() + " — Tier 2 unavailable", LogLevel.WARN);
        }
    }

    /**
     * Closes the ORT session and releases native memory.
     * Called by GeneralHandler / CustomTestNGListener at suite teardown.
     */
    public static synchronized void shutdown() {
        available = false;
        // Drain in-flight embeds before closing the ORT session (max 2 s) to prevent
        // a native crash when shutdown races a concurrent heal on another thread.
        long deadline = System.currentTimeMillis() + 5_000;
        while (EMBED_IN_FLIGHT.get() > 0 && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        Object sess;
        while ((sess = SESSION_POOL.poll()) != null) closeQuietly(sess);
        closeQuietly(ortEnvironment);
        ortEnvironment = null;
        tokenizer      = null;
        initialized    = false;
        INIT_FUTURE    = null;
        ElementVectorCache.getInstance().invalidate();
        Reporter.log("[ENSEMBLE] ONNX session closed", LogLevel.DEBUG);
    }

    public static boolean isAvailable() {
        awaitInit();
        return available;
    }

    public static boolean isModelPresent() {
        if (Ellithium.core.ai.crypto.ModelDecryptor.isModelResourcePresent()) return true;
        return java.nio.file.Files.exists(
                java.nio.file.Paths.get(EXTERNAL_MODEL_DIR, "model_quantized.onnx.enc"));
    }

    /**
     * Attempts to heal a broken locator using local ONNX embedding similarity search.
     * Returns null immediately when unavailable (silent no-op in the cascade).
     */
    public static HealOutcome tryEnsembleHeal(WebDriver driver, By locator,
                                               String actionType, String callerMethod,
                                               String fieldName, String locatorValue,
                                               ElementFingerprint baseline) {
        awaitInit();
        if (!available) return null;

        invalidateCacheOnDomMutation(driver);

        boolean switchedFrame = (baseline != null) && baseline.enterIframeContext(driver);
        if (switchedFrame) {
            Reporter.log("[TIER 2] iframe context switched", LogLevel.DEBUG);
        }
        try {
            String query = SemanticQueryBuilder.buildFromContext(actionType, locatorValue, callerMethod, baseline);
            if (query.isBlank()) return null;

            // Overlap query embedding (ONNX, ~20ms) with DOM candidate collection (WebDriver, ~30ms).
            // DOM calls stay on this thread; embed runs on a pool thread — no WebDriver sharing.
            java.util.concurrent.CompletableFuture<float[]> queryFuture =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> embed(query, true), EMBED_POOL);

            HealOutcome outcome = scoreAndSelectCandidate(driver, queryFuture, baseline, locator,
                    actionType, query, callerMethod, fieldName, locatorValue);
            return outcome;
        } finally {
            if (switchedFrame) {
                try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Embeds text into a 384-dim L2-normalised float vector.
     *
     * @param text    The text to embed
     * @param isQuery True for queries (BGE prefix applied); false for element documents
     * @return L2-normalised float[384], or null on any failure
     */
    static float[] embed(String text, boolean isQuery) {
        if (!available || ortEnvironment == null || tokenizer == null) return null;
        Object session;
        try {
            session = SESSION_POOL.poll(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        if (session == null) return null;
        EMBED_IN_FLIGHT.incrementAndGet();
        Object env = ortEnvironment, tok = tokenizer;
        String input = isQuery ? BGE_QUERY_PREFIX + text : text;
        Object tIds = null, tMask = null, tType = null, result = null;
        try {
            Object encoding = mEncode.invoke(tok, input);

            long[] ids     = (long[]) mGetIds.invoke(encoding);
            long[] mask    = (long[]) mGetMask.invoke(encoding);
            long[] typeIds = (long[]) mGetTypes.invoke(encoding);

            int seqLen = Math.min(Math.min(Math.min(ids.length, mask.length), typeIds.length), MAX_SEQ_LEN);
            long[] idsT  = (seqLen == ids.length)     ? ids     : java.util.Arrays.copyOf(ids, seqLen);
            long[] maskT = (seqLen == mask.length)    ? mask    : java.util.Arrays.copyOf(mask, seqLen);
            long[] typeT = (seqLen == typeIds.length) ? typeIds : java.util.Arrays.copyOf(typeIds, seqLen);

            long[] shape = {1L, (long) seqLen};
            tIds  = mCreateTensor.invoke(null, env, LongBuffer.wrap(idsT),  shape);
            tMask = mCreateTensor.invoke(null, env, LongBuffer.wrap(maskT), shape);
            tType = mCreateTensor.invoke(null, env, LongBuffer.wrap(typeT), shape);

            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("input_ids",      tIds);
            inputs.put("attention_mask", tMask);
            inputs.put("token_type_ids", tType);

            // Each thread holds its own session from the pool — no lock needed.
            float[] pooled;
            {
                try {
                    result = mSessionRun.invoke(session, inputs);
                    Object onnxValue = null;
                    for (Object entry : (Iterable<?>) result) {
                        Map.Entry<?, ?> e = (Map.Entry<?, ?>) entry;
                        String name = e.getKey() != null ? e.getKey().toString() : "";
                        if ("last_hidden_state".equals(name) || "token_embeddings".equals(name)) {
                            onnxValue = e.getValue(); break;
                        }
                        if (onnxValue == null) onnxValue = e.getValue();
                    }
                    if (onnxValue == null) return null;
                    Object rawTensor = mOnnxGetValue.invoke(onnxValue);
                    if (rawTensor instanceof float[][][]) {
                        pooled = clsPool(((float[][][]) rawTensor)[0]);
                    } else if (rawTensor instanceof float[][]) {
                        pooled = ((float[][]) rawTensor)[0].clone();
                    } else {
                        return null;
                    }
                } finally {
                    closeQuietly(result);
                    result = null;
                }
            }

            return l2Normalize(pooled);

        } catch (Exception ex) {
            Reporter.log("[ENSEMBLE] embed failed: " + ex.getMessage(), LogLevel.WARN);
            return null;
        } finally {
            EMBED_IN_FLIGHT.decrementAndGet();
            SESSION_POOL.offer(session);
            closeQuietly(result);
            closeQuietly(tIds);
            closeQuietly(tMask);
            closeQuietly(tType);
        }
    }

    /** Close an AutoCloseable (OnnxTensor / OrtSession.Result) without throwing — frees native memory. */
    private static void closeQuietly(Object o) {
        if (o instanceof AutoCloseable c) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }

    private static final String[] CANDIDATE_SELECTORS = {
        "button, a, input, select, textarea, summary, "
            + "[role='button'], [role='link'], [role='tab'], [role='menuitem'], "
            + "[role='checkbox'], [role='radio'], [role='switch'], [role='option'], [onclick]",
        "[role='alert'], [role='status'], [role='heading'], [role='log'], "
            + "h1, h2, h3, h4, h5, h6, label, legend",
        "[data-testid], [data-test], [data-cy], [aria-label], [placeholder], [title], [name], [id]",
        "p, li, td, th, dt, dd, article, section, aside, span, div",
    };

    private static HealOutcome scoreAndSelectCandidate(WebDriver driver,
                                                       java.util.concurrent.CompletableFuture<float[]> queryFuture,
                                                       ElementFingerprint baseline,
                                                       By brokenLocator, String actionType,
                                                       String query,
                                                       String callerMethod, String fieldName,
                                                       String locatorValue) {
        SemanticLocatorResolver.ElementCategory cat =
                SemanticLocatorResolver.categorizeAction(actionType);
        String category      = cat != null ? cat.name() : null;
        boolean isReadable    = SemanticLocatorResolver.ElementCategory.READABLE == cat;
        double  threshold     = AIConfigLoader.getOnnxSimilarityThreshold();
        int     maxCandidates = AIConfigLoader.getOnnxMaxCandidates();

        String semanticMethodName = isReadable ? null : callerMethod;
        List<String> semanticNames =
                SemanticNameExtractor.extract(semanticMethodName, fieldName, locatorValue);

        java.util.HashMap<WebElement, Double> resolverWeights = new java.util.HashMap<>();
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        List<WebElement> candidates;
        List<Map<String, Object>> batch;
        try {
            List<WebElement> resolverEls = new ArrayList<>();
            for (Ellithium.core.ai.models.SemanticHit hit : SemanticLocatorResolver.findSemanticHits(
                    driver, callerMethod, fieldName, actionType, locatorValue, baseline)) {
                resolverEls.add(hit.element);
                Double prev = resolverWeights.get(hit.element);
                if (prev == null || prev < hit.tierWeight) resolverWeights.put(hit.element, hit.tierWeight);
            }
            candidates = mergeCandidates(resolverEls, collectCandidates(driver, baseline, actionType));
            batch = fetchCandidateAttributes(driver, candidates);
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }

        // DOM collection done — now join the query embedding started concurrently above.
        float[] queryVector;
        try {
            queryVector = queryFuture.get(8, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return null;
        }
        if (queryVector == null) return null;

        record CandidateSlot(WebElement el, Map<String, Object> attrs, boolean isResolver, double f2pre) {}

        final int maxResolverCandidates = Math.min(resolverWeights.size(), 8);

        List<CandidateSlot> slots = new ArrayList<>(Math.min(candidates.size(), maxCandidates + maxResolverCandidates));
        for (int i = 0; i < candidates.size(); i++) {
            WebElement candidate = candidates.get(i);
            Map<String, Object> attrs = (batch != null && i < batch.size()) ? batch.get(i) : null;
            if (batch != null && attrs == null) continue;
            if (attrs != null) {
                if (Boolean.FALSE.equals(attrs.get("visible"))) continue;
            }
            boolean isResolver = resolverWeights.containsKey(candidate);
            double rawAttrW = (attrs != null)
                    ? SemanticLocatorResolver.strategyWeightForAttrs(attrs, semanticNames)
                    : Double.NaN;
            double attrW = (rawAttrW <= 0.0) ? Double.NaN : rawAttrW;
            Double hitW  = resolverWeights.get(candidate);
            double f2pre = maxScore(attrW, hitW == null ? Double.NaN : hitW);
            slots.add(new CandidateSlot(candidate, attrs, isResolver, f2pre));
        }
        slots.sort((a, b) -> {
            if (a.isResolver != b.isResolver) return a.isResolver ? -1 : 1;
            boolean aNaN = Double.isNaN(a.f2pre), bNaN = Double.isNaN(b.f2pre);
            if (aNaN && bNaN) return 0;
            if (aNaN) return 1;
            if (bNaN) return -1;
            return Double.compare(b.f2pre, a.f2pre);
        });

        WebElement bestElement = null;
        String     bestDoc     = null;
        Map<String, Object> bestAttrs = null;
        double bestCombined = -1.0, bestCosine = 0.0, bestF1 = Double.NaN, bestF2 = Double.NaN;
        int poolScored = 0;
        int resolverScored = 0;

        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            for (CandidateSlot slot : slots) {
                if (slot.isResolver && resolverScored >= maxResolverCandidates) continue;
                if (!slot.isResolver && poolScored    >= maxCandidates)         break;
                try {
                    Map<String, Object> attrs = slot.attrs;
                    WebElement candidate = slot.el;
                    if (attrs == null && !candidate.isDisplayed()) continue;
                    if (slot.isResolver) resolverScored++; else poolScored++;
                    String  cacheKey  = (attrs != null) ? buildCacheKey(attrs) : buildCacheKey(candidate);
                    float[] docVector = ElementVectorCache.getInstance().get(cacheKey);
                    String  doc       = null;
                    if (docVector == null) {
                        doc = (attrs != null) ? buildElementDocument(attrs, candidate)
                                              : buildElementDocument(candidate);
                        if (doc.isBlank()) continue;
                        docVector = embed(doc, false);
                        if (docVector != null && !cacheKey.isEmpty()) {
                            ElementVectorCache.getInstance().put(cacheKey, docVector);
                        }
                    }
                    if (docVector == null) continue;

                    double cosine = dotProduct(queryVector, docVector);
                    double f1     = (attrs != null) ? baselineProximity(baseline, attrs)
                                                    : baselineProximity(baseline, candidate);
                    double f2     = slot.f2pre;
                    double combined = fuseConfidence(f2, cosine);
                    if (!Double.isNaN(f1) && f1 > 0.5) {
                        combined = Math.min(1.0, combined + (f1 - 0.5) * 0.08);
                    }

                    if (bestElement == null || combined > bestCombined
                            || (Math.abs(combined - bestCombined) < 1e-6 && f1 > bestF1)) {
                        bestElement = candidate; bestCombined = combined; bestCosine = cosine;
                        bestF1 = f1; bestF2 = f2; bestDoc = doc; bestAttrs = attrs;
                    }

                    if (combined >= EARLY_EXIT_COMBINED && cosine >= COSINE_CORROBORATION_FLOOR) break;
                } catch (Exception ignored) {}
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }

        if (bestElement == null) return null;
        if (bestDoc == null) bestDoc = "(vector-cache hit)";

        // Verify winner is still live; the DOM may have re-rendered between batch-read and now.
        bestElement = ensureLive(driver, bestElement, bestAttrs);
        if (bestElement == null) return null;

        GateResult gate = decideGate(bestCombined, threshold, bestF2,
                AIConfigLoader.isStrategyRescueEnabled(), bestCosine);
        if (gate.accept) {
            Reporter.log(String.format("[TIER 2] healed via %s (combined=%.3f f2=%.2f f3=%.3f)",
                    gate.via, gate.score, bestF2, bestCosine), LogLevel.INFO_GREEN);
            HealingTelemetryStore.record(2, brokenLocator.toString(), bestDoc, gate.score, true, query, category);
            return HealOutcome.of(bestElement, gate.score, 2);
        }
        Reporter.log(String.format("[TIER 2] no heal — combined=%.3f < threshold=%.3f", bestCombined, threshold), LogLevel.DEBUG);
        HealingTelemetryStore.record(2, brokenLocator.toString(), bestDoc, bestCombined, false, query, category);
        return null;
    }

    // DOM mutation detection — generation counter approach.
    //
    // The JS side stores a monotonically incrementing integer (__ellHealGen) on window.
    // Each Java thread records the generation it last saw (threadLastGen) and invalidates
    // its per-thread vector cache only when the counter has advanced since its last check.
    // This is race-free: two threads reading the same counter value both see the mutation
    // and both invalidate independently, with no shared boolean to clobber.
    private static final String MUTATION_PROBE_SCRIPT =
            "if(window.__ellHealGen===undefined){"
            + "  window.__ellHealGen=0;"
            + "  try{ new MutationObserver(function(){window.__ellHealGen=(window.__ellHealGen+1)|0;})"
            + "    .observe(document.documentElement,{childList:true,subtree:true,attributes:true});"
            + "  }catch(e){}"
            + "}"
            + "return window.__ellHealGen;";

    // Per-thread generation stamp: the value of __ellHealGen the last time this thread checked.
    private static final ThreadLocal<Long> threadLastGen = ThreadLocal.withInitial(() -> 0L);

    private static void invalidateCacheOnDomMutation(WebDriver driver) {
        if (!(driver instanceof org.openqa.selenium.JavascriptExecutor js)) return;
        try {
            Object result = js.executeScript(MUTATION_PROBE_SCRIPT);
            if (result instanceof Number n) {
                long currentGen = n.longValue();
                long lastGen    = threadLastGen.get();
                if (currentGen != lastGen) {
                    threadLastGen.set(currentGen);
                    ElementVectorCache.getInstance().invalidate();
                }
            }
        } catch (Exception ignored) {}
    }

    private static double maxScore(double a, double b) {
        if (Double.isNaN(a)) return b;
        if (Double.isNaN(b)) return a;
        return Math.max(a, b);
    }

    private static final double SIGNAL_AGREEMENT = 0.35;

    /**
     * Fuses the resolver/strategy signal (f2) and the bi-encoder cosine (f3) into one confidence.
     * f3 is the primary signal — it is calibrated and semantically grounded. f2 (strategy heuristic)
     * adds a proportional bonus over the remaining headroom to 1.0, capped by SIGNAL_AGREEMENT so
     * f2 can never dominate. Max f2 boost = SIGNAL_AGREEMENT * (1 - f3): shrinks as f3 rises.
     * Hard cosine floor: f3 below GATE_RESCUE_COSINE_FLOOR returns f3 unchanged — wrong-type
     * attribute matches cannot override a disagreeing bi-encoder regardless of how high f2 is.
     */
    static double fuseConfidence(double f2, double f3) {
        if (Double.isNaN(f2)) return f3;
        if (f3 < GATE_RESCUE_COSINE_FLOOR && f2 > f3) return f3;
        return Math.min(1.0, f3 + (1.0 - f3) * f2 * SIGNAL_AGREEMENT);
    }

    static List<WebElement> mergeCandidates(List<WebElement> resolverElements, List<WebElement> poolElements) {
        java.util.LinkedHashSet<WebElement> ordered = new java.util.LinkedHashSet<>();
        if (resolverElements != null) ordered.addAll(resolverElements);
        if (poolElements != null) ordered.addAll(poolElements);
        return new ArrayList<>(ordered);
    }

    static final class GateResult {
        final boolean accept; final double score; final String via;
        GateResult(boolean accept, double score, String via) {
            this.accept = accept; this.score = score; this.via = via;
        }
    }

    private static final double GATE_STRATEGY_MIN       = 0.75;
    private static final double GATE_RESCUE_COSINE_FLOOR = 0.50;

    /** Bump when the ONNX model is updated — invalidates all stale per-element vector cache entries. */
    static final String MODEL_VERSION = "v1";

    /**
     * Accept decision (pure, unit-testable). Path A: the fused combined score clears the calibrated
     * threshold. Path B (strategy-rescue): a gold-tier match (f2 ≥ 0.95: exact data-testid /
     * AppiumBy / cross-validated mutation) corroborated by the bi-encoder cosine (f3 ≥ 0.35) —
     * two genuinely independent signals (algorithmic strategy vs. learned embedding) so the heal is
     * trusted even when combined hasn't cleared the threshold. Using cosine as the second gate instead
     * of the baseline fingerprint means cold-start elements (no stored baseline) can still be rescued,
     * and avoids the correlated-id-read-twice issue that occurs when f1 and f2 both key off the same
     * attribute. Path-B confidence = mean(f2, f3).
     */
    static GateResult decideGate(double combined, double threshold, double f2,
                                 boolean rescueEnabled, double f3) {
        if (combined >= threshold) return new GateResult(true, combined, "ensemble");
        if (rescueEnabled && f2 >= GATE_STRATEGY_MIN && f3 >= GATE_RESCUE_COSINE_FLOOR) {
            return new GateResult(true, (f2 + f3) / 2.0, "strategy-rescue");
        }
        return new GateResult(false, combined, "none");
    }

    /**
     * Collects candidate elements for ALL actions from one inclusive, priority-ordered list.
     * Nothing is excluded by tag/category — the baseline is used only for SCORING (tiebreak), never
     * to filter out elements, so an element that changed tag/structure is still reachable. Dedup by
     * element preserves priority order; the per-attempt cap (in the scoring loop) bounds the cost.
     */
    private static final int MOBILE_NATIVE_HARD_LIMIT = 50;

    private static final String[] APPIUM_NATIVE_SELECTORS = {
            "//*[@clickable='true']",
            "//*[@focusable='true' or @focused='true']",
            "//*[@text and string-length(@text) > 0]",
            "//*[@content-desc and string-length(@content-desc) > 0]",
            "//*[@resource-id and string-length(@resource-id) > 0]"
    };

    private static final String PRIORITY_COLLECT_SCRIPT =
            "var sels=arguments[0],lim=arguments[1],seen=new Set(),out=[];"
            + "for(var i=0;i<sels.length&&out.length<lim;i++){"
            + " var els=document.querySelectorAll(sels[i]);"
            + " for(var j=0;j<els.length&&out.length<lim;j++)"
            + "  if(!seen.has(els[j])){seen.add(els[j]);out.push(els[j]);}"
            + "} return out;";

    private static List<WebElement> collectCandidates(WebDriver driver,
                                                       ElementFingerprint baseline,
                                                       String actionType) {
        DriverProfile profile = DriverProfile.detect(driver);
        if (profile == DriverProfile.MOBILE_NATIVE) {
            return collectAppiumNativeCandidates(driver);
        }
        int hardLimit = AIConfigLoader.getOnnxHardCandidateLimit();
        java.util.LinkedHashSet<WebElement> seen = new java.util.LinkedHashSet<>();

        if (driver instanceof org.openqa.selenium.JavascriptExecutor js) {
            try {
                Object res = js.executeScript(PRIORITY_COLLECT_SCRIPT, CANDIDATE_SELECTORS, hardLimit);
                if (res instanceof List<?> rows) {
                    for (Object o : rows) if (o instanceof WebElement w) seen.add(w);
                }
            } catch (Exception ignored) {}
        }
        if (seen.isEmpty()) {
            outer:
            for (String selector : CANDIDATE_SELECTORS) {
                try {
                    for (WebElement el : driver.findElements(By.cssSelector(selector))) {
                        seen.add(el);
                        if (seen.size() >= hardLimit) break outer;
                    }
                } catch (Exception ignored) {}
            }
        }
        if (seen.size() < hardLimit) {
            seen.addAll(collectShadowDomCandidates(driver, hardLimit - seen.size()));
        }
        return new ArrayList<>(seen);
    }

    private static List<WebElement> collectShadowDomCandidates(WebDriver driver, int limit) {
        if (!(driver instanceof org.openqa.selenium.JavascriptExecutor) || limit <= 0) return List.of();
        try {
            Object res = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var sel=arguments[0], lim=arguments[1], out=[];"
                    + "function walk(root){"
                    + " if(out.length>=lim) return;"
                    + " var els=root.querySelectorAll(sel);"
                    + " for(var i=0;i<els.length && out.length<lim;i++) out.push(els[i]);"
                    + " var all=root.querySelectorAll('*');"
                    + " for(var j=0;j<all.length && out.length<lim;j++) if(all[j].shadowRoot) walk(all[j].shadowRoot);"
                    + "}"
                    + "walk(document); return out;",
                    SHADOW_INTERACTIVE_SELECTOR, limit);
            if (res instanceof List<?> rows) {
                List<WebElement> out = new ArrayList<>(rows.size());
                for (Object o : rows) if (o instanceof WebElement w) out.add(w);
                return out;
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    private static final String SHADOW_INTERACTIVE_SELECTOR =
            "input,button,select,textarea,a,form,label,[role='button'],[role='link'],"
            + "[role='textbox'],[role='checkbox'],[role='radio'],[role='tab'],[role='menuitem'],[data-testid]";

    private static List<WebElement> collectAppiumNativeCandidates(WebDriver driver) {
        java.util.LinkedHashSet<WebElement> seen = new java.util.LinkedHashSet<>();
        outer:
        for (String xp : APPIUM_NATIVE_SELECTORS) {
            try {
                for (WebElement el : driver.findElements(By.xpath(xp))) {
                    seen.add(el);
                    if (seen.size() >= MOBILE_NATIVE_HARD_LIMIT) break outer;
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(seen);
    }

    /**
     * Cache key for an element's embedding vector. Returns "" (do-not-cache) unless the element has a
     * STABLE identity (id / name / data-testid) — caching on tag alone collided every attribute-less
     * &lt;div&gt; to one key ("div||") and served the wrong vector. Callers skip caching on "".
     */
    private static String buildCacheKey(WebElement el) {
        try {
            // Fallback path (no batched attrs): no text-hash — reading getText() here would
            // re-introduce a per-element round-trip the batch exists to avoid.
            return cacheKey(el.getTagName(), el.getAttribute("id"),
                    el.getAttribute("name"), el.getAttribute("data-testid"),
                    el.getAttribute("resource-id"), el.getAttribute("accessibility-id"),
                    el.getAttribute("content-desc"), null);
        } catch (Exception e) { return ""; }
    }

    /** Cache key from pre-fetched (batched) attributes — identical format to the WebElement version. */
    private static String buildCacheKey(Map<String, Object> attrs) {
        return cacheKey(strOf(attrs.get("tag")), strOf(attrs.get("id")),
                strOf(attrs.get("name")), strOf(attrs.get("data-testid")),
                strOf(attrs.get("resource-id")), strOf(attrs.get("accessibility-id")),
                strOf(attrs.get("content-desc")), strOf(attrs.get("text")));
    }

    private static String cacheKey(String tag, String id, String name, String testid,
                                   String resId, String accId, String contentDesc, String text) {
        boolean hasIdentity = (id != null && !id.isBlank())
                || (name != null && !name.isBlank())
                || (testid != null && !testid.isBlank())
                || (resId != null && !resId.isBlank())
                || (accId != null && !accId.isBlank())
                || (contentDesc != null && !contentDesc.isBlank());
        if (!hasIdentity) return "";   // no stable key → never cache (avoid collisions)
        // text-hash: a re-rendered element that keeps its id but changes its text yields a new key,
        // so an SPA mutation is a natural cache miss (re-embed) rather than a stale-vector hit.
        String textHash = (text == null || text.isBlank()) ? "" : Integer.toHexString(text.hashCode());
        return MODEL_VERSION + "|" + nz(tag) + "|" + nz(id) + "|" + nz(name) + "|" + nz(testid)
                + "|" + nz(resId) + "|" + nz(accId) + "|" + nz(contentDesc) + "|" + textHash;
    }

    /**
     * Fetches the unambiguous string attributes for ALL candidates in ONE executeScript round-trip.
     * Returns a list aligned by index with {@code candidates} (entries may be null); returns null when
     * the driver has no JS (Appium native) or the call fails → caller falls back to per-element reads.
     * Excludes {@code type} (Selenium default-value semantics) and text/visibility (read natively).
     */
    static List<Map<String, Object>> fetchCandidateAttributes(WebDriver driver,
                                                              List<WebElement> candidates) {
        return Ellithium.core.ai.dom.CandidateAttributeBatcher.fetch(driver, candidates);
    }

    private static String nz(String s) { return s != null ? s : ""; }

    /**
     * Scores a single element against a baseline fingerprint using one batched JS round-trip.
     * Returns the actual similarity (floored to 0.60) when a baseline is present, or 0.65 as a
     * conservative cold-start estimate. Falls back to the live-WebElement overload only when the
     * driver has no JS (Appium native).
     */
    public static double scoreWithBatchedAttrs(Ellithium.core.ai.models.ElementFingerprint baseline,
                                               WebDriver driver,
                                               WebElement element) {
        double fallback = AIConfigLoader.getSemanticFallbackScore();
        double floor    = AIConfigLoader.getOnnxSimilarityThreshold();
        if (baseline == null) return fallback;
        List<Map<String, Object>> batch = Ellithium.core.ai.dom.CandidateAttributeBatcher.fetch(
                driver, List.of(element));
        if (batch != null && !batch.isEmpty() && batch.get(0) != null) {
            return Math.max(floor, baselineProximity(baseline, batch.get(0)));
        }
        try {
            return Math.max(floor, baseline.scoreSimilarity(element));
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Tiebreak signal for near-equal cosine scores: how well the candidate matches the stored
     * baseline fingerprint (0.0–1.0). Returns -1 when there is no baseline, so DOM order is kept.
     */
    private static double baselineProximity(ElementFingerprint baseline, WebElement candidate) {
        if (baseline == null) return 0.0;
        try {
            return baseline.scoreSimilarity(candidate);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static double baselineProximity(ElementFingerprint baseline, Map<String, Object> attrs) {
        if (baseline == null) return 0.0;
        try {
            return baseline.scoreSimilarity(attrs, structuralFrom(attrs));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static WebElement ensureLive(WebDriver driver, WebElement el, Map<String, Object> attrs) {
        try {
            el.isEnabled();
            return el;
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            WebElement refreshed = tryRefreshStale(driver, attrs);
            if (refreshed != null) Reporter.log("[ENSEMBLE] healed element was stale — re-found via stable locator", LogLevel.DEBUG);
            return refreshed;
        } catch (Exception e) {
            return el;
        }
    }

    private static WebElement tryRefreshStale(WebDriver driver, Map<String, Object> attrs) {
        if (attrs == null) return null;
        String id = strOf(attrs.get("id"));
        if (id != null && !id.isBlank()) {
            try { return driver.findElement(By.id(id)); } catch (Exception ignored) {}
        }
        String testId = strOf(attrs.get("data-testid"));
        if (testId != null && !testId.isBlank()) {
            try { return driver.findElement(By.cssSelector(
                    "[data-testid='" + testId.replace("\\", "\\\\").replace("'", "\\'") + "']")); } catch (Exception ignored) {}
        }
        String name = strOf(attrs.get("name"));
        if (name != null && !name.isBlank()) {
            try { return driver.findElement(By.name(name)); } catch (Exception ignored) {}
        }
        String ariaLabel = strOf(attrs.get("aria-label"));
        if (ariaLabel != null && !ariaLabel.isBlank()) {
            try { return driver.findElement(By.cssSelector(
                    "[aria-label='" + ariaLabel.replace("\\", "\\\\").replace("'", "\\'") + "']")); } catch (Exception ignored) {}
        }
        return null;
    }

    private static ElementFingerprint.StructuralContext structuralFrom(Map<String, Object> attrs) {
        Object pt = attrs.get("parent-tag");
        if (pt == null && attrs.get("child-index") == null) return null;
        int ci = attrs.get("child-index") instanceof Number n ? n.intValue() : -1;
        return new ElementFingerprint.StructuralContext(
                pt != null ? pt.toString() : null, ci,
                strOf(attrs.get("prev-sib")), strOf(attrs.get("next-sib")));
    }

    /**
     * CLS pooling: the BGE sentence embedding is the [CLS] token's hidden state (row 0 of the
     * sequence). Cloned so the subsequent in-place L2 normalisation never mutates the ORT output
     * buffer. Attention mask is irrelevant for CLS (token 0 is always present/attended).
     */
    private static float[] clsPool(float[][] tokenEmbeddings) {
        return tokenEmbeddings[0].clone();
    }

    private static float[] l2Normalize(float[] v) {
        double norm = 0.0;
        for (float f : v) norm += (double) f * f;
        norm = Math.sqrt(norm);
        if (norm > 1e-9) for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
        return v;
    }

    static double dotProduct(float[] a, float[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) sum += (double) a[i] * b[i];
        return sum;
    }

    /**
     * Cosine similarity between two vectors (handles non-normalised input).
     * Already-normalised vectors can use dotProduct() directly.
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot   += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static void initOrtSession(byte[] modelBytes) throws Exception {
        Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
        ortEnvironment = envClass.getMethod("getEnvironment").invoke(null);

        Class<?> optClass = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
        Object   opts     = optClass.getDeclaredConstructor().newInstance();
        capOrtThreads(optClass, opts);

        Object newSession = ortEnvironment.getClass()
                .getMethod("createSession", byte[].class, optClass)
                .invoke(ortEnvironment, modelBytes, opts);
        SESSION_POOL.offer(newSession);

        Class<?> tensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
        mCreateTensor = tensorClass.getMethod("createTensor", envClass, LongBuffer.class, long[].class);
        mSessionRun   = newSession.getClass().getMethod("run", Map.class);
        mOnnxGetValue = Class.forName("ai.onnxruntime.OnnxValue").getMethod("getValue");
    }

    /**
     * Bounds the ORT intra-op pool so it does not grab every core on a large machine, WITHOUT
     * disabling spin-wait — back-to-back per-candidate embeds need the spinning thread pool to stay
     * fast (parking + re-waking a single thread per run made a heal take ~100s). A small cap
     * (≤4 threads) keeps inference parallel and quick while leaving cores for the browser. Best-effort:
     * a missing method on an older onnxruntime is ignored.
     */
    private static void capOrtThreads(Class<?> optClass, Object opts) {
        int intra = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()));
        try { optClass.getMethod("setIntraOpNumThreads", int.class).invoke(opts, intra); } catch (Throwable ignored) {}
        try { optClass.getMethod("setInterOpNumThreads", int.class).invoke(opts, 1); } catch (Throwable ignored) {}
    }

    private static void initTokenizer(byte[] tokenizerBytes) throws Exception {
        Path tmp = Files.createTempFile("ell-tok-", ".json");
        try {
            Files.write(tmp, tokenizerBytes);
            Class<?> cls = Class.forName("ai.djl.huggingface.tokenizers.HuggingFaceTokenizer");
            tokenizer = cls.getMethod("newInstance", Path.class).invoke(null, tmp);

            mEncode = cls.getMethod("encode", String.class);
            Class<?> encClass = Class.forName("ai.djl.huggingface.tokenizers.Encoding");
            mGetIds   = encClass.getMethod("getIds");
            mGetMask  = encClass.getMethod("getAttentionMask");
            mGetTypes = encClass.getMethod("getTypeIds");
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }

    private static final String[] DOC_FIELD_ORDER = {
        "id", "name", "resource-id", "accessibility-id", "aria-label", "content-desc", "role",
        "placeholder", "data-testid", "data-test", "title", "type", "label"
    };

    /**
     * Single source of truth for the element-document string. Both the per-WebElement path and the
     * batched-attributes path call this with their own attribute resolver, so the document is
     * byte-identical regardless of how the values were fetched (guarantees train/serve + cache parity).
     *
     * @param attr resolves an attribute name → value (or null)
     * @param tag  element tag (lowercase), or null
     * @param text element visible text (Selenium getText semantics), or null
     */
    private static String assembleDocument(java.util.function.Function<String, String> attr,
                                           String tag, String text) {
        StringBuilder sb = new StringBuilder();
        for (String field : DOC_FIELD_ORDER) appendVal(sb, attr.apply(field));
        String cls = attr.apply("class");
        if (cls != null && !cls.isBlank()) {
            String tokens = cls.replaceAll("[_-]", " ").replaceAll("\\s{2,}", " ").toLowerCase().trim();
            if (tokens.length() > MAX_CLASS_CHARS) tokens = tokens.substring(0, MAX_CLASS_CHARS);
            sb.append(" ").append(tokens);
        }
        if (tag != null && !tag.isBlank()) sb.append(" ").append(tag.trim());
        if (text != null && !text.isBlank()) {
            String t = text.trim();
            if (t.length() > MAX_TEXT_CHARS) t = t.substring(0, MAX_TEXT_CHARS);
            sb.append(" ").append(t);
        }
        return sb.toString().trim();
    }

    /**
     * Builds an element document by reading each attribute from the live WebElement (one WebDriver
     * round-trip per attribute). Used for single-element callers and as the fallback when the
     * batched read is unavailable.
     */
    public static String buildElementDocument(WebElement element) {
        if (element == null) return "";
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            return assembleDocument(name -> safeAttr(element, name), safeTag(element), safeText(element));
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
    }

    /**
     * Builds an element document from PRE-FETCHED attributes (one batched executeScript for all
     * candidates) — eliminates ~12 per-candidate WebDriver round-trips. {@code type} and {@code text}
     * stay native because Selenium's getAttribute("type") (default "text" for inputs) and getText()
     * (rendered, whitespace-collapsed) have semantics plain JS does not replicate; reading them
     * natively keeps the document identical to {@link #buildElementDocument(WebElement)}.
     */
    private static String buildElementDocument(Map<String, Object> attrs, WebElement element) {
        if (attrs == null) return buildElementDocument(element);
        String tag = strOf(attrs.get("tag"));
        String text = strOf(attrs.get("text"));
        String base = assembleDocument(name -> strOf(attrs.get(name)), tag, text);
        // Append custom data-* values from the batch-collected dataAttrs map
        Object da = attrs.get("dataAttrs");
        if (da instanceof java.util.Map<?, ?> dm && !dm.isEmpty()) {
            java.util.Map<String, String> custom = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<?, ?> e : dm.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    custom.put(e.getKey().toString(), e.getValue().toString());
                }
            }
            return appendCustomDataAttrs(base, custom);
        }
        return base;
    }

    private static String safeAttr(WebElement el, String name) {
        try { return el.getAttribute(name); } catch (Exception e) { return null; }
    }
    private static String safeTag(WebElement el) {
        try { return el.getTagName(); } catch (Exception e) { return null; }
    }
    private static String safeText(WebElement el) {
        try { return el.getText(); } catch (Exception e) { return null; }
    }
    private static String strOf(Object o) { return o != null ? o.toString() : null; }

    /**
     * Builds an element document from a stored {@link ElementFingerprint}, using the SAME canonical
     * field order as {@link #buildElementDocument(WebElement)} so calibration scores documents in the
     * exact shape the model sees at train and inference time (no third doc format).
     *
     * <p>Delegates to {@link #assembleDocument} with a lambda resolver from fingerprint getters —
     * this guarantees automatic field-order parity with the live/batched paths even when new fields
     * (e.g. Appium resource-id, accessibility-id) are added to ElementFingerprint. Null fields are
     * silently skipped, matching the behaviour of the live builder when an attribute is absent.
     */
    public static String buildElementDocument(ElementFingerprint fp) {
        if (fp == null) return "";
        String base = assembleDocument(name -> switch (name) {
            case "id"               -> fp.getId();
            case "name"             -> fp.getName();
            case "resource-id"      -> fp.getResourceId();
            case "accessibility-id" -> fp.getAccessibilityId();
            case "content-desc"     -> fp.getContentDesc();
            case "class"            -> fp.getClassName();
            case "aria-label"       -> fp.getAriaLabel();
            case "role"             -> fp.getRole();
            case "placeholder"      -> fp.getPlaceholder();
            case "data-testid"      -> fp.getDataTestId();
            case "data-test"        -> fp.getDataTest();
            case "data-cy"          -> fp.getDataCy();
            case "data-qa"          -> fp.getDataQa();
            case "title"            -> fp.getTitle();
            case "type"             -> fp.getType();
            case "label"            -> fp.getLabel();
            default                 -> null;
        }, fp.getTagName(), fp.getText());
        return appendCustomDataAttrs(base, fp.getCustomDataAttrs());
    }

    /**
     * Builds an element document from a plain attribute map (offline / measurement use — the Tier-3
     * validation mini-set). Uses the SAME {@link #assembleDocument} field ordering as the live paths,
     * so a candidate scored offline is byte-identical to what the model sees at serve time. Keys are
     * the canonical field names (id, name, resource-id, accessibility-id, aria-label, content-desc,
     * role, placeholder, data-testid, data-test, title, type, label, class) plus {@code tag} and
     * {@code text}. Missing keys are skipped.
     */
    public static String buildElementDocument(Map<String, String> attrs) {
        if (attrs == null || attrs.isEmpty()) return "";
        return assembleDocument(attrs::get, attrs.get("tag"), attrs.get("text"));
    }

    private static void appendVal(StringBuilder sb, String v) {
        if (v != null && !v.isBlank()) sb.append(" ").append(v.trim());
    }

    /** Appends custom data-* attribute values (e.g. data-automation-id) to the document tail. */
    private static String appendCustomDataAttrs(String base, java.util.Map<String, String> custom) {
        if (custom == null || custom.isEmpty()) return base;
        StringBuilder sb = new StringBuilder(base);
        for (String v : custom.values()) {
            if (v != null && !v.isBlank()) sb.append(" ").append(v.trim().toLowerCase());
        }
        return sb.toString().trim();
    }


    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toByteArray();
    }
}
