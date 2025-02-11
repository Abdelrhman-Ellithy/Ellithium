package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

/**
 * Provides operations for Redis database access using a Jedis client.
 */
public class RedisDatabaseProvider implements NoSQLDatabaseProvider {

    private final Jedis jedisClient;
    private final Cache<String, Object> queryResultCache;

    /**
     * Constructs a RedisDatabaseProvider using the specified connection string.
     *
     * @param connectionString the connection string for Redis.
     */
    public RedisDatabaseProvider(String connectionString) {
        this.jedisClient = new Jedis(connectionString);
        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    /**
     * Executes a query by retrieving the value associated with the given key.
     *
     * @param key the key to query.
     * @return the value from Redis.
     */
    @Override
    public Object executeQuery(String key) {
        return jedisClient.get(key);
    }

    /**
     * Closes the Redis connection.
     */
    @Override
    public void closeConnection() {
        jedisClient.close();
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
     * @return the cached value, or null if not present.
     */
    @Override
    public Object getFromCache(String key) {
        return queryResultCache.getIfPresent(key);
    }

}
