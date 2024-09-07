package AutoEllithiumSphere.Utilities;

import com.google.gson.*;

import java.io.*;
import java.util.List;

public class JsonHelper extends DataUtils {
    // JSON File
    public static String getJsonData(String fileName, String key) {
        FileReader reader = null;
        try {
            reader = new FileReader(TEST_DATA_PATH + fileName + ".json");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        JsonElement jsonElement = JsonParser.parseReader(reader);
        return jsonElement.getAsJsonObject().get(key).getAsString();
    }
    public static void setJsonData(String fileName, String key, String value) throws IOException, JsonSyntaxException {
        // Read the existing JSON file
        FileReader reader = new FileReader(TEST_DATA_PATH + fileName + ".json");
        JsonElement jsonElement = JsonParser.parseReader(reader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Set the new value
        jsonObject.add(key, new JsonPrimitive(value));

        // Write the updated JSON back to the file
        FileWriter writer = new FileWriter(TEST_DATA_PATH + fileName + ".json");
        writer.write(jsonObject.toString());
        writer.close();
    }
    public static void writeStringIntoJson(String fileName, List<String> productNames) {
        StringBuilder jsonContent = new StringBuilder();
        for (String productName : productNames) {
            jsonContent.append(productName).append("\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_DATA_PATH + fileName + ".json"))) {
            writer.write(jsonContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeIntoJson(String fileName, List<String> productNames) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_DATA_PATH + fileName + ".json"))) {
            for (String productName : productNames) {
                writer.write(productName);
                writer.newLine(); // Add newline after each product name
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
