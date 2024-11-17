package Ellithium.core.execution.listener;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.proxy.MethodCallListener;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import java.lang.reflect.Method;
import java.util.Arrays;

public class appiumListener implements MethodCallListener {
    @Override
    public void afterCall(Object obj, Method method, Object[] args, Object result) {
        String methodName = method.getName();
        switch (methodName) {
            // Element Finding Methods
            case "findElement",
                    "findElementByAccessibilityId",
                    "findElementById",
                    "findElementByClassName",
                    "findElementByXPath",
                    "findElements" ->
                    Reporter.log("Locator used: " + getLocator(args), LogLevel.INFO_BLUE);

            case "click" ->
                    Reporter.log("Clicked on element: " + getElementDescription(args[0]), LogLevel.INFO_BLUE);

            case "sendKeys" -> {
                String textSent = getTextFromArgs(args);
                Reporter.log("Sent keys: \"" + textSent + "\" to element: " + getElementDescription(args[0]), LogLevel.INFO_BLUE);
            }
            case "clear" ->
                    Reporter.log("Cleared text in element: " + getElementDescription(args[0]), LogLevel.INFO_BLUE);

            case "navigateTo" ->
                    Reporter.log("Navigated to URL: " + args[0], LogLevel.INFO_BLUE);

            case "back" ->
                    Reporter.log("Navigated back", LogLevel.INFO_BLUE);

            case "forward" ->
                    Reporter.log("Navigated forward", LogLevel.INFO_BLUE);

            case "refresh" ->
                    Reporter.log("Page refreshed", LogLevel.INFO_BLUE);

            case "tap" ->
                    Reporter.log("Performed tap at: " + Arrays.toString(args), LogLevel.INFO_BLUE);

            case "doubleTap" ->
                    Reporter.log("Performed double-tap gesture", LogLevel.INFO_BLUE);

            case "longPress" ->
                    Reporter.log("Performed long press at: " + Arrays.toString(args), LogLevel.INFO_BLUE);

            case "swipe" ->
                    Reporter.log("Swiped from: " + args[0] + " to " + args[1], LogLevel.INFO_BLUE);

            case "scroll" ->
                    Reporter.log("Scrolled from: " + args[0] + " to " + args[1], LogLevel.INFO_BLUE);

            case "pinch" ->
                    Reporter.log("Performed pinch gesture on element: " + getElementDescription(args[0]), LogLevel.INFO_BLUE);

            case "zoom" ->
                    Reporter.log("Performed zoom gesture on element: " + getElementDescription(args[0]), LogLevel.INFO_BLUE);

            case "pressKeyCode" ->
                    Reporter.log("Pressed key code: " + args[0], LogLevel.INFO_BLUE);

            case "releaseKeyCode" ->
                    Reporter.log("Released key code: " + args[0], LogLevel.INFO_BLUE);

            case "getPageSource" ->
                    Reporter.log("Page source retrieved", LogLevel.INFO_BLUE);

            case "quit" ->
                    Reporter.log("Driver quit", LogLevel.INFO_BLUE);

            case "close" ->
                    Reporter.log("Driver closed", LogLevel.INFO_BLUE);

            default ->
                    Reporter.log("Method executed: " + methodName + ", Arguments: " + Arrays.toString(args), LogLevel.INFO_BLUE);
        }
    }

    // Helper to extract locator details from arguments
    private String getLocator(Object[] args) {
        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof By) {
                return arg.toString();
            }
        }
        return "Unknown locator";
    }

    // Helper to describe elements for logging
    private String getElementDescription(Object element) {
        if (element instanceof WebElement) {
            WebElement webElement = (WebElement) element;
            return "TagName: " + webElement.getTagName() + ", Text: " + webElement.getText();
        }
        if (element instanceof By) {
            return element.toString();
        }
        return "Unknown element";
    }

    private String getTextFromArgs(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof CharSequence[]) {
            StringBuilder stringBuilder = new StringBuilder();
            for (CharSequence charSequence : (CharSequence[]) args[0]) {
                stringBuilder.append(charSequence);
            }
            return stringBuilder.toString();
        }
        return "Unknown text";
    }
}