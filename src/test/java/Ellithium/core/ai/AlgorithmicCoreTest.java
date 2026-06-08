package Ellithium.core.ai;

import Ellithium.core.ai.scoring.LocatorMutationEngine;
import Ellithium.core.ai.scoring.SemanticQueryBuilder;
import Ellithium.core.ai.models.ElementFingerprint;
import com.google.gson.Gson;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the deterministic Tier 1 / query-building core. These exist specifically because
 * the {@code scoreSimilarity} normalization bug (parentTag/childIndex inflating the denominator and
 * capping a perfect match at ~0.857) shipped silently with zero coverage. A perfect-match==1.0
 * assertion is the guard.
 */
public class AlgorithmicCoreTest {

    private static final Gson GSON = new Gson();

    private static WebElement elem(String tag, String id, String name, String type,
                                   String cls, String ariaLabel) {
        WebElement e = mock(WebElement.class);
        when(e.getTagName()).thenReturn(tag);
        when(e.getAttribute("id")).thenReturn(id);
        when(e.getAttribute("name")).thenReturn(name);
        when(e.getAttribute("type")).thenReturn(type);
        when(e.getAttribute("class")).thenReturn(cls);
        when(e.getAttribute("aria-label")).thenReturn(ariaLabel);
        return e;
    }

    // ── scoreSimilarity normalization (the regression guard) ──

