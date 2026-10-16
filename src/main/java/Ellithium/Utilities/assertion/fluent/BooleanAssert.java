package Ellithium.Utilities.assertion.fluent;

/**
 * Fluent assertions specialized for {@link Boolean}.
 */
public class BooleanAssert extends AbstractAssert<BooleanAssert, Boolean> {

    public BooleanAssert(Boolean actual) {
        super(actual);
    }

    public BooleanAssert isTrue() {
        if (!Boolean.TRUE.equals(actual)) {
            fail("Expected condition to be true but was <" + actual + ">");
        } else {
            pass("Condition is true");
        }
        return this;
    }

    public BooleanAssert isFalse() {
        if (!Boolean.FALSE.equals(actual)) {
            fail("Expected condition to be false but was <" + actual + ">");
        } else {
            pass("Condition is false");
        }
        return this;
    }
}
