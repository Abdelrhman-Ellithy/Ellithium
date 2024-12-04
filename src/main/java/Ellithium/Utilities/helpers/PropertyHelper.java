package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import static Ellithium.core.reporting.Reporter.log;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyHelper {
    public static String getDataFromProperties(String filePath, String key) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(filePath + ".properties"));
            log("Successfully loaded properties file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to load properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
        }
        return prop.getProperty(key);
    }
    // Method to set data into a properties file with a key-value pair
    public static void setDataToProperties(String filePath, String key, String value) {
        Properties prop = new Properties();
        try (FileOutputStream out = new FileOutputStream(filePath + ".properties")) {
            prop.setProperty(key, value);
            prop.store(out, null);
            log("Successfully updated properties file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to write properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
        }
    }
    // Method to retrieve all key-value pairs from a properties file
    public static Properties getAllProperties(String filePath) {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath + ".properties")) {
            prop.load(input);
            log("Successfully loaded all properties from file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to load properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
        return prop;
    }
    // Method to remove a specific key from a properties file
    public static void removeKeyFromProperties(String filePath, String key) {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath + ".properties");
             FileOutputStream output = new FileOutputStream(filePath + ".properties")) {

            prop.load(input);
            if (prop.containsKey(key)) {
                prop.remove(key);
                prop.store(output, null);
                log("Successfully removed key: " + key + " from properties file: ", LogLevel.INFO_GREEN, filePath);
            } else {
                log("Key not found in properties file: ", LogLevel.WARN, key);
            }

        } catch (IOException e) {
            log("Failed to remove key from properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
    // Method to check if a specific key exists in a properties file
    public static boolean keyExists(String filePath, String key) {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath + ".properties")) {
            prop.load(input);
            boolean exists = prop.containsKey(key);
            log("Checked key existence: " + key + " in file: ", LogLevel.INFO_GREEN, filePath);
            return exists;
        } catch (IOException e) {
            log("Failed to check key in properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
        return false;
    }
    // Method to retrieve a key's value or return a default value if the key is not found
    public static String getOrDefault(String filePath, String key, String defaultValue) {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath + ".properties")) {
            prop.load(input);
            String value = prop.getProperty(key, defaultValue);
            log("Fetched key: " + key + " with value (or default): " + value + " from file: ", LogLevel.INFO_GREEN, filePath);
            return value;
        } catch (IOException e) {
            log("Failed to fetch key from properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
        return defaultValue;
    }
    // Method to update multiple properties at once
    public static void updateMultipleProperties(String filePath, Properties properties) {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath + ".properties");
             FileOutputStream output = new FileOutputStream(filePath + ".properties")) {

            prop.load(input);
            properties.forEach((key, value) -> prop.setProperty(key.toString(), value.toString()));
            prop.store(output, null);

            log("Successfully updated multiple properties in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to update multiple properties in file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
    // Method to add or update properties from a Map
    public static void addOrUpdatePropertiesFromMap(String filePath, Map<String, String> map) {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath + ".properties");
             FileOutputStream output = new FileOutputStream(filePath + ".properties")) {

            prop.load(input); // Load existing properties
            map.forEach(prop::setProperty); // Add or update properties
            prop.store(output, null); // Save updated properties

            log("Successfully added or updated properties from Map in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to add or update properties in file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
    // Method to return properties file as a Map
    public static Map<String, String> getPropertiesAsMap(String filePath) {
        Properties prop = new Properties();
        Map<String, String> map = new HashMap<>();

        try (FileInputStream input = new FileInputStream(filePath + ".properties")) {
            prop.load(input); // Load properties file
            prop.forEach((key, value) -> map.put(key.toString(), value.toString())); // Convert to Map

            log("Successfully retrieved properties as Map from file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to retrieve properties as Map from file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
        return map;
    }
    // Method to update specific properties in a Map
    public static void updatePropertiesFromMap(String filePath, Map<String, String> updates) {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath + ".properties");
             FileOutputStream output = new FileOutputStream(filePath + ".properties")) {

            prop.load(input); // Load existing properties
            updates.forEach((key, value) -> {
                if (prop.containsKey(key)) {
                    prop.setProperty(key, value); // Update only if the key exists
                }
            });
            prop.store(output, null); // Save updated properties

            log("Successfully updated properties from Map in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to update properties in file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
    // Method to add new properties from a Map
    public static void addNewPropertiesFromMap(String filePath, Map<String, String> newProperties) {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(filePath + ".properties");
             FileOutputStream output = new FileOutputStream(filePath + ".properties")) {

            prop.load(input); // Load existing properties
            newProperties.forEach((key, value) -> {
                if (!prop.containsKey(key)) {
                    prop.setProperty(key, value); // Add only if the key does not exist
                }
            });
            prop.store(output, null); // Save updated properties

            log("Successfully added new properties from Map in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to add new properties in file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
}