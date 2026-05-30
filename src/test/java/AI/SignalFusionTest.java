package ai;

import Ellithium.core.ai.scoring.SignalFusion;
import Ellithium.core.ai.scoring.SignalFusion.Candidate;
import Ellithium.core.ai.scoring.SignalFusion.Result;
import Ellithium.core.ai.scoring.SignalFusion.Weights;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static Ellithium.core.ai.scoring.SignalFusion.*;

/**
 * Behaviour spec for the deterministic ensemble fusion (Phase C core). Proves the hybrid:
 * weighted-RRF selection rewards cross-signal agreement, the {@code beta} knob slides between
 * rank-agreement and raw magnitude, missing signals (cold-start) are handled without NaN, and the
 * winner exposes the calibrated anchor score the precision gate uses.
 */
public class SignalFusionTest {

    private static Candidate cand(String id, Double f1, Double f2, Double f3) {
        Map<String, Double> s = new LinkedHashMap<>();
        if (f1 != null) s.put(F1_FINGERPRINT, f1);
        if (f2 != null) s.put(F2_STRATEGY, f2);
        if (f3 != null) s.put(F3_BIENCODER, f3);
        return new Candidate(id, s);
    }

    private static String idOf(SignalFusion.Scored sc) { return (String) sc.candidate.ref; }

    /** A: 2 corroborating signals but only 2nd-best cosine; B: best cosine but lone signal. */
    private List<Candidate> agreementVsLoneHighCosine() {
        return List.of(
                cand("A", 0.90, 0.90, 0.60),   // fingerprint #1 + strategy #1, cosine #2
                cand("B", null, null, 0.80),   // cosine #1 only
                cand("C", 0.50, 0.50, 0.50));  // mediocre everywhere
    }

    @Test
    public void pureRRF_rewardsAgreement_overLoneHighCosine() {
        Weights w = new Weights(Weights.defaults().signalWeights, /*beta*/ 0.0, 60, F3_BIENCODER);
        Result r = SignalFusion.fuse(agreementVsLoneHighCosine(), w);
        Assert.assertEquals(idOf(r.winner()), "A",
                "with beta=0 (pure weighted RRF) the corroborated candidate must beat the lone-high-cosine one");
    }

    @Test
    public void pureMagnitude_picksHighestCosine() {
        Weights w = new Weights(Weights.defaults().signalWeights, /*beta*/ 1.0, 60, F3_BIENCODER);
        Result r = SignalFusion.fuse(agreementVsLoneHighCosine(), w);
        Assert.assertEquals(idOf(r.winner()), "B",
                "with beta=1 (pure magnitude) the highest bi-encoder cosine must win");
    }

    @Test
    public void agreementCount_isNumberOfSignalsRankingCandidateFirst() {
        Result r = SignalFusion.fuse(agreementVsLoneHighCosine(), Weights.defaults());
        Map<String, Integer> agreement = new HashMap<>();
        for (SignalFusion.Scored sc : r.ranked) agreement.put(idOf(sc), sc.agreement);
        Assert.assertEquals((int) agreement.get("A"), 2, "A is #1 in f1 and f2");
        Assert.assertEquals((int) agreement.get("B"), 1, "B is #1 in f3 only");
        Assert.assertEquals((int) agreement.get("C"), 0, "C is never #1");
    }

    @Test
    public void winnerExposesRawAnchorScoreForTheGate() {
        Result r = SignalFusion.fuse(List.of(cand("X", 0.7, 0.7, 0.83)), Weights.defaults());
        Assert.assertEquals(r.winner().anchorScore, 0.83, 1e-9,
                "gate must see the raw bi-encoder cosine, not the fused/normalized score");
    }

    @Test
    public void coldStart_missingFingerprintAndStrategy_isScoredFromRemainingSignals() {
        // No baseline (f1 absent) and no Tier-2 strategy match (f2 absent) — only the bi-encoder spoke.
        List<Candidate> cands = List.of(
                cand("only-cosine", null, null, 0.71),
                cand("weak",        null, null, 0.30));
        Result r = SignalFusion.fuse(cands, Weights.defaults());
        Assert.assertNotNull(r.winner());
        Assert.assertEquals(idOf(r.winner()), "only-cosine");
        for (SignalFusion.Scored sc : r.ranked) {
            Assert.assertFalse(Double.isNaN(sc.fused), "no NaN must leak from a missing signal");
        }
    }

    @Test
    public void crossEncoderAbsent_doesNotAffectFusion() {
        // f4 is in the default weights but never supplied here — it must simply not contribute.
        Result r = SignalFusion.fuse(agreementVsLoneHighCosine(), Weights.defaults());
        Assert.assertNotNull(r.winner());
        Assert.assertEquals(r.ranked.size(), 3);
    }

    @Test
    public void emptyInput_hasNoWinner() {
        Result r = SignalFusion.fuse(Collections.emptyList(), Weights.defaults());
        Assert.assertNull(r.winner());
        Assert.assertTrue(r.ranked.isEmpty());
    }

    @Test
    public void hybrid_betaSlidesBetweenAgreementAndMagnitude() {
        List<Candidate> cands = agreementVsLoneHighCosine();
        // Balanced blend: rank agreement still carries the corroborated candidate A (cosine #2 but
        // fingerprint #1 + strategy #1) over the lone-high-cosine B.
        Weights balanced = new Weights(Weights.defaults().signalWeights, 0.5, 60, F3_BIENCODER);
        Assert.assertEquals(idOf(SignalFusion.fuse(cands, balanced).winner()), "A",
                "beta=0.5: rank agreement should still win");
        // Magnitude-leaning blend: the lone strong cosine B takes over — the knob really does slide.
        Weights magLeaning = new Weights(Weights.defaults().signalWeights, 0.9, 60, F3_BIENCODER);
        Assert.assertEquals(idOf(SignalFusion.fuse(cands, magLeaning).winner()), "B",
                "beta=0.9: raw magnitude should dominate");
    }
}
