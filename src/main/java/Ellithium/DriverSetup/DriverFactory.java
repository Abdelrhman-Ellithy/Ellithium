package Ellithium.DriverSetup;
import Ellithium.com.CucumberDefaultHooks;
import org.openqa.selenium.WebDriver;
public class DriverFactory {
    public static WebDriver getDriver(){
       return CucumberDefaultHooks.getDriver();
    }
}
