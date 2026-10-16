package Ellithium.core.DB;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.ExistsResult;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.query.QueryResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CouchbaseDatabaseProviderTest {

    private Cluster mockCluster;
    private Bucket mockBucket;
    private Collection mockCollection;
    private Cache<String, Object> cache;
    private CouchbaseDatabaseProvider couchbaseProvider;

    @BeforeMethod
    public void setUp() {
        mockCluster = mock(Cluster.class);
        mockBucket = mock(Bucket.class);
        mockCollection = mock(Collection.class);

        when(mockBucket.defaultCollection()).thenReturn(mockCollection);
        when(mockBucket.collection("custom_col")).thenReturn(mockCollection);

        cache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();

        couchbaseProvider = new CouchbaseDatabaseProvider(mockCluster, mockBucket, cache);
    }

    @Test
    public void testConstructorValidation() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new CouchbaseDatabaseProvider(null, mockBucket, cache);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new CouchbaseDatabaseProvider(mockCluster, null, cache);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new CouchbaseDatabaseProvider(mockCluster, mockBucket, null);
        });
    }

    @Test
    public void testExecuteQueryCacheMissAndHit() {
        QueryResult mockQueryResult = mock(QueryResult.class);
        JsonObject rowObj = JsonObject.create().put("key", "val");
        when(mockCluster.query(anyString())).thenReturn(mockQueryResult);
        when(mockQueryResult.rowsAsObject()).thenReturn(List.of(rowObj));

        // Cache miss
        Object result = couchbaseProvider.executeQuery("SELECT * FROM `travel-sample` LIMIT 1;");
        Assert.assertNotNull(result);
        verify(mockCluster, times(1)).query(anyString());

        // Cache hit
        Object cachedResult = couchbaseProvider.executeQuery("SELECT * FROM `travel-sample` LIMIT 1;");
        Assert.assertNotNull(cachedResult);
        verify(mockCluster, times(1)).query(anyString());
    }

    @Test
    public void testExecuteQueryExceptionHandling() {
        when(mockCluster.query(anyString())).thenThrow(new RuntimeException("Syntax error"));
        Object result = couchbaseProvider.executeQuery("INVALID N1QL");
        Assert.assertNull(result);
    }

    @Test
    public void testCacheOperations() {
        couchbaseProvider.addToCache("ckey1", "cval1");
        Assert.assertEquals(couchbaseProvider.getFromCache("ckey1"), "cval1");

        couchbaseProvider.clearCache("ckey1");
        Assert.assertNull(couchbaseProvider.getFromCache("ckey1"));

        couchbaseProvider.addToCache("ckey2", "cval2");
        couchbaseProvider.clearAllCaches();
        Assert.assertNull(couchbaseProvider.getFromCache("ckey2"));
    }

    @Test
    public void testIsHealthy() {
        Assert.assertTrue(couchbaseProvider.isHealthy());
        verify(mockCluster).ping();

        doThrow(new RuntimeException("Cluster unreachable")).when(mockCluster).ping();
        Assert.assertFalse(couchbaseProvider.isHealthy());
    }

    @Test
    public void testExists() {
        ExistsResult mockExists = mock(ExistsResult.class);
        when(mockExists.exists()).thenReturn(true);
        when(mockCollection.exists("doc:100")).thenReturn(mockExists);

        Assert.assertTrue(couchbaseProvider.exists("doc:100"));

        when(mockExists.exists()).thenReturn(false);
        when(mockCollection.exists("doc:missing")).thenReturn(mockExists);
        Assert.assertFalse(couchbaseProvider.exists("doc:missing"));

        when(mockCollection.exists("doc:err")).thenThrow(new RuntimeException("Failed"));
        Assert.assertFalse(couchbaseProvider.exists("doc:err"));
    }

    @Test
    public void testDelete() {
        Assert.assertTrue(couchbaseProvider.delete("doc:to_delete"));
        verify(mockCollection).remove("doc:to_delete");

        doThrow(new RuntimeException("Remove failed")).when(mockCollection).remove("doc:err");
        Assert.assertFalse(couchbaseProvider.delete("doc:err"));
    }

    @Test
    public void testExpire() {
        Assert.assertTrue(couchbaseProvider.expire("doc:expire_me", 60));
        verify(mockCollection).touch(eq("doc:expire_me"), eq(Duration.ofSeconds(60)));

        doThrow(new RuntimeException("Touch failed")).when(mockCollection).touch(anyString(), any(Duration.class));
        Assert.assertFalse(couchbaseProvider.expire("doc:err", 60));
    }

    @Test
    public void testGetDocument() {
        GetResult mockGetResult = mock(GetResult.class);
        JsonObject sample = JsonObject.create().put("name", "Alice");
        when(mockGetResult.contentAsObject()).thenReturn(sample);
        when(mockCollection.get("doc:alice")).thenReturn(mockGetResult);

        Optional<JsonObject> opt = couchbaseProvider.get("doc:alice");
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals(opt.get(), sample);

        when(mockCollection.get("doc:unknown")).thenThrow(new RuntimeException("Not found"));
        Optional<JsonObject> optMissing = couchbaseProvider.get("doc:unknown");
        Assert.assertFalse(optMissing.isPresent());
    }

    @Test
    public void testInsertDocument() {
        JsonObject doc = JsonObject.create().put("active", true);
        couchbaseProvider.insert("doc:new", doc);
        verify(mockCollection).insert("doc:new", doc);

        doThrow(new RuntimeException("Insert error")).when(mockCollection).insert(eq("doc:err"), any(JsonObject.class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            couchbaseProvider.insert("doc:err", doc);
        });
    }

    @Test
    public void testUpsertDocument() {
        JsonObject doc = JsonObject.create().put("active", false);
        couchbaseProvider.upsert("doc:upsert", doc);
        verify(mockCollection).upsert("doc:upsert", doc);

        doThrow(new RuntimeException("Upsert error")).when(mockCollection).upsert(eq("doc:err"), any(JsonObject.class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            couchbaseProvider.upsert("doc:err", doc);
        });
    }

    @Test
    public void testGetCollectionAndDisconnect() {
        Collection col = couchbaseProvider.getCollection("custom_col");
        Assert.assertSame(col, mockCollection);

        couchbaseProvider.closeConnection();
        verify(mockCluster).disconnect();
    }
}
