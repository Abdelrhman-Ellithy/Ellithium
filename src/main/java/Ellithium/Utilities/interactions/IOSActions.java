package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for iOS-specific mobile gestures and actions.
 * Based on Appium XCUITest driver mobile gestures.
 * Reference: https://appium.readthedocs.io/en/latest/en/writing-running-appium/ios/ios-xctest-mobile-gestures/
 */
public class IOSActions<T extends IOSDriver> extends BaseActions<T> {
    
    /**
     * Constructor for IOSActions.
     * @param driver The Appium driver instance.
     */
    protected IOSActions(T driver) {
        super(driver);
    }

    /**
     * Performs a swipe gesture on an element in the specified direction.
     * @param direction The direction to swipe.
     * @param element The element to swipe.
     */
    public void swipe(String direction, WebElement element) {
        Reporter.log("Performing swipe gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: swipe", params);
            Reporter.log("Successfully performed swipe gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform swipe gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a scroll gesture on an element in the specified direction.
     * @param element The element to scroll.
     * @param direction The direction to scroll.
     */
    public void scroll(WebElement element, String direction) {
        Reporter.log("Performing scroll gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: scroll", params);
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Scrolls to an element using predicate string.
     * @param element The element to scroll from.
     * @param predicateString The predicate string to find the target element.
     * @param direction The direction to scroll.
     */
    public void scrollToElement(WebElement element, String predicateString, String direction) {
        Reporter.log("Scrolling to element with predicate: " + predicateString + " in direction: " + direction, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("element", ((RemoteWebElement) element).getId());
            params.put("predicateString", predicateString);
            params.put("direction", direction);
            driver.executeScript("mobile: scroll", params);
            Reporter.log("Successfully scrolled to element", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to scroll to element: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch gesture on an element.
     * @param element The element to pinch.
     * @param scale The scale factor.
     * @param velocity The velocity of the pinch.
     */
    public void pinch(WebElement element, float scale, float velocity) {
        Reporter.log("Performing pinch gesture on element with scale: " + scale + " and velocity: " + velocity, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("scale", scale);
            params.put("velocity", velocity);
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: pinch", params);
            Reporter.log("Successfully performed pinch gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a double-tap gesture on an element.
     * @param element The element to double-tap.
     */
    public void doubleTap(WebElement element) {
        Reporter.log("Performing double-tap gesture on element", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: doubleTap", params);
            Reporter.log("Successfully performed double-tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a double-tap gesture at specified coordinates.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void doubleTap(float x, float y) {
        Reporter.log("Performing double-tap gesture at coordinates (" + x + ", " + y + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            driver.executeScript("mobile: doubleTap", params);
            Reporter.log("Successfully performed double-tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a touch and hold gesture on an element.
     * @param element The element to touch and hold.
     * @param duration The duration in seconds.
     */
    public void touchAndHold(WebElement element, float duration) {
        Reporter.log("Performing touch and hold gesture on element for " + duration + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("duration", duration);
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: touchAndHold", params);
            Reporter.log("Successfully performed touch and hold gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform touch and hold gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a touch and hold gesture at specified coordinates.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param duration The duration in seconds.
     */
    public void touchAndHold(float x, float y, float duration) {
        Reporter.log("Performing touch and hold gesture at coordinates (" + x + ", " + y + ") for " + duration + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            params.put("duration", duration);
            driver.executeScript("mobile: touchAndHold", params);
            Reporter.log("Successfully performed touch and hold gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform touch and hold gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a two-finger tap gesture on an element.
     * @param element The element to two-finger tap.
     */
    public void twoFingerTap(WebElement element) {
        Reporter.log("Performing two-finger tap gesture on element", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: twoFingerTap", params);
            Reporter.log("Successfully performed two-finger tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform two-finger tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a drag gesture from one point to another over a specified duration.
     * @param element The element to drag from.
     * @param fromX The starting X coordinate.
     * @param fromY The starting Y coordinate.
     * @param toX The ending X coordinate.
     * @param toY The ending Y coordinate.
     * @param duration The duration in seconds.
     */
    public void dragFromToForDuration(WebElement element, float fromX, float fromY, float toX, float toY, float duration) {
        Reporter.log("Performing drag gesture from (" + fromX + ", " + fromY + ") to (" + toX + ", " + toY + ") over " + duration + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("fromX", fromX);
            params.put("fromY", fromY);
            params.put("toX", toX);
            params.put("toY", toY);
            params.put("duration", duration);
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: dragFromToForDuration", params);
            Reporter.log("Successfully performed drag gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform drag gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Handles iOS alerts.
     * @param action The action to perform (accept, dismiss).
     * @param buttonLabel The label of the button to click.
     */
    public void alert(String action, String buttonLabel) {
        Reporter.log("Handling iOS alert with action: " + action + " and button: " + buttonLabel, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("action", action);
            params.put("buttonLabel", buttonLabel);
            driver.executeScript("mobile: alert", params);
            Reporter.log("Successfully handled iOS alert", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to handle iOS alert: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a swipe gesture from one point to another.
     * @param startX The starting X coordinate.
     * @param startY The starting Y coordinate.
     * @param endX The ending X coordinate.
     * @param endY The ending Y coordinate.
     * @param duration The duration in seconds.
     */
    public void swipe(float startX, float startY, float endX, float endY, float duration) {
        Reporter.log("Performing swipe gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") over " + duration + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", startX);
            params.put("startY", startY);
            params.put("endX", endX);
            params.put("endY", endY);
            params.put("duration", duration);
            driver.executeScript("mobile: swipe", params);
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
     * @param duration The duration in seconds.
     */
    public void scroll(float startX, float startY, float endX, float endY, float duration) {
        Reporter.log("Performing scroll gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") over " + duration + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", startX);
            params.put("startY", startY);
            params.put("endX", endX);
            params.put("endY", endY);
            params.put("duration", duration);
            driver.executeScript("mobile: scroll", params);
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a tap gesture at specified coordinates.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void tap(float x, float y) {
        Reporter.log("Performing tap gesture at coordinates (" + x + ", " + y + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            driver.executeScript("mobile: tap", params);
            Reporter.log("Successfully performed tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a tap gesture on an element.
     * @param element The element to tap.
     */
    public void tap(WebElement element) {
        Reporter.log("Performing tap gesture on element", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: tap", params);
            Reporter.log("Successfully performed tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform tap gesture: " + e.getMessage(), LogLevel.ERROR);
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
     * Performs a pinch gesture with custom parameters.
     * @param element The element to pinch.
     * @param scale The scale factor.
     * @param velocity The velocity of the pinch.
     */
    public void pinch(WebElement element, double scale, double velocity) {
        Reporter.log("Performing pinch gesture on element with scale: " + scale + " and velocity: " + velocity, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("scale", scale);
            params.put("velocity", velocity);
            params.put("element", ((RemoteWebElement) element).getId());
            driver.executeScript("mobile: pinch", params);
            Reporter.log("Successfully performed pinch gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch gesture at specified coordinates.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param scale The scale factor.
     * @param velocity The velocity of the pinch.
     */
    public void pinch(float x, float y, double scale, double velocity) {
        Reporter.log("Performing pinch gesture at coordinates (" + x + ", " + y + ") with scale: " + scale + " and velocity: " + velocity, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            params.put("scale", scale);
            params.put("velocity", velocity);
            driver.executeScript("mobile: pinch", params);
            Reporter.log("Successfully performed pinch gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a drag gesture from one point to another.
     * @param fromX The starting X coordinate.
     * @param fromY The starting Y coordinate.
     * @param toX The ending X coordinate.
     * @param toY The ending Y coordinate.
     * @param duration The duration in seconds.
     */
    public void dragFromToForDuration(float fromX, float fromY, float toX, float toY, float duration) {
        Reporter.log("Performing drag gesture from (" + fromX + ", " + fromY + ") to (" + toX + ", " + toY + ") over " + duration + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("fromX", fromX);
            params.put("fromY", fromY);
            params.put("toX", toX);
            params.put("toY", toY);
            params.put("duration", duration);
            driver.executeScript("mobile: dragFromToForDuration", params);
            Reporter.log("Successfully performed drag gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform drag gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a touch and hold gesture at specified coordinates with default duration (1 second).
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public void touchAndHold(float x, float y) {
        touchAndHold(x, y, 1.0f);
    }

    /**
     * Performs a touch and hold gesture on an element with default duration (1 second).
     * @param element The element to touch and hold.
     */
    public void touchAndHold(WebElement element) {
        touchAndHold(element, 1.0f);
    }

    /**
     * Performs a double-tap gesture at specified coordinates with custom duration.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param duration The duration in seconds.
     */
    public void doubleTap(float x, float y, float duration) {
        Reporter.log("Performing double-tap gesture at coordinates (" + x + ", " + y + ") with duration " + duration + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            params.put("duration", duration);
            driver.executeScript("mobile: doubleTap", params);
            Reporter.log("Successfully performed double-tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a double-tap gesture on an element with custom duration.
     * @param element The element to double-tap.
     * @param duration The duration in seconds.
     */
    public void doubleTap(WebElement element, float duration) {
        Reporter.log("Performing double-tap gesture on element with duration " + duration + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("element", ((RemoteWebElement) element).getId());
            params.put("duration", duration);
            driver.executeScript("mobile: doubleTap", params);
            Reporter.log("Successfully performed double-tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }
}
