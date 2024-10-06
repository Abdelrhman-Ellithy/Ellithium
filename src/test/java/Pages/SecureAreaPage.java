package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static Ellithium.Utilities.browser.DriverActions.getText;
public class SecureAreaPage {
    WebDriver driver;
    public SecureAreaPage(WebDriver Driver){
        driver=Driver;
    }
    public String getLoginMassega(){
        return getText(driver,By.id("flash"));
    }
}
