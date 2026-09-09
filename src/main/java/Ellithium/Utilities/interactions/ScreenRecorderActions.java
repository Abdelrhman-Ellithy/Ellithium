package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.config.management.ConfigContext;
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
 *   <li>Recording state is scoped to this instance - safe for parallel execution as long as
 *       each thread/test uses its own instance</li>
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
     * Stores one unique frame plus how many consecutive times it appeared.
     * Consecutive identical frames increment count instead of storing duplicate byte arrays —
     * keeping memory proportional to unique-frame count, not total-frame count.
     */
    private static final class FrameEntry {
        final byte[] data;
        volatile int count;
        FrameEntry(byte[] data) { this.data = data; this.count = 1; }
    }

    /**
     * Stores video frames captured during recording by this instance.
     * ConcurrentLinkedDeque is used because the CDP listener / snapshot executor writes
     * from a background thread while the main thread reads during stop. peekLast() is O(1).
     */
    private final java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> videoFrames =
        new java.util.concurrent.ConcurrentLinkedDeque<>();

    /**
     * The video name/identifier for this instance's active recording.
     */
    private String videoName;

    /**
     * Flag indicating if this instance's recording is currently active.
     * Used to control frame capture in CDP listeners and snapshot threads.
     */
    private final AtomicBoolean isRecording = new AtomicBoolean(false);

    /**
     * The background snapshot capture executor for this instance.
     * Used for Firefox/Safari browsers that don't support CDP.
     */
    private ScheduledExecutorService backgroundCapturer;

    /**
     * The DevTools session for this instance's CDP screencast recording.
     */
    private DevTools devToolsSession;

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

    private Long recordingStartTime;

    private static volatile String cdpVersionCache = null;
    private static final String CDP_NOT_FOUND = "";

    private static final AtomicInteger activeCompilations = new AtomicInteger(0);

    public static int pendingCompilations() { return activeCompilations.get(); }

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
     * <p><b>Thread Safety:</b> Recording state is scoped to this instance. Multiple tests can
     * record simultaneously without interference as long as each uses its own instance.
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
        videoName = sanitizedName;
        isRecording.set(true);
        videoFrames.clear();
        recordingStartTime = System.currentTimeMillis();

        try {
            if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
                startMobileRecording(sanitizedName);
                return;
            }

            DriverType driverType= DriverFactory.getCurrentDriverConfiguration().getDriverType();
            if (driverType== LocalDriverType.Chrome || driverType==LocalDriverType.Edge ||driverType== RemoteDriverType.REMOTE_Chrome || driverType==RemoteDriverType.REMOTE_Edge) {
                boolean cdpStarted = startCDPRecording(sanitizedName);
                if (cdpStarted) {
                    return;
                }
                Reporter.log("CDP recording failed, falling back to snapshot mode", LogLevel.WARN);
            }

            startSnapshotRecording(sanitizedName);

        } catch (Exception e) {
            Reporter.log("Failed to start recording: " + e.getMessage(), LogLevel.ERROR);
            isRecording.set(false);
            recordingStartTime = null;
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
     * The recording flag is cleared before the mobile/web stop strategy runs, so no new frame is
     * captured mid-teardown.
     *
     * @return Absolute path of the saved video file, null if recording failed or no frames captured
     */
    public String stopRecording() {
        String path = null;
        String name = videoName;

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
            isRecording.set(false);
            if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
                path = stopMobileRecording(name, videoFolder);
            }
            else {
                path = stopWebRecording(name, videoFolder);
            }
            if (path != null) {
                Reporter.log("Video recording saved: " + path, LogLevel.INFO_GREEN);
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
     * Starts CDP-based recording for Chromium browsers. The {@code Page.enable}/{@code startScreencast}
     * commands are located via reflection against the version-detected CDP {@code Page} class, trying
     * (in order) the {@code Optional}-parameter method signature (Selenium 4.10+), the no-arg method
     * signature (Selenium 4.0-4.9), a public field, then a declared field — since the exact shape of
     * these generated CDP command classes has changed across Selenium versions.
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
            devToolsSession = devTools;

            String detectedVersion = detectCDPVersion(chromiumDriver);
            if (detectedVersion == null) {
                Reporter.log("No CDP version found in classpath", LogLevel.WARN);
                return false;
            }
            Class<?> pageClass = Class.forName("org.openqa.selenium.devtools." + detectedVersion + ".page.Page");

            Object enableCommand = null;
            Exception lastException = null;

            try {
                Method enableMethod = pageClass.getMethod("enable", Optional.class);
                enableCommand = enableMethod.invoke(null, Optional.empty());
            } catch (NoSuchMethodException e) {
                lastException = e;

                try {
                    Method enableMethod = pageClass.getMethod("enable");
                    enableCommand = enableMethod.invoke(null);
                } catch (NoSuchMethodException e2) {
                    lastException = e2;

                    try {
                        java.lang.reflect.Field enableField = pageClass.getField("enable");
                        enableCommand = enableField.get(null);
                    } catch (NoSuchFieldException e3) {
                        lastException = e3;

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

            if (enableCommand == null) {
                Reporter.log("Could not find CDP enable command in " + detectedVersion, LogLevel.WARN);
                Logger.logException(lastException);
                devTools.close();
                devToolsSession = null;
                return false;
            }

            Object startCommand;
            try {
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
                try {
                    java.lang.reflect.Method startMethod = pageClass.getMethod("startScreencast");
                    startCommand = startMethod.invoke(null);
                } catch (NoSuchMethodException e2) {
                    Reporter.log("startScreencast method not found in " + detectedVersion, LogLevel.WARN);
                    devTools.close();
                    devToolsSession = null;
                    return false;
                }
            }

            addScreencastFrameListener(devTools, pageClass);
            devTools.send((org.openqa.selenium.devtools.Command<?>) enableCommand);
            devTools.send((org.openqa.selenium.devtools.Command<?>) startCommand);
            Reporter.log("Started web recording (CDP " + detectedVersion + "): " + name, LogLevel.DEBUG);
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
        if (devToolsSession != null) {
            try {
                devToolsSession.close();
            } catch (Exception ignored) {}
            devToolsSession = null;
        }
    }

    /**
     * Unwraps proxied/decorated drivers to get the actual ChromiumDriver instance, trying (in order)
     * the {@code WrapsDriver} interface, an {@code EventFiringDecorator}'s {@code decorated} field via
     * reflection, then a generic {@code driver} field (common in custom wrappers). Bounded to 10 levels
     * to guard against an unwrap cycle.
     */
    private ChromiumDriver unwrapChromiumDriver(WebDriver driver) {
        WebDriver current = driver;
        int maxUnwrapDepth = 10;

        for (int i = 0; i < maxUnwrapDepth; i++) {
            if (current instanceof ChromiumDriver) {
                return (ChromiumDriver) current;
            }

            if (current instanceof org.openqa.selenium.WrapsDriver) {
                current = ((org.openqa.selenium.WrapsDriver) current).getWrappedDriver();
                continue;
            }

            try {
                java.lang.reflect.Field decoratedField = current.getClass().getDeclaredField("decorated");
                decoratedField.setAccessible(true);
                Object decorated = decoratedField.get(current);
                if (decorated instanceof WebDriver) {
                    current = (WebDriver) decorated;
                    continue;
                }
            } catch (Exception ignored) {}

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
     * Dynamically detects available CDP version in classpath based on running browser version.
     */
    private String detectCDPVersion(WebDriver d) {
        if (d instanceof org.openqa.selenium.HasCapabilities hasCaps) {
            String browserVersion = hasCaps.getCapabilities().getBrowserVersion();
            if (browserVersion != null && !browserVersion.isBlank()) {
                try {
                    int major = Integer.parseInt(browserVersion.split("\\.")[0]);
                    for (int v = major; v >= Math.max(85, major - 5); v--) {
                        try {
                            Class.forName("org.openqa.selenium.devtools.v" + v + ".page.Page");
                            return "v" + v;
                        } catch (ClassNotFoundException ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }
        return detectCDPVersion();
    }

    /**
     * Dynamically detects available CDP version in classpath.
     * Scans for org.openqa.selenium.devtools.v* packages.
     */
    private String detectCDPVersion() {
        String cached = cdpVersionCache;
        if (cached != null) return cached.isEmpty() ? null : cached;
        synchronized (ScreenRecorderActions.class) {
            cached = cdpVersionCache;
            if (cached != null) return cached.isEmpty() ? null : cached;
            for (int version = 150; version >= 85; version--) {
                try {
                    Class.forName("org.openqa.selenium.devtools.v" + version + ".page.Page");
                    cdpVersionCache = "v" + version;
                    return cdpVersionCache;
                } catch (ClassNotFoundException ignored) {}
            }
            cdpVersionCache = CDP_NOT_FOUND;
            return null;
        }
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
     * Registers a CDP screencast-frame listener that decodes each frame's Base64 image data,
     * dedupes against the last stored frame, and acknowledges the frame back to CDP via reflection
     * (frame field/ack-command shapes vary by CDP version, same as {@link #startCDPRecording}).
     * Individual frame errors are swallowed to keep the screencast stream alive.
     */
    private void addScreencastFrameListener(DevTools devTools, Class<?> pageClass) throws Exception {
        Object screencastFrameEvent = pageClass.getMethod("screencastFrame").invoke(null);

        final java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> targetDeque = videoFrames;
        final AtomicBoolean recordingFlag = isRecording;

        devTools.addListener((org.openqa.selenium.devtools.Event<?>) screencastFrameEvent, frameData -> {
            if (recordingFlag.get()) {
                try {
                    Method getDataMethod = frameData.getClass().getMethod("getData");
                    String base64Data = (String) getDataMethod.invoke(frameData);
                    byte[] imageData = Base64.getDecoder().decode(base64Data);
                    FrameEntry last = targetDeque.peekLast();
                    if (last != null && java.util.Arrays.equals(last.data, imageData)) {
                        last.count++;
                    } else {
                        targetDeque.addLast(new FrameEntry(imageData));
                    }

                    Method getSessionIdMethod = frameData.getClass().getMethod("getSessionId");
                    Integer sessionId = (Integer) getSessionIdMethod.invoke(frameData);

                    Method ackMethod;
                    try {
                        ackMethod = pageClass.getMethod("screencastFrameAck", Integer.class);
                    } catch (NoSuchMethodException nsme) {
                        ackMethod = pageClass.getMethod("screencastFrameAck", int.class);
                    }
                    Object ackCommand = ackMethod.invoke(null, sessionId);
                    devTools.send((org.openqa.selenium.devtools.Command<?>) ackCommand);
                } catch (Exception e) {
                    Logger.debug("Error processing screencast frame: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Starts snapshot-based recording for Firefox/Safari or as fallback. The frame deque, recording
     * flag, and driver reference are captured into local variables up front for use inside the
     * background capture task's closure.
     */
    private void startSnapshotRecording(String name) {
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

        final java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> targetDeque = videoFrames;
        final AtomicBoolean recordingFlag = isRecording;
        final TakesScreenshot screenshotDriver = (TakesScreenshot) rawDriver;

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            return Thread.ofPlatform().daemon(true).name("SnapshotRecorder-" + name).unstarted(r);
        });
        backgroundCapturer = executor;

        executor.scheduleAtFixedRate(() -> {
            if (recordingFlag.get()) {
                try {
                    byte[] screenshot = screenshotDriver.getScreenshotAs(OutputType.BYTES);
                    if (screenshot != null && screenshot.length > 0) {
                        FrameEntry last = targetDeque.peekLast();
                        if (last != null && java.util.Arrays.equals(last.data, screenshot)) {
                            last.count++;
                        } else {
                            targetDeque.addLast(new FrameEntry(screenshot));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, 0, SNAPSHOT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        Reporter.log("Started web recording (Snapshot): " + name, LogLevel.DEBUG);
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
     * Stops web recording (CDP or Snapshot) and compiles frames to MP4. The deque reference is taken
     * before stopping the executor/CDP session, and one final screenshot is appended before either is
     * torn down — both CDP and snapshot recording stop emitting frames the instant the recording flag
     * clears, which would otherwise leave a gap between the last captured frame and the true end state.
     * Recording duration is captured here (on the calling thread) since the compile step may run async.
     * When report attachment is needed the video is compiled synchronously so it's ready to attach;
     * otherwise compilation is handed off to the background executor so the test isn't blocked, and
     * this method returns the (not-yet-ready) output path immediately.
     */
    private String stopWebRecording(String name, File videoFolder) {
        try {
            java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> frames = videoFrames;

            if (driver instanceof TakesScreenshot) {
                try {
                    byte[] lastFrame = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    if (lastFrame != null && lastFrame.length > 0) {
                        frames.addLast(new FrameEntry(lastFrame));
                    }
                } catch (Exception ignored) {}
            }

            if (devToolsSession != null) {
                stopCDPScreencast();
            }

            if (backgroundCapturer != null) {
                stopSnapshotExecutor();
            }

            if (frames.isEmpty()) {
                Reporter.log("No frames captured during recording", LogLevel.WARN);
                return null;
            }

            Long startTime = recordingStartTime;
            long durationMs = (startTime != null) ? System.currentTimeMillis() - startTime : 0;

            String fileName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
            File videoFile = new File(videoFolder, fileName);

            boolean needsAttachment = isAttachmentEnabled();
            if (needsAttachment) {
                Reporter.log("Compiling video synchronously for report attachment", LogLevel.INFO_GREEN);
                return compileFramesToMP4(frames, videoFile, durationMs);
            } else {
                final java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> framesCopy =
                        new java.util.concurrent.ConcurrentLinkedDeque<>(frames);
                final int frameCount = framesCopy.stream().mapToInt(e -> e.count).sum();
                final long capturedDuration = durationMs;
                activeCompilations.incrementAndGet();
                try {
                    videoCompilationExecutor.submit(() -> {
                        try {
                            compileFramesToMP4(framesCopy, videoFile, capturedDuration);
                            Logger.info("Video compiled asynchronously: " + videoFile.getName() +
                                    " (" + frameCount + " frames)");
                            // needsAttachment was already false when this task was queued, and that
                            // decision (isAttachmentEnabled()) is the same check handleVideoAttachment
                            // makes later — so this file is guaranteed to never be attached. Delete it
                            // here instead of leaving it for handleVideoAttachment's delete, which would
                            // otherwise race this still-in-flight compilation and silently no-op.
                            try { java.nio.file.Files.deleteIfExists(videoFile.toPath()); } catch (Exception ignored) {}
                        } catch (Exception e) {
                            Logger.error("Async video compilation failed: " + e.getMessage());
                        }
                        finally {
                            activeCompilations.decrementAndGet();
                        }
                    });
                } catch (java.util.concurrent.RejectedExecutionException rex) {
                    activeCompilations.decrementAndGet();
                    Logger.error("Video compilation rejected (executor shut down): " + rex.getMessage());
                }

                Reporter.log("Video compilation started in background (" + frameCount + " frames)",
                        LogLevel.INFO_BLUE);

                return videoFile.getAbsolutePath();
            }

        } catch (Exception e) {
            Reporter.log("Failed to stop web recording: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Stops the CDP screencast session with dynamic version detection. {@code Page.stopScreencast} is
     * located via the same reflection fallback chain as {@link #startCDPRecording} (Optional-parameter
     * method, no-arg method, public field, then declared field).
     */
    private void stopCDPScreencast() {
        try {
            DevTools devTools = devToolsSession;
            if (devTools != null) {
                String detectedVersion = detectCDPVersion();
                if (detectedVersion != null) {
                    try {
                        Class<?> pageClass = Class.forName("org.openqa.selenium.devtools." + detectedVersion + ".page.Page");
                        Object stopCommand = null;
                        try {
                            Method stopMethod = pageClass.getMethod("stopScreencast", Optional.class);
                            stopCommand = stopMethod.invoke(null, Optional.empty());
                        } catch (NoSuchMethodException e) {
                            try {
                                Method stopMethod = pageClass.getMethod("stopScreencast");
                                stopCommand = stopMethod.invoke(null);
                            } catch (NoSuchMethodException e2) {
                                try {
                                    java.lang.reflect.Field stopField = pageClass.getField("stopScreencast");
                                    stopCommand = stopField.get(null);
                                } catch (NoSuchFieldException e3) {
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
            devToolsSession = null;
        }
    }

    /**
     * Stops snapshot capture executor.
     */
    private void stopSnapshotExecutor() {
        ScheduledExecutorService executor = backgroundCapturer;
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
                backgroundCapturer = null;
            }
        }
    }

    /**
     * Compiles captured frames into an MP4 video file using JCodec. Uses batched parallel processing
     * to balance high speed with low memory usage: each batch is (A) polled off the deque one slot per
     * unique frame, (B) decoded in parallel (each unique frame decoded exactly once), (C) encoded with
     * each decoded image repeated {@code entry.count} times to reconstruct consecutive-duplicate runs
     * losslessly, then (D) the batch buffers are cleared before the next batch starts.
     *
     * @param frames Queue of frame data
     * @param outputFile Output video file
     * @param recordingDurationMs Recording duration in milliseconds (0 if unknown)
     * @return Absolute path of compiled video, or null if failed
     */
    private String compileFramesToMP4(java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> frames,
                                       File outputFile, long recordingDurationMs) {
        int totalFrames = frames.stream().mapToInt(e -> e.count).sum();
        if (totalFrames == 0) {
            Reporter.log("No frames to encode", LogLevel.WARN);
            return null;
        }
        AWTSequenceEncoder encoder = null;
        int successfulFrames = 0;
        final int MAX_WIDTH = 854;
        final int MAX_HEIGHT = 480;
        int batchSize = 50;
        try {
            FrameEntry firstEntry = frames.peek();
            if (firstEntry != null) {
                BufferedImage probe = ImageIO.read(new ByteArrayInputStream(firstEntry.data));
                if (probe != null) {
                    long frameSize = getFrameSize(probe, MAX_WIDTH, MAX_HEIGHT);
                    batchSize = calculateOptimalBatchSize(frameSize);
                }
            }
        } catch (Exception e) {
            Reporter.log("Failed to calculate dynamic batch size, using default: " + e.getMessage(), LogLevel.DEBUG);
        }

        try {
            double actualCaptureFPS = (double) totalFrames / (recordingDurationMs / 1000.0);
            int outputFPS = (actualCaptureFPS >= 1 && actualCaptureFPS <= 60)
                    ? Math.max(1, (int) Math.round(actualCaptureFPS))
                    : DEFAULT_FPS;

            encoder = AWTSequenceEncoder.createSequenceEncoder(outputFile, outputFPS);

            while (!frames.isEmpty()) {

                List<FrameEntry> entryBatch = new ArrayList<>(batchSize);
                for (int i = 0; i < batchSize && !frames.isEmpty(); i++) {
                    FrameEntry entry = frames.poll();
                    if (entry != null) entryBatch.add(entry);
                }

                if (entryBatch.isEmpty()) continue;

                List<BufferedImage> processedBatch = entryBatch.parallelStream()
                        .map(entry -> {
                            try {
                                BufferedImage original = ImageIO.read(new ByteArrayInputStream(entry.data));
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
                                try {
                                    if (needsScaling) {
                                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                    }
                                    g2d.drawImage(original, 0, 0, evenW, evenH, null);
                                } finally {
                                    g2d.dispose();
                                }
                                return resized;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .collect(Collectors.toList());

                for (int i = 0; i < processedBatch.size(); i++) {
                    BufferedImage img = processedBatch.get(i);
                    if (img == null) continue;
                    int repeatCount = entryBatch.get(i).count;
                    try {
                        for (int r = 0; r < repeatCount; r++) {
                            encoder.encodeImage(img);
                            successfulFrames++;
                        }
                    } catch (Exception e) {
                        Reporter.log("Failed to encode frame " + i + ": " + e.getMessage(), LogLevel.DEBUG);
                    }
                }

                processedBatch.clear();
                entryBatch.clear();
            }

            if (successfulFrames == 0) {
                Reporter.log("No valid frames encoded", LogLevel.ERROR);
                if (outputFile.exists()) outputFile.delete();
                return null;
            }

            double expectedDuration = (double) successfulFrames / outputFPS;
            Reporter.log("Encoded " + successfulFrames + "/" + totalFrames +
                    " frames at " + outputFPS + " FPS (~" + String.format("%.1f", expectedDuration) + "s video)",
                    successfulFrames < totalFrames ? LogLevel.WARN : LogLevel.INFO_GREEN);

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

    /** Estimated uncompressed in-memory size of the frame (width * height * 3 bytes for BGR) after any scaling. */
    private static long getFrameSize(BufferedImage probe, int MAX_WIDTH, int MAX_HEIGHT) {
        int w = probe.getWidth();
        int h = probe.getHeight();
        if (w > MAX_WIDTH || h > MAX_HEIGHT) {
            double scale = Math.min((double) MAX_WIDTH / w, (double) MAX_HEIGHT / h);
            w = (int) (w * scale);
            h = (int) (h * scale);
        }
        return ((long) w * h * 3);
    }

    /**
     * Calculates a safe batch size based on available JVM memory and specific frame size: takes
     * currently-usable memory ({@code (max - total) + free}), adds 20% overhead for Java object
     * headers, uses 40% of the usable memory for the batch, then clamps the result to [5, 500].
     *
     * @param singleFrameSizeBytes The estimated RAM usage of a single uncompressed frame.
     */
    private int calculateOptimalBatchSize(long singleFrameSizeBytes) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        long usableMemoryBytes = (maxMemory - totalMemory) + freeMemory;

        double realFrameCost = singleFrameSizeBytes * 1.2;

        int calculatedBatch = (int) ((usableMemoryBytes * 0.4) / realFrameCost);

        return Math.max(5, Math.min(calculatedBatch, 500));
    }

    /**
     * Sanitizes a file name by removing invalid characters for Windows, Linux, and macOS.
     *
     * @param fileName Original file name
     * @return Sanitized file name safe for all operating systems
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "recording";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                      .replaceAll("\\s+", "_")
                      .replaceAll("_{2,}", "_")
                      .trim();
    }

    /**
     * Cleans up this instance's recording resources to prevent memory/resource leaks.
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
            if (backgroundCapturer != null) {
                stopSnapshotExecutor();
            }

            if (devToolsSession != null) {
                try {
                    devToolsSession.close();
                } catch (Exception ignored) {}
                devToolsSession = null;
            }

            videoFrames.clear();
            videoName = null;
            isRecording.set(false);
            recordingStartTime = null;

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
     *
     * <p>A JVM shutdown hook waits up to 45 seconds for any in-flight compilations to finish before
     * the process exits, so a test run doesn't drop its final videos on exit.
     */
    private static final ExecutorService videoCompilationExecutor =
            new java.util.concurrent.ThreadPoolExecutor(
                    Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
                    Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
                    0L, TimeUnit.MILLISECONDS,
                    new java.util.concurrent.LinkedBlockingQueue<>(),
                    Thread.ofPlatform().daemon(true).name("VideoCompiler-", 0).priority(Thread.NORM_PRIORITY).factory());

        static {
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("VideoCompilationShutdownHook").unstarted(() -> {
            int active = activeCompilations.get();
            if (active > 0) {
                System.out.println("[Ellithium] Waiting for " + active +
                                 " video(s) to finish compiling...");
                try {
                    videoCompilationExecutor.shutdown();
                    boolean completed = videoCompilationExecutor.awaitTermination(45, TimeUnit.SECONDS);
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
        }));
        
        System.out.println("[Ellithium] Video compilation shutdown hook registered");
    }

}