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

public class MongoDatabaseProvider implements NoSQLDatabaseProvider {

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final Cache<String, Object> queryResultCache;

    public MongoDatabaseProvider(String connectionString, String dbName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.mongoDatabase = mongoClient.getDatabase(dbName);

        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    @Override
    public Object executeQuery(String collectionName) {
        MongoCollection<?> collection = mongoDatabase.getCollection(collectionName);
        return collection.find().into(new ArrayList<>());
    }

    @Override
    public void closeConnection() {
        mongoClient.close();
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
