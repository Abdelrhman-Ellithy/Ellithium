package Helpers;

import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;

public class JsonHelperTests extends NonBDDSetup {
    private static final String TEST_DIR = "src/test/resources/TestData/";
    private static final String TEST_JSON = TEST_DIR + "test.json";
    private static final String OUTPUT_JSON = TEST_DIR + "output.json";
    private static final String COMPARE_JSON = TEST_DIR + "compare.json";
    private static final String MERGED_JSON = TEST_DIR + "merged.json";

    @BeforeMethod
    public void setUp() throws IOException {
        try {
            new File(TEST_DIR).mkdirs();
            JsonObject testData = new JsonObject();
            testData.addProperty("name", "Test Name");
            testData.addProperty("age", 25);
            testData.addProperty("email", "test@test.com");

            try (FileWriter writer = new FileWriter(TEST_JSON)) {
                writer.write(testData.toString());
            }
            Reporter.log("Test setup completed successfully", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Failed to set up test environment: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @AfterMethod
    public void tearDown() throws IOException {
        try {
            Files.deleteIfExists(Path.of(TEST_JSON));
            Files.deleteIfExists(Path.of(OUTPUT_JSON));
            Files.deleteIfExists(Path.of(COMPARE_JSON));
            Files.deleteIfExists(Path.of(MERGED_JSON));
            Reporter.log("Test cleanup completed successfully", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Failed to clean up test files: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }
    @Test
    public void testSetJsonData() {
        try {
            List<Map<String, String>> data = new ArrayList<>();
            Map<String, String> record = new HashMap<>();
            record.put("key1", "value1");
            record.put("key2", "value2");
            data.add(record);

            JsonHelper.setJsonData(OUTPUT_JSON, data);
            List<Map<String, String>> readData = JsonHelper.getJsonData(OUTPUT_JSON);
            
            assertEquals(readData.size(), 1);
            assertEquals(readData.get(0).get("key1"), "value1");
            Reporter.log("JSON write test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("JSON write test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testGetJsonKeyValue() {
        try {
            String value = JsonHelper.getJsonKeyValue(TEST_JSON, "name");
            assertEquals(value, "Test Name");
            Reporter.log("JSON key value test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("JSON key value test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSetJsonKeyValue() {
        try {
            JsonHelper.setJsonKeyValue(TEST_JSON, "name", "Updated Name");
            String value = JsonHelper.getJsonKeyValue(TEST_JSON, "name");
            assertEquals(value, "Updated Name");
            Reporter.log("JSON key update test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("JSON key update test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testValidateJsonKeys() {
        try {
            List<String> requiredKeys = Arrays.asList("name", "age", "email");
            assertTrue(JsonHelper.validateJsonKeys(TEST_JSON, requiredKeys));
            Reporter.log("JSON key validation test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("JSON key validation test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testGetNestedJsonData() {
        try {
            JsonObject nestedData = new JsonObject();
            JsonObject address = new JsonObject();
            address.addProperty("street", "Test Street");
            address.addProperty("city", "Test City");
            nestedData.add("address", address);

            try (FileWriter writer = new FileWriter(TEST_JSON)) {
                writer.write(nestedData.toString());
            }

            Map<String, Object> data = JsonHelper.getNestedJsonData(TEST_JSON);
            assertNotNull(data.get("address"));
            Reporter.log("Nested JSON test passed successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Nested JSON test failed: ", LogLevel.ERROR, e.getMessage());
            throw new AssertionError(e);
        }
    }

    @Test
    public void testCompareJsonFiles() {
        try {
            Files.copy(Path.of(TEST_JSON), Path.of(COMPARE_JSON));
            assertTrue(JsonHelper.compareJsonFiles(TEST_JSON, COMPARE_JSON));
            Reporter.log("JSON comparison test passed successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("JSON comparison test failed: ", LogLevel.ERROR, e.getMessage());
            throw new AssertionError(e);
        }
    }

    @Test
    public void testMergeJsonFiles() {
        try {
            JsonObject compareData = new JsonObject();
            compareData.addProperty("address", "Test Address");

            try (FileWriter writer = new FileWriter(COMPARE_JSON)) {
                writer.write(compareData.toString());
            }

            JsonHelper.mergeJsonFiles(TEST_JSON, COMPARE_JSON, MERGED_JSON);
            assertTrue(Files.exists(Path.of(MERGED_JSON)));
            Reporter.log("JSON merge test passed successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("JSON merge test failed: ", LogLevel.ERROR, e.getMessage());
            throw new AssertionError(e);
        }
    }

    @Test
    public void testUpdateNestedJsonKey() {
        try {
            // First create the nested structure
            JsonObject nestedData = new JsonObject();
            JsonObject address = new JsonObject();
            address.addProperty("street", "Test Street");
            address.addProperty("city", "Test City");
            nestedData.add("address", address);

            // Write the nested structure to file
            try (FileWriter writer = new FileWriter(TEST_JSON)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(nestedData));
            }

            // Now update the nested key
            List<String> keys = Arrays.asList("address", "street");
            JsonHelper.updateNestedJsonKey(TEST_JSON, keys, "New Street");

            // Verify the update
            Map<String, Object> data = JsonHelper.getNestedJsonData(TEST_JSON);
            assertNotNull(data.get("address"), "Address object should exist");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> addressData = (Map<String, Object>) data.get("address");
            assertNotNull(addressData, "Address data should not be null");
            assertEquals("New Street", addressData.get("street"), "Street value should be updated");
            
            Reporter.log("Nested key update test passed successfully", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Test setup failed: ", LogLevel.ERROR, e.getMessage());
            fail("Test failed due to IO error: " + e.getMessage());
        } catch (AssertionError e) {
            Reporter.log("Nested key update test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testArrayOperations() {
        try {
            List<String> arrayPath = Arrays.asList("items");
            JsonHelper.createArrayAtPath(TEST_JSON, arrayPath);
            JsonHelper.appendToJsonArray(TEST_JSON, arrayPath, "item1");
            JsonHelper.insertIntoJsonArray(TEST_JSON, arrayPath, 0, "item0");
            assertTrue(JsonHelper.arrayContainsValue(TEST_JSON, arrayPath, "item0"));
            assertTrue(JsonHelper.arrayContainsValue(TEST_JSON, arrayPath, "item1"));
            Reporter.log("Array operations test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Array operations test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testJsonFormatting() {
        try {
            JsonHelper.prettyPrintJson(TEST_JSON);
            assertTrue(JsonHelper.isValidJson(TEST_JSON));
            Reporter.log("JSON formatting test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("JSON formatting test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testKeyOccurrences() {
        try {
            Map<String, Integer> occurrences = JsonHelper.getJsonKeyOccurrences(TEST_JSON);
            assertTrue(occurrences.containsKey("name"));
            assertTrue(occurrences.containsKey("age"));
            assertTrue(occurrences.containsKey("email"));
            Reporter.log("Key occurrences test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Key occurrences test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testBackupAndNullRemoval() {
        try {
            JsonHelper.backupJsonFile(TEST_JSON);
            JsonHelper.removeNullValues(TEST_JSON);
            assertTrue(JsonHelper.isValidJson(TEST_JSON));
            Reporter.log("Backup and null removal test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Backup and null removal test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testParseJsonToObject() {
        try {
            TestObject obj = JsonHelper.parseJsonToObject(TEST_JSON, TestObject.class);
            assertNotNull(obj);
            assertEquals(obj.name, "Test Name");
            Reporter.log("Parse to object test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Parse to object test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testParseJsonToList() {
        try {
            // Create test data with array
            List<TestObject> objects = Arrays.asList(
                createTestObject("Test1", 25, "test1@test.com"),
                createTestObject("Test2", 30, "test2@test.com")
            );
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(TEST_JSON)) {
                writer.write(gson.toJson(objects));
            }
            
            List<TestObject> result = JsonHelper.parseJsonToList(TEST_JSON, TestObject.class);
            assertEquals(2, result.size());
            assertEquals("Test1", result.get(0).name);
            assertEquals("Test2", result.get(1).name);
            Reporter.log("Parse JSON to list test passed successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Parse JSON to list test failed: ", LogLevel.ERROR, e.getMessage());
            throw new AssertionError(e);
        }
    }

    @Test
    public void testNavigateAndModifyNestedStructure() {
        try {
            // Create nested JSON structure
            JsonObject nested = new JsonObject();
            JsonObject address = new JsonObject();
            JsonArray phones = new JsonArray();
            
            address.addProperty("street", "123 Test St");
            phones.add("123-456-7890");
            nested.add("address", address);
            nested.add("phones", phones);
            
            try (FileWriter writer = new FileWriter(TEST_JSON)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(nested));
            }
            
            // Test navigation and modification
            List<String> addressPath = Arrays.asList("address", "street");
            JsonHelper.modifyInNestedPath(TEST_JSON, addressPath, "456 New St");
            
            String value = JsonHelper.getValueFromNestedPath(TEST_JSON, addressPath);
            assertEquals("456 New St", value);
            Reporter.log("Nested structure navigation test passed successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Nested structure navigation test failed: ", LogLevel.ERROR, e.getMessage());
            throw new AssertionError(e);
        }
    }

    @Test
    public void testArrayOperationsInDepth() {
        try {
            // Create JSON with array
            JsonObject root = new JsonObject();
            JsonArray items = new JsonArray();
            root.add("items", items);
            
            try (FileWriter writer = new FileWriter(TEST_JSON)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(root));
            }
            
            List<String> arrayPath = Arrays.asList("items");
            
            // Test array operations
            JsonHelper.appendToJsonArray(TEST_JSON, arrayPath, "item1");
            JsonHelper.insertIntoJsonArray(TEST_JSON, arrayPath, 0, "item0");
            JsonHelper.appendToJsonArray(TEST_JSON, arrayPath, "item2");
            
            assertTrue(JsonHelper.arrayContainsValue(TEST_JSON, arrayPath, "item0"));
            assertTrue(JsonHelper.arrayContainsValue(TEST_JSON, arrayPath, "item1"));
            assertTrue(JsonHelper.arrayContainsValue(TEST_JSON, arrayPath, "item2"));
            
            JsonHelper.removeFromJsonArray(TEST_JSON, arrayPath, 1);
            assertFalse(JsonHelper.arrayContainsValue(TEST_JSON, arrayPath, "item1"));
            
            Reporter.log("Array operations test passed successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Array operations test failed: ", LogLevel.ERROR, e.getMessage());
            throw new AssertionError(e);
        }
    }

    @Test
    public void testJsonBackupAndRestoration() {
        try {
            // Create initial content
            JsonHelper.setJsonKeyValue(TEST_JSON, "testKey", "testValue");
            
            // Create backup
            JsonHelper.backupJsonFile(TEST_JSON);
            
            // Modify original file
            JsonHelper.setJsonKeyValue(TEST_JSON, "testKey", "modifiedValue");
            
            // Verify backup exists with correct content
            File[] backupFiles = new File(TEST_DIR).listFiles((dir, name) -> name.startsWith("test.json.backup-"));
            assertTrue(backupFiles != null && backupFiles.length > 0);
            
            String backupContent = new String(Files.readAllBytes(backupFiles[0].toPath()));
            assertTrue(backupContent.contains("testValue"));
            
            Reporter.log("Backup and restore test passed successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Backup and restore test failed: ", LogLevel.ERROR, e.getMessage());
            throw new AssertionError(e);
        }
    }

    @Test
    public void testNullValueHandling() {
        try {
            // Create JSON with null values
            JsonObject obj = new JsonObject();
            obj.add("nullField", JsonNull.INSTANCE);
            obj.addProperty("nonNullField", "value");
            
            try (FileWriter writer = new FileWriter(TEST_JSON)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(obj));
            }
            
            JsonHelper.removeNullValues(TEST_JSON);
            
            // Verify null values are removed using JsonReader instead of deprecated JsonParser
            try (JsonReader reader = new JsonReader(new FileReader(TEST_JSON))) {
                JsonObject cleaned = JsonParser.parseReader(reader).getAsJsonObject();
                assertFalse(cleaned.has("nullField"));
                assertTrue(cleaned.has("nonNullField"));
            }
            
            Reporter.log("Null value handling test passed successfully", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Null value handling test failed: ", LogLevel.ERROR, e.getMessage());
            throw new AssertionError(e);
        }
    }

    private TestObject createTestObject(String name, int age, String email) {
        TestObject obj = new TestObject();
        obj.name = name;
        obj.age = age;
        obj.email = email;
        return obj;
    }

    // Test helper class
    private static class TestObject {
        public String name;
        public int age;
        public String email;
    }
}
