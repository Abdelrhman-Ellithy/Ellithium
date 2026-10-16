package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.*;
import java.time.Duration;
import java.util.Optional;

/**
 * Provides robust operations for Couchbase database access.
 * Supports document operations, N1QL queries, and health checks.
 * Queries are always executed live against the database to guarantee test integrity.
 */
public class CouchbaseDatabaseProvider implements NoSQLDatabaseProvider {

    private final Cluster cluster;
    private final Bucket bucket;

    /**
     * Primary constructor for production use.
     * Connects to Couchbase using the provided details.
     *
     * @param host       the Couchbase host
     * @param username   the username
     * @param password   the password
     * @param bucketName the bucket name
     */
    public CouchbaseDatabaseProvider(String host, String username, String password, String bucketName) {
        Reporter.log("Initializing Couchbase connection to " + host, LogLevel.INFO_YELLOW);
        this.cluster = createCouchbaseCluster(host, username, password);
        this.bucket = createCouchbaseBucket(this.cluster, bucketName);
        Reporter.log("Couchbase connection initialized successfully", LogLevel.INFO_YELLOW);
    }

    /**
     * @deprecated Caching has been removed from database providers. Use {@link #CouchbaseDatabaseProvider(String, String, String, String)} instead.
     */
    @Deprecated
    public CouchbaseDatabaseProvider(String host, String username, String password, String bucketName,
                                     long cacheTtlMinutes, long cacheMaxSize) {
        this(host, username, password, bucketName);
    }

    /**
     * Constructor for dependency injection (primarily for testing).
     * Accepts pre-configured Cluster and Bucket instances.
     *
     * @param cluster a pre-configured Couchbase Cluster instance
     * @param bucket  a pre-configured Couchbase Bucket instance
     */
    public CouchbaseDatabaseProvider(Cluster cluster, Bucket bucket) {
        if (cluster == null || bucket == null) {
            throw new IllegalArgumentException("Cluster and Bucket cannot be null when using this constructor for dependency injection.");
        }
        this.cluster = cluster;
        this.bucket = bucket;
        Reporter.log("Couchbase initialized successfully", LogLevel.INFO_YELLOW);
    }

    /**
     * Helper method to encapsulate Couchbase Cluster creation.
     *
     * @param host      the Couchbase host
     * @param username  the username
     * @param password  the password
     * @return a new Cluster instance
     */
    private static Cluster createCouchbaseCluster(String host, String username, String password) {
        return Cluster.connect(host, username, password);
    }

    /**
     * Helper method to encapsulate Couchbase Bucket retrieval.
     *
     * @param cluster    the Couchbase Cluster instance
     * @param bucketName the name of the bucket to connect to
     * @return a new Bucket instance
     */
    private static Bucket createCouchbaseBucket(Cluster cluster, String bucketName) {
        return cluster.bucket(bucketName);
    }

    /**
     * Executes a N1QL query live against the database.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param query the N1QL query to execute
     * @return the query result, or null if not found
     */
    @Override
    public Object executeQuery(String query) {
        Reporter.log("Executing Couchbase N1QL query: " + query, LogLevel.INFO_YELLOW);
        try {
            return cluster.query(query).rowsAsObject();
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
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    @Override
    public void clearCache(String key) {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    @Override
    public void clearAllCaches() {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    @Override
    public void addToCache(String key, Object value) {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    @Override
    public Object getFromCache(String key) {
        return null;
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
            throw new NoSQLRuntimeException("Failed to insert document: " + docId, e);
        }
    }

    /**
     * Updates or inserts a document.
     *
     * @param docId    the document ID
     * @param document the document to upsert
     * @throws NoSQLRuntimeException if the operation fails
     */
    public void upsert(String docId, JsonObject document) {
        try {
            Reporter.log("Upserting document: " + docId, LogLevel.INFO_YELLOW);
            bucket.defaultCollection().upsert(docId, document);
            Reporter.log("Document upserted successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to upsert document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new NoSQLRuntimeException("Failed to upsert document: " + docId, e);
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