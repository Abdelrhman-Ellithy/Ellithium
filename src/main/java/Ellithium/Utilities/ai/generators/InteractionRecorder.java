package Ellithium.Utilities.ai.generators;

import Ellithium.Utilities.ai.models.RecordedInteraction;
import Ellithium.Utilities.ai.provider.LLMProvider;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Playwright codegen-style interaction recorder for Ellithium.
 *
 * <p>Records every user interaction (click, type, select, navigate) performed
 * through the WebDriver, and generates POM + test code from the recording.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * // Start recording (injects a visible toolbar into the page)
 * InteractionRecorder recorder = InteractionRecorder.start(driver);
 *
 * // ... tester performs manual actions via driver ...
 * recorder.getDriver().findElement(By.id("login")).click();
 * recorder.getDriver().findElement(By.id("email")).sendKeys("admin@test.com");
 *
 * // Stop recording and generate code
 * List&lt;RecordedInteraction&gt; interactions = recorder.stop();
 * recorder.generateCode(llmProvider);
 * </pre>
 *
 * <h3>Toolbar</h3>
 * <p>When started, a floating toolbar is injected into the page via JavaScript.
 * The toolbar shows a recording indicator (red dot) and the number of captured
 * interactions. It is removed when recording stops.</p>
 *
 * <p><b>Important:</b> For full interaction capture, set {@code rootLogger.level=DEBUG}
 * in {@code log4j2.properties} before starting the recorder.</p>
 */
public class InteractionRecorder implements WebDriverListener {

    private final WebDriver originalDriver;
    private WebDriver decoratedDriver;
    private final List<RecordedInteraction> interactions = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean recording = false;

    private InteractionRecorder(WebDriver driver) {
        this.originalDriver = driver;
    }

    // ──────────────────────── Public API ────────────────────────

    /**
     * Starts recording interactions on the given driver.
     *
     * <p>Wraps the driver with an event-firing decorator that captures all
     * interactions. Injects a visual recording toolbar into the page.</p>
     *
     * @param driver The WebDriver to record interactions on
     * @return The recorder instance — use {@link #getDriver()} for subsequent operations
     */
    public static InteractionRecorder start(WebDriver driver) {
        InteractionRecorder recorder = new InteractionRecorder(driver);
        recorder.decoratedDriver = new EventFiringDecorator<>(recorder).decorate(driver);
        recorder.recording = true;
        recorder.injectToolbar();
        Reporter.log("InteractionRecorder: Recording started — use recorder.getDriver() for interactions", LogLevel.INFO_YELLOW);
        return recorder;
    }

    /**
     * Returns the decorated driver that captures interactions.
     * All WebDriver operations should go through this driver during recording.
     */
    public WebDriver getDriver() {
        return decoratedDriver != null ? decoratedDriver : originalDriver;
    }

    /**
     * Stops recording and removes the toolbar.
     *
     * @return The list of captured interactions
     */
    public List<RecordedInteraction> stop() {
        recording = false;
        removeToolbar();
        Reporter.log("InteractionRecorder: Recording stopped — " + interactions.size() + " interactions captured", LogLevel.INFO_GREEN);
        return new ArrayList<>(interactions);
    }

    /**
     * Generates POM + test code from the recorded interactions using the LLM.
     * Steps are executed immediately on the live driver, then POM is saved to disk.
     *
     * @param llmProvider The LLM provider to use for code generation
     */
    public void generateCode(LLMProvider llmProvider) {
        if (interactions.isEmpty()) {
            Reporter.log("InteractionRecorder: No interactions recorded — nothing to generate", LogLevel.WARN);
            return;
        }
        LiveContextGenerator.generateFromRecording(originalDriver, llmProvider, interactions);
    }

    /**
     * Returns the current list of recorded interactions (read-only snapshot).
     */
    public List<RecordedInteraction> getInteractions() {
        return new ArrayList<>(interactions);
    }

    /**
     * Returns whether the recorder is currently active.
     */
    public boolean isRecording() {
        return recording;
    }

    // ──────────────────────── WebDriverListener Overrides ────────────────────────

    @Override
    public void afterClick(WebElement element) {
        if (!recording) return;
        try {
            String locator = reconstructLocatorExpression(element);
            String name = getElementName(element);
            String tag = safeGetTag(element);
            interactions.add(new RecordedInteraction("click", locator, null, name, tag));
            updateToolbarCount();
            Reporter.log("InteractionRecorder: [" + interactions.size() + "] click on " + locator, LogLevel.DEBUG);
        } catch (Exception ignored) {}
    }

    @Override
    public void afterSendKeys(WebElement element, CharSequence... keysToSend) {
        if (!recording) return;
        try {
            String data = buildSentDataString(keysToSend);
            String locator = reconstructLocatorExpression(element);
            String name = getElementName(element);
            String tag = safeGetTag(element);
            interactions.add(new RecordedInteraction("sendData", locator, data, name, tag));
            updateToolbarCount();
            Reporter.log("InteractionRecorder: [" + interactions.size() + "] sendData \"" + data + "\" to " + locator, LogLevel.DEBUG);
        } catch (Exception ignored) {}
    }

