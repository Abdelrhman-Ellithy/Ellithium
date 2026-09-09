package Ellithium.Utilities.interactions;

import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.driver.LocalDriverType;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Shared base for all Arena real-browser integration tests.
 *
 * <p>
 * All Ellithium action wrappers are instantiated once and share a single
 * ChromeDriver session per test class. Every interaction goes through the
 * Ellithium API — never raw {@code driver.findElement()}.
 *
 * <p>
 * System properties:
 * <ul>
 * <li>{@code arena.headless} – {@code false} for headed mode (default:
 * {@code true})</li>
 * <li>{@code arena.base.url} – override the computed file:// base URL</li>
 * <li>{@code webdriver.chrome.driver} – explicit chromedriver path</li>
 * </ul>
 */
public abstract class ArenaBaseTest {

    protected WebDriver driver;

    // ── Ellithium action wrappers (all backed by the same driver) ─────────────
    protected ElementActions<WebDriver> elementActions;
    protected WaitActions<WebDriver> waitActions;
    protected AlertActions<WebDriver> alertActions;
    protected CookieActions<WebDriver> cookieActions;
    protected NavigationActions<WebDriver> navActions;
    protected FrameActions<WebDriver> frameActions;
    protected WindowActions<WebDriver> windowActions;
    protected MouseActions<WebDriver> mouseActions;
    protected SelectActions<WebDriver> selectActions;
    protected JavaScriptActions<WebDriver> jsActions;
    protected InteractionRecovery recovery;

    /** Base URL pointing at the test-website directory (file:// URI). */
    protected String BASE_URL;

    /**
     * Whether this class's browser was launched headless (see
     * {@code arena.headless}).
     */
    protected boolean headless;

    // Timeout / polling constants used by all arena tests
    protected static final int SHORT = 5; // seconds
    protected static final int MEDIUM = 10; // seconds
    protected static final int LONG = 15; // seconds
    protected static final int POLL = 300; // milliseconds

    @BeforeClass
    public void launchBrowser() {
        initializeArenaState();
    }

    @BeforeMethod(alwaysRun = true)
    public void ensureArenaInitialized() {
        if (driver == null || elementActions == null || waitActions == null || windowActions == null) {
            initializeArenaState();
        }
    }

    private void initializeArenaState() {
        BASE_URL = resolveBaseUrl();

        headless = !"false".equalsIgnoreCase(System.getProperty("arena.headless", "true"));
        driver = DriverFactory.getNewLocalDriver(
                LocalDriverType.Chrome,
                headless ? HeadlessMode.True : HeadlessMode.False);
        driver.get(BASE_URL + "index.html");

        elementActions = new ElementActions<>(driver);
        waitActions = new WaitActions<>(driver);
        alertActions = new AlertActions<>(driver);
        cookieActions = new CookieActions<>(driver);
        navActions = new NavigationActions<>(driver);
        frameActions = new FrameActions<>(driver);
        windowActions = new WindowActions<>(driver);
        mouseActions = new MouseActions<>(driver);
        selectActions = new SelectActions<>(driver);
        jsActions = new JavaScriptActions<>(driver);
        recovery = new InteractionRecovery(driver);
    }

    @AfterClass(alwaysRun = true)
    public void quitBrowser() {
        if (driver != null) {
            DriverFactory.quitDriver();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Navigate to a named arena page using Ellithium NavigationActions. */
    protected void goTo(String page) {
        ensureUsableContext();
        try {
            navActions.navigateToUrl(BASE_URL + page);
        } catch (org.openqa.selenium.UnhandledAlertException alertOpen) {
            try {
                driver.switchTo().alert().dismiss();
            } catch (org.openqa.selenium.WebDriverException ignored) {
            }
            navActions.navigateToUrl(BASE_URL + page);
        }
    }

    /**
     * Recovers a clean driver context left dirty by a sibling test in the same
     * class: dismisses a
     * stray alert and, if the current window handle was closed, switches to a
     * still-open one so the
     * next navigation does not fail with UnhandledAlert / NoSuchWindow.
     */
    private void ensureUsableContext() {
        try {
            driver.switchTo().alert().dismiss();
        } catch (org.openqa.selenium.WebDriverException ignored) {
        }
        try {
            driver.getWindowHandle();
        } catch (org.openqa.selenium.NoSuchWindowException windowGone) {
            java.util.Set<String> handles = driver.getWindowHandles();
            if (!handles.isEmpty())
                driver.switchTo().window(handles.iterator().next());
        } catch (org.openqa.selenium.WebDriverException ignored) {
        }
    }

    /** Execute a JS snippet via Ellithium JavaScriptActions scroll helper. */
    protected Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    private static String resolveBaseUrl() {
        String override = System.getProperty("arena.base.url");
        if (override != null && !override.isEmpty()) {
            return override.endsWith("/") ? override : override + "/";
        }
        File websiteDir = Paths.get(
                System.getProperty("user.dir"),
                "src", "test", "resources", "test-website").toFile();
        if (!websiteDir.exists()) {
            throw new IllegalStateException(
                    "test-website not found at: " + websiteDir.getAbsolutePath() +
                            "\nSet -Darena.base.url=file:///your/path/ to override.");
        }
        return ensureServer(websiteDir.toPath());
    }

    // ── Static localhost file server ──────────────────────────────────────────
    // Cookies, storage, and same-origin behaviour require an http(s) origin —
    // Chrome rejects
    // cookies on file:// (InvalidCookieDomainException). One server is shared by
    // all parallel
    // arena classes; it is a daemon and dies with the JVM.

    private static final Object SERVER_LOCK = new Object();
    private static volatile String serverBaseUrl;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "gif", "image/gif",
            "ico", "image/x-icon");

    private static String ensureServer(Path root) {
        String cached = serverBaseUrl;
        if (cached != null)
            return cached;
        synchronized (SERVER_LOCK) {
            if (serverBaseUrl != null)
                return serverBaseUrl;
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                Path base = root.toRealPath();
                server.createContext("/", exchange -> {
                    String rawPath = exchange.getRequestURI().getPath();
                    if (rawPath == null || rawPath.equals("/"))
                        rawPath = "/index.html";
                    Path target = base.resolve(rawPath.substring(1)).normalize();
                    byte[] body;
                    if (!target.startsWith(base) || !Files.isRegularFile(target)) {
                        body = "Not Found".getBytes();
                        exchange.getResponseHeaders().set("Content-Type", "text/plain");
                        exchange.sendResponseHeaders(404, body.length);
                    } else {
                        body = Files.readAllBytes(target);
                        exchange.getResponseHeaders().set("Content-Type", contentType(target));
                        exchange.sendResponseHeaders(200, body.length);
                    }
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                });
                server.setExecutor(Executors.newCachedThreadPool(r -> {
                    Thread t = new Thread(r, "arena-http");
                    t.setDaemon(true);
                    return t;
                }));
                server.start();
                serverBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
                return serverBaseUrl;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to start arena HTTP server", e);
            }
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }
}
