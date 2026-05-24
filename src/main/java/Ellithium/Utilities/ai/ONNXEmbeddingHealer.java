package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.models.ElementFingerprint;
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

/**
 * Tier 3 — Local ONNX Embedding Healer.
 *
 * Loads the fine-tuned BAAI/bge-small-en-v1.5 model from classpath resources,
 * embeds semantic queries and DOM element documents, and scores candidates via
 * cosine similarity to find the best matching element.
 *
 * Model: /Ellithium-ai-model/model_quantized.onnx  (INT8, ~8 MB)
 * Tokenizer: /Ellithium-ai-model/tokenizer.json
 *
 * All ORT and DJL calls use reflection so the class loads cleanly when the
 * optional JARs are absent — isAvailable() returns false and the cascade
 * falls through to Tier 4 silently.
 */
public class ONNXEmbeddingHealer {

    private static final String MODEL_RESOURCE     = "/Ellithium-ai-model/model_quantized.onnx";
    private static final String TOKENIZER_RESOURCE = "/Ellithium-ai-model/tokenizer.json";
    private static final String BGE_QUERY_PREFIX   =
            "Represent this sentence for searching relevant passages: ";
    // Token budget fed to the model. Raised 128 → 256 so richer element documents (parent/sibling
    // context + longer text) survive tokenization instead of being clipped. MUST match the
    // MAX_SEQ_LEN used in 02_finetune.py and 03_export_onnx.py — retrain/re-export after changing.
    private static final int MAX_SEQ_LEN = 256;

    // Character caps for element-document fields. Generous so long product titles / status messages
    // contribute; the tokenizer's MAX_SEQ_LEN is the real ceiling. Truncate, never drop.
    private static final int MAX_TEXT_CHARS  = 240;
    private static final int MAX_CLASS_CHARS = 200;

    // Two candidates whose cosine scores differ by less than this are treated as tied — the
    // baseline fingerprint then breaks the tie (prevents arbitrary picks on repeated/identical
    // elements like table rows or product-card grids).
    private static final double SCORE_TIE_EPSILON = 0.02;

    private static volatile boolean initialized = false;
    private static volatile boolean available   = false;

    /**
     * Per-thread similarity score of the most recent Tier 3 heal. Set immediately before
     * {@link #tryEmbeddingHeal} returns a non-null element, read by the caller to attach heal
     * provenance (confidence) to the gated capture / source patch.
     */
    private static final ThreadLocal<Double> LAST_HEAL_SCORE = ThreadLocal.withInitial(() -> 0.0);

    /** Similarity score (0.0–1.0) of the most recent Tier 3 heal on this thread. */
    public static double getLastHealScore() { return LAST_HEAL_SCORE.get(); }

    // Held as Object to avoid NoClassDefFoundError when optional JARs are absent
    private static Object ortEnvironment = null;
    private static Object ortSession     = null;
    private static Object tokenizer      = null;  // ai.djl.huggingface.tokenizers.HuggingFaceTokenizer

    // Reflection handles resolved ONCE at init (were re-resolved on every embed() — i.e. per
    // candidate). getMethod()/Class.forName() do linear method scans + security checks; caching them
    // removes ~9 reflective lookups per embedding with zero behaviour change.
    private static Method mEncode, mGetIds, mGetMask, mGetTypes, mCreateTensor, mSessionRun, mOnnxGetValue;

    // ──────────────────────── Lifecycle ────────────────────────

    /**
     * Loads the ONNX model and tokenizer from JAR resources.
     * Called by GeneralHandler at suite startup. Safe to call multiple times.
     */
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        InputStream modelIs = ONNXEmbeddingHealer.class.getResourceAsStream(MODEL_RESOURCE);
        if (modelIs == null) {
            Reporter.log("[TIER 3] Model not found at " + MODEL_RESOURCE + " — Tier 3 unavailable",
                    LogLevel.WARN);
            return;
        }

