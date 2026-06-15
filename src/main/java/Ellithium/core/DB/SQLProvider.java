package Ellithium.core.DB;

import javax.sql.rowset.CachedRowSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface SQLProvider extends AutoCloseable {

    CachedRowSet executeQuery(String query) throws SQLException;

    boolean executeUpdate(String sql);

    boolean executeUpdates(String... sqlStatements);

    int executeBatchInsert(String query, List<List<Object>> records);

    Connection beginTransaction() throws SQLException;

    void commitTransaction() throws SQLException;

    void rollbackTransaction() throws SQLException;

    <T> T executeInTransaction(SQLTransactionCallback<T> callback) throws SQLException;

    List<String> getColumnNames(String tableName);

    int getRowCount(String tableName);

    Map<String, String> getColumnDataTypes(String tableName);

    List<String> getPrimaryKeys(String tableName);

    Map<String, String> getForeignKeys(String tableName);

    boolean isConnectionValid();

    void closeConnection();

    @Override
    void close();
}
