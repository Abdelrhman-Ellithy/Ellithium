package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.File;
import java.time.Duration;
/**
 * Provides a comprehensive set of WebDriver interaction methods with built-in waits and reporting.
 * @param <T> The specific WebDriver type
 */
public class DriverActions<T extends WebDriver> extends BaseActions<T> {
    /**
     * Creates a new DriverActions instance.
     * @param driver WebDriver instance to wrap
     */
    @SuppressWarnings("unchecked")
    public DriverActions(T driver) {
        super(driver);
    }

    /**
     * Creates a WebDriverWait instance with specified timeout.
     * @param timeout Maximum wait time in seconds
     * @return WebDriverWait instance
     */
    public WebDriverWait generalWait(int timeout) {
        Reporter.log("Getting general Wait For "+ timeout + " seconds", LogLevel.INFO_BLUE);
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    /**
     * Sets the implicit wait timeout for the WebDriver.
     * @param timeout Maximum wait time in seconds
     */
    public  void setImplicitWait( int timeout) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
    }

    /**
     * Captures a screenshot and saves it with the specified name.
     * @param screenshotName The name of the screenshot file
     * @return The saved screenshot file
     */
    public  File captureScreenshot( String screenshotName) {
        try {
            TakesScreenshot camera = (TakesScreenshot) driver;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            File screenShotFolder = new File("Test-Output" + File.separator + "ScreenShots" + File.separator + "Captured" + File.separator);
            if (!screenShotFolder.exists()) {
                screenShotFolder.mkdirs();
            }
            String name=screenshotName + "-" + TestDataGenerator.getTimeStamp();
            File screenShotFile = new File(screenShotFolder.getPath() + File.separator + name + ".png");
            Files.move(screenshot, screenShotFile);
            Reporter.log("Screenshot captured: " + screenShotFile.getPath(), LogLevel.INFO_BLUE);
            Reporter.attachScreenshotToReport(screenShotFile,name,"Captured Screenshot");
            return screenShotFile;
        } catch (Exception e) {
            Reporter.log("Failed to capture screenshot: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    public JavaScriptActions JSActions(){
        return new JavaScriptActions<>(driver);
    }
    public Sleep sleep(){
        return new Sleep();
    }
    public AlertActions alerts() {
        return new AlertActions<>(driver);
    }

    public FrameActions frames() {
        return new FrameActions<>(driver);
    }

    public WindowActions windows() {
        return new WindowActions<>(driver);
    }

    public WaitActions waits() {
        return new WaitActions<>(driver);
    }
    public ElementActions elements() {
        return new ElementActions<>(driver);
    }

    public SelectActions select() {
        return new SelectActions<>(driver);
    }

    public NavigationActions navigation() {
        return new NavigationActions<>(driver);
    }

    public MouseActions mouse() {
        return new MouseActions<>(driver);
    }
}