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
                    Reporter.logSubStep("Finding element using: " + formatLocator(args[0]), LogLevel.INFO_BLUE);
            }
            case "findElements" -> {
                if (args != null && args.length > 0)
                    Reporter.logSubStep("Finding elements using: " + formatLocator(args[0]), LogLevel.INFO_BLUE);
            }

            // ── Mobile script execution ───────────────────────────────────────
            // args[0] = "mobile: <action>" — log only the action name
            case "executeScript" -> {
                if (args != null && args.length > 0) {
                    String script = String.valueOf(args[0]);
                    String action = script.startsWith("mobile: ")
                            ? script.substring("mobile: ".length())
                            : script;
                    Reporter.logSubStep(action, LogLevel.INFO_BLUE);
                }
            }

            // ── Key press (AndroidDriver.pressKey / longPressKey) ────────────
            // args[0] = KeyEvent; decode via AndroidKey enum lookup
            case "pressKey" -> {
                String key = (args != null && args.length > 0) ? decodeKeyEvent(args[0]) : "UNKNOWN";
                Reporter.logSubStep("Pressing key: " + key, LogLevel.INFO_BLUE);
            }
            case "longPressKey" -> {
                String key = (args != null && args.length > 0) ? decodeKeyEvent(args[0]) : "UNKNOWN";
                Reporter.logSubStep("Long pressing key: " + key, LogLevel.INFO_BLUE);
            }

            // ── W3C pointer/touch actions (PointerInput swipe/scroll/drag) ───
            // args[0] = Collection<Sequence>
            case "perform" -> {
                int count = (args != null && args.length > 0 && args[0] instanceof Collection<?> c)
                        ? c.size() : 0;
                Reporter.logSubStep("Performing W3C action sequence (" + count + " pointer" + (count != 1 ? "s" : "") + ")", LogLevel.INFO_BLUE);
            }

            // ── Navigation ────────────────────────────────────────────────────
            case "get", "to" -> {
                if (args != null && args.length > 0)
                    Reporter.logSubStep("Navigating to: " + args[0], LogLevel.INFO_BLUE);
            }
            case "back"    -> Reporter.logSubStep("Navigating back", LogLevel.INFO_BLUE);
            case "forward" -> Reporter.logSubStep("Navigating forward", LogLevel.INFO_BLUE);
            case "refresh" -> Reporter.logSubStep("Refreshing page", LogLevel.INFO_BLUE);

            // ── App management ────────────────────────────────────────────────
            case "activateApp" -> {
                if (args != null && args.length > 0)
                    Reporter.logSubStep("Activating app: " + args[0], LogLevel.INFO_BLUE);
            }
            case "terminateApp" -> {
                if (args != null && args.length > 0)
                    Reporter.logSubStep("Terminating app: " + args[0], LogLevel.INFO_BLUE);
            }
            case "installApp" -> {
                if (args != null && args.length > 0)
                    Reporter.logSubStep("Installing app: " + args[0], LogLevel.INFO_BLUE);
            }
            case "removeApp" -> {
                if (args != null && args.length > 0)
                    Reporter.logSubStep("Removing app: " + args[0], LogLevel.INFO_BLUE);
            }

            // ── Device control ────────────────────────────────────────────────
            case "lock", "lockDevice"     -> Reporter.logSubStep("Locking device", LogLevel.INFO_BLUE);
            case "unlock", "unlockDevice" -> Reporter.logSubStep("Unlocking device", LogLevel.INFO_BLUE);
            case "hideKeyboard"           -> Reporter.logSubStep("Hiding keyboard", LogLevel.INFO_BLUE);
            case "rotate" -> {
                if (args != null && args.length > 0)
                    Reporter.logSubStep("Rotating device: " + args[0], LogLevel.INFO_BLUE);
            }
            case "setContext" -> {
                if (args != null && args.length > 0)
                    Reporter.logSubStep("Switching context to: " + args[0], LogLevel.INFO_BLUE);
            }

            // ── Driver lifecycle ──────────────────────────────────────────────
            case "quit"            -> Reporter.logSubStep("Driver quit", LogLevel.INFO_BLUE);
            case "close"           -> Reporter.logSubStep("Window closed", LogLevel.INFO_BLUE);
            case "getScreenshotAs" -> Ellithium.core.logging.Logger.info("Taking screenshot");

            // ── Default: log method name; suppress infrastructure noise ──────────
            default -> {
                if (!isUtilityMethod(methodName))
                    Reporter.logSubStep(toReadable(methodName), LogLevel.INFO_BLUE);
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
