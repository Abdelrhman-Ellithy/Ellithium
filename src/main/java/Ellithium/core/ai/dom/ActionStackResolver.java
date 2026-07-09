package Ellithium.core.ai.dom;

import java.util.Set;

/**
 * Single source of truth for resolving the user-facing interaction method from a captured stack
 * trace — used both to build the healing request and to label the action sent to the LLM.
 *
 * <p>Exclusion is by CLASS, not by method name: every method (current and future) of the
 * framework's internal find / stale-retry / recovery / exception-handling / healing plumbing is
 * automatically kept out of the model input, so a newly added internal helper can never leak as
 * the "action". Only genuine user-facing interaction methods (in the interactions sub-classes —
 * {@code ElementActions}, {@code MouseActions}, {@code SelectActions}, {@code WaitActions}, …)
 * are eligible. Leaf class — depends only on the JDK.
 */
public final class ActionStackResolver {

    private ActionStackResolver() {
    }

    private static final String INTERACTIONS_PACKAGE = "Ellithium.Utilities.interactions.";

    /**
     * Internal infrastructure classes whose frames must never be reported as the action: the base
     * find/heal/retry engine, the interaction-recovery primitives, the exception classifier, and the
     * wait factory. Their public sub-classes (the actual user actions) are NOT listed and remain
     * eligible.
     */
    private static final Set<String> INFRA_CLASSES = Set.of(
            "Ellithium.Utilities.interactions.BaseActions",
            "Ellithium.Utilities.interactions.InteractionRecovery",
            "Ellithium.Utilities.interactions.SeleniumFailurePolicy",
            "Ellithium.Utilities.interactions.WaitManager");

    private static boolean isInfraFrame(String className) {
        for (String infra : INFRA_CLASSES) {
            if (className.equals(infra) || className.startsWith(infra + "$")) return true;
        }
        return false;
    }

    /**
     * Returns the first user-facing interaction method on the stack (e.g. "clickOnElement",
     * "sendData"), skipping all infrastructure frames. Returns "unknown" if none is found.
     */
    public static String extractAction(StackTraceElement[] stack) {
        if (stack == null) return "unknown";
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            if (cls.startsWith(INTERACTIONS_PACKAGE) && !isInfraFrame(cls)) {
                return frame.getMethodName();
            }
        }
        return "unknown";
    }
}
