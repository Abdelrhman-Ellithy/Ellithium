package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class NotesHomePage {
    private AndroidDriver driver;
    private DriverActions driverActions;
    public NotesHomePage(AndroidDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(this.driver);
    }

    public NotePage addNote(){
        driverActions.elements().clickOnElement(AppiumBy.id("com.miui.notes:id/note_add"));
        return new NotePage(driver);
    }
}
