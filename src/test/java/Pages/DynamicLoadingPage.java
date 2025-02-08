package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DynamicLoadingPage {

    WebDriver driver;
    DriverActions driverActions;
    public DynamicLoadingPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(driver);
    }
    /**
     * @param index starts at 1 end at 2
     */
    public LoadingExample1 clickExample(int index) {
        switch (index){
            case 2 :
                driverActions.clickOnElement(By.partialLinkText("Example 2"));
                return new LoadingExample1(driver) ;
            default:
                driverActions.clickOnElement(By.partialLinkText("Example 1"));
                return new LoadingExample1(driver);
        }
    }
}
