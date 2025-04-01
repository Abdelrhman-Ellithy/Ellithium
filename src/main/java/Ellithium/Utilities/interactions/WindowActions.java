package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.Point;
import org.openqa.selenium.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides window management actions for WebDriver interactions.
 * Handles window switching, resizing, positioning, and other window-related operations.
 * @param <T> Type of WebDriver being used
 */
public class WindowActions<T extends WebDriver> extends BaseActions<T> {
    
    public WindowActions(T driver) {
        super(driver);
    }

    /**
     * Switches to a new window with the specified title.
     * If the window is not found, returns to the original window.
     * @param windowTitle The title of the window to switch to
     * @throws org.openqa.selenium.NoSuchWindowException if the window handle is invalid
     */
    public void switchToNewWindow(String windowTitle) {
        String originalWindow = driver.getWindowHandle();
        for (String windowHandle : driver.getWindowHandles()) {
            driver.switchTo().window(windowHandle);
            if (driver.getTitle().equals(windowTitle)) {
                return;
            }
        }
        driver.switchTo().window(originalWindow);
    }

    /**
     * Closes the current window or tab.
     * Note: After closing, you need to switch to another window to continue interacting.
     * @throws org.openqa.selenium.NoSuchWindowException if the window is already closed
     */
    public void closeCurrentWindow() {
        driver.close();
    }

    /**
     * Switches to the original window.
     * @param originalWindowHandle The handle of the original window
     */
    public void switchToOriginalWindow(String originalWindowHandle) {
        driver.switchTo().window(originalWindowHandle);
    }

    /**
     * Switches to a popup window with the specified title.
     * @param expectedPopupTitle The title of the popup window to switch to
     * @param timeout Maximum wait time in seconds
     * @param pollingTime Polling interval in milliseconds
     */
    public void switchToPopupWindow(String expectedPopupTitle, int timeout, int pollingTime) {
        String mainWindow = driver.getWindowHandle();
        Reporter.log("Waiting for popup window to appear.", LogLevel.INFO_BLUE);

        boolean windowsAppeared = getFluentWait(timeout, pollingTime)
                .until(ExpectedConditions.numberOfWindowsToBe(2));

        if (windowsAppeared) {
            for (String windowHandle : driver.getWindowHandles()) {
                if (!windowHandle.equals(mainWindow)) {
                    driver.switchTo().window(windowHandle);
                    if (driver.getTitle().equals(expectedPopupTitle)) {
                        Reporter.log("Switched to popup window with title: " + expectedPopupTitle, LogLevel.INFO_BLUE);
                        return;
                    }
                }
            }
            Reporter.log("Popup window with title " + expectedPopupTitle + " not found", LogLevel.ERROR);
            driver.switchTo().window(mainWindow);
        } else {
            Reporter.log("Popup window did not appear within the timeout.", LogLevel.ERROR);
        }
    }

    /**
     * Closes the popup window and switches back to the main window.
     */
    public void closePopupWindow() {
        driver.close();
        Reporter.log("Popup window closed. Switching back to the main window.", LogLevel.INFO_BLUE);
        String mainWindow = driver.getWindowHandles().iterator().next();
        driver.switchTo().window(mainWindow);
    }

