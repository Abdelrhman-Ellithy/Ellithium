package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
public class HoverPage {
    WebDriver driver;
    private DriverActions driverActions;
    private String locator;
    public HoverPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions(driver);
    }

    /**
     * @param index starts at 1 ends at 3
     */
    public void hoverOverFigure(int index){
        locator="(//div[@class='figure'])["+ Integer.toString(index)+ "]";
        driverActions.hoverOverElement(By.xpath(locator));
    }
    public String getTitle(){
        return driverActions.findWebElement(By.xpath(locator))
                .findElement(By.className("figcaption"))
                .findElement(By.tagName("h5")).getText();
    }
    public String getLink(){
        String path= driverActions.findWebElement(By.xpath(locator))
                .findElement(By.className("figcaption"))
                .findElement(By.tagName("a")).getAttribute("href");
        return "https://the-internet.herokuapp.com/"+path;
    }
    public boolean isCaptionDisplayed(){
        return driverActions.findWebElement(By.xpath(locator)).findElement(By.xpath(locator))
                .findElement(By.className("figcaption"))
                .findElement(By.tagName("a")).isDisplayed();
    }
}
