package Ellithium.Utilities.assertion.fluent;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Fluent assertions specialized for {@link String}.
 */
public class StringAssert extends AbstractAssert<StringAssert, String> {

    public StringAssert(String actual) {
        super(actual);
    }

    public StringAssert contains(CharSequence sequence) {
        Objects.requireNonNull(sequence, "Sequence to search must not be null");
        if (actual == null || !actual.contains(sequence)) {
            fail("Expected <\"" + actual + "\"> to contain <\"" + sequence + "\">");
        } else {
            pass("<\"" + actual + "\"> contains <\"" + sequence + "\">");
        }
        return this;
    }

    public StringAssert doesNotContain(CharSequence sequence) {
        Objects.requireNonNull(sequence, "Sequence must not be null");
        if (actual != null && actual.contains(sequence)) {
            fail("Expected <\"" + actual + "\"> not to contain <\"" + sequence + "\">");
        } else {
            pass("<\"" + actual + "\"> does not contain <\"" + sequence + "\">");
        }
        return this;
    }

    public StringAssert containsIgnoringCase(CharSequence sequence) {
        Objects.requireNonNull(sequence, "Sequence must not be null");
        if (actual == null || !actual.toLowerCase().contains(sequence.toString().toLowerCase())) {
            fail("Expected <\"" + actual + "\"> to contain (ignoring case) <\"" + sequence + "\">");
        } else {
            pass("<\"" + actual + "\"> contains (ignoring case) <\"" + sequence + "\">");
        }
        return this;
    }

    public StringAssert startsWith(String prefix) {
        Objects.requireNonNull(prefix, "Prefix must not be null");
        if (actual == null || !actual.startsWith(prefix)) {
            fail("Expected <\"" + actual + "\"> to start with <\"" + prefix + "\">");
        } else {
            pass("<\"" + actual + "\"> starts with <\"" + prefix + "\">");
        }
        return this;
    }

    public StringAssert endsWith(String suffix) {
        Objects.requireNonNull(suffix, "Suffix must not be null");
        if (actual == null || !actual.endsWith(suffix)) {
            fail("Expected <\"" + actual + "\"> to end with <\"" + suffix + "\">");
        } else {
            pass("<\"" + actual + "\"> ends with <\"" + suffix + "\">");
        }
        return this;
    }

    public StringAssert matches(String regex) {
        Objects.requireNonNull(regex, "Regex must not be null");
        if (actual == null || !Pattern.matches(regex, actual)) {
            fail("Expected <\"" + actual + "\"> to match regex <" + regex + ">");
        } else {
            pass("<\"" + actual + "\"> matches regex <" + regex + ">");
        }
        return this;
    }

    public StringAssert isEmpty() {
        if (actual == null || !actual.isEmpty()) {
            fail("Expected string to be empty but was <\"" + actual + "\">");
        } else {
            pass("String is empty");
        }
        return this;
    }

    public StringAssert isNotEmpty() {
        if (actual == null || actual.isEmpty()) {
            fail("Expected string not to be empty");
        } else {
            pass("String is not empty: <\"" + actual + "\">");
        }
        return this;
    }

    public StringAssert isBlank() {
        if (actual == null || !actual.isBlank()) {
            fail("Expected string to be blank but was <\"" + actual + "\">");
        } else {
            pass("String is blank");
        }
        return this;
    }

    public StringAssert isNotBlank() {
        if (actual == null || actual.isBlank()) {
            fail("Expected string not to be blank");
        } else {
            pass("String is not blank: <\"" + actual + "\">");
        }
        return this;
    }

    public StringAssert hasSize(int expectedSize) {
        int length = actual != null ? actual.length() : -1;
        if (length != expectedSize) {
            fail("Expected string length <" + expectedSize + "> but was <" + length + ">");
        } else {
            pass("String has size <" + expectedSize + ">");
        }
        return this;
    }

    public StringAssert equalsIgnoreCase(String expected) {
        if (actual == null ? expected != null : !actual.equalsIgnoreCase(expected)) {
            fail("Expected <\"" + actual + "\"> to equal (ignoring case) <\"" + expected + "\">");
        } else {
            pass("<\"" + actual + "\"> equals (ignoring case) <\"" + expected + "\">");
        }
        return this;
    }
}
