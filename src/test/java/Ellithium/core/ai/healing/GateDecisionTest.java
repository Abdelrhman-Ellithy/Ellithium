package Ellithium.core.ai.healing;

import org.testng.Assert;
import org.testng.annotations.Test;

import static Ellithium.core.ai.healing.EnsembleHealer.GateResult;
import static Ellithium.core.ai.healing.EnsembleHealer.decideGate;

/**
 * Behaviour spec for the ensemble accept gate.
 * Path A: fused combined score ≥ threshold → accept.
 * Path B (strategy-rescue): f2 ≥ GATE_STRATEGY_MIN (0.75) AND f3 ≥ GATE_RESCUE_COSINE_FLOOR (0.50).
 * Path-B confidence = mean(f2, f3).
 *
 * GATE_RESCUE_COSINE_FLOOR was raised 0.35→0.45→0.50. Tests use the current 0.50 value.
 */
public class GateDecisionTest {

    private static final double THRESHOLD = 0.40;

    @Test
    public void combinedPath_acceptsWhenScoreClearsThreshold() {
        GateResult g = decideGate(0.55, THRESHOLD, Double.NaN, true, 0.55);
        Assert.assertTrue(g.accept);
        Assert.assertEquals(g.via, "ensemble");
        Assert.assertEquals(g.score, 0.55, 1e-9);
    }

    @Test
    public void rescuePath_goldStrategyCosineCorroboration_heals() {
        // combined=0.30 < threshold; f2=1.0 ≥ 0.75, f3=0.50 ≥ 0.50 → rescue
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.50);
        Assert.assertTrue(g.accept, "gold strategy + cosine ≥ 0.50 must rescue a weak-combined heal");
        Assert.assertEquals(g.via, "strategy-rescue");
        Assert.assertEquals(g.score, (1.0 + 0.50) / 2.0, 1e-9, "rescue confidence = mean(f2, f3)");
    }

    @Test
    public void rescuePath_disabled_fallsThrough() {
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, false, 0.50);
        Assert.assertFalse(g.accept, "rescue disabled → weak combined must not heal");
    }

    @Test
    public void coldStart_cosineAboveFloor_canRescue() {
        // No stored baseline — cosine is the only independent corroboration signal
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.51);
        Assert.assertTrue(g.accept, "cold start with cosine ≥ 0.50 and gold strategy may rescue");
        Assert.assertEquals(g.via, "strategy-rescue");
    }

    @Test
    public void weakStrategy_belowGold_doesNotRescue() {
        // f2=0.50 < GATE_STRATEGY_MIN(0.75) → not eligible for rescue regardless of cosine
        GateResult g = decideGate(0.30, THRESHOLD, 0.50, true, 0.80);
        Assert.assertFalse(g.accept, "only strategy matches with f2 ≥ 0.75 may rescue");
    }

    @Test
    public void cosineBelowFloor_doesNotRescue() {
        // f3=0.355 < 0.50 — the real-world case that caused invalid-element errors at old 0.35 floor
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.355);
        Assert.assertFalse(g.accept, "cosine below 0.50 floor → insufficient corroboration");
    }

    @Test
    public void cosineExactlyAtFloor_rescues() {
        // f3 == GATE_RESCUE_COSINE_FLOOR (0.50) → boundary: f3 >= 0.50 is true → rescues
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.50);
        Assert.assertTrue(g.accept, "cosine exactly at 0.50 floor must rescue");
        Assert.assertEquals(g.score, (1.0 + 0.50) / 2.0, 1e-9);
    }

    @Test
    public void combinedPath_takesPreferenceOverRescue() {
        // combined already clears threshold — Path A wins, rescue path never evaluated
        GateResult g = decideGate(0.45, THRESHOLD, 0.20, true, 0.10);
        Assert.assertTrue(g.accept);
        Assert.assertEquals(g.via, "ensemble");
    }

    @Test
    public void cosineAtOldFloor_0_46_noLongerRescues() {
        // 0.46 was above the old 0.45 floor but is below the current 0.50 floor
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.46);
        Assert.assertFalse(g.accept, "cosine 0.46 is below the current 0.50 floor — must not rescue");
    }

    @Test
    public void cosine_0_49_belowNewFloor_doesNotRescue() {
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.49);
        Assert.assertFalse(g.accept, "cosine 0.49 is just below the 0.50 floor — must not rescue");
    }

    @Test
    public void combinedExactlyAtThreshold_accepts() {
        // combined == threshold → boundary: ≥ not >, so it accepts via ensemble
        GateResult g = decideGate(THRESHOLD, THRESHOLD, Double.NaN, false, Double.NaN);
        Assert.assertTrue(g.accept);
        Assert.assertEquals(g.via, "ensemble");
        Assert.assertEquals(g.score, THRESHOLD, 1e-9);
    }

    @Test
    public void strategyMinBoundary_exactlyAt075_rescues() {
        // GATE_STRATEGY_MIN = 0.75; exactly at boundary must trigger rescue
        GateResult g = decideGate(0.30, THRESHOLD, 0.75, true, 0.50);
        Assert.assertTrue(g.accept, "f2 exactly at GATE_STRATEGY_MIN=0.75 must trigger rescue");
        Assert.assertEquals(g.via, "strategy-rescue");
    }

    @Test
    public void strategyJustBelowMin_0_74_doesNotRescue() {
        GateResult g = decideGate(0.30, THRESHOLD, 0.74, true, 0.80);
        Assert.assertFalse(g.accept, "f2=0.74 just below GATE_STRATEGY_MIN=0.75 must not rescue");
    }

    @Test
    public void cosineNaN_doesNotSatisfyRescue() {
        // NaN >= 0.50 is false in Java → rescue condition fails
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, Double.NaN);
        Assert.assertFalse(g.accept, "NaN cosine cannot satisfy the rescue floor requirement");
    }

    @Test
    public void combinedZero_withGoldAndHighCosine_rescues() {
        // Rescue path works even when combined=0.0 (cold-start or tier-2 bootstrap scenario)
        GateResult g = decideGate(0.0, THRESHOLD, 0.95, true, 0.60);
        Assert.assertTrue(g.accept);
        Assert.assertEquals(g.via, "strategy-rescue");
        Assert.assertEquals(g.score, (0.95 + 0.60) / 2.0, 1e-9);
    }
}
