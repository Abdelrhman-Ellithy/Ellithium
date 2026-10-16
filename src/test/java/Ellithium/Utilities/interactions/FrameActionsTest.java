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

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void switchToFrameByIndex_negativeIndex_throwsIllegalArgumentException() {
        frameActions.switchToFrameByIndex(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void switchToFrameByNameOrID_nullName_throwsIllegalArgumentException() {
        frameActions.switchToFrameByNameOrID(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void switchToFrameByNameOrID_blankName_throwsIllegalArgumentException() {
        frameActions.switchToFrameByNameOrID("   ");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void switchToFrameByElement_nullLocator_throwsIllegalArgumentException() {
        frameActions.switchToFrameByElement(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void waitForFrameToBeAvailableAndSwitchToIt_nullLocator_throwsIllegalArgumentException() {
        frameActions.waitForFrameToBeAvailableAndSwitchToIt(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void waitForFrameByNameOrIdToBeAvailableAndSwitchToIt_nullName_throwsIllegalArgumentException() {
        frameActions.waitForFrameByNameOrIdToBeAvailableAndSwitchToIt(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void waitForFrameByNameOrIdToBeAvailableAndSwitchToIt_blankName_throwsIllegalArgumentException() {
        frameActions.waitForFrameByNameOrIdToBeAvailableAndSwitchToIt("   ");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void waitForFrameByIndexToBeAvailableAndSwitchToIt_negativeIndex_throwsIllegalArgumentException() {
        frameActions.waitForFrameByIndexToBeAvailableAndSwitchToIt(-1);
    }
}
