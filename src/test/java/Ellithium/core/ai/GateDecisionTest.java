package Ellithium.core.ai;

import org.testng.Assert;
import org.testng.annotations.Test;

import static Ellithium.core.ai.EnsembleHealer.GateResult;
import static Ellithium.core.ai.EnsembleHealer.decideGate;

/**
 * Behaviour spec for the ensemble accept gate.
 * Path A = fused combined score clears the calibrated threshold.
 * Path B (strategy-rescue) = gold-tier f2 (≥0.95) corroborated by bi-encoder cosine f3 (≥0.45) —
 * two genuinely independent signals. Cold start (no baseline) can rescue when cosine ≥ floor.
 * Floor raised 0.35→0.45: neg-P95=0.347 proved 0.35 admitted wrong-type elements (invalid-element
 * state errors where strategy matched attributes of a non-interactable element).
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
        // combined=0.30 < threshold; gold f2=1.0 + cosine f3=0.50 ≥ 0.45 → rescue
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.50);
        Assert.assertTrue(g.accept, "gold strategy + cosine ≥ 0.45 must rescue a weak-combined heal");
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
        // No baseline (f1 is no longer a gate) — cosine is the independent corroboration signal
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.46);
        Assert.assertTrue(g.accept, "cold start with cosine ≥ 0.45 and gold strategy may rescue");
        Assert.assertEquals(g.via, "strategy-rescue");
    }

    @Test
    public void weakStrategy_belowGold_doesNotRescue() {
        // f2=0.50 (bronze) — not high-precision enough for rescue regardless of cosine
        GateResult g = decideGate(0.30, THRESHOLD, 0.50, true, 0.80);
        Assert.assertFalse(g.accept, "only gold-tier (f2≥0.95) strategy matches may rescue");
    }

    @Test
    public void cosineBelowFloor_doesNotRescue() {
        // f3=0.355 < 0.45 — this was the real-world case that caused invalid-element errors
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.355);
        Assert.assertFalse(g.accept, "cosine below 0.45 floor → insufficient independent corroboration");
    }

    @Test
    public void cosineExactlyAtFloor_rescues() {
        GateResult g = decideGate(0.30, THRESHOLD, 1.0, true, 0.45);
        Assert.assertTrue(g.accept, "cosine exactly at 0.45 floor must rescue");
        Assert.assertEquals(g.score, (1.0 + 0.45) / 2.0, 1e-9);
    }

    @Test
    public void combinedPath_takesPreferenceOverRescue() {
        // combined already clears threshold — Path A wins, rescue path never evaluated
        GateResult g = decideGate(0.45, THRESHOLD, 0.20, true, 0.10);
        Assert.assertTrue(g.accept);
        Assert.assertEquals(g.via, "ensemble");
    }
}
