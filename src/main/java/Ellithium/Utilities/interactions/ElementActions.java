package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.File;
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
     * Gets the value of a property from multiple elements.
     * @param locator Element locator
     * @param property Property name
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of attribute values from the elements
     */
    public List<String> getPropertyFromMultipleElements(By locator, String property, int timeout, int pollingEvery) {
        Reporter.log("Getting Property from multiple elements located: ", LogLevel.INFO_BLUE, locator.toString());
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        List<WebElement> elements = findWebElements(locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getDomProperty(property));
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
     * Retrieves the value of a Property from an element.
     * @param locator Element locator
     * @param property Property name
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return The attribute value
     */
    public String getPropertyValue(By locator, String property, int timeout, int pollingEvery) {
        Reporter.log("Getting Property: '" + property + "' from Element: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        return findWebElement(locator).getDomProperty(property);
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
     * Gets the value of a property from multiple elements with default timeout and polling time.
     * @param locator Element locator
     * @param property Attribute name
     * @return List of attribute values from the elements
     */
    public List<String> getPropertyFromMultipleElements(By locator, String property) {
        return getPropertyFromMultipleElements(locator, property, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Gets the value of a property from multiple elements with specified timeout.
     * @param locator Element locator
     * @param property Attribute name
     * @param timeout Maximum wait time in seconds
     * @return List of attribute values from the elements
     */
    public List<String> getPropertyFromMultipleElements(By locator, String property, int timeout) {
        return getPropertyFromMultipleElements(locator, property, timeout, WaitManager.getDefaultPollingTime());
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
     * Retrieves the value of a property from an element with default timeout and polling time.
     * @param locator Element locator
     * @param property Attribute name
     * @return The attribute value
     */
    public String getPropertyValue(By locator, String property) {
        return getPropertyValue(locator, property, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
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

    /**
     * Uploads a file using sendKeys method.
     * Works with input elements of type 'file'.
     * @param fileUploadLocator Locator for the file input element
     * @param filePath Absolute path to the file to upload
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @throws IllegalArgumentException if file does not exist
     */
    public void uploadFile(By fileUploadLocator, String filePath, int timeout, int pollingEvery) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + filePath);
            }
            
            WebElement uploadElement = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.presenceOfElementLocated(fileUploadLocator));
            
            uploadElement.sendKeys(file.getAbsolutePath());
            Reporter.log("File uploaded successfully: " + file.getName(), LogLevel.INFO_BLUE);
        } catch (Exception e) {
            Reporter.log("Failed to upload file: " + e.getMessage(), LogLevel.ERROR);
            throw e;
        }
    }

    /**
     * Uploads multiple files simultaneously.
     * Works with input elements that support multiple file uploads.
     * @param fileUploadLocator Locator for the file input element
     * @param filePaths Array of absolute file paths to upload
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @throws IllegalArgumentException if any file does not exist
     */
    public void uploadMultipleFiles(By fileUploadLocator, String[] filePaths, int timeout, int pollingEvery) {
        try {
            String pathsString = buildFilePathsString(filePaths);
            
            WebElement uploadElement = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.presenceOfElementLocated(fileUploadLocator));
            
            uploadElement.sendKeys(pathsString);
            Reporter.log("Multiple files uploaded successfully", LogLevel.INFO_BLUE);
        } catch (Exception e) {
            Reporter.log("Failed to upload multiple files: " + e.getMessage(), LogLevel.ERROR);
            throw e;
        }
    }
    
    private String buildFilePathsString(String[] filePaths) {
        StringBuilder paths = new StringBuilder();
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + filePath);
            }
            paths.append(file.getAbsolutePath()).append(System.lineSeparator());
        }
        return paths.toString().trim();
    }

    
    /**
     * Uploads a file using default timeout and polling time.
     * @param fileUploadLocator Locator for the file input element
     * @param filePath Absolute path to the file to upload
     */
    public void uploadFile(By fileUploadLocator, String filePath) {
        uploadFile(fileUploadLocator, filePath, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Uploads multiple files with default timeout and polling time.
     * @param fileUploadLocator Locator for the file input element
     * @param filePaths Array of absolute file paths to upload
     */
    public void uploadMultipleFiles(By fileUploadLocator, String[] filePaths) {
        uploadMultipleFiles(fileUploadLocator, filePaths, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    /**
     * Checks if an element is present in the DOM within the specified time.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return true if the element is present, false otherwise
     */
    public boolean isElementPresent(By locator, int timeout, int pollingEvery) {
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
            Reporter.log("Element is present: " + locator, LogLevel.INFO_BLUE);
            return true;
        } catch (TimeoutException e) {
            Reporter.log("Element not present within timeout: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Failed to check element presence: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if an element is present in the DOM with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return true if the element is present, false otherwise
     */
    public boolean isElementPresent(By locator, int timeout) {
        return isElementPresent(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is present in the DOM with default timeout and polling time.
     * @param locator Element locator
     * @return true if the element is present, false otherwise
     */
    public boolean isElementPresent(By locator) {
        return isElementPresent(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is displayed within the specified time.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return true if the element is displayed, false otherwise
     */
    public boolean isElementDisplayed(By locator, int timeout, int pollingEvery) {
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            Reporter.log("Element is displayed: " + locator, LogLevel.INFO_BLUE);
            return true;
        } catch (TimeoutException e) {
            Reporter.log("Element not displayed within timeout: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Failed to check element visibility: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if an element is displayed with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return true if the element is displayed, false otherwise
     */
    public boolean isElementDisplayed(By locator, int timeout) {
        return isElementDisplayed(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is displayed with default timeout and polling time.
     * @param locator Element locator
     * @return true if the element is displayed, false otherwise
     */
    public boolean isElementDisplayed(By locator) {
        return isElementDisplayed(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is enabled within the specified time.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return true if the element is enabled, false otherwise
     */
    public boolean isElementEnabled(By locator, int timeout, int pollingEvery) {
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            boolean enabled = findWebElement(locator).isEnabled();
            if (enabled) {
                Reporter.log("Element is enabled: " + locator, LogLevel.INFO_BLUE);
            } else {
                Reporter.log("Element is disabled: " + locator, LogLevel.WARN);
            }
            return enabled;
        } catch (TimeoutException e) {
            Reporter.log("Element not found or not visible within timeout (enabled check): " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Failed to check if element is enabled: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if an element is enabled with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return true if the element is enabled, false otherwise
     */
    public boolean isElementEnabled(By locator, int timeout) {
        return isElementEnabled(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is enabled with default timeout and polling time.
     * @param locator Element locator
     * @return true if the element is enabled, false otherwise
     */
    public boolean isElementEnabled(By locator) {
        return isElementEnabled(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is selected within the specified time.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return true if the element is selected, false otherwise
     */
    public boolean isElementSelected(By locator, int timeout, int pollingEvery) {
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            boolean selected = findWebElement(locator).isSelected();
            if (selected) {
                Reporter.log("Element is selected: " + locator, LogLevel.INFO_BLUE);
            } else {
                Reporter.log("Element is not selected: " + locator, LogLevel.WARN);
            }
            return selected;
        } catch (TimeoutException e) {
            Reporter.log("Element not found or not visible within timeout (selected check): " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Failed to check if element is selected: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if an element is selected with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return true if the element is selected, false otherwise
     */
    public boolean isElementSelected(By locator, int timeout) {
        return isElementSelected(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is selected with default timeout and polling time.
     * @param locator Element locator
     * @return true if the element is selected, false otherwise
     */
    public boolean isElementSelected(By locator) {
        return isElementSelected(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is clickable within the specified time.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return true if the element is clickable, false otherwise
     */
    public boolean isElementClickable(By locator, int timeout, int pollingEvery) {
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.elementToBeClickable(locator));
            Reporter.log("Element is clickable: " + locator, LogLevel.INFO_BLUE);
            return true;
        } catch (TimeoutException e) {
            Reporter.log("Element not clickable within timeout: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Failed to check if element is clickable: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if an element is clickable with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @return true if the element is clickable, false otherwise
     */
    public boolean isElementClickable(By locator, int timeout) {
        return isElementClickable(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if an element is clickable with default timeout and polling time.
     * @param locator Element locator
     * @return true if the element is clickable, false otherwise
     */
    public boolean isElementClickable(By locator) {
        return isElementClickable(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    /**
     * Clears the content of an input element after waiting for it to be visible.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void clearElement(By locator, int timeout, int pollingEvery) {
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            findWebElement(locator).clear();
            Reporter.log("Element cleared: " + locator, LogLevel.INFO_BLUE);
        } catch (TimeoutException e) {
            Reporter.log("Clear failed – element not visible within timeout: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            throw e;
        } catch (Exception e) {
            Reporter.log("Clear failed: " + e.getMessage(), LogLevel.ERROR);
            throw e;
        }
    }

    /**
     * Clears the content of an input element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public void clearElement(By locator, int timeout) {
        clearElement(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Clears the content of an input element with default timeout and polling time.
     * @param locator Element locator
     */
    public void clearElement(By locator) {
        clearElement(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Scrolls the element into view using JavaScript.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void scrollIntoView(By locator, int timeout, int pollingEvery) {
        try {
            WebElement element = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            Reporter.log("Scrolled into view: " + locator, LogLevel.INFO_BLUE);
        } catch (TimeoutException e) {
            Reporter.log("Scroll failed – element not present within timeout: " + locator + " | " + e.getMessage(), LogLevel.ERROR);
            throw e;
        } catch (Exception e) {
            Reporter.log("Scroll failed: " + e.getMessage(), LogLevel.ERROR);
            throw e;
        }
    }

    /**
     * Scrolls the element into view with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public void scrollIntoView(By locator, int timeout) {
        scrollIntoView(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Scrolls the element into view with default timeout and polling time.
     * @param locator Element locator
     */
    public void scrollIntoView(By locator) {
        scrollIntoView(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    /**
     * Checks if the element's text contains the specified substring.
     * @param locator Element locator
     * @param substring Substring to check
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return true if text contains substring, false otherwise
     */
    public boolean isTextContains(By locator, String substring, int timeout, int pollingEvery) {
        try {
            String actual = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator))
                    .getText();
            boolean contains = actual.contains(substring);
            Reporter.log((contains ? "Text contains" : "Text does NOT contain") + " '" + substring + "'",
                    contains ? LogLevel.INFO_BLUE : LogLevel.WARN);
            return contains;
        } catch (TimeoutException e) {
            Reporter.log("Text contains check timed out: " + locator, LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Text contains check failed: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if the element's text contains the specified substring with specified timeout.
     * @param locator Element locator
     * @param substring Substring to check
     * @param timeout Maximum wait time in seconds
     * @return true if text contains substring, false otherwise
     */
    public boolean isTextContains(By locator, String substring, int timeout) {
        return isTextContains(locator, substring, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if the element's text contains the specified substring with default timeout and polling time.
     * @param locator Element locator
     * @param substring Substring to check
     * @return true if text contains substring, false otherwise
     */
    public boolean isTextContains(By locator, String substring) {
        return isTextContains(locator, substring, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    /**
     * Checks if the element's attribute contains the specified substring.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param substring Substring to check
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return true if attribute contains substring, false otherwise
     */
    public boolean isAttributeContains(By locator, String attribute, String substring, int timeout, int pollingEvery) {
        try {
            String value = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator))
                    .getDomAttribute(attribute);
            boolean contains = value != null && value.contains(substring);
            Reporter.log((contains ? "Attribute '" + attribute + "' contains" : "Attribute '" + attribute + "' does NOT contain") + " '" + substring + "'",
                    contains ? LogLevel.INFO_BLUE : LogLevel.WARN);
            return contains;
        } catch (TimeoutException e) {
            Reporter.log("Attribute contains check timed out: " + locator, LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Attribute contains check failed: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if the element's attribute contains the specified substring with specified timeout.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param substring Substring to check
     * @param timeout Maximum wait time in seconds
     * @return true if attribute contains substring, false otherwise
     */
    public boolean isAttributeContains(By locator, String attribute, String substring, int timeout) {
        return isAttributeContains(locator, attribute, substring, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if the element's attribute contains the specified substring with default timeout and polling time.
     * @param locator Element locator
     * @param attribute Attribute name
     * @param substring Substring to check
     * @return true if attribute contains substring, false otherwise
     */
    public boolean isAttributeContains(By locator, String attribute, String substring) {
        return isAttributeContains(locator, attribute, substring, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
    /**
     * Checks if the element's text exactly matches the expected text.
     * @param locator Element locator
     * @param expected Expected text
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return true if text matches exactly, false otherwise
     */
    public boolean isTextEqual(By locator, String expected, int timeout, int pollingEvery) {
        try {
            String actual = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator))
                    .getText();
            boolean match = expected.equals(actual);
            Reporter.log((match ? "Text matches" : "Text mismatch") + " – expected: '" + expected + "', actual: '" + actual + "'",
                    match ? LogLevel.INFO_BLUE : LogLevel.WARN);
            return match;
        } catch (TimeoutException e) {
            Reporter.log("Text equality check timed out: " + locator, LogLevel.ERROR);
            return false;
        } catch (Exception e) {
            Reporter.log("Text equality check failed: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if the element's text exactly matches the expected text with specified timeout.
     * @param locator Element locator
     * @param expected Expected text
     * @param timeout Maximum wait time in seconds
     * @return true if text matches exactly, false otherwise
     */
    public boolean isTextEqual(By locator, String expected, int timeout) {
        return isTextEqual(locator, expected, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Checks if the element's text exactly matches the expected text with default timeout and polling time.
     * @param locator Element locator
     * @param expected Expected text
     * @return true if text matches exactly, false otherwise
     */
    public boolean isTextEqual(By locator, String expected) {
        return isTextEqual(locator, expected, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
}