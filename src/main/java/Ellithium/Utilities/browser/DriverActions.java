package Ellithium.Utilities.browser;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.logsUtils;
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
import java.util.*;

public class DriverActions {
    private static int defaultTimeout= 5;
    private static int defaultPollingTime=500;
    private static boolean defaultTimeoutGotFlag=false;
    private static boolean defaultPollingTimeGotFlag=false;
    public static void sendData(WebDriver driver, By locator, String data, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(driver,locator).clear();
        findWebElement(driver,locator).sendKeys(data);
    }
    public static void sendData(WebDriver driver, By locator, Keys data, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(driver,locator).clear();
        findWebElement(driver,locator).sendKeys(data);
    }
    public static String getText(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        String text = findWebElement(driver,locator).getText();
        return text;
    }
    public static void clickOnElement(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        findWebElement(driver,locator).click();
    }
    public static WebDriverWait generalWait(WebDriver driver, int timeout) {
        Reporter.log("Getting general Wait For "+ timeout + " seconds",LogLevel.INFO_BLUE);
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }
    public static void scrollToElement(WebDriver driver, By locator) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", findWebElement(driver, locator));
        Reporter.log("Scrolling To Element: ",LogLevel.INFO_BLUE,locator.toString());
    }
    public static void selectDropdownByText(WebDriver driver, By locator, String option, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement(driver, locator)).selectByVisibleText(option);
        Reporter.log("Selecting Dropdown Option By Text: " + option + " From Element: ",LogLevel.INFO_BLUE,locator.toString());
    }
    public static void selectDropdownByValue(WebDriver driver, By locator, String value, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement(driver, locator)).selectByValue(value);
        Reporter.log("Selecting Dropdown Option By Value: " + value + " From Element: ",LogLevel.INFO_BLUE,locator.toString());
    }

    public static void selectDropdownByIndex(WebDriver driver, By locator, int index, int timeout, int pollingEvery) {
        Reporter.log("Selecting Dropdown Option By Index: " + index + " From Element: " ,LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement(driver, locator)).selectByIndex(index);
    }
    public static void setImplicitWait(WebDriver driver, int timeout) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
    }

    public static void javascriptClick(WebDriver driver, By locator) {
        WebElement element = findWebElement(driver, locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        Reporter.log("JavaScript Click On Element: ",LogLevel.INFO_BLUE,locator.toString());
    }

    public static void waitForElementToDisappear(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        Reporter.log("Waiting for Element To Disappear: ",LogLevel.INFO_BLUE,locator.toString());
    }
    public static WebElement waitForElementToBeClickable(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        Reporter.log("Wait For Element To Be Clickable: ",LogLevel.INFO_BLUE,locator.toString());
        return findWebElement(driver,locator);
    }
    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Reporter.log("Wait For Element To Be Visible: ",LogLevel.INFO_BLUE,locator.toString());
        return findWebElement(driver, locator);
    }

    public static WebElement waitForElementPresence(WebDriver driver, By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Presence: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        return findWebElement(driver, locator);
    }

    public static WebElement waitForTextToBePresentInElement(WebDriver driver, By locator, String text, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Text: '" + text + "' to be present in Element: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
        return findWebElement(driver, locator);
    }

    public static String getAttributeValue(WebDriver driver, By locator, String attribute, int timeout, int pollingEvery) {
        Reporter.log("Getting Attribute: '" + attribute + "' from Element: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        String attributeValue = findWebElement(driver, locator).getAttribute(attribute);
        return attributeValue;
    }

    public static boolean waitForElementToBeSelected(WebDriver driver, By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element to be Selected: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean isSelected = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeSelected(locator));
        return isSelected;
    }

    public static boolean waitForElementAttributeToBe(WebDriver driver, By locator, String attribute, String value, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Attribute: '" + attribute + "' to be: '" + value + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.attributeToBe(locator, attribute, value));
        return result;
    }

    public static boolean waitForElementAttributeContains(WebDriver driver, By locator, String attribute, String value, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Attribute: '" + attribute + "' to contain: '" + value + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.attributeContains(locator, attribute, value));
        return result;
    }
    public static boolean waitForElementStaleness(WebDriver driver, WebElement element, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Staleness: " + element.toString(), LogLevel.INFO_BLUE);
        boolean isStale = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.stalenessOf(element));
        return isStale;
    }
    public static boolean waitForTitleContains(WebDriver driver, String titlePart, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Title to Contain: '" + titlePart + "'", LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.titleContains(titlePart));
        return result;
    }
    public static boolean waitForUrlContains(WebDriver driver, String urlPart, int timeout, int pollingEvery) {
        Reporter.log("Waiting for URL to Contain: '" + urlPart + "'", LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.urlContains(urlPart));
        return result;
    }
    public static WebDriver waitForFrameToBeAvailableAndSwitchToIt(WebDriver driver, By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available and Switching to it: " + locator.toString(), LogLevel.INFO_BLUE);
        WebDriver frame = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
        return frame;
    }
    public static WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(WebDriver driver, String nameOrId, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available by Name or ID: '" + nameOrId + "'", LogLevel.INFO_BLUE);
        WebDriver frame = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
        return frame;
    }
    public static WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt(WebDriver driver, int index, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Frame to be Available by Index: " + index, LogLevel.INFO_BLUE);
        WebDriver frame = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
        return frame;
    }
    public static boolean waitForElementToBeEnabled(WebDriver driver, By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element to be Enabled: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean isEnabled = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator)).isEnabled();
        return isEnabled;
    }
    public static boolean waitForTitleIs(WebDriver driver, String title, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Title to be: '" + title + "'", LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.titleIs(title));
        return result;
    }
    public static boolean waitForUrlToBe(WebDriver driver, String url, int timeout, int pollingEvery) {
        Reporter.log("Waiting for URL to be: '" + url + "'", LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.urlToBe(url));
        return result;
    }
    public static boolean waitForElementSelectionStateToBe(WebDriver driver, By locator, boolean selected, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Element Selection State to be: " + selected + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.elementSelectionStateToBe(locator, selected));
        return result;
    }
    public static boolean waitForTextToBePresentInElementValue(WebDriver driver, By locator, String text, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Text to be Present in Element Value: '" + text + "' for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementValue(locator, text));
        return result;
    }


    public static boolean waitForNumberOfWindowsToBe(WebDriver driver, int numberOfWindows, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Windows to be: " + numberOfWindows, LogLevel.INFO_BLUE);
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.numberOfWindowsToBe(numberOfWindows));
        return result;
    }

    public static boolean waitForNumberOfElementsToBeMoreThan(WebDriver driver, By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be More Than: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(locator, number)).size();
        return size > number;
    }

    public static boolean waitForNumberOfElementsToBeLessThan(WebDriver driver, By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be Less Than: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBeLessThan(locator, number)).size();
        return size < number;
    }

    public static List<WebElement> waitForVisibilityOfAllElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Visibility of All Elements for: " + locator.toString(), LogLevel.INFO_BLUE);
        List<WebElement> elements = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        return elements;
    }

    public static boolean waitForNumberOfElementsToBe(WebDriver driver, By locator, int number, int timeout, int pollingEvery) {
        Reporter.log("Waiting for Number of Elements to be: " + number + " for Element: " + locator.toString(), LogLevel.INFO_BLUE);
        int size = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBe(locator, number)).size();
        return size == number;
    }
    public static void switchToNewWindow(WebDriver driver, String windowTitle) {
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
    public static void closeCurrentWindow(WebDriver driver) {
        driver.close();
    }

    // Switch to the original window
    public static void switchToOriginalWindow(WebDriver driver, String originalWindowHandle) {
        driver.switchTo().window(originalWindowHandle);
    }
    // Find an element (no need for timeout or polling)
    public static WebElement findWebElement(WebDriver driver, By locator) {
        return driver.findElement(locator);
    }
    // Find elements (no need for timeout or polling)
    public static List<WebElement> findWebElements(WebDriver driver, By locator) {
        return driver.findElements(locator);
    }
    // Accept an alert
    public static void acceptAlert(WebDriver driver, int timeout, int pollingEvery) {
        getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
    }
    public static void dismissAlert(WebDriver driver, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().dismiss();
    }
    public static String getAlertText(WebDriver driver, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        String alertText = driver.switchTo().alert().getText();
        return alertText;
    }
    public static void sendDataToAlert(WebDriver driver, String data, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().sendKeys(data);
    }
    // Get text from multiple elements
    public static List<String> getTextFromMultipleElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery).until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        Reporter.log("Getting text from multiple elements located: ",LogLevel.INFO_BLUE,locator.toString());
        List<WebElement> elements = findWebElements(driver,locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }
    // Utility method to create a FluentWait instance
    public static FluentWait<WebDriver> getFluentWait(WebDriver driver, int timeoutInSeconds, int pollingEveryInMillis) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutInSeconds))
                .pollingEvery(Duration.ofMillis(pollingEveryInMillis));
    }
    // Get text from multiple elements
    public static List<String> getAttributeFromMultipleElements(WebDriver driver, By locator,String Attribute, int timeout, int pollingEvery) {
        Reporter.log("Getting Attribute from multiple elements located: ",LogLevel.INFO_BLUE,locator.toString());
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(driver,locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getAttribute(Attribute));
        }
        return texts;
    }
    // Click on multiple elements
    public static void clickOnMultipleElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(driver,locator);
        for (WebElement element : elements) {
            element.click();
        }
    }
    // Send data to multiple elements
    public static void sendDataToMultipleElements(WebDriver driver, By locator, String data, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        Reporter.log("Sending data to multiple elements located: ",LogLevel.INFO_BLUE,locator.toString());
        List<WebElement> elements = findWebElements(driver,locator);
        for (WebElement element : elements) {
            element.clear();
            element.sendKeys(data);
        }
    }
    // Select from dropdowns on multiple elements by visible text
    public static void selectDropdownByTextForMultipleElements(WebDriver driver, By locator, String option, int timeout, int pollingEvery) {
        Reporter.log("Selecting dropdown option by text for multiple elements: " + option + " for locator: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        List<WebElement> elements = findWebElements(driver, locator);
        for (WebElement element : elements) {
            new Select(element).selectByVisibleText(option);
        }
    }

    // Switch to frame by index
    public static void switchToFrameByIndex(WebDriver driver, int index, int timeout,int pollingTime) {
        getFluentWait(driver,timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }
    // Hover over an element with specified timeout
    public static void hoverOverElement(WebDriver driver, By locator, int timeout) {
        Reporter.log("Hovered over element: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait(driver, timeout, defaultPollingTime)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.moveToElement(element).perform();
    }


    // New method: Scroll to page bottom
    public static void scrollToPageBottom(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Reporter.log("Scrolled to page bottom", LogLevel.INFO_BLUE);
    }

    // New method: JS Executor - Set value using JavaScript
    public static void setElementValueUsingJS(WebDriver driver, By locator, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement element = findWebElement(driver, locator);
        js.executeScript("arguments[0].value = arguments[1];", element, value);
        Reporter.log("Set value using JavaScript: " + value + " on element: " + locator.toString(), LogLevel.INFO_BLUE);
    }

    // Switch to frame by name or ID
    public static void switchToFrameByNameOrID(WebDriver driver, String nameOrID, int timeout,int pollingTime) {
        getFluentWait(driver,timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrID));
    }
    // Switch to frame by WebElement
    public static void switchToFrameByElement(WebDriver driver, By locator, int timeout,int pollingTime) {
        getFluentWait(driver,timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    // Switch back to default content from frame
    public static void switchToDefaultContent(WebDriver driver) {
        driver.switchTo().defaultContent();
    }
    public static void switchToPopupWindow(WebDriver driver, String expectedPopupTitle, int timeout, int pollingTime) {
        String mainWindow = driver.getWindowHandle();
        Reporter.log("Waiting for popup window to appear.", LogLevel.INFO_BLUE);

        boolean windowsAppeared = getFluentWait(driver, timeout, pollingTime)
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
    public static void closePopupWindow(WebDriver driver) {
        driver.close();
        Reporter.log("Popup window closed. Switching back to the main window.", LogLevel.INFO_BLUE);
        String mainWindow = driver.getWindowHandles().iterator().next();
        driver.switchTo().window(mainWindow);
    }
    public static float moveSliderTo(WebDriver driver, By sliderLocator, By rangeLocator, float targetValue) {
        WebElement range = findWebElement(driver, rangeLocator);
        WebElement slider = findWebElement(driver, sliderLocator);
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
    public static void dragAndDrop(WebDriver driver, By sourceLocator, By targetLocator) {
        WebElement source = findWebElement(driver, sourceLocator);
        WebElement target = findWebElement(driver, targetLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveToElement(target)
                .release()
                .perform();

        Reporter.log("Drag and drop performed from " + sourceLocator + " to " + targetLocator, LogLevel.INFO_BLUE);
    }
    public static void dragAndDropByOffset(WebDriver driver, By sourceLocator, int xOffset, int yOffset) {
        WebElement source = findWebElement(driver, sourceLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();

        Reporter.log("Drag and drop performed with offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }
    public static void hoverAndClick(WebDriver driver, By locatorToHover, By locatorToClick, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to hover: " + locatorToHover.toString(), LogLevel.INFO_BLUE);
        WebElement elementToHover = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locatorToHover));

        Reporter.log("Waiting for element to click: " + locatorToClick.toString(), LogLevel.INFO_BLUE);
        WebElement elementToClick = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locatorToClick));

        Actions action = new Actions(driver);
        action.moveToElement(elementToHover).click(elementToClick).perform();

        Reporter.log("Hovered over " + locatorToHover + " and clicked " + locatorToClick, LogLevel.INFO_BLUE);
    }
    public static String captureScreenshot(WebDriver driver, String screenshotName) {
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
    public static void moveSliderByOffset(WebDriver driver, By sliderLocator, int xOffset, int yOffset, int timeout, int pollingEvery) {
        Reporter.log("Waiting for slider to be visible: " + sliderLocator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(sliderLocator));

        WebElement slider = findWebElement(driver, sliderLocator);
        Actions action = new Actions(driver);
        action.clickAndHold(slider)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();
        Reporter.log("Slider moved by offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);

    }
    public static void scrollByOffset(WebDriver driver, int xOffset, int yOffset) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(arguments[0], arguments[1]);", xOffset, yOffset);

        Reporter.log("Scrolled by offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }
    public static void rightClick(WebDriver driver, By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to right-click: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));

        Actions action = new Actions(driver);
        action.contextClick(element).perform();

        Reporter.log("Right-clicked on element: " + locator, LogLevel.INFO_BLUE);
    }
    public static void doubleClick(WebDriver driver, By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to double-click: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.doubleClick(element).perform();
        Reporter.log("Double-clicked on element: " + locator, LogLevel.INFO_BLUE);
    }


    // Maximize the browser window
    public static void maximizeWindow(WebDriver driver) {
        driver.manage().window().maximize();
    }

    // Minimize the browser window
    public static void minimizeWindow(WebDriver driver) {
        driver.manage().window().minimize();
    }
    // Get all elements matching the locator
    public static List<WebElement> getElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        return findWebElements(driver,locator);
    }

    public static void navigateToUrl(WebDriver driver, String url) {
        driver.get(url);
    }
    // Refresh the current page
    public static void refreshPage(WebDriver driver) {
        driver.navigate().refresh();
    }
    public static void navigateBack(WebDriver driver) {
        driver.navigate().back();
    }
    public static void navigateForward(WebDriver driver) {
        driver.navigate().forward();
    }









    public static boolean waitForElementToBeSelected(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return waitForElementToBeSelected(driver, locator, defaultTimeout, defaultPollingTime);
    }
    public static boolean waitForElementToBeSelected(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementToBeSelected(driver, locator, timeout, defaultPollingTime);
    }
    public static boolean waitForElementAttributeToBe(WebDriver driver, By locator, String attribute, String value) {
        initializeTimeoutAndPolling();
        return waitForElementAttributeToBe(driver, locator, attribute, value, defaultTimeout, defaultPollingTime);
    }
    public static boolean waitForElementAttributeToBe(WebDriver driver, By locator, String attribute, String value, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementAttributeToBe(driver, locator, attribute, value, timeout, defaultPollingTime);
    }
    public static boolean waitForElementAttributeContains(WebDriver driver, By locator, String attribute, String value) {
        initializeTimeoutAndPolling();
        return waitForElementAttributeContains(driver, locator, attribute, value, defaultTimeout, defaultPollingTime);
    }
    public static boolean waitForElementAttributeContains(WebDriver driver, By locator, String attribute, String value, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementAttributeContains(driver, locator, attribute, value, timeout, defaultPollingTime);
    }
    public static boolean waitForElementStaleness(WebDriver driver, WebElement element) {
        initializeTimeoutAndPolling();
        return waitForElementStaleness(driver, element, defaultTimeout, defaultPollingTime);
    }
    public static boolean waitForElementStaleness(WebDriver driver, WebElement element, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementStaleness(driver, element, timeout, defaultPollingTime);
    }
    public static boolean waitForTitleContains(WebDriver driver, String titlePart) {
        initializeTimeoutAndPolling();
        return waitForTitleContains(driver, titlePart, defaultTimeout, defaultPollingTime);
    }
    public static boolean waitForTitleContains(WebDriver driver, String titlePart, int timeout) {
        initializeTimeoutAndPolling();
        return waitForTitleContains(driver, titlePart, timeout, defaultPollingTime);
    }
    public static boolean waitForUrlContains(WebDriver driver, String urlPart) {
        initializeTimeoutAndPolling();
        return waitForUrlContains(driver, urlPart, defaultTimeout, defaultPollingTime);
    }
    public static List<String> getTextFromMultipleElements(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return getTextFromMultipleElements(driver, locator, defaultTimeout, defaultPollingTime);
    }
    public static List<String> getTextFromMultipleElements(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        return getTextFromMultipleElements(driver, locator, timeout, defaultPollingTime);
    }
    public static List<String> getAttributeFromMultipleElements(WebDriver driver, By locator, String attribute) {
        initializeTimeoutAndPolling();
        return getAttributeFromMultipleElements(driver, locator, attribute, defaultTimeout, defaultPollingTime);
    }
    public static List<String> getAttributeFromMultipleElements(WebDriver driver, By locator, String attribute, int timeout) {
        initializeTimeoutAndPolling();
        return getAttributeFromMultipleElements(driver, locator, attribute, timeout, defaultPollingTime);
    }
    public static void clickOnMultipleElements(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        clickOnMultipleElements(driver, locator, defaultTimeout, defaultPollingTime);
    }
    public static void clickOnMultipleElements(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        clickOnMultipleElements(driver, locator, timeout, defaultPollingTime);
    }
    public static void sendDataToMultipleElements(WebDriver driver, By locator, String data) {
        initializeTimeoutAndPolling();
        sendDataToMultipleElements(driver, locator, data, defaultTimeout, defaultPollingTime);
    }
    public static void sendDataToMultipleElements(WebDriver driver, By locator, String data, int timeout) {
        initializeTimeoutAndPolling();
        sendDataToMultipleElements(driver, locator, data, timeout, defaultPollingTime);
    }
    public static void selectDropdownByTextForMultipleElements(WebDriver driver, By locator, String option) {
        initializeTimeoutAndPolling();
        selectDropdownByTextForMultipleElements(driver, locator, option, defaultTimeout, defaultPollingTime);
    }
    public static void selectDropdownByTextForMultipleElements(WebDriver driver, By locator, String option, int timeout) {
        initializeTimeoutAndPolling();
        selectDropdownByTextForMultipleElements(driver, locator, option, timeout, defaultPollingTime);
    }
    public static void switchToFrameByIndex(WebDriver driver, int index) {
        initializeTimeoutAndPolling();
        switchToFrameByIndex(driver, index, defaultTimeout, defaultPollingTime);
    }
    public static void switchToFrameByIndex(WebDriver driver, int index, int timeout) {
        initializeTimeoutAndPolling();
        switchToFrameByIndex(driver, index, timeout, defaultPollingTime);
    }
    public static void switchToFrameByNameOrID(WebDriver driver, String nameOrID) {
        initializeTimeoutAndPolling();
        switchToFrameByNameOrID(driver, nameOrID, defaultTimeout, defaultPollingTime);
    }

    public static void switchToFrameByNameOrID(WebDriver driver, String nameOrID, int timeout) {
        initializeTimeoutAndPolling();
        switchToFrameByNameOrID(driver, nameOrID, timeout, defaultPollingTime);
    }
    public static void switchToFrameByElement(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        switchToFrameByElement(driver, locator, defaultTimeout, defaultPollingTime);
    }

    public static void switchToFrameByElement(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        switchToFrameByElement(driver, locator, timeout, defaultPollingTime);
    }
    public static void switchToPopupWindow(WebDriver driver, String expectedPopupTitle) {
        initializeTimeoutAndPolling();
        switchToPopupWindow(driver, expectedPopupTitle, defaultTimeout, defaultPollingTime);
    }

    public static void switchToPopupWindow(WebDriver driver, String expectedPopupTitle, int timeout) {
        initializeTimeoutAndPolling();
        switchToPopupWindow(driver, expectedPopupTitle, timeout, defaultPollingTime);
    }

    public static boolean waitForUrlContains(WebDriver driver, String urlPart, int timeout) {
        initializeTimeoutAndPolling();
        return waitForUrlContains(driver, urlPart, timeout, defaultPollingTime);
    }
    public static WebDriver waitForFrameToBeAvailableAndSwitchToIt(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return waitForFrameToBeAvailableAndSwitchToIt(driver, locator, defaultTimeout, defaultPollingTime);
    }

    public static WebDriver waitForFrameToBeAvailableAndSwitchToIt(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        return waitForFrameToBeAvailableAndSwitchToIt(driver, locator, timeout, defaultPollingTime);
    }
    public static WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(WebDriver driver, String nameOrId) {
        initializeTimeoutAndPolling();
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(driver, nameOrId, defaultTimeout, defaultPollingTime);
    }

    public static WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(WebDriver driver, String nameOrId, int timeout) {
        initializeTimeoutAndPolling();
        return waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(driver, nameOrId, timeout, defaultPollingTime);
    }
    public static WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt(WebDriver driver, int index) {
        initializeTimeoutAndPolling();
        return waitForFrameByIndexToBeAvailableAndSwitchToIt(driver, index, defaultTimeout, defaultPollingTime);
    }

    public static WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt(WebDriver driver, int index, int timeout) {
        initializeTimeoutAndPolling();
        return waitForFrameByIndexToBeAvailableAndSwitchToIt(driver, index, timeout, defaultPollingTime);
    }
    public static boolean waitForElementToBeEnabled(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return waitForElementToBeEnabled(driver, locator, defaultTimeout, defaultPollingTime);
    }

    public static boolean waitForElementToBeEnabled(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementToBeEnabled(driver, locator, timeout, defaultPollingTime);
    }
    public static boolean waitForTitleIs(WebDriver driver, String title) {
        initializeTimeoutAndPolling();
        return waitForTitleIs(driver, title, defaultTimeout, defaultPollingTime);
    }

    public static boolean waitForTitleIs(WebDriver driver, String title, int timeout) {
        initializeTimeoutAndPolling();
        return waitForTitleIs(driver, title, timeout, defaultPollingTime);
    }
    public static boolean waitForUrlToBe(WebDriver driver, String url) {
        initializeTimeoutAndPolling();
        return waitForUrlToBe(driver, url, defaultTimeout, defaultPollingTime);
    }

    public static boolean waitForUrlToBe(WebDriver driver, String url, int timeout) {
        initializeTimeoutAndPolling();
        return waitForUrlToBe(driver, url, timeout, defaultPollingTime);
    }
    public static boolean waitForElementSelectionStateToBe(WebDriver driver, By locator, boolean selected) {
        initializeTimeoutAndPolling();
        return waitForElementSelectionStateToBe(driver, locator, selected, defaultTimeout, defaultPollingTime);
    }

    public static boolean waitForElementSelectionStateToBe(WebDriver driver, By locator, boolean selected, int timeout) {
        initializeTimeoutAndPolling();
        return waitForElementSelectionStateToBe(driver, locator, selected, timeout, defaultPollingTime);
    }
    public static boolean waitForTextToBePresentInElementValue(WebDriver driver, By locator, String text) {
        initializeTimeoutAndPolling();
        return waitForTextToBePresentInElementValue(driver, locator, text, defaultTimeout, defaultPollingTime);
    }

    public static boolean waitForTextToBePresentInElementValue(WebDriver driver, By locator, String text, int timeout) {
        initializeTimeoutAndPolling();
        return waitForTextToBePresentInElementValue(driver, locator, text, timeout, defaultPollingTime);
    }
    public static boolean waitForNumberOfWindowsToBe(WebDriver driver, int numberOfWindows) {
        initializeTimeoutAndPolling();
        return waitForNumberOfWindowsToBe(driver, numberOfWindows, defaultTimeout, defaultPollingTime);
    }

    public static boolean waitForNumberOfWindowsToBe(WebDriver driver, int numberOfWindows, int timeout) {
        initializeTimeoutAndPolling();
        return waitForNumberOfWindowsToBe(driver, numberOfWindows, timeout, defaultPollingTime);
    }
    public static boolean waitForNumberOfElementsToBeMoreThan(WebDriver driver, By locator, int number) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBeMoreThan(driver, locator, number, defaultTimeout, defaultPollingTime);
    }

    public static boolean waitForNumberOfElementsToBeMoreThan(WebDriver driver, By locator, int number, int timeout) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBeMoreThan(driver, locator, number, timeout, defaultPollingTime);
    }
    public static boolean waitForNumberOfElementsToBeLessThan(WebDriver driver, By locator, int number) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBeLessThan(driver, locator, number, defaultTimeout, defaultPollingTime);
    }
    public static void javascriptClick(WebDriver driver, By locator, int timeout) {
        getFluentWait(driver, timeout, defaultPollingTime)
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick(driver, locator);
    }
    public static void javascriptClick(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        javascriptClick(driver, locator);
    }
    public static WebElement waitForTextToBePresentInElement(WebDriver driver, By locator, String text) {
        return waitForTextToBePresentInElement(driver, locator, text, defaultTimeout, defaultPollingTime);
    }

    public static WebElement waitForTextToBePresentInElement(WebDriver driver, By locator, String text, int timeout) {
        return waitForTextToBePresentInElement(driver, locator, text, timeout, defaultPollingTime);
    }

    public static boolean waitForNumberOfElementsToBeLessThan(WebDriver driver, By locator, int number, int timeout) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBeLessThan(driver, locator, number, timeout, defaultPollingTime);
    }
    public static List<WebElement> waitForVisibilityOfAllElements(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return waitForVisibilityOfAllElements(driver, locator, defaultTimeout, defaultPollingTime);
    }

    public static List<WebElement> waitForVisibilityOfAllElements(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        return waitForVisibilityOfAllElements(driver, locator, timeout, defaultPollingTime);
    }
    public static boolean waitForNumberOfElementsToBe(WebDriver driver, By locator, int number) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBe(driver, locator, number, defaultTimeout, defaultPollingTime);
    }

    public static boolean waitForNumberOfElementsToBe(WebDriver driver, By locator, int number, int timeout) {
        initializeTimeoutAndPolling();
        return waitForNumberOfElementsToBe(driver, locator, number, timeout, defaultPollingTime);
    }
    public static void rightClick(WebDriver driver, By locator, int timeout){
        initializeTimeoutAndPolling();
        rightClick(driver,locator,timeout,defaultPollingTime);
    }
    public static void rightClick(WebDriver driver, By locator){
        initializeTimeoutAndPolling();
        rightClick(driver,locator,defaultTimeout,defaultPollingTime);
    }

    public static void sendData(WebDriver driver, By locator, String data) {
        initializeTimeoutAndPolling();
        sendData(driver, locator, data, defaultTimeout, defaultPollingTime);
    }

    public static void sendData(WebDriver driver, By locator, Keys data) {
        initializeTimeoutAndPolling();
        sendData(driver, locator, data, defaultTimeout, defaultPollingTime);
    }

    // Overloaded getText method with default timeout and polling time
    public static String getText(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return getText(driver, locator, defaultTimeout, defaultPollingTime);
    }
    // Overloaded clickOnElement method with default timeout and polling time
    public static void clickOnElement(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        clickOnElement(driver, locator, defaultTimeout, defaultPollingTime);
    }
    public static void sendData(WebDriver driver, By locator, String data, int timout) {
        initializeTimeoutAndPolling();
        sendData(driver, locator, data, timout, defaultPollingTime);
    }

    public static void sendData(WebDriver driver, By locator, Keys data, int timout) {
        initializeTimeoutAndPolling();
        sendData(driver, locator, data, defaultTimeout, defaultPollingTime);
    }

    // Overloaded getText method with default timeout and polling time
    public static String getText(WebDriver driver, By locator, int timout) {
        initializeTimeoutAndPolling();
        return getText(driver, locator, timout, defaultPollingTime);
    }
    public static void acceptAlert(WebDriver driver, int timeout) {
        initializeTimeoutAndPolling();
        acceptAlert(driver,timeout, defaultPollingTime);
    }
    public static void acceptAlert(WebDriver driver) {
        initializeTimeoutAndPolling();
        acceptAlert(driver,defaultTimeout, defaultPollingTime);
    }
    public static String getAlertText(WebDriver driver, int timeout){
        initializeTimeoutAndPolling();
        return getAlertText(driver,timeout, defaultPollingTime);
    }
    public static String getAlertText(WebDriver driver){
        initializeTimeoutAndPolling();
        return getAlertText(driver,defaultTimeout, defaultPollingTime);
    }
    public static void dismissAlert(WebDriver driver, int timeout){
        initializeTimeoutAndPolling();
        dismissAlert(driver,timeout,defaultPollingTime);
    }
    public static void dismissAlert(WebDriver driver){
        initializeTimeoutAndPolling();
        dismissAlert(driver,defaultTimeout,defaultPollingTime);
    }
    public static void sendDataToAlert(WebDriver driver, String data, int timeout){
        initializeTimeoutAndPolling();
        sendDataToAlert(driver,data,timeout,defaultPollingTime);
    }
    public static void sendDataToAlert(WebDriver driver, String data){
        initializeTimeoutAndPolling();
        sendDataToAlert(driver,data,defaultTimeout,defaultPollingTime);
    }
    public static void clickOnElement(WebDriver driver, By locator,int timeout) {
        initializeTimeoutAndPolling();
        clickOnElement(driver, locator, timeout, defaultPollingTime);
    }

    // Overloaded selectDropdownByText method with default timeout and polling time
    public static void selectDropdownByText(WebDriver driver, By locator, String option) {
        initializeTimeoutAndPolling();
        selectDropdownByText(driver, locator, option, defaultTimeout, defaultPollingTime);
    }

    // Overloaded selectDropdownByValue method with default timeout and polling time
    public static void selectDropdownByValue(WebDriver driver, By locator, String value) {
        initializeTimeoutAndPolling();
        selectDropdownByValue(driver, locator, value, defaultTimeout, defaultPollingTime);
    }

    // Overloaded selectDropdownByIndex method with default timeout and polling time
    public static void selectDropdownByIndex(WebDriver driver, By locator, int index) {
        initializeTimeoutAndPolling();
        selectDropdownByIndex(driver, locator, index, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForElementToBeClickable method with default timeout and polling time
    public static WebElement waitForElementToBeClickable(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return waitForElementToBeClickable(driver, locator, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForElementToBeVisible method with default timeout and polling time
    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return waitForElementToBeVisible(driver, locator, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForElementPresence method with default timeout and polling time
    public static WebElement waitForElementPresence(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return waitForElementPresence(driver, locator, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForElementToDisappear method with default timeout and polling time
    public static void waitForElementToDisappear(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        waitForElementToDisappear(driver, locator, defaultTimeout, defaultPollingTime);
    }
    // Overloaded getAttributeValue method with default timeout and polling time
    public static String getAttributeValue(WebDriver driver, By locator, String attribute) {
        initializeTimeoutAndPolling();
        return getAttributeValue(driver, locator, attribute, defaultTimeout, defaultPollingTime);
    }
    // Double-click on an element with timeout (default polling time)
    public static void doubleClick(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        doubleClick(driver, locator, timeout, defaultPollingTime);
    }

    // Double-click on an element with default timeout and polling time
    public static void doubleClick(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        doubleClick(driver, locator, defaultTimeout, defaultPollingTime);
    }
    // Overloaded getElements method
    public static List<WebElement> getElements(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        return getElements(driver, locator, timeout, defaultPollingTime);
    }
    public static List<WebElement> getElements(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return getElements(driver, locator, defaultTimeout, defaultPollingTime);
    }
    // Overload example for moveSliderByOffset with default timeout
    public static void moveSliderByOffset(WebDriver driver, By sliderLocator, int xOffset, int yOffset) {
        initializeTimeoutAndPolling();
        moveSliderByOffset(driver, sliderLocator, xOffset, yOffset, defaultTimeout, defaultPollingTime);
    }

    // Overload example for hoverOverElement with default timeout
    public static void hoverOverElement(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        hoverOverElement(driver, locator, defaultTimeout);
    }
    // Hover and click method with overload for timeout and polling
    public static void hoverAndClick(WebDriver driver, By locatorToHover, By locatorToClick,int timeout) {
        initializeTimeoutAndPolling();
        hoverAndClick(driver, locatorToHover, locatorToClick, timeout,defaultPollingTime);
    }
    public static void hoverAndClick(WebDriver driver, By locatorToHover, By locatorToClick) {
        initializeTimeoutAndPolling();
        hoverAndClick(driver, locatorToHover, locatorToClick, defaultTimeout, defaultPollingTime);
    }

    // Initialize timeout and polling time only once
    private static void initializeTimeoutAndPolling() {
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
    private static int parseProperty(String value, int defaultValue, String propertyName) {
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
    private static void initTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultElementWaitTimeout");
            defaultTimeout = parseProperty(timeout, 5, "defaultElementWaitTimeout");
        } catch (Exception e) {
            logsUtils.logException(e);
            defaultTimeout = 5;  // Assign default if exception occurs
        }
    }
    // Initialize default polling time from properties file
    private static void initPolling() {
        try {
            String polling = PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(), "defaultElementPollingTime");
            defaultPollingTime = parseProperty(polling, 5, "defaultElementPollingTime");
        } catch (Exception e) {
            logsUtils.logException(e);
            defaultPollingTime = 5;  // Assign default if exception occurs
        }
    }
    // Sleep for a specified number of milliseconds
    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
            Allure.step("Sleeping for " + millis + " milliseconds", Status.PASSED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logsUtils.error("Sleep interrupted: " + e.getMessage());
            Allure.step("Sleep interrupted: " + e.getMessage(), Status.FAILED);
        }
    }
    public static void sleepSeconds(long seconds) {
        Allure.step("Sleeping for " + seconds + " seconds", Status.PASSED);
        sleepMillis(seconds * 1000);
    }
    public static void sleepMinutes(long minutes) {
        Allure.step("Sleeping for " + minutes + " minutes", Status.PASSED);
        sleepMillis(minutes * 60 * 1000);
    }
}