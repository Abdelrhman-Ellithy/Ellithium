package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SecureAreaPage {

    WebDriver driver;

    DriverActions driverActions;

    public SecureAreaPage(WebDriver Driver) {
        driver = Driver;
        driverActions = new DriverActions(driver);
    }

    public String getSecureAreaMessage() {
        return driverActions.elements().getText(By.cssSelector("content"));
    }
}
