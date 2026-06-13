package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import Ellithium.Utilities.interactions.DriverActions;

public class ProductPage {

    WebDriver driver;

    DriverActions driverActions;

    public ProductPage(WebDriver Driver) {
        driver = Driver;
        driverActions = new DriverActions(driver);
    }

    public void navigateToProducts() {
        driverActions.navigation().navigateToUrl("https://automationexercise.com/");
        By products = By.xpath("//a[normalize-space(.)=' Products']");
        driverActions.elements().clickOnElement(products);
    }
}
