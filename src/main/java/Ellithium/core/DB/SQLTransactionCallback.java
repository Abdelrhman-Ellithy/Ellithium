package Ellithium.core.DB;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SQLTransactionCallback<T> {
    T execute(Connection connection) throws SQLException;
}
