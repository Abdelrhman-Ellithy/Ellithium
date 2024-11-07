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

public class CouchbaseDatabaseProvider implements NoSQLDatabaseProvider {

    private final Cluster couchbaseCluster;
    private final Bucket couchbaseBucket;
    private final Collection couchbaseCollection;
    private final Cache<String, Object> queryResultCache;

    public CouchbaseDatabaseProvider(String connectionString, String username, String password, String bucketName) {
        this.couchbaseCluster = Cluster.connect(connectionString, username, password);
        this.couchbaseBucket = couchbaseCluster.bucket(bucketName);
        this.couchbaseCollection = couchbaseBucket.defaultCollection();
        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    @Override
    public Object executeQuery(String documentId) {
        GetResult result = couchbaseCollection.get(documentId);
        return result.contentAsObject();
    }

    @Override
    public void closeConnection() {
        couchbaseCluster.disconnect();
    }

    @Override
    public void clearCache(String key) {
        queryResultCache.invalidate(key);
        Reporter.log("Cleared cache for key: " + key, LogLevel.INFO_BLUE);
    }

    @Override
    public void clearAllCaches() {
        queryResultCache.invalidateAll();
        Reporter.log("All caches cleared.", LogLevel.INFO_BLUE);
    }

    @Override
    public void addToCache(String key, Object value) {
        queryResultCache.put(key, value);
        Reporter.log("Added result to cache for key: " + key, LogLevel.INFO_BLUE);
    }

    @Override
    public Object getFromCache(String key) {
        return queryResultCache.getIfPresent(key);
    }
}