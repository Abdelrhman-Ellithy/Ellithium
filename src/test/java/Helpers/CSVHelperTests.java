package Helpers;

import Ellithium.Utilities.helpers.CSVHelper;
import Ellithium.core.base.NonBDDSetup;
import org.testng.annotations.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.testng.Assert.*;

public class CSVHelperTests extends NonBDDSetup {
    private static final String TEST_CSV = "src/test/resources/TestData/test.csv";
    private static final String BACKUP_CSV = "src/test/resources/TestData/backup.csv";
    private static final String MERGE_CSV = "src/test/resources/TestData/merge.csv";
    
    private List<Map<String, String>> initialData;

    @BeforeClass
    public void setupTestDirectory() throws IOException {
        Files.createDirectories(Paths.get("src/test/resources/TestData"));
    }

    @BeforeMethod
    public void setUp() throws IOException {
        // Create initial test data
        initialData = new ArrayList<>();
        Map<String, String> row1 = new HashMap<>();
        row1.put("id", "1");
        row1.put("name", "John");
        row1.put("age", "30");
        
        Map<String, String> row2 = new HashMap<>();
        row2.put("id", "2");
        row2.put("name", "Jane");
        row2.put("age", "25");
        
        initialData.add(row1);
        initialData.add(row2);
        
        // Write initial data to file
        CSVHelper.setCsvData(TEST_CSV, initialData);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        // Clean up test files
        Files.deleteIfExists(Paths.get(TEST_CSV));
        Files.deleteIfExists(Paths.get(BACKUP_CSV));
        Files.deleteIfExists(Paths.get(MERGE_CSV));
    }

    @Test
    public void testGetCsvData() {
        List<Map<String, String>> data = CSVHelper.getCsvData(TEST_CSV);
        assertEquals(data.size(), 2);
        assertEquals(data.get(0).get("name"), "John");
        assertEquals(data.get(1).get("age"), "25");
    }

    @Test
    public void testSetCsvData() {
        Map<String, String> newRow = new HashMap<>();
        newRow.put("id", "3");
        newRow.put("name", "Bob");
        newRow.put("age", "35");
        
        List<Map<String, String>> newData = new ArrayList<>(initialData);
        newData.add(newRow);
        
        CSVHelper.setCsvData(TEST_CSV, newData);
        
        List<Map<String, String>> readData = CSVHelper.getCsvData(TEST_CSV);
        assertEquals(readData.size(), 3);
        assertEquals(readData.get(2).get("name"), "Bob");
    }

    @Test
    public void testGetCsvDataByColumn() {
        List<Map<String, String>> filteredData = CSVHelper.getCsvDataByColumn(TEST_CSV, "name", "John");
        assertEquals(filteredData.size(), 1);
        assertEquals(filteredData.get(0).get("age"), "30");
    }

    @Test
    public void testReadColumn() {
        List<String> names = CSVHelper.readColumn(TEST_CSV, "name");
        assertEquals(names.size(), 2);
        assertTrue(names.contains("John"));
        assertTrue(names.contains("Jane"));
    }

    @Test
    public void testReadCell() {
        String value = CSVHelper.readCell(TEST_CSV, 0, "name");
        assertEquals(value, "John");
    }

    @Test
    public void testReadRow() {
        Map<String, String> row = CSVHelper.readRow(TEST_CSV, 1);
        assertEquals(row.get("name"), "Jane");
        assertEquals(row.get("age"), "25");
    }

    @Test
    public void testWriteCell() {
        CSVHelper.writeCell(TEST_CSV, 0, "age", "31");
        String newAge = CSVHelper.readCell(TEST_CSV, 0, "age");
        assertEquals(newAge, "31");
        
        // Verify other data remains unchanged
        String name = CSVHelper.readCell(TEST_CSV, 0, "name");
        assertEquals(name, "John");
    }

    @Test
    public void testAppendData() {
        Map<String, String> newRow = new HashMap<>();
        newRow.put("id", "3");
        newRow.put("name", "Alice");
        newRow.put("age", "28");
        
        List<Map<String, String>> appendData = new ArrayList<>();
        appendData.add(newRow);
        
        CSVHelper.appendData(TEST_CSV, appendData);
        
        List<Map<String, String>> allData = CSVHelper.getCsvData(TEST_CSV);
        assertEquals(allData.size(), 3);
        assertEquals(allData.get(2).get("name"), "Alice");
    }

    @Test
    public void testDeleteRow() {
        CSVHelper.deleteRow(TEST_CSV, 0);
        List<Map<String, String>> remainingData = CSVHelper.getCsvData(TEST_CSV);
        assertEquals(remainingData.size(), 1);
        assertEquals(remainingData.get(0).get("name"), "Jane");
    }

