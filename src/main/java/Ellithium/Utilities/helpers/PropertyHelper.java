package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import static Ellithium.core.reporting.Reporter.log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to handle reading, writing, and updating properties files while preserving key order.
 */
public class PropertyHelper {

    private static final ConcurrentHashMap<String, Object> fileLocks = new ConcurrentHashMap<>();
    private static Object getFileLock(String filePath) {
        return fileLocks.computeIfAbsent(filePath, k -> new Object());
    }

    private static final Object LOCK = new Object();

    private static class LinkedProperties extends Properties {
        @Serial
        private static final long serialVersionUID = 1L;
        private final LinkedHashSet<Object> keys = new LinkedHashSet<>();

        @Override
        public synchronized Enumeration<Object> keys() {
            return Collections.enumeration(keys);
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            keys.add(key);
            return super.put(key, value);
        }

        @Override
        public synchronized Object remove(Object key) {
            keys.remove(key);
            return super.remove(key);
        }

        @Override
        public synchronized void clear() {
            keys.clear();
            super.clear();
        }

        @Override
        public Set<Object> keySet() {
            return new LinkedHashSet<>(keys);
        }

        @Override
        public Set<String> stringPropertyNames() {
            Set<String> names = new LinkedHashSet<>();
            for (Object key : keys) {
                names.add((String) key);
            }
            return names;
        }
    }

    /**
     * Ensures a file exists at the specified path, creating it if needed.
     * @param filePath the file path.
     * @throws IOException if file operations fail.
     */
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

    /**
     * Loads properties from a file.
     * @param filePath the file path.
     * @return Properties object loaded from file.
     * @throws IOException if reading the file fails.
     */
    private static Properties loadProperties(String filePath) throws IOException {
        Properties prop = new LinkedProperties();
        ensureFileExists(filePath);
        synchronized (getFileLock(filePath)) {
            try (FileInputStream fis = new FileInputStream(filePath)) {
                prop.load(fis);
            }
        }
        return prop;
    }

