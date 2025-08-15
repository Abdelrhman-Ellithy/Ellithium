package DB;


import org.testng.annotations.*;

import javax.sql.rowset.CachedRowSet;
import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class SQLiteDBEdgeCasesTest extends SQLiteDBTest {

    @BeforeClass(dependsOnMethods = "setup")
    public void setupEdgeCaseData() {
        provider.createTable(
                "CREATE TABLE edge_cases (" +
                        "id INTEGER PRIMARY KEY," +
                        "unicode_text TEXT," +
                        "long_text TEXT," +
                        "numeric_value REAL," +
                        "binary_data BLOB)"
        );
    }

    @Test(groups = "edge")
    public void testUnicodeHandling() throws SQLException {
        String unicodeText = "Hello, 世界! Привет мир! مرحبا بالعالم!";
        provider.executeUpdate(
                "INSERT INTO edge_cases (unicode_text) VALUES ('" + unicodeText + "')"
        );

        CachedRowSet result = provider.executeQuery(
                "SELECT unicode_text FROM edge_cases WHERE id = last_insert_rowid()"
        );
        result.next();
        softAssert.assertEquals(result.getString(1), unicodeText, "Unicode text should be preserved");
        softAssert.assertAll();
    }

    @Test(groups = "edge")
    public void testLongTextHandling() throws SQLException {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("Long text test. "); // Use StringBuilder.append instead of String.concat
        }

        List<List<Object>> batchData = Arrays.asList(Arrays.asList(longText.toString()));
        provider.executeBatchInsert(
                "INSERT INTO edge_cases (long_text) VALUES (?)",
                batchData
        );

        CachedRowSet result = provider.executeQuery(
                "SELECT length(long_text) as text_length FROM edge_cases WHERE id = last_insert_rowid()"
        );
        result.next();
        softAssert.assertTrue(result.getInt(1) > 10000, "Long text should be stored completely");
        softAssert.assertAll();
    }

    @Test(groups = "edge")
    public void testNumericPrecision() throws SQLException {
        double smallNumber = 0.0000000001;
        double largeNumber = 1e308;

        List<List<Object>> batchData = Arrays.asList(
                Arrays.asList(smallNumber),
                Arrays.asList(largeNumber)
        );

        provider.executeBatchInsert(
                "INSERT INTO edge_cases (numeric_value) VALUES (?)",
                batchData
        );

        CachedRowSet result = provider.executeQuery(
                "SELECT numeric_value FROM edge_cases WHERE numeric_value > 0 ORDER BY numeric_value"
        );

        result.next();
        double delta = 1e-10; // More appropriate delta for floating point comparison
        softAssert.assertEquals(
                result.getDouble(1),
                smallNumber,
                delta,
                "Small number should be preserved"
        );

        result.next();
        double largeDelta = largeNumber * 1e-10; // Proportional delta for large numbers
        softAssert.assertEquals(
                result.getDouble(1),
                largeNumber,
                largeDelta,
                "Large number should be preserved"
        );

        softAssert.assertAll();
    }

    @Test(groups = "edge")
    public void testBinaryDataHandling() throws SQLException {
        byte[] binaryData = new byte[1024];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte) i;
        }

        List<List<Object>> batchData = Arrays.asList(Arrays.asList((Object) binaryData));

        int inserted = provider.executeBatchInsert(
                "INSERT INTO edge_cases (binary_data) VALUES (?)",
                batchData
        );
        softAssert.assertEquals(inserted, 1, "Should insert binary data record");

        CachedRowSet result = provider.executeQuery(
                "SELECT binary_data FROM edge_cases WHERE id = last_insert_rowid()"
        );
        result.next();

        // Use getObject() for SQLite BLOB data and cast to byte array
        byte[] retrievedData = (byte[]) result.getObject("binary_data");
        softAssert.assertTrue(Arrays.equals(retrievedData, binaryData),
                "Binary data should be preserved exactly");
        softAssert.assertAll();
    }

    @AfterClass
    public void cleanup() {
        if (provider != null) {
            provider.executeUpdate("DROP TABLE IF EXISTS edge_cases");
            provider.close();
        }
        new File(DB_PATH).delete();
    }
}