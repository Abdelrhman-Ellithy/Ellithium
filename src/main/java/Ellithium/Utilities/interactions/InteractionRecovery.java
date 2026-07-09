package Ellithium.Utilities.interactions;

import Ellithium.core.ai.dom.InteractiveElements;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Recovery primitives for interaction failures where the element was found but the action was
 * blocked (wrong match among several, sticky-header/overlay obscuring, off-screen position).
 *
 * <p>Reused by both the single-element retry helpers and the multi-element loops so the recovery
 * ladder — re-select the interactable match, drill to the correct relative, scroll to centre,
 * await clickable, JS-click — is defined once. All methods are best-effort and never heal a locator.
 */
final class InteractionRecovery {

    private final WebDriver driver;

    InteractionRecovery(WebDriver driver) {
        this.driver = driver;
    }

    static boolean isNativeMobileContext(WebDriver driver) {
        if (!(driver instanceof io.appium.java_client.remote.SupportsContextSwitching ctxAware)) return false;
        try {
            String ctx = ctxAware.getContext();
            return ctx == null || ctx.toUpperCase(Locale.ROOT).contains("NATIVE");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInteractable(WebElement el) {
        try {
            return el.isDisplayed() && el.isEnabled();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Returns the best interactable element for {@code locator} — the first match that is displayed
     * and enabled, otherwise an interactable descendant/ancestor of the first match — scrolled to the
     * viewport centre and awaited clickable. Returns {@code null} when no correct interactable target
     * exists (the located element is wrong), signalling the caller to escalate to healing.
     */
    WebElement resolveInteractable(By locator, long timeoutMillis, int polling) {
        try {
            List<WebElement> matches = driver.findElements(locator);
            WebElement chosen = null;
            for (WebElement m : matches) {
                if (isInteractable(m)) {
                    chosen = m;
                    break;
                }
            }
            if (chosen == null && !matches.isEmpty()) {
                chosen = drillToInteractive(matches.get(0));
            }
            if (chosen == null) return null;
            scrollToCenter(chosen);
            awaitClickable(chosen, timeoutMillis, polling);
            return chosen;
        } catch (WebDriverException e) {
            return null;
        }
    }

    /**
     * From a non-interactable matched element, finds the genuinely interactive target: an
     * interactive descendant first (e.g. the {@code <input>} inside a matched {@code <td>}), then an
     * interactive ancestor (e.g. the {@code <a>} wrapping a matched {@code <span>}). Null if neither.
     */
    WebElement drillToInteractive(WebElement el) {
        try {
            for (WebElement kid : el.findElements(By.cssSelector(InteractiveElements.DESCENDANT_CSS))) {
                if (isInteractable(kid)) return kid;
            }
        } catch (WebDriverException ignored) {
        }
        try {
            WebElement ancestor = el.findElement(By.xpath(InteractiveElements.ANCESTOR_XPATH));
            if (isInteractable(ancestor)) return ancestor;
        } catch (WebDriverException ignored) {
        }
        return isInteractable(el) ? el : null;
    }

    void scrollToCenter(WebElement el) {
        if (el == null || isNativeMobileContext(driver) || !(driver instanceof JavascriptExecutor js)) return;
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'center'});", el);
        } catch (WebDriverException ignored) {
        }
    }

    void awaitClickable(WebElement el, long timeoutMillis, int polling) {
        if (el == null) return;
        try {
            WaitManager.getFluentWaitMillis(driver, timeoutMillis, polling)
                    .until(ExpectedConditions.elementToBeClickable(el));
        } catch (WebDriverException ignored) {
        }
    }

    boolean jsClick(WebElement el) {
        if (el == null || isNativeMobileContext(driver) || !(driver instanceof JavascriptExecutor js)) return false;
        try {
            js.executeScript("arguments[0].click();", el);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Handles an unexpected modal blocking commands ({@code UnhandledAlertException}): accepts it
     * (dismiss as fallback) so the original action can be retried. Returns whether a blocking alert
     * was found and cleared.
     */
    boolean handleUnexpectedAlert() {
        try {
            Alert alert = driver.switchTo().alert();
            String text;
            try { text = alert.getText(); } catch (WebDriverException ignored) { text = ""; }
            try {
                alert.accept();
            } catch (WebDriverException acceptFailed) {
                driver.switchTo().alert().dismiss();
            }
            Reporter.log("[RECOVER] Unexpected alert handled: " + text, LogLevel.INFO_YELLOW);
            return true;
        } catch (NoAlertPresentException none) {
            return false;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Restores a lost frame/window context: a {@link NoSuchWindowException} switches to an open
     * window; a {@link NoSuchFrameException} searches the frame tree for {@code locator} and switches
     * into the frame that contains it. Returns whether the context was restored.
     */
    boolean recoverContext(By locator, WebDriverException e) {
        if (e instanceof NoSuchWindowException) return switchToAvailableWindow();
        if (e instanceof NoSuchFrameException) return switchToFrameContaining(locator);
        return false;
    }

    /** Switches to an open window when the current one is gone; true if a usable window is active. */
    boolean switchToAvailableWindow() {
        try {
            Set<String> handles = driver.getWindowHandles();
            if (handles.isEmpty()) return false;
            try {
                if (handles.contains(driver.getWindowHandle())) return true;
            } catch (WebDriverException currentGone) {
            }
            driver.switchTo().window(handles.iterator().next());
            Reporter.log("[RECOVER] Current window unavailable — switched to an open window", LogLevel.INFO_YELLOW);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private static final int MAX_FRAME_DEPTH = 3;

    /**
     * Resets to the top document, then searches the frame tree (bounded depth) for the first frame
     * whose context contains {@code locator}, leaving the driver switched into it. Returns to default
     * content and reports false when no frame contains the element.
     */
    boolean switchToFrameContaining(By locator) {
        try { driver.switchTo().defaultContent(); } catch (WebDriverException ignored) {}
        try {
            if (!driver.findElements(locator).isEmpty()) return true;
        } catch (WebDriverException ignored) {}
        if (searchFrames(locator, 0)) {
            Reporter.log("[RECOVER] Located element inside a frame — switched frame context for " + locator,
                    LogLevel.INFO_YELLOW);
            return true;
        }
        try { driver.switchTo().defaultContent(); } catch (WebDriverException ignored) {}
        return false;
    }

    private boolean searchFrames(By locator, int depth) {
        if (depth >= MAX_FRAME_DEPTH) return false;
        int frameCount;
        try {
            frameCount = driver.findElements(By.cssSelector("iframe, frame")).size();
        } catch (WebDriverException e) {
            return false;
        }
        for (int i = 0; i < frameCount; i++) {
            try {
                driver.switchTo().frame(i);
            } catch (WebDriverException cannotEnter) {
                continue;
            }
            try {
                if (!driver.findElements(locator).isEmpty()) return true;
                if (searchFrames(locator, depth + 1)) return true;
            } catch (WebDriverException ignored) {}
            try {
                driver.switchTo().parentFrame();
            } catch (WebDriverException cannotAscend) {
                try { driver.switchTo().defaultContent(); } catch (WebDriverException ignored) {}
                return false;
            }
        }
        return false;
    }
}
