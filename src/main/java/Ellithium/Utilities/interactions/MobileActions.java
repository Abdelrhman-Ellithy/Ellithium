package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified utility class for cross-platform mobile gestures and actions.
 * Provides a consistent interface for iOS and Android mobile interactions.
 *
 * <p>Automatically detects the platform (iOS/Android) and executes the appropriate
 * platform-specific commands. Methods are designed to work seamlessly across both
 * platforms where functionality overlaps, while platform-specific methods are clearly
 * marked and validated at runtime.</p>
 *
 * <p><b>References:</b>
 * <ul>
 *   <li>iOS XCTest: <a href="https://appium.readthedocs.io/en/latest/en/writing-running-appium/ios/ios-xctest-mobile-gestures/">...</a></li>
 *   <li>Android UIAutomator2: https://github.com/appium/appium-uiautomator2-driver/blob/master/docs/android-mobile-gestures.md</li>
 * </ul>
 *
 * @param <T> The AppiumDriver type (IOSDriver or AndroidDriver)
 */
public class MobileActions<T extends AppiumDriver> extends BaseActions<T> {

    /**
     * Constructor for MobileActions.
     * @param driver The Appium driver instance (IOSDriver or AndroidDriver)
     */
    protected MobileActions(T driver) {
        super(driver);
    }

    // ========================================================================================
    // PLATFORM DETECTION UTILITIES
    // ========================================================================================

    /**
     * Checks if the current driver is an iOS driver.
     * @return true if iOS, false otherwise
     */
    private boolean isIOS() {
        return driver instanceof IOSDriver;
    }

    /**
     * Checks if the current driver is an Android driver.
     * @return true if Android, false otherwise
     */
    private boolean isAndroid() {
        return driver instanceof AndroidDriver;
    }

    /**
     * Validates that the current platform matches the requirement.
     * @param methodName The name of the method being called
     * @param requiresIOS Whether iOS is required
     * @param requiresAndroid Whether Android is required
     * @throws UnsupportedOperationException if platform requirements are not met
     */
    private void validatePlatform(String methodName, boolean requiresIOS, boolean requiresAndroid) {
        if (requiresIOS && !isIOS()) {
            Reporter.log(methodName + " is only supported on iOS devices", LogLevel.ERROR);
            throw new UnsupportedOperationException(methodName + " is only supported on iOS devices");
        }
        if (requiresAndroid && !isAndroid()) {
            Reporter.log(methodName + " is only supported on Android devices", LogLevel.ERROR);
            throw new UnsupportedOperationException(methodName + " is only supported on Android devices");
        }
    }

    // ========================================================================================
    // HELPER UTILITIES
    // ========================================================================================

    /**
     * Extracts the element ID from a RemoteWebElement.
     * @param element The WebElement
     * @return The element ID string
     */
    private String getElementId(WebElement element) {
        return ((RemoteWebElement) element).getId();
    }

    /**
     * Adds the element reference to params map based on platform.
     * iOS uses "element" key, Android uses "elementId" key.
     * @param params The parameters map
     * @param element The WebElement
     */
    private void putElement(Map<String, Object> params, WebElement element) {
        if (isIOS()) {
            params.put("element", getElementId(element));
        } else {
            params.put("elementId", getElementId(element));
        }
    }

    /**
     * Converts seconds to milliseconds for Android gestures.
     * @param seconds Duration in seconds
     * @return Duration in milliseconds
     */
    private int secondsToMillis(double seconds) {
        return (int) (seconds * 1000);
    }

    /**
     * Converts numeric coordinates to appropriate type based on platform.
     * iOS uses float/double, Android uses int.
     * @param value The coordinate value
     * @return The value as appropriate type
     */
    private Number convertCoordinate(double value) {
        return isAndroid() ? (int) value : value;
    }

    // ========================================================================================
    // TAP GESTURES (Cross-platform)
    // ========================================================================================

