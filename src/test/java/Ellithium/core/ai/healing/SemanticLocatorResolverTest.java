package Ellithium.core.ai.healing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static Ellithium.core.ai.healing.SemanticLocatorResolver.categorizeAction;
import static Ellithium.core.ai.healing.SemanticLocatorResolver.xpathLiteral;

public class SemanticLocatorResolverTest {

    @Test
    public void xpathLiteral_null_returnsEmptyQuotes() {
        Assert.assertEquals(xpathLiteral(null), "''");
    }

    @Test
    public void xpathLiteral_noQuotes_wrapsInSingleQuotes() {
        Assert.assertEquals(xpathLiteral("hello"), "'hello'");
    }

    @Test
    public void xpathLiteral_containsSingleQuote_wrapsInDoubleQuotes() {
        String result = xpathLiteral("it's");
        Assert.assertEquals(result, "\"it's\"");
    }

    @Test
    public void xpathLiteral_containsDoubleQuote_wrapsInSingleQuotes() {
        String result = xpathLiteral("say \"hi\"");
        Assert.assertEquals(result, "'say \"hi\"'");
    }

    @Test
    public void xpathLiteral_bothQuotes_usesConcatForm() {
        String result = xpathLiteral("it's \"fine\"");
        Assert.assertTrue(result.startsWith("concat("), result);
        Assert.assertTrue(result.contains("\"'\""), "must embed single-quote via double-quoted literal");
    }

    @Test
    public void xpathLiteral_empty_returnsSingleQuotedEmpty() {
        Assert.assertEquals(xpathLiteral(""), "''");
    }

    @Test
    public void categorizeAction_sendData_isType() {
        Assert.assertEquals(categorizeAction("sendData"), SemanticLocatorResolver.ElementCategory.TYPE);
    }

    @Test
    public void categorizeAction_clearElement_isType() {
        Assert.assertEquals(categorizeAction("clearElement"), SemanticLocatorResolver.ElementCategory.TYPE);
    }

    @Test
    public void categorizeAction_uploadFile_isType() {
        Assert.assertEquals(categorizeAction("uploadFile"), SemanticLocatorResolver.ElementCategory.TYPE);
    }

    @Test
    public void categorizeAction_clickOnElement_isClick() {
        Assert.assertEquals(categorizeAction("clickOnElement"), SemanticLocatorResolver.ElementCategory.CLICK);
    }

    @Test
    public void categorizeAction_doubleClick_isClick() {
        Assert.assertEquals(categorizeAction("doubleClick"), SemanticLocatorResolver.ElementCategory.CLICK);
    }

    @Test
    public void categorizeAction_selectDropdownByText_isSelect() {
        Assert.assertEquals(categorizeAction("selectDropdownByText"), SemanticLocatorResolver.ElementCategory.SELECT);
    }

    @Test
    public void categorizeAction_getText_isRead() {
        Assert.assertEquals(categorizeAction("getText"), SemanticLocatorResolver.ElementCategory.READ);
    }

    @Test
    public void categorizeAction_isElementClickable_isCheck() {
        Assert.assertEquals(categorizeAction("isElementClickable"), SemanticLocatorResolver.ElementCategory.CHECK);
    }

    @Test
    public void categorizeAction_waitForElementToBeClickable_isWait() {
        Assert.assertEquals(categorizeAction("waitForElementToBeClickable"), SemanticLocatorResolver.ElementCategory.WAIT);
    }

    @Test
    public void categorizeAction_isElementSelected_isCheck() {
        Assert.assertEquals(categorizeAction("isElementSelected"), SemanticLocatorResolver.ElementCategory.CHECK);
    }

    @Test
    public void categorizeAction_isElementDisplayed_isCheck() {
        Assert.assertEquals(categorizeAction("isElementDisplayed"), SemanticLocatorResolver.ElementCategory.CHECK);
    }

    @Test
    public void categorizeAction_null_isAny() {
        Assert.assertEquals(categorizeAction(null), SemanticLocatorResolver.ElementCategory.ANY);
    }

    @Test
    public void categorizeAction_unknown_isAny() {
        Assert.assertEquals(categorizeAction("unknownAction"), SemanticLocatorResolver.ElementCategory.ANY);
    }

