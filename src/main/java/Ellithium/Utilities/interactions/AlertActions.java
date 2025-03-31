package Ellithium.Utilities.interactions;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AlertActions<T extends WebDriver> extends BaseActions<T> {
    
    public AlertActions(T driver) {
        super(driver);
    }

    /**
     * Accepts an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void accept(int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
    }

    /**
     * Dismisses an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void dismiss(int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().dismiss();
    }

    /**
     * Gets the text of an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The alert text
     */
    public String getText(int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        return driver.switchTo().alert().getText();
    }

    /**
     * Sends data to an alert with specified timeout.
     * @param data The data to send
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void sendData(String data, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().sendKeys(data);
    }

    /**
     * Accepts an alert with default timeout and polling time.
     */
    public void accept() {
        accept(WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Dismisses an alert with default timeout and polling time.
     */
    public void dismiss() {
        dismiss(WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the text of an alert with default timeout and polling time.
     * @return The alert text
     */
    public String getText() {
        return getText(WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends data to an alert with default timeout and polling time.
     * @param data The data to send
     */
    public void sendData(String data) {
        sendData(data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Accepts an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     */
    public void accept(int timeout) {
        accept(timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Dismisses an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     */
    public void dismiss(int timeout) {
        dismiss(timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the text of an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @return The alert text
     */
    public String getText(int timeout) {
        return getText(timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends data to an alert with specified timeout.
     * @param data The data to send
     * @param timeout Maximum wait time in seconds
     */
    public void sendData(String data, int timeout) {
        sendData(data, timeout, WaitManager.getDefaultPollingTime());
    }
}