        try {
            byte[] modelBytes     = readAllBytes(modelIs);
            byte[] tokenizerBytes = loadResource(TOKENIZER_RESOURCE);
            if (tokenizerBytes == null) {
                Reporter.log("[TIER 3] Tokenizer not found at " + TOKENIZER_RESOURCE + " — Tier 3 unavailable", LogLevel.WARN);
                return;
            }

            initOrtSession(modelBytes);
            initTokenizer(tokenizerBytes);
            available = true;
            Reporter.log("[TIER 3] ONNX session active — " + modelBytes.length / 1024
                    + " KB INT8 model loaded | pooling=cls", LogLevel.INFO_GREEN);

        } catch (ClassNotFoundException e) {
            Reporter.log("[TIER 3] Required JARs not on classpath (onnxruntime / djl-tokenizers): "
                    + e.getMessage() + " — Tier 3 unavailable", LogLevel.WARN);
        } catch (Exception e) {
            Reporter.log("[TIER 3] Init failed: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage() + " — Tier 3 unavailable", LogLevel.WARN);
        } catch (Throwable t) {
            // Catches Error subtypes: ExceptionInInitializerError (native lib load failure in DJL),
            // UnsatisfiedLinkError (missing native DLL), NoClassDefFoundError (cascade from static init)
            Reporter.log("[TIER 3] Init failed (native/JVM error): " + t.getClass().getSimpleName()
                    + ": " + t.getMessage() + " — Tier 3 unavailable", LogLevel.WARN);
        }
    }

    /**
     * Closes the ORT session and releases native memory.
     * Called by GeneralHandler / CustomTestNGListener at suite teardown.
     */
    public static void shutdown() {
        available = false;
        closeQuietly(ortSession);
        closeQuietly(ortEnvironment);
        ortSession     = null;
        ortEnvironment = null;
        tokenizer      = null;
        initialized    = false;
        Reporter.log("[TIER 3] ONNX session closed", LogLevel.DEBUG);
    }

    /** True when the ORT session is loaded and ready for inference. */
    public static boolean isAvailable() { return available; }

    // ──────────────────────── Heal Entry Point ────────────────────────

    /**
     * Attempts to heal a broken locator using local ONNX embedding similarity search.
     * Returns null immediately when unavailable (silent no-op in the cascade).
     */
    public static WebElement tryEmbeddingHeal(WebDriver driver, By locator,
                                               String actionType, String callerMethod,
                                               String fieldName, String locatorValue,
                                               ElementFingerprint baseline) {
        if (!available) return null;
        LAST_HEAL_SCORE.set(0.0);   // reset per attempt — never leak a prior heal's score to the gate

        String query = SemanticQueryBuilder.buildFromContext(actionType, locatorValue, callerMethod, baseline);
        if (query.isBlank()) return null;

        Reporter.log("[TIER 3] Embedding search for: " + locator + " | query=\"" + query + "\"",
                LogLevel.INFO_BLUE);

        float[] queryVector = embed(query, true);
        if (queryVector == null) {
            Reporter.log("[TIER 3] Embedding failed (tokenizer error) — falling through to Tier 4", LogLevel.WARN);
            return null;
        }

        return scoreAndSelectCandidate(driver, queryVector, baseline, locator, actionType);
    }

    // ──────────────────────── Embedding ────────────────────────

