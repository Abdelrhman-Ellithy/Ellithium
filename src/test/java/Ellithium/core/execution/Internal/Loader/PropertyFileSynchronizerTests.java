package Ellithium.core.execution.Internal.Loader;

import org.testng.annotations.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;

public class PropertyFileSynchronizerTests {
    private static final String TEST_DIR = "src/test/resources/TestData/PropertyFileSynchronizer/";
    private static final String TEMPLATE_JAR = TEST_DIR + "template.jar";
    private static final String JAR_ENTRY = "properties/sample.properties";

    private static final String TEMPLATE_CONTENT =
            "# Important: Don't Delete any property key\n" +
            "\n" +
            "# Description of alpha\n" +
            "alpha=1\n" +
            "\n" +
            "# First line about beta\n" +
            "# Second line about beta\n" +
            "beta=2\n" +
            "\n" +
            "gamma=3\n";

    @BeforeClass
    public void setupTestDirectory() throws IOException {
        Files.createDirectories(Paths.get(TEST_DIR));
        createTemplateJar();
    }

    @AfterClass
    public void tearDownClass() throws IOException {
        Files.deleteIfExists(Paths.get(TEMPLATE_JAR));
        Files.walk(Paths.get(TEST_DIR))
                .sorted((p1, p2) -> -p1.compareTo(p2))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private void createTemplateJar() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(TEMPLATE_JAR);
             JarOutputStream jarOut = new JarOutputStream(fos)) {
            JarEntry entry = new JarEntry(JAR_ENTRY);
            jarOut.putNextEntry(entry);
            jarOut.write(TEMPLATE_CONTENT.getBytes(StandardCharsets.UTF_8));
            jarOut.closeEntry();
        }
    }

    private File writeTargetFile(String name, String content) throws IOException {
        File file = new File(TEST_DIR + name);
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    public void syncMissingKeys_appendsMissingKeyWithItsComment() throws IOException {
        File target = writeTargetFile("missingBeta.properties",
                "# Important: Don't Delete any property key\n\n# Description of alpha\nalpha=1\n\ngamma=3\n");

        PropertyFileSynchronizer.syncMissingKeys(new File(TEMPLATE_JAR), JAR_ENTRY, target.getPath(), "9.9.9");

        String result = Files.readString(target.toPath(), StandardCharsets.UTF_8);
        assertTrue(result.contains("# First line about beta"));
        assertTrue(result.contains("# Second line about beta"));
        assertTrue(result.contains("beta=2"));
        assertTrue(result.contains("9.9.9"));
    }

    @Test
    public void syncMissingKeys_preservesExistingKeysAndValues() throws IOException {
        String original = "# Important: Don't Delete any property key\n\nalpha=999\ngamma=3\n";
        File target = writeTargetFile("preserveExisting.properties", original);

        PropertyFileSynchronizer.syncMissingKeys(new File(TEMPLATE_JAR), JAR_ENTRY, target.getPath(), "9.9.9");

        Properties result = new Properties();
        try (FileInputStream fis = new FileInputStream(target)) {
            result.load(fis);
        }
        assertEquals(result.getProperty("alpha"), "999");
        String rawResult = Files.readString(target.toPath(), StandardCharsets.UTF_8);
        assertTrue(rawResult.startsWith(original));
    }

    @Test
    public void syncMissingKeys_addsAllMissingKeysAtOnce() throws IOException {
        File target = writeTargetFile("allMissing.properties", "gamma=3\n");

        PropertyFileSynchronizer.syncMissingKeys(new File(TEMPLATE_JAR), JAR_ENTRY, target.getPath(), "9.9.9");

        Properties result = new Properties();
        try (FileInputStream fis = new FileInputStream(target)) {
            result.load(fis);
        }
        assertEquals(result.getProperty("alpha"), "1");
        assertEquals(result.getProperty("beta"), "2");
        assertEquals(result.getProperty("gamma"), "3");
    }

    @Test
    public void syncMissingKeys_noopWhenNothingMissing() throws IOException {
        File target = writeTargetFile("nothingMissing.properties", TEMPLATE_CONTENT);
        long before = target.length();

        PropertyFileSynchronizer.syncMissingKeys(new File(TEMPLATE_JAR), JAR_ENTRY, target.getPath(), "9.9.9");

        assertEquals(target.length(), before);
    }

    @Test
    public void syncMissingKeys_noopWhenTargetFileMissing() {
        String missingPath = TEST_DIR + "doesNotExist.properties";
        PropertyFileSynchronizer.syncMissingKeys(new File(TEMPLATE_JAR), JAR_ENTRY, missingPath, "9.9.9");
        assertFalse(new File(missingPath).exists());
    }

    @Test
    public void syncMissingKeys_noopWhenJarFileNull() throws IOException {
        File target = writeTargetFile("nullJar.properties", "gamma=3\n");
        long before = target.length();

        PropertyFileSynchronizer.syncMissingKeys(null, JAR_ENTRY, target.getPath(), "9.9.9");

        assertEquals(target.length(), before);
    }

    @Test
    public void syncMissingKeys_noopWhenJarEntryMissing() throws IOException {
        File target = writeTargetFile("wrongEntry.properties", "gamma=3\n");
        long before = target.length();

        PropertyFileSynchronizer.syncMissingKeys(new File(TEMPLATE_JAR), "properties/does-not-exist.properties",
                target.getPath(), "9.9.9");

        assertEquals(target.length(), before);
    }
}
