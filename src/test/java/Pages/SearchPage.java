package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static Ellithium.Utilities.browser.DriverActions.*;

public class SearchPage {
    WebDriver driver;
    public SearchPage(WebDriver driver){
        this.driver=driver;
    }
    public void searchItem(String itemName){
        sendData(driver,By.id("searchBar"),itemName );
    }
    public void clickEnter(){
        sendData(driver,By.id("searchBar"),Keys.ENTER,3,200 );
    }
    public String getTextInSearchField(){
       return driver.findElement(By.id("searchBar")).getAttribute("value");
    }
    public void clickSortBy(String sortBy) {
         String sortByLower=sortBy.toLowerCase();
         clickOnElement(driver,By.xpath("(//div[@data-qa='select-menu-btn-plp_sort'])[1]"));
         switch (sortByLower){
             case "price low to high":
                 clickOnElement(driver,By.cssSelector("li[data-value='price-asc']"));
                break;
             default:
                 break;
         }
    }
    public List<String> getResultsPrice() {
        waitForElementToDisappear(driver,By.xpath("//img[@alt='Loading' and @loading='lazy']"),8,200);
        waitForTextToBePresentInElement(driver, By.xpath("(//span[@data-qa='select-menu-btn-label'])[1]"),"PRICE: LOW TO HIGH");
        List<String> prices=getTextFromMultipleElements(driver,By.className("amount"));
        return prices;
    }
    public List<String> getResultsNames(){
        waitForElementToDisappear(driver,By.xpath("//img[@alt='Loading' and @loading='lazy']"),8,200);
        List<String>itemsName=getAttributeFromMultipleElements(driver,By.cssSelector("div[data-qa='product-name']"),"title",8,200);
        return itemsName;
    }
    public void clickDell()  {
        clickOnElement(driver,By.cssSelector("label[data-qa=brand_DELL]"),5,300);
        waitForElementToDisappear(driver,By.xpath("//img[@alt='Loading' and @loading='lazy']"));
        waitForElementToBeVisible(driver,By.xpath("//h1[contains(.,'DELL')]"));
    }

    public void returnHome(){
        navigateToUrl(driver,"https://www.noon.com/egypt-en/");
    }
}
