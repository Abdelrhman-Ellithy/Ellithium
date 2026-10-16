package Ellithium.core.DB;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SQLDBTypeTest {

    @Test
    public void testEnumValuesCount() {
        Assert.assertEquals(SQLDBType.values().length, 7);
    }

    @Test
    public void testEnumConstants() {
        Assert.assertEquals(SQLDBType.valueOf("MY_SQL"), SQLDBType.MY_SQL);
        Assert.assertEquals(SQLDBType.valueOf("SQL_SERVER"), SQLDBType.SQL_SERVER);
        Assert.assertEquals(SQLDBType.valueOf("POSTGRES_SQL"), SQLDBType.POSTGRES_SQL);
        Assert.assertEquals(SQLDBType.valueOf("ORACLE_SID"), SQLDBType.ORACLE_SID);
        Assert.assertEquals(SQLDBType.valueOf("ORACLE_SERVICE_NAME"), SQLDBType.ORACLE_SERVICE_NAME);
        Assert.assertEquals(SQLDBType.valueOf("IBM_DB2"), SQLDBType.IBM_DB2);
        Assert.assertEquals(SQLDBType.valueOf("SQLITE"), SQLDBType.SQLITE);
    }
}
