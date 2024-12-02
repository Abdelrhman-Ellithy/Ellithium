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
    public static List<String> readColumn(String filePath, String sheetName, int columnIndex) {
        List<String> columnData = new ArrayList<>();
        Reporter.log("Attempting to read column from Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName, LogLevel.ERROR, " does not exist in " + filePath);
                return columnData;
            }
            sheet.forEach(row -> {
                Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                columnData.add(getCellValueAsString(cell));
            });
            Reporter.log("Successfully read column from Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        } catch (IOException e) {
            Reporter.log("Failed to read column from Excel file: ", LogLevel.ERROR, filePath + ", sheet: " + sheetName);
        }
        return columnData;
    }

    public static String readCell(String filePath, String sheetName, int rowIndex, int columnIndex) {
        String cellValue = "";
        Reporter.log("Attempting to read cell from Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName, LogLevel.ERROR, " does not exist in " + filePath);
                return cellValue;
            }
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cellValue = getCellValueAsString(cell);
            }
            Reporter.log("Successfully read cell from Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        } catch (IOException e) {
            Reporter.log("Failed to read cell from Excel file: ", LogLevel.ERROR, filePath + ", sheet: " + sheetName);
        }
        return cellValue;
    }
    public static Map<String, String> readRow(String filePath, String sheetName, int rowIndex) {
        Map<String, String> rowData = new HashMap<>();
        Reporter.log("Attempting to read row from Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName, LogLevel.ERROR, " does not exist in " + filePath);
                return rowData;
            }
            Row headerRow = sheet.getRow(0);
            Row dataRow = sheet.getRow(rowIndex);
            if (headerRow != null && dataRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    String header = getCellValueAsString(headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));
                    String value = getCellValueAsString(dataRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));
                    rowData.put(header, value);
                }
            }
            Reporter.log("Successfully read row from Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        } catch (IOException e) {
            Reporter.log("Failed to read row from Excel file: ", LogLevel.ERROR, filePath + ", sheet: " + sheetName);
        }
        return rowData;
    }

    public static void writeCell(String filePath, String sheetName, int rowIndex, int columnIndex, String value) {
        Reporter.log("Attempting to write to a cell in Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
        File excelFile = new File(filePath + ".xlsx");
        Workbook workbook = null;
        try {
            if (excelFile.exists()) {
                try (FileInputStream fis = new FileInputStream(excelFile)) {
                    workbook = new XSSFWorkbook(fis);
                }
            } else {
                workbook = new XSSFWorkbook();
                Reporter.log("Creating new Excel file: ", LogLevel.INFO_GREEN, filePath);
            }
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
            }
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value);

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
                Reporter.log("Successfully wrote to cell in Excel file: ", LogLevel.INFO_GREEN, filePath + ", sheet: " + sheetName);
            }
        } catch (IOException e) {
            Reporter.log("Failed to write to cell in Excel file: ", LogLevel.ERROR, filePath + ", sheet: " + sheetName);
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
    // Method to replace column data
    public static void replaceColumnData(String filePath, String sheetName, String columnName, String oldValue, String newValue) {
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
                return;
            }

            Row headerRow = sheet.getRow(0);
            int columnIndex = -1;
            for (Cell cell : headerRow) {
                if (cell.getStringCellValue().equals(columnName)) {
                    columnIndex = cell.getColumnIndex();
                    break;
                }
            }
            if (columnIndex == -1) {
                Reporter.log("Column " + columnName + " does not exist.", LogLevel.ERROR);
                return;
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (cell.getStringCellValue().equals(oldValue)) {
                    cell.setCellValue(newValue);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath + ".xlsx")) {
                workbook.write(fos);
            }
            Reporter.log("Replaced all occurrences of '" + oldValue + "' with '" + newValue + "' in column " + columnName, LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Error while replacing column data: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // Method to append data
    public static void appendData(String filePath, String sheetName, List<Map<String, String>> newRows) {
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
                return;
            }

            int lastRowNum = sheet.getLastRowNum();
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            headerRow.forEach(cell -> headers.add(cell.getStringCellValue()));

            for (Map<String, String> rowData : newRows) {
                Row row = sheet.createRow(++lastRowNum);
                for (int i = 0; i < headers.size(); i++) {
                    String value = rowData.getOrDefault(headers.get(i), "");
                    row.createCell(i).setCellValue(value);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath + ".xlsx")) {
                workbook.write(fos);
            }
            Reporter.log("Appended new rows to the Excel sheet.", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Error while appending data: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // Method to delete a row
    public static void deleteRow(String filePath, String sheetName, int rowIndex) {
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
                return;
            }

            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                sheet.removeRow(row);
                // Shift rows up to fill the gap
                int lastRowNum = sheet.getLastRowNum();
                if (rowIndex < lastRowNum) {
                    sheet.shiftRows(rowIndex + 1, lastRowNum, -1);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath + ".xlsx")) {
                workbook.write(fos);
            }
            Reporter.log("Deleted row " + rowIndex + " from the Excel sheet.", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Error while deleting row: " + e.getMessage(), LogLevel.ERROR);
        }
    }
    public static List<Row> filterRows(String filePath, String sheetName, int columnIndex, String filterValue) {
        List<Row> filteredRows = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
                return filteredRows;
            }

            for (Row row : sheet) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && cell.toString().equals(filterValue)) {
                    filteredRows.add(row);
                }
            }

            Reporter.log("Filtered rows in sheet " + sheetName + " based on value: " + filterValue + ".", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Error while filtering rows: " + e.getMessage(), LogLevel.ERROR);
        }
        return filteredRows;
    }

    public static void deleteColumn(String filePath, String sheetName, int columnIndex) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
                return;
            }

            for (Row row : sheet) {
                if (row.getCell(columnIndex) != null) {
                    row.removeCell(row.getCell(columnIndex));
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
            Reporter.log("Deleted column " + columnIndex + " from sheet " + sheetName + ".", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Error while deleting column: " + e.getMessage(), LogLevel.ERROR);
        }
    }
    public static void sortRows(String filePath, String sheetName, int columnIndex, boolean ascending) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
                return;
            }

            List<Row> rows = new ArrayList<>();
            for (Row row : sheet) {
                rows.add(row);
            }

            rows.sort((r1, r2) -> {
                Cell c1 = r1.getCell(columnIndex);
                Cell c2 = r2.getCell(columnIndex);

                if (c1 == null || c2 == null) return 0;
                String v1 = c1.toString();
                String v2 = c2.toString();

                return ascending ? v1.compareTo(v2) : v2.compareTo(v1);
            });

            for (int i = 0; i < rows.size(); i++) {
                Row newRow = sheet.createRow(i);
                copyRow(rows.get(i), newRow);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
            Reporter.log("Sorted rows in sheet " + sheetName + " based on column " + columnIndex +
                    (ascending ? " in ascending order." : " in descending order."), LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Error while sorting rows: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    private static void copyRow(Row sourceRow, Row targetRow) {
        for (int i = sourceRow.getFirstCellNum(); i < sourceRow.getLastCellNum(); i++) {
            Cell newCell = targetRow.createCell(i);
            if (sourceRow.getCell(i) != null) {
                newCell.setCellValue(sourceRow.getCell(i).toString());
            }
        }
    }
    public static void mergeExcelFiles(List<String> filePaths, String outputFilePath) {
        try (Workbook outputWorkbook = new XSSFWorkbook()) {
            Sheet outputSheet = outputWorkbook.createSheet("Merged Data");
            int rowIndex = 0;

            for (String filePath : filePaths) {
                try (FileInputStream fis = new FileInputStream(filePath);
                     Workbook workbook = WorkbookFactory.create(fis)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (Row row : sheet) {
                        Row newRow = outputSheet.createRow(rowIndex++);
                        for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
                            Cell newCell = newRow.createCell(colIndex);
                            Cell sourceCell = row.getCell(colIndex);
                            if (sourceCell != null) {
                                newCell.setCellValue(sourceCell.toString());
                            }
                        }
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                outputWorkbook.write(fos);
            }
            Reporter.log("Successfully merged " + filePaths.size() + " files into " + outputFilePath + ".", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Error while merging Excel files: " + e.getMessage(), LogLevel.ERROR);
        }
    }
    public static boolean validateFileStructure(String filePath, String sheetName, List<String> expectedHeaders) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Reporter.log("Sheet " + sheetName + " does not exist.", LogLevel.ERROR);
                return false;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                Reporter.log("No header row found in sheet " + sheetName + ".", LogLevel.ERROR);
                return false;
            }

            for (int i = 0; i < expectedHeaders.size(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null || !cell.toString().equals(expectedHeaders.get(i))) {
                    Reporter.log("Mismatch in header at column " + (i + 1) + ": Expected '"
                            + expectedHeaders.get(i) + "' but found '" + (cell != null ? cell.toString() : "null") + "'.", LogLevel.ERROR);
                    return false;
                }
            }
            Reporter.log("File structure validated successfully for sheet " + sheetName + ".", LogLevel.INFO_GREEN);
            return true;
        } catch (IOException e) {
            Reporter.log("Error while validating file structure: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }
}