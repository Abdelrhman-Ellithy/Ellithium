package AutoEllithiumSphere.Utilities;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVHelper extends DataUtils {
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
}
