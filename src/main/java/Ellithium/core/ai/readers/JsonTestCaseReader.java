package Ellithium.core.ai.readers;

import Ellithium.core.ai.models.TestCaseSource;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of TestCaseReader that parses test cases from a JSON file.
 *
 * <p>Supported formats:</p>
 * <pre>
 * [
 *   {
 *     "testId": "TC-01",
 *     "description": "Login with valid credentials",
 *     "targetUrl": "https://the-internet.herokuapp.com/login"
 *   },
 *   {
 *     "testId": "TC-02",
 *     "description": "Select Option 1 from dropdown"
 *   }
 * ]
 * </pre>
 *
 * <p>The {@code targetUrl} field is optional. When present, the AI engine will
 * open a headless browser, navigate to the URL, capture the live DOM, and send
 * it alongside the description to the LLM for grounded code generation.</p>
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
                String url = map.getOrDefault("targetUrl", null);

                testCases.add(new TestCaseSource(id, filePath, desc, url));
            }
        } catch (Exception e) {
            Reporter.log("Failed to parse JSON test cases from file: " + filePath + " | Error: " + e.getMessage(), LogLevel.ERROR);
        }

        return testCases;
    }
}
