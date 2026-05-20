package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.models.ElementFingerprint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CLI tool that calibrates the Tier 3 ONNX similarity threshold for the currently
 * configured model and baseline dataset.
 *
 * <h3>How to run</h3>
 * <pre>
 * java -cp ellithium-*.jar Ellithium.Utilities.ai.ModelCalibrationRunner [licenseKey]
 * </pre>
 * <p>The license key argument is optional — the tool reads {@code ai-config.properties}
 * if none is supplied.</p>
 *
 * <h3>What it does</h3>
 * <ol>
 *   <li>Loads all stored locator-to-fingerprint pairs from {@code healing-baselines.json}.</li>
 *   <li>For each pair, builds a {@link SemanticQueryBuilder} query and calls
 *       {@link ONNXEmbeddingHealer#embed(String)} to get the query vector.</li>
 *   <li>Embeds the corresponding element document string via
 *       {@link ONNXEmbeddingHealer#buildElementDocument} proxy (fingerprint fields).</li>
 *   <li>Computes cosine similarity between the two vectors.</li>
 *   <li>Calculates the 10th-percentile score of true-positive pairs.</li>
 *   <li>Writes {@code Test-Output/calibration-results.json} with the recommended threshold.</li>
 * </ol>
 *
 * <p><b>Pre-requisite:</b> {@code ONNXEmbeddingHealer.isAvailable()} must return {@code true}
 * (model embedded + license key valid). Until then the tool exits early with instructions.</p>
 */
public class ModelCalibrationRunner {

    private static final String BASELINE_FILE   = "Test-Output" + File.separator + "healing-baselines.json";
    private static final String OUTPUT_FILE      = "Test-Output" + File.separator + "calibration-results.json";
    private static final Gson   GSON             = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        System.out.println("=== Ellithium Tier 3 Model Calibration Runner ===");
        System.out.println("Timestamp: " + Instant.now());

        // Optional license key override from CLI argument
        if (args.length > 0 && !args[0].isBlank()) {
            System.setProperty("ellithium.license.key.override", args[0]);
        }

        // Initialise config + ONNX session
        Ellithium.Utilities.ai.config.AIConfigLoader.initialize();
        ONNXEmbeddingHealer.initialize();

        if (!ONNXEmbeddingHealer.isAvailable()) {
            System.err.println("[CALIBRATION] Tier 3 model is not available.");
            System.err.println("  → Ensure ai.license.key is set in ai-config.properties.");
            System.err.println("  → Ensure /ai-models/tier3.onnx.enc is bundled in the JAR.");
            System.err.println("  → Re-run after embedding the fine-tuned model.");
            return;
        }

        // Load baseline store
        Map<String, List<ElementFingerprint>> baselines = loadBaselines();
        if (baselines.isEmpty()) {
            System.err.println("[CALIBRATION] No baselines found in " + BASELINE_FILE
                    + ". Run a test suite first to capture baselines.");
            return;
        }

        System.out.println("[CALIBRATION] Loaded " + baselines.size() + " locator keys ("
                + baselines.values().stream().mapToInt(List::size).sum() + " fingerprints).");

        List<PairScore> scores = computePairScores(baselines);
        if (scores.isEmpty()) {
            System.err.println("[CALIBRATION] No embedding pairs could be scored. "
                    + "Check that the model produces non-null embeddings.");
            return;
        }

        CalibrationResult result = computeResult(scores);
        writeOutput(result);
        printSummary(result);

        ONNXEmbeddingHealer.shutdown();
    }

    // ──────────────────────── Scoring ────────────────────────

    private static List<PairScore> computePairScores(Map<String, List<ElementFingerprint>> baselines) {
        List<PairScore> scores = new ArrayList<>();
        int processed = 0, skipped = 0;

        for (Map.Entry<String, List<ElementFingerprint>> entry : baselines.entrySet()) {
            String locatorKey = entry.getKey();
            List<ElementFingerprint> history = entry.getValue();
            if (history.isEmpty()) continue;

            // Use the most recent fingerprint
            ElementFingerprint fp = history.get(history.size() - 1);

            // Build semantic query from fingerprint fields
            String query = SemanticQueryBuilder.buildFromContext(
                    null, locatorKey, null, fp);

            if (query.isBlank()) { skipped++; continue; }

            float[] queryVec = ONNXEmbeddingHealer.embed(query);
            if (queryVec == null) { skipped++; continue; }

            // Build element document from fingerprint fields
            String elementDoc = buildFingerprintDocument(fp);
            if (elementDoc.isBlank()) { skipped++; continue; }

            float[] elementVec = ONNXEmbeddingHealer.embed(elementDoc);
            if (elementVec == null) { skipped++; continue; }

            double similarity = ONNXEmbeddingHealer.cosineSimilarity(queryVec, elementVec);
            scores.add(new PairScore(locatorKey, query, elementDoc, similarity));
            processed++;
        }

        System.out.println("[CALIBRATION] Scored " + processed + " pairs, skipped " + skipped
                + " (no query or embed failed).");
        return scores;
    }

    private static String buildFingerprintDocument(ElementFingerprint fp) {
        StringBuilder sb = new StringBuilder();
        append(sb, fp.getTagName());
        append(sb, fp.getId());
        append(sb, fp.getName());
        append(sb, fp.getAriaLabel());
        append(sb, fp.getPlaceholder());
        append(sb, fp.getDataTestId());
        append(sb, fp.getRole());
        append(sb, fp.getType());
        if (fp.getText() != null && fp.getText().length() <= 80) append(sb, fp.getText());
        return sb.toString().trim();
    }

    private static void append(StringBuilder sb, String val) {
        if (val != null && !val.isBlank()) sb.append(" ").append(val.trim());
    }

    // ──────────────────────── Result Computation ────────────────────────

    private static CalibrationResult computeResult(List<PairScore> scores) {
        List<Double> sortedScores = scores.stream()
                .map(s -> s.similarity)
                .sorted()
                .collect(Collectors.toList());

        double p10  = percentile(sortedScores, 10);
        double p25  = percentile(sortedScores, 25);
        double p50  = percentile(sortedScores, 50);
        double p90  = percentile(sortedScores, 90);
        double mean = sortedScores.stream().mapToDouble(d -> d).average().orElse(0.0);

        // Recommended threshold = 10th percentile of true-positive scores.
        // Accept any candidate scoring above the worst 10% of known-good pairs.
        double recommendedThreshold = Math.round(p10 * 100.0) / 100.0;

        return new CalibrationResult(
                Instant.now().toString(),
                scores.size(),
                recommendedThreshold,
                p10, p25, p50, p90, mean,
                scores);
    }

    private static double percentile(List<Double> sorted, int p) {
        if (sorted.isEmpty()) return 0.0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }

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
            try (Writer writer = new FileWriter(out)) {
                GSON.toJson(result, writer);
            }
            System.out.println("[CALIBRATION] Results written to " + OUTPUT_FILE);
        } catch (IOException e) {
            System.err.println("[CALIBRATION] Failed to write output: " + e.getMessage());
        }
    }

    private static void printSummary(CalibrationResult result) {
        System.out.println("\n── Calibration Summary ─────────────────────────────────");
        System.out.printf("  Pairs scored:          %d%n", result.totalPairs);
        System.out.printf("  Mean similarity:       %.4f%n", result.meanSimilarity);
        System.out.printf("  P10 / P25 / P50 / P90: %.4f / %.4f / %.4f / %.4f%n",
                result.p10, result.p25, result.p50, result.p90);
        System.out.printf("  Recommended threshold: %.2f%n", result.recommendedThreshold);
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println("→ Set ai.onnx.similarityThreshold=" + result.recommendedThreshold
                + " in ai-config.properties");
    }

    // ──────────────────────── Data Models ────────────────────────

    static class PairScore {
        final String locatorKey;
        final String query;
        final String elementDoc;
        final double similarity;

        PairScore(String locatorKey, String query, String elementDoc, double similarity) {
            this.locatorKey  = locatorKey;
            this.query       = query;
            this.elementDoc  = elementDoc;
            this.similarity  = similarity;
        }
    }

    static class CalibrationResult {
        final String generatedAt;
        final int    totalPairs;
        final double recommendedThreshold;
        final double p10;
        final double p25;
        final double p50;
        final double p90;
        final double meanSimilarity;
        final List<PairScore> pairs;

        CalibrationResult(String generatedAt, int totalPairs, double recommendedThreshold,
                          double p10, double p25, double p50, double p90, double meanSimilarity,
                          List<PairScore> pairs) {
            this.generatedAt           = generatedAt;
            this.totalPairs            = totalPairs;
            this.recommendedThreshold  = recommendedThreshold;
            this.p10                   = p10;
            this.p25                   = p25;
            this.p50                   = p50;
            this.p90                   = p90;
            this.meanSimilarity        = meanSimilarity;
            this.pairs                 = pairs;
        }
    }
}
