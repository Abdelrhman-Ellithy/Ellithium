package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static Ellithium.core.recording.internal.VideoRecordingManager.isAttachmentEnabled;

/**
 * Advanced screen recording and screenshot functionality for web and mobile testing.
 *
 * <p><b>Architecture:</b> Driver-Centric Hybrid Recording System
 * <ul>
 *   <li><b>Mobile (Android/iOS):</b> Uses native Appium screen recording capabilities</li>
 *   <li><b>Chrome/Edge (Chromium):</b> Uses CDP (Chrome DevTools Protocol) screencast for high-performance,
 *       parallel-safe recording that captures only the browser viewport</li>
 *   <li><b>Firefox/Safari:</b> Uses snapshot stitching - captures driver screenshots every 100ms
 *       and compiles them into video, ensuring parallel execution safety</li>
 * </ul>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Parallel execution safe - each driver instance records independently</li>
 *   <li>Pure Java implementation using JCodec - no FFmpeg required</li>
 *   <li>Always outputs standard H.264 MP4 files for universal compatibility</li>
 *   <li>Thread-safe with ThreadLocal state management</li>
 *   <li>Automatic resource cleanup to prevent memory leaks</li>
 * </ul>
 *
 * <p><b>Technical Details:</b>
 * <ul>
 *   <li>CDP captures browser viewport directly from render stream (doesn't capture desktop)</li>
 *   <li>Snapshot mode captures only the specific WebDriver instance (safe for parallel)</li>
 *   <li>Frames stored in memory during recording, encoded to MP4 on stop</li>
 *   <li>Frame rate: ~10 FPS for optimal balance of quality and file size</li>
 * </ul>
 *
 * @param <T> Type of WebDriver being used (WebDriver, ChromiumDriver, or mobile driver)
 * @see <a href="https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-startScreencast">CDP Screencast</a>
 * @see <a href="https://github.com/jcodec/jcodec">JCodec Documentation</a>
 */
public class ScreenRecorderActions<T extends WebDriver> extends BaseActions<T> {

    /**
     * Thread-safe storage for video frames captured during recording.
     * Each thread maintains its own queue of frame data (as byte arrays).
     */
    private static final ThreadLocal<Queue<byte[]>> videoFrames =
        ThreadLocal.withInitial(ConcurrentLinkedQueue::new);

    /**
     * Thread-safe storage for the video name/identifier.
     */
    private static final ThreadLocal<String> videoName = new ThreadLocal<>();

    /**
     * Thread-safe flag indicating if recording is currently active.
     * Used to control frame capture in CDP listeners and snapshot threads.
     */
    private static final ThreadLocal<AtomicBoolean> isRecording =
        ThreadLocal.withInitial(() -> new AtomicBoolean(false));

    /**
     * Thread-safe storage for the background snapshot capture executor.
     * Used for Firefox/Safari browsers that don't support CDP.
     */
    private static final ThreadLocal<ScheduledExecutorService> backgroundCapturer = new ThreadLocal<>();

    /**
     * Thread-safe storage for DevTools session.
     * Used for Chrome/Edge CDP screencast recording.
     */
    private static final ThreadLocal<DevTools> devToolsSession = new ThreadLocal<>();

    /**
     * Default frame rate for video recording (frames per second).
     * Balances file size with smooth playback.
     */
    private static final int DEFAULT_FPS = 10;

    /**
     * Interval between snapshots in milliseconds for Firefox/Safari recording.
     */
    private static final int SNAPSHOT_INTERVAL_MS = 100;

    /**
     * CDP screencast JPEG quality (0-100).
     * Higher quality = larger frames but better video quality.
     */
    private static final int CDP_JPEG_QUALITY = 80;

    /**
     * Maximum wait time in milliseconds for executor shutdown.
     */
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_MS = 2000;

    private static Class<?> cachedPageClass = null;

    /**
     * Creates a new ScreenRecorderActions instance.
     *
     * @param driver WebDriver instance to use for recording/screenshots
     * @throws IllegalArgumentException if driver is null
     */
    public ScreenRecorderActions(T driver) {
        super(driver);
        if (driver == null) {
            throw new IllegalArgumentException("Driver cannot be null");
        }
    }

