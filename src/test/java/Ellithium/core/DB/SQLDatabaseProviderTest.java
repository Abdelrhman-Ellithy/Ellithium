package Ellithium.core.DB;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.sql.rowset.CachedRowSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class SQLDatabaseProviderTest {

    private File tempDbFile;
    private SQLDatabaseProvider dbProvider;

    @BeforeClass
    public void setUp() throws IOException {
        tempDbFile = File.createTempFile("ellithium_test_", ".db");
        tempDbFile.deleteOnExit();
        dbProvider = new SQLDatabaseProvider(SQLDBType.SQLITE, tempDbFile.getAbsolutePath());
    }

    @AfterClass
    public void tearDown() {
        if (dbProvider != null) {
            dbProvider.close();
        }
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test(priority = 1)
    public void testIsConnectionValidAndPoolStatistics() {
        Assert.assertTrue(dbProvider.isConnectionValid());
        HikariPoolStatistics stats = dbProvider.getPoolStatistics();
        Assert.assertNotNull(stats);
        Assert.assertNotNull(dbProvider.getDataSource());
        Assert.assertFalse(dbProvider.getDataSource().isClosed());
    }

    @Test(priority = 2)
    public void testCreateTableAndInsert() throws SQLException {
        String createSql = "CREATE TABLE test_users (" +
                "id INTEGER PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "email TEXT NOT NULL" +
                ");";
        dbProvider.createTable(createSql);

        boolean updated = dbProvider.executeUpdate(
                "INSERT INTO test_users (id, name, email) VALUES (1, 'Alice', 'alice@test.com');"
        );
        Assert.assertTrue(updated);

        boolean multiUpdated = dbProvider.executeUpdates(
                "INSERT INTO test_users (id, name, email) VALUES (2, 'Bob', 'bob@test.com');",
                "INSERT INTO test_users (id, name, email) VALUES (3, 'Charlie', 'charlie@test.com');"
        );
        Assert.assertTrue(multiUpdated);
    }

    @Test(priority = 3)
    public void testBatchInsert() throws SQLException {
        String batchSql = "INSERT INTO test_users (id, name, email) VALUES (?, ?, ?);";
        List<List<Object>> records = List.of(
                List.of(4, "Diana", "diana@test.com"),
                List.of(5, "Evan", "evan@test.com")
        );
        int rowsAffected = dbProvider.executeBatchInsert(batchSql, records);
        Assert.assertEquals(rowsAffected, 2);
    }

    @Test(priority = 4)
    public void testExecuteQueryAndTiming() throws SQLException {
        CachedRowSet rs = dbProvider.executeQuery("SELECT * FROM test_users WHERE id = 1;");
        Assert.assertTrue(rs.next());
        Assert.assertEquals(rs.getInt("id"), 1);
        Assert.assertEquals(rs.getString("name"), "Alice");
        Assert.assertEquals(rs.getString("email"), "alice@test.com");

        CachedRowSet rsTiming = dbProvider.executeQueryWithTiming("SELECT COUNT(*) FROM test_users;");
        Assert.assertTrue(rsTiming.next());
        Assert.assertEquals(rsTiming.getInt(1), 5);
    }

    @Test(priority = 5)
    public void testExecuteQueryOnNonExistentTableReturnsEmptyRowSet() throws SQLException {
        CachedRowSet rs = dbProvider.executeQuery("SELECT * FROM non_existent_table_xyz;");
        Assert.assertNotNull(rs);
        Assert.assertFalse(rs.next());
    }

    @Test(priority = 6)
    public void testMetadataAndCaching() {
        List<String> columns = dbProvider.getColumnNames("test_users");
        Assert.assertTrue(columns.contains("id"));
        Assert.assertTrue(columns.contains("name"));
        Assert.assertTrue(columns.contains("email"));

        // Verify cached retrieval
        List<String> columnsCached = dbProvider.getColumnNames("test_users");
        Assert.assertEquals(columnsCached, columns);

        int count = dbProvider.getRowCount("test_users");
        Assert.assertEquals(count, 5);

        Map<String, String> dataTypes = dbProvider.getColumnDataTypes("test_users");
        Assert.assertNotNull(dataTypes);
        Assert.assertTrue(dataTypes.containsKey("id"));

        List<String> pks = dbProvider.getPrimaryKeys("test_users");
        Assert.assertNotNull(pks);

        Map<String, String> fks = dbProvider.getForeignKeys("test_users");
        Assert.assertNotNull(fks);

        // Invalidation
        dbProvider.clearCacheForTable("test_users");
        dbProvider.clearAllCaches();
    }

    @Test(priority = 7)
    public void testTransactionsCommitAndRollback() throws SQLException {
        // Test commit
        Connection conn = dbProvider.beginTransaction();
        Assert.assertNotNull(conn);
        Assert.assertFalse(conn.getAutoCommit());
        dbProvider.commitTransaction();

        // Test rollback
        conn = dbProvider.beginTransaction();
        Assert.assertNotNull(conn);
        dbProvider.rollbackTransaction();

        // Test release leaked transaction
        dbProvider.beginTransaction();
        dbProvider.releaseLeakedTransaction();

        // Test executeInTransaction success
        String result = dbProvider.executeInTransaction(c -> {
            Assert.assertNotNull(c);
            return "SUCCESS";
        });
        Assert.assertEquals(result, "SUCCESS");

        // Test executeInTransaction rollback on exception
        Assert.assertThrows(SQLException.class, () -> {
            dbProvider.executeInTransaction(c -> {
                throw new SQLException("Simulated transaction error");
            });
        });
    }

    @Test(priority = 8)
    public void testBuildPaginatedQuery() {
        String base = "SELECT * FROM items";
        String sqlitePaged = dbProvider.buildPaginatedQuery(base, 0, 10, SQLDBType.SQLITE);
        Assert.assertTrue(sqlitePaged.contains("LIMIT 10 OFFSET 0"));

        String mysqlPaged = dbProvider.buildPaginatedQuery(base, 2, 25, SQLDBType.MY_SQL);
        Assert.assertTrue(mysqlPaged.contains("LIMIT 25 OFFSET 50"));

        String postgresPaged = dbProvider.buildPaginatedQuery(base, 1, 10, SQLDBType.POSTGRES_SQL);
        Assert.assertTrue(postgresPaged.contains("LIMIT 10 OFFSET 10"));

        String sqlServerPaged = dbProvider.buildPaginatedQuery(base, 0, 10, SQLDBType.SQL_SERVER);
        Assert.assertTrue(sqlServerPaged.contains("ROW_NUMBER()"));

        String oraclePaged = dbProvider.buildPaginatedQuery(base, 0, 10, SQLDBType.ORACLE_SID);
        Assert.assertTrue(oraclePaged.contains("ROWNUM"));

        Assert.assertThrows(UnsupportedOperationException.class, () -> {
            dbProvider.buildPaginatedQuery(base, 0, 10, SQLDBType.IBM_DB2);
        });
    }

    @Test(priority = 9)
    public void testValidationAndEdgeCases() {
        // Constructor validation
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new SQLDatabaseProvider(SQLDBType.MY_SQL, tempDbFile.getAbsolutePath());
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new SQLDatabaseProvider(SQLDBType.SQLITE, "");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new SQLDatabaseProvider(null, "user", "pass", "127.0.0.1", "3306", "db");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new SQLDatabaseProvider(SQLDBType.POSTGRES_SQL, "user", "pass", "", "5432", "db");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new SQLDatabaseProvider(SQLDBType.POSTGRES_SQL, "user", "pass", "127.0.0.1", "invalid_port", "db");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new SQLDatabaseProvider(SQLDBType.POSTGRES_SQL, "user", "pass", "127.0.0.1", "999999", "db");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new SQLDatabaseProvider(SQLDBType.POSTGRES_SQL, "user", "pass", "127.0.0.1", "5432", "");
        });

        // Query sanitization
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            dbProvider.executeQuery(null);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            dbProvider.executeQuery("   ");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            dbProvider.executeQuery("SELECT * FROM test_users WHERE");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            dbProvider.executeQuery("SELECT * FROM test_users; DROP TABLE test_users;");
        });

        // Table name validation
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            dbProvider.getColumnNames("");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            dbProvider.getColumnNames("invalid-table-name;");
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            dbProvider.getRowCount("test users");
        });
    }
}
