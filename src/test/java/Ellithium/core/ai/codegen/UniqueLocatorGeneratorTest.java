package Ellithium.core.ai.codegen;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UniqueLocatorGeneratorTest {

    private static Map<String, Object> attrs(String... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static LocatorCandidate byTier(List<LocatorCandidate> cs, String tier) {
        return cs.stream().filter(c -> c.tier().equals(tier)).findFirst().orElse(null);
    }

    @Test
    public void ranksTestidAboveIdAboveScopedCss() {
        Map<String, Object> a = attrs("data-testid", "submit", "id", "loginBtn",
                "class", "btn primary", "tag", "button");
        List<LocatorCandidate> ranked = UniqueLocatorGenerator.rank(a, by -> 1);
        Assert.assertEquals(ranked.get(0).tier(), "data-testid");
        double testid = byTier(ranked, "data-testid").score();
        double id = byTier(ranked, "id").score();
        double css = byTier(ranked, "css-scoped").score();
        Assert.assertTrue(testid > id && id > css, "stability order testid > id > scoped-css");
    }

    @Test
    public void dynamicId_isDemotedAndParameterizable() {
        Map<String, Object> a = attrs("id", "month_0", "tag", "input");
        List<LocatorCandidate> ranked = UniqueLocatorGenerator.rank(a, by -> 1);
        LocatorCandidate id = byTier(ranked, "id");
        Assert.assertNotNull(id);
        Assert.assertTrue(id.parameterizable(), "month_0 must be flagged parameterizable");
    }

    @Test
    public void genericDataAttribute_isCapturedAndParameterizable() {
        List<LocatorCandidate> ranked = UniqueLocatorGenerator.rank(
                Map.of(), Map.of("data-choice-index", "0"), by -> 1);
        LocatorCandidate c = byTier(ranked, "data-choice-index");
        Assert.assertNotNull(c, "generic data-* attribute must produce a candidate");
        Assert.assertTrue(c.parameterizable(), "data-choice-index='0' must be parameterizable");
        Assert.assertEquals(c.javaExpression(), "By.cssSelector(\"[data-choice-index='0']\")");
    }

    @Test
    public void nonUniqueCandidate_isDemotedBelowUnique() {
        Map<String, Object> a = attrs("data-testid", "row", "id", "uniqueRow", "tag", "div");
        UniqueLocatorGenerator.UniquenessProbe probe =
                by -> by.toString().contains("data-testid") ? 5 : 1;
        List<LocatorCandidate> ranked = UniqueLocatorGenerator.rank(a, probe);
        Assert.assertEquals(ranked.get(0).tier(), "id", "a unique id must outrank a non-unique data-testid");
        Assert.assertFalse(byTier(ranked, "data-testid").unique());
    }

    @Test
    public void fromCapture_mapsAllStrategiesToRealByExpressions() {
        Assert.assertEquals(UniqueLocatorGenerator.fromCapture("id", "[id='u']", "u", "id", true, false).javaExpression(),
                "By.id(\"u\")");
        Assert.assertEquals(UniqueLocatorGenerator.fromCapture("linkText", null, "Sign in", "link-text", true, false).javaExpression(),
                "By.linkText(\"Sign in\")");
        Assert.assertEquals(UniqueLocatorGenerator.fromCapture("partialLinkText", null, "Sign", "partial-link-text", false, false).javaExpression(),
                "By.partialLinkText(\"Sign\")");
        Assert.assertEquals(UniqueLocatorGenerator.fromCapture("className", null, "btn", "class-name", true, false).javaExpression(),
                "By.className(\"btn\")");
        Assert.assertEquals(UniqueLocatorGenerator.fromCapture("tagName", null, "h1", "tag-name", true, false).javaExpression(),
                "By.tagName(\"h1\")");
        Assert.assertEquals(UniqueLocatorGenerator.fromCapture("xpath", "//a[.='x']", "x", "text", true, false).javaExpression(),
                "By.xpath(\"//a[.='x']\")");
    }

    @Test
    public void fromCapture_uniqueIdOutranksNonUniqueLinkText() {
        LocatorCandidate id = UniqueLocatorGenerator.fromCapture("id", "[id='login']", "login", "id", true, false);
        LocatorCandidate link = UniqueLocatorGenerator.fromCapture("partialLinkText", null, "Log", "partial-link-text", false, false);
        Assert.assertTrue(id.score() > link.score(), "a unique id must outrank a non-unique partial-link-text");
    }

    @Test
    public void looksDynamic_detectsIndicesUuidsAndDigitsRuns() {
        Assert.assertTrue(UniqueLocatorGenerator.looksDynamic("month_0"));
        Assert.assertTrue(UniqueLocatorGenerator.looksDynamic("0"));
        Assert.assertTrue(UniqueLocatorGenerator.looksDynamic("user-12345"));
        Assert.assertTrue(UniqueLocatorGenerator.looksDynamic("a1b2c3d4e5f60718"));
        Assert.assertFalse(UniqueLocatorGenerator.looksDynamic("loginBtn"));
        Assert.assertFalse(UniqueLocatorGenerator.looksDynamic("submit"));
    }
}
