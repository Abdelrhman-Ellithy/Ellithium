package Ellithium.core.execution.listener;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.proxy.MethodCallListener;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class appiumListener implements MethodCallListener {
    @Override
    public void afterCall(Object obj, Method method, Object[] args, Object result) {
        String methodName = method.getName();
        switch (methodName) {
            case "findElement", "findElements" -> {
                if (args != null && args.length > 0) {
                    Reporter.log("Finding element using: " + cleanUpLocator(getLocator(args)), LogLevel.INFO_BLUE);
                }
            }
            case "quit" ->
                    Reporter.log("Driver quit", LogLevel.INFO_BLUE);
            case "close" ->
                    Reporter.log("Driver closed", LogLevel.INFO_BLUE);
            case "getScreenshotAs" ->
                    Reporter.log("Getting Screen Shoot", LogLevel.INFO_BLUE);

            case "execute", "executeScript" -> {
                if (args != null && args.length > 0) {
                    String scriptName = extractScriptName(args);
                    String scriptDetails = extractScriptDetails(args);
                    Reporter.log("Executing " + scriptName + " with details: " + scriptDetails, LogLevel.INFO_BLUE);
                }
            }
            default -> {
                if (!isUtilityMethod(methodName)) {
                    Reporter.log(methodName + " with Arguments: " + Arrays.toString(args), LogLevel.INFO_BLUE);
                }
            }
        }
    }

    private String cleanUpLocator(String locator) {
        return locator.replaceAll("By\\.|AppiumBy\\.", "")
                     .replaceAll("\\{|\\}", "")
                     .replaceAll("using=", "");
    }

    private String extractScriptName(Object[] args) {
        if (args.length >= 2 && args[1] instanceof Map) {
            Map<?, ?> scriptArgs = (Map<?, ?>) args[1];
            if (scriptArgs.containsKey("script")) {
                return scriptArgs.get("script").toString();
            }
        }
        return args[0].toString();
    }

    private boolean isUtilityMethod(String methodName) {
        return methodName.equals("getFileDetector") ||
               methodName.equals("getCapabilities") ||
               methodName.equals("getSessionId") ||
               methodName.equals("manage") ||
               methodName.equals("assertExtensionExists");
    }

    private String getLocator(Object[] args) {
        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof By) {
                String locator = arg.toString();
                return locator.replaceAll("By\\.", "").replaceAll("AppiumBy\\.", "");
            }
        }
        return "Unknown locator";
    }

    private String extractScriptDetails(Object[] args) {
        if (args.length >= 2 && args[1] instanceof Map) {
            Map<?, ?> scriptArgs = (Map<?, ?>) args[1];
            // Handle mobile: pressKey script
            if (scriptArgs.containsKey("script") && "mobile: pressKey".equals(scriptArgs.get("script"))) {
                if (scriptArgs.containsKey("args")) {
                    Object scriptParams = scriptArgs.get("args");
                    if (scriptParams instanceof List && !((List<?>) scriptParams).isEmpty()) {
                        Object param = ((List<?>) scriptParams).get(0);
                        if (param instanceof Map) {
                            Map<?, ?> keyMap = (Map<?, ?>) param;
                            if (keyMap.containsKey("keycode")) {
                                int keyCode = ((Number) keyMap.get("keycode")).intValue();
                                return KEYCODE_MAP.getOrDefault(keyCode, "KEY_" + keyCode);
                            }
                        }
                    }
                }
            }
            return scriptArgs.toString();
        }
        return Arrays.toString(args);
    }
    private static final Map<Integer, String> KEYCODE_MAP;
    static {
        KEYCODE_MAP = Map.ofEntries(
                Map.entry(66, "ENTER"),
                Map.entry(67, "DELETE"),
                Map.entry(61, "TAB"),
                Map.entry(62, "SPACE"),
                Map.entry(19, "DPAD UP"),
                Map.entry(20, "DPAD DOWN"),
                Map.entry(21, "DPAD LEFT"),
                Map.entry(22, "DPAD RIGHT"),
                Map.entry(92, "PAGE UP"),
                Map.entry(93, "PAGE DOWN"),
                Map.entry(111, "ESCAPE"),
                Map.entry(4, "BACK"),
                Map.entry(82, "MENU"),
                Map.entry(84, "SEARCH"),
                Map.entry(3, "HOME"),
                Map.entry(7, "0"),
                Map.entry(8, "1"),
                Map.entry(9, "2"),
                Map.entry(10, "3"),
                Map.entry(11, "4"),
                Map.entry(12, "5"),
                Map.entry(13, "6"),
                Map.entry(14, "7"),
                Map.entry(15, "8"),
                Map.entry(16, "9")
        );
    }

}