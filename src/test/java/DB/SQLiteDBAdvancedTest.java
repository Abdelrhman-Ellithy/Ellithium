package DB;

import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.DB.SQLDBType;
import Ellithium.core.DB.SQLDatabaseProvider;
import org.testng.annotations.*;

import javax.sql.rowset.CachedRowSet;
import java.io.File;
import java.sql.SQLException;
import java.util.*;

public class SQLiteDBAdvancedTest extends SQLiteDBTest {

    @BeforeClass(dependsOnMethods = "setup")
    public void setupTestData() throws SQLException {
        // First drop tables if they exist
        provider.executeUpdate("DROP TABLE IF EXISTS orders");
        provider.executeUpdate("DROP TABLE IF EXISTS products");

        provider.createTable(
                "CREATE TABLE products (" +
                        "id INTEGER PRIMARY KEY," +
                        "name TEXT NOT NULL," +
                        "price REAL," +
                        "stock INTEGER," +
                        "description TEXT," +
                        "active BOOLEAN DEFAULT 1)"
        );

        provider.createTable(
                "CREATE TABLE orders (" +
                        "order_id INTEGER PRIMARY KEY," +
                        "product_id INTEGER," +
                        "quantity INTEGER," +
                        "order_date TEXT," +
                        "FOREIGN KEY(product_id) REFERENCES products(id))"
        );

        // Insert test data with known state
        List<List<Object>> productData = Arrays.asList(
                Arrays.asList("Product A", 10.99, 100),
                Arrays.asList("Product B", 20.50, 50),
                Arrays.asList("Product C", 15.75, 75)
        );

        provider.executeBatchInsert(
                "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)",
                productData
        );
    }

    @Test(groups = "query", priority = 1)  // Add priority to ensure this runs early
    public void testSelectWithWhere() throws SQLException {
        // First ensure we have a known state by cleaning up any test data
        // Delete orders first due to foreign key constraints
        provider.executeUpdate("DELETE FROM orders");
        provider.executeUpdate("DELETE FROM products");

        // Insert fresh test data
        List<List<Object>> productData = Arrays.asList(
                Arrays.asList("Product A", 10.99, 100),
                Arrays.asList("Product B", 20.50, 50),
                Arrays.asList("Product C", 15.75, 75)
        );

        provider.executeBatchInsert(
                "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)",
                productData
        );

        // Test the query
        CachedRowSet result = provider.executeQuery(
                "SELECT name, price FROM products WHERE price > 20"
        );

        List<String> products = new ArrayList<>();
        while (result.next()) {
            products.add(result.getString("name"));
        }

        softAssert.assertEquals(products.size(), 1, "Should find one product above $20");
        softAssert.assertTrue(products.contains("Product B"), "Should find Product B");
        softAssert.assertAll();
    }

    @Test(groups = "query")
    public void testAggregateQueries() throws SQLException {
        // Test COUNT
        CachedRowSet countResult = provider.executeQuery("SELECT COUNT(*) as total FROM products");
        countResult.next();
        softAssert.assertEquals(countResult.getInt("total"), 3, "Should have 3 products");

        // Test AVG
        CachedRowSet avgResult = provider.executeQuery("SELECT AVG(price) as avg_price FROM products");
        avgResult.next();
        double avgPrice = avgResult.getDouble("avg_price");
        softAssert.assertTrue(avgPrice > 15.00 && avgPrice < 16.00, "Average price should be around 15.75");

        // Test GROUP BY
        CachedRowSet groupResult = provider.executeQuery(
                "SELECT stock > 50 as high_stock, COUNT(*) as count FROM products GROUP BY high_stock"
        );
        Map<Boolean, Integer> stockGroups = new HashMap<>();
        while (groupResult.next()) {
            stockGroups.put(groupResult.getBoolean(1), groupResult.getInt(2));
        }
        softAssert.assertEquals(stockGroups.get(true), 2, "Should have 2 products with high stock");

        softAssert.assertAll();
    }

    @Test(groups = "update")
    public void testUpdateOperations() throws SQLException {
        // First reset all data
        provider.executeUpdate("UPDATE products SET active = 1");
        provider.executeUpdate("UPDATE products SET stock = CASE " +
                "WHEN name = 'Product A' THEN 100 " +
                "WHEN name = 'Product B' THEN 50 " +
                "WHEN name = 'Product C' THEN 75 END");

        // Test UPDATE with WHERE
        provider.executeUpdate("UPDATE products SET stock = stock + 10 WHERE name = 'Product A'");

        CachedRowSet result = provider.executeQuery("SELECT stock FROM products WHERE name = 'Product A'");
        result.next();
        softAssert.assertEquals(result.getInt("stock"), 110, "Stock should be updated");

        // Test UPDATE multiple rows with more specific criteria
        provider.executeUpdate("UPDATE products SET active = 0 WHERE name IN ('Product B', 'Product C')");

        result = provider.executeQuery("SELECT COUNT(*) as count FROM products WHERE active = 0");
        result.next();
        softAssert.assertEquals(result.getInt("count"), 2, "Should update exactly two rows");

        softAssert.assertAll();
    }

