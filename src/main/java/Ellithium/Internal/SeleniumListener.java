package Ellithium.Internal;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Sequence;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.WebDriverListener;

public class SeleniumListener implements WebDriverListener {

   @Override
   public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
       Reporter.log("Error in call: " + method.getName(), LogLevel.ERROR, formatArgs(args));
       Reporter.log("Exception: " + e.getCause().getMessage(), LogLevel.ERROR, e.toString());
   }
   @Override
   public void beforeGet(WebDriver driver, String url) {
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
   public void afterFindElement(WebDriver driver, By locator, WebElement element) {
       Reporter.log("Element found by: "+ locator.toString(), LogLevel.INFO_BLUE);
   }

   @Override
   public void afterFindElements(WebDriver driver, By locator, List<WebElement> elements) {
       Reporter.log("Elements found by: "+ locator.toString() + ", Count: " + elements.size(), LogLevel.INFO_BLUE);
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
       Reporter.log("Clicked on element: " + element.getAccessibleName(), LogLevel.INFO_BLUE);
   }
   @Override
   public void afterSubmit(WebElement element) {
       Reporter.log("Submitted element: " + element.getAccessibleName(), LogLevel.INFO_BLUE);
   }
    @Override
    public void afterSendKeys(WebElement element, CharSequence... keysToSend) {
        var stringBuilder = new StringBuilder();
        Arrays.stream(keysToSend).toList().forEach(stringBuilder::append);
        Reporter.log("Sent Data: \"" + stringBuilder + "\" into " + element.getAccessibleName() + ".", LogLevel.INFO_BLUE);
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
   public void afterSetScriptTimeout(WebDriver.Timeouts timeouts, Duration duration) {
       Reporter.log("Set script timeout to: " + duration.toMillis(), LogLevel.INFO_BLUE, " mills");
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
       Reporter.log("Switched to frame by element: " + frameElement.getAccessibleName(), LogLevel.INFO_BLUE);
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
   // Helper method to format the arguments for logging
   private String formatArgs(Object[] args) {
       if (args == null || args.length == 0) {
           return "No arguments";
       }
       StringBuilder formattedArgs = new StringBuilder();
       for (Object arg : args) {
           formattedArgs.append(arg).append(", ");
       }
       return formattedArgs.toString().replaceAll(", $", "");  // Remove the last comma and space
   }
}
