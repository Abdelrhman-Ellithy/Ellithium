package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.*;
import java.util.*;

/**
 * Utility class for handling CSV file operations including reading, writing, and manipulating CSV data.
 */
public class CSVHelper {

    /**
     * Reads data from a CSV file and returns it as a list of maps.
     * @param filePath Path to the CSV file
     * @return List of maps where each map represents a row with column name as key
     */
    public static List<Map<String, String>> getCsvData(String filePath) {
        Reporter.log("Attempting to read CSV data from file: ", LogLevel.INFO_GREEN, filePath);
        List<Map<String, String>> data = new ArrayList<>();
        try (Reader reader = new FileReader(filePath );
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

    /**
     * Writes data to a CSV file, creating the file if it doesn't exist.
     * @param filePath Path where the CSV file should be created/written
     * @param data List of maps containing the data to write
     */
    public static void setCsvData(String filePath, List<Map<String, String>> data) {
        Reporter.log("Attempting to write data to CSV file: ", LogLevel.INFO_GREEN, filePath);
        File csvFile = new File(filePath );
        try {
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

    /**
     * Retrieves specific data from CSV file based on a column value.
     * @param filePath Path to the CSV file
     * @param columnName Name of the column to filter by
     * @param columnValue Value to match in the column
     * @return List of matching rows as maps
     */
    public static List<Map<String, String>> getCsvDataByColumn(String filePath, String columnName, String columnValue) {
        Reporter.log("Fetching CSV data by column: " + columnName + " with value: " + columnValue, LogLevel.INFO_GREEN, filePath);
        List<Map<String, String>> filteredData = new ArrayList<>();

        try (Reader reader = new FileReader(filePath );
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

    /**
     * Reads all values from a specific column in the CSV file.
     * @param filePath Path to the CSV file
     * @param columnName Name of the column to read
     * @return List of values in the specified column
     */
    public static List<String> readColumn(String filePath, String columnName) {
        List<String> columnData = new ArrayList<>();
        Reporter.log("Attempting to read column: " + columnName + " from CSV file: ", LogLevel.INFO_GREEN, filePath);
        try (Reader reader = new FileReader(filePath );
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

    /**
     * Reads a specific cell value from the CSV file.
     * @param filePath Path to the CSV file
     * @param rowIndex Index of the row
     * @param columnName Name of the column
     * @return Value in the specified cell
     */
    public static String readCell(String filePath, int rowIndex, String columnName) {
        String cellData = "";
        Reporter.log("Attempting to read cell at row: " + rowIndex + ", column: " + columnName, LogLevel.INFO_GREEN, filePath);
        try (Reader reader = new FileReader(filePath );
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

    /**
     * Reads a specific row from the CSV file.
     * @param filePath Path to the CSV file
     * @param rowIndex Index of the row to read
     * @return Map containing the row data
     */
    public static Map<String, String> readRow(String filePath, int rowIndex) {
        Map<String, String> rowData = new HashMap<>();
        Reporter.log("Attempting to read row at index: " + rowIndex + " from CSV file: ", LogLevel.INFO_GREEN, filePath);
        try (Reader reader = new FileReader(filePath );
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

    /**
     * Writes a value to a specific cell in the CSV file.
     * @param filePath Path to the CSV file
     * @param rowIndex Index of the row
     * @param columnName Name of the column
     * @param value Value to write
     */
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

    /**
     * Checks if a CSV file exists at the specified path.
     * @param filePath Path to check
     * @return true if file exists, false otherwise
     */
    public static boolean doesFileExist(String filePath) {
        File csvFile = new File(filePath );
        return csvFile.exists();
    }

    /**
     * Appends data to an existing CSV file.
     * @param filePath Path to the CSV file
     * @param data List of maps containing the data to append
     */
    public static void appendData(String filePath, List<Map<String, String>> data) {
        Reporter.log("Attempting to append data to CSV file: ", LogLevel.INFO_GREEN, filePath);
        try (Writer writer = new FileWriter(filePath , true);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(data.get(0).keySet().toArray(new String[0])).withSkipHeaderRecord())) {

            for (Map<String, String> record : data) {
                csvPrinter.printRecord(record.values());
            }
            Reporter.log("Successfully appended data to CSV file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to append data to CSV file: ", LogLevel.ERROR, filePath);
        }
    }

    /**
     * Deletes a specific row from the CSV file.
     * @param filePath Path to the CSV file
     * @param rowIndex Index of the row to delete
     */
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

    /**
     * Deletes a specific column from the CSV file.
     * @param filePath Path to the CSV file
     * @param columnName Name of the column to delete
     */
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

    /**
     * Filters CSV data based on multiple conditions.
     * @param filePath Path to the CSV file
     * @param conditions Map of column names and values to filter by
     * @return List of matching rows
     */
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

    /**
     * Sorts CSV data by a specific column.
     * @param filePath Path to the CSV file
     * @param columnName Column to sort by
     * @param ascending true for ascending order, false for descending
     * @return Sorted list of data
     */
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

    /**
     * Replaces all occurrences of a value in a specific column.
     * @param filePath Path to the CSV file
     * @param columnName Name of the column
     * @param oldValue Value to replace
     * @param newValue New value
     */
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

    /**
     * Exports CSV data to a list of lists format.
     * @param filePath Path to the CSV file
     * @return List of lists containing the CSV data
     */
    public static List<List<String>> exportToList(String filePath) {
        Reporter.log("Attempting to export CSV data to list of lists: ", LogLevel.INFO_GREEN, filePath);
        List<List<String>> listData = new ArrayList<>();
        try (Reader reader = new FileReader(filePath );
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

    /**
     * Merges two CSV files into one.
     * @param filePath1 Path to first CSV file
     * @param filePath2 Path to second CSV file
     * @param outputFilePath Path for the merged output file
     */
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

    /**
     * Merges multiple CSV files into one.
     * @param outputFilePath Path for the merged output file
     * @param inputFiles List of input file paths to merge
     */
    public static void mergeCsvFiles(String outputFilePath, List<String> inputFiles) {
        Reporter.log("Merging CSV files into: " + outputFilePath, LogLevel.INFO_GREEN, "");
        List<Map<String, String>> mergedData = new ArrayList<>();
        for (String file : inputFiles) {
            mergedData.addAll(getCsvData(file));
        }
        setCsvData(outputFilePath, mergedData);
        Reporter.log("Successfully merged CSV files.", LogLevel.INFO_GREEN, outputFilePath);
    }

    /**
     * Validates if the CSV file has the expected column structure.
     * @param filePath Path to the CSV file
     * @param expectedColumns List of expected column names
     * @return true if structure is valid, false otherwise
     */
    public static boolean validateFileStructure(String filePath, List<String> expectedColumns) {
        Reporter.log("Validating structure of CSV file: ", LogLevel.INFO_GREEN, filePath);
        try (Reader reader = new FileReader(filePath );
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            Set<String> actualColumns = csvParser.getHeaderMap().keySet();
            return actualColumns.containsAll(expectedColumns);
        } catch (IOException e) {
            Reporter.log("Failed to validate CSV file structure: ", LogLevel.ERROR, filePath);
            return false;
        }
    }

    /**
     * Updates a specific row with new data.
     * @param filePath Path to the CSV file
     * @param rowIndex Index of the row to update
     * @param newData Map containing the new row data
     */
    public static void updateRow(String filePath, int rowIndex, Map<String, String> newData) {
        List<Map<String, String>> data = getCsvData(filePath);
        if (rowIndex < data.size()) {
            data.set(rowIndex, newData);
            setCsvData(filePath, data);
            Reporter.log("Successfully updated row in CSV file.", LogLevel.INFO_GREEN, filePath);
        } else {
            Reporter.log("Row index out of bounds.", LogLevel.ERROR, filePath);
        }
    }

    /**
     * Updates all values in a specific column.
     * @param filePath Path to the CSV file
     * @param columnName Name of the column to update
     * @param newValue New value for all rows in the column
     */
    public static void updateColumn(String filePath, String columnName, String newValue) {
        List<Map<String, String>> data = getCsvData(filePath);
        for (Map<String, String> row : data) {
            if (row.containsKey(columnName)) {
                row.put(columnName, newValue);
            }
        }
        setCsvData(filePath, data);
        Reporter.log("Successfully updated column in CSV file.", LogLevel.INFO_GREEN, filePath);
    }

    /**
     * Gets the total number of rows in the CSV file.
     * @param filePath Path to the CSV file
     * @return Number of rows in the file
     */
    public static int getRowCount(String filePath) {
        return getCsvData(filePath).size();
    }

    /**
     * Finds duplicate entries in a specific column.
     * @param filePath Path to the CSV file
     * @param columnName Name of the column to check for duplicates
     * @return List of duplicate values found
     */
    public static List<String> findDuplicateEntries(String filePath, String columnName) {
        Reporter.log("Finding duplicates in column: " + columnName, LogLevel.INFO_GREEN, filePath);
        List<String> duplicates = new ArrayList<>();
        try {
            List<String> columnValues = readColumn(filePath, columnName);
            Set<String> uniqueValues = new HashSet<>();
            for (String value : columnValues) {
                if (!uniqueValues.add(value)) {
                    duplicates.add(value);
                }
            }
            Reporter.log("Found " + duplicates.size() + " duplicates", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to find duplicates: " + e.getMessage(), LogLevel.ERROR, filePath);
        }
        return duplicates;
    }

    /**
     * Adds a new column to the CSV file with a default value.
     * @param filePath Path to the CSV file
     * @param columnName Name of the new column
     * @param defaultValue Default value for all rows in the new column
     */
    public static void addNewColumn(String filePath, String columnName, String defaultValue) {
        Reporter.log("Adding new column: " + columnName, LogLevel.INFO_GREEN, filePath);
        try {
            List<Map<String, String>> data = getCsvData(filePath);
            for (Map<String, String> row : data) {
                row.put(columnName, defaultValue);
            }
            setCsvData(filePath, data);
            Reporter.log("Column added successfully", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to add column: " + e.getMessage(), LogLevel.ERROR, filePath);
        }
    }

    /**
     * Renames a column in the CSV file.
     * @param filePath Path to the CSV file
     * @param oldName Current name of the column
     * @param newName New name for the column
     */
    public static void renameColumn(String filePath, String oldName, String newName) {
        Reporter.log("Renaming column: " + oldName + " to " + newName, LogLevel.INFO_GREEN, filePath);
        try {
            List<Map<String, String>> data = getCsvData(filePath);
            for (Map<String, String> row : data) {
                String value = row.remove(oldName);
                row.put(newName, value);
            }
            setCsvData(filePath, data);
            Reporter.log("Column renamed successfully", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to rename column: " + e.getMessage(), LogLevel.ERROR, filePath);
        }
    }
}
