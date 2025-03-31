package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

public class SelectActions<T extends WebDriver> extends BaseActions<T> {
    
    public SelectActions(T driver) {
        super(driver);
    }

    /**
     * Selects a dropdown option by visible text.
     * @param locator Dropdown element locator
     * @param option Text of the option to select
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void selectDropdownByText(By locator, String option, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement(locator)).selectByVisibleText(option);
        Reporter.log("Selecting Dropdown Option By Text: " + option + " From Element: ", LogLevel.INFO_BLUE, locator.toString());
    }

    /**
     * Selects a dropdown option by value.
     * @param locator Dropdown element locator
     * @param value Value of the option to select
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void selectDropdownByValue(By locator, String value, int timeout, int pollingEvery) {
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement(locator)).selectByValue(value);
        Reporter.log("Selecting Dropdown Option By Value: " + value + " From Element: ", LogLevel.INFO_BLUE, locator.toString());
    }

    /**
     * Selects a dropdown option by index.
     * @param locator Dropdown element locator
     * @param index Index of the option to select
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void selectDropdownByIndex(By locator, int index, int timeout, int pollingEvery) {
        Reporter.log("Selecting Dropdown Option By Index: " + index + " From Element: ", LogLevel.INFO_BLUE, locator.toString());
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(findWebElement(locator)).selectByIndex(index);
    }

    /**
     * Retrieves the selected options' texts from a dropdown.
     * @param locator Dropdown element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     * @return List of selected options' texts
     */
    public List<String> getDropdownSelectedOptions(By locator, int timeout, int pollingEvery) {
        Reporter.log("Getting Dropdown Options Texts: ", LogLevel.INFO_BLUE, locator.toString());
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Select dropDown = new Select(findWebElement(locator));
        List<WebElement> elements = dropDown.getAllSelectedOptions();
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }

    /**
     * Selects a dropdown option by text for multiple elements.
     * @param locator Element locator
     * @param option Option to select
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void selectDropdownByTextForMultipleElements(By locator, String option, int timeout, int pollingEvery) {
        Reporter.log("Selecting dropdown option by text for multiple elements: " + option + " for locator: " + locator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));

        List<WebElement> elements = findWebElements(locator);
        for (WebElement element : elements) {
            new Select(element).selectByVisibleText(option);
        }
    }

    // Overloaded methods with default timeouts

    /**
     * Selects a dropdown option by visible text with default timeout and polling time.
     * @param locator Dropdown element locator
     * @param option Text of the option to select
     */
    public void selectDropdownByText(By locator, String option) {
        selectDropdownByText(locator, option, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by value with default timeout and polling time.
     * @param locator Dropdown element locator
     * @param value Value of the option to select
     */
    public void selectDropdownByValue(By locator, String value) {
        selectDropdownByValue(locator, value, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by index with default timeout and polling time.
     * @param locator Dropdown element locator
     * @param index Index of the option to select
     */
    public void selectDropdownByIndex(By locator, int index) {
        selectDropdownByIndex(locator, index, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Retrieves the selected options' texts from a dropdown with default timeout and polling time.
     * @param locator Dropdown element locator
     * @return List of selected options' texts
     */
    public List<String> getDropdownSelectedOptions(By locator) {
        return getDropdownSelectedOptions(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Retrieves the selected options' texts from a dropdown with specified timeout.
     * @param locator Dropdown element locator
     * @param timeout Maximum wait time in seconds
     * @return List of selected options' texts
     */
    public List<String> getDropdownSelectedOptions(By locator, int timeout) {
        return getDropdownSelectedOptions(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by text for multiple elements with default timeout and polling time.
     * @param locator Element locator
     * @param option Option to select
     */
    public void selectDropdownByTextForMultipleElements(By locator, String option) {
        selectDropdownByTextForMultipleElements(locator, option, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Selects a dropdown option by text for multiple elements with specified timeout.
     * @param locator Element locator
     * @param option Option to select
     * @param timeout Maximum wait time in seconds
     */
    public void selectDropdownByTextForMultipleElements(By locator, String option, int timeout) {
        selectDropdownByTextForMultipleElements(locator, option, timeout, WaitManager.getDefaultPollingTime());
    }
}
