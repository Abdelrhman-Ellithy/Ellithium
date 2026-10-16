package Ellithium.core.ai.models;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HealingRequestTest {

    @Test
    public void testHealingRequestConstructors() {
        By locator = By.id("test-id");
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();

        // 8-arg legacy constructor
        HealingRequest r1 = new HealingRequest(null, locator, trace, "clickOnElement",
                "testMethod", "submitBtn", "test-id", null);
        Assert.assertEquals(r1.brokenLocator(), locator);
        Assert.assertEquals(r1.actionType(), "clickOnElement");
        Assert.assertEquals(r1.callerMethod(), "testMethod");
        Assert.assertEquals(r1.fieldName(), "submitBtn");
        Assert.assertEquals(r1.locatorValue(), "test-id");
        Assert.assertNull(r1.baseline());
        Assert.assertNotNull(r1.hints());
        Assert.assertTrue(r1.hints().isEmpty());

        // 9-arg canonical constructor with explicit null hints
        HealingRequest r2 = new HealingRequest(null, locator, trace, "clickOnElement",
                "testMethod", "submitBtn", "test-id", null, null);
        Assert.assertNotNull(r2.hints(), "null hints should default to new HealingHints instance");

        // 9-arg canonical constructor with existing hints
        HealingHints hints = new HealingHints();
        org.openqa.selenium.WebElement mockElement = org.mockito.Mockito.mock(org.openqa.selenium.WebElement.class);
        hints.add(mockElement, By.id("finalist-1"), 0.85, "token-match", 1);
        HealingRequest r3 = new HealingRequest(null, locator, trace, "clickOnElement",
                "testMethod", "submitBtn", "test-id", null, hints);
        Assert.assertSame(r3.hints(), hints);
        Assert.assertFalse(r3.hints().isEmpty());
    }
}
