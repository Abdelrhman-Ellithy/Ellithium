package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SQLDatabaseProvider {
    private HikariDataSource dataSource;
    private String userName;
    private String password;
    private String port;
    private String dataBaseName;
    private String serverIP;
    private DBType dbType;

    private final Cache<String, List<String>> columnNamesCache;
    private final Cache<String, Integer> rowCountCache;
    private final Cache<String, Map<String, String>> columnDataTypesCache;
    private final Cache<String, List<String>> primaryKeysCache;
    private final Cache<String, Map<String, String>> foreignKeysCache;

    private static final Map<DBType, String> dbTypeConnectionMap = new HashMap<>();

    static {
        dbTypeConnectionMap.put(DBType.MY_SQL, "jdbc:mysql://");
        dbTypeConnectionMap.put(DBType.SQL_SERVER, "jdbc:sqlserver://");
        dbTypeConnectionMap.put(DBType.POSTGRES_SQL, "jdbc:postgresql://");
        dbTypeConnectionMap.put(DBType.ORACLE, "jdbc:oracle:thin:@");
        dbTypeConnectionMap.put(DBType.ORACLE_SERVICE_NAME, "jdbc:oracle:thin:@//");
        dbTypeConnectionMap.put(DBType.IBM_DB2, "jdbc:db2://");
    }

    public SQLDatabaseProvider(String userName, String password, String port, String dataBaseName, String serverIP, DBType dbType) {
        this.userName = userName;
        this.password = password;
        this.port = port;
        this.dataBaseName = dataBaseName;
        this.serverIP = serverIP;
        this.dbType = dbType;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbTypeConnectionMap.get(dbType) + serverIP + ":" + port + "/" + dataBaseName);
        config.setUsername(userName);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
        Reporter.log("Initialized SQLDatabaseProvider with HikariCP connection pool.", LogLevel.INFO_BLUE);

        columnNamesCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        rowCountCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        columnDataTypesCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        primaryKeysCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        foreignKeysCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
    }
    public CachedRowSet executeQuery(String query) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
            rowSet.populate(resultSet);

            Reporter.log("Executed query successfully: " + query, LogLevel.INFO_BLUE, "Success");
            return rowSet;

        } catch (SQLException e) {
            Reporter.log("Failed to execute query: " + query, LogLevel.ERROR);
            Reporter.log("SQL State: " + e.getSQLState(), LogLevel.ERROR);
            Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Reporter.log("Closed connection pool.", LogLevel.INFO_BLUE);
        }
    }

    // Utility Methods

    public List<String> getColumnNames(String tableName) {
        return columnNamesCache.get(tableName, key -> {
            List<String> columnNames = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, null)) {

                while (columns.next()) {
                    columnNames.add(columns.getString("COLUMN_NAME"));
                }
                Reporter.log("Retrieved column names for table: " + tableName, LogLevel.INFO_BLUE);

            } catch (SQLException e) {
                Reporter.log("Failed to retrieve column names for table: " + tableName, LogLevel.ERROR);
                Reporter.log("SQL State: " + e.getSQLState(), LogLevel.ERROR);
                Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
            }
            return columnNames;
        });
    }

    public int getRowCount(String tableName) {
        return rowCountCache.get(tableName, key -> {
            String query = "SELECT COUNT(*) FROM " + tableName;
            int rowCount = 0;
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {

                if (resultSet.next()) {
                    rowCount = resultSet.getInt(1);
                }
                Reporter.log("Row count for table " + tableName + ": " + rowCount, LogLevel.INFO_BLUE);

            } catch (SQLException e) {
                Reporter.log("Failed to retrieve row count for table: " + tableName, LogLevel.ERROR);
                Reporter.log("SQL State: " + e.getSQLState(), LogLevel.ERROR);
                Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
            }
            return rowCount;
        });
    }

    public Map<String, String> getColumnDataTypes(String tableName) {
        return columnDataTypesCache.get(tableName, key -> {
            Map<String, String> columnDataTypes = new HashMap<>();
            try (Connection connection = dataSource.getConnection();
                 ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, null)) {

                while (columns.next()) {
                    columnDataTypes.put(columns.getString("COLUMN_NAME"), columns.getString("TYPE_NAME"));
                }
                Reporter.log("Retrieved column data types for table: " + tableName, LogLevel.INFO_BLUE);

            } catch (SQLException e) {
                Reporter.log("Failed to retrieve column data types for table: " + tableName, LogLevel.ERROR);
                Reporter.log("SQL State: " + e.getSQLState(), LogLevel.ERROR);
                Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
            }
            return columnDataTypes;
        });
    }

    public List<String> getPrimaryKeys(String tableName) {
        return primaryKeysCache.get(tableName, key -> {
            List<String> primaryKeys = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 ResultSet pkResultSet = connection.getMetaData().getPrimaryKeys(null, null, tableName)) {

                while (pkResultSet.next()) {
                    primaryKeys.add(pkResultSet.getString("COLUMN_NAME"));
                }
                Reporter.log("Retrieved primary keys for table: " + tableName, LogLevel.INFO_BLUE);

            } catch (SQLException e) {
                Reporter.log("Failed to retrieve primary keys for table: " + tableName, LogLevel.ERROR);
                Reporter.log("SQL State: " + e.getSQLState(), LogLevel.ERROR);
                Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
            }
            return primaryKeys;
        });
    }

    public Map<String, String> getForeignKeys(String tableName) {
        return foreignKeysCache.get(tableName, key -> {
            Map<String, String> foreignKeys = new HashMap<>();
            try (Connection connection = dataSource.getConnection();
                 ResultSet fkResultSet = connection.getMetaData().getImportedKeys(null, null, tableName)) {

                while (fkResultSet.next()) {
                    foreignKeys.put(fkResultSet.getString("FKCOLUMN_NAME"), fkResultSet.getString("PKTABLE_NAME"));
                }
                Reporter.log("Retrieved foreign keys for table: " + tableName, LogLevel.INFO_BLUE);

            } catch (SQLException e) {
                Reporter.log("Failed to retrieve foreign keys for table: " + tableName, LogLevel.ERROR);
                Reporter.log("SQL State: " + e.getSQLState(), LogLevel.ERROR);
                Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
            }
            return foreignKeys;
        });
    }
    public int executeBatchInsert(String query, List<List<Object>> records) {
        int[] result;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            for (List<Object> record : records) {
                for (int i = 0; i < record.size(); i++) {
                    statement.setObject(i + 1, record.get(i));
                }
                statement.addBatch();
            }
            result = statement.executeBatch();
            Reporter.log("Batch insert executed successfully.", LogLevel.INFO_BLUE);
        } catch (SQLException e) {
            Reporter.log("Failed to execute batch insert.", LogLevel.ERROR);
            Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
            return 0;
        }
        return result.length;
    }
    public void beginTransaction() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            Reporter.log("Transaction started.", LogLevel.INFO_BLUE);
        } catch (SQLException e) {
            Reporter.log("Failed to begin transaction.", LogLevel.ERROR);
        }
    }

    public void commitTransaction() {
        try (Connection connection = dataSource.getConnection()) {
            connection.commit();
            Reporter.log("Transaction committed successfully.", LogLevel.INFO_BLUE);
        } catch (SQLException e) {
            Reporter.log("Failed to commit transaction.", LogLevel.ERROR);
        }
    }

    public void rollbackTransaction() {
        try (Connection connection = dataSource.getConnection()) {
            connection.rollback();
            Reporter.log("Transaction rolled back.", LogLevel.INFO_BLUE);
        } catch (SQLException e) {
            Reporter.log("Failed to rollback transaction.", LogLevel.ERROR);
        }
    }
    public void createTable(String createTableSQL) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
            Reporter.log("Table created successfully with SQL: " + createTableSQL, LogLevel.INFO_BLUE);
        } catch (SQLException e) {
            Reporter.log("Failed to create table with SQL: " + createTableSQL, LogLevel.ERROR);
            Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
        }
    }
    public void clearCacheForTable(String tableName) {
        columnNamesCache.invalidate(tableName);
        rowCountCache.invalidate(tableName);
        columnDataTypesCache.invalidate(tableName);
        primaryKeysCache.invalidate(tableName);
        foreignKeysCache.invalidate(tableName);
        Reporter.log("Cache cleared for table: " + tableName, LogLevel.INFO_BLUE);
    }

    public void clearAllCaches() {
        columnNamesCache.invalidateAll();
        rowCountCache.invalidateAll();
        columnDataTypesCache.invalidateAll();
        primaryKeysCache.invalidateAll();
        foreignKeysCache.invalidateAll();
        Reporter.log("All caches cleared.", LogLevel.INFO_BLUE);
    }
    public CachedRowSet executeQueryWithTiming(String query) {
        long startTime = System.currentTimeMillis();
        CachedRowSet result = executeQuery(query);
        long endTime = System.currentTimeMillis();
        Reporter.log("Query executed in " + (endTime - startTime) + " ms: " + query, LogLevel.INFO_BLUE);
        return result;
    }
}