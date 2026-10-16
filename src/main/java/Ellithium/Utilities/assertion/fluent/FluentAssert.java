package Ellithium.Utilities.assertion.fluent;

import java.util.Collection;
import java.util.Map;

/**
 * Entry point for fluent, composable assertions in Ellithium.
 * Supports both hard assertions and scoped soft assertions via {@link SoftAssertScope}.
 */
public final class FluentAssert {

    private FluentAssert() {}

    public static StringAssert assertThat(String actual) {
        return new StringAssert(actual);
    }

    public static BooleanAssert assertThat(Boolean actual) {
        return new BooleanAssert(actual);
    }

    public static <T extends Comparable<T>> ComparableAssert<T> assertThat(T actual) {
        return new ComparableAssert<>(actual);
    }

    public static <E, C extends Collection<E>> CollectionAssert<E, C> assertThat(C actual) {
        return new CollectionAssert<>(actual);
    }

    public static <K, V> MapAssert<K, V> assertThat(Map<K, V> actual) {
        return new MapAssert<>(actual);
    }

    public static <T> ObjectAssert<T> assertThat(T actual) {
        return new ObjectAssert<>(actual);
    }
}
