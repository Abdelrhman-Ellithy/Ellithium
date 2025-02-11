package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a comprehensive set of WebDriver interaction methods with built-in waits and reporting.
 * @param <T> The specific WebDriver type
 */
public class DriverActions<T extends WebDriver> {
    private final T driver;

    /**
     * Creates a new DriverActions instance.
     * @param driver WebDriver instance to wrap
     */
    @SuppressWarnings("unchecked")
    public DriverActions(T driver) {
        this.driver = driver;
    }

    /**
     * Sends text data to an element after waiting for it to be visible.
     * @param locator Element locator
     * @param data Text to send
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void sendData(By locator, String data, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
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
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(locator).clear();
        findWebElement(locator).sendKeys(data);
    }

    /**
     * Retrieves text from an element after waiting for it to be visible.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return Text content of the element
     */
    public String getText(By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        String text = findWebElement(locator).getText();
        return text;
    }

    /**
     * Clicks an element after waiting for it to be visible.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void clickOnElement(By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(locator).click();
    }

    /**
     * Creates a WebDriverWait instance with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @return WebDriverWait instance
     */
    public WebDriverWait generalWait(int timeout) {
        Reporter.log("Getting general Wait For "+ timeout + " seconds", LogLevel.INFO_BLUE);
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
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
     * Selects a dropdown option by visible text.
     * @param locator Dropdown element locator
     * @param option Text of the option to select
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void selectDropdownByText(By locator, String option, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement( locator)).selectByVisibleText(option);
        Reporter.log("Selecting Dropdown Option By Text: " + option + " From Element: ",LogLevel.INFO_BLUE,locator.toString());
    }