    /**
     * Waits for the number of windows to be a specific number.
     * @param numberOfWindows The expected number of windows
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the number of windows matches the expected number, false otherwise
     */
    public boolean waitForNumberOfWindowsToBe(int numberOfWindows, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Windows to be: " + numberOfWindows, LogLevel.INFO_BLUE);
        return getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.numberOfWindowsToBe(numberOfWindows));
    }

    /**
     * Maximizes the browser window.
     */
    public void maximizeWindow() {
        driver.manage().window().maximize();
    }

    /**
     * Minimizes the browser window.
     */
    public void minimizeWindow() {
        driver.manage().window().minimize();
    }

    // Overloaded methods with default timeouts
    public void switchToPopupWindow(String expectedPopupTitle) {
        switchToPopupWindow(expectedPopupTitle, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public void switchToPopupWindow(String expectedPopupTitle, int timeout) {
        switchToPopupWindow(expectedPopupTitle, timeout, WaitManager.getDefaultPollingTime());
    }

    public boolean waitForNumberOfWindowsToBe(int numberOfWindows) {
        return waitForNumberOfWindowsToBe(numberOfWindows, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public boolean waitForNumberOfWindowsToBe(int numberOfWindows, int timeout) {
        return waitForNumberOfWindowsToBe(numberOfWindows, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Sets the window position on the screen.
     * @param x X coordinate (horizontal position)
     * @param y Y coordinate (vertical position)
     * @throws org.openqa.selenium.WebDriverException if the driver fails to set position
     */
    public void setWindowPosition(int x, int y) {
        try {
            driver.manage().window().setPosition(new Point(x, y));
        } catch (Exception e) {
            Reporter.log("Failed to set window position: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Sets the window size (dimensions).
     * @param width Width of the window in pixels
     * @param height Height of the window in pixels
     * @throws org.openqa.selenium.WebDriverException if the driver fails to set size
     */
    public void setWindowSize(int width, int height) {
        try {
            driver.manage().window().setSize(new Dimension(width, height));
        } catch (Exception e) {
            Reporter.log("Failed to set window size: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Switches the browser to full screen mode.
     * Different from maximize - uses the entire screen without window decorations.
     * @throws org.openqa.selenium.WebDriverException if fullscreen mode is not supported
     */
    public void fullscreenWindow() {
        try {
            driver.manage().window().fullscreen();
        } catch (Exception e) {
            Reporter.log("Failed to switch to fullscreen: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Retrieves the current window position.
     * @return Point object containing x,y coordinates of the window
     * @throws org.openqa.selenium.WebDriverException if position cannot be determined
     */
    public Point getWindowPosition() {
        return driver.manage().window().getPosition();
    }

    /**
     * Retrieves the current window dimensions.
     * @return Dimension object containing width and height of the window
     * @throws org.openqa.selenium.WebDriverException if size cannot be determined
     */
    public Dimension getWindowSize() {
        return driver.manage().window().getSize();
    }

    /**
     * Retrieves a list of all window handles in the current session.
     * @return List of window handle strings
     * @throws org.openqa.selenium.WebDriverException if handles cannot be retrieved
     */
    public List<String> getAllWindowHandles() {
        return new ArrayList<>(driver.getWindowHandles());
    }

    /**
     * Retrieves the handle of the currently focused window.
     * @return String representing the current window handle
     * @throws org.openqa.selenium.NoSuchWindowException if the current window is closed
     */
    public String getCurrentWindowHandle() {
        return driver.getWindowHandle();
    }

    /**
     * Switches focus to a window by its index in the set of handles.
     * @param index Zero-based index of the window
     * @return true if switch was successful, false if index invalid or switch failed
     * @throws org.openqa.selenium.WebDriverException if window switching fails
     */
    public boolean switchToWindowByIndex(int index) {
        try {
            List<String> handles = getAllWindowHandles();
            if (index >= 0 && index < handles.size()) {
                driver.switchTo().window(handles.get(index));
                Reporter.log("Switched to window at index: " + index, LogLevel.INFO_BLUE);
                return true;
            }
            Reporter.log("Invalid window index: " + index, LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Failed to switch window by index: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Switches focus to the most recently opened window.
     * @return true if switch successful, false if no windows available
     * @throws org.openqa.selenium.WebDriverException if window switching fails
     */
    public boolean switchToLastWindow() {
        try {
            List<String> handles = getAllWindowHandles();
            if (!handles.isEmpty()) {
                driver.switchTo().window(handles.get(handles.size() - 1));
                Reporter.log("Switched to last window", LogLevel.INFO_BLUE);
                return true;
            }
            return false;
        } catch (Exception e) {
            Reporter.log("Failed to switch to last window: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Closes all secondary windows, keeping only the main window open.
     * Automatically switches back to the main window after closing others.
     * @throws org.openqa.selenium.WebDriverException if closing operations fail
     */
    public void closeAllExceptMain() {
        String mainHandle = getCurrentWindowHandle();
        for (String handle : getAllWindowHandles()) {
            if (!handle.equals(mainHandle)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(mainHandle);
        Reporter.log("Closed all windows except main window", LogLevel.INFO_BLUE);
    }

    /**
     * Gets the total number of open windows/tabs.
     * @return Integer representing the number of open windows
     * @throws org.openqa.selenium.WebDriverException if handle count cannot be retrieved
     */
    public int getNumberOfWindows() {
        return driver.getWindowHandles().size();
    }

    /**
     * Verifies if a window with the specified title exists.
     * Returns focus to the original window after checking.
     * @param title Title of the window to search for
     * @return true if window exists, false otherwise
     * @throws org.openqa.selenium.WebDriverException if window checking fails
     */
    public boolean doesWindowExist(String title) {
        String currentHandle = getCurrentWindowHandle();
        boolean exists = false;
        
        for (String handle : getAllWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getTitle().equals(title)) {
                exists = true;
                break;
            }
        }
        driver.switchTo().window(currentHandle);
        return exists;
    }
}
