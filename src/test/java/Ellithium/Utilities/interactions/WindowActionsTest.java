package Ellithium.Utilities.interactions;

import org.mockito.Mockito;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WindowActionsTest {

    private WebDriver driver;
    private WebDriver.Window window;
    private WebDriver.TargetLocator targetLocator;
    private WindowActions<WebDriver> windowActions;

    @BeforeMethod
    public void setUp() {
        driver = Mockito.mock(WebDriver.class,
                Mockito.withSettings().extraInterfaces(
                        org.openqa.selenium.JavascriptExecutor.class,
                        org.openqa.selenium.TakesScreenshot.class));
        WebDriver.Options options = Mockito.mock(WebDriver.Options.class);
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

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void switchToOriginalWindow_nullHandle_throwsIllegalArgumentException() {
        windowActions.switchToOriginalWindow(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void switchToOriginalWindow_blankHandle_throwsIllegalArgumentException() {
        windowActions.switchToOriginalWindow("   ");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void switchToWindowByHandle_nullHandle_throwsIllegalArgumentException() {
        windowActions.switchToWindowByHandle(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void switchToWindowByHandle_blankHandle_throwsIllegalArgumentException() {
        windowActions.switchToWindowByHandle("   ");
    }

    @Test
    public void switchToWindowByHandle_validHandle_delegates() {
        windowActions.switchToWindowByHandle("win-123");
        Mockito.verify(targetLocator).window("win-123");
    }

    @Test
    public void isWindowOpen_checksPresence() {
        Mockito.when(driver.getWindowHandles()).thenReturn(Set.of("w1", "w2"));
        Assert.assertTrue(windowActions.isWindowOpen("w1"));
        Assert.assertFalse(windowActions.isWindowOpen("w99"));
        Assert.assertFalse(windowActions.isWindowOpen(null));
    }

    @Test
    public void switchToWindowByIndex_validAndInvalid() {
        Mockito.when(driver.getWindowHandles()).thenReturn(new LinkedHashSet<>(List.of("w0", "w1")));
        Assert.assertTrue(windowActions.switchToWindowByIndex(1));
        Mockito.verify(targetLocator).window("w1");

        Assert.assertFalse(windowActions.switchToWindowByIndex(5));
        Assert.assertFalse(windowActions.switchToWindowByIndex(-1));
    }

    @Test
    public void switchToLastWindow_switchesToLast() {
        Mockito.when(driver.getWindowHandles()).thenReturn(new LinkedHashSet<>(List.of("w0", "w1", "w2")));
        Assert.assertTrue(windowActions.switchToLastWindow());
        Mockito.verify(targetLocator).window("w2");
    }

    @Test
    public void switchToLastWindow_empty_returnsFalse() {
        Mockito.when(driver.getWindowHandles()).thenReturn(Collections.emptySet());
        Assert.assertFalse(windowActions.switchToLastWindow());
    }

    @Test
    public void getNumberOfWindows_returnsSize() {
        Mockito.when(driver.getWindowHandles()).thenReturn(Set.of("w1", "w2", "w3", "w4"));
        Assert.assertEquals(windowActions.getNumberOfWindows(), 4);
    }

    @Test
    public void doesWindowExist_checksTitle() {
        Mockito.when(driver.getWindowHandle()).thenReturn("main-w");
        Mockito.when(driver.getWindowHandles()).thenReturn(new LinkedHashSet<>(List.of("main-w", "sub-w")));
        Mockito.when(driver.getTitle()).thenReturn("Main Page", "Popup Page");

        Assert.assertTrue(windowActions.doesWindowExist("Popup Page"));
        Assert.assertFalse(windowActions.doesWindowExist("Non-existent Page"));
    }

    @Test
    public void switchToNewWindow_matchesTitle() {
        Mockito.when(driver.getWindowHandle()).thenReturn("main-w");
        Mockito.when(driver.getWindowHandles()).thenReturn(new LinkedHashSet<>(List.of("main-w", "target-w")));
        Mockito.when(driver.getTitle()).thenReturn("Main Window", "Target Window");

        windowActions.switchToNewWindow("Target Window");
        Mockito.verify(targetLocator).window("target-w");
    }

    @Test
    public void closeAllExceptMain_closesSecondaryWindows() {
        Mockito.when(driver.getWindowHandle()).thenReturn("main-w");
        Mockito.when(driver.getWindowHandles()).thenReturn(new LinkedHashSet<>(List.of("main-w", "sec-1", "sec-2")));

        windowActions.closeAllExceptMain();
        Mockito.verify(targetLocator).window("sec-1");
        Mockito.verify(targetLocator).window("sec-2");
        Mockito.verify(driver, Mockito.times(2)).close();
        Mockito.verify(targetLocator, Mockito.atLeastOnce()).window("main-w");
    }

    @Test
    public void getCurrentWindowTitle_and_switchToParentWindow() {
        Mockito.when(driver.getTitle()).thenReturn("Home Page");
        Assert.assertEquals(windowActions.getCurrentWindowTitle(), "Home Page");

        windowActions.switchToParentWindow();
        Mockito.verify(targetLocator).parentFrame();
    }
}
