package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Provides robust operations for MongoDB database access with caching capabilities.
 * Supports document operations, queries, and health checks.
 */
public class MongoDatabaseProvider implements NoSQLDatabaseProvider {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final Cache<String, Object> queryResultCache;

    /**
     * Constructs a MongoDatabaseProvider with the specified connection details and cache configuration.
     *
     * @param connectionString the MongoDB connection string
     * @param databaseName     the database name
     * @param cacheTtlMinutes the time-to-live for cache entries in minutes
     * @param cacheMaxSize    the maximum number of entries to store in the cache
     */
    public MongoDatabaseProvider(String connectionString, String databaseName, long cacheTtlMinutes, long cacheMaxSize) {
        Reporter.log("Initializing MongoDB connection to " + connectionString, LogLevel.INFO_YELLOW);
        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(databaseName);
        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
        Reporter.log("MongoDB connection initialized successfully", LogLevel.INFO_YELLOW);
    }

    /**
     * Executes a MongoDB query.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param query the query to execute
     * @return the query result, or null if not found
     */
    @Override
    public Object executeQuery(String query) {
        Object cachedResult = getFromCache(query);
        if (cachedResult != null) {
            Reporter.log("Retrieved from cache for query: " + query, LogLevel.INFO_YELLOW);
            return cachedResult;
        }

        Reporter.log("Cache miss for query: " + query + ". Executing MongoDB query.", LogLevel.INFO_YELLOW);
        try {
            Document command = Document.parse(query);
            Object result = database.runCommand(command);
            if (result != null) {
                addToCache(query, result);
                Reporter.log("Query executed and result cached successfully", LogLevel.INFO_YELLOW);
            }
            return result;
        } catch (Exception e) {
            Reporter.log("Failed to execute query: " + query + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Closes the MongoDB connection.
     * This method implements the NoSQLDatabaseProvider interface.
     */
    @Override
    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            Reporter.log("MongoDB connection closed.", LogLevel.INFO_YELLOW);
        }
    }

    /**
     * Clears the cache for a specific key.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the cache key to clear
     */
    @Override
    public void clearCache(String key) {
        queryResultCache.invalidate(key);
        Reporter.log("Cleared cache for key: " + key, LogLevel.INFO_YELLOW);
    }

    /**
     * Clears all cached query results.
     * This method implements the NoSQLDatabaseProvider interface.
     */
    @Override
    public void clearAllCaches() {
        queryResultCache.invalidateAll();
        Reporter.log("All caches cleared.", LogLevel.INFO_YELLOW);
    }

    /**
     * Adds a query result to the cache.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key   the cache key
     * @param value the value to cache
     */
    @Override
    public void addToCache(String key, Object value) {
        queryResultCache.put(key, value);
        Reporter.log("Added result to cache for key: " + key, LogLevel.INFO_YELLOW);
    }

    /**
     * Retrieves a cached query result by key.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the cache key
     * @return the cached value, or null if not present
     */
    @Override
    public Object getFromCache(String key) {
        return queryResultCache.getIfPresent(key);
    }

    /**
     * Performs a health check against the MongoDB server.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @return true if the connection is healthy, false otherwise
     */
    @Override
    public boolean isHealthy() {
        try {
            Reporter.log("Performing health check", LogLevel.INFO_YELLOW);
            database.runCommand(new Document("ping", 1));
            Reporter.log("Health check completed. Status: Healthy", LogLevel.INFO_YELLOW);
            return true;
        } catch (Exception e) {
            Reporter.log("Health check failed! Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if a document exists.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the document ID to check
     * @return true if the document exists, false otherwise
     */
    @Override
    public boolean exists(String key) {
        try {
            Reporter.log("Checking existence of document: " + key, LogLevel.INFO_YELLOW);
            boolean result = database.getCollection("documents").countDocuments(new Document("_id", key)) > 0;
            Reporter.log("Document existence check completed", LogLevel.INFO_YELLOW);
            return result;
        } catch (Exception e) {
            Reporter.log("Failed to check document existence: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Deletes a document.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the document ID to delete
     * @return true if the document was deleted, false otherwise
     */
    @Override
    public boolean delete(String key) {
        try {
            Reporter.log("Deleting document: " + key, LogLevel.INFO_YELLOW);
            long result = database.getCollection("documents").deleteOne(new Document("_id", key)).getDeletedCount();
            Reporter.log("Document deleted successfully", LogLevel.INFO_YELLOW);
            return result > 0;
        } catch (Exception e) {
            Reporter.log("Failed to delete document: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Sets an expiration time on a document.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key     the document ID
     * @param seconds the expiration time in seconds
     * @return true if the expiration was set, false otherwise
     */
    @Override
    public boolean expire(String key, int seconds) {
        try {
            Reporter.log("Setting expiry for document: " + key + " to " + seconds + " seconds", LogLevel.INFO_YELLOW);
            database.getCollection("documents").updateOne(
                new Document("_id", key),
                new Document("$set", new Document("expiresAt", System.currentTimeMillis() + (seconds * 1000L)))
            );
            Reporter.log("Expiry set successfully", LogLevel.INFO_YELLOW);
            return true;
        } catch (Exception e) {
            Reporter.log("Failed to set expiry for document: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    // MongoDB-specific methods below

    /**
     * Gets a document by ID.
     *
     * @param collectionName the collection name
     * @param docId         the document ID
     * @return an Optional containing the document if found, empty otherwise
     */
    public Optional<Document> getDocument(String collectionName, String docId) {
        try {
            Reporter.log("Getting document: " + docId + " from collection: " + collectionName, LogLevel.INFO_YELLOW);
            Document result = database.getCollection(collectionName).find(new Document("_id", docId)).first();
            Reporter.log("Document retrieved successfully", LogLevel.INFO_YELLOW);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            Reporter.log("Failed to get document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return Optional.empty();
        }
    }

    /**
     * Inserts a new document.
     *
     * @param collectionName the collection name
     * @param document      the document to insert
     * @throws RuntimeException if the operation fails
     */
    public void insertDocument(String collectionName, Document document) {
        try {
            Reporter.log("Inserting document into collection: " + collectionName, LogLevel.INFO_YELLOW);
            database.getCollection(collectionName).insertOne(document);
            Reporter.log("Document inserted successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to insert document. Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to insert document", e);
        }
    }

    /**
     * Updates a document.
     *
     * @param collectionName the collection name
     * @param docId         the document ID
     * @param update        the update operation
     * @throws RuntimeException if the operation fails
     */
    public void updateDocument(String collectionName, String docId, Bson update) {
        try {
            Reporter.log("Updating document: " + docId + " in collection: " + collectionName, LogLevel.INFO_YELLOW);
            database.getCollection(collectionName).updateOne(new Document("_id", docId), update);
            Reporter.log("Document updated successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to update document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to update document: " + docId, e);
        }
    }

    /**
     * Deletes a document.
     *
     * @param collectionName the collection name
     * @param docId         the document ID
     * @throws RuntimeException if the operation fails
     */
    public void deleteDocument(String collectionName, String docId) {
        try {
            Reporter.log("Deleting document: " + docId + " from collection: " + collectionName, LogLevel.INFO_YELLOW);
            database.getCollection(collectionName).deleteOne(new Document("_id", docId));
            Reporter.log("Document deleted successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to delete document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to delete document: " + docId, e);
        }
    }

    /**
     * Creates an index on a collection.
     *
     * @param collectionName the collection name
     * @param field         the field to index
     * @param unique        whether the index should be unique
     * @throws RuntimeException if the operation fails
     */
    public void createIndex(String collectionName, String field, boolean unique) {
        try {
            Reporter.log("Creating index on field: " + field + " in collection: " + collectionName, LogLevel.INFO_YELLOW);
            IndexOptions options = new IndexOptions().unique(unique);
            database.getCollection(collectionName).createIndex(Indexes.ascending(field), options);
            Reporter.log("Index created successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to create index. Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to create index", e);
        }
    }

    /**
     * Gets a collection by name.
     *
     * @param collectionName the collection name
     * @return the collection
     */
    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }
}