    @Override
    public void afterTo(WebDriver.Navigation navigation, String url) {
        if (!recording) return;
        try {
            interactions.add(new RecordedInteraction("navigate", null, url, null, null));
            updateToolbarCount();
            Reporter.log("InteractionRecorder: [" + interactions.size() + "] navigate to " + url, LogLevel.DEBUG);
        } catch (Exception ignored) {}
    }

    @Override
    public void afterSubmit(WebElement element) {
        if (!recording) return;
        try {
            String locator = reconstructLocatorExpression(element);
            String name = getElementName(element);
            String tag = safeGetTag(element);
            interactions.add(new RecordedInteraction("submit", locator, null, name, tag));
            updateToolbarCount();
        } catch (Exception ignored) {}
    }

    // ──────────────────────── Element Introspection ────────────────────────

    /**
     * Reconstructs the best locator expression for a WebElement.
     * Priority: id > name > data-testid > css class > xpath
     */
    private String reconstructLocatorExpression(WebElement element) {
        try {
            String id = element.getAttribute("id");
            if (id != null && !id.isBlank()) return "By.id(\"" + id + "\")";

            String name = element.getAttribute("name");
            if (name != null && !name.isBlank()) return "By.name(\"" + name + "\")";

            String testId = element.getAttribute("data-testid");
            if (testId != null && !testId.isBlank()) return "By.cssSelector(\"[data-testid='" + testId + "']\")";

            String cssClass = element.getAttribute("class");
            String tag = element.getTagName();
            if (cssClass != null && !cssClass.isBlank() && tag != null) {
                String firstClass = cssClass.trim().split("\\s+")[0];
                return "By.cssSelector(\"" + tag + "." + firstClass + "\")";
            }

            return "By.tagName(\"" + (tag != null ? tag : "unknown") + "\")";
        } catch (Exception e) {
            return "By.tagName(\"unknown\")";
        }
    }

    /**
     * Gets a human-readable name for the element from accessible attributes.
     */
    private String getElementName(WebElement element) {
        try {
            String ariaLabel = element.getAttribute("aria-label");
            if (ariaLabel != null && !ariaLabel.isBlank()) return ariaLabel.trim();

            String placeholder = element.getAttribute("placeholder");
            if (placeholder != null && !placeholder.isBlank()) return placeholder.trim();

            String text = element.getText();
            if (text != null && !text.isBlank() && text.length() <= 50) return text.trim();

            String title = element.getAttribute("title");
            if (title != null && !title.isBlank()) return title.trim();

            return element.getAttribute("id");
        } catch (Exception e) {
            return null;
        }
    }

    private String safeGetTag(WebElement element) {
        try { return element.getTagName(); } catch (Exception e) { return "unknown"; }
    }

    private String buildSentDataString(CharSequence... keysToSend) {
        if (keysToSend == null || keysToSend.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (CharSequence cs : keysToSend) {
            sb.append(cs);
        }
        return sb.toString();
    }

    // ──────────────────────── Floating Toolbar ────────────────────────

    private static final String TOOLBAR_INJECT_SCRIPT = """
        (function() {
            if (document.getElementById('ellithium-recorder-toolbar')) return;
            var bar = document.createElement('div');
            bar.id = 'ellithium-recorder-toolbar';
            bar.style.cssText = 'position:fixed;top:10px;right:10px;z-index:2147483647;'
                + 'background:rgba(20,20,20,0.92);color:#fff;padding:8px 16px;border-radius:8px;'
                + 'font-family:system-ui,sans-serif;font-size:13px;display:flex;align-items:center;'
                + 'gap:8px;box-shadow:0 4px 12px rgba(0,0,0,0.4);backdrop-filter:blur(8px);';
            bar.innerHTML = '<span style="width:10px;height:10px;background:#ff3b30;border-radius:50%;'
                + 'display:inline-block;animation:pulse 1.2s infinite"></span>'
                + '<span>Ellithium Recording</span>'
                + '<span id="ellithium-rec-count" style="background:#333;padding:2px 8px;'
                + 'border-radius:4px;font-weight:bold;">0</span>';
            var style = document.createElement('style');
            style.textContent = '@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.3}}';
            document.head.appendChild(style);
            document.body.appendChild(bar);
        })();
        """;

    private static final String TOOLBAR_UPDATE_SCRIPT =
            "var c=document.getElementById('ellithium-rec-count');if(c)c.textContent=arguments[0];";

    private static final String TOOLBAR_REMOVE_SCRIPT =
            "var b=document.getElementById('ellithium-recorder-toolbar');if(b)b.remove();";

    private void injectToolbar() {
        try {
            if (originalDriver instanceof JavascriptExecutor js) {
                js.executeScript(TOOLBAR_INJECT_SCRIPT);
            }
        } catch (Exception e) {
            Reporter.log("InteractionRecorder: Could not inject toolbar (non-fatal): " + e.getMessage(), LogLevel.DEBUG);
        }
    }

    private void updateToolbarCount() {
        try {
            if (originalDriver instanceof JavascriptExecutor js) {
                js.executeScript(TOOLBAR_UPDATE_SCRIPT, String.valueOf(interactions.size()));
            }
        } catch (Exception ignored) {}
    }

    private void removeToolbar() {
        try {
            if (originalDriver instanceof JavascriptExecutor js) {
                js.executeScript(TOOLBAR_REMOVE_SCRIPT);
            }
        } catch (Exception ignored) {}
    }
}
