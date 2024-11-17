package Tests;

import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.core.DB.SQLDBType;
import Ellithium.core.DB.SQLDatabaseProvider;
import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.assertTrue;
import static org.testng.Assert.*;

public class DBTEst extends NonBDDSetup {
    SQLDatabaseProvider db;
    @BeforeClass
    public void setup(){
        String username=JsonHelper.getJsonKeyValue("../DB-Info","userName");
        String password=JsonHelper.getJsonKeyValue("../DB-Info","password");
        String serverIp=JsonHelper.getJsonKeyValue("../DB-Info","serverIp");
        String port=JsonHelper.getJsonKeyValue("../DB-Info","port");
        String dbName=JsonHelper.getJsonKeyValue("../DB-Info","dbName");
        System.out.println(username+"\n"+password+"\n"+serverIp+"\n"+port+"\n"+dbName);
        db=new SQLDatabaseProvider(
                SQLDBType.MY_SQL,
                username,
                password,
                serverIp,
                port,
                dbName);
    }
    @Test
    public void testDB(){
        var rs=db.getColumnNames("item");
        System.out.println(rs); // ensure functionalities of this class, give this test, apply more on this class
    }
    @Test
    public void testExecuteQuery() {
        String query = "SELECT * FROM item LIMIT 1";
        CachedRowSet rs = db.executeQuery(query);
        assertNotNull(rs, "Query execution failed; result set is null");
        try {
            while (rs.next()) {
                System.out.println("Item ID: " + rs.getInt("id"));
            }
        }  catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void testGetRowCount() {
        int rowCount = db.getRowCount("item");
        assertTrue(rowCount >= 0, "Row count should not be negative");
        System.out.println("Row count for 'item' table: " + rowCount);
    }
    @Test
    public void testGetPrimaryKeys() {
        List<String> primaryKeys = db.getPrimaryKeys("item");
        assertNotNull(primaryKeys, "Primary keys should not be null");
        assertFalse(primaryKeys.isEmpty(), "Primary keys list should not be empty");
        System.out.println("Primary keys for 'item' table: " + primaryKeys);
    }
    @Test
    public void testGetColumnDataTypes() {
        Map<String, String> columnDataTypes = db.getColumnDataTypes("item");
        assertNotNull(columnDataTypes, "Column data types map should not be null");
        assertFalse(columnDataTypes.isEmpty(), "Column data types map should not be empty");
        columnDataTypes.forEach((column, type) ->
                System.out.println("Column: " + column + ", Type: " + type));
    }
    @Test
    public void testClearCacheForTable() {
        db.getColumnNames("item");
        db.clearCacheForTable("item");
        // Ensuring cache clear doesn't affect functionality
        List<String> columnNames = db.getColumnNames("item");
        assertNotNull(columnNames, "Column names retrieval after cache clear failed");
    }
    @Test
    public void testCachingBehavior() {
        db.clearCacheForTable("item");
        long startTime = System.nanoTime();
        List<String> columnNames = db.getColumnNames("item");
        long firstQueryTime = System.nanoTime() - startTime;
        Reporter.log(Long.toString(firstQueryTime), LogLevel.INFO_BLUE);
        startTime = System.nanoTime();
        List<String> cachedColumnNames = db.getColumnNames("item");
        long secondQueryTime = System.nanoTime() - startTime;
        Reporter.log(Long.toString(secondQueryTime), LogLevel.INFO_BLUE);
        assertEquals(columnNames, cachedColumnNames, "Cache did not return consistent results");
        assertTrue(secondQueryTime < firstQueryTime, "Cached query should be faster than the first query");
    }
    @AfterClass
    public void tareDown(){
        db.closeConnection();
    }
}
