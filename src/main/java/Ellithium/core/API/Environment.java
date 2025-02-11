package Ellithium.core.API;

import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.core.logging.LogLevel;
import static Ellithium.core.reporting.Reporter.log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
public class Environment {
    private final String name;
    private final String envFilePath;
    private final Map<String, String> variableCache;
    private static final String ENV_DIR = "src"+File.separator+"test"+File.separator+"resources"
                                        +File.separator+"TestData"+File.separator+"Environments";

    /**
     * @param name The name of the environment to create or load
     */
    public Environment(String name)  {
        this.name = name;
        this.variableCache = new HashMap<>();
        File envDir = new File(ENV_DIR);
        if (!envDir.exists()) {
            envDir.mkdirs();
        }
        this.envFilePath = ENV_DIR + File.separator + name + ".json";
    }

    /**
     * @param key The key of the variable to set
     * @param value The value to associate with the key
     */
    public void setVariable(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            log("Key cannot be null or empty", LogLevel.ERROR);
            return;
        }

        String existingValue = JsonHelper.getJsonKeyValue(envFilePath, key);
        if (existingValue != null && !existingValue.equals(value)) {
            log("Updating existing value for key: " + key, LogLevel.INFO_BLUE);
        }

        variableCache.put(key, value);
        JsonHelper.setJsonKeyValue(envFilePath, key, value);
        log("Variable set successfully - Key: " + key + ", Value: " + value, LogLevel.INFO_GREEN);
    }

    /**
     * @param key The key of the variable to retrieve
     * @return The value associated with the key, or null if not found
     */
    public String getVariable(String key) {
        if (key == null || key.trim().isEmpty()) {
            log("Key cannot be null or empty", LogLevel.ERROR);
            return null;
        }

        String cachedValue = variableCache.get(key);
        if (cachedValue != null) {
            log("Retrieved value from cache for key: " + key, LogLevel.INFO_BLUE);
            return cachedValue;
        }

        String fileValue = JsonHelper.getJsonKeyValue(envFilePath, key);
        if (fileValue != null) {
            variableCache.put(key, fileValue);
            log("Retrieved value from file for key: " + key, LogLevel.INFO_BLUE);
            return fileValue;
        }

        log("No value found for key: " + key, LogLevel.INFO_RED);
        return null;
    }

    /**
     * @param key The key to check for existence
     * @return true if the variable exists, false otherwise
     */
    public boolean hasVariable(String key) {
        return variableCache.containsKey(key) || 
               JsonHelper.getJsonKeyValue(envFilePath, key) != null;
    }

    /**
     * @param key The key of the variable to remove
     */
    public void removeVariable(String key) {
        if (key == null || key.trim().isEmpty()) {
            log("Key cannot be null or empty", LogLevel.ERROR);
            return;
        }

        variableCache.remove(key);
        Map<String, Object> currentVars = JsonHelper.getNestedJsonData(envFilePath);
        if (currentVars != null) {
            currentVars.remove(key);
            // Convert Map<String, Object> to Map<String, String>
            Map<String, String> stringVars = new HashMap<>();
            for (Map.Entry<String, Object> entry : currentVars.entrySet()) {
                stringVars.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            List<Map<String, String>> varsList = new ArrayList<>();
            varsList.add(stringVars);
            JsonHelper.setJsonData(envFilePath, varsList);
            log("Variable removed successfully: " + key, LogLevel.INFO_GREEN);
        }
    }

    /**
     * Clears all variables from both cache and file
     */
    public void clearAllVariables() {
        variableCache.clear();
        List<Map<String, String>> emptyList = new ArrayList<>();
        JsonHelper.setJsonData(envFilePath, emptyList);
        log("All variables cleared from environment: " + name, LogLevel.INFO_GREEN);
    }

    /**
     * @return List of all variable names in both cache and file
     */
    public List<String> getAllVariableNames() {
        Set<String> allKeys = new HashSet<>(variableCache.keySet());
        
        Map<String, Object> fileVars = JsonHelper.getNestedJsonData(envFilePath);
        if (fileVars != null) {
            allKeys.addAll(fileVars.keySet());
        }
        
        return new ArrayList<>(allKeys);
    }

    /**
     * Refreshes the cache with current file contents
     */
    public void refreshCache() {
        variableCache.clear();
        Map<String, Object> fileVars = JsonHelper.getNestedJsonData(envFilePath);
        if (fileVars != null) {
            for (Map.Entry<String, Object> entry : fileVars.entrySet()) {
                variableCache.put(entry.getKey(), entry.getValue().toString());
            }
        }
        log("Cache refreshed for environment: " + name, LogLevel.INFO_GREEN);
    }

    /**
     * @return The name of the environment
     */
    public String getName() {
        return name;
    }

    /**
     * @return The file path of the environment configuration
     */
    public String getFilePath() {
        return envFilePath;
    }
}