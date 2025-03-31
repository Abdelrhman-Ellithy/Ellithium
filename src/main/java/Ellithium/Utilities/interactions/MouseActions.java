package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
public class MouseActions<T extends WebDriver> extends BaseActions<T> {
    
    public MouseActions(T driver) {
        super(driver);
    }

    /**
     * Hovers over an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public void hoverOverElement(By locator, int timeout) {
        Reporter.log("Hovered over element: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait(timeout, WaitManager.getDefaultPollingTime())
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.moveToElement(element).perform();
    }

    /**
     * Hovers over an element and clicks another element.
     * @param locatorToHover Element locator to hover
     * @param locatorToClick Element locator to click
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void hoverAndClick(By locatorToHover, By locatorToClick, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to hover: " + locatorToHover.toString(), LogLevel.INFO_BLUE);
        WebElement elementToHover = getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locatorToHover));

        Reporter.log("Waiting for element to click: " + locatorToClick.toString(), LogLevel.INFO_BLUE);
        WebElement elementToClick = getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.elementToBeClickable(locatorToClick));

        Actions action = new Actions(driver);
        action.moveToElement(elementToHover).click(elementToClick).perform();
        Reporter.log("Hovered over " + locatorToHover + " and clicked " + locatorToClick, LogLevel.INFO_BLUE);
    }

    /**
     * Performs a drag and drop action from a source element to a target element.
     * @param sourceLocator Source element locator
     * @param targetLocator Target element locator
     */
    public void dragAndDrop(By sourceLocator, By targetLocator) {
        WebElement source = findWebElement(sourceLocator);
        WebElement target = findWebElement(targetLocator);
        Actions action = new Actions(driver);
        action.clickAndHold(source)
                .moveToElement(target)
                .release()
                .perform();
        Reporter.log("Drag and drop performed from " + sourceLocator + " to " + targetLocator, LogLevel.INFO_BLUE);
    }

    /**
     * Performs a drag and drop action by offset.
     * @param sourceLocator Source element locator
     * @param xOffset X offset to move
     * @param yOffset Y offset to move
     */
    public void dragAndDropByOffset(By sourceLocator, int xOffset, int yOffset) {
        WebElement source = findWebElement(sourceLocator);
        Actions action = new Actions(driver);

        action.clickAndHold(source)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();

        Reporter.log("Drag and drop performed with offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }

    /**
     * Right-clicks on an element.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void rightClick(By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to right-click: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));

        Actions action = new Actions(driver);
        action.contextClick(element).perform();

        Reporter.log("Right-clicked on element: " + locator, LogLevel.INFO_BLUE);
    }

    /**
     * Double-clicks on an element.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void doubleClick(By locator, int timeout, int pollingEvery) {
        Reporter.log("Waiting for element to double-click: " + locator.toString(), LogLevel.INFO_BLUE);
        WebElement element = getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        Actions action = new Actions(driver);
        action.doubleClick(element).perform();
        Reporter.log("Double-clicked on element: " + locator, LogLevel.INFO_BLUE);
    }

    /**
     * Moves a slider to a specific value.
     * @param sliderLocator Slider element locator
     * @param rangeLocator Range element locator
     * @param targetValue Target value to move the slider to
     * @return The final value of the slider
     */
    public float moveSliderTo(By sliderLocator, By rangeLocator, float targetValue) {
        WebElement range = findWebElement(rangeLocator);
        WebElement slider = findWebElement(sliderLocator);
        float currentValue = Float.parseFloat(range.getText());
        int timeout = 0;
        while((Float.valueOf(range.getText())!=0) &&(timeout<5000) ){
            slider.sendKeys(Keys.ARROW_LEFT);
            timeout++;
        }
        timeout=0;
        while( (Float.valueOf(range.getText())!=targetValue) &&(timeout<5000)){
            slider.sendKeys(Keys.ARROW_RIGHT);
            timeout++;
        }
        Reporter.log("Slider moved to: " + currentValue, LogLevel.INFO_BLUE);
        return currentValue;
    }

    /**
     * Moves a slider by offset.
     * @param sliderLocator Slider element locator
     * @param xOffset X offset to move
     * @param yOffset Y offset to move
     * @param timeout Maximum wait time in seconds
     * @param pollingEvery Polling interval in milliseconds
     */
    public void moveSliderByOffset(By sliderLocator, int xOffset, int yOffset, int timeout, int pollingEvery) {
        Reporter.log("Waiting for slider to be visible: " + sliderLocator.toString(), LogLevel.INFO_BLUE);
        getFluentWait(timeout, pollingEvery)
                .until(ExpectedConditions.visibilityOfElementLocated(sliderLocator));

        WebElement slider = findWebElement(sliderLocator);
        Actions action = new Actions(driver);
        action.clickAndHold(slider)
                .moveByOffset(xOffset, yOffset)
                .release()
                .perform();
        Reporter.log("Slider moved by offset: X=" + xOffset + ", Y=" + yOffset, LogLevel.INFO_BLUE);
    }

    /**
     * Hovers over an element with default timeout.
     * @param locator Element locator
     */
    public void hoverOverElement(By locator) {
        hoverOverElement(locator, WaitManager.getDefaultTimeout());
    }

    /**
     * Hovers over an element and clicks another element with specified timeout.
     * @param locatorToHover Element locator to hover
     * @param locatorToClick Element locator to click
     * @param timeout Maximum wait time in seconds
     */
    public void hoverAndClick(By locatorToHover, By locatorToClick, int timeout) {
        hoverAndClick(locatorToHover, locatorToClick, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Hovers over an element and clicks another element with default timeout and polling time.
     * @param locatorToHover Element locator to hover
     * @param locatorToClick Element locator to click
     */
    public void hoverAndClick(By locatorToHover, By locatorToClick) {
        hoverAndClick(locatorToHover, locatorToClick, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Right-clicks on an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public void rightClick(By locator, int timeout) {
        rightClick(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Right-clicks on an element with default timeout and polling time.
     * @param locator Element locator
     */
    public void rightClick(By locator) {
        rightClick(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Double-clicks on an element with specified timeout.
     * @param locator Element locator
     * @param timeout Maximum wait time in seconds
     */
    public void doubleClick(By locator, int timeout) {
        doubleClick(locator, timeout, WaitManager.getDefaultPollingTime());
    }

    /**
     * Double-clicks on an element with default timeout and polling time.
     * @param locator Element locator
     */
    public void doubleClick(By locator) {
        doubleClick(locator, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }

    /**
     * Moves a slider by offset with default timeout and polling time.
     * @param sliderLocator Slider element locator
     * @param xOffset X offset to move
     * @param yOffset Y offset to move
     */
    public void moveSliderByOffset(By sliderLocator, int xOffset, int yOffset) {
        moveSliderByOffset(sliderLocator, xOffset, yOffset, WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
    }
}