    @Test
    public void scoreSimilarity_perfectMatch_isExactlyOne() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"id\":\"username\",\"name\":\"user\",\"tagName\":\"input\",\"type\":\"text\"}",
                ElementFingerprint.class);
        WebElement candidate = elem("input", "username", "user", "text", null, null);
        double score = fp.scoreSimilarity(candidate);
        Assert.assertEquals(score, 1.0, 1e-9,
                "A candidate matching every stored attribute must score exactly 1.0 "
                + "(regression guard for the dynamicMax unearnable-points bug). Got " + score);
    }

    @Test
    public void scoreSimilarity_noMatch_isZero() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"id\":\"username\",\"tagName\":\"input\"}", ElementFingerprint.class);
        WebElement candidate = elem("a", "totally-different", null, null, null, null);
        Assert.assertEquals(fp.scoreSimilarity(candidate), 0.0, 1e-9,
                "A candidate sharing nothing with the baseline must score 0.0");
    }

    @Test
    public void scoreSimilarity_idTokenRename_givesPartialCredit() {
        // Same identifier across naming conventions → token-Jaccard fuzzy match (12 of 25 pts).
        ElementFingerprint fp = GSON.fromJson(
                "{\"id\":\"login-submit-btn\"}", ElementFingerprint.class);
        WebElement candidate = elem(null, "loginSubmitBtn", null, null, null, null);
        double score = fp.scoreSimilarity(candidate);
        Assert.assertTrue(score > 0.0 && score < 1.0,
                "id token-rename should earn partial (fuzzy) credit, not 0 or 1. Got " + score);
        Assert.assertEquals(score, 12.0 / 25.0, 1e-9, "expected token-Jaccard partial = 12/25");
    }

    @Test
    public void scoreSimilarity_structuralContext_boostsSamePosition_andDiscriminatesDifferent() {
        // Two candidates with the SAME attributes; structural context decides between them.
        ElementFingerprint fp = GSON.fromJson(
                "{\"id\":\"submit\",\"tagName\":\"button\",\"parentTag\":\"form\",\"childIndex\":2,"
                + "\"prevSiblingTag\":\"input\",\"nextSiblingTag\":\"a\"}",
                ElementFingerprint.class);
        WebElement candidate = elem("button", "submit", null, null, null, null);

        double noCtx   = fp.scoreSimilarity(candidate);                       // structure omitted
        double samePos = fp.scoreSimilarity(candidate,
                new ElementFingerprint.StructuralContext("form", 2, "input", "a"));   // exact layout
        double diffPos = fp.scoreSimilarity(candidate,
                new ElementFingerprint.StructuralContext("div", 9, "span", "span"));  // wrong layout

        Assert.assertEquals(noCtx, 1.0, 1e-9, "attribute-only perfect match stays 1.0");
        Assert.assertEquals(samePos, 1.0, 1e-9, "matching layout keeps a perfect score (earnable points)");
        Assert.assertTrue(diffPos < samePos,
                "a candidate in the wrong DOM position must score lower than one in the right position");
    }

    @Test
    public void scoreSimilarity_classJaccard_matchesOnSharedClasses() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"className\":\"btn primary large\"}", ElementFingerprint.class);
        WebElement candidate = elem(null, null, null, null, "btn primary large", null);
        Assert.assertEquals(fp.scoreSimilarity(candidate), 1.0, 1e-9,
                "identical class set → Jaccard 1.0 → full (only) class points → 1.0");
    }

    // ── LocatorMutationEngine.generateValueMutations ──

    @Test
    public void scoreSimilarity_typoDrift_earnsLevenshteinFallback() {
        // token-Jaccard("usrname" vs "username") = 0 (different tokens); edit-ratio ~0.875 ≥ 0.82
        // → fuzzy id match earns the partial 12/25 instead of 0.
        ElementFingerprint fp = GSON.fromJson("{\"id\":\"username\"}", ElementFingerprint.class);
        WebElement candidate = elem(null, "usrname", null, null, null, null);
        double score = fp.scoreSimilarity(candidate);
        Assert.assertEquals(score, 12.0 / 25.0, 1e-9,
                "a 1-char typo must earn the Levenshtein fuzzy-id partial, not 0. Got " + score);
    }

    // ── LocatorMutationEngine cross-validation (the false-positive guard) ──

    @Test
    public void mutation_rejectsTagMismatchedWrongElement() {
        // Baseline is a BUTTON; the mutation resolves a LINK sharing the id token → must be rejected
        // (tag mismatch demands ≥0.75; a link with only a partial-id overlap can't clear it).
        ElementFingerprint baseline = GSON.fromJson(
                "{\"id\":\"loginBtn\",\"tagName\":\"button\"}", ElementFingerprint.class);
        WebElement wrongLink = elem("a", "login", null, null, null, null);
        double score = baseline.scoreSimilarity(wrongLink);
        Assert.assertTrue(score < 0.75,
                "a tag-mismatched same-token element must score below the mutation accept bar. Got " + score);
    }

    @Test
    public void mutation_acceptsSameTagConventionRename() {
        // Same tag (button) + id convention rename → clears the 0.55 bar.
        ElementFingerprint baseline = GSON.fromJson(
                "{\"id\":\"login-btn\",\"tagName\":\"button\"}", ElementFingerprint.class);
        WebElement renamed = elem("button", "loginBtn", null, null, null, null);
        Assert.assertTrue(baseline.scoreSimilarity(renamed) >= 0.55,
                "a same-tag convention rename must clear the 0.55 mutation accept bar");
    }

    @Test
    public void generateValueMutations_coversConventionRenames() {
        List<String> m = LocatorMutationEngine.generateValueMutations("loginBtn");
        Assert.assertTrue(m.contains("login-btn"), "kebab variant missing: " + m);
        Assert.assertTrue(m.contains("login_btn"), "snake variant missing: " + m);
        Assert.assertTrue(m.contains("login"),     "suffix-strip variant missing: " + m);
        Assert.assertTrue(m.contains("btn-login"), "reversed-token variant missing: " + m);
        Assert.assertFalse(m.contains("loginBtn"), "must not re-suggest the original broken value");
    }

    // ── SemanticQueryBuilder.deCamelCase ──

    @Test
    public void deCamelCase_splitsAllConventions() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("loginSubmitBtn"), "login submit btn");
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("data-testid"), "data testid");
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("user_name"), "user name");
    }
}