    /**
     * Saves the given properties to a file.
     * @param filePath the file path.
     * @param prop Properties to save.
     * @throws IOException if writing to the file fails.
     */
    private static void saveProperties(String filePath, Properties prop) throws IOException {
        ensureFileExists(filePath);
        synchronized (getFileLock(filePath)) {
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                prop.store(fos, null);
            }
        }
    }

    /**
     * Retrieves the value for a specific key from a properties file.
     * @param filePath the properties file path.
     * @param key key to retrieve.
     * @return the property value or null.
     */
    public static String getDataFromProperties(String filePath, String key) {
        try {
            Properties prop = loadProperties(filePath);
            return prop.getProperty(key);
        } catch (IOException e) {
            log("Error accessing properties file: ", LogLevel.ERROR, e.getMessage());
            return null;
        }
    }

    /**
     * Sets a key-value pair in the properties file.
     * @param filePath the properties file path.
     * @param key property key.
     * @param value property value.
     */
    public static void setDataToProperties(String filePath, String key, String value) {
        synchronized (getFileLock(filePath)) {
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
    }

    /**
     * Loads all properties from a file.
     * @param filePath the properties file path.
     * @return Properties object.
     */
    public static Properties getAllProperties(String filePath) {
        try {
            return loadProperties(filePath);
        } catch (IOException e) {
            log("Failed to load properties file: ", LogLevel.ERROR, filePath);
            log("Root Cause: ", LogLevel.ERROR, e.getMessage());
            return new Properties();
        }
    }

    /**
     * Removes a key from the properties file.
     * @param filePath the properties file path.
     * @param key key to remove.
     */
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

    /**
     * Checks if a key exists in the properties file.
     * @param filePath the properties file path.
     * @param key key to check.
     * @return true if the key exists, false otherwise.
     */
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

    /**
     * Retrieves the value for a specific key from a properties file, or returns a default value if the key does not exist.
     * @param filePath the properties file path.
     * @param key key to retrieve.
     * @param defaultValue default value to return if the key does not exist.
     * @return the property value or the default value.
     */
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

    /**
     * Updates multiple properties in the properties file.
     * @param filePath the properties file path.
     * @param properties Properties object containing the key-value pairs to update.
     */
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

    /**
     * Adds or updates properties from a Map in the properties file.
     * @param filePath the properties file path.
     * @param map Map containing the key-value pairs to add or update.
     */
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

    /**
     * Retrieves properties as a Map from the properties file.
     * @param filePath the properties file path.
     * @return Map containing the properties.
     */
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

    /**
     * Updates properties from a Map in the properties file.
     * @param filePath the properties file path.
     * @param updates Map containing the key-value pairs to update.
     */
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

    /**
     * Adds new properties from a Map in the properties file.
     * @param filePath the properties file path.
     * @param newProperties Map containing the key-value pairs to add.
     */
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

    /**
     * Creates a backup of the properties file.
     * @param filePath the properties file path.
     */
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

    /**
     * Restores the properties file from a backup.
     * @param filePath the properties file path.
     */
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

    /**
     * Loads properties from a file with a specified encoding.
     * @param filePath the file path.
     * @param encoding the encoding to use.
     * @return Properties object loaded from file.
     */
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

    /**
     * Saves properties to a file with a specified encoding.
     * @param filePath the file path.
     * @param prop Properties to save.
     * @param encoding the encoding to use.
     */
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

    /**
     * Compares properties between two files and returns the differences.
     * @param file1Path the first file path.
     * @param file2Path the second file path.
     * @return Map containing the differences, with keys and their respective values in both files.
     */
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

    /**
     * Retrieves keys matching a specific pattern from the properties file.
     * @param filePath the properties file path.
     * @param pattern the pattern to match.
     * @return Set of keys matching the pattern.
     */
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

    /**
     * Clears all properties in the properties file.
     * @param filePath the properties file path.
     */
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

    /**
     * Retrieves the count of properties in the properties file.
     * @param filePath the properties file path.
     * @return the count of properties.
     */
    public static int getPropertyCount(String filePath) {
        Properties prop = getAllProperties(filePath);
        return prop.size();
    }

    /**
     * Finds values containing a specific string in the properties file.
     * @param filePath the properties file path.
     * @param searchStr the string to search for.
     * @return List of values containing the search string.
     */
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

    /**
     * Validates a property value against a regular expression.
     * @param filePath the properties file path.
     * @param key the property key.
     * @param regex the regular expression to validate against.
     * @return true if the value matches the regex, false otherwise.
     */
    public static boolean validatePropertyValue(String filePath, String key, String regex) {
        String value = getDataFromProperties(filePath, key);
        return value != null && value.matches(regex);
    }

    /**
     * Sorts properties by key in the properties file.
     * @param filePath the properties file path.
     */
    public static void sortPropertiesByKey(String filePath) {
        synchronized (getFileLock(filePath)) {
            try {
                Properties original = loadProperties(filePath);
                
                List<String> sortedKeys = new ArrayList<>(original.stringPropertyNames());
                Collections.sort(sortedKeys);
                
                LinkedProperties sortedProps = new LinkedProperties();
                
                for (String key : sortedKeys) {
                    sortedProps.setProperty(key, original.getProperty(key));
                }
                
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
                    sortedProps.store(writer, null);
                }
                
                log("Successfully sorted properties by key: ", LogLevel.INFO_GREEN, filePath);
            } catch (IOException e) {
                log("Failed to sort properties: ", LogLevel.ERROR, filePath);
                log("Root Cause: ", LogLevel.ERROR, e.getMessage());
            }
        }
    }

    /**
     * Retrieves properties matching a specific value pattern from the properties file.
     * @param filePath the properties file path.
     * @param pattern the pattern to match.
     * @return Map of properties matching the value pattern.
     */
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