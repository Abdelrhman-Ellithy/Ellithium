package AutoEllithiumSphere.Utilities;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class ExcelHelper extends DataUtils {
    // Excel
    public static List<Map<String, String>> getExcelData(String fileName, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(TEST_DATA_PATH + fileName + ".xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet " + sheetName + " does not exist in " + fileName);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
    public static void setExcelData(String fileName, String sheetName, List<Map<String, String>> data) {
        try (FileInputStream fis = new FileInputStream(TEST_DATA_PATH + fileName + ".xlsx");
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
            try (FileOutputStream fos = new FileOutputStream(TEST_DATA_PATH + fileName + ".xlsx")) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
