package Ellithium.Utilities.interactions;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class AlertActionsTest {

    private WebDriver mockDriver;
    private WebDriver.TargetLocator mockTargetLocator;
    private Alert mockAlert;
    private AlertActions<WebDriver> alertActions;

    @BeforeMethod
    public void setup() {
        mockDriver        = mock(WebDriver.class);
        mockTargetLocator = mock(WebDriver.TargetLocator.class);
        mockAlert         = mock(Alert.class);
        when(mockDriver.switchTo()).thenReturn(mockTargetLocator);
        when(mockTargetLocator.alert()).thenReturn(mockAlert);
        alertActions = new AlertActions<>(mockDriver);
    }

    @Test
    public void accept_callsAlertAccept() {
        alertActions.accept(5, 100);
        verify(mockAlert).accept();
    }

    @Test
    public void dismiss_callsAlertDismiss() {
        alertActions.dismiss(5, 100);
        verify(mockAlert).dismiss();
    }

    @Test
    public void getText_returnsAlertText() {
        when(mockAlert.getText()).thenReturn("Are you sure?");
        String text = alertActions.getText(5, 100);
        Assert.assertEquals(text, "Are you sure?");
    }

    @Test
    public void sendData_callsAlertSendKeys() {
        alertActions.sendData("confirm text", 5, 100);
        verify(mockAlert).sendKeys("confirm text");
    }

    @Test
    public void accept_delegatesToFullOverload_viaDefaultTimeout() {
        alertActions.accept(5);
        verify(mockAlert).accept();
    }

    @Test
    public void dismiss_delegatesToFullOverload_viaDefaultTimeout() {
        alertActions.dismiss(5);
        verify(mockAlert).dismiss();
    }

    @Test
    public void getText_delegatesToFullOverload_viaDefaultTimeout() {
        when(mockAlert.getText()).thenReturn("Hello");
        String result = alertActions.getText(5);
        Assert.assertEquals(result, "Hello");
    }

    @Test
    public void sendData_delegatesToFullOverload_viaDefaultTimeout() {
        alertActions.sendData("input", 5);
        verify(mockAlert).sendKeys("input");
    }
}
