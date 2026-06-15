package Ellithium.Utilities.assertion;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import java.util.*;

/**
 * Provides utility methods for performing hard and soft assertions.
 */
public class AssertionExecutor {

    /**
     * Provides static methods for performing hard assertions.
     */
    public static class hard {

        private static void execute(Runnable assertion, String passMsg, String failMsg) {
            try {
                assertion.run();
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, passMsg);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, failMsg);
                throw e;
            }
        }

        public static void assertTrue(boolean condition, String message) {
            execute(() -> Assert.assertTrue(condition, message),
                    " - Condition is true", " - Condition is false");
        }

        public static void assertTrue(boolean condition) {
            execute(() -> Assert.assertTrue(condition),
                    " - Condition is true", " - Condition is not true");
        }

        public static void assertFalse(boolean condition, String message) {
            execute(() -> Assert.assertFalse(condition, message),
                    " - Condition is false", " - Condition is true");
        }

        public static void assertFalse(boolean condition) {
            execute(() -> Assert.assertFalse(condition),
                    " - Condition is false", " - Condition is not false");
        }

        public static void assertNull(Object object, String message) {
            execute(() -> Assert.assertNull(object, message),
                    " - Object is null", " - Object is not null");
        }

        public static void assertNull(Object object) {
            execute(() -> Assert.assertNull(object),
                    " - Object is null", " - Object is not null (Object: " + object + ")");
        }

        public static void assertNotNull(Object object, String message) {
            execute(() -> Assert.assertNotNull(object, message),
                    " - Object is not null", " - Object is null");
        }

        public static void assertNotNull(Object object) {
            execute(() -> Assert.assertNotNull(object),
                    " - Object is not null", " - Object is null");
        }

        public static void assertSame(Object actual, Object expected, String message) {
            execute(() -> Assert.assertSame(actual, expected, message),
                    " - Objects are the same", " - Objects are not the same");
        }

        public static void assertSame(Object actual, Object expected) {
            execute(() -> Assert.assertSame(actual, expected),
                    " - Objects are the same (Expected: " + expected + ", Actual: " + actual + ")",
                    " - Objects are not the same (Expected: " + expected + ", Actual: " + actual + ")");
        }

        public static void assertNotSame(Object actual, Object expected, String message) {
            execute(() -> Assert.assertNotSame(actual, expected, message),
                    " - Objects are not the same", " - Objects are the same");
        }

        public static void assertNotSame(Object actual, Object expected) {
            execute(() -> Assert.assertNotSame(actual, expected),
                    " - Objects are not the same (Expected: " + expected + ", Actual: " + actual + ")",
                    " - Objects are the same (Expected: " + expected + ", Actual: " + actual + ")");
        }

        public static void assertEquals(Object actual, Object expected) {
            execute(() -> Assert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual),
                    " - Objects are equal", " - Objects are not equal");
        }

        public static void assertEquals(Object actual, Object expected, String message) {
            execute(() -> Assert.assertEquals(actual, expected, message),
                    " - Objects are equal", " - Objects are not equal");
        }

        public static void assertEquals(String actual, String expected, String message) {
            execute(() -> Assert.assertEquals(actual, expected, message),
                    " - Strings are equal", " - Strings are not equal");
        }

        public static void assertEquals(long actual, long expected, String message) {
            execute(() -> Assert.assertEquals(actual, expected, message),
                    " - Long values are equal", " - Long values are not equal");
        }

        public static void assertEquals(boolean actual, boolean expected, String message) {
            execute(() -> Assert.assertEquals(actual, expected, message),
                    " - Boolean values are equal", " - Boolean values are not equal");
        }

        public static void assertEquals(double actual, double expected, double delta, String message) {
            execute(() -> Assert.assertEquals(actual, expected, delta, message),
                    String.format(" - Values are equal within delta %f", delta),
                    String.format(" - Values differ by more than delta %f", delta));
        }

        public static void assertContains(Object container, Object value) {
            execute(() -> {
                if (container instanceof String) {
                    Assert.assertTrue(((String) container).contains(String.valueOf(value)),
                            "Container does not contain value: " + value);
                } else if (container instanceof Collection) {
                    Assert.assertTrue(((Collection<?>) container).contains(value),
                            "Container does not contain value: " + value);
                } else {
                    Assert.fail("assertContains requires String or Collection, got: " +
                            (container == null ? "null" : container.getClass().getSimpleName()));
                }
            }, " - Container contains value: " + value, " - Container does not contain value: " + value);
        }

        public static <T> void assertContains(List<T> actual, T value) {
            execute(() -> Assert.assertTrue(actual.contains(value), "List does not contain expected value: " + value),
                    " - List contains value", " - List does not contain value");
        }

        public static <T> void assertContains(List<T> actual, T value, String message) {
            execute(() -> Assert.assertTrue(actual.contains(value), message),
                    " - List contains value (List: " + actual + ", Value: " + value + ")",
                    " - List does not contain value (List: " + actual + ", Value: " + value + ")");
        }

        public static void assertListEquals(List<?> actual, List<?> expected) {
            execute(() -> Assert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual),
                    " - Lists are equal", " - Lists are not equal");
        }

        public static void assertListEquals(List<?> actual, List<?> expected, String message) {
            execute(() -> Assert.assertEquals(actual, expected, message),
                    " - Lists are equal (Expected: " + expected + ", Actual: " + actual + ")",
                    " - Lists are not equal (Expected: " + expected + ", Actual: " + actual + ")");
        }

        public static void assertListContainsAll(List<?> actual, List<?> expected) {
            execute(() -> Assert.assertTrue(new HashSet<>(actual).containsAll(expected),
                            "Actual list does not contain all expected elements."),
                    " - List contains all expected elements", " - List does not contain all expected elements");
        }

        public static void assertListContainsAll(List<?> actual, List<?> expected, String message) {
            execute(() -> Assert.assertTrue(new HashSet<>(actual).containsAll(expected), message),
                    " - All elements of expected list present in actual (Expected: " + expected + ", Actual: " + actual + ")",
                    " - Not all elements of expected list present in actual (Expected: " + expected + ", Actual: " + actual + ")");
        }

        public static void assertGreaterThan(double actual, double expected, String message) {
            execute(() -> Assert.assertTrue(actual > expected, message),
                    " - Actual is greater than Expected", " - Actual is not greater than Expected");
        }

        public static void assertGreaterThan(double actual, double expected) {
            execute(() -> Assert.assertTrue(actual > expected, "Actual value is not greater than expected"),
                    " - Actual: " + actual + " > Expected: " + expected,
                    " - Actual: " + actual + " <= Expected: " + expected);
        }

        public static void assertGreaterThan(int actual, int expected, String message) {
            execute(() -> Assert.assertTrue(actual > expected, message),
                    " - Actual is greater than Expected (int)", " - Actual is not greater than Expected (int)");
        }

        public static void assertGreaterThan(int actual, int expected) {
            execute(() -> Assert.assertTrue(actual > expected,
                            "Actual value: " + actual + " is not greater than expected: " + expected),
                    " - Actual: " + actual + " > Expected: " + expected,
                    " - Actual: " + actual + " <= Expected: " + expected);
        }

        public static void assertLessThan(double actual, double expected, String message) {
            execute(() -> Assert.assertTrue(actual < expected, message),
                    " - Actual is less than Expected", " - Actual is not less than Expected");
        }

        public static void assertLessThan(double actual, double expected) {
            execute(() -> Assert.assertTrue(actual < expected, "Actual value is not less than expected"),
                    " - Actual: " + actual + " < Expected: " + expected,
                    " - Actual: " + actual + " >= Expected: " + expected);
        }

        public static void assertLessThan(int actual, int expected, String message) {
            execute(() -> Assert.assertTrue(actual < expected, message),
                    " - " + message + " (Actual: " + actual + " < Expected: " + expected + ")",
                    " - " + message + " (Actual: " + actual + " >= Expected: " + expected + ")");
        }

        public static void assertLessThan(int actual, int expected) {
            execute(() -> Assert.assertTrue(actual < expected,
                            "Actual value: " + actual + " is not less than expected: " + expected),
                    " - Actual: " + actual + " < Expected: " + expected,
                    " - Actual: " + actual + " >= Expected: " + expected);
        }

        public static void assertEmpty(Object container, String message) {
            execute(() -> {
                if (container instanceof String) {
                    Assert.assertTrue(((String) container).isEmpty(), message);
                } else if (container instanceof Collection) {
                    Assert.assertTrue(((Collection<?>) container).isEmpty(), message);
                } else {
                    Assert.fail(message + " - Unsupported container type: " +
                            (container == null ? "null" : container.getClass().getSimpleName()));
                }
            }, " - Container is empty", " - Container is not empty");
        }

        public static void assertEmpty(Object container) {
            execute(() -> {
                if (container instanceof String) {
                    Assert.assertTrue(((String) container).isEmpty(), "String is not empty");
                } else if (container instanceof Collection) {
                    Assert.assertTrue(((Collection<?>) container).isEmpty(), "Collection is not empty");
                } else {
                    Assert.fail("assertEmpty requires String or Collection, got: " +
                            (container == null ? "null" : container.getClass().getSimpleName()));
                }
            }, " - Container is empty: " + container, " - Container is not empty: " + container);
        }

        public static void assertNotEmpty(Object container, String message) {
            execute(() -> {
                if (container instanceof String) {
                    Assert.assertFalse(((String) container).isEmpty(), message);
                } else if (container instanceof Collection) {
                    Assert.assertFalse(((Collection<?>) container).isEmpty(), message);
                } else {
                    Assert.fail(message + " - Unsupported container type: " +
                            (container == null ? "null" : container.getClass().getSimpleName()));
                }
            }, " - Container is not empty", " - Container is empty");
        }

        public static void assertNotEmpty(Object container) {
            execute(() -> {
                if (container instanceof String) {
                    Assert.assertFalse(((String) container).isEmpty(), "String is empty");
                } else if (container instanceof Collection) {
                    Assert.assertFalse(((Collection<?>) container).isEmpty(), "Collection is empty");
                } else {
                    Assert.fail("assertNotEmpty requires String or Collection, got: " +
                            (container == null ? "null" : container.getClass().getSimpleName()));
                }
            }, " - Container is not empty: " + container, " - Container is empty: " + container);
        }

        public static void assertInstanceOf(Object object, Class<?> clazz, String message) {
            execute(() -> Assert.assertTrue(clazz.isInstance(object), message),
                    " - Object is of the correct type", " - Object is not of the correct type");
        }

        public static void assertInstanceOf(Object object, Class<?> clazz) {
            execute(() -> Assert.assertTrue(clazz.isInstance(object)),
                    " - Object is instance of " + clazz.getSimpleName(),
                    " - Object is not instance of " + clazz.getSimpleName());
        }

        public static void assertContainsIgnoreCase(String actual, String expected) {
            execute(() -> {
                Assert.assertNotNull(actual, "actual string must not be null");
                Assert.assertNotNull(expected, "expected string must not be null");
                Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()),
                        "String does not contain expected value: " + expected);
            }, " - String contains expected value (ignore case)",
               " - String does not contain expected value (ignore case)");
        }

        public static void assertContainsIgnoreCase(String actual, String expected, String message) {
            execute(() -> {
                Assert.assertNotNull(actual, "actual string must not be null");
                Assert.assertNotNull(expected, "expected string must not be null");
                Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), message);
            }, " - String contains (case-insensitive) (Actual: " + actual + ", Expected: " + expected + ")",
               " - String does not contain (case-insensitive) (Actual: " + actual + ", Expected: " + expected + ")");
        }

        public static void assertBetween(int value, int lowerBound, int upperBound) {
            execute(() -> Assert.assertTrue(value >= lowerBound && value <= upperBound,
                            "Value is not within the expected range."),
                    " - Value is within range [" + lowerBound + ", " + upperBound + "]",
                    " - Value is not within range [" + lowerBound + ", " + upperBound + "]");
        }

        public static void assertBetween(int value, int lowerBound, int upperBound, String message) {
            execute(() -> Assert.assertTrue(value >= lowerBound && value <= upperBound, message),
                    " - Value " + value + " is within range [" + lowerBound + ", " + upperBound + "]",
                    " - Value " + value + " is not within range [" + lowerBound + ", " + upperBound + "]");
        }

        public static void assertMatches(String actual, String regex) {
            execute(() -> {
                Assert.assertNotNull(actual, "actual string must not be null");
                Assert.assertTrue(actual.matches(regex), "String does not match the expected pattern.");
            }, " - String matches the regex: " + regex, " - String does not match the regex: " + regex);
        }

        public static void assertMatches(String actual, String regex, String message) {
            execute(() -> {
                Assert.assertNotNull(actual, "actual string must not be null");
                Assert.assertTrue(actual.matches(regex), message);
            }, " - String matches the regex pattern", " - String does not match the regex pattern");
        }

        public static void assertSameSize(List<?> actual, List<?> expected) {
            execute(() -> Assert.assertEquals(actual.size(), expected.size(), "Lists do not have the same size."),
                    " - Lists have the same size.", " - Lists do not have the same size.");
        }

        public static void assertSameSize(List<?> actual, List<?> expected, String message) {
            execute(() -> Assert.assertEquals(actual.size(), expected.size(), message),
                    " - Lists have the same size", " - Lists do not have the same size");
        }

        public static void assertMapEmpty(Map<?, ?> map) {
            execute(() -> Assert.assertTrue(map.isEmpty(), "Map is not empty."),
                    " - Map is empty.", " - Map is not empty.");
        }

        public static void assertMapEmpty(Map<?, ?> map, String message) {
            execute(() -> Assert.assertTrue(map.isEmpty(), message),
                    " - Map is empty", " - Map is not empty");
        }

        public static void assertMapNotEmpty(Map<?, ?> map) {
            execute(() -> Assert.assertFalse(map.isEmpty(), "Map is empty."),
                    " - Map is not empty.", " - Map is empty.");
        }

        public static void assertMapNotEmpty(Map<?, ?> map, String message) {
            execute(() -> Assert.assertFalse(map.isEmpty(), message),
                    " - Map is not empty", " - Map is empty");
        }

        public static void assertStartsWith(String actual, String prefix) {
            execute(() -> {
                Assert.assertNotNull(actual, "actual string must not be null");
                Assert.assertTrue(actual.startsWith(prefix), "String does not start with the expected prefix.");
            }, " - String starts with the prefix: " + prefix, " - String does not start with the prefix: " + prefix);
        }

        public static void assertStartsWith(String actual, String prefix, String message) {
            execute(() -> {
                Assert.assertNotNull(actual, "actual string must not be null");
                Assert.assertTrue(actual.startsWith(prefix), message);
            }, " - String starts with prefix: " + prefix, " - String does not start with prefix: " + prefix);
        }

        public static void assertEndsWith(String actual, String suffix) {
            execute(() -> {
                Assert.assertNotNull(actual, "actual string must not be null");
                Assert.assertTrue(actual.endsWith(suffix), "String does not end with the expected suffix.");
            }, " - String ends with the suffix: " + suffix, " - String does not end with the suffix: " + suffix);
        }

        public static void assertEndsWith(String actual, String suffix, String message) {
            execute(() -> {
                Assert.assertNotNull(actual, "actual string must not be null");
                Assert.assertTrue(actual.endsWith(suffix), message);
            }, " - String ends with suffix: " + suffix, " - String does not end with suffix: " + suffix);
        }

        public static void assertArrayEquals(int[] actual, int[] expected) {
            execute(() -> Assert.assertEquals(actual, expected, "Arrays are not equal"),
                    " - Integer arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")",
                    " - Integer arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
        }

        public static void assertArrayEquals(int[] actual, int[] expected, String message) {
            execute(() -> Assert.assertEquals(actual, expected, message),
                    " - Integer arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")",
                    " - Integer arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
        }

        public static void assertArrayEquals(double[] actual, double[] expected) {
            execute(() -> Assert.assertEquals(actual, expected, "Double arrays are not equal"),
                    " - Double arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")",
                    " - Double arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
        }

        public static void assertArrayEquals(double[] actual, double[] expected, double delta) {
            execute(() -> Assert.assertEquals(actual, expected, delta, "Double arrays are not equal within tolerance"),
                    " - Double arrays are equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")",
                    " - Double arrays are not equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
        }

        public static void assertArrayEquals(double[] actual, double[] expected, double delta, String message) {
            execute(() -> Assert.assertEquals(actual, expected, delta, message),
                    " - Double arrays are equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")",
                    " - Double arrays are not equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
        }

        public static void assertEqualsNoOrder(Object[] actual, Object[] expected, String message) {
            execute(() -> Assert.assertEqualsNoOrder(actual, expected, message),
                    " - Arrays are equal (ignoring order)", " - Arrays are not equal (ignoring order)");
        }

        public static void fail(String message) {
            try {
                Assert.fail(message);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Forced failure");
                throw e;
            }
        }

        public static void fail(String message, Throwable throwable) {
            try {
                Assert.fail(message, throwable);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Forced failure with throwable");
                throw e;
            }
        }
    }

    /**
     * Provides methods for performing soft assertions.
     */
    public static class soft {
        private final SoftAssert softAssert = new SoftAssert();

        public void assertTrue(boolean condition, String message) {
            softAssert.assertTrue(condition, message);
            Reporter.log("Soft Assert: ", condition ? LogLevel.INFO_GREEN : LogLevel.WARN, "Condition is " + condition + " - " + message);
        }

        public void assertTrue(boolean condition) {
            softAssert.assertTrue(condition, "Condition should be true but was false");
            Reporter.log("Soft Assert: Condition is " + condition, condition ? LogLevel.INFO_GREEN : LogLevel.WARN);
        }

        public void assertFalse(boolean condition, String message) {
            softAssert.assertFalse(condition, message);
            Reporter.log("Soft Assert: ", !condition ? LogLevel.INFO_GREEN : LogLevel.WARN, "Condition is " + condition + " - " + message);
        }

        public void assertFalse(boolean condition) {
            softAssert.assertFalse(condition, "Condition should be false but was true");
            Reporter.log("Soft Assert: Condition is " + condition, !condition ? LogLevel.INFO_GREEN : LogLevel.WARN);
        }

        public void assertNull(Object object, String message) {
            softAssert.assertNull(object, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Object is null: " + object + " - " + message);
        }

        public void assertNull(Object object) {
            softAssert.assertNull(object, "Object should be null but was not");
            Reporter.log("Soft Assert: Object is null", LogLevel.INFO_GREEN);
        }

        public void assertNotNull(Object object, String message) {
            softAssert.assertNotNull(object, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Object is not null: " + object + " - " + message);
        }

        public void assertNotNull(Object object) {
            softAssert.assertNotNull(object, "Object should not be null but was");
            Reporter.log("Soft Assert: Object is not null", LogLevel.INFO_GREEN);
        }

        public void assertSame(Object actual, Object expected, String message) {
            softAssert.assertSame(actual, expected, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are the same - Expected: " + expected + ", Actual: " + actual + " - " + message);
        }

        public void assertSame(Object actual, Object expected) {
            softAssert.assertSame(actual, expected, "Expected and actual objects should be the same but were not");
            Reporter.log("Soft Assert Same - Expected: " + expected + " - Actual: " + actual, LogLevel.INFO_GREEN);
        }

        public void assertNotSame(Object actual, Object expected, String message) {
            softAssert.assertNotSame(actual, expected, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are not the same - Expected: " + expected + ", Actual: " + actual + " - " + message);
        }

        public void assertNotSame(Object actual, Object expected) {
            softAssert.assertNotSame(actual, expected, "Expected and actual objects should not be the same but were");
            Reporter.log("Soft Assert Not Same - Expected: " + expected + " - Actual: " + actual, LogLevel.INFO_GREEN);
        }

        public void assertEquals(Object actual, Object expected) {
            softAssert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are equal - Expected: " + expected + ", Actual: " + actual);
        }

        public void assertEquals(Object actual, Object expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Objects are equal");
        }

        public void assertEquals(String actual, String expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Strings are equal");
        }

        public void assertEquals(long actual, long expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Long values are equal");
        }

        public void assertEquals(boolean actual, boolean expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Boolean values are equal");
        }

        public void assertEquals(double actual, double expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Double values are equal");
        }

        public void assertEquals(double actual, double expected) {
            softAssert.assertEquals(actual, expected);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, " - Double values are equal");
        }

        public void assertEquals(double actual, double expected, double delta, String message) {
            softAssert.assertEquals(actual, expected, delta, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN,
                String.format(" - Values are equal within delta %f", delta));
        }

        public void assertEquals(byte[] actual, byte[] expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Byte values are equal");
        }

        public void assertEquals(byte[] actual, byte[] expected) {
            softAssert.assertEquals(actual, expected);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, " - Byte values are equal");
        }

        public void assertEquals(List<?> actual, List<?> expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - List values are equal");
        }

        public void assertEquals(List<?> actual, List<?> expected) {
            softAssert.assertEquals(actual, expected);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, " - List values are equal");
        }

        public void assertContains(Object container, Object value) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains(String.valueOf(value)),
                        "Container does not contain value: " + value);
                Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "String contains value - Container: " + container + ", Value: " + value);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value),
                        "Container does not contain value: " + value);
                Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Collection contains value - Container: " + container + ", Value: " + value);
            } else {
                softAssert.fail("assertContains requires String or Collection, got: " +
                        (container == null ? "null" : container.getClass().getSimpleName()));
            }
        }

        public void assertContains(Object container, Object value, String message) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains(String.valueOf(value)), message);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value), message);
            } else {
                softAssert.fail(message + " - Unsupported container type");
            }
            Reporter.log("Soft Assert Contains - Container: " + container + " contains Value: " + value, LogLevel.INFO_GREEN);
        }

        public <T> void assertContains(List<T> actual, T value) {
            softAssert.assertTrue(actual.contains(value), "List does not contain expected value: " + value);
            Reporter.log("Soft Assert: List contains value: " + value, LogLevel.INFO_GREEN, " - Actual List: " + actual);
        }

        public <T> void assertContains(List<T> actual, T value, String message) {
            softAssert.assertTrue(actual.contains(value), message);
            Reporter.log("Soft Assert Contains - List: " + actual + " contains Value: " + value, LogLevel.INFO_GREEN);
        }

        public void assertListEquals(List<?> actual, List<?> expected) {
            softAssert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Lists are equal - Expected: " + expected + ", Actual: " + actual);
        }

        public void assertListEquals(List<?> actual, List<?> expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert List Equals - Expected: " + expected + " - Actual: " + actual, LogLevel.INFO_GREEN);
        }

        public void assertListContainsAll(List<?> actual, List<?> expected) {
            softAssert.assertTrue(new HashSet<>(actual).containsAll(expected), "Actual list does not contain all expected elements.");
            Reporter.log("Soft Assert: Actual list contains all expected elements - Actual: " + actual, LogLevel.INFO_GREEN, ", Expected: " + expected);
        }

        public void assertListContainsAll(List<?> actual, List<?> expected, String message) {
            softAssert.assertTrue(new HashSet<>(actual).containsAll(expected), message);
            Reporter.log("Soft Assert List Contains All - Actual: " + actual + " - Expected Elements: " + expected, LogLevel.INFO_BLUE);
        }

        public void assertGreaterThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual, LogLevel.INFO_GREEN, " > Expected: " + expected);
        }

        public void assertGreaterThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual, LogLevel.INFO_GREEN, " > Expected: " + expected);
        }

        public void assertLessThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual, LogLevel.INFO_GREEN, " < Expected: " + expected);
        }

        public void assertLessThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual, LogLevel.INFO_GREEN, " < Expected: " + expected);
        }

        public void assertEmpty(Object container, String message) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).isEmpty(), message);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).isEmpty(), message);
            } else {
                softAssert.fail(message + " - Unsupported container type: " +
                        (container == null ? "null" : container.getClass().getSimpleName()));
            }
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Container is empty: " + container);
        }

        public void assertNotEmpty(Object container, String message) {
            if (container instanceof String) {
                softAssert.assertTrue(!((String) container).isEmpty(), message);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(!((Collection<?>) container).isEmpty(), message);
            } else {
                softAssert.fail(message + " - Unsupported container type: " +
                        (container == null ? "null" : container.getClass().getSimpleName()));
            }
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Container is not empty: " + container);
        }

        public void assertInstanceOf(Object object, Class<?> clazz, String message) {
            softAssert.assertTrue(clazz.isInstance(object), message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Object: " + object + " is an instance of: " + clazz.getName());
        }

        public void assertContainsIgnoreCase(String actual, String expected) {
            if (actual == null || expected == null) {
                softAssert.fail("assertContainsIgnoreCase: actual and expected must not be null");
            } else {
                softAssert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()),
                        "String does not contain expected value: " + expected);
            }
            Reporter.log("Soft Assert: Actual string contains expected string (ignore case) - Actual: " + actual, LogLevel.INFO_GREEN, ", Expected: " + expected);
        }

        public void assertContainsIgnoreCase(String actual, String expected, String message) {
            if (actual == null || expected == null) {
                softAssert.fail(message + " - actual and expected must not be null");
            } else {
                softAssert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), message);
            }
            Reporter.log("Soft Assert Contains Ignore Case - Actual: " + actual + " contains Expected: " + expected, LogLevel.INFO_BLUE);
        }

        public void assertBetween(int value, int lowerBound, int upperBound, String message) {
            softAssert.assertTrue(value >= lowerBound && value <= upperBound, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN,
                " - Value " + value + " is within range [" + lowerBound + ", " + upperBound + "]");
        }

        public void assertMatches(String actual, String regex, String message) {
            if (actual == null) {
                softAssert.fail(message + " - actual string must not be null");
            } else {
                softAssert.assertTrue(actual.matches(regex), message);
            }
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - String matches the regex pattern");
        }

        public void assertSameSize(List<?> actual, List<?> expected, String message) {
            softAssert.assertEquals(actual.size(), expected.size(), message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Lists have the same size");
        }

        public void assertArrayEquals(int[] actual, int[] expected) {
            softAssert.assertEquals(actual, expected, "Arrays are not equal");
            Reporter.log("Soft Assert: Integer arrays are equal - Expected: " + Arrays.toString(expected), LogLevel.INFO_GREEN, ", Actual: " + Arrays.toString(actual));
        }

        public void assertArrayEquals(int[] actual, int[] expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert Array Equals (int[]) - Expected: " + Arrays.toString(expected) + " - Actual: " + Arrays.toString(actual), LogLevel.INFO_GREEN);
        }

        public void assertArrayEquals(double[] actual, double[] expected, double delta) {
            if (actual.length != expected.length) {
                softAssert.fail("Arrays have different lengths: expected " + expected.length + ", actual " + actual.length);
                Reporter.log("Soft Assert Fail: Arrays have different lengths - Expected: " + expected.length, LogLevel.ERROR, ", Actual: " + actual.length);
                return;
            }
            for (int i = 0; i < actual.length; i++) {
                softAssert.assertTrue(Math.abs(actual[i] - expected[i]) <= delta,
                        "Array elements at index " + i + " differ by more than " + delta +
                        ". Expected: " + expected[i] + ", Actual: " + actual[i]);
                Reporter.log("Soft Assert: Array element at index " + i + " - Expected: " + expected[i], LogLevel.INFO_GREEN, ", Actual: " + actual[i] + ", Tolerance: " + delta);
            }
        }

        public void assertArrayEquals(double[] actual, double[] expected, double delta, String message) {
            if (actual.length != expected.length) {
                softAssert.fail("Arrays have different lengths: expected " + expected.length + ", actual " + actual.length);
                Reporter.log("Soft Assert Fail: Arrays have different lengths", LogLevel.ERROR, " - Expected: " + expected.length + ", Actual: " + actual.length);
                return;
            }
            for (int i = 0; i < actual.length; i++) {
                softAssert.assertTrue(Math.abs(actual[i] - expected[i]) <= delta,
                        message + " Array elements at index " + i + " differ by more than " + delta +
                        ". Expected: " + expected[i] + ", Actual: " + actual[i]);
                Reporter.log("Soft Assert: " + message + " Array element at index " + i + " within tolerance: " + delta, LogLevel.INFO_GREEN);
            }
        }

        public void assertEqualsNoOrder(Object[] actual, Object[] expected, String message) {
            softAssert.assertEqualsNoOrder(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Arrays are equal (ignoring order)");
        }

        public void assertNotEquals(int actual, int expected, String message) {
            softAssert.assertNotEquals(actual, expected, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, " - values are not equal");
        }

        public void assertNotEquals(int actual, int expected) {
            softAssert.assertNotEquals(actual, expected);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, " - values are not equal");
        }

        public void fail(String message) {
            softAssert.fail(message);
            Reporter.log("Soft Assert: " + message, LogLevel.ERROR, " - Forced failure");
        }

        public void fail(String message, Throwable throwable) {
            softAssert.fail(message, throwable);
            Reporter.log("Soft Assert: " + message, LogLevel.ERROR, " - Forced failure with throwable");
        }

        public void assertAll() {
            try {
                softAssert.assertAll();
                Reporter.log("Soft Assertions Completed and Passed. Validating all collected conditions.", LogLevel.INFO_GREEN);
            } catch (AssertionError e) {
                Reporter.log("Soft Assertions Completed and Failed. Validating all collected conditions.", LogLevel.ERROR);
                throw e;
            }
        }
    }
}
