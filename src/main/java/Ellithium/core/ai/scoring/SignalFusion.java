package Ellithium.core.ai.scoring;

import java.util.*;

/** Deterministic multi-signal fusion. See ai-context for design notes. */
public final class SignalFusion {

    private SignalFusion() {}

    public static final String F1_FINGERPRINT = "f1";
    public static final String F2_STRATEGY    = "f2";
    public static final String F3_BIENCODER   = "f3";

    public static final class Weights {
        public final Map<String, Double> signalWeights;
        public final double beta;
        public final int    k;
        public final String anchorSignal;

        public Weights(Map<String, Double> signalWeights, double beta, int k, String anchorSignal) {
            this.signalWeights = Collections.unmodifiableMap(new LinkedHashMap<>(signalWeights));
            this.beta = clamp01(beta);
            this.k = Math.max(1, k);
            this.anchorSignal = anchorSignal;
        }

        public static Weights defaults() {
            Map<String, Double> w = new LinkedHashMap<>();
            w.put(F1_FINGERPRINT, 0.8);
            w.put(F2_STRATEGY,    0.7);
            w.put(F3_BIENCODER,   1.5);
            return new Weights(w, 0.65, 60, F3_BIENCODER);
        }

        double weightOf(String signal) {
            return signalWeights.getOrDefault(signal, 0.0);
        }
    }

    public static final class Candidate {
        public final Object ref;
        public final Map<String, Double> signals;

        public Candidate(Object ref, Map<String, Double> signals) {
            this.ref = ref;
            this.signals = Collections.unmodifiableMap(new LinkedHashMap<>(signals));
        }

        double score(String signal) {
            Double v = signals.get(signal);
            return (v == null || v.isNaN()) ? Double.NaN : v;
        }
        boolean has(String signal) { return !Double.isNaN(score(signal)); }
    }

    public static final class Scored {
        public final Candidate candidate;
        public final double fused;
        public final double anchorScore;
        public final int    agreement;

        Scored(Candidate candidate, double fused, double anchorScore, int agreement) {
            this.candidate = candidate;
            this.fused = fused;
            this.anchorScore = anchorScore;
            this.agreement = agreement;
        }
    }

    public static final class Result {
        public final List<Scored> ranked;
        Result(List<Scored> ranked) { this.ranked = Collections.unmodifiableList(ranked); }
        public Scored winner() { return ranked.isEmpty() ? null : ranked.get(0); }
    }

    public static Result fuse(List<Candidate> candidates, Weights w) {
        if (candidates == null || candidates.isEmpty()) return new Result(Collections.emptyList());
        if (w == null) w = Weights.defaults();

        Set<String> activeSignals = new LinkedHashSet<>();
        for (Candidate c : candidates) for (String s : c.signals.keySet()) if (c.has(s)) activeSignals.add(s);

        Map<String, Map<Candidate, Integer>> ranksBySignal = new HashMap<>();
        for (String s : activeSignals) ranksBySignal.put(s, ranksForSignal(candidates, s));

        double[] rrf = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            double sum = 0.0;
            for (String s : activeSignals) {
                Integer rank = ranksBySignal.get(s).get(c);
                if (rank != null) sum += w.weightOf(s) / (w.k + rank);
            }
            rrf[i] = sum;
        }
        double[] rrfNorm = minMaxNormalize(rrf);

        List<Scored> scored = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            double anchor = c.has(w.anchorSignal) ? c.score(w.anchorSignal) : 0.0;
            double magNorm = clamp01(anchor);
            double fused = w.beta * magNorm + (1.0 - w.beta) * rrfNorm[i];

            int agreement = 0;
            for (String s : activeSignals) {
                Integer rank = ranksBySignal.get(s).get(c);
                if (rank != null && rank == 1) agreement++;
            }
            scored.add(new Scored(c, fused, anchor, agreement));
        }

        scored.sort((a, b) -> {
            int byFused = Double.compare(b.fused, a.fused);
            if (byFused != 0) return byFused;
            int byAnchor = Double.compare(b.anchorScore, a.anchorScore);
            if (byAnchor != 0) return byAnchor;
            return Integer.compare(b.agreement, a.agreement);
        });
        return new Result(scored);
    }

    private static Map<Candidate, Integer> ranksForSignal(List<Candidate> candidates, String signal) {
        List<Candidate> present = new ArrayList<>();
        for (Candidate c : candidates) if (c.has(signal)) present.add(c);
        present.sort((a, b) -> Double.compare(b.score(signal), a.score(signal)));
        Map<Candidate, Integer> ranks = new IdentityHashMap<>();
        int rank = 0, idx = 0;
        Double prev = null;
        for (Candidate c : present) {
            idx++;
            double sc = c.score(signal);
            if (prev == null || sc != prev) { rank = idx; prev = sc; }
            ranks.put(c, rank);
        }
        return ranks;
    }

    private static double[] minMaxNormalize(double[] xs) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double x : xs) { min = Math.min(min, x); max = Math.max(max, x); }
        double[] out = new double[xs.length];
        double range = max - min;
        if (range <= 1e-12) { Arrays.fill(out, 0.5); return out; }
        for (int i = 0; i < xs.length; i++) out[i] = (xs[i] - min) / range;
        return out;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
