package Ellithium.core.execution.listener;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.proxy.MethodCallListener;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public class appiumListener implements MethodCallListener {

    @Override
    public void afterCall(Object obj, Method method, Object[] args, Object result) {
        if (ListenerLogSuppression.isSuppressed()) return;
        String methodName = method.getName();
        switch (methodName) {

            // ── Locator ──────────────────────────────────────────────────────
            case "findElement" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Finding element using: " + formatLocator(args[0]), LogLevel.INFO_BLUE);
            }
            case "findElements" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Finding elements using: " + formatLocator(args[0]), LogLevel.INFO_BLUE);
            }

            // ── Mobile script execution ───────────────────────────────────────
            // args[0] = "mobile: <action>" — log only the action name
            case "executeScript" -> {
                if (args != null && args.length > 0) {
                    String script = String.valueOf(args[0]);
                    String action = script.startsWith("mobile: ")
                            ? script.substring("mobile: ".length())
                            : script;
                    Reporter.log(action, LogLevel.INFO_BLUE);
                }
            }

            // ── Key press (AndroidDriver.pressKey / longPressKey) ────────────
            // args[0] = KeyEvent; decode via AndroidKey enum lookup
            case "pressKey" -> {
                String key = (args != null && args.length > 0) ? decodeKeyEvent(args[0]) : "UNKNOWN";
                Reporter.log("Pressing key: " + key, LogLevel.INFO_BLUE);
            }
            case "longPressKey" -> {
                String key = (args != null && args.length > 0) ? decodeKeyEvent(args[0]) : "UNKNOWN";
                Reporter.log("Long pressing key: " + key, LogLevel.INFO_BLUE);
            }

            // ── W3C pointer/touch actions (PointerInput swipe/scroll/drag) ───
            // args[0] = Collection<Sequence>
            case "perform" -> {
                int count = (args != null && args.length > 0 && args[0] instanceof Collection<?> c)
                        ? c.size() : 0;
                Reporter.log("Performing W3C action sequence (" + count + " pointer" + (count != 1 ? "s" : "") + ")", LogLevel.INFO_BLUE);
            }

            // ── Navigation ────────────────────────────────────────────────────
            case "get", "to" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Navigating to: " + args[0], LogLevel.INFO_BLUE);
            }
            case "back"    -> Reporter.log("Navigating back", LogLevel.INFO_BLUE);
            case "forward" -> Reporter.log("Navigating forward", LogLevel.INFO_BLUE);
            case "refresh" -> Reporter.log("Refreshing page", LogLevel.INFO_BLUE);

            // ── App management ────────────────────────────────────────────────
            case "activateApp" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Activating app: " + args[0], LogLevel.INFO_BLUE);
            }
            case "terminateApp" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Terminating app: " + args[0], LogLevel.INFO_BLUE);
            }
            case "installApp" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Installing app: " + args[0], LogLevel.INFO_BLUE);
            }
            case "removeApp" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Removing app: " + args[0], LogLevel.INFO_BLUE);
            }

            // ── Device control ────────────────────────────────────────────────
            case "lock", "lockDevice"     -> Reporter.log("Locking device", LogLevel.INFO_BLUE);
            case "unlock", "unlockDevice" -> Reporter.log("Unlocking device", LogLevel.INFO_BLUE);
            case "hideKeyboard"           -> Reporter.log("Hiding keyboard", LogLevel.INFO_BLUE);
            case "rotate" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Rotating device: " + args[0], LogLevel.INFO_BLUE);
            }
            case "setContext" -> {
                if (args != null && args.length > 0)
                    Reporter.log("Switching context to: " + args[0], LogLevel.INFO_BLUE);
            }

            // ── Driver lifecycle ──────────────────────────────────────────────
            case "quit"            -> Reporter.log("Driver quit", LogLevel.INFO_BLUE);
            case "close"           -> Reporter.log("Window closed", LogLevel.INFO_BLUE);
            case "getScreenshotAs" -> Reporter.log("Taking screenshot", LogLevel.INFO_BLUE);

            // ── Default: log method name; suppress infrastructure noise ──────────
            default -> {
                if (!isUtilityMethod(methodName))
                    Reporter.log(toReadable(methodName), LogLevel.INFO_BLUE);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatLocator(Object arg) {
        if (arg == null) return "null";
        return arg.toString()
                .replaceAll("By\\.|AppiumBy\\.", "")
                .replaceAll("[{}]", "")
                .replaceAll("using=", "")
                .trim();
    }

    private String toReadable(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c) && !sb.isEmpty()) sb.append(' ');
            sb.append(sb.isEmpty() ? Character.toUpperCase(c) : c);
        }
        return sb.toString();
    }

    private boolean isUtilityMethod(String name) {
        return switch (name) {
            case "getFileDetector", "getCapabilities", "getSessionId",
                 "manage", "assertExtensionExists",
                 "getCommandExecutor", "getRemoteControlServerAddress", "getWrappedDriver",
                 "getPageSource", "getTitle", "getCurrentUrl",
                 "getWindowHandle", "getWindowHandles",
                 "getContextHandles", "getContext",
                 "getAttribute", "getLocation", "getSize", "getRect",
                 "isDisplayed", "isEnabled", "isSelected",
                 "getTagName", "getCssValue",
                 "getAvailableEngines", "getActiveEngine" -> true;
            default -> false;
        };
    }

    private String decodeKeyEvent(Object arg) {
        if (!(arg instanceof KeyEvent keyEvent)) return String.valueOf(arg);
        try {
            Map<String, Object> built = keyEvent.build();
            Object code = built.get("keycode");
            if (code instanceof Number n) {
                int keycode = n.intValue();
                for (AndroidKey k : AndroidKey.values()) {
                    if (k.getCode() == keycode) return k.name();
                }
                return "KEY_" + keycode;
            }
        } catch (Exception ignored) {}
        return String.valueOf(arg);
    }

}
