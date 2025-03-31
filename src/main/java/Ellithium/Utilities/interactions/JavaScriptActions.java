package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
public class JavaScriptActions<T extends WebDriver> {
    private final T driver;
    /**
     * Creates a new DriverActions instance.
     * @param driver WebDriver instance to wrap
     */
    @SuppressWarnings("unchecked")
    public JavaScriptActions(T driver) {
        this.driver = driver;
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
     * Scrolls to the bottom of the page.
     */
    public  void scrollToPageBottom() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Reporter.log("Scrolled to page bottom", LogLevel.INFO_BLUE);
    }
    @SuppressWarnings("unchecked")
    private FluentWait<T> getFluentWait(int timeoutInSeconds, int pollingEveryInMillis) {
        return WaitManager.getFluentWait(driver,timeoutInSeconds,pollingEveryInMillis);
    }
    /**
     * Finds a WebElement using the given locator.
     * @param locator Element locator
     * @return The found WebElement
     */
    private WebElement findWebElement( By locator) {
        return driver.findElement(locator);
    }
}
