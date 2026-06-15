package Ellithium.Utilities.interactions;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class FrameActionsTest {

    private WebDriver mockDriver;
    private WebDriver.TargetLocator mockTargetLocator;
    private FrameActions<WebDriver> frameActions;

    @BeforeMethod
    public void setup() {
        mockDriver        = mock(WebDriver.class);
        mockTargetLocator = mock(WebDriver.TargetLocator.class);
        when(mockDriver.switchTo()).thenReturn(mockTargetLocator);
        frameActions = new FrameActions<>(mockDriver);
    }

    @Test
    public void switchToDefaultContent_callsSwitchToDefaultContent() {
        frameActions.switchToDefaultContent();
        verify(mockTargetLocator).defaultContent();
    }

    @Test
    public void switchToDefaultContent_onlyCallsSwitchOnce() {
        frameActions.switchToDefaultContent();
        verify(mockDriver, times(1)).switchTo();
    }
}
