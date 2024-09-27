package Ellithium.DriverSetup;

import Ellithium.Internal.ConfigContext;
import Ellithium.Internal.CustomTestNGListener;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
@Listeners({CustomTestNGListener.class, AllureTestNg.class})
public class NonBDDSetup {
    @Parameters({"BrowserName","HeadlessMode","PageLoadStrategy","PrivateMode","SandboxMode", "WebSecurityMode" })
    @BeforeTest(alwaysRun = true)
    protected void setUp(@Optional("Chrome") String BrowserName, @Optional("false") String HeadlessMode, @Optional("Normal") String PageLoadStrategy, @Optional("True") String PrivateMode, @Optional("Sandbox") String SandboxMode, @Optional("True") String WebSecurityMode) {
        ConfigContext.setConfig(BrowserName,HeadlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
    }
}
