package UI_NonBDD;

import Base.BaseTests;
import org.testng.Assert;
import org.testng.annotations.Test;
public class HoverPageTests extends BaseTests {
    @Test(priority = 1)
    public void isCaptionDisplayed()  {
        var hoverPage=home.clickHover();
        hoverPage.hoverOverFigure(1);
        boolean expectedResult=true;
        boolean actualResult=hoverPage.isCaptionDisplayed();
        Assert.assertEquals(actualResult,expectedResult,"Caption Not Displayed");
    }
    @Test(priority = 2, dependsOnMethods = {"isCaptionDisplayed"})
    public void correctLinkNavigated() {
        var hoverPage=home.clickHover();
        hoverPage.hoverOverFigure(1);
        String  expectedResult="https://the-internet.herokuapp.com/users/1";
        String actualResult=hoverPage.getLink();
        Assert.assertTrue(actualResult.contains(expectedResult),"Incorrect Link");
    }
    @Test(priority = 2, dependsOnMethods = {"isCaptionDisplayed"})
    public void correctTitleDisplayed() {
        var hoverPage=home.clickHover();
        hoverPage.hoverOverFigure(1);
        String  expectedResult="name: user1";
        String actualResult=hoverPage.getTitle();
        Assert.assertTrue(actualResult.contains(expectedResult),"Incorrect Link");
    }
}