    /**
     * Performs a tap gesture on an element.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: tap' command</li>
     *   <li>Android: Uses 'mobile: tapGesture' command</li>
     * </ul>
     *
     * @param locator Locator to the element to tap
     */
    public void tap(By locator) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing tap gesture on element", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);

            if (isIOS()) {
                driver.executeScript("mobile: tap", params);
            } else {
                driver.executeScript("mobile: tapGesture", params);
            }
            Reporter.log("Successfully performed tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a tap gesture at specified coordinates.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: tap' command</li>
     *   <li>Android: Uses 'mobile: tapGesture' command</li>
     * </ul>
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public void tap(double x, double y) {
        Reporter.log("Performing tap gesture at coordinates (" + x + ", " + y + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", convertCoordinate(x));
            params.put("y", convertCoordinate(y));

            if (isIOS()) {
                driver.executeScript("mobile: tap", params);
            } else {
                driver.executeScript("mobile: tapGesture", params);
            }
            Reporter.log("Successfully performed tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // ========================================================================================
    // DOUBLE TAP GESTURES (Cross-platform)
    // ========================================================================================

    /**
     * Performs a double-tap gesture on an element.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: doubleTap' command</li>
     *   <li>Android: Uses 'mobile: doubleClickGesture' command</li>
     * </ul>
     *
     * @param locator Locator to the element to double-tap
     */
    public void doubleTap(By locator) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing double-tap gesture on element", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);

            if (isIOS()) {
                driver.executeScript("mobile: doubleTap", params);
            } else {
                driver.executeScript("mobile: doubleClickGesture", params);
            }
            Reporter.log("Successfully performed double-tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a double-tap gesture at specified coordinates.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: doubleTap' command</li>
     *   <li>Android: Uses 'mobile: doubleClickGesture' command</li>
     * </ul>
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public void doubleTap(double x, double y) {
        Reporter.log("Performing double-tap gesture at coordinates (" + x + ", " + y + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", convertCoordinate(x));
            params.put("y", convertCoordinate(y));

            if (isIOS()) {
                driver.executeScript("mobile: doubleTap", params);
            } else {
                driver.executeScript("mobile: doubleClickGesture", params);
            }
            Reporter.log("Successfully performed double-tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a double-tap gesture on an element with custom duration.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: doubleTap' command with duration parameter</li>
     *   <li>Android: Not supported with duration parameter</li>
     * </ul>
     *
     * @param locator Locator to the element to double-tap
     * @param durationSeconds The duration in seconds
     */
    public void doubleTap(By locator, double durationSeconds) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing double-tap gesture on element with duration " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);

            if (isIOS()) {
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: doubleTap", params);
            } else {
                // Android doesn't support duration for double-tap, perform standard double-tap
                driver.executeScript("mobile: doubleClickGesture", params);
                Reporter.log("Note: Android does not support duration parameter for double-tap", LogLevel.INFO_BLUE);
            }
            Reporter.log("Successfully performed double-tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a double-tap gesture at specified coordinates with custom duration.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: doubleTap' command with duration parameter</li>
     *   <li>Android: Not supported with duration parameter</li>
     * </ul>
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param durationSeconds The duration in seconds
     */
    public void doubleTap(double x, double y, double durationSeconds) {
        Reporter.log("Performing double-tap gesture at coordinates (" + x + ", " + y + ") with duration " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", convertCoordinate(x));
            params.put("y", convertCoordinate(y));

            if (isIOS()) {
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: doubleTap", params);
            } else {
                driver.executeScript("mobile: doubleClickGesture", params);
                Reporter.log("Note: Android does not support duration parameter for double-tap", LogLevel.INFO_BLUE);
            }
            Reporter.log("Successfully performed double-tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform double-tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // ========================================================================================
    // LONG PRESS / TOUCH AND HOLD GESTURES (Cross-platform)
    // ========================================================================================

    /**
     * Performs a long press gesture on an element with default duration (1 second).
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: touchAndHold' command</li>
     *   <li>Android: Uses 'mobile: longClickGesture' command</li>
     * </ul>
     *
     * @param locator Locator to the element to long press
     */
    public void longPress(By locator) {
        longPress(locator, 1.0);
    }

    /**
     * Performs a long press gesture on an element with custom duration.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: touchAndHold' command (duration in seconds)</li>
     *   <li>Android: Uses 'mobile: longClickGesture' command (duration in milliseconds)</li>
     * </ul>
     *
     * @param locator Locator to the element to long press
     * @param durationSeconds The duration in seconds
     */
    public void longPress(By locator, double durationSeconds) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing long press gesture on element for " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);

            if (isIOS()) {
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: touchAndHold", params);
            } else {
                params.put("duration", secondsToMillis(durationSeconds));
                driver.executeScript("mobile: longClickGesture", params);
            }
            Reporter.log("Successfully performed long press gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform long press gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a long press gesture at specified coordinates with default duration (1 second).
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: touchAndHold' command</li>
     *   <li>Android: Uses 'mobile: longClickGesture' command</li>
     * </ul>
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public void longPress(double x, double y) {
        longPress(x, y, 1.0);
    }

    /**
     * Performs a long press gesture at specified coordinates with custom duration.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: touchAndHold' command (duration in seconds)</li>
     *   <li>Android: Uses 'mobile: longClickGesture' command (duration in milliseconds)</li>
     * </ul>
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param durationSeconds The duration in seconds
     */
    public void longPress(double x, double y, double durationSeconds) {
        Reporter.log("Performing long press gesture at coordinates (" + x + ", " + y + ") for " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("x", convertCoordinate(x));
            params.put("y", convertCoordinate(y));

            if (isIOS()) {
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: touchAndHold", params);
            } else {
                params.put("duration", secondsToMillis(durationSeconds));
                driver.executeScript("mobile: longClickGesture", params);
            }
            Reporter.log("Successfully performed long press gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform long press gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // ========================================================================================
    // SWIPE GESTURES (Cross-platform)
    // ========================================================================================

    /**
     * Performs a swipe gesture on an element in the specified direction.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: swipe' command</li>
     *   <li>Android: Uses 'mobile: swipeGesture' command with default 50% swipe distance</li>
     * </ul>
     *
     * @param direction The direction to swipe (up, down, left, right)
     * @param locator Locator to the element to swipe on
     */
    public void swipe(String direction, By locator) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing swipe gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            putElement(params, element);

            if (isIOS()) {
                driver.executeScript("mobile: swipe", params);
            } else {
                params.put("percent", 0.5); // Default 50% swipe for Android
                driver.executeScript("mobile: swipeGesture", params);
            }
            Reporter.log("Successfully performed swipe gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform swipe gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a swipe gesture on an element in the specified direction with custom percentage.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: swipe' command (percent parameter not supported, will be ignored)</li>
     *   <li>Android: Uses 'mobile: swipeGesture' command with specified percent</li>
     * </ul>
     *
     * @param locator Locator to the element to swipe on
     * @param direction The direction to swipe (up, down, left, right)
     * @param percent The swipe distance as percentage (0.0 to 1.0)
     */
    public void swipe(By locator, String direction, double percent) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing swipe gesture on element in direction " + direction + " with " + (percent * 100) + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            putElement(params, element);

            if (isIOS()) {
                driver.executeScript("mobile: swipe", params);
                if (percent != 0.5) {
                    Reporter.log("Note: iOS does not support percent parameter for swipe, using default", LogLevel.INFO_BLUE);
                }
            } else {
                params.put("percent", percent);
                driver.executeScript("mobile: swipeGesture", params);
            }
            Reporter.log("Successfully performed swipe gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform swipe gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a swipe gesture from one point to another.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: swipe' command with coordinate parameters (duration in seconds)</li>
     *   <li>Android: Uses 'mobile: swipeGesture' command (duration in milliseconds)</li>
     * </ul>
     *
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param endX The ending X coordinate
     * @param endY The ending Y coordinate
     * @param durationSeconds The duration in seconds
     */
    public void swipe(double startX, double startY, double endX, double endY, double durationSeconds) {
        Reporter.log("Performing swipe gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") over " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", convertCoordinate(startX));
            params.put("startY", convertCoordinate(startY));
            params.put("endX", convertCoordinate(endX));
            params.put("endY", convertCoordinate(endY));

            if (isIOS()) {
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: swipe", params);
            } else {
                params.put("duration", secondsToMillis(durationSeconds));
                driver.executeScript("mobile: swipeGesture", params);
            }
            Reporter.log("Successfully performed swipe gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform swipe gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a swipe gesture in a specified area.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: swipeGesture' command with area parameters</li>
     * </ul>
     *
     * @param left The left coordinate
     * @param top The top coordinate
     * @param width The width
     * @param height The height
     * @param direction The direction to swipe
     * @param percent The swipe percentage
     * @throws UnsupportedOperationException if called on iOS
     */
    public void swipeInArea(int left, int top, int width, int height, String direction, double percent) {
        validatePlatform("swipeInArea", false, true);
        Reporter.log("Performing swipe gesture in area (" + left + ", " + top + ", " + width + ", " + height + ") in direction " + direction + " with " + (percent * 100) + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("direction", direction);
            params.put("percent", percent);
            driver.executeScript("mobile: swipeGesture", params);
            Reporter.log("Successfully performed swipe gesture in area", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform swipe gesture in area: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // ========================================================================================
    // SCROLL GESTURES (Cross-platform)
    // ========================================================================================

    /**
     * Performs a scroll gesture on an element in the specified direction.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: scroll' command</li>
     *   <li>Android: Uses 'mobile: scrollGesture' command with default 50% scroll distance</li>
     * </ul>
     *
     * @param locator Locator to the element to scroll
     * @param direction The direction to scroll (up, down, left, right)
     */
    public void scroll(By locator, String direction) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing scroll gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            putElement(params, element);

            if (isIOS()) {
                driver.executeScript("mobile: scroll", params);
            } else {
                params.put("percent", 50); // Default 50% scroll for Android
                driver.executeScript("mobile: scrollGesture", params);
            }
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a scroll gesture on an element in the specified direction with custom percentage.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: scroll' command (percent parameter not supported, will be ignored)</li>
     *   <li>Android: Uses 'mobile: scrollGesture' command with specified percent</li>
     * </ul>
     *
     * @param locator Locator to the element to scroll
     * @param direction The direction to scroll (up, down, left, right)
     * @param percent The scroll distance as percentage (0-100)
     */
    public void scroll(By locator, String direction, int percent) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing scroll gesture on element in direction " + direction + " with " + percent + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            putElement(params, element);

            if (isIOS()) {
                driver.executeScript("mobile: scroll", params);
                if (percent != 50) {
                    Reporter.log("Note: iOS does not support percent parameter for scroll, using default", LogLevel.INFO_BLUE);
                }
            } else {
                params.put("percent", percent);
                driver.executeScript("mobile: scrollGesture", params);
            }
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a scroll gesture from one point to another.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: scroll' command with coordinate parameters (duration in seconds)</li>
     *   <li>Android: Uses 'mobile: scrollGesture' command with coordinate parameters</li>
     * </ul>
     *
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param endX The ending X coordinate
     * @param endY The ending Y coordinate
     */
    public void scroll(double startX, double startY, double endX, double endY) {
        Reporter.log("Performing scroll gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", convertCoordinate(startX));
            params.put("startY", convertCoordinate(startY));
            params.put("endX", convertCoordinate(endX));
            params.put("endY", convertCoordinate(endY));

            if (isIOS()) {
                params.put("duration", 1.0); // Default duration for iOS
                driver.executeScript("mobile: scroll", params);
            } else {
                params.put("percent", 50); // Default percent for Android
                driver.executeScript("mobile: scrollGesture", params);
            }
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a scroll gesture from one point to another with custom duration/percent.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: scroll' command (durationSeconds parameter)</li>
     *   <li>Android: Uses 'mobile: scrollGesture' command (percent parameter)</li>
     * </ul>
     *
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param endX The ending X coordinate
     * @param endY The ending Y coordinate
     * @param durationSeconds The duration in seconds for iOS, treated as percent (0-100) for Android
     */
    public void scroll(double startX, double startY, double endX, double endY, double durationSeconds) {
        Reporter.log("Performing scroll gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ")", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", convertCoordinate(startX));
            params.put("startY", convertCoordinate(startY));
            params.put("endX", convertCoordinate(endX));
            params.put("endY", convertCoordinate(endY));

            if (isIOS()) {
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: scroll", params);
            } else {
                params.put("percent", (int) durationSeconds); // Treat as percent for Android
                driver.executeScript("mobile: scrollGesture", params);
            }
            Reporter.log("Successfully performed scroll gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a scroll gesture in a specified area.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: scrollGesture' command with area parameters</li>
     * </ul>
     *
     * @param left The left coordinate
     * @param top The top coordinate
     * @param width The width
     * @param height The height
     * @param direction The direction to scroll
     * @param percent The scroll percentage (0-100)
     * @throws UnsupportedOperationException if called on iOS
     */
    public void scrollInArea(int left, int top, int width, int height, String direction, int percent) {
        validatePlatform("scrollInArea", false, true);
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
            Reporter.log("Successfully performed scroll gesture in area", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform scroll gesture in area: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Scrolls to an element using predicate string (iOS) or UiSelector (Android).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: scroll' command with predicateString</li>
     *   <li>Android: Uses 'mobile: scroll' command with UiSelector strategy</li>
     * </ul>
     * 
     * @param locator Locator to the to scroll from (iOS only)
     * @param selector The predicate string (iOS) or UiSelector string (Android)
     * @param direction The direction to scroll
     */
    public void scrollToElement(By locator, String selector, String direction) {
        WebElement element=findWebElement(locator);
        Reporter.log("Scrolling to element with selector: " + selector + " in direction: " + direction, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            
            if (isIOS()) {
                putElement(params, element);
                params.put("predicateString", selector);
                params.put("direction", direction);
                driver.executeScript("mobile: scroll", params);
            } else {
                params.put("strategy", "-android uiautomator");
                params.put("selector", selector);
                driver.executeScript("mobile: scroll", params);
            }
            Reporter.log("Successfully scrolled to element", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to scroll to element: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Scrolls to an element using UiSelector (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: scroll' command with UiSelector strategy</li>
     * </ul>
     * 
     * @param uiSelector The UiSelector string
     * @throws UnsupportedOperationException if called on iOS
     */
    public void scrollToElementBySelector(String uiSelector) {
        validatePlatform("scrollToElementBySelector", false, true);
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

    // ========================================================================================
    // DRAG GESTURES (Cross-platform)
    // ========================================================================================

    /**
     * Performs a drag gesture on an element to specified coordinates.
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: dragFromToForDuration' command with element coordinates as start</li>
     *   <li>Android: Uses 'mobile: dragGesture' command</li>
     * </ul>
     * 
     * @param locator Locator to the element to drag from
     * @param endX The end X coordinate
     * @param endY The end Y coordinate
     */
    public void drag(By locator, double endX, double endY) {
        drag(locator, endX, endY, 1.0);
    }

    /**
     * Performs a drag gesture on an element to specified coordinates with custom duration.
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: dragFromToForDuration' command (duration in seconds)</li>
     *   <li>Android: Uses 'mobile: dragGesture' command (duration not supported, will be ignored)</li>
     * </ul>
     * 
     * @param locator Locator to the element to drag from
     * @param endX The end X coordinate
     * @param endY The end Y coordinate
     * @param durationSeconds The duration in seconds
     */
    public void drag(By locator, double endX, double endY, double durationSeconds) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing drag gesture on element to coordinates (" + endX + ", " + endY + ") over " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            
            if (isIOS()) {
                // For iOS, we need fromX, fromY which should be the element's center
                // Since we don't have element coordinates, we'll use 0,0 and rely on element reference
                params.put("fromX", 0.0);
                params.put("fromY", 0.0);
                params.put("toX", endX);
                params.put("toY", endY);
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: dragFromToForDuration", params);
            } else {
                params.put("endX", (int) endX);
                params.put("endY", (int) endY);
                driver.executeScript("mobile: dragGesture", params);
                if (durationSeconds != 1.0) {
                    Reporter.log("Note: Android does not support duration parameter for drag", LogLevel.INFO_BLUE);
                }
            }
            Reporter.log("Successfully performed drag gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform drag gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a drag gesture from one point to another.
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: dragFromToForDuration' command (duration in seconds)</li>
     *   <li>Android: Uses 'mobile: dragGesture' command (duration not supported, will be ignored)</li>
     * </ul>
     * 
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param endX The ending X coordinate
     * @param endY The ending Y coordinate
     */
    public void drag(double startX, double startY, double endX, double endY) {
        drag(startX, startY, endX, endY, 1.0);
    }

    /**
     * Performs a drag gesture from one point to another with custom duration.
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: dragFromToForDuration' command (duration in seconds)</li>
     *   <li>Android: Uses 'mobile: dragGesture' command (duration not supported, will be ignored)</li>
     * </ul>
     * 
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param endX The ending X coordinate
     * @param endY The ending Y coordinate
     * @param durationSeconds The duration in seconds
     */
    public void drag(double startX, double startY, double endX, double endY, double durationSeconds) {
        Reporter.log("Performing drag gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") over " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            
            if (isIOS()) {
                params.put("fromX", startX);
                params.put("fromY", startY);
                params.put("toX", endX);
                params.put("toY", endY);
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: dragFromToForDuration", params);
            } else {
                params.put("startX", (int) startX);
                params.put("startY", (int) startY);
                params.put("endX", (int) endX);
                params.put("endY", (int) endY);
                driver.executeScript("mobile: dragGesture", params);
                if (durationSeconds != 1.0) {
                    Reporter.log("Note: Android does not support duration parameter for drag", LogLevel.INFO_BLUE);
                }
            }
            Reporter.log("Successfully performed drag gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform drag gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // ========================================================================================
    // PINCH GESTURES (Cross-platform with different implementations)
    // ========================================================================================

    /**
     * Performs a pinch gesture on an element.
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: pinch' command with scale and velocity</li>
     *   <li>Android: Uses 'mobile: pinchOpenGesture' or 'mobile: pinchCloseGesture' based on zoomIn parameter</li>
     * </ul>
     * 
     * @param locator Locator to the element to pinch
     * @param zoomIn true to zoom in (pinch open), false to zoom out (pinch close)
     * @param percent The pinch percentage (0.0 to 1.0) - Android only, ignored on iOS
     */
    public void pinch(By locator, boolean zoomIn, double percent) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing pinch " + (zoomIn ? "open" : "close") + " gesture on element with " + (percent * 100) + "%", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            
            if (isIOS()) {
                // iOS uses scale and velocity
                double scale = zoomIn ? 2.0 : 0.5; // Scale > 1 zooms in, < 1 zooms out
                params.put("scale", scale);
                params.put("velocity", 1.0);
                driver.executeScript("mobile: pinch", params);
                if (percent != 0.5) {
                    Reporter.log("Note: iOS does not use percent parameter, using scale instead", LogLevel.INFO_BLUE);
                }
            } else {
                params.put("percent", percent);
                if (zoomIn) {
                    driver.executeScript("mobile: pinchOpenGesture", params);
                } else {
                    driver.executeScript("mobile: pinchCloseGesture", params);
                }
            }
            Reporter.log("Successfully performed pinch gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch gesture on an element with custom scale and velocity.
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: pinch' command with scale and velocity parameters</li>
     *   <li>Android: Uses 'mobile: pinchGesture' command if available, or pinchOpen/Close based on scale</li>
     * </ul>
     * 
     * @param locator Locator to the element to pinch
     * @param scale The scale factor (> 1 zooms in, < 1 zooms out)
     * @param velocity The velocity of the pinch
     */
    public void pinch(By locator, double scale, double velocity) {
        WebElement element=findWebElement(locator);
        Reporter.log("Performing pinch gesture on element with scale: " + scale + " and velocity: " + velocity, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            params.put("scale", scale);
            params.put("velocity", velocity);
            
            if (isIOS()) {
                driver.executeScript("mobile: pinch", params);
            } else {
                // Android supports pinchGesture with scale and velocity
                driver.executeScript("mobile: pinchGesture", params);
            }
            Reporter.log("Successfully performed pinch gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform pinch gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a pinch gesture at specified coordinates.
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: pinch' command with x, y, scale, and velocity</li>
     *   <li>Android: Not directly supported - will throw UnsupportedOperationException</li>
     * </ul>
     * 
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param scale The scale factor
     * @param velocity The velocity of the pinch
     * @throws UnsupportedOperationException if called on Android
     */
    public void pinch(double x, double y, double scale, double velocity) {
        validatePlatform("pinch at coordinates", true, false);
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
     * Performs a pinch close gesture in a specified area (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: pinchCloseGesture' command</li>
     * </ul>
     * 
     * @param left The left coordinate
     * @param top The top coordinate
     * @param width The width
     * @param height The height
     * @param percent The pinch percentage
     * @throws UnsupportedOperationException if called on iOS
     */
    public void pinchCloseInArea(int left, int top, int width, int height, double percent) {
        validatePlatform("pinchCloseInArea", false, true);
        Reporter.log("Performing pinch close gesture in area (" + left + ", " + top + ", " + width + ", " + height + ") with " + (percent * 100) + "%", LogLevel.INFO_BLUE);
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
     * Performs a pinch open gesture in a specified area (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: pinchOpenGesture' command</li>
     * </ul>
     * 
     * @param left The left coordinate
     * @param top The top coordinate
     * @param width The width
     * @param height The height
     * @param percent The pinch percentage
     * @throws UnsupportedOperationException if called on iOS
     */
    public void pinchOpenInArea(int left, int top, int width, int height, double percent) {
        validatePlatform("pinchOpenInArea", false, true);
        Reporter.log("Performing pinch open gesture in area (" + left + ", " + top + ", " + width + ", " + height + ") with " + (percent * 100) + "%", LogLevel.INFO_BLUE);
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
     * Performs a pinch gesture in a specified area with scale and velocity (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: pinchGesture' command with area parameters</li>
     * </ul>
     * 
     * @param left The left coordinate
     * @param top The top coordinate
     * @param width The width
     * @param height The height
     * @param scale The scale factor
     * @param velocity The velocity of the pinch
     * @throws UnsupportedOperationException if called on iOS
     */
    public void pinchInArea(int left, int top, int width, int height, double scale, double velocity) {
        validatePlatform("pinchInArea", false, true);
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

    // ========================================================================================
    // MULTI-TOUCH GESTURES (Both platforms)
    // ========================================================================================

    /**
     * Performs a multi-touch gesture with multiple fingers.
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: multiTouchGesture' command</li>
     *   <li>Android: Uses 'mobile: multiTouchGesture' command</li>
     * </ul>
     * 
     * @param actions Array of touch actions for each finger
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

    // ========================================================================================
    // iOS-SPECIFIC GESTURES
    // ========================================================================================

    /**
     * Performs a two-finger tap gesture on an element (iOS only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: twoFingerTap' command</li>
     *   <li>Android: Not supported - will throw UnsupportedOperationException</li>
     * </ul>
     * 
     * @param locator Locator to the element to two-finger tap
     * @throws UnsupportedOperationException if called on Android
     */
    public void twoFingerTap(By locator) {
        WebElement element=findWebElement(locator);
        validatePlatform("twoFingerTap", true, false);
        Reporter.log("Performing two-finger tap gesture on element", LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("element", getElementId(element));
            driver.executeScript("mobile: twoFingerTap", params);
            Reporter.log("Successfully performed two-finger tap gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform two-finger tap gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Handles iOS alerts (iOS only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Uses 'mobile: alert' command</li>
     *   <li>Android: Not supported - will throw UnsupportedOperationException</li>
     * </ul>
     * 
     * @param action The action to perform (accept, dismiss)
     * @param buttonLabel The label of the button to click
     * @throws UnsupportedOperationException if called on Android
     */
    public void handleAlert(String action, String buttonLabel) {
        validatePlatform("handleAlert", true, false);
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

    // ========================================================================================
    // ANDROID-SPECIFIC GESTURES
    // ========================================================================================

    /**
     * Performs a fling gesture on an element in the specified direction (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: flingGesture' command</li>
     * </ul>
     * 
     * @param locator Locator to the element to fling
     * @param direction The direction to fling (up, down, left, right)
     * @throws UnsupportedOperationException if called on iOS
     */
    public void fling(By locator, String direction) {
        WebElement element=findWebElement(locator);
        validatePlatform("fling", false, true);
        Reporter.log("Performing fling gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", getElementId(element));
            params.put("direction", direction);
            driver.executeScript("mobile: flingGesture", params);
            Reporter.log("Successfully performed fling gesture", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to perform fling gesture: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Performs a fling gesture from specified coordinates (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: flingGesture' command</li>
     * </ul>
     * 
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param endX The ending X coordinate
     * @param endY The ending Y coordinate
     * @param velocity The velocity of the fling
     * @throws UnsupportedOperationException if called on iOS
     */
    public void fling(int startX, int startY, int endX, int endY, int velocity) {
        validatePlatform("fling", false, true);
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

    // ========================================================================================
    // ANDROID-SPECIFIC KEY PRESS ACTIONS
    // ========================================================================================

    /**
     * Presses any key event on Android device (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses AndroidDriver.pressKey() method</li>
     * </ul>
     * 
     * @param keyEvent KeyEvent to press
     * @throws UnsupportedOperationException if called on iOS
     */
    public void pressKey(KeyEvent keyEvent) {
        validatePlatform("pressKey", false, true);
        Reporter.log("Pressing key event on Android device", LogLevel.INFO_BLUE);
        try {
            ((AndroidDriver) driver).pressKey(keyEvent);
            Reporter.log("Successfully pressed key event", LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("Failed to press key event: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Long presses any key event on Android device (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses AndroidDriver.longPressKey() method</li>
     * </ul>
     * 
     * @param keyEvent KeyEvent to press
     * @param durationMillis Duration to hold the key in milliseconds
     * @throws UnsupportedOperationException if called on iOS
     */
    public void longPressKey(KeyEvent keyEvent, long durationMillis) {
        validatePlatform("longPressKey", false, true);
        Reporter.log("Long pressing key event for " + durationMillis + "ms on Android device", LogLevel.INFO_BLUE);
        try {
            AndroidDriver androidDriver = (AndroidDriver) driver;
            androidDriver.longPressKey(keyEvent);
            Thread.sleep(durationMillis);
            Reporter.log("Long pressed key for " + durationMillis + "ms", LogLevel.INFO_GREEN);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Reporter.log("Failed to long press key: " + e.getMessage(), LogLevel.ERROR);
        } catch (Exception e) {
            Reporter.log("Failed to long press key: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    /**
     * Long presses an Android key (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses AndroidDriver.longPressKey() method</li>
     * </ul>
     * 
     * @param key AndroidKey enum value
     * @param durationMillis Duration to hold the key in milliseconds
     * @throws UnsupportedOperationException if called on iOS
     */
    public void longPressAndroidKey(AndroidKey key, long durationMillis) {
        longPressKey(new KeyEvent(key), durationMillis);
    }

    /**
     * Presses an Android key (Android only).
     * 
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses AndroidDriver.pressKey() method</li>
     * </ul>
     * 
     * @param key AndroidKey enum value
     * @throws UnsupportedOperationException if called on iOS
     */
    public void pressAndroidKey(AndroidKey key) {
        pressKey(new KeyEvent(key));
    }
}