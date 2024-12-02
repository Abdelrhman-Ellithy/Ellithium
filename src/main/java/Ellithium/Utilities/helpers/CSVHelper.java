package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.*;
import java.util.*;

public class CSVHelper {

    // Method to get data from a CSV file and return it as a list of maps
    public static List<Map<String, String>> getCsvData(String filePath) {
        Reporter.log("Attempting to read CSV data from file: ", LogLevel.INFO_GREEN, filePath);
        List<Map<String, String>> data = new ArrayList<>();
        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> recordMap = new HashMap<>();
                csvRecord.toMap().forEach(recordMap::put);
                data.add(recordMap);
            }
            Reporter.log("Successfully read CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (FileNotFoundException e) {
            Reporter.log("CSV file not found: ", LogLevel.ERROR, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to read CSV file: ", LogLevel.ERROR, filePath);
        }
        return data;
    }

    // Method to write data to a CSV file and create the file if it doesn't exist
    public static void setCsvData(String filePath, List<Map<String, String>> data) {
        Reporter.log("Attempting to write data to CSV file: ", LogLevel.INFO_GREEN, filePath);
        File csvFile = new File(filePath + ".csv");
        try {
            // Check if the CSV file exists, if not create a new one
            if (!csvFile.exists()) {
                csvFile.createNewFile();
                Reporter.log("Creating new CSV file: ", LogLevel.INFO_GREEN, filePath);
            }
            try (Writer writer = new FileWriter(csvFile);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(data.get(0).keySet().toArray(new String[0])))) {

                for (Map<String, String> record : data) {
                    csvPrinter.printRecord(record.values());
                }
                Reporter.log("Successfully wrote data to CSV file: ", LogLevel.INFO_GREEN, filePath);
            } catch (IOException e) {
                Reporter.log("Failed to write data to CSV file: ", LogLevel.ERROR, filePath);
            }
        } catch (IOException e) {
            Reporter.log("Failed to create CSV file: ", LogLevel.ERROR, filePath);
        }
    }

    // Helper method to read specific data based on a column value
    public static List<Map<String, String>> getCsvDataByColumn(String filePath, String columnName, String columnValue) {
        Reporter.log("Fetching CSV data by column: " + columnName + " with value: " + columnValue, LogLevel.INFO_GREEN, filePath);
        List<Map<String, String>> filteredData = new ArrayList<>();

        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                if (csvRecord.get(columnName).equals(columnValue)) {
                    filteredData.add(csvRecord.toMap());
                }
            }
            Reporter.log("Successfully fetched filtered CSV data from file: ", LogLevel.INFO_GREEN, filePath);
        } catch (FileNotFoundException e) {
            Reporter.log("CSV file not found: ", LogLevel.ERROR, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to fetch CSV data from file: ", LogLevel.ERROR, filePath);
        }
        return filteredData;
    }
    // Method to read a specific column by name
    public static List<String> readColumn(String filePath, String columnName) {
        List<String> columnData = new ArrayList<>();
        Reporter.log("Attempting to read column: " + columnName + " from CSV file: ", LogLevel.INFO_GREEN, filePath);
        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                columnData.add(record.get(columnName));
            }
            Reporter.log("Successfully read column: " + columnName + " from CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to read column from CSV file: ", LogLevel.ERROR, filePath);
        }
        return columnData;
    }

    // Method to read a specific cell by row and column name
    public static String readCell(String filePath, int rowIndex, String columnName) {
        String cellData = "";
        Reporter.log("Attempting to read cell at row: " + rowIndex + ", column: " + columnName, LogLevel.INFO_GREEN, filePath);
        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            List<CSVRecord> records = csvParser.getRecords();
            if (rowIndex < records.size()) {
                cellData = records.get(rowIndex).get(columnName);
            }
            Reporter.log("Successfully read cell data from CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to read cell data from CSV file: ", LogLevel.ERROR, filePath);
        }
        return cellData;
    }

    // Method to read a specific row by index
    public static Map<String, String> readRow(String filePath, int rowIndex) {
        Map<String, String> rowData = new HashMap<>();
        Reporter.log("Attempting to read row at index: " + rowIndex + " from CSV file: ", LogLevel.INFO_GREEN, filePath);
        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            List<CSVRecord> records = csvParser.getRecords();
            if (rowIndex < records.size()) {
                rowData = records.get(rowIndex).toMap();
            }
            Reporter.log("Successfully read row from CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to read row from CSV file: ", LogLevel.ERROR, filePath);
        }
        return rowData;
    }

    // Method to write to a specific cell
    public static void writeCell(String filePath, int rowIndex, String columnName, String value) {
        Reporter.log("Attempting to write to cell at row: " + rowIndex + ", column: " + columnName, LogLevel.INFO_GREEN, filePath);
        try {
            List<Map<String, String>> data = getCsvData(filePath);
            if (rowIndex < data.size()) {
                data.get(rowIndex).put(columnName, value);
            }
            setCsvData(filePath, data);
            Reporter.log("Successfully wrote data to cell in CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to write to cell in CSV file: ", LogLevel.ERROR, filePath);
        }
    }
    public static boolean doesFileExist(String filePath) {
        File csvFile = new File(filePath + ".csv");
        return csvFile.exists();
    }
    public static void appendData(String filePath, List<Map<String, String>> data) {
        Reporter.log("Attempting to append data to CSV file: ", LogLevel.INFO_GREEN, filePath);
        try (Writer writer = new FileWriter(filePath + ".csv", true);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(data.get(0).keySet().toArray(new String[0])).withSkipHeaderRecord())) {

            for (Map<String, String> record : data) {
                csvPrinter.printRecord(record.values());
            }
            Reporter.log("Successfully appended data to CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to append data to CSV file: ", LogLevel.ERROR, filePath);
        }
    }
    public static void deleteRow(String filePath, int rowIndex) {
        Reporter.log("Attempting to delete row at index: " + rowIndex, LogLevel.INFO_GREEN, filePath);
        try {
            List<Map<String, String>> data = getCsvData(filePath);
            if (rowIndex < data.size()) {
                data.remove(rowIndex);
            }
            setCsvData(filePath, data);
            Reporter.log("Successfully deleted row from CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to delete row from CSV file: ", LogLevel.ERROR, filePath);
        }
    }
    public static void deleteColumn(String filePath, String columnName) {
        Reporter.log("Attempting to delete column: " + columnName + " from CSV file: ", LogLevel.INFO_GREEN, filePath);
        try {
            List<Map<String, String>> data = getCsvData(filePath);
            for (Map<String, String> record : data) {
                record.remove(columnName);
            }
            setCsvData(filePath, data);
            Reporter.log("Successfully deleted column from CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to delete column from CSV file: ", LogLevel.ERROR, filePath);
        }
    }

    public static List<Map<String, String>> filterData(String filePath, Map<String, String> conditions) {
        Reporter.log("Attempting to filter data based on conditions: " + conditions, LogLevel.INFO_GREEN, filePath);
        List<Map<String, String>> filteredData = new ArrayList<>();
        try {
            List<Map<String, String>> data = getCsvData(filePath);
            for (Map<String, String> record : data) {
                boolean matches = true;
                for (Map.Entry<String, String> condition : conditions.entrySet()) {
                    if (!record.getOrDefault(condition.getKey(), "").equals(condition.getValue())) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    filteredData.add(record);
                }
            }
            Reporter.log("Successfully filtered data from CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to filter data from CSV file: ", LogLevel.ERROR, filePath);
        }
        return filteredData;
    }
    public static List<Map<String, String>> sortData(String filePath, String columnName, boolean ascending) {
        Reporter.log("Attempting to sort data by column: " + columnName, LogLevel.INFO_GREEN, filePath);
        List<Map<String, String>> sortedData = getCsvData(filePath);
        sortedData.sort((row1, row2) -> {
            String val1 = row1.get(columnName);
            String val2 = row2.get(columnName);
            return ascending ? val1.compareTo(val2) : val2.compareTo(val1);
        });
        Reporter.log("Successfully sorted data in CSV file: ", LogLevel.INFO_GREEN, filePath);
        return sortedData;
    }
    public static void replaceColumnData(String filePath, String columnName, String oldValue, String newValue) {
        Reporter.log("Attempting to replace data in column: " + columnName, LogLevel.INFO_GREEN, filePath);
        try {
            List<Map<String, String>> data = getCsvData(filePath);
            for (Map<String, String> record : data) {
                if (record.containsKey(columnName) && record.get(columnName).equals(oldValue)) {
                    record.put(columnName, newValue);
                }
            }
            setCsvData(filePath, data);
            Reporter.log("Successfully replaced column data in CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to replace column data in CSV file: ", LogLevel.ERROR, filePath);
        }
    }
    public static List<List<String>> exportToList(String filePath) {
        Reporter.log("Attempting to export CSV data to list of lists: ", LogLevel.INFO_GREEN, filePath);
        List<List<String>> listData = new ArrayList<>();
        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                listData.add(new ArrayList<>(record.toMap().values()));
            }
            Reporter.log("Successfully exported CSV data to list: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to export CSV data to list: ", LogLevel.ERROR, filePath);
        }
        return listData;
    }
    public static void mergeCsvFiles(String filePath1, String filePath2, String outputFilePath) {
        Reporter.log("Attempting to merge CSV files: " + filePath1 + " and " + filePath2, LogLevel.INFO_GREEN, outputFilePath);
        try {
            List<Map<String, String>> data1 = getCsvData(filePath1);
            List<Map<String, String>> data2 = getCsvData(filePath2);

            data1.addAll(data2);
            setCsvData(outputFilePath, data1);
            Reporter.log("Successfully merged CSV files: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (Exception e) {
            Reporter.log("Failed to merge CSV files: ", LogLevel.ERROR, outputFilePath);
        }
    }
    public static boolean validateFileStructure(String filePath, List<String> expectedColumns) {
        Reporter.log("Validating structure of CSV file: ", LogLevel.INFO_GREEN, filePath);
        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            Set<String> actualColumns = csvParser.getHeaderMap().keySet();
            return actualColumns.containsAll(expectedColumns);
        } catch (IOException e) {
            Reporter.log("Failed to validate CSV file structure: ", LogLevel.ERROR, filePath);
            return false;
        }
    }

}
