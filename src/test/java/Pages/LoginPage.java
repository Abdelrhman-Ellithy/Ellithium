package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage {

    WebDriver driver;

    DriverActions driverActions;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        driverActions = new DriverActions(driver);
    }

    public void setUserName(String username) {
        driverActions.elements().sendData(By.cssSelector("input#emaaaiiillll"), username);
    }

    public void setPassword(String password) {
        driverActions.elements().sendData(By.tagName("seecrret"), password);
    }

    public SecureAreaPage clickLoginBtn() {
        driverActions.elements().clickOnElement(By.cssSelector("login.bttn"));
        return new SecureAreaPage(driver);
    }
}
