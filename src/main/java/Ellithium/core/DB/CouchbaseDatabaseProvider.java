package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Provides robust operations for Couchbase database access with caching capabilities.
 * Supports document operations, N1QL queries, and health checks.
 */
public class CouchbaseDatabaseProvider implements NoSQLDatabaseProvider {

    private final Cluster cluster;
    private final Bucket bucket;
    private final Cache<String, Object> queryResultCache;

    /**
     * Constructs a CouchbaseDatabaseProvider with the specified connection details and cache configuration.
     *
     * @param host            the Couchbase host
     * @param username        the username
     * @param password        the password
     * @param bucketName      the bucket name
     * @param cacheTtlMinutes the time-to-live for cache entries in minutes
     * @param cacheMaxSize    the maximum number of entries to store in the cache
     */
    public CouchbaseDatabaseProvider(String host, String username, String password, String bucketName, 
                                   long cacheTtlMinutes, long cacheMaxSize) {
        Reporter.log("Initializing Couchbase connection to " + host, LogLevel.INFO_YELLOW);
        this.cluster = Cluster.connect(host, username, password);
        this.bucket = cluster.bucket(bucketName);
        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
        Reporter.log("Couchbase connection initialized successfully", LogLevel.INFO_YELLOW);
    }

    /**
     * Executes a N1QL query against the database.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param query the N1QL query to execute
     * @return the query result, or null if not found
     */
    @Override
    public Object executeQuery(String query) {
        Object cachedResult = getFromCache(query);
        if (cachedResult != null) {
            Reporter.log("Retrieved from cache for query: " + query, LogLevel.INFO_YELLOW);
            return cachedResult;
        }

        Reporter.log("Cache miss for query: " + query + ". Executing N1QL query.", LogLevel.INFO_YELLOW);
        try {
            Object result = cluster.query(query).rowsAsObject();
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
     * Closes the Couchbase connection.
     * This method implements the NoSQLDatabaseProvider interface.
     */
    @Override
    public void closeConnection() {
        if (cluster != null) {
            cluster.disconnect();
            Reporter.log("Couchbase connection closed.", LogLevel.INFO_YELLOW);
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
     * Performs a health check against the Couchbase server.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @return true if the connection is healthy, false otherwise
     */
    @Override
    public boolean isHealthy() {
        try {
            Reporter.log("Performing health check", LogLevel.INFO_YELLOW);
            cluster.ping();
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
            boolean result = bucket.defaultCollection().exists(key).exists();
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
            bucket.defaultCollection().remove(key);
            Reporter.log("Document deleted successfully", LogLevel.INFO_YELLOW);
            return true;
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
            bucket.defaultCollection().touch(key, Duration.ofSeconds(seconds));
            Reporter.log("Expiry set successfully", LogLevel.INFO_YELLOW);
            return true;
        } catch (Exception e) {
            Reporter.log("Failed to set expiry for document: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    // Couchbase-specific methods below

    /**
     * Gets a document by ID.
     *
     * @param docId the document ID
     * @return an Optional containing the document if found, empty otherwise
     */
    public Optional<JsonObject> get(String docId) {
        try {
            Reporter.log("Getting document: " + docId, LogLevel.INFO_YELLOW);
            GetResult result = bucket.defaultCollection().get(docId);
            JsonObject document = result.contentAsObject();
            Reporter.log("Document retrieved successfully", LogLevel.INFO_YELLOW);
            return Optional.of(document);
        } catch (Exception e) {
            Reporter.log("Failed to get document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return Optional.empty();
        }
    }

    /**
     * Inserts a new document.
     *
     * @param docId    the document ID
     * @param document the document to insert
     * @throws RuntimeException if the operation fails
     */
    public void insert(String docId, JsonObject document) {
        try {
            Reporter.log("Inserting document: " + docId, LogLevel.INFO_YELLOW);
            bucket.defaultCollection().insert(docId, document);
            Reporter.log("Document inserted successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to insert document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to insert document: " + docId, e);
        }
    }

    /**
     * Updates or inserts a document.
     *
     * @param docId    the document ID
     * @param document the document to upsert
     * @throws RuntimeException if the operation fails
     */
    public void upsert(String docId, JsonObject document) {
        try {
            Reporter.log("Upserting document: " + docId, LogLevel.INFO_YELLOW);
            bucket.defaultCollection().upsert(docId, document);
            Reporter.log("Document upserted successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to upsert document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to upsert document: " + docId, e);
        }
    }

    /**
     * Gets a collection by name.
     *
     * @param collectionName the collection name
     * @return the collection
     */
    public Collection getCollection(String collectionName) {
        return bucket.collection(collectionName);
    }
}