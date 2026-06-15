package Ellithium.core.ai.codegen;

import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import Ellithium.core.ai.codegen.LocatorCandidate;
import Ellithium.core.ai.codegen.UniqueLocatorGenerator;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MobileLocatorTest {

    private static LocatorCandidate byTier(List<LocatorCandidate> cs, String tier) {
        return cs.stream().filter(c -> c.tier().equals(tier)).findFirst().orElse(null);
    }

    @Test
    public void emitsAppiumLocatorsFromNativeAttributes() {
        WebElement el = mock(WebElement.class);
        when(el.getAttribute("resource-id")).thenReturn("com.app:id/loginBtn");
        when(el.getAttribute("accessibility-id")).thenReturn(null);
        when(el.getAttribute("content-desc")).thenReturn("Login");
        when(el.getAttribute("name")).thenReturn(null);
        when(el.getAttribute("text")).thenReturn(null);

        List<LocatorCandidate> ranked = UniqueLocatorGenerator.rankMobile(el, by -> 1);

        LocatorCandidate acc = byTier(ranked, "accessibility-id");
        Assert.assertNotNull(acc, "content-desc must yield an accessibility-id candidate");
        Assert.assertEquals(acc.javaExpression(), "AppiumBy.accessibilityId(\"Login\")");

        LocatorCandidate res = byTier(ranked, "resource-id");
        Assert.assertNotNull(res, "resource-id must yield a candidate");
    }

    @Test
    public void fallsBackToXpathWhenNoStableMobileIds() {
        WebElement el = mock(WebElement.class);
        when(el.getAttribute("resource-id")).thenReturn(null);
        when(el.getAttribute("accessibility-id")).thenReturn(null);
        when(el.getAttribute("content-desc")).thenReturn(null);
        when(el.getAttribute("name")).thenReturn(null);
        when(el.getAttribute("text")).thenReturn("Login");
        when(el.getAttribute("class")).thenReturn("android.widget.Button");

        List<LocatorCandidate> ranked = UniqueLocatorGenerator.rankMobile(el, by -> 1);

        LocatorCandidate xp = byTier(ranked, "xpath-text");
        Assert.assertNotNull(xp, "a text-only native element must still get an XPath candidate");
        Assert.assertEquals(xp.javaExpression(), "By.xpath(\"//android.widget.Button[@text='Login']\")");
    }
}
