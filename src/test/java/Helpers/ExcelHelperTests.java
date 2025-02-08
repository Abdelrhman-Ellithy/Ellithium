package Helpers;

import Ellithium.Utilities.helpers.ExcelHelper;
import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;

public class ExcelHelperTests extends NonBDDSetup {
    private static final String TEST_DIR = "src/test/resources/TestData/";
    private static final String TEST_EXCEL = TEST_DIR + "test.xlsx";
    private static final String OUTPUT_EXCEL = TEST_DIR + "output.xlsx";
    private static final String COMPARE_EXCEL = TEST_DIR + "compare.xlsx";

    @BeforeMethod
    public void setUp() throws IOException {
        try {
            new File(TEST_DIR).mkdirs();
            
            // Create a test Excel file with sample data
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("TestSheet");
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Name");
                headerRow.createCell(1).setCellValue("Age");
                
                Row dataRow = sheet.createRow(1);
                dataRow.createCell(0).setCellValue("John Doe");
                dataRow.createCell(1).setCellValue(30);
                
                try (FileOutputStream fos = new FileOutputStream(TEST_EXCEL)) {
                    workbook.write(fos);
                }
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
            Files.deleteIfExists(Path.of(TEST_EXCEL));
            Files.deleteIfExists(Path.of(OUTPUT_EXCEL));
            Files.deleteIfExists(Path.of(COMPARE_EXCEL));
            Reporter.log("Test cleanup completed successfully", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Failed to clean up test files: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testReadExcelData() {
        try {
            List<Map<String, String>> data = ExcelHelper.getExcelData(TEST_EXCEL, "TestSheet");
            assertNotNull(data);
            assertEquals(1, data.size());
            assertEquals("John Doe", data.get(0).get("Name"));
            assertEquals("30", data.get(0).get("Age")); // Just expects "30" as string
            Reporter.log("Excel read test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Excel read test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testWriteExcelData() {
        try {
            List<Map<String, String>> data = new ArrayList<>();
            Map<String, String> row = new HashMap<>();
            row.put("Name", "Jane Smith");
            row.put("Age", "25");
            data.add(row);

            ExcelHelper.setExcelData(OUTPUT_EXCEL, "TestSheet", data);
            List<Map<String, String>> readData = ExcelHelper.getExcelData(OUTPUT_EXCEL, "TestSheet");
            
            assertEquals(1, readData.size());
            assertEquals("Jane Smith", readData.get(0).get("Name"));
            Reporter.log("Excel write test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Excel write test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testUpdateExcelCell() {
        try {
            ExcelHelper.writeCell(TEST_EXCEL, "TestSheet", 1, 0, "Updated Name");
            String value = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 1, 0);
            assertEquals("Updated Name", value);
            Reporter.log("Excel cell update test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Excel cell update test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testGetCellValue() {
        try {
            String value = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 1, 0);
            assertEquals("John Doe", value);
            Reporter.log("Get cell value test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Get cell value test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSearchInExcel() {
        try {
            List<String> columnData = ExcelHelper.readColumn(TEST_EXCEL, "TestSheet", 0);
            assertTrue(columnData.contains("John Doe"));
            Reporter.log("Search in Excel test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Search in Excel test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testMergeExcelFiles() {
        try {
            // Create a second Excel file
            List<Map<String, String>> testData = Collections.singletonList(
                Collections.singletonMap("Name", "Test User")
            );
            ExcelHelper.setExcelData(COMPARE_EXCEL, "TestSheet", testData);
            
            List<String> files = Arrays.asList(TEST_EXCEL, COMPARE_EXCEL);
            ExcelHelper.mergeExcelFiles(files, OUTPUT_EXCEL);
            
            List<Map<String, String>> mergedData = ExcelHelper.getExcelData(OUTPUT_EXCEL, "Merged Data");
            assertTrue(mergedData.size() > 1);
            Reporter.log("Merge Excel files test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Merge Excel files test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testIsSheetEmpty() {
        try {
            // Should return false for non-empty sheet
            assertFalse(ExcelHelper.isSheetEmpty(TEST_EXCEL, "TestSheet"));
            
            // Create and test empty sheet
            ExcelHelper.createSheet(TEST_EXCEL, "EmptySheet");
            assertTrue(ExcelHelper.isSheetEmpty(TEST_EXCEL, "EmptySheet"));
            Reporter.log("Sheet empty check test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Sheet empty check test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testCreateSheet() {
        try {
            String newSheetName = "NewTestSheet";
            ExcelHelper.createSheet(TEST_EXCEL, newSheetName);
            
            // Verify sheet exists and is empty
            assertTrue(ExcelHelper.isSheetEmpty(TEST_EXCEL, newSheetName));
            Reporter.log("Sheet creation test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Sheet creation test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testCellUpdateIsolation() {
        try {
            // Get initial values
            String initialName = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 1, 0);
            String initialAge = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 1, 1);
            
            // Update name cell
            ExcelHelper.writeCell(TEST_EXCEL, "TestSheet", 1, 0, "Updated Name");
            
            // Verify age cell remains unchanged
            String afterAge = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 1, 1);
            assertEquals(afterAge, initialAge, "Age cell should remain unchanged");
            
            Reporter.log("Cell update isolation test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Cell update isolation test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testRowUpdatePreservesOtherRows() {
        try {
            // Add another row
            ExcelHelper.writeCell(TEST_EXCEL, "TestSheet", 2, 0, "Jane Doe");
            ExcelHelper.writeCell(TEST_EXCEL, "TestSheet", 2, 1, "25");
            
            // Update first data row
            Map<String, String> updatedRow = new HashMap<>();
            updatedRow.put("Name", "Updated Name");
            updatedRow.put("Age", "35");
            
            // Read second row before update
            String initialName2 = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 2, 0);
            String initialAge2 = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 2, 1);
            
            // Perform update on first row
            ExcelHelper.writeCell(TEST_EXCEL, "TestSheet", 1, 0, updatedRow.get("Name"));
            ExcelHelper.writeCell(TEST_EXCEL, "TestSheet", 1, 1, updatedRow.get("Age"));
            
            // Verify second row remains unchanged
            String afterName2 = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 2, 0);
            String afterAge2 = ExcelHelper.readCell(TEST_EXCEL, "TestSheet", 2, 1);
            
            assertEquals(afterName2, initialName2, "Name in second row should remain unchanged");
            assertEquals(afterAge2, initialAge2, "Age in second row should remain unchanged");
            
            Reporter.log("Row update isolation test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Row update isolation test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }
}
