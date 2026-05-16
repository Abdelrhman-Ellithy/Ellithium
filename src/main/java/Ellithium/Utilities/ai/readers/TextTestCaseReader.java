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
 *
 * <p>Supported formats:</p>
 * <pre>
 * ID: TC-01
 * URL: https://the-internet.herokuapp.com/login
 * Login with valid credentials and verify dashboard loads
 *
 * ID: TC-02
 * Select "Option 1" from the dropdown and verify it's selected
 * </pre>
 *
 * <p>The URL line is optional. If omitted, no live DOM grounding is performed.</p>
 */
public class TextTestCaseReader implements TestCaseReader {
    private static final Pattern ID_LINE_PATTERN =
            Pattern.compile("(?im)^\\s*ID\\s*:\\s*([^\\r\\n]+)\\s*$");
    private static final Pattern URL_LINE_PATTERN =
            Pattern.compile("(?im)^\\s*URL\\s*:\\s*(https?://[^\\r\\n]+)\\s*$");

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
                // Still check for a URL at the top
                String url = extractUrl(normalized);
                String description = removeUrlLine(normalized).trim();
                testCases.add(new TestCaseSource("TC-TEXT-1", filePath, description, url));
                return testCases;
            }

            for (int i = 0; i < ids.size(); i++) {
                int contentStart = idRanges.get(i)[1];
                int contentEnd = (i + 1 < ids.size()) ? idRanges.get(i + 1)[0] : normalized.length();
                String rawContent = normalized.substring(contentStart, contentEnd).trim();

                if (rawContent.isEmpty()) {
                    Reporter.log("Text test case '" + ids.get(i) + "' has empty description. Skipping.", LogLevel.WARN);
                    continue;
                }

                // Extract optional URL from this test case's content
                String url = extractUrl(rawContent);
                String description = removeUrlLine(rawContent).trim();

                if (description.isEmpty()) {
                    Reporter.log("Text test case '" + ids.get(i) + "' has empty description after URL extraction. Skipping.", LogLevel.WARN);
                    continue;
                }

                testCases.add(new TestCaseSource(ids.get(i), filePath, description, url));
            }
        } catch (Exception e) {
            Reporter.log("Failed to parse Text test cases from file: " + filePath + " | Error: " + e.getMessage(), LogLevel.ERROR);
        }
        return testCases;
    }

    /**
     * Extracts the first URL from a text block.
     */
    private String extractUrl(String text) {
        Matcher m = URL_LINE_PATTERN.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Removes URL lines from text to get clean description.
     */
    private String removeUrlLine(String text) {
        return URL_LINE_PATTERN.matcher(text).replaceAll("").trim();
    }
}
