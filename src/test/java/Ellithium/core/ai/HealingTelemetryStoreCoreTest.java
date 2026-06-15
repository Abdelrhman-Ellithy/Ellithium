package Ellithium.core.ai;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

public class HealingTelemetryStoreCoreTest {

    @BeforeMethod
    @AfterMethod
    public void reset() {
        HealingTelemetryStore.clear();
        HealingTelemetryStore.clearCurrentTest();
    }

    // ── categoryForAction ───────────────────────────────────────────────

    @DataProvider
    public Object[][] clickableActions() {
        return new Object[][]{
            {"click"}, {"tapElement"}, {"pressKey"}, {"hoverOver"},
            {"submitForm"}, {"toggleCheckbox"}
        };
    }

    @Test(dataProvider = "clickableActions")
    public void categoryForAction_clickVariants_returnsClickable(String action) {
        Assert.assertEquals(HealingTelemetryStore.categoryForAction(action), "CLICKABLE");
    }

    @DataProvider
    public Object[][] inputActions() {
        return new Object[][]{
            {"sendKeys"}, {"typeText"}, {"inputValue"}, {"setText"},
            {"clearField"}, {"selectDropdown"}, {"uploadFile"}, {"fillForm"}, {"enterData"}, {"setData"}
        };
    }

    @Test(dataProvider = "inputActions")
    public void categoryForAction_inputVariants_returnsInput(String action) {
        Assert.assertEquals(HealingTelemetryStore.categoryForAction(action), "INPUT");
    }

    @DataProvider
    public Object[][] readableActions() {
        return new Object[][]{
            {"getText"}, {"readAttribute"}, {"extractValue"},
            {"verifyText"}, {"checkStatus"}, {"assertVisible"}
        };
    }

    @Test(dataProvider = "readableActions")
    public void categoryForAction_readableVariants_returnsReadable(String action) {
        Assert.assertEquals(HealingTelemetryStore.categoryForAction(action), "READABLE");
    }

    @Test
    public void categoryForAction_unknownAction_returnsNull() {
        Assert.assertNull(HealingTelemetryStore.categoryForAction("doSomethingWeird"));
    }

    @Test
    public void categoryForAction_null_returnsNull() {
        Assert.assertNull(HealingTelemetryStore.categoryForAction(null));
    }

    // ── basic record / size ─────────────────────────────────────────────

    @Test
    public void record_incrementsSize() {
        Assert.assertEquals(HealingTelemetryStore.size(), 0);
        HealingTelemetryStore.record(1, "By.id: btn", "By.id: button", 0.9, true);
        Assert.assertEquals(HealingTelemetryStore.size(), 1);
    }

    @Test
    public void getAllRecords_returnsSnapshot() {
        HealingTelemetryStore.record(1, "broken", "healed", 0.8, true);
        HealingTelemetryStore.record(2, "broken2", "healed2", 0.6, false);
        List<HealingTelemetryStore.TelemetryRecord> all = HealingTelemetryStore.getAllRecords();
        Assert.assertEquals(all.size(), 2);
    }

    @Test
    public void getRecordsForTier_filtersByTier() {
        HealingTelemetryStore.record(1, "b1", "h1", 0.9, true);
        HealingTelemetryStore.record(2, "b2", "h2", 0.7, false);
        HealingTelemetryStore.record(1, "b3", "h3", 0.8, true);

        List<HealingTelemetryStore.TelemetryRecord> tier1 = HealingTelemetryStore.getRecordsForTier(1);
        List<HealingTelemetryStore.TelemetryRecord> tier2 = HealingTelemetryStore.getRecordsForTier(2);

        Assert.assertEquals(tier1.size(), 2);
        Assert.assertEquals(tier2.size(), 1);
    }

    @Test
    public void getRecordsForTier_unknownTier_returnsEmpty() {
        HealingTelemetryStore.record(1, "b", "h", 0.9, true);
        Assert.assertTrue(HealingTelemetryStore.getRecordsForTier(99).isEmpty());
    }

    // ── markTestFailed ──────────────────────────────────────────────────

    @Test
    public void markTestFailed_flagsSuccessfulHealsForTest() {
        HealingTelemetryStore.setCurrentTest("test-001");
        HealingTelemetryStore.record(1, "broken", "healed", 0.9, true);
        HealingTelemetryStore.record(2, "broken2", null, 0.0, false);

        int flagged = HealingTelemetryStore.markTestFailed("test-001");
        Assert.assertEquals(flagged, 1);
    }

