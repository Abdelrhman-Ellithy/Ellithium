package Ellithium.Utilities.interactions;
import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.logsUtils;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import io.appium.java_client.AppiumFluentWait;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
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
import java.util.*;
import java.util.concurrent.ExecutionException;
public class IOSDriverActions{
    private  int defaultTimeout= 5;
    private  int defaultPollingTime=500;
    private  boolean defaultTimeoutGotFlag=false;
    private  boolean defaultPollingTimeGotFlag=false;
    private final IOSDriver driver;
    public IOSDriverActions(IOSDriver driver) {
        this.driver = driver;
    }
    public void sendData(By locator, String data, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(locator).clear();
        findWebElement(locator).sendKeys(data);
    }
    public void sendData(By locator, Keys data, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(locator).clear();
        findWebElement(locator).sendKeys(data);
    }
    public  String getText( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        String text = findWebElement(locator).getText();
        return text;
    }
    public  void clickOnElement( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        findWebElement(locator).click();
    }
    public WebDriverWait generalWait(int timeout) {
        Reporter.log("Getting general Wait For "+ timeout + " seconds", LogLevel.INFO_BLUE);
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }
    public  void scrollToElement( By locator) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", findWebElement( locator));
        Reporter.log("Scrolling To Element: ",LogLevel.INFO_BLUE,locator.toString());
    }
    public  void selectDropdownByText( By locator, String option, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement( locator)).selectByVisibleText(option);
        Reporter.log("Selecting Dropdown Option By Text: " + option + " From Element: ",LogLevel.INFO_BLUE,locator.toString());
    }
    public void selectDropdownByValue( By locator, String value, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement( locator)).selectByValue(value);
        Reporter.log("Selecting Dropdown Option By Value: " + value + " From Element: ",LogLevel.INFO_BLUE,locator.toString());
    }

    public  void selectDropdownByIndex( By locator, int index, int timeout, int pollingEvery) {
        Reporter.log("Selecting Dropdown Option By Index: " + index + " From Element: " ,LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement( locator)).selectByIndex(index);
    }
    public  void setImplicitWait( int timeout) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
    }

    public  void javascriptClick( By locator) {
        WebElement element = findWebElement( locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        Reporter.log("JavaScript Click On Element: ",LogLevel.INFO_BLUE,locator.toString());
    }

    public  void waitForElementToDisappear( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        Reporter.log("Waiting for Element To Disappear: ",LogLevel.INFO_BLUE,locator.toString());
    }
    public  WebElement waitForElementToBeClickable( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        Reporter.log("Wait For Element To Be Clickable: ",LogLevel.INFO_BLUE,locator.toString());
        return findWebElement(locator);
    }
    public  WebElement waitForElementToBeVisible( By locator, int timeout, int pollingEvery) {
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Reporter.log("Wait For Element To Be Visible: ",LogLevel.INFO_BLUE,locator.toString());
        return findWebElement( locator);
    }

    public  WebElement waitForElementPresence( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Presence: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        return findWebElement( locator);
    }

    public  WebElement waitForTextToBePresentInElement( By locator, String text, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Text: '" + text + "' to be present in Element: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
        return findWebElement( locator);
    }

    public  String getAttributeValue( By locator, String attribute, int timeout, int pollingEvery) {
        Reporter.log("Getting Attribute: '" + attribute + "' from Element: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        String attributeValue = findWebElement( locator).getAttribute(attribute);
        return attributeValue;
    }

    public  boolean waitForElementToBeSelected( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element to be Selected: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean isSelected = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeSelected(locator));
        return isSelected;
    }

    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Attribute: '" + attribute + "' to be: '" + value + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.attributeToBe(locator, attribute, value));
        return result;
    }

    public  boolean waitForElementAttributeContains( By locator, String attribute, String value, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Attribute: '" + attribute + "' to contain: '" + value + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.attributeContains(locator, attribute, value));
        return result;
    }
    public  boolean waitForElementStaleness( WebElement element, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Staleness: " + element.toString(), LogLevel.INFO_BLUE);
        boolean isStale = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.stalenessOf(element));
        return isStale;
    }
    public  boolean waitForTitleContains( String titlePart, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Title to Contain: '" + titlePart + "'", LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.titleContains(titlePart));
        return result;
    }
    public  boolean waitForUrlContains( String urlPart, int timeout, int pollingEvery) {
        Reporter.log("Waiting for URL to Contain: '" + urlPart + "'", LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.urlContains(urlPart));
        return result;
    }
    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available and Switching to it: " + locator.toString(), LogLevel.INFO_BLUE);
        WebDriver frame = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
        return frame;
    }
    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available by Name or ID: '" + nameOrId + "'", LogLevel.INFO_BLUE);
        WebDriver frame = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
        return frame;
    }
    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available by Index: " + index, LogLevel.INFO_BLUE);
        WebDriver frame = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
        return frame;
    }
    public  boolean waitForElementToBeEnabled( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element to be Enabled: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean isEnabled = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator)).isEnabled();
        return isEnabled;
    }
    public  boolean waitForTitleIs( String title, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Title to be: '" + title + "'", LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.titleIs(title));
        return result;
    }
    public  boolean waitForUrlToBe( String url, int timeout, int pollingEvery) {
        Reporter.log("Waiting for URL to be: '" + url + "'", LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.urlToBe(url));
        return result;
    }
    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Selection State to be: " + selected + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementSelectionStateToBe(locator, selected));
        return result;
    }
    public  boolean waitForTextToBePresentInElementValue( By locator, String text, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Text to be Present in Element Value: '" + text + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementValue(locator, text));
        return result;
    }


    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Windows to be: " + numberOfWindows, LogLevel.INFO_BLUE);
        boolean result = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfWindowsToBe(numberOfWindows));
        return result;
    }

    public  boolean waitForNumberOfElementsToBeMoreThan( By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be More Than: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(locator, number)).size();
        return size > number;
    }

    public  boolean waitForNumberOfElementsToBeLessThan( By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be Less Than: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBeLessThan(locator, number)).size();
        return size < number;
    }

    public List<WebElement> waitForVisibilityOfAllElements(By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Visibility of All Elements for: " + locator.toString(), LogLevel.INFO_BLUE);
        List<WebElement> elements = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        return elements;
    }

    public  boolean waitForNumberOfElementsToBe( By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBe(locator, number)).size();
        return size == number;
    }
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
    // Close the current window or tab
    public  void closeCurrentWindow() {
        driver.close();
    }

    // Switch to the original window
    public  void switchToOriginalWindow( String originalWindowHandle) {
        driver.switchTo().window(originalWindowHandle);
    }
    // Find an element (no need for timeout or polling)
    public  WebElement findWebElement( By locator) {
        return driver.findElement(locator);
    }
    // Find elements (no need for timeout or polling)
    public  List<WebElement> findWebElements( By locator) {
        return driver.findElements(locator);
    }
    // Accept an alert
    public  void acceptAlert( int timeout, int pollingEvery) {
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
    }
    public  void dismissAlert( int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().dismiss();
    }
    public  String getAlertText( int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        String alertText = driver.switchTo().alert().getText();
        return alertText;
    }
    public  void sendDataToAlert( String data, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().sendKeys(data);
    }
    // Get text from multiple elements
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
    public FluentWait<IOSDriver> getFluentWait(int timeoutInSeconds, int pollingEveryInMillis) {
        return new AppiumFluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutInSeconds))
                .pollingEvery(Duration.ofMillis(pollingEveryInMillis))
                .ignoreAll(expectedExceptions);
    }
    public  List<String> getAttributeFromMultipleElements( By locator,String Attribute, int timeout, int pollingEvery) {
        Reporter.log("Getting Attribute from multiple elements located: ",LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getAttribute(Attribute));
        }
        return texts;
    }
    // Click on multiple elements
    public  void clickOnMultipleElements( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(locator);
        for (WebElement element : elements) {
            element.click();
        }
    }
    // Send data to multiple elements
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
    // Select from dropdowns on multiple elements by visible text
    public  void selectDropdownByTextForMultipleElements( By locator, String option, int timeout, int pollingEvery) {
        Reporter.log("Selecting dropdown option by text for multiple elements: " + option + " for locator: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        List<WebElement> elements = findWebElements( locator);
        for (WebElement element : elements) {
            new Select(element).selectByVisibleText(option);
        }
    }

    // Switch to frame by index
    public  void switchToFrameByIndex( int index, int timeout,int pollingTime) {
        getFluentWait(timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }
    // Hover over an element with specified timeout
    public  void hoverOverElement( By locator, int timeout) {
        Reporter.log("Hovered over element: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait( timeout, defaultPollingTime)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.moveToElement(element).perform();
    }


    // New method: Scroll to page bottom
    public  void scrollToPageBottom() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Reporter.log("Scrolled to page bottom", LogLevel.INFO_BLUE);
    }

    // New method: JS Executor - Set value using JavaScript
    public  void setElementValueUsingJS( By locator, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement element = findWebElement( locator);
        js.executeScript("arguments[0].value = arguments[1];", element, value);
        Reporter.log("Set value using JavaScript: " + value + " on element: " + locator.toString(), LogLevel.INFO_BLUE);
    }

    // Switch to frame by name or ID
    public  void switchToFrameByNameOrID( String nameOrID, int timeout,int pollingTime) {
        getFluentWait(timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrID));
    }
    // Switch to frame by WebElement
    public  void switchToFrameByElement( By locator, int timeout,int pollingTime) {
        getFluentWait(timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    // Switch back to default content from frame
    public  void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }
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
    public  void closePopupWindow() {
        driver.close();
        Reporter.log("Popup window closed. Switching back to the main window.", LogLevel.INFO_BLUE);
        String mainWindow = driver.getWindowHandles().iterator().next();
        driver.switchTo().window(mainWindow);
    }
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
    public  void dragAndDropByOffset( By sourceLocator, int xOffset, int yOffset) {
        WebElement source = findWebElement( sourceLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();

        Reporter.log("Drag and drop performed with offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }
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
    public  void scrollByOffset( int xOffset, int yOffset) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(arguments[0], arguments[1]);", xOffset, yOffset);

        Reporter.log("Scrolled by offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }
    public  void rightClick( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to right-click: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));

        Actions action = new Actions(driver);
        action.contextClick(element).perform();

        Reporter.log("Right-clicked on element: " + locator, LogLevel.INFO_BLUE);
    }
    public  void doubleClick( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to double-click: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.doubleClick(element).perform();
        Reporter.log("Double-clicked on element: " + locator, LogLevel.INFO_BLUE);
    }


    // Maximize the browser window
    public  void maximizeWindow() {
        driver.manage().window().maximize();
    }

    // Minimize the browser window
    public  void minimizeWindow() {
        driver.manage().window().minimize();
    }
    // Get all elements matching the locator
    public  List<WebElement> getElements( By locator, int timeout, int pollingEvery) {
        getFluentWait(timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        return findWebElements(locator);
    }

    public  void navigateToUrl( String url) {
        driver.get(url);
    }
    // Refresh the current page
    public  void refreshPage() {
        driver.navigate().refresh();
    }
    public  void navigateBack() {
        driver.navigate().back();
    }
    public  void navigateForward() {
        driver.navigate().forward();
    }
    public  boolean waitForElementToBeSelected( By locator) {
        initializeTimeoutAndPolling();
        return waitForElementToBeSelected( locator, defaultTimeout, defaultPollingTime);
    }
    public  boolean waitForElementToBeSelected( By locator, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementToBeSelected( locator, timeout, defaultPollingTime);
    }
    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value) {
        initializeTimeoutAndPolling();
        return waitForElementAttributeToBe( locator, attribute, value, defaultTimeout, defaultPollingTime);
    }
    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementAttributeToBe( locator, attribute, value, timeout, defaultPollingTime);
    }
    public  boolean waitForElementAttributeContains( By locator, String attribute, String value) {
        initializeTimeoutAndPolling();
        return waitForElementAttributeContains( locator, attribute, value, defaultTimeout, defaultPollingTime);
    }
    public  boolean waitForElementAttributeContains( By locator, String attribute, String value, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementAttributeContains( locator, attribute, value, timeout, defaultPollingTime);
    }
    public  boolean waitForElementStaleness( WebElement element) {
        initializeTimeoutAndPolling();
        return waitForElementStaleness( element, defaultTimeout, defaultPollingTime);
    }
    public  boolean waitForElementStaleness( WebElement element, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementStaleness( element, timeout, defaultPollingTime);
    }
    public  boolean waitForTitleContains( String titlePart) {
        initializeTimeoutAndPolling();
        return waitForTitleContains( titlePart, defaultTimeout, defaultPollingTime);
    }
    public  boolean waitForTitleContains( String titlePart, int timeout) {
        initializeTimeoutAndPolling();
        return waitForTitleContains( titlePart, timeout, defaultPollingTime);
    }
    public  boolean waitForUrlContains( String urlPart) {
        initializeTimeoutAndPolling();
        return waitForUrlContains( urlPart, defaultTimeout, defaultPollingTime);
    }
    public  List<String> getTextFromMultipleElements( By locator) {
        initializeTimeoutAndPolling();
        return getTextFromMultipleElements( locator, defaultTimeout, defaultPollingTime);
    }
    public  List<String> getTextFromMultipleElements( By locator, int timeout) {
        initializeTimeoutAndPolling();
        return getTextFromMultipleElements( locator, timeout, defaultPollingTime);
    }
    public  List<String> getAttributeFromMultipleElements( By locator, String attribute) {
        initializeTimeoutAndPolling();
        return getAttributeFromMultipleElements( locator, attribute, defaultTimeout, defaultPollingTime);
    }
    public  List<String> getAttributeFromMultipleElements( By locator, String attribute, int timeout) {
        initializeTimeoutAndPolling();
        return getAttributeFromMultipleElements( locator, attribute, timeout, defaultPollingTime);
    }
    public  void clickOnMultipleElements( By locator) {
        initializeTimeoutAndPolling();
        clickOnMultipleElements( locator, defaultTimeout, defaultPollingTime);
    }
    public  void clickOnMultipleElements( By locator, int timeout) {
        initializeTimeoutAndPolling();
        clickOnMultipleElements( locator, timeout, defaultPollingTime);
    }
    public  void sendDataToMultipleElements( By locator, String data) {
        initializeTimeoutAndPolling();
        sendDataToMultipleElements( locator, data, defaultTimeout, defaultPollingTime);
    }
    public  void sendDataToMultipleElements( By locator, String data, int timeout) {
        initializeTimeoutAndPolling();
        sendDataToMultipleElements( locator, data, timeout, defaultPollingTime);
    }
    public  void selectDropdownByTextForMultipleElements( By locator, String option) {
        initializeTimeoutAndPolling();
        selectDropdownByTextForMultipleElements( locator, option, defaultTimeout, defaultPollingTime);
    }
    public  void selectDropdownByTextForMultipleElements( By locator, String option, int timeout) {
        initializeTimeoutAndPolling();
        selectDropdownByTextForMultipleElements( locator, option, timeout, defaultPollingTime);
    }
    public  void switchToFrameByIndex( int index) {
        initializeTimeoutAndPolling();
        switchToFrameByIndex( index, defaultTimeout, defaultPollingTime);
    }
    public  void switchToFrameByIndex( int index, int timeout) {
        initializeTimeoutAndPolling();
        switchToFrameByIndex( index, timeout, defaultPollingTime);
    }
    public  void switchToFrameByNameOrID( String nameOrID) {
        initializeTimeoutAndPolling();
        switchToFrameByNameOrID( nameOrID, defaultTimeout, defaultPollingTime);
    }

    public  void switchToFrameByNameOrID( String nameOrID, int timeout) {
        initializeTimeoutAndPolling();
        switchToFrameByNameOrID( nameOrID, timeout, defaultPollingTime);
    }
    public  void switchToFrameByElement( By locator) {
        initializeTimeoutAndPolling();
        switchToFrameByElement( locator, defaultTimeout, defaultPollingTime);
    }

    public  void switchToFrameByElement( By locator, int timeout) {
        initializeTimeoutAndPolling();
        switchToFrameByElement( locator, timeout, defaultPollingTime);
    }
    public  void switchToPopupWindow( String expectedPopupTitle) {
        initializeTimeoutAndPolling();
        switchToPopupWindow( expectedPopupTitle, defaultTimeout, defaultPollingTime);
    }

    public  void switchToPopupWindow( String expectedPopupTitle, int timeout) {
        initializeTimeoutAndPolling();
        switchToPopupWindow( expectedPopupTitle, timeout, defaultPollingTime);
    }

    public  boolean waitForUrlContains( String urlPart, int timeout) {
        initializeTimeoutAndPolling();
        return waitForUrlContains( urlPart, timeout, defaultPollingTime);
    }
    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator) {
        initializeTimeoutAndPolling();
        return waitForFrameToBeAvailableAndSwitchToIt( locator, defaultTimeout, defaultPollingTime);
    }

    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator, int timeout) {
        initializeTimeoutAndPolling();
        return waitForFrameToBeAvailableAndSwitchToIt( locator, timeout, defaultPollingTime);
    }
    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId) {
        initializeTimeoutAndPolling();
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(nameOrId, defaultTimeout, defaultPollingTime);
    }

    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId, int timeout) {
        initializeTimeoutAndPolling();
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( nameOrId, timeout, defaultPollingTime);
    }
    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index) {
        initializeTimeoutAndPolling();
        return waitForFrameByIndexToBeAvailableAndSwitchToIt( index, defaultTimeout, defaultPollingTime);
    }

    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index, int timeout) {
        initializeTimeoutAndPolling();
        return waitForFrameByIndexToBeAvailableAndSwitchToIt( index, timeout, defaultPollingTime);
    }
    public  boolean waitForElementToBeEnabled( By locator) {
        initializeTimeoutAndPolling();
        return waitForElementToBeEnabled(locator, defaultTimeout, defaultPollingTime);
    }

    public  boolean waitForElementToBeEnabled( By locator, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementToBeEnabled( locator, timeout, defaultPollingTime);
    }
    public  boolean waitForTitleIs( String title) {
        initializeTimeoutAndPolling();
        return waitForTitleIs(title, defaultTimeout, defaultPollingTime);
    }

    public  boolean waitForTitleIs( String title, int timeout) {
        initializeTimeoutAndPolling();
        return waitForTitleIs( title, timeout, defaultPollingTime);
    }
    public  boolean waitForUrlToBe( String url) {
        initializeTimeoutAndPolling();
        return waitForUrlToBe( url, defaultTimeout, defaultPollingTime);
    }

    public  boolean waitForUrlToBe( String url, int timeout) {
        initializeTimeoutAndPolling();
        return waitForUrlToBe( url, timeout, defaultPollingTime);
    }
    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected) {
        initializeTimeoutAndPolling();
        return waitForElementSelectionStateToBe( locator, selected, defaultTimeout, defaultPollingTime);
    }

    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementSelectionStateToBe( locator, selected, timeout, defaultPollingTime);
    }
    public  boolean waitForTextToBePresentInElementValue( By locator, String text) {
        initializeTimeoutAndPolling();
        return waitForTextToBePresentInElementValue( locator, text, defaultTimeout, defaultPollingTime);
    }

    public  boolean waitForTextToBePresentInElementValue( By locator, String text, int timeout) {
        initializeTimeoutAndPolling();
        return waitForTextToBePresentInElementValue( locator, text, timeout, defaultPollingTime);
    }
    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows) {
        initializeTimeoutAndPolling();
        return waitForNumberOfWindowsToBe( numberOfWindows, defaultTimeout, defaultPollingTime);
    }

    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows, int timeout) {
        initializeTimeoutAndPolling();
        return waitForNumberOfWindowsToBe( numberOfWindows, timeout, defaultPollingTime);
    }
    public  boolean waitForNumberOfElementsToBeMoreThan( By locator, int number) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBeMoreThan( locator, number, defaultTimeout, defaultPollingTime);
    }

    public  boolean waitForNumberOfElementsToBeMoreThan( By locator, int number, int timeout) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBeMoreThan( locator, number, timeout, defaultPollingTime);
    }
    public  boolean waitForNumberOfElementsToBeLessThan( By locator, int number) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBeLessThan( locator, number, defaultTimeout, defaultPollingTime);
    }
    public  void javascriptClick( By locator, int timeout) {
        getFluentWait( timeout, defaultPollingTime)
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick( locator);
    }
    public  void javascriptClick( By locator, int timeout, int pollingEvery) {
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick( locator);
    }
    public  WebElement waitForTextToBePresentInElement( By locator, String text) {
        return waitForTextToBePresentInElement( locator, text, defaultTimeout, defaultPollingTime);
    }

    public  WebElement waitForTextToBePresentInElement( By locator, String text, int timeout) {
        return waitForTextToBePresentInElement( locator, text, timeout, defaultPollingTime);
    }

    public  boolean waitForNumberOfElementsToBeLessThan( By locator, int number, int timeout) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBeLessThan( locator, number, timeout, defaultPollingTime);
    }
    public  List<WebElement> waitForVisibilityOfAllElements( By locator) {
        initializeTimeoutAndPolling();
        return waitForVisibilityOfAllElements( locator, defaultTimeout, defaultPollingTime);
    }

    public  List<WebElement> waitForVisibilityOfAllElements( By locator, int timeout) {
        initializeTimeoutAndPolling();
        return waitForVisibilityOfAllElements( locator, timeout, defaultPollingTime);
    }
    public  boolean waitForNumberOfElementsToBe( By locator, int number) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBe( locator, number, defaultTimeout, defaultPollingTime);
    }

    public  boolean waitForNumberOfElementsToBe( By locator, int number, int timeout) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBe( locator, number, timeout, defaultPollingTime);
    }
    public  void rightClick( By locator, int timeout){
        initializeTimeoutAndPolling();
        rightClick(locator,timeout,defaultPollingTime);
    }
    public  void rightClick( By locator){
        initializeTimeoutAndPolling();
        rightClick(locator,defaultTimeout,defaultPollingTime);
    }

    public  void sendData( By locator, String data) {
        initializeTimeoutAndPolling();
        sendData( locator, data, defaultTimeout, defaultPollingTime);
    }

    public  void sendData( By locator, Keys data) {
        initializeTimeoutAndPolling();
        sendData( locator, data, defaultTimeout, defaultPollingTime);
    }

    // Overloaded getText method with default timeout and polling time
    public  String getText( By locator) {
        initializeTimeoutAndPolling();
        return getText( locator, defaultTimeout, defaultPollingTime);
    }
    // Overloaded clickOnElement method with default timeout and polling time
    public  void clickOnElement( By locator) {
        initializeTimeoutAndPolling();
        clickOnElement( locator, defaultTimeout, defaultPollingTime);
    }
    public  void sendData( By locator, String data, int timout) {
        initializeTimeoutAndPolling();
        sendData( locator, data, timout, defaultPollingTime);
    }

    public  void sendData( By locator, Keys data, int timout) {
        initializeTimeoutAndPolling();
        sendData( locator, data, defaultTimeout, defaultPollingTime);
    }

    // Overloaded getText method with default timeout and polling time
    public  String getText( By locator, int timout) {
        initializeTimeoutAndPolling();
        return getText( locator, timout, defaultPollingTime);
    }
    public  void acceptAlert( int timeout) {
        initializeTimeoutAndPolling();
        acceptAlert(timeout, defaultPollingTime);
    }
    public  void acceptAlert() {
        initializeTimeoutAndPolling();
        acceptAlert(defaultTimeout, defaultPollingTime);
    }
    public  String getAlertText( int timeout){
        initializeTimeoutAndPolling();
        return getAlertText( timeout, defaultPollingTime);
    }
    public  String getAlertText(){
        initializeTimeoutAndPolling();
        return getAlertText(defaultTimeout, defaultPollingTime);
    }
    public  void dismissAlert( int timeout){
        initializeTimeoutAndPolling();
        dismissAlert( timeout,defaultPollingTime);
    }
    public  void dismissAlert(){
        initializeTimeoutAndPolling();
        dismissAlert(defaultTimeout,defaultPollingTime);
    }
    public  void sendDataToAlert( String data, int timeout){
        initializeTimeoutAndPolling();
        sendDataToAlert(data,timeout,defaultPollingTime);
    }
    public  void sendDataToAlert( String data){
        initializeTimeoutAndPolling();
        sendDataToAlert(data,defaultTimeout,defaultPollingTime);
    }
    public  void clickOnElement( By locator,int timeout) {
        initializeTimeoutAndPolling();
        clickOnElement( locator, timeout, defaultPollingTime);
    }

    // Overloaded selectDropdownByText method with default timeout and polling time
    public  void selectDropdownByText( By locator, String option) {
        initializeTimeoutAndPolling();
        selectDropdownByText( locator, option, defaultTimeout, defaultPollingTime);
    }

    // Overloaded selectDropdownByValue method with default timeout and polling time
    public  void selectDropdownByValue( By locator, String value) {
        initializeTimeoutAndPolling();
        selectDropdownByValue( locator, value, defaultTimeout, defaultPollingTime);
    }

    // Overloaded selectDropdownByIndex method with default timeout and polling time
    public  void selectDropdownByIndex( By locator, int index) {
        initializeTimeoutAndPolling();
        selectDropdownByIndex( locator, index, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForElementToBeClickable method with default timeout and polling time
    public  WebElement waitForElementToBeClickable( By locator) {
        initializeTimeoutAndPolling();
        return waitForElementToBeClickable( locator, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForElementToBeVisible method with default timeout and polling time
    public  WebElement waitForElementToBeVisible( By locator) {
        initializeTimeoutAndPolling();
        return waitForElementToBeVisible( locator, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForElementPresence method with default timeout and polling time
    public  WebElement waitForElementPresence( By locator) {
        initializeTimeoutAndPolling();
        return waitForElementPresence( locator, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForElementToDisappear method with default timeout and polling time
    public  void waitForElementToDisappear( By locator) {
        initializeTimeoutAndPolling();
        waitForElementToDisappear(locator, defaultTimeout, defaultPollingTime);
    }
    // Overloaded getAttributeValue method with default timeout and polling time
    public  String getAttributeValue( By locator, String attribute) {
        initializeTimeoutAndPolling();
        return getAttributeValue( locator, attribute, defaultTimeout, defaultPollingTime);
    }
    // Double-click on an element with timeout (default polling time)
    public  void doubleClick( By locator, int timeout) {
        initializeTimeoutAndPolling();
        doubleClick( locator, timeout, defaultPollingTime);
    }

    // Double-click on an element with default timeout and polling time
    public  void doubleClick( By locator) {
        initializeTimeoutAndPolling();
        doubleClick( locator, defaultTimeout, defaultPollingTime);
    }
    // Overloaded getElements method
    public  List<WebElement> getElements( By locator, int timeout) {
        initializeTimeoutAndPolling();
        return getElements( locator, timeout, defaultPollingTime);
    }
    public  List<WebElement> getElements( By locator) {
        initializeTimeoutAndPolling();
        return getElements( locator, defaultTimeout, defaultPollingTime);
    }
    // Overload example for moveSliderByOffset with default timeout
    public  void moveSliderByOffset( By sliderLocator, int xOffset, int yOffset) {
        initializeTimeoutAndPolling();
        moveSliderByOffset( sliderLocator, xOffset, yOffset, defaultTimeout, defaultPollingTime);
    }

    // Overload example for hoverOverElement with default timeout
    public  void hoverOverElement( By locator) {
        initializeTimeoutAndPolling();
        hoverOverElement( locator, defaultTimeout);
    }
    // Hover and click method with overload for timeout and polling
    public  void hoverAndClick( By locatorToHover, By locatorToClick,int timeout) {
        initializeTimeoutAndPolling();
        hoverAndClick( locatorToHover, locatorToClick, timeout,defaultPollingTime);
    }
    public  void hoverAndClick( By locatorToHover, By locatorToClick) {
        initializeTimeoutAndPolling();
        hoverAndClick( locatorToHover, locatorToClick, defaultTimeout, defaultPollingTime);
    }

    // Initialize timeout and polling time only once
    private  void initializeTimeoutAndPolling() {
        if (!defaultTimeoutGotFlag) {
            initTimeout();
            defaultTimeoutGotFlag = true;
            Allure.step("Initialize default Timeout for Element ", Status.PASSED);
        }
        if (!defaultPollingTimeGotFlag) {
            initPolling();
            defaultPollingTimeGotFlag = true;
            Allure.step("Initialize default Polling Time for Element ", Status.PASSED);
        }
    }
    private  int parseProperty(String value, int defaultValue, String propertyName) {
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logsUtils.warn("Invalid value for " + propertyName + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    // Initialize default timeout from properties file
    private  void initTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultElementWaitTimeout");
            defaultTimeout = parseProperty(timeout, 5, "defaultElementWaitTimeout");
        } catch (Exception e) {
            logsUtils.logException(e);
            defaultTimeout = 5;  // Assign default if exception occurs
        }
    }
    // Initialize default polling time from properties file
    private  void initPolling() {
        try {
            String polling = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultElementPollingTime");
            defaultPollingTime = parseProperty(polling, 5, "defaultElementPollingTime");
        } catch (Exception e) {
            logsUtils.logException(e);
            defaultPollingTime = 5;  // Assign default if exception occurs
        }
    }
    // Sleep for a specified number of milliseconds
    public  void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
            Allure.step("Sleeping for " + millis + " milliseconds", Status.PASSED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logsUtils.error("Sleep interrupted: " + e.getMessage());
            Allure.step("Sleep interrupted: " + e.getMessage(), Status.FAILED);
        }
    }
    public  void sleepSeconds(long seconds) {
        Allure.step("Sleeping for " + seconds + " seconds", Status.PASSED);
        sleepMillis(seconds * 1000);
    }
    public  void sleepMinutes(long minutes) {
        Allure.step("Sleeping for " + minutes + " minutes", Status.PASSED);
        sleepMillis(minutes * 60 * 1000);
    }
    private static ArrayList<Class<? extends Exception>> expectedExceptions;
    {
        expectedExceptions = new ArrayList<>();
        expectedExceptions.add(java.lang.ClassCastException.class);
        expectedExceptions.add(org.openqa.selenium.NoSuchElementException.class);
        expectedExceptions.add(org.openqa.selenium.StaleElementReferenceException.class);
        expectedExceptions.add(org.openqa.selenium.JavascriptException.class);
        expectedExceptions.add(org.openqa.selenium.ElementClickInterceptedException.class);
        expectedExceptions.add(org.openqa.selenium.ElementNotInteractableException.class);
        expectedExceptions.add(org.openqa.selenium.InvalidElementStateException.class);
        expectedExceptions.add(org.openqa.selenium.interactions.MoveTargetOutOfBoundsException.class);
        expectedExceptions.add(org.openqa.selenium.WebDriverException.class);
        expectedExceptions.add(ExecutionException.class);
        expectedExceptions.add(InterruptedException.class);
    }
}