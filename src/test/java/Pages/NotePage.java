package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;

public class NotePage {
    AndroidDriver driver;
    DriverActions driverActions;
    public NotePage(AndroidDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(this.driver);
    }
    public void typeNote(String note){
        driverActions.sendData(AppiumBy.id("com.miui.notes:id/rich_editor"),note);
        new KeyEvent(AndroidKey.ENTER);
    }
    public void clickDone(){
        driverActions.clickOnElement(AppiumBy.accessibilityId("Done"));
    }
    public void setTitle(String title){
        driverActions.sendData(AppiumBy.id("com.miui.notes:id/note_title"),title);
        driver.pressKey(new KeyEvent(AndroidKey.ENTER));
    }
    public String getTitle(){
        return driverActions.getText(AppiumBy.id("com.miui.notes:id/note_title"));
    }
    public String getNote(){
        return driverActions.getText(AppiumBy.id("com.miui.notes:id/rich_editor"));
    }
}
