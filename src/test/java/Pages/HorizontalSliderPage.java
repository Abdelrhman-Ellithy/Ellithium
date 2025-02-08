package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class HorizontalSliderPage {
    WebDriver driver;
    public HorizontalSliderPage(WebDriver driver){
        this.driver=driver;
    }

    /**
     * @param number strats at 0 ends at 5
     */
    public float moveSliderTo(float number) {
        long timeout=0;
        WebElement range=driver.findElement(By.id("range"));
        WebElement slider=driver.findElement(By.tagName("input"));
        while((Float.valueOf(range.getText())!=0) &&(timeout<5000) ){
            slider.sendKeys(Keys.ARROW_LEFT);
            timeout++;
        }
        timeout=0;
        while( (Float.valueOf(range.getText())!=number) &&(timeout<5000)){
            slider.sendKeys(Keys.ARROW_RIGHT);
            timeout++;
        }
        return Float.valueOf(range.getText());
    }

}
