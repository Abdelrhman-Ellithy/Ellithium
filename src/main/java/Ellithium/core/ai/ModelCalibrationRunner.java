package Ellithium.core.ai;

import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.scoring.SemanticQueryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * CLI tool that calibrates the Tier 3 ONNX similarity thresholds for the currently
 * configured model and baseline dataset — on a PRECISION basis.
 *
 * <h3>Why this is precision-first (and what the old version got wrong)</h3>
 * The previous calibrator recommended the 10th percentile of true-positive scores and never looked
 * at where WRONG elements score — so it could not bound false heals and tended to recommend a
 * dangerously low threshold. It also paired each fingerprint's query against its OWN document
 * (degenerate self-similarity) and built documents in a third, inconsistent format. This version:
 * <ol>
 *   <li>Builds a query per baseline and scores it against the matching document (POSITIVE) and
 *       against a sample of OTHER baselines' documents (NEGATIVES) — a real cross-element negative
 *       distribution.</li>
 *   <li>Uses {@link EnsembleHealer#buildElementDocument(ElementFingerprint)} — the SAME field
 *       order the model sees at train/inference time.</li>
 *   <li>Sweeps a precision/recall curve and recommends two thresholds:
 *       <b>useThreshold</b> = max-F0.5 operating point (precision-weighted), and
 *       <b>storeThreshold</b> = the negative-P99 floor (almost no wrong element clears it).</li>
 * </ol>
 *
 * <h3>How to run</h3>
 * <pre>java -cp ellithium-*.jar Ellithium.Utilities.ai.ModelCalibrationRunner [licenseKey]</pre>
 * Pre-requisite: {@code EnsembleHealer.isAvailable()} must be true (model embedded).
 */
public class ModelCalibrationRunner {

    private static final String BASELINE_FILE = "Test-Output" + File.separator + "healing-baselines.json";
    private static final String OUTPUT_FILE   = "Test-Output" + File.separator + "calibration-results.json";
    private static final Gson   GSON          = new GsonBuilder().setPrettyPrinting().create();

    // How many cross-element negatives to score per positive query.
    private static final int NEG_SAMPLES_PER_QUERY = 12;

    public static void main(String[] args) {
        System.out.println("=== Ellithium Tier 3 Model Calibration Runner (precision-first) ===");
        System.out.println("Timestamp: " + Instant.now());

        if (args.length > 0 && !args[0].isBlank()) {
            System.setProperty("ellithium.license.key.override", args[0]);
        }

        AIConfigLoader.initialize();
        EnsembleHealer.initialize();

        if (!EnsembleHealer.isAvailable()) {
            System.err.println("[CALIBRATION] Tier 3 model is not available — embed the model and re-run.");
            return;
        }

        Map<String, List<ElementFingerprint>> baselines = loadBaselines();
        if (baselines.size() < 2) {
            System.err.println("[CALIBRATION] Need >= 2 baselines for a negative distribution. "
                    + "Run a test suite first to capture baselines.");
            return;
        }
        System.out.println("[CALIBRATION] Loaded " + baselines.size() + " locator keys.");

        // 1. Pre-embed one representative document per baseline (the positive document pool).
        List<Item> items = buildItems(baselines);
        if (items.size() < 2) {
            System.err.println("[CALIBRATION] Too few embeddable baselines.");
            return;
        }

        // 2. Score positives (query vs its own doc) and negatives (query vs other docs).
        List<Double> positives = new ArrayList<>();
        List<Double> negatives = new ArrayList<>();
        Random rng = new Random(42);

        for (Item item : items) {
            // Vectors are already L2-normalized by embed() — dot product == cosine similarity.
            // Using dotProduct matches the runtime scoring path in EnsembleHealer.scoreAndSelectCandidate().
            positives.add((double) dotProduct(item.queryVec, item.docVec));
            int collected = 0;
            int maxAttempts = NEG_SAMPLES_PER_QUERY * 3;  // cap retries to avoid infinite loop on tiny sets
            for (int attempt = 0; collected < NEG_SAMPLES_PER_QUERY && attempt < maxAttempts; attempt++) {
                Item other = items.get(rng.nextInt(items.size()));
                if (other == item) continue;   // retry instead of silently skipping
                negatives.add((double) dotProduct(item.queryVec, other.docVec));
                collected++;
            }
        }
        Collections.sort(positives);
        Collections.sort(negatives);

        CalibrationResult result = computeResult(positives, negatives);
        writeOutput(result);
        printSummary(result);

        EnsembleHealer.shutdown();
    }

    // ──────────────────────── Item construction ────────────────────────

    private static List<Item> buildItems(Map<String, List<ElementFingerprint>> baselines) {
        List<Item> items = new ArrayList<>();
        int skipped = 0;
        for (Map.Entry<String, List<ElementFingerprint>> e : baselines.entrySet()) {
            List<ElementFingerprint> history = e.getValue();
            if (history.isEmpty()) continue;
            ElementFingerprint fp = history.get(history.size() - 1);

            String query = SemanticQueryBuilder.buildFromContext(null, e.getKey(), null, fp);
            String doc   = EnsembleHealer.buildElementDocument(fp);   // shared, canonical format
            if (query.isBlank() || doc.isBlank()) { skipped++; continue; }

            float[] qv = EnsembleHealer.embed(query, true);
            float[] dv = EnsembleHealer.embed(doc, false);
            if (qv == null || dv == null) { skipped++; continue; }

            items.add(new Item(qv, dv));
        }
        System.out.println("[CALIBRATION] Embedded " + items.size() + " items, skipped " + skipped + ".");
        return items;
    }

    // ──────────────────────── PR-curve threshold selection ────────────────────────

    private static CalibrationResult computeResult(List<Double> positives, List<Double> negatives) {
        double posP10 = percentile(positives, 10);
        double posP50 = percentile(positives, 50);
        double negP95 = percentile(negatives, 95);
        double negP99 = percentile(negatives, 99);
        double posMean = positives.stream().mapToDouble(d -> d).average().orElse(0);
        double negMean = negatives.stream().mapToDouble(d -> d).average().orElse(0);

        // Sweep thresholds; pick the one maximizing F0.5 (precision weighted 2x recall).
        double bestF = -1, bestT = 0.5, bestP = 0, bestR = 0;
        for (int i = 0; i <= 100; i++) {
            double t = i / 100.0;
            long tp = positives.stream().filter(p -> p >= t).count();
            long fn = positives.size() - tp;
            long fp = negatives.stream().filter(nv -> nv >= t).count();
            if (tp == 0) continue;
            double precision = tp / (double) (tp + fp);
            double recall    = tp / (double) (tp + fn);
            double beta2 = 0.25; // F0.5
            double denom = (beta2 * precision) + recall;
            double f = denom == 0 ? 0 : (1 + beta2) * (precision * recall) / denom;
            if (f > bestF) { bestF = f; bestT = t; bestP = precision; bestR = recall; }
        }

        double useThreshold   = round2(bestT);
        // Store/patch bar: above almost all wrong elements. Never below the F0.5 use point.
        double storeThreshold = round2(Math.max(bestT, negP99));

        return new CalibrationResult(Instant.now().toString(),
                positives.size(), negatives.size(),
                useThreshold, storeThreshold,
                round4(bestP), round4(bestR), round4(bestF),
                round4(posP10), round4(posP50), round4(posMean),
                round4(negMean), round4(negP95), round4(negP99));
    }

    private static double percentile(List<Double> sorted, int p) {
        if (sorted.isEmpty()) return 0.0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }

    // ──────────────────────── I/O ────────────────────────

    private static Map<String, List<ElementFingerprint>> loadBaselines() {
        Path path = Paths.get(BASELINE_FILE);
        if (!Files.exists(path)) return Map.of();
        try (Reader reader = new FileReader(path.toFile())) {
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return Map.of();
            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<
                    Map<String, List<ElementFingerprint>>>() {}.getType();
            Map<String, List<ElementFingerprint>> map = GSON.fromJson(root, mapType);
            return map != null ? map : Map.of();
        } catch (Exception e) {
            System.err.println("[CALIBRATION] Failed to load baselines: " + e.getMessage());
            return Map.of();
        }
    }

    private static void writeOutput(CalibrationResult result) {
        try {
            File out = new File(OUTPUT_FILE);
            out.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(out)) { GSON.toJson(result, writer); }
            System.out.println("[CALIBRATION] Results written to " + OUTPUT_FILE);
        } catch (IOException e) {
            System.err.println("[CALIBRATION] Failed to write output: " + e.getMessage());
        }
    }

    private static void printSummary(CalibrationResult r) {
        System.out.println("\n── Calibration Summary (precision-first) ───────────────");
        System.out.printf("  Positives: %d   Negatives: %d%n", r.positivePairs, r.negativePairs);
        System.out.printf("  Positive mean=%.4f P10=%.4f P50=%.4f%n", r.positiveMean, r.positiveP10, r.positiveP50);
        System.out.printf("  Negative mean=%.4f P95=%.4f P99=%.4f%n", r.negativeMean, r.negativeP95, r.negativeP99);
        System.out.printf("  F0.5-optimal: t=%.2f (precision=%.4f recall=%.4f F0.5=%.4f)%n",
                r.useThreshold, r.precisionAtUse, r.recallAtUse, r.f05AtUse);
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println("→ ai.onnx.similarityThreshold = " + r.useThreshold + "   (use: accept a heal for the action)");
        System.out.println("→ ai.healing.storeThreshold    = " + r.storeThreshold + "   (persist baseline + patch source)");
        if (r.negativeP95 >= r.useThreshold) {
            System.out.println("  WARNING: wrong elements score above the use threshold (neg-P95 >= use). "
                    + "The bi-encoder cannot separate these classes alone — add the cross-encoder reranker.");
        }
    }

    // ──────────────────────── Math ────────────────────────

    /** Dot product on L2-normalised vectors (== cosine similarity). Delegates to EnsembleHealer. */
    private static double dotProduct(float[] a, float[] b) {
        return EnsembleHealer.dotProduct(a, b);
    }

    // ──────────────────────── Data Models ────────────────────────

    private static class Item {
        final float[] queryVec, docVec;
        Item(float[] queryVec, float[] docVec) {
            this.queryVec = queryVec; this.docVec = docVec;
        }
    }

    static class CalibrationResult {
        final String generatedAt;
        final int positivePairs, negativePairs;
        final double useThreshold, storeThreshold;
        final double precisionAtUse, recallAtUse, f05AtUse;
        final double positiveP10, positiveP50, positiveMean;
        final double negativeMean, negativeP95, negativeP99;

        CalibrationResult(String generatedAt, int positivePairs, int negativePairs,
                          double useThreshold, double storeThreshold,
                          double precisionAtUse, double recallAtUse, double f05AtUse,
                          double positiveP10, double positiveP50, double positiveMean,
                          double negativeMean, double negativeP95, double negativeP99) {
            this.generatedAt = generatedAt;
            this.positivePairs = positivePairs; this.negativePairs = negativePairs;
            this.useThreshold = useThreshold; this.storeThreshold = storeThreshold;
            this.precisionAtUse = precisionAtUse; this.recallAtUse = recallAtUse; this.f05AtUse = f05AtUse;
            this.positiveP10 = positiveP10; this.positiveP50 = positiveP50; this.positiveMean = positiveMean;
            this.negativeMean = negativeMean; this.negativeP95 = negativeP95; this.negativeP99 = negativeP99;
        }
    }
}
