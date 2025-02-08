package Helpers;

import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
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
    public void testGetJsonData() {
        try {
            List<Map<String, String>> data = JsonHelper.getJsonData(TEST_JSON);
            assertNotNull(data);
            assertEquals(1, data.size());
            assertEquals("Test Name", data.get(0).get("name"));
            assertEquals("25", data.get(0).get("age"));
            assertEquals("test@test.com", data.get(0).get("email"));
            Reporter.log("JSON read test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("JSON read test failed: ", LogLevel.ERROR, e.getMessage());
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

    // Test helper class
    private static class TestObject {
        public String name;
        public int age;
        public String email;
    }
}
