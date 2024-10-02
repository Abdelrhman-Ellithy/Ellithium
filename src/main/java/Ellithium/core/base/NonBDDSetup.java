package Ellithium.core.base;

import Ellithium.config.managment.ConfigContext;
import Ellithium.core.execution.listener.CustomTestNGListener;
import Ellithium.config.managment.GeneralHandler;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
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
            Reporter.log("Invalid runMode Selection", LogLevel.ERROR);
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