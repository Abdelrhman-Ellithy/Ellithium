package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoadingExample1 {
    WebDriver driver;
    DriverActions driverActions;
    public LoadingExample1(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(driver);
    }
    public void clickStartBtn()  {
        driverActions.elements().clickOnElement(By.tagName("button"));
    }
    public String getText(){
        return driverActions.elements().getText(By.id("finish"));
    }
}
