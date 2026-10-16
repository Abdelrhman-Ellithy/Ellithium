package Ellithium.Utilities.assertion.fluent;

/**
 * Generic fluent assertion for any object.
 *
 * @param <T> type of actual object under test
 */
public class ObjectAssert<T> extends AbstractAssert<ObjectAssert<T>, T> {

    public ObjectAssert(T actual) {
        super(actual);
    }
}
