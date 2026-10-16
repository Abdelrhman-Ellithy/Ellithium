package Ellithium.Utilities.interactions;

import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ElementActionsUnitTest {

    private WebDriver driver;
    private ElementActions<WebDriver> actions;
    private RemoteWebElement mockElement;
    private static final By LOCATOR = By.id("test-element");

    @BeforeMethod
    public void setUp() {
        driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        mockElement = mock(RemoteWebElement.class);
        when(mockElement.getId()).thenReturn("mock-element-001");
        actions = new ElementActions<>(driver);

        WebDriver.TargetLocator targetLocator = mock(WebDriver.TargetLocator.class);
        when(driver.switchTo()).thenReturn(targetLocator);
        when(driver.findElement(LOCATOR)).thenReturn(mockElement);
        when(driver.findElements(LOCATOR)).thenReturn(List.of(mockElement));
        when(mockElement.isDisplayed()).thenReturn(true);
        when(mockElement.isEnabled()).thenReturn(true);
        when(mockElement.getTagName()).thenReturn("input");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        when(js.executeScript(anyString(), any())).thenReturn(Collections.emptyMap());
    }

    // ── Null Locator Guards ──────────────────────────────────────────────

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void sendData_nullLocator_throwsIllegalArgumentException() {
        actions.sendData(null, "text", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void clickOnElement_nullLocator_throwsIllegalArgumentException() {
        actions.clickOnElement(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getText_nullLocator_throwsIllegalArgumentException() {
        actions.getText(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getTextFromMultipleElements_nullLocator_throwsIllegalArgumentException() {
        actions.getTextFromMultipleElements(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getAttributeFromMultipleElements_nullLocator_throwsIllegalArgumentException() {
        actions.getAttributeFromMultipleElements(null, "class", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getPropertyFromMultipleElements_nullLocator_throwsIllegalArgumentException() {
        actions.getPropertyFromMultipleElements(null, "value", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void clickOnMultipleElements_nullLocator_throwsIllegalArgumentException() {
        actions.clickOnMultipleElements(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getAttributeValue_nullLocator_throwsIllegalArgumentException() {
        actions.getAttributeValue(null, "class", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getPropertyValue_nullLocator_throwsIllegalArgumentException() {
        actions.getPropertyValue(null, "value", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadFile_nullLocator_throwsIllegalArgumentException() {
        actions.uploadFile(null, "somePath", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadFile_nullPath_throwsIllegalArgumentException() {
        actions.uploadFile(LOCATOR, null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadFile_blankPath_throwsIllegalArgumentException() {
        actions.uploadFile(LOCATOR, "   ", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadFile_nonexistentFile_throwsIllegalArgumentException() {
        actions.uploadFile(LOCATOR, "non_existent_file_12345.tmp", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadMultipleFiles_nullLocator_throwsIllegalArgumentException() {
        actions.uploadMultipleFiles(null, new String[]{"path"}, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadMultipleFiles_nullArray_throwsIllegalArgumentException() {
        actions.uploadMultipleFiles(LOCATOR, null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadMultipleFiles_emptyArray_throwsIllegalArgumentException() {
        actions.uploadMultipleFiles(LOCATOR, new String[0], 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void uploadMultipleFiles_nullElementInArray_throwsIllegalArgumentException() {
        actions.uploadMultipleFiles(LOCATOR, new String[]{null}, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void isElementPresent_nullLocator_throwsIllegalArgumentException() {
        actions.isElementPresent(null, 1, 100, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void isElementDisplayed_nullLocator_throwsIllegalArgumentException() {
        actions.isElementDisplayed(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void isElementEnabled_nullLocator_throwsIllegalArgumentException() {
        actions.isElementEnabled(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void isElementSelected_nullLocator_throwsIllegalArgumentException() {
        actions.isElementSelected(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void isElementClickable_nullLocator_throwsIllegalArgumentException() {
        actions.isElementClickable(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void clearElement_nullLocator_throwsIllegalArgumentException() {
        actions.clearElement(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void scrollIntoView_nullLocator_throwsIllegalArgumentException() {
        actions.scrollIntoView(null, 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void isTextContains_nullLocator_throwsIllegalArgumentException() {
        actions.isTextContains(null, "sub", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void isAttributeContains_nullLocator_throwsIllegalArgumentException() {
        actions.isAttributeContains(null, "attr", "sub", 1, 100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void isTextEqual_nullLocator_throwsIllegalArgumentException() {
        actions.isTextEqual(null, "expected", 1, 100);
    }

    // ── Functional Unit Tests ──────────────────────────────────────────

    @Test
    public void sendData_clearsAndTypes() {
        actions.sendData(LOCATOR, "sample text", 1, 100);
        verify(mockElement).clear();
        verify(mockElement).sendKeys("sample text");
    }

    @Test
    public void clickOnElement_invokesClick() {
        actions.clickOnElement(LOCATOR, 1, 100);
        verify(mockElement).click();
    }

    @Test
    public void getText_returnsElementText() {
        when(mockElement.getText()).thenReturn("Hello Ellithium");
        String text = actions.getText(LOCATOR, 1, 100);
        Assert.assertEquals(text, "Hello Ellithium");
    }

    @Test
    public void getAttributeValue_returnsDomAttribute() {
        when(mockElement.getDomAttribute("data-custom")).thenReturn("custom-val");
        String val = actions.getAttributeValue(LOCATOR, "data-custom", 1, 100);
        Assert.assertEquals(val, "custom-val");
    }

    @Test
    public void getPropertyValue_returnsDomProperty() {
        when(mockElement.getDomProperty("value")).thenReturn("input-val");
        String val = actions.getPropertyValue(LOCATOR, "value", 1, 100);
        Assert.assertEquals(val, "input-val");
    }

    @Test
    public void isElementPresent_whenFound_returnsTrue() {
        boolean present = actions.isElementPresent(LOCATOR, 1, 100, false);
        Assert.assertTrue(present);
    }

    @Test
    public void isElementDisplayed_returnsUnderlyingState() {
        when(mockElement.isDisplayed()).thenReturn(true);
        Assert.assertTrue(actions.isElementDisplayed(LOCATOR, 1, 100));

        when(mockElement.isDisplayed()).thenReturn(false);
        Assert.assertFalse(actions.isElementDisplayed(LOCATOR, 1, 100));
    }

    @Test
    public void isElementEnabled_returnsUnderlyingState() {
        when(mockElement.isEnabled()).thenReturn(true);
        Assert.assertTrue(actions.isElementEnabled(LOCATOR, 1, 100));

        when(mockElement.isEnabled()).thenReturn(false);
        Assert.assertFalse(actions.isElementEnabled(LOCATOR, 1, 100));
    }

    @Test
    public void isElementSelected_returnsUnderlyingState() {
        when(mockElement.isSelected()).thenReturn(true);
        Assert.assertTrue(actions.isElementSelected(LOCATOR, 1, 100));

        when(mockElement.isSelected()).thenReturn(false);
        Assert.assertFalse(actions.isElementSelected(LOCATOR, 1, 100));
    }

    @Test
    public void isElementClickable_bothDisplayedAndEnabled_returnsTrue() {
        when(mockElement.isDisplayed()).thenReturn(true);
        when(mockElement.isEnabled()).thenReturn(true);
        Assert.assertTrue(actions.isElementClickable(LOCATOR, 1, 100));

        when(mockElement.isEnabled()).thenReturn(false);
        Assert.assertFalse(actions.isElementClickable(LOCATOR, 1, 100));
    }

    @Test
    public void isTextContains_verifiesSubstringAndNullSafety() {
        when(mockElement.getText()).thenReturn("Search Results (42 items)");
        Assert.assertTrue(actions.isTextContains(LOCATOR, "Results", 1, 100));
        Assert.assertFalse(actions.isTextContains(LOCATOR, "Missing", 1, 100));
        Assert.assertFalse(actions.isTextContains(LOCATOR, null, 1, 100));

        when(mockElement.getText()).thenReturn(null);
        Assert.assertFalse(actions.isTextContains(LOCATOR, "Results", 1, 100));
    }

    @Test
    public void isAttributeContains_verifiesAttributeAndNullSafety() {
        when(mockElement.getDomAttribute("class")).thenReturn("btn btn-primary active");
        Assert.assertTrue(actions.isAttributeContains(LOCATOR, "class", "btn-primary", 1, 100));
        Assert.assertFalse(actions.isAttributeContains(LOCATOR, "class", "btn-danger", 1, 100));
        Assert.assertFalse(actions.isAttributeContains(LOCATOR, "class", null, 1, 100));

        when(mockElement.getDomAttribute("class")).thenReturn(null);
        Assert.assertFalse(actions.isAttributeContains(LOCATOR, "class", "btn", 1, 100));
    }

    @Test
    public void isTextEqual_verifiesExactMatchAndNullSafety() {
        when(mockElement.getText()).thenReturn("Exact Match");
        Assert.assertTrue(actions.isTextEqual(LOCATOR, "Exact Match", 1, 100));
        Assert.assertFalse(actions.isTextEqual(LOCATOR, "Different", 1, 100));
        Assert.assertFalse(actions.isTextEqual(LOCATOR, null, 1, 100));

        when(mockElement.getText()).thenReturn(null);
        Assert.assertTrue(actions.isTextEqual(LOCATOR, null, 1, 100));
    }

    @Test
    public void clearElement_clearsElement() {
        actions.clearElement(LOCATOR, 1, 100);
        verify(mockElement).clear();
    }

    @Test
    public void uploadFile_validFile_sendsAbsolutePath() throws IOException {
        File temp = File.createTempFile("test-upload", ".txt");
        try {
            actions.uploadFile(LOCATOR, temp.getAbsolutePath(), 1, 100);
            verify(mockElement).sendKeys(temp.getAbsolutePath());
        } finally {
            temp.delete();
        }
    }

    @Test
    public void uploadMultipleFiles_validFiles_sendsJoinedPaths() throws IOException {
        File temp1 = File.createTempFile("test-upload-1", ".txt");
        File temp2 = File.createTempFile("test-upload-2", ".txt");
        try {
            actions.uploadMultipleFiles(LOCATOR, new String[]{temp1.getAbsolutePath(), temp2.getAbsolutePath()}, 1, 100);
            String expected = temp1.getAbsolutePath() + "\n" + temp2.getAbsolutePath();
            verify(mockElement).sendKeys(expected);
        } finally {
            temp1.delete();
            temp2.delete();
        }
    }
}
