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

    // ── title/label field scoring (regression guard for the fix that added these two fields) ──

    @Test
    public void scoreSimilarity_titleField_scoredAsOnlyField() {
        ElementFingerprint fp = GSON.fromJson("{\"title\":\"Submit form\"}", ElementFingerprint.class);
        WebElement match = mock(WebElement.class);
        when(match.getAttribute("title")).thenReturn("Submit form");
        Assert.assertEquals(fp.scoreSimilarity(match), 1.0, 1e-9,
                "title-only fingerprint with exact match must score 1.0");
    }

    @Test
    public void scoreSimilarity_labelField_scoredAsOnlyField() {
        ElementFingerprint fp = GSON.fromJson("{\"label\":\"Continue\"}", ElementFingerprint.class);
        WebElement match = mock(WebElement.class);
        when(match.getAttribute("label")).thenReturn("Continue");
        Assert.assertEquals(fp.scoreSimilarity(match), 1.0, 1e-9,
                "label-only fingerprint with exact match must score 1.0");
    }

    @Test
    public void scoreSimilarity_titleAndLabel_bothContributeToScore() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"title\":\"Save\",\"label\":\"Save button\"}", ElementFingerprint.class);

        WebElement fullMatch = mock(WebElement.class);
        when(fullMatch.getAttribute("title")).thenReturn("Save");
        when(fullMatch.getAttribute("label")).thenReturn("Save button");
        Assert.assertEquals(fp.scoreSimilarity(fullMatch), 1.0, 1e-9,
                "title+label fingerprint with both fields matching must score 1.0");

        WebElement noMatch = mock(WebElement.class);
        Assert.assertEquals(fp.scoreSimilarity(noMatch), 0.0, 1e-9,
                "title+label fingerprint with neither field matching must score 0.0");
    }

    @Test
    public void scoreSimilarity_name_exactMatch_isOne() {
        ElementFingerprint fp = GSON.fromJson("{\"name\":\"email\"}", ElementFingerprint.class);
        WebElement match = elem(null, null, "email", null, null, null);
        Assert.assertEquals(fp.scoreSimilarity(match), 1.0, 1e-9,
                "name-only fingerprint exact match must score 1.0 (20 pts / 20 dynamicMax)");
    }

    @Test
    public void scoreSimilarity_placeholder_exactMatch_isOne() {
        ElementFingerprint fp = GSON.fromJson("{\"placeholder\":\"Enter email\"}", ElementFingerprint.class);
        WebElement match = mock(WebElement.class);
        when(match.getAttribute("placeholder")).thenReturn("Enter email");
        Assert.assertEquals(fp.scoreSimilarity(match), 1.0, 1e-9,
                "placeholder-only fingerprint exact match must score 1.0 (15 pts / 15 dynamicMax)");
    }

    @Test
    public void scoreSimilarity_type_partialMatch_givesExpectedScore() {
        // fp has id(25pts) + type(8pts) = 33 dynamicMax; candidate matches only type
        ElementFingerprint fp = GSON.fromJson(
                "{\"id\":\"email-input\",\"type\":\"email\"}", ElementFingerprint.class);
        WebElement typeOnly = elem(null, null, null, "email", null, null);
        double score = fp.scoreSimilarity(typeOnly);
        Assert.assertTrue(score > 0.0 && score < 1.0,
                "type-only match should earn partial credit, not 0 or 1. Got " + score);
        Assert.assertEquals(score, 8.0 / 33.0, 1e-9,
                "type match = 8pts, dynamicMax = 25(id) + 8(type) = 33 → 8/33");
    }

    // ── computeDynamicMax ──

    @Test
    public void computeDynamicMax_tagOnly_returnsFive() {
        ElementFingerprint fp = GSON.fromJson("{\"tagName\":\"button\"}", ElementFingerprint.class);
        Assert.assertEquals(fp.computeDynamicMax(), 5,
                "tagName alone contributes 5 pts — all other attributes null");
    }

    @Test
    public void computeDynamicMax_tagAndClass_returnsTen() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"tagName\":\"div\",\"className\":\"btn-primary\"}", ElementFingerprint.class);
        Assert.assertEquals(fp.computeDynamicMax(), 10, "tagName(5) + className(5) = 10");
    }

    @Test
    public void computeDynamicMax_idOnly_returns25() {
        ElementFingerprint fp = GSON.fromJson("{\"id\":\"loginBtn\"}", ElementFingerprint.class);
        Assert.assertEquals(fp.computeDynamicMax(), 25, "id contributes 25 pts");
    }

    @Test
    public void computeDynamicMax_dataTestIdOnly_returns30() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"dataTestId\":\"login-submit\"}", ElementFingerprint.class);
        Assert.assertEquals(fp.computeDynamicMax(), 30, "dataTestId contributes 30 pts");
    }

    @Test
    public void computeDynamicMax_multipleFields_returnsCorrectSum() {
        // id(25) + name(20) + type(8) + tagName(5) = 58
        ElementFingerprint fp = GSON.fromJson(
                "{\"id\":\"email\",\"name\":\"email\",\"type\":\"email\",\"tagName\":\"input\"}",
                ElementFingerprint.class);
        Assert.assertEquals(fp.computeDynamicMax(), 58,
                "id(25) + name(20) + type(8) + tagName(5) = 58");
    }

    @Test
    public void computeDynamicMax_belowPersistThreshold_whenTagAndTypeOnly() {
        // tag(5) + type(8) = 13 — below the dynamicMax<15 persist-skip guard in BaselineStore
        ElementFingerprint fp = GSON.fromJson(
                "{\"tagName\":\"button\",\"type\":\"submit\"}", ElementFingerprint.class);
        int dmax = fp.computeDynamicMax();
        Assert.assertEquals(dmax, 13, "tagName(5) + type(8) = 13");
        Assert.assertTrue(dmax < 15,
                "dynamicMax < 15 triggers the BaselineStore persist-skip gate; 13 must be < 15");
    }

    // ── hasStrongIdentity ──

    @Test
    public void hasStrongIdentity_withId_returnsTrue() {
        ElementFingerprint fp = GSON.fromJson("{\"id\":\"submit\"}", ElementFingerprint.class);
        Assert.assertTrue(fp.hasStrongIdentity(), "id is a strong-identity anchor");
    }

    @Test
    public void hasStrongIdentity_withName_returnsTrue() {
        ElementFingerprint fp = GSON.fromJson("{\"name\":\"username\"}", ElementFingerprint.class);
        Assert.assertTrue(fp.hasStrongIdentity(), "name is a strong-identity anchor");
    }

    @Test
    public void hasStrongIdentity_withDataTestId_returnsTrue() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"dataTestId\":\"login-btn\"}", ElementFingerprint.class);
        Assert.assertTrue(fp.hasStrongIdentity(), "dataTestId is a strong-identity anchor");
    }

    @Test
    public void hasStrongIdentity_withResourceId_returnsTrue() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"resourceId\":\"com.app:id/loginBtn\"}", ElementFingerprint.class);
        Assert.assertTrue(fp.hasStrongIdentity(), "resourceId is a strong-identity anchor");
    }

    @Test
    public void hasStrongIdentity_tagAndClassOnly_returnsFalse() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"tagName\":\"div\",\"className\":\"grid-row active\"}", ElementFingerprint.class);
        Assert.assertFalse(fp.hasStrongIdentity(),
                "tagName + className are weak signals — no stable identity anchor");
    }

    @Test
    public void hasStrongIdentity_ariaLabelOnly_returnsFalse() {
        ElementFingerprint fp = GSON.fromJson("{\"ariaLabel\":\"Login\"}", ElementFingerprint.class);
        Assert.assertFalse(fp.hasStrongIdentity(),
                "ariaLabel is not a strong-identity anchor (scores 15 pts but is mutable text)");
    }

    @Test
    public void hasStrongIdentity_roleAndTypeOnly_returnsFalse() {
        ElementFingerprint fp = GSON.fromJson(
                "{\"role\":\"button\",\"type\":\"submit\"}", ElementFingerprint.class);
        Assert.assertFalse(fp.hasStrongIdentity(),
                "role + type have no stable identity value — expected false");
    }
}
