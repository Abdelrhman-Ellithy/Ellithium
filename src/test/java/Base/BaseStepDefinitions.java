package Base;
import Ellithium.core.driver.*;
import org.openqa.selenium.WebDriver;

public class BaseStepDefinitions {
    protected WebDriver driver;
    public BaseStepDefinitions(){
        driver= DriverFactory.getNewLocalDriver(LocalDriverType.Chrome, HeadlessMode.False, PrivateMode.True);
    }
}