package Ellithium.core.ai.scoring;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;

public class SemanticNameExtractorTest {

    // ── stripActionPrefix ─────────────────────────────────────────────────────

    @Test
    public void stripActionPrefix_removesClickPrefix() {
        Assert.assertEquals(SemanticNameExtractor.stripActionPrefix("clickLoginBtn"), "LoginBtn");
    }

    @Test
    public void stripActionPrefix_removesSetPrefix() {
        Assert.assertEquals(SemanticNameExtractor.stripActionPrefix("setUserEmail"), "UserEmail");
    }

    @Test
    public void stripActionPrefix_removesGetPrefix() {
        Assert.assertEquals(SemanticNameExtractor.stripActionPrefix("getPassword"), "Password");
    }

    @Test
    public void stripActionPrefix_removesIsPrefix() {
        Assert.assertEquals(SemanticNameExtractor.stripActionPrefix("isEnabled"), "Enabled");
    }

    @Test
    public void stripActionPrefix_removesLongestMatchFirst() {
        Assert.assertEquals(SemanticNameExtractor.stripActionPrefix("sendDataToField"), "ToField");
    }

    @Test
    public void stripActionPrefix_noMatchReturnsUnchanged() {
        Assert.assertEquals(SemanticNameExtractor.stripActionPrefix("loginSubmit"), "loginSubmit");
    }

    @Test
    public void stripActionPrefix_exactPrefixMatchOnly_notEntireName() {
        // "click" is a prefix but "click" alone has no suffix → returns unchanged
        Assert.assertEquals(SemanticNameExtractor.stripActionPrefix("click"), "click");
    }

    // ── stripUISuffixes ───────────────────────────────────────────────────────

    @Test
    public void stripUISuffixes_removesField() {
        Assert.assertEquals(SemanticNameExtractor.stripUISuffixes("emailField"), "email");
    }

    @Test
    public void stripUISuffixes_removesButton() {
        Assert.assertEquals(SemanticNameExtractor.stripUISuffixes("loginButton"), "login");
    }

    @Test
    public void stripUISuffixes_removesBtn() {
        Assert.assertEquals(SemanticNameExtractor.stripUISuffixes("submitBtn"), "submit");
    }

    @Test
    public void stripUISuffixes_removesInput() {
        Assert.assertEquals(SemanticNameExtractor.stripUISuffixes("passwordInput"), "password");
    }

    @Test
    public void stripUISuffixes_removesLocator() {
        Assert.assertEquals(SemanticNameExtractor.stripUISuffixes("usernameLocator"), "username");
    }

    @Test
    public void stripUISuffixes_noSuffixReturnsUnchanged() {
        Assert.assertEquals(SemanticNameExtractor.stripUISuffixes("email"), "email");
    }

    @Test
    public void stripUISuffixes_doesNotRemoveSuffixThatIsEntireName() {
        // "Btn" alone as entire name should stay unchanged
        Assert.assertEquals(SemanticNameExtractor.stripUISuffixes("Btn"), "Btn");
    }

    // ── extract: field name ───────────────────────────────────────────────────

    @Test
    public void extract_fieldName_producesVariants() {
        List<String> result = SemanticNameExtractor.extract(null, "emailField", null);
        Assert.assertFalse(result.isEmpty(), "must produce candidates from field name");
        Assert.assertTrue(result.contains("email") || result.contains("Email") || result.contains("EMAIL"),
                "must produce email variants, got: " + result);
    }

    @Test
    public void extract_fieldName_isHigherPriorityThanMethodName() {
        List<String> withField  = SemanticNameExtractor.extract("clickUsername", "emailField", null);
        List<String> noField    = SemanticNameExtractor.extract("clickUsername", null, null);
        Assert.assertTrue(withField.size() >= noField.size(),
                "field-based candidates should add to total");
        // first candidates should come from field
        Assert.assertTrue(withField.get(0).toLowerCase().contains("email"),
                "first candidate should be from field: got " + withField.get(0));
    }

    // ── extract: method name ──────────────────────────────────────────────────

    @Test
    public void extract_methodName_stripsActionPrefixAndProducesVariants() {
        List<String> result = SemanticNameExtractor.extract("clickLoginBtn", null, null);
        Assert.assertFalse(result.isEmpty());
        String joined = result.toString().toLowerCase();
        Assert.assertTrue(joined.contains("login"), "should include 'login' variant, got: " + result);
    }

    @Test
    public void extract_methodName_noUsefulTokenAfterStrip_producesEmptyOrSkips() {
        // "click" → stripped → "" → should not add empty candidate
        List<String> result = SemanticNameExtractor.extract("click", null, null);
        for (String s : result) {
            Assert.assertFalse(s.isBlank(), "must not produce blank candidates");
        }
    }

    // ── extract: locator value ────────────────────────────────────────────────

    @Test
    public void extract_locatorValue_normalizesUnderscores() {
        List<String> result = SemanticNameExtractor.extract(null, null, "user_name");
        Assert.assertTrue(result.contains("user name"), "underscore must normalize to space, got: " + result);
    }

    @Test
    public void extract_locatorValue_normalizesHyphens() {
        List<String> result = SemanticNameExtractor.extract(null, null, "login-btn");
        Assert.assertTrue(result.contains("login btn"), "hyphen must normalize to space, got: " + result);
    }

    @Test
    public void extract_locatorValue_capitalizesWords() {
        List<String> result = SemanticNameExtractor.extract(null, null, "first_name");
        Assert.assertTrue(result.contains("First Name"), "should produce title-case, got: " + result);
    }

    @Test
    public void extract_locatorValue_keepsOriginalForm() {
        List<String> result = SemanticNameExtractor.extract(null, null, "user_email");
        Assert.assertTrue(result.contains("user_email"), "original locator value must be kept, got: " + result);
    }

    // ── extract: all null / blank ─────────────────────────────────────────────

    @Test
    public void extract_allNull_returnsEmptyList() {
        List<String> result = SemanticNameExtractor.extract(null, null, null);
        Assert.assertTrue(result.isEmpty(), "all-null inputs must yield empty list");
    }

    @Test
    public void extract_allBlank_returnsEmptyList() {
        List<String> result = SemanticNameExtractor.extract("", "  ", "");
        Assert.assertTrue(result.isEmpty(), "all-blank inputs must yield empty list");
    }

    // ── extract: deduplication ────────────────────────────────────────────────

    @Test
    public void extract_noDuplicateCandidates() {
        List<String> result = SemanticNameExtractor.extract("clickEmail", "emailField", "email");
        long distinct = result.stream().distinct().count();
        Assert.assertEquals(result.size(), distinct, "duplicates must be removed: " + result);
    }
}
