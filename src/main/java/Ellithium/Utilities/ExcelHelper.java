package Ellithium.Utilities;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ExcelHelper {

    // Method to get Excel data
    public static List<Map<String, String>> getExcelData(String filePath, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        Allure.step("Attempting to read Excel data from file: " + filePath + ", sheet: " + sheetName, Status.PASSED);

        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet " + sheetName + " does not exist in " + filePath);
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
            logsUtils.info(Colors.GREEN + "Successfully read Excel file: " + filePath + Colors.RESET);
            Allure.step("Successfully read Excel data from file: " + filePath + ", sheet: " + sheetName, Status.PASSED);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to read Excel file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to read Excel data from file: " + filePath + ", sheet: " + sheetName, Status.FAILED);
        }
        return data;
    }

    // Method to set Excel data
    public static void setExcelData(String filePath, String sheetName, List<Map<String, String>> data) {
        Allure.step("Attempting to write data to Excel file: " + filePath + ", sheet: " + sheetName, Status.PASSED);
        try (FileInputStream fis = new FileInputStream(filePath + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
            }

            int rowNum = 0;
            if (sheet.getRow(0) == null) {
                Row headerRow = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String key : data.get(0).keySet()) {
                    Cell cell = headerRow.createCell(colNum++);
                    cell.setCellValue(key);
                }
            } else {
                rowNum = sheet.getLastRowNum() + 1;
            }

            for (Map<String, String> record : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String value : record.values()) {
                    Cell cell = row.createCell(colNum++);
                    cell.setCellValue(value);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath + ".xlsx")) {
                workbook.write(fos);
                logsUtils.info(Colors.GREEN + "Successfully wrote data to Excel file: " + filePath + Colors.RESET);
                Allure.step("Successfully wrote data to Excel file: " + filePath + ", sheet: " + sheetName, Status.PASSED);
            }
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write data to Excel file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to write data to Excel file: " + filePath + ", sheet: " + sheetName, Status.FAILED);
        }
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
