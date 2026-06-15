package Ellithium.Utilities.interactions;

import org.mockito.Mockito;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WindowActionsTest {

    private WebDriver driver;
    private WebDriver.Options options;
    private WebDriver.Window window;
    private WebDriver.TargetLocator targetLocator;
    private WindowActions<WebDriver> windowActions;

    @BeforeMethod
    public void setUp() {
        driver = Mockito.mock(WebDriver.class,
                Mockito.withSettings().extraInterfaces(
                        org.openqa.selenium.JavascriptExecutor.class,
                        org.openqa.selenium.TakesScreenshot.class));
        options = Mockito.mock(WebDriver.Options.class);
        window = Mockito.mock(WebDriver.Window.class);
        targetLocator = Mockito.mock(WebDriver.TargetLocator.class);

        Mockito.when(driver.manage()).thenReturn(options);
        Mockito.when(options.window()).thenReturn(window);
        Mockito.when(driver.switchTo()).thenReturn(targetLocator);

        windowActions = new WindowActions<>(driver);
    }

    @Test
    public void maximizeWindow_callsManageWindow() {
        windowActions.maximizeWindow();
        Mockito.verify(window).maximize();
    }

    @Test
    public void minimizeWindow_callsManageWindow() {
        windowActions.minimizeWindow();
        Mockito.verify(window).minimize();
    }

    @Test
    public void fullscreenWindow_callsManageWindow() {
        windowActions.fullscreenWindow();
        Mockito.verify(window).fullscreen();
    }

    @Test
    public void setWindowPosition_delegatesToWindow() {
        windowActions.setWindowPosition(100, 200);
        Mockito.verify(window).setPosition(new Point(100, 200));
    }

    @Test
    public void setWindowSize_delegatesToWindow() {
        windowActions.setWindowSize(1280, 720);
        Mockito.verify(window).setSize(new Dimension(1280, 720));
    }

    @Test
    public void getWindowPosition_returnsDriverPosition() {
        Point expected = new Point(50, 60);
        Mockito.when(window.getPosition()).thenReturn(expected);
        Assert.assertEquals(windowActions.getWindowPosition(), expected);
    }

    @Test
    public void getWindowSize_returnsDriverSize() {
        Dimension expected = new Dimension(1920, 1080);
        Mockito.when(window.getSize()).thenReturn(expected);
        Assert.assertEquals(windowActions.getWindowSize(), expected);
    }

    @Test
    public void closeCurrentWindow_callsDriverClose() {
        windowActions.closeCurrentWindow();
        Mockito.verify(driver).close();
    }

    @Test
    public void switchToOriginalWindow_switchesTo() {
        windowActions.switchToOriginalWindow("handle-abc");
        Mockito.verify(targetLocator).window("handle-abc");
    }

    @Test
    public void getAllWindowHandles_returnsList() {
        Set<String> handles = new LinkedHashSet<>(Arrays.asList("h1", "h2", "h3"));
        Mockito.when(driver.getWindowHandles()).thenReturn(handles);
        List<String> result = windowActions.getAllWindowHandles();
        Assert.assertEquals(result.size(), 3);
        Assert.assertTrue(result.containsAll(handles));
    }

    @Test
    public void waitForNumberOfWindowsToBe_matchingCount_returnsTrue() {
        Set<String> handles = new LinkedHashSet<>(Arrays.asList("h1", "h2"));
        Mockito.when(driver.getWindowHandles()).thenReturn(handles);
        Assert.assertTrue(windowActions.waitForNumberOfWindowsToBe(2, 3, 200));
    }

    @Test(expectedExceptions = org.openqa.selenium.TimeoutException.class)
    public void waitForNumberOfWindowsToBe_mismatch_throwsTimeout() {
        Set<String> handles = new LinkedHashSet<>(List.of("h1"));
        Mockito.when(driver.getWindowHandles()).thenReturn(handles);
        windowActions.waitForNumberOfWindowsToBe(3, 1, 200);
    }
}
