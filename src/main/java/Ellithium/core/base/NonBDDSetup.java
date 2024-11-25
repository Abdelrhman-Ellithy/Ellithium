package Ellithium.core.base;
import Ellithium.core.execution.listener.CustomTestNGListener;
import Ellithium.config.managment.GeneralHandler;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.annotations.*;
@Listeners({CustomTestNGListener.class, AllureTestNg.class})
public class NonBDDSetup {
    @AfterTest(alwaysRun = true, description = "Test Attachments")
    protected void testEnd(){
        GeneralHandler.AttachLogs();
    }
}