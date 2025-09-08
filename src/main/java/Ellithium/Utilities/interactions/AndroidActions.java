package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Android-specific mobile gestures and actions.
 * Based on Appium UIAutomator2 driver mobile gestures.
 * Reference: https://github.com/appium/appium-uiautomator2-driver/blob/master/docs/android-mobile-gestures.md
 */
public class AndroidActions<T extends AndroidDriver> extends KeyPressActions<T> {
    
    /**
     * Constructor for AndroidActions.
     * @param driver The Appium driver instance.
     */
    protected AndroidActions(T driver) {
        super(driver);
    }

    /**
     * Performs a drag gesture on an element to specified coordinates.
     * @param element The element to drag.
     * @param endX The end X coordinate.
     * @param endY The end Y coordinate.
     */
    public void dragGesture(WebElement element, int endX, int endY) {
        Reporter.log("Performing drag gesture on element to coordinates (" + endX + ", " + endY + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            params.put("endX", endX);
            params.put("endY", endY);
            driver.executeScript("mobile: dragGesture", params);
            Reporter.log("Successfully performed drag gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform drag gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a drag gesture from specified start coordinates to end coordinates.
     * @param startX The start X coordinate.
     * @param startY The start Y coordinate.
     * @param endX The end X coordinate.
     * @param endY The end Y coordinate.
     */
    public void dragGesture(int startX, int startY, int endX, int endY) {
        Reporter.log("Performing drag gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", startX);
            params.put("startY", startY);
            params.put("endX", endX);
            params.put("endY", endY);
            driver.executeScript("mobile: dragGesture", params);
            Reporter.log("Successfully performed drag gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform drag gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a fling gesture on an element in the specified direction.
     * @param element The element to fling.
     * @param direction The direction to fling (up, down, left, right).
     */
    public void flingGesture(WebElement element, String direction) {
        Reporter.log("Performing fling gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            params.put("direction", direction);
            driver.executeScript("mobile: flingGesture", params);
            Reporter.log("Successfully performed fling gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform fling gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a double-click gesture on an element.
     * @param element The element to double-click.
     */
    public void doubleClickGesture(WebElement element) {
        Reporter.log("Performing double-click gesture on element", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: doubleClickGesture", params);
            Reporter.log("Successfully performed double-click gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-click gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a double-click gesture at specified coordinates.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void doubleClickGesture(int x, int y) {
        Reporter.log("Performing double-click gesture at coordinates (" + x + ", " + y + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            driver.executeScript("mobile: doubleClickGesture", params);
            Reporter.log("Successfully performed double-click gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-click gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a long-click gesture on an element with default duration (500ms).
     * @param element The element to long-click.
     */
    public void longClickGesture(WebElement element) {
        longClickGesture(element, 500);
    }

    /**
     * Performs a long-click gesture at specified coordinates with default duration (500ms).
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void longClickGesture(int x, int y) {
        longClickGesture(x, y, 500);
    }

    /**
     * Performs a long-click gesture at specified coordinates with custom duration.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param durationMs The duration in milliseconds.
     */
    public void longClickGesture(int x, int y, int durationMs) {
        Reporter.log("Performing long-click gesture at coordinates (" + x + ", " + y + ") for " + durationMs + "ms", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            params.put("duration", durationMs);
            driver.executeScript("mobile: longClickGesture", params);
            Reporter.log("Successfully performed long-click gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform long-click gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a long-click gesture on an element with custom duration.
     * @param element The element to long-click.
     * @param durationMs The duration in milliseconds.
     */
    public void longClickGesture(WebElement element, int durationMs) {
        Reporter.log("Performing long-click gesture on element for " + durationMs + "ms", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            params.put("duration", durationMs);
            driver.executeScript("mobile: longClickGesture", params);
            Reporter.log("Successfully performed long-click gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform long-click gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch close gesture on an element.
     * @param element The element to pinch.
     * @param percent The pinch percentage.
     */
    public void pinchCloseGesture(WebElement element, double percent) {
        Reporter.log("Performing pinch close gesture on element with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            params.put("percent", percent);
            driver.executeScript("mobile: pinchCloseGesture", params);
            Reporter.log("Successfully performed pinch close gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch close gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch close gesture in a specified area.
     * @param left The left coordinate.
     * @param top The top coordinate.
     * @param width The width.
     * @param height The height.
     * @param percent The pinch percentage.
     */
    public void pinchCloseGesture(int left, int top, int width, int height, double percent) {
        Reporter.log("Performing pinch close gesture in area (" + left + ", " + top + ", " + width + ", " + height + ") with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("percent", percent);
            driver.executeScript("mobile: pinchCloseGesture", params);
            Reporter.log("Successfully performed pinch close gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch close gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch open gesture on an element.
     * @param element The element to pinch.
     * @param percent The pinch percentage.
     */
    public void pinchOpenGesture(WebElement element, double percent) {
        Reporter.log("Performing pinch open gesture on element with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            params.put("percent", percent);
            driver.executeScript("mobile: pinchOpenGesture", params);
            Reporter.log("Successfully performed pinch open gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch open gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch open gesture in a specified area.
     * @param left The left coordinate.
     * @param top The top coordinate.
     * @param width The width.
     * @param height The height.
     * @param percent The pinch percentage.
     */
    public void pinchOpenGesture(int left, int top, int width, int height, double percent) {
        Reporter.log("Performing pinch open gesture in area (" + left + ", " + top + ", " + width + ", " + height + ") with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("percent", percent);
            driver.executeScript("mobile: pinchOpenGesture", params);
            Reporter.log("Successfully performed pinch open gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch open gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a swipe gesture on an element.
     * @param element The element to swipe.
     * @param direction The direction to swipe.
     * @param percent The swipe percentage.
     */
    public void swipeGesture(WebElement element, String direction, double percent) {
        Reporter.log("Performing swipe gesture on element in direction " + direction + " with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            params.put("direction", direction);
            params.put("percent", percent);
            driver.executeScript("mobile: swipeGesture", params);
            Reporter.log("Successfully performed swipe gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform swipe gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a swipe gesture in a specified area.
     * @param left The left coordinate.
     * @param top The top coordinate.
     * @param width The width.
     * @param height The height.
     * @param direction The direction to swipe.
     * @param percent The swipe percentage.
     */
    public void swipeGesture(int left, int top, int width, int height, String direction, double percent) {
        Reporter.log("Performing swipe gesture in area (" + left + ", " + top + ", " + width + ", " + height + ") in direction " + direction + " with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("direction", direction);
            params.put("percent", percent);
            driver.executeScript("mobile: swipeGesture", params);
            Reporter.log("Successfully performed swipe gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform swipe gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a scroll gesture on an element.
     * @param element The element to scroll.
     * @param direction The direction to scroll.
     * @param percent The scroll percentage.
     */
    public void scrollGesture(WebElement element, String direction, int percent) {
        Reporter.log("Performing scroll gesture on element in direction " + direction + " with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            params.put("direction", direction);
            params.put("percent", percent);
            driver.executeScript("mobile: scrollGesture", params);
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a scroll gesture in a specified area.
     * @param left The left coordinate.
     * @param top The top coordinate.
     * @param width The width.
     * @param height The height.
     * @param direction The direction to scroll.
     * @param percent The scroll percentage.
     */
    public void scrollGesture(int left, int top, int width, int height, String direction, int percent) {
        Reporter.log("Performing scroll gesture in area (" + left + ", " + top + ", " + width + ", " + height + ") in direction " + direction + " with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("direction", direction);
            params.put("percent", percent);
            driver.executeScript("mobile: scrollGesture", params);
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Scrolls to an element using UiSelector.
     * @param uiSelector The UiSelector string.
     */
    public void scrollToElement(String uiSelector) {
        Reporter.log("Scrolling to element with UiSelector: " + uiSelector, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("strategy", "-android uiautomator");
            params.put("selector", uiSelector);
            driver.executeScript("mobile: scroll", params);
            Reporter.log("Successfully scrolled to element", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to scroll to element: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a multi-touch gesture with multiple fingers.
     * @param actions Array of touch actions for each finger.
     */
    public void multiTouchGesture(Map<String, Object>[] actions) {
        Reporter.log("Performing multi-touch gesture with " + actions.length + " fingers", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("actions", actions);
            driver.executeScript("mobile: multiTouchGesture", params);
            Reporter.log("Successfully performed multi-touch gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform multi-touch gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a tap gesture at specified coordinates.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void tapGesture(int x, int y) {
        Reporter.log("Performing tap gesture at coordinates (" + x + ", " + y + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            driver.executeScript("mobile: tapGesture", params);
            Reporter.log("Successfully performed tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a tap gesture on an element.
     * @param element The element to tap.
     */
    public void tapGesture(WebElement element) {
        Reporter.log("Performing tap gesture on element", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: tapGesture", params);
            Reporter.log("Successfully performed tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a swipe gesture from one point to another.
     * @param startX The starting X coordinate.
     * @param startY The starting Y coordinate.
     * @param endX The ending X coordinate.
     * @param endY The ending Y coordinate.
     * @param duration The duration in milliseconds.
     */
    public void swipeGesture(int startX, int startY, int endX, int endY, int duration) {
        Reporter.log("Performing swipe gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") over " + duration + "ms", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", startX);
            params.put("startY", startY);
            params.put("endX", endX);
            params.put("endY", endY);
            params.put("duration", duration);
            driver.executeScript("mobile: swipeGesture", params);
            Reporter.log("Successfully performed swipe gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform swipe gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a scroll gesture from one point to another.
     * @param startX The starting X coordinate.
     * @param startY The starting Y coordinate.
     * @param endX The ending X coordinate.
     * @param endY The ending Y coordinate.
     * @param percent The scroll percentage.
     */
    public void scrollGesture(int startX, int startY, int endX, int endY, int percent) {
        Reporter.log("Performing scroll gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", startX);
            params.put("startY", startY);
            params.put("endX", endX);
            params.put("endY", endY);
            params.put("percent", percent);
            driver.executeScript("mobile: scrollGesture", params);
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a fling gesture from specified coordinates.
     * @param startX The starting X coordinate.
     * @param startY The starting Y coordinate.
     * @param endX The ending X coordinate.
     * @param endY The ending Y coordinate.
     * @param velocity The velocity of the fling.
     */
    public void flingGesture(int startX, int startY, int endX, int endY, int velocity) {
        Reporter.log("Performing fling gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") with velocity " + velocity, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", startX);
            params.put("startY", startY);
            params.put("endX", endX);
            params.put("endY", endY);
            params.put("velocity", velocity);
            driver.executeScript("mobile: flingGesture", params);
            Reporter.log("Successfully performed fling gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform fling gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch gesture with custom parameters.
     * @param element The element to pinch.
     * @param scale The scale factor.
     * @param velocity The velocity of the pinch.
     */
    public void pinchGesture(WebElement element, double scale, double velocity) {
        Reporter.log("Performing pinch gesture on element with scale: " + scale + " and velocity: " + velocity, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", ((RemoteWebElement) element).getId());
            params.put("scale", scale);
            params.put("velocity", velocity);
            driver.executeScript("mobile: pinchGesture", params);
            Reporter.log("Successfully performed pinch gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch gesture in a specified area with custom parameters.
     * @param left The left coordinate.
     * @param top The top coordinate.
     * @param width The width.
     * @param height The height.
     * @param scale The scale factor.
     * @param velocity The velocity of the pinch.
     */
    public void pinchGesture(int left, int top, int width, int height, double scale, double velocity) {
        Reporter.log("Performing pinch gesture in area (" + left + ", " + top + ", " + width + ", " + height + ") with scale: " + scale + " and velocity: " + velocity, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("scale", scale);
            params.put("velocity", velocity);
            driver.executeScript("mobile: pinchGesture", params);
            Reporter.log("Successfully performed pinch gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }
}