    @Test(groups = "delete")
    public void testDeleteOperations() throws SQLException {
        // Setup test data
        provider.executeUpdate("INSERT INTO products (name, price, stock) VALUES ('Temp Product', 9.99, 10)");

        // Test DELETE with WHERE
        provider.executeUpdate("DELETE FROM products WHERE name = 'Temp Product'");

        CachedRowSet result = provider.executeQuery("SELECT COUNT(*) as count FROM products WHERE name = 'Temp Product'");
        result.next();
        softAssert.assertEquals(result.getInt("count"), 0, "Record should be deleted");

        softAssert.assertAll();
    }

    @Test(groups = "joins")
    public void testJoinQueries() throws SQLException {
        // Setup order data
        provider.executeUpdate(
                "INSERT INTO orders (product_id, quantity, order_date) VALUES (1, 5, '2024-02-10')"
        );
        provider.executeUpdate(
                "INSERT INTO orders (product_id, quantity, order_date) VALUES (2, 3, '2024-02-10')"
        );

        // Test INNER JOIN
        CachedRowSet result = provider.executeQuery(
                "SELECT p.name, o.quantity " +
                        "FROM products p " +
                        "INNER JOIN orders o ON p.id = o.product_id"
        );

        List<String> orderDetails = new ArrayList<>();
        while (result.next()) {
            orderDetails.add(result.getString("name") + ":" + result.getInt("quantity"));
        }

        softAssert.assertEquals(orderDetails.size(), 2, "Should have 2 orders");
        softAssert.assertTrue(orderDetails.contains("Product A:5"), "Should find Product A order");
        softAssert.assertTrue(orderDetails.contains("Product B:3"), "Should find Product B order");

        softAssert.assertAll();
    }

    @Test(groups = "query")
    public void testComplexFilters() throws SQLException {
        CachedRowSet result = provider.executeQuery(
                "SELECT name, price " +
                        "FROM products " +
                        "WHERE price BETWEEN 10 AND 30 " +
                        "AND stock >= 50 " +
                        "ORDER BY price DESC, name ASC"  // Added deterministic ordering
        );

        List<String> products = new ArrayList<>();
        while (result.next()) {
            products.add(result.getString("name"));
        }

        softAssert.assertEquals(products.size(), 3, "Should find three products");
        softAssert.assertEquals(products.get(0), "Product B", "First product should be Product B (highest price)");
        softAssert.assertEquals(products.get(1), "Product C", "Second product should be Product C (medium price)");
        softAssert.assertEquals(products.get(2), "Product A", "Third product should be Product A (lowest price)");

        softAssert.assertAll();
    }

    @Test(groups = "metadata")
    public void testTableMetadata() {
        // Test column types
        Map<String, String> productColumns = provider.getColumnDataTypes("products");
        softAssert.assertEquals(productColumns.get("id"), "INTEGER", "ID should be INTEGER");
        softAssert.assertEquals(productColumns.get("price"), "REAL", "Price should be REAL");
        softAssert.assertEquals(productColumns.get("name"), "TEXT", "Name should be TEXT");

        // Test primary keys
        List<String> primaryKeys = provider.getPrimaryKeys("products");
        softAssert.assertEquals(primaryKeys.size(), 1, "Should have one primary key");
        softAssert.assertEquals(primaryKeys.get(0), "id", "Primary key should be id");

        // Test foreign keys
        Map<String, String> foreignKeys = provider.getForeignKeys("orders");
        softAssert.assertEquals(foreignKeys.get("product_id"), "products", "Should have foreign key to products");

        softAssert.assertAll();
    }

    @Test(groups = "batch")
    public void testBatchOperationsWithDifferentTypes() throws SQLException {
        List<List<Object>> batchData = Arrays.asList(
                Arrays.asList("Test Product 1", 25.99, 30),
                Arrays.asList("Test Product 2", 35.99, 40),
                Arrays.asList("Test Product 3", 45.99, 50)
        );

        int inserted = provider.executeBatchInsert(
                "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)",
                batchData
        );

        softAssert.assertEquals(inserted, 3, "Should insert 3 records");

        // Verify different data types were inserted correctly
        CachedRowSet result = provider.executeQuery(
                "SELECT name, price, stock FROM products WHERE name LIKE 'Test Product%'"
        );

        while (result.next()) {
            String name = result.getString("name");
            if (name.equals("Test Product 1")) {
                softAssert.assertEquals(result.getDouble("price"), 25.99, "Price should be exact");
                softAssert.assertEquals(result.getInt("stock"), 30, "Stock should be exact");
            }
        }

        softAssert.assertAll();
    }

    @AfterClass
    public void cleanup() {
        if (provider != null) {
            provider.executeUpdate("DROP TABLE IF EXISTS orders");
            provider.executeUpdate("DROP TABLE IF EXISTS products");
            provider.close();
        }
        new File(DB_PATH).delete();
    }
}