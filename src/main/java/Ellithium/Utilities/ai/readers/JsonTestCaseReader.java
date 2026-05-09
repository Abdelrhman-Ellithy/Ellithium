package Ellithium.Utilities.ai.readers;

import Ellithium.Utilities.ai.models.TestCaseSource;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of TestCaseReader that parses test cases from a JSON file.
 * Expects a JSON format like:
 * [
 *   { "testId": "TC-01", "description": "Login with valid credentials" }
 * ]
 */
public class JsonTestCaseReader implements TestCaseReader {

    @Override
    public List<TestCaseSource> read(String filePath) {
        List<TestCaseSource> testCases = new ArrayList<>();
        try {
            List<Map<String, String>> data = JsonHelper.getJsonData(filePath);
            
            for (Map<String, String> map : data) {
                String id = map.getOrDefault("testId", "UNKNOWN");
                String desc = map.getOrDefault("description", "");
                
                testCases.add(new TestCaseSource(id, filePath, desc));
            }
        } catch (Exception e) {
            Reporter.log("Failed to parse JSON test cases from file: " + filePath + " | Error: " + e.getMessage(), LogLevel.ERROR);
        }
        
        return testCases;
    }
}
