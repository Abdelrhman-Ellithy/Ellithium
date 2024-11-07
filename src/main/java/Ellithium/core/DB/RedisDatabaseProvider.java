package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

public class RedisDatabaseProvider implements NoSQLDatabaseProvider {

    private final Jedis jedisClient;
    private final Cache<String, Object> queryResultCache;

    public RedisDatabaseProvider(String connectionString) {
        this.jedisClient = new Jedis(connectionString);
        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    @Override
    public Object executeQuery(String key) {
        return jedisClient.get(key);
    }

    @Override
    public void closeConnection() {
        jedisClient.close();
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
