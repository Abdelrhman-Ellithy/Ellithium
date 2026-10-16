package Ellithium.core.ai.readers;

import Ellithium.core.ai.models.TestCaseSource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestCaseReaderTest {

    private Path tempDir;

    @BeforeClass
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("ellithium_readers_test");
    }

    @AfterClass
    public void tearDown() {
        if (tempDir != null) {
            try {
                Files.walk(tempDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception ignored) {}
        }
    }

    // ── JsonTestCaseReader Tests ─────────────────────────────────────────────

    @Test
    public void testJsonReader_ValidCases() throws IOException {
        String json = """
                [
                  {
                    "testId": "TC-01",
                    "description": "Login with valid credentials",
                    "targetUrl": "https://example.com/login"
                  },
                  {
                    "testId": "TC-02",
                    "description": "Select option from dropdown"
                  }
                ]
                """;
        Path jsonFile = tempDir.resolve("test_cases.json");
        Files.writeString(jsonFile, json);

        JsonTestCaseReader reader = new JsonTestCaseReader();
        List<TestCaseSource> cases = reader.read(jsonFile.toString());

        Assert.assertEquals(cases.size(), 2);
        Assert.assertEquals(cases.get(0).getTestId(), "TC-01");
        Assert.assertEquals(cases.get(0).getDescription(), "Login with valid credentials");
        Assert.assertEquals(cases.get(0).getTargetUrl(), "https://example.com/login");

        Assert.assertEquals(cases.get(1).getTestId(), "TC-02");
        Assert.assertEquals(cases.get(1).getDescription(), "Select option from dropdown");
        Assert.assertNull(cases.get(1).getTargetUrl());
    }

    @Test
    public void testJsonReader_EmptyAndMissingFile() {
        JsonTestCaseReader reader = new JsonTestCaseReader();
        List<TestCaseSource> empty = reader.read("non_existent_file_12345.json");
        Assert.assertNotNull(empty);
        Assert.assertTrue(empty.isEmpty());
    }

    // ── TextTestCaseReader Tests ─────────────────────────────────────────────

    @Test
    public void testTextReader_WithExplicitIdsAndUrls() throws IOException {
        String text = """
                ID: TC-AUTH-01
                URL: https://example.com/login
                Enter username and password, then click Submit.
                
                ID: TC-AUTH-02
                Verify logout button redirects to login page.
                """;
        Path textFile = tempDir.resolve("test_cases.txt");
        Files.writeString(textFile, text);

        TextTestCaseReader reader = new TextTestCaseReader();
        List<TestCaseSource> cases = reader.read(textFile.toString());

        Assert.assertEquals(cases.size(), 2);
        Assert.assertEquals(cases.get(0).getTestId(), "TC-AUTH-01");
        Assert.assertEquals(cases.get(0).getTargetUrl(), "https://example.com/login");
        Assert.assertTrue(cases.get(0).getDescription().contains("Enter username and password"));

        Assert.assertEquals(cases.get(1).getTestId(), "TC-AUTH-02");
        Assert.assertNull(cases.get(1).getTargetUrl());
        Assert.assertTrue(cases.get(1).getDescription().contains("Verify logout button"));
    }

    @Test
    public void testTextReader_NoExplicitIds_FallsBackToSingleCase() throws IOException {
        String text = """
                URL: https://example.com/checkout
                Complete checkout with credit card payment and verify order number.
                """;
        Path textFile = tempDir.resolve("single_case.txt");
        Files.writeString(textFile, text);

        TextTestCaseReader reader = new TextTestCaseReader();
        List<TestCaseSource> cases = reader.read(textFile.toString());

        Assert.assertEquals(cases.size(), 1);
        Assert.assertEquals(cases.get(0).getTestId(), "TC-TEXT-1");
        Assert.assertEquals(cases.get(0).getTargetUrl(), "https://example.com/checkout");
        Assert.assertTrue(cases.get(0).getDescription().contains("Complete checkout"));
    }

    @Test
    public void testTextReader_EmptyAndWhitespace() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.writeString(emptyFile, "   \n\n   ");

        TextTestCaseReader reader = new TextTestCaseReader();
        List<TestCaseSource> cases = reader.read(emptyFile.toString());
        Assert.assertTrue(cases.isEmpty());

        List<TestCaseSource> missing = reader.read("missing_file_xyz.txt");
        Assert.assertTrue(missing.isEmpty());
    }
}
