package Ellithium.core.DB;

/**
 * Custom runtime exception for NoSQL database operations.
 */
public class NoSQLRuntimeException extends RuntimeException {
    public NoSQLRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSQLRuntimeException(String message) {
        super(message);
    }
}
