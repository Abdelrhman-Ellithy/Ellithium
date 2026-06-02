package Ellithium.core.ai;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static Ellithium.core.ai.SemanticLocatorResolver.categorizeAction;
import static Ellithium.core.ai.SemanticLocatorResolver.xpathLiteral;

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
    public void categorizeAction_sendData_isInput() {
        Assert.assertEquals(categorizeAction("sendData"), SemanticLocatorResolver.ElementCategory.INPUT);
    }

    @Test
    public void categorizeAction_clearElement_isInput() {
        Assert.assertEquals(categorizeAction("clearElement"), SemanticLocatorResolver.ElementCategory.INPUT);
    }

    @Test
    public void categorizeAction_uploadFile_isInput() {
        Assert.assertEquals(categorizeAction("uploadFile"), SemanticLocatorResolver.ElementCategory.INPUT);
    }

    @Test
    public void categorizeAction_clickOnElement_isClickable() {
        Assert.assertEquals(categorizeAction("clickOnElement"), SemanticLocatorResolver.ElementCategory.CLICKABLE);
    }

    @Test
    public void categorizeAction_doubleClick_isClickable() {
        Assert.assertEquals(categorizeAction("doubleClick"), SemanticLocatorResolver.ElementCategory.CLICKABLE);
    }

    @Test
    public void categorizeAction_selectDropdownByText_isSelect() {
        Assert.assertEquals(categorizeAction("selectDropdownByText"), SemanticLocatorResolver.ElementCategory.SELECT);
    }

    @Test
    public void categorizeAction_getText_isReadable() {
        Assert.assertEquals(categorizeAction("getText"), SemanticLocatorResolver.ElementCategory.READABLE);
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
}
