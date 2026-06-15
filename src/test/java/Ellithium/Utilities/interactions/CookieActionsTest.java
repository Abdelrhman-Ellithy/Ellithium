package Ellithium.Utilities.interactions;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static org.mockito.Mockito.*;

public class CookieActionsTest {

    private WebDriver mockDriver;
    private WebDriver.Options mockOptions;
    private CookieActions<WebDriver> cookieActions;

    @BeforeMethod
    public void setup() {
        mockDriver  = mock(WebDriver.class);
        mockOptions = mock(WebDriver.Options.class);
        when(mockDriver.manage()).thenReturn(mockOptions);
        cookieActions = new CookieActions<>(mockDriver);
    }

    @Test
    public void addCookie_delegatesToDriverManage() {
        Cookie cookie = new Cookie("session", "abc123");
        cookieActions.addCookie(cookie);
        verify(mockOptions).addCookie(cookie);
    }

    @Test
    public void deleteCookieNamed_delegatesToDriverManage() {
        cookieActions.deleteCookieNamed("session");
        verify(mockOptions).deleteCookieNamed("session");
    }

    @Test
    public void deleteCookie_delegatesToDriverManage() {
        Cookie cookie = new Cookie("token", "xyz");
        cookieActions.deleteCookie(cookie);
        verify(mockOptions).deleteCookie(cookie);
    }

    @Test
    public void deleteAllCookies_delegatesToDriverManage() {
        cookieActions.deleteAllCookies();
        verify(mockOptions).deleteAllCookies();
    }

    @Test
    public void getCookies_returnsWhatDriverReturns() {
        Set<Cookie> expected = Set.of(new Cookie("a", "1"), new Cookie("b", "2"));
        when(mockOptions.getCookies()).thenReturn(expected);
        Set<Cookie> actual = cookieActions.getCookies();
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void getCookieNamed_returnsWhatDriverReturns() {
        Cookie expected = new Cookie("session", "token-value");
        when(mockOptions.getCookieNamed("session")).thenReturn(expected);
        Cookie actual = cookieActions.getCookieNamed("session");
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void getCookieNamed_returnsNull_whenNotFound() {
        when(mockOptions.getCookieNamed("missing")).thenReturn(null);
        Assert.assertNull(cookieActions.getCookieNamed("missing"));
    }
}
