package AI;

import Ellithium.Utilities.ai.AISelfHealer;
import Ellithium.Utilities.ai.BaselineStore;
import Ellithium.Utilities.ai.SemanticQueryBuilder;
import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.models.ElementFingerprint;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * P0 correctness-gate tests — the safety mechanism that stops a low-confidence heal from
 * poisoning the baseline store or rewriting source with a wrong locator. These cover the exact
 * failure traced from the real run (Tier 3 picked the wrong &lt;h2&gt; for By.id: message and it
 * was committed to source over the verified Tier 4 By.id("flash")).
 */
public class HealingGateTest {

    // ── P0-2: baseline promotion gate ──

    @Test
    public void belowStoreThreshold_heal_isUsedButNotPersisted() {
        try (MockedStatic<AIConfigLoader> cfg = Mockito.mockStatic(AIConfigLoader.class, Mockito.CALLS_REAL_METHODS)) {
            cfg.when(AIConfigLoader::getHealingStoreThreshold).thenReturn(0.85);

            WebDriver driver = mock(WebDriver.class);
            WebElement element = mock(WebElement.class);
            By unique = By.id("gate-test-low-conf-" + System.nanoTime());

            // Tier 3 heal at 0.55 — below the 0.85 store bar.
            BaselineStore.capture(driver, unique, element, 0.55, 3);

            // Must NOT have been persisted, and the gate must short-circuit before any DOM read.
            Assert.assertNull(BaselineStore.getBaseline(unique.toString()),
                    "Below-threshold heal must not be promoted to a stored baseline");
            verifyNoInteractions(element);
        }
    }

    // ── P0-3: source-patch conflict resolution ──

    @Test
    public void patchConflict_higherConfidenceWins_regardlessOfOrder() {
        // Same file + field healed twice in one run: Tier 3 guess (h2, 0.55) vs Tier 4 LLM (flash, 0.95).
        AISelfHealer.SourcePatch lowTier3 = new AISelfHealer.SourcePatch(
                "SecureAreaPage.java", "getLoginMessage", "id", "message",
                "By.tagName(\"h2\")", 0.55, 3);
        AISelfHealer.SourcePatch highTier4 = new AISelfHealer.SourcePatch(
                "SecureAreaPage.java", "getLoginMessage", "id", "message",
                "By.id(\"flash\")", 0.95, 4);

        // Low queued first, then high.
        Map<String, AISelfHealer.SourcePatch> r1 =
                AISelfHealer.resolvePatchConflicts(Arrays.asList(lowTier3, highTier4));
        Assert.assertEquals(r1.size(), 1, "Conflicting patches must collapse to one");
        Assert.assertEquals(r1.values().iterator().next().newLocatorExpression, "By.id(\"flash\")",
                "Higher-confidence Tier 4 patch must win");

        // High queued first, then low — order must not matter.
        Map<String, AISelfHealer.SourcePatch> r2 =
                AISelfHealer.resolvePatchConflicts(Arrays.asList(highTier4, lowTier3));
        Assert.assertEquals(r2.values().iterator().next().newLocatorExpression, "By.id(\"flash\")",
                "Higher-confidence patch must win regardless of queue order");
    }

    @Test
    public void patchConflict_distinctTargets_areKeptSeparately() {
        AISelfHealer.SourcePatch a = new AISelfHealer.SourcePatch(
                "LoginPage.java", "clickLoginBtn", "id", "button", "By.cssSelector(\"button.radius\")", 0.9, 1);
        AISelfHealer.SourcePatch b = new AISelfHealer.SourcePatch(
                "SecureAreaPage.java", "getLoginMessage", "id", "message", "By.id(\"flash\")", 0.95, 4);

        Map<String, AISelfHealer.SourcePatch> r =
                AISelfHealer.resolvePatchConflicts(Arrays.asList(a, b));
        Assert.assertEquals(r.size(), 2, "Patches for different fields must not be merged");
    }

    // ── P0-4: query builder must not poison READABLE queries with last-known text ──

    @Test
    public void readableQuery_dropsLastKnownText() {
        ElementFingerprint fp = mock(ElementFingerprint.class);
        when(fp.getText()).thenReturn("Login Page");   // the text that wrongly pulled toward the <h2>
        when(fp.getTagName()).thenReturn("h2");

        String query = SemanticQueryBuilder.buildFromContext(
                "getText", "By.id: message", "getLoginMessage", fp);

        Assert.assertFalse(query.contains("login page"),
                "READABLE query must not inject last-known rendered text — got: " + query);
    }

    @Test
    public void nonReadableQuery_keepsLastKnownText() {
        ElementFingerprint fp = mock(ElementFingerprint.class);
        when(fp.getText()).thenReturn("Sign In");

        String query = SemanticQueryBuilder.buildFromContext(
                "clickOnElement", "By.id: loginBtn", "clickLoginButton", fp);

        Assert.assertTrue(query.contains("sign in"),
                "Non-READABLE query should still use last-known text — got: " + query);
    }
}
