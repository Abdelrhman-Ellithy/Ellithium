package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.util.Set;

/**
 * Provides a comprehensive set of methods to manage browser cookies.
 *
 * @param <T> The specific WebDriver type
 */
public class CookieActions<T extends WebDriver> extends BaseActions<T> {

    public CookieActions(T driver) {
        super(driver);
    }

    /**
     * Adds a cookie to the current browser session.
     * @param cookie The cookie to add
     */
    public void addCookie(Cookie cookie) {
        if (cookie == null) {
            throw new IllegalArgumentException("cookie must not be null");
        }
        Reporter.log("Adding Cookie: " + cookie.getName(), LogLevel.INFO_BLUE);
        driver.manage().addCookie(cookie);
    }

    /**
     * Deletes a specific cookie by its name.
     * @param cookieName The name of the cookie to delete
     */
    public void deleteCookieNamed(String cookieName) {
        if (cookieName == null || cookieName.isBlank()) {
            throw new IllegalArgumentException("cookieName must not be null or blank");
        }
        Reporter.log("Deleting Cookie Named: " + cookieName, LogLevel.INFO_BLUE);
        driver.manage().deleteCookieNamed(cookieName);
    }

    /**
     * Deletes a specific cookie object.
     * @param cookie The cookie to delete
     */
    public void deleteCookie(Cookie cookie) {
        if (cookie == null) {
            throw new IllegalArgumentException("cookie must not be null");
        }
        Reporter.log("Deleting Cookie: " + cookie.getName(), LogLevel.INFO_BLUE);
        driver.manage().deleteCookie(cookie);
    }

    /**
     * Deletes all cookies for the current domain.
     */
    public void deleteAllCookies() {
        Reporter.log("Deleting All Cookies", LogLevel.INFO_BLUE);
        driver.manage().deleteAllCookies();
    }

    /**
     * Retrieves all cookies for the current domain.
     * @return A set of cookies
     */
    public Set<Cookie> getCookies() {
        Reporter.log("Retrieving All Cookies", LogLevel.INFO_BLUE);
        return driver.manage().getCookies();
    }

    /**
     * Retrieves a specific cookie by its name.
     * @param cookieName The name of the cookie
     * @return The cookie, or null if not found
     */
    public Cookie getCookieNamed(String cookieName) {
        if (cookieName == null || cookieName.isBlank()) {
            throw new IllegalArgumentException("cookieName must not be null or blank");
        }
        Reporter.log("Retrieving Cookie Named: " + cookieName, LogLevel.INFO_BLUE);
        return driver.manage().getCookieNamed(cookieName);
    }
}
