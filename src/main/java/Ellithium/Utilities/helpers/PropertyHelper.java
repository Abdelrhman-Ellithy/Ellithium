package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import static Ellithium.core.reporting.Reporter.log;
import java.io.*;
import java.util.Properties;

public class PropertyHelper {

    // Method to get data from a properties file using a key
    public static String getDataFromProperties(String filePath, String key) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(filePath + ".properties"));
            log("Successfully loaded properties file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            log("Failed to load properties file: ", LogLevel.ERROR, filePath);
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }
}