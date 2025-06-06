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
        driverActions.elements().clickOnElement(By.partialLinkText("Form Authentication"));
        return new LoginPage(driver);
    }
    public AlertsPage clickAlerts(){
        returnHome();
        driverActions.elements().clickOnElement(By.partialLinkText("JavaScript Alerts"));
        return new AlertsPage(driver);
    }
    public DropDownPage clickDropDown(){
        returnHome();
        driverActions.elements().clickOnElement(By.partialLinkText("Dropdown"));
        return new DropDownPage(driver);

    }
    public HoverPage clickHover(){
        returnHome();
        driverActions.elements().clickOnElement(By.partialLinkText("Hovers"));
        return new HoverPage(driver);
    }
    public HorizontalSliderPage clickHorizontalSlider(){
        returnHome();
        driverActions.elements().clickOnElement(By.partialLinkText("Horizontal Slider"));
        return new HorizontalSliderPage(driver);
    }
    public DragDropPage clickDragDrop(){
        returnHome();
        driverActions.elements().clickOnElement(By.partialLinkText("Drag and Drop"));
        return new DragDropPage(driver);
    }
    public DynamicLoadingPage clickDynamicLoading(){
        returnHome();
        driverActions.elements().clickOnElement(By.partialLinkText("Dynamic Loading"));
        return new DynamicLoadingPage(driver);
    }
    private void returnHome(){
        driverActions.navigation().navigateToUrl(homeUrl);
    }
}
