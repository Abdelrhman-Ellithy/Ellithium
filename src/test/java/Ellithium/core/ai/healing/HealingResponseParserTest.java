package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.HealingResult;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;

public class HealingResponseParserTest {

    // ── extractValue ──────────────────────────────────────────────────────────

    @Test
    public void extractValue_singleQuotedValue() {
        Assert.assertEquals(HealingResponseParser.extractValue("By.id(\"login-btn\")"), "login-btn");
    }

    @Test
    public void extractValue_cssSelector() {
        Assert.assertEquals(HealingResponseParser.extractValue("By.cssSelector(\".submit\")"), ".submit");
    }

    @Test
    public void extractValue_xpath() {
        Assert.assertEquals(HealingResponseParser.extractValue("By.xpath(\"//input[@id='x']\")"), "//input[@id='x']");
    }

    // ── parseByFromExpression ─────────────────────────────────────────────────

    @Test
    public void parseByFromExpression_byId() {
        By by = HealingResponseParser.parseByFromExpression("By.id(\"login\")");
        Assert.assertNotNull(by);
        Assert.assertTrue(by.toString().contains("login"), "Must resolve to By.id: " + by);
    }

    @Test
    public void parseByFromExpression_byCssSelector() {
        By by = HealingResponseParser.parseByFromExpression("By.cssSelector(\".btn-primary\")");
        Assert.assertNotNull(by);
        Assert.assertTrue(by.toString().contains("btn-primary"), "Must resolve to cssSelector: " + by);
    }

    @Test
    public void parseByFromExpression_byXpath() {
        By by = HealingResponseParser.parseByFromExpression("By.xpath(\"//button[@type='submit']\")");
        Assert.assertNotNull(by);
        Assert.assertTrue(by.toString().contains("//button"), "Must resolve to xpath: " + by);
    }

    @Test
    public void parseByFromExpression_byName() {
        By by = HealingResponseParser.parseByFromExpression("By.name(\"username\")");
        Assert.assertNotNull(by);
        Assert.assertTrue(by.toString().contains("username"));
    }

    @Test
    public void parseByFromExpression_byClassName() {
        By by = HealingResponseParser.parseByFromExpression("By.className(\"submit-btn\")");
        Assert.assertNotNull(by);
    }

    @Test
    public void parseByFromExpression_byLinkText() {
        By by = HealingResponseParser.parseByFromExpression("By.linkText(\"Click Here\")");
        Assert.assertNotNull(by);
    }

    @Test
    public void parseByFromExpression_byPartialLinkText() {
        By by = HealingResponseParser.parseByFromExpression("By.partialLinkText(\"Click\")");
        Assert.assertNotNull(by);
    }

    @Test
    public void parseByFromExpression_byTagName() {
        By by = HealingResponseParser.parseByFromExpression("By.tagName(\"input\")");
        Assert.assertNotNull(by);
    }

    @Test
    public void parseByFromExpression_unknownExpression_returnsNull() {
        By by = HealingResponseParser.parseByFromExpression("By.unknown(\"value\")");
        Assert.assertNull(by, "Unknown strategy must return null");
    }

    // ── isStableLocatorStrategy ───────────────────────────────────────────────

    @Test
    public void isStableLocatorStrategy_byId_isStable() {
        Assert.assertTrue(HealingResponseParser.isStableLocatorStrategy(By.id("login-btn")));
    }

    @Test
    public void isStableLocatorStrategy_byName_isStable() {
        Assert.assertTrue(HealingResponseParser.isStableLocatorStrategy(By.name("username")));
    }

    @Test
    public void isStableLocatorStrategy_cssWithDataTestId_isStable() {
        Assert.assertTrue(HealingResponseParser.isStableLocatorStrategy(
                By.cssSelector("[data-testid='submit']")));
    }

    @Test
    public void isStableLocatorStrategy_cssWithAriaLabel_isStable() {
        Assert.assertTrue(HealingResponseParser.isStableLocatorStrategy(
                By.cssSelector("[aria-label='Submit']")));
    }

