package Ellithium.core.ai.healing;

import Ellithium.core.ai.config.AIConfigLoader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SourcePatchQueueTest {

    @Test
    public void testResolvePatchConflicts_KeepsHighestConfidence() {
        ConcurrentLinkedQueue<AISelfHealer.SourcePatch> queue = new ConcurrentLinkedQueue<>();
        SourcePatchQueue spq = new SourcePatchQueue(queue);

        AISelfHealer.SourcePatch p1 = new AISelfHealer.SourcePatch(
                "LoginPage.java", "submitBtn", "id", "submit", "By.id(\"submit_new\")", 0.80, 2);
        AISelfHealer.SourcePatch p2 = new AISelfHealer.SourcePatch(
                "LoginPage.java", "submitBtn", "id", "submit", "By.cssSelector(\"#submit_best\")", 0.95, 2);

        Map<String, AISelfHealer.SourcePatch> resolved = spq.resolvePatchConflicts(List.of(p1, p2));
        Assert.assertEquals(resolved.size(), 1);
        AISelfHealer.SourcePatch winner = resolved.values().iterator().next();
        Assert.assertEquals(winner.newLocatorExpression, "By.cssSelector(\"#submit_best\")");
        Assert.assertEquals(winner.confidence, 0.95);
    }

    @Test
    public void testApply_InCIMode_ExportsProposals() throws Exception {
        ConcurrentLinkedQueue<AISelfHealer.SourcePatch> queue = new ConcurrentLinkedQueue<>();
        SourcePatchQueue spq = new SourcePatchQueue(queue);

        AISelfHealer.SourcePatch patch = new AISelfHealer.SourcePatch(
                "src/test/java/Pages/LoginPage.java", "loginButton", "id", "old-login",
                "By.id(\"new-login-btn\")", 0.92, 2);
        spq.queue(patch);

        // Apply will trigger proposal export since default execution mode is CI
        spq.apply();

        Path jsonManifest = Paths.get("Test-Output", "healing-proposals", "locator-patches.json");
        Path patchFile = Paths.get("Test-Output", "healing-proposals", "healing-locators.patch");

        Assert.assertTrue(Files.exists(jsonManifest), "locator-patches.json must be generated");
        Assert.assertTrue(Files.exists(patchFile), "healing-locators.patch must be generated");

        String jsonContent = Files.readString(jsonManifest);
        Assert.assertTrue(jsonContent.contains("new-login-btn"));
        Assert.assertTrue(jsonContent.contains("0.920"));

        String diffContent = Files.readString(patchFile);
        Assert.assertTrue(diffContent.contains("LoginPage.java"));
        Assert.assertTrue(diffContent.contains("new-login-btn"));
    }

    @Test
    public void testApply_MultipleRuns_AccumulatesProposals() throws Exception {
        ConcurrentLinkedQueue<AISelfHealer.SourcePatch> queue = new ConcurrentLinkedQueue<>();
        SourcePatchQueue spq = new SourcePatchQueue(queue);

        AISelfHealer.SourcePatch patch1 = new AISelfHealer.SourcePatch(
                "src/test/java/Pages/LoginPage.java", "userField", "id", "old-user",
                "By.id(\"new-user-input\")", 0.91, 2);
        spq.queue(patch1);
        spq.apply();

        AISelfHealer.SourcePatch patch2 = new AISelfHealer.SourcePatch(
                "src/test/java/Pages/DashboardPage.java", "logoutBtn", "id", "old-logout",
                "By.id(\"new-logout-btn\")", 0.94, 2);
        spq.queue(patch2);
        spq.apply();

        Path jsonManifest = Paths.get("Test-Output", "healing-proposals", "locator-patches.json");
        Path patchFile = Paths.get("Test-Output", "healing-proposals", "healing-locators.patch");

        Assert.assertTrue(Files.exists(jsonManifest));
        Assert.assertTrue(Files.exists(patchFile));

        String jsonContent = Files.readString(jsonManifest);
        Assert.assertTrue(jsonContent.contains("new-user-input"), "Must retain first patch");
        Assert.assertTrue(jsonContent.contains("new-logout-btn"), "Must retain second patch");

        String diffContent = Files.readString(patchFile);
        Assert.assertTrue(diffContent.contains("LoginPage.java"), "Diff must include first file");
        Assert.assertTrue(diffContent.contains("DashboardPage.java"), "Diff must include second file");
    }

    @Test
    public void testResolvePatchConflicts_DifferentFieldNamesInSameFile_DoNotCollide() {
        ConcurrentLinkedQueue<AISelfHealer.SourcePatch> queue = new ConcurrentLinkedQueue<>();
        SourcePatchQueue spq = new SourcePatchQueue(queue);

        AISelfHealer.SourcePatch p1 = new AISelfHealer.SourcePatch(
                "LoginPage.java", "usernameField", "id", "user", "By.id(\"user_new\")", 0.90, 2);
        AISelfHealer.SourcePatch p2 = new AISelfHealer.SourcePatch(
                "LoginPage.java", "passwordField", "id", "pass", "By.id(\"pass_new\")", 0.90, 2);

        Map<String, AISelfHealer.SourcePatch> resolved = spq.resolvePatchConflicts(List.of(p1, p2));
        Assert.assertEquals(resolved.size(), 2, "Different fields in the same file must not conflict with each other");
    }

    @Test
    public void testApply_NormalizesBackslashesToForwardSlashesInPatch() throws Exception {
        ConcurrentLinkedQueue<AISelfHealer.SourcePatch> queue = new ConcurrentLinkedQueue<>();
        SourcePatchQueue spq = new SourcePatchQueue(queue);

        AISelfHealer.SourcePatch patch = new AISelfHealer.SourcePatch(
                "src\\test\\java\\Pages\\SettingsPage.java", "saveBtn", "id", "save",
                "By.id(\"new-save\")", 0.95, 2);
        spq.queue(patch);
        spq.apply();

        Path patchFile = Paths.get("Test-Output", "healing-proposals", "healing-locators.patch");
        String diffContent = Files.readString(patchFile);
        Assert.assertTrue(diffContent.contains("--- a/src/test/java/Pages/SettingsPage.java"),
                "Diff path must normalize backslashes to forward slashes: " + diffContent);
    }
}
