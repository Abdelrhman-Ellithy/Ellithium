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
    private static final int MAX_SEQ_LEN = 64;

    private static volatile boolean initialized = false;
    private static volatile boolean available   = false;

    // Held as Object to avoid NoClassDefFoundError when optional JARs are absent
    private static Object ortEnvironment = null;
    private static Object ortSession     = null;
    private static Object tokenizer      = null;  // ai.djl.huggingface.tokenizers.HuggingFaceTokenizer

    // ──────────────────────── Lifecycle ────────────────────────

    /**
     * Loads the ONNX model and tokenizer from JAR resources.
     * Called by GeneralHandler at suite startup. Safe to call multiple times.
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;

        InputStream modelIs = ONNXEmbeddingHealer.class.getResourceAsStream(MODEL_RESOURCE);
        if (modelIs == null) {
            Reporter.log("[TIER 3] Model not found at " + MODEL_RESOURCE + " — Tier 3 unavailable",
                    LogLevel.DEBUG);
            return;
        }

        try {
            byte[] modelBytes     = readAllBytes(modelIs);
            byte[] tokenizerBytes = loadResource(TOKENIZER_RESOURCE);
            if (tokenizerBytes == null) {
                Reporter.log("[TIER 3] Tokenizer not found — Tier 3 unavailable", LogLevel.WARN);
                return;
            }

            initOrtSession(modelBytes);
            initTokenizer(tokenizerBytes);
            available = true;
            Reporter.log("[TIER 3] ONNX session active — " + modelBytes.length / 1024
                    + " KB INT8 model loaded", LogLevel.INFO_GREEN);

        } catch (ClassNotFoundException e) {
            Reporter.log("[TIER 3] Required JARs (onnxruntime / djl-tokenizers) not on classpath"
                    + " — Tier 3 unavailable", LogLevel.DEBUG);
        } catch (Exception e) {
            Reporter.log("[TIER 3] Init failed: " + e.getMessage() + " — Tier 3 unavailable",
                    LogLevel.WARN);
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

        String query = SemanticQueryBuilder.buildFromContext(actionType, locatorValue, callerMethod, baseline);
        if (query.isBlank()) return null;

        float[] queryVector = embed(query, true);
        if (queryVector == null) return null;

        return scoreAndSelectCandidate(driver, queryVector, baseline, locator);
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
        try {
            // Step 1: Tokenize via DJL HuggingFaceTokenizer (reflection)
            Object encoding = tokenizer.getClass()
                    .getMethod("encode", String.class)
                    .invoke(tokenizer, input);

            long[] ids     = (long[]) encoding.getClass().getMethod("getIds").invoke(encoding);
            long[] mask    = (long[]) encoding.getClass().getMethod("getAttentionMask").invoke(encoding);
            long[] typeIds = (long[]) encoding.getClass().getMethod("getTypeIds").invoke(encoding);

            // Step 2: Truncate + pad to MAX_SEQ_LEN
            int seqLen        = Math.min(ids.length, MAX_SEQ_LEN);
            long[] paddedIds  = new long[MAX_SEQ_LEN];
            long[] paddedMask = new long[MAX_SEQ_LEN];
            long[] paddedType = new long[MAX_SEQ_LEN];
            System.arraycopy(ids,     0, paddedIds,  0, seqLen);
            System.arraycopy(mask,    0, paddedMask, 0, seqLen);
            System.arraycopy(typeIds, 0, paddedType, 0, seqLen);

            // Step 3: Build ORT tensors via reflection
            long[] shape = {1L, (long) MAX_SEQ_LEN};
            Class<?> envClass    = Class.forName("ai.onnxruntime.OrtEnvironment");
            Class<?> tensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
            Method   create      = tensorClass.getMethod("createTensor", envClass,
                    LongBuffer.class, long[].class);

            Object tIds  = create.invoke(null, ortEnvironment, LongBuffer.wrap(paddedIds),  shape);
            Object tMask = create.invoke(null, ortEnvironment, LongBuffer.wrap(paddedMask), shape);
            Object tType = create.invoke(null, ortEnvironment, LongBuffer.wrap(paddedType), shape);

            // Step 4: Run ORT session
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("input_ids",      tIds);
            inputs.put("attention_mask", tMask);
            inputs.put("token_type_ids", tType);

            Object result = ortSession.getClass()
                    .getMethod("run", Map.class)
                    .invoke(ortSession, inputs);

            // Step 5: Extract last_hidden_state (first output)
            // OrtSession.Result implements Iterable<Map.Entry<String, OnnxValue>>
            Iterator<?> it    = ((Iterable<?>) result).iterator();
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
            Object onnxValue  = e.getValue();
            float[][][] tokenEmbeddings =
                    (float[][][]) onnxValue.getClass().getMethod("getValue").invoke(onnxValue);

            // Step 6: Mean pool over valid (non-padding) tokens
            float[] pooled = meanPool(tokenEmbeddings[0], paddedMask);

            // Step 7: L2 normalise
            return l2Normalize(pooled);

        } catch (Exception ex) {
            Reporter.log("[TIER 3] embed failed: " + ex.getMessage(), LogLevel.WARN);
            return null;
        }
    }

    // ──────────────────────── Candidate Scoring ────────────────────────

    private static WebElement scoreAndSelectCandidate(WebDriver driver, float[] queryVector,
                                                       ElementFingerprint baseline,
                                                       By brokenLocator) {
        double threshold    = AIConfigLoader.getOnnxSimilarityThreshold();
        int    maxCandidates = AIConfigLoader.getOnnxMaxCandidates();

        List<WebElement> candidates = collectCandidates(driver, baseline);

        double     bestScore   = -1.0;
        WebElement bestElement = null;
        int        count       = 0;

        for (WebElement candidate : candidates) {
            if (count++ >= maxCandidates) break;
            try {
                if (!candidate.isDisplayed()) continue;

                // Check ElementVectorCache before re-embedding
                String  cacheKey  = buildCacheKey(candidate);
                float[] docVector = ElementVectorCache.getInstance().get(cacheKey);

                if (docVector == null) {
                    String doc = buildElementDocument(candidate);
                    if (doc.isBlank()) continue;
                    docVector = embed(doc, false);
                    if (docVector != null && !cacheKey.isEmpty()) {
                        ElementVectorCache.getInstance().put(cacheKey, docVector);
                    }
                }

                if (docVector == null) continue;

                // Vectors are L2-normalised → dot product == cosine similarity
                double score = dotProduct(queryVector, docVector);
                if (score > bestScore) {
                    bestScore   = score;
                    bestElement = candidate;
                }
            } catch (Exception ignored) {}
        }

        if (bestElement != null && bestScore >= threshold) {
            Reporter.log(String.format("[TIER 3] Embedding match: score=%.4f (threshold=%.2f)",
                    bestScore, threshold), LogLevel.INFO_GREEN);
            HealingTelemetryStore.record(3, brokenLocator.toString(),
                    buildElementDocument(bestElement), bestScore, true);
            return bestElement;
        }

        if (bestElement != null) {
            Reporter.log(String.format("[TIER 3] Best score %.4f below threshold %.2f — no match",
                    bestScore, threshold), LogLevel.DEBUG);
            HealingTelemetryStore.record(3, brokenLocator.toString(),
                    buildElementDocument(bestElement), bestScore, false);
        }
        return null;
    }

    private static List<WebElement> collectCandidates(WebDriver driver, ElementFingerprint baseline) {
        try {
            if (baseline != null && baseline.getTagName() != null
                    && !baseline.getTagName().isBlank()) {
                return driver.findElements(By.tagName(baseline.getTagName()));
            }
            return driver.findElements(By.cssSelector(
                    "input, button, select, textarea, a, [role='button'], [role='link']"));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static String buildCacheKey(WebElement el) {
        try {
            String tag  = el.getTagName();
            String id   = el.getAttribute("id");
            String name = el.getAttribute("name");
            return tag + "|" + (id   != null ? id   : "")
                       + "|" + (name != null ? name : "");
        } catch (Exception e) { return ""; }
    }

    // ──────────────────────── Math ────────────────────────

    private static float[] meanPool(float[][] tokenEmbeddings, long[] attentionMask) {
        int hiddenSize = tokenEmbeddings[0].length;
        float[] pooled = new float[hiddenSize];
        int valid = 0;
        for (int i = 0; i < tokenEmbeddings.length; i++) {
            if (i < attentionMask.length && attentionMask[i] == 1L) {
                for (int j = 0; j < hiddenSize; j++) pooled[j] += tokenEmbeddings[i][j];
                valid++;
            }
        }
        if (valid > 0) for (int j = 0; j < hiddenSize; j++) pooled[j] /= valid;
        return pooled;
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
    }

    private static void initTokenizer(byte[] tokenizerBytes) throws Exception {
        // DJL HuggingFaceTokenizer.newInstance(Path) — write to temp file briefly
        Path tmp = Files.createTempFile("ell-tok-", ".json");
        try {
            Files.write(tmp, tokenizerBytes);
            Class<?> cls = Class.forName("ai.djl.huggingface.tokenizers.HuggingFaceTokenizer");
            tokenizer = cls.getMethod("newInstance", Path.class).invoke(null, tmp);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }

    // ──────────────────────── Document Builder ────────────────────────

    /**
     * Builds a concise document string from a WebElement's visible attributes
     * for embedding as a DOM candidate. Mirrors buildFingerprintDocument() output.
     */
    public static String buildElementDocument(WebElement element) {
        if (element == null) return "";
        StringBuilder sb = new StringBuilder();
        appendAttr(sb, element, "id");
        appendAttr(sb, element, "name");
        appendAttr(sb, element, "aria-label");
        appendAttr(sb, element, "placeholder");
        appendAttr(sb, element, "data-testid");
        appendAttr(sb, element, "data-test");
        appendAttr(sb, element, "title");
        appendAttr(sb, element, "type");
        try {
            String tag  = element.getTagName();
            String text = element.getText();
            if (tag  != null && !tag.isBlank())  sb.append(" ").append(tag.trim());
            if (text != null && !text.isBlank() && text.length() <= 80)
                sb.append(" ").append(text.trim());
        } catch (Exception ignored) {}
        return sb.toString().trim();
    }

    private static void appendAttr(StringBuilder sb, WebElement el, String attr) {
        try {
            String v = el.getAttribute(attr);
            if (v != null && !v.isBlank()) sb.append(" ").append(v.trim());
        } catch (Exception ignored) {}
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

    private static void closeQuietly(Object obj) {
        if (obj == null) return;
        try { obj.getClass().getMethod("close").invoke(obj); } catch (Exception ignored) {}
    }
}
