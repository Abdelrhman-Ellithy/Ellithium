package AutoEllithiumSphere.DriverSetup;
import AutoEllithiumSphere.com.CucumberDefaultHooks;
import org.openqa.selenium.WebDriver;
public class DriverFactory {
    public static WebDriver getDriver(){
       return CucumberDefaultHooks.getDriver();
    }
}
