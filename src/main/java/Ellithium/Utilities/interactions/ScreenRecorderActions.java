package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import java.util.Base64;
import java.io.FileOutputStream;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.monte.media.FormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;

public class ScreenRecorderActions<T extends WebDriver> extends BaseActions<T> {
    private ScreenRecorder screenRecorder;
    private final String recordingsDirectory;

    public ScreenRecorderActions(T driver) {
        super(driver);
        this.recordingsDirectory = "Test-Output" + File.separator + "Recordings" + File.separator;
        createDirectories();
    }

    private void createDirectories() {
        new File(recordingsDirectory + "Screenshots").mkdirs();
        new File(recordingsDirectory + "Videos").mkdirs();
    }

    /**
     * Captures a screenshot and saves it with the specified name.
     * @param screenshotName The name of the screenshot file
     * @return The saved screenshot file
     */
    public File captureScreenshot(String screenshotName) {
        try {
            TakesScreenshot camera = (TakesScreenshot) driver;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            File screenShotFolder = new File("Test-Output" + File.separator + "ScreenShots" + File.separator + "Captured" + File.separator);
            if (!screenShotFolder.exists()) {
                screenShotFolder.mkdirs();
            }
            String name = screenshotName + "-" + TestDataGenerator.getTimeStamp();
            File screenShotFile = new File(screenShotFolder.getPath() + File.separator + name + ".png");
            Files.move(screenshot, screenShotFile);
            Reporter.log("Screenshot captured: " + screenShotFile.getPath(), LogLevel.INFO_BLUE);
            Reporter.attachScreenshotToReport(screenShotFile, name, "Captured Screenshot");
            return screenShotFile;
        } catch (Exception e) {
            Reporter.log("Failed to capture screenshot: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Starts video recording of the screen.
     * @param videoName Name of the video file
     * @throws Exception if recording fails to start
     */
    public void startRecording(String videoName) throws Exception {
        if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
            // Mobile recording
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).startRecordingScreen();
            } else {
                ((IOSDriver) driver).startRecordingScreen();
            }
            Reporter.log("Started mobile screen recording: " + videoName, LogLevel.INFO_BLUE);
        } else {
            // Web recording using monte media
            File videoFolder = new File("Test-Output" + File.separator + "ScreenShots" + File.separator + "Videos" + File.separator);
            if (!videoFolder.exists()) {
                videoFolder.mkdirs();
            }
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle captureSize = new Rectangle(0, 0, screenSize.width, screenSize.height);
            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();

            Format fileFormat = new Format(MediaTypeKey, FormatKeys.MediaType.FILE, MimeTypeKey, MIME_MP4);
            Format screenFormat = new Format(MediaTypeKey, FormatKeys.MediaType.VIDEO,
                    EncodingKey, "h264",
                    CompressorNameKey, "h264",
                    DepthKey, 24,
                    FrameRateKey, Rational.valueOf(30),
                    QualityKey, 1.0f,
                    KeyFrameIntervalKey, 30 * 60);
            
            screenRecorder = new ScreenRecorder(gc, captureSize, fileFormat, screenFormat, null, null, videoFolder);
            screenRecorder.start();
            Reporter.log("Started web recording: " + videoName, LogLevel.INFO_BLUE);
        }
    }

    /**
     * Stops the video recording and saves it with a timestamp.
     * @param videoName Name of the video file
     * @throws Exception if recording fails to stop
     */
    public void stopRecording(String videoName) throws Exception {
        if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
            String video;
            if (driver instanceof AndroidDriver) {
                video = ((AndroidDriver) driver).stopRecordingScreen();
            } else {
                video = ((IOSDriver) driver).stopRecordingScreen();
            }
            
            byte[] videoData = Base64.getDecoder().decode(video);
            File videoFolder = new File("Test-Output" + File.separator + "ScreenShots" + File.separator + "Videos" + File.separator);
            if (!videoFolder.exists()) {
                videoFolder.mkdirs();
            }
            
            String newName = videoName + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
            File videoFile = new File(videoFolder, newName);
            
            try (FileOutputStream fos = new FileOutputStream(videoFile)) {
                fos.write(videoData);
                Reporter.log("Mobile video recording saved: " + videoFile.getPath(), LogLevel.INFO_BLUE);
            } catch (Exception e) {
                Reporter.log("Failed to save mobile recording: " + e.getMessage(), LogLevel.ERROR);
                throw e;
            }
        } else {
            screenRecorder.stop();
            File videoFolder = new File("Test-Output" + File.separator + "ScreenShots" + File.separator + "Videos" + File.separator);
            File[] files = videoFolder.listFiles();

            if (files != null && files.length > 0) {
                File lastVideo = files[files.length - 1];
                String newName = videoName + "-" + TestDataGenerator.getTimeStamp() + ".mp4";  // Changed extension to .mp4
                File renamedVideo = new File(videoFolder, newName);

                if (lastVideo.renameTo(renamedVideo)) {
                    Reporter.log("Video recording saved: " + renamedVideo.getPath(), LogLevel.INFO_BLUE);
                } else {
                    Reporter.log("Failed to rename video file", LogLevel.ERROR);
                }
            } else {
                Reporter.log("No video file found after recording", LogLevel.ERROR);
            }
        }
    }
}
