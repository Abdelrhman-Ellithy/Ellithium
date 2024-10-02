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
}
