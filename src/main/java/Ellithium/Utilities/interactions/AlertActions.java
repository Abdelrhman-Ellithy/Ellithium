package Ellithium.Utilities.interactions;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AlertActions<T extends WebDriver> extends BaseActions<T> {
    
    public AlertActions(T driver) {
        super(driver);
    }

    private boolean tryHandlePromptBiDi(boolean accept, String promptText) {
        if (driver instanceof org.openqa.selenium.bidi.HasBiDi hasBiDi) {
            try {
                if (hasBiDi.getBiDi() == null) return false;
                String handle = driver.getWindowHandle();
                org.openqa.selenium.bidi.browsingcontext.BrowsingContext context =
                        new org.openqa.selenium.bidi.browsingcontext.BrowsingContext(driver, handle);
                if (promptText != null) {
                    context.handleUserPrompt(accept, promptText);
                } else {
                    context.handleUserPrompt(accept);
                }
                Ellithium.core.reporting.Reporter.log("BiDi: Handled user prompt (accept=" + accept + ")", Ellithium.core.logging.LogLevel.INFO_GREEN);
                return true;
            } catch (Exception ignored) {
                // BiDi prompt handling not supported on current context or prompt not open; fall through to wait
            }
        }
        return false;
    }

    /**
     * Accepts an alert with specified timeout.
     * Uses WebDriver BiDi if active on the current session, falling back to fluent wait.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void accept(int timeout, int pollingEvery) {
        if (tryHandlePromptBiDi(true, null)) return;
        org.openqa.selenium.Alert alert = getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        alert.accept();
    }

    /**
     * Dismisses an alert with specified timeout.
     * Uses WebDriver BiDi if active on the current session, falling back to fluent wait.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void dismiss(int timeout, int pollingEvery) {
        if (tryHandlePromptBiDi(false, null)) return;
        org.openqa.selenium.Alert alert = getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        alert.dismiss();
    }

    /**
     * Gets the text of an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The alert text
     */
    public String getText(int timeout, int pollingEvery) {
        org.openqa.selenium.Alert alert = getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        return alert.getText();
    }

    /**
     * Sends data to an alert with specified timeout.
     * Uses WebDriver BiDi if active on the current session, falling back to fluent wait.
     * @param data The data to send
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void sendData(String data, int timeout, int pollingEvery) {
        if (tryHandlePromptBiDi(true, data)) return;
        org.openqa.selenium.Alert alert = getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        alert.sendKeys(data);
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
