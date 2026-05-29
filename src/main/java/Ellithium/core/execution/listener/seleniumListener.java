package Ellithium.core.execution.listener;
import Ellithium.core.ai.generators.LiveContextGenerator;
import Ellithium.core.ai.models.RecordedInteraction;
import Ellithium.core.ai.provider.LLMProvider;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.*;

import java.net.URL;
import java.time.Duration;
import java.util.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.events.WebDriverListener;
public class seleniumListener implements WebDriverListener {

    private static volatile boolean RECORDING = false;
    private static volatile WebDriver RECORDING_DRIVER = null;
    private static final List<RecordedInteraction> RECORDED =
            Collections.synchronizedList(new ArrayList<>());

    private static final String TOOLBAR_INJECT_SCRIPT =
            "(function() {"
            + "  if (document.getElementById('ellithium-recorder-toolbar')) return;"
            + "  var bar = document.createElement('div');"
            + "  bar.id = 'ellithium-recorder-toolbar';"
            + "  bar.style.cssText = 'position:fixed;top:10px;right:10px;z-index:2147483647;'"
            + "    + 'background:rgba(20,20,20,0.92);color:#fff;padding:8px 16px;border-radius:8px;'"
            + "    + 'font-family:system-ui,sans-serif;font-size:13px;display:flex;align-items:center;'"
            + "    + 'gap:8px;box-shadow:0 4px 12px rgba(0,0,0,0.4);backdrop-filter:blur(8px);';"
            + "  bar.innerHTML = '<span style=\"width:10px;height:10px;background:#ff3b30;border-radius:50%;'"
            + "    + 'display:inline-block;animation:pulse 1.2s infinite\"></span>'"
            + "    + '<span>Ellithium Recording</span>'"
            + "    + '<span id=\"ellithium-rec-count\" style=\"background:#333;padding:2px 8px;'"
            + "    + 'border-radius:4px;font-weight:bold;\">0</span>';"
            + "  var style = document.createElement('style');"
            + "  style.textContent = '@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.3}}';"
            + "  document.head.appendChild(style);"
            + "  document.body.appendChild(bar);"
            + "})();";

    private static final String TOOLBAR_UPDATE_SCRIPT =
            "var c=document.getElementById('ellithium-rec-count');if(c)c.textContent=arguments[0];";

    private static final String TOOLBAR_REMOVE_SCRIPT =
            "var b=document.getElementById('ellithium-recorder-toolbar');if(b)b.remove();";

    /**
     * Starts capturing user-driven WebDriver interactions for Playwright-style codegen.
     * Toggles a process-wide flag that the existing after-callbacks check; no decorator wrap.
     */
    public static void startRecording(WebDriver driver) {
        RECORDED.clear();
        RECORDING_DRIVER = driver;
        RECORDING = true;
        injectRecordingToolbar(driver);
        Reporter.log("seleniumListener: Recording started", LogLevel.INFO_YELLOW);
    }

    /** Stops capturing, removes the toolbar, returns the captured interactions. */
    public static List<RecordedInteraction> stopRecording() {
        RECORDING = false;
        removeRecordingToolbar();
        List<RecordedInteraction> snapshot = new ArrayList<>(RECORDED);
        RECORDING_DRIVER = null;
        Reporter.log("seleniumListener: Recording stopped — " + snapshot.size() + " interactions captured",
                LogLevel.INFO_GREEN);
        return snapshot;
    }

    /** Hands the recorded interactions to the codegen pipeline. */
    public static void generateCode(LLMProvider llmProvider) {
        if (RECORDED.isEmpty()) {
            Reporter.log("seleniumListener: No interactions recorded — nothing to generate", LogLevel.WARN);
            return;
        }
        WebDriver driver = RECORDING_DRIVER;
        if (driver == null) {
            Reporter.log("seleniumListener: generateCode called with no active driver", LogLevel.WARN);
            return;
        }
        LiveContextGenerator.generateFromRecording(driver, llmProvider, new ArrayList<>(RECORDED));
    }

    public static List<RecordedInteraction> getInteractions() { return new ArrayList<>(RECORDED); }
    public static boolean isRecording() { return RECORDING; }

    private static void injectRecordingToolbar(WebDriver driver) {
        try {
            if (driver instanceof JavascriptExecutor js) js.executeScript(TOOLBAR_INJECT_SCRIPT);
        } catch (Exception e) {
            Reporter.log("seleniumListener: Toolbar inject failed (non-fatal): " + e.getMessage(), LogLevel.DEBUG);
        }
    }

    private static void updateRecordingToolbar() {
        try {
            WebDriver d = RECORDING_DRIVER;
            if (d instanceof JavascriptExecutor js) {
                js.executeScript(TOOLBAR_UPDATE_SCRIPT, String.valueOf(RECORDED.size()));
            }
        } catch (Exception ignored) {}
    }

