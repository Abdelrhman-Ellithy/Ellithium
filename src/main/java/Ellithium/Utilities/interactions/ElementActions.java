package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;

public class ElementActions<T extends WebDriver> extends BaseActions<T> {
    
    public ElementActions(T driver) {
        super(driver);
    }

    /**
     * Sends text data to an element after waiting for it to be visible.
     * @param locator Element locator
     * @param data Text to send
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void sendData(By locator, String data, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(locator).clear();
        findWebElement(locator).sendKeys(data);
    }

    /**
     * Sends keyboard keys to an element after waiting for it to be visible.
     * @param locator Element locator
     * @param data Keys to send
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void sendData(By locator, Keys data, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(locator).clear();
        findWebElement(locator).sendKeys(data);
    }

    /**
     * Clicks an element after waiting for it to be visible.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void clickOnElement(By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(locator).click();
    }

    /**
     * Retrieves text from an element after waiting for it to be visible.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return Text content of the element
     */
    public String getText(By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        return findWebElement(locator).getText();
    }

    /**
     * Gets the text from multiple elements.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of text from the elements
     */
    public List<String> getTextFromMultipleElements(By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery).until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        Reporter.log("Getting text from multiple elements located: ", LogLevel.INFO_BLUE, locator.toString());
        List<WebElement> elements = findWebElements(locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }

    /**
     * Gets the value of an attribute from multiple elements.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of attribute values from the elements
     */
    public List<String> getAttributeFromMultipleElements(By locator, String attribute, int timeout, int pollingEvery) {
        Reporter.log("Getting Attribute from multiple elements located: ", LogLevel.INFO_BLUE, locator.toString());
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getDomAttribute(attribute));
        }
        return texts;
    }

    /**
     * Clicks on multiple elements.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void clickOnMultipleElements(By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(locator);
        for (WebElement element : elements) {
            element.click();
        }
    }

    /**
     * Retrieves the value of an attribute from an element.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The attribute value
     */
    public String getAttributeValue(By locator, String attribute, int timeout, int pollingEvery) {
        Reporter.log("Getting Attribute: '" + attribute + "' from Element: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        return findWebElement(locator).getDomAttribute(attribute);
    }

    /**
     * Gets all elements matching the locator.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of found WebElements
     */
    public List<WebElement> getElements(By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        return findWebElements(locator);
    }

    // Overloaded methods with default timeouts

    /**
     * Sends text data to an element with default timeout and polling time.
     * @param locator Element locator
     * @param data Text to send
     */
    public void sendData(By locator, String data) {
        sendData(locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends text data to an element with specified timeout.
     * @param locator Element locator
     * @param data Text to send
     * @param timeout Maximum wait time in seconds
     */
    public void sendData(By locator, String data, int timeout) {
        sendData(locator, data, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends keyboard keys to an element with default timeout and polling time.
     * @param locator Element locator
     * @param data Keys to send
     */
    public void sendData(By locator, Keys data) {
        sendData(locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends keyboard keys to an element with specified timeout.
     * @param locator Element locator
     * @param data Keys to send
     * @param timeout Maximum wait time in seconds
     */
    public void sendData(By locator, Keys data, int timeout) {
        sendData(locator, data, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Clicks an element with default timeout and polling time.
     * @param locator Element locator
     */
    public void clickOnElement(By locator) {
        clickOnElement(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Clicks an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public void clickOnElement(By locator, int timeout) {
        clickOnElement(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Retrieves text from an element with default timeout and polling time.
     * @param locator Element locator
     * @return Text content of the element
     */
    public String getText(By locator) {
        return getText(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Retrieves text from an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return Text content of the element
     */
    public String getText(By locator, int timeout) {
        return getText(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the text from multiple elements with default timeout and polling time.
     * @param locator Element locator
     * @return List of text from the elements
     */
    public List<String> getTextFromMultipleElements(By locator) {
        return getTextFromMultipleElements(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the text from multiple elements with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return List of text from the elements
     */
    public List<String> getTextFromMultipleElements(By locator, int timeout) {
        return getTextFromMultipleElements(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the value of an attribute from multiple elements with default timeout and polling time.
     * @param locator Element locator
     * @param attribute Attribute name
     * @return List of attribute values from the elements
     */
    public List<String> getAttributeFromMultipleElements(By locator, String attribute) {
        return getAttributeFromMultipleElements(locator, attribute, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the value of an attribute from multiple elements with specified timeout.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param timeout Maximum wait time in seconds
     * @return List of attribute values from the elements
     */
    public List<String> getAttributeFromMultipleElements(By locator, String attribute, int timeout) {
        return getAttributeFromMultipleElements(locator, attribute, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Clicks on multiple elements with default timeout and polling time.
     * @param locator Element locator
     */
    public void clickOnMultipleElements(By locator) {
        clickOnMultipleElements(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Clicks on multiple elements with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public void clickOnMultipleElements(By locator, int timeout) {
        clickOnMultipleElements(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Retrieves the value of an attribute from an element with default timeout and polling time.
     * @param locator Element locator
     * @param attribute Attribute name
     * @return The attribute value
     */
    public String getAttributeValue(By locator, String attribute) {
        return getAttributeValue(locator, attribute, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets all elements matching the locator with default timeout and polling time.
     * @param locator Element locator
     * @return List of found WebElements
     */
    public List<WebElement> getElements(By locator) {
        return getElements(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets all elements matching the locator with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return List of found WebElements
     */
    public List<WebElement> getElements(By locator, int timeout) {
        return getElements(locator, timeout, WaitManager.getDefaultPollingTime());
    }
}