    /**
     * Selects a dropdown option by value.
     * @param locator Dropdown element locator
     * @param value Value of the option to select
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void selectDropdownByValue( By locator, String value, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement( locator)).selectByValue(value);
        Reporter.log("Selecting Dropdown Option By Value: " + value + " From Element: ",LogLevel.INFO_BLUE,locator.toString());
    }

    /**
     * Selects a dropdown option by index.
     * @param locator Dropdown element locator
     * @param index Index of the option to select
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void selectDropdownByIndex( By locator, int index, int timeout, int pollingEvery) {
        Reporter.log("Selecting Dropdown Option By Index: " + index + " From Element: " ,LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement( locator)).selectByIndex(index);
    }

    /**
     * Retrieves the selected options' texts from a dropdown.
     * @param locator Dropdown element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of selected options' texts
     */
    public List<String> getDropdownSelectedOptions( By locator, int timeout, int pollingEvery) {
        Reporter.log("Getting Dropdown Options Texts: " ,LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Select dropDown= new Select(findWebElement( locator));
        List<WebElement>elements =dropDown.getAllSelectedOptions();
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }

    /**
     * Retrieves the selected options' texts from a dropdown with default polling time.
     * @param locator Dropdown element locator
     * @param timeout Maximum wait time in seconds
     * @return List of selected options' texts
     */
    public List<String> getDropdownSelectedOptions( By locator, int timeout) {
        Reporter.log("Getting Dropdown Options Texts: " ,LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(timeout,WaitManager.getDefaultPollingTime())
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Select dropDown= new Select(findWebElement( locator));
        List<WebElement>elements =dropDown.getAllSelectedOptions();
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }

    /**
     * Retrieves the selected options' texts from a dropdown with default timeout and polling time.
     * @param locator Dropdown element locator
     * @return List of selected options' texts
     */
    public List<String> getDropdownSelectedOptions( By locator) {
        Reporter.log("Getting Dropdown Options Texts: " ,LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(WaitManager.getDefaultTimeout(),WaitManager.getDefaultPollingTime())
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Select dropDown= new Select(findWebElement( locator));
        List<WebElement>elements =dropDown.getAllSelectedOptions();
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }

    /**
     * Sets the implicit wait timeout for the WebDriver.
     * @param timeout Maximum wait time in seconds
     */
    public  void setImplicitWait( int timeout) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
    }

    /**
     * Clicks an element using JavaScript.
     * @param locator Element locator
     */
    public  void javascriptClick( By locator) {
        WebElement element = findWebElement( locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        Reporter.log("JavaScript Click On Element: ",LogLevel.INFO_BLUE,locator.toString());
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
     * Retrieves the value of an attribute from an element.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The attribute value
     */
    public  String getAttributeValue( By locator, String attribute, int timeout, int pollingEvery) {
        Reporter.log("Getting Attribute: '" + attribute + "' from Element: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        return findWebElement( locator).getDomAttribute(attribute);
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
     * Switches to a new window with the specified title.
     * @param windowTitle The title of the window to switch to
     */
    public  void switchToNewWindow( String windowTitle) {
        String originalWindow = driver.getWindowHandle();
        for (String windowHandle : driver.getWindowHandles()) {
            driver.switchTo().window(windowHandle);
            if (driver.getTitle().equals(windowTitle)) {
                return;
            }
        }
        driver.switchTo().window(originalWindow);
    }

    /**
     * Closes the current window or tab.
     */
    public  void closeCurrentWindow() {
        driver.close();
    }

    /**
     * Switches to the original window.
     * @param originalWindowHandle The handle of the original window
     */
    public  void switchToOriginalWindow( String originalWindowHandle) {
        driver.switchTo().window(originalWindowHandle);
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
    public  List<WebElement> findWebElements( By locator) {
        return driver.findElements(locator);
    }

    /**
     * Accepts an alert.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void acceptAlert( int timeout, int pollingEvery) {
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
    }

    /**
     * Dismisses an alert.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void dismissAlert( int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().dismiss();
    }

    /**
     * Gets the text of an alert.
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The alert text
     */
    public  String getAlertText( int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        return driver.switchTo().alert().getText();
    }

    /**
     * Sends data to an alert.
     * @param data The data to send
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void sendDataToAlert( String data, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().sendKeys(data);
    }

    /**
     * Gets the text from multiple elements.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of text from the elements
     */
    public  List<String> getTextFromMultipleElements( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery).until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        Reporter.log("Getting text from multiple elements located: ",LogLevel.INFO_BLUE,locator.toString());
        List<WebElement> elements = findWebElements(locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }

    /**
     * Gets a FluentWait instance with specified timeout and polling interval.
     * @param timeoutInSeconds Maximum wait time in seconds
     * @param pollingEveryInMillis Polling interval in milliseconds
     * @return FluentWait instance
     */
    @SuppressWarnings("unchecked")
    public FluentWait<T> getFluentWait(int timeoutInSeconds, int pollingEveryInMillis) {
            return WaitManager.getFluentWait(driver,timeoutInSeconds,pollingEveryInMillis);
    }

    /**
     * Gets the value of an attribute from multiple elements.
     * @param locator Element locator
     * @param Attribute Attribute name
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of attribute values from the elements
     */
    public  List<String> getAttributeFromMultipleElements( By locator,String Attribute, int timeout, int pollingEvery) {
        Reporter.log("Getting Attribute from multiple elements located: ",LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getDomAttribute(Attribute));
        }
        return texts;
    }

    /**
     * Clicks on multiple elements.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void clickOnMultipleElements( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(locator);
        for (WebElement element : elements) {
            element.click();
        }
    }

    /**
     * Sends data to multiple elements.
     * @param locator Element locator
     * @param data Data to send
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void sendDataToMultipleElements( By locator, String data, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        Reporter.log("Sending data to multiple elements located: ",LogLevel.INFO_BLUE,locator.toString());
        List<WebElement> elements = findWebElements(locator);
        for (WebElement element : elements) {
            element.clear();
            element.sendKeys(data);
        }
    }

    /**
     * Selects a dropdown option by text for multiple elements.
     * @param locator Element locator
     * @param option Option to select
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void selectDropdownByTextForMultipleElements( By locator, String option, int timeout, int pollingEvery) {
        Reporter.log("Selecting dropdown option by text for multiple elements: " + option + " for locator: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        List<WebElement> elements = findWebElements( locator);
        for (WebElement element : elements) {
            new Select(element).selectByVisibleText(option);
        }
    }

    /**
     * Switches to a frame by index.
     * @param index Frame index
     * @param timeout Maximum wait time in seconds
     * @param pollingTime Polling interval in milliseconds
     */
    public  void switchToFrameByIndex( int index, int timeout,int pollingTime) {
        getFluentWait(timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }

    /**
     * Hovers over an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public  void hoverOverElement( By locator, int timeout) {
        Reporter.log("Hovered over element: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait( timeout, WaitManager.getDefaultPollingTime())
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.moveToElement(element).perform();
    }

    /**
     * Scrolls to the bottom of the page.
     */
    public  void scrollToPageBottom() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Reporter.log("Scrolled to page bottom", LogLevel.INFO_BLUE);
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
     * Switches to a frame by name or ID.
     * @param nameOrID Frame name or ID
     * @param timeout Maximum wait time in seconds
     * @param pollingTime Polling interval in milliseconds
     */
    public  void switchToFrameByNameOrID( String nameOrID, int timeout,int pollingTime) {
        getFluentWait(timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrID));
    }

    /**
     * Switches to a frame by WebElement.
     * @param locator Frame locator
     * @param timeout Maximum wait time in seconds
     * @param pollingTime Polling interval in milliseconds
     */
    public  void switchToFrameByElement( By locator, int timeout,int pollingTime) {
        getFluentWait(timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    /**
     * Switches back to the default content from a frame.
     */
    public  void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    /**
     * Switches to a popup window with the specified title.
     * @param expectedPopupTitle The title of the popup window to switch to
     * @param timeout Maximum wait time in seconds
     * @param pollingTime Polling interval in milliseconds
     */
    public  void switchToPopupWindow( String expectedPopupTitle, int timeout, int pollingTime) {
        String mainWindow = driver.getWindowHandle();
        Reporter.log("Waiting for popup window to appear.", LogLevel.INFO_BLUE);

        boolean windowsAppeared = getFluentWait( timeout, pollingTime)
                .until(ExpectedConditions.numberOfWindowsToBe(2));

        if (windowsAppeared) {
            for (String windowHandle : driver.getWindowHandles()) {
                if (!windowHandle.equals(mainWindow)) {
                    driver.switchTo().window(windowHandle);
                    if (driver.getTitle().equals(expectedPopupTitle)) {
                        Reporter.log("Switched to popup window with title: " + expectedPopupTitle, LogLevel.INFO_BLUE);
                        return;
                    }
                }
            }
            Reporter.log("Popup window with title " + expectedPopupTitle + " not found", LogLevel.ERROR);
            driver.switchTo().window(mainWindow);
        } else {
            Reporter.log("Popup window did not appear within the timeout.", LogLevel.ERROR);
        }
    }

    /**
     * Closes the popup window and switches back to the main window.
     */
    public  void closePopupWindow() {
        driver.close();
        Reporter.log("Popup window closed. Switching back to the main window.", LogLevel.INFO_BLUE);
        String mainWindow = driver.getWindowHandles().iterator().next();
        driver.switchTo().window(mainWindow);
    }

    /**
     * Moves a slider to a specific value.
     * @param sliderLocator Slider element locator
     * @param rangeLocator Range element locator
     * @param targetValue Target value to move the slider to
     * @return The final value of the slider
     */
    public  float moveSliderTo( By sliderLocator, By rangeLocator, float targetValue) {
        WebElement range = findWebElement( rangeLocator);
        WebElement slider = findWebElement( sliderLocator);
        float currentValue = Float.parseFloat(range.getText());
        Actions action = new Actions(driver);

        // Move slider to minimum (assumed to be 0)
        int timeout = 0;
        while ((currentValue != 0) && (timeout < 3000)) {
            action.sendKeys(slider, Keys.ARROW_LEFT).perform();
            currentValue = Float.parseFloat(range.getText());
            timeout++;
        }

        timeout = 0;
        // Now, move slider to the target value
        while ((currentValue != targetValue) && (timeout < 3000)) {
            action.sendKeys(slider, Keys.ARROW_RIGHT).perform();
            currentValue = Float.parseFloat(range.getText());
            timeout++;
        }

        Reporter.log("Slider moved to: " + currentValue, LogLevel.INFO_BLUE);
        return currentValue;
    }

    /**
     * Performs a drag and drop action from a source element to a target element.
     * @param sourceLocator Source element locator
     * @param targetLocator Target element locator
     */
    public  void dragAndDrop( By sourceLocator, By targetLocator) {
        WebElement source = findWebElement( sourceLocator);
        WebElement target = findWebElement( targetLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveToElement(target)
                .release()
                .perform();

        Reporter.log("Drag and drop performed from " + sourceLocator + " to " + targetLocator, LogLevel.INFO_BLUE);
    }

    /**
     * Performs a drag and drop action by offset.
     * @param sourceLocator Source element locator
     * @param xOffset X offset to move
     * @param yOffset Y offset to move
     */
    public  void dragAndDropByOffset( By sourceLocator, int xOffset, int yOffset) {
        WebElement source = findWebElement( sourceLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();

        Reporter.log("Drag and drop performed with offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }

    /**
     * Hovers over an element and clicks another element.
     * @param locatorToHover Element locator to hover
     * @param locatorToClick Element locator to click
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void hoverAndClick( By locatorToHover, By locatorToClick, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to hover: " + locatorToHover.toString(), LogLevel.INFO_BLUE);
        WebElement elementToHover = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locatorToHover));

        Reporter.log("Waiting for element to click: " + locatorToClick.toString(), LogLevel.INFO_BLUE);
        WebElement elementToClick = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locatorToClick));

        Actions action = new Actions(driver);
        action.moveToElement(elementToHover).click(elementToClick).perform();

        Reporter.log("Hovered over " + locatorToHover + " and clicked " + locatorToClick, LogLevel.INFO_BLUE);
    }

    /**
     * Captures a screenshot and saves it with the specified name.
     * @param screenshotName The name of the screenshot file
     * @return The path of the saved screenshot file
     */
    public  String captureScreenshot( String screenshotName) {
        try {
            TakesScreenshot camera = (TakesScreenshot) driver;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            File screenShotUser = new File("Test-Output" + File.separator + "ScreenShots" + File.separator + "Captured" + File.separator);

            if (!screenShotUser.exists()) {
                screenShotUser.mkdirs();
            }

            File screenShotFile = new File(screenShotUser.getPath() + screenshotName + "_" + TestDataGenerator.getTimeStamp() + ".png");
            Files.move(screenshot, screenShotFile);

            Reporter.log("Screenshot captured: " + screenShotFile.getPath(), LogLevel.INFO_BLUE);
            return screenShotFile.getPath();
        } catch (Exception e) {
            Reporter.log("Failed to capture screenshot: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Moves a slider by offset.
     * @param sliderLocator Slider element locator
     * @param xOffset X offset to move
     * @param yOffset Y offset to move
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void moveSliderByOffset( By sliderLocator, int xOffset, int yOffset, int timeout, int pollingEvery) {
        Reporter.log("Waiting for slider to be visible: " + sliderLocator.toString(), LogLevel.INFO_BLUE);
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(sliderLocator));

        WebElement slider = findWebElement( sliderLocator);
        Actions action = new Actions(driver);
        action.clickAndHold(slider)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();
        Reporter.log("Slider moved by offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);

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
     * Right-clicks on an element.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void rightClick( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to right-click: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));

        Actions action = new Actions(driver);
        action.contextClick(element).perform();

        Reporter.log("Right-clicked on element: " + locator, LogLevel.INFO_BLUE);
    }

    /**
     * Double-clicks on an element.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void doubleClick( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to double-click: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.doubleClick(element).perform();
        Reporter.log("Double-clicked on element: " + locator, LogLevel.INFO_BLUE);
    }

    /**
     * Maximizes the browser window.
     */
    public  void maximizeWindow() {
        driver.manage().window().maximize();
    }

    /**
     * Minimizes the browser window.
     */
    public  void minimizeWindow() {
        driver.manage().window().minimize();
    }

    /**
     * Gets all elements matching the locator.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of found WebElements
     */
    public  List<WebElement> getElements( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        return findWebElements(locator);
    }

    /**
     * Navigates to the specified URL.
     * @param url The URL to navigate to
     */
    public  void navigateToUrl( String url) {
        driver.get(url);
    }

    /**
     * Refreshes the current page.
     */
    public  void refreshPage() {
        driver.navigate().refresh();
    }

    /**
     * Navigates back to the previous page.
     */
    public  void navigateBack() {
        driver.navigate().back();
    }

    /**
     * Navigates forward to the next page.
     */
    public  void navigateForward() {
        driver.navigate().forward();
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
     * Gets the text from multiple elements with default timeout and polling time.
     * @param locator Element locator
     * @return List of text from the elements
     */
    public  List<String> getTextFromMultipleElements( By locator) {
        return getTextFromMultipleElements( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the text from multiple elements with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return List of text from the elements
     */
    public  List<String> getTextFromMultipleElements( By locator, int timeout) {
        return getTextFromMultipleElements( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the value of an attribute from multiple elements with default timeout and polling time.
     * @param locator Element locator
     * @param attribute Attribute name
     * @return List of attribute values from the elements
     */
    public  List<String> getAttributeFromMultipleElements( By locator, String attribute) {
        return getAttributeFromMultipleElements( locator, attribute, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the value of an attribute from multiple elements with specified timeout.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param timeout Maximum wait time in seconds
     * @return List of attribute values from the elements
     */
    public  List<String> getAttributeFromMultipleElements( By locator, String attribute, int timeout) {
        return getAttributeFromMultipleElements( locator, attribute, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Clicks on multiple elements with default timeout and polling time.
     * @param locator Element locator
     */
    public  void clickOnMultipleElements( By locator) {
        clickOnMultipleElements( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Clicks on multiple elements with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public  void clickOnMultipleElements( By locator, int timeout) {
        clickOnMultipleElements( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends data to multiple elements with default timeout and polling time.
     * @param locator Element locator
     * @param data Data to send
     */
    public  void sendDataToMultipleElements( By locator, String data) {
        sendDataToMultipleElements( locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends data to multiple elements with specified timeout.
     * @param locator Element locator
     * @param data Data to send
     * @param timeout Maximum wait time in seconds
     */
    public  void sendDataToMultipleElements( By locator, String data, int timeout) {
        sendDataToMultipleElements( locator, data, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by text for multiple elements with default timeout and polling time.
     * @param locator Element locator
     * @param option Option to select
     */
    public  void selectDropdownByTextForMultipleElements( By locator, String option) {
        selectDropdownByTextForMultipleElements( locator, option, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by text for multiple elements with specified timeout.
     * @param locator Element locator
     * @param option Option to select
     * @param timeout Maximum wait time in seconds
     */
    public  void selectDropdownByTextForMultipleElements( By locator, String option, int timeout) {
        selectDropdownByTextForMultipleElements( locator, option, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by index with default timeout and polling time.
     * @param index Frame index
     */
    public  void switchToFrameByIndex( int index) {
        switchToFrameByIndex( index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by index with specified timeout.
     * @param index Frame index
     * @param timeout Maximum wait time in seconds
     */
    public  void switchToFrameByIndex( int index, int timeout) {
        switchToFrameByIndex( index, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by name or ID with default timeout and polling time.
     * @param nameOrID Frame name or ID
     */
    public  void switchToFrameByNameOrID( String nameOrID) {
        switchToFrameByNameOrID( nameOrID, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by name or ID with specified timeout.
     * @param nameOrID Frame name or ID
     * @param timeout Maximum wait time in seconds
     */
    public  void switchToFrameByNameOrID( String nameOrID, int timeout) {
        switchToFrameByNameOrID( nameOrID, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by WebElement with default timeout and polling time.
     * @param locator Frame locator
     */
    public  void switchToFrameByElement( By locator) {
        switchToFrameByElement( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a frame by WebElement with specified timeout.
     * @param locator Frame locator
     * @param timeout Maximum wait time in seconds
     */
    public  void switchToFrameByElement( By locator, int timeout) {
        switchToFrameByElement( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a popup window with the specified title and default timeout and polling time.
     * @param expectedPopupTitle The title of the popup window to switch to
     */
    public  void switchToPopupWindow( String expectedPopupTitle) {
        switchToPopupWindow( expectedPopupTitle, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Switches to a popup window with the specified title and specified timeout.
     * @param expectedPopupTitle The title of the popup window to switch to
     * @param timeout Maximum wait time in seconds
     */
    public  void switchToPopupWindow( String expectedPopupTitle, int timeout) {
        switchToPopupWindow( expectedPopupTitle, timeout, WaitManager.getDefaultPollingTime());
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
     * Clicks an element using JavaScript with specified timeout and polling interval.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public  void javascriptClick( By locator, int timeout, int pollingEvery) {
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick( locator);
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
     * Right-clicks on an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public  void rightClick( By locator, int timeout){
        rightClick(locator,timeout,WaitManager.getDefaultPollingTime());
    }

    /**
     * Right-clicks on an element with default timeout and polling time.
     * @param locator Element locator
     */
    public  void rightClick( By locator){
        rightClick(locator,WaitManager.getDefaultTimeout(),WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends text data to an element with default timeout and polling time.
     * @param locator Element locator
     * @param data Text to send
     */
    public  void sendData( By locator, String data) {
        sendData( locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends keyboard keys to an element with default timeout and polling time.
     * @param locator Element locator
     * @param data Keys to send
     */
    public  void sendData( By locator, Keys data) {
        sendData( locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Retrieves text from an element with default timeout and polling time.
     * @param locator Element locator
     * @return Text content of the element
     */
    public  String getText( By locator) {
        return getText( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Clicks an element with default timeout and polling time.
     * @param locator Element locator
     */
    public  void clickOnElement( By locator) {
        clickOnElement( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends text data to an element with specified timeout.
     * @param locator Element locator
     * @param data Text to send
     * @param timeout Maximum wait time in seconds
     */
    public  void sendData( By locator, String data, int timeout) {
        sendData( locator, data, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends keyboard keys to an element with specified timeout.
     * @param locator Element locator
     * @param data Keys to send
     * @param timeout Maximum wait time in seconds
     */
    public  void sendData( By locator, Keys data, int timeout) {
        sendData( locator, data,timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Retrieves text from an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return Text content of the element
     */
    public  String getText( By locator, int timeout) {
        return getText( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Accepts an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     */
    public  void acceptAlert( int timeout) {
        acceptAlert(timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Accepts an alert with default timeout and polling time.
     */
    public  void acceptAlert() {
        acceptAlert(WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the text of an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @return The alert text
     */
    public  String getAlertText( int timeout){
        return getAlertText( timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the text of an alert with default timeout and polling time.
     * @return The alert text
     */
    public  String getAlertText(){
        return getAlertText(WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Dismisses an alert with specified timeout.
     * @param timeout Maximum wait time in seconds
     */
    public  void dismissAlert( int timeout){
        dismissAlert( timeout,WaitManager.getDefaultPollingTime());
    }

    /**
     * Dismisses an alert with default timeout and polling time.
     */
    public  void dismissAlert(){
        dismissAlert(WaitManager.getDefaultTimeout(),WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends data to an alert with specified timeout.
     * @param data The data to send
     * @param timeout Maximum wait time in seconds
     */
    public  void sendDataToAlert( String data, int timeout){
        sendDataToAlert(data,timeout,WaitManager.getDefaultPollingTime());
    }

    /**
     * Sends data to an alert with default timeout and polling time.
     * @param data The data to send
     */
    public  void sendDataToAlert( String data){
        sendDataToAlert(data,WaitManager.getDefaultTimeout(),WaitManager.getDefaultPollingTime());
    }

    /**
     * Clicks an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public  void clickOnElement( By locator,int timeout) {
        clickOnElement( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by visible text with default timeout and polling time.
     * @param locator Dropdown element locator
     * @param option Text of the option to select
     */
    public  void selectDropdownByText( By locator, String option) {
        selectDropdownByText( locator, option, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by value with default timeout and polling time.
     * @param locator Dropdown element locator
     * @param value Value of the option to select
     */
    public  void selectDropdownByValue( By locator, String value) {
        selectDropdownByValue( locator, value, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by index with default timeout and polling time.
     * @param locator Dropdown element locator
     * @param index Index of the option to select
     */
    public  void selectDropdownByIndex( By locator, int index) {
        selectDropdownByIndex( locator, index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
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
     * Retrieves the value of an attribute from an element with default timeout and polling time.
     * @param locator Element locator
     * @param attribute Attribute name
     * @return The attribute value
     */
    public  String getAttributeValue( By locator, String attribute) {
        return getAttributeValue( locator, attribute, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Double-clicks on an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public  void doubleClick( By locator, int timeout) {
        doubleClick( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Double-clicks on an element with default timeout and polling time.
     * @param locator Element locator
     */
    public  void doubleClick( By locator) {
        doubleClick( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets all elements matching the locator with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return List of found WebElements
     */
    public  List<WebElement> getElements( By locator, int timeout) {
        return getElements( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets all elements matching the locator with default timeout and polling time.
     * @param locator Element locator
     * @return List of found WebElements
     */
    public  List<WebElement> getElements( By locator) {
        return getElements( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Moves a slider by offset with default timeout and polling time.
     * @param sliderLocator Slider element locator
     * @param xOffset X offset to move
     * @param yOffset Y offset to move
     */
    public  void moveSliderByOffset( By sliderLocator, int xOffset, int yOffset) {
        moveSliderByOffset( sliderLocator, xOffset, yOffset, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Hovers over an element with default timeout.
     * @param locator Element locator
     */
    public  void hoverOverElement( By locator) {
        hoverOverElement( locator, WaitManager.getDefaultTimeout());
    }

    /**
     * Hovers over an element and clicks another element with specified timeout.
     * @param locatorToHover Element locator to hover
     * @param locatorToClick Element locator to click
     * @param timeout Maximum wait time in seconds
     */
    public  void hoverAndClick( By locatorToHover, By locatorToClick,int timeout) {
        hoverAndClick( locatorToHover, locatorToClick, timeout,WaitManager.getDefaultPollingTime());
    }

    /**
     * Hovers over an element and clicks another element with default timeout and polling time.
     * @param locatorToHover Element locator to hover
     * @param locatorToClick Element locator to click
     */
    public  void hoverAndClick( By locatorToHover, By locatorToClick) {
        hoverAndClick( locatorToHover, locatorToClick, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Sleeps for a specified number of milliseconds.
     * @param millis Number of milliseconds to sleep
     */
    public  void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
            Reporter.log("Sleeping for " + millis + " milliseconds", LogLevel.INFO_BLUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.logException( e);
            Reporter.log("Sleep interrupted: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Sleeps for a specified number of seconds.
     * @param seconds Number of seconds to sleep
     */
    public  void sleepSeconds(long seconds) {
        Allure.step("Sleeping for " + seconds + " seconds", Status.PASSED);
        sleepMillis(seconds * 1000);
    }

    /**
     * Sleeps for a specified number of minutes.
     * @param minutes Number of minutes to sleep
     */
    public  void sleepMinutes(long minutes) {
        Allure.step("Sleeping for " + minutes + " minutes", Status.PASSED);
        sleepMillis(minutes * 60 * 1000);
    }
}