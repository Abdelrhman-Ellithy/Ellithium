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
        /**
         * Assert that a condition is true.
         * @param condition the condition to evaluate.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the condition is false.
         */
        public static void assertTrue(boolean condition, String message) {
            try {
                Assert.assertTrue(condition, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN," - Condition is true");
            }catch (AssertionError e){
                Reporter.log("Hard Assert: ", LogLevel.ERROR," - Condition is false");
                throw e;
            }
        }

        /**
         * Assert that a condition is false.
         * @param condition the condition to evaluate.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the condition is true.
         */
        public static void assertFalse(boolean condition, String message) {
            try {
                Assert.assertFalse(condition, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Condition is false");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Condition is true");
                throw e;
            }
        }

        /**
         * Assert that an object is null.
         * @param object the object to check.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the object is not null.
         */
        public static void assertNull(Object object, String message) {
            try {
                Assert.assertNull(object, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is null");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is not null");
                throw e;
            }
        }

        /**
         * Assert that an object is not null.
         * @param object the object to check.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the object is null.
         */
        public static void assertNotNull(Object object, String message) {
            try {
                Assert.assertNotNull(object, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is not null");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is null");
                throw e;
            }
        }

        /**
         * Assert that two objects are the same (reference equality).
         * @param actual the actual object.
         * @param expected the expected object.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the objects are not the same.
         */
        public static void assertSame(Object actual, Object expected, String message) {
            try {
                Assert.assertSame(actual, expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are the same");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are not the same");
                throw e;
            }
        }

        /**
         * Assert that two objects are not the same (reference equality).
         * @param actual the actual object.
         * @param expected the expected object.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the objects are the same.
         */
        public static void assertNotSame(Object actual, Object expected, String message) {
            try {
                Assert.assertNotSame(actual, expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are not the same");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are the same");
                throw e;
            }
        }

        /**
         * Assert that two objects are equal.
         * @param actual the actual object.
         * @param expected the expected object.
         * @throws AssertionError if the objects are not equal.
         */
        public static void assertEquals(Object actual, Object expected) {
            try {
                Assert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are equal");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are not equal");
                throw e;
            }
        }

        /**
         * Assert that a collection or string contains a specific value.
         * @param container the collection or string to check.
         * @param value the value that is expected to be present in the container.
         * @throws AssertionError if the container does not contain the value.
         */
        public static void assertContains(Object container, Object value) {
            try {
                if (container instanceof String) {
                    Assert.assertTrue(((String) container).contains((String) value), "Container does not contain value: " + value);
                } else if (container instanceof Collection) {
                    Assert.assertTrue(((Collection<?>) container).contains(value), "Container does not contain value: " + value);
                }
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Container contains value: " + value);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Container does not contain value: " + value);
                throw e;
            }
        }

        /**
         * Assert that two lists are equal.
         * @param actual the actual list.
         * @param expected the expected list.
         * @throws AssertionError if the lists are not equal.
         */
        public static void assertListEquals(List<?> actual, List<?> expected) {
            try {
                Assert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Lists are equal");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Lists are not equal");
                throw e;
            }
        }

        /**
         * Assert that all elements of one list are present in another list.
         * @param actual the actual list.
         * @param expected the expected list.
         * @throws AssertionError if not all elements of the expected list are present in the actual list.
         */
        public static void assertListContainsAll(List<?> actual, List<?> expected) {
            try {
                Assert.assertTrue(new HashSet<>(actual).containsAll(expected), "Actual list does not contain all expected elements.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - List contains all expected elements");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - List does not contain all expected elements");
                throw e;
            }
        }

        /**
         * Assert that a value is greater than another value.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the actual value is not greater than the expected value.
         */
        public static void assertGreaterThan(double actual, double expected, String message) {
            try {
                Assert.assertTrue(actual > expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual is greater than Expected");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual is not greater than Expected");
                throw e;
            }
        }

        /**
         * Assert that a value is less than another value.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the actual value is not less than the expected value.
         */
        public static void assertLessThan(double actual, double expected, String message) {
            try {
                Assert.assertTrue(actual < expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual is less than Expected");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual is not less than Expected");
                throw e;
            }
        }

        /**
         * Assert that a collection or string is empty.
         * @param container the collection or string to check.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the container is not empty.
         */
        public static void assertEmpty(Object container, String message) {
            try {
                if (container instanceof String) {
                    Assert.assertTrue(((String) container).isEmpty(), message);
                } else if (container instanceof Collection) {
                    Assert.assertTrue(((Collection<?>) container).isEmpty(), message);
                }
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Container is empty");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Container is not empty");
                throw e;
            }
        }

        /**
         * Assert that a collection or string is not empty.
         * @param container the collection or string to check.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the container is empty.
         */
        public static void assertNotEmpty(Object container, String message) {
            try {
                if (container instanceof String) {
                    Assert.assertFalse(((String) container).isEmpty(), message);
                } else if (container instanceof Collection) {
                    Assert.assertFalse(((Collection<?>) container).isEmpty(), message);
                }
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Container is not empty");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Container is empty");
                throw e;
            }
        }

        /**
         * Assert that an object is of a specific type.
         * @param object the object to check.
         * @param clazz the expected class of the object.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the object is not of the specified type.
         */
        public static void assertInstanceOf(Object object, Class<?> clazz, String message) {
            try {
                Assert.assertTrue(clazz.isInstance(object), message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is of the correct type");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is not of the correct type");
                throw e;
            }
        }

        /**
         * Assert that a string contains another string (case-insensitive).
         * @param actual the actual string.
         * @param expected the expected string.
         * @throws AssertionError if the actual string does not contain the expected string (case-insensitive).
         */
        public static void assertContainsIgnoreCase(String actual, String expected) {
            try {
                Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), "String does not contain expected value: " + expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String contains expected value (ignore case)");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not contain expected value (ignore case)");
                throw e;
            }
        }

        /**
         * Overload: Assert that a list contains a specific value.
         * @param actual the actual list.
         * @param value the value that is expected to be present in the list.
         * @param <T> the type of elements in the list.
         * @throws AssertionError if the list does not contain the value.
         */
        public static <T> void assertContains(List<T> actual, T value) {
            try {
                Assert.assertTrue(actual.contains(value), "List does not contain expected value: " + value);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - List contains value");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - List does not contain value");
                throw e;
            }
        }

        /**
         * Assert that a value is greater than another value (int).
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the actual value is not greater than the expected value.
         */
        public static void assertGreaterThan(int actual, int expected, String message) {
            try {
                Assert.assertTrue(actual > expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual is greater than Expected (int)");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual is not greater than Expected (int)");
                throw e;
            }
        }

        /**
         * Assert that a value is between two values (inclusive).
         * @param value the value to check.
         * @param lowerBound the lower bound of the range.
         * @param upperBound the upper bound of the range.
         * @throws AssertionError if the value is not within the specified range.
         */
        public static void assertBetween(int value, int lowerBound, int upperBound) {
            try {
                Assert.assertTrue(value >= lowerBound && value <= upperBound, "Value is not within the expected range.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Value is within range [" + lowerBound + ", " + upperBound + "]");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Value is not within range [" + lowerBound + ", " + upperBound + "]");
                throw e; // Re-throw exception to ensure test fails
            }
        }

        /**
         * Assert that a string matches a regular expression.
         * @param actual the actual string.
         * @param regex the regular expression to match.
         * @throws AssertionError if the string does not match the regular expression.
         */
        public static void assertMatches(String actual, String regex) {
            try {
                Assert.assertTrue(actual.matches(regex), "String does not match the expected pattern.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String matches the regex: " + regex);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not match the regex: " + regex);
                throw e;
            }
        }

        /**
         * Assert that two lists have the same size.
         * @param actual the actual list.
         * @param expected the expected list.
         * @throws AssertionError if the lists do not have the same size.
         */
        public static void assertSameSize(List<?> actual, List<?> expected) {
            try {
                Assert.assertEquals(actual.size(), expected.size(), "Lists do not have the same size.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Lists have the same size.");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Lists do not have the same size.");
                throw e;
            }
        }

        /**
         * Assert that a map is empty.
         * @param map the map to check.
         * @throws AssertionError if the map is not empty.
         */
        public static void assertMapEmpty(Map<?, ?> map) {
            try {
                Assert.assertTrue(map.isEmpty(), "Map is not empty.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Map is empty.");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Map is not empty.");
                throw e;
            }
        }

        /**
         * Assert that a map is not empty.
         * @param map the map to check.
         * @throws AssertionError if the map is empty.
         */
        public static void assertMapNotEmpty(Map<?, ?> map) {
            try {
                Assert.assertFalse(map.isEmpty(), "Map is empty.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Map is not empty.");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Map is empty.");
                throw e;
            }
        }

        /**
         * Assert that a string starts with a specific prefix.
         * @param actual the actual string.
         * @param prefix the expected prefix.
         * @throws AssertionError if the string does not start with the specified prefix.
         */
        public static void assertStartsWith(String actual, String prefix) {
            try {
                Assert.assertTrue(actual.startsWith(prefix), "String does not start with the expected prefix.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String starts with the prefix: " + prefix);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not start with the prefix: " + prefix);
                throw e;
            }
        }

        /**
         * Assert that a string ends with a specific suffix.
         * @param actual the actual string.
         * @param suffix the expected suffix.
         * @throws AssertionError if the string does not end with the specified suffix.
         */
        public static void assertEndsWith(String actual, String suffix) {
            try {
                Assert.assertTrue(actual.endsWith(suffix), "String does not end with the expected suffix.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String ends with the suffix: " + suffix);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not end with the suffix: " + suffix);
                throw e;
            }
        }

        /**
         * Assert that a value is less than another value (int).
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the actual value is not less than the expected value.
         */
        public static void assertLessThan(int actual, int expected, String message) {
            try {
                Assert.assertTrue(actual < expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - " + message + " (Actual: " + actual + " < Expected: " + expected + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - " + message + " (Actual: " + actual + " >= Expected: " + expected + ")");
                throw e;
            }
        }

        /**
         * Assert that two integer arrays are equal.
         * @param actual the actual array.
         * @param expected the expected array.
         * @throws AssertionError if the arrays are not equal.
         */
        public static void assertArrayEquals(int[] actual, int[] expected) {
            try {
                Assert.assertEquals(actual, expected, "Arrays are not equal");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Integer arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Integer arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        /**
         * Assert that two double arrays are equal within a tolerance.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param delta the maximum difference between two values for considering them equal.
         * @throws AssertionError if the arrays are not equal within the specified tolerance.
         */
        public static void assertArrayEquals(double[] actual, double[] expected, double delta) {
            try {
                Assert.assertEquals(actual, expected, delta, "Double arrays are not equal within tolerance");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Double arrays are equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Double arrays are not equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        /**
         * Overloaded assertTrue without message.
         * @param condition the condition to evaluate.
         * @throws AssertionError if the condition is false.
         */
        public static void assertTrue(boolean condition) {
            try {
                Assert.assertTrue(condition);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Condition is true");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Condition is not true");
                throw e;
            }
        }

        /**
         * Overloaded assertFalse without message.
         * @param condition the condition to evaluate.
         * @throws AssertionError if the condition is true.
         */
        public static void assertFalse(boolean condition) {
            try {
                Assert.assertFalse(condition);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Condition is false");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Condition is not false");
                throw e;
            }
        }

        /**
         * Overloaded assertNull without message.
         * @param object the object to check.
         * @throws AssertionError if the object is not null.
         */
        public static void assertNull(Object object) {
            try {
                Assert.assertNull(object);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is null");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is not null (Object: " + object + ")");
                throw e;
            }
        }

        /**
         * Overloaded assertNotNull without message.
         * @param object the object to check.
         * @throws AssertionError if the object is null.
         */
        public static void assertNotNull(Object object) {
            try {
                Assert.assertNotNull(object);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is not null");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is null");
                throw e;
            }
        }

        /**
         * Overloaded assertSame without message.
         * @param actual the actual object.
         * @param expected the expected object.
         * @throws AssertionError if the objects are not the same.
         */
        public static void assertSame(Object actual, Object expected) {
            try {
                Assert.assertSame(actual, expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are the same (Expected: " + expected + ", Actual: " + actual + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are not the same (Expected: " + expected + ", Actual: " + actual + ")");
                throw e;
            }
        }

        /**
         * Overloaded assertNotSame without message.
         * @param actual the actual object.
         * @param expected the expected object.
         * @throws AssertionError if the objects are the same.
         */
        public static void assertNotSame(Object actual, Object expected) {
            try {
                Assert.assertNotSame(actual, expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are not the same (Expected: " + expected + ", Actual: " + actual + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are the same (Expected: " + expected + ", Actual: " + actual + ")");
                throw e;
            }
        }

        /**
         * Overloaded assertGreaterThan without message (for double).
         * @param actual the actual value.
         * @param expected the expected value.
         * @throws AssertionError if the actual value is not greater than the expected value.
         */
        public static void assertGreaterThan(double actual, double expected) {
            try {
                Assert.assertTrue(actual > expected, "Actual value is not greater than expected");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual: " + actual + " > Expected: " + expected);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual: " + actual + " <= Expected: " + expected);
                throw e;
            }
        }

        /**
         * Overloaded assertLessThan without message (for double).
         * @param actual the actual value.
         * @param expected the expected value.
         * @throws AssertionError if the actual value is not less than the expected value.
         */
        public static void assertLessThan(double actual, double expected) {
            try {
                Assert.assertTrue(actual < expected, "Actual value is not less than expected");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual: " + actual + " < Expected: " + expected);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual: " + actual + " >= Expected: " + expected);
                throw e;
            }
        }

        /**
         * Overloaded assertGreaterThan without message (for int).
         * @param actual the actual value.
         * @param expected the expected value.
         * @throws AssertionError if the actual value is not greater than the expected value.
         */
        public static void assertGreaterThan(int actual, int expected) {
            try {
                Assert.assertTrue(actual > expected, "Actual value: " + actual + " is not greater than expected: " + expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual: " + actual + " > Expected: " + expected);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual: " + actual + " <= Expected: " + expected);
                throw e;
            }
        }

        /**
         * Overloaded assertLessThan without message (for int).
         * @param actual the actual value.
         * @param expected the expected value.
         * @throws AssertionError if the actual value is not less than the expected value.
         */
        public static void assertLessThan(int actual, int expected) {
            try {
                Assert.assertTrue(actual < expected, "Actual value: " + actual + " is not less than expected: " + expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual: " + actual + " < Expected: " + expected);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual: " + actual + " >= Expected: " + expected);
                throw e;
            }
        }

        /**
         * Overloaded assertArrayEquals without delta (for double arrays).
         * @param actual the actual array.
         * @param expected the expected array.
         * @throws AssertionError if the arrays are not equal.
         */
        public static void assertArrayEquals(double[] actual, double[] expected) {
            try {
                Assert.assertEquals(actual, expected, "Double arrays are not equal");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Double arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Double arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        /**
         * Overloaded assertEmpty without message.
         * @param container the container to check.
         * @throws AssertionError if the container is not empty.
         */
        public static void assertEmpty(Object container) {
            try {
                if (container instanceof String) {
                    Assert.assertTrue(((String) container).isEmpty(), "String is not empty");
                } else if (container instanceof Collection) {
                    Assert.assertTrue(((Collection<?>) container).isEmpty(), "Collection is not empty");
                }
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Container is empty: " + container);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Container is not empty: " + container);
                throw e;
            }
        }

        /**
         * Overloaded assertNotEmpty without message.
         * @param container the container to check.
         * @throws AssertionError if the container is empty.
         */
        public static void assertNotEmpty(Object container) {
            try {
                if (container instanceof String) {
                    Assert.assertFalse(((String) container).isEmpty(), "String is empty");
                } else if (container instanceof Collection) {
                    Assert.assertFalse(((Collection<?>) container).isEmpty(), "Collection is empty");
                }
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Container is not empty: " + container);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Container is empty: " + container);
                throw e;
            }
        }

        /**
         * Overloaded: Assert that two lists are equal with a message.
         * @param actual the actual list.
         * @param expected the expected list.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the lists are not equal.
         */
        public static void assertListEquals(List<?> actual, List<?> expected, String message) {
            try {
                Assert.assertEquals(actual, expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Lists are equal (Expected: " + expected + ", Actual: " + actual + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Lists are not equal (Expected: " + expected + ", Actual: " + actual + ")");
                throw e;
            }
        }

        /**
         * Overloaded: Assert that all elements of one list are present in another list with a message.
         * @param actual the actual list.
         * @param expected the expected list.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if not all elements of the expected list are present in the actual list.
         */
        public static void assertListContainsAll(List<?> actual, List<?> expected, String message) {
            try {
                Assert.assertTrue(new HashSet<>(actual).containsAll(expected), message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - All elements of the expected list are present in the actual list (Expected: " + expected + ", Actual: " + actual + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Not all elements of the expected list are present in the actual list (Expected: " + expected + ", Actual: " + actual + ")");
                throw e;
            }
        }

        /**
         * Overloaded: Assert that a string contains another string (case-insensitive) with a message.
         * @param actual the actual string.
         * @param expected the expected string.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the actual string does not contain the expected string (case-insensitive).
         */
        public static void assertContainsIgnoreCase(String actual, String expected, String message) {
            try {
                Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String contains (case-insensitive) (Actual: " + actual + ", Expected: " + expected + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not contain (case-insensitive) (Actual: " + actual + ", Expected: " + expected + ")");
                throw e;
            }
        }

        /**
         * Overloaded: Assert that a list contains a specific value with a message.
         * @param actual the actual list.
         * @param value the value that is expected to be present in the list.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the list does not contain the value.
         */
        public static <T> void assertContains(List<T> actual, T value, String message) {
            try {
                Assert.assertTrue(actual.contains(value), message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - List contains value (List: " + actual + ", Value: " + value + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - List does not contain value (List: " + actual + ", Value: " + value + ")");
                throw e;
            }
        }

        /**
         * Overloaded: Assert that two integer arrays are equal with a message.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the arrays are not equal.
         */
        public static void assertArrayEquals(int[] actual, int[] expected, String message) {
            try {
                Assert.assertEquals(actual, expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Integer arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Integer arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        /**
         * Overloaded: Assert that two double arrays are equal within a tolerance with a message.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param delta the maximum difference between two values for considering them equal.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the arrays are not equal within the specified tolerance.
         */
        public static void assertArrayEquals(double[] actual, double[] expected, double delta, String message) {
            try {
                Assert.assertEquals(actual, expected, delta, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Double arrays are equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Double arrays are not equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        /**
         * Add these new overloaded methods with message parameter.
         * @param actual the actual object.
         * @param expected the expected object.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the objects are not equal.
         */
        public static void assertEquals(Object actual, Object expected, String message) {
            try {
                Assert.assertEquals(actual, expected, message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - Objects are equal");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Objects are not equal");
                throw e;
            }
        }

        /**
         * Assert that a value is between two values (inclusive) with a message.
         * @param value the value to check.
         * @param lowerBound the lower bound of the range.
         * @param upperBound the upper bound of the range.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the value is not within the specified range.
         */
        public static void assertBetween(int value, int lowerBound, int upperBound, String message) {
            try {
                Assert.assertTrue(value >= lowerBound && value <= upperBound, message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, 
                    " - Value " + value + " is within range [" + lowerBound + ", " + upperBound + "]");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, 
                    " - Value " + value + " is not within range [" + lowerBound + ", " + upperBound + "]");
                throw e;
            }
        }

        /**
         * Assert that a string matches a regular expression with a message.
         * @param actual the actual string.
         * @param regex the regular expression to match.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the string does not match the regular expression.
         */
        public static void assertMatches(String actual, String regex, String message) {
            try {
                Assert.assertTrue(actual.matches(regex), message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - String matches the regex pattern");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - String does not match the regex pattern");
                throw e;
            }
        }

        /**
         * Assert that two lists have the same size with a message.
         * @param actual the actual list.
         * @param expected the expected list.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the lists do not have the same size.
         */
        public static void assertSameSize(List<?> actual, List<?> expected, String message) {
            try {
                Assert.assertEquals(actual.size(), expected.size(), message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - Lists have the same size");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Lists do not have the same size");
                throw e;
            }
        }

        /**
         * Assert that a map is empty with a message.
         * @param map the map to check.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the map is not empty.
         */
        public static void assertMapEmpty(Map<?, ?> map, String message) {
            try {
                Assert.assertTrue(map.isEmpty(), message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - Map is empty");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Map is not empty");
                throw e;
            }
        }

        /**
         * Assert that a map is not empty with a message.
         * @param map the map to check.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the map is empty.
         */
        public static void assertMapNotEmpty(Map<?, ?> map, String message) {
            try {
                Assert.assertFalse(map.isEmpty(), message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - Map is not empty");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Map is empty");
                throw e;
            }
        }

        /**
         * Assert that a string starts with a specific prefix with a message.
         * @param actual the actual string.
         * @param prefix the expected prefix.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the string does not start with the specified prefix.
         */
        public static void assertStartsWith(String actual, String prefix, String message) {
            try {
                Assert.assertTrue(actual.startsWith(prefix), message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - String starts with prefix: " + prefix);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - String does not start with prefix: " + prefix);
                throw e;
            }
        }

        /**
         * Assert that a string ends with a specific suffix with a message.
         * @param actual the actual string.
         * @param suffix the expected suffix.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the string does not end with the specified suffix.
         */
        public static void assertEndsWith(String actual, String suffix, String message) {
            try {
                Assert.assertTrue(actual.endsWith(suffix), message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - String ends with suffix: " + suffix);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - String does not end with suffix: " + suffix);
                throw e;
            }
        }

        /**
         * Assert that an object is of a specific type without a message.
         * @param object the object to check.
         * @param clazz the expected class of the object.
         * @throws AssertionError if the object is not of the specified type.
         */
        public static void assertInstanceOf(Object object, Class<?> clazz) {
            try {
                Assert.assertTrue(clazz.isInstance(object));
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is instance of " + clazz.getSimpleName());
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is not instance of " + clazz.getSimpleName());
                throw e;
            }
        }

        /**
         * Add delta-based comparison for doubles.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param delta the maximum difference between two values for considering them equal.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the values are not equal within the specified delta.
         */
        public static void assertEquals(double actual, double expected, double delta, String message) {
            try {
                Assert.assertEquals(actual, expected, delta, message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, 
                    String.format(" - Values are equal within delta %f", delta));
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, 
                    String.format(" - Values differ by more than delta %f", delta));
                throw e;
            }
        }

        /**
         * Add missing TestNG assertion methods.
         * @param actual the actual string.
         * @param expected the expected string.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the strings are not equal.
         */
        public static void assertEquals(String actual, String expected, String message) {
            try {
                Assert.assertEquals(actual, expected, message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - Strings are equal");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Strings are not equal");
                throw e;
            }
        }

        /**
         * Assert that two long values are equal with a message.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the values are not equal.
         */
        public static void assertEquals(long actual, long expected, String message) {
            try {
                Assert.assertEquals(actual, expected, message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - Long values are equal");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Long values are not equal");
                throw e;
            }
        }

        /**
         * Assert that two boolean values are equal with a message.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the values are not equal.
         */
        public static void assertEquals(boolean actual, boolean expected, String message) {
            try {
                Assert.assertEquals(actual, expected, message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - Boolean values are equal");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Boolean values are not equal");
                throw e;
            }
        }

        /**
         * Assert that two arrays are equal ignoring order with a message.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param message the message to display if the assertion fails.
         * @throws AssertionError if the arrays are not equal ignoring order.
         */
        public static void assertEqualsNoOrder(Object[] actual, Object[] expected, String message) {
            try {
                Assert.assertEqualsNoOrder(actual, expected, message);
                Reporter.log("Hard Assert: " + message, LogLevel.INFO_GREEN, " - Arrays are equal (ignoring order)");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Arrays are not equal (ignoring order)");
                throw e;
            }
        }

        /**
         * Force a failure with a message.
         * @param message the message to display.
         * @throws AssertionError always.
         */
        public static void fail(String message) {
            try {
                Assert.fail(message);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: " + message, LogLevel.ERROR, " - Forced failure");
                throw e;
            }
        }

        /**
         * Force a failure with a message and a throwable.
         * @param message the message to display.
         * @param throwable the throwable to include.
         * @throws AssertionError always.
         */
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

        /**
         * Soft Assert that a condition is true.
         * @param condition the condition to evaluate.
         * @param message the message to display if the assertion fails.
         */
        public void assertTrue(boolean condition, String message) {
            softAssert.assertTrue(condition, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Condition is true: " + condition + " - " + message);
        }

        /**
         * Soft Assert that a condition is false.
         * @param condition the condition to evaluate.
         * @param message the message to display if the assertion fails.
         */
        public void assertFalse(boolean condition, String message) {
            softAssert.assertFalse(condition, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Condition is false: " + condition + " - " + message);
        }

        /**
         * Soft Assert that an object is null.
         * @param object the object to check.
         * @param message the message to display if the assertion fails.
         */
        public void assertNull(Object object, String message) {
            softAssert.assertNull(object, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Object is null: " + object + " - " + message);
        }

        /**
         * Soft Assert that an object is not null.
         * @param object the object to check.
         * @param message the message to display if the assertion fails.
         */
        public void assertNotNull(Object object, String message) {
            softAssert.assertNotNull(object, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Object is not null: " + object + " - " + message);
        }

        /**
         * Soft Assert that two objects are the same (reference equality).
         * @param actual the actual object.
         * @param expected the expected object.
         * @param message the message to display if the assertion fails.
         */
        public void assertSame(Object actual, Object expected, String message) {
            softAssert.assertSame(actual, expected, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are the same - Expected: " + expected + ", Actual: " + actual + " - " + message);
        }

        /**
         * Soft Assert that two objects are not the same (reference equality).
         * @param actual the actual object.
         * @param expected the expected object.
         * @param message the message to display if the assertion fails.
         */
        public void assertNotSame(Object actual, Object expected, String message) {
            softAssert.assertNotSame(actual, expected, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are not the same - Expected: " + expected + ", Actual: " + actual + " - " + message);
        }

        /**
         * Soft Assert that two objects are equal.
         * @param actual the actual object.
         * @param expected the expected object.
         */
        public void assertEquals(Object actual, Object expected) {
            softAssert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are equal - Expected: " + expected + ", Actual: " + actual);
        }

        /**
         * Soft Assert that a collection or string contains a specific value.
         * @param container the collection or string to check.
         * @param value the value that is expected to be present in the container.
         */
        public void assertContains(Object container, Object value) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains((String) value), "Container does not contain value: " + value);
                Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "String contains value - Container: " + container + ", Value: " + value);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value), "Container does not contain value: " + value);
                Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Collection contains value - Container: " + container + ", Value: " + value);
            }
        }

        /**
         * Soft Assert that two lists are equal.
         * @param actual the actual list.
         * @param expected the expected list.
         */
        public void assertListEquals(List<?> actual, List<?> expected) {
            softAssert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Lists are equal - Expected: " + expected + ", Actual: " + actual);
        }

        /**
         * Soft Assert that all elements of one list are present in another list.
         * @param actual the actual list.
         * @param expected the expected list.
         */
        public void assertListContainsAll(List<?> actual, List<?> expected) {
            softAssert.assertTrue(new HashSet<>(actual).containsAll(expected), "Actual list does not contain all expected elements.");
            Reporter.log("Soft Assert: Actual list contains all expected elements - Actual: " + actual , LogLevel.INFO_GREEN, ", Expected: " + expected);
        }

        /**
         * Soft Assert that a value is greater than another value.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         */
        public void assertGreaterThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual , LogLevel.INFO_GREEN, " > Expected: " + expected);
        }

        /**
         * Soft Assert that a value is less than another value.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         */
        public void assertLessThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual , LogLevel.INFO_GREEN, " < Expected: " + expected);
        }

        /**
         * Soft Assert that a collection or string is empty.
         * @param container the collection or string to check.
         * @param message the message to display if the assertion fails.
         */
        public void assertEmpty(Object container, String message) {
            boolean isEmpty = (container instanceof String) ? ((String) container).isEmpty() : ((container instanceof Collection) && ((Collection<?>) container).isEmpty());
            softAssert.assertTrue(isEmpty, message);
            Reporter.log("Soft Assert: " + message , LogLevel.INFO_GREEN, " - Container is empty: " + container);
        }

        /**
         * Soft Assert that a collection or string is not empty.
         * @param container the collection or string to check.
         * @param message the message to display if the assertion fails.
         */
        public void assertNotEmpty(Object container, String message) {
            boolean isNotEmpty = (container instanceof String) ? !((String) container).isEmpty() : ((container instanceof Collection) && !((Collection<?>) container).isEmpty());
            softAssert.assertTrue(isNotEmpty, message);
            Reporter.log("Soft Assert: " + message , LogLevel.INFO_GREEN, " - Container is not empty: " + container);
        }

        /**
         * Soft Assert that an object is of a specific type.
         * @param object the object to check.
         * @param clazz the expected class of the object.
         * @param message the message to display if the assertion fails.
         */
        public void assertInstanceOf(Object object, Class<?> clazz, String message) {
            softAssert.assertTrue(clazz.isInstance(object), message);
            Reporter.log("Soft Assert: " + message , LogLevel.INFO_GREEN, " - Object: " + object + " is an instance of: " + clazz.getName());
        }

        /**
         * Call assertAll to trigger soft assertion validation.
         */
        public void assertAll() {
            try {
                softAssert.assertAll();
                Reporter.log("Soft Assertions Completed and Passed. Validating all collected conditions.", LogLevel.INFO_GREEN);
            }catch (AssertionError e){
                Reporter.log("Soft Assertions Completed and Failed. Validating all collected conditions.", LogLevel.ERROR);
                throw e;
            }
        }

        /**
         * Soft Assert that a string contains another string (case-insensitive).
         * @param actual the actual string.
         * @param expected the expected string.
         */
        public void assertContainsIgnoreCase(String actual, String expected) {
            softAssert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), "String does not contain expected value: " + expected);
            Reporter.log("Soft Assert: Actual string contains expected string (ignore case) - Actual: " + actual , LogLevel.INFO_GREEN, ", Expected: " + expected);
        }

        /**
         * Overload: Soft Assert that a list contains a specific value.
         * @param actual the actual list.
         * @param value the value that is expected to be present in the list.
         * @param <T> the type of elements in the list.
         */
        public <T> void assertContains(List<T> actual, T value) {
            softAssert.assertTrue(actual.contains(value), "List does not contain expected value: " + value);
            Reporter.log("Soft Assert: List contains value: " + value , LogLevel.INFO_GREEN, " - Actual List: " + actual);
        }

        /**
         * Soft Assert that a value is greater than another value (int).
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         */
        public void assertGreaterThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual , LogLevel.INFO_GREEN, " > Expected: " + expected);
        }

        /**
         * Soft Assert that a value is less than another value (int).
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         */
        public void assertLessThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual , LogLevel.INFO_GREEN, " < Expected: " + expected);
        }

        /**
         * Soft Assert that two integer arrays are equal.
         * @param actual the actual array.
         * @param expected the expected array.
         */
        public void assertArrayEquals(int[] actual, int[] expected) {
            softAssert.assertEquals(actual, expected, "Arrays are not equal");
            Reporter.log("Soft Assert: Integer arrays are equal - Expected: " + java.util.Arrays.toString(expected) , LogLevel.INFO_GREEN, ", Actual: " + java.util.Arrays.toString(actual));
        }

        /**
         * Soft Assert that two double arrays are equal within a delta.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param delta the maximum difference between two values for considering them equal.
         */
        public void assertArrayEquals(double[] actual, double[] expected, double delta) {
            if (actual.length != expected.length) {
                softAssert.fail("Arrays are not of the same length");
                Reporter.log("Soft Assert Fail: Arrays have different lengths - Expected Length: " + expected.length , LogLevel.INFO_GREEN, ", Actual Length: " + actual.length);
                return;
            }
            for (int i = 0; i < actual.length; i++) {
                boolean withinDelta = Math.abs(actual[i] - expected[i]) <= delta;
                softAssert.assertTrue(withinDelta, "Array elements at index " + i + " differ by more than " + delta + ". Expected: " + expected[i] + ", Actual: " + actual[i]);
                Reporter.log("Soft Assert: Array element at index " + i + " is within tolerance - Expected: " + expected[i] , LogLevel.INFO_GREEN, ", Actual: " + actual[i] + ", Tolerance: " + delta);
            }
        }

        /**
         * Overloaded: Soft Assert that a condition is true without a message.
         * @param condition the condition to evaluate.
         */
        public void assertTrue(boolean condition) {
            softAssert.assertTrue(condition, "Condition should be true but was false");
            Reporter.log("Soft Assert: Condition is true",LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that a condition is false without a message.
         * @param condition the condition to evaluate.
         */
        public void assertFalse(boolean condition) {
            softAssert.assertFalse(condition, "Condition should be false but was true");
            Reporter.log("Soft Assert: Condition is false", LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that an object is null without a message.
         * @param object the object to check.
         */
        public void assertNull(Object object) {
            softAssert.assertNull(object, "Object should be null but was not");
            Reporter.log("Soft Assert: Object is null", LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that an object is not null without a message.
         * @param object the object to check.
         */
        public void assertNotNull(Object object) {
            softAssert.assertNotNull(object, "Object should not be null but was");
            Reporter.log("Soft Assert: Object is not null", LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that two objects are the same (reference equality) without a message.
         * @param actual the actual object.
         * @param expected the expected object.
         */
        public void assertSame(Object actual, Object expected) {
            softAssert.assertSame(actual, expected, "Expected and actual objects should be the same but were not");
            Reporter.log( "Soft Assert Same - Expected: " + expected + " - Actual: " + actual , LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that two objects are not the same (reference equality) without a message.
         * @param actual the actual object.
         * @param expected the expected object.
         */
        public void assertNotSame(Object actual, Object expected) {
            softAssert.assertNotSame(actual, expected, "Expected and actual objects should not be the same but were");
            Reporter.log( "Soft Assert Not Same - Expected: " + expected + " - Actual: " + actual , LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that a collection or string contains a specific value with a message.
         * @param container the collection or string to check.
         * @param value the value that is expected to be present in the container.
         * @param message the message to display if the assertion fails.
         */
        public void assertContains(Object container, Object value, String message) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains((String) value), message);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value), message);
            }
            Reporter.log( "Soft Assert Contains - Container: " + container + " contains Value: " + value , LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that two lists are equal with a message.
         * @param actual the actual list.
         * @param expected the expected list.
         * @param message the message to display if the assertion fails.
         */
        public void assertListEquals(List<?> actual, List<?> expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log( "Soft Assert List Equals - Expected: " + expected + " - Actual: " + actual , LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that all elements of one list are present in another list with a message.
         * @param actual the actual list.
         * @param expected the expected list.
         * @param message the message to display if the assertion fails.
         */
        public void assertListContainsAll(List<?> actual, List<?> expected, String message) {
            softAssert.assertTrue(new HashSet<>(actual).containsAll(expected), message);
            Reporter.log( "Soft Assert List Contains All - Actual: " + actual + " - Expected Elements: " + expected ,LogLevel.INFO_BLUE);
        }

        /**
         * Overloaded: Soft Assert that a string contains another string (case-insensitive) with a message.
         * @param actual the actual string.
         * @param expected the expected string.
         * @param message the message to display if the assertion fails.
         */
        public void assertContainsIgnoreCase(String actual, String expected, String message) {
            softAssert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), message);
            Reporter.log( "Soft Assert Contains Ignore Case - Actual: " + actual + " contains Expected: " + expected ,LogLevel.INFO_BLUE);
        }

        /**
         * Overloaded: Soft Assert that a list contains a specific value with a message.
         * @param actual the actual list.
         * @param value the value that is expected to be present in the list.
         * @param message the message to display if the assertion fails.
         * @param <T> the type of elements in the list.
         */
        public <T> void assertContains(List<T> actual, T value, String message) {
            softAssert.assertTrue(actual.contains(value), message);
            Reporter.log( "Soft Assert Contains - List: " + actual + " contains Value: " + value , LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that two integer arrays are equal with a message.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param message the message to display if the assertion fails.
         */
        public void assertArrayEquals(int[] actual, int[] expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log( "Soft Assert Array Equals (int[]) - Expected: " + Arrays.toString(expected) + " - Actual: " + Arrays.toString(actual), LogLevel.INFO_GREEN);
        }

        /**
         * Overloaded: Soft Assert that two double arrays are equal within a tolerance with a message.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param delta the maximum difference between two values for considering them equal.
         * @param message the message to display if the assertion fails.
         */
        public void assertArrayEquals(double[] actual, double[] expected, double delta, String message) {
            if (actual.length != expected.length) {
                softAssert.fail("Arrays are not of the same length");
                Reporter.log("Soft Assert: Arrays have different lengths", LogLevel.INFO_GREEN);
                return;
            }
            for (int i = 0; i < actual.length; i++) {
                softAssert.assertTrue(Math.abs(actual[i] - expected[i]) <= delta,
                        message + " Array elements at index " + i + " differ by more than " + delta + ". Expected: " + expected[i] + ", Actual: " + actual[i]);
                Reporter.log("Soft Assert: " + message + " Array element at index " + i + " is within tolerance: " + delta, LogLevel.INFO_GREEN);
            }
        }

        /**
         * Add these new overloaded methods with message parameter.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param delta the maximum difference between two values for considering them equal.
         * @param message the message to display if the assertion fails.
         */
        public void assertEquals(double actual, double expected, double delta, String message) {
            softAssert.assertEquals(actual, expected, delta, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, 
                String.format(" - Values are equal within delta %f", delta));
        }

        /**
         * Soft Assert that a value is between two values (inclusive) with a message.
         * @param value the value to check.
         * @param lowerBound the lower bound of the range.
         * @param upperBound the upper bound of the range.
         * @param message the message to display if the assertion fails.
         */
        public void assertBetween(int value, int lowerBound, int upperBound, String message) {
            softAssert.assertTrue(value >= lowerBound && value <= upperBound, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, 
                " - Value " + value + " is within range [" + lowerBound + ", " + upperBound + "]");
        }

        /**
         * Soft Assert that a string matches a regular expression with a message.
         * @param actual the actual string.
         * @param regex the regular expression to match.
         * @param message the message to display if the assertion fails.
         */
        public void assertMatches(String actual, String regex, String message) {
            softAssert.assertTrue(actual.matches(regex), message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - String matches the regex pattern");
        }

        /**
         * Soft Assert that two strings are equal with a message.
         * @param actual the actual string.
         * @param expected the expected string.
         * @param message the message to display if the assertion fails.
         */
        public void assertEquals(String actual, String expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Strings are equal");
        }

        /**
         * Soft Assert that two long values are equal with a message.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         */
        public void assertEquals(long actual, long expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Long values are equal");
        }

        /**
         * Soft Assert that two boolean values are equal with a message.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         */
        public void assertEquals(boolean actual, boolean expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Boolean values are equal");
        }

        /**
         * Soft Assert that two arrays are equal ignoring order with a message.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param message the message to display if the assertion fails.
         */
        public void assertEqualsNoOrder(Object[] actual, Object[] expected, String message) {
            softAssert.assertEqualsNoOrder(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Arrays are equal (ignoring order)");
        }

        /**
         * Force a soft assertion failure with a message.
         * @param message the message to display.
         */
        public void fail(String message) {
            softAssert.fail(message);
            Reporter.log("Soft Assert: " + message, LogLevel.ERROR, " - Forced failure");
        }

        /**
         * Force a soft assertion failure with a message and a throwable.
         * @param message the message to display.
         * @param throwable the throwable to include.
         */
        public void fail(String message, Throwable throwable) {
            softAssert.fail(message, throwable);
            Reporter.log("Soft Assert: " + message, LogLevel.ERROR, " - Forced failure with throwable");
        }

        /**
         * Soft Assert that two double values are equal with a message.
         * @param actual the actual value.
         * @param expected the expected value.
         * @param message the message to display if the assertion fails.
         */
        public void assertEquals(double actual, double expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Double values are equal");
        }

        /**
         * Soft Assert that two byte arrays are equal with a message.
         * @param actual the actual array.
         * @param expected the expected array.
         * @param message the message to display if the assertion fails.
         */
        public void assertEquals(byte[] actual, byte[] expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - Byte values are equal");
        }

        /**
         * Soft Assert that two double values are equal without a message.
         * @param actual the actual value.
         * @param expected the expected value.
         */
        public void assertEquals(double actual, double expected) {
            softAssert.assertEquals(actual, expected);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, " - Double values are equal");
        }

        /**
         * Soft Assert that two byte arrays are equal without a message.
         * @param actual the actual array.
         * @param expected the expected array.
         */
        public void assertEquals(byte[] actual, byte[] expected) {
            softAssert.assertEquals(actual, expected);
            Reporter.log("Soft Assert: " , LogLevel.INFO_GREEN, " - Byte values are equal");
        }

        /**
         * Soft Assert that two lists are equal with a message.
         * @param actual the actual list.
         * @param expected the expected list.
         * @param message the message to display if the assertion fails.
         */
        public void assertEquals(List actual, List expected, String message) {
            softAssert.assertEquals(actual, expected,message);
            Reporter.log("Soft Assert: " + message, LogLevel.INFO_GREEN, " - List values are equal");
        }

        /**
         * Soft Assert that two lists are equal without a message.
         * @param actual the actual list.
         * @param expected the expected list.
         */
        public void assertEquals(List actual, List expected) {
            softAssert.assertEquals(actual, expected);
            Reporter.log("Soft Assert: " , LogLevel.INFO_GREEN, " - List values are equal");
        }

        public void assertNotEquals(int actual, int expected, String message) {
            softAssert.assertNotEquals(actual,expected,message);
            Reporter.log("Soft Assert: " , LogLevel.INFO_GREEN, " -values are not equal");
        }
        public void assertNotEquals(int actual, int expected) {
            softAssert.assertNotEquals(actual,expected);
            Reporter.log("Soft Assert: " , LogLevel.INFO_GREEN, " -values are not equal");
        }
    }
}