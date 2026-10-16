package Ellithium.core.ai.dom;

import org.testng.Assert;
import org.testng.annotations.Test;

public class InteractiveElementsTest {

    @Test
    public void testIsClickLikeAction_positiveCases() {
        Assert.assertTrue(InteractiveElements.isClickLikeAction("clickOnElement"));
        Assert.assertTrue(InteractiveElements.isClickLikeAction("clickOnMultipleElements"));
        Assert.assertTrue(InteractiveElements.isClickLikeAction("tap"));
        Assert.assertTrue(InteractiveElements.isClickLikeAction("pressKey"));
        Assert.assertTrue(InteractiveElements.isClickLikeAction("hoverOverElement"));
        Assert.assertTrue(InteractiveElements.isClickLikeAction("doubleClick"));
        Assert.assertTrue(InteractiveElements.isClickLikeAction("rightClick"));
    }

    @Test
    public void testIsClickLikeAction_negativeAndNullCases() {
        Assert.assertFalse(InteractiveElements.isClickLikeAction(null));
        Assert.assertFalse(InteractiveElements.isClickLikeAction("unknown"));
        Assert.assertFalse(InteractiveElements.isClickLikeAction("sendData"));
        Assert.assertFalse(InteractiveElements.isClickLikeAction("getText"));
        Assert.assertFalse(InteractiveElements.isClickLikeAction(""));
    }

    @Test
    public void testIsPlainClick() {
        Assert.assertTrue(InteractiveElements.isPlainClick("clickOnElement"));
        Assert.assertTrue(InteractiveElements.isPlainClick("clickOnMultipleElements"));
        Assert.assertFalse(InteractiveElements.isPlainClick("javascriptClick"));
        Assert.assertFalse(InteractiveElements.isPlainClick("doubleClick"));
        Assert.assertFalse(InteractiveElements.isPlainClick("rightClick"));
        Assert.assertFalse(InteractiveElements.isPlainClick(null));
        Assert.assertFalse(InteractiveElements.isPlainClick("sendData"));
    }

    @Test
    public void testIsTextInputAction_positiveCases() {
        Assert.assertTrue(InteractiveElements.isTextInputAction("sendData"));
        Assert.assertTrue(InteractiveElements.isTextInputAction("sendKeys"));
        Assert.assertTrue(InteractiveElements.isTextInputAction("clearElement"));
        Assert.assertTrue(InteractiveElements.isTextInputAction("setText"));
        Assert.assertTrue(InteractiveElements.isTextInputAction("type"));
        Assert.assertTrue(InteractiveElements.isTextInputAction("uploadFile"));
        Assert.assertTrue(InteractiveElements.isTextInputAction("fillForm"));
    }

    @Test
    public void testIsTextInputAction_negativeCases() {
        Assert.assertFalse(InteractiveElements.isTextInputAction(null));
        Assert.assertFalse(InteractiveElements.isTextInputAction("unknown"));
        Assert.assertFalse(InteractiveElements.isTextInputAction("getText"));
        Assert.assertFalse(InteractiveElements.isTextInputAction("isElementDisplayed"));
        Assert.assertFalse(InteractiveElements.isTextInputAction("waitForElement"));
        Assert.assertFalse(InteractiveElements.isTextInputAction("readTable"));
        Assert.assertFalse(InteractiveElements.isTextInputAction("clickOnElement"));
    }

    @Test
    public void testConstantsIntegrity() {
        Assert.assertTrue(InteractiveElements.TAGS.contains("button"));
        Assert.assertTrue(InteractiveElements.TAGS.contains("input"));
        Assert.assertTrue(InteractiveElements.TAGS.contains("label"));

        Assert.assertTrue(InteractiveElements.ROLES.contains("button"));
        Assert.assertTrue(InteractiveElements.ROLES.contains("checkbox"));
        Assert.assertTrue(InteractiveElements.ROLES.contains("radio"));

        Assert.assertTrue(InteractiveElements.TEXT_INPUT_TAGS.contains("input"));
        Assert.assertTrue(InteractiveElements.TEXT_INPUT_TAGS.contains("textarea"));

        Assert.assertTrue(InteractiveElements.TEXT_INPUT_ROLES.contains("textbox"));
        Assert.assertTrue(InteractiveElements.TEXT_INPUT_ROLES.contains("searchbox"));

        Assert.assertNotNull(InteractiveElements.DESCENDANT_CSS);
        Assert.assertNotNull(InteractiveElements.ANCESTOR_XPATH);
        Assert.assertTrue(InteractiveElements.INNER_SELECTORS.length > 0);
        Assert.assertTrue(InteractiveElements.INNER_TEXT_SELECTORS.length > 0);
    }
}
