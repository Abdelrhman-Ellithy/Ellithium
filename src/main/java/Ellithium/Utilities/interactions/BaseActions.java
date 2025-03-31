package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.List;

public class BaseActions<T extends WebDriver> {
    protected final T driver;
    protected BaseActions(T driver) {
        this.driver = driver;
    }
    /**
     * Gets a FluentWait instance with specified timeout and polling interval.
     * @param timeoutInSeconds Maximum wait time in seconds
     * @param pollingEveryInMillis Polling interval in milliseconds
     * @return FluentWait instance
     */
    @SuppressWarnings("unchecked")
    protected FluentWait<T> getFluentWait(int timeoutInSeconds, int pollingEveryInMillis) {
        return WaitManager.getFluentWait(driver, timeoutInSeconds, pollingEveryInMillis);
    }
    /**
     * Finds a WebElement using the given locator.
     * @param locator Element locator
     * @return The found WebElement
     */
    public  WebElement findWebElement( By locator) {
        return driver.findElement(locator);
    }

    /**
     * Finds all WebElements matching the given locator.
     * @param locator Element locator
     * @return List of found WebElements
     */
    public List<WebElement> findWebElements(By locator) {
        return driver.findElements(locator);
    }
}
