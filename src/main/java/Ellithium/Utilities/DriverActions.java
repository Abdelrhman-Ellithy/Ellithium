package Ellithium.Utilities;

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
    private static final Map<Keys, String> keyMap;
    static {
        // Add more mappings as needed
        keyMap = Map.ofEntries(
                Map.entry(Keys.ENTER, "ENTER"),
                Map.entry(Keys.TAB, "TAB"),
                Map.entry(Keys.ESCAPE, "ESCAPE"),
                Map.entry(Keys.BACK_SPACE, "BACKSPACE"),
                Map.entry(Keys.SPACE, "SPACE"),
                Map.entry(Keys.ARROW_UP, "UP ARROW"),
                Map.entry(Keys.ARROW_DOWN, "DOWN ARROW"),
                Map.entry(Keys.ARROW_LEFT, "LEFT ARROW"),
                Map.entry(Keys.ARROW_RIGHT, "RIGHT ARROW"),
                Map.entry(Keys.DELETE, "DELETE"),
                Map.entry(Keys.HOME, "HOME"),
                Map.entry(Keys.END, "END"),
                Map.entry(Keys.PAGE_UP, "PAGE UP"),
                Map.entry(Keys.PAGE_DOWN, "PAGE DOWN"),
                Map.entry(Keys.SHIFT, "SHIFT"),
                Map.entry(Keys.CONTROL, "CONTROL"),
                Map.entry(Keys.ALT, "ALT"));
    }
    private static String getKeyName(Keys key) {
        return keyMap.getOrDefault(key, key.toString()); // Efficient lookup
    }
    private static final String configPath="src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config";
    public static void sendData(WebDriver driver, By locator, String data, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(driver,locator).clear();
        logsUtils.info(Colors.BLUE+"Sending Data \""+ data+"\" To Element: "+locator+Colors.RESET);
        findWebElement(driver,locator).sendKeys(data);
        Allure.step("Sending Data \""+ data+"\" To Element: "+locator, Status.PASSED);
    }
    public static void sendData(WebDriver driver, By locator, Keys data, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        findWebElement(driver,locator).clear();
        String keyName=getKeyName(data);
        logsUtils.info(Colors.BLUE+"Sending Data \""+ keyName+"\" To Element: "+locator+Colors.RESET);
        findWebElement(driver,locator).sendKeys(data);
        Allure.step("Sending Data \""+ keyName+"\" To Element: "+locator, Status.PASSED);
    }
    public static String getText(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Get Text From Element: "+locator+Colors.RESET);
        String text = findWebElement(driver,locator).getText();
        Allure.step("Getting Text From Element: "+locator, Status.PASSED);
        return text;
    }
    public static void clickOnElement(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        findWebElement(driver,locator).click();
        logsUtils.info(Colors.BLUE+"Click On Element: "+locator+Colors.RESET);
        Allure.step("Clicking On Element: "+locator, Status.PASSED);
    }
    public static WebDriverWait generalWait(WebDriver driver, int timeout) {
        Allure.step("General Wait For " + timeout + " seconds", Status.PASSED);
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }
    public static void scrollToElement(WebDriver driver, By locator) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", findWebElement(driver, locator));
        logsUtils.info(Colors.BLUE+"Scrolling To Element Located: "+locator+Colors.RESET);
        Allure.step("Scrolling To Element: "+locator, Status.PASSED);
    }
    public static void selectDropdownByText(WebDriver driver, By locator, String option, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Text: "+option+" From Element: "+locator+Colors.RESET);
        new Select(findWebElement(driver, locator)).selectByVisibleText(option);
        Allure.step("Selecting Dropdown Option By Text: " + option + " From Element: " + locator, Status.PASSED);
    }
    public static void selectDropdownByValue(WebDriver driver, By locator, String value, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Value: "+value+" From Element: "+locator+Colors.RESET);
        new Select(findWebElement(driver, locator)).selectByValue(value);
        Allure.step("Selecting Dropdown Option By Value: " + value + " From Element: " + locator, Status.PASSED);
    }

    public static void selectDropdownByIndex(WebDriver driver, By locator, int index, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Index: "+index+" From Element: "+locator+Colors.RESET);
        new Select(findWebElement(driver, locator)).selectByIndex(index);
        Allure.step("Selecting Dropdown Option By Index: " + index + " From Element: " + locator, Status.PASSED);
    }
    public static void setImplicitWait(WebDriver driver, int timeout) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
        logsUtils.info(Colors.BLUE + "Set Implicit Wait To: " + timeout + " seconds" + Colors.RESET);
        Allure.step("Setting Implicit Wait to " + timeout + " seconds", Status.PASSED);
    }

    public static void javascriptClick(WebDriver driver, By locator) {
        WebElement element = findWebElement(driver, locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        logsUtils.info(Colors.BLUE+"JavaScript Click On Element: "+locator+Colors.RESET);
        Allure.step("JavaScript Click On Element: " + locator, Status.PASSED);
    }

    public static void waitForElementToDisappear(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Waiting for Element To Disappear: "+locator+Colors.RESET);
        Allure.step("Waiting For Element To Disappear: " + locator, Status.PASSED);
    }
    public static WebElement waitForElementToBeClickable(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator));
        logsUtils.info(Colors.BLUE + "Wait For Element To Be Clickable: " + locator + Colors.RESET);
        Allure.step("Waiting for Element To Be Clickable: " + locator, Status.PASSED);
        return findWebElement(driver,locator);
    }
    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE + "Wait For Element To Be Visible: " + locator + Colors.RESET);
        Allure.step("Waiting for Element To Be Visible: " + locator, Status.PASSED);
        return findWebElement(driver, locator);
    }

    public static WebElement waitForElementPresence(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        logsUtils.info(Colors.BLUE + "Wait For Element Presence: " + locator + Colors.RESET);
        Allure.step("Waiting for Element Presence: " + locator, Status.PASSED);
        return findWebElement(driver,locator);
    }
    public static WebElement waitForTextToBePresentInElement(WebDriver driver, By locator,String text, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementLocated(locator,text));
        logsUtils.info(Colors.BLUE + "Wait For Element Presence: " + locator + Colors.RESET);
        Allure.step("Waiting for Element Presence: " + locator, Status.PASSED);
        return findWebElement(driver,locator);
    }
    public static String getAttributeValue(WebDriver driver, By locator, String attribute, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Getting Attribute: "+attribute+" From Element: "+locator+Colors.RESET);
        String attributeValue = findWebElement(driver,locator).getAttribute(attribute);
        Allure.step("Getting Attribute: " + attribute + " From Element: " + locator, Status.PASSED);
        return attributeValue;
    }
    public static boolean waitForElementToBeSelected(WebDriver driver, By locator, int timeout, int pollingEvery) {
        boolean isSelected = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeSelected(locator));
        logsUtils.info(Colors.BLUE + "Wait for element to be selected: " + locator + Colors.RESET);
        Allure.step("Waiting for element to be selected: " + locator, Status.PASSED);
        return isSelected;
    }
    public static boolean waitForElementAttributeToBe(WebDriver driver, By locator, String attribute, String value, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.attributeToBe(locator, attribute, value));
        logsUtils.info(Colors.BLUE + "Wait for element attribute to be: " + attribute + " = " + value + " for " + locator + Colors.RESET);
        Allure.step("Waiting for element attribute to be: " + attribute + " = " + value + " for " + locator, Status.PASSED);
        return result;
    }
    public static boolean waitForElementAttributeContains(WebDriver driver, By locator, String attribute, String value, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.attributeContains(locator, attribute, value));
        logsUtils.info(Colors.BLUE + "Wait for element attribute to contain: " + attribute + " = " + value + " for " + locator + Colors.RESET);
        Allure.step("Waiting for element attribute to contain: " + attribute + " = " + value + " for " + locator, Status.PASSED);
        return result;
    }
    public static boolean waitForElementStaleness(WebDriver driver, WebElement element, int timeout, int pollingEvery) {
        boolean isStale = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.stalenessOf(element));
        logsUtils.info(Colors.BLUE + "Wait for element staleness: " + element + Colors.RESET);
        Allure.step("Waiting for element staleness: " + element, Status.PASSED);
        return isStale;
    }
    public static boolean waitForTitleContains(WebDriver driver, String titlePart, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.titleContains(titlePart));
        logsUtils.info(Colors.BLUE + "Wait for title to contain: " + titlePart + Colors.RESET);
        Allure.step("Waiting for title to contain: " + titlePart, Status.PASSED);
        return result;
    }
    public static boolean waitForUrlContains(WebDriver driver, String urlPart, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.urlContains(urlPart));
        logsUtils.info(Colors.BLUE + "Wait for URL to contain: " + urlPart + Colors.RESET);
        Allure.step("Waiting for URL to contain: " + urlPart, Status.PASSED);
        return result;
    }
    public static WebDriver waitForFrameToBeAvailableAndSwitchToIt(WebDriver driver, By locator, int timeout, int pollingEvery) {
        WebDriver frame = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
        logsUtils.info(Colors.BLUE + "Wait for frame to be available and switch to it: " + locator + Colors.RESET);
        Allure.step("Waiting for frame to be available and switch to it: " + locator, Status.PASSED);
        return frame;
    }
    public static WebDriver waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(WebDriver driver, String nameOrId, int timeout, int pollingEvery) {
        WebDriver frame = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
        logsUtils.info(Colors.BLUE + "Wait for frame to be available by name or id: " + nameOrId + Colors.RESET);
        Allure.step("Waiting for frame to be available by name or id: " + nameOrId, Status.PASSED);
        return frame;
    }
    public static WebDriver waitForFrameByIndexToBeAvailableAndSwitchToIt(WebDriver driver, int index, int timeout, int pollingEvery) {
        WebDriver frame = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
        logsUtils.info(Colors.BLUE + "Wait for frame to be available by index: " + index + Colors.RESET);
        Allure.step("Waiting for frame to be available by index: " + index, Status.PASSED);
        return frame;
    }
    public static boolean waitForElementToBeEnabled(WebDriver driver, By locator, int timeout, int pollingEvery) {
        boolean isEnabled = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locator)).isEnabled();
        logsUtils.info(Colors.BLUE + "Wait for element to be enabled: " + locator + Colors.RESET);
        Allure.step("Waiting for element to be enabled: " + locator, Status.PASSED);
        return isEnabled;
    }
    public static boolean waitForTitleIs(WebDriver driver, String title, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.titleIs(title));
        logsUtils.info(Colors.BLUE + "Wait for title to be: " + title + Colors.RESET);
        Allure.step("Waiting for title to be: " + title, Status.PASSED);
        return result;
    }
    public static boolean waitForUrlToBe(WebDriver driver, String url, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.urlToBe(url));
        logsUtils.info(Colors.BLUE + "Wait for URL to be: " + url + Colors.RESET);
        Allure.step("Waiting for URL to be: " + url, Status.PASSED);
        return result;
    }
    public static boolean waitForElementSelectionStateToBe(WebDriver driver, By locator, boolean selected, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.elementSelectionStateToBe(locator, selected));
        logsUtils.info(Colors.BLUE + "Wait for element selection state to be: " + selected + " for " + locator + Colors.RESET);
        Allure.step("Waiting for element selection state to be: " + selected + " for " + locator, Status.PASSED);
        return result;
    }
    public static boolean waitForTextToBePresentInElementValue(WebDriver driver, By locator, String text, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.textToBePresentInElementValue(locator, text));
        logsUtils.info(Colors.BLUE + "Wait for text to be present in element value: " + text + " for " + locator + Colors.RESET);
        Allure.step("Waiting for text to be present in element value: " + text + " for " + locator, Status.PASSED);
        return result;
    }
    public static boolean waitForNumberOfWindowsToBe(WebDriver driver, int numberOfWindows, int timeout, int pollingEvery) {
        boolean result = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.numberOfWindowsToBe(numberOfWindows));
        logsUtils.info(Colors.BLUE + "Wait for number of windows to be: " + numberOfWindows + Colors.RESET);
        Allure.step("Waiting for number of windows to be: " + numberOfWindows, Status.PASSED);
        return result;
    }
    public static boolean waitForNumberOfElementsToBeMoreThan(WebDriver driver, By locator, int number, int timeout, int pollingEvery) {
        int size = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(locator, number)).size();
        logsUtils.info(Colors.BLUE + "Wait for number of elements to be more than: " + number + " for " + locator + Colors.RESET);
        Allure.step("Waiting for number of elements to be more than: " + number + " for " + locator, Status.PASSED);
        return size>number;
    }
    public static boolean waitForNumberOfElementsToBeLessThan(WebDriver driver, By locator, int number, int timeout, int pollingEvery) {
        int size = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBeLessThan(locator, number)).size();
        logsUtils.info(Colors.BLUE + "Wait for number of elements to be less than: " + number + " for " + locator + Colors.RESET);
        Allure.step("Waiting for number of elements to be less than: " + number + " for " + locator, Status.PASSED);
        return size<number;
    }
    public static List<WebElement> waitForVisibilityOfAllElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
        List<WebElement> elements = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        logsUtils.info(Colors.BLUE + "Wait for visibility of all elements: " + locator + Colors.RESET);
        Allure.step("Waiting for visibility of all elements: " + locator, Status.PASSED);
        return elements;
    }
    public static boolean waitForNumberOfElementsToBe(WebDriver driver, By locator, int number, int timeout, int pollingEvery) {
        int size = getFluentWait(driver, timeout, pollingEvery)
                .until(ExpectedConditions.numberOfElementsToBe(locator, number)).size();
        logsUtils.info(Colors.BLUE + "Wait for number of elements to be: " + number + " for " + locator + Colors.RESET);
        Allure.step("Waiting for number of elements to be: " + number + " for " + locator, Status.PASSED);
        return size==number;
    }

    public static void switchToNewWindow(WebDriver driver, String windowTitle) {
        String originalWindow = driver.getWindowHandle();
        for (String windowHandle : driver.getWindowHandles()) {
            driver.switchTo().window(windowHandle);
            if (driver.getTitle().equals(windowTitle)) {
                logsUtils.info(Colors.BLUE + "Switched To New Window: " + windowTitle + Colors.RESET);
                Allure.step("Switched to new window with title: " + windowTitle, Status.PASSED);
                return;
            }
        }
        driver.switchTo().window(originalWindow);
        logsUtils.info(Colors.RED + "Window with title " + windowTitle + " not found. Switched back to original window." + Colors.RESET);
        Allure.step("Failed to find window with title: " + windowTitle, Status.FAILED);
    }
    // Close the current window or tab
    public static void closeCurrentWindow(WebDriver driver) {
        driver.close();
        logsUtils.info(Colors.BLUE + "Closed Current Window" + Colors.RESET);
        Allure.step("Closing current window", Status.PASSED);
    }

    // Switch to the original window
    public static void switchToOriginalWindow(WebDriver driver, String originalWindowHandle) {
        driver.switchTo().window(originalWindowHandle);
        logsUtils.info(Colors.BLUE + "Switched To Original Window" + Colors.RESET);
        Allure.step("Switched to original window", Status.PASSED);
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
        logsUtils.info(Colors.BLUE + "Alert present. Accepting the alert." + Colors.RESET);
        Allure.step("Accepting the alert", Status.PASSED);
        driver.switchTo().alert().accept();
    }
    public static void dismissAlert(WebDriver driver, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        logsUtils.info(Colors.BLUE + "Alert present. Dismissing the alert." + Colors.RESET);
        Allure.step("Dismissing the alert", Status.PASSED);
        driver.switchTo().alert().dismiss();
    }
    public static String getAlertText(WebDriver driver, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        logsUtils.info(Colors.BLUE + "Getting alert text." + Colors.RESET);
        Allure.step("Getting alert text", Status.PASSED);
        String alertText = driver.switchTo().alert().getText();
        return alertText;
    }
    public static void sendDataToAlert(WebDriver driver, String data, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.alertIsPresent());
        logsUtils.info(Colors.BLUE + "Sending data to alert: " + data + Colors.RESET);
        Allure.step("Sending data to alert: " + data, Status.PASSED);
        driver.switchTo().alert().sendKeys(data);
    }
    // Get text from multiple elements
    public static List<String> getTextFromMultipleElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery).until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        logsUtils.info(Colors.BLUE + "Getting text from multiple elements located: " + locator + Colors.RESET);
        Allure.step("Getting text from multiple elements located: " + locator, Status.PASSED);
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
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        logsUtils.info(Colors.BLUE + "Getting Attribute from multiple elements located: " + locator + Colors.RESET);
        Allure.step("Getting Attribute from multiple elements located: " + locator, Status.PASSED);
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
        logsUtils.info(Colors.BLUE + "Clicking on multiple elements located: " + locator + Colors.RESET);
        Allure.step("Clicking on multiple elements located: " + locator, Status.PASSED);
        List<WebElement> elements = findWebElements(driver,locator);
        for (WebElement element : elements) {
            element.click();
        }
    }
    // Send data to multiple elements
    public static void sendDataToMultipleElements(WebDriver driver, By locator, String data, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        logsUtils.info(Colors.BLUE + "Sending data to multiple elements located: " + locator + Colors.RESET);
        List<WebElement> elements = findWebElements(driver,locator);
        for (WebElement element : elements) {
            element.clear();
            element.sendKeys(data);
        }
        Allure.step("Sending Data \"" + data + "\" To Multiple Elements: " + locator, Status.PASSED);
    }
    // Select from dropdowns on multiple elements by visible text
    public static void selectDropdownByTextForMultipleElements(WebDriver driver, By locator, String option, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        logsUtils.info(Colors.BLUE + "Selecting dropdown option by text for multiple elements: " + option + Colors.RESET);
        List<WebElement> elements = findWebElements(driver,locator);
        for (WebElement element : elements) {
            new Select(element).selectByVisibleText(option);
        }
        Allure.step("Selecting Dropdown Option \"" + option + "\" For Multiple Elements: " + locator, Status.PASSED);
    }

    // Switch to frame by index
    public static void switchToFrameByIndex(WebDriver driver, int index, int timeout,int pollingTime) {
        getFluentWait(driver,timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));

        logsUtils.info(Colors.BLUE + "Switched to frame by index: " + index + Colors.RESET);
        Allure.step("Switched To Frame By Index: " + index, Status.PASSED);
    }
    // Hover over an element with specified timeout
    public static void hoverOverElement(WebDriver driver, By locator, int timeout) {
        WebElement element =getFluentWait(driver,timeout,defaultPollingTime)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));

        Actions action = new Actions(driver);
        action.moveToElement(element).perform();

        logsUtils.info(Colors.BLUE + "Hovered over element: " + locator + Colors.RESET);
        Allure.step("Hovered over element: " + locator, Status.PASSED);
    }

    // New method: Scroll to page bottom
    public static void scrollToPageBottom(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        logsUtils.info(Colors.BLUE + "Scrolled to page bottom" + Colors.RESET);
        Allure.step("Scrolled to page bottom", Status.PASSED);
    }

    // New method: JS Executor - Set value using JavaScript
    public static void setElementValueUsingJS(WebDriver driver, By locator, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement element = findWebElement(driver, locator);
        js.executeScript("arguments[0].value = arguments[1];", element, value);
        logsUtils.info(Colors.BLUE + "Set value using JavaScript: " + value + Colors.RESET);
        Allure.step("Set value using JavaScript: " + value + " on element: " + locator, Status.PASSED);
    }
    // Switch to frame by name or ID
    public static void switchToFrameByNameOrID(WebDriver driver, String nameOrID, int timeout,int pollingTime) {
        getFluentWait(driver,timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrID));
        logsUtils.info(Colors.BLUE + "Switched to frame by name or ID: " + nameOrID + Colors.RESET);
        Allure.step("Switched To Frame By Name/ID: " + nameOrID, Status.PASSED);
    }
    // Switch to frame by WebElement
    public static void switchToFrameByElement(WebDriver driver, By locator, int timeout,int pollingTime) {
        getFluentWait(driver,timeout,pollingTime)
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
        logsUtils.info(Colors.BLUE + "Switched to frame by element located: " + locator + Colors.RESET);
        Allure.step("Switched To Frame By Element Located: " + locator, Status.PASSED);
    }

    // Switch back to default content from frame
    public static void switchToDefaultContent(WebDriver driver) {
        driver.switchTo().defaultContent();
        logsUtils.info(Colors.BLUE + "Switched back to default content from frame" + Colors.RESET);
        Allure.step("Switched Back To Default Content From Frame", Status.PASSED);
    }

    // Handle popups or additional windows
    public static void switchToPopupWindow(WebDriver driver, String expectedPopupTitle, int timeout,int pollingTime) {
        String mainWindow = driver.getWindowHandle();
        getFluentWait(driver,timeout,pollingTime)
                .until(ExpectedConditions.numberOfWindowsToBe(2));
        for (String windowHandle : driver.getWindowHandles()) {
            if (!windowHandle.equals(mainWindow)) {
                driver.switchTo().window(windowHandle);
                if (driver.getTitle().equals(expectedPopupTitle)) {
                    logsUtils.info(Colors.BLUE + "Switched to popup window with title: " + expectedPopupTitle + Colors.RESET);
                    Allure.step("Switched To Popup Window With Title: " + expectedPopupTitle, Status.PASSED);
                    return;
                }
            }
        }
        logsUtils.error(Colors.RED + "Popup window with title " + expectedPopupTitle + " not found" + Colors.RESET);
        driver.switchTo().window(mainWindow);
        Allure.step("Failed To Switch To Popup Window With Title: " + expectedPopupTitle, Status.FAILED);
    }

    // Close popup window and switch back to main window
    public static void closePopupWindow(WebDriver driver) {
        driver.close();
        logsUtils.info(Colors.BLUE + "Popup window closed. Switching back to the main window." + Colors.RESET);
        Allure.step("Closing popup window and switching back to the main window", Status.PASSED);
        String mainWindow = driver.getWindowHandles().iterator().next();
        driver.switchTo().window(mainWindow);
    }

    // Maximize the browser window
    public static void maximizeWindow(WebDriver driver) {
        driver.manage().window().maximize();
        logsUtils.info(Colors.BLUE + "Browser window maximized" + Colors.RESET);
        Allure.step("Maximizing browser window", Status.PASSED);
    }

    // Minimize the browser window
    public static void minimizeWindow(WebDriver driver) {
        driver.manage().window().minimize();
        logsUtils.info(Colors.BLUE + "Browser window minimized" + Colors.RESET);
        Allure.step("Minimizing browser window", Status.PASSED);
    }
    // Get all elements matching the locator
    public static List<WebElement> getElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
       getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        logsUtils.info(Colors.BLUE + "Getting all elements located: " + locator + Colors.RESET);
        Allure.step("Getting all elements located by: " + locator, Status.PASSED);
        return findWebElements(driver,locator);
    }

    // Move slider to a target value
    public static float moveSliderTo(WebDriver driver, By sliderLocator, By rangeLocator, float targetValue) {
        WebElement range = findWebElement(driver,rangeLocator);
        WebElement slider =  findWebElement(driver,sliderLocator);
        float currentValue = Float.parseFloat(range.getText());
        Actions action = new Actions(driver);
        // Move slider to minimum (assumed to be 0)
        int timeout=0;
        while ((currentValue != 0) && (timeout<3000)) {
            action.sendKeys(slider, Keys.ARROW_LEFT).perform();
            currentValue = Float.parseFloat(range.getText());
            timeout++;
        }
        timeout=0;
        // Now, move slider to the target value
        while ( (currentValue != targetValue) && (timeout<3000) ) {
            action.sendKeys(slider, Keys.ARROW_RIGHT).perform();
            currentValue = Float.parseFloat(range.getText());
            timeout++;
        }
        logsUtils.info(Colors.BLUE + "Slider moved to: " + currentValue + Colors.RESET);
        Allure.step("Slider moved to target value: " + currentValue, Status.PASSED);
        return currentValue;
    }
    // Drag and drop from source to target element
    public static void dragAndDrop(WebDriver driver, By sourceLocator, By targetLocator) {
        WebElement source =  findWebElement(driver,sourceLocator);
        WebElement target =  findWebElement(driver,targetLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveToElement(target)
                .release()
                .perform();
        logsUtils.info(Colors.BLUE + "Drag and drop performed from " + sourceLocator + " to " + targetLocator + Colors.RESET);
        Allure.step("Drag and drop from " + sourceLocator + " to " + targetLocator, Status.PASSED);
    }

    // Drag and drop using offsets
    public static void dragAndDropByOffset(WebDriver driver, By sourceLocator, int xOffset, int yOffset) {
        WebElement source =  findWebElement(driver,sourceLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();

        logsUtils.info(Colors.BLUE + "Drag and drop performed with offset: X=" + xOffset + ", Y=" + yOffset + Colors.RESET);
        Allure.step("Drag and drop performed with offset: X=" + xOffset + ", Y=" + yOffset, Status.PASSED);
    }

    // Hover over an element and click another
    public static void hoverAndClick(WebDriver driver, By locatorToHover, By locatorToClick, int timeout, int pollingEvery) {
        WebElement elementToHover =getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locatorToHover));

        WebElement elementToClick =getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locatorToClick));

        Actions action = new Actions(driver);
        action.moveToElement(elementToHover).click(elementToClick).perform();

        logsUtils.info(Colors.BLUE + "Hovered over " + locatorToHover + " and clicked " + locatorToClick + Colors.RESET);
        Allure.step("Hovered over " + locatorToHover + " and clicked " + locatorToClick, Status.PASSED);
    }

    // Capture screenshot
    public static String captureScreenshot(WebDriver driver, String screenshotName) {
        try {
            TakesScreenshot camera = (TakesScreenshot) driver;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            File screenShotUser = new File("Test-Output" + File.separator + "ScreenShots" + File.separator + "Captured" + File.separator);

            if (!screenShotUser.exists()) {
                screenShotUser.mkdirs();
            }

            File screenShotFile = new File(screenShotUser.getPath() + screenshotName + "_" + TestDataGenerator.getTimeStamp()+ ".png");
            Files.move(screenshot, screenShotFile);

            logsUtils.info(Colors.BLUE + "Screenshot captured: " + screenShotFile.getPath() + Colors.RESET);
            Allure.step("Screenshot captured: " + screenShotFile.getPath(), Status.PASSED);
            return screenShotFile.getPath();
        } catch (Exception e) {
            logsUtils.logException(e);
            Allure.step("Failed to capture screenshot: " + e.getMessage(), Status.FAILED);
            return null;
        }
    }
    // Move slider by offset with timeout and pollingEvery
    public static void moveSliderByOffset(WebDriver driver, By sliderLocator, int xOffset, int yOffset, int timeout, int pollingEvery) {
        getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(sliderLocator));

        WebElement slider = findWebElement(driver,sliderLocator);
        Actions action = new Actions(driver);
        action.clickAndHold(slider)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();

        logsUtils.info(Colors.BLUE + "Slider moved by offset: X=" + xOffset + ", Y=" + yOffset + Colors.RESET);
        Allure.step("Slider moved by offset: X=" + xOffset + ", Y=" + yOffset, Status.PASSED);
    }

    // New method: Scroll by offset
    public static void scrollByOffset(WebDriver driver, int xOffset, int yOffset) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(arguments[0], arguments[1]);", xOffset, yOffset);

        logsUtils.info(Colors.BLUE + "Scrolled by offset: X=" + xOffset + ", Y=" + yOffset + Colors.RESET);
        Allure.step("Scrolled by offset: X=" + xOffset + ", Y=" + yOffset, Status.PASSED);
    }
    // Right-click on an element
    public static void rightClick(WebDriver driver, By locator, int timeout, int pollingEvery) {
        WebElement element =getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.contextClick(element).perform();
        logsUtils.info(Colors.BLUE + "Right-clicked on element: " + locator + Colors.RESET);
        Allure.step("Right-clicked on element: " + locator, Status.PASSED);
    }
    // Double-click on an element with timeout and pollingEvery
    public static void doubleClick(WebDriver driver, By locator, int timeout, int pollingEvery) {
        WebElement element =getFluentWait(driver,timeout,pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));

        Actions action = new Actions(driver);
        action.doubleClick(element).perform();

        logsUtils.info(Colors.BLUE + "Double-clicked on element: " + locator + Colors.RESET);
        Allure.step("Double-clicked on element: " + locator, Status.PASSED);
    }
    public static void navigateToUrl(WebDriver driver, String url) {
        driver.get(url);
        logsUtils.info(Colors.BLUE + "Navigated To URL: " + url + Colors.RESET);
        Allure.step("Navigating to URL: " + url, Status.PASSED);
    }
    // Refresh the current page
    public static void refreshPage(WebDriver driver) {
        driver.navigate().refresh();
        logsUtils.info(Colors.BLUE + "Page Refreshed" + Colors.RESET);
        Allure.step("Refreshing the current page", Status.PASSED);
    }
    public static void navigateBack(WebDriver driver) {
        driver.navigate().back();
        logsUtils.info(Colors.BLUE + "Navigated Back In Browser History" + Colors.RESET);
        Allure.step("Navigating back in browser history", Status.PASSED);
    }
    public static void navigateForward(WebDriver driver) {
        driver.navigate().forward();
        logsUtils.info(Colors.BLUE + "Navigated Forward In Browser History" + Colors.RESET);
        Allure.step("Navigating forward in browser history", Status.PASSED);
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
            String timeout = PropertyHelper.getDataFromProperties(configPath, "defaultElementWaitTimeout");
            defaultTimeout = parseProperty(timeout, 5, "defaultElementWaitTimeout");
        } catch (Exception e) {
            logsUtils.logException(e);
            defaultTimeout = 5;  // Assign default if exception occurs
        }
    }
    // Initialize default polling time from properties file
    private static void initPolling() {
        try {
            String polling = PropertyHelper.getDataFromProperties(configPath, "defaultElementPollingTime");
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