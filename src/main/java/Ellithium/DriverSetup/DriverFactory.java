package Ellithium.DriverSetup;
import Ellithium.Utilities.logsUtils;
import Ellithium.Utilities.Colors;
import Ellithium.com.CucumberDefaultHooks;
import org.openqa.selenium.WebDriver;
public class DriverFactory {
    public static WebDriver getDriver(){
        WebDriver driver = CucumberDefaultHooks.getDriver();
        if(driver!=null){
            logsUtils.info(Colors.GREEN+ "WebDriver Created"+Colors.RESET);
        }
        else {
            logsUtils.error(Colors.RED+ "WebDriver Creation Failed"+Colors.RESET);
        }
       return driver;
    }
}
