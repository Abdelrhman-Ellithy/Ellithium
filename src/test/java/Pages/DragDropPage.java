package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
public class DragDropPage {
    WebDriver driver;
    DriverActions driverActions;

    public DragDropPage(WebDriver driver) {
        this.driver = driver;
        driverActions=new DriverActions<>(driver);
    }

    public void dragDropBox(int indexSource, int indexDestination) {
        switch (indexSource) {
            case 1:
                driverActions.dragAndDrop(By.id("column-a"),By.id("column-b"));
                break;
            case 2:
                driverActions.dragAndDrop(By.id("column-b"),By.id("column-a"));
                break;
        }
    }

    public String getBoxText(int index) {
        By parentLocator;

        switch (index) {
            case 1:
                parentLocator = By.id("column-a");
                break;
            case 2:
                parentLocator = By.id("column-b");
                break;
            default:
                return "failed";
        }
        By headerLocator = By.xpath("//*[@id='" + parentLocator.toString().split(": ")[1] + "']/header");
        return driverActions.getText(headerLocator);
    }


}
