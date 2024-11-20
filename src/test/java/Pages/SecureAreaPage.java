package Pages;

import Ellithium.Utilities.interactions.WebDriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
public class SecureAreaPage {
    WebDriver driver;
    WebDriverActions driverActions;
    public SecureAreaPage(WebDriver Driver){
        driver=Driver;

        driverActions=new WebDriverActions(driver);
    }
    public String getLoginMassega(){
        return driverActions.getText(By.id("flash"));
    }
}
