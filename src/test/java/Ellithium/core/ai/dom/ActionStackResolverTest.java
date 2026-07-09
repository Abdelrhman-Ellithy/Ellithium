package Ellithium.core.ai.dom;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

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
