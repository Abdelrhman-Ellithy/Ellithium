package Ellithium.core.execution.listener;

import org.mockito.Mockito;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;

public class SeleniumListenerTest {

    private WebDriver driver;

    @BeforeMethod
    public void setUp() {
        driver = Mockito.mock(WebDriver.class,
                Mockito.withSettings().extraInterfaces(JavascriptExecutor.class));
        seleniumListener.stopRecording(); // ensure clean state
    }

    @AfterMethod
    public void tearDown() {
        seleniumListener.stopRecording();
        ListenerLogSuppression.resume();
    }

    // ── Recording state machine ───────────────────────────────────────────

    @Test
    public void isRecording_initially_false() {
        Assert.assertFalse(seleniumListener.isRecording());
    }

    @Test
    public void startRecording_setsRecordingTrue() {
        seleniumListener.startRecording(driver);
        Assert.assertTrue(seleniumListener.isRecording());
    }

    @Test
    public void stopRecording_clearsRecordingFlag() {
        seleniumListener.startRecording(driver);
        seleniumListener.stopRecording();
        Assert.assertFalse(seleniumListener.isRecording());
    }

    @Test
    public void getInteractions_initiallyEmpty() {
        Assert.assertTrue(seleniumListener.getInteractions().isEmpty());
    }

    @Test
    public void stopRecording_returnsSnapshotAndClearsInternal() {
        seleniumListener.startRecording(driver);
        // simulate recording an interaction by calling afterClick
        WebElement el = Mockito.mock(WebElement.class);
        Mockito.when(el.getAccessibleName()).thenReturn("btn");
        new seleniumListener().afterClick(el);

        List<?> snapshot = seleniumListener.stopRecording();
        Assert.assertEquals(snapshot.size(), 1);
        Assert.assertTrue(seleniumListener.getInteractions().isEmpty());
    }

    @Test
    public void startRecording_clearsPreviousInteractions() {
        seleniumListener.startRecording(driver);
        WebElement el = Mockito.mock(WebElement.class);
        Mockito.when(el.getAccessibleName()).thenReturn("x");
        new seleniumListener().afterClick(el);

        seleniumListener.startRecording(driver); // second start clears
        Assert.assertTrue(seleniumListener.getInteractions().isEmpty());
    }

    // ── Logging suppression delegation ───────────────────────────────────

    @Test
    public void suppressLogging_delegatesToListenerLogSuppression() {
        seleniumListener.suppressLogging();
        Assert.assertTrue(ListenerLogSuppression.isSuppressed());
        seleniumListener.resumeLogging();
    }

    @Test
    public void resumeLogging_delegatesToListenerLogSuppression() {
        seleniumListener.suppressLogging();
        seleniumListener.resumeLogging();
        Assert.assertFalse(ListenerLogSuppression.isSuppressed());
    }

    // ── reconstructLocatorExpression (private static, via reflection) ────

    @Test
    public void reconstructLocator_withId_returnsById() throws Exception {
        WebElement el = Mockito.mock(WebElement.class);
        Mockito.when(el.getAttribute("id")).thenReturn("submit-btn");
        Assert.assertEquals(invokeReconstructLocator(el), "By.id(\"submit-btn\")");
    }

    @Test
    public void reconstructLocator_noId_withName_returnsByName() throws Exception {
        WebElement el = Mockito.mock(WebElement.class);
        Mockito.when(el.getAttribute("id")).thenReturn(null);
        Mockito.when(el.getAttribute("name")).thenReturn("username");
        Assert.assertEquals(invokeReconstructLocator(el), "By.name(\"username\")");
    }

    @Test
    public void reconstructLocator_withDataTestId_returnsByCss() throws Exception {
        WebElement el = Mockito.mock(WebElement.class);
        Mockito.when(el.getAttribute("id")).thenReturn(null);
        Mockito.when(el.getAttribute("name")).thenReturn(null);
        Mockito.when(el.getAttribute("data-testid")).thenReturn("login-btn");
        Assert.assertTrue(invokeReconstructLocator(el).contains("data-testid"));
    }

    @Test
    public void reconstructLocator_fallsBackToTagName() throws Exception {
        WebElement el = Mockito.mock(WebElement.class);
        Mockito.when(el.getAttribute("id")).thenReturn(null);
        Mockito.when(el.getAttribute("name")).thenReturn(null);
        Mockito.when(el.getAttribute("data-testid")).thenReturn(null);
        Mockito.when(el.getAttribute("class")).thenReturn(null);
        Mockito.when(el.getTagName()).thenReturn("input");
        String result = invokeReconstructLocator(el);
        Assert.assertTrue(result.contains("input"));
    }

    private static String invokeReconstructLocator(WebElement el) throws Exception {
        Method m = seleniumListener.class.getDeclaredMethod("reconstructLocatorExpression", WebElement.class);
        m.setAccessible(true);
        return (String) m.invoke(null, el);
    }
}
