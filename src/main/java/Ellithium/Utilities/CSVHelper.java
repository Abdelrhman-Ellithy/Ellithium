package Ellithium.Utilities;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.*;
import java.util.*;

public class CSVHelper {

    // Method to get data from a CSV file and return it as a list of maps
    public static List<Map<String, String>> getCsvData(String filePath) {
        Allure.step("Reading CSV data from file: " + filePath, Status.PASSED);
        List<Map<String, String>> data = new ArrayList<>();

        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> recordMap = new HashMap<>();
                csvRecord.toMap().forEach(recordMap::put);
                data.add(recordMap);
            }

            logsUtils.info(Colors.GREEN + "Successfully read CSV file: " + filePath + Colors.RESET);
            Allure.step("Successfully read CSV data from file: " + filePath, Status.PASSED);

        } catch (FileNotFoundException e) {
            logsUtils.error(Colors.RED + "CSV file not found: " + filePath + Colors.RESET);
            Allure.step("CSV file not found: " + filePath, Status.FAILED);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to read CSV file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to read CSV file: " + filePath, Status.FAILED);
        }
        return data;
    }

    // Method to write data to a CSV file and create the file if it doesn't exist
    public static void setCsvData(String filePath, List<Map<String, String>> data) {
        Allure.step("Writing data to CSV file: " + filePath, Status.PASSED);
        File csvFile = new File(filePath + ".csv");
        try {
            // Check if the CSV file exists, if not create a new one
            if (!csvFile.exists()) {
                csvFile.createNewFile();
                logsUtils.info(Colors.GREEN + "Creating new CSV file: " + filePath + Colors.RESET);
                Allure.step("Creating new CSV file: " + filePath, Status.PASSED);
            }

            try (Writer writer = new FileWriter(csvFile);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(data.get(0).keySet().toArray(new String[0])))) {

                for (Map<String, String> record : data) {
                    csvPrinter.printRecord(record.values());
                }

                logsUtils.info(Colors.GREEN + "Successfully wrote CSV file: " + filePath + Colors.RESET);
                Allure.step("Successfully wrote data to CSV file: " + filePath, Status.PASSED);

            } catch (IOException e) {
                logsUtils.error(Colors.RED + "Failed to write CSV file: " + filePath + Colors.RESET);
                logsUtils.logException(e);
                Allure.step("Failed to write data to CSV file: " + filePath, Status.FAILED);
            }
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to create CSV file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to create CSV file: " + filePath, Status.FAILED);
        }
    }

    // Helper method to read specific data based on a column value
    public static List<Map<String, String>> getCsvDataByColumn(String filePath, String columnName, String columnValue) {
        Allure.step("Fetching CSV data by column: " + columnName + " with value: " + columnValue, Status.PASSED);
        List<Map<String, String>> filteredData = new ArrayList<>();

        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                if (csvRecord.get(columnName).equals(columnValue)) {
                    filteredData.add(csvRecord.toMap());
                }
            }

            logsUtils.info(Colors.GREEN + "Successfully fetched filtered CSV data from file: " + filePath + Colors.RESET);
            Allure.step("Successfully fetched filtered CSV data from file: " + filePath, Status.PASSED);

        } catch (FileNotFoundException e) {
            logsUtils.error(Colors.RED + "CSV file not found: " + filePath + Colors.RESET);
            Allure.step("CSV file not found: " + filePath, Status.FAILED);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to fetch CSV data from file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to fetch filtered CSV data from file: " + filePath, Status.FAILED);
        }
        return filteredData;
    }
}