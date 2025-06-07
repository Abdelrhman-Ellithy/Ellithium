package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Provides robust operations for Redis database access with caching capabilities.
 * Supports key-value operations, hash operations, list operations, set operations,
 * and health checks.
 */
public class RedisDatabaseProvider implements NoSQLDatabaseProvider {

    private final JedisPool jedisPool;
    private final Cache<String, Object> queryResultCache;

    /**
     * Constructs a RedisDatabaseProvider with the specified connection details and cache configuration.
     *
     * @param host            the Redis host
     * @param port            the Redis port
     * @param cacheTtlMinutes the time-to-live for cache entries in minutes
     * @param cacheMaxSize    the maximum number of entries to store in the cache
     */
    public RedisDatabaseProvider(String host, int port, long cacheTtlMinutes, long cacheMaxSize) {
        Reporter.log("Initializing Redis connection to " + host + ":" + port, LogLevel.INFO_YELLOW);
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        this.jedisPool = new JedisPool(poolConfig, host, port);
        this.queryResultCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
        Reporter.log("Redis connection initialized successfully", LogLevel.INFO_YELLOW);
    }

    /**
     * Executes a query by retrieving the value associated with the given key.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the key to query
     * @return the value from Redis, or null if not found
     */
    @Override
    public Object executeQuery(String key) {
        Object cachedResult = getFromCache(key);
        if (cachedResult != null) {
            Reporter.log("Retrieved from cache for key: " + key, LogLevel.INFO_YELLOW);
            return cachedResult;
        }

        Reporter.log("Cache miss for key: " + key + ". Querying Redis.", LogLevel.INFO_YELLOW);
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key);
            if (value != null) {
                addToCache(key, value);
                Reporter.log("Value retrieved and cached successfully", LogLevel.INFO_YELLOW);
            }
            return value;
        } catch (Exception e) {
            Reporter.log("Failed to execute query for key: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Closes the Redis connection pool.
     * This method implements the NoSQLDatabaseProvider interface.
     */
    @Override
    public void closeConnection() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            Reporter.log("Redis connection closed.", LogLevel.INFO_YELLOW);
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
     * Performs a health check against the Redis server.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @return true if the connection is healthy, false otherwise
     */
    @Override
    public boolean isHealthy() {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Performing health check", LogLevel.INFO_YELLOW);
            String pong = jedis.ping();
            boolean isHealthy = "PONG".equals(pong);
            Reporter.log("Health check completed. Status: " + (isHealthy ? "Healthy" : "Unhealthy"), LogLevel.INFO_YELLOW);
            return isHealthy;
        } catch (Exception e) {
            Reporter.log("Health check failed! Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if a key exists.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    @Override
    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Checking existence of key: " + key, LogLevel.INFO_YELLOW);
            boolean result = jedis.exists(key);
            Reporter.log("Key existence check completed", LogLevel.INFO_YELLOW);
            return result;
        } catch (Exception e) {
            Reporter.log("Failed to check key existence: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Deletes a key.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the key to delete
     * @return true if the key was deleted, false otherwise
     */
    @Override
    public boolean delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Deleting key: " + key, LogLevel.INFO_YELLOW);
            long result = jedis.del(key);
            Reporter.log("Key deleted successfully", LogLevel.INFO_YELLOW);
            return result > 0;
        } catch (Exception e) {
            Reporter.log("Failed to delete key: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Sets an expiration time on a key.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key     the key
     * @param seconds the expiration time in seconds
     * @return true if the expiration was set, false otherwise
     */
    @Override
    public boolean expire(String key, int seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Setting expiry for key: " + key + " to " + seconds + " seconds", LogLevel.INFO_YELLOW);
            boolean result = jedis.expire(key, seconds) == 1;
            Reporter.log("Expiry set successfully", LogLevel.INFO_YELLOW);
            return result;
        } catch (Exception e) {
            Reporter.log("Failed to set expiry for key: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    // Redis-specific methods below

    /**
     * Sets a key-value pair in Redis.
     *
     * @param key   the key
     * @param value the value
     * @throws RuntimeException if the operation fails
     */
    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Setting key: " + key, LogLevel.INFO_YELLOW);
            jedis.set(key, value);
            Reporter.log("Key set successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to set key: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to set key: " + key, e);
        }
    }

    /**
     * Sets a key-value pair in Redis with an expiration time.
     *
     * @param key     the key
     * @param value   the value
     * @param seconds the expiration time in seconds
     * @throws RuntimeException if the operation fails
     */
    public void setWithExpiry(String key, String value, int seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Setting key: " + key + " with expiry: " + seconds + " seconds", LogLevel.INFO_YELLOW);
            jedis.set(key, value, SetParams.setParams().ex(seconds));
            Reporter.log("Key set with expiry successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to set key with expiry: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to set key with expiry: " + key, e);
        }
    }

    /**
     * Gets the value associated with a key.
     *
     * @param key the key
     * @return the value, or null if not found
     */
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Getting value for key: " + key, LogLevel.INFO_YELLOW);
            String value = jedis.get(key);
            Reporter.log("Value retrieved successfully", LogLevel.INFO_YELLOW);
            return value;
        } catch (Exception e) {
            Reporter.log("Failed to get value for key: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Sets multiple hash fields in a single operation.
     *
     * @param key  the key
     * @param hash the hash map of field-value pairs
     * @throws RuntimeException if the operation fails
     */
    public void hset(String key, Map<String, String> hash) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Setting hash fields for key: " + key, LogLevel.INFO_YELLOW);
            jedis.hset(key, hash);
            Reporter.log("Hash fields set successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to set hash fields for key: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to set hash fields for key: " + key, e);
        }
    }

    /**
     * Gets all fields and values in a hash.
     *
     * @param key the key
     * @return a map of field-value pairs, or an empty map if not found
     */
    public Map<String, String> hgetAll(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Getting all hash fields for key: " + key, LogLevel.INFO_YELLOW);
            Map<String, String> result = jedis.hgetAll(key);
            Reporter.log("Hash fields retrieved successfully", LogLevel.INFO_YELLOW);
            return result;
        } catch (Exception e) {
            Reporter.log("Failed to get hash fields for key: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return Map.of();
        }
    }

    /**
     * Pushes values to the head of a list.
     *
     * @param key    the key
     * @param values the values to push
     * @throws RuntimeException if the operation fails
     */
    public void lpush(String key, String... values) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Pushing values to list: " + key, LogLevel.INFO_YELLOW);
            jedis.lpush(key, values);
            Reporter.log("Values pushed successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to push values to list: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to push values to list: " + key, e);
        }
    }

    /**
     * Gets a range of elements from a list.
     *
     * @param key   the key
     * @param start the start index
     * @param stop  the stop index
     * @return a list of elements, or an empty list if not found
     */
    public List<String> lrange(String key, int start, int stop) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Getting range from list: " + key, LogLevel.INFO_YELLOW);
            List<String> result = jedis.lrange(key, start, stop);
            Reporter.log("List range retrieved successfully", LogLevel.INFO_YELLOW);
            return result;
        } catch (Exception e) {
            Reporter.log("Failed to get range from list: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return List.of();
        }
    }

    /**
     * Adds members to a set.
     *
     * @param key     the key
     * @param members the members to add
     * @throws RuntimeException if the operation fails
     */
    public void sadd(String key, String... members) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Adding members to set: " + key, LogLevel.INFO_YELLOW);
            jedis.sadd(key, members);
            Reporter.log("Members added successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to add members to set: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to add members to set: " + key, e);
        }
    }

    /**
     * Gets all members of a set.
     *
     * @param key the key
     * @return a set of members, or an empty set if not found
     */
    public Set<String> smembers(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Getting members from set: " + key, LogLevel.INFO_YELLOW);
            Set<String> result = jedis.smembers(key);
            Reporter.log("Set members retrieved successfully", LogLevel.INFO_YELLOW);
            return result;
        } catch (Exception e) {
            Reporter.log("Failed to get members from set: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return Set.of();
        }
    }

    /**
     * Executes multiple commands in a pipeline.
     *
     * @param commands the commands to execute
     * @throws RuntimeException if the operation fails
     */
    public void pipeline(Consumer<Pipeline> commands) {
        try (Jedis jedis = jedisPool.getResource()) {
            Reporter.log("Executing pipeline commands", LogLevel.INFO_YELLOW);
            Pipeline pipeline = jedis.pipelined();
            commands.accept(pipeline);
            pipeline.syncAndReturnAll();
            Reporter.log("Pipeline commands executed successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to execute pipeline commands. Error: " + e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException("Failed to execute pipeline commands", e);
        }
    }
}
