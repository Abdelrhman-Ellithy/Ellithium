package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import static Ellithium.core.reporting.Reporter.log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class PropertyHelper {
    public static String getDataFromProperties(String filePath, String key) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(filePath ));
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
        try (FileOutputStream out = new FileOutputStream(filePath )) {
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
        try (FileInputStream input = new FileInputStream(filePath )) {
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
        try (FileInputStream input = new FileInputStream(filePath );
             FileOutputStream output = new FileOutputStream(filePath )) {

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
        try (FileInputStream input = new FileInputStream(filePath )) {
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
        try (FileInputStream input = new FileInputStream(filePath )) {
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
        try (FileInputStream input = new FileInputStream(filePath );
             FileOutputStream output = new FileOutputStream(filePath )) {

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
        try (FileInputStream input = new FileInputStream(filePath );
             FileOutputStream output = new FileOutputStream(filePath )) {

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

        try (FileInputStream input = new FileInputStream(filePath )) {
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
        try (FileInputStream input = new FileInputStream(filePath );
             FileOutputStream output = new FileOutputStream(filePath )) {

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
        try (FileInputStream input = new FileInputStream(filePath );
             FileOutputStream output = new FileOutputStream(filePath )) {

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

    // Backup functionality
    public static void backupProperties(String filePath) {
        try {
            Path source = Path.of(filePath);
            Path backup = Path.of(filePath + ".backup");
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            log("Created backup of properties file: ", LogLevel.INFO_GREEN, backup.toString());
        } catch (IOException e) {
            log("Failed to create backup: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Restore from backup
    public static void restoreFromBackup(String filePath) {
        try {
            Path backup = Path.of(filePath + ".backup");
            Path target = Path.of(filePath);
            if (Files.exists(backup)) {
                Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                log("Restored properties from backup: ", LogLevel.INFO_GREEN, filePath);
            } else {
                log("Backup file not found: ", LogLevel.ERROR, backup.toString());
            }
        } catch (IOException e) {
            log("Failed to restore from backup: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Load properties with specific encoding
    public static Properties loadWithEncoding(String filePath, String encoding) {
        Properties prop = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(filePath), encoding)) {
            prop.load(reader);
            log("Successfully loaded properties with encoding " + encoding + ": ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to load properties with encoding: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
        return prop;
    }

    // Save properties with specific encoding
    public static void saveWithEncoding(String filePath, Properties prop, String encoding) {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(filePath), encoding)) {
            prop.store(writer, null);
            log("Successfully saved properties with encoding " + encoding + ": ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to save properties with encoding: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Compare two properties files
    public static Map<String, String[]> compareProperties(String file1Path, String file2Path) {
        Map<String, String[]> differences = new HashMap<>();
        Properties prop1 = getAllProperties(file1Path);
        Properties prop2 = getAllProperties(file2Path);

        // Check all keys in first file
        for (String key : prop1.stringPropertyNames()) {
            if (!prop2.containsKey(key)) {
                differences.put(key, new String[]{prop1.getProperty(key), null});
            } else if (!prop1.getProperty(key).equals(prop2.getProperty(key))) {
                differences.put(key, new String[]{prop1.getProperty(key), prop2.getProperty(key)});
            }
        }

        // Check for keys only in second file
        for (String key : prop2.stringPropertyNames()) {
            if (!prop1.containsKey(key)) {
                differences.put(key, new String[]{null, prop2.getProperty(key)});
            }
        }

        return differences;
    }

    // Get all keys matching a pattern
    public static Set<String> getKeysMatchingPattern(String filePath, String pattern) {
        Properties prop = getAllProperties(filePath);
        Set<String> matchingKeys = new HashSet<>();
        
        for (String key : prop.stringPropertyNames()) {
            if (key.matches(pattern)) {
                matchingKeys.add(key);
            }
        }
        
        return matchingKeys;
    }

    // Clear all properties
    public static void clearProperties(String filePath) {
        try (FileOutputStream output = new FileOutputStream(filePath)) {
            Properties prop = new Properties();
            prop.store(output, null);
            log("Successfully cleared all properties: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to clear properties: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Count properties
    public static int getPropertyCount(String filePath) {
        Properties prop = getAllProperties(filePath);
        return prop.size();
    }

    // Get all property values containing a specific string
    public static List<String> findValuesContaining(String filePath, String searchStr) {
        Properties prop = getAllProperties(filePath);
        List<String> matchingValues = new ArrayList<>();
        
        prop.forEach((key, value) -> {
            if (value.toString().contains(searchStr)) {
                matchingValues.add(value.toString());
            }
        });
        
        return matchingValues;
    }

    // Validate property value format
    public static boolean validatePropertyValue(String filePath, String key, String regex) {
        String value = getDataFromProperties(filePath, key);
        return value != null && value.matches(regex);
    }

    // Sort properties by key
    public static void sortPropertiesByKey(String filePath) {
        Properties prop = getAllProperties(filePath);
        Properties sortedProp = new Properties() {
            @Override
            public Set<Object> keySet() {
                return Collections.unmodifiableSet(new TreeSet<>(super.keySet()));
            }
        };
        
        prop.forEach(sortedProp::put);
        
        try (FileOutputStream output = new FileOutputStream(filePath)) {
            sortedProp.store(output, null);
            log("Successfully sorted properties by key: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to sort properties: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Get properties with values matching a specific pattern
    public static Map<String, String> getPropertiesMatchingValuePattern(String filePath, String pattern) {
        Properties prop = getAllProperties(filePath);
        Map<String, String> matching = new HashMap<>();
        
        prop.forEach((key, value) -> {
            if (value.toString().matches(pattern)) {
                matching.put(key.toString(), value.toString());
            }
        });
        
        return matching;
    }
}