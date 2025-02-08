package Tests;

import Base.BaseTests;
import Pages.HorizontalSliderPage;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class HorizontalSliderTests extends BaseTests {
    @Test
    public void testSlider()throws InterruptedException{
       HorizontalSliderPage horizontalSliderPage= home.clickHorizontalSlider();
        float actualResult=horizontalSliderPage.moveSliderTo(4);
        float expectedResult=4;
        SoftAssert softAssert=new SoftAssert();
        softAssert.assertEquals(actualResult,expectedResult,"Slider doesn't move Correctly");
        actualResult=horizontalSliderPage.moveSliderTo(3.5f);
        expectedResult=3.5f;
        softAssert.assertEquals(actualResult,expectedResult,"Slider doesn't move Correctly");
        softAssert.assertAll();
    }
}
