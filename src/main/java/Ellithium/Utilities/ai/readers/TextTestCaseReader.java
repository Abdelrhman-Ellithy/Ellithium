package Ellithium.Utilities.ai.readers;

import Ellithium.Utilities.ai.models.TestCaseSource;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of TestCaseReader that parses test cases from a plain text file.
 * Assumes a specific format, for example each test case separated by a double newline,
 * starting with "ID: [testId]" followed by the description.
 */
public class TextTestCaseReader implements TestCaseReader {
    private static final Pattern ID_LINE_PATTERN =
            Pattern.compile("(?im)^\\s*ID\\s*:\\s*([^\\r\\n]+)\\s*$");

    @Override
    public List<TestCaseSource> read(String filePath) {
        List<TestCaseSource> testCases = new ArrayList<>();
        try {
            String fullText = java.nio.file.Files.readString(java.nio.file.Paths.get(filePath));
            if (fullText == null || fullText.trim().isEmpty()) {
                return testCases;
            }

            String normalized = fullText.replace("\r\n", "\n");
            Matcher matcher = ID_LINE_PATTERN.matcher(normalized);

            List<int[]> idRanges = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            while (matcher.find()) {
                ids.add(matcher.group(1).trim());
                idRanges.add(new int[]{matcher.start(), matcher.end()});
            }

            if (ids.isEmpty()) {
                // No explicit ID markers: treat entire document as a single testcase.
                testCases.add(new TestCaseSource("TC-TEXT-1", filePath, normalized.trim()));
                return testCases;
            }

            for (int i = 0; i < ids.size(); i++) {
                int contentStart = idRanges.get(i)[1];
                int contentEnd = (i + 1 < ids.size()) ? idRanges.get(i + 1)[0] : normalized.length();
                String description = normalized.substring(contentStart, contentEnd).trim();
                if (description.isEmpty()) {
                    Reporter.log("Text test case '" + ids.get(i) + "' has empty description. Skipping.", LogLevel.WARN);
                    continue;
                }
                testCases.add(new TestCaseSource(ids.get(i), filePath, description));
            }
        } catch (Exception e) {
            Reporter.log("Failed to parse Text test cases from file: " + filePath + " | Error: " + e.getMessage(), LogLevel.ERROR);
        }
        return testCases;
    }
}
