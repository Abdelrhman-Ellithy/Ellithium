package Pages;

import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class AmazonSearchPage {
    private WebDriver driver;
    private DriverActions driverActions;
    private final By searchBar= By.id("twotabsearchtextbox");
    private final By searchBtn =By.id("nav-search-submit-button");
    private final By searchItemName=By.cssSelector("h2[class ='a-size-base-plus a-spacing-none a-color-base a-text-normal']");
    private final By searchItemPrice=By.className("a-price-whole");
    public AmazonSearchPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(driver);
        driverActions.navigateToUrl(JsonHelper.getJsonKeyValue("src/test/resources/TestData/TestData.json","baseUrl"));
    }
    public void searchForItem(String itemName){
        driverActions.sendData(searchBar, itemName);
    }
    public void clickSearch(){
        driverActions.clickOnElement(searchBtn);
    }
    public List<String> getItemsNames(){
        return driverActions.getAttributeFromMultipleElements(searchItemName,"aria-label");
    }
    public List<String> getItemsPrices(){
        return driverActions.getTextFromMultipleElements(searchItemPrice);
    }
}
