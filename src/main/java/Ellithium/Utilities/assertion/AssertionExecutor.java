package Ellithium.Utilities.assertion;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import Ellithium.core.reporting.internal.Colors;
import Ellithium.core.logging.logsUtils;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AssertionExecutor {
    public static class hard {
        // Assert that a condition is true
        public static void assertTrue(boolean condition, String message) {
            try {
                Assert.assertTrue(condition, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN," - Condition is true");
            }catch (AssertionError e){
                Reporter.log("Hard Assert: ", LogLevel.ERROR," - Condition is false");
                throw e;
            }
        }

        // Assert that a condition is false
        public static void assertFalse(boolean condition, String message) {
            try {
                Assert.assertFalse(condition, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Condition is false");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Condition is true");
                throw e;
            }
        }

        // Assert that an object is null
        public static void assertNull(Object object, String message) {
            try {
                Assert.assertNull(object, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is null");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is not null");
                throw e;
            }
        }

        // Assert that an object is not null
        public static void assertNotNull(Object object, String message) {
            try {
                Assert.assertNotNull(object, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is not null");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is null");
                throw e;
            }
        }

        // Assert that two objects are the same (reference equality)
        public static void assertSame(Object actual, Object expected, String message) {
            try {
                Assert.assertSame(actual, expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are the same");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are not the same");
                throw e;
            }
        }

        // Assert that two objects are not the same (reference equality)
        public static void assertNotSame(Object actual, Object expected, String message) {
            try {
                Assert.assertNotSame(actual, expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are not the same");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are the same");
                throw e;
            }
        }

        // Assert that two objects are equal
        public static void assertEquals(Object actual, Object expected) {
            try {
                Assert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are equal");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are not equal");
                throw e;
            }
        }

        // Assert that a collection or string contains a specific value
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

        // Assert that two lists are equal
        public static void assertListEquals(List<?> actual, List<?> expected) {
            try {
                Assert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Lists are equal");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Lists are not equal");
                throw e;
            }
        }

        // Assert that all elements of one list are present in another list
        public static void assertListContainsAll(List<?> actual, List<?> expected) {
            try {
                Assert.assertTrue(actual.containsAll(expected), "Actual list does not contain all expected elements.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - List contains all expected elements");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - List does not contain all expected elements");
                throw e;
            }
        }

        // Assert that a value is greater than another value
        public static void assertGreaterThan(double actual, double expected, String message) {
            try {
                Assert.assertTrue(actual > expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual is greater than Expected");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual is not greater than Expected");
                throw e;
            }
        }

        // Assert that a value is less than another value
        public static void assertLessThan(double actual, double expected, String message) {
            try {
                Assert.assertTrue(actual < expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual is less than Expected");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual is not less than Expected");
                throw e;
            }
        }

        // Assert that a collection or string is empty
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

        // Assert that a collection or string is not empty
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

        // Assert that an object is of a specific type
        public static void assertInstanceOf(Object object, Class<?> clazz, String message) {
            try {
                Assert.assertTrue(clazz.isInstance(object), message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is of the correct type");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is not of the correct type");
                throw e;
            }
        }

        // Assert that a string contains another string (case-insensitive)
        public static void assertContainsIgnoreCase(String actual, String expected) {
            try {
                Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), "String does not contain expected value: " + expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String contains expected value (ignore case)");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not contain expected value (ignore case)");
                throw e;
            }
        }

        // Overload: Assert that a list contains a specific value
        public static <T> void assertContains(List<T> actual, T value) {
            try {
                Assert.assertTrue(actual.contains(value), "List does not contain expected value: " + value);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - List contains value");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - List does not contain value");
                throw e;
            }
        }

        // Assert that a value is greater than another value (int)
        public static void assertGreaterThan(int actual, int expected, String message) {
            try {
                Assert.assertTrue(actual > expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual is greater than Expected (int)");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual is not greater than Expected (int)");
                throw e;
            }
        }
        // Assert that a value is between two values (inclusive)
        public static void assertBetween(int value, int lowerBound, int upperBound) {
            try {
                Assert.assertTrue(value >= lowerBound && value <= upperBound, "Value is not within the expected range.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Value is within range [" + lowerBound + ", " + upperBound + "]");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Value is not within range [" + lowerBound + ", " + upperBound + "]");
                throw e; // Re-throw exception to ensure test fails
            }
        }

        // Assert that a string matches a regular expression
        public static void assertMatches(String actual, String regex) {
            try {
                Assert.assertTrue(actual.matches(regex), "String does not match the expected pattern.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String matches the regex: " + regex);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not match the regex: " + regex);
                throw e;
            }
        }

        // Assert that two lists have the same size
        public static void assertSameSize(List<?> actual, List<?> expected) {
            try {
                Assert.assertEquals(actual.size(), expected.size(), "Lists do not have the same size.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Lists have the same size.");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Lists do not have the same size.");
                throw e;
            }
        }

        // Assert that a map is empty
        public static void assertMapEmpty(Map<?, ?> map) {
            try {
                Assert.assertTrue(map.isEmpty(), "Map is not empty.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Map is empty.");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Map is not empty.");
                throw e;
            }
        }

        // Assert that a map is not empty
        public static void assertMapNotEmpty(Map<?, ?> map) {
            try {
                Assert.assertFalse(map.isEmpty(), "Map is empty.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Map is not empty.");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Map is empty.");
                throw e;
            }
        }

        // Assert that a string starts with a specific prefix
        public static void assertStartsWith(String actual, String prefix) {
            try {
                Assert.assertTrue(actual.startsWith(prefix), "String does not start with the expected prefix.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String starts with the prefix: " + prefix);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not start with the prefix: " + prefix);
                throw e;
            }
        }

        // Assert that a string ends with a specific suffix
        public static void assertEndsWith(String actual, String suffix) {
            try {
                Assert.assertTrue(actual.endsWith(suffix), "String does not end with the expected suffix.");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String ends with the suffix: " + suffix);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not end with the suffix: " + suffix);
                throw e;
            }
        }

        // Assert that a value is less than another value (int)
        public static void assertLessThan(int actual, int expected, String message) {
            try {
                Assert.assertTrue(actual < expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - " + message + " (Actual: " + actual + " < Expected: " + expected + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - " + message + " (Actual: " + actual + " >= Expected: " + expected + ")");
                throw e;
            }
        }

        // Assert that two integer arrays are equal
        public static void assertArrayEquals(int[] actual, int[] expected) {
            try {
                Assert.assertEquals(actual, expected, "Arrays are not equal");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Integer arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Integer arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        // Assert that two double arrays are equal within a tolerance
        public static void assertArrayEquals(double[] actual, double[] expected, double delta) {
            try {
                Assert.assertEquals(actual, expected, delta, "Double arrays are not equal within tolerance");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Double arrays are equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Double arrays are not equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        // Overloaded assertTrue without message
        public static void assertTrue(boolean condition) {
            try {
                Assert.assertTrue(condition);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Condition is true");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Condition is not true");
                throw e;
            }
        }

        // Overloaded assertFalse without message
        public static void assertFalse(boolean condition) {
            try {
                Assert.assertFalse(condition);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Condition is false");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Condition is not false");
                throw e;
            }
        }

        // Overloaded assertNull without message
        public static void assertNull(Object object) {
            try {
                Assert.assertNull(object);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is null");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is not null (Object: " + object + ")");
                throw e;
            }
        }

        // Overloaded assertNotNull without message
        public static void assertNotNull(Object object) {
            try {
                Assert.assertNotNull(object);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Object is not null");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Object is null");
                throw e;
            }
        }

        // Overloaded assertSame without message
        public static void assertSame(Object actual, Object expected) {
            try {
                Assert.assertSame(actual, expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are the same (Expected: " + expected + ", Actual: " + actual + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are not the same (Expected: " + expected + ", Actual: " + actual + ")");
                throw e;
            }
        }

        // Overloaded assertNotSame without message
        public static void assertNotSame(Object actual, Object expected) {
            try {
                Assert.assertNotSame(actual, expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Objects are not the same (Expected: " + expected + ", Actual: " + actual + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Objects are the same (Expected: " + expected + ", Actual: " + actual + ")");
                throw e;
            }
        }

        // Overloaded assertGreaterThan without message (for double)
        public static void assertGreaterThan(double actual, double expected) {
            try {
                Assert.assertTrue(actual > expected, "Actual value is not greater than expected");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual: " + actual + " > Expected: " + expected);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual: " + actual + " <= Expected: " + expected);
                throw e;
            }
        }

        // Overloaded assertLessThan without message (for double)
        public static void assertLessThan(double actual, double expected) {
            try {
                Assert.assertTrue(actual < expected, "Actual value is not less than expected");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual: " + actual + " < Expected: " + expected);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual: " + actual + " >= Expected: " + expected);
                throw e;
            }
        }

        // Overloaded assertGreaterThan without message (for int)
        public static void assertGreaterThan(int actual, int expected) {
            try {
                Assert.assertTrue(actual > expected, "Actual value: " + actual + " is not greater than expected: " + expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual: " + actual + " > Expected: " + expected);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual: " + actual + " <= Expected: " + expected);
                throw e;
            }
        }

        // Overloaded assertLessThan without message (for int)
        public static void assertLessThan(int actual, int expected) {
            try {
                Assert.assertTrue(actual < expected, "Actual value: " + actual + " is not less than expected: " + expected);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Actual: " + actual + " < Expected: " + expected);
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Actual: " + actual + " >= Expected: " + expected);
                throw e;
            }
        }

        // Overloaded assertArrayEquals without delta (for double arrays)
        public static void assertArrayEquals(double[] actual, double[] expected) {
            try {
                Assert.assertEquals(actual, expected, "Double arrays are not equal");
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Double arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Double arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        // Overloaded assertEmpty without message
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

        // Overloaded assertNotEmpty without message
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

        // Overloaded: Assert that two lists are equal with a message
        public static void assertListEquals(List<?> actual, List<?> expected, String message) {
            try {
                Assert.assertEquals(actual, expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Lists are equal (Expected: " + expected + ", Actual: " + actual + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Lists are not equal (Expected: " + expected + ", Actual: " + actual + ")");
                throw e;
            }
        }

        // Overloaded: Assert that all elements of one list are present in another list with a message
        public static void assertListContainsAll(List<?> actual, List<?> expected, String message) {
            try {
                Assert.assertTrue(actual.containsAll(expected), message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - All elements of the expected list are present in the actual list (Expected: " + expected + ", Actual: " + actual + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Not all elements of the expected list are present in the actual list (Expected: " + expected + ", Actual: " + actual + ")");
                throw e;
            }
        }

        // Overloaded: Assert that a string contains another string (case-insensitive) with a message
        public static void assertContainsIgnoreCase(String actual, String expected, String message) {
            try {
                Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - String contains (case-insensitive) (Actual: " + actual + ", Expected: " + expected + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - String does not contain (case-insensitive) (Actual: " + actual + ", Expected: " + expected + ")");
                throw e;
            }
        }

        // Overloaded: Assert that a list contains a specific value with a message
        public static <T> void assertContains(List<T> actual, T value, String message) {
            try {
                Assert.assertTrue(actual.contains(value), message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - List contains value (List: " + actual + ", Value: " + value + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - List does not contain value (List: " + actual + ", Value: " + value + ")");
                throw e;
            }
        }

        // Overloaded: Assert that two integer arrays are equal with a message
        public static void assertArrayEquals(int[] actual, int[] expected, String message) {
            try {
                Assert.assertEquals(actual, expected, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Integer arrays are equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Integer arrays are not equal (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }

        // Overloaded: Assert that two double arrays are equal within a tolerance with a message
        public static void assertArrayEquals(double[] actual, double[] expected, double delta, String message) {
            try {
                Assert.assertEquals(actual, expected, delta, message);
                Reporter.log("Hard Assert: ", LogLevel.INFO_GREEN, " - Double arrays are equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
            } catch (AssertionError e) {
                Reporter.log("Hard Assert: ", LogLevel.ERROR, " - Double arrays are not equal within tolerance: " + delta + " (Expected: " + Arrays.toString(expected) + ", Actual: " + Arrays.toString(actual) + ")");
                throw e;
            }
        }
    }

    public static class soft {
        private final SoftAssert softAssert = new SoftAssert();
        // Soft Assert that a condition is true
// Soft Assert that a condition is true
        public void assertTrue(boolean condition, String message) {
            softAssert.assertTrue(condition, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Condition is true: " + condition + " - " + message);
        }

        // Soft Assert that a condition is false
        public void assertFalse(boolean condition, String message) {
            softAssert.assertFalse(condition, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Condition is false: " + condition + " - " + message);
        }

        // Soft Assert that an object is null
        public void assertNull(Object object, String message) {
            softAssert.assertNull(object, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Object is null: " + object + " - " + message);
        }

        // Soft Assert that an object is not null
        public void assertNotNull(Object object, String message) {
            softAssert.assertNotNull(object, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Object is not null: " + object + " - " + message);
        }

        // Soft Assert that two objects are the same (reference equality)
        public void assertSame(Object actual, Object expected, String message) {
            softAssert.assertSame(actual, expected, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are the same - Expected: " + expected + ", Actual: " + actual + " - " + message);
        }

        // Soft Assert that two objects are not the same (reference equality)
        public void assertNotSame(Object actual, Object expected, String message) {
            softAssert.assertNotSame(actual, expected, message);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are not the same - Expected: " + expected + ", Actual: " + actual + " - " + message);
        }

        // Soft Assert that two objects are equal
        public void assertEquals(Object actual, Object expected) {
            softAssert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Objects are equal - Expected: " + expected + ", Actual: " + actual);
        }

        // Soft Assert that a collection or string contains a specific value
        public void assertContains(Object container, Object value) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains((String) value), "Container does not contain value: " + value);
                Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "String contains value - Container: " + container + ", Value: " + value);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value), "Container does not contain value: " + value);
                Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Collection contains value - Container: " + container + ", Value: " + value);
            }
        }

        // Soft Assert that two lists are equal
        public void assertListEquals(List<?> actual, List<?> expected) {
            softAssert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual);
            Reporter.log("Soft Assert: ", LogLevel.INFO_GREEN, "Lists are equal - Expected: " + expected + ", Actual: " + actual);
        }

        // Soft Assert that all elements of one list are present in another list
        public void assertListContainsAll(List<?> actual, List<?> expected) {
            softAssert.assertTrue(actual.containsAll(expected), "Actual list does not contain all expected elements.");
            Reporter.log("Soft Assert: Actual list contains all expected elements - Actual: " + actual , LogLevel.INFO_GREEN, ", Expected: " + expected);
        }

        // Soft Assert that a value is greater than another value
        public void assertGreaterThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual , LogLevel.INFO_GREEN, " > Expected: " + expected);
        }

        // Soft Assert that a value is less than another value
        public void assertLessThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual , LogLevel.INFO_GREEN, " < Expected: " + expected);
        }

        // Soft Assert that a collection or string is empty
        public void assertEmpty(Object container, String message) {
            boolean isEmpty = (container instanceof String) ? ((String) container).isEmpty() : ((container instanceof Collection) && ((Collection<?>) container).isEmpty());
            softAssert.assertTrue(isEmpty, message);
            Reporter.log("Soft Assert: " + message , LogLevel.INFO_GREEN, " - Container is empty: " + container);
        }

        // Soft Assert that a collection or string is not empty
        public void assertNotEmpty(Object container, String message) {
            boolean isNotEmpty = (container instanceof String) ? !((String) container).isEmpty() : ((container instanceof Collection) && !((Collection<?>) container).isEmpty());
            softAssert.assertTrue(isNotEmpty, message);
            Reporter.log("Soft Assert: " + message , LogLevel.INFO_GREEN, " - Container is not empty: " + container);
        }

        // Soft Assert that an object is of a specific type
        public void assertInstanceOf(Object object, Class<?> clazz, String message) {
            softAssert.assertTrue(clazz.isInstance(object), message);
            Reporter.log("Soft Assert: " + message , LogLevel.INFO_GREEN, " - Object: " + object + " is an instance of: " + clazz.getName());
        }

        // Call assertAll to trigger soft assertion validation
        public void assertAll() {
            try {
                softAssert.assertAll();
                Reporter.log("Soft Assertions Completed and Passed. Validating all collected conditions.", LogLevel.INFO_GREEN);
            }catch (AssertionError e){
                Reporter.log("Soft Assertions Completed and Failed. Validating all collected conditions.", LogLevel.ERROR);
                throw e;
            }
        }

        // Soft Assert that a string contains another string (case-insensitive)
        public void assertContainsIgnoreCase(String actual, String expected) {
            softAssert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), "String does not contain expected value: " + expected);
            Reporter.log("Soft Assert: Actual string contains expected string (ignore case) - Actual: " + actual , LogLevel.INFO_GREEN, ", Expected: " + expected);
        }

        // Overload: Soft Assert that a list contains a specific value
        public <T> void assertContains(List<T> actual, T value) {
            softAssert.assertTrue(actual.contains(value), "List does not contain expected value: " + value);
            Reporter.log("Soft Assert: List contains value: " + value , LogLevel.INFO_GREEN, " - Actual List: " + actual);
        }

        // Soft Assert that a value is greater than another value (int)
        public void assertGreaterThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual , LogLevel.INFO_GREEN, " > Expected: " + expected);
        }

        // Soft Assert that a value is less than another value (int)
        public void assertLessThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            Reporter.log("Soft Assert: " + message + " - Actual: " + actual , LogLevel.INFO_GREEN, " < Expected: " + expected);
        }

        // Soft Assert that two integer arrays are equal
        public void assertArrayEquals(int[] actual, int[] expected) {
            softAssert.assertEquals(actual, expected, "Arrays are not equal");
            Reporter.log("Soft Assert: Integer arrays are equal - Expected: " + java.util.Arrays.toString(expected) , LogLevel.INFO_GREEN, ", Actual: " + java.util.Arrays.toString(actual));
        }

        // Soft Assert that two double arrays are equal within a delta
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

        // Overloaded: Soft Assert that a condition is true without a message
        public void assertTrue(boolean condition) {
            softAssert.assertTrue(condition, "Condition should be true but was false");
            Reporter.log("Soft Assert: Condition is true",LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that a condition is false without a message
        public void assertFalse(boolean condition) {
            softAssert.assertFalse(condition, "Condition should be false but was true");
            Reporter.log("Soft Assert: Condition is false", LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that an object is null without a message
        public void assertNull(Object object) {
            softAssert.assertNull(object, "Object should be null but was not");
            Reporter.log("Soft Assert: Object is null", LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that an object is not null without a message
        public void assertNotNull(Object object) {
            softAssert.assertNotNull(object, "Object should not be null but was");
            Reporter.log("Soft Assert: Object is not null", LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that two objects are the same (reference equality) without a message
        public void assertSame(Object actual, Object expected) {
            softAssert.assertSame(actual, expected, "Expected and actual objects should be the same but were not");
            Reporter.log( "Soft Assert Same - Expected: " + expected + " - Actual: " + actual , LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that two objects are not the same (reference equality) without a message
        public void assertNotSame(Object actual, Object expected) {
            softAssert.assertNotSame(actual, expected, "Expected and actual objects should not be the same but were");
            Reporter.log( "Soft Assert Not Same - Expected: " + expected + " - Actual: " + actual , LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that a collection or string contains a specific value with a message
        public void assertContains(Object container, Object value, String message) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains((String) value), message);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value), message);
            }
            Reporter.log( "Soft Assert Contains - Container: " + container + " contains Value: " + value , LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that two lists are equal with a message
        public void assertListEquals(List<?> actual, List<?> expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log( "Soft Assert List Equals - Expected: " + expected + " - Actual: " + actual , LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that all elements of one list are present in another list with a message
        public void assertListContainsAll(List<?> actual, List<?> expected, String message) {
            softAssert.assertTrue(actual.containsAll(expected), message);
            Reporter.log( "Soft Assert List Contains All - Actual: " + actual + " - Expected Elements: " + expected ,LogLevel.INFO_BLUE);
        }

        // Overloaded: Soft Assert that a string contains another string (case-insensitive) with a message
        public void assertContainsIgnoreCase(String actual, String expected, String message) {
            softAssert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), message);
            Reporter.log( "Soft Assert Contains Ignore Case - Actual: " + actual + " contains Expected: " + expected ,LogLevel.INFO_BLUE);
        }

        // Overloaded: Soft Assert that a list contains a specific value with a message
        public <T> void assertContains(List<T> actual, T value, String message) {
            softAssert.assertTrue(actual.contains(value), message);
            Reporter.log( "Soft Assert Contains - List: " + actual + " contains Value: " + value , LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that two integer arrays are equal with a message
        public void assertArrayEquals(int[] actual, int[] expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            Reporter.log( "Soft Assert Array Equals (int[]) - Expected: " + expected + " - Actual: " + actual, LogLevel.INFO_GREEN);
        }

        // Overloaded: Soft Assert that two double arrays are equal within a tolerance with a message
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
    }
}