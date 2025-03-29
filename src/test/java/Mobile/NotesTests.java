package Mobile;

import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.MobileDriverType;
import Ellithium.core.driver.RemoteDriverType;
import Pages.NotesHomePage;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class NotesTests {
    NotesHomePage notesHomePage;
    @BeforeClass
    public void setUp() throws MalformedURLException {
        UiAutomator2Options options=new UiAutomator2Options();
        options.setDeviceName("Xiaomi Redmi Note 8");
        options.setAppPackage("com.miui.notes");
        options.setAppActivity("com.miui.notes.ui.NotesListActivity");
        options.setCapability("appium:platformName","android");
        options.setCapability("appium:platformName","android");
        options.setCapability("appium:automationName", "UIAutomator2");
        options.setCapability("appium:noReset", true);
        options.setCapability("appium:fullReset", false);
        DriverFactory.getNewMobileDriver(MobileDriverType.Android,new URL("http://127.0.0.1:4723"),options);
        notesHomePage=new NotesHomePage(DriverFactory.getCurrentDriver());
    }
    @Test
    public void creteNote(){
        var notePage=notesHomePage.addNote();
        String expectedNote="Automated Successfully";
        String expectedTitle="Automation Note";
        notePage.setTitle(expectedTitle);
        notePage.typeNote(expectedNote);
        AssertionExecutor.hard.assertEquals(expectedNote,notePage.getNote());
        AssertionExecutor.hard.assertEquals(expectedTitle,notePage.getTitle());
    }
    @AfterClass
    public void tareDown(){
        DriverFactory.quitDriver();
    }
}