    @Test
    public void isStableLocatorStrategy_cssWithIdHash_isStable() {
        Assert.assertTrue(HealingResponseParser.isStableLocatorStrategy(
                By.cssSelector("#main-login-btn")));
    }

    @Test
    public void isStableLocatorStrategy_cssDynamicClass_isUnstable() {
        Assert.assertFalse(HealingResponseParser.isStableLocatorStrategy(
                By.cssSelector(".dynamic-generated-class")));
    }

    @Test
    public void isStableLocatorStrategy_xpath_isUnstable() {
        Assert.assertFalse(HealingResponseParser.isStableLocatorStrategy(
                By.xpath("//div[3]/button")));
    }

    @Test
    public void isStableLocatorStrategy_null_returnsFalse() {
        Assert.assertFalse(HealingResponseParser.isStableLocatorStrategy(null));
    }

    // ── parseMultiCandidateResponse ───────────────────────────────────────────

    @Test
    public void parseMultiCandidateResponse_nullInput_returnsEmptyList() {
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse(null);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void parseMultiCandidateResponse_emptyString_returnsEmptyList() {
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse("   ");
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void parseMultiCandidateResponse_invalidJson_returnsEmptyList() {
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse("not json at all");
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void parseMultiCandidateResponse_singleObject_withLocator() {
        String json = "{\"locator\":\"By.id(\\\"login\\\")\",\"confidence\":0.9,\"reasoning\":\"id match\"}";
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse(json);
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getNewLocatorExpression(), "By.id(\"login\")");
        Assert.assertEquals(results.get(0).getConfidence(), 0.9, 1e-9);
    }

    @Test
    public void parseMultiCandidateResponse_candidatesArray_parsesAll() {
        String json = "{\"candidates\":[" +
                "{\"locator\":\"By.id(\\\"btn1\\\")\",\"confidence\":0.9}," +
                "{\"locator\":\"By.cssSelector(\\\".cls\\\")\",\"confidence\":0.7}" +
                "]}";
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse(json);
        Assert.assertEquals(results.size(), 2);
        Assert.assertEquals(results.get(0).getNewLocatorExpression(), "By.id(\"btn1\")");
        Assert.assertEquals(results.get(0).getConfidence(), 0.9, 1e-9);
        Assert.assertEquals(results.get(1).getConfidence(), 0.7, 1e-9);
    }

    @Test
    public void parseMultiCandidateResponse_jsonArray_parsesAll() {
        String json = "[" +
                "{\"locator\":\"By.id(\\\"a\\\")\",\"confidence\":0.8}," +
                "{\"locator\":\"By.id(\\\"b\\\")\",\"confidence\":0.6}" +
                "]";
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse(json);
        Assert.assertEquals(results.size(), 2);
    }

    @Test
    public void parseMultiCandidateResponse_markdownCodeBlock_strippedBeforeParsing() {
        String json = "```json\n{\"locator\":\"By.id(\\\"x\\\")\",\"confidence\":0.9}\n```";
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse(json);
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getNewLocatorExpression(), "By.id(\"x\")");
    }

    @Test
    public void parseMultiCandidateResponse_missingConfidence_defaultsToHalf() {
        String json = "{\"locator\":\"By.id(\\\"x\\\")\"}";
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse(json);
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getConfidence(), 0.5, 1e-9,
                "Missing confidence must default to 0.5");
    }

    @Test
    public void parseMultiCandidateResponse_missingReasoning_defaultsToEmpty() {
        String json = "{\"locator\":\"By.id(\\\"x\\\")\",\"confidence\":0.9}";
        List<HealingResult> results = HealingResponseParser.parseMultiCandidateResponse(json);
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getReasoning(), "");
    }

    @Test
    public void parseSingleCandidate_missingLocatorField_returnsNull() {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        obj.addProperty("confidence", 0.9);
        Assert.assertNull(HealingResponseParser.parseSingleCandidate(obj),
                "Missing 'locator' field must return null");
    }
}
