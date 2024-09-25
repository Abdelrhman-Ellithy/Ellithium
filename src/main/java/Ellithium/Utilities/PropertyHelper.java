package Ellithium.Utilities;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;

import java.io.*;
import java.util.Properties;

public class PropertyHelper {

    // Method to get data from a properties file using a key
    public static String getDataFromProperties(String filePath, String key) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(filePath + ".properties"));
            logsUtils.info(Colors.GREEN + "Successfully loaded properties file: " + filePath + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to load properties file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
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
            logsUtils.info(Colors.GREEN + "Successfully updated properties file: " + filePath + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write properties file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            throw new RuntimeException(e);
        }
    }
}