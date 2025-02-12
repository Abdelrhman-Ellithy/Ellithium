package DB;

import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.DB.SQLDBType;
import Ellithium.core.DB.SQLDatabaseProvider;
import Ellithium.core.DB.SQLRuntimeException;
import Ellithium.core.base.NonBDDSetup;
import org.testng.annotations.*;

import javax.sql.rowset.CachedRowSet;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;

public class SQLiteDBTest extends NonBDDSetup {
    protected SQLDatabaseProvider provider;
    protected static final String DB_PATH = "src/test/resources/TestData/test_provider.db";
    protected AssertionExecutor.soft softAssert;

    @BeforeMethod
    public void beforeEachTest() {
        softAssert = new AssertionExecutor.soft();
    }

    @BeforeClass
    public void setup() {
        File dbFile = new File(DB_PATH);
        if (dbFile.exists()) dbFile.delete();
        provider = new SQLDatabaseProvider(SQLDBType.SQLITE, DB_PATH);
        provider.createTable("CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT)");
    }

    @Test
    public void testConnectionPoolManagement() throws SQLException {
        softAssert.assertNotNull(provider.getDataSource(), "DataSource should be initialized");
        softAssert.assertTrue(provider.isConnectionValid(), "Connection should be valid");

        var stats = provider.getPoolStatistics();
        softAssert.assertEquals(stats.getActiveConnections(), 0, "No active connections initially");
        softAssert.assertTrue(stats.getTotalConnections() <= 10, "Total connections within limit");
        try (Connection conn = provider.getDataSource().getConnection()) {
            softAssert.assertNotNull(conn, "Should get connection from pool");
            softAssert.assertFalse(conn.isClosed(), "Connection should be open");
        }
        softAssert.assertAll();
    }

    @Test
    public void testCacheManagement() throws SQLException {

        List<String> columnNames = provider.getColumnNames("test_table");
        List<String> cachedColumnNames = provider.getColumnNames("test_table");
        softAssert.assertEquals(columnNames, cachedColumnNames, "Should return cached column names");

        provider.clearCacheForTable("test_table");
        List<String> newColumnNames = provider.getColumnNames("test_table");
        softAssert.assertEquals(newColumnNames, columnNames, "Should reload cache after clearing");

        Map<String, String> columnTypes = provider.getColumnDataTypes("test_table");
        softAssert.assertNotNull(columnTypes.get("id"), "Should cache column types");
        softAssert.assertEquals(columnTypes.get("id"), "INTEGER", "Should cache correct type");

        softAssert.assertAll();
    }

    @Test
    public void testErrorHandling() {
        // Test invalid table name
        List<String> columns = provider.getColumnNames("nonexistent_table");
        softAssert.assertTrue(columns.isEmpty(), "Should handle nonexistent table gracefully");

        try {
            provider.executeQuery("SELECT * FROM test_table WHERE;");
            softAssert.fail("Should throw exception for invalid SQL");
        } catch (SQLException | SQLRuntimeException e) {
            String errorMsg = e.getMessage().toLowerCase();
            softAssert.assertTrue(
                errorMsg.contains("syntax error") || 
                errorMsg.contains("invalid sql") ||
                errorMsg.contains("incomplete input") ||
                errorMsg.contains("sql error"),
                "Should provide meaningful error message: " + errorMsg
            );
        }

        try {
            CachedRowSet result = provider.executeQuery("SELECT * FROM nonexistent_table");
            softAssert.assertTrue(!result.next(), "Should return empty result for nonexistent table");
        } catch (SQLException | SQLRuntimeException e) {
            String errorMsg = e.getMessage().toLowerCase();
            softAssert.assertTrue(
                errorMsg.contains("no such table") ||
                errorMsg.contains("table not found") ||
                errorMsg.contains("does not exist"),
                "Should provide meaningful error for missing table: " + errorMsg
            );
        }

        softAssert.assertAll();
    }

