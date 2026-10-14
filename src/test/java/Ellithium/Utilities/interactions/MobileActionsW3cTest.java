package Ellithium.Utilities.interactions;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MobileActionsW3cTest {

    @Test
    public void w3cTap_dispatchesPointerSequence() {
        AppiumDriver driver = mock(AppiumDriver.class);
        MobileActions<AppiumDriver> actions = new MobileActions<>(driver);

        actions.w3cTap(150, 300);

        verify(driver).perform(any());
    }

    @Test
    public void w3cDoubleTap_dispatchesPointerSequence() {
        AppiumDriver driver = mock(AppiumDriver.class);
        MobileActions<AppiumDriver> actions = new MobileActions<>(driver);

        actions.w3cDoubleTap(200, 400);

        verify(driver).perform(any());
    }

    @Test
    public void w3cLongPress_dispatchesPointerSequence() {
        AppiumDriver driver = mock(AppiumDriver.class);
        MobileActions<AppiumDriver> actions = new MobileActions<>(driver);

        actions.w3cLongPress(250, 500, Duration.ofMillis(800));

        verify(driver).perform(any());
    }

    @Test
    public void w3cDragAndDrop_calculatesCentersAndPerformsSequence() {
        AppiumDriver driver = mock(AppiumDriver.class);
        MobileActions<AppiumDriver> actions = new MobileActions<>(driver);

        WebElement source = mock(WebElement.class);
        WebElement target = mock(WebElement.class);
        when(source.getRect()).thenReturn(new Rectangle(10, 20, 100, 50));
        when(target.getRect()).thenReturn(new Rectangle(200, 400, 100, 50));

        actions.w3cDragAndDrop(source, target, Duration.ofSeconds(1));

        verify(driver).perform(any());
    }

    @Test
    public void tapCoordinates_fallsBackToW3cWhenMobileScriptUnsupported() {
        AppiumDriver driver = mock(AppiumDriver.class);
        MobileActions<AppiumDriver> actions = new MobileActions<>(driver);

        // When executeScript mobile: tap throws UnsupportedCommandException
        when(driver.executeScript(anyString(), any())).thenThrow(new UnsupportedCommandException("mobile: tapGesture is not supported"));

        actions.tap(100.0, 200.0);

        // Verify fallback to W3C perform() occurred
        verify(driver).perform(any());
    }
}
