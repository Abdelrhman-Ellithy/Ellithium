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
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class SQLDatabaseProvider implements AutoCloseable {
    private HikariDataSource dataSource;
    private final String userName;
    private final String password;
    private final String port;
    private final String dataBaseName;
    private final String serverIP;
    private final SQLDBType dbType;
    private final Cache<String, List<String>> columnNamesCache;
    private final Cache<String, Integer> rowCountCache;
    private final Cache<String, Map<String, String>> columnDataTypesCache;
    private final Cache<String, List<String>> primaryKeysCache;
    private final Cache<String, Map<String, String>> foreignKeysCache;
    private static final Map<SQLDBType, String> dbTypeConnectionMap = new HashMap<>();
    private static final Map<SQLDBType, Properties> dbTypeProperties = new HashMap<>();
    private ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    static {
        dbTypeConnectionMap.put(SQLDBType.MY_SQL, "jdbc:mysql://");
        dbTypeConnectionMap.put(SQLDBType.SQL_SERVER, "jdbc:sqlserver://");
        dbTypeConnectionMap.put(SQLDBType.POSTGRES_SQL, "jdbc:postgresql://");
        dbTypeConnectionMap.put(SQLDBType.ORACLE_SID, "jdbc:oracle:thin:@");
        dbTypeConnectionMap.put(SQLDBType.ORACLE_SERVICE_NAME, "jdbc:oracle:thin:@//");
        dbTypeConnectionMap.put(SQLDBType.IBM_DB2, "jdbc:db2://");
        dbTypeConnectionMap.put(SQLDBType.SQLITE, "jdbc:sqlite:");

        // Initialize database-specific properties
        Properties mysqlProps = new Properties();
        mysqlProps.setProperty("useSSL", "false");
        mysqlProps.setProperty("serverTimezone", "UTC");
        mysqlProps.setProperty("rewriteBatchedStatements", "true");
        dbTypeProperties.put(SQLDBType.MY_SQL, mysqlProps);

        Properties postgresProps = new Properties();
        postgresProps.setProperty("ApplicationName", "Ellithium");
        postgresProps.setProperty("reWriteBatchedInserts", "true");
        dbTypeProperties.put(SQLDBType.POSTGRES_SQL, postgresProps);

        Properties sqlServerProps = new Properties();
        sqlServerProps.setProperty("trustServerCertificate", "true");
        sqlServerProps.setProperty("sendStringParametersAsUnicode", "true");
        dbTypeProperties.put(SQLDBType.SQL_SERVER, sqlServerProps);

        Properties oracleProps = new Properties();
        oracleProps.setProperty("oracle.jdbc.implicitStatementCacheSize", "true");
        dbTypeProperties.put(SQLDBType.ORACLE_SID, oracleProps);
        dbTypeProperties.put(SQLDBType.ORACLE_SERVICE_NAME, oracleProps);
    }

    public SQLDatabaseProvider(SQLDBType dbType, String userName, String password, String serverIP, String port, String dataBaseName) {
        validateConfiguration(dbType, userName, password, serverIP, port, dataBaseName);
        this.userName = userName;
        this.password = password;
        this.port = port;
        this.dataBaseName = dataBaseName;
        this.serverIP = serverIP;
        this.dbType = dbType;

        // Initialize caches first
        columnNamesCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        rowCountCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        columnDataTypesCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        primaryKeysCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        foreignKeysCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();

        // Initialize the connection pool
        HikariConfig config = createHikariConfig(dbType, dbTypeConnectionMap.get(this.dbType) + this.serverIP + ":" + this.port + "/" + this.dataBaseName);
        this.dataSource = new HikariDataSource(config);
        Reporter.log("Initialized SQLDatabaseProvider with HikariCP connection pool.", LogLevel.INFO_BLUE);
    }

    public SQLDatabaseProvider(SQLDBType dbType, String pathToSQLiteDataBase) {
        // Add validation at the start
        if (dbType != SQLDBType.SQLITE) {
            throw new IllegalArgumentException("This constructor is only for SQLite databases");
        }
        if (pathToSQLiteDataBase == null || pathToSQLiteDataBase.trim().isEmpty()) {
            throw new IllegalArgumentException("SQLite database path cannot be empty");
        }

        this.userName = null;
        this.password = null;
        this.port = null;
        this.dataBaseName = pathToSQLiteDataBase;
        this.serverIP = null;
        this.dbType = dbType;

        // Initialize caches first
        columnNamesCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        rowCountCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        columnDataTypesCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        primaryKeysCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();
        foreignKeysCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();

        // Register SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Reporter.log("Failed to load SQLite JDBC driver", LogLevel.ERROR);
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }

        // Initialize the connection pool
        HikariConfig config = createHikariConfig(dbType, dbTypeConnectionMap.get(dbType) + dataBaseName);
        this.dataSource = new HikariDataSource(config);
        Reporter.log("Initialized SQLDatabaseProvider with HikariCP connection pool.", LogLevel.INFO_BLUE);
    }

    private HikariConfig createHikariConfig(SQLDBType dbType, String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);

        // SQLite-specific configurations need different pool settings
        if (dbType == SQLDBType.SQLITE) {
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(5000); // Lower timeout for SQLite
            config.setIdleTimeout(300000);     // 5 minutes
            config.setMaxLifetime(600000);     // 10 minutes
            config.setAutoCommit(true);        // Important for SQLite
            Properties sqliteProps = new Properties();
            sqliteProps.setProperty("foreign_keys", "ON");
            sqliteProps.setProperty("journal_mode", "WAL");
            sqliteProps.setProperty("synchronous", "NORMAL"); // Better performance
            sqliteProps.setProperty("busy_timeout", "10000"); // 10 seconds
            config.setDataSourceProperties(sqliteProps);
        } else {
            // Default pool settings for other databases
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
        }

        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000);

        // Set database-specific properties
        Properties dbProps = dbTypeProperties.get(dbType);
        if (dbProps != null) {
            config.setDataSourceProperties(dbProps);
        }

        config.setConnectionTestQuery(getValidationQuery(dbType));
        return config;
    }

    private String getValidationQuery(SQLDBType dbType) {
        switch (dbType) {
            case MY_SQL:
            case POSTGRES_SQL:
            case SQLITE:
                return "SELECT 1";
            case SQL_SERVER:
                return "SELECT 1";
            case ORACLE_SID:
            case ORACLE_SERVICE_NAME:
                return "SELECT 1 FROM DUAL";
            case IBM_DB2:
                return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
            default:
                return "SELECT 1";
        }
    }

    public Connection beginTransaction() throws SQLException {
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        transactionConnection.set(conn);
        Reporter.log("Transaction started.", LogLevel.INFO_BLUE);
        return conn;
    }

    public void commitTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            try {
                conn.commit();
                Reporter.log("Transaction committed successfully.", LogLevel.INFO_BLUE);
            } finally {
                conn.close();
                transactionConnection.remove();
            }
        }
    }

    public void rollbackTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            try {
                conn.rollback();
                Reporter.log("Transaction rolled back.", LogLevel.INFO_BLUE);
            } finally {
                conn.close();
                transactionConnection.remove();
            }
        }
    }

    public <T> T executeInTransaction(SQLTransactionCallback<T> callback) throws SQLException {
        Connection conn = beginTransaction();
        try {
            T result = callback.execute(conn);
            commitTransaction();
            return result;
        } catch (Exception e) {
            rollbackTransaction();
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException("Transaction failed", e);
            }
        }
    }
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Reporter.log("Database connection pool closed.", LogLevel.INFO_BLUE);
        }
    }

    public boolean isConnectionValid() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            Reporter.log("Connection validation failed: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    public HikariPoolStatistics getPoolStatistics() {
        if (dataSource == null || dataSource.isClosed()) {
            throw new IllegalStateException("Connection pool is not available");
        }
        return new HikariPoolStatistics(
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Builds a paginated query for the specified database type.
     * Note: For SQL Server and Oracle, the baseQuery must include an ORDER BY clause.
     */
    public String buildPaginatedQuery(String baseQuery, int page, int pageSize, SQLDBType dbType) {
        switch (dbType) {
            case MY_SQL:
            case SQLITE:
                return baseQuery + " LIMIT " + pageSize + " OFFSET " + (page * pageSize);
            case POSTGRES_SQL:
                return baseQuery + " LIMIT " + pageSize + " OFFSET " + (page * pageSize);
            case SQL_SERVER:
                return "SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS RowNum, * FROM (" + 
                       baseQuery + ") AS BaseQuery) AS RowConstrainedResult WHERE RowNum > " + 
                       (page * pageSize) + " AND RowNum <= " + ((page + 1) * pageSize);
            case ORACLE_SID:
            case ORACLE_SERVICE_NAME:
                return "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (" + baseQuery + 
                       ") a WHERE ROWNUM <= " + ((page + 1) * pageSize) + 
                       ") WHERE rnum > " + (page * pageSize);
            default:
                throw new UnsupportedOperationException("Pagination not supported for database type: " + dbType);
        }
    }

    public CachedRowSet executeQuery(String query) throws SQLException {
        validateConnection();
        query = sanitizeQuery(query);
        
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            try {
                ResultSet resultSet = statement.executeQuery(query);
                rowSet.populate(resultSet);
                Reporter.log("Executed query successfully: " + query, LogLevel.INFO_BLUE);
                return rowSet;
            } catch (SQLException e) {
                String errorMsg = e.getMessage().toLowerCase();
                if (errorMsg.contains("no such table") || 
                    errorMsg.contains("table not found") ||
                    errorMsg.contains("does not exist")) {
                    return rowSet; // Return empty result set for missing tables
                }
                if (errorMsg.contains("syntax error") ||
                    errorMsg.contains("sql error")) {
                    throw new SQLException("Invalid SQL syntax: " + e.getMessage(), e);
                }
                throw e;
            }
        } catch (SQLException e) {
            handleSQLException("Query execution failed", e);
            throw e;
        }
    }

    private void validateConnection() {
        if (!isConnectionValid()) {
            throw new IllegalStateException("Database connection is not valid");
        }
    }

    private void handleSQLException(String message, SQLException e) {
        String errorDetail = e.getMessage() != null ? e.getMessage() : "Unknown error";
        String fullMessage = message + ": " + errorDetail;
        
        Reporter.log(fullMessage, LogLevel.ERROR);
        Reporter.log("SQL State: " + e.getSQLState(), LogLevel.ERROR);
        Reporter.log("Error Code: " + e.getErrorCode(), LogLevel.ERROR);
        
        if (e.getNextException() != null) {
            Reporter.log("Nested Exception: " + e.getNextException().getMessage(), LogLevel.ERROR);
        }
        
        throw new SQLRuntimeException(fullMessage, e);
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Reporter.log("Closed connection pool.", LogLevel.INFO_BLUE);
        }
    }

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
    public List<String> getColumnNames(String catalog, String schema, String tableName) {
        String cacheKey = String.format("%s.%s.%s", catalog, schema, tableName);
        return columnNamesCache.get(cacheKey, key -> {
            List<String> columnNames = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 ResultSet columns = connection.getMetaData().getColumns(catalog, schema, tableName, null)) {
                while (columns.next()) {
                    columnNames.add(columns.getString("COLUMN_NAME"));
                }
            } catch (SQLException e) {
                // Handle exception
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
        int totalRowsAffected = 0;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (List<Object> record : records) {
                    for (int i = 0; i < record.size(); i++) {
                        statement.setObject(i + 1, record.get(i));
                    }
                    statement.addBatch();
                }
                int[] batchResults = statement.executeBatch();
                connection.commit();
                for (int rows : batchResults) {
                    totalRowsAffected += rows > 0 ? rows : 0;
                }
            } catch (SQLException e) {
                connection.rollback();
                handleSQLException("Batch insert failed", e);
            }
        } catch (SQLException e) {
            handleSQLException("Database connection error during batch insert", e);
        }
        return totalRowsAffected;
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

    public CachedRowSet executeQueryWithTiming(String query) throws SQLException {
        long startTime = System.currentTimeMillis();
        CachedRowSet result = executeQuery(query);
        long endTime = System.currentTimeMillis();
        Reporter.log("Query executed in " + (endTime - startTime) + " ms: " + query, LogLevel.INFO_BLUE);
        return result;
    }

    public boolean executeUpdate(String sql) {
        try (Connection connection = dataSource.getConnection()) {
            if (dbType == SQLDBType.SQLITE) {
                int retries = 5;
                while (retries > 0) {
                    try (Statement statement = connection.createStatement()) {
                        int result = statement.executeUpdate(sql);
                        Reporter.log("Executed update successfully: " + sql, LogLevel.INFO_BLUE);
                        return result > 0;
                    } catch (SQLException e) {
                        if (e.getMessage().contains("database is locked") && --retries > 0) {
                            Thread.sleep(1000);
                            continue;
                        }
                        throw e;
                    }
                }
            }
            try (Statement statement = connection.createStatement()) {
                int result = statement.executeUpdate(sql);
                Reporter.log("Executed update successfully: " + sql, LogLevel.INFO_BLUE);
                return result > 0;
            }
        } catch (SQLException e) {
            handleSQLException("Failed to execute update: " + sql, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLRuntimeException("Update interrupted", e);
        }
    }

    public boolean executeUpdates(String... sqlStatements) {
        boolean success = true;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (String sql : sqlStatements) {
                    statement.executeUpdate(sql);
                }
                connection.commit();
                Reporter.log("Executed multiple updates successfully", LogLevel.INFO_BLUE);
            } catch (SQLException e) {
                connection.rollback();
                handleSQLException("Failed to execute updates, rolling back", e);
                success = false;
            }
        } catch (SQLException e) {
            handleSQLException("Database connection error", e);
            success = false;
        }
        return success;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    private void validateConfiguration(SQLDBType dbType, String... params) {
        if (dbType == null) {
            throw new IllegalArgumentException("Database type cannot be null");
        }

        switch (dbType) {
            case SQLITE:
                if (params[0] == null || params[0].trim().isEmpty()) {
                    throw new IllegalArgumentException("SQLite database path cannot be empty");
                }
                break;
            default:
                if (params[2] == null || params[2].trim().isEmpty()) { // serverIP
                    throw new IllegalArgumentException("Server address cannot be empty");
                }
                if (params[3] != null) {
                    validatePort(params[3]);
                }
                if (params[4] == null || params[4].trim().isEmpty()) { // database name
                    throw new IllegalArgumentException("Database name cannot be empty");
                }
        }
    }

    private void validatePort(String port) {
        try {
            int portNum = Integer.parseInt(port);
            if (portNum <= 0 || portNum > 65535) {
                throw new IllegalArgumentException("Invalid port number: " + port);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port format: " + port);
        }
    }

    private String sanitizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        String sanitized = query.trim();
        if (sanitized.endsWith("WHERE") || 
            sanitized.endsWith("FROM") || 
            sanitized.endsWith("AND") || 
            sanitized.endsWith("OR")) {
            throw new IllegalArgumentException("Invalid SQL syntax: Incomplete or invalid statement");
        }
        if (sanitized.contains(";") && !sanitized.endsWith(";")) {
            throw new IllegalArgumentException("Multiple SQL statements are not allowed");
        }
        return sanitized;
    }
}