package Helpers;

import Ellithium.Utilities.helpers.JarExtractor;
import Ellithium.core.base.NonBDDSetup;
import org.testng.annotations.*;
import java.io.*;
import java.nio.file.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;

public class JarExtractorTests extends NonBDDSetup {
    private static final String TEST_DIR = "src/test/resources/TestData/";
    private static final String TEST_JAR = TEST_DIR + "test.jar";
    private static final String EXTRACTED_DIR = TEST_DIR + "extracted/";

    @BeforeClass
    public void setupTestDirectory() throws IOException {
        Files.createDirectories(Paths.get(TEST_DIR));
    }

    @BeforeMethod
    public void setUp() throws IOException {
        // Create a test JAR file
        createTestJar(TEST_JAR);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        // Clean up test files
        deleteDirectory(new File(EXTRACTED_DIR));
        Files.deleteIfExists(Paths.get(TEST_JAR));
    }

    private void createTestJar(String jarPath) throws IOException {
        File jarFile = new File(jarPath);
        try (FileOutputStream fos = new FileOutputStream(jarFile);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            // Add a sample text file
            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "sample.txt");
            
            // Add a sample directory
            addDirectoryToJar(jarOut, "testdir/");
            
            // Add a sample file inside the directory
            addFileToJar(new File("src/test/resources/TestData/sample_in_dir.txt"), jarOut, "testdir/sample_in_dir.txt");
        }
    }

    private void addFileToJar(File file, JarOutputStream jarOut, String entryName) throws IOException {
        if (!file.exists()) {
            Files.createFile(file.toPath());
            Files.write(file.toPath(), "Sample content".getBytes());
        }
        
        JarEntry entry = new JarEntry(entryName);
        jarOut.putNextEntry(entry);
        Files.copy(file.toPath(), jarOut);
        jarOut.closeEntry();
    }

    private void addDirectoryToJar(JarOutputStream jarOut, String entryName) throws IOException {
        JarEntry entry = new JarEntry(entryName);
        jarOut.putNextEntry(entry);
        jarOut.closeEntry();
    }

    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            Files.walk(directory.toPath())
                 .sorted((p1, p2) -> -p1.compareTo(p2))
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }

    @Test
    public void testExtractJar() throws IOException {
        JarExtractor.extractFolderFromJar(new File(TEST_JAR), "", new File(EXTRACTED_DIR));

        File extractedSample = new File(EXTRACTED_DIR + "sample.txt");
        File extractedDir = new File(EXTRACTED_DIR + "testdir");
        File extractedSampleInDir = new File(EXTRACTED_DIR + "testdir/sample_in_dir.txt");

        assertTrue(extractedSample.exists());
        assertTrue(extractedDir.exists());
        assertTrue(extractedSampleInDir.exists());
    }

    @Test
    public void testExtractNonExistentJar() {
        JarExtractor.extractFolderFromJar(new File("nonexistent.jar"), "", new File(EXTRACTED_DIR));
        File extractedDir = new File(EXTRACTED_DIR);
        assertFalse(extractedDir.exists());
    }

    @Test
    public void testExtractJarWithEmptyJar() throws IOException {
        String emptyJarPath = TEST_DIR + "empty.jar";
        try (FileOutputStream fos = new FileOutputStream(emptyJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {
            // Create an empty JAR
        }
        JarExtractor.extractFolderFromJar(new File(emptyJarPath), "", new File(EXTRACTED_DIR));
        File extractedDir = new File(EXTRACTED_DIR);
        assertTrue(extractedDir.exists());
        Files.deleteIfExists(Paths.get(emptyJarPath));
    }

    @Test
    public void testExtractJarWithSubdirectories() throws IOException {
        String subDirJarPath = TEST_DIR + "subdir.jar";
        try (FileOutputStream fos = new FileOutputStream(subDirJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addDirectoryToJar(jarOut, "level1/");
            addDirectoryToJar(jarOut, "level1/level2/");
            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "level1/level2/sample.txt");
        }

        JarExtractor.extractFolderFromJar(new File(subDirJarPath), "", new File(EXTRACTED_DIR));
        File level1Dir = new File(EXTRACTED_DIR + "level1");
        File level2Dir = new File(EXTRACTED_DIR + "level1/level2");
        File sampleInLevel2 = new File(EXTRACTED_DIR + "level1/level2/sample.txt");

        assertTrue(level1Dir.exists());
        assertTrue(level2Dir.exists());
        assertTrue(sampleInLevel2.exists());
        Files.deleteIfExists(Paths.get(subDirJarPath));
    }

    @Test
    public void testExtractJarWithSpecialCharacters() throws IOException {
        String specialCharsJarPath = TEST_DIR + "specialchars.jar";
        try (FileOutputStream fos = new FileOutputStream(specialCharsJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "file!@#$%.txt");
        }

        JarExtractor.extractFolderFromJar(new File(specialCharsJarPath), "", new File(EXTRACTED_DIR));
        File specialCharsFile = new File(EXTRACTED_DIR + "file!@#$%.txt");
        assertTrue(specialCharsFile.exists());
        Files.deleteIfExists(Paths.get(specialCharsJarPath));
    }

    @Test
    public void testExtractJarWithLargeFile() throws IOException {
        String largeFilePath = TEST_DIR + "large_file.txt";
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("Line ").append(i).append("\n");
        }
        Files.write(Paths.get(largeFilePath), largeContent.toString().getBytes());

        String largeJarPath = TEST_DIR + "largefile.jar";
        try (FileOutputStream fos = new FileOutputStream(largeJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addFileToJar(new File(largeFilePath), jarOut, "large_file.txt");
        }

        JarExtractor.extractFolderFromJar(new File(largeJarPath), "", new File(EXTRACTED_DIR));
        File extractedLargeFile = new File(EXTRACTED_DIR + "large_file.txt");
        assertTrue(extractedLargeFile.exists());
        Files.deleteIfExists(Paths.get(largeJarPath));
        Files.deleteIfExists(Paths.get(largeFilePath));
    }

    @Test
    public void testExtractJarWithMultipleEntries() throws IOException {
        String multiEntryJarPath = TEST_DIR + "multientry.jar";
        try (FileOutputStream fos = new FileOutputStream(multiEntryJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "sample1.txt");
            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "sample2.txt");
        }

        JarExtractor.extractFolderFromJar(new File(multiEntryJarPath), "", new File(EXTRACTED_DIR));
        File sample1 = new File(EXTRACTED_DIR + "sample1.txt");
        File sample2 = new File(EXTRACTED_DIR + "sample2.txt");

        assertTrue(sample1.exists());
        assertTrue(sample2.exists());
        Files.deleteIfExists(Paths.get(multiEntryJarPath));
    }

    @Test
    public void testExtractJarWithDirectoryTraversal() throws IOException {
        String traversalJarPath = TEST_DIR + "traversal.jar";
        try (FileOutputStream fos = new FileOutputStream(traversalJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "testdir/../../evil.txt");
        }

        JarExtractor.extractFolderFromJar(new File(traversalJarPath), "", new File(EXTRACTED_DIR));
        File evilFile = new File(EXTRACTED_DIR + "evil.txt");
        assertFalse(evilFile.exists());
        Files.deleteIfExists(Paths.get(traversalJarPath));
    }

    @Test
    public void testExtractJarWithUnicodeEntryNames() throws IOException {
        String unicodeJarPath = TEST_DIR + "unicodeentry.jar";
        try (FileOutputStream fos = new FileOutputStream(unicodeJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "你好世界.txt");
        }

        JarExtractor.extractFolderFromJar(new File(unicodeJarPath), "", new File(EXTRACTED_DIR));
        File unicodeFile = new File(EXTRACTED_DIR + "你好世界.txt");
        assertTrue(unicodeFile.exists());
        Files.deleteIfExists(Paths.get(unicodeJarPath));
    }

    @Test
    public void testExtractJarWithEmptyDirectories() throws IOException {
        String emptyDirJarPath = TEST_DIR + "emptydir.jar";
        try (FileOutputStream fos = new FileOutputStream(emptyDirJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addDirectoryToJar(jarOut, "empty/");
        }

        JarExtractor.extractFolderFromJar(new File(emptyDirJarPath), "", new File(EXTRACTED_DIR));
        File emptyDir = new File(EXTRACTED_DIR + "empty");
        assertTrue(emptyDir.exists());
        Files.deleteIfExists(Paths.get(emptyDirJarPath));
    }

    @Test
    public void testExtractJarWithFileAndDirectorySameName() throws IOException {
        String sameNameJarPath = TEST_DIR + "samename.jar";
        try (FileOutputStream fos = new FileOutputStream(sameNameJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addDirectoryToJar(jarOut, "sameName/");
            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "sameName");
        }

        JarExtractor.extractFolderFromJar(new File(sameNameJarPath), "", new File(EXTRACTED_DIR));
        File sameNameDir = new File(EXTRACTED_DIR + "sameName");
        File sameNameFile = new File(EXTRACTED_DIR + "sameName");
        assertTrue(sameNameDir.exists());
        assertTrue(sameNameFile.exists());
        Files.deleteIfExists(Paths.get(sameNameJarPath));
    }

    @Test
    public void testExtractJarWithNoEntries() throws IOException {
        String noEntriesJarPath = TEST_DIR + "noentries.jar";
        try (FileOutputStream fos = new FileOutputStream(noEntriesJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {
            // Create an empty JAR with no entries
        }

        JarExtractor.extractFolderFromJar(new File(noEntriesJarPath), "", new File(EXTRACTED_DIR));
        File extractedDir = new File(EXTRACTED_DIR);
        assertTrue(extractedDir.exists());
        Files.deleteIfExists(Paths.get(noEntriesJarPath));
    }

    @Test
    public void testExtractJarWithLongFileNames() throws IOException {
        String longName = "this_is_a_very_long_file_name_to_test_the_limits_of_the_extractor_" +
                          "this_is_a_very_long_file_name_to_test_the_limits_of_the_extractor.txt";
        String longFileJarPath = TEST_DIR + "longfile.jar";
        try (FileOutputStream fos = new FileOutputStream(longFileJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, longName);
        }

        JarExtractor.extractFolderFromJar(new File(longFileJarPath), "", new File(EXTRACTED_DIR));
        File longNameFile = new File(EXTRACTED_DIR + longName);
        assertTrue(longNameFile.exists());
        Files.deleteIfExists(Paths.get(longFileJarPath));
    }

    @Test
    public void testExtractJarWithDifferentFileTypes() throws IOException {
        String differentTypesJarPath = TEST_DIR + "differenttypes.jar";
        try (FileOutputStream fos = new FileOutputStream(differentTypesJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "sample.txt");
            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "sample.pdf");
            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "sample.jpg");
        }

        JarExtractor.extractFolderFromJar(new File(differentTypesJarPath), "", new File(EXTRACTED_DIR));
        File sampleTxt = new File(EXTRACTED_DIR + "sample.txt");
        File samplePdf = new File(EXTRACTED_DIR + "sample.pdf");
        File sampleJpg = new File(EXTRACTED_DIR + "sample.jpg");

        assertTrue(sampleTxt.exists());
        assertTrue(samplePdf.exists());
        assertTrue(sampleJpg.exists());
        Files.deleteIfExists(Paths.get(differentTypesJarPath));
    }


    @Test
    public void testExtractJarWithSameNameFileAndDirectory() throws IOException {
        String sameNameJarPath = TEST_DIR + "samename.jar";
        try (FileOutputStream fos = new FileOutputStream(sameNameJarPath);
             JarOutputStream jarOut = new JarOutputStream(fos)) {

            addDirectoryToJar(jarOut, "sameName/");
            addFileToJar(new File("src/test/resources/TestData/sample.txt"), jarOut, "sameName");
        }

        JarExtractor.extractFolderFromJar(new File(sameNameJarPath), "", new File(EXTRACTED_DIR));
        File sameNameDir = new File(EXTRACTED_DIR + "sameName");
        File sameNameFile = new File(EXTRACTED_DIR + "sameName");
        assertTrue(sameNameDir.exists());
        assertTrue(sameNameFile.exists());
    }
}
