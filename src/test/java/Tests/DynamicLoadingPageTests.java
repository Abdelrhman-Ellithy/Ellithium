package Tests;

import Base.BaseTests;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DynamicLoadingPageTests extends BaseTests {
    @DataProvider(name = "exampleIndex")
    public Object[] [] getExampleIndex(){
        return new Object[][]{
                {1},
                {2}
        };
    }
    @Test(dataProvider = "exampleIndex")
    public void example1TextIsDisplayed(int index) {
        var DynamicloadingPage=home.clickDynamicLoading();
        var loadingExample1Page=DynamicloadingPage.clickExample(index);
        loadingExample1Page.clickStartBtn();
        String expectedResult="Hello World!";
        String actualResult=loadingExample1Page.getText();
        Assert.assertEquals(actualResult,expectedResult,"Element Is not visible");
    }
}
