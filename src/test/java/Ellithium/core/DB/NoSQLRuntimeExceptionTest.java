package Ellithium.core.DB;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NoSQLRuntimeExceptionTest {

    @Test
    public void testExceptionWithMessageOnly() {
        NoSQLRuntimeException ex = new NoSQLRuntimeException("NoSQL error occurred");
        Assert.assertEquals(ex.getMessage(), "NoSQL error occurred");
        Assert.assertNull(ex.getCause());
    }

    @Test
    public void testExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("Underlying failure");
        NoSQLRuntimeException ex = new NoSQLRuntimeException("Query failed", cause);
        Assert.assertEquals(ex.getMessage(), "Query failed");
        Assert.assertSame(ex.getCause(), cause);
    }
}
