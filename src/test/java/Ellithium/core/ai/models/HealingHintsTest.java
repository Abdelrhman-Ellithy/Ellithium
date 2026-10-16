package Ellithium.core.ai.models;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;

public class HealingHintsTest {

    private WebElement createMockElement(String id) {
        return (WebElement) Proxy.newProxyInstance(
                WebElement.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    if ("equals".equals(method.getName())) {
                        if (args != null && args.length == 1 && args[0] != null) {
                            return proxy.toString().equals(args[0].toString());
                        }
                        return false;
                    }
                    if ("toString".equals(method.getName())) {
                        return "MockElement[" + id + "]";
                    }
                    return null;
                }
        );
    }

    @Test
    public void testEmptyHints() {
        HealingHints hints = new HealingHints();
        Assert.assertTrue(hints.isEmpty());
        Assert.assertEquals(hints.size(), 0);
        Assert.assertEquals(hints.describe(), "");
        Assert.assertTrue(hints.shortlist().isEmpty());
    }

    @Test
    public void testAddAndCapAtMaxHints() {
        HealingHints hints = new HealingHints();

        WebElement el1 = createMockElement("1");
        WebElement el2 = createMockElement("2");
        WebElement el3 = createMockElement("3");
        WebElement el4 = createMockElement("4");
        WebElement el5 = createMockElement("5");
        WebElement el6 = createMockElement("6");

        hints.add(el1, By.id("el1"), 0.85, "mutation match", 1);
        Assert.assertEquals(hints.size(), 1);
        Assert.assertFalse(hints.isEmpty());

        // Null element should be silently ignored
        hints.add(null, By.id("null"), 0.5, "none", 1);
        Assert.assertEquals(hints.size(), 1);

        // Duplicate element should be deduplicated
        hints.add(el1, By.id("el1_dup"), 0.90, "dup", 2);
        Assert.assertEquals(hints.size(), 1);

        // Add up to 5 elements
        hints.add(el2, By.id("el2"), 0.80, "tier 2", 2);
        hints.add(el3, By.id("el3"), 0.75, "tier 2", 2);
        hints.add(el4, By.id("el4"), 0.70, "tier 1", 1);
        hints.add(el5, By.id("el5"), 0.65, "tier 1", 1);
        Assert.assertEquals(hints.size(), 5);

        // 6th element should be ignored (MAX_HINTS = 5)
        hints.add(el6, By.id("el6"), 0.60, "tier 2", 2);
        Assert.assertEquals(hints.size(), 5);

        // Check describe content
        String desc = hints.describe();
        Assert.assertTrue(desc.contains("By.id: el1"));
        Assert.assertTrue(desc.contains("[tier 1, mutation match, score 0.85]"));
        Assert.assertTrue(desc.contains(" | "));
    }

    @Test
    public void testDescribeWithNullLocator() {
        HealingHints hints = new HealingHints();
        WebElement el = createMockElement("test");
        hints.add(el, null, 0.72, "dom scan", 1);

        String desc = hints.describe();
        Assert.assertTrue(desc.contains("(element)"));
        Assert.assertTrue(desc.contains("[tier 1, dom scan, score 0.72]"));
    }
}