    /**
     * Embeds text into a 384-dim L2-normalised float vector.
     *
     * @param text    The text to embed
     * @param isQuery True for queries (BGE prefix applied); false for element documents
     * @return L2-normalised float[384], or null on any failure
     */
    static float[] embed(String text, boolean isQuery) {
        if (!available || ortSession == null || tokenizer == null) return null;
        String input = isQuery ? BGE_QUERY_PREFIX + text : text;
        // Declared outside the try so they can be closed in finally — OnnxTensor and
        // OrtSession.Result hold OFF-HEAP native memory; without explicit close, every embed()
        // (one per candidate per heal) leaks until GC, which can balloon native memory in a long suite.
        Object tIds = null, tMask = null, tType = null, result = null;
        try {
            // Step 1: Tokenize via DJL HuggingFaceTokenizer (cached reflection handles)
            Object encoding = mEncode.invoke(tokenizer, input);

            long[] ids     = (long[]) mGetIds.invoke(encoding);
            long[] mask    = (long[]) mGetMask.invoke(encoding);
            long[] typeIds = (long[]) mGetTypes.invoke(encoding);

            // Step 2: Truncate + pad to MAX_SEQ_LEN
            int seqLen        = Math.min(ids.length, MAX_SEQ_LEN);
            long[] paddedIds  = new long[MAX_SEQ_LEN];
            long[] paddedMask = new long[MAX_SEQ_LEN];
            long[] paddedType = new long[MAX_SEQ_LEN];
            System.arraycopy(ids,     0, paddedIds,  0, seqLen);
            System.arraycopy(mask,    0, paddedMask, 0, seqLen);
            System.arraycopy(typeIds, 0, paddedType, 0, seqLen);

            // Step 3: Build ORT tensors (cached createTensor handle)
            long[] shape = {1L, (long) MAX_SEQ_LEN};
            tIds  = mCreateTensor.invoke(null, ortEnvironment, LongBuffer.wrap(paddedIds),  shape);
            tMask = mCreateTensor.invoke(null, ortEnvironment, LongBuffer.wrap(paddedMask), shape);
            tType = mCreateTensor.invoke(null, ortEnvironment, LongBuffer.wrap(paddedType), shape);

            // Step 4: Run ORT session (cached run handle)
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("input_ids",      tIds);
            inputs.put("attention_mask", tMask);
            inputs.put("token_type_ids", tType);

            result = mSessionRun.invoke(ortSession, inputs);

            // Step 5: Extract last_hidden_state (first output)
            // OrtSession.Result implements Iterable<Map.Entry<String, OnnxValue>>.
            // getValue() materializes a Java float[][][] copy, so the Result can be closed afterwards.
            Iterator<?> it    = ((Iterable<?>) result).iterator();
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
            Object onnxValue  = e.getValue();
            float[][][] tokenEmbeddings = (float[][][]) mOnnxGetValue.invoke(onnxValue);

            // Step 6: CLS pooling — BGE uses the [CLS] token (index 0) as the sentence embedding
            // (1_Pooling/config.json → pooling_mode_cls_token=true). Always CLS for this model family.
            float[] pooled = clsPool(tokenEmbeddings[0]);

            // Step 7: L2 normalise
            return l2Normalize(pooled);

        } catch (Exception ex) {
            Reporter.log("[TIER 3] embed failed: " + ex.getMessage(), LogLevel.WARN);
            return null;
        } finally {
            // Close native resources (Result first — it may reference output tensors).
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

    // ──────────────────────── Candidate Scoring ────────────────────────

    // ONE inclusive, priority-ordered candidate list used for ALL actions. Ordered so the
    // highest-value elements are scored first (the per-attempt cap then reaches them), but NOTHING
    // is excluded up front — the model decides, not a hardcoded tag filter. Earlier versions
    // narrowed to the baseline's tag (excluding the right element if it changed tag) and dropped
    // clickable div/span/custom components; both caused real misses. Dedup preserves this order.
    private static final String[] CANDIDATE_SELECTORS = {
        // 1. Interactive controls + ARIA widgets (buttons, links, inputs, custom clickables)
        "button, a, input, select, textarea, summary, "
            + "[role='button'], [role='link'], [role='tab'], [role='menuitem'], "
            + "[role='checkbox'], [role='radio'], [role='switch'], [role='option'], [onclick]",
        // 2. Status / headings / labels (status-bearing + semantic text)
        "[role='alert'], [role='status'], [role='heading'], [role='log'], "
            + "h1, h2, h3, h4, h5, h6, label, legend",
        // 3. Test- / accessibility- / identity-tagged (stable, high-signal identity)
        "[data-testid], [data-test], [data-cy], [aria-label], [placeholder], [title], [name], [id]",
        // 4. Descriptive / tabular / generic text (broad fallback — nothing excluded)
        "p, li, td, th, dt, dd, article, section, aside, span, div",
    };

    private static WebElement scoreAndSelectCandidate(WebDriver driver, float[] queryVector,
                                                       ElementFingerprint baseline,
                                                       By brokenLocator, String actionType) {
        boolean isReadable   = SemanticLocatorResolver.ElementCategory.READABLE
                == SemanticLocatorResolver.categorizeAction(actionType);
        double  threshold    = isReadable
                ? AIConfigLoader.getOnnxReadableThreshold()
                : AIConfigLoader.getOnnxSimilarityThreshold();
        int     maxCandidates = isReadable
                ? AIConfigLoader.getOnnxReadableMaxCandidates()
                : AIConfigLoader.getOnnxMaxCandidates();

        List<WebElement> candidates = collectCandidates(driver, baseline, actionType);

        double     bestScore          = -1.0;
        WebElement bestElement        = null;
        double     bestTieScore       = -1.0;               // baseline proximity of the current best
        List<WebElement> borderline   = new ArrayList<>();  // 0.45 ≤ score < threshold
        int        scored             = 0;

        // ONE batched executeScript fetches the unambiguous attributes for every candidate, replacing
        // ~12 WebDriver round-trips PER candidate. type/getText/isDisplayed stay native (semantics).
        // Null → graceful fallback to per-element reads (e.g. Appium native context with no JS).
        List<Map<String, Object>> batch = fetchCandidateAttributes(driver, candidates);
        String bestDoc = null;

        for (int i = 0; i < candidates.size(); i++) {
            if (scored >= maxCandidates) break;
            WebElement candidate = candidates.get(i);
            try {
                if (!candidate.isDisplayed()) continue;
                scored++;

                Map<String, Object> attrs = (batch != null && i < batch.size()) ? batch.get(i) : null;
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

                double score = dotProduct(queryVector, docVector);
                if (score > bestScore + SCORE_TIE_EPSILON) {
                    // Clear winner on cosine.
                    bestScore     = score;
                    bestElement   = candidate;
                    bestTieScore  = baselineProximity(baseline, candidate);
                    bestDoc       = doc;
                } else if (score >= bestScore - SCORE_TIE_EPSILON && bestElement != null) {
                    // Near-tie (e.g. a grid of identical "Add to cart" buttons / repeated rows):
                    // cosine can't choose, so prefer the candidate closest to the stored baseline.
                    // With no baseline, keep the earlier DOM-order element (do nothing).
                    double tie = baselineProximity(baseline, candidate);
                    if (tie > bestTieScore) {
                        bestElement  = candidate;
                        bestTieScore = tie;
                        bestScore    = Math.max(bestScore, score);
                        bestDoc      = doc;
                    }
                }
                if (score >= 0.45 && score < threshold) borderline.add(candidate);
            } catch (Exception ignored) {}
        }

        if (bestElement != null && bestScore >= threshold) {
            Reporter.log(String.format("[TIER 3] Embedding match: score=%.4f (threshold=%.2f)",
                    bestScore, threshold), LogLevel.INFO_GREEN);
            // Reuse the doc computed during scoring (avoid a redundant attribute re-read for telemetry).
            String doc = bestDoc != null ? bestDoc : buildElementDocument(bestElement);
            HealingTelemetryStore.record(3, brokenLocator.toString(), doc, bestScore, true);
            LAST_HEAL_SCORE.set(bestScore);
            return bestElement;
        }

        // Context-enrichment pass: if first attempt scored borderline, re-embed top candidates
        // with parent tag/class/id context (mirrors the accessibility-tree context the LLM has).
        if (!borderline.isEmpty() && driver instanceof org.openqa.selenium.JavascriptExecutor) {
            Reporter.log("[TIER 3] First pass borderline — running context-enrichment pass on "
                    + Math.min(borderline.size(), 5) + " candidates", LogLevel.INFO_BLUE);
            double[] enrichedScoreOut = new double[1];
            WebElement enrichedMatch = contextEnrichmentPass(driver, queryVector, borderline, threshold, enrichedScoreOut);
            if (enrichedMatch != null) {
                double enrichedScore = enrichedScoreOut[0];
                Reporter.log(String.format("[TIER 3] Embedding match via context-enrichment pass: score=%.4f",
                        enrichedScore), LogLevel.INFO_GREEN);
                HealingTelemetryStore.record(3, brokenLocator.toString(),
                        buildElementDocument(enrichedMatch), enrichedScore, true);
                // Record the REAL enrichment score for telemetry accuracy. The correctness gate
                // (storeThreshold check in BaselineStore/AISelfHealer) prevents persistence of
                // borderline heals — we don't need to fabricate a low score to achieve that.
                LAST_HEAL_SCORE.set(enrichedScore);
                return enrichedMatch;
            }
        }

        if (bestElement != null) {
            Reporter.log(String.format("[TIER 3] Best score %.4f below threshold %.2f — falling through to Tier 4",
                    bestScore, threshold), LogLevel.INFO_YELLOW);
            HealingTelemetryStore.record(3, brokenLocator.toString(),
                    buildElementDocument(bestElement), bestScore, false);
        } else {
            Reporter.log("[TIER 3] No candidates scored — falling through to Tier 4", LogLevel.INFO_YELLOW);
        }
        return null;
    }

    /**
     * Second-pass enrichment: for each borderline candidate, builds an enriched document that
     * includes parent tag/class/id and nearby label text via a single JS call — giving the
     * model the same structural context that the Tier 4 LLM gets from the accessibility tree.
     */
    private static WebElement contextEnrichmentPass(WebDriver driver, float[] queryVector,
                                                      List<WebElement> borderline, double threshold,
                                                      double[] bestScoreOut) {
        double     bestScore   = -1.0;
        WebElement bestElement = null;
        int limit = Math.min(borderline.size(), 5);
        for (int i = 0; i < limit; i++) {
            WebElement candidate = borderline.get(i);
            try {
                String enrichedDoc = buildContextEnrichedDocument(driver, candidate);
                if (enrichedDoc.isBlank()) continue;
                float[] enrichedVector = embed(enrichedDoc, false);
                if (enrichedVector == null) continue;
                double score = dotProduct(queryVector, enrichedVector);
                if (score > bestScore) {
                    bestScore   = score;
                    bestElement = candidate;
                }
            } catch (Exception ignored) {}
        }
        if (bestScoreOut != null && bestScoreOut.length > 0) bestScoreOut[0] = bestScore;
        return (bestElement != null && bestScore >= threshold) ? bestElement : null;
    }

    /**
     * Appends parent tag + class tokens + nearby label text to the base element document.
     * <p>CONTEXT-FORMAT PARITY: the token shape produced here (parentTag [parentId raw]
     * [parentClass with -/_ → space] [parentLabel ≤40 chars]) is mirrored byte-for-byte by
     * {@code _format_parent_ctx()} in {@code kaggle-finetune/01_data_generation.py}. Changing this
     * JS requires the identical change there, or the model sees enriched docs at train vs serve time
     * in different shapes (train/serve skew).
     */
    private static String buildContextEnrichedDocument(WebDriver driver, WebElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildElementDocument(element));
        try {
            Object parentCtx = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var e=arguments[0], p=e.parentElement, ctx='';" +
                    "if(p){" +
                    "  ctx+=p.tagName.toLowerCase();" +
                    "  if(p.id) ctx+=' '+p.id;" +
                    "  if(p.className) ctx+=' '+p.className.replace(/[-_]/g,' ');" +
                    "  var lbl=p.querySelector('label');" +
                    "  if(lbl&&lbl.textContent) ctx+=' '+lbl.textContent.trim().substring(0,40);" +
                    "}" +
                    "return ctx.trim();",
                    element);
            if (parentCtx instanceof String s && !s.isBlank()) sb.append(" ").append(s.trim());
        } catch (Exception ignored) {}
        return sb.toString().trim();
    }

