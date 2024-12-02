package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ExcelHelper {

    // Method to get Excel data
    public static List<Map<String, String>> getExcelData(String filePath, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        Reporter.log("Attempting to read Excel data from file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName, LogLevel.ERROR, " does not exist in " + filePath);
                return data;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                List<String> headers = new ArrayList<>();
                headerRow.forEach(cell -> headers.add(cell.getStringCellValue()));
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Map<String, String> recordMap = new HashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        recordMap.put(headers.get(i), getCellValueAsString(cell));
                    }
                    data.add(recordMap);
                }
            }
            Reporter.log("Successfully read Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        } catch (IOException e) {
            Reporter.log("Failed to read Excel data from file: ", LogLevel.ERROR, filePath + ", sheet: " + sheetName);
        }
        return data;
    }

    // Method to set Excel data
    public static void setExcelData(String filePath, String sheetName, List<Map<String, String>> data) {
        Reporter.log("Attempting to write data to Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        File excelFile = new File(filePath + ".xlsx");
        Workbook workbook = null;
        try {
            if (excelFile.exists()) {
                try (FileInputStream fis = new FileInputStream(excelFile)) {
                    workbook = new XSSFWorkbook(fis);
                }
            } else {
                workbook = new XSSFWorkbook();
                Reporter.log("Creating new Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
            }

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
            }

            int rowNum = sheet.getLastRowNum() + 1;

            if (rowNum == 0 && !data.isEmpty()) {
                Row headerRow = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String key : data.get(0).keySet()) {
                    Cell cell = headerRow.createCell(colNum++);
                    cell.setCellValue(key);
                }
            }

            for (Map<String, String> record : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String value : record.values()) {
                    Cell cell = row.createCell(colNum++);
                    cell.setCellValue(value);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
                Reporter.log("Successfully wrote data to Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
            }
        } catch (IOException e) {
            Reporter.log("Failed to write data to Excel file: ", LogLevel.ERROR, filePath + ", sheet: " + sheetName);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    Reporter.log("Failed to close workbook: ", LogLevel.ERROR, filePath + ", sheet: " + sheetName);
                }
            }
        }
    }

    // Utility method to get a specific column
    public static List<String> getColumnData(String filePath, String sheetName, int columnIndex) {
        List<String> columnData = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet != null) {
                for (Row row : sheet) {
                    Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    columnData.add(getCellValueAsString(cell));
                }
            } else {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
            }
        } catch (IOException e) {
            Reporter.log("Error while reading column data: ", LogLevel.ERROR, e.getMessage());
        }
        return columnData;
    }

    // Utility method to get a specific cell value
    public static String getCellData(String filePath, String sheetName, int rowIndex, int columnIndex) {
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet != null) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    return getCellValueAsString(cell);
                }
            } else {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
            }
        } catch (IOException e) {
            Reporter.log("Error while reading cell data: ", LogLevel.ERROR, e.getMessage());
        }
        return "";
    }

    // Utility method to read row by index
    public static List<String> getRowData(String filePath, String sheetName, int rowIndex) {
        List<String> rowData = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet != null) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    for (Cell cell : row) {
                        rowData.add(getCellValueAsString(cell));
                    }
                }
            } else {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
            }
        } catch (IOException e) {
            Reporter.log("Error while reading row data: ", LogLevel.ERROR, e.getMessage());
        }
        return rowData;
    }

    // Helper method to get cell value as string
    private static String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
            case _NONE:
            case ERROR:
            default:
                return "";
        }
    }
}