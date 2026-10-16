package Ellithium.Utilities.assertion.fluent;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Base class for all fluent assertions with built-in hard/soft evaluation
 * and Allure reporting steps.
 *
 * @param <SELF> the concrete assertion type for fluent chaining
 * @param <ACTUAL> the actual object type under test
 */
public abstract class AbstractAssert<SELF extends AbstractAssert<SELF, ACTUAL>, ACTUAL> {

    protected final ACTUAL actual;
    protected String description;

    protected AbstractAssert(ACTUAL actual) {
        this.actual = actual;
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    /**
     * Attaches a custom description to the assertion for clearer failure messages.
     * @param description custom message
     * @return this assertion instance
     */
    public SELF as(String description) {
        this.description = description;
        return self();
    }

    protected void pass(String message) {
        String fullMsg = formatMessage(message);
        Reporter.log("Assert: ", LogLevel.INFO_GREEN, fullMsg);
    }

    protected void fail(String message) {
        String fullMsg = formatMessage(message);
        Reporter.log("Assert: ", LogLevel.ERROR, fullMsg);
        AssertionError error = new AssertionError(fullMsg);
        SoftAssertScope.recordFailure(error);
    }

    private String formatMessage(String message) {
        if (description != null && !description.isBlank()) {
            return "[" + description + "] " + message;
        }
        return message;
    }

    public SELF isEqualTo(Object expected) {
        if (!Objects.equals(actual, expected)) {
            fail("Expected <" + actual + "> to be equal to <" + expected + ">");
        } else {
            pass("<" + actual + "> is equal to <" + expected + ">");
        }
        return self();
    }

    public SELF isNotEqualTo(Object other) {
        if (Objects.equals(actual, other)) {
            fail("Expected <" + actual + "> to not be equal to <" + other + ">");
        } else {
            pass("<" + actual + "> is not equal to <" + other + ">");
        }
        return self();
    }

    public SELF isNull() {
        if (actual != null) {
            fail("Expected <" + actual + "> to be null");
        } else {
            pass("Object is null");
        }
        return self();
    }

    public SELF isNotNull() {
        if (actual == null) {
            fail("Expected actual value to not be null");
        } else {
            pass("<" + actual + "> is not null");
        }
        return self();
    }

    public SELF isSameAs(Object expected) {
        if (actual != expected) {
            fail("Expected <" + actual + "> to be same instance as <" + expected + ">");
        } else {
            pass("Instances are the same");
        }
        return self();
    }

    public SELF isNotSameAs(Object other) {
        if (actual == other) {
            fail("Expected <" + actual + "> not to be same instance as <" + other + ">");
        } else {
            pass("Instances are not the same");
        }
        return self();
    }

    public SELF isInstanceOf(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }
        if (!type.isInstance(actual)) {
            fail("Expected <" + actual + "> to be an instance of " + type.getName());
        } else {
            pass("<" + actual + "> is an instance of " + type.getName());
        }
        return self();
    }

    public SELF satisfies(Predicate<ACTUAL> predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("Predicate must not be null");
        }
        if (!predicate.test(actual)) {
            fail("Actual value <" + actual + "> did not satisfy predicate");
        } else {
            pass("Actual value <" + actual + "> satisfied predicate");
        }
        return self();
    }
}
