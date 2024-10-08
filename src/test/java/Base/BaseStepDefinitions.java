package Base;
import Ellithium.core.driver.DriverFactory;
import org.openqa.selenium.WebDriver;

public class BaseStepDefinitions {
    protected WebDriver driver;
    public BaseStepDefinitions(){
        driver= DriverFactory.getNewDriver();
    }
}