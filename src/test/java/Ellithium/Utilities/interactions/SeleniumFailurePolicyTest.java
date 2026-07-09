package Ellithium.Utilities.interactions;

import Ellithium.Utilities.interactions.SeleniumFailurePolicy.FailureClass;
import org.openqa.selenium.ElementClickInterceptedException;
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
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.remote.NoSuchDriverException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SeleniumFailurePolicyTest {

    @Test
    public void stale_isRelocate() {
        assertEquals(SeleniumFailurePolicy.classify(new StaleElementReferenceException("x")), FailureClass.RELOCATE);
    }

    @Test
    public void notInteractable_isRecoverInteraction() {
        assertEquals(SeleniumFailurePolicy.classify(new ElementNotInteractableException("x")),
                FailureClass.RECOVER_INTERACTION);
    }

    @Test
    public void clickIntercepted_isRecoverInteraction_evenThoughItExtendsInvalidState() {
        assertEquals(SeleniumFailurePolicy.classify(new ElementClickInterceptedException("x")),
                FailureClass.RECOVER_INTERACTION);
    }

    @Test
    public void moveTargetOutOfBounds_isRecoverInteraction() {
        assertEquals(SeleniumFailurePolicy.classify(new MoveTargetOutOfBoundsException("x")),
                FailureClass.RECOVER_INTERACTION);
    }

    @Test
    public void invalidElementState_isInvalidState_notRecover() {
        assertEquals(SeleniumFailurePolicy.classify(new InvalidElementStateException("x")),
                FailureClass.INVALID_STATE);
    }

    @Test
    public void unhandledAlert_isAlert() {
        assertEquals(SeleniumFailurePolicy.classify(new UnhandledAlertException("x")), FailureClass.ALERT);
    }

    @Test
    public void frameAndWindow_areContext() {
        assertEquals(SeleniumFailurePolicy.classify(new NoSuchFrameException("x")), FailureClass.CONTEXT);
        assertEquals(SeleniumFailurePolicy.classify(new NoSuchWindowException("x")), FailureClass.CONTEXT);
    }

    @Test
    public void sessionAndDriverFailures_areFatal() {
        assertEquals(SeleniumFailurePolicy.classify(new NoSuchSessionException("x")), FailureClass.FATAL);
        assertEquals(SeleniumFailurePolicy.classify(new SessionNotCreatedException("x")), FailureClass.FATAL);
        assertEquals(SeleniumFailurePolicy.classify(new NoSuchDriverException("x")), FailureClass.FATAL);
        assertEquals(SeleniumFailurePolicy.classify(new InsecureCertificateException("x")), FailureClass.FATAL);
    }

    @Test
    public void notFoundAndSelector_areHeal() {
        assertEquals(SeleniumFailurePolicy.classify(new NoSuchElementException("x")), FailureClass.HEAL);
        assertEquals(SeleniumFailurePolicy.classify(new InvalidSelectorException("x")), FailureClass.HEAL);
        assertEquals(SeleniumFailurePolicy.classify(new TimeoutException("x")), FailureClass.HEAL);
    }

    @Test
    public void otherWebDriverException_isUnknown() {
        assertEquals(SeleniumFailurePolicy.classify(new JavascriptException("x")), FailureClass.UNKNOWN);
    }

    @Test
    public void isTerminal_trueForFatalAlertContext_falseForRecoverable() {
        assertTrue(SeleniumFailurePolicy.isTerminal(new NoSuchSessionException("x")));
        assertTrue(SeleniumFailurePolicy.isTerminal(new UnhandledAlertException("x")));
        assertTrue(SeleniumFailurePolicy.isTerminal(new NoSuchFrameException("x")));
        assertFalse(SeleniumFailurePolicy.isTerminal(new ElementNotInteractableException("x")));
        assertFalse(SeleniumFailurePolicy.isTerminal(new StaleElementReferenceException("x")));
        assertFalse(SeleniumFailurePolicy.isTerminal(new NoSuchElementException("x")));
    }

    @Test
    public void isRecoverableInteraction_onlyForInteractionClasses() {
        assertTrue(SeleniumFailurePolicy.isRecoverableInteraction(new ElementClickInterceptedException("x")));
        assertTrue(SeleniumFailurePolicy.isRecoverableInteraction(new MoveTargetOutOfBoundsException("x")));
        assertFalse(SeleniumFailurePolicy.isRecoverableInteraction(new InvalidElementStateException("x")));
        assertFalse(SeleniumFailurePolicy.isRecoverableInteraction(new NoSuchElementException("x")));
    }

    @Test
    public void shouldHeal_trueForHealAndUnknown_falseForTerminalAndInteraction() {
        assertTrue(SeleniumFailurePolicy.shouldHeal(new NoSuchElementException("x")));
        assertTrue(SeleniumFailurePolicy.shouldHeal(new JavascriptException("x")));
        assertFalse(SeleniumFailurePolicy.shouldHeal(new NoSuchSessionException("x")));
        assertFalse(SeleniumFailurePolicy.shouldHeal(new ElementNotInteractableException("x")));
    }
}
