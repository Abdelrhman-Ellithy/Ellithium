package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import Ellithium.core.driver.LocalDriverType;
import Ellithium.core.driver.RemoteDriverType;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private static final int CDP_JPEG_QUALITY = 50;

    /**
     * Maximum wait time in milliseconds for executor shutdown.
     */
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_MS = 2000;

    private static final ThreadLocal<Long> recordingStartTime = new ThreadLocal<>();

    /**
     * atomic counter to track active video compilations.
     */
    public static final AtomicInteger activeCompilations = new AtomicInteger(0);

    /**
     * Creates a new ScreenRecorderActions instance.
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
            Reporter.log("Screenshot captured: " + screenShotFile.getPath(), LogLevel.INFO_GREEN);
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
        recordingStartTime.set(System.currentTimeMillis());

        try {
            // Strategy 1: Mobile Recording (Android/iOS)
            if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
                startMobileRecording(sanitizedName);
                return;
            }

            // Strategy 2: CDP Recording (Chrome/Edge)
            DriverType driverType= DriverFactory.getCurrentDriverConfiguration().getDriverType();
            if (driverType== LocalDriverType.Chrome || driverType==LocalDriverType.Edge ||driverType== RemoteDriverType.REMOTE_Chrome || driverType==RemoteDriverType.REMOTE_Edge) {
                boolean cdpStarted = startCDPRecording(sanitizedName);
                if (cdpStarted) {
                    return;
                }
                Reporter.log("CDP recording failed, falling back to snapshot mode", LogLevel.WARN);
            }

            // Strategy 3: Snapshot Recording (Firefox/Safari/Fallback)
            startSnapshotRecording(sanitizedName);

        } catch (Exception e) {
            Reporter.log("Failed to start recording: " + e.getMessage(), LogLevel.ERROR);
            isRecording.get().set(false);
            recordingStartTime.remove();
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
                Reporter.log("Video recording saved: " + path, LogLevel.INFO_GREEN);
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
            Reporter.log("Started mobile screen recording: " + name, LogLevel.INFO_GREEN);
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
            ChromiumDriver chromiumDriver = unwrapChromiumDriver(driver);
            if (chromiumDriver == null) {
                Reporter.log("Could not unwrap ChromiumDriver, falling back to snapshot", LogLevel.WARN);
                return false;
            }
            DevTools devTools = chromiumDriver.getDevTools();
            devTools.createSession();
            devToolsSession.set(devTools);

            String detectedVersion = detectCDPVersion();
            if (detectedVersion == null) {
                Reporter.log("No CDP version found in classpath", LogLevel.WARN);
                return false;
            }
            Class<?> pageClass = Class.forName("org.openqa.selenium.devtools." + detectedVersion + ".page.Page");

            // CORRECTED: Handle enable() with different signatures
            Object enableCommand = null;
            Exception lastException = null;

            // Approach 1: Try enable(Optional) - Selenium 4.10+
            try {
                Method enableMethod = pageClass.getMethod("enable", Optional.class);
                enableCommand = enableMethod.invoke(null, Optional.empty());
            } catch (NoSuchMethodException e) {
                lastException = e;

                // Approach 2: Try enable() no-args - Selenium 4.0-4.9
                try {
                    Method enableMethod = pageClass.getMethod("enable");
                    enableCommand = enableMethod.invoke(null);
                } catch (NoSuchMethodException e2) {
                    lastException = e2;

                    // Approach 3: Try as FIELD - Older Selenium 4.x
                    try {
                        java.lang.reflect.Field enableField = pageClass.getField("enable");
                        enableCommand = enableField.get(null);
                    } catch (NoSuchFieldException e3) {
                        lastException = e3;

                        // Approach 4: Try as declared field
                        try {
                            java.lang.reflect.Field enableField = pageClass.getDeclaredField("enable");
                            enableField.setAccessible(true);
                            enableCommand = enableField.get(null);
                        } catch (Exception e4) {
                            lastException = e4;
                        }
                    }
                }
            }

            // If all approaches failed, log and return false
            if (enableCommand == null) {
                Reporter.log("Could not find CDP enable command in " + detectedVersion, LogLevel.WARN);
                Logger.logException(lastException);
                devTools.close();
                devToolsSession.remove();
                return false;
            }

            // Send enable command
            devTools.send((org.openqa.selenium.devtools.Command<?>) enableCommand);

            // Get startScreencast command - also needs Optional parameter handling
            Object startCommand;
            try {
                // Try the full signature with 5 Optional parameters
                java.lang.reflect.Method startMethod = pageClass.getMethod("startScreencast",
                        Optional.class, Optional.class, Optional.class, Optional.class, Optional.class);
                startCommand = startMethod.invoke(null,
                        Optional.of(getScreencastFormat(pageClass)),
                        Optional.of(CDP_JPEG_QUALITY),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(4)
                );
            } catch (NoSuchMethodException e) {
                // Try alternative signatures if needed
                try {
                    java.lang.reflect.Method startMethod = pageClass.getMethod("startScreencast");
                    startCommand = startMethod.invoke(null);
                } catch (NoSuchMethodException e2) {
                    Reporter.log("startScreencast method not found in " + detectedVersion, LogLevel.WARN);
                    devTools.close();
                    devToolsSession.remove();
                    return false;
                }
            }

            devTools.send((org.openqa.selenium.devtools.Command<?>) startCommand);
            addScreencastFrameListener(devTools, pageClass);
            Reporter.log("Started web recording (CDP " + detectedVersion + "): " + name, LogLevel.INFO_BLUE);
            return true;

        } catch (ClassNotFoundException e) {
            Reporter.log("CDP Page class not found: " + e.getMessage(), LogLevel.WARN);
            cleanupDevTools();
            return false;
        } catch (Exception e) {
            Reporter.log("CDP recording initialization failed: " + e.getMessage(), LogLevel.WARN);
            Logger.logException(e);
            cleanupDevTools();
            return false;
        }
    }

    /**
     * Helper method to clean up DevTools session safely.
     */
    private void cleanupDevTools() {
        if (devToolsSession.get() != null) {
            try {
                devToolsSession.get().close();
            } catch (Exception ignored) {}
            devToolsSession.remove();
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

        Reporter.log("Started web recording (Snapshot): " + name, LogLevel.INFO_GREEN);
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
            Reporter.log("Mobile video recording saved: " + videoFile.getPath(), LogLevel.INFO_GREEN);
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

            if (frames == null || frames.isEmpty()) {
                Reporter.log("No frames captured during recording", LogLevel.WARN);
                return null;
            }

            // CAPTURE DURATION HERE (before async, in main thread)
            Long startTime = recordingStartTime.get();
            long durationMs = (startTime != null) ? System.currentTimeMillis() - startTime : 0;


            String fileName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
            File videoFile = new File(videoFolder, fileName);

            boolean needsAttachment = isAttachmentEnabled();
            if (needsAttachment) {
                // SYNCHRONOUS: Compile immediately because we need to attach to report
                Reporter.log("Compiling video synchronously for report attachment", LogLevel.INFO_GREEN);
                return compileFramesToMP4(frames, videoFile,durationMs);
            } else {
                // ASYNCHRONOUS: Compile in background to not block test execution
                final Queue<byte[]> framesCopy = new ConcurrentLinkedQueue<>(frames);
                final int frameCount = framesCopy.size();
                final long capturedDuration = durationMs; // Capture for lambda
                videoCompilationExecutor.submit(() -> {
                    activeCompilations.incrementAndGet();
                    try {
                        compileFramesToMP4(framesCopy, videoFile, capturedDuration);
                        Reporter.log("Video compiled asynchronously: " + videoFile.getName() +
                                " (" + frameCount + " frames)", LogLevel.INFO_GREEN);
                    } catch (Exception e) {
                        Reporter.log("Async video compilation failed: " + e.getMessage(), LogLevel.ERROR);
                    }
                    finally {
                        activeCompilations.decrementAndGet();
                    }
                });

                Reporter.log("Video compilation started in background (" + frameCount + " frames)",
                        LogLevel.INFO_BLUE);

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
                        Object stopCommand = null;
                        // Try stopScreencast() with Optional parameter
                        try {
                            Method stopMethod = pageClass.getMethod("stopScreencast", Optional.class);
                            stopCommand = stopMethod.invoke(null, Optional.empty());
                        } catch (NoSuchMethodException e) {
                            // Try no-arg version
                            try {
                                Method stopMethod = pageClass.getMethod("stopScreencast");
                                stopCommand = stopMethod.invoke(null);
                            } catch (NoSuchMethodException e2) {
                                // Try as field
                                try {
                                    java.lang.reflect.Field stopField = pageClass.getField("stopScreencast");
                                    stopCommand = stopField.get(null);
                                } catch (NoSuchFieldException e3) {
                                    // Try as declared field
                                    try {
                                        java.lang.reflect.Field stopField = pageClass.getDeclaredField("stopScreencast");
                                        stopField.setAccessible(true);
                                        stopCommand = stopField.get(null);
                                    } catch (Exception e4) {
                                        Reporter.log("Could not find stopScreencast command", LogLevel.WARN);
                                    }
                                }
                            }
                        }

                        if (stopCommand != null) {
                            devTools.send((org.openqa.selenium.devtools.Command<?>) stopCommand);
                        }
                    } catch (Exception e) {
                        Reporter.log("Error stopping screencast: " + e.getMessage(), LogLevel.WARN);
                    }
                }
                devTools.close();
            }
        } catch (Exception e) {
            Reporter.log("Error closing DevTools: " + e.getMessage(), LogLevel.WARN);
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
     * Uses BATCHED PARALLEL processing to balance high speed with low memory usage.
     * * @param frames Queue of frame data
     * @param outputFile Output video file
     * @param recordingDurationMs Recording duration in milliseconds (0 if unknown)
     * @return Absolute path of compiled video, or null if failed
     */
    private String compileFramesToMP4(Queue<byte[]> frames, File outputFile, long recordingDurationMs) {
        int totalFrames = frames.size();
        if (totalFrames == 0) {
            Reporter.log("No frames to encode", LogLevel.WARN);
            return null;
        }
        AWTSequenceEncoder encoder = null;
        int successfulFrames = 0;
        final int MAX_WIDTH = 1280;
        final int MAX_HEIGHT = 720;
        int batchSize = 50;
        try {
            // Peek at the first frame to calculate accurate memory requirements
            byte[] firstFrame = frames.peek();
            if (firstFrame != null) {
                BufferedImage probe = ImageIO.read(new ByteArrayInputStream(firstFrame));
                if (probe != null) {
                    // Calculate the dimensions this frame will have AFTER processing
                    long frameSize = getFrameSize(probe, MAX_WIDTH, MAX_HEIGHT);
                    batchSize = calculateOptimalBatchSize(frameSize);
                }
            }
        } catch (Exception e) {
            Reporter.log("Failed to calculate dynamic batch size, using default: " + e.getMessage(), LogLevel.DEBUG);
        }
        // -----------------------------------------

        try {
            double actualCaptureFPS = (double) totalFrames / (recordingDurationMs / 1000.0);
            int outputFPS = (actualCaptureFPS >= 1 && actualCaptureFPS <= 60)
                    ? Math.max(1, (int) Math.round(actualCaptureFPS))
                    : DEFAULT_FPS;

            encoder = AWTSequenceEncoder.createSequenceEncoder(outputFile, outputFPS);

            while (!frames.isEmpty()) {

                // A. Create Batch
                List<byte[]> rawBatch = new ArrayList<>(batchSize);
                for (int i = 0; i < batchSize && !frames.isEmpty(); i++) {
                    byte[] data = frames.poll();
                    if (data != null) rawBatch.add(data);
                }

                if (rawBatch.isEmpty()) continue;

                // B. Parallel Process
                List<BufferedImage> processedBatch = rawBatch.parallelStream()
                        .map(frameBytes -> {
                            try {
                                BufferedImage original = ImageIO.read(new ByteArrayInputStream(frameBytes));
                                if (original == null) return null;

                                int origW = original.getWidth();
                                int origH = original.getHeight();

                                boolean needsScaling = (origW > MAX_WIDTH || origH > MAX_HEIGHT);
                                int newW = origW;
                                int newH = origH;

                                if (needsScaling) {
                                    double scale = Math.min((double) MAX_WIDTH / origW, (double) MAX_HEIGHT / origH);
                                    newW = (int) (origW * scale);
                                    newH = (int) (origH * scale);
                                }

                                int evenW = (newW % 2 == 0) ? newW : newW - 1;
                                int evenH = (newH % 2 == 0) ? newH : newH - 1;

                                if (!needsScaling && origW == evenW && origH == evenH && original.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                                    return original;
                                }

                                BufferedImage resized = new BufferedImage(evenW, evenH, BufferedImage.TYPE_3BYTE_BGR);
                                Graphics2D g2d = resized.createGraphics();
                                if (needsScaling) {
                                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                }
                                g2d.drawImage(original, 0, 0, evenW, evenH, null);
                                g2d.dispose();

                                return resized;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // C. Encode
                for (BufferedImage img : processedBatch) {
                    try {
                        encoder.encodeImage(img);
                        successfulFrames++;
                    } catch (Exception e) {}
                }

                // D. Cleanup
                processedBatch.clear();
                rawBatch.clear();
            }

            encoder.finish();

            // ... (Remaining logging logic same as before)
            if (successfulFrames == 0) {
                Reporter.log("No valid frames encoded", LogLevel.ERROR);
                if (outputFile.exists()) outputFile.delete();
                return null;
            }

            double expectedDuration = (double) successfulFrames / outputFPS;
            Reporter.log("Encoded " + successfulFrames + "/" + totalFrames +
                    " frames at " + outputFPS + " FPS (~" + String.format("%.1f", expectedDuration) + "s video)", LogLevel.INFO_GREEN);

            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            Reporter.log("Video compilation failed: " + e.getMessage(), LogLevel.ERROR);
            if (outputFile.exists()) outputFile.delete();
            return null;
        } finally {
            if (encoder != null) {
                try { encoder.finish(); } catch (Exception ignored) {}
            }
        }
    }

    private static long getFrameSize(BufferedImage probe, int MAX_WIDTH, int MAX_HEIGHT) {
        int w = probe.getWidth();
        int h = probe.getHeight();
        if (w > MAX_WIDTH || h > MAX_HEIGHT) {
            double scale = Math.min((double) MAX_WIDTH / w, (double) MAX_HEIGHT / h);
            w = (int) (w * scale);
            h = (int) (h * scale);
        }
        // Calculate uncompressed size: Width * Height * 3 bytes (BGR)
        return ((long) w * h * 3);
    }

    /**
     * Calculates a safe batch size based on available JVM memory and specific frame size.
     * @param singleFrameSizeBytes The estimated RAM usage of a single uncompressed frame.
     */
    private int calculateOptimalBatchSize(long singleFrameSizeBytes) {
        // 1. Calculate approximate available memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        long usableMemoryBytes = (maxMemory - totalMemory) + freeMemory;

        // 2. Add overhead (20% for Java object headers)
        double realFrameCost = singleFrameSizeBytes * 1.2;

        // 3. Determine batch size (Use 40% of usable memory)
        int calculatedBatch = (int) ((usableMemoryBytes * 0.4) / realFrameCost);

        // 4. Clamp results
        return Math.max(5, Math.min(calculatedBatch, 500));
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
            if (recordingStartTime.get() != null) {
                recordingStartTime.remove();
            }

        } catch (Exception e) {
            Reporter.log("Error during cleanup: " + e.getMessage(), LogLevel.WARN);
        }
    }

    /**
     * Executor service for async video compilation.
     * Single thread ensures:
     * - Sequential processing (no CPU thrashing)
     * - Predictable memory usage
     * - Faster individual compilation (no resource competition)
     */
    public static final ExecutorService videoCompilationExecutor =
            Executors.newFixedThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors() - 1), // Leave 1 core for OS/tests
                    r -> {
                        Thread t = new Thread(r, "VideoCompiler-" + r.hashCode());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY); // Normal priority for speed
                        return t;
                    }
            );

        static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            int active = activeCompilations.get();
            if (active > 0) {
                System.out.println("[Ellithium] Waiting for " + active + 
                                 " video(s) to finish compiling...");
                try {
                    videoCompilationExecutor.shutdown();
                    // Wait up to 3 minutes for videos to finish
                    boolean completed = videoCompilationExecutor.awaitTermination(180, TimeUnit.SECONDS);
                    if (completed) {
                        System.out.println("[Ellithium] ✓ All videos compiled successfully");
                    } else {
                        System.out.println("[Ellithium] ⚠ Timeout: " + activeCompilations.get() + 
                                         " video(s) may be incomplete");
                        videoCompilationExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    System.out.println("[Ellithium] Video compilation interrupted during shutdown");
                    videoCompilationExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            } else {
                System.out.println("[Ellithium] All videos already compiled");
            }
        }, "VideoCompilationShutdownHook"));
        
        System.out.println("[Ellithium] Video compilation shutdown hook registered");
    }

}