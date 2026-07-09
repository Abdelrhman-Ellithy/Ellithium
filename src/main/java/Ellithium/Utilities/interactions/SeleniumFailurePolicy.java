package Ellithium.Utilities.interactions;

import org.openqa.selenium.DetachedShadowRootException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InsecureCertificateException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.remote.NoSuchDriverException;
import org.openqa.selenium.remote.UnreachableBrowserException;

/**
 * Single source of truth that maps a Selenium {@link Throwable} to the strategy that should
 * handle it, per the documented semantics of each exception. Stateless and pure.
 *
 * <p>Class names and hierarchy are the Java Selenium 4.45 set (not the Python API): e.g.
 * {@link ElementNotInteractableException} extends {@link InvalidElementStateException}, so the
 * interaction classes are matched before the generic invalid-state class.
 */
final class SeleniumFailurePolicy {

    private SeleniumFailurePolicy() {
    }

    /**
     * Disposition for a failed WebDriver interaction.
     * <ul>
     *   <li>{@code RELOCATE} — element reference is stale; re-find the same locator and retry.</li>
     *   <li>{@code RECOVER_INTERACTION} — element found but the interaction was blocked
     *       (paint order, overlay, off-screen, wrong match); recover, do not heal the locator.</li>
     *   <li>{@code INVALID_STATE} — element in an invalid state for the command; real test error.</li>
     *   <li>{@code HEAL} — locator could not resolve an element; attempt locator healing.</li>
     *   <li>{@code CONTEXT} — frame/window target missing; not healable in the current context.</li>
     *   <li>{@code ALERT} — a modal alert is blocking commands.</li>
     *   <li>{@code FATAL} — session/driver/certificate failure; never retry or heal.</li>
     *   <li>{@code UNKNOWN} — any other WebDriver failure.</li>
     * </ul>
     */
    enum FailureClass {
        RELOCATE, RECOVER_INTERACTION, INVALID_STATE, HEAL, CONTEXT, ALERT, FATAL, UNKNOWN
    }

    static FailureClass classify(Throwable e) {
        if (e instanceof StaleElementReferenceException || e instanceof DetachedShadowRootException) {
            return FailureClass.RELOCATE;
        }
        if (e instanceof ElementNotInteractableException || e instanceof MoveTargetOutOfBoundsException) {
            return FailureClass.RECOVER_INTERACTION;
        }
        if (e instanceof InvalidElementStateException) {
            return FailureClass.INVALID_STATE;
        }
        if (e instanceof UnhandledAlertException) {
            return FailureClass.ALERT;
        }
        if (e instanceof NoSuchFrameException || e instanceof NoSuchWindowException) {
            return FailureClass.CONTEXT;
        }
        if (e instanceof NoSuchSessionException || e instanceof SessionNotCreatedException
                || e instanceof NoSuchDriverException || e instanceof UnreachableBrowserException
                || e instanceof InsecureCertificateException) {
            return FailureClass.FATAL;
        }
        if (e instanceof NoSuchElementException || e instanceof InvalidSelectorException
                || e instanceof TimeoutException) {
            return FailureClass.HEAL;
        }
        return FailureClass.UNKNOWN;
    }

    /**
     * Failures where the element was found but the interaction itself was blocked — recover the
     * interaction, never heal the locator.
     */
    static boolean isRecoverableInteraction(Throwable e) {
        return classify(e) == FailureClass.RECOVER_INTERACTION;
    }

    /**
     * Failures that must short-circuit immediately: a heal/retry cannot help and would only waste
     * the timeout budget (dead session, missing driver, blocking alert, wrong frame/window context).
     */
    static boolean isTerminal(Throwable e) {
        FailureClass c = classify(e);
        return c == FailureClass.FATAL || c == FailureClass.ALERT || c == FailureClass.CONTEXT;
    }

    /**
     * True when a failure at the find/resolution step warrants locator healing.
     */
    static boolean shouldHeal(WebDriverException e) {
        return classify(e) == FailureClass.HEAL || classify(e) == FailureClass.UNKNOWN;
    }
}
