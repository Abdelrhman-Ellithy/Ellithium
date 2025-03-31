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

public class ScreenRecorderActions<T extends WebDriver> extends BaseActions<T> {
    private static final ThreadLocal<ScreenRecorder> screenRecorder = new ThreadLocal<>();
    private static final ThreadLocal<String> videoName = new ThreadLocal<>();
    
    public ScreenRecorderActions(T driver) {
        super(driver);
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
     * Starts video recording of the screen.
     * @param name Name of the video file
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
            Format fileFormat = new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI);
            Format screenFormat = new Format(MediaTypeKey, MediaType.VIDEO,
                    EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                    CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                    DepthKey, 24,
                    FrameRateKey, Rational.valueOf(15),
                    QualityKey, 1.0f,
                    KeyFrameIntervalKey, 15 * 60);
            Format mouseFormat = new Format(MediaTypeKey, MediaType.VIDEO,
                    EncodingKey, "black",
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
     * Stops the video recording and saves it with a timestamp.
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
            try {
                screenRecorder.get().stop();
            }catch (Exception e){
                Reporter.log("Failed to save Web recording: " + e.getMessage(), LogLevel.ERROR);
            }
            File videoFolder = new File(ConfigContext.getRecordedExecutionsPath() + File.separator);
            File[] files = videoFolder.listFiles();
            if (files != null && files.length > 0) {
                File lastVideo = files[files.length - 1];
                String newName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
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
        cleanup();
    }

    /**
     * Cleans up ThreadLocal resources
     */
    private void cleanup() {
        videoName.remove();
        screenRecorder.remove();
    }
}