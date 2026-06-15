package Ellithium.core.ai.models;

import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class ElementFingerprintTest {

    private static final Gson GSON = new Gson();

    private ElementFingerprint fp(String json) {
        return GSON.fromJson(json, ElementFingerprint.class);
    }

    // ── computeDynamicMax ────────────────────────────────────────────────────

    @Test
    public void computeDynamicMax_emptyFingerprint_returnsZero() {
        Assert.assertEquals(fp("{}").computeDynamicMax(), 0);
    }

    @Test
    public void computeDynamicMax_idOnly_returns25() {
        Assert.assertEquals(fp("{\"id\":\"login\"}").computeDynamicMax(), 25);
    }

    @Test
    public void computeDynamicMax_tagOnly_returns5() {
        Assert.assertEquals(fp("{\"tagName\":\"button\"}").computeDynamicMax(), 5);
    }

    @Test
    public void computeDynamicMax_dataTestId_returns30() {
        Assert.assertEquals(fp("{\"dataTestId\":\"submit-btn\"}").computeDynamicMax(), 30);
    }

    @Test
    public void computeDynamicMax_multipleAttributes_sumIsCorrect() {
        // id=25, name=20, ariaLabel=15
        ElementFingerprint f = fp("{\"id\":\"u\",\"name\":\"n\",\"ariaLabel\":\"a\"}");
        Assert.assertEquals(f.computeDynamicMax(), 60);
    }

    @Test
    public void computeDynamicMax_resourceId_returns30() {
        Assert.assertEquals(fp("{\"resourceId\":\"com.app:id/btn\"}").computeDynamicMax(), 30);
    }

    // ── hasStrongIdentity ────────────────────────────────────────────────────

    @Test
    public void hasStrongIdentity_withId_returnsTrue() {
        Assert.assertTrue(fp("{\"id\":\"login\"}").hasStrongIdentity());
    }

    @Test
    public void hasStrongIdentity_withDataTestId_returnsTrue() {
        Assert.assertTrue(fp("{\"dataTestId\":\"submit\"}").hasStrongIdentity());
    }

    @Test
    public void hasStrongIdentity_withResourceId_returnsTrue() {
        Assert.assertTrue(fp("{\"resourceId\":\"com.app:id/btn\"}").hasStrongIdentity());
    }

    @Test
    public void hasStrongIdentity_weakSignalsOnly_returnsFalse() {
        Assert.assertFalse(fp("{\"tagName\":\"button\",\"className\":\"primary\"}").hasStrongIdentity());
    }

    @Test
    public void hasStrongIdentity_emptyFingerprint_returnsFalse() {
        Assert.assertFalse(fp("{}").hasStrongIdentity());
    }

    // ── isNativeWidgetClass ──────────────────────────────────────────────────

    @Test
    public void isNativeWidgetClass_androidWidget_returnsTrue() {
        Assert.assertTrue(ElementFingerprint.isNativeWidgetClass("android.widget.Button"));
    }

    @Test
    public void isNativeWidgetClass_xcuiType_returnsTrue() {
        Assert.assertTrue(ElementFingerprint.isNativeWidgetClass("XCUIElementTypeButton"));
    }

    @Test
    public void isNativeWidgetClass_htmlTag_returnsFalse() {
        Assert.assertFalse(ElementFingerprint.isNativeWidgetClass("button"));
    }

    @Test
    public void isNativeWidgetClass_null_returnsFalse() {
        Assert.assertFalse(ElementFingerprint.isNativeWidgetClass(null));
    }

    @Test
    public void isNativeWidgetClass_blank_returnsFalse() {
        Assert.assertFalse(ElementFingerprint.isNativeWidgetClass("   "));
    }

    // ── isCssSafeIdentifier ─────────────────────────────────────────────────

    @Test
    public void isCssSafeIdentifier_validClass_returnsTrue() {
        Assert.assertTrue(ElementFingerprint.isCssSafeIdentifier("btn-primary"));
    }

    @Test
    public void isCssSafeIdentifier_underscoredClass_returnsTrue() {
        Assert.assertTrue(ElementFingerprint.isCssSafeIdentifier("_my_class"));
    }

    @Test
    public void isCssSafeIdentifier_withSpaces_returnsFalse() {
        Assert.assertFalse(ElementFingerprint.isCssSafeIdentifier("two words"));
    }

    @Test
    public void isCssSafeIdentifier_withColon_returnsFalse() {
        Assert.assertFalse(ElementFingerprint.isCssSafeIdentifier("ng:model"));
    }

    @Test
    public void isCssSafeIdentifier_startsWithDigit_returnsFalse() {
        Assert.assertFalse(ElementFingerprint.isCssSafeIdentifier("1invalid"));
    }

    @Test
    public void isCssSafeIdentifier_null_returnsFalse() {
        Assert.assertFalse(ElementFingerprint.isCssSafeIdentifier(null));
    }

    // ── isInsideIframe / getIframeChain ──────────────────────────────────────

    @Test
    public void isInsideIframe_noChain_returnsFalse() {
        Assert.assertFalse(fp("{}").isInsideIframe());
    }

    @Test
    public void getIframeChain_noChain_returnsEmptyList() {
        Assert.assertTrue(fp("{}").getIframeChain().isEmpty());
    }

    // ── scoreSimilarity(Map) — zero-WebDriver-call path ──────────────────────

    @Test
    public void scoreSimilarityMap_exactIdMatch_returnsHighScore() {
        ElementFingerprint baseline = fp("{\"id\":\"login-btn\",\"tagName\":\"button\"}");
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("id", "login-btn");
        candidate.put("tag", "button");
        double score = baseline.scoreSimilarity(candidate);
        Assert.assertTrue(score >= 0.99, "Exact id+tag match should be near 1.0, was " + score);
    }

    @Test
    public void scoreSimilarityMap_noMatch_returnsZero() {
        ElementFingerprint baseline = fp("{\"id\":\"login-btn\"}");
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("id", "totally-different");
        double score = baseline.scoreSimilarity(candidate);
        Assert.assertEquals(score, 0.0, 0.01);
    }

    @Test
    public void scoreSimilarityMap_nullMap_returnsZero() {
        ElementFingerprint baseline = fp("{\"id\":\"login-btn\"}");
        Assert.assertEquals(baseline.scoreSimilarity((Map<String, Object>) null), 0.0, 0.001);
    }

    @Test
    public void scoreSimilarityMap_emptyFingerprint_returnsZero() {
        ElementFingerprint baseline = fp("{}");
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("id", "any");
        Assert.assertEquals(baseline.scoreSimilarity(candidate), 0.0, 0.001);
    }

    @Test
    public void scoreSimilarityMap_ariaLabelExact_scores15of15() {
        ElementFingerprint baseline = fp("{\"ariaLabel\":\"Submit form\"}");
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("aria-label", "Submit form");
        Assert.assertEquals(baseline.scoreSimilarity(candidate), 1.0, 0.001);
    }

    @Test
    public void scoreSimilarityMap_dataTestIdExact_scores30of30() {
        ElementFingerprint baseline = fp("{\"dataTestId\":\"submit-btn\"}");
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("data-testid", "submit-btn");
        Assert.assertEquals(baseline.scoreSimilarity(candidate), 1.0, 0.001);
    }

    // ── getters / toString ───────────────────────────────────────────────────

    @Test
    public void getters_returnExpectedValues() {
        ElementFingerprint f = fp("{\"id\":\"u\",\"tagName\":\"input\",\"type\":\"text\",\"placeholder\":\"Enter email\"}");
        Assert.assertEquals(f.getId(), "u");
        Assert.assertEquals(f.getTagName(), "input");
        Assert.assertEquals(f.getType(), "text");
        Assert.assertEquals(f.getPlaceholder(), "Enter email");
    }

    @Test
    public void toString_containsLocatorKey() {
        ElementFingerprint f = fp("{\"locatorKey\":\"By.id: login\",\"id\":\"login\"}");
        Assert.assertTrue(f.toString().contains("By.id: login"));
    }
}
