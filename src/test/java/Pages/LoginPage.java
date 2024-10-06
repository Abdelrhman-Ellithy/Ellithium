package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static Ellithium.Utilities.browser.DriverActions.clickOnElement;
import static Ellithium.Utilities.browser.DriverActions.sendData;

public class LoginPage {
    WebDriver driver;
    public LoginPage(WebDriver driver) {
        this.driver=driver;
    }
    public void setUserName(String username){
        sendData(driver,By.id("username"),username);
    }
    public void setPassword(String password){
        sendData(driver,By.id("password"),password);
    }
    public SecureAreaPage clickLoginBtn(){
        clickOnElement(driver,By.tagName("button"));
        return new SecureAreaPage(driver);
    }

}
