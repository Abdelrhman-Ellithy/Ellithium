package Ellithium.Utilities.ai.readers;

import Ellithium.Utilities.ai.models.TestCaseSource;
import Ellithium.Utilities.helpers.TextHelper;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of TestCaseReader that parses test cases from a plain text file.
 * Assumes a specific format, for example each test case separated by a double newline,
 * starting with "ID: [testId]" followed by the description.
 */
public class TextTestCaseReader implements TestCaseReader {

    @Override
    public List<TestCaseSource> read(String filePath) {
        List<TestCaseSource> testCases = new ArrayList<>();
        try {
            // Read all lines using standard Java NIO
            String fullText = java.nio.file.Files.readString(java.nio.file.Paths.get(filePath));
            if (fullText == null || fullText.trim().isEmpty()) {
                return testCases;
            }

            // A simple parser for demonstration. In a real scenario, this would
            // use regex to reliably split on "ID: TC-01" markers.
            String[] blocks = fullText.split("\n\n");
            int counter = 1;

            for (String block : blocks) {
                if (block.trim().isEmpty()) continue;

                String id = "TC-TEXT-" + counter++;
                String description = block.trim();

                // If the block starts with "ID:", parse it explicitly
                if (block.startsWith("ID:")) {
                    int newLineIndex = block.indexOf("\n");
                    if (newLineIndex > 0) {
                        id = block.substring(3, newLineIndex).trim();
                        description = block.substring(newLineIndex).trim();
                    }
                }

                testCases.add(new TestCaseSource(id, filePath, description));
            }
        } catch (Exception e) {
            Reporter.log("Failed to parse Text test cases from file: " + filePath + " | Error: " + e.getMessage(), LogLevel.ERROR);
        }
        return testCases;
    }
}
