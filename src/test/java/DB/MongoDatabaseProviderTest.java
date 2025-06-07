package DB;

import Ellithium.core.DB.MongoDatabaseProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;
import static org.mockito.Mockito.*;

public class MongoDatabaseProviderTest {

    @Mock
    private MongoClient mockMongoClient;

    @Mock
    private MongoDatabase mockDatabase;

    @Mock
    private MongoCollection<Document> mockCollection;

    @Mock
    private Cache<String, Object> mockCache;

    @Mock
    private DeleteResult mockDeleteResult;

    @Mock
    private UpdateResult mockUpdateResult;

    private MongoDatabaseProvider databaseProvider;
    private final String TEST_DATABASE = "testDB";
    private final String TEST_COLLECTION = "testCollection";
    private final String TEST_DOC_ID = "testDocId";

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockMongoClient.getDatabase(TEST_DATABASE)).thenReturn(mockDatabase);
        when(mockDatabase.getCollection(anyString())).thenReturn(mockCollection);
        when(mockCollection.deleteOne(any(Document.class))).thenReturn(mockDeleteResult);
        when(mockCollection.updateOne(any(Document.class), any(Document.class))).thenReturn(mockUpdateResult);
        when(mockDeleteResult.getDeletedCount()).thenReturn(1L);
        when(mockUpdateResult.getModifiedCount()).thenReturn(1L);
        when(mockCollection.countDocuments(any(Document.class))).thenReturn(1L);

        // Set up FindIterable mock
        com.mongodb.client.FindIterable<Document> mockFindIterable = mock(com.mongodb.client.FindIterable.class);
        when(mockCollection.find(any(Document.class))).thenReturn(mockFindIterable);
        when(mockFindIterable.first()).thenReturn(new Document("_id", TEST_DOC_ID));
        databaseProvider = new MongoDatabaseProvider(mockMongoClient,mockDatabase,mockCache);
    }

    @Test
    void testExecuteQuery() {
        String query = "{ \"find\": \"testCollection\" }";
        Document result = new Document("result", "test");
        when(mockDatabase.runCommand(any(Document.class))).thenReturn(result);

        Object queryResult = databaseProvider.executeQuery(query);

        assertNotNull(queryResult);
        verify(mockDatabase).runCommand(any(Document.class));
        verify(mockCache).put(eq(query), eq(result));
    }

    @Test
    void testCloseConnection() {
        databaseProvider.closeConnection();
        verify(mockMongoClient).close();
    }

    @Test
    void testClearCache() {
        String key = "testKey";
        databaseProvider.clearCache(key);
        verify(mockCache).invalidate(key);
    }

    @Test
    void testClearAllCaches() {
        databaseProvider.clearAllCaches();
        verify(mockCache).invalidateAll();
    }

    @Test
    void testAddToCache() {
        String key = "testKey";
        Object value = new Object();
        databaseProvider.addToCache(key, value);
        verify(mockCache).put(key, value);
    }

    @Test
    void testGetFromCache() {
        String key = "testKey";
        Object expected = new Object();
        when(mockCache.getIfPresent(key)).thenReturn(expected);

        Object result = databaseProvider.getFromCache(key);

        assertEquals(result, expected);
        verify(mockCache).getIfPresent(key);
    }

    @Test
    void testIsHealthy() {
        when(mockDatabase.runCommand(any(Document.class))).thenReturn(new Document("ok", 1.0));

        boolean result = databaseProvider.isHealthy();

        assertTrue(result);
        verify(mockDatabase).runCommand(any(Document.class));
    }

    @Test
    void testExists() {
        when(mockCollection.countDocuments(any(Document.class))).thenReturn(1L);

        boolean result = databaseProvider.exists(TEST_DOC_ID);

        assertTrue(result);
        verify(mockCollection).countDocuments(any(Document.class));
    }

    @Test
    void testDelete() {
        when(mockCollection.deleteOne(any(Document.class))).thenReturn(mockDeleteResult);
        when(mockDeleteResult.getDeletedCount()).thenReturn(1L);

        boolean result = databaseProvider.delete(TEST_DOC_ID);

        assertTrue(result);
        verify(mockCollection).deleteOne(any(Document.class));
    }

    @Test
    void testExpire() {
        when(mockCollection.updateOne(any(Document.class), any(Document.class))).thenReturn(mockUpdateResult);
        when(mockUpdateResult.getModifiedCount()).thenReturn(1L);

        boolean result = databaseProvider.expire(TEST_DOC_ID, 60);

        assertTrue(result);
        verify(mockCollection).updateOne(any(Document.class), any(Document.class));
    }

    @Test
    void testGetDocument() {
        Document expected = new Document("_id", TEST_DOC_ID);
        com.mongodb.client.FindIterable<Document> mockFindIterable = mock(com.mongodb.client.FindIterable.class);
        when(mockCollection.find(any(Document.class))).thenReturn(mockFindIterable);
        when(mockFindIterable.first()).thenReturn(expected);

        Optional<Document> result = databaseProvider.getDocument(TEST_COLLECTION, TEST_DOC_ID);

        assertTrue(result.isPresent());
        assertEquals(result.get(), expected);
        verify(mockCollection).find(any(Document.class));
    }

    @Test
    void testInsertDocument() {
        Document document = new Document("_id", TEST_DOC_ID);
        doNothing().when(mockCollection).insertOne(any(Document.class));

        databaseProvider.insertDocument(TEST_COLLECTION, document);

        verify(mockCollection).insertOne(document);
    }

    @Test
    void testUpdateDocument() {
        Bson update = new Document("$set", new Document("field", "value"));
        when(mockCollection.updateOne(any(Document.class), eq(update))).thenReturn(mockUpdateResult);

        databaseProvider.updateDocument(TEST_COLLECTION, TEST_DOC_ID, update);

        verify(mockCollection).updateOne(any(Document.class), eq(update));
    }

    @Test
    void testDeleteDocument() {
        when(mockCollection.deleteOne(any(Document.class))).thenReturn(mockDeleteResult);

        databaseProvider.deleteDocument(TEST_COLLECTION, TEST_DOC_ID);

        verify(mockCollection).deleteOne(any(Document.class));
    }

    @Test
    void testCreateIndex() {
        String field = "testField";
        when(mockCollection.createIndex(any(), any(IndexOptions.class))).thenReturn("indexName");

        databaseProvider.createIndex(TEST_COLLECTION, field, true);

        verify(mockCollection).createIndex(eq(Indexes.ascending(field)), any(IndexOptions.class));
    }

    @Test
    void testGetCollection() {
        MongoCollection<Document> result = databaseProvider.getCollection(TEST_COLLECTION);

        assertNotNull(result);
        assertEquals(result, mockCollection);
        verify(mockDatabase).getCollection(TEST_COLLECTION);
    }
} 