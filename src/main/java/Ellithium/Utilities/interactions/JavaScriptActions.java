package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class JavaScriptActions<T extends WebDriver> extends BaseActions<T> {

    /**
     * Creates a new DriverActions instance.
     * @param driver WebDriver instance to wrap
     */
    public JavaScriptActions(T driver) {
        super(driver);
    }

    /**
     * Clicks an element using JavaScript with specified timeout and polling interval.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void javascriptClick(By locator, int timeout, int pollingEvery) {
        requireJavascriptContext("javascriptClick");
        performWithStaleRetry(locator, timeout, pollingEvery,
                el -> ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el));
        Reporter.log("JavaScript Click On Element: ", LogLevel.INFO_BLUE, locator.toString());
    }

    /**
     * Clicks an element using JavaScript.
     * @param locator Element locator
     */
    public void javascriptClick(By locator) {
        requireJavascriptContext("javascriptClick");
        performWithStaleRetry(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime(),
                el -> ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el));
        Reporter.log("JavaScript Click On Element: ", LogLevel.INFO_BLUE, locator.toString());
    }

    /**
     * Clicks an element using JavaScript with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public void javascriptClick(By locator, int timeout) {
        requireJavascriptContext("javascriptClick");
        performWithStaleRetry(locator, timeout, WaitManager.getDefaultPollingTime(),
                el -> ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el));
        Reporter.log("JavaScript Click On Element: ", LogLevel.INFO_BLUE, locator.toString());
    }

    /**
     * Scrolls the page to bring an element into view.
     * @param locator Element locator
     */
    public void scrollToElement(By locator) {
        requireJavascriptContext("scrollToElement");
        performWithStaleRetry(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime(),
                el -> ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el));
        Reporter.log("Scrolling To Element: ", LogLevel.INFO_BLUE, locator.toString());
    }

    /**
     * Scrolls the page by offset.
     * @param xOffset X offset to scroll
     * @param yOffset Y offset to scroll
     */
    public void scrollByOffset(int xOffset, int yOffset) {
        requireJavascriptContext("scrollByOffset");
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(arguments[0], arguments[1]);", xOffset, yOffset);
        Reporter.log("Scrolled by offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }

    /**
     * Sets the value of an element using JavaScript.
     * @param locator Element locator
     * @param value Value to set
     */
    public void setElementValueUsingJS(By locator, String value) {
        requireJavascriptContext("setElementValueUsingJS");
        performWithStaleRetry(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime(),
                el -> ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", el, value));
        Reporter.log("Set value using JavaScript: " + value + " on element: " + locator.toString(), LogLevel.INFO_BLUE);
    }

    /**
     * Uploads a file using JavaScript with default timeout and polling time.
     * @param fileUploadLocator Locator for the file input element
     * @param filePath Absolute path to the file to upload
     */
    public void uploadFileUsingJS(By fileUploadLocator, String filePath) {
        uploadFileUsingJS(fileUploadLocator, filePath, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Uploads a file using JavaScript.
     * Useful when standard sendKeys method doesn't work.
     * @param fileUploadLocator Locator for the file input element
     * @param filePath Absolute path to the file to upload
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @throws IllegalArgumentException if file does not exist
     */
    public void uploadFileUsingJS(By fileUploadLocator, String filePath, int timeout, int pollingEvery) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        performWithStaleRetry(fileUploadLocator, timeout, pollingEvery, el -> {
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='block';", el);
            el.sendKeys(file.getAbsolutePath());
        });
        Reporter.log("File uploaded successfully using JavaScript: " + file.getName(), LogLevel.INFO_BLUE);
    }
}