    @Test
    public void testTransactionManagement() throws SQLException {

        try {
            provider.executeInTransaction(conn -> {
                conn.createStatement().executeUpdate("INSERT INTO test_table (name) VALUES ('test')");
                return true;
            });
            softAssert.assertTrue(true, "Transaction should complete successfully");
        } catch (SQLException e) {
            softAssert.fail("Transaction should not fail: " + e.getMessage());
        }

        try {
            provider.executeInTransaction(conn -> {
                throw new SQLException("Test rollback");
            });
            softAssert.fail("Should throw exception");
        } catch (SQLException e) {
            softAssert.assertEquals(e.getMessage(), "Test rollback", "Should rollback on exception");
        }

        softAssert.assertAll();
    }

    @Test
    public void testConnectionValidation() {
        softAssert.assertTrue(provider.isConnectionValid(), "Connection should be valid initially");
        try {
            provider.executeUpdate("PRAGMA locking_mode = EXCLUSIVE");
            provider.executeUpdate("BEGIN EXCLUSIVE");
            softAssert.assertTrue(provider.isConnectionValid(), "Connection should remain valid under lock");
            provider.executeUpdate("COMMIT");
        } catch (Exception e) {
            softAssert.fail("Should handle database locks gracefully");
        }

        softAssert.assertAll();
    }

