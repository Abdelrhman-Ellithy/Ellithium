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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DriverActions {
    private static int defaultTimeout= 5;
    private static int defaultPollingTime=500;
    private static boolean defaultTimeoutGotFlag=false;
    private static boolean defaultPollingTimeGotFlag=false;
    private static final String configPath=System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config.properties";
    public static void sendData(WebDriver driver, By locator, String data, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        driver.findElement(locator).clear();
        logsUtils.info(Colors.BLUE+"Sending Data \""+ data+"\" To Element: "+locator+Colors.RESET);
        driver.findElement(locator).sendKeys(data);
        Allure.step("Sending Data \""+ data+"\" To Element: "+locator, Status.PASSED);
    }

    public static void sendData(WebDriver driver, By locator, Keys data, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        driver.findElement(locator).clear();
        logsUtils.info(Colors.BLUE+"Sending Data \""+ data+"\" To Element: "+locator+Colors.RESET);
        driver.findElement(locator).sendKeys(data);
        Allure.step("Sending Data \""+ data+"\" To Element: "+locator, Status.PASSED);
    }

    public static String getText(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Get Text From Element: "+locator+Colors.RESET);
        String text = driver.findElement(locator).getText();
        Allure.step("Getting Text From Element: "+locator, Status.PASSED);
        return text;
    }

    public static void clickingOnElement(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.elementToBeClickable(locator));
        driver.findElement(locator).click();
        logsUtils.info(Colors.BLUE+"Click On Element: "+locator+Colors.RESET);
        Allure.step("Clicking On Element: "+locator, Status.PASSED);
    }

    public static void waitForInvisibility(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Waiting for Invisibility Of Element Located: "+locator+Colors.RESET);
        Allure.step("Waiting For Invisibility Of Element Located: "+locator, Status.PASSED);
    }

    public static WebDriverWait generalWait(WebDriver driver, int timeout) {
        Allure.step("General Wait For " + timeout + " seconds", Status.PASSED);
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    public static void scrollToElement(WebDriver driver, By locator) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", findWebelement(driver, locator));
        logsUtils.info(Colors.BLUE+"Scrolling To Element Located: "+locator+Colors.RESET);
        Allure.step("Scrolling To Element: "+locator, Status.PASSED);
    }

    public static String getTimeStamp() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-h-m-ssa").format(new Date());
        Allure.step("Getting Timestamp: " + timestamp, Status.PASSED);
        return timestamp;
    }

    public static void selectDropdownByText(WebDriver driver, By locator, String option, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Text: "+option+" From Element: "+locator+Colors.RESET);
        new Select(findWebelement(driver, locator)).selectByVisibleText(option);
        Allure.step("Selecting Dropdown Option By Text: " + option + " From Element: " + locator, Status.PASSED);
    }

    public static void selectDropdownByValue(WebDriver driver, By locator, String value, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Value: "+value+" From Element: "+locator+Colors.RESET);
        new Select(findWebelement(driver, locator)).selectByValue(value);
        Allure.step("Selecting Dropdown Option By Value: " + value + " From Element: " + locator, Status.PASSED);
    }

    public static void selectDropdownByIndex(WebDriver driver, By locator, int index, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Index: "+index+" From Element: "+locator+Colors.RESET);
        new Select(findWebelement(driver, locator)).selectByIndex(index);
        Allure.step("Selecting Dropdown Option By Index: " + index + " From Element: " + locator, Status.PASSED);
    }

    public static void javascriptClick(WebDriver driver, By locator) {
        WebElement element = findWebelement(driver, locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        logsUtils.info(Colors.BLUE+"JavaScript Click On Element: "+locator+Colors.RESET);
        Allure.step("JavaScript Click On Element: " + locator, Status.PASSED);
    }

    public static void waitForElementToDisappear(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Waiting for Element To Disappear: "+locator+Colors.RESET);
        Allure.step("Waiting For Element To Disappear: " + locator, Status.PASSED);
    }

    public static String getAttributeValue(WebDriver driver, By locator, String attribute, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Getting Attribute: "+attribute+" From Element: "+locator+Colors.RESET);
        String attributeValue = driver.findElement(locator).getAttribute(attribute);
        Allure.step("Getting Attribute: " + attribute + " From Element: " + locator, Status.PASSED);
        return attributeValue;
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

    public static void setImplicitWait(WebDriver driver, int timeout) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
        logsUtils.info(Colors.BLUE + "Set Implicit Wait To: " + timeout + " seconds" + Colors.RESET);
        Allure.step("Setting Implicit Wait to " + timeout + " seconds", Status.PASSED);
    }

    public static WebElement waitForElementToBeClickable(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.elementToBeClickable(locator));
        logsUtils.info(Colors.BLUE + "Wait For Element To Be Clickable: " + locator + Colors.RESET);
        Allure.step("Waiting for Element To Be Clickable: " + locator, Status.PASSED);
        return driver.findElement(locator);
    }

    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE + "Wait For Element To Be Visible: " + locator + Colors.RESET);
        Allure.step("Waiting for Element To Be Visible: " + locator, Status.PASSED);
        return driver.findElement(locator);
    }

    public static WebElement waitForElementPresence(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        logsUtils.info(Colors.BLUE + "Wait For Element Presence: " + locator + Colors.RESET);
        Allure.step("Waiting for Element Presence: " + locator, Status.PASSED);
        return driver.findElement(locator);
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

    // Overloaded clickingOnElement method with default timeout and polling time
    public static void clickingOnElement(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        clickingOnElement(driver, locator, defaultTimeout, defaultPollingTime);
    }

    // Overloaded waitForInvisibility method with default timeout and polling time
    public static void waitForInvisibility(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        waitForInvisibility(driver, locator, defaultTimeout, defaultPollingTime);
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

    // Find an element (no need for timeout or polling)
    public static WebElement findWebelement(WebDriver driver, By locator) {
        return driver.findElement(locator);
    }

    // Accept an alert
    public static void acceptAlert(WebDriver driver, int timeout) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.alertIsPresent());
        logsUtils.info(Colors.BLUE + "Alert present. Accepting the alert." + Colors.RESET);
        Allure.step("Accepting the alert", Status.PASSED);
        driver.switchTo().alert().accept();
    }

    public static void dismissAlert(WebDriver driver, int timeout) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.alertIsPresent());
        logsUtils.info(Colors.BLUE + "Alert present. Dismissing the alert." + Colors.RESET);
        Allure.step("Dismissing the alert", Status.PASSED);
        driver.switchTo().alert().dismiss();
    }

    public static String getAlertText(WebDriver driver, int timeout) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.alertIsPresent());
        logsUtils.info(Colors.BLUE + "Getting alert text." + Colors.RESET);
        Allure.step("Getting alert text", Status.PASSED);
        String alertText = driver.switchTo().alert().getText();
        return alertText;
    }

    public static void sendDataToAlert(WebDriver driver, String data, int timeout) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.alertIsPresent());
        logsUtils.info(Colors.BLUE + "Sending data to alert: " + data + Colors.RESET);
        Allure.step("Sending data to alert: " + data, Status.PASSED);
        driver.switchTo().alert().sendKeys(data);
    }

    // Get text from multiple elements
    public static List<String> getTextFromMultipleElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        logsUtils.info(Colors.BLUE + "Getting text from multiple elements located: " + locator + Colors.RESET);
        Allure.step("Getting text from multiple elements located: " + locator, Status.PASSED);
        List<WebElement> elements = driver.findElements(locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }

    // Click on multiple elements
    public static void clickOnMultipleElements(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        logsUtils.info(Colors.BLUE + "Clicking on multiple elements located: " + locator + Colors.RESET);
        Allure.step("Clicking on multiple elements located: " + locator, Status.PASSED);
        List<WebElement> elements = driver.findElements(locator);
        for (WebElement element : elements) {
            element.click();
        }
    }

    // Send data to multiple elements
    public static void sendDataToMultipleElements(WebDriver driver, By locator, String data, int timeout, int pollingEvery) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        logsUtils.info(Colors.BLUE + "Sending data to multiple elements located: " + locator + Colors.RESET);
        List<WebElement> elements = driver.findElements(locator);
        for (WebElement element : elements) {
            element.clear();
            element.sendKeys(data);
        }
        Allure.step("Sending Data \"" + data + "\" To Multiple Elements: " + locator, Status.PASSED);
    }

    // Select from dropdowns on multiple elements by visible text
    public static void selectDropdownByTextForMultipleElements(WebDriver driver, By locator, String option, int timeout, int pollingEvery) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        logsUtils.info(Colors.BLUE + "Selecting dropdown option by text for multiple elements: " + option + Colors.RESET);
        List<WebElement> elements = driver.findElements(locator);
        for (WebElement element : elements) {
            new Select(element).selectByVisibleText(option);
        }
        Allure.step("Selecting Dropdown Option \"" + option + "\" For Multiple Elements: " + locator, Status.PASSED);
    }

    // Switch to frame by index
    public static void switchToFrameByIndex(WebDriver driver, int index, int timeout) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));

        logsUtils.info(Colors.BLUE + "Switched to frame by index: " + index + Colors.RESET);
        Allure.step("Switched To Frame By Index: " + index, Status.PASSED);
    }

    // Switch to frame by name or ID
    public static void switchToFrameByNameOrID(WebDriver driver, String nameOrID, int timeout) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrID));

        logsUtils.info(Colors.BLUE + "Switched to frame by name or ID: " + nameOrID + Colors.RESET);
        Allure.step("Switched To Frame By Name/ID: " + nameOrID, Status.PASSED);
    }

    // Switch to frame by WebElement
    public static void switchToFrameByElement(WebDriver driver, By locator, int timeout) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
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
    public static void switchToPopupWindow(WebDriver driver, String expectedPopupTitle, int timeout) {
        String mainWindow = driver.getWindowHandle();
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
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
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        logsUtils.info(Colors.BLUE + "Getting all elements located: " + locator + Colors.RESET);
        Allure.step("Getting all elements located by: " + locator, Status.PASSED);
        return driver.findElements(locator);
    }

    // Move slider to a target value
    public static float moveSliderTo(WebDriver driver, By sliderLocator, By rangeLocator, float targetValue, int timeout) {
        WebElement range = driver.findElement(rangeLocator);
        WebElement slider = driver.findElement(sliderLocator);
        float currentValue = Float.parseFloat(range.getText());
        Actions action = new Actions(driver);

        // Move slider to minimum (assumed to be 0)
        while (currentValue != 0 && timeout > 0) {
            action.sendKeys(slider, Keys.ARROW_LEFT).perform();
            currentValue = Float.parseFloat(range.getText());
            timeout--;
        }
        // Now, move slider to the target value
        while (currentValue != targetValue && timeout > 0) {
            action.sendKeys(slider, Keys.ARROW_RIGHT).perform();
            currentValue = Float.parseFloat(range.getText());
            timeout--;
        }

        logsUtils.info(Colors.BLUE + "Slider moved to: " + currentValue + Colors.RESET);
        Allure.step("Slider moved to target value: " + currentValue, Status.PASSED);
        return currentValue;
    }

    // Drag and drop from source to target element
    public static void dragAndDrop(WebDriver driver, By sourceLocator, By targetLocator) {
        WebElement source = driver.findElement(sourceLocator);
        WebElement target = driver.findElement(targetLocator);
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
        WebElement source = driver.findElement(sourceLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();

        logsUtils.info(Colors.BLUE + "Drag and drop performed with offset: X=" + xOffset + ", Y=" + yOffset + Colors.RESET);
        Allure.step("Drag and drop performed with offset: X=" + xOffset + ", Y=" + yOffset, Status.PASSED);
    }

    // Hover over an element and click another
    public static void hoverAndClick(WebDriver driver, By locatorToHover, By locatorToClick, int timeout) {
        WebElement elementToHover = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.visibilityOfElementLocated(locatorToHover));

        WebElement elementToClick = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
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

            File screenShotFile = new File(screenShotUser.getPath() + screenshotName + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".png");
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
    // Right-click on an element
    public static void rightClick(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        WebElement element = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));

        Actions action = new Actions(driver);
        action.contextClick(element).perform();

        logsUtils.info(Colors.BLUE + "Right-clicked on element: " + locator + Colors.RESET);
        Allure.step("Right-clicked on element: " + locator, Status.PASSED);
    }

    // Double-click on an element
    public static void doubleClick(WebDriver driver, By locator, int timeout) {
        initializeTimeoutAndPolling();
        WebElement element = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));

        Actions action = new Actions(driver);
        action.doubleClick(element).perform();

        logsUtils.info(Colors.BLUE + "Double-clicked on element: " + locator + Colors.RESET);
        Allure.step("Double-clicked on element: " + locator, Status.PASSED);
    }

    // Overload example for moveSliderByOffset with default timeout
    public static void moveSliderByOffset(WebDriver driver, By sliderLocator, int xOffset, int yOffset) {
        initializeTimeoutAndPolling();
        moveSliderByOffset(driver, sliderLocator, xOffset, yOffset, defaultTimeout, defaultPollingTime);
    }

    // Move slider by offset with timeout and pollingEvery
    public static void moveSliderByOffset(WebDriver driver, By sliderLocator, int xOffset, int yOffset, int timeout, int pollingEvery) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(sliderLocator));

        WebElement slider = driver.findElement(sliderLocator);
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

    // Overload example for hoverOverElement with default timeout
    public static void hoverOverElement(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        hoverOverElement(driver, locator, defaultTimeout);
    }

    // Hover over an element with specified timeout
    public static void hoverOverElement(WebDriver driver, By locator, int timeout) {
        WebElement element = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(defaultPollingTime))
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
        WebElement element = findWebelement(driver, locator);
        js.executeScript("arguments[0].value = arguments[1];", element, value);

        logsUtils.info(Colors.BLUE + "Set value using JavaScript: " + value + Colors.RESET);
        Allure.step("Set value using JavaScript: " + value + " on element: " + locator, Status.PASSED);
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
    // Initialize default timeout from properties file
    private static void initTimeout() {
        try {
            String timeout = PropertyHelper.getDataFromProperties(configPath, "defaultElementWaitTimeout");
            defaultTimeout = Integer.parseInt(timeout);
        } catch (Exception e) {
            logsUtils.logException(e);
        }
    }
    // Initialize default polling time from properties file
    private static void initPolling() {
        try {
            String polling = PropertyHelper.getDataFromProperties(configPath, "defaultElementPollingTime");
            defaultPollingTime = Integer.parseInt(polling);
        } catch (Exception e) {
            logsUtils.logException(e);
        }
    }
}