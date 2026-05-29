package Ellithium.core.ai;

import org.testng.Assert;
import org.testng.annotations.Test;

import static Ellithium.core.ai.EnsembleHealer.GateResult;
import static Ellithium.core.ai.EnsembleHealer.decideGate;

/**
 * Behaviour spec for the ensemble accept gate (the strategy-rescue fix). Path A = calibrated anchor
 * cosine clears the threshold; Path B = gold-tier Tier-2 (f2≥0.95) corroborated by the baseline
 * fingerprint (f1≥floor) rescues a heal the weak bi-encoder rejected — two independent high-precision
 * signals, precision-first. Cold start (no fingerprint) must never rescue.
 */
public class GateDecisionTest {

    private static final double THRESHOLD = 0.40;
    private static final double FLOOR = 0.50;

    @Test
    public void cosinePath_acceptsWhenAnchorClearsThreshold() {
        GateResult g = decideGate(0.55, THRESHOLD, 0.20, Double.NaN, true, FLOOR);
        Assert.assertTrue(g.accept);
        Assert.assertEquals(g.via, "cosine");
        Assert.assertEquals(g.score, 0.55, 1e-9);
    }

    @Test
    public void rescuePath_goldStrategyPlusFingerprint_healsDespiteWeakCosine() {
        // cosine 0.30 < 0.40, but gold f2=1.0 + fingerprint f1=0.60 ≥ floor → rescue.
        GateResult g = decideGate(0.30, THRESHOLD, 0.60, 1.0, true, FLOOR);
        Assert.assertTrue(g.accept, "gold strategy + fingerprint must rescue a weak-cosine heal");
        Assert.assertEquals(g.via, "strategy-rescue");
        Assert.assertEquals(g.score, 0.80, 1e-9, "rescue confidence = mean(f1, f2)");
    }

    @Test
    public void rescuePath_disabled_fallsThrough() {
        GateResult g = decideGate(0.30, THRESHOLD, 0.60, 1.0, false, FLOOR);
        Assert.assertFalse(g.accept, "rescue disabled → weak cosine must not heal");
    }

    @Test
    public void coldStart_noFingerprint_doesNotRescueOnStrategyAlone() {
        // f1 NaN (no baseline) → a lone gold strategy match could be the wrong element → no rescue.
        GateResult g = decideGate(0.30, THRESHOLD, Double.NaN, 1.0, true, FLOOR);
        Assert.assertFalse(g.accept, "a strategy match with no fingerprint corroboration must not heal");
    }

    @Test
    public void weakStrategy_belowGold_doesNotRescue() {
        // f2=0.50 (bronze) is not high-precision enough to rescue even with a strong fingerprint.
        GateResult g = decideGate(0.30, THRESHOLD, 0.90, 0.50, true, FLOOR);
        Assert.assertFalse(g.accept, "only gold-tier (f2≥0.95) strategy matches may rescue");
    }

    @Test
    public void fingerprintBelowFloor_doesNotRescue() {
        GateResult g = decideGate(0.30, THRESHOLD, 0.30, 1.0, true, FLOOR);
        Assert.assertFalse(g.accept, "fingerprint below floor → insufficient corroboration");
    }
}
