package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

public class DropDownPage {
    WebDriver driver;
    DriverActions driverActions;
    public DropDownPage(WebDriver driver){
        this.driver=driver;
        driverActions=new DriverActions<>(driver);
    }
    public void dropDownSelect(String option){
        driverActions.select().selectDropdownByText(By.cssSelector("select#dropdown"),option);
    }
    /**
     * @param index starts at 1 ends at 2
     */
    public void dropDownSelect(int index){
        driverActions.select().selectDropdownByIndex(By.cssSelector("select#dropdown"),index-1);
    }
    public String dropDownGetSelected(){
        List<String>texts=driverActions.select().getDropdownSelectedOptions(By.cssSelector("select#dropdown"));
        return texts.get(0);
    }
}
