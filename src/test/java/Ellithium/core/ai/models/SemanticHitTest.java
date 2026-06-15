package Ellithium.core.ai.models;

import org.mockito.Mockito;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SemanticHitTest {

    @Test
    public void constructor_storesElement() {
        WebElement el = Mockito.mock(WebElement.class);
        SemanticHit hit = new SemanticHit(el, 1.0, "gold: exact id");
        Assert.assertSame(hit.element, el);
    }

    @Test
    public void constructor_storesToierWeight() {
        WebElement el = Mockito.mock(WebElement.class);
        SemanticHit hit = new SemanticHit(el, 0.75, "silver");
        Assert.assertEquals(hit.tierWeight, 0.75, 0.001);
    }

    @Test
    public void constructor_storesStrategyDescription() {
        WebElement el = Mockito.mock(WebElement.class);
        SemanticHit hit = new SemanticHit(el, 0.5, "bronze: aria-label contains");
        Assert.assertEquals(hit.strategyDescription, "bronze: aria-label contains");
    }

    @Test
    public void goldWeight_is1_0() {
        WebElement el = Mockito.mock(WebElement.class);
        SemanticHit hit = new SemanticHit(el, 1.0, "gold");
        Assert.assertEquals(hit.tierWeight, 1.0, 0.001);
    }

    @Test
    public void ironWeight_is0_3() {
        WebElement el = Mockito.mock(WebElement.class);
        SemanticHit hit = new SemanticHit(el, 0.3, "iron");
        Assert.assertEquals(hit.tierWeight, 0.3, 0.001);
    }
}
