package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.ElementFingerprint;
import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class BaselineSqliteStorageTest {

    private static final Gson GSON = new Gson();
    private Path tempDbFile;
    private BaselineSqliteStorage storage;

    private ElementFingerprint createFingerprint(String locator, String id, long lastSeen) {
        String json = String.format("{\"locatorKey\":\"%s\",\"id\":\"%s\",\"lastSeenEpoch\":%d,\"tagName\":\"button\"}",
                locator, id, lastSeen);
        return GSON.fromJson(json, ElementFingerprint.class);
    }

    @BeforeMethod
    public void setup() throws IOException {
        tempDbFile = Files.createTempFile("ellithium-test-baselines-", ".db");
        Files.deleteIfExists(tempDbFile);
        storage = new BaselineSqliteStorage(tempDbFile);
    }

    @AfterMethod
    public void tearDown() {
        try {
            Files.deleteIfExists(tempDbFile);
        } catch (Exception ignored) {}
    }

    @Test
    public void newStorage_isEmpty() {
        Assert.assertTrue(storage.isEmpty(), "New storage should report empty");
        Assert.assertEquals(storage.count(), 0, "New storage should have count 0");
    }

    @Test
    public void upsertAndGet_persistsAndRetrievesFingerprints() {
        ElementFingerprint fp = createFingerprint("By.id: submitBtn", "submitBtn", 1000L);
        storage.upsert("By.id: submitBtn", List.of(fp));

        Assert.assertFalse(storage.isEmpty());
        Assert.assertEquals(storage.count(), 1);

        List<ElementFingerprint> retrieved = storage.get("By.id: submitBtn");
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(retrieved.size(), 1);
        Assert.assertEquals(retrieved.get(0).getId(), "submitBtn");
        Assert.assertEquals(retrieved.get(0).getLastSeenEpoch(), 1000L);
    }

    @Test
    public void upsertBatchAndLoadAll_persistsMultipleRecords() {
        ElementFingerprint fp1 = createFingerprint("By.id: user", "user", 2000L);
        ElementFingerprint fp2 = createFingerprint("By.id: pass", "pass", 3000L);

        Map<String, List<ElementFingerprint>> batch = Map.of(
                "By.id: user", List.of(fp1),
                "By.id: pass", List.of(fp2)
        );

        storage.upsertBatch(batch);
        Assert.assertEquals(storage.count(), 2);

        Map<String, List<ElementFingerprint>> loaded = storage.loadAll();
        Assert.assertEquals(loaded.size(), 2);
        Assert.assertTrue(loaded.containsKey("By.id: user"));
        Assert.assertTrue(loaded.containsKey("By.id: pass"));
        Assert.assertEquals(loaded.get("By.id: user").get(0).getId(), "user");
        Assert.assertEquals(loaded.get("By.id: pass").get(0).getId(), "pass");
    }

    @Test
    public void upsert_conflictUpdatesExistingRecord() {
        ElementFingerprint initial = createFingerprint("By.id: login", "login-v1", 1000L);
        storage.upsert("By.id: login", List.of(initial));

        ElementFingerprint updated = createFingerprint("By.id: login", "login-v2", 5000L);
        storage.upsert("By.id: login", List.of(updated));

        Assert.assertEquals(storage.count(), 1);
        List<ElementFingerprint> retrieved = storage.get("By.id: login");
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(retrieved.size(), 1);
        Assert.assertEquals(retrieved.get(0).getId(), "login-v2");
        Assert.assertEquals(retrieved.get(0).getLastSeenEpoch(), 5000L);
    }

    @Test
    public void delete_removesSingleRecord() {
        ElementFingerprint fp = createFingerprint("By.id: cancel", "cancel", 1000L);
        storage.upsert("By.id: cancel", List.of(fp));
        Assert.assertEquals(storage.count(), 1);

        boolean deleted = storage.delete("By.id: cancel");
        Assert.assertTrue(deleted);
        Assert.assertEquals(storage.count(), 0);
        Assert.assertNull(storage.get("By.id: cancel"));
    }

    @Test
    public void deleteAll_clearsAllRecords() {
        storage.upsert("By.id: a", List.of(createFingerprint("By.id: a", "a", 1000L)));
        storage.upsert("By.id: b", List.of(createFingerprint("By.id: b", "b", 2000L)));
        Assert.assertEquals(storage.count(), 2);

        storage.deleteAll();
        Assert.assertEquals(storage.count(), 0);
        Assert.assertTrue(storage.isEmpty());
    }

    @Test
    public void pruneStale_removesExpiredEntries() {
        storage.upsert("By.id: old", List.of(createFingerprint("By.id: old", "old", 500L)));
        storage.upsert("By.id: new", List.of(createFingerprint("By.id: new", "new", 1500L)));

        int pruned = storage.pruneStale(1000L);
        Assert.assertEquals(pruned, 1);
        Assert.assertEquals(storage.count(), 1);
        Assert.assertNull(storage.get("By.id: old"));
        Assert.assertNotNull(storage.get("By.id: new"));
    }

    @Test
    public void pruneByCount_evictsOldestEntriesWhenOverLimit() {
        storage.upsert("By.id: 1", List.of(createFingerprint("By.id: 1", "1", 100L)));
        storage.upsert("By.id: 2", List.of(createFingerprint("By.id: 2", "2", 200L)));
        storage.upsert("By.id: 3", List.of(createFingerprint("By.id: 3", "3", 300L)));
        storage.upsert("By.id: 4", List.of(createFingerprint("By.id: 4", "4", 400L)));

        Assert.assertEquals(storage.count(), 4);

        int evicted = storage.pruneByCount(2);
        Assert.assertEquals(evicted, 2);
        Assert.assertEquals(storage.count(), 2);

        // Oldest (1 and 2) should be evicted, newest (3 and 4) kept
        Assert.assertNull(storage.get("By.id: 1"));
        Assert.assertNull(storage.get("By.id: 2"));
        Assert.assertNotNull(storage.get("By.id: 3"));
        Assert.assertNotNull(storage.get("By.id: 4"));
    }

    @Test
    public void largeBatch_pointLookupsAreFastAndAccurate() {
        Map<String, List<ElementFingerprint>> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            String key = "By.id: element-" + i;
            ElementFingerprint fp = createFingerprint(key, "el-" + i, 1000L + i);
            map.put(key, List.of(fp));
        }

        storage.upsertBatch(map);
        Assert.assertEquals(storage.count(), 100);

        // Point lookup for element-42
        List<ElementFingerprint> result = storage.get("By.id: element-42");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.get(0).getId(), "el-42");
        Assert.assertEquals(result.get(0).getLastSeenEpoch(), 1042L);
    }
}
