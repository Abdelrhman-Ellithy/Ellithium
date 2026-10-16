package Ellithium.core.DB;

/**
 * Interface for NoSQL database providers with common operations and caching capabilities.
 * Provides a unified interface for different NoSQL database implementations.
 */
public interface NoSQLDatabaseProvider {
    /**
     * Executes a query against the database.
     *
     * @param query the query to execute
     * @return the query result, or null if not found
     */
    Object executeQuery(String query);

    /**
     * Closes the database connection.
     */
    void closeConnection();

    /**
     * Clears the cache for a specific key.
     *
     * @param key the cache key to clear
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    default void clearCache(String key) {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * Clears all cached query results.
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    default void clearAllCaches() {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * Adds a query result to the cache.
     *
     * @param key   the cache key
     * @param value the value to cache
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    default void addToCache(String key, Object value) {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * Retrieves a cached query result by key.
     *
     * @param key the cache key
     * @return null as caching is disabled
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    default Object getFromCache(String key) {
        return null;
    }

    /**
     * Performs a health check against the database server.
     *
     * @return true if the connection is healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Checks if a key/document exists in the database.
     *
     * @param key the key/document identifier to check
     * @return true if the key/document exists, false otherwise
     */
    boolean exists(String key);

    /**
     * Deletes a key/document from the database.
     *
     * @param key the key/document identifier to delete
     * @return true if the key/document was deleted, false otherwise
     */
    boolean delete(String key);

    /**
     * Sets an expiration time on a key/document.
     *
     * @param key     the key/document identifier
     * @param seconds the expiration time in seconds
     * @return true if the expiration was set, false otherwise
     */
    boolean expire(String key, int seconds);
}
