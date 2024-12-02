package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import static Ellithium.core.reporting.Reporter.log;

import Ellithium.core.reporting.Reporter;
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
        }

        log("Attempting to read JSON data from file: ", LogLevel.INFO_BLUE, filePath);

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);

            if (!jsonElement.isJsonArray()) {
                Reporter.log("JSON data in file ",LogLevel.ERROR,filePath + " is not an array");
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
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
            throw new RuntimeException(e);
        } catch (IOException | JsonSyntaxException e) {
            log("Failed to read JSON file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
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
                Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
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
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
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
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
            return null;
        } catch (IOException | JsonSyntaxException e) {
            log("Failed to read JSON file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
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
                Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
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
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
        }

    }
    public static boolean validateJsonKeys(String filePath, List<String> requiredKeys) {
        log("Validating keys in JSON file: ", LogLevel.INFO_BLUE, filePath);
        File jsonFile = new File(filePath + ".json");

        if (!jsonFile.exists()) {
            log("JSON file does not exist: ", LogLevel.ERROR, filePath);
            return false;
        }

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            for (String key : requiredKeys) {
                if (!jsonObject.has(key)) {
                    log("Missing key in JSON file: " + key, LogLevel.ERROR);
                    return false;
                }
            }
            log("All required keys are present in JSON file: ", LogLevel.INFO_GREEN, filePath);
            return true;

        } catch (IOException e) {
            log("Failed to validate keys in JSON file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
        return false;
    }
    // Method to read nested JSON data and return it as a Map
    public static Map<String, Object> getNestedJsonData(String filePath) {
        File jsonFile = new File(filePath + ".json");
        if (!jsonFile.exists()) {
            log("JSON file does not exist: ", LogLevel.ERROR, filePath);
        }

        log("Attempting to read nested JSON data from file: ", LogLevel.INFO_BLUE, filePath);

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            return parseJsonObject(jsonObject);

        } catch (FileNotFoundException e) {
            log("Failed to find JSON file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return Collections.emptyMap();

        } catch (IOException | JsonSyntaxException e) {
            log("Failed to read JSON file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return Collections.emptyMap();
        }
    }

    // Recursive helper method to parse JsonObject into a Map
    private static Map<String, Object> parseJsonObject(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                map.put(entry.getKey(), parseJsonObject(value.getAsJsonObject()));
            } else if (value.isJsonArray()) {
                map.put(entry.getKey(), parseJsonArray(value.getAsJsonArray()));
            } else {
                map.put(entry.getKey(), value.getAsString());
            }
        }
        return map;
    }

    // Recursive helper method to parse JsonArray into a List
    private static List<Object> parseJsonArray(JsonArray jsonArray) {
        List<Object> list = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            if (element.isJsonObject()) {
                list.add(parseJsonObject(element.getAsJsonObject()));
            } else if (element.isJsonArray()) {
                list.add(parseJsonArray(element.getAsJsonArray()));
            } else {
                list.add(element.getAsString());
            }
        }
        return list;
    }

    // Method to compare two JSON files for equality
    public static boolean compareJsonFiles(String filePath1, String filePath2) {
        File file1 = new File(filePath1 + ".json");
        File file2 = new File(filePath2 + ".json");

        if (!file1.exists() || !file2.exists()) {
            log("One or both JSON files do not exist.", LogLevel.ERROR, filePath1 + ", " + filePath2);
            return false;
        }

        log("Comparing JSON files: ", LogLevel.INFO_BLUE, filePath1 + " and " + filePath2);

        try (FileReader reader1 = new FileReader(file1); FileReader reader2 = new FileReader(file2)) {
            JsonElement json1 = JsonParser.parseReader(reader1);
            JsonElement json2 = JsonParser.parseReader(reader2);
            boolean isEqual = json1.equals(json2);
            log("Comparison result: " + isEqual, isEqual ? LogLevel.INFO_GREEN : LogLevel.ERROR, null);
            return isEqual;

        } catch (IOException | JsonSyntaxException e) {
            log("Failed to compare JSON files.", LogLevel.ERROR, filePath1 + ", " + filePath2);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return false;
        }
    }

    // Method to merge JSON data from two files and save to a new file
    public static void mergeJsonFiles(String sourceFilePath1, String sourceFilePath2, String targetFilePath) {
        File file1 = new File(sourceFilePath1 + ".json");
        File file2 = new File(sourceFilePath2 + ".json");
        File targetFile = new File(targetFilePath + ".json");

        if (!file1.exists() || !file2.exists()) {
            log("One or both source JSON files do not exist.", LogLevel.ERROR, sourceFilePath1 + ", " + sourceFilePath2);
            return;
        }

        log("Merging JSON files: ", LogLevel.INFO_BLUE, sourceFilePath1 + " and " + sourceFilePath2);

        try (FileReader reader1 = new FileReader(file1); FileReader reader2 = new FileReader(file2)) {
            JsonObject json1 = JsonParser.parseReader(reader1).getAsJsonObject();
            JsonObject json2 = JsonParser.parseReader(reader2).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : json2.entrySet()) {
                json1.add(entry.getKey(), entry.getValue());
            }

            try (FileWriter writer = new FileWriter(targetFile)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json1));
                log("Successfully merged JSON files to: ", LogLevel.INFO_GREEN, targetFilePath);
            }

        } catch (IOException | JsonSyntaxException e) {
            log("Failed to merge JSON files.", LogLevel.ERROR, sourceFilePath1 + ", " + sourceFilePath2);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

    // Method to update a nested JSON key with a new value
    public static void updateNestedJsonKey(String filePath, List<String> keys, String newValue) {
        File jsonFile = new File(filePath + ".json");
        if (!jsonFile.exists()) {
            log("JSON file does not exist: ", LogLevel.ERROR, filePath);
            return;
        }

        log("Updating nested JSON key in file: ", LogLevel.INFO_BLUE, filePath);

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject current = jsonObject;

            for (int i = 0; i < keys.size() - 1; i++) {
                String key = keys.get(i);
                if (current.has(key) && current.get(key).isJsonObject()) {
                    current = current.getAsJsonObject(key);
                } else {
                    log("Key not found or invalid structure: " + key, LogLevel.ERROR, filePath);
                    return;
                }
            }

            String finalKey = keys.get(keys.size() - 1);
            current.addProperty(finalKey, newValue);

            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject));
                log("Successfully updated nested key in JSON file: ", LogLevel.INFO_GREEN, filePath);
            }

        } catch (IOException | JsonSyntaxException e) {
            log("Failed to update nested JSON key.", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }
}
