package Ellithium.core.DB;
public interface NoSQLDatabaseProvider {
    Object executeQuery(String query);
    void closeConnection();
    void clearCache(String key);
    void clearAllCaches();
    void addToCache(String key, Object value);
    Object getFromCache(String key);
}
