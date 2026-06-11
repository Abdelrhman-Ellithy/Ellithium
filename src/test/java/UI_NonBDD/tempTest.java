package UI_NonBDD;

import org.openqa.selenium.By;
import org.testng.annotations.Test;
import Ellithium.Utilities.interactions.DriverActions;
import Pages.ProductPage;

public class tempTest extends Base.BaseTests {

    DriverActions driverActions;

    @Test
    public void testProducts() {
        driverActions = new DriverActions(driver);
        driverActions.navigation().navigateToUrl("https://automationexercise.com/");
        By products = By.xpath("//a[normalize-space(.)=' Products']");
        driverActions.elements().clickOnElement(products);
    }
}
