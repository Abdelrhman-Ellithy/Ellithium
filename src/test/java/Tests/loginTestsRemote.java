package Tests;
import Base.BaseRemote;
import Pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
public class loginTestsRemote extends BaseRemote {
    @DataProvider(name= "invalidLoginData")
            Object[][] getInvalidTestData(){
        return new Object[][]{
                {"tomsmith","hamada","Your password is invalid"},
                {"hamada","SuperSecretPassword!","Your username is invalid"}
        };
    }
    LoginPage login;
    @Test(priority = 1, dataProvider = "invalidLoginData")
    public void invalidLogin(String username, String password, String expectedMessage){
        login =home.clickFormAuthentication();
        login.setUserName(username);
        login.setPassword(password);
        var secureAreaPage=login.clickLoginBtn();
        String actualMessage=secureAreaPage.getLoginMassega();
        Assert.assertTrue(actualMessage.contains(expectedMessage));
    }
    @Test(priority = 2)
    public void validLogin() {
        login = home.clickFormAuthentication();
        login.setPassword("SuperSecretPassword!");
        login.setUserName("tomsmith");
        var secureAreaPage=login.clickLoginBtn();
        String actualMessage=secureAreaPage.getLoginMassega();
        String expectedMessage="You logged into a secure area!";
        Assert.assertTrue(actualMessage.contains(expectedMessage));
    }
}
