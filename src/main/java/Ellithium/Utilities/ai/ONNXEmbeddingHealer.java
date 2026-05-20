package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.models.ElementFingerprint;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Tier 3 — Local ONNX Embedding Healer (paid feature skeleton).
 *
 * <h3>Current state</h3>
 * <p>This class is fully scaffolded and wired into the healing cascade.
 * {@link #isAvailable()} returns {@code false} until two conditions are both true:</p>
 * <ol>
 *   <li>A valid Tier 3 license key is configured in {@code ai-config.properties}.</li>
 *   <li>The encrypted fine-tuned model ({@code /ai-models/tier3.onnx.enc}) is bundled
 *       inside the Ellithium JAR.</li>
 * </ol>
 * <p>Until then, {@link #tryEmbeddingHeal} returns {@code null} silently, and the
 * healing cascade falls through to Tier 4 (LLM) with no behavior change.</p>
 *
 * <h3>Activation (future sprint)</h3>
 * <ol>
 *   <li>Export the Kaggle fine-tuned model to ONNX.</li>
 *   <li>Encrypt with {@code ModelEncryptionUtil} → {@code tier3.onnx.enc}.</li>
 *   <li>Place under {@code src/main/resources/ai-models/}.</li>
 *   <li>Implement {@link #embed(String)} using the ORT session.</li>
 *   <li>Implement full {@link #tryEmbeddingHeal} candidate scoring loop.</li>
 * </ol>
 */
public class ONNXEmbeddingHealer {

    private static volatile boolean initialized = false;
    private static volatile boolean available   = false;

    // OrtEnvironment and OrtSession held as Object references to avoid ClassNotFoundException
    // when onnxruntime JAR is absent (optional dependency).
    private static Object ortEnvironment = null;
    private static Object ortSession     = null;

    // ──────────────────────── Lifecycle ────────────────────────

    /**
     * Initialises the ONNX session if license + model are present.
     * Called by {@code GeneralHandler} at suite startup, after {@code AISelfHealer.initialize()}.
     * Safe to call multiple times — no-op after first call.
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;

        if (!AIConfigLoader.isOnnxEnabled()) {
            Reporter.log("[TIER 3] Disabled — license key not configured or model not yet embedded",
                    LogLevel.DEBUG);
            return;
        }

        String licenseKey = AIConfigLoader.getLicenseKey();
        byte[] modelBytes = ModelDecryptor.decryptModel(licenseKey);
        if (modelBytes == null) {
            Reporter.log("[TIER 3] Model decryption failed — Tier 3 unavailable", LogLevel.WARN);
            return;
        }

        try {
            initOrtSession(modelBytes);
            available = true;
            Reporter.log("[TIER 3] ONNX session initialised — Tier 3 active ("
                    + modelBytes.length + " bytes)", LogLevel.INFO_GREEN);
        } catch (ClassNotFoundException e) {
            Reporter.log("[TIER 3] onnxruntime JAR not on classpath — Tier 3 unavailable", LogLevel.DEBUG);
        } catch (Exception e) {
            Reporter.log("[TIER 3] ONNX session init failed: " + e.getMessage() + " — Tier 3 unavailable",
                    LogLevel.WARN);
        }
    }

    /**
     * Closes the ONNX session and releases native memory.
     * Called by {@code GeneralHandler} / {@code CustomTestNGListener} at suite teardown.
     */
    public static void shutdown() {
        available = false;
        if (ortSession != null) {
            try {
                ortSession.getClass().getMethod("close").invoke(ortSession);
            } catch (Exception ignored) {}
            ortSession = null;
        }
        if (ortEnvironment != null) {
            try {
                ortEnvironment.getClass().getMethod("close").invoke(ortEnvironment);
            } catch (Exception ignored) {}
            ortEnvironment = null;
        }
        initialized = false;
        Reporter.log("[TIER 3] ONNX session closed", LogLevel.DEBUG);
    }

    /** Returns {@code true} when the ONNX session is loaded and ready for inference. */
    public static boolean isAvailable() { return available; }

    // ──────────────────────── Heal Entry Point ────────────────────────

    /**
     * Attempts to heal a broken locator using local ONNX embedding similarity search.
     *
     * <p>Returns {@code null} immediately when {@link #isAvailable()} is false (silent no-op).
     * The full implementation (DOM candidate embedding + cosine scoring + cache) is activated
     * when the fine-tuned model is embedded.</p>
     *
     * @param driver        The WebDriver instance
     * @param locator       The broken locator
     * @param actionType    Ellithium action type (e.g., "sendData")
     * @param callerMethod  Caller POM method name (e.g., "clickLoginButton")
     * @param fieldName     Locator field name, may be null
     * @param locatorValue  Raw locator value string
     * @param baseline      Stored baseline fingerprint, may be null
     * @return The healed WebElement, or {@code null} if unavailable or no confident match
     */
    public static WebElement tryEmbeddingHeal(WebDriver driver, By locator,
                                               String actionType, String callerMethod,
                                               String fieldName, String locatorValue,
                                               ElementFingerprint baseline) {
        if (!available) return null;

        // Build semantic query for embedding
        String query = SemanticQueryBuilder.buildFromContext(actionType, locatorValue, callerMethod, baseline);
        if (query.isBlank()) return null;

        // Embed the query
        float[] queryVector = embed(query);
        if (queryVector == null) return null;

        // Score DOM candidates — stub until model is embedded
        return scoreAndSelectCandidate(driver, queryVector, baseline, locator);
    }

    // ──────────────────────── Embedding ────────────────────────

    /**
     * Embeds a text string into a float vector using the loaded ONNX model.
     * Returns {@code null} when the session is unavailable or inference fails.
     *
     * <p>Stub implementation — full tokenization + ORT inference wired when model is added.</p>
     */
    static float[] embed(String text) {
        if (!available || ortSession == null) return null;
        // Full implementation: tokenize(text) → OrtTensor → session.run() → float[]
        // Wired in the sprint that embeds the Kaggle model.
        return null;
    }

    // ──────────────────────── Candidate Scoring ────────────────────────

    private static WebElement scoreAndSelectCandidate(WebDriver driver, float[] queryVector,
                                                       ElementFingerprint baseline, By brokenLocator) {
        // Stub — full scoring loop (ElementVectorCache + cosineSimilarity + threshold/maxCandidates)
        // wired in the sprint that activates embed().
        return null;
    }

    /**
     * Computes cosine similarity between two float vectors.
     * Returns 0.0 if either vector is null or zero-magnitude.
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

    /**
     * Builds a concise document string from a WebElement's visible attributes
     * for embedding as a DOM candidate.
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
            if (text != null && !text.isBlank() && text.length() <= 80) sb.append(" ").append(text.trim());
        } catch (Exception ignored) {}
        return sb.toString().trim();
    }

    private static void appendAttr(StringBuilder sb, WebElement el, String attr) {
        try {
            String v = el.getAttribute(attr);
            if (v != null && !v.isBlank()) sb.append(" ").append(v.trim());
        } catch (Exception ignored) {}
    }

    // ──────────────────────── ORT Lifecycle (Reflection-Guarded) ────────────────────────

    private static void initOrtSession(byte[] modelBytes) throws Exception {
        // ClassNotFoundException propagates to caller if onnxruntime is absent
        Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
        ortEnvironment = envClass.getMethod("getEnvironment").invoke(null);

        Class<?> sessionOptionsClass = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
        Object sessionOptions = sessionOptionsClass.getDeclaredConstructor().newInstance();

        ortSession = ortEnvironment.getClass()
                .getMethod("createSession", byte[].class, sessionOptionsClass)
                .invoke(ortEnvironment, modelBytes, sessionOptions);
    }
}
