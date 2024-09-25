package Ellithium.Utilities;

import com.google.gson.*;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;

import java.io.*;
import java.util.List;

public class JsonHelper {

    // Method to read JSON data from a file and retrieve a specific key's value
    public static String getJsonData(String filePath, String key) {
        Allure.step("Reading JSON data from file: " + filePath + " for key: " + key, Status.PASSED);
        FileReader reader = null;

        try {
            reader = new FileReader(filePath + ".json");
            JsonElement jsonElement = JsonParser.parseReader(reader);

            logsUtils.info(Colors.GREEN + "Successfully read JSON file: " + filePath + Colors.RESET);
            Allure.step("Successfully read JSON file: " + filePath, Status.PASSED);

            return jsonElement.getAsJsonObject().get(key).getAsString();

        } catch (FileNotFoundException e) {
            logsUtils.error(Colors.RED + "Failed to find JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to find JSON file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        }
    }

    // Method to write or update a key-value pair in a JSON file
    public static void setJsonData(String filePath, String key, String value) {
        Allure.step("Updating JSON data in file: " + filePath + " with key: " + key + " and value: " + value, Status.PASSED);

        try (FileReader reader = new FileReader(filePath + ".json")) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Set the new value
            jsonObject.add(key, new JsonPrimitive(value));

            // Write the updated JSON back to the file
            try (FileWriter writer = new FileWriter(filePath + ".json")) {
                writer.write(jsonObject.toString());
                logsUtils.info(Colors.GREEN + "Successfully updated JSON file: " + filePath + Colors.RESET);
                Allure.step("Successfully updated JSON file: " + filePath, Status.PASSED);
            }

        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to write JSON file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        } catch (JsonSyntaxException e) {
            logsUtils.error(Colors.RED + "JSON syntax error in file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("JSON syntax error in file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        }
    }

    // Method to write a list of product names as a JSON file content
    public static void writeStringIntoJson(String filePath, List<String> productNames) {
        Allure.step("Writing list of product names to JSON file: " + filePath, Status.PASSED);

        StringBuilder jsonContent = new StringBuilder();
        for (String productName : productNames) {
            jsonContent.append(productName).append("\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath + ".json"))) {
            writer.write(jsonContent.toString());
            logsUtils.info(Colors.GREEN + "Successfully wrote list of product names into JSON file: " + filePath + Colors.RESET);
            Allure.step("Successfully wrote product names into JSON file: " + filePath, Status.PASSED);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to write JSON file: " + filePath, Status.FAILED);
        }
    }

    // Method to write a list of product names into JSON file with new lines
    public static void writeIntoJson(String filePath, List<String> productNames) {
        Allure.step("Writing product names into JSON file with new lines: " + filePath, Status.PASSED);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath + ".json"))) {
            for (String productName : productNames) {
                writer.write(productName);
                writer.newLine(); // Add newline after each product name
            }
            logsUtils.info(Colors.GREEN + "Successfully wrote product names into JSON file: " + filePath + Colors.RESET);
            Allure.step("Successfully wrote product names into JSON file: " + filePath, Status.PASSED);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to write JSON file: " + filePath, Status.FAILED);
        }
    }
}