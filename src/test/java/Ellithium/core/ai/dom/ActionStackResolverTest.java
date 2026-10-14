package Ellithium.core.ai.dom;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ActionStackResolverTest {

    private static StackTraceElement frame(String cls, String method) {
        return new StackTraceElement(cls, method, cls.substring(cls.lastIndexOf('.') + 1) + ".java", 1);
    }

    @Test
    public void internalHealAndRetryFramesNeverLeakAsAction() {
        StackTraceElement[] stack = {
                frame("java.lang.Thread", "getStackTrace"),
                frame("Ellithium.Utilities.interactions.BaseActions", "healOnce"),
                frame("Ellithium.Utilities.interactions.BaseActions", "performWithStaleRetry"),
                frame("Ellithium.Utilities.interactions.ElementActions", "clickOnElement"),
                frame("Pages.LoginPage", "clickSubmit"),
        };
        assertEquals(ActionStackResolver.extractAction(stack), "clickOnElement");
    }

    @Test
    public void shadowDomResolverFramesNeverLeakAsAction() {
        StackTraceElement[] stack = {
                frame("Ellithium.Utilities.interactions.ShadowDomResolver", "findDeepJsElement"),
                frame("Ellithium.Utilities.interactions.ShadowDomResolver", "findNativeShadowElements"),
                frame("Ellithium.Utilities.interactions.ShadowDomResolver", "findElement"),
                frame("Ellithium.Utilities.interactions.BaseActions", "rawVisibleElement"),
                frame("Ellithium.Utilities.interactions.ElementActions", "sendData"),
                frame("Pages.LoginPage", "enterPassword"),
        };
        assertEquals(ActionStackResolver.extractAction(stack), "sendData");
    }

    @Test
    public void networkActionsFramesNeverLeakAsAction() {
        StackTraceElement[] stack = {
                frame("Ellithium.Utilities.interactions.NetworkActions", "mockResponse"),
                frame("Ellithium.Utilities.interactions.ElementActions", "clickOnElement"),
                frame("Pages.CheckoutPage", "submitOrder"),
        };
        assertEquals(ActionStackResolver.extractAction(stack), "clickOnElement");
    }

    @Test
    public void addedInternalMethodsAreExcludedEvenIfClassMatches() {
        StackTraceElement[] stack = {
                frame("Ellithium.Utilities.interactions.ElementActions", "rawVisibleElement"),
                frame("Ellithium.Utilities.interactions.ElementActions", "waitForVisibilityAndFindElement"),
                frame("Ellithium.Utilities.interactions.ElementActions", "getText"),
                frame("Pages.ProfilePage", "getHeaderTitle"),
        };
        assertEquals(ActionStackResolver.extractAction(stack), "getText");
    }

    @Test
    public void sanitizeStackTrace_stripsAllAddedAndInfraFrames() {
        StackTraceElement[] raw = {
                frame("java.lang.Thread", "getStackTrace"),
                frame("Ellithium.Utilities.interactions.BaseActions", "buildHealingRequest"),
                frame("Ellithium.Utilities.interactions.ShadowDomResolver", "findElement"),
                frame("Ellithium.Utilities.interactions.BaseActions", "rawVisibleElement"),
                frame("Ellithium.Utilities.interactions.ElementActions", "clickOnElement"),
                frame("Pages.LoginPage", "clickSubmit"),
                frame("Tests.LoginTest", "testLogin"),
        };

        StackTraceElement[] cleaned = ActionStackResolver.sanitizeStackTrace(raw);

        assertEquals(cleaned.length, 3);
        assertEquals(cleaned[0].getMethodName(), "clickOnElement");
        assertEquals(cleaned[1].getClassName(), "Pages.LoginPage");
        assertEquals(cleaned[2].getClassName(), "Tests.LoginTest");
    }

    @Test
    public void recoveryAndPolicyAndWaitFactoryFramesAreSkipped() {
        StackTraceElement[] stack = {
                frame("Ellithium.Utilities.interactions.InteractionRecovery", "resolveInteractable"),
                frame("Ellithium.Utilities.interactions.SeleniumFailurePolicy", "classify"),
                frame("Ellithium.Utilities.interactions.WaitManager", "getFluentWaitMillis"),
                frame("Ellithium.Utilities.interactions.BaseActions", "rawVisibleElement"),
                frame("Ellithium.Utilities.interactions.SelectActions", "selectDropdownByText"),
        };
        assertEquals(ActionStackResolver.extractAction(stack), "selectDropdownByText");
    }

    @Test
    public void lambdaAndInnerFramesOfInfraClassesAreSkipped() {
        StackTraceElement[] stack = {
                frame("Ellithium.Utilities.interactions.BaseActions$CachedSource", "lines"),
                frame("Ellithium.Utilities.interactions.BaseActions", "lambda$performWithStaleRetry$0"),
                frame("Ellithium.Utilities.interactions.MouseActions", "doubleClick"),
        };
        assertEquals(ActionStackResolver.extractAction(stack), "doubleClick");
    }

    @Test
    public void waitActionsRemainsReportable() {
        StackTraceElement[] stack = {
                frame("Ellithium.Utilities.interactions.BaseActions", "findWebElement"),
                frame("Ellithium.Utilities.interactions.WaitActions", "waitForElementToBeClickable"),
                frame("Pages.HomePage", "waitForBanner"),
        };
        assertEquals(ActionStackResolver.extractAction(stack), "waitForElementToBeClickable");
    }

    @Test
    public void unknownWhenNoInteractionFrame() {
        StackTraceElement[] stack = {
                frame("Pages.LoginPage", "clickSubmit"),
                frame("org.testng.TestRunner", "run"),
        };
        assertEquals(ActionStackResolver.extractAction(stack), "unknown");
        assertEquals(ActionStackResolver.extractAction(null), "unknown");
    }
}
