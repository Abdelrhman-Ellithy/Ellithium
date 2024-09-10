package Ellithium.Utilities;

import com.google.gson.*;

import java.io.*;
import java.util.List;

public class JsonHelper extends DataUtils {

    // Method to read JSON data from a file and retrieve a specific key's value
    public static String getJsonData(String fileName, String key) {
        FileReader reader = null;
        try {
            reader = new FileReader(TEST_DATA_PATH + fileName + ".json");
        } catch (FileNotFoundException e) {
            logsUtils.error(Colors.RED + "Failed to find JSON file: " + fileName + Colors.RESET);
            logsUtils.logException(e);
            throw new RuntimeException(e);
        }
        JsonElement jsonElement = JsonParser.parseReader(reader);
        logsUtils.info(Colors.GREEN + "Successfully read JSON file: " + fileName + Colors.RESET);
        return jsonElement.getAsJsonObject().get(key).getAsString();
    }

    // Method to write or update a key-value pair in a JSON file
    public static void setJsonData(String fileName, String key, String value) {
        try (FileReader reader = new FileReader(TEST_DATA_PATH + fileName + ".json")) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Set the new value
            jsonObject.add(key, new JsonPrimitive(value));

            // Write the updated JSON back to the file
            try (FileWriter writer = new FileWriter(TEST_DATA_PATH + fileName + ".json")) {
                writer.write(jsonObject.toString());
                logsUtils.info(Colors.GREEN + "Successfully updated JSON file: " + fileName + Colors.RESET);
            }
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write JSON file: " + fileName + Colors.RESET);
            logsUtils.logException(e);
            throw new RuntimeException(e);
        } catch (JsonSyntaxException e) {
            logsUtils.error(Colors.RED + "JSON syntax error in file: " + fileName + Colors.RESET);
            logsUtils.logException(e);
            throw new RuntimeException(e);
        }
    }

    // Method to write a list of product names as a JSON file content
    public static void writeStringIntoJson(String fileName, List<String> productNames) {
        StringBuilder jsonContent = new StringBuilder();
        for (String productName : productNames) {
            jsonContent.append(productName).append("\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_DATA_PATH + fileName + ".json"))) {
            writer.write(jsonContent.toString());
            logsUtils.info(Colors.GREEN + "Successfully wrote list of product names into JSON file: " + fileName + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write JSON file: " + fileName + Colors.RESET);
            logsUtils.logException(e);
        }
    }

    // Method to write a list of product names into JSON file with new lines
    public static void writeIntoJson(String fileName, List<String> productNames) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_DATA_PATH + fileName + ".json"))) {
            for (String productName : productNames) {
                writer.write(productName);
                writer.newLine(); // Add newline after each product name
            }
            logsUtils.info(Colors.GREEN + "Successfully wrote product names into JSON file: " + fileName + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write JSON file: " + fileName + Colors.RESET);
            logsUtils.logException(e);
        }
    }
}