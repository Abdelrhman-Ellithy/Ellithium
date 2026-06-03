package Ellithium.core.ai;

import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.scoring.SemanticQueryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Phase B4 — Tier 3 measurement runner / SHIP GATE.
 *
 * <p>Runs the embedded bi-encoder over the real-page validation mini-set
 * ({@code src/test/resources/ai/tier3-validation-miniset.json}, B1) and reports, per category and
 * overall: <b>Recall@1/3/5</b> (is the correct element ranked first / in top-K among the page's
 * candidates) and the <b>coverage at ≥99% precision</b> (what fraction of correct heals clear a
 * threshold that almost no wrong candidate clears) — its complement is the <b>fallthrough rate</b>.
 *
 * <p>This is the decision instrument for B5: build the cross-encoder ONLY if gate-alone bi-encoder
 * fallthrough at ≥99% precision is too high. Synthetic-only numbers don't count — this set is the bar.
 *
 * <p>Placed in package {@code Ellithium.Utilities.ai} so it can call the package-private
 * {@link EnsembleHealer#embed} on the exact serving path. Skips cleanly (no failure) when no
 * model is embedded, so CI without the model stays green; run it after embedding a retrained model:
 * <pre>mvn -o test -Dtest=Tier3ValidationTest</pre>
 */
public class EnsembleValidationTest {

    private static final String MINISET_RESOURCE = "/ai/tier3-validation-miniset.json";
    private static final String REPORT_FILE =
            "Test-Output" + File.separator + "tier3-validation-report.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int[] RECALL_KS = {1, 3, 5};
    private static final double PRECISION_TARGET = 0.99;

    @BeforeClass
    public void setUp() {
        AIConfigLoader.initialize();
        EnsembleHealer.initialize();
    }

    @AfterClass
    public void tearDown() {
        EnsembleHealer.shutdown();
    }

    @Test
    public void measureEnsembleOnRealMiniSet() throws IOException {
        if (!EnsembleHealer.isAvailable()) {
            throw new SkipException("Tier 3 model not embedded — skipping the validation mini-set "
                    + "measurement. Embed a model into src/main/resources/Ellithium-ai-model/ and re-run.");
        }

        MiniSet miniSet = loadMiniSet();
        if (miniSet == null || miniSet.entries == null || miniSet.entries.isEmpty()) {
            throw new SkipException("Validation mini-set is empty/unreadable: " + MINISET_RESOURCE);
        }

        List<EntryResult> results = new ArrayList<>();
        for (Entry e : miniSet.entries) {
            EntryResult r = scoreEntry(e);
            if (r != null) results.add(r);
        }
        if (results.isEmpty()) {
            throw new SkipException("No mini-set entry produced an embeddable query/candidate set.");
        }

        Report report = buildReport(results);
        writeReport(report);
        printReport(report);

        // Not a hard pass/fail gate yet — the acceptance bar (X% coverage) is set in B5 after reading
        // these numbers. We only assert the run actually measured something usable.
        org.testng.Assert.assertTrue(report.overall.entries > 0, "no entries measured");
    }

    // ──────────────────────── Scoring ────────────────────────

    private EntryResult scoreEntry(Entry e) {
        Map<String, String> fp = e.fingerprint != null ? e.fingerprint : Map.of();
        boolean readable = SemanticLocatorResolver.ElementCategory.READABLE
                == SemanticLocatorResolver.categorizeAction(e.action);
        // Mirror SemanticQueryBuilder.buildFromContext: READABLE drops last-known text (de-poisoning).
        String fpText = readable ? null : fp.get("text");
        String query = SemanticQueryBuilder.build(e.action, e.brokenLocator, e.method, null,
                fpText, fp.get("id"), fp.get("aria-label"), fp.get("placeholder"),
                fp.get("data-testid"), fp.get("tag"));
        if (query.isBlank()) return null;

        float[] qv = EnsembleHealer.embed(query, true);
        if (qv == null) return null;

        ElementFingerprint baseline = fingerprintFromMap(fp);

        // Score every candidate using the same inline formula as EnsembleHealer:
        // combined = cosine (f3) when no strategy signal (f2=NaN offline), f1 used as tiebreak.
        record ScoredCand(Map<String, Object> cand, double combined, double cosine, double f1, boolean correct) {}
        List<ScoredCand> cands = new ArrayList<>();
        for (Map<String, Object> cand : e.candidates) {
            boolean isCorrect = Boolean.TRUE.equals(cand.get("correct"));
            Map<String, String> attrs = toAttrs(cand);
            String doc = EnsembleHealer.buildElementDocument(attrs);
            if (doc.isBlank()) continue;
            float[] dv = EnsembleHealer.embed(doc, false);
            if (dv == null) continue;
            double cosine = cosine(qv, dv);
            double f1 = (baseline != null) ? baseline.scoreSimilarity(toObjAttrs(cand)) : Double.NaN;
            // f2 is unavailable offline (no live DOM strategy match) → combined = cosine
            double combined = cosine;
            cands.add(new ScoredCand(cand, combined, cosine, f1, isCorrect));
        }
        if (cands.isEmpty()) return null;

        cands.sort((a, b) -> {
            int c = Double.compare(b.combined(), a.combined());
            if (c != 0) return c;
            return Double.compare(Double.isNaN(b.f1()) ? -1 : b.f1(), Double.isNaN(a.f1()) ? -1 : a.f1());
        });

        double correctFused = Double.NEGATIVE_INFINITY, correctCosine = Double.NEGATIVE_INFINITY;
        List<Double> wrongScores = new ArrayList<>();
        int correctCount = 0, rank = 1;
        for (ScoredCand sc : cands) {
            if (sc.correct()) {
                correctFused  = Math.max(correctFused,  sc.combined());
                correctCosine = Math.max(correctCosine, sc.cosine());
                correctCount++;
            } else {
                wrongScores.add(sc.cosine());
            }
        }
        if (correctCount == 0) return null;
        for (ScoredCand sc : cands) {
            if (!sc.correct() && sc.combined() > correctFused) rank++;
        }

        EntryResult r = new EntryResult();
        r.name = e.name;
        r.category = e.category;
        r.query = query;
        r.correctScore = round4(correctCosine);
        r.bestWrongScore = round4(wrongScores.stream().mapToDouble(d -> d).max().orElse(0.0));
        r.rank = rank;
        r.wrongScores = wrongScores;
        return r;
    }

    /** Builds an ElementFingerprint from the mini-set's last-known attribute map (Gson re-key). */
    private static ElementFingerprint fingerprintFromMap(Map<String, String> fp) {
        if (fp == null || fp.isEmpty()) return null;
        Map<String, String> reKeyed = new LinkedHashMap<>();
        putIf(reKeyed, "id", fp.get("id"));
        putIf(reKeyed, "name", fp.get("name"));
        putIf(reKeyed, "tagName", fp.get("tag"));
        putIf(reKeyed, "ariaLabel", fp.get("aria-label"));
        putIf(reKeyed, "placeholder", fp.get("placeholder"));
        putIf(reKeyed, "dataTestId", fp.get("data-testid"));
        putIf(reKeyed, "text", fp.get("text"));
        putIf(reKeyed, "resourceId", fp.get("resource-id"));
        putIf(reKeyed, "accessibilityId", fp.get("accessibility-id"));
        putIf(reKeyed, "contentDesc", fp.get("content-desc"));
        if (reKeyed.isEmpty()) return null;
        return GSON.fromJson(GSON.toJson(reKeyed), ElementFingerprint.class);
    }

    private static void putIf(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v);
    }

    private static Map<String, Object> toObjAttrs(Map<String, Object> cand) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : cand.entrySet()) {
            if (!"correct".equals(e.getKey())) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    // ──────────────────────── Aggregation ────────────────────────

    private Report buildReport(List<EntryResult> results) {
        Report report = new Report();
        report.generatedAt = Instant.now().toString();
        report.useThreshold = AIConfigLoader.getOnnxSimilarityThreshold();
        report.readableThreshold = AIConfigLoader.getOnnxReadableThreshold();
        report.overall = summarize(results);

        Map<String, List<EntryResult>> byCat = new TreeMap<>();
        for (EntryResult r : results) byCat.computeIfAbsent(r.category, k -> new ArrayList<>()).add(r);
        report.byCategory = new LinkedHashMap<>();
        for (Map.Entry<String, List<EntryResult>> e : byCat.entrySet()) {
            report.byCategory.put(e.getKey(), summarize(e.getValue()));
        }
        report.entries = results;
        report.misses = new ArrayList<>();
        for (EntryResult r : results) if (r.rank > 1) report.misses.add(r.name + " (rank " + r.rank + ")");
        return report;
    }

    private CategorySummary summarize(List<EntryResult> rs) {
        CategorySummary s = new CategorySummary();
        s.entries = rs.size();
        for (int k : RECALL_KS) {
            long hit = rs.stream().filter(r -> r.rank <= k).count();
            s.recallAtK.put("R@" + k, round4((double) hit / rs.size()));
        }
        // Pooled PR over positives (one correct score per entry) and negatives (all wrong scores).
        List<Double> positives = new ArrayList<>();
        List<Double> negatives = new ArrayList<>();
        for (EntryResult r : rs) {
            positives.add(r.correctScore);
            negatives.addAll(r.wrongScores);
        }
        // Lowest threshold reaching >=99% precision; coverage there = recall = fraction of correct
        // heals that still clear it. Fallthrough = 1 - coverage (they'd defer to Tier 4 instead of
        // a confident wrong heal). Step finely so the operating point is accurate.
        double bestCoverage = 0.0, chosenT = 1.01;
        for (int i = 0; i <= 200; i++) {
            double t = i / 200.0;
            long tp = positives.stream().filter(p -> p >= t).count();
            long fpc = negatives.stream().filter(n -> n >= t).count();
            if (tp == 0) continue;
            double precision = tp / (double) (tp + fpc);
            if (precision >= PRECISION_TARGET) {
                double coverage = tp / (double) positives.size();
                if (coverage > bestCoverage) { bestCoverage = coverage; chosenT = t; }
            }
        }
        s.thresholdAt99Precision = round4(chosenT > 1.0 ? Double.NaN : chosenT);
        s.coverageAt99Precision = round4(bestCoverage);
        s.fallthroughAt99Precision = round4(1.0 - bestCoverage);
        s.meanCorrectScore = round4(positives.stream().mapToDouble(d -> d).average().orElse(0));
        s.meanWrongScore = round4(negatives.stream().mapToDouble(d -> d).average().orElse(0));
        return s;
    }

    // ──────────────────────── I/O + helpers ────────────────────────

    private MiniSet loadMiniSet() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(MINISET_RESOURCE)) {
            if (is == null) return null;
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, MiniSet.class);
            }
        }
    }

    private void writeReport(Report report) {
        try {
            File out = new File(REPORT_FILE);
            if (out.getParentFile() != null) out.getParentFile().mkdirs();
            try (Writer w = new FileWriter(out)) { GSON.toJson(report, w); }
            System.out.println("[TIER3-VALIDATION] Report written to " + REPORT_FILE);
        } catch (IOException e) {
            System.err.println("[TIER3-VALIDATION] Failed to write report: " + e.getMessage());
        }
    }

    private void printReport(Report r) {
        System.out.println("\n══ Tier 3 Validation Mini-Set (real-page ship gate) ══════════════");
        System.out.printf("  Entries: %d   useThreshold=%.2f readableThreshold=%.2f%n",
                r.overall.entries, r.useThreshold, r.readableThreshold);
        printSummaryLine("OVERALL", r.overall);
        for (Map.Entry<String, CategorySummary> e : r.byCategory.entrySet()) {
            printSummaryLine(e.getKey(), e.getValue());
        }
        if (!r.misses.isEmpty()) {
            System.out.println("  Misses (correct not ranked #1): " + String.join(", ", r.misses));
        }
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("  B5 decision input: if fallthrough@99%-precision is high (esp. READABLE),");
        System.out.println("  the bi-encoder alone can't separate those classes → build the cross-encoder.");
        System.out.println("═════════════════════════════════════════════════════════════════");
    }

    private void printSummaryLine(String label, CategorySummary s) {
        System.out.printf("  %-10s n=%-2d  R@1=%.2f R@3=%.2f R@5=%.2f  cover@99%%P=%.2f fallthrough=%.2f  "
                        + "posMean=%.3f negMean=%.3f%n",
                label, s.entries, s.recallAtK.getOrDefault("R@1", 0.0),
                s.recallAtK.getOrDefault("R@3", 0.0), s.recallAtK.getOrDefault("R@5", 0.0),
                s.coverageAt99Precision, s.fallthroughAt99Precision,
                s.meanCorrectScore, s.meanWrongScore);
    }

    private static Map<String, String> toAttrs(Map<String, Object> cand) {
        Map<String, String> attrs = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : cand.entrySet()) {
            if ("correct".equals(e.getKey()) || e.getValue() == null) continue;
            attrs.put(e.getKey(), String.valueOf(e.getValue()));
        }
        return attrs;
    }

    /** Cosine via dot product — embed() returns L2-normalised vectors, so this == cosine. */
    private static double cosine(float[] a, float[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) sum += (double) a[i] * b[i];
        return sum;
    }

    private static double round4(double v) {
        if (Double.isNaN(v)) return Double.NaN;
        return Math.round(v * 10000.0) / 10000.0;
    }

    // ──────────────────────── JSON models ────────────────────────

    private static class MiniSet { List<Entry> entries; }

    private static class Entry {
        String name, sector, category, action, method, brokenLocator;
        Map<String, String> fingerprint;
        List<Map<String, Object>> candidates;
    }

    static class EntryResult {
        String name, category, query;
        double correctScore, bestWrongScore;
        int rank;
        transient List<Double> wrongScores;   // not serialized (verbose) — used only for aggregation
    }

    static class CategorySummary {
        int entries;
        final Map<String, Double> recallAtK = new LinkedHashMap<>();
        double thresholdAt99Precision, coverageAt99Precision, fallthroughAt99Precision;
        double meanCorrectScore, meanWrongScore;
    }

    static class Report {
        String generatedAt;
        double useThreshold, readableThreshold;
        CategorySummary overall;
        Map<String, CategorySummary> byCategory;
        List<EntryResult> entries;
        List<String> misses;
    }
}
