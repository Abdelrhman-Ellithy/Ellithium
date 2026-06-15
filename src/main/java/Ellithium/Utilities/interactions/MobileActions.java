package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
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
    // GESTURE EXECUTION & EXCEPTION HANDLING
    // ========================================================================================

    @FunctionalInterface
    private interface ElementGesture {
        void run(WebElement element);
    }

    /**
     * Runs an element-targeted gesture with intelligent recovery:
     * re-locates and retries once on a stale element, surfaces a missing/invisible
     * target as its real exception, and classifies any backend failure rather than
     * swallowing it.
     */
    private void performOnElement(String gestureName, By locator, ElementGesture gesture) {
        StaleElementReferenceException stale = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            WebElement element;
            try {
                element = waitForVisibilityAndFindElement(locator,
                        WaitManager.getDefaultTimeout(), WaitManager.getDefaultPollingTime());
            } catch (NoSuchElementException | TimeoutException e) {
                Reporter.log(gestureName + ": target element not found or not visible — " + e.getMessage(), LogLevel.ERROR);
                throw e;
            }
            try {
                gesture.run(element);
                Reporter.log("Successfully performed " + gestureName, LogLevel.INFO_GREEN);
                return;
            } catch (StaleElementReferenceException e) {
                stale = e;
                Reporter.log(gestureName + ": element went stale — re-locating and retrying", LogLevel.INFO_BLUE);
            } catch (Exception e) {
                throw classifyGestureFailure(gestureName, e);
            }
        }
        Reporter.log(gestureName + ": element still stale after retry", LogLevel.ERROR);
        if (stale != null) throw stale;
        throw new StaleElementReferenceException(gestureName + ": element still stale after retry");
    }

    /**
     * Runs a coordinate/area/device gesture (no element), classifying any failure
     * instead of swallowing it.
     */
    private void performGesture(String gestureName, Runnable gesture) {
        try {
            gesture.run();
            Reporter.log("Successfully performed " + gestureName, LogLevel.INFO_GREEN);
        } catch (Exception e) {
            throw classifyGestureFailure(gestureName, e);
        }
    }

    /**
     * Maps a gesture failure to a typed, actionable exception. Unsupported backend
     * commands become an {@link UnsupportedOperationException}; element-state, not-found
     * and timeout failures are rethrown with their original Selenium type so callers and
     * assertions see the true cause; anything else is wrapped with context.
     */
    private RuntimeException classifyGestureFailure(String gestureName, Exception e) {
        if (isUnsupportedCommand(e)) {
            Reporter.log(gestureName + " is not supported by the current Appium driver/automation backend — "
                    + e.getMessage(), LogLevel.ERROR);
            return new UnsupportedOperationException(
                    gestureName + " is not supported on the current driver/backend", e);
        }
        if (e instanceof InvalidElementStateException) {
            Reporter.log(gestureName + ": element is not in an interactable state — " + e.getMessage(), LogLevel.ERROR);
            return (InvalidElementStateException) e;
        }
        if (e instanceof NoSuchElementException) {
            Reporter.log(gestureName + ": target element not found — " + e.getMessage(), LogLevel.ERROR);
            return (NoSuchElementException) e;
        }
        if (e instanceof TimeoutException) {
            Reporter.log(gestureName + ": timed out — " + e.getMessage(), LogLevel.ERROR);
            return (TimeoutException) e;
        }
        if (e instanceof WebDriverException) {
            Reporter.log(gestureName + " failed: " + e.getMessage(), LogLevel.ERROR);
            return (WebDriverException) e;
        }
        Reporter.log(gestureName + " failed: " + e.getMessage(), LogLevel.ERROR);
        return new RuntimeException(gestureName + " failed", e);
    }

    private boolean isUnsupportedCommand(Throwable e) {
        if (e instanceof UnsupportedCommandException) return true;
        String m = e.getMessage();
        if (m == null) return false;
        String lm = m.toLowerCase(Locale.ROOT);
        return lm.contains("is not implemented") || lm.contains("not supported")
                || lm.contains("unknown command") || lm.contains("did not match a supported");
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
        Reporter.log("Performing tap gesture on element", LogLevel.INFO_BLUE);
        performOnElement("tap gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            if (isIOS()) {
                driver.executeScript("mobile: tap", params);
            } else {
                driver.executeScript("mobile: tapGesture", params);
            }
        });
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
        performGesture("tap gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("x", convertCoordinate(x));
            params.put("y", convertCoordinate(y));
            if (isIOS()) {
                driver.executeScript("mobile: tap", params);
            } else {
                driver.executeScript("mobile: tapGesture", params);
            }
        });
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
        Reporter.log("Performing double-tap gesture on element", LogLevel.INFO_BLUE);
        performOnElement("double-tap gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            if (isIOS()) {
                driver.executeScript("mobile: doubleTap", params);
            } else {
                driver.executeScript("mobile: doubleClickGesture", params);
            }
        });
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
        performGesture("double-tap gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("x", convertCoordinate(x));
            params.put("y", convertCoordinate(y));
            if (isIOS()) {
                driver.executeScript("mobile: doubleTap", params);
            } else {
                driver.executeScript("mobile: doubleClickGesture", params);
            }
        });
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
        Reporter.log("Performing double-tap gesture on element with duration " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        performOnElement("double-tap gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            if (isIOS()) {
                // XCUITest mobile: doubleTap does not accept a duration parameter — performs standard double-tap
                driver.executeScript("mobile: doubleTap", params);
            } else {
                driver.executeScript("mobile: doubleClickGesture", params);
            }
        });
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
        performGesture("double-tap gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("x", convertCoordinate(x));
            params.put("y", convertCoordinate(y));
            if (isIOS()) {
                // XCUITest mobile: doubleTap does not accept a duration parameter — performs standard double-tap
                driver.executeScript("mobile: doubleTap", params);
            } else {
                driver.executeScript("mobile: doubleClickGesture", params);
            }
        });
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
        Reporter.log("Performing long press gesture on element for " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        performOnElement("long press gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            if (isIOS()) {
                params.put("duration", durationSeconds);
                driver.executeScript("mobile: touchAndHold", params);
            } else {
                params.put("duration", secondsToMillis(durationSeconds));
                driver.executeScript("mobile: longClickGesture", params);
            }
        });
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
        performGesture("long press gesture", () -> {
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
        });
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
        Reporter.log("Performing swipe gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        performOnElement("swipe gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            putElement(params, element);
            if (isIOS()) {
                driver.executeScript("mobile: swipe", params);
            } else {
                params.put("percent", 0.5); // Default 50% swipe for Android
                driver.executeScript("mobile: swipeGesture", params);
            }
        });
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
        Reporter.log("Performing swipe gesture on element in direction " + direction + " with " + (percent * 100) + "%", LogLevel.INFO_BLUE);
        performOnElement("swipe gesture", locator, element -> {
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
        });
    }

    /**
     * Performs a swipe gesture from one point to another using W3C PointerInput actions.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: W3C touch pointer sequence (cross-platform)</li>
     *   <li>Android: W3C touch pointer sequence (cross-platform)</li>
     * </ul>
     *
     * @param startX The starting X coordinate (absolute viewport)
     * @param startY The starting Y coordinate (absolute viewport)
     * @param endX The ending X coordinate (absolute viewport)
     * @param endY The ending Y coordinate (absolute viewport)
     * @param durationSeconds The gesture duration in seconds
     */
    public void swipe(double startX, double startY, double endX, double endY, double durationSeconds) {
        Reporter.log("Performing swipe gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") over " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        performGesture("swipe gesture", () -> {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), (int) startX, (int) startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(secondsToMillis(durationSeconds)), PointerInput.Origin.viewport(), (int) endX, (int) endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));
        });
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
        performGesture("swipe gesture in area", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("direction", direction);
            params.put("percent", percent);
            driver.executeScript("mobile: swipeGesture", params);
        });
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
        Reporter.log("Performing scroll gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        performOnElement("scroll gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            putElement(params, element);
            if (isIOS()) {
                driver.executeScript("mobile: scroll", params);
            } else {
                params.put("percent", 0.5);
                driver.executeScript("mobile: scrollGesture", params);
            }
        });
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
        Reporter.log("Performing scroll gesture on element in direction " + direction + " with " + percent + "%", LogLevel.INFO_BLUE);
        performOnElement("scroll gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", direction);
            putElement(params, element);
            if (isIOS()) {
                driver.executeScript("mobile: scroll", params);
                if (percent != 50) {
                    Reporter.log("Note: iOS does not support percent parameter for scroll, using default", LogLevel.INFO_BLUE);
                }
            } else {
                params.put("percent", percent / 100.0);
                driver.executeScript("mobile: scrollGesture", params);
            }
        });
    }

    /**
     * Performs a scroll gesture from one point to another using W3C PointerInput actions (600ms).
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: W3C touch pointer sequence (cross-platform)</li>
     *   <li>Android: W3C touch pointer sequence (cross-platform)</li>
     * </ul>
     *
     * @param startX The starting X coordinate (absolute viewport)
     * @param startY The starting Y coordinate (absolute viewport)
     * @param endX The ending X coordinate (absolute viewport)
     * @param endY The ending Y coordinate (absolute viewport)
     */
    public void scroll(double startX, double startY, double endX, double endY) {
        Reporter.log("Performing scroll gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ")", LogLevel.INFO_BLUE);
        performGesture("scroll gesture", () -> {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 1);
            scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), (int) startX, (int) startY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), (int) endX, (int) endY));
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(scroll));
        });
    }

    /**
     * Performs a scroll gesture from one point to another with custom duration using W3C PointerInput actions.
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: W3C touch pointer sequence (cross-platform)</li>
     *   <li>Android: W3C touch pointer sequence (cross-platform)</li>
     * </ul>
     *
     * @param startX The starting X coordinate (absolute viewport)
     * @param startY The starting Y coordinate (absolute viewport)
     * @param endX The ending X coordinate (absolute viewport)
     * @param endY The ending Y coordinate (absolute viewport)
     * @param durationSeconds The gesture duration in seconds (applied on both platforms)
     */
    public void scroll(double startX, double startY, double endX, double endY, double durationSeconds) {
        Reporter.log("Performing scroll gesture from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ") over " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        performGesture("scroll gesture", () -> {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 1);
            scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), (int) startX, (int) startY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(finger.createPointerMove(Duration.ofMillis(secondsToMillis(durationSeconds)), PointerInput.Origin.viewport(), (int) endX, (int) endY));
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(scroll));
        });
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
        performGesture("scroll gesture in area", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("direction", direction);
            params.put("percent", percent / 100.0);
            driver.executeScript("mobile: scrollGesture", params);
        });
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
        Reporter.log("Scrolling to element with selector: " + selector + " in direction: " + direction, LogLevel.INFO_BLUE);
        performOnElement("scroll to element", locator, element -> {
            if (isIOS()) {
                Map<String, Object> params = new HashMap<>();
                putElement(params, element);
                params.put("predicateString", selector);
                params.put("direction", direction);
                driver.executeScript("mobile: scroll", params);
            } else {
                driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector()." + selector + ")"
                ));
            }
        });
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
        performGesture("scroll to element", () -> driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector()." + uiSelector + ")"
        )));
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
        Reporter.log("Performing drag gesture on element to coordinates (" + endX + ", " + endY + ") over " + durationSeconds + " seconds", LogLevel.INFO_BLUE);
        performOnElement("drag gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            if (isIOS()) {
                // XCUITest: when "element" is present, fromX/fromY are ignored and element center is used as drag source
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
        });
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
        performGesture("drag gesture", () -> {
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
        });
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
        Reporter.log("Performing pinch " + (zoomIn ? "open" : "close") + " gesture on element with " + (percent * 100) + "%", LogLevel.INFO_BLUE);
        performOnElement("pinch gesture", locator, element -> {
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
        });
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
        Reporter.log("Performing pinch gesture on element with scale: " + scale + " and velocity: " + velocity, LogLevel.INFO_BLUE);
        performOnElement("pinch gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            putElement(params, element);
            if (isIOS()) {
                params.put("scale", scale);
                params.put("velocity", velocity);
                driver.executeScript("mobile: pinch", params);
            } else {
                // UIAutomator2 has no scale/velocity pinch — route by direction with clamped percent
                params.put("percent", Math.min(Math.abs(scale - 1.0), 1.0));
                driver.executeScript(scale >= 1.0 ? "mobile: pinchOpenGesture" : "mobile: pinchCloseGesture", params);
            }
        });
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
        performGesture("pinch gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("x", x);
            params.put("y", y);
            params.put("scale", scale);
            params.put("velocity", velocity);
            driver.executeScript("mobile: pinch", params);
        });
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
        performGesture("pinch close gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("percent", percent);
            driver.executeScript("mobile: pinchCloseGesture", params);
        });
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
        performGesture("pinch open gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("percent", percent);
            driver.executeScript("mobile: pinchOpenGesture", params);
        });
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
        performGesture("pinch gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("left", left);
            params.put("top", top);
            params.put("width", width);
            params.put("height", height);
            params.put("percent", Math.min(Math.abs(scale - 1.0), 1.0));
            driver.executeScript(scale >= 1.0 ? "mobile: pinchOpenGesture" : "mobile: pinchCloseGesture", params);
        });
    }

    // ========================================================================================
    // MULTI-TOUCH GESTURES (Both platforms)
    // ========================================================================================

    /**
     * Performs a multi-touch gesture with multiple fingers (Android only).
     *
     * <p><b>Platform Support:</b>
     * <ul>
     *   <li>iOS: Not supported - will throw UnsupportedOperationException</li>
     *   <li>Android: Uses 'mobile: multiTouchGesture' command (UIAutomator2)</li>
     * </ul>
     *
     * @param actions Array of touch actions for each finger
     * @throws UnsupportedOperationException if called on iOS
     */
    public void multiTouchGesture(Map<String, Object>[] actions) {
        validatePlatform("multiTouchGesture", false, true);
        Reporter.log("Performing multi-touch gesture with " + actions.length + " fingers", LogLevel.INFO_BLUE);
        performGesture("multi-touch gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("actions", actions);
            driver.executeScript("mobile: multiTouchGesture", params);
        });
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
        validatePlatform("twoFingerTap", true, false);
        Reporter.log("Performing two-finger tap gesture on element", LogLevel.INFO_BLUE);
        performOnElement("two-finger tap gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            params.put("element", getElementId(element));
            driver.executeScript("mobile: twoFingerTap", params);
        });
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
        performGesture("iOS alert handling", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("action", action);
            params.put("buttonLabel", buttonLabel);
            driver.executeScript("mobile: alert", params);
        });
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
        validatePlatform("fling", false, true);
        Reporter.log("Performing fling gesture on element in direction: " + direction, LogLevel.INFO_BLUE);
        performOnElement("fling gesture", locator, element -> {
            Map<String, Object> params = new HashMap<>();
            params.put("elementId", getElementId(element));
            params.put("direction", direction);
            driver.executeScript("mobile: flingGesture", params);
        });
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
        performGesture("fling gesture", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("startX", startX);
            params.put("startY", startY);
            params.put("endX", endX);
            params.put("endY", endY);
            params.put("velocity", velocity);
            driver.executeScript("mobile: flingGesture", params);
        });
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
        performGesture("press key event", () -> ((AndroidDriver) driver).pressKey(keyEvent));
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
     * @param durationMillis Intended hold duration in milliseconds — not honored by Appium 10.1.1;
     *                       {@link io.appium.java_client.android.nativekey.KeyEvent} has no
     *                       {@code withDurationMs} method in this version
     * @throws UnsupportedOperationException if called on iOS
     */
    public void longPressKey(KeyEvent keyEvent, long durationMillis) {
        validatePlatform("longPressKey", false, true);
        Reporter.log("Long pressing key event for " + durationMillis + "ms on Android device", LogLevel.INFO_BLUE);
        performGesture("long press key event", () -> ((AndroidDriver) driver).longPressKey(keyEvent));
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
