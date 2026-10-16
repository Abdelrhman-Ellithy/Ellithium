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
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
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
     * Stores one unique captured frame's raw image data.
     */
    private static final class FrameEntry {
        final byte[] data;
        FrameEntry(byte[] data) { this.data = data; }
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
     * Standard 10 FPS provides smooth, natural test action playback.
     */
    private static final int DEFAULT_FPS = 10;

    /**
     * Minimum interval between captured frames in milliseconds (~10 FPS max capture rate).
     * Prevents browser render loops from saturating DevTools WebSocket or queueing redundant frames.
     */
    private static final long MIN_FRAME_INTERVAL_MS = 100;

    /**
     * Interval between snapshots in milliseconds for Firefox/Safari recording.
     */
    private static final int SNAPSHOT_INTERVAL_MS = 100;

    /**
     * CDP screencast JPEG quality (0-100).
     * Quality 95 ensures crystal-clear UI text, sharp lines, and vivid color reproduction without artifacts.
     */
    private static final int CDP_JPEG_QUALITY = 95;

    /**
     * Atomic references tracking the previous frame for fast perceptual deduplication.
     */
    private final java.util.concurrent.atomic.AtomicReference<BufferedImage> lastDecodedImage =
            new java.util.concurrent.atomic.AtomicReference<>();
    private final java.util.concurrent.atomic.AtomicReference<byte[]> lastRawBytes =
            new java.util.concurrent.atomic.AtomicReference<>();
    private final java.util.concurrent.atomic.AtomicLong lastCaptureTimestamp =
            new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * Maximum wait time in milliseconds for executor shutdown.
     */
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_MS = 2000;

    private Long recordingStartTime;

    private String activeCdpVersion;
    private static final java.util.Set<Integer> cdpUnsupportedDrivers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static volatile String cdpVersionCache = null;
    private static final String CDP_NOT_FOUND = "";

    private static final AtomicInteger activeCompilations = new AtomicInteger(0);
    private volatile CompletableFuture<File> lastCompilationFuture = null;

    public static int pendingCompilations() { return activeCompilations.get(); }

    /**
     * Gets the CompletableFuture representing the compilation of the most recent recording.
     *
     * @return CompletableFuture resolving to the compiled video File, or null/completed future if no compilation
     */
    public CompletableFuture<File> getLastCompilationFuture() {
        return lastCompilationFuture;
    }

    /**
     * Blocks until all in-flight asynchronous video compilations have completed,
     * or the specified timeout elapses.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if all compilations completed, false if the timeout elapsed before all finished
     */
    public static boolean awaitPendingCompilations(long timeout, TimeUnit unit) {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (activeCompilations.get() > 0) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                Logger.warn("Timed out waiting for pending video compilations (" + activeCompilations.get() + " remaining)");
                return false;
            }
            try {
                Thread.sleep(Math.min(100, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.warn("Interrupted while waiting for pending video compilations");
                return false;
            }
        }
        return true;
    }

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
            Logger.warn("Screenshot name cannot be null or empty, using default");
            screenshotName = "screenshot";
        }

        try {
            if (!(driver instanceof TakesScreenshot camera)) {
                Logger.error("Driver does not support screenshots");
                return null;
            }
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            if (screenshot == null || !screenshot.exists()) {
                Logger.error("Screenshot file was not created");
                return null;
            }

            File screenShotFolder = new File(ConfigContext.getCapturedScreenShotPath() + File.separator);
            if (!screenShotFolder.exists()) {
                boolean created = screenShotFolder.mkdirs();
                if (!created) {
                    Logger.error("Failed to create screenshot folder: " + screenShotFolder.getPath());
                    return null;
                }
            }
            String sanitizedName = sanitizeFileName(screenshotName);
            String name = sanitizedName + "-" + TestDataGenerator.getTimeStamp();
            File screenShotFile = new File(screenShotFolder.getPath() + File.separator + name + ".png");
            Files.move(screenshot, screenShotFile);
            Logger.info("Screenshot captured: " + screenShotFile.getPath());
            Reporter.attachScreenshotToReport(screenShotFile, name, "Captured Screenshot");
            return screenShotFile;
        } catch (IOException e) {
            Logger.error("Failed to save screenshot: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Logger.error("Failed to capture screenshot: " + e.getMessage());
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
            Logger.warn("Recording name cannot be null or empty, using default");
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
                int driverId = System.identityHashCode(driver);
                if (!cdpUnsupportedDrivers.contains(driverId)) {
                    boolean cdpStarted = startCDPRecording(sanitizedName);
                    if (cdpStarted) {
                        return;
                    }
                    cdpUnsupportedDrivers.add(driverId);
                    Logger.warn("CDP recording failed, falling back to snapshot mode");
                }
            }

            startSnapshotRecording(sanitizedName);

        } catch (Exception e) {
            Logger.error("Failed to start recording: " + e.getMessage());
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
            Logger.warn("No active recording found to stop");
            cleanup();
            return null;
        }

        File videoFolder = new File(ConfigContext.getRecordedExecutionsPath() + File.separator);
        if (!videoFolder.exists()) {
            boolean created = videoFolder.mkdirs();
            if (!created) {
                Logger.error("Failed to create video folder: " + videoFolder.getPath());
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
                Logger.info("Video recording saved: " + path);
            }
        } catch (Exception e) {
            Logger.error("Error stopping recording: " + e.getMessage());
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
            Logger.info("Started mobile screen recording: " + name);
        } catch (Exception e) {
            Logger.error("Failed to start mobile recording: " + e.getMessage());
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
                Logger.warn("Could not unwrap ChromiumDriver, falling back to snapshot");
                return false;
            }
            DevTools devTools = chromiumDriver.getDevTools();
            devTools.createSessionIfThereIsNotOne();
            devToolsSession = devTools;

            String detectedVersion = detectCDPVersion(chromiumDriver);
            if (detectedVersion == null) {
                Logger.warn("No CDP version found in classpath");
                return false;
            }
            activeCdpVersion = detectedVersion;
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
                Logger.warn("Could not find CDP enable command in " + detectedVersion);
                Logger.logException(lastException);
                cleanupDevTools();
                return false;
            }

            Object startCommand = null;
            Method startMethod = findStartScreencastMethod(pageClass);
            if (startMethod == null) {
                Logger.warn("startScreencast method not found in " + detectedVersion);
                cleanupDevTools();
                return false;
            }

            try {
                Class<?>[] paramTypes = startMethod.getParameterTypes();
                if (paramTypes.length == 0) {
                    startCommand = startMethod.invoke(null);
                } else {
                    Object[] args = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i].equals(Optional.class)) {
                            if (i == 0) {
                                try {
                                    args[i] = Optional.of(getScreencastFormat(pageClass));
                                } catch (Exception ignored) {
                                    args[i] = Optional.empty();
                                }
                            } else if (i == 1) {
                                args[i] = Optional.of(CDP_JPEG_QUALITY);
                            } else if (i == 2) {
                                args[i] = Optional.of(1920); // maxWidth: full HD bounds to prevent internal browser downsampling
                            } else if (i == 3) {
                                args[i] = Optional.of(1080); // maxHeight: full HD bounds
                            } else if (i == 4) {
                                args[i] = Optional.of(2); // capture every 2nd rendered frame from browser
                            } else {
                                args[i] = Optional.empty();
                            }
                        } else {
                            args[i] = null;
                        }
                    }
                    startCommand = startMethod.invoke(null, args);
                }
            } catch (Exception e) {
                Logger.warn("Failed to invoke startScreencast in " + detectedVersion + ": " + e.getMessage());
                Logger.logException(e);
                cleanupDevTools();
                return false;
            }

            addScreencastFrameListener(devTools, pageClass);
            devTools.send((org.openqa.selenium.devtools.Command<?>) enableCommand);
            devTools.send((org.openqa.selenium.devtools.Command<?>) startCommand);
            Logger.debug("Started web recording (CDP " + detectedVersion + "): " + name);
            return true;

        } catch (ClassNotFoundException e) {
            Logger.warn("CDP Page class not found: " + e.getMessage());
            cleanupDevTools();
            return false;
        } catch (Exception e) {
            Logger.warn("CDP recording initialization failed: " + e.getMessage());
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

        for (int i = 0; i < maxUnwrapDepth && current != null; i++) {
            if (current instanceof ChromiumDriver chromiumDriver) {
                return chromiumDriver;
            }
            WebDriver next = unwrapNextDriver(current);
            if (next == null || next == current) {
                break;
            }
            current = next;
        }

        return null;
    }

    private static WebDriver unwrapNextDriver(WebDriver current) {
        if (current instanceof org.openqa.selenium.WrapsDriver wrapsDriver) {
            return wrapsDriver.getWrappedDriver();
        }

        try {
            java.lang.reflect.Field decoratedField = current.getClass().getDeclaredField("decorated");
            decoratedField.setAccessible(true);
            Object decorated = decoratedField.get(current);
            if (decorated instanceof WebDriver d) {
                return d;
            }
        } catch (Exception ignored) {}

        try {
            java.lang.reflect.Field driverField = current.getClass().getDeclaredField("driver");
            driverField.setAccessible(true);
            Object innerDriver = driverField.get(current);
            if (innerDriver instanceof WebDriver d) {
                return d;
            }
        } catch (Exception ignored) {}

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
                            cdpVersionCache = "v" + v;
                            return cdpVersionCache;
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
            for (int version = 200; version >= 85; version--) {
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
        final java.util.concurrent.atomic.AtomicReference<BufferedImage> prevImgRef = lastDecodedImage;
        final java.util.concurrent.atomic.AtomicReference<byte[]> prevBytesRef = lastRawBytes;
        final java.util.concurrent.atomic.AtomicLong lastTimeRef = lastCaptureTimestamp;

        devTools.addListener((org.openqa.selenium.devtools.Event<?>) screencastFrameEvent, frameData -> {
            if (recordingFlag.get()) {
                try {
                    // Extract session ID and acknowledge frame back to CDP immediately to maintain stream flow
                    Method getSessionIdMethod = frameData.getClass().getMethod("getSessionId");
                    Integer sessionId = (Integer) getSessionIdMethod.invoke(frameData);

                    Method ackMethod = null;
                    for (Method m : pageClass.getMethods()) {
                        if ("screencastFrameAck".equals(m.getName()) && java.lang.reflect.Modifier.isStatic(m.getModifiers())
                                && m.getParameterCount() == 1) {
                            ackMethod = m;
                            break;
                        }
                    }
                    if (ackMethod != null) {
                        Object ackCommand = ackMethod.invoke(null, sessionId);
                        devTools.send((org.openqa.selenium.devtools.Command<?>) ackCommand);
                    }

                    // Enforce capture interval throttling (~10 FPS max)
                    long now = System.currentTimeMillis();
                    if (now - lastTimeRef.get() < MIN_FRAME_INTERVAL_MS) {
                        return;
                    }

                    Method getDataMethod = frameData.getClass().getMethod("getData");
                    String base64Data = (String) getDataMethod.invoke(frameData);
                    byte[] imageData = Base64.getDecoder().decode(base64Data);

                    // Quick byte equality check
                    byte[] prevBytes = prevBytesRef.get();
                    if (prevBytes != null && java.util.Arrays.equals(prevBytes, imageData)) {
                        return;
                    }

                    // Decode candidate frame and verify if there is actual visual change
                    BufferedImage candidate = ImageIO.read(new ByteArrayInputStream(imageData));
                    if (candidate == null) return;

                    BufferedImage prevImg = prevImgRef.get();
                    if (prevImg != null && isSameVisualFrame(prevImg, candidate)) {
                        return; // Static screen - do NOT append duplicate frame!
                    }

                    // New unique visual state! Store frame and update state references
                    targetDeque.addLast(new FrameEntry(imageData));
                    prevImgRef.set(candidate);
                    prevBytesRef.set(imageData);
                    lastTimeRef.set(now);

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
            Logger.error("Driver does not support screenshots");
            throw new UnsupportedOperationException("Driver does not support screenshots");
        }

        final java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> targetDeque = videoFrames;
        final AtomicBoolean recordingFlag = isRecording;
        final TakesScreenshot screenshotDriver = (TakesScreenshot) rawDriver;
        final java.util.concurrent.atomic.AtomicReference<BufferedImage> prevImgRef = lastDecodedImage;
        final java.util.concurrent.atomic.AtomicReference<byte[]> prevBytesRef = lastRawBytes;

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            return Thread.ofPlatform().daemon(true).name("SnapshotRecorder-" + name).unstarted(r);
        });
        backgroundCapturer = executor;

        executor.scheduleAtFixedRate(() -> {
            if (recordingFlag.get()) {
                try {
                    Ellithium.core.execution.listener.ListenerLogSuppression.suppress();
                    byte[] screenshot;
                    try {
                        screenshot = screenshotDriver.getScreenshotAs(OutputType.BYTES);
                    } finally {
                        Ellithium.core.execution.listener.ListenerLogSuppression.resume();
                    }
                    if (screenshot != null && screenshot.length > 0) {
                        byte[] prevBytes = prevBytesRef.get();
                        if (prevBytes != null && java.util.Arrays.equals(prevBytes, screenshot)) {
                            return;
                        }

                        BufferedImage candidate = ImageIO.read(new ByteArrayInputStream(screenshot));
                        if (candidate == null) return;

                        BufferedImage prevImg = prevImgRef.get();
                        if (prevImg != null && isSameVisualFrame(prevImg, candidate)) {
                            return; // Static screen - do NOT append duplicate frame!
                        }

                        targetDeque.addLast(new FrameEntry(screenshot));
                        prevImgRef.set(candidate);
                        prevBytesRef.set(screenshot);
                    }
                } catch (Exception ignored) {}
            }
        }, 0, SNAPSHOT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        Logger.debug("Started web recording (Snapshot): " + name);
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
                Logger.error("Mobile recording returned empty data");
                return null;
            }

            byte[] videoData = Base64.getDecoder().decode(base64Video);
            String fileName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
            File videoFile = new File(videoFolder, fileName);

            try (FileOutputStream fos = new FileOutputStream(videoFile)) {
                fos.write(videoData);
                fos.flush();
            }
            this.lastCompilationFuture = CompletableFuture.completedFuture(videoFile);
            Logger.info("Mobile video recording saved: " + videoFile.getPath());
            return videoFile.getAbsolutePath();

        } catch (IllegalArgumentException e) {
            Logger.error("Invalid Base64 data from mobile recording: " + e.getMessage());
            this.lastCompilationFuture = CompletableFuture.completedFuture(null);
            return null;
        } catch (IOException e) {
            Logger.error("Failed to write mobile video file: " + e.getMessage());
            this.lastCompilationFuture = CompletableFuture.completedFuture(null);
            return null;
        } catch (Exception e) {
            Logger.error("Failed to stop mobile recording: " + e.getMessage());
            this.lastCompilationFuture = CompletableFuture.completedFuture(null);
            return null;
        }
    }

    /**
     * Stops web recording (CDP or Snapshot) and compiles frames to MP4 asynchronously.
     * The deque reference is taken before stopping the executor/CDP session, and one final
     * screenshot is appended before either is torn down — both CDP and snapshot recording stop
     * emitting frames the instant the recording flag clears.
     * Compilation is handed off to the background executor so the test thread and browser driver
     * are never blocked, allowing driver teardown and test cleanup to overlap with video compilation.
     */
    private String stopWebRecording(String name, File videoFolder) {
        try {
            java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> frames = videoFrames;

            if (driver instanceof TakesScreenshot) {
                try {
                    byte[] lastFrame = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    if (lastFrame != null && lastFrame.length > 0) {
                        byte[] prevBytes = lastRawBytes.get();
                        if (prevBytes == null || !java.util.Arrays.equals(prevBytes, lastFrame)) {
                            BufferedImage candidate = ImageIO.read(new ByteArrayInputStream(lastFrame));
                            BufferedImage prevImg = lastDecodedImage.get();
                            if (candidate != null && (prevImg == null || !isSameVisualFrame(prevImg, candidate))) {
                                frames.addLast(new FrameEntry(lastFrame));
                            }
                        }
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
                Logger.warn("No frames captured during recording");
                this.lastCompilationFuture = CompletableFuture.completedFuture(null);
                return null;
            }

            Long startTime = recordingStartTime;
            long durationMs = (startTime != null) ? System.currentTimeMillis() - startTime : 0;

            String fileName = name + "-" + TestDataGenerator.getTimeStamp() + ".mp4";
            File videoFile = new File(videoFolder, fileName);

            final java.util.concurrent.ConcurrentLinkedDeque<FrameEntry> framesCopy =
                    new java.util.concurrent.ConcurrentLinkedDeque<>(frames);
            final int frameCount = framesCopy.size();
            final long capturedDuration = durationMs;
            activeCompilations.incrementAndGet();

            CompletableFuture<File> compilationFuture = new CompletableFuture<>();
            try {
                videoCompilationExecutor.submit(() -> {
                    try {
                        String compiledPath = compileFramesToMP4(framesCopy, videoFile, capturedDuration);
                        if (compiledPath != null) {
                            Logger.info("Video compiled asynchronously: " + videoFile.getName() +
                                    " (" + frameCount + " frames)");
                            compilationFuture.complete(videoFile);
                        } else {
                            compilationFuture.complete(null);
                        }
                    } catch (Exception e) {
                        Logger.error("Async video compilation failed: " + e.getMessage());
                        compilationFuture.completeExceptionally(e);
                    } finally {
                        activeCompilations.decrementAndGet();
                    }
                });
            } catch (java.util.concurrent.RejectedExecutionException rex) {
                activeCompilations.decrementAndGet();
                compilationFuture.completeExceptionally(rex);
                Logger.error("Video compilation rejected (executor shut down): " + rex.getMessage());
            }

            this.lastCompilationFuture = compilationFuture;
            Logger.info("Video compilation dispatched in background: " + videoFile.getName() +
                    " (" + frameCount + " frames)");

            return videoFile.getAbsolutePath();

        } catch (Exception e) {
            Logger.error("Failed to stop web recording: " + e.getMessage());
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
                String detectedVersion = (activeCdpVersion != null) ? activeCdpVersion : detectCDPVersion(driver);
                if (detectedVersion != null) {
                    try {
                        Class<?> pageClass = Class.forName("org.openqa.selenium.devtools." + detectedVersion + ".page.Page");
                        Object stopCommand = null;
                        Method stopMethod = findStopScreencastMethod(pageClass);
                        if (stopMethod != null) {
                            if (stopMethod.getParameterCount() == 0) {
                                stopCommand = stopMethod.invoke(null);
                            } else {
                                Object[] args = new Object[stopMethod.getParameterCount()];
                                java.util.Arrays.fill(args, Optional.empty());
                                stopCommand = stopMethod.invoke(null, args);
                            }
                        } else {
                            try {
                                java.lang.reflect.Field stopField = pageClass.getField("stopScreencast");
                                stopCommand = stopField.get(null);
                            } catch (NoSuchFieldException e3) {
                                java.lang.reflect.Field stopField = pageClass.getDeclaredField("stopScreencast");
                                stopField.setAccessible(true);
                                stopCommand = stopField.get(null);
                            }
                        }

                        if (stopCommand != null) {
                            devTools.send((org.openqa.selenium.devtools.Command<?>) stopCommand);
                        }
                    } catch (Exception e) {
                        Logger.debug("Could not send CDP stopScreencast: " + e.getMessage());
                    }
                }
                cleanupDevTools();
            }
        } catch (Exception e) {
            Logger.warn("Error stopping screencast: " + e.getMessage());
        } finally {
            devToolsSession = null;
        }
    }

    private Method findStartScreencastMethod(Class<?> pageClass) {
        Method fallback = null;
        for (Method m : pageClass.getMethods()) {
            if ("startScreencast".equals(m.getName()) && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                if (m.getParameterCount() > 0) {
                    return m;
                }
                fallback = m;
            }
        }
        return fallback;
    }

    private Method findStopScreencastMethod(Class<?> pageClass) {
        for (Method m : pageClass.getMethods()) {
            if ("stopScreencast".equals(m.getName()) && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                return m;
            }
        }
        return null;
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
        int totalFrames = frames.size();
        if (totalFrames == 0) {
            Logger.warn("No frames to encode");
            return null;
        }
        AWTSequenceEncoder encoder = null;
        int successfulFrames = 0;
        final int MAX_WIDTH = 1920;
        final int MAX_HEIGHT = 1080;
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
            Logger.debug("Failed to calculate dynamic batch size, using default: " + e.getMessage());
        }

        try {
            int outputFPS = DEFAULT_FPS;

            encoder = AWTSequenceEncoder.createSequenceEncoder(outputFile, outputFPS);
            applyHighQualityRateControl(encoder);

            // If only 1-9 unique frames were captured, repeat each so the video has a playable minimum duration (~1 sec)
            int repeatPerFrame = (totalFrames < 10) ? Math.max(1, 10 / totalFrames) : 1;

            while (!frames.isEmpty()) {

                List<FrameEntry> entryBatch = new ArrayList<>(batchSize);
                for (int i = 0; i < batchSize && !frames.isEmpty(); i++) {
                    FrameEntry entry = frames.poll();
                    if (entry != null) entryBatch.add(entry);
                }

                if (entryBatch.isEmpty()) continue;

                // Parallelize frame decode, bicubic scaling, and YUV Picture conversion across all CPU cores
                List<Picture> processedBatch = entryBatch.parallelStream()
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

                                BufferedImage readyImage;
                                if (!needsScaling && origW == evenW && origH == evenH && original.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                                    readyImage = original;
                                } else {
                                    readyImage = new BufferedImage(evenW, evenH, BufferedImage.TYPE_3BYTE_BGR);
                                    Graphics2D g2d = readyImage.createGraphics();
                                    try {
                                        if (needsScaling) {
                                            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                                            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                        }
                                        g2d.drawImage(original, 0, 0, evenW, evenH, null);
                                    } finally {
                                        g2d.dispose();
                                    }
                                }

                                return AWTUtil.fromBufferedImageRGB(readyImage);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // Sequential bitstream encoding: encodes Picture directly
                for (Picture pic : processedBatch) {
                    if (pic == null) continue;
                    try {
                        for (int r = 0; r < repeatPerFrame; r++) {
                            encoder.encodeNativeFrame(pic);
                            successfulFrames++;
                        }
                    } catch (Exception e) {
                        Logger.debug("Failed to encode frame: " + e.getMessage());
                    }
                }

                processedBatch.clear();
                entryBatch.clear();
            }

            if (successfulFrames == 0) {
                Logger.error("No valid frames encoded");
                if (outputFile.exists()) outputFile.delete();
                return null;
            }

            double expectedDuration = (double) successfulFrames / outputFPS;
            Logger.info("Encoded " + successfulFrames + " frames (" + totalFrames +
                    " unique states) at " + outputFPS + " FPS (~" + String.format("%.1f", expectedDuration) + "s video)");

            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            Logger.error("Video compilation failed: " + e.getMessage());
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
     * Efficiently tests whether two frames are visually identical.
     * Uses a fast 32x32 distributed sample grid (1,024 points) across the frame with color tolerance.
     * Early-terminates in microseconds as soon as sufficient visual differences are detected.
     */
    private static boolean isSameVisualFrame(BufferedImage a, BufferedImage b) {
        if (a == null || b == null) return false;
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        int w = a.getWidth();
        int h = a.getHeight();
        int stepX = Math.max(1, w / 32);
        int stepY = Math.max(1, h / 32);
        int diffs = 0;
        int total = 0;
        for (int y = stepY / 2; y < h; y += stepY) {
            for (int x = stepX / 2; x < w; x += stepX) {
                total++;
                int rgbA = a.getRGB(x, y);
                int rgbB = b.getRGB(x, y);
                if (rgbA != rgbB) {
                    int rDiff = Math.abs(((rgbA >> 16) & 0xFF) - ((rgbB >> 16) & 0xFF));
                    int gDiff = Math.abs(((rgbA >> 8) & 0xFF) - ((rgbB >> 8) & 0xFF));
                    int bDiff = Math.abs((rgbA & 0xFF) - (rgbB & 0xFF));
                    if (rDiff > 8 || gDiff > 8 || bDiff > 8) {
                        diffs++;
                        if (diffs > (total * 0.02) && total > 20) {
                            return false;
                        }
                    }
                }
            }
        }
        return (double) diffs / total <= 0.02;
    }

    /**
     * Configures the JCodec SequenceEncoder with high-quality H.264 RateControl (QP 12/16)
     * instead of the default DumbRateControl (QP 20/26), producing crisp, readable UI text
     * and pristine line graphics without compression artifacts.
     */
    private static void applyHighQualityRateControl(org.jcodec.api.SequenceEncoder encoder) {
        if (encoder == null) return;
        try {
            java.lang.reflect.Field sinkField = org.jcodec.api.SequenceEncoder.class.getDeclaredField("sink");
            sinkField.setAccessible(true);
            Object sink = sinkField.get(encoder);
            if (sink != null) {
                java.lang.reflect.Field veField = sink.getClass().getDeclaredField("videoEncoder");
                veField.setAccessible(true);
                Object ve = veField.get(sink);
                if (ve instanceof org.jcodec.codecs.h264.H264Encoder) {
                    java.lang.reflect.Field rcField = ve.getClass().getDeclaredField("rc");
                    rcField.setAccessible(true);
                    rcField.set(ve, new org.jcodec.codecs.h264.encode.RateControl() {
                        @Override
                        public int startPicture(org.jcodec.common.model.Size size, int maxSize, org.jcodec.codecs.h264.io.model.SliceType sliceType) {
                            return (sliceType == org.jcodec.codecs.h264.io.model.SliceType.P) ? 16 : 12;
                        }
                        @Override
                        public int initialQpDelta() { return 0; }
                        @Override
                        public int accept(int bits) { return 0; }
                    });
                }
            }
        } catch (Exception e) {
            Logger.debug("Using default JCodec RateControl: " + e.getMessage());
        }
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
            lastDecodedImage.set(null);
            lastRawBytes.set(null);
            lastCaptureTimestamp.set(0);

        } catch (Exception e) {
            Logger.warn("Error during cleanup: " + e.getMessage());
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
                try {
                    videoCompilationExecutor.shutdown();
                    boolean completed = videoCompilationExecutor.awaitTermination(45, TimeUnit.SECONDS);
                    if (!completed) {
                        videoCompilationExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    videoCompilationExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }));
    }

}