    /**
     * Captures a screenshot of the current browser window or mobile screen.
     * <p>
     * The screenshot is saved with a timestamp and automatically attached to the test report.
     * This method is thread-safe and works in parallel execution.
     *
     * @param screenshotName Base name for the screenshot file (will be sanitized)
     * @return File object of saved screenshot, null if capture fails
     * @throws IllegalArgumentException if screenshotName is null or empty
     */
    public File captureScreenshot(String screenshotName) {
        if (screenshotName == null || screenshotName.trim().isEmpty()) {
            Reporter.log("Screenshot name cannot be null or empty, using default", LogLevel.WARN);
            screenshotName = "screenshot";
        }

        try {
            if (!(driver instanceof TakesScreenshot camera)) {
                Reporter.log("Driver does not support screenshots", LogLevel.ERROR);
                return null;
            }

            File screenshot = camera.getScreenshotAs(OutputType.FILE);

            if (screenshot == null || !screenshot.exists()) {
                Reporter.log("Screenshot file was not created", LogLevel.ERROR);
                return null;
            }

            File screenShotFolder = new File(ConfigContext.getCapturedScreenShotPath() + File.separator);
            if (!screenShotFolder.exists()) {
                boolean created = screenShotFolder.mkdirs();
                if (!created) {
                    Reporter.log("Failed to create screenshot folder: " + screenShotFolder.getPath(),
                        LogLevel.ERROR);
                    return null;
                }
            }

            String sanitizedName = sanitizeFileName(screenshotName);
            String name = sanitizedName + "-" + TestDataGenerator.getTimeStamp();
            File screenShotFile = new File(screenShotFolder.getPath() + File.separator + name + ".png");

            Files.move(screenshot, screenShotFile);

            Reporter.log("Screenshot captured: " + screenShotFile.getPath(), LogLevel.INFO_BLUE);
            Reporter.attachScreenshotToReport(screenShotFile, name, "Captured Screenshot");

            return screenShotFile;
        } catch (IOException e) {
            Reporter.log("Failed to save screenshot: " + e.getMessage(), LogLevel.ERROR);
            return null;
        } catch (Exception e) {
            Reporter.log("Failed to capture screenshot: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Initiates screen recording with the specified name.
     * <p>
     * <b>Recording Strategy by Driver Type:</b>
     * <ul>
     *   <li><b>Mobile (Android/iOS):</b> Uses native Appium recording</li>
     *   <li><b>Chrome/Edge:</b> Uses CDP screencast (high performance, parallel-safe)</li>
     *   <li><b>Firefox/Safari:</b> Uses snapshot stitching (parallel-safe fallback)</li>
     * </ul>
     *
     * <p><b>Thread Safety:</b> Each thread maintains its own recording state via ThreadLocal.
     * Multiple tests can record simultaneously without interference.
     *
     * @param name Base name for the video file (will be sanitized and timestamped)
     * @throws IllegalArgumentException if name is null or empty
     */
    public void startRecording(String name) {
        if (name == null || name.trim().isEmpty()) {
            Reporter.log("Recording name cannot be null or empty, using default", LogLevel.WARN);
            name = "recording";
        }

        String sanitizedName = sanitizeFileName(name);
        videoName.set(sanitizedName);
        isRecording.get().set(true);
        videoFrames.get().clear();

        try {
            // Strategy 1: Mobile Recording (Android/iOS)
            if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
                startMobileRecording(sanitizedName);
                return;
            }

            // Strategy 2: CDP Recording (Chrome/Edge)
            if (driver instanceof ChromiumDriver) {
                boolean cdpStarted = startCDPRecording(sanitizedName);
                if (cdpStarted) {
                    return;
                }
                // If CDP fails, fall through to snapshot mode
                Reporter.log("CDP recording failed, falling back to snapshot mode", LogLevel.WARN);
            }

            // Strategy 3: Snapshot Recording (Firefox/Safari/Fallback)
            startSnapshotRecording(sanitizedName);

        } catch (Exception e) {
            Reporter.log("Failed to start recording: " + e.getMessage(), LogLevel.ERROR);
            isRecording.get().set(false);
            cleanup();
        }
    }

    /**
     * Stops the current recording and saves it as an MP4 file.
     * <p>
     * The video is compiled from captured frames using JCodec and saved with a timestamp.
     * All recording resources are automatically cleaned up.
     *
     * <p><b>Output Format:</b> Standard H.264 MP4 (compatible with all modern players and reports)
     *
     * <p><b>Thread Safety:</b> Safe to call from any thread. Only affects the current thread's recording.
     *
     * @return Absolute path of the saved video file, null if recording failed or no frames captured
     */
    public String stopRecording() {
        String path = null;
        String name = videoName.get();

        if (name == null) {
            Reporter.log("No active recording found to stop", LogLevel.WARN);
            cleanup();
            return null;
        }

        File videoFolder = new File(ConfigContext.getRecordedExecutionsPath() + File.separator);
        if (!videoFolder.exists()) {
            boolean created = videoFolder.mkdirs();
            if (!created) {
                Reporter.log("Failed to create video folder: " + videoFolder.getPath(), LogLevel.ERROR);
                cleanup();
                return null;
            }
        }

        try {
            // Stop recording flag first to prevent new frames
            isRecording.get().set(false);

            // Strategy 1: Mobile Recording Stop
            if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
                path = stopMobileRecording(name, videoFolder);
            }
            // Strategy 2 & 3: Web Recording Stop (CDP or Snapshot)
            else {
                path = stopWebRecording(name, videoFolder);
            }

            if (path != null) {
                Reporter.log("Video recording saved: " + path, LogLevel.INFO_BLUE);
            } else {
                Reporter.log("Failed to save video recording", LogLevel.ERROR);
            }

        } catch (Exception e) {
            Reporter.log("Error stopping recording: " + e.getMessage(), LogLevel.ERROR);
        } finally {
            cleanup();
        }

        return path;
    }

    /**
     * Starts mobile recording using Appium's native capabilities.
     */
    private void startMobileRecording(String name) {
        try {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).startRecordingScreen();
            } else if (driver instanceof IOSDriver) {
                ((IOSDriver) driver).startRecordingScreen();
            }
            Reporter.log("Started mobile screen recording: " + name, LogLevel.INFO_BLUE);
        } catch (Exception e) {
            Reporter.log("Failed to start mobile recording: " + e.getMessage(), LogLevel.ERROR);
            throw e;
        }
    }

    /**
     * Starts CDP-based recording for Chromium browsers.
     *
     * @return true if CDP recording started successfully, false otherwise
     */
    private boolean startCDPRecording(String name) {
        try {
            // Unwrap the driver to get the actual ChromiumDriver instance
            ChromiumDriver chromiumDriver = unwrapChromiumDriver(driver);
            if (chromiumDriver == null) {
                Reporter.log("Could not unwrap ChromiumDriver, falling back to snapshot", LogLevel.WARN);
                return false;
            }

            DevTools devTools = chromiumDriver.getDevTools();
            devTools.createSession();
            devToolsSession.set(devTools);

            // Dynamically detect available CDP version
            String detectedVersion = detectCDPVersion();
            if (detectedVersion == null) {
                Reporter.log("No CDP version found in classpath", LogLevel.WARN);
                return false;
            }

            try {
                Class<?> pageClass = Class.forName("org.openqa.selenium.devtools." + detectedVersion + ".page.Page");
                Object enableCommand = pageClass.getMethod("enable").invoke(null);
                devTools.send((org.openqa.selenium.devtools.Command<?>) enableCommand);

                Object startScreencastCommand = pageClass.getMethod("startScreencast",
                                Optional.class, Optional.class, Optional.class, Optional.class, Optional.class)
                        .invoke(null,
                                Optional.of(getScreencastFormat(pageClass)),
                                Optional.of(CDP_JPEG_QUALITY),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()
                        );
                devTools.send((org.openqa.selenium.devtools.Command<?>) startScreencastCommand);

                addScreencastFrameListener(devTools, pageClass);

                Reporter.log("Started web recording (CDP " + detectedVersion + "): " + name, LogLevel.INFO_BLUE);
                return true;

            } catch (Exception e) {
                Reporter.log("CDP version " + detectedVersion + " failed: " + e.getMessage(), LogLevel.WARN);
                return false;
            }

        } catch (Exception e) {
            Reporter.log("CDP initialization failed: " + e.getMessage(), LogLevel.WARN);
            if (devToolsSession.get() != null) {
                try {
                    devToolsSession.get().close();
                } catch (Exception ignored) {}
                devToolsSession.remove();
            }
            return false;
        }
    }

    /**
     * Unwraps proxied/decorated drivers to get the actual ChromiumDriver instance.
     */
    private ChromiumDriver unwrapChromiumDriver(WebDriver driver) {
        WebDriver current = driver;
        int maxUnwrapDepth = 10; // Prevent infinite loops

        for (int i = 0; i < maxUnwrapDepth; i++) {
            // Check if current is ChromiumDriver
            if (current instanceof ChromiumDriver) {
                return (ChromiumDriver) current;
            }

            // Try WrapsDriver interface
            if (current instanceof org.openqa.selenium.WrapsDriver) {
                current = ((org.openqa.selenium.WrapsDriver) current).getWrappedDriver();
                continue;
            }

            // Try EventFiringDecorator reflection
            try {
                java.lang.reflect.Field decoratedField = current.getClass().getDeclaredField("decorated");
                decoratedField.setAccessible(true);
                Object decorated = decoratedField.get(current);
                if (decorated instanceof WebDriver) {
                    current = (WebDriver) decorated;
                    continue;
                }
            } catch (Exception ignored) {}

            // Try getting field "driver" (common in custom wrappers)
            try {
                java.lang.reflect.Field driverField = current.getClass().getDeclaredField("driver");
                driverField.setAccessible(true);
                Object innerDriver = driverField.get(current);
                if (innerDriver instanceof WebDriver) {
                    current = (WebDriver) innerDriver;
                    continue;
                }
            } catch (Exception ignored) {}

            // Can't unwrap further
            break;
        }

        return null;
    }

    /**
     * Dynamically detects available CDP version in classpath.
     * Scans for org.openqa.selenium.devtools.v* packages.
     */
    private String detectCDPVersion() {
        // Try to find the highest available version
        for (int version = 150; version >= 85; version--) {
            String versionStr = "v" + version;
            try {
                Class.forName("org.openqa.selenium.devtools." + versionStr + ".page.Page");
                return versionStr;
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    /**
     * Tries fallback CDP versions (kept for compatibility).
     */
    private boolean tryFallbackCDPVersions(DevTools devTools, String name) {
        // This method is now rarely called since we detect version dynamically
        // But kept as additional fallback
        String[] versions = {"v143", "v142", "v141", "v140", "v139", "v138", "v137", "v136", "v135"};
        for (String version : versions) {
            try {
                Class<?> pageClass = Class.forName("org.openqa.selenium.devtools." + version + ".page.Page");
                Object enableCommand = pageClass.getMethod("enable").invoke(null);
                devTools.send((org.openqa.selenium.devtools.Command<?>) enableCommand);

                Object startScreencastCommand = pageClass.getMethod("startScreencast",
                                Optional.class, Optional.class, Optional.class, Optional.class, Optional.class)
                        .invoke(null,
                                Optional.of(getScreencastFormat(pageClass)),
                                Optional.of(CDP_JPEG_QUALITY),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()
                        );
                devTools.send((org.openqa.selenium.devtools.Command<?>) startScreencastCommand);

                addScreencastFrameListener(devTools, pageClass);

                Reporter.log("Started web recording (CDP " + version + "): " + name, LogLevel.INFO_BLUE);
                return true;

            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Gets the screencast format enum for CDP.
     */
    private Object getScreencastFormat(Class<?> pageClass) throws Exception {
        Class<?>[] innerClasses = pageClass.getDeclaredClasses();
        for (Class<?> inner : innerClasses) {
            if (inner.getSimpleName().equals("StartScreencastFormat")) {
                return inner.getField("JPEG").get(null);
            }
        }
        throw new ClassNotFoundException("StartScreencastFormat not found in " + pageClass.getName());
    }

    /**
     * Adds listener for CDP screencast frames.
     */
    private void addScreencastFrameListener(DevTools devTools, Class<?> pageClass) throws Exception {
        Object screencastFrameEvent = pageClass.getMethod("screencastFrame").invoke(null);

        final Queue<byte[]> targetQueue = videoFrames.get();
        final AtomicBoolean recordingFlag = isRecording.get();

        devTools.addListener((org.openqa.selenium.devtools.Event<?>) screencastFrameEvent, frameData -> {
            if (recordingFlag.get()) {
                try {
                    // Reflection: frameData.getData() -> Base64 String
                    Method getDataMethod = frameData.getClass().getMethod("getData");
                    String base64Data = (String) getDataMethod.invoke(frameData);
                    byte[] imageData = Base64.getDecoder().decode(base64Data);
                    targetQueue.add(imageData);

                    // Reflection: frameData.getSessionId() -> Integer
                    Method getSessionIdMethod = frameData.getClass().getMethod("getSessionId");
                    Integer sessionId = (Integer) getSessionIdMethod.invoke(frameData);

                    // Reflection: Page.screencastFrameAck(sessionId)
                    Object ackCommand = pageClass.getMethod("screencastFrameAck", Integer.class)
                            .invoke(null, sessionId);
                    devTools.send((org.openqa.selenium.devtools.Command<?>) ackCommand);
                } catch (Exception ignored) {
                    // Ignore individual frame errors to keep stream alive
                }
            }
        });
    }

    /**
     * Starts snapshot-based recording for Firefox/Safari or as fallback.
     * FIX: Captures Main Thread references to avoid ThreadLocal isolation.
     */
    private void startSnapshotRecording(String name) {
        // Unwrap driver proxy if needed
        WebDriver rawDriver = driver;
        try {
            if (driver instanceof org.openqa.selenium.WrapsDriver) {
                rawDriver = ((org.openqa.selenium.WrapsDriver) driver).getWrappedDriver();
            }
        } catch (Exception ignored) {}

        if (!(rawDriver instanceof TakesScreenshot)) {
            Reporter.log("Driver does not support screenshots", LogLevel.ERROR);
            throw new UnsupportedOperationException("Driver does not support screenshots");
        }

        // Capture references from Main Thread
        final Queue<byte[]> targetQueue = videoFrames.get();
        final AtomicBoolean recordingFlag = isRecording.get();
        final TakesScreenshot screenshotDriver = (TakesScreenshot) rawDriver;

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "SnapshotRecorder-" + name);
            thread.setDaemon(true);
            return thread;
        });
        backgroundCapturer.set(executor);

        executor.scheduleAtFixedRate(() -> {
            if (recordingFlag.get()) {
                try {
                    byte[] screenshot = screenshotDriver.getScreenshotAs(OutputType.BYTES);
                    if (screenshot != null && screenshot.length > 0) {
                        targetQueue.add(screenshot);
                    }
                } catch (Exception ignored) {}
            }
        }, 0, SNAPSHOT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        Reporter.log("Started web recording (Snapshot): " + name, LogLevel.INFO_BLUE);
    }

    /**
     * Stops mobile recording and saves the video.
     */
    private String stopMobileRecording(String name, File videoFolder) {
        try {
            String base64Video;
            if (driver instanceof AndroidDriver) {
                base64Video = ((AndroidDriver) driver).stopRecordingScreen();
            } else {
                base64Video = ((IOSDriver) driver).stopRecordingScreen();
            }

            if (base64Video == null || base64Video.isEmpty()) {
                Reporter.log("Mobile recording returned empty data", LogLevel.ERROR);
                return null;
            }

            byte[] videoData = Base64.getDecoder().decode(base64Video);
            String fileName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
            File videoFile = new File(videoFolder, fileName);

            try (FileOutputStream fos = new FileOutputStream(videoFile)) {
                fos.write(videoData);
                fos.flush();
            }
            Reporter.log("Mobile video recording saved: " + videoFile.getPath(), LogLevel.INFO_BLUE);
            return videoFile.getAbsolutePath();

        } catch (IllegalArgumentException e) {
            Reporter.log("Invalid Base64 data from mobile recording: " + e.getMessage(), LogLevel.ERROR);
            return null;
        } catch (IOException e) {
            Reporter.log("Failed to write mobile video file: " + e.getMessage(), LogLevel.ERROR);
            return null;
        } catch (Exception e) {
            Reporter.log("Failed to stop mobile recording: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Stops web recording (CDP or Snapshot) and compiles frames to MP4.
     */
    private String stopWebRecording(String name, File videoFolder) {
        try {
            // Get queue reference BEFORE stopping executor
            Queue<byte[]> frames = videoFrames.get();

            // Stop CDP if active
            if (devToolsSession.get() != null) {
                stopCDPScreencast();
            }

            // Stop snapshot executor if active
            if (backgroundCapturer.get() != null) {
                stopSnapshotExecutor();
            }
            // Small delay for last frames
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (frames == null || frames.isEmpty()) {
                Reporter.log("No frames captured during recording", LogLevel.WARN);
                return null;
            }

            String fileName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
            File videoFile = new File(videoFolder, fileName);

            boolean needsAttachment = isAttachmentEnabled();

            if (needsAttachment) {
                // SYNCHRONOUS: Compile immediately because we need to attach to report
                Reporter.log("Compiling video synchronously for report attachment", LogLevel.DEBUG);
                return compileFramesToMP4(frames, videoFile);
            } else {
                // ASYNCHRONOUS: Compile in background to not block test execution
                final Queue<byte[]> framesCopy = new ConcurrentLinkedQueue<>(frames);
                final int frameCount = framesCopy.size();

                videoCompilationExecutor.submit(() -> {
                    try {
                        compileFramesToMP4(framesCopy, videoFile);
                        Reporter.log("Video compiled asynchronously: " + videoFile.getName() +
                                " (" + frameCount + " frames)", LogLevel.INFO_GREEN);
                    } catch (Exception e) {
                        Reporter.log("Async video compilation failed: " + e.getMessage(), LogLevel.ERROR);
                    }
                });

                Reporter.log("Video compilation started in background (" + frameCount + " frames)",
                        LogLevel.DEBUG);

                // Return path immediately (file will be ready in a few seconds)
                return videoFile.getAbsolutePath();
            }

        } catch (Exception e) {
            Reporter.log("Failed to stop web recording: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Stops CDP screencast session with dynamic version detection.
     */
    private void stopCDPScreencast() {
        try {
            DevTools devTools = devToolsSession.get();
            if (devTools != null) {
                String detectedVersion = detectCDPVersion();
                if (detectedVersion != null) {
                    try {
                        Class<?> pageClass = Class.forName("org.openqa.selenium.devtools." + detectedVersion + ".page.Page");
                        Object stopCommand = pageClass.getMethod("stopScreencast").invoke(null);
                        devTools.send((org.openqa.selenium.devtools.Command<?>) stopCommand);
                    } catch (Exception e) {
                        // Try fallback versions
                        String[] versions = {"v143", "v142", "v141", "v140", "v139", "v138"};
                        for (String version : versions) {
                            try {
                                Class<?> pageClass = Class.forName("org.openqa.selenium.devtools." + version + ".page.Page");
                                Object stopCommand = pageClass.getMethod("stopScreencast").invoke(null);
                                devTools.send((org.openqa.selenium.devtools.Command<?>) stopCommand);
                                break;
                            } catch (Exception ignored) {}
                        }
                    }
                }
                devTools.close();
            }
        } catch (Exception ignored) {
        } finally {
            devToolsSession.remove();
        }
    }

    /**
     * Stops snapshot capture executor.
     */
    private void stopSnapshotExecutor() {
        ScheduledExecutorService executor = backgroundCapturer.get();
        if (executor != null) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                backgroundCapturer.remove();
            }
        }
    }

    /**
     * Compiles captured frames into an MP4 video file using JCodec.
     */
    /**
     * Compiles captured frames into an MP4 video file using JCodec.
     */
    private String compileFramesToMP4(Queue<byte[]> frames, File outputFile) {
        AWTSequenceEncoder encoder = null;
        int successfulFrames = 0;
        int failedFrames = 0;

        try {
            encoder = AWTSequenceEncoder.createSequenceEncoder(outputFile, DEFAULT_FPS);

            for (byte[] frameBytes : frames) {
                try {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(frameBytes));

                    if (image != null) {
                        int width = image.getWidth();
                        int height = image.getHeight();

                        // Ensure even dimensions for JCodec
                        int evenWidth = (width % 2 == 0) ? width : width - 1;
                        int evenHeight = (height % 2 == 0) ? height : height - 1;

                        BufferedImage processedImage;

                        // Only process if dimensions need adjustment or type conversion needed
                        if (width != evenWidth || height != evenHeight || image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                            processedImage = new BufferedImage(evenWidth, evenHeight, BufferedImage.TYPE_3BYTE_BGR);
                            processedImage.getGraphics().drawImage(
                                    image,
                                    0, 0, evenWidth, evenHeight,
                                    0, 0, evenWidth, evenHeight,
                                    null
                            );
                        } else {
                            processedImage = image;
                        }

                        encoder.encodeImage(processedImage);
                        successfulFrames++;
                    } else {
                        failedFrames++;
                    }
                } catch (Exception e) {
                    failedFrames++;
                }
            }

            encoder.finish();

            if (successfulFrames == 0) {
                Reporter.log("No valid frames to encode", LogLevel.ERROR);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                return null;
            }

            Reporter.log("Encoded " + successfulFrames + " frames successfully", LogLevel.DEBUG);

            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            Reporter.log("Failed to compile frames to video: " + e.getMessage(), LogLevel.ERROR);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            return null;
        } finally {
            if (encoder != null) {
                try {
                    encoder.finish();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Sanitizes a file name by removing invalid characters.
     *
     * @param fileName Original file name
     * @return Sanitized file name safe for all operating systems
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "recording";
        }
        // Remove invalid file name characters for Windows, Linux, and macOS
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                      .replaceAll("\\s+", "_")
                      .replaceAll("_{2,}", "_")
                      .trim();
    }

    /**
     * Cleans up ThreadLocal resources to prevent memory leaks.
     * <p>
     * This method should be called after recording is complete or in case of failures.
     * It's automatically called by stopRecording() but can be called manually if needed.
     *
     * <p><b>Cleaned Resources:</b>
     * <ul>
     *   <li>Video frames queue</li>
     *   <li>Video name</li>
     *   <li>Recording flag</li>
     *   <li>Background capture executor</li>
     *   <li>DevTools session</li>
     * </ul>
     */
    private void cleanup() {
        try {
            if (backgroundCapturer.get() != null) {
                stopSnapshotExecutor();
            }

            if (devToolsSession.get() != null) {
                try {
                    devToolsSession.get().close();
                } catch (Exception ignored) {}
                devToolsSession.remove();
            }

            if (videoFrames.get() != null) {
                videoFrames.get().clear();
                videoFrames.remove();
            }

            videoName.remove();

            if (isRecording.get() != null) {
                isRecording.get().set(false);
                isRecording.remove();
            }

        } catch (Exception e) {
            Reporter.log("Error during cleanup: " + e.getMessage(), LogLevel.WARN);
        }
    }
    /**
     * Executor service for async video compilation.
     */
    public static final ExecutorService videoCompilationExecutor =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "VideoCompiler-" + r.hashCode());
                t.setDaemon(true);
                return t;
            });

}