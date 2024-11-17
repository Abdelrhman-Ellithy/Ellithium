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
            case "findElement":
                Reporter.log("Element found using locator: " + getLocator(args), LogLevel.INFO_BLUE);
                break;

            case "findElements":
                Reporter.log("Elements found using locator: " + getLocator(args), LogLevel.INFO_BLUE);
                break;

            case "execute":
                Reporter.log("Clicked on element: " + getElementDescription(args[0]), LogLevel.INFO_BLUE);
                break;

            case "sendKeys":
                String textSent = getTextFromArgs(args);
                Reporter.log("Sent keys: \"" + textSent + "\" to element: " + getElementDescription(args[0]), LogLevel.INFO_BLUE);
                break;

            case "getPageSource":
                Reporter.log("Page source retrieved", LogLevel.INFO_BLUE);
                break;

            case "quit":
                Reporter.log("Driver quit", LogLevel.INFO_BLUE);
                break;

            case "close":
                Reporter.log("Driver closed", LogLevel.INFO_BLUE);
                break;

            case "tap":
                Reporter.log("Performed tap gesture at coordinates: " + Arrays.toString(args), LogLevel.INFO_BLUE);
                break;

            case "swipe":
                Reporter.log("Swiped from " + args[0] + " to " + args[1], LogLevel.INFO_BLUE);
                break;

            case "longPress":
                Reporter.log("Performed long press on coordinates or element: " + Arrays.toString(args), LogLevel.INFO_BLUE);
                break;

            case "pressKeyCode":
                Reporter.log("Pressed key code: " + args[0], LogLevel.INFO_BLUE);
                break;

            case "releaseKeyCode":
                Reporter.log("Released key code: " + args[0], LogLevel.INFO_BLUE);
                break;

            case "clear":
                Reporter.log("Cleared text in element: " + getElementDescription(args[0]), LogLevel.INFO_BLUE);
                break;

            default:
                Reporter.log("Method: " + methodName + " executed", LogLevel.INFO_BLUE);
                break;
        }
    }

    private String getLocator(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof By) {
            return args[0].toString();
        }
        return "Unknown locator";
    }
    private String getElementDescription(Object element) {
        if (element instanceof WebElement) {
            return element.toString(); // Replace with a custom formatter if needed
        }
        else if (element instanceof By){
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