    @Test
    public void testResourceManagement() throws InterruptedException {

        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    provider.executeQuery("SELECT * FROM test_table");
                    latch.countDown();
                } catch (SQLException e) {
                    softAssert.fail("Concurrent queries should not fail: " + e.getMessage());
                }
            });
        }
        softAssert.assertTrue(latch.await(5, TimeUnit.SECONDS), "All queries should complete");
        executor.shutdown();
        softAssert.assertAll();
    }

    @Test
    public void testConfigurationValidation() {
        // Test empty database path
        Exception emptyPathException = null;
        try {
            new SQLDatabaseProvider(SQLDBType.SQLITE, "");
        } catch (IllegalArgumentException e) {
            emptyPathException = e;
        }
        softAssert.assertNotNull(emptyPathException, "Should throw exception for empty path");
        softAssert.assertTrue(
            emptyPathException != null && 
            emptyPathException.getMessage().contains("cannot be empty"),
            "Should have correct error message for empty path"
        );

        // Test invalid MySQL configuration
        Exception invalidMySQLException = null;
        try {
            new SQLDatabaseProvider(SQLDBType.MY_SQL, null, null, "", "invalid", null);
        } catch (IllegalArgumentException e) {
            invalidMySQLException = e;
        }
        softAssert.assertNotNull(invalidMySQLException, "Should throw exception for invalid MySQL config");
        softAssert.assertTrue(
            invalidMySQLException != null && 
            (invalidMySQLException.getMessage().contains("port") || 
             invalidMySQLException.getMessage().contains("address")),
            "Should have correct error message for invalid MySQL config"
        );

        softAssert.assertAll();
    }

    @Test(groups = "metadata")
    public void testMetadataOperations() throws SQLException {
        Map<String, String> columnTypes = provider.getColumnDataTypes("test_table");
        softAssert.assertNotNull(columnTypes, "Column types should not be null");
        softAssert.assertTrue(columnTypes.size() >= 2, "Should have at least 2 columns");
        List<String> primaryKeys = provider.getPrimaryKeys("test_table");
        softAssert.assertTrue(primaryKeys.contains("id"), "Should identify primary key");
        Map<String, String> cachedTypes = provider.getColumnDataTypes("test_table");
        softAssert.assertEquals(cachedTypes.toString(), columnTypes.toString(), "Cached types should match");

        softAssert.assertAll();
    }

    @Test(groups = "queries")
    public void testComplexQueries() throws SQLException {
        // Setup test data
        provider.executeUpdate("INSERT INTO test_table (name) VALUES ('test1')");
        provider.executeUpdate("INSERT INTO test_table (name) VALUES ('test2')");

        // Test pagination
        String baseQuery = "SELECT * FROM test_table";
        String paginatedQuery = provider.buildPaginatedQuery(baseQuery, 0, 1, SQLDBType.SQLITE);
        CachedRowSet result = provider.executeQuery(paginatedQuery);
        
        softAssert.assertTrue(result.next(), "Should have first record");
        softAssert.assertFalse(result.next(), "Should not have second record");

        softAssert.assertAll();
    }

    @Test(groups = "transactions")
    public void testAdvancedTransactions() throws SQLException {
        // Use executeInTransaction for better transaction handling
        try {
            provider.executeInTransaction(conn -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("INSERT INTO test_table (name) VALUES ('trans1')");
                }
                return true;
            });
            
            // Verify the insert
            CachedRowSet result = provider.executeQuery("SELECT COUNT(*) FROM test_table WHERE name = 'trans1'");
            result.next();
            softAssert.assertEquals(result.getInt(1), 1, "Main transaction should be committed");

        } catch (SQLException e) {
            softAssert.fail("Transaction failed: " + e.getMessage());
        }

        softAssert.assertAll();
    }

    @Test(groups = "performance")
    public void testBatchOperations() throws SQLException {
        // Test large batch insert
        List<List<Object>> batchData = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            batchData.add(Arrays.asList("batch" + i));
        }

        long start = System.currentTimeMillis();
        int inserted = provider.executeBatchInsert(
            "INSERT INTO test_table (name) VALUES (?)",
            batchData
        );
        long end = System.currentTimeMillis();

        softAssert.assertEquals(inserted, 1000, "Should insert all records");
        softAssert.assertTrue((end - start) < 5000, "Batch insert should complete within 5 seconds");

        // Verify with count
        CachedRowSet result = provider.executeQuery("SELECT COUNT(*) FROM test_table WHERE name LIKE 'batch%'");
        result.next();
        softAssert.assertEquals(result.getInt(1), 1000, "Should find all batch inserted records");

        softAssert.assertAll();
    }

    @Test(groups = "error")
    public void testErrorRecovery() {
        // Test invalid SQL
        try {
            provider.executeQuery("SELECT * FROM nonexistent_table");
            CachedRowSet result = provider.executeQuery("SELECT * FROM test_table");
            softAssert.assertTrue(result.next(), "Should recover and execute valid query");
        } catch (SQLException e) {
            softAssert.assertTrue(e.getMessage().contains("no such table"), 
                "Should provide meaningful error message");
        }

        // Test connection recovery
        softAssert.assertTrue(provider.isConnectionValid(), 
            "Connection should remain valid after error");

        softAssert.assertAll();
    }

    @Test(groups = "pooling")
    public void testConnectionPoolBehavior() throws SQLException, InterruptedException {
        // Test connection reuse
        Connection conn1 = provider.getDataSource().getConnection();
        String conn1Wrapped = ((org.sqlite.jdbc4.JDBC4Connection)conn1.unwrap(Connection.class)).toString();
        conn1.close();

        Connection conn2 = provider.getDataSource().getConnection();
        String conn2Wrapped = ((org.sqlite.jdbc4.JDBC4Connection)conn2.unwrap(Connection.class)).toString();
        conn2.close();

        softAssert.assertEquals(conn1Wrapped, conn2Wrapped, "Underlying connections should be reused from pool");

        // Test pool statistics
        var stats = provider.getPoolStatistics();
        softAssert.assertTrue(stats.getIdleConnections() > 0, "Should have idle connections");
        softAssert.assertEquals(stats.getActiveConnections(), 0, "Should have no active connections");

        softAssert.assertAll();
    }

    @Test(groups = "configuration")
    public void testProviderConfiguration() {
        // Test invalid configurations
        try {
            new SQLDatabaseProvider(SQLDBType.MY_SQL, null, null, null, "invalid", null);
            softAssert.fail("Should reject invalid configuration");
        } catch (IllegalArgumentException e) {
            softAssert.assertTrue(e.getMessage().contains("Server address cannot be empty") 
                || e.getMessage().contains("Invalid port"), "Should provide validation error");
        }

        // Test SQLite validation
        try {
            new SQLDatabaseProvider(SQLDBType.SQLITE, "");
            softAssert.fail("Should reject empty SQLite path");
        } catch (IllegalArgumentException e) {
            softAssert.assertTrue(e.getMessage().contains("cannot be empty"), 
                "Should validate SQLite path");
        }

        softAssert.assertAll();
    }

    @AfterClass
    public void cleanup() {
        if (provider != null) {
            provider.close();
        }
        new File(DB_PATH).delete();
    }
}