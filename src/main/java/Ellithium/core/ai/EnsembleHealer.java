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
    private static final double EARLY_EXIT_COSINE = 0.95;

    private static volatile boolean initialized = false;
    private static volatile boolean available   = false;

    private static Object ortEnvironment = null;
    private static Object ortSession     = null;
    private static Object tokenizer      = null;

    private static volatile java.util.concurrent.CompletableFuture<Void> INIT_FUTURE;

    public static synchronized void initializeAsync() {
        if (INIT_FUTURE != null || initialized) return;
        java.util.concurrent.CompletableFuture<Void> f = new java.util.concurrent.CompletableFuture<>();
        INIT_FUTURE = f;
        Thread t = new Thread(() -> {
            try { initialize(); } finally { f.complete(null); }
        }, "ellithium-onnx-init");
        t.setDaemon(true);
        t.start();
    }

    static void awaitInit() {
        java.util.concurrent.CompletableFuture<Void> f = INIT_FUTURE;
        if (f != null && !f.isDone()) {
            try { f.get(30, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        if (!initialized) initialize();
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

            int modelKb = modelBytes.length / 1024;
            initOrtSession(modelBytes);
            java.util.Arrays.fill(modelBytes, (byte) 0);
            initTokenizer(tokenizerBytes);
            available = true;
            Reporter.log("[ENSEMBLE] ONNX session active — " + modelKb
                    + " KB INT8 model loaded | pooling=cls", LogLevel.INFO_GREEN);

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
        closeQuietly(ortSession);
        closeQuietly(ortEnvironment);
        ortSession     = null;
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

        String query = SemanticQueryBuilder.buildFromContext(actionType, locatorValue, callerMethod, baseline);
        if (query.isBlank()) return null;

        float[] queryVector = embed(query, true);
        if (queryVector == null) return null;

        return scoreAndSelectCandidate(driver, queryVector, baseline, locator, actionType, query,
                callerMethod, fieldName, locatorValue);
    }

    /**
     * Embeds text into a 384-dim L2-normalised float vector.
     *
     * @param text    The text to embed
     * @param isQuery True for queries (BGE prefix applied); false for element documents
     * @return L2-normalised float[384], or null on any failure
     */
    static float[] embed(String text, boolean isQuery) {
        Object session = ortSession, env = ortEnvironment, tok = tokenizer;
        if (!available || session == null || env == null || tok == null) return null;
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

            result = mSessionRun.invoke(session, inputs);

            Iterator<?> it    = ((Iterable<?>) result).iterator();
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
            Object onnxValue  = e.getValue();
            float[][][] tokenEmbeddings = (float[][][]) mOnnxGetValue.invoke(onnxValue);

            float[] pooled = clsPool(tokenEmbeddings[0]);

            return l2Normalize(pooled);

        } catch (Exception ex) {
            Reporter.log("[ENSEMBLE] embed failed: " + ex.getMessage(), LogLevel.WARN);
            return null;
        } finally {
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

    private static HealOutcome scoreAndSelectCandidate(WebDriver driver, float[] queryVector,
                                                       ElementFingerprint baseline,
                                                       By brokenLocator, String actionType,
                                                       String query,
                                                       String callerMethod, String fieldName,
                                                       String locatorValue) {
        SemanticLocatorResolver.ElementCategory cat =
                SemanticLocatorResolver.categorizeAction(actionType);
        String category      = cat != null ? cat.name() : null;
        boolean isReadable   = SemanticLocatorResolver.ElementCategory.READABLE == cat;
        double  threshold    = isReadable
                ? AIConfigLoader.getOnnxReadableThreshold()
                : AIConfigLoader.getOnnxSimilarityThreshold();
        int     maxCandidates = isReadable
                ? AIConfigLoader.getOnnxReadableMaxCandidates()
                : AIConfigLoader.getOnnxMaxCandidates();

        String semanticMethodName = isReadable ? null : callerMethod;
        List<String> semanticNames =
                SemanticNameExtractor.extract(semanticMethodName, fieldName, locatorValue);

        java.util.IdentityHashMap<WebElement, Double> resolverWeights = new java.util.IdentityHashMap<>();
        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        List<WebElement> candidates;
        List<Map<String, Object>> batch;
        try {
            List<WebElement> resolverEls = new ArrayList<>();
            for (Ellithium.core.ai.models.SemanticHit hit : SemanticLocatorResolver.findExactHits(
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

        WebElement bestElement = null;
        String     bestDoc     = null;
        double bestCombined = -1.0, bestCosine = 0.0, bestF1 = Double.NaN, bestF2 = Double.NaN;
        int scored = 0;

        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            for (int i = 0; i < candidates.size(); i++) {
                if (scored >= maxCandidates) break;
                WebElement candidate = candidates.get(i);
                try {
                    Map<String, Object> attrs = (batch != null && i < batch.size()) ? batch.get(i) : null;
                    if (attrs != null) {
                        if (Boolean.FALSE.equals(attrs.get("visible"))) continue;
                    } else if (!candidate.isDisplayed()) {
                        continue;
                    }
                    scored++;
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

                    double cosine = dotProduct(queryVector, docVector);                 // f3: bi-encoder
                    double f1     = (attrs != null) ? baselineProximity(baseline, attrs)
                                                    : baselineProximity(baseline, candidate);
                    double attrW  = (attrs != null)
                            ? SemanticLocatorResolver.strategyWeightForAttrs(attrs, semanticNames)
                            : Double.NaN;
                    Double hitW   = resolverWeights.get(candidate);
                    double f2     = maxScore(attrW, hitW == null ? Double.NaN : hitW);   // resolver retriever
                    double combined = Double.isNaN(f2) ? cosine : (f2 + cosine) / 2.0;

                    if (bestElement == null || combined > bestCombined
                            || (Math.abs(combined - bestCombined) < 1e-6 && f1 > bestF1)) {
                        bestElement = candidate; bestCombined = combined; bestCosine = cosine;
                        bestF1 = f1; bestF2 = f2; bestDoc = doc;
                    }

                    if (combined >= EARLY_EXIT_COSINE) break;
                } catch (Exception ignored) {}
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }

        if (bestElement == null) return null;
        if (bestDoc == null) bestDoc = buildElementDocument(bestElement);

        GateResult gate = decideGate(bestCombined, threshold, bestF1, bestF2,
                AIConfigLoader.isStrategyRescueEnabled(), AIConfigLoader.getGateFingerprintFloor());
        if (gate.accept) {
            Reporter.log(String.format("[ENSEMBLE] heal via %s combined=%.3f (f1=%.2f f2=%.2f f3=%.3f)",
                    gate.via, gate.score, bestF1, bestF2, bestCosine), LogLevel.INFO_GREEN);
            HealingTelemetryStore.record(2, brokenLocator.toString(), bestDoc, gate.score, true, query, category);
            return HealOutcome.of(bestElement, gate.score, 2);
        }
        Reporter.log(String.format("[ENSEMBLE] no heal — combined=%.3f < threshold=%.3f (f1=%.2f f2=%.2f f3=%.3f)",
                bestCombined, threshold, bestF1, bestF2, bestCosine), LogLevel.DEBUG);
        HealingTelemetryStore.record(2, brokenLocator.toString(), bestDoc, bestCombined, false, query, category);
        return null;
    }

    private static final String MUTATION_PROBE_SCRIPT =
            "if(!window.__ellHealMO){ window.__ellHealMutated=false;"
            + " try{ window.__ellHealMO=new MutationObserver(function(){window.__ellHealMutated=true;});"
            + "   window.__ellHealMO.observe(document.documentElement,{childList:true,subtree:true,attributes:true}); }catch(e){}"
            + " return false; }"
            + " var m=window.__ellHealMutated===true; window.__ellHealMutated=false; return m;";

    private static void invalidateCacheOnDomMutation(WebDriver driver) {
        if (!(driver instanceof org.openqa.selenium.JavascriptExecutor js)) return;
        try {
            Object changed = js.executeScript(MUTATION_PROBE_SCRIPT);
            if (Boolean.TRUE.equals(changed)) {
                ElementVectorCache.getInstance().invalidate();
            }
        } catch (Exception ignored) {}
    }

    private static double maxScore(double a, double b) {
        if (Double.isNaN(a)) return b;
        if (Double.isNaN(b)) return a;
        return Math.max(a, b);
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

    private static final double GATE_STRATEGY_MIN = 0.95;

    /**
     * Accept decision (pure, unit-testable). Path A: the calibrated anchor cosine clears the
     * threshold. Path B (strategy-rescue): a gold-tier Tier-2 match (f2 ≥ 0.95: exact data-testid /
     * AppiumBy / cross-validated mutation) corroborated by the baseline fingerprint (f1 ≥ floor) —
     * two independent high-precision signals, so the heal is trusted even when the bi-encoder cosine
     * is weak. Path-B confidence = mean(f1, f2). Cold start (f1 NaN) never rescues.
     */
    static GateResult decideGate(double combined, double threshold, double f1, double f2,
                                 boolean rescueEnabled, double fingerprintFloor) {
        if (combined >= threshold) return new GateResult(true, combined, "ensemble");
        if (rescueEnabled && f2 >= GATE_STRATEGY_MIN && f1 >= fingerprintFloor) {
            return new GateResult(true, (f1 + f2) / 2.0, "strategy-rescue");
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

    private static List<WebElement> collectCandidates(WebDriver driver,
                                                       ElementFingerprint baseline,
                                                       String actionType) {
        DriverProfile profile = DriverProfile.detect(driver);
        if (profile == DriverProfile.MOBILE_NATIVE) {
            return collectAppiumNativeCandidates(driver);
        }
        int hardLimit = AIConfigLoader.getOnnxHardCandidateLimit();
        java.util.LinkedHashSet<WebElement> seen = new java.util.LinkedHashSet<>();
        outer:
        for (String selector : CANDIDATE_SELECTORS) {
            try {
                for (WebElement el : driver.findElements(By.cssSelector(selector))) {
                    seen.add(el);
                    if (seen.size() >= hardLimit) break outer;
                }
            } catch (Exception ignored) {}
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
        return nz(tag) + "|" + nz(id) + "|" + nz(name) + "|" + nz(testid)
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

        ortSession = ortEnvironment.getClass()
                .getMethod("createSession", byte[].class, optClass)
                .invoke(ortEnvironment, modelBytes, opts);

        Class<?> tensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
        mCreateTensor = tensorClass.getMethod("createTensor", envClass, LongBuffer.class, long[].class);
        mSessionRun   = ortSession.getClass().getMethod("run", Map.class);
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
        return assembleDocument(name -> strOf(attrs.get(name)), tag, text);
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
        return assembleDocument(name -> switch (name) {
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
            case "type"             -> fp.getType();
            default                 -> null;
        }, fp.getTagName(), fp.getText());
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


    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toByteArray();
    }
}
