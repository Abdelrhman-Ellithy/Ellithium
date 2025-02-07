package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AlertsPage {
    WebDriver driver;
    DriverActions driverActions;
    public AlertsPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(driver);
    }
    public void clickJsAlert(){
        driverActions.clickOnElement(By.xpath("//button[@onclick=\"jsAlert()\"]"));
    }
    public void clickJsConfirm(){
        driverActions.clickOnElement(By.xpath("//button[@onclick=\"jsConfirm()\"]"));
    }
    public void clickJsPrompt(){
        driverActions.clickOnElement(By.xpath("//button[@onclick=\"jsPrompt()\"]"));
    }
    public void alert_accept(){
        driverActions.acceptAlert();
    }
    public void alert_Cancel(){
        driverActions.dismissAlert();
    }
    public void sendPrompt(String input){
        driverActions.sendDataToAlert(input);
    }
    public String getAlertMessage(){
       return driverActions.getAlertText();
    }
    public String getResultMessage(){
        return  driverActions.getText(By.id("result"));
    }

}
