package Ellithium.Utilities.assertion.fluent;

import java.util.Collection;
import java.util.Objects;

/**
 * Fluent assertions for collections.
 *
 * @param <E> element type
 * @param <C> collection type
 */
public class CollectionAssert<E, C extends Collection<E>> extends AbstractAssert<CollectionAssert<E, C>, C> {

    public CollectionAssert(C actual) {
        super(actual);
    }

    public CollectionAssert<E, C> contains(E element) {
        if (actual == null || !actual.contains(element)) {
            fail("Expected collection to contain <" + element + "> but elements were <" + actual + ">");
        } else {
            pass("Collection contains <" + element + ">");
        }
        return this;
    }

    public CollectionAssert<E, C> doesNotContain(E element) {
        if (actual != null && actual.contains(element)) {
            fail("Expected collection not to contain <" + element + "> but found in <" + actual + ">");
        } else {
            pass("Collection does not contain <" + element + ">");
        }
        return this;
    }

    public CollectionAssert<E, C> containsAll(Collection<E> elements) {
        Objects.requireNonNull(elements, "Elements collection must not be null");
        if (actual == null || !actual.containsAll(elements)) {
            fail("Expected collection to contain all elements <" + elements + "> but was <" + actual + ">");
        } else {
            pass("Collection contains all requested elements");
        }
        return this;
    }

    public CollectionAssert<E, C> isEmpty() {
        if (actual == null || !actual.isEmpty()) {
            fail("Expected collection to be empty but had size " + (actual != null ? actual.size() : -1));
        } else {
            pass("Collection is empty");
        }
        return this;
    }

    public CollectionAssert<E, C> isNotEmpty() {
        if (actual == null || actual.isEmpty()) {
            fail("Expected collection to not be empty");
        } else {
            pass("Collection is not empty (size: " + actual.size() + ")");
        }
        return this;
    }

    public CollectionAssert<E, C> hasSize(int expectedSize) {
        int size = actual != null ? actual.size() : -1;
        if (size != expectedSize) {
            fail("Expected collection size <" + expectedSize + "> but was <" + size + ">");
        } else {
            pass("Collection has size <" + expectedSize + ">");
        }
        return this;
    }

    public CollectionAssert<E, C> hasSizeGreaterThan(int minExclusive) {
        int size = actual != null ? actual.size() : -1;
        if (size <= minExclusive) {
            fail("Expected collection size to be greater than <" + minExclusive + "> but was <" + size + ">");
        } else {
            pass("Collection size <" + size + "> is greater than <" + minExclusive + ">");
        }
        return this;
    }

    public CollectionAssert<E, C> hasSizeLessThan(int maxExclusive) {
        int size = actual != null ? actual.size() : -1;
        if (size >= maxExclusive) {
            fail("Expected collection size to be less than <" + maxExclusive + "> but was <" + size + ">");
        } else {
            pass("Collection size <" + size + "> is less than <" + maxExclusive + ">");
        }
        return this;
    }
}
