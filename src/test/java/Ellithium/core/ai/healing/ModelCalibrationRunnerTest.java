package Ellithium.core.ai.healing;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

public class ModelCalibrationRunnerTest {

    @Test
    public void percentile_emptyList_returnsZero() {
        assertEquals(ModelCalibrationRunner.percentile(Collections.emptyList(), 50), 0.0);
    }

    @Test
    public void percentile_singleElement_returnsElement() {
        List<Double> list = List.of(0.85);
        assertEquals(ModelCalibrationRunner.percentile(list, 10), 0.85);
        assertEquals(ModelCalibrationRunner.percentile(list, 50), 0.85);
        assertEquals(ModelCalibrationRunner.percentile(list, 99), 0.85);
    }

    @Test
    public void percentile_multipleElements_calculatesCorrectRank() {
        List<Double> list = new ArrayList<>(List.of(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));
        Collections.sort(list);

        double p50 = ModelCalibrationRunner.percentile(list, 50);
        assertTrue(p50 >= 0.4 && p50 <= 0.6, "P50 should be around median: " + p50);

        double p99 = ModelCalibrationRunner.percentile(list, 99);
        assertEquals(p99, 1.0);

        double p10 = ModelCalibrationRunner.percentile(list, 10);
        assertEquals(p10, 0.1);
    }

    @Test
    public void round2_and_round4_roundCorrectly() {
        assertEquals(ModelCalibrationRunner.round2(0.12345), 0.12);
        assertEquals(ModelCalibrationRunner.round2(0.126), 0.13);
        assertEquals(ModelCalibrationRunner.round4(0.123456), 0.1235);
        assertEquals(ModelCalibrationRunner.round4(0.12344), 0.1234);
    }

    @Test
    public void computeResult_wellSeparatedScores_yieldsHighThresholdAndPrecision() {
        // Positives cleanly above negatives
        List<Double> positives = Arrays.asList(0.85, 0.88, 0.90, 0.92, 0.95);
        List<Double> negatives = Arrays.asList(0.10, 0.15, 0.20, 0.25, 0.30);

        ModelCalibrationRunner.CalibrationResult result =
                ModelCalibrationRunner.computeResult(positives, negatives);

        assertNotNull(result);
        assertEquals(result.positivePairs, 5);
        assertTrue(result.useThreshold >= 0.30 && result.useThreshold <= 0.85,
                "Use threshold should separate positives from negatives: " + result.useThreshold);
        assertTrue(result.storeThreshold >= result.useThreshold, "Store threshold must be >= use threshold");
        assertEquals(result.precisionAtUse, 1.0, 0.001);
        assertEquals(result.recallAtUse, 1.0, 0.001);
    }

    @Test
    public void computeResult_overlappingScores_boundsStoreThresholdToNegP99() {
        List<Double> positives = Arrays.asList(0.70, 0.75, 0.80, 0.85, 0.90);
        List<Double> negatives = Arrays.asList(0.50, 0.60, 0.70, 0.78, 0.82);

        ModelCalibrationRunner.CalibrationResult result =
                ModelCalibrationRunner.computeResult(positives, negatives);

        assertNotNull(result);
        assertTrue(result.storeThreshold >= result.negativeP99, "Store threshold must be >= negative P99 floor");
        assertTrue(result.useThreshold <= result.storeThreshold, "Use threshold must be <= store threshold");
    }

    @Test
    public void computeResult_emptyPositives_handlesGracefully() {
        List<Double> positives = Collections.emptyList();
        List<Double> negatives = Arrays.asList(0.1, 0.2, 0.3);

        ModelCalibrationRunner.CalibrationResult result =
                ModelCalibrationRunner.computeResult(positives, negatives);

        assertNotNull(result);
        assertEquals(result.positivePairs, 0);
        assertEquals(result.negativePairs, 3);
    }
}
