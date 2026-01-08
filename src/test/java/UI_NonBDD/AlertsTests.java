package UI_NonBDD;

import Base.BaseTests;
import Ellithium.Utilities.interactions.Sleep;
import Pages.AlertsPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AlertsTests extends BaseTests {
    boolean debug=false;
    @Test(priority = 1)
    public void alertClicked(){
        Sleep sleep=new Sleep();
        if (debug) sleep.sleepSeconds(2);
        AlertsPage alertsPage=home.clickAlerts();
        if (debug) sleep.sleepSeconds(2);
        alertsPage.clickJsAlert();
        if (debug) sleep.sleepSeconds(2);
        alertsPage.alert_accept();
        if (debug) sleep.sleepSeconds(2);
        String expectedResult ="You successfully clicked an alert";
        String actualResult=alertsPage.getResultMessage();
        if (debug) sleep.sleepSeconds(2);
        Assert.assertTrue(actualResult.contains(expectedResult),"Clicking Alert failed");
        if (debug) sleep.sleepSeconds(2);
    }
    @Test(priority = 1)
    public void confirmAlertCancel() throws InterruptedException {
        Sleep sleep=new Sleep();
        if (debug) sleep.sleepSeconds(2);
        AlertsPage alertsPage=home.clickAlerts();
        if (debug) sleep.sleepSeconds(2);
        alertsPage.clickJsConfirm();
        if (debug) sleep.sleepSeconds(2);
        alertsPage.alert_Cancel();
        if (debug) sleep.sleepSeconds(2);
        String expectedResult ="You clicked: Cancel";
        if (debug) sleep.sleepSeconds(2);
        String actualResult=alertsPage.getResultMessage();
        if (debug) sleep.sleepSeconds(2);
        Assert.assertTrue(actualResult.contains(expectedResult),"Canceling Confirm Alert failed");
        if (debug) sleep.sleepSeconds(2);
    }
    @Test(priority = 1)
    public void confirmAlertAccept() throws InterruptedException {
        Sleep sleep= new Sleep();
        if (debug) sleep.sleepSeconds(2);
        AlertsPage alertsPage=home.clickAlerts();
        if (debug) sleep.sleepSeconds(2);
        alertsPage.clickJsConfirm();
        alertsPage.alert_accept();
        if (debug) sleep.sleepSeconds(2);
        String expectedResult ="You clicked: Ok";
        String actualResult=alertsPage.getResultMessage();
        if (debug) sleep.sleepSeconds(2);
        Assert.assertTrue(actualResult.contains(expectedResult),"Accepting Confirm Alert failed");
        if (debug) sleep.sleepSeconds(2);
    }
    @Test(priority = 1)
    public void correctPromptEntered()  {
        Sleep sleep=new Sleep();
        if (debug) sleep.sleepSeconds(2);
        AlertsPage alertsPage=home.clickAlerts();
        if (debug) sleep.sleepSeconds(2);
        alertsPage.clickJsPrompt();
        if (debug) sleep.sleepSeconds(2);
        String input="Abdelrahman Ellithy";
        if (debug) sleep.sleepSeconds(2);
        alertsPage.sendPrompt(input);
        if (debug) sleep.sleepSeconds(2);
        alertsPage.alert_accept();
        if (debug) sleep.sleepSeconds(2);
        String expectedResult ="You entered: "+input;
        if (debug) sleep.sleepSeconds(2);
        String actualResult=alertsPage.getResultMessage();
        Assert.assertTrue(actualResult.contains(expectedResult),"Sending Prompt failed");
        if (debug) sleep.sleepSeconds(2);
    }
}
