package Ellithium.Utilities.interactions;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
/**
 * Provides keyboard and key press actions for both web and mobile drivers.
 * Handles Android KeyEvents and iOS key press operations.
 * @param <T> Type of WebDriver being used
 */
public class KeyPressActions<T extends AppiumDriver> extends BaseActions<T> {
    public KeyPressActions(T driver) {
        super(driver);
    }

    /**
     * Presses any key event on Android device.
     * @param keyEvent KeyEvent to press
     * @throws UnsupportedOperationException if not using AndroidDriver
     */
    public void pressKey(KeyEvent keyEvent) {
        if (driver instanceof AndroidDriver) {
            try {
                ((AndroidDriver) driver).pressKey(keyEvent);
            } catch (Exception e) {
                Reporter.log("Failed to press key event: " + e.getMessage(), LogLevel.ERROR);
            }
        } else {
            Reporter.log("pressKey requires AndroidDriver", LogLevel.ERROR);
        }
    }

    /**
     * Long presses any key event on Android device.
     * @param keyEvent KeyEvent to press
     * @param durationMillis Duration to hold the key in milliseconds
     */
    public void longPressKey(KeyEvent keyEvent, long durationMillis) {
        if (driver instanceof AndroidDriver) {
            try {
                AndroidDriver androidDriver = (AndroidDriver) driver;
                androidDriver.longPressKey(keyEvent);
                new Sleep().sleepMillis(durationMillis);
                Reporter.log("Long pressed key for " + durationMillis + "ms", LogLevel.INFO_BLUE);
            } catch (Exception e) {
                Reporter.log("Failed to long press key: " + e.getMessage(), LogLevel.ERROR);
            }
        } else {
            Reporter.log("longPressKey requires AndroidDriver" , LogLevel.ERROR);
        }
    }

    /**
     * Long presses a key on Android device.
     * @param key AndroidKey enum value
     * @param durationMillis Duration to hold the key in milliseconds
     */
    public void longPressAndroidKey(AndroidKey key, long durationMillis) {
        longPressKey(new KeyEvent(key), durationMillis);
    }
}
