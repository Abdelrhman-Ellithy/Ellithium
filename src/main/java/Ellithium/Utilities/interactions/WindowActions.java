package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class WindowActions<T extends WebDriver> extends BaseActions<T> {
    
    public WindowActions(T driver) {
        super(driver);
    }

    /**
     * Switches to a new window with the specified title.
     * @param windowTitle The title of the window to switch to
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
}
