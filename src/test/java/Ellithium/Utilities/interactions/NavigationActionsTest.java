package Ellithium.Utilities.interactions;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class NavigationActionsTest {

    private WebDriver mockDriver;
    private WebDriver.Navigation mockNav;
    private NavigationActions<WebDriver> nav;

    @BeforeMethod
    public void setup() {
        mockDriver = mock(WebDriver.class);
        mockNav    = mock(WebDriver.Navigation.class);
        when(mockDriver.navigate()).thenReturn(mockNav);
        nav = new NavigationActions<>(mockDriver);
    }

    @Test
    public void navigateToUrl_callsDriverGet() {
        nav.navigateToUrl("https://example.com");
        verify(mockDriver).get("https://example.com");
    }

    @Test
    public void refreshPage_callsNavigateRefresh() {
        nav.refreshPage();
        verify(mockNav).refresh();
    }

    @Test
    public void navigateBack_callsNavigateBack() {
        nav.navigateBack();
        verify(mockNav).back();
    }

    @Test
    public void navigateForward_callsNavigateForward() {
        nav.navigateForward();
        verify(mockNav).forward();
    }
}
