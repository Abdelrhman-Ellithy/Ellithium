package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.File;

public class JavaScriptActions<T extends WebDriver> extends BaseActions<T>{

    /**
     * Creates a new DriverActions instance.
     * @param driver WebDriver instance to wrap
     */
    @SuppressWarnings("unchecked")
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
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick( locator);
    }

    /**
     * Clicks an element using JavaScript.
     * @param locator Element locator
     */
    public  void javascriptClick( By locator) {
        WebElement element = findWebElement( locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        Reporter.log("JavaScript Click On Element: ", LogLevel.INFO_BLUE,locator.toString());
    }
    /**
     * Scrolls the page to bring an element into view.
     * @param locator Element locator
     */
    public void scrollToElement(By locator) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", findWebElement( locator));
        Reporter.log("Scrolling To Element: ",LogLevel.INFO_BLUE,locator.toString());
    }
    /**
     * Clicks an element using JavaScript with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public  void javascriptClick( By locator, int timeout) {
        getFluentWait( timeout, WaitManager.getDefaultPollingTime())
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick( locator);
    }
    /**
     * Scrolls the page by offset.
     * @param xOffset X offset to scroll
     * @param yOffset Y offset to scroll
     */
    public  void scrollByOffset( int xOffset, int yOffset) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(arguments[0], arguments[1]);", xOffset, yOffset);

        Reporter.log("Scrolled by offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }
    /**
     * Sets the value of an element using JavaScript.
     * @param locator Element locator
     * @param value Value to set
     */
    public  void setElementValueUsingJS( By locator, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement element = findWebElement( locator);
        js.executeScript("arguments[0].value = arguments[1];", element, value);
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
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + filePath);
            }

            WebElement uploadElement = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.presenceOfElementLocated(fileUploadLocator));

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].style.display='block';", uploadElement);
            uploadElement.sendKeys(file.getAbsolutePath());
            Reporter.log("File uploaded successfully using JavaScript: " + file.getName(), LogLevel.INFO_BLUE);
        } catch (Exception e) {
            Reporter.log("Failed to upload file using JavaScript: " + e.getMessage(), LogLevel.ERROR);
            throw e;
        }
    }
}
