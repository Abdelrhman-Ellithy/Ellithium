package Ellithium.core.ai.generators;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PomClassGeneratorTest {

    private Path tempDir;

    @BeforeClass
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("pom-gen-test");
    }

    @AfterClass
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    public void createPomClass_createsValidJavaFile() throws IOException {
        Path output = tempDir.resolve("pages/LoginPage.java");
        List<String> fields = List.of(
                "private final By usernameInput = By.id(\"user\");",
                "private final By passwordInput = By.id(\"pass\");"
        );
        List<String> methods = List.of(
                "public LoginPage typeUsername(String user) {\n    driverActions.elements().sendData(usernameInput, user);\n    return this;\n}"
        );

        boolean created = PomClassGenerator.createPomClass(
                output.toString(),
                "pages",
                "LoginPage",
                fields,
                methods
        );

        Assert.assertTrue(created, "POM class creation should succeed");
        Assert.assertTrue(Files.exists(output), "Generated file must exist");
        String content = Files.readString(output);
        Assert.assertTrue(content.contains("package pages;"));
        Assert.assertTrue(content.contains("public class LoginPage {"));
        Assert.assertTrue(content.contains("private final DriverActions<?> driverActions;"));
        Assert.assertTrue(content.contains("private final By usernameInput = By.id(\"user\");"));
        Assert.assertTrue(content.contains("public LoginPage typeUsername(String user)"));
    }

    @Test
    public void createPomClass_rejectsMalformedSyntax() {
        Path output = tempDir.resolve("pages/MalformedPage.java");
        List<String> fields = List.of("private final By broken = By.id("); // Syntax error: unclosed parenthesis
        List<String> methods = List.of("public brokenMethod() {");

        boolean created = PomClassGenerator.createPomClass(
                output.toString(),
                "pages",
                "MalformedPage",
                fields,
                methods
        );

        Assert.assertFalse(created, "POM class creation with syntax errors must be rejected");
        Assert.assertFalse(Files.exists(output), "Malformed file must not be written to disk");
    }

    @Test
    public void injectIntoExistingPom_addsLocatorAndMethodCorrectly() throws IOException {
        Path pomFile = tempDir.resolve("pages/DashboardPage.java");
        Files.createDirectories(pomFile.getParent());
        String initialSource = """
                package pages;
                import Ellithium.Utilities.interactions.DriverActions;
                import org.openqa.selenium.By;
                import org.openqa.selenium.WebDriver;

                public class DashboardPage {
                    private final DriverActions<?> driverActions;
                    public DashboardPage(WebDriver driver) {
                        this.driverActions = new DriverActions<>(driver);
                    }
                }
                """;
        Files.writeString(pomFile, initialSource);

        boolean injected = PomClassGenerator.injectIntoExistingPom(
                pomFile.toString(),
                "private final By logoutBtn = By.id(\"logout\");",
                "public DashboardPage clickLogout()",
                "driverActions.elements().clickOnElement(logoutBtn); return this;"
        );

        Assert.assertTrue(injected, "Injection into POM class should succeed");
        String updated = Files.readString(pomFile);
        Assert.assertTrue(updated.contains("private final By logoutBtn = By.id(\"logout\");"));
        Assert.assertTrue(updated.contains("public DashboardPage clickLogout()"));
        Assert.assertTrue(updated.contains("driverActions.elements().clickOnElement(logoutBtn);"));
    }

    @Test
    public void injectIntoExistingPom_returnsFalseWhenFileNotFound() {
        boolean result = PomClassGenerator.injectIntoExistingPom(
                tempDir.resolve("non_existent_page.java").toString(),
                "private final By btn = By.id(\"x\");",
                "public void clickX()",
                "return;"
        );
        Assert.assertFalse(result, "Injection should return false for non-existent file");
    }

    @Test
    public void injectIntoExistingPom_returnsFalseWhenFileHasNoClass() throws IOException {
        Path invalidFile = tempDir.resolve("pages/EmptyFile.java");
        Files.createDirectories(invalidFile.getParent());
        Files.writeString(invalidFile, "package pages;\n// No class declaration here\n");

        boolean result = PomClassGenerator.injectIntoExistingPom(
                invalidFile.toString(),
                "private final By btn = By.id(\"x\");",
                "public void clickX()",
                "return;"
        );
        Assert.assertFalse(result, "Injection should return false when file contains no class");
    }

    @Test
    public void injectIntoExistingPom_returnsFalseOnSyntaxError() throws IOException {
        Path pomFile = tempDir.resolve("pages/SyntaxTestPage.java");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, "package pages; public class SyntaxTestPage {}");

        boolean result = PomClassGenerator.injectIntoExistingPom(
                pomFile.toString(),
                "private final By badField =", // Syntax error
                "public invalidMethod(", // Syntax error
                "bad body"
        );
        Assert.assertFalse(result, "Injection should return false when parsed snippet has syntax errors");
    }
}
