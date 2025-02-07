package Tests;

import Base.BaseTests;
import Pages.AlertsPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AlertsTests extends BaseTests {
    @Test(priority = 1)
    public void alertClicked(){
        AlertsPage alertsPage=home.clickAlerts();
        alertsPage.clickJsAlert();
        alertsPage.alert_accept();
        String expectedResult ="You successfully clicked an alert";
        String actualResult=alertsPage.getResultMessage();
        Assert.assertTrue(actualResult.contains(expectedResult),"Clicking Alert failed");
    }
    @Test(priority = 1)
    public void confirmAlertCancel(){
        AlertsPage alertsPage=home.clickAlerts();
        alertsPage.clickJsConfirm();
        alertsPage.getAlertMessage();
        alertsPage.alert_Cancel();
        String expectedResult ="You clicked: Cancel";
        String actualResult=alertsPage.getResultMessage();
        Assert.assertTrue(actualResult.contains(expectedResult),"Canceling Confirm Alert failed");
    }
    @Test(priority = 1)
    public void confirmAlertAccept(){
        AlertsPage alertsPage=home.clickAlerts();
        alertsPage.clickJsConfirm();
        alertsPage.getAlertMessage();
        alertsPage.alert_accept();
        String expectedResult ="You clicked: Ok";
        String actualResult=alertsPage.getResultMessage();
        Assert.assertTrue(actualResult.contains(expectedResult),"Accepting Confirm Alert failed");
    }
    @Test(priority = 1)
    public void correctPromptEntered(){
        AlertsPage alertsPage=home.clickAlerts();
        alertsPage.clickJsPrompt();
        String input="Abdelrahman Ellithy";
        alertsPage.sendPrompt(input);
        alertsPage.alert_accept();
        String expectedResult ="You entered: "+input;
        String actualResult=alertsPage.getResultMessage();
        Assert.assertTrue(actualResult.contains(expectedResult),"Sending Prompt failed");
    }
}
