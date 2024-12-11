package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
public class SecureAreaPage {
    WebDriver driver;
    DriverActions driverActions;
    public SecureAreaPage(WebDriver Driver){
        driver=Driver;
        driverActions=new DriverActions(driver);
    }
    public String getLoginMassega(){
        return driverActions.getText(By.id("flash"));
    }
}