    @Test
    public void markTestFailed_doesNotFlagFallthroughs() {
        HealingTelemetryStore.setCurrentTest("test-002");
        HealingTelemetryStore.record(1, "broken", null, 0.0, false);

        int flagged = HealingTelemetryStore.markTestFailed("test-002");
        Assert.assertEquals(flagged, 0);
    }

    @Test
    public void markTestFailed_idempotent_alreadyFlaggedNotCountedTwice() {
        HealingTelemetryStore.setCurrentTest("test-003");
        HealingTelemetryStore.record(1, "broken", "healed", 0.9, true);

        int first = HealingTelemetryStore.markTestFailed("test-003");
        int second = HealingTelemetryStore.markTestFailed("test-003");

        Assert.assertEquals(first, 1);
        Assert.assertEquals(second, 0);
    }

    @Test
    public void markTestFailed_nullTestId_returnsZero() {
        Assert.assertEquals(HealingTelemetryStore.markTestFailed(null), 0);
    }

    @Test
    public void markTestFailed_unknownTestId_returnsZero() {
        HealingTelemetryStore.record(1, "broken", "healed", 0.9, true);
        Assert.assertEquals(HealingTelemetryStore.markTestFailed("no-such-test"), 0);
    }

    // ── setCurrentTest / clearCurrentTest ──────────────────────────────

    @Test
    public void setCurrentTest_attributesRecordToTest() {
        HealingTelemetryStore.setCurrentTest("my-test");
        HealingTelemetryStore.record(1, "b", "h", 0.9, true);

        List<HealingTelemetryStore.TelemetryRecord> all = HealingTelemetryStore.getAllRecords();
        Assert.assertEquals(all.get(0).testId, "my-test");
    }

    @Test
    public void clearCurrentTest_subsequentRecordHasNullTestId() {
        HealingTelemetryStore.setCurrentTest("my-test");
        HealingTelemetryStore.clearCurrentTest();
        HealingTelemetryStore.record(1, "b", "h", 0.9, true);

        List<HealingTelemetryStore.TelemetryRecord> all = HealingTelemetryStore.getAllRecords();
        Assert.assertNull(all.get(0).testId);
    }

    // ── ring-buffer eviction ────────────────────────────────────────────

    @Test
    public void ringBuffer_neverExceedsMaxRecords() {
        for (int i = 0; i < 20; i++) {
            HealingTelemetryStore.record(1, "b" + i, "h" + i, 0.5, true);
        }
        int size = HealingTelemetryStore.size();
        Assert.assertTrue(size > 0, "Must have recorded some entries");
        Assert.assertTrue(size <= 100000, "Must not exceed max telemetry records");
    }

    // ── record fields ───────────────────────────────────────────────────

    @Test
    public void record_storesAllFields() {
        HealingTelemetryStore.setCurrentTest("t1");
        HealingTelemetryStore.record(3, "By.id: x", "By.name: x", 0.75, true, "semantic query", "CLICKABLE");

        HealingTelemetryStore.TelemetryRecord r = HealingTelemetryStore.getAllRecords().get(0);
        Assert.assertEquals(r.tier, 3);
        Assert.assertEquals(r.brokenLocator, "By.id: x");
        Assert.assertEquals(r.healedLocator, "By.name: x");
        Assert.assertEquals(r.score, 0.75, 1e-9);
        Assert.assertTrue(r.success);
        Assert.assertEquals(r.query, "semantic query");
        Assert.assertEquals(r.category, "CLICKABLE");
        Assert.assertEquals(r.testId, "t1");
        Assert.assertFalse(r.suspectWrongHeal);
        Assert.assertNotNull(r.threadName);
        Assert.assertNotNull(r.timestamp);
    }

    // ── clear ───────────────────────────────────────────────────────────

    @Test
    public void clear_resetsEverything() {
        HealingTelemetryStore.setCurrentTest("t");
        HealingTelemetryStore.record(1, "b", "h", 0.9, true);
        Assert.assertEquals(HealingTelemetryStore.size(), 1);

        HealingTelemetryStore.clear();

        Assert.assertEquals(HealingTelemetryStore.size(), 0);
        Assert.assertTrue(HealingTelemetryStore.getAllRecords().isEmpty());
        Assert.assertTrue(HealingTelemetryStore.getRecordsForTier(1).isEmpty());
    }
}
