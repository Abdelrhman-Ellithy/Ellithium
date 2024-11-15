package Tests;

import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.core.DB.SQLDBType;
import Ellithium.core.DB.SQLDatabaseProvider;
import org.testng.annotations.Test;

import java.util.List;

public class DBTEst {
    @Test
    public void testDB(){
        String username=JsonHelper.getJsonKeyValue("src/test/resources/TestData/DB-Info","userName");
        String password=JsonHelper.getJsonKeyValue("src/test/resources/TestData/DB-Info","password");
        String serverIp=JsonHelper.getJsonKeyValue("src/test/resources/TestData/DB-Info","serverIp");
        String port=JsonHelper.getJsonKeyValue("src/test/resources/TestData/DB-Info","port");
        String dbName=JsonHelper.getJsonKeyValue("src/test/resources/TestData/DB-Info","dbName");
        SQLDatabaseProvider db=new SQLDatabaseProvider(
                SQLDBType.MY_SQL,
                username,
                password,
                serverIp,
                port,
                dbName);
        var rs=db.getColumnNames("item");
        System.out.println(rs);
    }
}
