package Ellithium.Utilities;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

public class AssertionUtility {
    public static class HardAssertionUtility {
        // Assert element is displayed
        public static void assertElementDisplayed(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            boolean isDisplayed = element.isDisplayed();
            Assert.assertTrue(isDisplayed, "Element should be displayed: " + locator.toString());
            logsUtils.info(Colors.BLUE + "Assert Element Displayed: " + locator + " - Result: " + isDisplayed + Colors.RESET);
        }

        // Assert element is not displayed
        public static void assertElementNotDisplayed(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            boolean isDisplayed = element.isDisplayed();
            Assert.assertFalse(isDisplayed, "Element should not be displayed: " + locator.toString());
            logsUtils.info(Colors.BLUE + "Assert Element Not Displayed: " + locator + " - Result: " + isDisplayed + Colors.RESET);
        }

        // Assert element is enabled
        public static void assertElementEnabled(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            boolean isEnabled = element.isEnabled();
            Assert.assertTrue(isEnabled, "Element should be enabled: " + locator.toString());
            logsUtils.info(Colors.BLUE + "Assert Element Enabled: " + locator + " - Result: " + isEnabled + Colors.RESET);
        }

        // Assert element is disabled
        public static void assertElementDisabled(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            boolean isEnabled = element.isEnabled();
            Assert.assertFalse(isEnabled, "Element should be disabled: " + locator.toString());
            logsUtils.info(Colors.BLUE + "Assert Element Disabled: " + locator + " - Result: " + isEnabled + Colors.RESET);
        }

        // Assert element text
        public static void assertElementText(WebDriver driver, By locator, String expectedText) {
            WebElement element = driver.findElement(locator);
            String actualText = element.getText().toLowerCase();
            Assert.assertEquals(actualText, expectedText.toLowerCase(), "Text should be: " + expectedText.toLowerCase() + " but was: " + actualText);
            logsUtils.info(Colors.BLUE + "Assert Element Text: " + locator + " - Expected: " + expectedText + " - Actual: " + actualText + Colors.RESET);
        }

        // Assert element attribute value
        public static void assertElementAttribute(WebDriver driver, By locator, String attribute, String expectedValue) {
            WebElement element = driver.findElement(locator);
            String actualValue = element.getAttribute(attribute);
            Assert.assertEquals(actualValue, expectedValue, "Attribute " + attribute + " should be: " + expectedValue + " but was: " + actualValue);
            logsUtils.info(Colors.BLUE + "Assert Element Attribute: " + locator + " - Attribute: " + attribute + " - Expected: " + expectedValue + " - Actual: " + actualValue + Colors.RESET);
        }

        // Assert current URL equals expected URL
        public static void assertCurrentUrlEquals(WebDriver driver, String expectedUrl) {
            String currentUrl = driver.getCurrentUrl();
            Assert.assertEquals(currentUrl, expectedUrl, "Current URL does not match.");
            logsUtils.info(Colors.BLUE + "Assert Current URL Equals: Expected: " + expectedUrl + " - Actual: " + currentUrl + Colors.RESET);
        }
    }

    public static class SoftAssertionUtility {
        private final SoftAssert softAssert;

        public SoftAssertionUtility() {
            this.softAssert = new SoftAssert();
        }

        // Assert element is displayed
        public void assertElementDisplayed(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            boolean isDisplayed = element.isDisplayed();
            softAssert.assertTrue(isDisplayed, "Element should be displayed: " + locator.toString());
            logsUtils.info(Colors.BLUE + "Soft Assert Element Displayed: " + locator + " - Result: " + isDisplayed + Colors.RESET);
        }

        // Assert element is not displayed
        public void assertElementNotDisplayed(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            boolean isDisplayed = element.isDisplayed();
            softAssert.assertFalse(isDisplayed, "Element should not be displayed: " + locator.toString());
            logsUtils.info(Colors.BLUE + "Soft Assert Element Not Displayed: " + locator + " - Result: " + isDisplayed + Colors.RESET);
        }

        // Assert element is enabled
        public void assertElementEnabled(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            boolean isEnabled = element.isEnabled();
            softAssert.assertTrue(isEnabled, "Element should be enabled: " + locator.toString());
            logsUtils.info(Colors.BLUE + "Soft Assert Element Enabled: " + locator + " - Result: " + isEnabled + Colors.RESET);
        }

        // Assert element is disabled
        public void assertElementDisabled(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            boolean isEnabled = element.isEnabled();
            softAssert.assertFalse(isEnabled, "Element should be disabled: " + locator.toString());
            logsUtils.info(Colors.BLUE + "Soft Assert Element Disabled: " + locator + " - Result: " + isEnabled + Colors.RESET);
        }

        // Assert element text
        public void assertElementText(WebDriver driver, By locator, String expectedText) {
            WebElement element = driver.findElement(locator);
            String actualText = element.getText();
            softAssert.assertEquals(actualText, expectedText, "Text should be: " + expectedText + " but was: " + actualText);
            logsUtils.info(Colors.BLUE + "Soft Assert Element Text: " + locator + " - Expected: " + expectedText + " - Actual: " + actualText + Colors.RESET);
        }

        // Assert element attribute value
        public void assertElementAttribute(WebDriver driver, By locator, String attribute, String expectedValue) {
            WebElement element = driver.findElement(locator);
            String actualValue = element.getAttribute(attribute);
            softAssert.assertEquals(actualValue, expectedValue, "Attribute " + attribute + " should be: " + expectedValue + " but was: " + actualValue);
            logsUtils.info(Colors.BLUE + "Soft Assert Element Attribute: " + locator + " - Attribute: " + attribute + " - Expected: " + expectedValue + " - Actual: " + actualValue + Colors.RESET);
        }

        // Call this method at the end of the test to assert all soft assertions
        public void assertAll() {
            softAssert.assertAll();
            logsUtils.info(Colors.BLUE + "All Soft Assertions Completed" + Colors.RESET);
        }
    }
}