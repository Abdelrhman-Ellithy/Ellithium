package Helpers;

import Ellithium.Utilities.helpers.TextHelper;
import Ellithium.core.base.NonBDDSetup;
import org.testng.annotations.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;

public class TextHelperTests extends NonBDDSetup {
    private static final String TEST_DIR = "src/test/resources/TestData/";
    private static final String TEST_FILE = TEST_DIR + "test.txt";
    private static final String OUTPUT_FILE = TEST_DIR + "output.txt";
    private static final String MERGED_FILE = TEST_DIR + "merged.txt";

    @BeforeMethod
    public void setUp() throws IOException {
        new File(TEST_DIR).mkdirs();
        List<String> initialContent = Arrays.asList(
            "Line 1: Test content",
            "Line 2: More content",
            "Line 3: Final line"
        );
        TextHelper.writeTextFile(TEST_FILE, initialContent);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        try {
            // Add delay to ensure file handles are released
            Thread.sleep(100);
            
            // Close any potential remaining file handles
            System.gc();
            
            // Delete files with retries
            deleteFileWithRetry(TEST_FILE);
            deleteFileWithRetry(OUTPUT_FILE);
            deleteFileWithRetry(MERGED_FILE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for file handles to be released", e);
        }
    }
    
    private void deleteFileWithRetry(String filePath) throws IOException {
        int maxRetries = 3;
        int retryDelay = 100; // milliseconds
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                Files.deleteIfExists(Path.of(filePath));
                return;
            } catch (IOException e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to retry file deletion", ie);
                }
            }
        }
    }

    @Test
    public void testReadTextFile() {
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("Line 1"));
    }

    @Test
    public void testWriteTextFile() {
        List<String> newContent = Arrays.asList("New line 1", "New line 2");
        TextHelper.writeTextFile(OUTPUT_FILE, newContent);
        List<String> readLines = TextHelper.readTextFile(OUTPUT_FILE);
        assertEquals(newContent, readLines);
    }

    @Test
    public void testAppendLineToFile() {
        String newLine = "Appended line";
        TextHelper.appendLineToFile(TEST_FILE, newLine);
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals(4, lines.size());
        assertEquals(newLine, lines.get(3));
    }

    @Test
    public void testFindLineWithKeyword() {
        String line = TextHelper.findLineWithKeyword(TEST_FILE, "More");
        assertNotNull(line);
        assertTrue(line.contains("Line 2"));
    }

    @Test
    public void testDeleteLine() {
        TextHelper.deleteLine(TEST_FILE, "Line 2: More content");
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals(2, lines.size());
        assertFalse(lines.contains("Line 2: More content"));
    }

    // Add more test methods...
    @Test
    public void testSpecialCharacterHandling() {
        String specialChars = "!@#$%^&*()_+ 你好";
        TextHelper.writeTextFile(OUTPUT_FILE, Arrays.asList(specialChars));
        List<String> lines = TextHelper.readTextFile(OUTPUT_FILE);
        assertEquals(specialChars, lines.get(0));
    }

    @Test
    public void testEmptyFileHandling() {
        TextHelper.writeTextFile(OUTPUT_FILE, Collections.emptyList());
        List<String> lines = TextHelper.readTextFile(OUTPUT_FILE);
        assertTrue(lines.isEmpty());
        assertTrue(TextHelper.isEmptyFile(OUTPUT_FILE));
    }

    @Test
    public void testLargeFileHandling() {
        List<String> largeContent = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeContent.add("Line " + i);
        }
        TextHelper.writeTextFile(OUTPUT_FILE, largeContent);
        List<String> lines = TextHelper.readTextFile(OUTPUT_FILE);
        assertEquals(10000, lines.size());
    }

    @Test
    public void testConcurrentOperations() throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                TextHelper.appendLineToFile(TEST_FILE, "Thread " + threadNum);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals(13, lines.size()); // 3 original + 10 appended
    }

    @Test
    public void testReadFileAsString() {
        String content = TextHelper.readFileAsString(TEST_FILE);
        assertNotNull(content);
        assertTrue(content.contains("Line 1"));
        assertTrue(content.contains("Line 2"));
        assertTrue(content.contains("Line 3"));
    }

    @Test
    public void testWriteStringToFile() {
        String content = "Test content" + System.lineSeparator() + "New line";
        TextHelper.writeStringToFile(OUTPUT_FILE, content);
        String readContent = TextHelper.readFileAsString(OUTPUT_FILE);
        assertEquals(content, readContent, "File content should match exactly, including line endings");
    }

    @Test
    public void testDeleteLinesContainingKeyword() {
        TextHelper.deleteLinesContainingKeyword(TEST_FILE, "More");
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertFalse(lines.stream().anyMatch(line -> line.contains("More")));
        assertEquals(2, lines.size());
    }

    @Test
    public void testContainsKeyword() {
        assertTrue(TextHelper.containsKeyword(TEST_FILE, "Test"));
        assertFalse(TextHelper.containsKeyword(TEST_FILE, "nonexistent"));
    }

    @Test
    public void testReplaceLineInFile() {
        String newLine = "New content";
        TextHelper.replaceLineInFile(TEST_FILE, "Line 2: More content", newLine);
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertTrue(lines.contains(newLine));
        assertFalse(lines.contains("Line 2: More content"));
    }

    @Test
    public void testCountLinesInFile() {
        assertEquals(3, TextHelper.countLinesInFile(TEST_FILE));
        TextHelper.appendLineToFile(TEST_FILE, "New Line");
        assertEquals(4, TextHelper.countLinesInFile(TEST_FILE));
    }

    @Test
    public void testIsEmptyFile() {
        assertFalse(TextHelper.isEmptyFile(TEST_FILE));
        TextHelper.writeTextFile(OUTPUT_FILE, Collections.emptyList());
        assertTrue(TextHelper.isEmptyFile(OUTPUT_FILE));
    }

    @Test
    public void testReplaceAllOccurrences() {
        TextHelper.replaceAllOccurrences(TEST_FILE, "Line", "Row");
        String content = TextHelper.readFileAsString(TEST_FILE);
        assertFalse(content.contains("Line"));
        assertTrue(content.contains("Row"));
    }

    @Test
    public void testGetAllLinesWithKeyword() {
        List<String> matches = TextHelper.getAllLinesWithKeyword(TEST_FILE, "Line");
        assertEquals(3, matches.size());
        assertTrue(matches.stream().allMatch(line -> line.contains("Line")));
    }

    @Test
    public void testCopyTextFile() {
        TextHelper.copyTextFile(TEST_FILE, OUTPUT_FILE);
        assertTrue(TextHelper.areFilesIdentical(TEST_FILE, OUTPUT_FILE));
    }

    @Test
    public void testAreFilesIdentical() {
        TextHelper.copyTextFile(TEST_FILE, OUTPUT_FILE);
        assertTrue(TextHelper.areFilesIdentical(TEST_FILE, OUTPUT_FILE));
        TextHelper.appendLineToFile(OUTPUT_FILE, "Different");
        assertFalse(TextHelper.areFilesIdentical(TEST_FILE, OUTPUT_FILE));
    }

    @Test
    public void testGetLineNumbersWithKeyword() {
        List<Integer> lineNumbers = TextHelper.getLineNumbersWithKeyword(TEST_FILE, "Line");
        assertEquals(3, lineNumbers.size());
        assertEquals(Arrays.asList(1, 2, 3), lineNumbers);
    }

    @Test
    public void testInsertLineAt() {
        TextHelper.insertLineAt(TEST_FILE, 2, "Inserted Line");
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals("Inserted Line", lines.get(1));
        assertEquals(4, lines.size());
    }

    @Test
    public void testAppendAfterLine() {
        TextHelper.appendAfterLine(TEST_FILE, 1, "Appended Line");
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals("Appended Line", lines.get(1));
        assertEquals(4, lines.size());
    }

    @Test
    public void testReadSpecificLines() {
        List<String> specificLines = TextHelper.readSpecificLines(TEST_FILE, 1, 2);
        assertEquals(2, specificLines.size());
        assertTrue(specificLines.get(0).contains("Line 1"));
        assertTrue(specificLines.get(1).contains("Line 2"));
    }

    @Test
    public void testWriteAtLine() {
        TextHelper.writeAtLine(TEST_FILE, 2, "Modified Line");
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals("Modified Line", lines.get(1));
    }

    @Test
    public void testGetFileSizeBytes() {
        long size = TextHelper.getFileSizeBytes(TEST_FILE);
        assertTrue(size > 0);
    }

    @Test
    public void testGetLastModifiedTimestamp() {
        long timestamp = TextHelper.getLastModifiedTimestamp(TEST_FILE);
        assertTrue(timestamp > 0);
    }

    @Test
    public void testTruncateFile() {
        TextHelper.truncateFile(TEST_FILE, 2);
        assertEquals(2, TextHelper.countLinesInFile(TEST_FILE));
    }

    @Test
    public void testRemoveDuplicateLines() {
        TextHelper.appendLineToFile(TEST_FILE, "Line 1: Test content");
        TextHelper.removeDuplicateLines(TEST_FILE);
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals(3, lines.size());
    }

    @Test
    public void testReadLastNLines() {
        List<String> lastLines = TextHelper.readLastNLines(TEST_FILE, 2);
        assertEquals(2, lastLines.size());
        assertTrue(lastLines.get(1).contains("Line 3"));
    }

    @Test
    public void testReadFirstNLines() {
        List<String> firstLines = TextHelper.readFirstNLines(TEST_FILE, 2);
        assertEquals(2, firstLines.size());
        assertTrue(firstLines.get(0).contains("Line 1"));
    }

    @Test
    public void testReadLineNumber() {
        String line = TextHelper.readLineNumber(TEST_FILE, 2);
        assertNotNull(line);
        assertTrue(line.contains("Line 2"));
    }

    @Test
    public void testIsValidLineNumber() {
        assertTrue(TextHelper.isValidLineNumber(TEST_FILE, 1));
        assertFalse(TextHelper.isValidLineNumber(TEST_FILE, 4));
        assertFalse(TextHelper.isValidLineNumber(TEST_FILE, 0));
    }

    @Test
    public void testSearchLinesMatching() {
        List<String> matches = TextHelper.searchLinesMatching(TEST_FILE, ".*Line [1-2].*");
        assertEquals(2, matches.size());
    }

    @Test
    public void testMergeTwoFiles() {
        // Create second test file
        List<String> secondContent = Arrays.asList("Second1", "Second2");
        TextHelper.writeTextFile(OUTPUT_FILE, secondContent);
        
        TextHelper.mergeTwoFiles(TEST_FILE, OUTPUT_FILE, MERGED_FILE);
        List<String> mergedContent = TextHelper.readTextFile(MERGED_FILE);
        assertEquals(5, mergedContent.size());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("txt", TextHelper.getFileExtension(TEST_FILE));
        assertEquals("", TextHelper.getFileExtension("noextension"));
    }

    @Test
    public void testReverseFileContent() {
        TextHelper.reverseFileContent(TEST_FILE);
        List<String> lines = TextHelper.readTextFile(TEST_FILE);
        assertEquals("Line 3: Final line", lines.get(0));
        assertEquals("Line 1: Test content", lines.get(2));
    }

    @Test
    public void testGetWordFrequency() {
        Map<String, Integer> frequency = TextHelper.getWordFrequency(TEST_FILE);
        assertEquals(Integer.valueOf(3), frequency.get("Line"));
        assertTrue(frequency.containsKey("content"));
    }

    @Test
    public void testReadWriteWithEncoding() {
        List<String> originalLines = Arrays.asList("Test UTF-8 encoding", "με ελληνικά", "with 日本語");
        TextHelper.writeWithEncoding(OUTPUT_FILE, originalLines, "UTF-8");
        List<String> readLines = TextHelper.readWithEncoding(OUTPUT_FILE, "UTF-8");
        assertEquals(originalLines, readLines);
    }

    @Test
    public void testEdgeCases() {
        // Test with empty file
        TextHelper.writeTextFile(OUTPUT_FILE, Collections.emptyList());
        assertTrue(TextHelper.isEmptyFile(OUTPUT_FILE));
        assertEquals(0, TextHelper.countLinesInFile(OUTPUT_FILE));
        
        // Test with very large content
        List<String> largeContent = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeContent.add("Line " + i);
        }
        TextHelper.writeTextFile(OUTPUT_FILE, largeContent);
        assertEquals(10000, TextHelper.countLinesInFile(OUTPUT_FILE));
        
        // Test with special characters
        String specialChars = "!@#$%^&*()_+ 你好 привет";
        TextHelper.writeStringToFile(OUTPUT_FILE, specialChars);
        assertEquals(specialChars, TextHelper.readFileAsString(OUTPUT_FILE).trim());
    }

    @Test
    public void testErrorHandling() {
        // Test with non-existent file
        assertNull(TextHelper.readFileAsString("nonexistent.txt"));
        assertEquals(0, TextHelper.countLinesInFile("nonexistent.txt"));
        
        // Test with invalid line numbers
        assertNull(TextHelper.readLineNumber(TEST_FILE, -1));
        assertNull(TextHelper.readLineNumber(TEST_FILE, 1000));
        
        // Test with invalid encoding
        List<String> testLines = Arrays.asList("Test line");
        TextHelper.writeWithEncoding(OUTPUT_FILE, testLines, "UTF-8");
        
        try {
            List<String> result = TextHelper.readWithEncoding(OUTPUT_FILE, "invalid-encoding");
            assertTrue(result.isEmpty());
        } catch (Exception e) {
            // Expected exception for invalid encoding
            assertNotNull(e);
        }
    }
}
