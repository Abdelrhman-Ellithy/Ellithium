package Ellithium.core.ai.scoring;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SemanticQueryBuilderTest {

    // ── deCamelCase ───────────────────────────────────────────────────────────

    @Test
    public void deCamelCase_splitsLowerToUpperBoundary() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("loginButton"), "login button");
    }

    @Test
    public void deCamelCase_splitsUpperToUpperLowerBoundary() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("HTMLParser"), "html parser");
    }

    @Test
    public void deCamelCase_splitsOnHyphen() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("user-email"), "user email");
    }

    @Test
    public void deCamelCase_splitsOnUnderscore() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("user_name"), "user name");
    }

    @Test
    public void deCamelCase_multiWord() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("UserEmailField"), "user email field");
    }

    @Test
    public void deCamelCase_singleWord_isLowercased() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("Login"), "login");
    }

    @Test
    public void deCamelCase_alreadyLowercase_unchanged() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("email"), "email");
    }

    @Test
    public void deCamelCase_null_returnsEmpty() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase(null), "");
    }

    @Test
    public void deCamelCase_blank_returnsEmpty() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("   "), "");
    }

    @Test
    public void deCamelCase_mixedDelimiters() {
        Assert.assertEquals(SemanticQueryBuilder.deCamelCase("first_nameField"), "first name field");
    }

    // ── extractLocatorValue ───────────────────────────────────────────────────

    @Test
    public void extractLocatorValue_stripsById_prefix() {
        Assert.assertEquals(SemanticQueryBuilder.extractLocatorValue("By.id: login-form"), "login-form");
    }

    @Test
    public void extractLocatorValue_stripsByCssSelector_prefix() {
        Assert.assertEquals(SemanticQueryBuilder.extractLocatorValue("By.cssSelector: .submit-btn"), ".submit-btn");
    }

    @Test
    public void extractLocatorValue_stripsByXpath_prefix() {
        String xp = "//input[@type='email']";
        Assert.assertEquals(SemanticQueryBuilder.extractLocatorValue("By.xpath: " + xp), xp);
    }

    @Test
    public void extractLocatorValue_plainValue_returnedAsIs() {
        Assert.assertEquals(SemanticQueryBuilder.extractLocatorValue("user-email"), "user-email");
    }

    @Test
    public void extractLocatorValue_null_returnsEmpty() {
        Assert.assertEquals(SemanticQueryBuilder.extractLocatorValue(null), "");
    }

    // ── stripMethodPrefixV2 ───────────────────────────────────────────────────

    @Test
    public void stripMethodPrefixV2_removesClickPrefix() {
        Assert.assertEquals(SemanticQueryBuilder.stripMethodPrefixV2("clickLoginBtn"), "LoginBtn");
    }

    @Test
    public void stripMethodPrefixV2_removesGetPrefix() {
        Assert.assertEquals(SemanticQueryBuilder.stripMethodPrefixV2("getUserEmail"), "UserEmail");
    }

    @Test
    public void stripMethodPrefixV2_removesSetPrefix() {
        Assert.assertEquals(SemanticQueryBuilder.stripMethodPrefixV2("setPassword"), "Password");
    }

    @Test
    public void stripMethodPrefixV2_noMatchReturnsUnchanged() {
        Assert.assertEquals(SemanticQueryBuilder.stripMethodPrefixV2("loginUser"), "loginUser");
    }

    @Test
    public void stripMethodPrefixV2_noUpperAfterPrefix_returnsUnchanged() {
        Assert.assertEquals(SemanticQueryBuilder.stripMethodPrefixV2("getpassword"), "getpassword");
    }

    @Test
    public void stripMethodPrefixV2_null_returnsEmpty() {
        Assert.assertEquals(SemanticQueryBuilder.stripMethodPrefixV2(null), "");
    }

    @Test
    public void stripMethodPrefixV2_prefixEqualsName_returnsUnchanged() {
        Assert.assertEquals(SemanticQueryBuilder.stripMethodPrefixV2("click"), "click");
    }

    // ── expandAction ─────────────────────────────────────────────────────────

    @Test
    public void expandAction_clickable_containsClick() {
        String result = SemanticQueryBuilder.expandAction("click");
        Assert.assertTrue(result.contains("click"), "click action must expand to click-related terms: " + result);
    }

    @Test
    public void expandAction_readable_containsRead() {
        String result = SemanticQueryBuilder.expandAction("getText");
        Assert.assertTrue(result.contains("read") || result.contains("text"),
                "getText must expand to readable terms: " + result);
    }

    @Test
    public void expandAction_input_containsType() {
        String result = SemanticQueryBuilder.expandAction("sendData");
        Assert.assertTrue(result.contains("type") || result.contains("input"),
                "sendData must expand to input terms: " + result);
    }

    @Test
    public void expandAction_null_returnsEmpty() {
        Assert.assertEquals(SemanticQueryBuilder.expandAction(null), "");
    }

    @Test
    public void expandAction_blank_returnsEmpty() {
        Assert.assertEquals(SemanticQueryBuilder.expandAction("  "), "");
    }

    // ── build ─────────────────────────────────────────────────────────────────

    @Test
    public void build_allNull_returnsEmpty() {
        String result = SemanticQueryBuilder.build(null, null, null, null,
                null, null, null, null, null, null);
        Assert.assertTrue(result.isEmpty(), "All-null input must produce empty query");
    }

    @Test
    public void build_withLocatorValue_includesIt() {
        String result = SemanticQueryBuilder.build(null, "By.id: login-btn", null, null,
                null, null, null, null, null, null);
        Assert.assertTrue(result.contains("login"), "Query must include locator token: " + result);
    }

    @Test
    public void build_withActionAndLocator_bothPresent() {
        String result = SemanticQueryBuilder.build("click", "By.id: submit", null, null,
                null, null, null, null, null, null);
        Assert.assertFalse(result.isBlank(), "Must produce non-blank query");
        Assert.assertTrue(result.contains("submit"), "Must include submit locator: " + result);
        Assert.assertTrue(result.contains("click") || result.contains("press"),
                "Must include click expansion: " + result);
    }

    @Test
    public void build_withAriaLabel_included() {
        String result = SemanticQueryBuilder.build(null, null, null, null,
                null, null, "Sign In Button", null, null, null);
        Assert.assertTrue(result.contains("sign in button"), "AriaLabel must appear lowercased: " + result);
    }

    @Test
    public void build_withId_deCameled() {
        String result = SemanticQueryBuilder.build(null, null, null, null,
                null, "loginFormInput", null, null, null, null);
        Assert.assertTrue(result.contains("login"), "Id must be deCameled: " + result);
    }

    @Test
    public void build_duplicateTokensRemovedByDedup() {
        String result = SemanticQueryBuilder.build(null, "By.id: login", null, null,
                null, "login", null, null, null, null);
        long loginCount = java.util.Arrays.stream(result.split("\\s+"))
                .filter("login"::equals).count();
        Assert.assertEquals(loginCount, 1L, "Duplicate token 'login' must appear only once: " + result);
    }

    @Test
    public void build_withMethodName_prefixStripped() {
        String result = SemanticQueryBuilder.build(null, null, "clickSubmitButton", null,
                null, null, null, null, null, null);
        Assert.assertTrue(result.contains("submit") || result.contains("button"),
                "Method name must contribute stripped tokens: " + result);
    }

    @Test
    public void build_withResourceId_tailExtracted() {
        String result = SemanticQueryBuilder.build(null, null, null, null,
                null, null, null, null, null, null,
                null, null, "com.app:id/loginButton", null, null);
        Assert.assertTrue(result.contains("login") || result.contains("button"),
                "Resource ID tail must be deCameled: " + result);
    }

    @Test
    public void build_textTruncatedAt100Chars() {
        String longText = "a".repeat(150);
        String result = SemanticQueryBuilder.build(null, null, null, null,
                longText, null, null, null, null, null);
        Assert.assertFalse(result.contains("a".repeat(101)),
                "Text must be truncated to 100 chars: " + result);
    }
}
