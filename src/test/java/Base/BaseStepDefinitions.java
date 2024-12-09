package Base;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.driver.PrivateMode;
import org.openqa.selenium.WebDriver;

public class BaseStepDefinitions {
    protected WebDriver driver;
    public BaseStepDefinitions(){
        driver= DriverFactory.getNewLocalWebDriver(DriverType.Chrome, HeadlessMode.False, PrivateMode.True);
    }
}