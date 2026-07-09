package Ellithium.Utilities.interactions;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class StackHygieneTest {

    private static StackTraceElement frame(String cls, String method) {
        return new StackTraceElement(cls, method, cls.substring(cls.lastIndexOf('.') + 1) + ".java", 1);
    }

    @Test
    public void infraFramesAreSkipped_realUserActionSurfaces() {
        StackTraceElement[] stack = {
                frame("java.lang.Thread", "getStackTrace"),
                frame("Ellithium.Utilities.interactions.BaseActions", "healOnce"),
                frame("Ellithium.Utilities.interactions.InteractionRecovery", "resolveInteractable"),
                frame("Ellithium.Utilities.interactions.SeleniumFailurePolicy", "classify"),
                frame("Ellithium.Utilities.interactions.BaseActions", "performWithStaleRetry"),
                frame("Ellithium.Utilities.interactions.ElementActions", "clickOnElement"),
                frame("Pages.LoginPage", "clickSubmit"),
        };
        assertEquals(BaseActions.extractActionFromStack(stack), "clickOnElement");
    }

    @Test
    public void newHelpersInInfraClasses_neverLeakAsAction() {
        String[] hypotheticalFutureHelpers = {
                "resolveInteractable", "drillToInteractive", "scrollToCenter", "awaitClickable",
                "jsClick", "rawVisibleElement", "healOnce", "recoverInListOrRethrow",
                "classify", "isTerminal", "someBrandNewHelperAddedLater"
        };
        for (String helper : hypotheticalFutureHelpers) {
            StackTraceElement[] stack = {
                    frame("Ellithium.Utilities.interactions.InteractionRecovery", helper),
                    frame("Ellithium.Utilities.interactions.BaseActions", helper),
                    frame("Ellithium.Utilities.interactions.SeleniumFailurePolicy", helper),
                    frame("Ellithium.Utilities.interactions.MouseActions", "doubleClick"),
            };
            String action = BaseActions.extractActionFromStack(stack);
            assertEquals(action, "doubleClick", "infra helper leaked as action: " + helper);
            assertNotEquals(action, helper);
        }
    }

    @Test
    public void lambdaAndInnerFramesOfInfraClassesAreSkipped() {
        StackTraceElement[] stack = {
                frame("Ellithium.Utilities.interactions.BaseActions$CachedSource", "lines"),
                frame("Ellithium.Utilities.interactions.BaseActions", "lambda$performWithStaleRetry$0"),
                frame("Ellithium.Utilities.interactions.SelectActions", "selectDropdownByText"),
        };
        assertEquals(BaseActions.extractActionFromStack(stack), "selectDropdownByText");
    }
}
