package Ellithium.Utilities.interactions;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class KeyPressActionsTest {

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void pressKey_throwsUnsupportedOperation_onNonAndroidDriver() {
        AppiumDriver mockAppiumDriver = mock(AppiumDriver.class);
        KeyPressActions<AppiumDriver> actions = new KeyPressActions<>(mockAppiumDriver);
        actions.pressKey(new KeyEvent(AndroidKey.ENTER));
    }

    @Test
    public void pressKey_delegatesToAndroidDriver() {
        AndroidDriver mockAndroid = mock(AndroidDriver.class);
        KeyPressActions<AndroidDriver> actions = new KeyPressActions<>(mockAndroid);
        KeyEvent event = new KeyEvent(AndroidKey.BACK);
        actions.pressKey(event);
        verify(mockAndroid).pressKey(event);
    }

    @Test
    public void longPressAndroidKey_delegatesViaLongPressKey() {
        AndroidDriver mockAndroid = mock(AndroidDriver.class);
        KeyPressActions<AndroidDriver> actions = new KeyPressActions<>(mockAndroid);
        actions.longPressAndroidKey(AndroidKey.HOME, 500L);
        verify(mockAndroid).longPressKey(any(KeyEvent.class));
    }

    @Test
    public void longPressKey_silentlyIgnores_nonAndroidDriver() {
        AppiumDriver mockAppiumDriver = mock(AppiumDriver.class);
        KeyPressActions<AppiumDriver> actions = new KeyPressActions<>(mockAppiumDriver);
        actions.longPressKey(new KeyEvent(AndroidKey.ENTER), 300L);
    }
}
