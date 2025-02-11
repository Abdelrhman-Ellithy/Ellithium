package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

/**
 * Provides operations for Couchbase database access.
 */
public class CouchbaseDatabaseProvider implements NoSQLDatabaseProvider {

    private final Cluster couchbaseCluster;
    private final Bucket couchbaseBucket;
    private final Collection couchbaseCollection;
    private final Cache<String, Object> queryResultCache;

    /**
     * Constructs a CouchbaseDatabaseProvider using the given connection details.
     *
     * @param connectionString the connection string.
     * @param username the username.
     * @param password the password.
     * @param bucketName the bucket name.
     */
    public CouchbaseDatabaseProvider(String connectionString, String username, String password, String bucketName) {
        this.couchbaseCluster = Cluster.connect(connectionString, username, password);
        this.couchbaseBucket = couchbaseCluster.bucket(bucketName);
        this.couchbaseCollection = couchbaseBucket.defaultCollection();
        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    /**
     * Executes a query by retrieving the document with the specified ID.
     *
     * @param documentId the document identifier.
     * @return the document content.
     */
    @Override
    public Object executeQuery(String documentId) {
        GetResult result = couchbaseCollection.get(documentId);
        return result.contentAsObject();
    }

    /**
     * Closes the Couchbase connection.
     */
    @Override
    public void closeConnection() {
        couchbaseCluster.disconnect();
    }

    /**
     * Clears the cache for the specified key.
     *
     * @param key the cache key.
     */
    @Override
    public void clearCache(String key) {
        queryResultCache.invalidate(key);
        Reporter.log("Cleared cache for key: " + key, LogLevel.INFO_BLUE);
    }

    /**
     * Clears all cached query results.
     */
    @Override
    public void clearAllCaches() {
        queryResultCache.invalidateAll();
        Reporter.log("All caches cleared.", LogLevel.INFO_BLUE);
    }

    /**
     * Adds a query result to the cache.
     *
     * @param key the cache key.
     * @param value the result to cache.
     */
    @Override
    public void addToCache(String key, Object value) {
        queryResultCache.put(key, value);
        Reporter.log("Added result to cache for key: " + key, LogLevel.INFO_BLUE);
    }

    /**
     * Retrieves a cached query result by key.
     *
     * @param key the cache key.
     * @return the cached result, or null if not available.
     */
    @Override
    public Object getFromCache(String key) {
        return queryResultCache.getIfPresent(key);
    }
}