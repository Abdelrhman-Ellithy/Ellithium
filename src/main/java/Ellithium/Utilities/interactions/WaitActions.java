package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.util.List;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WaitActions <T extends WebDriver> extends BaseActions<T>{
    public WaitActions(T driver) {
        super(driver);
    }

    /**
     * Waits for an element to disappear from the DOM.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void waitForElementToDisappear( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        Reporter.log("Waiting for Element To Disappear: ",LogLevel.INFO_BLUE,locator.toString());
    }

    /**
     * Waits for an element to be clickable.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The clickable WebElement
     */
    public  WebElement waitForElementToBeClickable( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        Reporter.log("Wait For Element To Be Clickable: ",LogLevel.INFO_BLUE,locator.toString());
        return findWebElement(locator);
    }

    /**
     * Waits for an element to be visible.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The visible WebElement
     */
    public  WebElement waitForElementToBeVisible( By locator, int timeout, int pollingEvery) {
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Reporter.log("Wait For Element To Be Visible: ",LogLevel.INFO_BLUE,locator.toString());
        return findWebElement( locator);
    }

    /**
     * Waits for an element to be present in the DOM.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The present WebElement
     */
    public  WebElement waitForElementPresence( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Presence: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        return findWebElement( locator);
    }

    /**
     * Waits for specific text to be present in an element.
     * @param locator Element locator
     * @param text The text to wait for
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The WebElement with the specified text
     */
    public  WebElement waitForTextToBePresentInElement( By locator, String text, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Text: '" + text + "' to be present in Element: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
        return findWebElement( locator);
    }


    /**
     * Waits for an element to be selected.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the element is selected, false otherwise
     */
    public  boolean waitForElementToBeSelected( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element to be Selected: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeSelected(locator));
    }

    /**
     * Waits for an element's attribute to have a specific value.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param value Expected attribute value
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the attribute has the expected value, false otherwise
     */
    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Attribute: '" + attribute + "' to be: '" + value + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.attributeToBe(locator, attribute, value));
    }

    /**
     * Waits for an element's attribute to contain a specific value.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param value Expected attribute value
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the attribute contains the expected value, false otherwise
     */
    public  boolean waitForElementAttributeContains( By locator, String attribute, String value, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Attribute: '" + attribute + "' to contain: '" + value + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.attributeContains(locator, attribute, value));
    }

    /**
     * Waits for an element to become stale.
     * @param element The WebElement to wait for
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the element becomes stale, false otherwise
     */
    public  boolean waitForElementStaleness( WebElement element, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Staleness: " + element.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.stalenessOf(element));
    }

    /**
     * Waits for the page title to contain a specific text.
     * @param titlePart The text to wait for in the title
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the title contains the text, false otherwise
     */
    public  boolean waitForTitleContains( String titlePart, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Title to Contain: '" + titlePart + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.titleContains(titlePart));
    }

    /**
     * Waits for the page URL to contain a specific text.
     * @param urlPart The text to wait for in the URL
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the URL contains the text, false otherwise
     */
    public  boolean waitForUrlContains( String urlPart, int timeout, int pollingEvery) {
        Reporter.log("Waiting for URL to Contain: '" + urlPart + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.urlContains(urlPart));
    }

    /**
     * Waits for a frame to be available and switches to it.
     * @param locator Frame locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available and Switching to it: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    /**
     * Waits for a frame to be available by name or ID and switches to it.
     * @param nameOrId Frame name or ID
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available by Name or ID: '" + nameOrId + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
    }

    /**
     * Waits for a frame to be available by index and switches to it.
     * @param index Frame index
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available by Index: " + index, LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }

    /**
     * Waits for an element to be enabled.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the element is enabled, false otherwise
     */
    public  boolean waitForElementToBeEnabled( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element to be Enabled: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator)).isEnabled();
    }

    /**
     * Waits for the page title to be a specific text.
     * @param title The expected title
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the title matches the expected title, false otherwise
     */
    public  boolean waitForTitleIs( String title, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Title to be: '" + title + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.titleIs(title));
    }

    /**
     * Waits for the page URL to be a specific URL.
     * @param url The expected URL
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the URL matches the expected URL, false otherwise
     */
    public  boolean waitForUrlToBe( String url, int timeout, int pollingEvery) {
        Reporter.log("Waiting for URL to be: '" + url + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.urlToBe(url));
    }

    /**
     * Waits for an element's selection state to be a specific state.
     * @param locator Element locator
     * @param selected Expected selection state
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the element has the expected selection state, false otherwise
     */
    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Selection State to be: " + selected + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementSelectionStateToBe(locator, selected));
    }

    /**
     * Waits for specific text to be present in an element's value attribute.
     * @param locator Element locator
     * @param text The text to wait for in the value attribute
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the text is present in the element's value attribute, false otherwise
     */
    public  boolean waitForTextToBePresentInElementValue( By locator, String text, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Text to be Present in Element Value: '" + text + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementValue(locator, text));
    }

    /**
     * Waits for the number of windows to be a specific number.
     * @param numberOfWindows The expected number of windows
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the number of windows matches the expected number, false otherwise
     */
    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Windows to be: " + numberOfWindows, LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfWindowsToBe(numberOfWindows));
    }

    /**
     * Waits for the number of elements to be more than a specific number.
     * @param locator Element locator
     * @param number The number to compare against
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the number of elements is more than the specified number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBeMoreThan( By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be More Than: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(locator, number)).size();
        return size > number;
    }

    /**
     * Waits for the number of elements to be less than a specific number.
     * @param locator Element locator
     * @param number The number to compare against
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the number of elements is less than the specified number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBeLessThan( By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be Less Than: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBeLessThan(locator, number)).size();
        return size < number;
    }

    /**
     * Waits for visibility of all elements located by the given locator.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of visible WebElements
     */
    public List<WebElement> waitForVisibilityOfAllElements(By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Visibility of All Elements for: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /**
     * Waits for the number of elements to be a specific number.
     * @param locator Element locator
     * @param number The expected number of elements
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return True if the number of elements matches the expected number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBe( By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBe(locator, number)).size();
        return size == number;
    }

    /**
     * Waits for an element to be selected with default timeout and polling time.
     * @param locator Element locator
     * @return True if the element is selected, false otherwise
     */
    public  boolean waitForElementToBeSelected( By locator) {
        return waitForElementToBeSelected( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to be selected with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return True if the element is selected, false otherwise
     */
    public  boolean waitForElementToBeSelected( By locator, int timeout) {
        return waitForElementToBeSelected( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element's attribute to have a specific value with default timeout and polling time.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param value Expected attribute value
     * @return True if the attribute has the expected value, false otherwise
     */
    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value) {
        return waitForElementAttributeToBe( locator, attribute, value, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element's attribute to have a specific value with specified timeout.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param value Expected attribute value
     * @param timeout Maximum wait time in seconds
     * @return True if the attribute has the expected value, false otherwise
     */
    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value, int timeout) {
        return waitForElementAttributeToBe( locator, attribute, value, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element's attribute to contain a specific value with default timeout and polling time.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param value Expected attribute value
     * @return True if the attribute contains the expected value, false otherwise
     */
    public  boolean waitForElementAttributeContains( By locator, String attribute, String value) {
        return waitForElementAttributeContains( locator, attribute, value, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element's attribute to contain a specific value with specified timeout.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param value Expected attribute value
     * @param timeout Maximum wait time in seconds
     * @return True if the attribute contains the expected value, false otherwise
     */
    public  boolean waitForElementAttributeContains( By locator, String attribute, String value, int timeout) {
        return waitForElementAttributeContains( locator, attribute, value, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to become stale with default timeout and polling time.
     * @param element The WebElement to wait for
     * @return True if the element becomes stale, false otherwise
     */
    public  boolean waitForElementStaleness( WebElement element) {
        return waitForElementStaleness( element, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to become stale with specified timeout.
     * @param element The WebElement to wait for
     * @param timeout Maximum wait time in seconds
     * @return True if the element becomes stale, false otherwise
     */
    public  boolean waitForElementStaleness( WebElement element, int timeout) {
        return waitForElementStaleness( element, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the page title to contain a specific text with default timeout and polling time.
     * @param titlePart The text to wait for in the title
     * @return True if the title contains the text, false otherwise
     */
    public  boolean waitForTitleContains( String titlePart) {
        return waitForTitleContains( titlePart, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the page title to contain a specific text with specified timeout.
     * @param titlePart The text to wait for in the title
     * @param timeout Maximum wait time in seconds
     * @return True if the title contains the text, false otherwise
     */
    public  boolean waitForTitleContains( String titlePart, int timeout) {
        return waitForTitleContains( titlePart, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the page URL to contain a specific text with default timeout and polling time.
     * @param urlPart The text to wait for in the URL
     * @return True if the URL contains the text, false otherwise
     */
    public  boolean waitForUrlContains( String urlPart) {
        return waitForUrlContains( urlPart, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the page URL to contain a specific text with specified timeout.
     * @param urlPart The text to wait for in the URL
     * @param timeout Maximum wait time in seconds
     * @return True if the URL contains the text, false otherwise
     */
    public  boolean waitForUrlContains( String urlPart, int timeout) {
        return waitForUrlContains( urlPart, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available and switches to it with default timeout and polling time.
     * @param locator Frame locator
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator) {
        return waitForFrameToBeAvailableAndSwitchToIt( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available and switches to it with specified timeout.
     * @param locator Frame locator
     * @param timeout Maximum wait time in seconds
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator, int timeout) {
        return waitForFrameToBeAvailableAndSwitchToIt( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available by name or ID and switches to it with default timeout and polling time.
     * @param nameOrId Frame name or ID
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId) {
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(nameOrId, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available by name or ID and switches to it with specified timeout.
     * @param nameOrId Frame name or ID
     * @param timeout Maximum wait time in seconds
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId, int timeout) {
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( nameOrId, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available by index and switches to it with default timeout and polling time.
     * @param index Frame index
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index) {
        return waitForFrameByIndexToBeAvailableAndSwitchToIt( index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for a frame to be available by index and switches to it with specified timeout.
     * @param index Frame index
     * @param timeout Maximum wait time in seconds
     * @return The WebDriver instance switched to the frame
     */
    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index, int timeout) {
        return waitForFrameByIndexToBeAvailableAndSwitchToIt( index, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to be enabled with default timeout and polling time.
     * @param locator Element locator
     * @return True if the element is enabled, false otherwise
     */
    public  boolean waitForElementToBeEnabled( By locator) {
        return waitForElementToBeEnabled(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to be enabled with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return True if the element is enabled, false otherwise
     */
    public  boolean waitForElementToBeEnabled( By locator, int timeout) {
        return waitForElementToBeEnabled( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the page title to be a specific text with default timeout and polling time.
     * @param title The expected title
     * @return True if the title matches the expected title, false otherwise
     */
    public  boolean waitForTitleIs( String title) {
        return waitForTitleIs(title, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the page title to be a specific text with specified timeout.
     * @param title The expected title
     * @param timeout Maximum wait time in seconds
     * @return True if the title matches the expected title, false otherwise
     */
    public  boolean waitForTitleIs( String title, int timeout) {
        return waitForTitleIs( title, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the page URL to be a specific URL with default timeout and polling time.
     * @param url The expected URL
     * @return True if the URL matches the expected URL, false otherwise
     */
    public  boolean waitForUrlToBe( String url) {
        return waitForUrlToBe( url, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the page URL to be a specific URL with specified timeout.
     * @param url The expected URL
     * @param timeout Maximum wait time in seconds
     * @return True if the URL matches the expected URL, false otherwise
     */
    public  boolean waitForUrlToBe( String url, int timeout) {
        return waitForUrlToBe( url, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element's selection state to be a specific state with default timeout and polling time.
     * @param locator Element locator
     * @param selected Expected selection state
     * @return True if the element has the expected selection state, false otherwise
     */
    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected) {
        return waitForElementSelectionStateToBe( locator, selected, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element's selection state to be a specific state with specified timeout.
     * @param locator Element locator
     * @param selected Expected selection state
     * @param timeout Maximum wait time in seconds
     * @return True if the element has the expected selection state, false otherwise
     */
    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected, int timeout) {
        return waitForElementSelectionStateToBe( locator, selected, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for specific text to be present in an element's value attribute with default timeout and polling time.
     * @param locator Element locator
     * @param text The text to wait for in the value attribute
     * @return True if the text is present in the element's value attribute, false otherwise
     */
    public  boolean waitForTextToBePresentInElementValue( By locator, String text) {
        return waitForTextToBePresentInElementValue( locator, text, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for specific text to be present in an element's value attribute with specified timeout.
     * @param locator Element locator
     * @param text The text to wait for in the value attribute
     * @param timeout Maximum wait time in seconds
     * @return True if the text is present in the element's value attribute, false otherwise
     */
    public  boolean waitForTextToBePresentInElementValue( By locator, String text, int timeout) {
        return waitForTextToBePresentInElementValue( locator, text, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the number of windows to be a specific number with default timeout and polling time.
     * @param numberOfWindows The expected number of windows
     * @return True if the number of windows matches the expected number, false otherwise
     */
    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows) {
        return waitForNumberOfWindowsToBe( numberOfWindows, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the number of windows to be a specific number with specified timeout.
     * @param numberOfWindows The expected number of windows
     * @param timeout Maximum wait time in seconds
     * @return True if the number of windows matches the expected number, false otherwise
     */
    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows, int timeout) {
        return waitForNumberOfWindowsToBe( numberOfWindows, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the number of elements to be more than a specific number with default timeout and polling time.
     * @param locator Element locator
     * @param number The number to compare against
     * @return True if the number of elements is more than the specified number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBeMoreThan( By locator, int number) {
        return waitForNumberOfElementsToBeMoreThan( locator, number, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the number of elements to be more than a specific number with specified timeout.
     * @param locator Element locator
     * @param number The number to compare against
     * @param timeout Maximum wait time in seconds
     * @return True if the number of elements is more than the specified number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBeMoreThan( By locator, int number, int timeout) {
        return waitForNumberOfElementsToBeMoreThan( locator, number, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the number of elements to be less than a specific number with default timeout and polling time.
     * @param locator Element locator
     * @param number The number to compare against
     * @return True if the number of elements is less than the specified number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBeLessThan( By locator, int number) {
        return waitForNumberOfElementsToBeLessThan( locator, number, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    /**
     * Waits for specific text to be present in an element with default timeout and polling time.
     * @param locator Element locator
     * @param text The text to wait for
     * @return The WebElement with the specified text
     */
    public  WebElement waitForTextToBePresentInElement( By locator, String text) {
        return waitForTextToBePresentInElement( locator, text, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for specific text to be present in an element with specified timeout.
     * @param locator Element locator
     * @param text The text to wait for
     * @param timeout Maximum wait time in seconds
     * @return The WebElement with the specified text
     */
    public  WebElement waitForTextToBePresentInElement( By locator, String text, int timeout) {
        return waitForTextToBePresentInElement( locator, text, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the number of elements to be less than a specific number with specified timeout.
     * @param locator Element locator
     * @param number The number to compare against
     * @param timeout Maximum wait time in seconds
     * @return True if the number of elements is less than the specified number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBeLessThan( By locator, int number, int timeout) {
        return waitForNumberOfElementsToBeLessThan( locator, number, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for visibility of all elements located by the given locator with default timeout and polling time.
     * @param locator Element locator
     * @return List of visible WebElements
     */
    public  List<WebElement> waitForVisibilityOfAllElements( By locator) {
        return waitForVisibilityOfAllElements( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for visibility of all elements located by the given locator with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return List of visible WebElements
     */
    public  List<WebElement> waitForVisibilityOfAllElements( By locator, int timeout) {
        return waitForVisibilityOfAllElements( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the number of elements to be a specific number with default timeout and polling time.
     * @param locator Element locator
     * @param number The expected number of elements
     * @return True if the number of elements matches the expected number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBe( By locator, int number) {
        return waitForNumberOfElementsToBe( locator, number, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for the number of elements to be a specific number with specified timeout.
     * @param locator Element locator
     * @param number The expected number of elements
     * @param timeout Maximum wait time in seconds
     * @return True if the number of elements matches the expected number, false otherwise
     */
    public  boolean waitForNumberOfElementsToBe( By locator, int number, int timeout) {
        return waitForNumberOfElementsToBe( locator, number, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to be clickable with default timeout and polling time.
     * @param locator Element locator
     * @return The clickable WebElement
     */
    public  WebElement waitForElementToBeClickable( By locator) {
        return waitForElementToBeClickable( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to be visible with default timeout and polling time.
     * @param locator Element locator
     * @return The visible WebElement
     */
    public  WebElement waitForElementToBeVisible( By locator) {
        return waitForElementToBeVisible( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to be present in the DOM with default timeout and polling time.
     * @param locator Element locator
     * @return The present WebElement
     */
    public  WebElement waitForElementPresence( By locator) {
        return waitForElementPresence( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Waits for an element to disappear from the DOM with default timeout and polling time.
     * @param locator Element locator
     */
    public  void waitForElementToDisappear( By locator) {
        waitForElementToDisappear(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    /**
     * Creates a WebDriverWait instance with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @return WebDriverWait instance
     */
    public WebDriverWait getGeneralWait(int timeout) {
        Reporter.log("Getting general Wait For "+ timeout + " seconds", LogLevel.INFO_BLUE);
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

}
