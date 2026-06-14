package Ellithium.core.ai.config;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AIConfigLoaderTest {

    @BeforeClass
    public void init() {
        AIConfigLoader.initialize();
    }

    // ── strategy & mode ───────────────────────────────────────────────────────

    @Test
    public void getHealingStrategy_returnsNonNull() {
        Assert.assertNotNull(AIConfigLoader.getHealingStrategy());
    }

    @Test
    public void getExecutionMode_returnsNonNull() {
        Assert.assertNotNull(AIConfigLoader.getExecutionMode());
    }

    @Test
    public void isCI_matchesExecutionMode() {
        boolean ciMode = AIConfigLoader.getExecutionMode() == AIConfigLoader.ExecutionMode.CI;
        Assert.assertEquals(AIConfigLoader.isCI(), ciMode,
                "isCI() must match ExecutionMode.CI comparison");
    }

    // ── thresholds are in [0,1] ───────────────────────────────────────────────

    @Test
    public void confidenceThreshold_isInUnitInterval() {
        double v = AIConfigLoader.getConfidenceThreshold();
        Assert.assertTrue(v >= 0.0 && v <= 1.0,
                "Confidence threshold must be in [0,1], got: " + v);
    }

    @Test
    public void healingStoreThreshold_isInUnitInterval() {
        double v = AIConfigLoader.getHealingStoreThreshold();
        Assert.assertTrue(v >= 0.0 && v <= 1.0,
                "Store threshold must be in [0,1], got: " + v);
    }

    @Test
    public void onnxSimilarityThreshold_isInUnitInterval() {
        double v = AIConfigLoader.getOnnxSimilarityThreshold();
        Assert.assertTrue(v >= 0.0 && v <= 1.0,
                "ONNX similarity threshold must be in [0,1], got: " + v);
    }

    @Test
    public void semanticFallbackScore_isInUnitInterval() {
        double v = AIConfigLoader.getSemanticFallbackScore();
        Assert.assertTrue(v >= 0.0 && v <= 1.0,
                "Semantic fallback score must be in [0,1], got: " + v);
    }

    @Test
    public void tier3BaselineMatchFloor_isInUnitInterval() {
        double v = AIConfigLoader.getTier3BaselineMatchFloor();
        Assert.assertTrue(v >= 0.0 && v <= 1.0,
                "Tier-3 baseline match floor must be in [0,1], got: " + v);
    }

    // ── candidate and retry limits are positive ───────────────────────────────

    @Test
    public void maxCandidates_isInValidRange() {
        int v = AIConfigLoader.getMaxCandidates();
        Assert.assertTrue(v >= 1 && v <= 10,
                "maxCandidates must be in [1,10], got: " + v);
    }

    @Test
    public void onnxMaxCandidates_isPositive() {
        Assert.assertTrue(AIConfigLoader.getOnnxMaxCandidates() > 0,
                "onnxMaxCandidates must be positive");
    }

    @Test
    public void onnxHardCandidateLimit_isPositive() {
        Assert.assertTrue(AIConfigLoader.getOnnxHardCandidateLimit() > 0);
    }

    @Test
    public void llmMaxRetries_isNonNegative() {
        Assert.assertTrue(AIConfigLoader.getLlmMaxRetries() >= 0);
    }

    @Test
    public void llmHealMaxWaitMs_isPositive() {
        Assert.assertTrue(AIConfigLoader.getLlmHealMaxWaitMs() > 0,
                "LLM max wait must be > 0ms");
    }

    @Test
    public void telemetryMaxRecords_isPositive() {
        Assert.assertTrue(AIConfigLoader.getTelemetryMaxRecords() > 0);
    }

    @Test
    public void baselineTtlDays_isPositive() {
        Assert.assertTrue(AIConfigLoader.getBaselineTtlDays() > 0);
    }

    // ── grouped config records are non-null and consistent ───────────────────

    @Test
    public void llmConfigRecord_isNonNull() {
        Assert.assertNotNull(AIConfigLoader.llm());
    }

    @Test
    public void thresholdsConfigRecord_isNonNull() {
        Assert.assertNotNull(AIConfigLoader.thresholds());
    }

    @Test
    public void modelConfigRecord_isNonNull() {
        Assert.assertNotNull(AIConfigLoader.model());
    }

    @Test
    public void thresholdsRecord_matchesIndividualGetters() {
        AIConfigLoader.ThresholdConfig t = AIConfigLoader.thresholds();
        Assert.assertEquals(t.confidence(), AIConfigLoader.getConfidenceThreshold(), 1e-9);
        Assert.assertEquals(t.storeThreshold(), AIConfigLoader.getHealingStoreThreshold(), 1e-9);
        Assert.assertEquals(t.visionWeb(), AIConfigLoader.isVisionAllowedOnWeb());
        Assert.assertEquals(t.visionMobile(), AIConfigLoader.isVisionAllowedOnMobile());
    }

    @Test
    public void modelConfigRecord_matchesIndividualGetters() {
        AIConfigLoader.ModelConfig m = AIConfigLoader.model();
        Assert.assertEquals(m.maxCandidates(), AIConfigLoader.getMaxCandidates());
        Assert.assertEquals(m.tier3Enabled(), AIConfigLoader.isTier3Enabled());
        Assert.assertEquals(m.onnxMaxCandidates(), AIConfigLoader.getOnnxMaxCandidates());
    }

    @Test
    public void llmConfigRecord_matchesIndividualGetters() {
        AIConfigLoader.LlmConfig l = AIConfigLoader.llm();
        Assert.assertEquals(l.apiKey(), AIConfigLoader.getLlmApiKey());
        Assert.assertEquals(l.maxWaitMs(), AIConfigLoader.getLlmHealMaxWaitMs());
        Assert.assertEquals(l.maxRetries(), AIConfigLoader.getLlmMaxRetries());
    }

    // ── idempotency ───────────────────────────────────────────────────────────

    @Test
    public void initialize_isIdempotent() {
        double before = AIConfigLoader.getConfidenceThreshold();
        AIConfigLoader.initialize();
        Assert.assertEquals(AIConfigLoader.getConfidenceThreshold(), before, 1e-9,
                "Repeated initialize() must not change confidence threshold");
    }
}
