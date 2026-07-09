package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

/**
 * {@code normalizeLocator} corrects an XPath expression wrapped in the wrong {@code By} strategy —
 * a real-world refactor typo (e.g. {@code By.id("//button[...]")} instead of {@code By.xpath(...)})
 * that the healing cascade cannot fix on its own: the XPath punctuation tokenizes into garbage
 * mutation/query candidates instead of a meaningful identifier.
 */
public class BaseActionsNormalizeLocatorTest {

    private static class Probe extends BaseActions<WebDriver> {
        Probe() { super(mock(WebDriver.class)); }
        By normalize(By locator) { return normalizeLocator(locator); }
    }

    @Test
    public void xpathWrappedInById_isCorrectedToXpath() {
        By fixed = new Probe().normalize(By.id("//button[normalize-space(.)='Login' and @type='submit']"));
        Assert.assertEquals(fixed, By.xpath("//button[normalize-space(.)='Login' and @type='submit']"));
    }

    @Test
    public void xpathWrappedInByName_isCorrectedToXpath() {
        By fixed = new Probe().normalize(By.name("//input[@type='email']"));
        Assert.assertEquals(fixed, By.xpath("//input[@type='email']"));
    }

    @Test
    public void xpathWrappedInByClassName_isCorrectedToXpath() {
        By fixed = new Probe().normalize(By.className("//div[@id='x']"));
        Assert.assertEquals(fixed, By.xpath("//div[@id='x']"));
    }

    @Test
    public void xpathWrappedInByCssSelector_stillCorrected() {
        By fixed = new Probe().normalize(By.cssSelector("//div[@id='x']"));
        Assert.assertEquals(fixed, By.xpath("//div[@id='x']"));
    }

    @Test
    public void relativeXpath_alsoCorrected() {
        By fixed = new Probe().normalize(By.id("./div[@class='x']"));
        Assert.assertEquals(fixed, By.xpath("./div[@class='x']"));
    }

    @Test
    public void genuineXpathLocator_isReturnedUnchanged() {
        By original = By.xpath("//button[normalize-space(.)='Login']");
        Assert.assertEquals(new Probe().normalize(original), original);
    }

    @Test
    public void cssClassWrappedInByXpath_isCorrectedToCssSelector() {
        By fixed = new Probe().normalize(By.xpath(".login-btn"));
        Assert.assertEquals(fixed, By.cssSelector(".login-btn"));
    }

    @Test
    public void cssIdWrappedInByXpath_isCorrectedToCssSelector() {
        By fixed = new Probe().normalize(By.xpath("#login-btn"));
        Assert.assertEquals(fixed, By.cssSelector("#login-btn"));
    }

    @Test
    public void cssAttributeSelectorWrappedInByXpath_isCorrectedToCssSelector() {
        By fixed = new Probe().normalize(By.xpath("[data-testid='login-btn']"));
        Assert.assertEquals(fixed, By.cssSelector("[data-testid='login-btn']"));
    }

    @Test
    public void cssTagClassWrappedInByXpath_isCorrectedToCssSelector() {
        By fixed = new Probe().normalize(By.xpath("button.login-btn"));
        Assert.assertEquals(fixed, By.cssSelector("button.login-btn"));
    }

    @Test
    public void xpathAttributePredicate_isNotMisdetectedAsCss() {
        By original = By.xpath("//button[@id='login-btn']");
        Assert.assertEquals(new Probe().normalize(original), original);
    }

    @Test
    public void bareXpathAttributePredicate_withAtSign_isNotMisdetectedAsCss() {
        By original = By.xpath("[@id='login-btn']");
        Assert.assertEquals(new Probe().normalize(original), original);
    }

    @Test
    public void bareTagAttributeSelector_withoutDotOrHash_isCorrectedToCssSelector() {
        // "input[type='text']" has no @ and no dot/hash after the tag — XPath has no predicate
        // syntax without @ (or a numeric index), so this is unambiguously a CSS attribute selector.
        By fixed = new Probe().normalize(By.xpath("input[type='text']"));
        Assert.assertEquals(fixed, By.cssSelector("input[type='text']"));
    }

    @Test
    public void cssAttributeSelector_withAtSignInValue_isStillCorrected() {
        // The attribute VALUE happens to contain '@' (an email) — this must not be mistaken for an
        // XPath predicate marker, since '@' here is data, not syntax.
        By fixed = new Probe().normalize(By.xpath("[data-testid='foo@bar.com']"));
        Assert.assertEquals(fixed, By.cssSelector("[data-testid='foo@bar.com']"));
    }

    @Test
    public void genuineIdLocator_isReturnedUnchanged() {
        By original = By.id("login-btn");
        Assert.assertEquals(new Probe().normalize(original), original);
    }

    @Test
    public void genuineCssSelector_isReturnedUnchanged() {
        By original = By.cssSelector(".login-btn > span");
        Assert.assertEquals(new Probe().normalize(original), original);
    }

    @Test
    public void nullLocator_returnsNull() {
        Assert.assertNull(new Probe().normalize(null));
    }
}
