package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

public class NotesHomePage {
    private AndroidDriver driver;
    private DriverActions driverActions;
    public NotesHomePage(AndroidDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(this.driver);
    }

    public NotePage addNote(){
        driverActions.clickOnElement(AppiumBy.id("com.miui.notes:id/content_add"));
        return new NotePage(driver);
    }
}
