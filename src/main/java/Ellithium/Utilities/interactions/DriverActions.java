package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
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
public class DriverActions<T extends WebDriver> extends BaseActions<T> {
    /**
     * Creates a new DriverActions instance.
     * @param driver WebDriver instance to wrap
     */
    @SuppressWarnings("unchecked")
    public DriverActions(T driver) {
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
     * @return The saved screenshot file
     */
    public  File captureScreenshot( String screenshotName) {
        try {
            TakesScreenshot camera = (TakesScreenshot) driver;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            File screenShotFolder = new File("Test-Output" + File.separator + "ScreenShots" + File.separator + "Captured" + File.separator);
            if (!screenShotFolder.exists()) {
                screenShotFolder.mkdirs();
            }
            String name=screenshotName + "-" + TestDataGenerator.getTimeStamp();
            File screenShotFile = new File(screenShotFolder.getPath() + File.separator + name + ".png");
            Files.move(screenshot, screenShotFile);
            Reporter.log("Screenshot captured: " + screenShotFile.getPath(), LogLevel.INFO_BLUE);
            Reporter.attachScreenshotToReport(screenShotFile,name,"Captured Screenshot");
            return screenShotFile;
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
    public JavaScriptActions JSActions(){
        return new JavaScriptActions<>(driver);
    }
    public Sleep sleep(){
        return new Sleep();
    }
    public AlertActions alerts() {
        return new AlertActions<>(driver);
    }

    public FrameActions frames() {
        return new FrameActions<>(driver);
    }
    
    public WindowActions windows() {
        return new WindowActions<>(driver);
    }
    
    public ElementActions elements() {
        return new ElementActions<>(driver);
    }
    
    public SelectActions select() {
        return new SelectActions<>(driver);
    }

    public NavigationActions navigation() {
        return new NavigationActions<>(driver);
    }

    public WaitActions waits() {
        return new WaitActions<>(driver);
    }

    public MouseActions mouse() {
        return new MouseActions<>(driver);
    }
}