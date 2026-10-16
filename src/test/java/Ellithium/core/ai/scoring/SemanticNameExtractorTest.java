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

    @Test
    public void isStateOrUtilityModifier_detectsModifiers() {
        Assert.assertTrue(SemanticNameExtractor.isStateOrUtilityModifier("active"));
        Assert.assertTrue(SemanticNameExtractor.isStateOrUtilityModifier("disabled"));
        Assert.assertTrue(SemanticNameExtractor.isStateOrUtilityModifier("selected"));
        Assert.assertTrue(SemanticNameExtractor.isStateOrUtilityModifier("btn"));
        Assert.assertTrue(SemanticNameExtractor.isStateOrUtilityModifier("primary"));
        Assert.assertTrue(SemanticNameExtractor.isStateOrUtilityModifier(""));
        Assert.assertTrue(SemanticNameExtractor.isStateOrUtilityModifier(null));
    }

    @Test
    public void isStateOrUtilityModifier_allowsContentTokens() {
        Assert.assertFalse(SemanticNameExtractor.isStateOrUtilityModifier("username"));
        Assert.assertFalse(SemanticNameExtractor.isStateOrUtilityModifier("login"));
        Assert.assertFalse(SemanticNameExtractor.isStateOrUtilityModifier("checkout"));
        Assert.assertFalse(SemanticNameExtractor.isStateOrUtilityModifier("nav-link"));
    }

    @Test
    public void extractDistinctiveTokens_filtersGenericTerms() {
        java.util.Set<String> tokens = SemanticNameExtractor.extractDistinctiveTokens(org.openqa.selenium.By.id("radio-option-firefox"));
        Assert.assertTrue(tokens.contains("firefox"), "Must extract 'firefox'");
        Assert.assertFalse(tokens.contains("radio"), "Must filter generic 'radio'");
        Assert.assertFalse(tokens.contains("option"), "Must filter generic 'option'");
    }

    @Test
    public void candidateMatchesDistinctiveTokens_matchesCorrectCandidate() {
        java.util.Set<String> tokens = java.util.Set.of("firefox");
        java.util.Map<String, Object> firefoxAttrs = java.util.Map.of("id", "radio-firefox", "text", "Firefox");
        java.util.Map<String, Object> chromeAttrs = java.util.Map.of("id", "radio-chrome", "text", "Chrome");

        Assert.assertTrue(SemanticNameExtractor.candidateMatchesDistinctiveTokens(firefoxAttrs, null, tokens));
        Assert.assertFalse(SemanticNameExtractor.candidateMatchesDistinctiveTokens(chromeAttrs, null, tokens));
    }

    @Test
    public void extractDistinctiveTokens_fromXPath_filtersAttributesAndExtractsKeyword() {
        java.util.Set<String> tokens = SemanticNameExtractor.extractDistinctiveTokens(
                org.openqa.selenium.By.xpath("//input[@id='radio-option-firefox']"));
        Assert.assertTrue(tokens.contains("firefox"), "Must extract 'firefox'");
        Assert.assertFalse(tokens.contains("input"), "Must filter generic 'input'");
        Assert.assertFalse(tokens.contains("id"), "Must filter generic 'id'");
        Assert.assertFalse(tokens.contains("radio"), "Must filter generic 'radio'");
        Assert.assertFalse(tokens.contains("option"), "Must filter generic 'option'");
    }

    @Test
    public void extractDistinctiveTokens_fromCss_extractsKeyword() {
        java.util.Set<String> tokens = SemanticNameExtractor.extractDistinctiveTokens(
                org.openqa.selenium.By.cssSelector("input[name='browser-safari']"));
        Assert.assertTrue(tokens.contains("safari"), "Must extract 'safari'");
        Assert.assertFalse(tokens.contains("name"), "Must filter generic 'name'");
        Assert.assertFalse(tokens.contains("input"), "Must filter generic 'input'");
    }

    @Test
    public void candidateMatchesDistinctiveTokens_matchesClassAttribute() {
        java.util.Set<String> tokens = java.util.Set.of("firefox");
        java.util.Map<String, Object> attrs = java.util.Map.of("class", "custom-control radio-firefox active");
        Assert.assertTrue(SemanticNameExtractor.candidateMatchesDistinctiveTokens(attrs, null, tokens));
    }

    @Test
    public void extractDistinctiveTokens_fromSubmitButton_extractsSubmit() {
        java.util.Set<String> tokens = SemanticNameExtractor.extractDistinctiveTokens(
                org.openqa.selenium.By.id("submit-form-button"));
        Assert.assertTrue(tokens.contains("submit"), "Must extract 'submit' as distinctive action keyword");
        Assert.assertFalse(tokens.contains("form"), "Must filter generic 'form'");
        Assert.assertFalse(tokens.contains("button"), "Must filter generic 'button'");
    }

    @Test
    public void extractDistinctiveTokens_fromComplexXPathWithMixedQuotes_filtersSyntaxAndExtractsDistinctive() {
        org.openqa.selenium.By complexXpath = org.openqa.selenium.By.xpath(
                "//div[@data-test='user-profile']//button[@type='submit' and contains(normalize-space(), \"Save Profile\")]");
        java.util.Set<String> tokens = SemanticNameExtractor.extractDistinctiveTokens(complexXpath);
        Assert.assertTrue(tokens.contains("profile"), "Must extract 'profile'");
        Assert.assertTrue(tokens.contains("save"), "Must extract 'save'");
        Assert.assertTrue(tokens.contains("user"), "Must extract 'user'");
        Assert.assertTrue(tokens.contains("submit"), "Must preserve distinctive action keyword 'submit'");
        Assert.assertFalse(tokens.contains("div"), "Must filter generic tag 'div'");
        Assert.assertFalse(tokens.contains("button"), "Must filter generic tag 'button'");
        Assert.assertFalse(tokens.contains("normalize"), "Must filter XPath function 'normalize'");
        Assert.assertFalse(tokens.contains("space"), "Must filter XPath function 'space'");
    }

    @Test
    public void candidateMatchesDistinctiveTokens_shortTokensRequireWordBoundary() {
        java.util.Set<String> tokens = java.util.Set.of("cat");
        // "location" contains "cat" as substring, but is NOT the word "cat"
        java.util.Map<String, Object> falseAttrs = java.util.Map.of("name", "location");
        Assert.assertFalse(SemanticNameExtractor.candidateMatchesDistinctiveTokens(falseAttrs, null, tokens),
                "Short token 'cat' should not match inside 'location'");

        // "cat-selector" or "cat" has word boundary
        java.util.Map<String, Object> trueAttrs = java.util.Map.of("name", "user-cat-option");
        Assert.assertTrue(SemanticNameExtractor.candidateMatchesDistinctiveTokens(trueAttrs, null, tokens),
                "Short token 'cat' should match with delimiter boundary in 'user-cat-option'");
    }

    @Test
    public void candidateMatchesDistinctiveTokens_onWebElementDirectly_checksAllAttributes() {
        org.openqa.selenium.WebElement mockEl = org.mockito.Mockito.mock(org.openqa.selenium.WebElement.class);
        org.mockito.Mockito.when(mockEl.getAttribute("data-test")).thenReturn("browser-edge-choice");

        java.util.Set<String> tokens = java.util.Set.of("edge");
        Assert.assertTrue(SemanticNameExtractor.candidateMatchesDistinctiveTokens(null, mockEl, tokens),
                "candidateMatchesDistinctiveTokens must check data-test attribute on WebElement directly");
    }
}
