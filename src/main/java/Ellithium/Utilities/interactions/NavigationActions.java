package Ellithium.Utilities.interactions;

import Ellithium.core.ai.scoring.ElementVectorCache;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

public class NavigationActions<T extends WebDriver> extends BaseActions<T> {

    public NavigationActions(T driver) {
        super(driver);
    }

    @Step("Navigate to URL: {url}")
    public void navigateToUrl(String url) {
        driver.get(url);
        ElementVectorCache.getInstance().invalidate();
    }

    @Step("Refresh page")
    public void refreshPage() {
        driver.navigate().refresh();
        ElementVectorCache.getInstance().invalidate();
    }

    @Step("Navigate back")
    public void navigateBack() {
        driver.navigate().back();
        ElementVectorCache.getInstance().invalidate();
    }

    @Step("Navigate forward")
    public void navigateForward() {
        driver.navigate().forward();
        ElementVectorCache.getInstance().invalidate();
    }
}
