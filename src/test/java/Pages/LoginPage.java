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
    // I will broke the locators to trigger self healing and commit to enable healing and correcting the locators
    // I must commit the file after change to enable healing and correcting the locators
    public void setUserName(String username) {
        driverActions.elements().sendData(By.tagName("emmmaaaiiilll"), username);
    }

    public void setPassword(String password) {
        driverActions.elements().sendData(By.id("secccrreeeeett"), password);
    }

    public SecureAreaPage clickLoginBtn() {
        driverActions.elements().clickOnElement(By.xpath("btttttnnnnnnn"));
        return new SecureAreaPage(driver);
    }
}
