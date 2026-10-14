package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.qameta.allure.Step;
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
    @Step("Add cookie '{cookie.name}'")
    public void addCookie(Cookie cookie) {
        Reporter.log("Adding Cookie: " + cookie.getName(), LogLevel.INFO_BLUE);
        driver.manage().addCookie(cookie);
    }

    /**
     * Deletes a specific cookie by its name.
     * @param cookieName The name of the cookie to delete
     */
    @Step("Delete cookie named '{cookieName}'")
    public void deleteCookieNamed(String cookieName) {
        Reporter.log("Deleting Cookie Named: " + cookieName, LogLevel.INFO_BLUE);
        driver.manage().deleteCookieNamed(cookieName);
    }

    /**
     * Deletes a specific cookie object.
     * @param cookie The cookie to delete
     */
    @Step("Delete cookie '{cookie.name}'")
    public void deleteCookie(Cookie cookie) {
        Reporter.log("Deleting Cookie: " + cookie.getName(), LogLevel.INFO_BLUE);
        driver.manage().deleteCookie(cookie);
    }

    /**
     * Deletes all cookies for the current domain.
     */
    @Step("Delete all cookies")
    public void deleteAllCookies() {
        Reporter.log("Deleting All Cookies", LogLevel.INFO_BLUE);
        driver.manage().deleteAllCookies();
    }

    /**
     * Retrieves all cookies for the current domain.
     * @return A set of cookies
     */
    @Step("Get cookies")
    public Set<Cookie> getCookies() {
        Reporter.log("Retrieving All Cookies", LogLevel.INFO_BLUE);
        return driver.manage().getCookies();
    }

    /**
     * Retrieves a specific cookie by its name.
     * @param cookieName The name of the cookie
     * @return The cookie, or null if not found
     */
    @Step("Get cookie named '{cookieName}'")
    public Cookie getCookieNamed(String cookieName) {
        Reporter.log("Retrieving Cookie Named: " + cookieName, LogLevel.INFO_BLUE);
        return driver.manage().getCookieNamed(cookieName);
    }
}