    private static void removeRecordingToolbar() {
        try {
            WebDriver d = RECORDING_DRIVER;
            if (d instanceof JavascriptExecutor js) js.executeScript(TOOLBAR_REMOVE_SCRIPT);
        } catch (Exception ignored) {}
    }

    private static String reconstructLocatorExpression(WebElement element) {
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

    private static String recordedElementName(WebElement element) {
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

    private static String recordedTag(WebElement element) {
        try { return element.getTagName(); } catch (Exception e) { return "unknown"; }
    }

    /**
     * Thread-local flag to suppress logging during internal AI operations
     * (e.g., ElementFingerprint capture reads 13+ attributes per element).
     * Set to true via {@link #suppressLogging()} and reset via {@link #resumeLogging()}.
     */
    private static final ThreadLocal<Integer> SUPPRESS_DEPTH = ThreadLocal.withInitial(() -> 0);

    /** Suppress listener logging on the current thread (reentrant — pair every call with resumeLogging()). */
    public static void suppressLogging() { SUPPRESS_DEPTH.set(SUPPRESS_DEPTH.get() + 1); }

    /** Resume listener logging on the current thread (decrements the suppression depth). */
    public static void resumeLogging() { SUPPRESS_DEPTH.set(Math.max(0, SUPPRESS_DEPTH.get() - 1)); }

    /** Returns true if logging is currently suppressed. */
    private static boolean isSuppressed() { return SUPPRESS_DEPTH.get() > 0; }

    @Override
    public void afterSendKeys(WebElement element, CharSequence... keysToSend) {
        String sentData = buildSentDataString(keysToSend);
        Reporter.log("Sent Data: \"" + sentData + "\" into " + nameOf(element) + ".", LogLevel.INFO_BLUE);
        if (RECORDING) {
            RECORDED.add(new RecordedInteraction("sendData", reconstructLocatorExpression(element),
                    sentData, recordedElementName(element), recordedTag(element)));
            updateRecordingToolbar();
        }
    }
    
    private String buildSentDataString(CharSequence... keysToSend) {
        StringBuilder stringBuilder = new StringBuilder();
        for (CharSequence charSequence : keysToSend) {
            if (charSequence instanceof Keys) {
                stringBuilder.append(getKeyName((Keys) charSequence));
            } else {
                stringBuilder.append(charSequence);
            }
        }
        return stringBuilder.toString();
    }
   @Override
   public void afterGet(WebDriver driver, String url) {
       Reporter.log("Navigating to URL: ", LogLevel.INFO_BLUE, url);
   }
   @Override
    public void afterGetCurrentUrl(WebDriver driver, String url) {
        if (isSuppressed()) return;
        Reporter.log("Current URL retrieved: " + url, LogLevel.DEBUG);
    }
    @Override
    public void afterDefaultContent(WebDriver.TargetLocator targetLocator, WebDriver driver) {
        Reporter.log("Switched Back To Default Content From Frame" , LogLevel.INFO_BLUE);
    }
   @Override
   public void afterGetTitle(WebDriver driver, String title) {
       Reporter.log("Page title retrieved: " + title, LogLevel.DEBUG);
   }
   @Override
    public void afterGetPageSource(WebDriver driver, String source) {
        if (isSuppressed()) return;
        Reporter.log("Page source retrieved", LogLevel.DEBUG);
    }
   @Override
   public void afterClose(WebDriver driver) {
       Reporter.log("WebDriver closed", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterQuit(WebDriver driver) {
       Reporter.log("WebDriver quit", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterGetWindowHandles(WebDriver driver, Set<String> result) {
       Reporter.log("Window handles retrieved: " + result, LogLevel.DEBUG);
   }
   @Override
   public void afterGetWindowHandle(WebDriver driver, String result) {
       Reporter.log("Window handle retrieved: " + result, LogLevel.DEBUG);
   }
   @Override
    public void afterExecuteScript(WebDriver driver, String script, Object[] args, Object result) {
        if (isSuppressed()) return;
        Reporter.log("Executed script (" + scriptLen(script) + " chars)", LogLevel.DEBUG);
    }
   @Override
   public void afterExecuteAsyncScript(WebDriver driver, String script, Object[] args, Object result) {
       if (isSuppressed()) return;
       Reporter.log("Executed async script (" + scriptLen(script) + " chars)", LogLevel.DEBUG);
   }

   private static int scriptLen(String script) { return script != null ? script.length() : 0; }
   @Override
   public void afterClick(WebElement element) {
       Reporter.log("Clicked on element: " + nameOf(element), LogLevel.INFO_BLUE);
       if (RECORDING) {
           RECORDED.add(new RecordedInteraction("click", reconstructLocatorExpression(element),
                   null, recordedElementName(element), recordedTag(element)));
           updateRecordingToolbar();
       }
   }
   @Override
   public void afterSubmit(WebElement element) {
       Reporter.log("Submitted element: " + nameOf(element), LogLevel.INFO_BLUE);
       if (RECORDING) {
           RECORDED.add(new RecordedInteraction("submit", reconstructLocatorExpression(element),
                   null, recordedElementName(element), recordedTag(element)));
           updateRecordingToolbar();
       }
   }
    @Override
    public void afterGetTagName(WebElement element, String result) {
        if (isSuppressed()) return;
        Reporter.log("Tag name retrieved: " + result, LogLevel.DEBUG);
    }
   @Override
   public void afterGetAttribute(WebElement element, String name, String result) {
       if (isSuppressed()) return;
       Reporter.log("Attribute \"" + name + "\" retrieved with value: " + result, LogLevel.DEBUG);
   }
   @Override
   public void afterIsSelected(WebElement element, boolean result) {
       Reporter.log("Element selected: " + result, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterIsEnabled(WebElement element, boolean result) {
       Reporter.log("Element enabled: " + result, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterGetLocation(WebElement element, Point result) {
       Reporter.log("Location retrieved: " + result.toString(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterGetSize(WebElement element, Dimension result) {
       Reporter.log("Size retrieved: " + result.toString(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterGetCssValue(WebElement element, String propertyName, String result) {
       Reporter.log("CSS value for \"" + propertyName + "\" retrieved: " + result, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterTo(WebDriver.Navigation navigation, String url) {
       Reporter.log("Navigated to URL: " + url, LogLevel.INFO_BLUE);
       if (RECORDING) {
           RECORDED.add(new RecordedInteraction("navigate", null, url, null, null));
           updateRecordingToolbar();
       }
   }

   @Override
   public void afterBack(WebDriver.Navigation navigation) {
       Reporter.log("Navigated back", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterForward(WebDriver.Navigation navigation) {
       Reporter.log("Navigated forward", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterRefresh(WebDriver.Navigation navigation) {
       Reporter.log("Page refreshed", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterAccept(Alert alert) {
       Reporter.log("Accepted alert", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterDismiss(Alert alert) {
       Reporter.log("Dismissed alert", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterGetText(Alert alert, String result) {
       Reporter.log("Alert text retrieved: " + result, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterSendKeys(Alert alert, String text) {
       Reporter.log("Sent keys to alert: " + text, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterAddCookie(WebDriver.Options options, Cookie cookie) {
       Reporter.log("Added cookie: " + cookie.getName(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterDeleteCookieNamed(WebDriver.Options options, String name) {
       Reporter.log("Deleted cookie by name: " + name, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterDeleteCookie(WebDriver.Options options, Cookie cookie) {
       Reporter.log("Deleted cookie: " + cookie.getName(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterDeleteAllCookies(WebDriver.Options options) {
       Reporter.log("Deleted all cookies", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterGetCookies(WebDriver.Options options, Set<Cookie> result) {
       Reporter.log("Retrieved cookies: " + result.toString(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterGetCookieNamed(WebDriver.Options options, String name, Cookie result) {
       Reporter.log("Retrieved cookie by name: " + name + ", Result: " + result, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterImplicitlyWait(WebDriver.Timeouts timeouts, Duration duration) {
       Reporter.log("Set implicit wait timeout to: " + duration.toMillis(), LogLevel.INFO_BLUE, " mills");
   }

   @Override
   public void afterPageLoadTimeout(WebDriver.Timeouts timeouts, Duration duration) {
       Reporter.log("Set page load timeout to: " + duration.toMillis(), LogLevel.INFO_BLUE, " mills");
   }

   @Override
   public void afterGetSize(WebDriver.Window window, Dimension result) {
       Reporter.log("Window size retrieved: " + result.toString(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterSetSize(WebDriver.Window window, Dimension size) {
       Reporter.log("Window size set to: " + size.toString(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterGetPosition(WebDriver.Window window, Point result) {
       Reporter.log("Window position retrieved: " + result.toString(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterSetPosition(WebDriver.Window window, Point position) {
       Reporter.log("Window position set to: " + position.toString(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterMaximize(WebDriver.Window window) {
       Reporter.log("Window maximized", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterFullscreen(WebDriver.Window window) {
       Reporter.log("Window set to fullscreen", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterFrame(WebDriver.TargetLocator targetLocator, int index, WebDriver driver) {
       Reporter.log("Switched to frame by index: " + index, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterFrame(WebDriver.TargetLocator targetLocator, String nameOrId, WebDriver driver) {
       Reporter.log("Switched to frame by name or ID: " + nameOrId, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterFrame(WebDriver.TargetLocator targetLocator, WebElement frameElement, WebDriver driver) {
       Reporter.log("Switched to frame by element: " + nameOf(frameElement), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterParentFrame(WebDriver.TargetLocator targetLocator, WebDriver driver) {
       Reporter.log("Switched to parent frame", LogLevel.INFO_BLUE);
   }

   @Override
   public void afterWindow(WebDriver.TargetLocator targetLocator, String nameOrHandle, WebDriver driver) {
       Reporter.log("Switched to window: " + nameOrHandle, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterNewWindow(WebDriver.TargetLocator targetLocator, WindowType typeHint, WebDriver driver) {
       Reporter.log("New window opened with type: " + typeHint, LogLevel.INFO_BLUE);
   }

   @Override
   public void afterActiveElement(WebDriver.TargetLocator targetLocator, WebDriver driver) {
       Reporter.log("Switched to active element", LogLevel.INFO_BLUE);
   }

   @Override
   public void beforeAlert(WebDriver.TargetLocator targetLocator) {
       Reporter.log("Handling alert", LogLevel.INFO_BLUE);
   }
    @Override
    public void afterClear(WebElement element) {
        Reporter.log("Cleared element: " + nameOf(element), LogLevel.INFO_BLUE);
    }

    @Override
    public void afterGetText(WebElement element, String result) {
        if (isSuppressed()) return;
        Reporter.log("Text retrieved: \"" + result + "\" from " + nameOf(element), LogLevel.DEBUG);
    }

//    @Override
//    public void afterIsDisplayed(WebElement element, boolean result) {
//        Reporter.log("Element displayed: " + result +" "+ nameOf(element), LogLevel.INFO_BLUE);
//    }

    @Override
    public  <X> void afterGetScreenshotAs(WebElement element, OutputType<X> target, X result) {
        Reporter.log("Screenshot taken of element: " + nameOf(element), LogLevel.INFO_BLUE);
    }

    public  <X> void afterGetScreenshotAs(WebDriver
                                                  driver, OutputType<X> target, X result) {
        Reporter.log("Full page screenshot taken", LogLevel.INFO_BLUE);
    }

    @Override
    public void afterPerform(WebDriver driver, Collection<Sequence> actions) {
        Reporter.log("Actions performed (e.g., drag-drop, hover, etc.)", LogLevel.INFO_BLUE);
    }

    @Override
    public void afterResetInputState(WebDriver driver) {
        Reporter.log("Input state reset", LogLevel.INFO_BLUE);
    }

    @Override
    public void afterScriptTimeout(WebDriver.Timeouts timeouts, Duration duration) {
        Reporter.log("Script timeout set to: " + duration.toMillis() + " ms", LogLevel.INFO_BLUE);
    }

    @Override
    public void afterAlert(WebDriver.TargetLocator targetLocator, Alert alert) {
        Reporter.log("Switched to alert", LogLevel.INFO_BLUE);
    }

    @Override
    public void afterTo(WebDriver.Navigation navigation, URL url) {
        Reporter.log("Navigated to URL: " + url, LogLevel.INFO_BLUE);
    }
    private String nameOf(WebElement element) {
        if (element == null) return "";
        try {
            String name = element.getAccessibleName();
            if (name == null || name.isBlank()) {
                name = element.getText();
            }
            if (name == null || name.isBlank()) {
                name = element.getAttribute("placeholder");
            }
            if (name == null || name.isBlank()) {
                name = element.getAttribute("value");
            }
            if (name == null || name.isBlank()) {
                name = element.getAttribute("id");
            }
            return (name != null) ? name.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
    private static final Map<Keys, String> keyMap;
    static {
        keyMap = Map.ofEntries(
                Map.entry(Keys.ENTER, "ENTER"),
                Map.entry(Keys.TAB, "TAB"),
                Map.entry(Keys.ESCAPE, "ESCAPE"),
                Map.entry(Keys.BACK_SPACE, "BACKSPACE"),
                Map.entry(Keys.SPACE, "SPACE"),
                Map.entry(Keys.ARROW_UP, "UP ARROW"),
                Map.entry(Keys.ARROW_DOWN, "DOWN ARROW"),
                Map.entry(Keys.ARROW_LEFT, "LEFT ARROW"),
                Map.entry(Keys.ARROW_RIGHT, "RIGHT ARROW"),
                Map.entry(Keys.DELETE, "DELETE"),
                Map.entry(Keys.HOME, "HOME"),
                Map.entry(Keys.END, "END"),
                Map.entry(Keys.PAGE_UP, "PAGE UP"),
                Map.entry(Keys.PAGE_DOWN, "PAGE DOWN"),
                Map.entry(Keys.SHIFT, "SHIFT"),
                Map.entry(Keys.CONTROL, "CONTROL"),
                Map.entry(Keys.ALT, "ALT"));
    }
    private static String getKeyName(Keys key) {
        return keyMap.getOrDefault(key, key.toString()); // Efficient lookup
    }
}
