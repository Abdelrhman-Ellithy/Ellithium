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

    private static void ensureFileExists(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            file.createNewFile();
        }
    }

    private static Properties loadProperties(String filePath) throws IOException {
        Properties prop = new Properties();
        ensureFileExists(filePath);
        try (FileInputStream fis = new FileInputStream(filePath)) {
            prop.load(fis);
        }
        return prop;
    }

    private static void saveProperties(String filePath, Properties prop) throws IOException {
        ensureFileExists(filePath);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            prop.store(fos, null);
        }
    }

    public static String getDataFromProperties(String filePath, String key) {
        try {
            Properties prop = loadProperties(filePath);
            return prop.getProperty(key);
        } catch (IOException e) {
            log("Error accessing properties file: ", LogLevel.ERROR, e.getMessage());
            return null;
        }
    }

    public static void setDataToProperties(String filePath, String key, String value) {
        try {
            Properties prop = loadProperties(filePath);
            prop.setProperty(key, value);
            saveProperties(filePath, prop);
            log("Successfully updated property [" + key + "] in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to update property file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    public static Properties getAllProperties(String filePath) {
        try {
            return loadProperties(filePath);
        } catch (IOException e) {
            log("Failed to load properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
            return new Properties();
        }
    }

    public static void removeKeyFromProperties(String filePath, String key) {
        try {
            Properties prop = loadProperties(filePath);
            if (prop.containsKey(key)) {
                prop.remove(key);
                saveProperties(filePath, prop);
                log("Successfully removed key: " + key + " from properties file: ", LogLevel.INFO_GREEN, filePath);
            } else {
                log("Key not found in properties file: ", LogLevel.WARN, key);
            }
        } catch (IOException e) {
            log("Failed to remove key from properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    public static boolean keyExists(String filePath, String key) {
        try {
            Properties prop = loadProperties(filePath);
            boolean exists = prop.containsKey(key);
            log("Checked key existence: " + key + " in file: ", LogLevel.INFO_GREEN, filePath);
            return exists;
        } catch (IOException e) {
            log("Failed to check key in properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
            return false;
        }
    }

    public static String getOrDefault(String filePath, String key, String defaultValue) {
        try {
            Properties prop = loadProperties(filePath);
            String value = prop.getProperty(key, defaultValue);
            log("Fetched key: " + key + " with value (or default): " + value + " from file: ", LogLevel.INFO_GREEN, filePath);
            return value;
        } catch (IOException e) {
            log("Failed to fetch key from properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
            return defaultValue;
        }
    }

    public static void updateMultipleProperties(String filePath, Properties properties) {
        try {
            Properties prop = loadProperties(filePath);
            properties.forEach((key, value) -> prop.setProperty(key.toString(), value.toString()));
            saveProperties(filePath, prop);
            log("Successfully updated multiple properties in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to update multiple properties in file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    public static void addOrUpdatePropertiesFromMap(String filePath, Map<String, String> map) {
        try {
            Properties prop = loadProperties(filePath);
            map.forEach(prop::setProperty);
            saveProperties(filePath, prop);
            log("Successfully added or updated properties from Map in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to add or update properties in file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    public static Map<String, String> getPropertiesAsMap(String filePath) {
        Map<String, String> map = new HashMap<>();
        try {
            Properties prop = loadProperties(filePath);
            prop.forEach((key, value) -> map.put(key.toString(), value.toString()));
            log("Successfully retrieved properties as Map from file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to retrieve properties as Map from file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
        return map;
    }

    public static void updatePropertiesFromMap(String filePath, Map<String, String> updates) {
        try {
            Properties prop = loadProperties(filePath);
            updates.forEach((key, value) -> {
                if (prop.containsKey(key)) {
                    prop.setProperty(key, value);
                }
            });
            saveProperties(filePath, prop);
            log("Successfully updated properties from Map in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to update properties in file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    public static void addNewPropertiesFromMap(String filePath, Map<String, String> newProperties) {
        try {
            Properties prop = loadProperties(filePath);
            newProperties.forEach((key, value) -> {
                if (!prop.containsKey(key)) {
                    prop.setProperty(key, value);
                }
            });
            saveProperties(filePath, prop);
            log("Successfully added new properties from Map in file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to add new properties in file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

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

    public static Map<String, String[]> compareProperties(String file1Path, String file2Path) {
        Map<String, String[]> differences = new HashMap<>();
        Properties prop1 = getAllProperties(file1Path);
        Properties prop2 = getAllProperties(file2Path);

        for (String key : prop1.stringPropertyNames()) {
            if (!prop2.containsKey(key)) {
                differences.put(key, new String[]{prop1.getProperty(key), null});
            } else if (!prop1.getProperty(key).equals(prop2.getProperty(key))) {
                differences.put(key, new String[]{prop1.getProperty(key), prop2.getProperty(key)});
            }
        }

        for (String key : prop2.stringPropertyNames()) {
            if (!prop1.containsKey(key)) {
                differences.put(key, new String[]{null, prop2.getProperty(key)});
            }
        }

        return differences;
    }

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

    public static int getPropertyCount(String filePath) {
        Properties prop = getAllProperties(filePath);
        return prop.size();
    }

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

    public static boolean validatePropertyValue(String filePath, String key, String regex) {
        String value = getDataFromProperties(filePath, key);
        return value != null && value.matches(regex);
    }

    public static void sortPropertiesByKey(String filePath) {
        try {
            // Load original properties
            Properties originalProp = loadProperties(filePath);
            
            // Create sorted map to maintain order
            TreeMap<String, String> sortedMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            
            // Transfer properties to sorted map
            for (String key : originalProp.stringPropertyNames()) {
                sortedMap.put(key, originalProp.getProperty(key));
            }
            
            // Create new properties and maintain order
            Properties sortedProp = new Properties() {
                private static final long serialVersionUID = 1L;
                
                @Override
                public synchronized Enumeration<Object> keys() {
                    return Collections.enumeration(new ArrayList<>(sortedMap.keySet()));
                }
                
                @Override
                public Set<Object> keySet() {
                    return new LinkedHashSet<>(sortedMap.keySet());
                }
                
                @Override
                public Set<String> stringPropertyNames() {
                    return new LinkedHashSet<>(sortedMap.keySet());
                }
            };
            
            // Add properties in sorted order
            sortedMap.forEach(sortedProp::setProperty);
            
            // Save properties using a different approach to maintain order
            try (Writer writer = new FileWriter(filePath)) {
                sortedMap.forEach((key, value) -> {
                    try {
                        writer.write(key + "=" + value + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            
            log("Successfully sorted properties by key: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to sort properties: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

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