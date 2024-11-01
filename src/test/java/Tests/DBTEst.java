package Tests;

import Ellithium.core.DB.DBType;
import Ellithium.core.DB.SQLDatabaseProvider;
import org.testng.annotations.Test;

public class DBTEst {
    @Test
    public void testDB(){
        SQLDatabaseProvider db=new SQLDatabaseProvider(DBType.MY_SQL,"remote_user","Appy@innovate", "10.147.17.34","3306","webinvoice_bigdata");
        var rs=db.getColumnNames("item");
        System.out.println(rs);
    }
}
