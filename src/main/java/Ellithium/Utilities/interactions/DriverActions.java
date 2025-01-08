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
public class DriverActions <T extends WebDriver>{
    private final T driver;
    @SuppressWarnings("unchecked")
    public DriverActions(T driver) {
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
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
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
        return findWebElement( locator).getDomAttribute(attribute);
    }

    public  boolean waitForElementToBeSelected( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element to be Selected: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeSelected(locator));
    }

    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Attribute: '" + attribute + "' to be: '" + value + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.attributeToBe(locator, attribute, value));
    }

    public  boolean waitForElementAttributeContains( By locator, String attribute, String value, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Attribute: '" + attribute + "' to contain: '" + value + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.attributeContains(locator, attribute, value));
    }
    public  boolean waitForElementStaleness( WebElement element, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Staleness: " + element.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.stalenessOf(element));
    }
    public  boolean waitForTitleContains( String titlePart, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Title to Contain: '" + titlePart + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.titleContains(titlePart));
    }
    public  boolean waitForUrlContains( String urlPart, int timeout, int pollingEvery) {
        Reporter.log("Waiting for URL to Contain: '" + urlPart + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.urlContains(urlPart));
    }
    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available and Switching to it: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }
    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available by Name or ID: '" + nameOrId + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
    }
    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available by Index: " + index, LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }
    public  boolean waitForElementToBeEnabled( By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element to be Enabled: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator)).isEnabled();
    }
    public  boolean waitForTitleIs( String title, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Title to be: '" + title + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.titleIs(title));
    }
    public  boolean waitForUrlToBe( String url, int timeout, int pollingEvery) {
        Reporter.log("Waiting for URL to be: '" + url + "'", LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.urlToBe(url));
    }
    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Selection State to be: " + selected + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementSelectionStateToBe(locator, selected));
    }
    public  boolean waitForTextToBePresentInElementValue( By locator, String text, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Text to be Present in Element Value: '" + text + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementValue(locator, text));
    }


    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Windows to be: " + numberOfWindows, LogLevel.INFO_BLUE);
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.numberOfWindowsToBe(numberOfWindows));
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
        return getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
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
        return driver.switchTo().alert().getText();
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
    @SuppressWarnings("unchecked")
    public FluentWait<T> getFluentWait(int timeoutInSeconds, int pollingEveryInMillis) {
            return (FluentWait<T> )WaitManager.getFluentWait(driver,timeoutInSeconds,pollingEveryInMillis);
    }
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
        WebElement element = getFluentWait( timeout, WaitManager.getDefaultPollingTime())
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
        return waitForElementToBeSelected( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementToBeSelected( By locator, int timeout) {
        return waitForElementToBeSelected( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value) {
        return waitForElementAttributeToBe( locator, attribute, value, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementAttributeToBe( By locator, String attribute, String value, int timeout) {
        return waitForElementAttributeToBe( locator, attribute, value, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementAttributeContains( By locator, String attribute, String value) {
        return waitForElementAttributeContains( locator, attribute, value, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementAttributeContains( By locator, String attribute, String value, int timeout) {
        return waitForElementAttributeContains( locator, attribute, value, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementStaleness( WebElement element) {
        return waitForElementStaleness( element, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementStaleness( WebElement element, int timeout) {
        return waitForElementStaleness( element, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForTitleContains( String titlePart) {
        return waitForTitleContains( titlePart, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForTitleContains( String titlePart, int timeout) {
        return waitForTitleContains( titlePart, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForUrlContains( String urlPart) {
        return waitForUrlContains( urlPart, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  List<String> getTextFromMultipleElements( By locator) {
        return getTextFromMultipleElements( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  List<String> getTextFromMultipleElements( By locator, int timeout) {
        return getTextFromMultipleElements( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    public  List<String> getAttributeFromMultipleElements( By locator, String attribute) {
        return getAttributeFromMultipleElements( locator, attribute, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  List<String> getAttributeFromMultipleElements( By locator, String attribute, int timeout) {
        return getAttributeFromMultipleElements( locator, attribute, timeout, WaitManager.getDefaultPollingTime());
    }
    public  void clickOnMultipleElements( By locator) {
        clickOnMultipleElements( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  void clickOnMultipleElements( By locator, int timeout) {
        clickOnMultipleElements( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    public  void sendDataToMultipleElements( By locator, String data) {
        sendDataToMultipleElements( locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  void sendDataToMultipleElements( By locator, String data, int timeout) {
        sendDataToMultipleElements( locator, data, timeout, WaitManager.getDefaultPollingTime());
    }
    public  void selectDropdownByTextForMultipleElements( By locator, String option) {
        selectDropdownByTextForMultipleElements( locator, option, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  void selectDropdownByTextForMultipleElements( By locator, String option, int timeout) {
        selectDropdownByTextForMultipleElements( locator, option, timeout, WaitManager.getDefaultPollingTime());
    }
    public  void switchToFrameByIndex( int index) {
        switchToFrameByIndex( index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  void switchToFrameByIndex( int index, int timeout) {
        switchToFrameByIndex( index, timeout, WaitManager.getDefaultPollingTime());
    }
    public  void switchToFrameByNameOrID( String nameOrID) {
        switchToFrameByNameOrID( nameOrID, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  void switchToFrameByNameOrID( String nameOrID, int timeout) {
        switchToFrameByNameOrID( nameOrID, timeout, WaitManager.getDefaultPollingTime());
    }
    public  void switchToFrameByElement( By locator) {
        switchToFrameByElement( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  void switchToFrameByElement( By locator, int timeout) {
        switchToFrameByElement( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    public  void switchToPopupWindow( String expectedPopupTitle) {
        switchToPopupWindow( expectedPopupTitle, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  void switchToPopupWindow( String expectedPopupTitle, int timeout) {
        switchToPopupWindow( expectedPopupTitle, timeout, WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForUrlContains( String urlPart, int timeout) {
        return waitForUrlContains( urlPart, timeout, WaitManager.getDefaultPollingTime());
    }
    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator) {
        return waitForFrameToBeAvailableAndSwitchToIt( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  WebDriver waitForFrameToBeAvailableAndSwitchToIt( By locator, int timeout) {
        return waitForFrameToBeAvailableAndSwitchToIt( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId) {
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(nameOrId, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( String nameOrId, int timeout) {
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt( nameOrId, timeout, WaitManager.getDefaultPollingTime());
    }
    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index) {
        return waitForFrameByIndexToBeAvailableAndSwitchToIt( index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt( int index, int timeout) {
        return waitForFrameByIndexToBeAvailableAndSwitchToIt( index, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementToBeEnabled( By locator) {
        return waitForElementToBeEnabled(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForElementToBeEnabled( By locator, int timeout) {
        return waitForElementToBeEnabled( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForTitleIs( String title) {
        return waitForTitleIs(title, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForTitleIs( String title, int timeout) {
        return waitForTitleIs( title, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForUrlToBe( String url) {
        return waitForUrlToBe( url, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForUrlToBe( String url, int timeout) {
        return waitForUrlToBe( url, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected) {
        return waitForElementSelectionStateToBe( locator, selected, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForElementSelectionStateToBe( By locator, boolean selected, int timeout) {
        return waitForElementSelectionStateToBe( locator, selected, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForTextToBePresentInElementValue( By locator, String text) {
        return waitForTextToBePresentInElementValue( locator, text, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForTextToBePresentInElementValue( By locator, String text, int timeout) {
        return waitForTextToBePresentInElementValue( locator, text, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows) {
        return waitForNumberOfWindowsToBe( numberOfWindows, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForNumberOfWindowsToBe( int numberOfWindows, int timeout) {
        return waitForNumberOfWindowsToBe( numberOfWindows, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForNumberOfElementsToBeMoreThan( By locator, int number) {
        return waitForNumberOfElementsToBeMoreThan( locator, number, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForNumberOfElementsToBeMoreThan( By locator, int number, int timeout) {
        return waitForNumberOfElementsToBeMoreThan( locator, number, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForNumberOfElementsToBeLessThan( By locator, int number) {
        return waitForNumberOfElementsToBeLessThan( locator, number, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  void javascriptClick( By locator, int timeout) {
        getFluentWait( timeout, WaitManager.getDefaultPollingTime())
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick( locator);
    }
    public  void javascriptClick( By locator, int timeout, int pollingEvery) {
        getFluentWait( timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick( locator);
    }
    public  WebElement waitForTextToBePresentInElement( By locator, String text) {
        return waitForTextToBePresentInElement( locator, text, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  WebElement waitForTextToBePresentInElement( By locator, String text, int timeout) {
        return waitForTextToBePresentInElement( locator, text, timeout, WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForNumberOfElementsToBeLessThan( By locator, int number, int timeout) {
        return waitForNumberOfElementsToBeLessThan( locator, number, timeout, WaitManager.getDefaultPollingTime());
    }
    public  List<WebElement> waitForVisibilityOfAllElements( By locator) {
        return waitForVisibilityOfAllElements( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  List<WebElement> waitForVisibilityOfAllElements( By locator, int timeout) {
        return waitForVisibilityOfAllElements( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    public  boolean waitForNumberOfElementsToBe( By locator, int number) {
        return waitForNumberOfElementsToBe( locator, number, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  boolean waitForNumberOfElementsToBe( By locator, int number, int timeout) {
        return waitForNumberOfElementsToBe( locator, number, timeout, WaitManager.getDefaultPollingTime());
    }
    public  void rightClick( By locator, int timeout){
        rightClick(locator,timeout,WaitManager.getDefaultPollingTime());
    }
    public  void rightClick( By locator){
        rightClick(locator,WaitManager.getDefaultTimeout(),WaitManager.getDefaultPollingTime());
    }

    public  void sendData( By locator, String data) {
        sendData( locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    public  void sendData( By locator, Keys data) {
        sendData( locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overloaded getText method with default timeout and polling time
    public  String getText( By locator) {
        return getText( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    // Overloaded clickOnElement method with default timeout and polling time
    public  void clickOnElement( By locator) {
        clickOnElement( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  void sendData( By locator, String data, int timout) {
        sendData( locator, data, timout, WaitManager.getDefaultPollingTime());
    }

    public  void sendData( By locator, Keys data, int timout) {
        sendData( locator, data, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overloaded getText method with default timeout and polling time
    public  String getText( By locator, int timout) {
        return getText( locator, timout, WaitManager.getDefaultPollingTime());
    }
    public  void acceptAlert( int timeout) {
        acceptAlert(timeout, WaitManager.getDefaultPollingTime());
    }
    public  void acceptAlert() {
        acceptAlert(WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  String getAlertText( int timeout){
        return getAlertText( timeout, WaitManager.getDefaultPollingTime());
    }
    public  String getAlertText(){
        return getAlertText(WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    public  void dismissAlert( int timeout){
        dismissAlert( timeout,WaitManager.getDefaultPollingTime());
    }
    public  void dismissAlert(){
        dismissAlert(WaitManager.getDefaultTimeout(),WaitManager.getDefaultPollingTime());
    }
    public  void sendDataToAlert( String data, int timeout){
        sendDataToAlert(data,timeout,WaitManager.getDefaultPollingTime());
    }
    public  void sendDataToAlert( String data){
        sendDataToAlert(data,WaitManager.getDefaultTimeout(),WaitManager.getDefaultPollingTime());
    }
    public  void clickOnElement( By locator,int timeout) {
        clickOnElement( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    // Overloaded selectDropdownByText method with default timeout and polling time
    public  void selectDropdownByText( By locator, String option) {
        selectDropdownByText( locator, option, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overloaded selectDropdownByValue method with default timeout and polling time
    public  void selectDropdownByValue( By locator, String value) {
        selectDropdownByValue( locator, value, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overloaded selectDropdownByIndex method with default timeout and polling time
    public  void selectDropdownByIndex( By locator, int index) {
        selectDropdownByIndex( locator, index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overloaded waitForElementToBeClickable method with default timeout and polling time
    public  WebElement waitForElementToBeClickable( By locator) {
        return waitForElementToBeClickable( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overloaded waitForElementToBeVisible method with default timeout and polling time
    public  WebElement waitForElementToBeVisible( By locator) {
        return waitForElementToBeVisible( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overloaded waitForElementPresence method with default timeout and polling time
    public  WebElement waitForElementPresence( By locator) {
        return waitForElementPresence( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overloaded waitForElementToDisappear method with default timeout and polling time
    public  void waitForElementToDisappear( By locator) {
        waitForElementToDisappear(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    // Overloaded getAttributeValue method with default timeout and polling time
    public  String getAttributeValue( By locator, String attribute) {
        return getAttributeValue( locator, attribute, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    // Double-click on an element with timeout (default polling time)
    public  void doubleClick( By locator, int timeout) {
        doubleClick( locator, timeout, WaitManager.getDefaultPollingTime());
    }

    // Double-click on an element with default timeout and polling time
    public  void doubleClick( By locator) {
        doubleClick( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    // Overloaded getElements method
    public  List<WebElement> getElements( By locator, int timeout) {
        return getElements( locator, timeout, WaitManager.getDefaultPollingTime());
    }
    public  List<WebElement> getElements( By locator) {
        return getElements( locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    // Overload example for moveSliderByOffset with default timeout
    public  void moveSliderByOffset( By sliderLocator, int xOffset, int yOffset) {
        moveSliderByOffset( sliderLocator, xOffset, yOffset, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    // Overload example for hoverOverElement with default timeout
    public  void hoverOverElement( By locator) {
        hoverOverElement( locator, WaitManager.getDefaultTimeout());
    }
    // Hover and click method with overload for timeout and polling
    public  void hoverAndClick( By locatorToHover, By locatorToClick,int timeout) {
        hoverAndClick( locatorToHover, locatorToClick, timeout,WaitManager.getDefaultPollingTime());
    }
    public  void hoverAndClick( By locatorToHover, By locatorToClick) {
        hoverAndClick( locatorToHover, locatorToClick, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }


    // Sleep for a specified number of milliseconds
    public  void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
            Allure.step("Sleeping for " + millis + " milliseconds", Status.PASSED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.error("Sleep interrupted: " + e.getMessage());
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
}