package Ellithium.Utilities;

import com.google.gson.*;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;

import java.io.*;
import java.util.*;

public class JsonHelper {

    // Method to read JSON data and return it as a list of maps
    public static List<Map<String, String>> getJsonData(String filePath) {
        List<Map<String, String>> data = new ArrayList<>();
        File jsonFile = new File(filePath + ".json");

        if (!jsonFile.exists()) {
            logsUtils.error(Colors.RED + "JSON file does not exist: " + filePath + Colors.RESET);
            Allure.step("JSON file does not exist: " + filePath, Status.FAILED);
            throw new RuntimeException("JSON file not found: " + filePath);
        }

        Allure.step("Attempting to read JSON data from file: " + filePath, Status.PASSED);

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);

            if (!jsonElement.isJsonArray()) {
                throw new IllegalArgumentException("JSON data in file " + filePath + " is not an array");
            }

            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                Map<String, String> recordMap = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    recordMap.put(entry.getKey(), entry.getValue().getAsString());
                }

                data.add(recordMap);
            }
            logsUtils.info(Colors.GREEN + "Successfully read JSON file: " + filePath + Colors.RESET);
            Allure.step("Successfully read JSON data from file: " + filePath, Status.PASSED);

        } catch (FileNotFoundException e) {
            logsUtils.error(Colors.RED + "Failed to find JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to find JSON file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        } catch (IOException | JsonSyntaxException e) {
            logsUtils.error(Colors.RED + "Failed to read JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to read JSON data from file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        }

        return data;
    }

    // Method to write a list of maps as JSON data
    public static void setJsonData(String filePath, List<Map<String, String>> data) {
        Allure.step("Attempting to write JSON data to file: " + filePath, Status.PASSED);
        File jsonFile = new File(filePath + ".json");

        // If the file doesn't exist, create it
        if (!jsonFile.exists()) {
            try {
                if (jsonFile.createNewFile()) {
                    logsUtils.info(Colors.GREEN + "Created new JSON file: " + filePath + Colors.RESET);
                    Allure.step("Created new JSON file: " + filePath, Status.PASSED);
                }
            } catch (IOException e) {
                logsUtils.error(Colors.RED + "Failed to create JSON file: " + filePath + Colors.RESET);
                logsUtils.logException(e);
                Allure.step("Failed to create JSON file: " + filePath, Status.FAILED);
                throw new RuntimeException(e);
            }
        }

        JsonArray jsonArray = new JsonArray();
        for (Map<String, String> record : data) {
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<String, String> entry : record.entrySet()) {
                jsonObject.add(entry.getKey(), new JsonPrimitive(entry.getValue()));
            }
            jsonArray.add(jsonObject);
        }

        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(jsonArray));
            logsUtils.info(Colors.GREEN + "Successfully wrote data to JSON file: " + filePath + Colors.RESET);
            Allure.step("Successfully wrote JSON data to file: " + filePath, Status.PASSED);

        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to write JSON data to file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        }
    }

    // Method to get a value for a specific key from JSON file
    public static String getJsonKeyValue(String filePath, String key) {
        File jsonFile = new File(filePath + ".json");

        if (!jsonFile.exists()) {
            logsUtils.error(Colors.RED + "JSON file does not exist: " + filePath + Colors.RESET);
            Allure.step("JSON file does not exist: " + filePath, Status.FAILED);
            throw new RuntimeException("JSON file not found: " + filePath);
        }

        Allure.step("Reading value for key: " + key + " from JSON file: " + filePath, Status.PASSED);

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            logsUtils.info(Colors.GREEN + "Successfully read JSON file: " + filePath + Colors.RESET);
            Allure.step("Successfully read value for key: " + key + " from JSON file: " + filePath, Status.PASSED);

            return jsonObject.get(key).getAsString();

        } catch (FileNotFoundException e) {
            logsUtils.error(Colors.RED + "Failed to find JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to find JSON file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        } catch (IOException | JsonSyntaxException e) {
            logsUtils.error(Colors.RED + "Failed to read JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to read JSON file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        }
    }

    // Method to set or update a specific key-value pair in JSON file
    public static void setJsonKeyValue(String filePath, String key, String value) {
        File jsonFile = new File(filePath + ".json");

        // If the file doesn't exist, create it
        if (!jsonFile.exists()) {
            try {
                if (jsonFile.createNewFile()) {
                    logsUtils.info(Colors.GREEN + "Created new JSON file: " + filePath + Colors.RESET);
                    Allure.step("Created new JSON file: " + filePath, Status.PASSED);
                }
            } catch (IOException e) {
                logsUtils.error(Colors.RED + "Failed to create JSON file: " + filePath + Colors.RESET);
                logsUtils.logException(e);
                Allure.step("Failed to create JSON file: " + filePath, Status.FAILED);
                throw new RuntimeException(e);
            }
        }

        Allure.step("Setting value for key: " + key + " in JSON file: " + filePath, Status.PASSED);

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            jsonObject.addProperty(key, value);

            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject));
                logsUtils.info(Colors.GREEN + "Successfully updated JSON file: " + filePath + Colors.RESET);
                Allure.step("Successfully updated key-value pair in JSON file: " + filePath, Status.PASSED);
            }

        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to update JSON file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            Allure.step("Failed to update JSON file: " + filePath, Status.FAILED);
            throw new RuntimeException(e);
        }
    }
}