    @Test
    public void rebuildLocator_byId_createsById() throws Exception {
        By result = invokeRebuildLocator("By.id: loginBtn");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("loginBtn"));
    }

    @Test
    public void rebuildLocator_byCss_createsByCssSelector() throws Exception {
        By result = invokeRebuildLocator("By.cssSelector: #login");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("#login"));
    }

    @Test
    public void rebuildLocator_byName_createsByName() throws Exception {
        By result = invokeRebuildLocator("By.name: username");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("username"));
    }

    @Test
    public void rebuildLocator_byXpath_createsByXpath() throws Exception {
        By result = invokeRebuildLocator("By.xpath: //button[@id='submit']");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.toString().contains("//button"));
    }

    @Test
    public void rebuildLocator_blank_returnsNull() throws Exception {
        Assert.assertNull(invokeRebuildLocator(""));
    }

    @Test
    public void rebuildLocator_null_returnsNull() throws Exception {
        Assert.assertNull(invokeRebuildLocator(null));
    }

    @Test
    public void rebuildLocator_unknownFormat_returnsNull() throws Exception {
        Assert.assertNull(invokeRebuildLocator("By.something: value"));
    }

    @Test
    public void multiCaseXpath_emptyName_returnsSimpleTagXpath() throws Exception {
        String result = invokeMultiCaseXpath("input", "id", "", "//");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("input"), result);
    }

    @Test
    public void multiCaseXpath_normalName_includesAllCaseVariants() throws Exception {
        String result = invokeMultiCaseXpath("button", "aria-label", "Login", "//");
        Assert.assertTrue(result.contains("Login"), result);
        Assert.assertTrue(result.contains("login"), result);
        Assert.assertTrue(result.contains("LOGIN"), result);
    }

    @Test
    public void multiCaseXpath_nullName_doesNotThrow() throws Exception {
        String result = invokeMultiCaseXpath("input", "placeholder", null, "//");
        Assert.assertNotNull(result);
    }

    private static By invokeRebuildLocator(String locatorValue) throws Exception {
        Method m = SemanticLocatorResolver.class.getDeclaredMethod("rebuildLocator", String.class);
        m.setAccessible(true);
        return (By) m.invoke(null, locatorValue);
    }

    private static String invokeMultiCaseXpath(String tag, String attr, String name, String scopePrefix) throws Exception {
        Method m = SemanticLocatorResolver.class.getDeclaredMethod(
                "multiCaseXpath", String.class, String.class, String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, tag, attr, name, scopePrefix);
    }

    @Test
    public void scoreColdStart_stateModifierMatch_rejectedAsZero() {
        WebElement mockEl = Mockito.mock(WebElement.class);
        Mockito.when(mockEl.getTagName()).thenReturn("div");
        Mockito.when(mockEl.getDomAttribute("class")).thenReturn("o_last_breadcrumb_item");
        Mockito.when(mockEl.getText()).thenReturn("Quotations");

        double score = SemanticLocatorResolver.scoreColdStart(null, mockEl, ".nav-link.active",
                SemanticLocatorResolver.ElementCategory.CLICK, "class*='active'");
        Assert.assertEquals(score, 0.0, "State-modifier false candidate with no discriminative token overlap must score 0.0");
    }

    @Test
    public void scoreColdStart_validRename_accepted() {
        WebElement mockEl = Mockito.mock(WebElement.class);
        Mockito.when(mockEl.getTagName()).thenReturn("input");
        Mockito.when(mockEl.getDomAttribute("id")).thenReturn("userName");
        Mockito.when(mockEl.getDomAttribute("name")).thenReturn("username");

        double score = SemanticLocatorResolver.scoreColdStart(null, mockEl, "input#user_name",
                SemanticLocatorResolver.ElementCategory.TYPE, "input[id~='user_name']");
        Assert.assertTrue(score >= 0.70, "Legitimate cold-start candidate with token and tag overlap must clear threshold, got: " + score);
    }

    @Test
    public void testPickBest_AmbiguityMargin_KeywordTieBreak() {
        org.openqa.selenium.WebDriver driver = Mockito.mock(org.openqa.selenium.WebDriver.class);
        WebElement elChrome = Mockito.mock(WebElement.class);
        WebElement elFirefox = Mockito.mock(WebElement.class);

        Mockito.when(elChrome.isDisplayed()).thenReturn(true);
        Mockito.when(elFirefox.isDisplayed()).thenReturn(true);
        Mockito.when(elChrome.getTagName()).thenReturn("input");
        Mockito.when(elFirefox.getTagName()).thenReturn("input");
        Mockito.when(elChrome.getDomAttribute("id")).thenReturn("radio-chrome");
        Mockito.when(elFirefox.getDomAttribute("id")).thenReturn("radio-firefox");

        java.util.LinkedHashMap<WebElement, String> candidates = new java.util.LinkedHashMap<>();
        candidates.put(elChrome, "chrome-radio");
        candidates.put(elFirefox, "firefox-radio");

        SemanticLocatorResolver.Scored result = SemanticLocatorResolver.pickBest(
                driver, candidates, null, 0.30,
                "input[id='radio-option-firefox']",
                SemanticLocatorResolver.ElementCategory.CLICK);

        Assert.assertNotNull(result, "Should heal to distinctive keyword match");
        Assert.assertEquals(result.element(), elFirefox, "Tie-break must pick firefox candidate over chrome");
    }

    @Test
    public void testPickBest_GenuinelyAmbiguous_Abstains() {
        org.openqa.selenium.WebDriver driver = Mockito.mock(org.openqa.selenium.WebDriver.class);
        WebElement el1 = Mockito.mock(WebElement.class);
        WebElement el2 = Mockito.mock(WebElement.class);

        Mockito.when(el1.isDisplayed()).thenReturn(true);
        Mockito.when(el2.isDisplayed()).thenReturn(true);
        Mockito.when(el1.getTagName()).thenReturn("button");
        Mockito.when(el2.getTagName()).thenReturn("button");
        Mockito.when(el1.getDomAttribute("id")).thenReturn("btn-1");
        Mockito.when(el2.getDomAttribute("id")).thenReturn("btn-2");

        java.util.LinkedHashMap<WebElement, String> candidates = new java.util.LinkedHashMap<>();
        candidates.put(el1, "btn-one");
        candidates.put(el2, "btn-two");

        SemanticLocatorResolver.Scored result = SemanticLocatorResolver.pickBest(
                driver, candidates, null, 0.30,
                "button.btn-unknown",
                SemanticLocatorResolver.ElementCategory.CLICK);

        // When candidates have identical cold-start scores within ambiguity margin and no distinctive tokens match, abstain!
        Assert.assertNull(result, "Genuinely ambiguous candidates within margin must abstain to prevent false heals");
    }

    @Test
    public void testPickBest_BothMatchDistinctive_ConfirmsTop1() {
        org.openqa.selenium.WebDriver driver = Mockito.mock(org.openqa.selenium.WebDriver.class);
        WebElement el1 = Mockito.mock(WebElement.class);
        WebElement el2 = Mockito.mock(WebElement.class);

        Mockito.when(el1.isDisplayed()).thenReturn(true);
        Mockito.when(el2.isDisplayed()).thenReturn(true);
        Mockito.when(el1.getTagName()).thenReturn("label");
        Mockito.when(el2.getTagName()).thenReturn("input");
        Mockito.when(el1.getDomAttribute("id")).thenReturn("label-firefox");
        Mockito.when(el2.getDomAttribute("id")).thenReturn("radio-firefox");

        java.util.LinkedHashMap<WebElement, String> candidates = new java.util.LinkedHashMap<>();
        candidates.put(el1, "label-firefox");
        candidates.put(el2, "input-firefox");

        SemanticLocatorResolver.Scored result = SemanticLocatorResolver.pickBest(
                driver, candidates, null, 0.30,
                "input[id='radio-option-firefox']",
                SemanticLocatorResolver.ElementCategory.CLICK);

        Assert.assertNotNull(result, "When both candidates match distinctive keyword, top candidate must be confirmed");
        Assert.assertEquals(result.element(), el2);
    }
}
