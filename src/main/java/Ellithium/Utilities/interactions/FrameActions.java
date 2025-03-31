package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class FrameActions<T extends WebDriver> extends BaseActions<T> {
    
    public FrameActions(T driver) {
        super(driver);
    }

    /**
     * Switches to a frame by index.
     * @param index Frame index
     * @param timeout Maximum wait time in seconds
     * @param pollingTime Polling interval in milliseconds
     */
    public void switchToFrameByIndex(int index, int timeout, int pollingTime) {
        getFluentWait(timeout, pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }

    /**
     * Switches to a frame by name or ID.
     * @param nameOrID Frame name or ID
     * @param timeout Maximum wait time in seconds
     * @param pollingTime Polling interval in milliseconds
     */
    public void switchToFrameByNameOrID(String nameOrID, int timeout, int pollingTime) {
        getFluentWait(timeout, pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrID));
    }

    /**
     * Switches to a frame by WebElement.
     * @param locator Frame locator
     * @param timeout Maximum wait time in seconds
     * @param pollingTime Polling interval in milliseconds
     */
    public void switchToFrameByElement(By locator, int timeout, int pollingTime) {
        getFluentWait(timeout, pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    /**
     * Switches back to the default content from a frame.
     */
    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    /**
     * Waits for a frame to be available and switches to it.
     * @param locator Frame locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameToBeAvailableAndSwitchToIt(By locator, int timeout, int pollingEvery) {
        return getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    /**
     * Waits for a frame to be available by name or ID and switches to it.
     * @param nameOrId Frame name or ID
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(String nameOrId, int timeout, int pollingEvery) {
        return getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
    }

    /**
     * Waits for a frame to be available by index and switches to it.
     * @param index Frame index
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt(int index, int timeout, int pollingEvery) {
        return getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }

    // Overloaded methods with default timeouts
    /**
     * Switches to a frame by index with default timeout and polling time.
     * @param index Frame index
     */
    public void switchToFrameByIndex(int index) {
        switchToFrameByIndex(index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by index with specified timeout.
     * @param index Frame index
     * @param timeout Maximum wait time in seconds
     */
    public void switchToFrameByIndex(int index, int timeout) {
        switchToFrameByIndex(index, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by name or ID with default timeout and polling time.
     * @param nameOrID Frame name or ID
     */
    public void switchToFrameByNameOrID(String nameOrID) {
        switchToFrameByNameOrID(nameOrID, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by name or ID with specified timeout.
     * @param nameOrID Frame name or ID
     * @param timeout Maximum wait time in seconds
     */
    public void switchToFrameByNameOrID(String nameOrID, int timeout) {
        switchToFrameByNameOrID(nameOrID, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by WebElement with default timeout and polling time.
     * @param locator Frame locator
     */
    public void switchToFrameByElement(By locator) {
        switchToFrameByElement(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by WebElement with specified timeout.
     * @param locator Frame locator
     * @param timeout Maximum wait time in seconds
     */
    public void switchToFrameByElement(By locator, int timeout) {
        switchToFrameByElement(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available and switches to it with default timeout and polling time.
     * @param locator Frame locator
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameToBeAvailableAndSwitchToIt(By locator) {
        return waitForFrameToBeAvailableAndSwitchToIt(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available and switches to it with specified timeout.
     * @param locator Frame locator
     * @param timeout Maximum wait time in seconds
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameToBeAvailableAndSwitchToIt(By locator, int timeout) {
        return waitForFrameToBeAvailableAndSwitchToIt(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available by name or ID and switches to it with default timeout and polling time.
     * @param nameOrId Frame name or ID
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(String nameOrId) {
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(nameOrId, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available by name or ID and switches to it with specified timeout.
     * @param nameOrId Frame name or ID
     * @param timeout Maximum wait time in seconds
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(String nameOrId, int timeout) {
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(nameOrId, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available by index and switches to it with default timeout and polling time.
     * @param index Frame index
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt(int index) {
        return waitForFrameByIndexToBeAvailableAndSwitchToIt(index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available by index and switches to it with specified timeout.
     * @param index Frame index
     * @param timeout Maximum wait time in seconds
     * @return The WebDriver instance switched to the frame
     */
    public WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt(int index, int timeout) {
        return waitForFrameByIndexToBeAvailableAndSwitchToIt(index, timeout, WaitManager.getDefaultPollingTime());
    }
}
