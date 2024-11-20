package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class SearchPage {
    WebDriver driver;
    DriverActions driverActions;
    public SearchPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions(driver);
    }
    public void searchItem(String itemName){
        driverActions.sendData(By.id("searchBar"),itemName );
    }
    public void clickEnter(){
        driverActions.sendData(By.id("searchBar"),Keys.ENTER,3,200 );
    }
    public String getTextInSearchField(){
       return driver.findElement(By.id("searchBar")).getAttribute("value");
    }
    public void clickSortBy(String sortBy) {
         String sortByLower=sortBy.toLowerCase();
        driverActions.clickOnElement(By.xpath("(//div[@data-qa='select-menu-btn-plp_sort'])[1]"));
         switch (sortByLower){
             case "price low to high":
                 driverActions.clickOnElement(By.cssSelector("li[data-value='price-asc']"));
                break;
             default:
                 break;
         }
    }
    public List<String> getResultsPrice() {
        driverActions.waitForElementToDisappear(By.xpath("//img[@alt='Loading' and @loading='lazy']"),8,200);
        driverActions.waitForTextToBePresentInElement( By.xpath("(//span[@data-qa='select-menu-btn-label'])[1]"),"PRICE: LOW TO HIGH");
        List<String> prices=driverActions.getTextFromMultipleElements(By.className("amount"));
        return prices;
    }
    public List<String> getResultsNames(){
        driverActions.waitForElementToDisappear(By.xpath("//img[@alt='Loading' and @loading='lazy']"),8,200);
        List<String>itemsName=driverActions.getAttributeFromMultipleElements(By.cssSelector("div[data-qa='product-name']"),"title",8,200);
        return itemsName;
    }
    public void clickDell()  {
        driverActions.clickOnElement(By.cssSelector("label[data-qa=brand_DELL]"),5,300);
        driverActions.waitForElementToDisappear(By.xpath("//img[@alt='Loading' and @loading='lazy']"));
        driverActions.waitForElementToBeVisible(By.xpath("//h1[contains(.,'DELL')]"));
    }

    public void returnHome(){
        driverActions.navigateToUrl("https://www.noon.com/egypt-en/");
    }
}
