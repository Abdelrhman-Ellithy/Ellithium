package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class NoonSearchPage {
    WebDriver driver;
    DriverActions driverActions;
    private final By searchField=By.id("search-input");
    private final By sortBtn=By.cssSelector("button[class*='DesktopSortDropdown_trigger']");
    private final By lowToHigh =By.xpath("//a[contains(text(),'Price: Low to High')]");
    private final By sortBtnText=By.cssSelector("span[class*='DesktopSort_text']");
    private final By elementPrice=By.cssSelector("strong[class*='Price_amount']");
    private final By resultsLoadedHeader=By.cssSelector("div[class*='DesktopListHeader_subTitle']");
    private final By dellBtn=By.xpath("//a[starts-with(@class,'NavPills_navPill') and span[text()='DELL']]");
    private final By elementTitle=By.cssSelector("h2[class*='ProductDetailsSection_title']");
    public NoonSearchPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions(driver);
    }
    public void searchItem(String itemName){
        driverActions.elements().sendData(searchField,itemName );
    }
    public void clickEnter(){
        driverActions.elements().sendData(searchField,Keys.ENTER );
    }
    public String getTextInSearchField(){
       return driver.findElement(searchField).getAttribute("value");
    }
    public void clickSortBy(String sortBy) {
         String sortByLower=sortBy.toLowerCase();
        driverActions.elements().clickOnElement(sortBtn);
         switch (sortByLower){
             case "price low to high":
                 driverActions.elements().clickOnElement(lowToHigh);
                break;
             default:
                 break;
         }
    }
    public List<String> getResultsPrice() {
        driverActions.waits().waitForTextToBePresentInElement(sortBtnText,"Price: Low to High");
        driverActions.waits().waitForElementToBeVisible(resultsLoadedHeader);
        driverActions.sleep().sleepMillis(500);
        List<String> prices=driverActions.elements().getTextFromMultipleElements(elementPrice);
        return prices;
    }
    public List<String> getResultsNames(){
        driverActions.waits().waitForElementToBeVisible(resultsLoadedHeader);
        List<String>itemsName=driverActions.elements().getTextFromMultipleElements(elementTitle);
        return itemsName;
    }
    public void clickDell()  {
        driverActions.elements().clickOnElement(dellBtn);
        driverActions.waits().waitForElementToBeVisible(resultsLoadedHeader);
    }

    public void returnHome(){
        driverActions.navigation().navigateToUrl("https://www.noon.com/egypt-en/");
    }
}
