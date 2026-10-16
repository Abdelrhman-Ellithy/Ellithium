package Ellithium.core.execution.Internal.Loader;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class PropertyFileSynchronizerTest {

    @Test
    public void testParseEntries_withCommentsAndBlankLines() {
        List<String> lines = List.of(
                "# Leading file header comment",
                "",
                "# Comment for key1",
                "! Alternative comment for key1",
                "app.name = Ellithium Test",
                "",
                "# Disconnected comment",
                "",
                "app.timeout=5000",
                "app.url=https://example.com?query=1&b=2",
                "invalid_line_without_equals"
        );

        List<PropertyFileSynchronizer.TemplateEntry> entries = PropertyFileSynchronizer.parseEntries(lines);
        Assert.assertEquals(entries.size(), 3);

        // First entry
        PropertyFileSynchronizer.TemplateEntry e1 = entries.get(0);
        Assert.assertEquals(e1.key(), "app.name");
        Assert.assertEquals(e1.commentLines().size(), 2);
        Assert.assertEquals(e1.commentLines().get(0), "# Comment for key1");
        Assert.assertEquals(e1.commentLines().get(1), "! Alternative comment for key1");
        Assert.assertEquals(e1.rawLine(), "app.name = Ellithium Test");

        // Second entry: blank line after disconnected comment means comment was reset
        PropertyFileSynchronizer.TemplateEntry e2 = entries.get(1);
        Assert.assertEquals(e2.key(), "app.timeout");
        Assert.assertTrue(e2.commentLines().isEmpty());
        Assert.assertEquals(e2.rawLine(), "app.timeout=5000");

        // Third entry: multiple equals in value
        PropertyFileSynchronizer.TemplateEntry e3 = entries.get(2);
        Assert.assertEquals(e3.key(), "app.url");
        Assert.assertEquals(e3.rawLine(), "app.url=https://example.com?query=1&b=2");
    }

    @Test
    public void testParseEntries_emptyInput() {
        List<PropertyFileSynchronizer.TemplateEntry> entries = PropertyFileSynchronizer.parseEntries(List.of());
        Assert.assertTrue(entries.isEmpty());
    }

    @Test
    public void testSyncMissingKeys_nullAndNonExistentGuards() {
        // Should not throw NullPointerException or any exception
        PropertyFileSynchronizer.syncMissingKeys(null, "some/path", "target/path", "3.3.0");
        PropertyFileSynchronizer.syncMissingKeys(new File("non_existent.jar"), "some/path", null, "3.3.0");
        PropertyFileSynchronizer.syncMissingKeys(new File("non_existent.jar"), "some/path", "target/path", "3.3.0");
    }

    @Test
    public void testSyncMissingKeys_appendsMissingOnly() throws IOException {
        Path tempDir = Files.createTempDirectory("props_sync_test_");
        Path targetFile = tempDir.resolve("my-config.properties");
        Path jarPath = tempDir.resolve("template.jar");

        try {
            // Existing target properties file has key1
            Files.writeString(targetFile, "existing.key=originalValue\n", StandardCharsets.UTF_8);

            // Create a temporary JAR with a bundled template file containing existing.key and new.feature.key
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
                JarEntry entry = new JarEntry("properties/template.properties");
                jos.putNextEntry(entry);
                String templateContent = """
                        # Existing key
                        existing.key=templateDefault
                        
                        # New feature setting
                        new.feature.key=enabledValue
                        """;
                jos.write(templateContent.getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }

            // Execute synchronization
            PropertyFileSynchronizer.syncMissingKeys(jarPath.toFile(), "properties/template.properties",
                    targetFile.toString(), "4.0.0");

            // Verify content
            String targetContent = Files.readString(targetFile, StandardCharsets.UTF_8);
            Assert.assertTrue(targetContent.contains("existing.key=originalValue"));
            Assert.assertTrue(targetContent.contains("new.feature.key=enabledValue"));
            Assert.assertTrue(targetContent.contains("# New feature setting"));
            Assert.assertTrue(targetContent.contains("Properties added automatically by Ellithium v4.0.0"));

            // Second sync when nothing is missing should no-op
            long lengthBefore = targetFile.toFile().length();
            PropertyFileSynchronizer.syncMissingKeys(jarPath.toFile(), "properties/template.properties",
                    targetFile.toString(), "4.0.0");
            Assert.assertEquals(targetFile.toFile().length(), lengthBefore);

        } finally {
            Files.deleteIfExists(targetFile);
            Files.deleteIfExists(jarPath);
            Files.deleteIfExists(tempDir);
        }
    }
}
