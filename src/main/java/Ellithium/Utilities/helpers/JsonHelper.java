package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import static Ellithium.core.reporting.Reporter.log;

import Ellithium.core.reporting.Reporter;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class JsonHelper {

    // Method to read JSON data and return it as a list of maps
    public static List<Map<String, String>> getJsonData(String filePath) {
        List<Map<String, String>> data = new ArrayList<>();
        File jsonFile = new File(filePath);

        if (!jsonFile.exists()) {
            log("JSON file does not exist: ", LogLevel.ERROR, filePath);
            return data;
        }

        log("Attempting to read JSON data from file: ", LogLevel.INFO_BLUE, filePath);

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);

            if (jsonElement.isJsonArray()) {
                // Handle JSON array
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                for (JsonElement element : jsonArray) {
                    if (element.isJsonObject()) {
                        data.add(convertJsonObjectToMap(element.getAsJsonObject()));
                    }
                }
            } else if (jsonElement.isJsonObject()) {
                // Handle single JSON object
                data.add(convertJsonObjectToMap(jsonElement.getAsJsonObject()));
            } else {
                Reporter.log("JSON data in file is neither an array nor an object", LogLevel.ERROR, filePath);
            }

            log("Successfully read JSON file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException | JsonSyntaxException e) {
            log("Failed to read JSON file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
        return data;
    }

    private static Map<String, String> convertJsonObjectToMap(JsonObject jsonObject) {
        Map<String, String> recordMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                recordMap.put(entry.getKey(), value.getAsString());
            } else {
                recordMap.put(entry.getKey(), value.toString());
            }
        }
        return recordMap;
    }

    // Method to write a list of maps as JSON data
    public static void setJsonData(String filePath, List<Map<String, String>> data) {
        log("Attempting to write JSON data to file: ", LogLevel.INFO_BLUE, filePath);
        File jsonFile = new File(filePath);

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
        File jsonFile = new File(filePath );
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
        File jsonFile = new File(filePath);

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
        File jsonFile = new File(filePath);

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
        File jsonFile = new File(filePath );
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
        File file1 = new File(filePath1 );
        File file2 = new File(filePath2 );

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
        File file1 = new File(sourceFilePath1 );
        File file2 = new File(sourceFilePath2 );
        File targetFile = new File(targetFilePath);

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
        File jsonFile = new File(filePath );
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
    public static void appendToJsonArray(String filePath, List<String> arrayPath, String value) {
        log("Appending to JSON array at path: " + String.join(".", arrayPath), LogLevel.INFO_BLUE, filePath);
        modifyJsonStructure(filePath, (jsonObject) -> {
            JsonArray targetArray = navigateToArray(jsonObject, arrayPath);
            if(targetArray != null) {
                targetArray.add(value);
                return true;
            }
            return false;
        });
    }

    public static void insertIntoJsonArray(String filePath, List<String> arrayPath, int index, String value) {
        log("Inserting into JSON array at path: " + String.join(".", arrayPath), LogLevel.INFO_BLUE, filePath);
        modifyJsonStructure(filePath, (jsonObject) -> {
            JsonArray targetArray = navigateToArray(jsonObject, arrayPath);
            if (targetArray != null && index >= 0 && index <= targetArray.size()) {
                JsonArray newArray = new JsonArray();
                for (int i = 0; i < targetArray.size(); i++) {
                    if (i == index) {
                        newArray.add(new JsonPrimitive(value));
                    }
                    newArray.add(targetArray.get(i));
                }
                if (index == targetArray.size()) {
                    newArray.add(new JsonPrimitive(value));
                }
                // Replace the original array with the new one
                jsonObject.add(arrayPath.get(arrayPath.size() - 1), newArray);
                return true;
            }
            return false;
        });
    }

    public static void removeFromJsonArray(String filePath, List<String> arrayPath, int index) {
        log("Removing from JSON array at path: " + String.join(".", arrayPath), LogLevel.INFO_BLUE, filePath);
        modifyJsonStructure(filePath, (jsonObject) -> {
            JsonArray targetArray = navigateToArray(jsonObject, arrayPath);
            if(targetArray != null && index >= 0 && index < targetArray.size()) {
                targetArray.remove(index);
                return true;
            }
            return false;
        });
    }

    public static String getValueFromNestedPath(String filePath, List<String> pathKeys) {
        log("Reading from nested path: " + String.join(".", pathKeys), LogLevel.INFO_BLUE, filePath);
        File jsonFile = new File(filePath);

        if (!jsonFile.exists()) {
            log("JSON file does not exist: ", LogLevel.ERROR, filePath);
            return null;
        }

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonElement currentElement = JsonParser.parseReader(reader);

            for(String key : pathKeys) {
                if(currentElement.isJsonObject()) {
                    JsonObject obj = currentElement.getAsJsonObject();
                    currentElement = obj.has(key) ? obj.get(key) : null;
                } else if(currentElement.isJsonArray()) {
                    try {
                        int index = Integer.parseInt(key);
                        JsonArray array = currentElement.getAsJsonArray();
                        currentElement = index < array.size() ? array.get(index) : null;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                if(currentElement == null) break;
            }

            if(currentElement != null && currentElement.isJsonPrimitive()) {
                log("Successfully read nested value", LogLevel.INFO_GREEN, filePath);
                return currentElement.getAsString();
            }
            return null;

        } catch (Exception e) {
            log("Failed to read nested path", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return null;
        }
    }

    public static void modifyInNestedPath(String filePath, List<String> pathKeys, String newValue) {
        log("Modifying nested path: " + String.join(".", pathKeys), LogLevel.INFO_BLUE, filePath);
        modifyJsonStructure(filePath, (jsonObject) -> {
            JsonElement currentElement = jsonObject;
            JsonObject parentObject = null;
            String finalKey = null;

            for(String key : pathKeys) {
                parentObject = currentElement.isJsonObject() ? currentElement.getAsJsonObject() : null;
                if(parentObject == null) return false;

                if(parentObject.has(key)) {
                    currentElement = parentObject.get(key);
                    finalKey = key;
                } else {
                    return false;
                }
            }

            if(parentObject != null && finalKey != null) {
                parentObject.addProperty(finalKey, newValue);
                return true;
            }
            return false;
        });
    }

    // Generic modification helper
    private static void modifyJsonStructure(String filePath, java.util.function.Function<JsonObject, Boolean> modifier) {
        File jsonFile = new File(filePath);

        if (!jsonFile.exists()) {
            log("JSON file does not exist: ", LogLevel.ERROR, filePath);
            return;
        }

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            if(modifier.apply(jsonObject)) {
                try (FileWriter writer = new FileWriter(jsonFile)) {
                    writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject));
                    log("Successfully modified JSON structure", LogLevel.INFO_GREEN, filePath);
                }
            } else {
                log("Failed to locate target structure", LogLevel.ERROR, filePath);
            }

        } catch (Exception e) {
            log("Failed to modify JSON structure", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

    // Array navigation helper
    private static JsonArray navigateToArray(JsonObject root, List<String> path) {
        JsonElement current = root;
        for(int i = 0; i < path.size(); i++) {
            if(current.isJsonObject()) {
                JsonObject obj = current.getAsJsonObject();
                if(!obj.has(path.get(i))) return null;
                current = obj.get(path.get(i));
            }
            // Handle array indices if needed
            if(i == path.size()-1 && !current.isJsonArray()) return null;
        }
        return current.getAsJsonArray();
    }

    public static void createArrayAtPath(String filePath, List<String> arrayPath) {
        log("Creating array at path: " + String.join(".", arrayPath), LogLevel.INFO_BLUE, filePath);
        modifyJsonStructure(filePath, (jsonObject) -> {
            JsonObject current = jsonObject;
            for(int i = 0; i < arrayPath.size(); i++) {
                String key = arrayPath.get(i);
                if(i == arrayPath.size()-1) {
                    current.add(key, new JsonArray());
                    return true;
                }
                if(!current.has(key)) {
                    current.add(key, new JsonObject());
                }
                current = current.getAsJsonObject(key);
            }
            return false;
        });
    }

    public static boolean arrayContainsValue(String filePath, List<String> arrayPath, String value) {
        log("Checking array contains value in path: " + String.join(".", arrayPath), LogLevel.INFO_BLUE, filePath);
        File jsonFile = new File(filePath);

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonArray targetArray = navigateToArray(JsonParser.parseReader(reader).getAsJsonObject(), arrayPath);
            if(targetArray != null) {
                for(JsonElement element : targetArray) {
                    if(element.isJsonPrimitive() && element.getAsString().equals(value)) {
                        log("Value found in array", LogLevel.INFO_GREEN, filePath);
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log("Failed to check array contents", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return false;
        }
    }

    public static <T> T parseJsonToObject(String filePath, Class<T> classOfT) {
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            return new Gson().fromJson(jsonContent, classOfT);
        } catch (IOException e) {
            log("Failed to parse JSON to object", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            return null;
        }
    }

    public static <T> List<T> parseJsonToList(String filePath, Class<T> classOfT) {
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            Type listType = TypeToken.getParameterized(List.class, classOfT).getType();
            return new Gson().fromJson(jsonContent, listType);
        } catch (IOException e) {
            log("Failed to parse JSON to list", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            return new ArrayList<>();
        }
    }

    public static boolean isValidJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            JsonParser.parseReader(reader);
            return true;
        } catch (JsonSyntaxException | IOException e) {
            log("Invalid JSON format", LogLevel.ERROR, filePath);
            return false;
        }
    }

    public static void prettyPrintJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            String prettyJson = new GsonBuilder().setPrettyPrinting().create().toJson(jsonElement);
            Files.write(Paths.get(filePath), prettyJson.getBytes(StandardCharsets.UTF_8));
            log("Successfully formatted JSON file", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to format JSON file", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    public static Map<String, Integer> getJsonKeyOccurrences(String filePath) {
        Map<String, Integer> keyOccurrences = new HashMap<>();
        try (FileReader reader = new FileReader(filePath)) {
            countKeysRecursively(JsonParser.parseReader(reader), keyOccurrences);
            log("Successfully counted key occurrences", LogLevel.INFO_GREEN, filePath);
            return keyOccurrences;
        } catch (IOException e) {
            log("Failed to count key occurrences", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            return keyOccurrences;
        }
    }

    private static void countKeysRecursively(JsonElement element, Map<String, Integer> keyOccurrences) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                keyOccurrences.merge(entry.getKey(), 1, Integer::sum);
                countKeysRecursively(entry.getValue(), keyOccurrences);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement arrayElement : array) {
                countKeysRecursively(arrayElement, keyOccurrences);
            }
        }
    }

    public static void removeNullValues(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonElement cleaned = removeNullValuesRecursively(element);
            try (FileWriter writer = new FileWriter(filePath)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(cleaned, writer);
                log("Successfully removed null values", LogLevel.INFO_GREEN, filePath);
            }
        } catch (IOException e) {
            log("Failed to remove null values", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    private static JsonElement removeNullValuesRecursively(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonObject cleaned = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (!entry.getValue().isJsonNull()) {
                    cleaned.add(entry.getKey(), removeNullValuesRecursively(entry.getValue()));
                }
            }
            return cleaned;
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            JsonArray cleaned = new JsonArray();
            for (JsonElement arrayElement : array) {
                if (!arrayElement.isJsonNull()) {
                    cleaned.add(removeNullValuesRecursively(arrayElement));
                }
            }
            return cleaned;
        }
        return element;
    }

    public static String backupJsonFile(String filePath) {
        try {
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                log("Source file does not exist for backup: ", LogLevel.ERROR, filePath);
                return null;
            }

            String backupPath = filePath + ".backup-" + System.currentTimeMillis();
            File backupFile = new File(backupPath);
            
            // Ensure parent directory exists
            File parentDir = backupFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Copy the file and ensure it's exactly the same
            Files.copy(sourceFile.toPath(), backupFile.toPath());

            // Verify backup was created and content matches
            if (backupFile.exists() && compareJsonFiles(filePath, backupPath)) {
                log("Successfully created backup at: ", LogLevel.INFO_GREEN, backupPath);
                return backupPath;
            } else {
                log("Failed to verify backup creation or content at: ", LogLevel.ERROR, backupPath);
                return null;
            }
        } catch (IOException e) {
            log("Failed to create backup", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            return null;
        }
    }
}
