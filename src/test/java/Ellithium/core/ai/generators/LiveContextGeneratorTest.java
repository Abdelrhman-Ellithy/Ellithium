package Ellithium.core.ai.generators;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

public class LiveContextGeneratorTest {

    // ── sameOrigin (private static) ───────────────────────────────────────

    private static boolean sameOrigin(String a, String b) throws Exception {
        Method m = LiveContextGenerator.class.getDeclaredMethod("sameOrigin", String.class, String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, a, b);
    }

    @Test
    public void sameOrigin_identicalUrls_returnsTrue() throws Exception {
        Assert.assertTrue(sameOrigin("https://example.com/login", "https://example.com/dashboard"));
    }

    @Test
    public void sameOrigin_differentScheme_returnsFalse() throws Exception {
        Assert.assertFalse(sameOrigin("https://example.com/page", "http://example.com/page"));
    }

    @Test
    public void sameOrigin_differentHost_returnsFalse() throws Exception {
        Assert.assertFalse(sameOrigin("https://example.com/page", "https://evil.com/page"));
    }

    @Test
    public void sameOrigin_differentPort_returnsFalse() throws Exception {
        Assert.assertFalse(sameOrigin("https://example.com:8080/page", "https://example.com:9090/page"));
    }

    @Test
    public void sameOrigin_samePortExplicit_returnsTrue() throws Exception {
        Assert.assertTrue(sameOrigin("https://example.com:443/a", "https://example.com:443/b"));
    }

    @Test
    public void sameOrigin_subdomainVsApex_returnsFalse() throws Exception {
        Assert.assertFalse(sameOrigin("https://app.example.com/x", "https://example.com/x"));
    }

    @Test
    public void sameOrigin_nullA_returnsFalse() throws Exception {
        Assert.assertFalse(sameOrigin(null, "https://example.com/page"));
    }

    @Test
    public void sameOrigin_nullB_returnsFalse() throws Exception {
        Assert.assertFalse(sameOrigin("https://example.com/page", null));
    }

    @Test
    public void sameOrigin_bothNull_returnsFalse() throws Exception {
        Assert.assertFalse(sameOrigin(null, null));
    }

    @Test
    public void sameOrigin_schemeIsCaseInsensitive() throws Exception {
        Assert.assertTrue(sameOrigin("HTTPS://example.com/a", "https://example.com/b"));
    }

    @Test
    public void sameOrigin_hostIsCaseInsensitive() throws Exception {
        Assert.assertTrue(sameOrigin("https://Example.COM/a", "https://example.com/b"));
    }

    @Test
    public void sameOrigin_malformedUrl_returnsFalse() throws Exception {
        Assert.assertFalse(sameOrigin("not a url", "https://example.com"));
    }

    // ── parseLocator (private static) ─────────────────────────────────────

    private static By parseLocator(String expression) throws Exception {
        Method m = LiveContextGenerator.class.getDeclaredMethod("parseLocator", String.class);
        m.setAccessible(true);
        return (By) m.invoke(null, expression);
    }

    @Test
    public void parseLocator_byId_returnsById() throws Exception {
        By by = parseLocator("By.id(\"username\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.id("username"));
    }

    @Test
    public void parseLocator_byName_returnsByName() throws Exception {
        By by = parseLocator("By.name(\"email\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.name("email"));
    }

    @Test
    public void parseLocator_byCssSelector_returnsByCss() throws Exception {
        By by = parseLocator("By.cssSelector(\"[data-testid='submit']\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.cssSelector("[data-testid='submit']"));
    }

    @Test
    public void parseLocator_byXpath_returnsByXpath() throws Exception {
        By by = parseLocator("By.xpath(\"//button[text()='Login']\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.xpath("//button[text()='Login']"));
    }

    @Test
    public void parseLocator_byLinkText_returnsByLinkText() throws Exception {
        By by = parseLocator("By.linkText(\"Forgot Password\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.linkText("Forgot Password"));
    }

    @Test
    public void parseLocator_byPartialLinkText_returnsByPartialLinkText() throws Exception {
        By by = parseLocator("By.partialLinkText(\"Forgot\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.partialLinkText("Forgot"));
    }

    @Test
    public void parseLocator_byClassName_returnsByClassName() throws Exception {
        By by = parseLocator("By.className(\"btn-primary\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.className("btn-primary"));
    }

    @Test
    public void parseLocator_byTagName_returnsByTagName() throws Exception {
        By by = parseLocator("By.tagName(\"button\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.tagName("button"));
    }

    @Test
    public void parseLocator_singleQuotes_parsed() throws Exception {
        By by = parseLocator("By.id('loginBtn')");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.id("loginBtn"));
    }

    @Test
    public void parseLocator_nullExpression_returnsNull() throws Exception {
        Assert.assertNull(parseLocator(null));
    }

    @Test
    public void parseLocator_blankExpression_returnsNull() throws Exception {
        Assert.assertNull(parseLocator("   "));
    }

    @Test
    public void parseLocator_malformed_returnsNull() throws Exception {
        Assert.assertNull(parseLocator("By.id username"));
    }

    @Test
    public void parseLocator_unknownMethod_returnsNull() throws Exception {
        Assert.assertNull(parseLocator("By.shadowRoot(\"#host\")"));
    }

    @Test
    public void parseLocator_valueWithSpecialChars_parsed() throws Exception {
        By by = parseLocator("By.cssSelector(\"input[type='password']\")");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.cssSelector("input[type='password']"));
    }

    @Test
    public void parseLocator_extraWhitespace_parsed() throws Exception {
        By by = parseLocator("By.id( \"submit\" )");
        Assert.assertNotNull(by);
        Assert.assertEquals(by, By.id("submit"));
    }
}
