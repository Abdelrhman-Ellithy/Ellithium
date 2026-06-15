package Ellithium.core.ai.models;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HealingResultTest {

    @Test
    public void getNewLocatorExpression_returnsValue() {
        HealingResult r = new HealingResult("By.id(\"login\")", 0.9, "Found by id");
        Assert.assertEquals(r.getNewLocatorExpression(), "By.id(\"login\")");
    }

    @Test
    public void getConfidence_returnsValue() {
        HealingResult r = new HealingResult("By.id(\"x\")", 0.75, "reason");
        Assert.assertEquals(r.getConfidence(), 0.75, 0.001);
    }

    @Test
    public void getReasoning_returnsValue() {
        HealingResult r = new HealingResult("By.id(\"x\")", 0.9, "Exact attribute match");
        Assert.assertEquals(r.getReasoning(), "Exact attribute match");
    }

    @Test
    public void isConfidentEnough_aboveThreshold_returnsTrue() {
        HealingResult r = new HealingResult("By.id(\"x\")", 0.9, "");
        Assert.assertTrue(r.isConfidentEnough(0.85));
    }

    @Test
    public void isConfidentEnough_atThreshold_returnsTrue() {
        HealingResult r = new HealingResult("By.id(\"x\")", 0.85, "");
        Assert.assertTrue(r.isConfidentEnough(0.85));
    }

    @Test
    public void isConfidentEnough_belowThreshold_returnsFalse() {
        HealingResult r = new HealingResult("By.id(\"x\")", 0.7, "");
        Assert.assertFalse(r.isConfidentEnough(0.85));
    }

    @Test
    public void toString_containsLocatorAndConfidence() {
        HealingResult r = new HealingResult("By.id(\"btn\")", 0.92, "matched");
        String s = r.toString();
        Assert.assertTrue(s.contains("By.id(\"btn\")"));
        Assert.assertTrue(s.contains("0.92"));
    }
}
