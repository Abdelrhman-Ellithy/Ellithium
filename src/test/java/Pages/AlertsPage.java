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
        driver.findElement(By.xpath("(//button)[1]")).click();
    }
    public void clickJsConfirm() throws InterruptedException {
        driver.findElement(By.xpath("(//button)[2]")).click();
    }
    public void clickJsPrompt(){
       driver.findElement(By.xpath("(//button)[3]")).click();
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
