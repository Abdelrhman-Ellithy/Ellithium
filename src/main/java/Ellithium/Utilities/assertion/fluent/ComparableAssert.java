package Ellithium.Utilities.assertion.fluent;

import java.util.Objects;

/**
 * Fluent assertions for {@link Comparable} objects (numbers, dates, etc.).
 *
 * @param <T> comparable object type
 */
public class ComparableAssert<T extends Comparable<T>> extends AbstractAssert<ComparableAssert<T>, T> {

    public ComparableAssert(T actual) {
        super(actual);
    }

    public ComparableAssert<T> isGreaterThan(T other) {
        Objects.requireNonNull(other, "Value to compare must not be null");
        if (actual == null || actual.compareTo(other) <= 0) {
            fail("Expected <" + actual + "> to be greater than <" + other + ">");
        } else {
            pass("<" + actual + "> is greater than <" + other + ">");
        }
        return this;
    }

    public ComparableAssert<T> isGreaterThanOrEqualTo(T other) {
        Objects.requireNonNull(other, "Value to compare must not be null");
        if (actual == null || actual.compareTo(other) < 0) {
            fail("Expected <" + actual + "> to be greater than or equal to <" + other + ">");
        } else {
            pass("<" + actual + "> is greater than or equal to <" + other + ">");
        }
        return this;
    }

    public ComparableAssert<T> isLessThan(T other) {
        Objects.requireNonNull(other, "Value to compare must not be null");
        if (actual == null || actual.compareTo(other) >= 0) {
            fail("Expected <" + actual + "> to be less than <" + other + ">");
        } else {
            pass("<" + actual + "> is less than <" + other + ">");
        }
        return this;
    }

    public ComparableAssert<T> isLessThanOrEqualTo(T other) {
        Objects.requireNonNull(other, "Value to compare must not be null");
        if (actual == null || actual.compareTo(other) > 0) {
            fail("Expected <" + actual + "> to be less than or equal to <" + other + ">");
        } else {
            pass("<" + actual + "> is less than or equal to <" + other + ">");
        }
        return this;
    }

    public ComparableAssert<T> isBetween(T start, T end) {
        Objects.requireNonNull(start, "Start bound must not be null");
        Objects.requireNonNull(end, "End bound must not be null");
        if (actual == null || actual.compareTo(start) < 0 || actual.compareTo(end) > 0) {
            fail("Expected <" + actual + "> to be between <" + start + "> and <" + end + ">");
        } else {
            pass("<" + actual + "> is between <" + start + "> and <" + end + ">");
        }
        return this;
    }
}
