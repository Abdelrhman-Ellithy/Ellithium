package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class HorizontalSliderPage {
    WebDriver driver;
    DriverActions driverActions;
    public HorizontalSliderPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(driver);
    }

    /**
     * @param number strats at 0 ends at 5
     */
    public float moveSliderTo(float number) {
        long timeout=0;
        driverActions.mouse().moveSliderTo(By.tagName("input"),By.id("range"),number);
        return Float.valueOf(driverActions.elements().getText(By.id("range")));
    }

}
