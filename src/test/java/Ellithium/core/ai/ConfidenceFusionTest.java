package Ellithium.core.ai;

import org.testng.Assert;
import org.testng.annotations.Test;

import static Ellithium.core.ai.EnsembleHealer.fuseConfidence;

/**
 * Behaviour spec for {@link EnsembleHealer#fuseConfidence}. The strongest signal sets the floor —
 * a cosine degraded by a broken/typo'd query can never drag a gold resolver hit below its own
 * value — and agreement adds a damped bonus over the remaining headroom.
 */
public class ConfidenceFusionTest {

    @Test
    public void goldDominates_weakCosineCannotSubtract() {
        Assert.assertEquals(fuseConfidence(1.0, 0.417), 1.0, 1e-9,
                "a gold resolver hit (f2=1.0) must stay 1.0 regardless of a typo-degraded cosine");
        Assert.assertEquals(fuseConfidence(1.0, 0.0), 1.0, 1e-9,
                "even a zero cosine cannot drag a gold hit below 1.0");
    }

    @Test
    public void noResolverSignal_fallsBackToCosine() {
        Assert.assertEquals(fuseConfidence(Double.NaN, 0.6), 0.6, 1e-9,
                "NaN f2 (no resolver signal) → cosine alone, preserving prior behaviour");
    }

    @Test
    public void agreement_recoversRealHealTheMeanDropped() {
        double fused = fuseConfidence(0.7, 0.72);
        Assert.assertTrue(fused > 0.75,
                "two agreeing mid signals must clear a 0.75 threshold (was 0.71 under the mean): " + fused);
        Assert.assertTrue(fused <= 1.0, "fused confidence is bounded by 1.0");
    }

    @Test
    public void loneSilver_staysConservative_defersToLlm() {
        double fused = fuseConfidence(0.6, 0.417);
        Assert.assertTrue(fused < 0.75,
                "a lone silver hit + weak cosine must stay below 0.75 (precision-first): " + fused);
    }

    @Test
    public void symmetric_inArguments() {
        Assert.assertEquals(fuseConfidence(0.6, 0.8), fuseConfidence(0.8, 0.6), 1e-9,
                "fusion must be symmetric in its two signals");
    }

    @Test
    public void bounded_byOne() {
        Assert.assertTrue(fuseConfidence(0.9, 0.9) <= 1.0, "fused confidence never exceeds 1.0");
        Assert.assertEquals(fuseConfidence(0.9, 0.9), 0.945, 1e-9);
    }
}
