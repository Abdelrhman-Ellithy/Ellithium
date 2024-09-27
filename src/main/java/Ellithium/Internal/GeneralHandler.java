package Ellithium.Internal;

import Ellithium.Utilities.PropertyHelper;
import Ellithium.Utilities.logsUtils;
import com.google.common.io.Files;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

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
    public static void attachAndOpen(){
        String logFilePath = PropertyHelper.getDataFromProperties(
                "src" + File.separator + "main" + File.separator + "resources" + File.separator +
                        "properties" + File.separator + "default" + File.separator + "log4j2",
                "property.basePath"
        );
        logFilePath = logFilePath.concat(File.separator).concat(
                PropertyHelper.getDataFromProperties(
                        "src" + File.separator + "main" + File.separator + "resources" + File.separator +
                                "properties" + File.separator + "default" + File.separator + "log4j2",
                        "property.fileName"
                )
        );
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            logsUtils.error("Log file not found at: " + logFilePath);
            Allure.step("Log file not found: " + logFilePath, Status.FAILED);
            return;
        }
        try (FileInputStream fis = new FileInputStream(logFile)) {
            String uuid = UUID.randomUUID().toString();
            TestResult result = new TestResult()
                    .setUuid(uuid)
                    .setName("Full Execution Log")
                    .setDescription("This is the execution log for the entire test run.")
                    .setStatus(Status.PASSED);
            Allure.getLifecycle().scheduleTestCase(result);
            Allure.getLifecycle().startTestCase(uuid);
            // Attach the log file
            Allure.addAttachment("Execution Log File", "text/plain", fis, ".log");
            Allure.getLifecycle().stopTestCase(uuid);
            Allure.getLifecycle().writeTestCase(uuid);  // Write the test case in the Allure report
            logsUtils.info("Log file successfully attached to the Allure report.");
        } catch (IOException e) {
            logsUtils.logException(e);
            Allure.step("Failed to attach log file: " + e.getMessage(), Status.FAILED);
        }
        AllureHelper.allureOpen();
    }
}
