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
            Assert.assertTrue(element.isDisplayed(), "Element should be displayed: " + locator.toString());
        }
        // Assert element is not displayed
        public static void assertElementNotDisplayed(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            Assert.assertFalse(element.isDisplayed(), "Element should not be displayed: " + locator.toString());
        }
        // Assert element is enabled
        public static void assertElementEnabled(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            Assert.assertTrue(element.isEnabled(), "Element should be enabled: " + locator.toString());
        }
        // Assert element is disabled
        public static void assertElementDisabled(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            Assert.assertFalse(element.isEnabled(), "Element should be disabled: " + locator.toString());
        }
        // Assert element text
        public static void assertElementText(WebDriver driver, By locator, String expectedText) {
            WebElement element = driver.findElement(locator);
            String actualText = element.getText().toLowerCase();
            Assert.assertEquals(actualText, expectedText, "Text should be: " + expectedText.toLowerCase() + " but was: " + actualText);
        }
        // Assert element attribute value
        public static void assertElementAttribute(WebDriver driver, By locator, String attribute, String expectedValue) {
            WebElement element = driver.findElement(locator);
            String actualValue = element.getAttribute(attribute);
            Assert.assertEquals(actualValue, expectedValue, "Attribute " + attribute + " should be: " + expectedValue + " but was: " + actualValue);
        }
        public static void assertCurrentUrlEquals(WebDriver driver, String expectedUrl) {
            Assert.assertEquals(driver.getCurrentUrl(), expectedUrl, "Current URL does not match.");
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
            softAssert.assertTrue(element.isDisplayed(), "Element should be displayed: " + locator.toString());
        }
        // Assert element is not displayed
        public void assertElementNotDisplayed(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            softAssert.assertFalse(element.isDisplayed(), "Element should not be displayed: " + locator.toString());
        }
        // Assert element is enabled
        public void assertElementEnabled(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            softAssert.assertTrue(element.isEnabled(), "Element should be enabled: " + locator.toString());
        }
        // Assert element is disabled
        public void assertElementDisabled(WebDriver driver, By locator) {
            WebElement element = driver.findElement(locator);
            softAssert.assertFalse(element.isEnabled(), "Element should be disabled: " + locator.toString());
        }
        // Assert element text
        public void assertElementText(WebDriver driver, By locator, String expectedText) {
            WebElement element = driver.findElement(locator);
            String actualText = element.getText();
            softAssert.assertEquals(actualText, expectedText, "Text should be: " + expectedText + " but was: " + actualText);
        }
        // Assert element attribute value
        public void assertElementAttribute(WebDriver driver, By locator, String attribute, String expectedValue) {
            WebElement element = driver.findElement(locator);
            String actualValue = element.getAttribute(attribute);
            softAssert.assertEquals(actualValue, expectedValue, "Attribute " + attribute + " should be: " + expectedValue + " but was: " + actualValue);
        }
        // Call this method at the end of the test to assert all soft assertions
        public void assertAll() {
            softAssert.assertAll();
        }
    }
}
