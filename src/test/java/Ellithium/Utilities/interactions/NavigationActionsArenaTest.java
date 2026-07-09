package Ellithium.Utilities.interactions;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Real-browser integration tests for {@link NavigationActions} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code navigation.html}. All element interactions use Ellithium
 * {@link ElementActions} and {@link WaitActions}.
 * Complements (does not replace) {@link NavigationActionsTest}.
 */
public class NavigationActionsArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetPage() {
        goTo("navigation.html");
    }

    // ── 1. navigateToUrl ──────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void navigateToUrl_goesToTargetPage_urlUpdates() {
        navActions.navigateToUrl(BASE_URL + "forms.html");
        boolean result = waitActions.waitForUrlContains("forms.html", SHORT, POLL);
        Assert.assertTrue(result, "URL should contain 'forms.html' after navigateToUrl");
    }

    @Test(groups = "arena")
    public void navigateToUrl_titleMatchesTargetPage() {
        navActions.navigateToUrl(BASE_URL + "waits.html");
        waitActions.waitForTitleContains("Wait", SHORT, POLL);
        boolean ok = waitActions.waitForTitleContains("Wait", SHORT, POLL);
        Assert.assertTrue(ok, "Title should contain 'Wait' after navigating to waits.html");
    }

    // ── 2. navigateBack ───────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void navigateBack_returnsToNavigationHtml() {
        // Go forward to forms.html
        navActions.navigateToUrl(BASE_URL + "forms.html");
        waitActions.waitForUrlContains("forms.html", SHORT, POLL);
        // Then go back
        navActions.navigateBack();
        boolean result = waitActions.waitForUrlContains("navigation.html", SHORT, POLL);
        Assert.assertTrue(result, "Back navigation must restore navigation.html in URL");
    }

    // ── 3. navigateForward ────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void navigateForward_afterBack_advancesToPreviousPage() {
        navActions.navigateToUrl(BASE_URL + "forms.html");
        waitActions.waitForUrlContains("forms.html", SHORT, POLL);
        navActions.navigateBack();
        waitActions.waitForUrlContains("navigation.html", SHORT, POLL);
        navActions.navigateForward();
        boolean result = waitActions.waitForUrlContains("forms.html", SHORT, POLL);
        Assert.assertTrue(result, "Forward navigation must reach forms.html again");
    }

    // ── 4. refreshPage ────────────────────────────────────────────────────────

    @Test(groups = "arena")
    public void refreshPage_staysOnSamePage_urlUnchanged() {
        String urlBefore = driver.getCurrentUrl();
        navActions.refreshPage();
        boolean same = waitActions.waitForUrlContains("navigation.html", SHORT, POLL);
        Assert.assertTrue(same, "After refresh, URL should still contain 'navigation.html'");
        Assert.assertEquals(driver.getCurrentUrl(), urlBefore,
                "URL must not change after page refresh");
    }

    // ── 5. waitForTitleContains — 3s delayed title change ────────────────────

    @Test(groups = "arena")
    public void waitForTitleContains_waitsForDelayedTitleChange() {
        // The arena page changes title after 3s when button is clicked
        elementActions.clickOnElement(org.openqa.selenium.By.id("change-title-btn"), SHORT, POLL);
        // Title changes after 3s — MEDIUM (10s) is more than enough
        boolean result = waitActions.waitForTitleContains("READY", MEDIUM, POLL);
        Assert.assertTrue(result, "Page title should contain 'READY' after 3s delay");
    }

    @Test(groups = "arena")
    public void waitForTitleIs_exactMatchAfterTitleReset() {
        // Reset to original title using the reset button
        elementActions.clickOnElement(org.openqa.selenium.By.id("reset-title-btn"), SHORT, POLL);
        boolean result = waitActions.waitForTitleIs(
                "Navigation Actions — Ellithium Arena", SHORT, POLL);
        Assert.assertTrue(result, "Title should exactly match after reset");
    }

    // ── 6. waitForUrlContains — redirect chain ────────────────────────────────

    @Test(groups = "arena")
    public void waitForUrlContains_afterRedirectChain_landsCookiesHtml() {
        // Clicking start-redirect-btn triggers a JS redirect to cookies.html after 3s
        elementActions.clickOnElement(org.openqa.selenium.By.id("start-redirect-btn"), SHORT, POLL);
        boolean result = waitActions.waitForUrlContains("cookies.html", MEDIUM, POLL);
        Assert.assertTrue(result, "Should redirect to cookies.html after delay; current URL: "
                + driver.getCurrentUrl());
    }

    // ── 7. Hash anchor navigation ─────────────────────────────────────────────

    @Test(groups = "arena")
    public void hashNavigation_scrollsToAlphaSection() {
        goTo("navigation.html");
        elementActions.clickOnElement(org.openqa.selenium.By.id("scroll-to-alpha"), SHORT, POLL);
        boolean result = waitActions.waitForUrlContains("#hash-target-alpha", SHORT, POLL);
        Assert.assertTrue(result, "URL should include hash anchor #hash-target-alpha after click");
    }

    @Test(groups = "arena")
    public void hashNavigation_scrollsToBetaSection() {
        goTo("navigation.html");
        elementActions.clickOnElement(org.openqa.selenium.By.id("scroll-to-beta"), SHORT, POLL);
        boolean result = waitActions.waitForUrlContains("#hash-target-beta", SHORT, POLL);
        Assert.assertTrue(result, "URL should include hash anchor #hash-target-beta after click");
    }

    // ── 8. Multi-page journey ─────────────────────────────────────────────────

    @Test(groups = "arena")
    public void multiPageJourney_navigateThroughAllPages_urlAssertAtEach() {
        navActions.navigateToUrl(BASE_URL + "waits.html");
        Assert.assertTrue(waitActions.waitForUrlContains("waits.html", SHORT, POLL));

        navActions.navigateToUrl(BASE_URL + "state.html");
        Assert.assertTrue(waitActions.waitForUrlContains("state.html", SHORT, POLL));

        navActions.navigateToUrl(BASE_URL + "forms.html");
        Assert.assertTrue(waitActions.waitForUrlContains("forms.html", SHORT, POLL));

        navActions.navigateBack();
        Assert.assertTrue(waitActions.waitForUrlContains("state.html", SHORT, POLL),
                "Back from forms.html should restore state.html");

        navActions.navigateForward();
        Assert.assertTrue(waitActions.waitForUrlContains("forms.html", SHORT, POLL),
                "Forward should restore forms.html");
    }
}
