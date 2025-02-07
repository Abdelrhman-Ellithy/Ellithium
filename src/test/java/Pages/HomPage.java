package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomPage {
    WebDriver driver;
    DriverActions driverActions;
    private final String homeUrl="https://the-internet.herokuapp.com/";
    public HomPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(driver);
    }
    public LoginPage clickFormAuthentication(){
        returnHome();
        driverActions.clickOnElement(By.partialLinkText("Form Authentication"));
        return new LoginPage(driver);
    }
    public AlertsPage clickAlerts(){
        returnHome();
        driverActions.clickOnElement(By.partialLinkText("JavaScript Alerts"));
        return new AlertsPage(driver);
    }
    private void returnHome(){
        driverActions.navigateToUrl(homeUrl);
    }
}
