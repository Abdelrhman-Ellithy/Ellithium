package ai;

import Ellithium.Utilities.interactions.BaseActions;
import Ellithium.core.ai.healing.AISelfHealer;
import Ellithium.core.ai.healing.BaselineStore;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration coverage for the BaseActions heal cascade wiring (T1 → ensemble → T4) — the
 * orchestration that the per-component unit tests never touch. Uses a mocked WebDriver so the
 * cascade runs end-to-end without a browser.
 */
public class CascadeHealingTest {

    /** Minimal concrete BaseActions to reach the protected constructor + public findWebElement. */
    static class TestActions extends BaseActions<WebDriver> {
        TestActions(WebDriver driver) { super(driver); }
    }

    @BeforeClass
    public void disableHealing() {
        // Defaults are already DISABLED + null provider (no initialize call needed); just ensure no
        // stray baseline from another test heals this locator.
        BaselineStore.clear();
    }

    @Test
    public void missingElement_noHealPossible_failsLoudlyNotSilently() {
        WebDriver driver = mock(WebDriver.class);
        when(driver.findElement(any(By.class)))
                .thenThrow(new NoSuchElementException("no such element"));
        lenient().when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());

        TestActions actions = new TestActions(driver);

        // No baseline + no model + DISABLED Tier 4 → every tier returns null → the cascade must
        // throw (test fails), NOT return null or a wrong element (which would silently pass).
        boolean threw = false;
        try {
            actions.findWebElement(By.id("doesNotExist"));
        } catch (AssertionError | RuntimeException expected) {
            threw = true;
        }
        Assert.assertTrue(threw,
                "cascade must fail loudly when no tier can heal — silently returning is the worst outcome");
    }

    @Test
    public void killSwitch_tier4NotInvokedWhenDisabled() {
        WebDriver driver = mock(WebDriver.class);
        // attemptHeal must short-circuit on DISABLED without touching the (null) provider.
        WebElement healed = AISelfHealer.attemptHeal(driver, By.id("anything"),
                Thread.currentThread().getStackTrace());
        Assert.assertNull(healed, "Tier 4 must be a no-op when strategy=DISABLED / provider=null");
    }
}
