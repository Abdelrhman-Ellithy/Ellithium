package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class NoonSearchPage {
    WebDriver driver;
    DriverActions driverActions;
    public NoonSearchPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions(driver);
    }
    public void searchItem(String itemName){
        driverActions.elements().sendData(By.name("site-search"),itemName );
    }
    public void clickEnter(){
        driverActions.elements().sendData(By.name("site-search"),Keys.ENTER,3,200 );
    }
    public String getTextInSearchField(){
       return driver.findElement(By.name("site-search")).getAttribute("value");
    }
    public void clickSortBy(String sortBy) {
         String sortByLower=sortBy.toLowerCase();
        driverActions.elements().clickOnElement(By.xpath("(//div[@data-qa='select-menu-btn-plp_sort'])[1]"));
         switch (sortByLower){
             case "price low to high":
                 driverActions.elements().clickOnElement(By.cssSelector("li[data-value='price-asc']"));
                break;
             default:
                 break;
         }
    }
    public List<String> getResultsPrice() {
        driverActions.waits().waitForElementToDisappear(By.xpath("//img[@alt='Loading' and @loading='lazy']"),8,200);
        driverActions.waits().waitForTextToBePresentInElement( By.xpath("(//span[@data-qa='select-menu-btn-label'])[1]"),"PRICE: LOW TO HIGH");
        List<String> prices=driverActions.elements().getTextFromMultipleElements(By.className("amount"));
        return prices;
    }
    public List<String> getResultsNames(){
        driverActions.waits().waitForElementToDisappear(By.xpath("//img[@alt='Loading' and @loading='lazy']"),8,200);
        List<String>itemsName=driverActions.elements().getAttributeFromMultipleElements(By.cssSelector("div[data-qa='product-name']"),"title",8,200);
        return itemsName;
    }
    public void clickDell()  {
        driverActions.elements().clickOnElement(By.cssSelector("label[data-qa=brand_DELL]"),5,300);
        driverActions.waits().waitForElementToDisappear(By.xpath("//img[@alt='Loading' and @loading='lazy']"));
        driverActions.waits().waitForElementToBeVisible(By.xpath("//h1[contains(.,'DELL')]"));
    }

    public void returnHome(){
        driverActions.navigation().navigateToUrl("https://www.noon.com/egypt-en/");
    }
}