    @Test
    public void testDeleteColumn() {
        CSVHelper.deleteColumn(TEST_CSV, "age");
        List<Map<String, String>> data = CSVHelper.getCsvData(TEST_CSV);
        assertFalse(data.get(0).containsKey("age"));
        assertTrue(data.get(0).containsKey("name"));
    }

    @Test
    public void testFilterData() {
        Map<String, String> conditions = new HashMap<>();
        conditions.put("age", "30");
        
        List<Map<String, String>> filtered = CSVHelper.filterData(TEST_CSV, conditions);
        assertEquals(filtered.size(), 1);
        assertEquals(filtered.get(0).get("name"), "John");
    }

    @Test
    public void testSortData() {
        List<Map<String, String>> sorted = CSVHelper.sortData(TEST_CSV, "name", true);
        assertEquals(sorted.get(0).get("name"), "Jane");
        assertEquals(sorted.get(1).get("name"), "John");
    }

    @Test
    public void testReplaceColumnData() {
        CSVHelper.replaceColumnData(TEST_CSV, "name", "John", "Johnny");
        List<Map<String, String>> data = CSVHelper.getCsvData(TEST_CSV);
        assertEquals(data.get(0).get("name"), "Johnny");
        assertEquals(data.get(1).get("name"), "Jane"); // Verify other data unchanged
    }

    @Test
    public void testExportToList() {
        List<List<String>> listData = CSVHelper.exportToList(TEST_CSV);
        assertEquals(listData.size(), 2);
        assertTrue(listData.get(0).contains("John"));
    }

    @Test
    public void testMergeCsvFiles() {
        // Create second file
        List<Map<String, String>> secondFileData = new ArrayList<>();
        Map<String, String> newRow = new HashMap<>();
        newRow.put("id", "3");
        newRow.put("name", "Bob");
        newRow.put("age", "40");
        secondFileData.add(newRow);
        CSVHelper.setCsvData(BACKUP_CSV, secondFileData);

        // Test merge
        CSVHelper.mergeCsvFiles(TEST_CSV, BACKUP_CSV, MERGE_CSV);
        List<Map<String, String>> mergedData = CSVHelper.getCsvData(MERGE_CSV);
        assertEquals(mergedData.size(), 3);
    }

    @Test
    public void testValidateFileStructure() {
        List<String> expectedColumns = Arrays.asList("id", "name", "age");
        assertTrue(CSVHelper.validateFileStructure(TEST_CSV, expectedColumns));
    }

    @Test
    public void testUpdateRow() {
        Map<String, String> newData = new HashMap<>();
        newData.put("id", "1");
        newData.put("name", "John Updated");
        newData.put("age", "31");
        
        CSVHelper.updateRow(TEST_CSV, 0, newData);
        
        Map<String, String> updatedRow = CSVHelper.readRow(TEST_CSV, 0);
        assertEquals(updatedRow.get("name"), "John Updated");
        assertEquals(updatedRow.get("age"), "31");
    }

    @Test
    public void testUpdateColumn() {
        CSVHelper.updateColumn(TEST_CSV, "age", "50");
        List<Map<String, String>> data = CSVHelper.getCsvData(TEST_CSV);
        assertEquals(data.get(0).get("age"), "50");
        assertEquals(data.get(1).get("age"), "50");
    }

    @Test
    public void testGetRowCount() {
        assertEquals(CSVHelper.getRowCount(TEST_CSV), 2);
    }

    @Test
    public void testFindDuplicateEntries() {
        // Add duplicate entry
        Map<String, String> duplicateRow = new HashMap<>(initialData.get(0));
        initialData.add(duplicateRow);
        CSVHelper.setCsvData(TEST_CSV, initialData);
        
        List<String> duplicates = CSVHelper.findDuplicateEntries(TEST_CSV, "name");
        assertEquals(duplicates.size(), 1);
        assertTrue(duplicates.contains("John"));
    }

    @Test
    public void testAddNewColumn() {
        CSVHelper.addNewColumn(TEST_CSV, "country", "USA");
        List<Map<String, String>> data = CSVHelper.getCsvData(TEST_CSV);
        assertTrue(data.get(0).containsKey("country"));
        assertEquals(data.get(0).get("country"), "USA");
    }

    @Test
    public void testRenameColumn() {
        CSVHelper.renameColumn(TEST_CSV, "name", "full_name");
        List<Map<String, String>> data = CSVHelper.getCsvData(TEST_CSV);
        assertFalse(data.get(0).containsKey("name"));
        assertTrue(data.get(0).containsKey("full_name"));
        assertEquals(data.get(0).get("full_name"), "John");
    }
}
