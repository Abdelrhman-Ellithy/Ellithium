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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class Environment {
    private final String name;
    private final String envFilePath;
    private static final String ENV_DIR = "src" + File.separator + "test" + File.separator + "resources"
            + File.separator + "TestData" + File.separator + "Environments";
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 100;

    /**
     * @param name The name of the environment to create or load
     */
    public Environment(String name) {
        this.name = name;
        File envDir = new File(ENV_DIR);
        if (!envDir.exists()) {
            envDir.mkdirs();
        }
        this.envFilePath = ENV_DIR + File.separator + name + ".json";
    }

    @FunctionalInterface
    private interface FileLockAction<T> {
        T run() throws Exception;
    }

    /**
     * Sets a value in the environment
     * @param key The variable name
     * @param value The value to set (supports any object type)
     */
    public void set(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            log("Key cannot be null or empty", LogLevel.ERROR);
            return;
        }
        File envFile = new File(envFilePath);
        if (!envFile.getParentFile().exists()) {
            envFile.getParentFile().mkdirs();
        }
        JsonHelper.setJsonKeyValue(envFilePath, key, value);
        log("Variable set successfully - Key: " + key, LogLevel.INFO_GREEN);
    }

    /**
     * Gets a value with type conversion
     * @param key The variable name
     * @param type The expected return type
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        String value = get(key);
        if (value == null) return null;
        try {
            if (type == String.class) {
                return (T) value;
            } else if (type == Integer.class) {
                return (T) Integer.valueOf(value);
            } else if (type == Double.class) {
                return (T) Double.valueOf(value);
            } else if (type == Boolean.class) {
                value = value.toLowerCase().trim();
                boolean boolValue = switch (value) {
                    case "true", "yes", "1", "on", "enabled" -> true;
                    case "false", "no", "0", "off", "disabled" -> false;
                    default -> Boolean.parseBoolean(value);
                };
                return (T) Boolean.valueOf(boolValue);
            }
        } catch (Exception e) {
            log("Failed to convert value for key: " + key + " to type: " + type.getSimpleName(), LogLevel.ERROR);
        }
        return null;
    }

    /**
     * Gets a value as string
     * @param key The variable name
     */
    public String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            log("Key cannot be null or empty", LogLevel.ERROR);
            return null;
        }
        String fileValue =  JsonHelper.getJsonKeyValue(envFilePath, key);
        if (fileValue != null) {
            log("Retrieved value from file for key: " + key, LogLevel.INFO_BLUE);
            return fileValue;
        }
        log("No value found for key: " + key, LogLevel.INFO_RED);
        return null;
    }

    /**
     * Sets an integer value
     */
    public void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    /**
     * Gets a value as integer
     */
    public int getAsInteger(String key) {
        Integer value = get(key, Integer.class);
        return value != null ? value : 0;
    }

    /**
     * Sets a double value
     */
    public void set(String key, double value) {
        set(key, String.valueOf(value));
    }

    /**
     * Gets a value as double
     */
    public double getAsDouble(String key) {
        Double value = get(key, Double.class);
        return value != null ? value : 0.0;
    }

    /**
     * Sets a boolean value
     */
    public void set(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    /**
     * Gets a value as boolean
     */
    public boolean getAsBoolean(String key) {
        Boolean value = get(key, Boolean.class);
        return value != null ? value : false;
    }

    /**
     * Checks if key exists
     */
    public boolean has(String key) {
        return JsonHelper.getJsonKeyValue(envFilePath, key) != null;
    }

    /**
     * Removes a value
     */
    public void remove(String key) {
        if (key == null || key.trim().isEmpty()) {
            log("Key cannot be null or empty", LogLevel.ERROR);
            return;
        }
        File envFile = new File(envFilePath);
            Map<String, Object> currentVars = JsonHelper.getNestedJsonData(envFilePath);
            if (currentVars != null) {
                currentVars.remove(key);
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
     * Clears all values
     */
    public void clear() {
        File envFile = new File(envFilePath);
        List<Map<String, String>> emptyList = new ArrayList<>();
            JsonHelper.setJsonData(envFilePath, emptyList);
        log("All variables cleared from environment: " + name, LogLevel.INFO_GREEN);
    }

    /**
     * Gets all keys
     */
    public List<String> keys() {
        File envFile = new File(envFilePath);
        Map<String, Object> fileVars = JsonHelper.getNestedJsonData(envFilePath);
        Set<String> allKeys = new HashSet<>();
        if (fileVars != null) {
            allKeys.addAll(fileVars.keySet());
        }
        return new ArrayList<>(allKeys);
    }

    /**
     * Gets environment name
     */
    public String name() {
        return name;
    }

    /**
     * Gets environment file path
     */
    public String path() {
        return envFilePath;
    }
}