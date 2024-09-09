package Ellithium.Utilities;

import java.io.*;
import java.util.Properties;
public class PropertyHelper extends DataUtils {
    public static String getDataFromProperties(String FilePath,  String key) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(FilePath +".properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prop.getProperty(key);
    }
    public static void setDataToProperties(String FilePath, String key, String value) {
        Properties prop = new Properties();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(FilePath + ".properties");
            prop.setProperty(key, value);
            try {
                prop.store(out, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
