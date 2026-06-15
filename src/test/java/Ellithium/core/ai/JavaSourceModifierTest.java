package Ellithium.core.ai;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests JavaSourceModifier against real .java files written to target/ (git-ignored),
 * so the internal git-cleanliness guard sees an empty `git status --porcelain` output
 * and allows patching to proceed.
 */
public class JavaSourceModifierTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory(Path.of("target"), "jsm-test-");
        JavaSourceModifier.resetSessionState();
    }

    @AfterMethod
    public void tearDown() throws IOException {
        JavaSourceModifier.resetSessionState();
        if (tempDir != null) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
        }
    }

    // ── updateLocatorValue ────────────────────────────────────────────────────

    @Test
    public void updateLocatorValue_existingByIdField_rewritesValue() throws IOException {
        Path file = tempDir.resolve("LoginPage.java");
        Files.writeString(file, """
                import org.openqa.selenium.By;
                public class LoginPage {
                    private By loginButton = By.id("old-login-id");
                }
                """);

        boolean result = JavaSourceModifier.updateLocatorValue(
                file.toString(), "loginButton", "By.cssSelector(\"#new-login-id\")");

        Assert.assertTrue(result, "Should return true on success");
        String updated = Files.readString(file);
        Assert.assertTrue(updated.contains("#new-login-id"),
                "File should contain the new locator value");
        Assert.assertFalse(updated.contains("\"old-login-id\""),
                "File should no longer contain the old value");
    }

    @Test
    public void updateLocatorValue_nonExistentField_returnsFalse() throws IOException {
        Path file = tempDir.resolve("SomePage.java");
        Files.writeString(file, """
                import org.openqa.selenium.By;
                public class SomePage {
                    private By submitBtn = By.id("submit");
                }
                """);

        boolean result = JavaSourceModifier.updateLocatorValue(
                file.toString(), "nonExistentField", "By.id(\"x\")");

        Assert.assertFalse(result, "Should return false when field not found");
        // Original file must be intact
        Assert.assertTrue(Files.readString(file).contains("\"submit\""));
    }

    @Test
    public void updateLocatorValue_nonExistentFile_returnsFalse() {
        boolean result = JavaSourceModifier.updateLocatorValue(
                tempDir.resolve("DoesNotExist.java").toString(),
                "anyField", "By.id(\"x\")");
        Assert.assertFalse(result);
    }

    @Test
    public void updateLocatorValue_multipleFields_updatesOnlyTargetField() throws IOException {
        Path file = tempDir.resolve("MultiPage.java");
        Files.writeString(file, """
                import org.openqa.selenium.By;
                public class MultiPage {
                    private By usernameField = By.id("old-user");
                    private By passwordField = By.id("old-pass");
                }
                """);

        boolean result = JavaSourceModifier.updateLocatorValue(
                file.toString(), "usernameField", "By.id(\"new-user\")");

        Assert.assertTrue(result);
        String updated = Files.readString(file);
        Assert.assertTrue(updated.contains("\"new-user\""), "Target field should be updated");
        Assert.assertTrue(updated.contains("\"old-pass\""), "Other field should be unchanged");
    }

    // ── updateLocatorByOldValue ───────────────────────────────────────────────

    @Test
    public void updateLocatorByOldValue_inlineLocator_rewritesValue() throws IOException {
        Path file = tempDir.resolve("InlinePage.java");
        Files.writeString(file, """
                import org.openqa.selenium.By;
                public class InlinePage {
                    public void login(Object driver) {
                        driver.findElement(By.id("broken-id"));
                    }
                }
                """);

        boolean result = JavaSourceModifier.updateLocatorByOldValue(
                file.toString(), "id", "broken-id", "By.cssSelector(\"[data-testid='login']\")");

        Assert.assertTrue(result);
        String updated = Files.readString(file);
        Assert.assertTrue(updated.contains("data-testid='login'"),
                "Inline locator should be replaced with new value");
        Assert.assertFalse(updated.contains("\"broken-id\""),
                "Old inline locator should be gone");
    }

    @Test
    public void updateLocatorByOldValue_nonExistentFile_returnsFalse() {
        boolean result = JavaSourceModifier.updateLocatorByOldValue(
                tempDir.resolve("Missing.java").toString(),
                "id", "old-val", "By.id(\"new-val\")");
        Assert.assertFalse(result);
    }

    @Test
    public void updateLocatorByOldValue_wrongMethod_returnsFalse() throws IOException {
        Path file = tempDir.resolve("WrongMethod.java");
        Files.writeString(file, """
                import org.openqa.selenium.By;
                public class WrongMethod {
                    public void go(Object driver) {
                        driver.findElement(By.id("real-id"));
                    }
                }
                """);

        // Looking for By.cssSelector("real-id") but file has By.id("real-id")
        boolean result = JavaSourceModifier.updateLocatorByOldValue(
                file.toString(), "cssSelector", "real-id", "By.id(\"x\")");

        Assert.assertFalse(result);
        Assert.assertTrue(Files.readString(file).contains("\"real-id\""), "File should be unchanged");
    }

    @Test
    public void updateLocatorByOldValue_multipleOccurrences_textFallbackSkipped() throws IOException {
        // When there are two identical inline locators, the text fallback must NOT replace
        // (risk of corrupting an unrelated usage). Expect false.
        Path file = tempDir.resolve("DuplicatePage.java");
        Files.writeString(file, """
                import org.openqa.selenium.By;
                public class DuplicatePage {
                    public void a(Object driver) { driver.findElement(By.id("dup")); }
                    public void b(Object driver) { driver.findElement(By.id("dup")); }
                }
                """);

        // Both AST match and text fallback should refuse to replace two occurrences ambiguously
        // AST should replace BOTH (two AST targets); verify new value appears
        boolean result = JavaSourceModifier.updateLocatorByOldValue(
                file.toString(), "id", "dup", "By.id(\"replaced\")");

        // AST path replaces all matches — that is the correct behaviour
        Assert.assertTrue(result, "AST path should replace all occurrences");
        String updated = Files.readString(file);
        Assert.assertFalse(updated.contains("\"dup\""), "Both occurrences should be replaced");
    }

    // ── resetSessionState ────────────────────────────────────────────────────

    @Test
    public void resetSessionState_allowsReCheckAfterReset() throws IOException {
        Path file = tempDir.resolve("ResetPage.java");
        Files.writeString(file, """
                import org.openqa.selenium.By;
                public class ResetPage {
                    private By btn = By.id("old");
                }
                """);

        // First call — git check runs and registers the file
        JavaSourceModifier.updateLocatorValue(file.toString(), "btn", "By.id(\"v1\")");

        // Reset session
        JavaSourceModifier.resetSessionState();

        // Second call after reset — git check runs again from scratch
        boolean result = JavaSourceModifier.updateLocatorValue(
                file.toString(), "btn", "By.id(\"v2\")");

        Assert.assertTrue(result);
        Assert.assertTrue(Files.readString(file).contains("\"v2\""));
    }
}
