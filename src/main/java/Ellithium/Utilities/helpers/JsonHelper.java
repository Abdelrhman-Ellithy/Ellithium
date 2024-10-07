package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import static Ellithium.core.reporting.Reporter.log;
import com.google.gson.*;
import java.io.*;
import java.util.*;

public class JsonHelper {

    // Method to read JSON data and return it as a list of maps
    public static List<Map<String, String>> getJsonData(String filePath) {
        List<Map<String, String>> data = new ArrayList<>();
        File jsonFile = new File(filePath + ".json");

        if (!jsonFile.exists()) {
            log("JSON file does not exist: ", LogLevel.ERROR, filePath);
            throw new RuntimeException("JSON file not found: " + filePath);
        }

        log("Attempting to read JSON data from file: ", LogLevel.INFO_BLUE, filePath);

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
            log("Successfully read JSON file: ", LogLevel.INFO_GREEN, filePath);

        } catch (FileNotFoundException e) {
            log("Failed to find JSON file: ", LogLevel.ERROR, filePath);
            throw new RuntimeException(e);
        } catch (IOException | JsonSyntaxException e) {
            log("Failed to read JSON file: ", LogLevel.ERROR, filePath);
        }
        return data;
    }

    // Method to write a list of maps as JSON data
    public static void setJsonData(String filePath, List<Map<String, String>> data) {
        log("Attempting to write JSON data to file: ", LogLevel.INFO_BLUE, filePath);
        File jsonFile = new File(filePath + ".json");

        if (!jsonFile.exists()) {
            try {
                if (jsonFile.createNewFile()) {
                    log("Created new JSON file: ", LogLevel.INFO_GREEN, filePath);
                }
            } catch (IOException e) {
                log("Failed to create JSON file: ", LogLevel.ERROR, filePath);
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
            log("Successfully wrote data to JSON file: ", LogLevel.INFO_GREEN, filePath);

        } catch (IOException e) {
            log("Failed to write JSON file: ", LogLevel.ERROR, filePath);
        }
    }

    // Method to get a value for a specific key from JSON file
    public static String getJsonKeyValue(String filePath, String key) {
        File jsonFile = new File(filePath + ".json");
        if (!jsonFile.exists()) {
            log("JSON file does not exist: ", LogLevel.ERROR, filePath);
        }
        log("Reading value for key: " + key + " from JSON file: ", LogLevel.INFO_BLUE, filePath);
        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            log("Successfully read value for key: " + key + " from JSON file: ", LogLevel.INFO_GREEN, filePath);
            var value= jsonObject.get(key);
            if(value!=null) {
                return value.getAsString();
            }
            else {
                return null;
            }
        } catch (FileNotFoundException e) {
            log("Failed to find JSON file: ", LogLevel.ERROR, filePath);
            return null;
        } catch (IOException | JsonSyntaxException e) {
            log("Failed to read JSON file: ", LogLevel.ERROR, filePath);
            return null;
        }
    }
    // Method to set or update a specific key-value pair in JSON file
    public static void setJsonKeyValue(String filePath, String key, String value) {
        File jsonFile = new File(filePath + ".json");

        if (!jsonFile.exists()) {
            try {
                if (jsonFile.createNewFile()) {
                    log("Created new JSON file: ", LogLevel.INFO_GREEN, filePath);
                }
            } catch (IOException e) {
                log("Failed to create JSON file: ", LogLevel.ERROR, filePath);
            }
        }
        log("Setting value for key: " + key + " in JSON file: ", LogLevel.INFO_BLUE, filePath);
        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            jsonObject.addProperty(key, value);

            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject));
                log("Successfully updated JSON file: ", LogLevel.INFO_GREEN, filePath);
            }

        } catch (IOException e) {
            log("Failed to update JSON file: ", LogLevel.ERROR, filePath);
        }
    }
}
