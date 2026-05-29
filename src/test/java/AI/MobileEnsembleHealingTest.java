package ai;

import Ellithium.core.ai.AISelfHealer;
import Ellithium.core.ai.SemanticQueryBuilder;
import Ellithium.core.ai.models.ElementFingerprint;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Mobile (Appium) path coverage for the unified ensemble healer — verifies the framework heals
 * native mobile locators correctly without needing a live Appium session:
 * <ul>
 *   <li>{@link ElementFingerprint#reconstructLocator} prefers AppiumBy on native attributes;</li>
 *   <li>{@link ElementFingerprint#scoreSimilarity(java.util.Map)} scores mobile-primary attributes;</li>
 *   <li>{@link AISelfHealer#byToJavaExpression} emits a COMPILABLE {@code AppiumBy.*} source patch;</li>
 *   <li>{@link SemanticQueryBuilder} folds mobile attrs into the served query.</li>
 * </ul>
 */
public class MobileEnsembleHealingTest {

    private static WebElement mobileElement(String resourceId, String accessibilityId,
                                            String contentDesc, String className, String text) {
        WebElement el = mock(WebElement.class);
        lenient().when(el.getAttribute("resource-id")).thenReturn(resourceId);
        lenient().when(el.getAttribute("accessibility-id")).thenReturn(accessibilityId);
        lenient().when(el.getAttribute("content-desc")).thenReturn(contentDesc);
        lenient().when(el.getAttribute("class")).thenReturn(className);
        lenient().when(el.getTagName()).thenReturn(className);
        lenient().when(el.getText()).thenReturn(text);
        // Web attrs absent on native
        lenient().when(el.getAttribute("id")).thenReturn(null);
        lenient().when(el.getAttribute("name")).thenReturn(null);
        lenient().when(el.getAttribute("data-testid")).thenReturn(null);
        lenient().when(el.getAttribute("aria-label")).thenReturn(null);
        return el;
    }

    @Test
    public void reconstructLocatorPrefersAccessibilityIdOnNative() {
        WebElement el = mobileElement("com.app:id/loginBtn", "login_button", "Login", "android.widget.Button", "Login");
        By by = ElementFingerprint.reconstructLocator(el);
        Assert.assertTrue(by instanceof AppiumBy,
                "native element with accessibility-id must reconstruct to AppiumBy, got: " + by);
        Assert.assertTrue(by.toString().contains("login_button"),
                "reconstructed AppiumBy must carry the accessibility id: " + by);
    }

    @Test
    public void reconstructLocatorFallsBackToResourceIdWhenNoAccessibilityId() {
        WebElement el = mobileElement("com.app:id/loginBtn", null, null, "android.widget.Button", "Login");
        By by = ElementFingerprint.reconstructLocator(el);
        Assert.assertTrue(by instanceof AppiumBy,
                "native element with only resource-id must reconstruct to AppiumBy, got: " + by);
        Assert.assertTrue(by.toString().contains("com.app:id/loginBtn"), by.toString());
    }

    @Test
    public void scoreSimilarityRewardsMatchingMobileAttributes() {
        WebElement captured = mobileElement("com.app:id/loginBtn", "login_button", "Login",
                "android.widget.Button", "Login");
        ElementFingerprint fp = ElementFingerprint.capture(null, By.id("loginBtn"), captured);

        java.util.Map<String, Object> sameElem = new java.util.HashMap<>();
        sameElem.put("resource-id", "com.app:id/loginBtn");
        sameElem.put("accessibility-id", "login_button");
        sameElem.put("content-desc", "Login");
        sameElem.put("tag", "android.widget.Button");

        java.util.Map<String, Object> differentElem = new java.util.HashMap<>();
        differentElem.put("resource-id", "com.app:id/cancelBtn");
        differentElem.put("accessibility-id", "cancel_button");
        differentElem.put("content-desc", "Cancel");
        differentElem.put("tag", "android.widget.Button");

        double same = fp.scoreSimilarity(sameElem);
        double diff = fp.scoreSimilarity(differentElem);
        Assert.assertTrue(same > diff, "matching mobile element must outscore a different one: "
                + same + " vs " + diff);
        Assert.assertTrue(same > 0.8, "exact mobile attribute match should score high, got " + same);
    }

    @Test
    public void byToJavaExpressionEmitsCompilableAppiumBy() {
        Assert.assertEquals(
                AISelfHealer.byToJavaExpression(AppiumBy.accessibilityId("login_button")),
                "AppiumBy.accessibilityId(\"login_button\")",
                "AppiumBy heal must serialize to a compilable AppiumBy.* expression, never By.*");
    }

    @Test
    public void byToJavaExpressionStillHandlesPlainBy() {
        Assert.assertEquals(
                AISelfHealer.byToJavaExpression(By.id("username")),
                "By.id(\"username\")");
    }

    @Test
    public void queryBuilderFoldsMobileAttributesIntoQuery() {
        String query = SemanticQueryBuilder.build("clickOnElement", "By.id: loginBtn", "clickLogin",
                null, null, null, null, null, null, null,
                "com.app:id/loginBtn", "login_button", "Login");
        Assert.assertTrue(query.contains("login"), "query must include mobile-derived tokens: " + query);
    }
}
