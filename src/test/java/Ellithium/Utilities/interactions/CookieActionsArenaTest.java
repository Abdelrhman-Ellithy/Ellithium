package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Real-browser integration tests for {@link CookieActions} using the Ellithium Test Arena.
 *
 * <p>Runs against {@code cookies.html}. The page auto-seeds known cookies on load.
 * All UI interactions use Ellithium {@link ElementActions}.
 * Complements (does not replace) {@link CookieActionsTest}.
 */
public class CookieActionsArenaTest extends ArenaBaseTest {

    @BeforeMethod
    public void resetPage() {
        goTo("cookies.html");
    }

    // ── 1. Pre-seeded cookies — no interaction needed ─────────────────────────

    @Test(groups = "arena")
    public void getCookieNamed_returnsPreSeededAuth() {
        Cookie auth = cookieActions.getCookieNamed("ellithium_auth");
        Assert.assertNotNull(auth, "ellithium_auth must be auto-seeded on page load");
        Assert.assertEquals(auth.getValue(), "token_qa_12345",
                "ellithium_auth value must match known seed");
    }

    @Test(groups = "arena")
    public void getCookieNamed_returnsPreSeededRole() {
        Cookie role = cookieActions.getCookieNamed("ellithium_role");
        Assert.assertNotNull(role, "ellithium_role must be auto-seeded on page load");
        Assert.assertEquals(role.getValue(), "QA_ENGINEER");
    }

    @Test(groups = "arena")
    public void getCookieNamed_returnsPreSeededPref() {
        Cookie pref = cookieActions.getCookieNamed("ellithium_pref");
        Assert.assertNotNull(pref, "ellithium_pref must be auto-seeded on page load");
        Assert.assertEquals(pref.getValue(), "dark_mode=true");
    }

    @Test(groups = "arena")
    public void getCookieNamed_returnsPreSeededSessionId_withDynamicValue() {
        Cookie session = cookieActions.getCookieNamed("ellithium_session_id");
        Assert.assertNotNull(session, "ellithium_session_id must be auto-seeded");
        Assert.assertTrue(session.getValue().startsWith("sess-"),
                "Session ID must start with 'sess-'; got: " + session.getValue());
    }

    // ── 2. getCookies — all pre-seeds present ─────────────────────────────────

    @Test(groups = "arena")
    public void getCookies_returnsAtLeast4PreSeededCookies() {
        Set<Cookie> all = cookieActions.getCookies();
        Assert.assertTrue(all.size() >= 4,
                "At least 4 cookies must be seeded on load; got: " + all.size());
    }

    // ── 3. addCookie — via UI then framework verification ─────────────────────

    @Test(groups = "arena")
    public void addCookie_viaEllithiumApi_verifiedByGetCookieNamed() {
        // Use Ellithium CookieActions directly (bypassing UI)
        cookieActions.addCookie(new Cookie("arena_session", "test_token_999"));

        Cookie added = cookieActions.getCookieNamed("arena_session");
        Assert.assertNotNull(added, "arena_session cookie should exist after addCookie");
        Assert.assertEquals(added.getValue(), "test_token_999");
    }

    @Test(groups = "arena")
    public void addCookie_viaArenaUiButtons_andVerifyGetCookieNamed() {
        // Fill the form and click Add Cookie button via Ellithium
        elementActions.sendData(By.id("cookie-name-input"),  "ui_added_cookie", SHORT, POLL);
        elementActions.sendData(By.id("cookie-value-input"), "ui_value_123",    SHORT, POLL);
        elementActions.clickOnElement(By.id("add-cookie-btn"), SHORT, POLL);

        // Verify via framework call
        Cookie added = cookieActions.getCookieNamed("ui_added_cookie");
        Assert.assertNotNull(added, "Cookie added via UI should be retrievable by framework");
        Assert.assertEquals(added.getValue(), "ui_value_123");
    }

    // ── 4. deleteCookieNamed ──────────────────────────────────────────────────

