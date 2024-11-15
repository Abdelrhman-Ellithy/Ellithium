package Tests;

import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.core.DB.SQLDBType;
import Ellithium.core.DB.SQLDatabaseProvider;
import org.testng.annotations.Test;

import java.util.List;

public class DBTEst {
    @Test
    public void testDB(){
        String username=JsonHelper.getJsonKeyValue("DB-Info","userName");
        String password=JsonHelper.getJsonKeyValue("DB-Info","password");
        String serverIp=JsonHelper.getJsonKeyValue("DB-Info","serverIp");
        String port=JsonHelper.getJsonKeyValue("DB-Info","port");
        String dbName=JsonHelper.getJsonKeyValue("DB-Info","dbName");
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
