package Ellithium.Utilities.ai.readers;

import Ellithium.Utilities.ai.models.TestCaseSource;
import java.util.List;

/**
 * Strategy interface for parsing natural language test cases from various sources.
 */
public interface TestCaseReader {
    
    /**
     * Reads a file and extracts all test cases from it.
     *
     * @param filePath The absolute or relative path to the source file
     * @return A list of TestCaseSource DTOs parsed from the file
     */
    List<TestCaseSource> read(String filePath);
}
