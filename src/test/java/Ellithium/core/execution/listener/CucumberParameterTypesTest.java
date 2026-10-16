package Ellithium.core.execution.listener;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CucumberParameterTypesTest {

    private final CucumberParameterTypes types = new CucumberParameterTypes();

    @Test
    public void testLocator_plainString() {
        By byId = types.locator("id=login-button");
        Assert.assertEquals(byId, By.id("login-button"));

        By byName = types.locator("name=username");
        Assert.assertEquals(byName, By.name("username"));
    }

    @Test
    public void testLocator_doubleQuotedString() {
        By byCss = types.locator("\"css=.submit-btn\"");
        Assert.assertEquals(byCss, By.cssSelector(".submit-btn"));
    }

    @Test
    public void testLocator_singleQuotedString() {
        By byXpath = types.locator("'xpath=//input[@type=\"text\"]'");
        Assert.assertEquals(byXpath, By.xpath("//input[@type=\"text\"]"));
    }

    @Test
    public void testBy_aliasMethod() {
        By byTag = types.by("tag=h1");
        Assert.assertEquals(byTag, By.tagName("h1"));

        By byId = types.by("\"id=main\"");
        Assert.assertEquals(byId, By.id("main"));
    }

    @Test
    public void testNullAndEmptyHandling() {
        Assert.assertThrows(IllegalArgumentException.class, () -> types.locator(null));
        Assert.assertThrows(IllegalArgumentException.class, () -> types.by(null));
        Assert.assertThrows(IllegalArgumentException.class, () -> types.locator(""));
        Assert.assertThrows(IllegalArgumentException.class, () -> types.by("   "));
    }
}
