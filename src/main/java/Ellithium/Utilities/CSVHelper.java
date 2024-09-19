package Ellithium.Utilities;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.*;
import java.util.*;

public class CSVHelper {

    public static List<Map<String, String>> getCsvData(String filePath) {
        List<Map<String, String>> data = new ArrayList<>();
        try (Reader reader = new FileReader(filePath + ".csv");
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> recordMap = new HashMap<>();
                csvRecord.toMap().forEach(recordMap::put);
                data.add(recordMap);
            }
            logsUtils.info(Colors.GREEN + "Successfully read CSV file: " + filePath + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to read CSV file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
        }
        return data;
    }

    public static void setCsvData(String filePath, List<Map<String, String>> data) {
        try (Writer writer = new FileWriter(filePath+ ".csv");
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(data.get(0).keySet().toArray(new String[0])))) {
            for (Map<String, String> record : data) {
                csvPrinter.printRecord(record.values());
            }
            logsUtils.info(Colors.GREEN + "Successfully write CSV file: " + filePath + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write CSV file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
        }
    }
}
