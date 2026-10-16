package Ellithium.core.ai;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.Assert.*;

public class HardNegativeStoreTest {

    @BeforeMethod
    @AfterMethod
    public void cleanup() {
        HardNegativeStore.clear();
        File file = new File(HardNegativeStore.OUTPUT_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testRecordAndLookup() {
        String broken = "By.id: submit-button";
        String badCandidate = "By.cssSelector: .submit-btn-wrong";

        assertFalse(HardNegativeStore.isHardNegative(broken, badCandidate));

        HardNegativeStore.record("testLogin_fail", 2, broken, badCandidate, 0.88, "CLICK");

        assertTrue(HardNegativeStore.isHardNegative(broken, badCandidate));
        assertFalse(HardNegativeStore.isHardNegative(broken, "By.id: valid-button"));
        assertFalse(HardNegativeStore.isHardNegative("By.id: other", badCandidate));

        List<HardNegativeStore.HardNegativeRecord> records = HardNegativeStore.getAllRecords();
        assertEquals(records.size(), 1);
        HardNegativeStore.HardNegativeRecord r = records.get(0);
        assertEquals(r.testId(), "testLogin_fail");
        assertEquals(r.tier(), 2);
        assertEquals(r.brokenLocator(), broken);
        assertEquals(r.falseHealedLocator(), badCandidate);
        assertEquals(r.score(), 0.88, 0.001);
        assertEquals(r.actionType(), "CLICK");
        assertNotNull(r.timestamp());
    }

    @Test
    public void testFlushAndReloadFromDisk() {
        String broken = "By.xpath: //input[@name='search']";
        String falseCandidate = "By.cssSelector: input.search-filter";

        HardNegativeStore.record("testSearch_fail", 2, broken, falseCandidate, 0.82, "TYPE");
        HardNegativeStore.flush();

        File file = new File(HardNegativeStore.OUTPUT_FILE);
        assertTrue(file.exists(), "Hard negatives output file should be created");

        // Clear in-memory state and delete disk file to verify false
        HardNegativeStore.clear();
        file.delete();
        assertFalse(HardNegativeStore.isHardNegative(broken, falseCandidate));

        // Re-record, flush, clear, and verify auto-reload from disk
        HardNegativeStore.record("testSearch_fail", 2, broken, falseCandidate, 0.82, "TYPE");
        HardNegativeStore.flush();
        HardNegativeStore.clear();

        // Re-initialize from disk
        HardNegativeStore.initialize();
        assertTrue(HardNegativeStore.isHardNegative(broken, falseCandidate));
        assertEquals(HardNegativeStore.getAllRecords().size(), 1);
    }

    @Test
    public void testNullGuards() {
        HardNegativeStore.record(null, 1, null, null, 0.0, null);
        assertTrue(HardNegativeStore.getAllRecords().isEmpty());

        assertFalse(HardNegativeStore.isHardNegative(null, null));
        assertFalse(HardNegativeStore.isHardNegative("By.id: x", null));
        assertFalse(HardNegativeStore.isHardNegative(null, "By.id: y"));
    }
}
