package UI_NonBDD;

import Base.BaseTests;
import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.driver.*;
import Pages.HomPage;
import Pages.LoginPage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class loginTestsParallel {
    //ThreadLocal<ScreenRecorderActions> screenRecorderActions = new ThreadLocal<>();
    @DataProvider(name= "invalidLoginData")
            Object[][] getInvalidTestData(){
        return new Object[][]{
                {"tomsmith","hamada","Your password is invalid"},
                {"hamada","SuperSecretPassword!","Your username is invalid"}
        };
    }
    @Test( dataProvider = "invalidLoginData")
    public void invalidLogin(String username, String password, String expectedMessage){
        var home=new HomPage(DriverFactory.getCurrentDriver());
        LoginPage login = home.clickFormAuthentication();
        login =home.clickFormAuthentication();
        login.setUserName(username);
        login.setPassword(password);
        var secureAreaPage=login.clickLoginBtn();
        String actualMessage=secureAreaPage.getLoginMessage();
        AssertionExecutor.hard.assertTrue(actualMessage.contains(expectedMessage));
//        var path =screenRecorderActions.get().stopRecording();
//        Reporter.attachFileToReport(path, "valid login", "valid login");
    }
    @Test()
    public void validLogin() {
        var home=new HomPage(DriverFactory.getCurrentDriver());
        LoginPage login = home.clickFormAuthentication();
        login.setPassword("SuperSecretPassword!");
        login.setUserName("tomsmith");
        var secureAreaPage=login.clickLoginBtn();
        String actualMessage=secureAreaPage.getLoginMessage();
        String expectedMessage="You logged into a secure area!";
        AssertionExecutor.hard.assertTrue(actualMessage.contains(expectedMessage));
//        var path =screenRecorderActions.get().stopRecording();
//        Reporter.attachFileToReport(path, "valid login", "valid login");
    }
    @BeforeMethod
    public void create(){
        LocalDriverConfig driverConfig=new LocalDriverConfig(LocalDriverType.Chrome,
                HeadlessMode.False, PrivateMode.False,
                PageLoadStrategyMode.Normal,
                WebSecurityMode.SecureMode,
                SandboxMode.Sandbox);
        DriverFactory.getNewDriver(driverConfig);
//        screenRecorderActions.set(new ScreenRecorderActions<>(DriverFactory.getCurrentDriver()));
//        screenRecorderActions.get().startRecording(TestDataGenerator.getRandomFullName());
    }
    @AfterMethod
    public void attach(){
        //screenRecorderActions.remove();
        DriverFactory.quitDriver();
    }
}
