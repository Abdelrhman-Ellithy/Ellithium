package Ellithium.core.ai.models;

import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HealOutcomeTest {

    @Test
    public void of_withElement_isHitTrue() {
        WebElement el = Mockito.mock(WebElement.class);
        HealOutcome outcome = HealOutcome.of(el, 0.9, 1);
        Assert.assertTrue(outcome.isHit());
    }

    @Test
    public void of_withNullElement_isHitFalse() {
        HealOutcome outcome = HealOutcome.of(null, 0.0, 1);
        Assert.assertFalse(outcome.isHit());
    }

    @Test
    public void of_withLocator_storesLocator() {
        WebElement el = Mockito.mock(WebElement.class);
        By locator = By.id("login");
        HealOutcome outcome = HealOutcome.of(el, locator, 0.85, 2);
        Assert.assertEquals(outcome.reconstructedLocator(), locator);
    }

    @Test
    public void of_withoutLocator_locatorIsNull() {
        WebElement el = Mockito.mock(WebElement.class);
        HealOutcome outcome = HealOutcome.of(el, 0.75, 1);
        Assert.assertNull(outcome.reconstructedLocator());
    }

    @Test
    public void score_storesValue() {
        WebElement el = Mockito.mock(WebElement.class);
        HealOutcome outcome = HealOutcome.of(el, 0.92, 3);
        Assert.assertEquals(outcome.score(), 0.92, 0.001);
    }

    @Test
    public void tier_storesValue() {
        WebElement el = Mockito.mock(WebElement.class);
        HealOutcome outcome = HealOutcome.of(el, 0.8, 2);
        Assert.assertEquals(outcome.tier(), 2);
    }

    @Test
    public void element_storesReference() {
        WebElement el = Mockito.mock(WebElement.class);
        HealOutcome outcome = HealOutcome.of(el, 0.9, 1);
        Assert.assertSame(outcome.element(), el);
    }
}
