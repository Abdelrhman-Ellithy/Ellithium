package Ellithium.core.DB;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SQLRuntimeExceptionTest {

    @Test
    public void testExceptionWithMessageAndCause() {
        Throwable cause = new IllegalArgumentException("Root cause");
        SQLRuntimeException ex = new SQLRuntimeException("DB operation failed", cause);

        Assert.assertEquals(ex.getMessage(), "DB operation failed");
        Assert.assertSame(ex.getCause(), cause);
    }
}
