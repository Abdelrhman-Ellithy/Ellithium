package Ellithium.Utilities.helpers;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FilePolicyTest {

    private Path tempDir;

    @BeforeClass
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("file_policy_test_");
    }

    @AfterClass
    public void teardown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var s = Files.walk(tempDir)) {
                s.sorted(Comparator.reverseOrder())
                 .forEach(p -> {
                     try {
                         Files.deleteIfExists(p);
                     } catch (IOException ignored) {}
                 });
            }
        }
    }

    @Test
    public void testCanonicalEncodingIsUtf8() {
        Assert.assertEquals(FilePolicy.ENCODING, StandardCharsets.UTF_8, "FilePolicy canonical encoding must be UTF-8");
    }

    @Test
    public void testToPathAndRequirePath() {
        Path p = FilePolicy.toPath("src/main/resources/../resources");
        Assert.assertNotNull(p);
        Assert.assertEquals(p, FilePolicy.requirePath(p));

        Assert.assertThrows(IllegalArgumentException.class, () -> FilePolicy.toPath(null));
        Assert.assertThrows(IllegalArgumentException.class, () -> FilePolicy.toPath("   "));
        Assert.assertThrows(IllegalArgumentException.class, () -> FilePolicy.requirePath(null));
    }

    @Test
    public void testUtf8ReadWriteRoundTrip() throws IOException {
        Path testFile = tempDir.resolve("unicode_test.txt");
        String unicodeContent = "Testing UTF-8: 🌍 こんにちは العالم Special: © ® ™ \uD83D\uDE80";

        try (BufferedWriter writer = FilePolicy.newUtf8Writer(testFile)) {
            writer.write(unicodeContent);
            writer.newLine();
        }

        try (BufferedReader reader = FilePolicy.newUtf8Reader(testFile)) {
            String readBack = reader.readLine();
            Assert.assertEquals(readBack, unicodeContent, "Read content must match UTF-8 written content exactly");
        }
    }

    @Test
    public void testTextHelperPathOverloadsWithUtf8() {
        Path textFile = tempDir.resolve("text_helper_path.txt");
        List<String> lines = List.of(
                "Line 1: UTF-8 Test 🌟",
                "Line 2: Arabic النص العربي",
                "Line 3: CJK 中文测试"
        );

        TextHelper.writeTextFile(textFile, lines);
        Assert.assertFalse(TextHelper.isEmptyFile(textFile));
        Assert.assertEquals(TextHelper.countLinesInFile(textFile), 3);

        List<String> readLines = TextHelper.readTextFile(textFile);
        Assert.assertEquals(readLines, lines);

        TextHelper.appendLineToFile(textFile, "Line 4: Appended 🎉");
        Assert.assertEquals(TextHelper.countLinesInFile(textFile), 4);

        Path copyFile = tempDir.resolve("text_helper_copy.txt");
        TextHelper.copyTextFile(textFile, copyFile);
        Assert.assertTrue(TextHelper.areFilesIdentical(textFile, copyFile));
    }

    @Test
    public void testJsonHelperPathOverloadsWithUtf8() {
        Path jsonFile = tempDir.resolve("sample_utf8.json");
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> data = new HashMap<>();
        data.put("title", "Ellithium Automation 🚀");
        data.put("description", "Strict UTF-8 & Path I/O: العربية / 中文");
        data.put("version", "25");
        list.add(data);

        JsonHelper.setJsonData(jsonFile, list);
        Assert.assertTrue(JsonHelper.isValidJson(jsonFile));

        List<Map<String, String>> readData = JsonHelper.getJsonData(jsonFile);
        Assert.assertFalse(readData.isEmpty());
        Assert.assertEquals(readData.get(0).get("title"), "Ellithium Automation 🚀");

        Path kvFile = tempDir.resolve("kv_utf8.json");
        JsonHelper.setJsonKeyValue(kvFile, "framework", "Ellithium ⚡");
        Assert.assertEquals(JsonHelper.getJsonKeyValue(kvFile, "framework"), "Ellithium ⚡");
    }

    @Test
    public void testCsvHelperPathOverloadsWithUtf8() {
        Path csvFile = tempDir.resolve("sample_utf8.csv");
        List<Map<String, String>> data = new ArrayList<>();

        Map<String, String> row1 = new LinkedHashMap<>();
        row1.put("id", "1");
        row1.put("name", "User 1: Ahmed أحمد");
        row1.put("locale", "ar-EG");

        Map<String, String> row2 = new LinkedHashMap<>();
        row2.put("id", "2");
        row2.put("name", "User 2: Chen 陈");
        row2.put("locale", "zh-CN");

        data.add(row1);
        data.add(row2);

        CSVHelper.setCsvData(csvFile, data);
        Assert.assertTrue(CSVHelper.doesFileExist(csvFile));

        List<Map<String, String>> readRows = CSVHelper.getCsvData(csvFile);
        Assert.assertEquals(readRows.size(), 2);
        Assert.assertEquals(readRows.get(0).get("name"), "User 1: Ahmed أحمد");
        Assert.assertEquals(readRows.get(1).get("name"), "User 2: Chen 陈");
    }
}
