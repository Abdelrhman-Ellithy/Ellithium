package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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

    /**
     * Safely iterates over elements using a consumer function.
     * Re-locates elements on each iteration to prevent stale element exceptions.
     * Handles dynamic list size changes gracefully.
     * 
     * @param locator Element locator
     * @param action Consumer function to apply to each element
     */
    protected void forEachElementSafely(By locator, Consumer<WebElement> action) {
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            List<WebElement> currentElements = findWebElements(locator);
            if (currentIndex >= currentElements.size()) {
                break; // No more elements or list shrunk
            }
            
            try {
                WebElement element = currentElements.get(currentIndex);
                action.accept(element);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (StaleElementReferenceException e) {
                // Element became stale, re-query and retry same index
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    // Too many consecutive failures, move to next index
                    currentIndex++;
                    consecutiveFailures = 0;
                }
                // Continue to retry same index
            } catch (IndexOutOfBoundsException e) {
                // List shrunk during iteration, exit gracefully
                break;
            }
        }
    }

    /**
     * Safely maps elements to a result list using a function.
     * Re-locates elements on each iteration to prevent stale element exceptions.
     * Handles dynamic list size changes gracefully.
     * 
     * @param <R> The type of result
     * @param locator Element locator
     * @param mapper Function to transform each element to a result
     * @return List of results from applying the mapper function
     */
    protected <R> List<R> mapElementsSafely(By locator, Function<WebElement, R> mapper) {
        List<R> results = new ArrayList<>();
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            List<WebElement> currentElements = findWebElements(locator);
            if (currentIndex >= currentElements.size()) {
                break;
            }
            
            try {
                WebElement element = currentElements.get(currentIndex);
                R result = mapper.apply(element);
                results.add(result);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (StaleElementReferenceException e) {
                // Re-query and retry same index
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    // Too many consecutive failures, skip this index
                    currentIndex++;
                    consecutiveFailures = 0;
                }
                // Continue to retry same index
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }
        return results;
    }

    /**
     * Safely retrieves an element by index with bounds checking.
     * Re-queries the element list to prevent stale element exceptions.
     * 
     * @param locator Element locator
     * @param index Zero-based index of the element
     * @return The WebElement at the specified index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    protected WebElement findElementByIndexSafely(By locator, int index) {
        List<WebElement> elements = findWebElements(locator);
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for list of size " + elements.size());
        }
        return elements.get(index);
    }

    /**
     * Gets the current count of elements matching the locator.
     * Re-queries each time to get the most up-to-date count.
     * 
     * @param locator Element locator
     * @return Current number of elements matching the locator
     */
    protected int getElementCount(By locator) {
        return findWebElements(locator).size();
    }

    /**
     * Safely maps Select dropdown options to a result list using a function.
     * Re-creates the Select object and re-queries options on each iteration
     * to prevent stale element exceptions. Handles dynamic list size changes gracefully.
     * 
     * @param <R> The type of result
     * @param locator Dropdown element locator
     * @param mapper Function to transform each option element to a result
     * @return List of results from applying the mapper function
     */
    protected <R> List<R> mapSelectOptionsSafely(By locator, Function<WebElement, R> mapper) {
        List<R> results = new ArrayList<>();
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            try {
                org.openqa.selenium.support.ui.Select dropDown = new org.openqa.selenium.support.ui.Select(findWebElement(locator));
                List<WebElement> currentOptions = dropDown.getAllSelectedOptions();
                if (currentIndex >= currentOptions.size()) {
                    break;
                }
                WebElement option = currentOptions.get(currentIndex);
                R result = mapper.apply(option);
                results.add(result);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (StaleElementReferenceException | IndexOutOfBoundsException e) {
                // Re-query and retry same index
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    // Too many consecutive failures, skip this index
                    currentIndex++;
                    consecutiveFailures = 0;
                }
                // Continue to retry same index
            }
        }
        return results;
    }

    /**
     * Safely iterates over Select dropdown elements using a consumer function.
     * Re-creates the Select object on each iteration to prevent stale element exceptions.
     * Handles dynamic list size changes gracefully.
     * 
     * @param locator Dropdown element locator
     * @param action Consumer function to apply to each dropdown element
     */
    protected void forEachSelectElementSafely(By locator, Consumer<org.openqa.selenium.support.ui.Select> action) {
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            try {
                List<WebElement> currentElements = findWebElements(locator);
                if (currentIndex >= currentElements.size()) {
                    break; // No more elements or list shrunk
                }
                
                WebElement element = currentElements.get(currentIndex);
                org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(element);
                action.accept(select);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (StaleElementReferenceException e) {
                // Element became stale, re-query and retry same index
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    // Too many consecutive failures, move to next index
                    currentIndex++;
                    consecutiveFailures = 0;
                }
                // Continue to retry same index
            } catch (IndexOutOfBoundsException e) {
                // List shrunk during iteration, exit gracefully
                break;
            }
        }
    }
}
