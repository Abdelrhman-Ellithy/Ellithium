package Helpers;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.core.base.NonBDDSetup;
import org.testng.annotations.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;

public class PropertyHelperTests extends NonBDDSetup {
    private static final String TEST_FILE = "src/test/resources/TestData/test.properties";
    private static final String BACKUP_FILE = "src/test/resources/TestData/test.properties.backup";
    private static final String COMPARE_FILE = "src/test/resources/TestData/compare.properties";

    @BeforeClass
    public void setUpClass() {
        // Ensure the test directory exists
        new File(TEST_FILE).getParentFile().mkdirs();
    }

    @BeforeMethod
    public void setUp() {
        try {
            Properties prop = new Properties();
            prop.setProperty("test.key1", "value1");
            prop.setProperty("test.key2", "value2");
            try (FileOutputStream fos = new FileOutputStream(TEST_FILE)) {
                prop.store(fos, null);
            }
        } catch (IOException e) {
            fail("Failed to set up test properties: " + e.getMessage());
        }
    }

    @AfterMethod
    public void tearDown() {
        try {
            // Close any open file handles
            System.gc(); // Help release file handles
            Thread.sleep(100); // Give OS time to release handles
            
            // Delete files with retries
            deleteFileWithRetry(Path.of(TEST_FILE));
            deleteFileWithRetry(Path.of(BACKUP_FILE));
            deleteFileWithRetry(Path.of(COMPARE_FILE));
        } catch (Exception e) {
            Reporter.log("Warning during cleanup: " + e.getMessage(), LogLevel.WARN);
            // Don't fail the test for cleanup issues
        }
    }