    /**
     * Collects candidate elements for ALL actions from one inclusive, priority-ordered list.
     * Nothing is excluded by tag/category — the baseline is used only for SCORING (tiebreak), never
     * to filter out elements, so an element that changed tag/structure is still reachable. Dedup by
     * element preserves priority order; the per-attempt cap (in the scoring loop) bounds the cost.
     */
    private static List<WebElement> collectCandidates(WebDriver driver,
                                                       ElementFingerprint baseline,
                                                       String actionType) {
        java.util.LinkedHashMap<String, WebElement> seen = new java.util.LinkedHashMap<>();
        for (String selector : CANDIDATE_SELECTORS) {
            try {
                for (WebElement el : driver.findElements(By.cssSelector(selector))) {
                    String key = buildCacheKey(el);
                    seen.putIfAbsent(key.isEmpty() ? el.toString() : key, el);
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(seen.values());
    }

    /**
     * Cache key for an element's embedding vector. Returns "" (do-not-cache) unless the element has a
     * STABLE identity (id / name / data-testid) — caching on tag alone collided every attribute-less
     * &lt;div&gt; to one key ("div||") and served the wrong vector. Callers skip caching on "".
     */
    private static String buildCacheKey(WebElement el) {
        try {
            return cacheKey(el.getTagName(), el.getAttribute("id"),
                    el.getAttribute("name"), el.getAttribute("data-testid"));
        } catch (Exception e) { return ""; }
    }

    /** Cache key from pre-fetched (batched) attributes — identical format to the WebElement version. */
    private static String buildCacheKey(Map<String, Object> attrs) {
        return cacheKey(strOf(attrs.get("tag")), strOf(attrs.get("id")),
                strOf(attrs.get("name")), strOf(attrs.get("data-testid")));
    }

    private static String cacheKey(String tag, String id, String name, String testid) {
        boolean hasIdentity = (id != null && !id.isBlank())
                || (name != null && !name.isBlank())
                || (testid != null && !testid.isBlank());
        if (!hasIdentity) return "";   // no stable key → never cache (avoid collisions)
        return nz(tag) + "|" + nz(id) + "|" + nz(name) + "|" + nz(testid);
    }

    /**
     * Fetches the unambiguous string attributes for ALL candidates in ONE executeScript round-trip.
     * Returns a list aligned by index with {@code candidates} (entries may be null); returns null when
     * the driver has no JS (Appium native) or the call fails → caller falls back to per-element reads.
     * Excludes {@code type} (Selenium default-value semantics) and text/visibility (read natively).
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fetchCandidateAttributes(WebDriver driver,
                                                                      List<WebElement> candidates) {
        if (!(driver instanceof org.openqa.selenium.JavascriptExecutor) || candidates.isEmpty()) return null;
        try {
            Object res = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "return arguments[0].map(function(el){"
                            + " if(!el) return null;"
                            + " function a(n){ return el.getAttribute(n); }"
                            + " return {'id':a('id'),'name':a('name'),'class':a('class'),"
                            + "  'aria-label':a('aria-label'),'data-testid':a('data-testid'),'role':a('role'),"
                            + "  'placeholder':a('placeholder'),'resource-id':a('resource-id'),"
                            + "  'accessibility-id':a('accessibility-id'),"
                            + "  'content-desc':a('content-desc'),'data-test':a('data-test'),"
                            + "  'title':a('title'),'label':a('label'),"
                            + "  'tag':el.tagName?el.tagName.toLowerCase():null};"
                            + "});", candidates);
            if (res instanceof List<?> rows) {
                List<Map<String, Object>> out = new ArrayList<>(rows.size());
                for (Object row : rows) out.add(row instanceof Map<?, ?> ? (Map<String, Object>) row : null);
                return out;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String nz(String s) { return s != null ? s : ""; }

    // ──────────────────────── Math ────────────────────────

    /**
     * Tiebreak signal for near-equal cosine scores: how well the candidate matches the stored
     * baseline fingerprint (0.0–1.0). Returns -1 when there is no baseline, so DOM order is kept.
     */
    private static double baselineProximity(ElementFingerprint baseline, WebElement candidate) {
        if (baseline == null) return -1.0;
        try {
            return baseline.scoreSimilarity(candidate);
        } catch (Exception e) {
            return -1.0;
        }
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

    private static double dotProduct(float[] a, float[] b) {
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

    // ──────────────────────── ORT Init (Reflection-Guarded) ────────────────────────

    private static void initOrtSession(byte[] modelBytes) throws Exception {
        Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
        ortEnvironment = envClass.getMethod("getEnvironment").invoke(null);

        Class<?> optClass = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
        Object   opts     = optClass.getDeclaredConstructor().newInstance();

        ortSession = ortEnvironment.getClass()
                .getMethod("createSession", byte[].class, optClass)
                .invoke(ortEnvironment, modelBytes, opts);

        // Resolve ORT reflection handles once (reused by every embed()).
        Class<?> tensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
        mCreateTensor = tensorClass.getMethod("createTensor", envClass, LongBuffer.class, long[].class);
        mSessionRun   = ortSession.getClass().getMethod("run", Map.class);
        mOnnxGetValue = Class.forName("ai.onnxruntime.OnnxValue").getMethod("getValue");
    }

    private static void initTokenizer(byte[] tokenizerBytes) throws Exception {
        // DJL HuggingFaceTokenizer.newInstance(Path) — write to temp file briefly
        Path tmp = Files.createTempFile("ell-tok-", ".json");
        try {
            Files.write(tmp, tokenizerBytes);
            Class<?> cls = Class.forName("ai.djl.huggingface.tokenizers.HuggingFaceTokenizer");
            tokenizer = cls.getMethod("newInstance", Path.class).invoke(null, tmp);

            // Resolve tokenizer/encoding reflection handles once (reused by every embed()).
            mEncode = cls.getMethod("encode", String.class);
            Class<?> encClass = Class.forName("ai.djl.huggingface.tokenizers.Encoding");
            mGetIds   = encClass.getMethod("getIds");
            mGetMask  = encClass.getMethod("getAttentionMask");
            mGetTypes = encClass.getMethod("getTypeIds");
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }

    // ──────────────────────── Document Builder ────────────────────────

    // Canonical field order — MUST stay byte-for-byte in sync with 01_data_generation.py build_doc().
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
        return assembleDocument(name -> safeAttr(element, name), safeTag(element), safeText(element));
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
        return assembleDocument(
                name -> "type".equals(name) ? safeAttr(element, "type") : strOf(attrs.get(name)),
                tag,
                safeText(element));   // native — getText() semantics
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
            case "class"            -> fp.getClassName();
            case "aria-label"       -> fp.getAriaLabel();
            case "role"             -> fp.getRole();
            case "placeholder"      -> fp.getPlaceholder();
            case "data-testid"      -> fp.getDataTestId();
            case "type"             -> fp.getType();
            // resource-id, accessibility-id, content-desc, data-test, title, label:
            // not captured in ElementFingerprint yet (Appium runtime only / uncommon web attrs).
            // When getters are added, add cases here — assembleDocument handles the field ordering.
            default                 -> null;
        }, fp.getTagName(), fp.getText());
    }

    private static void appendVal(StringBuilder sb, String v) {
        if (v != null && !v.isBlank()) sb.append(" ").append(v.trim());
    }


    // ──────────────────────── IO Helpers ────────────────────────

    private static byte[] loadResource(String path) {
        try (InputStream is = ONNXEmbeddingHealer.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return readAllBytes(is);
        } catch (Exception e) { return null; }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toByteArray();
    }
}
