package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import org.monte.media.Format;
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
import static org.monte.media.FormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;

/**
 * Handles screen recording and screenshot functionality for web and mobile testing.
 * Supports both web browser recording using Monte Media Library and mobile device recording.
 * @param <T> Type of WebDriver being used (can be web or mobile driver)
 */
public class ScreenRecorderActions<T extends WebDriver> extends BaseActions<T> {
    /**
     * Thread-safe storage for screen recorder instances
     */
    private static final ThreadLocal<ScreenRecorder> screenRecorder = new ThreadLocal<>();
    
    /**
     * Thread-safe storage for video names
     */
    private static final ThreadLocal<String> videoName = new ThreadLocal<>();
    
    /**
     * Creates a new ScreenRecorderActions instance.
     * @param driver WebDriver instance to use for recording/screenshots
     */
    public ScreenRecorderActions(T driver) {
        super(driver);
    }

    /**
     * Captures a screenshot of the current browser window or mobile screen.
     * Saves the screenshot with timestamp and attaches it to the test report.
     * @param screenshotName Base name for the screenshot file
     * @return File object of saved screenshot, null if capture fails
     */
    public File captureScreenshot(String screenshotName) {
        try {
            TakesScreenshot camera = (TakesScreenshot) driver;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            File screenShotFolder = new File(ConfigContext.getCapturedScreenShotPath() + File.separator);
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
     * Initiates screen recording with the specified name.
     * For web browsers: Uses Monte Media Library
     * For mobile: Uses native device recording capabilities
     * @param name Base name for the video file
     */
    public void startRecording(String name) {
        videoName.set(name);
        if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).startRecordingScreen();
            } else {
                ((IOSDriver) driver).startRecordingScreen();
            }
            Reporter.log("Started mobile screen recording: " + name, LogLevel.INFO_BLUE);
        } else {
            File videoFolder = new File(ConfigContext.getRecordedExecutionsPath()+ File.separator);
            if (!videoFolder.exists()) {
                videoFolder.mkdirs();
            }
            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
            Rectangle captureSize = new Rectangle(0, 0, 
                gc.getBounds().width, 
                gc.getBounds().height);
            
            // Updated format configuration
            Format fileFormat = new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI);
            Format screenFormat = new Format(MediaTypeKey, MediaType.VIDEO,
                    EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                    CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                    DepthKey, 24,
                    FrameRateKey, Rational.valueOf(15),
                    QualityKey, 0.6f,
                    KeyFrameIntervalKey, 15 * 60);
            Format mouseFormat = new Format(MediaTypeKey, MediaType.VIDEO,
                    EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                    FrameRateKey, Rational.valueOf(30));

            try {
                ScreenRecorder recorder = new ScreenRecorder(gc, captureSize,
                        fileFormat, screenFormat, mouseFormat, null, videoFolder);
                screenRecorder.set(recorder);
                recorder.start();
                Reporter.log("Started web recording: " + name, LogLevel.INFO_BLUE);
            } catch (Exception e) {
                Reporter.log("Failed to start recording: " + e.getMessage(), LogLevel.ERROR);
            }
        }
    }

    /**
     * Stops the current recording and saves it with timestamp.
     * Handles both web and mobile recording scenarios.
     * Automatically cleans up ThreadLocal resources after completion.
     */
    public void stopRecording() {
        String name = videoName.get();
        if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
            String video;
            if (driver instanceof AndroidDriver) {
                video = ((AndroidDriver) driver).stopRecordingScreen();
            } else {
                video = ((IOSDriver) driver).stopRecordingScreen();
            }
            byte[] videoData = Base64.getDecoder().decode(video);
            File videoFolder = new File(ConfigContext.getRecordedExecutionsPath() + File.separator);
            if (!videoFolder.exists()) {
                videoFolder.mkdirs();
            }
            String newName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
            File videoFile = new File(videoFolder, newName);
            try (FileOutputStream fos = new FileOutputStream(videoFile)) {
                fos.write(videoData);
                Reporter.log("Mobile video recording saved: " + videoFile.getPath(), LogLevel.INFO_BLUE);
            } catch (Exception e) {
                Reporter.log("Failed to save mobile recording: " + e.getMessage(), LogLevel.ERROR);
            }
        } else {
            File videoFolder = new File(ConfigContext.getRecordedExecutionsPath() + File.separator);
            if (!videoFolder.exists()) {
                videoFolder.mkdirs();
            }
            try {
                screenRecorder.get().stop();
                java.util.List<File> createdFiles = screenRecorder.get().getCreatedMovieFiles();
                if (createdFiles != null && !createdFiles.isEmpty()) {
                    File recordedFile = createdFiles.get(0);
                    String newName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
                    File mp4File = new File(videoFolder, newName);
                    if (recordedFile.renameTo(mp4File)) {
                        Reporter.log("Video recording saved: " + mp4File.getPath(), LogLevel.INFO_BLUE);
                    } else {
                        Reporter.log("Failed to rename video file to mp4", LogLevel.ERROR);
                    }
                } else {
                    Reporter.log("No video file found after recording", LogLevel.ERROR);
                }
            } catch (Exception e) {
                Reporter.log("Failed to save Web recording: " + e.getMessage(), LogLevel.ERROR);
            }
        }
        cleanup();
    }

    /**
     * Cleans up ThreadLocal resources to prevent memory leaks.
     * Should be called after recording is complete or in case of failures.
     */
    private void cleanup() {
        videoName.remove();
        screenRecorder.remove();
    }
}