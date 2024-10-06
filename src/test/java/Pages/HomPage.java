package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomPage {
    WebDriver driver;
    private final String homeUrl="https://the-internet.herokuapp.com/";
    public HomPage(WebDriver driver){
        this.driver=driver;
    }
    public LoginPage clickFormAuthentication(){
        returnHome();
        driver.findElement(By.partialLinkText("Form Authentication")).click();
        return new LoginPage(driver);
    }
    private void returnHome(){
        driver.get(homeUrl);
    }
}
