package Tests;

import Base.BaseTests;
import Pages.DropDownPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DropDownTests extends BaseTests {
    DropDownPage dropDownPage;
    @Test
    public void TC_checkSelected() throws InterruptedException {
        DropDownPage dropdownPage= home.clickDropDown();
        String option="Option 1";
        dropdownPage.dropDownSelect(option);
        String actualSelected=dropdownPage.dropDownGetSelected();
        Assert.assertEquals(option, actualSelected, "Not Correctly selected");
    }
}
