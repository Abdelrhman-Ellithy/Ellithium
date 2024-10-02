package Ellithium.DriverSetup;

import Ellithium.Internal.ConfigContext;
import Ellithium.Internal.CustomTestNGListener;
import Ellithium.Internal.GeneralHandler;
import Ellithium.Internal.SeleniumListener;
import Ellithium.Utilities.Colors;
import Ellithium.Utilities.logsUtils;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.ITestResult;
import org.testng.annotations.*;

@Listeners({CustomTestNGListener.class, AllureTestNg.class})
public class NonBDDSetup {
    @Parameters({"BrowserName","HeadlessMode","PageLoadStrategy","PrivateMode","SandboxMode", "WebSecurityMode" })
    @BeforeTest(alwaysRun = true)
    protected void BrowserConfig(@Optional("Chrome") String BrowserName, @Optional("false") String HeadlessMode, @Optional("Normal") String PageLoadStrategy, @Optional("True") String PrivateMode, @Optional("Sandbox") String SandboxMode, @Optional("True") String WebSecurityMode) {
        if(!GeneralHandler.getBDDMode()){
            ConfigContext.setConfig(BrowserName,HeadlessMode,PageLoadStrategy,PrivateMode,SandboxMode,WebSecurityMode);
        }
        else{
            logsUtils.error(Colors.RED+ "Invalid runMode Selection"+Colors.RESET);
        }
    }
    @AfterMethod
    public void attachedFailedScreenShot(ITestResult result){
        if(ConfigContext.isLastUIFailed()){
            GeneralHandler.attachScreenshotToReport(ConfigContext.getLastScreenShot(),ConfigContext.getLastScreenShot().getName(),
                    ConfigContext.getBrowserName().toUpperCase(),result.getName());
            ConfigContext.setLastUIFailed(false);
        }
    }
}