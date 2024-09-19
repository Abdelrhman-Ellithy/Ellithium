package Ellithium.Utilities;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

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
    }

    public static void sendData(WebDriver driver, By locator, Keys data, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        driver.findElement(locator).clear();
        logsUtils.info(Colors.BLUE+"Sending Data \""+ data+"\" To Element: "+locator+Colors.RESET);
        driver.findElement(locator).sendKeys(data);
    }

    public static String getText(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Get Text From Element: "+locator+Colors.RESET);
        return driver.findElement(locator).getText();
    }

    public static void clickingOnElement(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.elementToBeClickable(locator));
        driver.findElement(locator).click();
        logsUtils.info(Colors.BLUE+"Click On Element: "+locator+Colors.RESET);
    }

    public static void waitForInvisibility(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Waiting for invisibility Of Element Located: "+locator+Colors.RESET);
    }

    // General wait
    public static WebDriverWait generalWait(WebDriver driver, int timeout) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    // Scroll to an element
    public static void scrollToElement(WebDriver driver, By locator) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", findWebelement(driver, locator));
        logsUtils.info(Colors.BLUE+"Scrolling To Element Located: "+locator+Colors.RESET);
    }

    // Get a timestamp
    public static String getTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd-h-m-ssa").format(new Date());
    }

    // Select dropdown option by visible text
    public static void selectDropdownByText(WebDriver driver, By locator, String option, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Text: "+option+" From Element: "+locator+Colors.RESET);
        new Select(findWebelement(driver, locator)).selectByVisibleText(option);
    }

    // Select dropdown option by value
    public static void selectDropdownByValue(WebDriver driver, By locator, String value, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Value: "+value+" From Element: "+locator+Colors.RESET);
        new Select(findWebelement(driver, locator)).selectByValue(value);
    }

    // Select dropdown option by index
    public static void selectDropdownByIndex(WebDriver driver, By locator, int index, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Select Dropdown Option By Index: "+index+" From Element: "+locator+Colors.RESET);
        new Select(findWebelement(driver, locator)).selectByIndex(index);
    }

    // JavaScript click on an element
    public static void javascriptClick(WebDriver driver, By locator) {
        WebElement element = findWebelement(driver, locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        logsUtils.info(Colors.BLUE+"JavaScript Click On Element: "+locator+Colors.RESET);
    }

    // Wait for an element to disappear
    public static void waitForElementToDisappear(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Waiting for Element To Disappear: "+locator+Colors.RESET);
    }

    // Get attribute value from an element
    public static String getAttributeValue(WebDriver driver, By locator, String attribute, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Getting Attribute: "+attribute+" From Element: "+locator+Colors.RESET);
        return driver.findElement(locator).getAttribute(attribute);
    }
    // Sleep for a specified number of milliseconds
    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            logsUtils.error("Sleep interrupted: "+ e.getMessage());
        }
    }
    // Sleep for a specified number of seconds
    public static void sleepSeconds(long seconds) {
        sleepMillis(seconds * 1000);
    }

    // Sleep for a specified number of minutes
    public static void sleepMinutes(long minutes) {
        sleepMillis(minutes * 60 * 1000);
    }

    // Set implicit wait
    public static void setImplicitWait(WebDriver driver, int timeout) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
        logsUtils.info(Colors.BLUE+"Set Implicit Wait To: "+timeout+" seconds"+Colors.RESET);
    }

    // Explicit wait for element to be clickable
    public static WebElement waitForElementToBeClickable(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.elementToBeClickable(locator));
        logsUtils.info(Colors.BLUE+"Wait For Element To Be Clickable: "+locator+Colors.RESET);
        return driver.findElement(locator);
    }

    // Explicit wait for element to be visible
    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Wait For Element To Be Visible: "+locator+Colors.RESET);
        return driver.findElement(locator);
    }

    // Explicit wait for element to be present in the DOM
    public static WebElement waitForElementPresence(WebDriver driver, By locator, int timeout, int pollingEvery) {
        new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(pollingEvery))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        logsUtils.info(Colors.BLUE+"Wait For Element Presence: "+locator+Colors.RESET);
        return driver.findElement(locator);
    }

    // Navigate to a URL
    public static void navigateToUrl(WebDriver driver, String url) {
        driver.get(url);
        logsUtils.info(Colors.BLUE+"Navigated To URL: "+url+Colors.RESET);
    }

    // Refresh the current page
    public static void refreshPage(WebDriver driver) {
        driver.navigate().refresh();
        logsUtils.info(Colors.BLUE+"Page Refreshed"+Colors.RESET);
    }

    // Navigate back in the browser history
    public static void navigateBack(WebDriver driver) {
        driver.navigate().back();
        logsUtils.info(Colors.BLUE+"Navigated Back In Browser History"+Colors.RESET);
    }

    // Navigate forward in the browser history
    public static void navigateForward(WebDriver driver) {
        driver.navigate().forward();
        logsUtils.info(Colors.BLUE+"Navigated Forward In Browser History"+Colors.RESET);
    }

    // Switch to a new window or tab
    public static void switchToNewWindow(WebDriver driver, String windowTitle) {
        String originalWindow = driver.getWindowHandle();
        for (String windowHandle : driver.getWindowHandles()) {
            driver.switchTo().window(windowHandle);
            if (driver.getTitle().equals(windowTitle)) {
                logsUtils.info(Colors.BLUE+"Switched To New Window: "+windowTitle+Colors.RESET);
                return;
            }
        }
        driver.switchTo().window(originalWindow);
        logsUtils.info(Colors.RED+"Window with title "+windowTitle+" not found. Switched back to original window."+Colors.RESET);
    }

    // Close the current window or tab
    public static void closeCurrentWindow(WebDriver driver) {
        driver.close();
        logsUtils.info(Colors.BLUE+"Closed Current Window"+Colors.RESET);
    }

    // Switch to the original window
    public static void switchToOriginalWindow(WebDriver driver, String originalWindowHandle) {
        driver.switchTo().window(originalWindowHandle);
        logsUtils.info(Colors.BLUE+"Switched To Original Window"+Colors.RESET);
    }
    // Overloaded sendData method with pollingEvery 500ms
    public static void sendData(WebDriver driver, By locator, String data) {
        sendData(driver, locator, data, 5, 500); // Timeout 5 seconds, pollingEvery 500ms
    }

    public static void sendData(WebDriver driver, By locator, Keys data) {
        sendData(driver, locator, data, 5, 500); // Timeout 5 seconds, pollingEvery 500ms
    }

    // Overloaded getText method with default timeout and polling time
    public static String getText(WebDriver driver, By locator) {
        initializeTimeoutAndPolling();
        return getText(driver, locator, defaultTimeout, defaultPollingTime); // Timeout 5 seconds, pollingEvery 500ms
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

    // Initialize timeout and polling time only once
    private static void initializeTimeoutAndPolling() {
        if (!defaultTimeoutGotFlag) {
            initTimeout();
            defaultTimeoutGotFlag = true;
        }
        if (!defaultPollingTimeGotFlag) {
            initPolling();
            defaultPollingTimeGotFlag = true;
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