package Runner;

import Ellithium.core.driver.*;
import io.cucumber.java.After;
import io.cucumber.java.Before;
public class Hooks {
    @Before
    public void setUp(){
        DriverFactory.getNewLocalDriver(LocalDriverType.Chrome, HeadlessMode.False, PrivateMode.True, PageLoadStrategyMode.Eager);
    }
    @After
    public void tareDown(){
        DriverFactory.quitDriver();
    }
}
