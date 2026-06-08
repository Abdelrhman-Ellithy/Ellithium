package AI;

import Ellithium.core.ai.HealingTelemetryStore;
import Ellithium.core.ai.HealingTelemetryStore.TelemetryRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Covers B2 (Tier-3 telemetry context: query/category) and B3 (the false-heal detector, R9).
 * The false-heal detector is the only production ground-truth precision signal — a heal that was
 * USED in a test that later FAILED — so it ships with coverage.
 */
public class HealingTelemetryTest {

    @BeforeMethod
    public void reset() {
        HealingTelemetryStore.clear();
        HealingTelemetryStore.clearCurrentTest();
    }

    private TelemetryRecord only() {
        List<TelemetryRecord> all = HealingTelemetryStore.getAllRecords();
        Assert.assertEquals(all.size(), 1, "expected exactly one record");
        return all.get(0);
    }

    @Test
    public void recordCapturesQueryCategoryAndCurrentTest() {
        HealingTelemetryStore.setCurrentTest("Pages.LoginTest.loginValid");
        HealingTelemetryStore.record(3, "By.id: loginBtn", "submit button", 0.91, true,
                "click press button login btn", "CLICKABLE");
        TelemetryRecord r = only();
        Assert.assertEquals(r.query, "click press button login btn");
        Assert.assertEquals(r.category, "CLICKABLE");
        Assert.assertEquals(r.testId, "Pages.LoginTest.loginValid");
        Assert.assertTrue(r.success);
        Assert.assertFalse(r.suspectWrongHeal);
    }

    @Test
    public void markTestFailed_flagsUsedHealInThatTest() {
        HealingTelemetryStore.setCurrentTest("T.a");
        HealingTelemetryStore.record(3, "By.id: msg", "h2 login page", 0.74, true, "q", "READABLE");

        int flagged = HealingTelemetryStore.markTestFailed("T.a");
        Assert.assertEquals(flagged, 1, "the used heal in the failed test must be flagged");
        Assert.assertTrue(only().suspectWrongHeal);
    }

    @Test
    public void markTestFailed_doesNotFlagFellThroughHeals() {
        HealingTelemetryStore.setCurrentTest("T.a");
        // success=false → fell through to the next tier; not a wrong heal (it was never used).
        HealingTelemetryStore.record(3, "By.id: msg", "best below threshold", 0.31, false, "q", "READABLE");

        int flagged = HealingTelemetryStore.markTestFailed("T.a");
        Assert.assertEquals(flagged, 0);
        Assert.assertFalse(only().suspectWrongHeal);
    }

    @Test
    public void markTestFailed_isScopedToTheOwningTest() {
        HealingTelemetryStore.setCurrentTest("T.a");
        HealingTelemetryStore.record(1, "By.id: x", "y", 0.95, true, null, null);
        HealingTelemetryStore.setCurrentTest("T.b");
        HealingTelemetryStore.record(3, "By.id: z", "w", 0.88, true, null, null);

        int flagged = HealingTelemetryStore.markTestFailed("T.a");
        Assert.assertEquals(flagged, 1, "only the heal owned by the failed test is flagged");
        for (TelemetryRecord r : HealingTelemetryStore.getAllRecords()) {
            Assert.assertEquals(r.suspectWrongHeal, "T.a".equals(r.testId));
        }
    }

    @Test
    public void markTestFailed_isIdempotent() {
        HealingTelemetryStore.setCurrentTest("T.a");
        HealingTelemetryStore.record(4, "By.id: x", "y", 0.99, true, null, null);
        Assert.assertEquals(HealingTelemetryStore.markTestFailed("T.a"), 1);
        Assert.assertEquals(HealingTelemetryStore.markTestFailed("T.a"), 0, "already-flagged not recounted");
    }
}
