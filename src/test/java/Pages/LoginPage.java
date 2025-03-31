package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
public class LoginPage {
    WebDriver driver;
    DriverActions driverActions;
    public LoginPage(WebDriver driver) {
        this.driver=driver;
        driverActions=new DriverActions(driver);
    }
    public void setUserName(String username){
        driverActions.elements().sendData(By.id("username"),username);
    }
    public void setPassword(String password){
        driverActions.elements().sendData(By.id("password"),password);
    }
    public SecureAreaPage clickLoginBtn(){
        driverActions.elements().clickOnElement(By.tagName("button"));
        return new SecureAreaPage(driver);
    }

}
