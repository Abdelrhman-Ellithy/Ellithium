package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class ElementActionsRecoveryTest {

    private static final By LOCATOR = By.xpath("//button[contains(@title,'add')]");
    private static final int TIMEOUT = 2;
    private static final int POLLING = 100;

    private WebDriver driver;
    private ElementActions<WebDriver> actions;

    @BeforeMethod
    public void setup() {
        driver = mock(WebDriver.class);
        actions = new ElementActions<>(driver);
    }

    private WebElement button(boolean displayed, boolean enabled) {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(displayed);
        when(el.isEnabled()).thenReturn(enabled);
        when(el.getTagName()).thenReturn("button");
        return el;
    }

    @Test
    public void clickOnElement_reselectsInteractableMatchWhenFirstIsNotInteractable() {
        WebElement firstNonInteractable = button(true, false);
        doThrow(new ElementNotInteractableException("element not interactable"))
                .when(firstNonInteractable).click();
        WebElement realButton = button(true, true);

        when(driver.findElement(LOCATOR)).thenReturn(firstNonInteractable);
        when(driver.findElements(LOCATOR)).thenReturn(List.of(firstNonInteractable, realButton));

        actions.clickOnElement(LOCATOR, TIMEOUT, POLLING);

        verify(realButton).click();
    }
}
