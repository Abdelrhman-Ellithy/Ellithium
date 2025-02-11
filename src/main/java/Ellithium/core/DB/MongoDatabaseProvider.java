package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Provides operations for MongoDB database access.
 */
public class MongoDatabaseProvider implements NoSQLDatabaseProvider {

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final Cache<String, Object> queryResultCache;

    /**
     * Constructs a MongoDatabaseProvider with the provided connection string and database name.
     *
     * @param connectionString the MongoDB connection string.
     * @param dbName the name of the database.
     */
    public MongoDatabaseProvider(String connectionString, String dbName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.mongoDatabase = mongoClient.getDatabase(dbName);

        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    /**
     * Executes a query by retrieving all documents from the specified collection.
     *
     * @param collectionName the collection to query.
     * @return the list of documents.
     */
    @Override
    public Object executeQuery(String collectionName) {
        MongoCollection<?> collection = mongoDatabase.getCollection(collectionName);
        return collection.find().into(new ArrayList<>());
    }

    /**
     * Closes the MongoDB connection.
     */
    @Override
    public void closeConnection() {
        mongoClient.close();
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
     * Clears all cached results.
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
     * @param value the value to cache.
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
     * @return the cached result, or null if not present.
     */
    @Override
    public Object getFromCache(String key) {
        return queryResultCache.getIfPresent(key);
    }
}
