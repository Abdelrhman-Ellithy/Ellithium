package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedded SQLite storage for Tier 1 baseline fingerprints.
 *
 * <p>Provides ACID guarantees, page-level atomic writes with WAL (Write-Ahead Logging),
 * and indexed lookups, avoiding full-file JSON rewriting during test executions.</p>
 */
public class BaselineSqliteStorage {

    public static final String DEFAULT_DB_FILE = "Test-Output" + File.separator + "healing-baselines.db";
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type LIST_TYPE = new TypeToken<List<ElementFingerprint>>() {}.getType();

    private final Path dbPath;
    private final String jdbcUrl;
    private volatile boolean initialized = false;

    private static final BaselineSqliteStorage DEFAULT_INSTANCE =
            new BaselineSqliteStorage(Paths.get(DEFAULT_DB_FILE));

    public static BaselineSqliteStorage getDefault() {
        return DEFAULT_INSTANCE;
    }

    public BaselineSqliteStorage(Path dbPath) {
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    public Path getDbPath() {
        return dbPath;
    }

    public synchronized void ensureInitialized() {
        if (initialized) return;
        try {
            if (dbPath.getParent() != null) {
                Files.createDirectories(dbPath.getParent());
            }
            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS baselines ("
                        + "locator_key TEXT PRIMARY KEY, "
                        + "fingerprints_json TEXT NOT NULL, "
                        + "last_seen_epoch INTEGER NOT NULL);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_baselines_last_seen ON baselines(last_seen_epoch);");
            }
            initialized = true;
        } catch (Exception e) {
            Reporter.log("BaselineSqliteStorage: Failed to initialize schema: " + e.getMessage(), LogLevel.WARN);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL;");
            s.execute("PRAGMA synchronous=NORMAL;");
            s.execute("PRAGMA busy_timeout=5000;");
        }
        return conn;
    }

    public boolean isEmpty() {
        if (!Files.exists(dbPath)) return true;
        ensureInitialized();
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM baselines")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: isEmpty check failed: " + e.getMessage(), LogLevel.DEBUG);
        }
        return true;
    }

    public int count() {
        if (!Files.exists(dbPath)) return 0;
        ensureInitialized();
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM baselines")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: count check failed: " + e.getMessage(), LogLevel.DEBUG);
        }
        return 0;
    }

    public Map<String, List<ElementFingerprint>> loadAll() {
        if (!Files.exists(dbPath)) return Collections.emptyMap();
        ensureInitialized();
        Map<String, List<ElementFingerprint>> result = new LinkedHashMap<>();
        String sql = "SELECT locator_key, fingerprints_json FROM baselines ORDER BY last_seen_epoch ASC";
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString(1);
                String json = rs.getString(2);
                try {
                    List<ElementFingerprint> fps = GSON.fromJson(json, LIST_TYPE);
                    if (fps != null && !fps.isEmpty()) {
                        result.put(key, List.copyOf(fps));
                    }
                } catch (Exception parseErr) {
                    Reporter.log("BaselineSqliteStorage: Failed to deserialize fingerprint for " + key, LogLevel.DEBUG);
                }
            }
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: loadAll failed: " + e.getMessage(), LogLevel.WARN);
        }
        return result;
    }

    public List<ElementFingerprint> get(String locatorKey) {
        if (locatorKey == null || !Files.exists(dbPath)) return null;
        ensureInitialized();
        String sql = "SELECT fingerprints_json FROM baselines WHERE locator_key = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, locatorKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString(1);
                    return GSON.fromJson(json, LIST_TYPE);
                }
            }
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: get failed for key " + locatorKey + ": " + e.getMessage(), LogLevel.DEBUG);
        }
        return null;
    }

    public void upsert(String locatorKey, List<ElementFingerprint> fingerprints) {
        if (locatorKey == null || fingerprints == null || fingerprints.isEmpty()) return;
        ensureInitialized();
        long maxEpoch = fingerprints.stream()
                .mapToLong(ElementFingerprint::getLastSeenEpoch)
                .max().orElse(System.currentTimeMillis());
        String json = GSON.toJson(fingerprints);
        String sql = "INSERT INTO baselines (locator_key, fingerprints_json, last_seen_epoch) "
                + "VALUES (?, ?, ?) "
                + "ON CONFLICT(locator_key) DO UPDATE SET "
                + "fingerprints_json = excluded.fingerprints_json, "
                + "last_seen_epoch = excluded.last_seen_epoch;";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, locatorKey);
            ps.setString(2, json);
            ps.setLong(3, maxEpoch);
            ps.executeUpdate();
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: upsert failed for key " + locatorKey + ": " + e.getMessage(), LogLevel.WARN);
        }
    }

    public void upsertBatch(Map<String, List<ElementFingerprint>> entries) {
        if (entries == null || entries.isEmpty()) return;
        ensureInitialized();
        String sql = "INSERT INTO baselines (locator_key, fingerprints_json, last_seen_epoch) "
                + "VALUES (?, ?, ?) "
                + "ON CONFLICT(locator_key) DO UPDATE SET "
                + "fingerprints_json = excluded.fingerprints_json, "
                + "last_seen_epoch = excluded.last_seen_epoch;";
        try (Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<String, List<ElementFingerprint>> entry : entries.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) continue;
                    long maxEpoch = entry.getValue().stream()
                            .mapToLong(ElementFingerprint::getLastSeenEpoch)
                            .max().orElse(System.currentTimeMillis());
                    String json = GSON.toJson(entry.getValue());
                    ps.setString(1, entry.getKey());
                    ps.setString(2, json);
                    ps.setLong(3, maxEpoch);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: upsertBatch failed: " + e.getMessage(), LogLevel.WARN);
        }
    }

    public boolean delete(String locatorKey) {
        if (locatorKey == null || !Files.exists(dbPath)) return false;
        ensureInitialized();
        String sql = "DELETE FROM baselines WHERE locator_key = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, locatorKey);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: delete failed for key " + locatorKey + ": " + e.getMessage(), LogLevel.DEBUG);
            return false;
        }
    }

    public void deleteAll() {
        if (!Files.exists(dbPath)) return;
        ensureInitialized();
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM baselines;");
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: deleteAll failed: " + e.getMessage(), LogLevel.DEBUG);
        }
    }

    public int pruneStale(long cutoffEpoch) {
        if (cutoffEpoch <= 0 || !Files.exists(dbPath)) return 0;
        ensureInitialized();
        String sql = "DELETE FROM baselines WHERE last_seen_epoch < ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cutoffEpoch);
            return ps.executeUpdate();
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: pruneStale failed: " + e.getMessage(), LogLevel.DEBUG);
            return 0;
        }
    }

    public int pruneByCount(int maxLocators) {
        if (maxLocators <= 0 || !Files.exists(dbPath)) return 0;
        ensureInitialized();
        try (Connection conn = openConnection()) {
            int currentCount;
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT count(*) FROM baselines")) {
                currentCount = rs.next() ? rs.getInt(1) : 0;
            }
            if (currentCount <= maxLocators) return 0;
            int toEvict = currentCount - maxLocators;
            String sql = "DELETE FROM baselines WHERE locator_key IN ("
                    + "SELECT locator_key FROM baselines ORDER BY last_seen_epoch ASC LIMIT ?);";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, toEvict);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            Reporter.log("BaselineSqliteStorage: pruneByCount failed: " + e.getMessage(), LogLevel.DEBUG);
            return 0;
        }
    }
}