    @Test(groups = "arena")
    public void deleteCookieNamed_removesOnlyCookieWithThatName() {
        // Pre-condition: cookie exists
        Assert.assertNotNull(cookieActions.getCookieNamed("ellithium_auth"));

        cookieActions.deleteCookieNamed("ellithium_auth");
        Cookie after = cookieActions.getCookieNamed("ellithium_auth");
        Assert.assertNull(after, "ellithium_auth must be null after deleteCookieNamed");

        // Other cookies must remain
        Assert.assertNotNull(cookieActions.getCookieNamed("ellithium_role"),
                "ellithium_role must still exist after deleting a different cookie");
    }

    @Test(groups = "arena")
    public void deleteCookieNamed_usingArenaUiButton_verifiedByFramework() {
        // Use Ellithium to interact with the UI delete control
        elementActions.sendData(By.id("delete-cookie-name"), "ellithium_pref", SHORT, POLL);
        elementActions.clickOnElement(By.id("delete-named-cookie-btn"), SHORT, POLL);

        // Verify gone via framework
        Assert.assertNull(cookieActions.getCookieNamed("ellithium_pref"),
                "ellithium_pref should be null after UI delete");
    }

    // ── 5. deleteCookie (by object) ───────────────────────────────────────────

    @Test(groups = "arena")
    public void deleteCookie_byObject_removesTargetCookie() {
        Cookie roleToDelete = cookieActions.getCookieNamed("ellithium_role");
        Assert.assertNotNull(roleToDelete, "ellithium_role must exist before delete-by-object");

        cookieActions.deleteCookie(roleToDelete);
        Assert.assertNull(cookieActions.getCookieNamed("ellithium_role"),
                "ellithium_role must be null after deleteCookie(cookieObject)");
    }

    // ── 6. deleteAllCookies ───────────────────────────────────────────────────

    @Test(groups = "arena")
    public void deleteAllCookies_resultsInEmptyCookieSet() {
        // Pre-condition: at least 1 cookie
        Assert.assertFalse(cookieActions.getCookies().isEmpty(), "Cookies must exist before deleteAll");

        cookieActions.deleteAllCookies();
        Set<Cookie> all = cookieActions.getCookies();
        Assert.assertTrue(all.isEmpty(),
                "getCookies() must return empty set after deleteAllCookies; got: " + all);
    }

    // ── 7. getCookieNamed returns null for non-existent ───────────────────────

    @Test(groups = "arena")
    public void getCookieNamed_returnsNull_forNonExistentCookie() {
        Assert.assertNull(cookieActions.getCookieNamed("completely_nonexistent_cookie_xyz"),
                "getCookieNamed must return null for a cookie that doesn't exist");
    }

    // ── 8. LocalStorage via JavaScriptActions ─────────────────────────────────

    @Test(groups = "arena")
    public void localStorage_setAndGet_viaJavaScriptActions() {
        // Use Ellithium jsActions for localStorage
        jsActions.setElementValueUsingJS(By.id("ls-key"),   "arena_test_key");
        jsActions.setElementValueUsingJS(By.id("ls-value"), "arena_test_value");
        elementActions.clickOnElement(By.id("ls-set-btn"), SHORT, POLL);

        // Retrieve via Ellithium jsActions JS executor
        Object retrieved = js("return localStorage.getItem('arena_test_key')");
        Assert.assertEquals(String.valueOf(retrieved), "arena_test_value",
                "localStorage.getItem should return what was set");
    }

    @Test(groups = "arena")
    public void sessionStorage_setAndGet_viaJavaScriptActions() {
        jsActions.setElementValueUsingJS(By.id("ss-key"),   "arena_ss_key");
        jsActions.setElementValueUsingJS(By.id("ss-value"), "arena_ss_value");
        elementActions.clickOnElement(By.id("ss-set-btn"), SHORT, POLL);

        Object retrieved = js("return sessionStorage.getItem('arena_ss_key')");
        Assert.assertEquals(String.valueOf(retrieved), "arena_ss_value",
                "sessionStorage.getItem should return what was set");
    }

    // ── 9. Verify seed button via UI ──────────────────────────────────────────

    @Test(groups = "arena")
    public void verifySeedCookies_uiButtonAndFrameworkAgree() {
        elementActions.clickOnElement(By.id("verify-seeds-btn"), SHORT, POLL);
        String status = elementActions.getText(By.id("seed-verify-status"), SHORT, POLL);
        Assert.assertTrue(status.contains("✅") || status.contains("OK"),
                "Seed verification button should report all seeds present; got: " + status);
    }
}
