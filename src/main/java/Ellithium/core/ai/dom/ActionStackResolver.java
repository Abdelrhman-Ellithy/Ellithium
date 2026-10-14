package Ellithium.core.ai.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Single source of truth for resolving the user-facing interaction method from a captured stack
 * trace — used both to build the healing request and to label the action sent to the local model and LLM.
 *
 * <p>Excludes internal framework infrastructure classes and methods: every method of the framework's
 * internal find / stale-retry / recovery / shadow-DOM / exception-handling / healing plumbing is
 * automatically kept out of the model input context, so newly added internal helpers can never leak as
 * the "action". Only genuine user-facing interaction methods (in the interactions sub-classes —
 * {@code ElementActions}, {@code MouseActions}, {@code SelectActions}, {@code WaitActions}, …)
 * are eligible.</p>
 */
public final class ActionStackResolver {

    private ActionStackResolver() {
    }

    private static final String INTERACTIONS_PACKAGE = "Ellithium.Utilities.interactions.";

    /**
     * Internal infrastructure classes whose frames must never be reported as the action or sent
     * to the model as context.
     */
    private static final Set<String> INFRA_CLASSES = Set.of(
            "Ellithium.Utilities.interactions.BaseActions",
            "Ellithium.Utilities.interactions.InteractionRecovery",
            "Ellithium.Utilities.interactions.SeleniumFailurePolicy",
            "Ellithium.Utilities.interactions.WaitManager",
            "Ellithium.Utilities.interactions.ShadowDomResolver",
            "Ellithium.Utilities.interactions.NetworkActions");

    /**
     * Internal methods that must be excluded from action extraction and sanitized context.
     */
    private static final Set<String> EXCLUDED_METHODS = Set.of(
            "findWebElement",
            "findWebElements",
            "rawVisibleElement",
            "rawPresentElement",
            "waitForVisibilityAndFindElement",
            "waitForVisibilityAndFindElements",
            "performWithStaleRetry",
            "performAndGet",
            "healOnce",
            "healElementSet",
            "buildHealingRequest",
            "findElement",
            "findElements",
            "findVisibleElement",
            "findNativeShadowElements",
            "findDeepJsElement",
            "findDeepJsElements",
            "toCssSelector",
            "convertSimpleXpathToCss",
            "resolveInteractable",
            "drillToInteractive",
            "isInteractable",
            "normalizeLocator",
            "cleanStackTrace",
            "tryHandlePromptBiDi",
            "getElementCenter",
            "w3cTap",
            "w3cDoubleTap",
            "w3cLongPress",
            "w3cDragAndDrop");

    public static boolean isInfraFrame(String className, String methodName) {
        if (className == null) return false;
        if (className.startsWith("java.lang.Thread") || className.startsWith("java.lang.reflect.")) {
            return true;
        }
        for (String infra : INFRA_CLASSES) {
            if (className.equals(infra) || className.startsWith(infra + "$")) return true;
        }
        if (methodName != null && EXCLUDED_METHODS.contains(methodName)) {
            return true;
        }
        return false;
    }

    public static boolean isInfraFrame(String className) {
        return isInfraFrame(className, null);
    }

    /**
     * Returns the first user-facing interaction method on the stack (e.g. "clickOnElement",
     * "sendData"), skipping all infrastructure frames and internal plumbing methods.
     * Returns "unknown" if none is found.
     */
    public static String extractAction(StackTraceElement[] stack) {
        if (stack == null) return "unknown";
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            String method = frame.getMethodName();
            if (cls.startsWith(INTERACTIONS_PACKAGE) && !isInfraFrame(cls, method)) {
                return method;
            }
        }
        return "unknown";
    }

    /**
     * Sanitizes a captured stack trace by stripping all internal plumbing, shadow resolution,
     * and retry loop frames before passing to local models, embedding engines, or LLM context.
     *
     * @param stack the raw stack trace
     * @return cleaned array of StackTraceElement frames
     */
    public static StackTraceElement[] sanitizeStackTrace(StackTraceElement[] stack) {
        if (stack == null) return new StackTraceElement[0];
        List<StackTraceElement> cleaned = new ArrayList<>();
        for (StackTraceElement frame : stack) {
            if (isInfraFrame(frame.getClassName(), frame.getMethodName())) {
                continue;
            }
            cleaned.add(frame);
        }
        return cleaned.toArray(new StackTraceElement[0]);
    }
}
