package AutoEllithiumSphere.Utilities;

import com.google.gson.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class DataUtils {
    private static final String TEST_DATA_PATH = "src/test/resources/TestData/";

    // JSON File
    public static String getJsonData(String fileName, String key) {
        FileReader reader = null;
        try {
            reader = new FileReader(TEST_DATA_PATH + fileName + ".json");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        JsonElement jsonElement = JsonParser.parseReader(reader);
        return jsonElement.getAsJsonObject().get(key).getAsString();
    }
    public static void setJsonData(String fileName, String key, String value) throws IOException, JsonSyntaxException {
        // Read the existing JSON file
        FileReader reader = new FileReader(TEST_DATA_PATH + fileName + ".json");
        JsonElement jsonElement = JsonParser.parseReader(reader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Set the new value
        jsonObject.add(key, new JsonPrimitive(value));

        // Write the updated JSON back to the file
        FileWriter writer = new FileWriter(TEST_DATA_PATH + fileName + ".json");
        writer.write(jsonObject.toString());
        writer.close();
    }
    public static void writeStringIntoJson(String fileName, List<String> productNames) {
        StringBuilder jsonContent = new StringBuilder();
        for (String productName : productNames) {
            jsonContent.append(productName).append("\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_DATA_PATH + fileName + ".json"))) {
            writer.write(jsonContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeIntoJson(String fileName, List<String> productNames) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_DATA_PATH + fileName + ".json"))) {
            for (String productName : productNames) {
                writer.write(productName);
                writer.newLine(); // Add newline after each product name
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // File Properties
    public static String getDataFromProperties(String key) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(TEST_DATA_PATH + "enviroment.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prop.getProperty(key);
    }
    public static void setDataToProperties(String key, String value) {
        Properties prop = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(TEST_DATA_PATH + "enviroment.properties");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            prop.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(TEST_DATA_PATH + "enviroment.properties");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        prop.setProperty(key, value);
        try {
            prop.store(out, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // CSV
    public static List<Map<String, String>> getCsvData(String fileName) {
        List<Map<String, String>> data = new ArrayList<>();
        try (Reader reader = new FileReader(TEST_DATA_PATH + fileName + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> recordMap = new HashMap<>();
                csvRecord.toMap().forEach(recordMap::put);
                data.add(recordMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
    public static void setCsvData(String fileName, List<Map<String, String>> data) {
        try (Writer writer = new FileWriter(TEST_DATA_PATH + fileName + ".csv");
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(data.get(0).keySet().toArray(new String[0])))) {

            for (Map<String, String> record : data) {
                csvPrinter.printRecord(record.values());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
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