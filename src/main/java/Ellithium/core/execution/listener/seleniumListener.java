package Ellithium.core.execution.listener;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.*;
import java.time.Duration;
import java.util.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.WebDriverListener;
public class seleniumListener implements WebDriverListener {
    @Override
    public void afterSendKeys(WebElement element, CharSequence... keysToSend) {
        StringBuilder stringBuilder = new StringBuilder();
        for (CharSequence charSequence : keysToSend) {
            if (charSequence instanceof Keys) {
                stringBuilder.append(getKeyName((Keys) charSequence));
            } else {
                stringBuilder.append(charSequence);
            }
        }
        Reporter.log("Sent Data: \"" + stringBuilder + "\" into " + nameOf(element) + ".", LogLevel.INFO_BLUE);
    }
   @Override
   public void afterGet(WebDriver driver, String url) {
       Reporter.log("Navigating to URL: ", LogLevel.INFO_BLUE, url);
   }
   @Override
   public void afterGetCurrentUrl(WebDriver driver, String url) {
       Reporter.log("Current URL retrieved: " + url, LogLevel.INFO_BLUE);
   }
    @Override
    public void afterDefaultContent(WebDriver.TargetLocator targetLocator, WebDriver driver) {
        Reporter.log("Switched Back To Default Content From Frame" , LogLevel.INFO_BLUE);
    }
   @Override
   public void afterGetTitle(WebDriver driver, String title) {
       Reporter.log("Page title retrieved: " + title, LogLevel.INFO_BLUE);
   }
   @Override
   public void afterGetPageSource(WebDriver driver, String source) {
       Reporter.log("Page source retrieved", LogLevel.INFO_BLUE);
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
       Reporter.log("Window handles retrieved: " + result, LogLevel.INFO_BLUE);
   }
   @Override
   public void afterGetWindowHandle(WebDriver driver, String result) {
       Reporter.log("Window handle retrieved: " + result, LogLevel.INFO_BLUE);
   }
   @Override
   public void afterExecuteScript(WebDriver driver, String script, Object[] args, Object result) {
       Reporter.log("Executed script: " + script + ", Result: " + result, LogLevel.INFO_BLUE);
   }
   @Override
   public void afterExecuteAsyncScript(WebDriver driver, String script, Object[] args, Object result) {
       Reporter.log("Executed async script: " + script + ", Result: " + result, LogLevel.INFO_BLUE);
   }
   @Override
   public void afterClick(WebElement element) {
       Reporter.log("Clicked on element: " + nameOf(element), LogLevel.INFO_BLUE);
   }
   @Override
   public void afterSubmit(WebElement element) {
       Reporter.log("Submitted element: " + nameOf(element), LogLevel.INFO_BLUE);
   }
    @Override
   public void afterGetTagName(WebElement element, String result) {
       Reporter.log("Tag name retrieved: " + result, LogLevel.INFO_BLUE);
   }
   @Override
   public void afterGetAttribute(WebElement element, String name, String result) {
       Reporter.log("Attribute \"" + name + "\" retrieved with value: " + result, LogLevel.INFO_BLUE);
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
   private String nameOf(WebElement element){
       try {
           return element.getAccessibleName();
       }catch (Exception e){
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
