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

/**
 * Provides database operations with connection pooling and caching capabilities.
 * Supports multiple SQL database types including MySQL, PostgreSQL, SQLite, and Oracle.
 */
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

    /**
     * Initializes a new database provider for standard SQL databases.
     * @param dbType The type of database to connect to
     * @param userName Database username
     * @param password Database password
     * @param serverIP Database server IP address
     * @param port Database port number
     * @param dataBaseName Name of the database
     */
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

    /**
     * Initializes a new database provider specifically for SQLite databases.
     * @param dbType Must be SQLDBType.SQLITE
     * @param pathToSQLiteDataBase Path to the SQLite database file
     * @throws IllegalArgumentException if dbType is not SQLite or path is invalid
     */
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
            throw new SQLRuntimeException("SQLite JDBC driver not found", e);
        }

        // Initialize the connection pool
        HikariConfig config = createHikariConfig(dbType, dbTypeConnectionMap.get(dbType) + dataBaseName);
        this.dataSource = new HikariDataSource(config);
        Reporter.log("Initialized SQLDatabaseProvider with HikariCP connection pool.", LogLevel.INFO_BLUE);
    }

    /**
     * Creates a HikariCP configuration for the specified database type.
     * @param dbType The type of database
     * @param jdbcUrl The JDBC URL for the database
     * @return Configured HikariConfig object
     */
    private HikariConfig createHikariConfig(SQLDBType dbType, String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);

        Properties mergedProps = new Properties();

        // SQLite-specific configurations need different pool settings
        if (dbType == SQLDBType.SQLITE) {
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(5000);
            config.setIdleTimeout(300000);     // 5 minutes
            config.setMaxLifetime(600000);     // 10 minutes
            config.setAutoCommit(true);        // Important for SQLite
            Properties sqliteProps = new Properties();
            sqliteProps.setProperty("foreign_keys", "ON");
            sqliteProps.setProperty("journal_mode", "WAL");
            sqliteProps.setProperty("synchronous", "NORMAL");
            sqliteProps.setProperty("busy_timeout", "10000");
            mergedProps.putAll(sqliteProps);
        } else {
            config.setUsername(userName);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
        }

        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000);
        Properties dbProps = dbTypeProperties.get(dbType);
        if (dbProps != null) {
            mergedProps.putAll(dbProps);
        }
        if (!mergedProps.isEmpty()) {
            config.setDataSourceProperties(mergedProps);
        }

        config.setConnectionTestQuery(getValidationQuery(dbType));
        return config;
    }

    /**
     * Gets the appropriate validation query for the database type.
     * @param dbType The type of database
     * @return SQL query string for validation
     */
    private String getValidationQuery(SQLDBType dbType) {
        return switch (dbType) {
            case ORACLE_SID, ORACLE_SERVICE_NAME -> "SELECT 1 FROM DUAL";
            case IBM_DB2 -> "SELECT 1 FROM SYSIBM.SYSDUMMY1";
            default -> "SELECT 1";
        };
    }

    /**
     * Starts a new database transaction.
     * @return Active database connection
     * @throws SQLException if transaction cannot be started
     */
    public Connection beginTransaction() throws SQLException {
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        transactionConnection.set(conn);
        Reporter.log("Transaction started.", LogLevel.INFO_BLUE);
        return conn;
    }

    /**
     * Commits the current transaction.
     * @throws SQLException if commit fails
     */
    public void commitTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            try (conn) {
                conn.commit();
                Reporter.log("Transaction committed successfully.", LogLevel.INFO_BLUE);
            } finally {
                transactionConnection.remove();
            }
        }
    }

    /**
     * Rolls back the current transaction.
     * @throws SQLException if rollback fails
     */
    public void rollbackTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            try (conn) {
                conn.rollback();
                Reporter.log("Transaction rolled back.", LogLevel.INFO_BLUE);
            } finally {
                transactionConnection.remove();
            }
        }
    }

    /**
     * Executes code within a transaction context.
     * @param callback Code to execute within transaction
     * @param <T> Return type of the callback
     * @return Result of the callback execution
     * @throws SQLException if transaction operations fail
     */
    public <T> T executeInTransaction(SQLTransactionCallback<T> callback) throws SQLException {
        Connection conn = beginTransaction();
        try {
            T result = callback.execute(conn);
            commitTransaction();
            return result;
        } catch (SQLException e) {
            rollbackTransaction();
            throw e;
        } catch (Exception e) {
            rollbackTransaction();
            throw new SQLException("Transaction failed", e);
        }
    }

    /**
     * Closes the database provider and its resources.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Reporter.log("Database connection pool closed.", LogLevel.INFO_BLUE);
        }
    }

    /**
     * Checks if the database connection is valid.
     * @return true if connection is valid, false otherwise
     */
    public boolean isConnectionValid() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            Reporter.log("Connection validation failed: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Gets statistics about the connection pool.
     * @return HikariPoolStatistics object containing pool metrics
     * @throws IllegalStateException if pool is not available
     */
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
     * Constructs a paginated query based on database type.
     * @param baseQuery Original SQL query
     * @param page Page number (0-based)
     * @param pageSize Number of records per page
     * @param dbType Database type for pagination syntax
     * @return Modified query with pagination
     */
    public String buildPaginatedQuery(String baseQuery, int page, int pageSize, SQLDBType dbType) {
        return switch (dbType) {
            case MY_SQL, SQLITE, POSTGRES_SQL -> baseQuery + " LIMIT " + pageSize + " OFFSET " + (page * pageSize);
            case SQL_SERVER -> "SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS RowNum, * FROM (" +
                    baseQuery + ") AS BaseQuery) AS RowConstrainedResult WHERE RowNum > " +
                    (page * pageSize) + " AND RowNum <= " + ((page + 1) * pageSize);
            case ORACLE_SID, ORACLE_SERVICE_NAME -> "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (" + baseQuery +
                    ") a WHERE ROWNUM <= " + ((page + 1) * pageSize) +
                    ") WHERE rnum > " + (page * pageSize);
            default -> throw new UnsupportedOperationException("Pagination not supported for database type: " + dbType);
        };
    }

    /**
     * Executes a SQL query and returns the results.
     * @param query SQL query to execute
     * @return CachedRowSet containing query results
     * @throws SQLException if query execution fails
     */
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

    /**
     * Validates the current database connection.
     * @throws IllegalStateException if connection is invalid
     */
    private void validateConnection() {
        if (!isConnectionValid()) {
            throw new IllegalStateException("Database connection is not valid");
        }
    }

    /**
     * Handles SQL exceptions with proper logging and wrapping.
     * @param message Error message
     * @param e Original SQLException
     * @throws SQLRuntimeException wrapped exception
     */
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

    /**
     * Closes the active database connection.
     * Releases all resources and shuts down the connection pool.
     */
    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Reporter.log("Closed connection pool.", LogLevel.INFO_BLUE);
        }
    }

    /**
     * Gets the names of columns in a specific table.
     * Results are cached for improved performance.
     * @param tableName The name of the table
     * @return List of column names
     */
    public List<String> getColumnNames(String tableName) {
        validateTableName(tableName);
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

    /**
     * Gets column names for a table in a specific catalog and schema.
     * @param catalog The catalog name
     * @param schema The schema name
     * @param tableName The table name
     * @return List of column names
     */
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
                Reporter.log("Failed to retrieve column names for table: " + tableName, LogLevel.ERROR);
                Reporter.log("SQL State: " + e.getSQLState(), LogLevel.ERROR);
                Reporter.log("Error Message: " + e.getMessage(), LogLevel.ERROR);
            }
            return columnNames;
        });
    }

    /**
     * Gets the total number of rows in a table.
     * Results are cached for improved performance.
     * @param tableName The name of the table
     * @return Number of rows in the table
     */
    public int getRowCount(String tableName) {
        return rowCountCache.get(tableName, key -> {
            int rowCount = 0;
            // Table name is already validated by validateTableName
            String query = "SELECT COUNT(*) FROM " + tableName;
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement();
                 ResultSet resultSet = stmt.executeQuery(query)) {
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

    /**
     * Gets the data types of columns in a table.
     * Results are cached for improved performance.
     * @param tableName The name of the table
     * @return Map of column names to their SQL data types
     */
    public Map<String, String> getColumnDataTypes(String tableName) {
        validateTableName(tableName);
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

    /**
     * Gets the primary key columns of a table.
     * Results are cached for improved performance.
     * @param tableName The name of the table
     * @return List of primary key column names
     */
    public List<String> getPrimaryKeys(String tableName) {
        validateTableName(tableName);
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

    /**
     * Gets the foreign key relationships of a table.
     * Results are cached for improved performance.
     * @param tableName The name of the table
     * @return Map of foreign key columns to their referenced tables
     */
    public Map<String, String> getForeignKeys(String tableName) {
        validateTableName(tableName);
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

    /**
     * Executes a batch insert operation.
     * @param query The SQL insert query with placeholders
     * @param records List of record values to insert
     * @return Total number of rows affected
     */
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
                    totalRowsAffected += Math.max(rows, 0);
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

    /**
     * Creates a new table in the database.
     * @param createTableSQL The CREATE TABLE SQL statement
     */
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

    /**
     * Clears all cached data for a specific table.
     * @param tableName The name of the table
     */
    public void clearCacheForTable(String tableName) {
        columnNamesCache.invalidate(tableName);
        rowCountCache.invalidate(tableName);
        columnDataTypesCache.invalidate(tableName);
        primaryKeysCache.invalidate(tableName);
        foreignKeysCache.invalidate(tableName);
        Reporter.log("Cache cleared for table: " + tableName, LogLevel.INFO_BLUE);
    }

    /**
     * Clears all cached data for all tables.
     */
    public void clearAllCaches() {
        columnNamesCache.invalidateAll();
        rowCountCache.invalidateAll();
        columnDataTypesCache.invalidateAll();
        primaryKeysCache.invalidateAll();
        foreignKeysCache.invalidateAll();
        Reporter.log("All caches cleared.", LogLevel.INFO_BLUE);
    }

    /**
     * Executes a query and measures its execution time.
     * @param query The SQL query to execute
     * @return CachedRowSet containing the query results
     * @throws SQLException if query execution fails
     */
    public CachedRowSet executeQueryWithTiming(String query) throws SQLException {
        long startTime = System.currentTimeMillis();
        CachedRowSet result = executeQuery(query);
        long endTime = System.currentTimeMillis();
        Reporter.log("Query executed in " + (endTime - startTime) + " ms: " + query, LogLevel.INFO_BLUE);
        return result;
    }

    /**
     * Executes an update SQL statement with retry logic for SQLite.
     * @param sql The SQL update statement
     * @return true if at least one row was affected, false otherwise
     */
    public boolean executeUpdate(String sql) {
        try (Connection connection = dataSource.getConnection()) {
            if (dbType == SQLDBType.SQLITE) {
                byte retries = 5;
                while (retries > 0) {
                    try (Statement statement = connection.createStatement()) {
                        int result = statement.executeUpdate(sql);
                        Reporter.log("Executed update successfully: " + sql, LogLevel.INFO_BLUE);
                        return result > 0;
                    } catch (SQLException e) {
                        if (e.getMessage().contains("database is locked") && (--retries) > 0) {
                            Thread.sleep(1000);
                        }
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

    /**
     * Executes multiple SQL statements as a single transaction.
     * @param sqlStatements Variable number of SQL statements to execute
     * @return true if all statements executed successfully, false otherwise
     */
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

    /**
     * Gets the underlying HikariDataSource instance.
     * @return The configured HikariDataSource
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Validates the database configuration parameters.
     * @param dbType The type of database
     * @param params Variable array of configuration parameters
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateConfiguration(SQLDBType dbType, String... params) {
        if (dbType == null) {
            throw new IllegalArgumentException("Database type cannot be null");
        }
        if (dbType == SQLDBType.SQLITE) {
            if (params[0] == null || params[0].trim().isEmpty()) {
                throw new IllegalArgumentException("SQLite database path cannot be empty");
            }
        } else {
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

    /**
     * Validates a port number string.
     * @param port The port number as a string
     * @throws IllegalArgumentException if port is invalid
     */
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

    /**
     * Sanitizes and validates an SQL query.
     * @param query The SQL query to sanitize
     * @return The sanitized query
     * @throws IllegalArgumentException if query is invalid
     */
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

    /**
     * Validates a table name.
     * @param tableName The table name to validate
     * @throws IllegalArgumentException if table name is invalid
     */
    private void validateTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        // Add regex pattern to ensure table name contains only valid characters
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name format");
        }
    }
}