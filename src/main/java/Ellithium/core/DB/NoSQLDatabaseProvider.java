package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import redis.clients.jedis.Jedis;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class NoSQLDatabaseProvider {

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private Jedis jedisClient;
    private Cluster couchbaseCluster;
    private Bucket couchbaseBucket;
    private Collection couchbaseCollection;
    private final DBType dbType;
    private final Cache<String, Object> queryResultCache;

    public NoSQLDatabaseProvider(DBType dbType, String connectionString, String dbName, String username, String password) {
        this.dbType = dbType;

        // Initialize cache
        queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();

        initializeConnection(connectionString, dbName, username, password);
    }

    private void initializeConnection(String connectionString, String dbName, String username, String password) {
        try {
            switch (dbType) {
                case MONGO -> initializeMongoDB(connectionString, dbName);
                case REDIS -> initializeRedis(connectionString);
                case COUCHBASE -> initializeCouchbase(connectionString, dbName, username, password);
                default -> Reporter.log("Unsupported DB Type: " + dbType, LogLevel.ERROR);
            }
        } catch (Exception e) {
            Reporter.log("Failed to initialize " + dbType + " connection: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    private void initializeMongoDB(String connectionString, String dbName) {
        mongoClient = new MongoClient(new MongoClientURI(connectionString));
        mongoDatabase = mongoClient.getDatabase(dbName);
        Reporter.log("Initialized MongoDB connection.", LogLevel.INFO_BLUE);
    }

    private void initializeRedis(String connectionString) {
        jedisClient = new Jedis(connectionString);
        Reporter.log("Initialized Redis connection.", LogLevel.INFO_BLUE);
    }

    private void initializeCouchbase(String connectionString, String dbName, String username, String password) {
        couchbaseCluster = Cluster.connect(connectionString, username, password);
        couchbaseBucket = couchbaseCluster.bucket(dbName);
        couchbaseCollection = couchbaseBucket.defaultCollection();
        Reporter.log("Initialized Couchbase connection.", LogLevel.INFO_BLUE);
    }

    public Object executeQuery(String query) {
        Objects.requireNonNull(query, "Query must not be null");

        return switch (dbType) {
            case MONGO -> executeMongoQuery(query);
            case REDIS -> executeRedisQuery(query);
            case COUCHBASE -> executeCouchbaseQuery(query);
            default -> {
                Reporter.log("Unsupported DB operation for type: " + dbType, LogLevel.ERROR);
                yield null;
            }
        };
    }

    private Object executeMongoQuery(String collectionName) {
        if (mongoDatabase == null) {
            Reporter.log("MongoDB is not initialized.", LogLevel.ERROR);
            return null;
        }
        MongoCollection<?> collection = mongoDatabase.getCollection(collectionName);
        if (collection == null) {
            Reporter.log("MongoDB collection not found: " + collectionName, LogLevel.ERROR);
            return null;
        }
        return collection.find().into(new ArrayList<>());
    }

    private Object executeRedisQuery(String key) {
        if (jedisClient == null) {
            Reporter.log("Redis is not initialized.", LogLevel.ERROR);
            return null;
        }
        return jedisClient.get(key);
    }

    private Object executeCouchbaseQuery(String documentId) {
        if (couchbaseCollection == null) {
            Reporter.log("Couchbase is not initialized.", LogLevel.ERROR);
            return null;
        }
        try {
            GetResult result = couchbaseCollection.get(documentId);
            return result.contentAsObject();
        } catch (Exception e) {
            Reporter.log("Error executing Couchbase query: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    public synchronized void closeConnection() {
        try {
            switch (dbType) {
                case MONGO -> closeMongoConnection();
                case REDIS -> closeRedisConnection();
                case COUCHBASE -> closeCouchbaseConnection();
                default -> Reporter.log("No connection to close for unsupported DB type.", LogLevel.ERROR);
            }
        } catch (Exception e) {
            Reporter.log("Error closing " + dbType + " connection: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    private void closeMongoConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            Reporter.log("Closed MongoDB connection.", LogLevel.INFO_BLUE);
        }
    }

    private void closeRedisConnection() {
        if (jedisClient != null) {
            jedisClient.close();
            jedisClient = null;
            Reporter.log("Closed Redis connection.", LogLevel.INFO_BLUE);
        }
    }

    private void closeCouchbaseConnection() {
        if (couchbaseCluster != null) {
            couchbaseCluster.disconnect();
            couchbaseCluster = null;
            Reporter.log("Closed Couchbase connection.", LogLevel.INFO_BLUE);
        }
    }

    public void clearCache(String key) {
        queryResultCache.invalidate(key);
        Reporter.log("Cleared cache for key: " + key, LogLevel.INFO_BLUE);
    }

    public void clearAllCaches() {
        queryResultCache.invalidateAll();
        Reporter.log("All caches cleared.", LogLevel.INFO_BLUE);
    }

    public void addToCache(String key, Object value) {
        queryResultCache.put(key, value);
        Reporter.log("Added result to cache for key: " + key, LogLevel.INFO_BLUE);
    }

    public Object getFromCache(String key) {
        return queryResultCache.getIfPresent(key);
    }
}