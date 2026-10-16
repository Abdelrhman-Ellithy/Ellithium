package Ellithium.Utilities.assertion.fluent;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Scoped soft assertion context.
 * Enables collecting multiple assertion failures without aborting test execution,
 * automatically evaluating and reporting all failures upon scope close.
 */
public final class SoftAssertScope implements AutoCloseable {

    private static final ThreadLocal<SoftAssertScope> CURRENT_SCOPE = new ThreadLocal<>();

    private final List<AssertionError> failures = new ArrayList<>();
    private final SoftAssertScope parent;

    private SoftAssertScope() {
        this.parent = CURRENT_SCOPE.get();
        CURRENT_SCOPE.set(this);
    }

    /**
     * Opens a new soft assertion scope for the current thread.
     * @return newly opened scope
     */
    public static SoftAssertScope open() {
        return new SoftAssertScope();
    }

    /**
     * Executes the given assertion block inside an isolated soft assertion scope.
     * @param action block containing fluent assertions
     */
    public static void run(Consumer<SoftAssertScope> action) {
        try (SoftAssertScope scope = open()) {
            action.accept(scope);
        }
    }

    /**
     * Checks if a soft assertion scope is currently active on the calling thread.
     * @return true if soft assertion mode is active, false for hard assertions
     */
    public static boolean isSoft() {
        return CURRENT_SCOPE.get() != null;
    }

    /**
     * Records a failure in the currently active scope, or throws immediately if in hard mode.
     * @param error the assertion error
     */
    public static void recordFailure(AssertionError error) {
        SoftAssertScope scope = CURRENT_SCOPE.get();
        if (scope != null) {
            scope.failures.add(error);
        } else {
            throw error;
        }
    }

    /**
     * Returns all failures collected in this scope so far.
     * @return unmodifiable list of failure errors
     */
    public List<AssertionError> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    /**
     * Asserts that all collected assertions passed.
     * If any failed, throws a compound AssertionError containing all accumulated failures.
     */
    public void assertAll() {
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following ").append(failures.size()).append(" soft assertion(s) failed:\n");
            for (int i = 0; i < failures.size(); i++) {
                sb.append(i + 1).append(") ").append(failures.get(i).getMessage()).append("\n");
            }
            AssertionError compound = new AssertionError(sb.toString().trim());
            for (AssertionError f : failures) {
                compound.addSuppressed(f);
            }
            failures.clear();
            Reporter.log("Soft Assert Failures: ", LogLevel.ERROR, compound.getMessage());
            throw compound;
        }
    }

    @Override
    public void close() {
        CURRENT_SCOPE.set(parent);
        assertAll();
    }
}
