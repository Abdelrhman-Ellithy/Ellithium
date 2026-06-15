package Ellithium.core.ai.healing;

import org.testng.Assert;
import org.testng.annotations.Test;

import static Ellithium.core.ai.healing.EnsembleHealer.fuseConfidence;

/**
 * Behaviour spec for {@link EnsembleHealer#fuseConfidence}.
 *
 * Current formula (GATE_RESCUE_COSINE_FLOOR=0.50, SIGNAL_AGREEMENT=0.35):
 *   if f2 is NaN                      → return f3
 *   if f3 < 0.50 AND f2 > f3          → return f3 (floor guard: wrong-type boost blocked)
 *   else                              → min(1.0, f3 + (1−f3)·f2·0.35)
 *
 * f3 (cosine) is the primary, calibrated signal. f2 (strategy heuristic) adds a
 * proportional bonus over remaining headroom, capped by SIGNAL_AGREEMENT.
 */
public class ConfidenceFusionTest {

    @Test
    public void nanF2_returnsCosine() {
        Assert.assertEquals(fuseConfidence(Double.NaN, 0.6), 0.6, 1e-9);
        Assert.assertEquals(fuseConfidence(Double.NaN, 0.0), 0.0, 1e-9);
        Assert.assertEquals(fuseConfidence(Double.NaN, 1.0), 1.0, 1e-9);
    }

    @Test
    public void floorGuard_f3BelowFloor_f2AboveF3_returnsF3Unchanged() {
        // f3 < 0.50 AND f2 > f3 → floor guard fires, f3 returned as-is
        // A high strategy signal cannot override a local model that disagrees at < 0.50
        Assert.assertEquals(fuseConfidence(1.0, 0.417), 0.417, 1e-9);
        Assert.assertEquals(fuseConfidence(1.0, 0.0),   0.0,   1e-9);
        Assert.assertEquals(fuseConfidence(0.6, 0.3),   0.3,   1e-9);
    }

    @Test
    public void floorGuard_doesNotFire_whenF2EqualsF3() {
        // f2 == f3 → "f2 > f3" is false → formula applies even when both are below 0.50
        double expected = 0.4 + (1.0 - 0.4) * 0.4 * 0.35;
        Assert.assertEquals(fuseConfidence(0.4, 0.4), expected, 1e-9);
    }

    @Test
    public void floorGuard_doesNotFire_whenF2BelowF3() {
        // f3 < 0.50 AND f2 < f3 → "f2 > f3" is false → formula applies
        double expected = 0.417 + (1.0 - 0.417) * 0.3 * 0.35;
        Assert.assertEquals(fuseConfidence(0.3, 0.417), expected, 1e-9);
    }

    @Test
    public void floorGuard_boundary_f3ExactlyAtFloor_usesFormula() {
        // f3 == 0.50 → condition "f3 < 0.50" is FALSE → formula applies, not floor guard
        // 0.50 + 0.50 × 1.0 × 0.35 = 0.675
        Assert.assertEquals(fuseConfidence(1.0, 0.50), 0.675, 1e-9);
    }

    @Test
    public void formula_zeroF2_doesNotBoostCosine() {
        // f2=0 contributes nothing; fused == f3
        Assert.assertEquals(fuseConfidence(0.0, 0.8), 0.8, 1e-9);
        Assert.assertEquals(fuseConfidence(0.0, 0.6), 0.6, 1e-9);
    }

    @Test
    public void formula_knownValue_halfSignals() {
        // f3=0.60, f2=0.50: 0.60 + 0.40 × 0.50 × 0.35 = 0.67
        Assert.assertEquals(fuseConfidence(0.5, 0.60), 0.67, 1e-9);
    }

    @Test
    public void formula_knownValue_highSignals() {
        // f3=0.9, f2=0.9: 0.9 + 0.1 × 0.9 × 0.35 = 0.9315
        Assert.assertEquals(fuseConfidence(0.9, 0.9), 0.9315, 1e-9);
    }

    @Test
    public void formula_f3AtOne_cannotBeBoosted() {
        // headroom = (1 − 1.0) = 0 → boost = 0 → stays 1.0 regardless of f2
        Assert.assertEquals(fuseConfidence(1.0, 1.0), 1.0, 1e-9);
        Assert.assertEquals(fuseConfidence(0.5, 1.0), 1.0, 1e-9);
    }

    @Test
    public void formula_notSymmetric_f2AndF3HaveDifferentRoles() {
        // f2 and f3 are semantically distinct: swapping them gives different results
        // fuseConfidence(0.6, 0.8) = 0.8 + 0.2×0.6×0.35 = 0.842
        // fuseConfidence(0.8, 0.6) = 0.6 + 0.4×0.8×0.35 = 0.712
        Assert.assertEquals(fuseConfidence(0.6, 0.8), 0.842, 1e-9);
        Assert.assertEquals(fuseConfidence(0.8, 0.6), 0.712, 1e-9);
        Assert.assertNotEquals(fuseConfidence(0.6, 0.8), fuseConfidence(0.8, 0.6), 1e-9);
    }

    @Test
    public void formula_agreementBoost_clearsThreshold() {
        // Two agreeing mid-signals clear a 0.75 bar that either alone would miss
        // fuseConfidence(0.7, 0.72) = 0.72 + 0.28×0.7×0.35 = 0.7886
        double fused = fuseConfidence(0.7, 0.72);
        Assert.assertTrue(fused > 0.75, "agreeing mid-signals must clear 0.75 threshold: " + fused);
        Assert.assertTrue(fused <= 1.0);
    }

    @Test
    public void formula_bounded_byOne() {
        Assert.assertTrue(fuseConfidence(0.9, 0.9) <= 1.0);
        Assert.assertTrue(fuseConfidence(1.0, 1.0) <= 1.0);
    }

    @Test
    public void floorGuard_highF2_cannotOverrideLowCosine() {
        // Demonstrates the core anti-false-positive property: the cosine gate
        // cannot be bypassed even by a gold-tier strategy hit
        Assert.assertEquals(fuseConfidence(1.0,  0.3),  0.3,  1e-9, "f3=0.30 below floor: unchanged");
        Assert.assertEquals(fuseConfidence(0.95, 0.45), 0.45, 1e-9, "f3=0.45 below floor: unchanged");
    }

    @Test
    public void formula_signalAgreementCap_maxBoostFromFloor() {
        // At f3=0.50 (exactly at floor boundary), max possible boost = (1−0.50)×1.0×0.35 = 0.175
        // So max fused = 0.675 when both signals are perfect
        Assert.assertEquals(fuseConfidence(1.0, 0.50), 0.675, 1e-9);
    }

    @Test
    public void nanF2_floorGuardNotReached() {
        // NaN branch is checked first — even when f3 < floor, NaN f2 returns f3 cleanly
        Assert.assertEquals(fuseConfidence(Double.NaN, 0.3), 0.3, 1e-9);
        Assert.assertEquals(fuseConfidence(Double.NaN, 0.1), 0.1, 1e-9);
    }
}