    private void deleteFileWithRetry(Path path) {
        int maxRetries = 3;
        int retryDelay = 100; // milliseconds
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (IOException e) {
                if (i == maxRetries - 1) {
                    Reporter.log("Could not delete file after " + maxRetries + " attempts: " + path, LogLevel.WARN);
                } else {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    @Test(groups = {"properties"}, description = "Test getting data from properties file")
    public void testGetDataFromProperties() {
        String value = PropertyHelper.getDataFromProperties(TEST_FILE, "test.key1");
        assertEquals(value, "value1", "Property value does not match expected");
    }

    @Test(groups = {"properties"}, description = "Test setting data to properties file")
    public void testSetDataToProperties() {
        PropertyHelper.setDataToProperties(TEST_FILE, "test.key1", "newValue");
        String value = PropertyHelper.getDataFromProperties(TEST_FILE, "test.key1");
        assertEquals(value, "newValue", "Updated property value does not match expected");

        // Verify other properties remain unchanged
        String value2 = PropertyHelper.getDataFromProperties(TEST_FILE, "test.key2");
        assertEquals(value2, "value2");
    }

    @Test(groups = {"properties"}, description = "Test getting all properties")
    public void testGetAllProperties() {
        Properties props = PropertyHelper.getAllProperties(TEST_FILE);
        assertEquals(props.size(), 2, "Incorrect number of properties");
        assertEquals(props.getProperty("test.key1"), "value1", "Property value does not match expected");
        assertEquals(props.getProperty("test.key2"), "value2", "Property value does not match expected");
    }

    @Test(groups = {"properties"}, description = "Test removing key from properties file")
    public void testRemoveKeyFromProperties() {
        PropertyHelper.removeKeyFromProperties(TEST_FILE, "test.key1");
        assertNull(PropertyHelper.getDataFromProperties(TEST_FILE, "test.key1"));
        assertNotNull(PropertyHelper.getDataFromProperties(TEST_FILE, "test.key2"));
    }

    @Test(groups = {"properties"}, description = "Test checking if key exists in properties file")
    public void testKeyExists() {
        assertTrue(PropertyHelper.keyExists(TEST_FILE, "test.key1"));
        assertFalse(PropertyHelper.keyExists(TEST_FILE, "nonexistent.key"));
    }

    @Test(groups = {"properties"}, description = "Test getting property value or default if key does not exist")
    public void testGetOrDefault() {
        assertEquals(PropertyHelper.getOrDefault(TEST_FILE, "test.key1", "default"), "value1");
        assertEquals(PropertyHelper.getOrDefault(TEST_FILE, "nonexistent.key", "default"), "default");
    }

    @Test(groups = {"properties"}, description = "Test updating multiple properties in properties file")
    public void testUpdateMultipleProperties() {
        Properties updates = new Properties();
        updates.setProperty("test.key1", "updatedValue1");
        updates.setProperty("test.key3", "value3");

        PropertyHelper.updateMultipleProperties(TEST_FILE, updates);

        Properties result = PropertyHelper.getAllProperties(TEST_FILE);
        assertEquals(result.getProperty("test.key1"), "updatedValue1");
        assertEquals(result.getProperty("test.key2"), "value2");
        assertEquals(result.getProperty("test.key3"), "value3");
    }

    @Test(groups = {"properties"}, description = "Test backing up and restoring properties file")
    public void testBackupAndRestore() {
        PropertyHelper.backupProperties(TEST_FILE);
        assertTrue(Files.exists(Path.of(BACKUP_FILE)));

        PropertyHelper.setDataToProperties(TEST_FILE, "test.key1", "modified");
        PropertyHelper.restoreFromBackup(TEST_FILE);

        String restoredValue = PropertyHelper.getDataFromProperties(TEST_FILE, "test.key1");
        assertEquals(restoredValue, "value1");
    }

    @Test(groups = {"properties"}, description = "Test loading properties file with specific encoding")
    public void testLoadWithEncoding() {
        Properties props = PropertyHelper.loadWithEncoding(TEST_FILE, "UTF-8");
        assertEquals(props.getProperty("test.key1"), "value1");
    }

    @Test(groups = {"properties"}, description = "Test comparing two properties files")
    public void testCompareProperties() {
        Properties prop2 = new Properties();
        FileOutputStream fos = null;
        try {
            prop2.setProperty("test.key1", "different");
            prop2.setProperty("test.key3", "value3");
            fos = new FileOutputStream(COMPARE_FILE);
            prop2.store(fos, null);
            
            Map<String, String[]> differences = PropertyHelper.compareProperties(TEST_FILE, COMPARE_FILE);
            
            assertEquals(differences.size(), 3);
            assertEquals(differences.get("test.key1"), new String[]{"value1", "different"});
            assertEquals(differences.get("test.key2"), new String[]{"value2", null});
            assertEquals(differences.get("test.key3"), new String[]{null, "value3"});
        } catch (IOException e) {
            fail("Test failed: " + e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Reporter.log("Warning: Could not close file stream", LogLevel.WARN);
                }
            }
        }
    }

    @Test(groups = {"pattern-matching"}, description = "Test getting keys matching a pattern in properties file")
    public void testGetKeysMatchingPattern() {
        Set<String> matches = PropertyHelper.getKeysMatchingPattern(TEST_FILE, "test\\.key\\d");
        assertEquals(matches.size(), 2);
        assertTrue(matches.contains("test.key1"));
        assertTrue(matches.contains("test.key2"));
    }

    @Test(groups = {"file-operations"}, description = "Test clearing all properties in properties file")
    public void testClearProperties() {
        PropertyHelper.clearProperties(TEST_FILE);
        Properties props = PropertyHelper.getAllProperties(TEST_FILE);
        assertEquals(props.size(), 0);
    }

    @Test(groups = {"file-operations"}, description = "Test getting the count of properties in properties file")
    public void testGetPropertyCount() {
        assertEquals(PropertyHelper.getPropertyCount(TEST_FILE), 2);
    }

    @Test(groups = {"properties"}, description = "Test finding values containing a specific string in properties file")
    public void testFindValuesContaining() {
        List<String> matches = PropertyHelper.findValuesContaining(TEST_FILE, "value");
        assertEquals(matches.size(), 2);
        assertTrue(matches.contains("value1"));
        assertTrue(matches.contains("value2"));
    }

    @Test(groups = {"properties"}, description = "Test validating property value against a pattern")
    public void testValidatePropertyValue() {
        assertTrue(PropertyHelper.validatePropertyValue(TEST_FILE, "test.key1", "value\\d"));
        assertFalse(PropertyHelper.validatePropertyValue(TEST_FILE, "test.key1", "invalid\\d"));
    }

    @Test(groups = {"properties"}, description = "Test sorting properties by key")
    public void testSortPropertiesByKey() {
        try {
            // Create properties in specific order
            PropertyHelper.clearProperties(TEST_FILE);
            PropertyHelper.setDataToProperties(TEST_FILE, "test.key2", "value2");
            PropertyHelper.setDataToProperties(TEST_FILE, "a.key", "valueA");
            PropertyHelper.setDataToProperties(TEST_FILE, "test.key1", "value1");

            PropertyHelper.sortPropertiesByKey(TEST_FILE);

            // Read and verify order
            Properties props = PropertyHelper.getAllProperties(TEST_FILE);
            List<String> keys = new ArrayList<>(props.stringPropertyNames());
            
            assertEquals(keys.size(), 3, "Should have exactly 3 properties");
            assertEquals(keys.get(0), "a.key", "First key should be a.key");
            assertEquals(keys.get(1), "test.key1");
            assertEquals(keys.get(2), "test.key2");
        } catch (AssertionError e) {
            Reporter.log("Property sorting test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"properties"}, description = "Test getting properties matching a value pattern")
    public void testGetPropertiesMatchingValuePattern() {
        Map<String, String> matches = PropertyHelper.getPropertiesMatchingValuePattern(TEST_FILE, "value\\d");
        assertEquals(matches.size(), 2);
        assertEquals(matches.get("test.key1"), "value1");
        assertEquals(matches.get("test.key2"), "value2");
    }

    @Test(groups = {"properties"}, description = "Test that updating one property doesn't affect others")
    public void testPropertyUpdateIsolation() {
        // Get initial values
        String initialValue2 = PropertyHelper.getDataFromProperties(TEST_FILE, "test.key2");
        
        // Update test.key1
        PropertyHelper.setDataToProperties(TEST_FILE, "test.key1", "newValue");
        
        // Verify test.key2 remains unchanged
        String afterValue2 = PropertyHelper.getDataFromProperties(TEST_FILE, "test.key2");
        assertEquals(afterValue2, initialValue2, "Unmodified property should remain unchanged");
    }

    @Test(groups = {"properties"}, description = "Test multiple property operations preserve unrelated values")
    public void testMultipleOperationsIsolation() {
        // Add a new property
        PropertyHelper.setDataToProperties(TEST_FILE, "test.key3", "value3");
        
        // Remove test.key1
        PropertyHelper.removeKeyFromProperties(TEST_FILE, "test.key1");
        
        // Verify test.key2 and test.key3 are intact
        String value2 = PropertyHelper.getDataFromProperties(TEST_FILE, "test.key2");
        String value3 = PropertyHelper.getDataFromProperties(TEST_FILE, "test.key3");
        
        assertEquals(value2, "value2", "Existing unmodified property should remain unchanged");
        assertEquals(value3, "value3", "Newly added property should remain unchanged");
    }

    @Test(groups = {"properties"}, description = "Test handling of special characters in property values")
    public void testSpecialCharactersHandling() {
        String specialValue = "!@#$%^&*()_+=<>?";
        PropertyHelper.setDataToProperties(TEST_FILE, "special.key", specialValue);
        assertEquals(PropertyHelper.getDataFromProperties(TEST_FILE, "special.key"), specialValue);
    }

    @Test(groups = {"properties"}, description = "Test unicode character handling")
    public void testUnicodeHandling() {
        String unicodeValue = "测试中文";
        PropertyHelper.setDataToProperties(TEST_FILE, "unicode.key", unicodeValue);
        assertEquals(PropertyHelper.getDataFromProperties(TEST_FILE, "unicode.key"), unicodeValue);
    }

    @Test(groups = {"properties"}, description = "Test empty value handling")
    public void testEmptyValueHandling() {
        PropertyHelper.setDataToProperties(TEST_FILE, "empty.key", "");
        assertEquals(PropertyHelper.getDataFromProperties(TEST_FILE, "empty.key"), "");
    }

    @Test(groups = {"properties"}, description = "Test null key handling")
    public void testNullKeyHandling() {
        try {
            PropertyHelper.setDataToProperties(TEST_FILE, null, "value");
            fail("Should throw exception for null key");
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test(groups = {"properties"}, description = "Test property value length limits")
    public void testPropertyValueLengthLimits() {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("a");
        }
        PropertyHelper.setDataToProperties(TEST_FILE, "long.key", longValue.toString());
        assertEquals(PropertyHelper.getDataFromProperties(TEST_FILE, "long.key"), longValue.toString());
    }

    @Test(groups = {"file-operations"}, description = "Test concurrent access handling")
    public void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                PropertyHelper.setDataToProperties(TEST_FILE, "key" + index, "value" + index);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        Properties props = PropertyHelper.getAllProperties(TEST_FILE);
        assertEquals(props.size(), threadCount + 2); // +2 for initial properties
    }
}
