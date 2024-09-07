package AutoEllithiumSphere.Utilities;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
public class PropertyHelper extends DataUtils {
    // File Properties
    public static String getDataFromProperties(String key) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(TEST_DATA_PATH + "environment.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prop.getProperty(key);
    }
    public static void setDataToProperties(String key, String value) {
        Properties prop = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(TEST_DATA_PATH + "environment.properties");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            prop.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(TEST_DATA_PATH + "environment.properties");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        prop.setProperty(key, value);
        try {
            prop.store(out, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
