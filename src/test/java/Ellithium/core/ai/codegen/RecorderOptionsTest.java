package Ellithium.core.ai.codegen;

import org.testng.Assert;
import org.testng.annotations.Test;


public class RecorderOptionsTest {

    @Test
    public void defaults_returnsExpectedValues() {
        RecorderOptions d = RecorderOptions.defaults();
        Assert.assertNotNull(d.outputBasePath());
        Assert.assertNotNull(d.packageName());
        Assert.assertNotNull(d.browser());
        Assert.assertNotNull(d.assertMode());
    }

    @Test
    public void isTest_trueForTestTarget() {
        RecorderOptions o = new RecorderOptions("out", "pkg", "Chrome", "test", "soft", false, false);
        Assert.assertTrue(o.isTest());
    }

    @Test
    public void isTest_falseForPomTarget() {
        RecorderOptions o = new RecorderOptions("out", "pkg", "Chrome", "pom", "soft", false, false);
        Assert.assertFalse(o.isTest());
    }

    @Test
    public void isSoftAssert_trueWhenNotHard() {
        RecorderOptions o = new RecorderOptions("out", "pkg", "Chrome", "test", "soft", false, false);
        Assert.assertTrue(o.isSoftAssert());
    }

    @Test
    public void isSoftAssert_falseWhenHard() {
        RecorderOptions o = new RecorderOptions("out", "pkg", "Chrome", "test", "hard", false, false);
        Assert.assertFalse(o.isSoftAssert());
    }

    @Test
    public void withAssertMode_returnsNewOptionsWithUpdatedMode() {
        RecorderOptions original = new RecorderOptions("out", "pkg", "Chrome", "test", "soft", false, false);
        RecorderOptions updated = original.withAssertMode("hard");
        Assert.assertFalse(updated.isSoftAssert());
        Assert.assertTrue(original.isSoftAssert(), "original must not mutate");
    }

    @Test
    public void withAssertMode_preservesOtherFields() {
        RecorderOptions o = new RecorderOptions("myOut", "myPkg", "Firefox", "pom", "soft", true, true);
        RecorderOptions u = o.withAssertMode("hard");
        Assert.assertEquals(u.outputBasePath(), "myOut");
        Assert.assertEquals(u.packageName(), "myPkg");
        Assert.assertEquals(u.browser(), "Firefox");
        Assert.assertFalse(u.isTest());
        Assert.assertTrue(u.llmPolish());
    }
}
