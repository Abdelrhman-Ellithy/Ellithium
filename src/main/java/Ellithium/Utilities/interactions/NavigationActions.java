package Ellithium.Utilities.interactions;

import org.openqa.selenium.WebDriver;

public class NavigationActions<T extends WebDriver> extends BaseActions<T> {
    
    public NavigationActions(T driver) {
        super(driver);
    }

    /**
     * Navigates to the specified URL.
     * @param url The URL to navigate to
     */
    public void navigateToUrl(String url) {
        driver.get(url);
    }

    /**
     * Refreshes the current page.
     */
    public void refreshPage() {
        driver.navigate().refresh();
    }

    /**
     * Navigates back to the previous page.
     */
    public void navigateBack() {
        driver.navigate().back();
    }

    /**
     * Navigates forward to the next page.
     */
    public void navigateForward() {
        driver.navigate().forward();
    }
}
