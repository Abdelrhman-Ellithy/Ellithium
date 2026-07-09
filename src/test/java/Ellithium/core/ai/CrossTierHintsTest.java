package Ellithium.core.ai;

import Ellithium.core.ai.healing.BaselineStore;
import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.models.HealingHints;
import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.scoring.LocatorMutationEngine;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cross-tier candidate handoff ({@link HealingHints}): a Tier-1 abstain must hand its near-miss
 * finalists to the next tier instead of discarding them, and the voting winner path must stay
 * hint-free (nothing to hand off when Tier 1 succeeds).
 */
public class CrossTierHintsTest {

    @BeforeMethod
    public void reset() {
        BaselineStore.clear();
        LocatorMutationEngine.resetCache();
    }

    private WebDriver mockDriverWithContainsMatches(List<WebElement> containsMatches) {
        WebDriver driver = mock(WebDriver.class);
        when(driver.findElement(any(By.class)))
                .thenThrow(new NoSuchElementException("no such element"));
        lenient().when(driver.findElements(any(By.class))).thenReturn(containsMatches);
        return driver;
    }

    @Test
    public void tokenVoteTie_abstainsAndHandsFinalistsToHints() {
        WebElement formEmail = mock(WebElement.class);
        WebElement emailInput = mock(WebElement.class);
        WebDriver driver = mockDriverWithContainsMatches(List.of(formEmail, emailInput));

        HealingHints hints = new HealingHints();
        WebElement healed = LocatorMutationEngine.tryMutations(By.id("email"), driver, null, hints);

        Assert.assertNull(healed, "a 2-way token-vote tie must abstain, never pick DOM order");
        Assert.assertEquals(hints.size(), 2,
                "both tie finalists must be handed to the next tier via hints");
        for (HealingHints.CandidateHint h : hints.shortlist()) {
            Assert.assertEquals(h.sourceTier(), 1);
            Assert.assertTrue(h.evidence().contains("token-vote tie"),
                    "hint must carry the tie evidence; got: " + h.evidence());
        }
    }

    @Test
    public void uniqueTokenMatch_winsVoting_noHintsEmitted() {
        WebElement only = mock(WebElement.class);
        WebDriver driver = mockDriverWithContainsMatches(List.of(only));

        HealingHints hints = new HealingHints();
        WebElement healed = LocatorMutationEngine.tryMutations(By.id("email"), driver, null, hints);

        Assert.assertSame(healed, only, "a unique contains match must heal via voting");
        Assert.assertTrue(hints.isEmpty(), "a successful Tier-1 heal hands nothing off");
    }

    @Test
    public void coldStartAbstain_populatesHintsThroughBaselineStore() {
        WebElement a = mock(WebElement.class);
        WebElement b = mock(WebElement.class);
        WebDriver driver = mockDriverWithContainsMatches(List.of(a, b));

        HealingHints hints = new HealingHints();
        HealOutcome outcome = BaselineStore.tryAlgorithmicHeal(driver, By.id("email"),
                Thread.currentThread().getStackTrace(), "clickOnElement", hints);

        Assert.assertNull(outcome, "cold-start tie must fall through Tier 1");
        Assert.assertEquals(hints.size(), 2,
                "Tier 1's abstain must hand the tie finalists downstream");
    }

    @Test
    public void hints_dedupAndCap() {
        HealingHints hints = new HealingHints();
        WebElement same = mock(WebElement.class);
        hints.add(same, null, 0.5, "first", 1);
        hints.add(same, null, 0.9, "duplicate", 1);
        Assert.assertEquals(hints.size(), 1, "the same element must not be added twice");

        for (int i = 0; i < 10; i++) {
            hints.add(mock(WebElement.class), null, 0.1, "filler", 1);
        }
        Assert.assertEquals(hints.size(), 5, "shortlist must stay capped so later tiers stay O(1)");
    }

    @Test
    public void healingRequest_alwaysCarriesHints() {
        WebDriver driver = mock(WebDriver.class);
        lenient().when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
        HealingRequest legacy = new HealingRequest(driver, By.id("x"),
                Thread.currentThread().getStackTrace(), "clickOnElement", "caller",
                null, "x", null);
        Assert.assertNotNull(legacy.hints(), "8-arg constructor must create empty hints");
        HealingRequest explicitNull = new HealingRequest(driver, By.id("x"),
                Thread.currentThread().getStackTrace(), "clickOnElement", "caller",
                null, "x", null, null);
        Assert.assertNotNull(explicitNull.hints(), "canonical constructor must null-guard hints");
    }
}
