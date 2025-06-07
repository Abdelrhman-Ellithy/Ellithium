package DB;

import Ellithium.core.DB.CouchbaseDatabaseProvider;
import com.couchbase.client.core.diagnostics.PingResult;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.*;
import com.couchbase.client.java.query.QueryResult;
import com.github.benmanes.caffeine.cache.Cache;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class CouchbaseDatabaseProviderTest {

    @Mock
    private Cluster mockCluster;

    @Mock
    private Bucket mockBucket;

    @Mock
    private Collection mockCollection;

    @Mock
    private GetResult mockGetResult;

    @Mock
    private MutationResult mockMutationResult;

    @Mock
    private Cache<String, Object> mockCache;

    private CouchbaseDatabaseProvider databaseProvider;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up default mock behavior for Cluster and Bucket to return other mocks
        when(mockCluster.bucket(anyString())).thenReturn(mockBucket);
        when(mockBucket.defaultCollection()).thenReturn(mockCollection);
        when(mockBucket.collection(anyString())).thenReturn(mockCollection);

        // Create provider with all required parameters
        databaseProvider = new CouchbaseDatabaseProvider(
            "dummyHost", 
            "dummyUser", 
            "dummyPass", 
            "dummyBucket",
            30L, // cacheTtlMinutes
            1000L // cacheMaxSize
        );

        // Inject mocks using reflection
        try {
            // Inject mockCluster
            java.lang.reflect.Field clusterField = CouchbaseDatabaseProvider.class.getDeclaredField("cluster");
            clusterField.setAccessible(true);
            clusterField.set(databaseProvider, mockCluster);

            // Inject mockBucket
            java.lang.reflect.Field bucketField = CouchbaseDatabaseProvider.class.getDeclaredField("bucket");
            bucketField.setAccessible(true);
            bucketField.set(databaseProvider, mockBucket);

            // Inject mockCache
            java.lang.reflect.Field cacheField = CouchbaseDatabaseProvider.class.getDeclaredField("queryResultCache");
            cacheField.setAccessible(true);
            cacheField.set(databaseProvider, mockCache);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            fail("Failed to inject mocks using reflection.");
        }
    }

    @Test
    public void testInsert() {
        String docId = "testDoc";
        JsonObject document = JsonObject.create();

        databaseProvider.insert(docId, document);

        verify(mockCollection).insert(eq(docId), eq(document));
    }

    @Test
    public void testGet_WhenDocumentExists() {
        String docId = "testDoc";
        JsonObject expected = JsonObject.fromJson("{\"key\":\"value\"}");

        when(mockCollection.get(docId)).thenReturn(mockGetResult);
        when(mockGetResult.contentAsObject()).thenReturn(expected);

        Optional<JsonObject> result = databaseProvider.get(docId);

        assertTrue(result.isPresent());
        assertEquals(result.get(), expected);
        verify(mockCollection).get(eq(docId));
        verify(mockGetResult).contentAsObject();
    }

    @Test
    public void testGet_WhenDocumentDoesNotExist() {
        String docId = "nonExistentDoc";

        when(mockCollection.get(docId)).thenThrow(new RuntimeException("Document not found"));

        Optional<JsonObject> result = databaseProvider.get(docId);

        assertFalse(result.isPresent());
        verify(mockCollection).get(eq(docId));
    }

    @Test
    public void testUpsert() {
        String docId = "testDoc";
        JsonObject document = JsonObject.create();

        when(mockCollection.upsert(anyString(), any(JsonObject.class))).thenReturn(mockMutationResult);

        databaseProvider.upsert(docId, document);

        verify(mockCollection).upsert(eq(docId), eq(document));
    }

    @Test
    public void testDelete() {
        String docId = "testDoc";

        when(mockCollection.remove(anyString())).thenReturn(mockMutationResult);

        boolean result = databaseProvider.delete(docId);

        assertTrue(result);
        verify(mockCollection).remove(eq(docId));
    }

    @Test
    public void testExists() {
        String docId = "testDoc";
        ExistsResult mockExistsResult = mock(ExistsResult.class);
        when(mockExistsResult.exists()).thenReturn(true);
        when(mockCollection.exists(anyString())).thenReturn(mockExistsResult);

        boolean result = databaseProvider.exists(docId);

        assertTrue(result);
        verify(mockCollection).exists(eq(docId));
    }

    @Test
    public void testExpire() {
        String docId = "testDoc";
        int seconds = 3600;

        boolean result = databaseProvider.expire(docId, seconds);

        assertTrue(result);
        verify(mockCollection).touch(eq(docId), eq(Duration.ofSeconds(seconds)));
    }

    @Test
    public void testExecuteQuery_WhenCached() {
        String query = "SELECT * FROM `bucket`";
        JsonObject expectedResult = JsonObject.create().put("data", "cached");

        when(mockCache.getIfPresent(query)).thenReturn(expectedResult);

        Object result = databaseProvider.executeQuery(query);

        assertNotNull(result);
        assertEquals(result, expectedResult);
        verify(mockCache).getIfPresent(eq(query));
        verifyNoMoreInteractions(mockCluster, mockBucket, mockCollection);
    }

    @Test
    public void testExecuteQuery_WhenNotCached() {
        String query = "SELECT * FROM `bucket`";
        List<JsonObject> expectedResult = List.of(JsonObject.create().put("data", "result"));

        when(mockCache.getIfPresent(query)).thenReturn(null);
        QueryResult mockQueryResult = mock(QueryResult.class);
        when(mockCluster.query(query)).thenReturn(mockQueryResult);
        when(mockQueryResult.rowsAsObject()).thenReturn(expectedResult);

        Object result = databaseProvider.executeQuery(query);

        assertNotNull(result);
        assertEquals(result, expectedResult);
        verify(mockCache).getIfPresent(eq(query));
        verify(mockCluster).query(eq(query));
        verify(mockCache).put(eq(query), eq(expectedResult));
    }

    @Test
    public void testAddToCache() {
        String key = "testKey";
        Object value = new Object();

        databaseProvider.addToCache(key, value);

        verify(mockCache).put(eq(key), eq(value));
    }

    @Test
    public void testClearAllCaches() {
        databaseProvider.clearAllCaches();

        verify(mockCache).invalidateAll();
    }

    @Test
    public void testClearCache() {
        String key = "testKey";

        databaseProvider.clearCache(key);

        verify(mockCache).invalidate(eq(key));
    }

    @Test
    public void testCloseConnection() {
        databaseProvider.closeConnection();

        verify(mockCluster).disconnect();
    }

    @Test
    public void testGetFromCache() {
        String key = "testKey";
        Object expected = new Object();

        when(mockCache.getIfPresent(key)).thenReturn(expected);

        Object result = databaseProvider.getFromCache(key);

        assertEquals(result, expected);
        verify(mockCache).getIfPresent(eq(key));
    }

    @Test
    public void testIsHealthy() {
        when(mockCluster.ping()).thenReturn(mock(PingResult.class));

        boolean result = databaseProvider.isHealthy();

        assertTrue(result);
        verify(mockCluster).ping();
    }

    @Test
    public void testGetCollection() {
        String collectionName = "myCollection";

        Collection result = databaseProvider.getCollection(collectionName);

        assertNotNull(result);
        assertEquals(result, mockCollection);
        verify(mockBucket).collection(eq(collectionName));
    }
}