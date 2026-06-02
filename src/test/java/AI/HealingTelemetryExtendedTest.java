package ai;

import Ellithium.core.ai.HealingTelemetryStore;
import Ellithium.core.ai.HealingTelemetryStore.TelemetryRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

public class HealingTelemetryExtendedTest {

    @BeforeMethod
    public void reset() {
        HealingTelemetryStore.clear();
        HealingTelemetryStore.clearCurrentTest();
    }

    @Test
    public void setCurrentTest_bindsTelemetryToThread() {
        HealingTelemetryStore.setCurrentTest("my.Test");
        HealingTelemetryStore.record(1, "By.id: a", "By.id: b", 0.9, true);
        TelemetryRecord r = HealingTelemetryStore.getAllRecords().get(0);
        Assert.assertEquals(r.testId, "my.Test");
    }

    @Test
    public void setCurrentTest_null_clearsBinding() {
        HealingTelemetryStore.setCurrentTest("my.Test");
        HealingTelemetryStore.setCurrentTest(null);
        HealingTelemetryStore.record(1, "By.id: a", "By.id: b", 0.9, true);
        TelemetryRecord r = HealingTelemetryStore.getAllRecords().get(0);
        Assert.assertNull(r.testId);
    }

    @Test
    public void clearCurrentTest_removesBinding() {
        HealingTelemetryStore.setCurrentTest("my.Test");
        HealingTelemetryStore.clearCurrentTest();
        HealingTelemetryStore.record(1, "By.id: a", "By.id: b", 0.9, true);
        TelemetryRecord r = HealingTelemetryStore.getAllRecords().get(0);
        Assert.assertNull(r.testId);
    }

    @Test
    public void record_5arg_delegatesWithNullQueryAndCategory() {
        HealingTelemetryStore.record(2, "By.name: q", "By.id: q", 0.82, false);
        TelemetryRecord r = HealingTelemetryStore.getAllRecords().get(0);
        Assert.assertEquals(r.tier, 2);
        Assert.assertEquals(r.brokenLocator, "By.name: q");
        Assert.assertEquals(r.healedLocator, "By.id: q");
        Assert.assertEquals(r.score, 0.82, 1e-9);
        Assert.assertFalse(r.success);
        Assert.assertNull(r.query);
        Assert.assertNull(r.category);
    }

    @Test
    public void record_7arg_capturesAllFields() {
        HealingTelemetryStore.record(3, "By.id: x", "By.css: .x", 0.91, true, "the query", "INPUT");
        TelemetryRecord r = HealingTelemetryStore.getAllRecords().get(0);
        Assert.assertEquals(r.tier, 3);
        Assert.assertEquals(r.query, "the query");
        Assert.assertEquals(r.category, "INPUT");
        Assert.assertTrue(r.success);
        Assert.assertNotNull(r.threadName);
        Assert.assertNotNull(r.timestamp);
    }

    @Test
    public void getRecordsForTier_filtersCorrectly() {
        HealingTelemetryStore.record(1, "a", "b", 0.9, true);
        HealingTelemetryStore.record(2, "c", "d", 0.8, true);
        HealingTelemetryStore.record(1, "e", "f", 0.7, false);

        List<TelemetryRecord> tier1 = HealingTelemetryStore.getRecordsForTier(1);
        List<TelemetryRecord> tier2 = HealingTelemetryStore.getRecordsForTier(2);
        List<TelemetryRecord> tier3 = HealingTelemetryStore.getRecordsForTier(3);

        Assert.assertEquals(tier1.size(), 2);
        Assert.assertEquals(tier2.size(), 1);
        Assert.assertEquals(tier3.size(), 0);
        Assert.assertTrue(tier1.stream().allMatch(r -> r.tier == 1));
    }

    @Test
    public void size_reflectsQueueCount() {
        Assert.assertEquals(HealingTelemetryStore.size(), 0);
        HealingTelemetryStore.record(1, "a", "b", 0.9, true);
        HealingTelemetryStore.record(2, "c", "d", 0.8, false);
        Assert.assertEquals(HealingTelemetryStore.size(), 2);
    }

    @Test
    public void clear_emptiesQueue() {
        HealingTelemetryStore.record(1, "a", "b", 0.9, true);
        HealingTelemetryStore.clear();
        Assert.assertEquals(HealingTelemetryStore.size(), 0);
        Assert.assertTrue(HealingTelemetryStore.getAllRecords().isEmpty());
    }

    @Test
    public void markTestFailed_nullTestId_returnsZero() {
        HealingTelemetryStore.record(1, "a", "b", 0.9, true, null, null);
        Assert.assertEquals(HealingTelemetryStore.markTestFailed(null), 0);
    }

    @Test
    public void telemetryRecord_suspectWrongHealStartsFalse() {
        HealingTelemetryStore.record(1, "a", "b", 0.9, true);
        Assert.assertFalse(HealingTelemetryStore.getAllRecords().get(0).suspectWrongHeal);
    }
}
