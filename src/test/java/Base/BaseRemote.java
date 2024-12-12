package Base;

import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import Ellithium.core.driver.RemoteDriverType;
import Pages.HomPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.net.MalformedURLException;
import java.net.URL;

public class BaseRemote {
   protected WebDriver driver;
   protected HomPage home;
    @BeforeClass
    public void Setup() throws MalformedURLException {
        DesiredCapabilities capabilities=new DesiredCapabilities();
        driver= DriverFactory.getNewRemoteDriver(RemoteDriverType.REMOTE_Chrome ,new URL("http://localhost:4444/wd/hub"),capabilities);
        home=new HomPage(driver);
    }
    @AfterClass
    public void tareDown(){
        driver.quit();;
    }
}
