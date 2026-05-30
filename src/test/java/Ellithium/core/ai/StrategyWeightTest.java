package Ellithium.core.ai;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Ellithium.core.ai.SemanticLocatorResolver.strategyWeightForAttrs;

/**
 * Spec for the Tier-2 resolver weight grading. An EXACT identity or full-text match is gold (1.0);
 * a mere substring match in the visible text or in the concatenated attribute values is silver
 * (0.75) — substring containment is not strong enough to be trusted equal to an exact data-testid.
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
    public void exactText_isGold() {
        Assert.assertEquals(strategyWeightForAttrs(attrs("text", "login"), NAMES), 1.00, 1e-9);
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
}
