package Pages;

import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import java.util.List;

public class AmazonSearchPage {

    private WebDriver driver;

    private DriverActions driverActions;

    private final By searchBar = By.id("search");

    private final By searchBtn = By.tagName("searchbutton");

    private final By searchItemName = By.cssSelector("h2 span");

    private final By searchItemPrice = By.cssSelector(".a-price");

    public AmazonSearchPage(WebDriver driver) {
        this.driver = driver;
        driverActions = new DriverActions<>(driver);
        driverActions.navigation().navigateToUrl(JsonHelper.getJsonKeyValue("src/test/resources/TestData/TestData.json", "baseUrl"));
    }

    public void searchForItem(String itemName) {
        driverActions.elements().sendData(searchBar, itemName);
    }

    public void clickSearch() {
        driverActions.elements().clickOnElement(searchBtn);
    }

    public List<String> getItemsNames() {
        return driverActions.elements().getTextFromMultipleElements(searchItemName);
    }

    public List<String> getItemsPrices() {
        return driverActions.elements().getTextFromMultipleElements(searchItemPrice);
    }
}
