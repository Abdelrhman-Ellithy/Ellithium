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
        Files.deleteIfExists(Path.of(TEST_FILE));
        Files.deleteIfExists(Path.of(OUTPUT_FILE));
        Files.deleteIfExists(Path.of(MERGED_FILE));
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
}
