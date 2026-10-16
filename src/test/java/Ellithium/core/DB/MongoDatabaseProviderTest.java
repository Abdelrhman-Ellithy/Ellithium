package Ellithium.core.DB;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MongoDatabaseProviderTest {

    private MongoClient mockClient;
    private MongoDatabase mockDatabase;
    @SuppressWarnings("unchecked")
    private MongoCollection<Document> mockCollection;
    private MongoDatabaseProvider mongoProvider;

    @BeforeMethod
    public void setUp() {
        mockClient = mock(MongoClient.class);
        mockDatabase = mock(MongoDatabase.class);
        mockCollection = mock(MongoCollection.class);

        when(mockDatabase.getCollection("documents")).thenReturn(mockCollection);
        when(mockDatabase.getCollection("custom_collection")).thenReturn(mockCollection);

        mongoProvider = new MongoDatabaseProvider(mockClient, mockDatabase, "documents");
    }

    @Test
    public void testConstructors() {
        MongoDatabaseProvider defaultCollProvider = new MongoDatabaseProvider(mockClient, mockDatabase);
        Assert.assertNotNull(defaultCollProvider);
    }

    @Test
    public void testExecuteQueryAlwaysLive() {
        String jsonQuery = "{\"ping\": 1}";
        Document commandResult = new Document("ok", 1.0);
        when(mockDatabase.runCommand(any(Document.class))).thenReturn(commandResult);

        // First call
        Object result = mongoProvider.executeQuery(jsonQuery);
        Assert.assertEquals(result, commandResult);
        verify(mockDatabase, times(1)).runCommand(any(Document.class));

        // Second call - must execute live against DB, never cached
        Object secondResult = mongoProvider.executeQuery(jsonQuery);
        Assert.assertEquals(secondResult, commandResult);
        verify(mockDatabase, times(2)).runCommand(any(Document.class));
    }

    @Test
    public void testExecuteQueryExceptionHandling() {
        when(mockDatabase.runCommand(any(Document.class))).thenThrow(new RuntimeException("Database error"));
        Object result = mongoProvider.executeQuery("{\"invalid\": true}");
        Assert.assertNull(result);
    }

    @Test
    public void testCacheOperationsNoOp() {
        mongoProvider.addToCache("q1", "res1");
        Assert.assertNull(mongoProvider.getFromCache("q1"));

        mongoProvider.clearCache("q1");
        mongoProvider.clearAllCaches();
        Assert.assertNull(mongoProvider.getFromCache("q2"));
    }

    @Test
    public void testIsHealthy() {
        when(mockDatabase.runCommand(any(Document.class))).thenReturn(new Document("ok", 1));
        Assert.assertTrue(mongoProvider.isHealthy());

        when(mockDatabase.runCommand(any(Document.class))).thenThrow(new RuntimeException("Connection failed"));
        Assert.assertFalse(mongoProvider.isHealthy());
    }

    @Test
    public void testExists() {
        when(mockCollection.countDocuments(any(Document.class))).thenReturn(1L);
        Assert.assertTrue(mongoProvider.exists("doc-123"));

        when(mockCollection.countDocuments(any(Document.class))).thenReturn(0L);
        Assert.assertFalse(mongoProvider.exists("doc-999"));

        when(mockCollection.countDocuments(any(Document.class))).thenThrow(new RuntimeException("Error"));
        Assert.assertFalse(mongoProvider.exists("doc-err"));
    }

    @Test
    public void testDelete() {
        DeleteResult mockDeleteResult = mock(DeleteResult.class);
        when(mockDeleteResult.getDeletedCount()).thenReturn(1L);
        when(mockCollection.deleteOne(any(Document.class))).thenReturn(mockDeleteResult);

        Assert.assertTrue(mongoProvider.delete("doc-123"));

        when(mockDeleteResult.getDeletedCount()).thenReturn(0L);
        Assert.assertFalse(mongoProvider.delete("doc-999"));

        when(mockCollection.deleteOne(any(Document.class))).thenThrow(new RuntimeException("Error"));
        Assert.assertFalse(mongoProvider.delete("doc-err"));
    }

    @Test
    public void testExpire() {
        UpdateResult mockUpdateResult = mock(UpdateResult.class);
        when(mockCollection.updateOne(any(Document.class), any(Document.class))).thenReturn(mockUpdateResult);

        Assert.assertTrue(mongoProvider.expire("doc-1", 60));

        when(mockCollection.updateOne(any(Document.class), any(Document.class))).thenThrow(new RuntimeException("Error"));
        Assert.assertFalse(mongoProvider.expire("doc-err", 60));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetDocument() {
        FindIterable<Document> mockFindIterable = mock(FindIterable.class);
        Document sampleDoc = new Document("_id", "doc-1").append("name", "Item");
        when(mockCollection.find(any(Document.class))).thenReturn(mockFindIterable);
        when(mockFindIterable.first()).thenReturn(sampleDoc);

        Optional<Document> opt = mongoProvider.getDocument("documents", "doc-1");
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals(opt.get(), sampleDoc);

        when(mockFindIterable.first()).thenReturn(null);
        Optional<Document> optEmpty = mongoProvider.getDocument("documents", "doc-unknown");
        Assert.assertFalse(optEmpty.isPresent());

        when(mockCollection.find(any(Document.class))).thenThrow(new RuntimeException("Find error"));
        Optional<Document> optErr = mongoProvider.getDocument("documents", "doc-err");
        Assert.assertFalse(optErr.isPresent());
    }

    @Test
    public void testInsertDocument() {
        Document doc = new Document("title", "Test");
        mongoProvider.insertDocument("documents", doc);
        verify(mockCollection).insertOne(doc);

        doThrow(new RuntimeException("Insert error")).when(mockCollection).insertOne(any(Document.class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            mongoProvider.insertDocument("documents", doc);
        });
    }

    @Test
    public void testUpdateDocument() {
        Bson update = new Document("$set", new Document("score", 100));
        mongoProvider.updateDocument("documents", "doc-1", update);
        verify(mockCollection).updateOne(any(Document.class), eq(update));

        doThrow(new RuntimeException("Update error")).when(mockCollection).updateOne(any(Document.class), any(Bson.class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            mongoProvider.updateDocument("documents", "doc-1", update);
        });
    }

    @Test
    public void testDeleteDocument() {
        mongoProvider.deleteDocument("documents", "doc-1");
        verify(mockCollection).deleteOne(any(Document.class));

        doThrow(new RuntimeException("Delete error")).when(mockCollection).deleteOne(any(Document.class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            mongoProvider.deleteDocument("documents", "doc-1");
        });
    }

    @Test
    public void testCreateIndex() {
        mongoProvider.createIndex("documents", "email", true);
        verify(mockCollection).createIndex(any(Bson.class), any(com.mongodb.client.model.IndexOptions.class));

        doThrow(new RuntimeException("Index error")).when(mockCollection).createIndex(any(Bson.class), any(com.mongodb.client.model.IndexOptions.class));
        Assert.assertThrows(NoSQLRuntimeException.class, () -> {
            mongoProvider.createIndex("documents", "email", true);
        });
    }

    @Test
    public void testGetCollectionAndClose() {
        MongoCollection<Document> coll = mongoProvider.getCollection("custom_collection");
        Assert.assertSame(coll, mockCollection);

        mongoProvider.closeConnection();
        verify(mockClient).close();
    }
}
