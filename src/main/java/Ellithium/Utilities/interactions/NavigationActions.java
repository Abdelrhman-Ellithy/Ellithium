package Ellithium.Utilities.interactions;

import Ellithium.Utilities.ai.ElementVectorCache;
import org.openqa.selenium.WebDriver;

public class NavigationActions<T extends WebDriver> extends BaseActions<T> {

    public NavigationActions(T driver) {
        super(driver);
    }

    public void navigateToUrl(String url) {
        driver.get(url);
        ElementVectorCache.getInstance().invalidate();
    }

    public void refreshPage() {
        driver.navigate().refresh();
        ElementVectorCache.getInstance().invalidate();
    }

    public void navigateBack() {
        driver.navigate().back();
        ElementVectorCache.getInstance().invalidate();
    }

    public void navigateForward() {
        driver.navigate().forward();
        ElementVectorCache.getInstance().invalidate();
    }
}
