package Ellithium.core.ai.healing;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Ellithium.core.ai.healing.SemanticLocatorResolver.strategyWeightForAttrs;

/**
 * Unit tests for Tier-2 resolver weight grading.
 * Exact identity matches (id, name, data-testid, aria-label) are gold (1.0).
 * Text and substring matches are silver (0.75).
 */
public class StrategyWeightTest {

    private static final List<String> NAMES = List.of("login");

    private static Map<String, Object> attrs(String key, String value) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, value);
        return m;
    }

    @Test
    public void exactId_isGold() {
        Assert.assertEquals(strategyWeightForAttrs(attrs("id", "login"), NAMES), 1.00, 1e-9);
    }

    @Test
    public void exactText_isSilver() {
        Assert.assertEquals(strategyWeightForAttrs(attrs("text", "login"), NAMES), 0.75, 1e-9,
                "exact text match must be silver (0.75), not gold");
    }

    @Test
    public void substringText_isSilver_notGold() {
        Assert.assertEquals(strategyWeightForAttrs(attrs("text", "please login here"), NAMES), 0.75, 1e-9,
                "a substring text match must not be graded equal to an exact identity match");
    }

    @Test
    public void substringInAllAttrs_isSilver() {
        Assert.assertEquals(strategyWeightForAttrs(attrs("allattrs", "btn login primary"), NAMES), 0.75, 1e-9);
    }

    @Test
    public void noMatch_isNaN() {
        Assert.assertTrue(Double.isNaN(strategyWeightForAttrs(attrs("id", "checkout"), NAMES)));
    }

    // ── looksDynamic gold-demotion guard ──

    @Test
    public void dynamicUuidId_isSilverNotGold() {
        // A UUID-valued id that happens to match a semantic name must not reach gold — it's auto-generated.
        List<String> names = List.of("3f2504e0");
        Map<String, Object> attrs = attrs("id", "3f2504e0-4f89-11d3-9a0c-0305e82c3301");
        // UUID ids are dynamic → silver at best
        double w = strategyWeightForAttrs(attrs, names);
        Assert.assertTrue(Double.isNaN(w) || w <= 0.75,
                "a UUID-valued id must not reach gold (1.0). Got: " + w);
    }

    @Test
    public void numericId_isSilverNotGold() {
        List<String> names = List.of("42");
        Map<String, Object> m = new HashMap<>();
        m.put("id", "42");
        double w = strategyWeightForAttrs(m, names);
        Assert.assertTrue(Double.isNaN(w) || w <= 0.75,
                "a pure-numeric id must not reach gold. Got: " + w);
    }

    @Test
    public void reactAutoId_isSilverNotGold() {
        List<String> names = List.of(":r0:");
        Map<String, Object> m = new HashMap<>();
        m.put("id", ":r0:");
        double w = strategyWeightForAttrs(m, names);
        Assert.assertTrue(Double.isNaN(w) || w <= 0.75,
                "a React auto-id ':r0:' must not reach gold. Got: " + w);
    }

    @Test
    public void numericSuffixId_isSilverNotGold() {
        // react-aria-42 is auto-generated (numeric suffix) — must not reach gold
        List<String> names = List.of("react-aria-42");
        Map<String, Object> m = new HashMap<>();
        m.put("id", "react-aria-42");
        double w = strategyWeightForAttrs(m, names);
        Assert.assertTrue(Double.isNaN(w) || w <= 0.75,
                "a numeric-suffix id 'react-aria-42' must not reach gold. Got: " + w);
    }

    @Test
    public void stableHumanReadableId_isGold() {
        Assert.assertEquals(strategyWeightForAttrs(attrs("id", "login"), NAMES), 1.00, 1e-9,
                "a stable human-readable id must reach gold");
    }

    // ── looksDynamic unit tests ──

    @Test
    public void looksDynamic_uuid_isTrue() {
        Assert.assertTrue(SemanticLocatorResolver.looksDynamic("3f2504e0-4f89-11d3-9a0c-0305e82c3301"));
    }

    @Test
    public void looksDynamic_pureNumeric_isTrue() {
        Assert.assertTrue(SemanticLocatorResolver.looksDynamic("42"));
        Assert.assertTrue(SemanticLocatorResolver.looksDynamic("0"));
    }

    @Test
    public void looksDynamic_reactAutoId_isTrue() {
        Assert.assertTrue(SemanticLocatorResolver.looksDynamic(":r0:"));
        Assert.assertTrue(SemanticLocatorResolver.looksDynamic(":rA3:"));
    }

    @Test
    public void looksDynamic_numericSuffix_isTrue() {
        Assert.assertTrue(SemanticLocatorResolver.looksDynamic("react-aria-42"));
        Assert.assertTrue(SemanticLocatorResolver.looksDynamic("item_99"));
    }

    @Test
    public void looksDynamic_humanReadable_isFalse() {
        Assert.assertFalse(SemanticLocatorResolver.looksDynamic("login"));
        Assert.assertFalse(SemanticLocatorResolver.looksDynamic("submitButton"));
        Assert.assertFalse(SemanticLocatorResolver.looksDynamic("email-input"));
        Assert.assertFalse(SemanticLocatorResolver.looksDynamic(null));
        Assert.assertFalse(SemanticLocatorResolver.looksDynamic(""));
    }
}

