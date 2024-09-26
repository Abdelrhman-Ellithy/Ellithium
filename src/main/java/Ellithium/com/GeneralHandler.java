package Ellithium.com;

import Ellithium.Utilities.logsUtils;
import com.google.common.io.Files;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GeneralHandler {
    public static void testFailed(WebDriver localDriver, String browserName, String testName){
        try {
            TakesScreenshot camera = (TakesScreenshot) localDriver;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            File screenShotFile = new File("Test-Output/ScreenShots/Failed/" + browserName + testName + ".png");
            Files.move(screenshot, screenShotFile);
            try (FileInputStream fis = new FileInputStream(screenShotFile)) {
                Allure.description(browserName.toUpperCase()+ "-" + testName+ " FAILED");
                Allure.addAttachment(browserName.toUpperCase() + "-" + testName, "image/png", fis, ".png");
            }
        } catch (IOException e) {
            logsUtils.logException(e);
        }
    }
}
