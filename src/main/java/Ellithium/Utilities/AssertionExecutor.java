package Ellithium.Utilities;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import java.util.Collection;
import java.util.List;

public class AssertionExecutor {
    public static class hard {
        // Assert that a condition is true
        public static void assertTrue(boolean condition, String message) {
            Assert.assertTrue(condition, message);
            logsUtils.info(Colors.BLUE + "Hard Assert True - Condition is true: " + condition + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Condition is true", Status.PASSED);
        }

        // Assert that a condition is false
        public static void assertFalse(boolean condition, String message) {
            Assert.assertFalse(condition, message);
            logsUtils.info(Colors.BLUE + "Hard Assert False - Condition is false: " + condition + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Condition is false", Status.PASSED);
        }

        // Assert that an object is null
        public static void assertNull(Object object, String message) {
            Assert.assertNull(object, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Null - Object is null: " + object + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Object is null", Status.PASSED);
        }

        // Assert that an object is not null
        public static void assertNotNull(Object object, String message) {
            Assert.assertNotNull(object, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Not Null - Object is not null: " + object + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Object is not null", Status.PASSED);
        }

        // Assert that two objects are the same (reference equality)
        public static void assertSame(Object actual, Object expected, String message) {
            Assert.assertSame(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Same - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Objects are the same", Status.PASSED);
        }

        // Assert that two objects are not the same (reference equality)
        public static void assertNotSame(Object actual, Object expected, String message) {
            Assert.assertNotSame(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Not Same - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Objects are not the same", Status.PASSED);
        }
        // Assert that two objects are equal
        public static void assertEquals(Object actual, Object expected) {
            Assert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual);
            logsUtils.info(Colors.BLUE + "Hard Assert Equals - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Assert that a collection or string contains a specific value
        public static void assertContains(Object container, Object value) {
            if (container instanceof String) {
                Assert.assertTrue(((String) container).contains((String) value), "Container does not contain value: " + value);
            } else if (container instanceof Collection) {
                Assert.assertTrue(((Collection<?>) container).contains(value), "Container does not contain value: " + value);
            }
            logsUtils.info(Colors.BLUE + "Hard Assert Contains - Container: " + container + " contains Value: " + value + Colors.RESET);
            Allure.step("Hard Assert: Container contains value: " + value, Status.PASSED);
        }

        // Assert that two lists are equal
        public static void assertListEquals(List<?> actual, List<?> expected) {
            Assert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual);
            logsUtils.info(Colors.BLUE + "Hard Assert List Equals - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: List equals - Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Assert that all elements of one list are present in another list
        public static void assertListContainsAll(List<?> actual, List<?> expected) {
            Assert.assertTrue(actual.containsAll(expected), "Actual list does not contain all expected elements.");
            logsUtils.info(Colors.BLUE + "Hard Assert List Contains All - Actual: " + actual + " - Expected Elements: " + expected + Colors.RESET);
            Allure.step("Hard Assert: List contains all elements - Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Assert that a value is greater than another value
        public static void assertGreaterThan(double actual, double expected, String message) {
            Assert.assertTrue(actual > expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Greater Than - Actual: " + actual + " > Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Actual: " + actual + " > Expected: " + expected, Status.PASSED);
        }

        // Assert that a value is less than another value
        public static void assertLessThan(double actual, double expected, String message) {
            Assert.assertTrue(actual < expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Less Than - Actual: " + actual + " < Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Actual: " + actual + " < Expected: " + expected, Status.PASSED);
        }

        // Assert that a collection or string is empty
        public static void assertEmpty(Object container, String message) {
            if (container instanceof String) {
                Assert.assertTrue(((String) container).isEmpty(), message);
            } else if (container instanceof Collection) {
                Assert.assertTrue(((Collection<?>) container).isEmpty(), message);
            }
            logsUtils.info(Colors.BLUE + "Hard Assert Empty - Container is empty: " + container + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }
        // Assert that a collection or string is not empty
        public static void assertNotEmpty(Object container, String message) {
            if (container instanceof String) {
                Assert.assertFalse(((String) container).isEmpty(), message);
            } else if (container instanceof Collection) {
                Assert.assertFalse(((Collection<?>) container).isEmpty(), message);
            }
            logsUtils.info(Colors.BLUE + "Hard Assert Not Empty - Container is not empty: " + container + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

        // Assert that an object is of a specific type
        public static void assertInstanceOf(Object object, Class<?> clazz, String message) {
            Assert.assertTrue(clazz.isInstance(object), message);
            logsUtils.info(Colors.BLUE + "Hard Assert Instance Of - Object: " + object + " is an instance of: " + clazz.getName() + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }
        // Assert that a string contains another string (case-insensitive)
        public static void assertContainsIgnoreCase(String actual, String expected) {
            Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), "String does not contain expected value: " + expected);
            logsUtils.info(Colors.BLUE + "Hard Assert Contains Ignore Case - Actual: " + actual + " contains Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: Actual string contains expected string (ignore case): " + expected, Status.PASSED);
        }
        public static void assertContains(Object container, Object value, String message) {
            if (container instanceof String) {
                Assert.assertTrue(((String) container).contains((String) value), message);
            } else if (container instanceof Collection) {
                Assert.assertTrue(((Collection<?>) container).contains(value), message);
            }
            logsUtils.info(Colors.BLUE + "Hard Assert Contains - Container: " + container + " contains Value: " + value + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

        // Overload: Assert that a list contains a specific value
        public static <T> void assertContains(List<T> actual, T value) {
            Assert.assertTrue(actual.contains(value), "List does not contain expected value: " + value);
            logsUtils.info(Colors.BLUE + "Hard Assert Contains - List: " + actual + " contains Value: " + value + Colors.RESET);
            Allure.step("Hard Assert: List contains value: " + value, Status.PASSED);
        }

        // Assert that a value is greater than another value (int)
        public static void assertGreaterThan(int actual, int expected, String message) {
            Assert.assertTrue(actual > expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Greater Than (int) - Actual: " + actual + " > Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Actual: " + actual + " > Expected: " + expected, Status.PASSED);
        }

        // Assert that a value is less than another value (int)
        public static void assertLessThan(int actual, int expected, String message) {
            Assert.assertTrue(actual < expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Less Than (int) - Actual: " + actual + " < Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: " + message + " - Actual: " + actual + " < Expected: " + expected, Status.PASSED);
        }

        // Assert that two integer arrays are equal
        public static void assertArrayEquals(int[] actual, int[] expected) {
            Assert.assertEquals(actual, expected, "Arrays are not equal");
            logsUtils.info(Colors.BLUE + "Hard Assert Array Equals (int[]) - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: Integer arrays are equal", Status.PASSED);
        }

        // Assert that two double arrays are equal within a tolerance
        public static void assertArrayEquals(double[] actual, double[] expected, double delta) {
            Assert.assertEquals(actual, expected, delta, "Double arrays are not equal within tolerance");
            logsUtils.info(Colors.BLUE + "Hard Assert Array Equals (double[]) with delta - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: Double arrays are equal within tolerance: " + delta, Status.PASSED);
        }
        // Overloaded assertTrue without message
        public static void assertTrue(boolean condition) {
            Assert.assertTrue(condition);
            logsUtils.info(Colors.BLUE + "Hard Assert True - Condition is true: " + condition + Colors.RESET);
            Allure.step("Hard Assert: Condition is true", Status.PASSED);
        }

        // Overloaded assertFalse without message
        public static void assertFalse(boolean condition) {
            Assert.assertFalse(condition);
            logsUtils.info(Colors.BLUE + "Hard Assert False - Condition is false: " + condition + Colors.RESET);
            Allure.step("Hard Assert: Condition is false", Status.PASSED);
        }

        // Overloaded assertNull without message
        public static void assertNull(Object object) {
            Assert.assertNull(object);
            logsUtils.info(Colors.BLUE + "Hard Assert Null - Object is null: " + object + Colors.RESET);
            Allure.step("Hard Assert: Object is null", Status.PASSED);
        }

        // Overloaded assertNotNull without message
        public static void assertNotNull(Object object) {
            Assert.assertNotNull(object);
            logsUtils.info(Colors.BLUE + "Hard Assert Not Null - Object is not null: " + object + Colors.RESET);
            Allure.step("Hard Assert: Object is not null", Status.PASSED);
        }

        // Overloaded assertSame without message
        public static void assertSame(Object actual, Object expected) {
            Assert.assertSame(actual, expected);
            logsUtils.info(Colors.BLUE + "Hard Assert Same - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: Objects are the same", Status.PASSED);
        }

        // Overloaded assertNotSame without message
        public static void assertNotSame(Object actual, Object expected) {
            Assert.assertNotSame(actual, expected);
            logsUtils.info(Colors.BLUE + "Hard Assert Not Same - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: Objects are not the same", Status.PASSED);
        }

        // Overloaded assertGreaterThan without message (for double)
        public static void assertGreaterThan(double actual, double expected) {
            Assert.assertTrue(actual > expected, "Actual value: " + actual + " is not greater than expected: " + expected);
            logsUtils.info(Colors.BLUE + "Hard Assert Greater Than - Actual: " + actual + " > Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: Actual: " + actual + " > Expected: " + expected, Status.PASSED);
        }
        // Overloaded assertLessThan without message (for double)
        public static void assertLessThan(double actual, double expected) {
            Assert.assertTrue(actual < expected, "Actual value: " + actual + " is not less than expected: " + expected);
            logsUtils.info(Colors.BLUE + "Hard Assert Less Than - Actual: " + actual + " < Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: Actual: " + actual + " < Expected: " + expected, Status.PASSED);
        }

        // Overloaded assertGreaterThan without message (for int)
        public static void assertGreaterThan(int actual, int expected) {
            Assert.assertTrue(actual > expected, "Actual value: " + actual + " is not greater than expected: " + expected);
            logsUtils.info(Colors.BLUE + "Hard Assert Greater Than (int) - Actual: " + actual + " > Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: Actual: " + actual + " > Expected: " + expected, Status.PASSED);
        }
        // Overloaded assertLessThan without message (for int)
        public static void assertLessThan(int actual, int expected) {
            Assert.assertTrue(actual < expected, "Actual value: " + actual + " is not less than expected: " + expected);
            logsUtils.info(Colors.BLUE + "Hard Assert Less Than (int) - Actual: " + actual + " < Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: Actual: " + actual + " < Expected: " + expected, Status.PASSED);
        }
        // Overloaded assertArrayEquals without delta (for double arrays)
        public static void assertArrayEquals(double[] actual, double[] expected) {
            Assert.assertEquals(actual, expected, "Double arrays are not equal");
            logsUtils.info(Colors.BLUE + "Hard Assert Array Equals (double[]) - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: Double arrays are equal", Status.PASSED);
        }

        // Overloaded assertEmpty without message
        public static void assertEmpty(Object container) {
            if (container instanceof String) {
                Assert.assertTrue(((String) container).isEmpty());
            } else if (container instanceof Collection) {
                Assert.assertTrue(((Collection<?>) container).isEmpty());
            }
            logsUtils.info(Colors.BLUE + "Hard Assert Empty - Container is empty: " + container + Colors.RESET);
            Allure.step("Hard Assert: Container is empty", Status.PASSED);
        }

        // Overloaded assertNotEmpty without message
        public static void assertNotEmpty(Object container) {
            if (container instanceof String) {
                Assert.assertFalse(((String) container).isEmpty());
            } else if (container instanceof Collection) {
                Assert.assertFalse(((Collection<?>) container).isEmpty());
            }
            logsUtils.info(Colors.BLUE + "Hard Assert Not Empty - Container is not empty: " + container + Colors.RESET);
            Allure.step("Hard Assert: Container is not empty", Status.PASSED);
        }
        // Overloaded: Assert that two lists are equal with a message
        public static void assertListEquals(List<?> actual, List<?> expected, String message) {
            Assert.assertEquals(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert List Equals - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

        // Overloaded: Assert that all elements of one list are present in another list with a message
        public static void assertListContainsAll(List<?> actual, List<?> expected, String message) {
            Assert.assertTrue(actual.containsAll(expected), message);
            logsUtils.info(Colors.BLUE + "Hard Assert List Contains All - Actual: " + actual + " - Expected Elements: " + expected + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

        // Overloaded: Assert that a string contains another string (case-insensitive) with a message
        public static void assertContainsIgnoreCase(String actual, String expected, String message) {
            Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), message);
            logsUtils.info(Colors.BLUE + "Hard Assert Contains Ignore Case - Actual: " + actual + " contains Expected: " + expected + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

        // Overloaded: Assert that a list contains a specific value with a message
        public static <T> void assertContains(List<T> actual, T value, String message) {
            Assert.assertTrue(actual.contains(value), message);
            logsUtils.info(Colors.BLUE + "Hard Assert Contains - List: " + actual + " contains Value: " + value + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

        // Overloaded: Assert that two integer arrays are equal with a message
        public static void assertArrayEquals(int[] actual, int[] expected, String message) {
            Assert.assertEquals(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Array Equals (int[]) - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

        // Overloaded: Assert that two double arrays are equal within a tolerance with a message
        public static void assertArrayEquals(double[] actual, double[] expected, double delta, String message) {
            Assert.assertEquals(actual, expected, delta, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Array Equals (double[]) with delta - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

        // Overloaded: Assert that two double arrays are equal with a message
        public static void assertArrayEquals(double[] actual, double[] expected, String message) {
            Assert.assertEquals(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Hard Assert Array Equals (double[]) - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Hard Assert: " + message, Status.PASSED);
        }

    }

    public static class soft {
        private final SoftAssert softAssert = new SoftAssert();
        // Soft Assert that a condition is true
        public void assertTrue(boolean condition, String message) {
            softAssert.assertTrue(condition, message);
            logsUtils.info(Colors.BLUE + "Soft Assert True - Condition is true: " + condition + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Condition is true", Status.PASSED);
        }

        // Soft Assert that a condition is false
        public void assertFalse(boolean condition, String message) {
            softAssert.assertFalse(condition, message);
            logsUtils.info(Colors.BLUE + "Soft Assert False - Condition is false: " + condition + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Condition is false", Status.PASSED);
        }

        // Soft Assert that an object is null
        public void assertNull(Object object, String message) {
            softAssert.assertNull(object, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Null - Object is null: " + object + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Object is null", Status.PASSED);
        }

        // Soft Assert that an object is not null
        public void assertNotNull(Object object, String message) {
            softAssert.assertNotNull(object, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Not Null - Object is not null: " + object + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Object is not null", Status.PASSED);
        }

        // Soft Assert that two objects are the same (reference equality)
        public void assertSame(Object actual, Object expected, String message) {
            softAssert.assertSame(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Same - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Objects are the same", Status.PASSED);
        }

        // Soft Assert that two objects are not the same (reference equality)
        public void assertNotSame(Object actual, Object expected, String message) {
            softAssert.assertNotSame(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Not Same - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Objects are not the same", Status.PASSED);
        }

        // Soft Assert that two objects are equal
        public void assertEquals(Object actual, Object expected) {
            softAssert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual);
            logsUtils.info(Colors.BLUE + "Soft Assert Equals - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Soft Assert that a collection or string contains a specific value
        public void assertContains(Object container, Object value) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains((String) value), "Container does not contain value: " + value);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value), "Container does not contain value: " + value);
            }
            logsUtils.info(Colors.BLUE + "Soft Assert Contains - Container: " + container + " contains Value: " + value + Colors.RESET);
            Allure.step("Soft Assert: Container contains value: " + value, Status.PASSED);
        }

        // Soft Assert that two lists are equal
        public void assertListEquals(List<?> actual, List<?> expected) {
            softAssert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual);
            logsUtils.info(Colors.BLUE + "Soft Assert List Equals - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: List equals - Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Soft Assert that all elements of one list are present in another list
        public void assertListContainsAll(List<?> actual, List<?> expected) {
            softAssert.assertTrue(actual.containsAll(expected), "Actual list does not contain all expected elements.");
            logsUtils.info(Colors.BLUE + "Soft Assert List Contains All - Actual: " + actual + " - Expected Elements: " + expected + Colors.RESET);
            Allure.step("Soft Assert: List contains all elements - Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Soft Assert that a value is greater than another value
        public void assertGreaterThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Greater Than - Actual: " + actual + " > Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Actual: " + actual + " > Expected: " + expected, Status.PASSED);
        }

        // Soft Assert that a value is less than another value
        public void assertLessThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Less Than - Actual: " + actual + " < Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Actual: " + actual + " < Expected: " + expected, Status.PASSED);
        }

        // Soft Assert that a collection or string is empty
        public void assertEmpty(Object container, String message) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).isEmpty(), message);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).isEmpty(), message);
            }
            logsUtils.info(Colors.BLUE + "Soft Assert Empty - Container is empty: " + container + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Soft Assert that a collection or string is not empty
        public void assertNotEmpty(Object container, String message) {
            if (container instanceof String) {
                softAssert.assertFalse(((String) container).isEmpty(), message);
            } else if (container instanceof Collection) {
                softAssert.assertFalse(((Collection<?>) container).isEmpty(), message);
            }
            logsUtils.info(Colors.BLUE + "Soft Assert Not Empty - Container is not empty: " + container + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Soft Assert that an object is of a specific type
        public void assertInstanceOf(Object object, Class<?> clazz, String message) {
            softAssert.assertTrue(clazz.isInstance(object), message);
            logsUtils.info(Colors.BLUE + "Soft Assert Instance Of - Object: " + object + " is an instance of: " + clazz.getName() + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Call assertAll to trigger soft assertion validation
        public void assertAll() {
            logsUtils.info(Colors.BLUE + "Soft Assertions Completed and Asserting All" + Colors.RESET);
            Allure.step("Soft Assert: Asserting all collected conditions", Status.PASSED);
            softAssert.assertAll(); // Will throw if any soft assertions failed
        }
        // Soft Assert that a string contains another string (case-insensitive)
        public void assertContainsIgnoreCase(String actual, String expected) {
            softAssert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), "String does not contain expected value: " + expected);
            logsUtils.info(Colors.BLUE + "Soft Assert Contains Ignore Case - Actual: " + actual + " contains Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: Actual string contains expected string (ignore case): " + expected, Status.PASSED);
        }

        // Overload: Soft Assert that a list contains a specific value
        public <T> void assertContains(List<T> actual, T value) {
            softAssert.assertTrue(actual.contains(value), "List does not contain expected value: " + value);
            logsUtils.info(Colors.BLUE + "Soft Assert Contains - List: " + actual + " contains Value: " + value + Colors.RESET);
            Allure.step("Soft Assert: List contains value: " + value, Status.PASSED);
        }

        // Soft Assert that a value is greater than another value (int)
        public void assertGreaterThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Greater Than (int) - Actual: " + actual + " > Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Actual: " + actual + " > Expected: " + expected, Status.PASSED);
        }

        // Soft Assert that a value is less than another value (int)
        public void assertLessThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Less Than (int) - Actual: " + actual + " < Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Actual: " + actual + " < Expected: " + expected, Status.PASSED);
        }

        // Soft Assert that two integer arrays are equal
        public void assertArrayEquals(int[] actual, int[] expected) {
            softAssert.assertEquals(actual, expected, "Arrays are not equal");
            logsUtils.info(Colors.BLUE + "Soft Assert Array Equals (int[]) - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: Integer arrays are equal", Status.PASSED);
        }

        public void assertArrayEquals(double[] actual, double[] expected, double delta) {
            if (actual.length != expected.length) {
                softAssert.fail("Arrays are not of the same length");
                logsUtils.info(Colors.RED + "Soft Assert Fail - Arrays have different lengths. Expected: " + expected.length + " - Actual: " + actual.length + Colors.RESET);
                Allure.step("Soft Assert: Arrays have different lengths", Status.FAILED);
                return;
            }
            for (int i = 0; i < actual.length; i++) {
                softAssert.assertTrue(Math.abs(actual[i] - expected[i]) <= delta,
                        "Array elements at index " + i + " differ by more than " + delta + ". Expected: " + expected[i] + ", Actual: " + actual[i]);
                logsUtils.info(Colors.BLUE + "Soft Assert - Comparing element at index " + i + ": Expected: " + expected[i] + " - Actual: " + actual[i] + Colors.RESET);
                Allure.step("Soft Assert: Array element at index " + i + " is within tolerance: " + delta, Status.PASSED);
            }
        }
        // Overloaded: Soft Assert that a condition is true without a message
        public void assertTrue(boolean condition) {
            softAssert.assertTrue(condition, "Condition should be true but was false");
            logsUtils.info(Colors.BLUE + "Soft Assert True - Condition is true: " + condition + Colors.RESET);
            Allure.step("Soft Assert: Condition is true", Status.PASSED);
        }

        // Overloaded: Soft Assert that a condition is false without a message
        public void assertFalse(boolean condition) {
            softAssert.assertFalse(condition, "Condition should be false but was true");
            logsUtils.info(Colors.BLUE + "Soft Assert False - Condition is false: " + condition + Colors.RESET);
            Allure.step("Soft Assert: Condition is false", Status.PASSED);
        }

        // Overloaded: Soft Assert that an object is null without a message
        public void assertNull(Object object) {
            softAssert.assertNull(object, "Object should be null but was not");
            logsUtils.info(Colors.BLUE + "Soft Assert Null - Object is null: " + object + Colors.RESET);
            Allure.step("Soft Assert: Object is null", Status.PASSED);
        }

        // Overloaded: Soft Assert that an object is not null without a message
        public void assertNotNull(Object object) {
            softAssert.assertNotNull(object, "Object should not be null but was");
            logsUtils.info(Colors.BLUE + "Soft Assert Not Null - Object is not null: " + object + Colors.RESET);
            Allure.step("Soft Assert: Object is not null", Status.PASSED);
        }

        // Overloaded: Soft Assert that two objects are the same (reference equality) without a message
        public void assertSame(Object actual, Object expected) {
            softAssert.assertSame(actual, expected, "Expected and actual objects should be the same but were not");
            logsUtils.info(Colors.BLUE + "Soft Assert Same - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: Objects are the same", Status.PASSED);
        }

        // Overloaded: Soft Assert that two objects are not the same (reference equality) without a message
        public void assertNotSame(Object actual, Object expected) {
            softAssert.assertNotSame(actual, expected, "Expected and actual objects should not be the same but were");
            logsUtils.info(Colors.BLUE + "Soft Assert Not Same - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: Objects are not the same", Status.PASSED);
        }

        // Overloaded: Soft Assert that a collection or string contains a specific value with a message
        public void assertContains(Object container, Object value, String message) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains((String) value), message);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value), message);
            }
            logsUtils.info(Colors.BLUE + "Soft Assert Contains - Container: " + container + " contains Value: " + value + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Overloaded: Soft Assert that two lists are equal with a message
        public void assertListEquals(List<?> actual, List<?> expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Soft Assert List Equals - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Overloaded: Soft Assert that all elements of one list are present in another list with a message
        public void assertListContainsAll(List<?> actual, List<?> expected, String message) {
            softAssert.assertTrue(actual.containsAll(expected), message);
            logsUtils.info(Colors.BLUE + "Soft Assert List Contains All - Actual: " + actual + " - Expected Elements: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Overloaded: Soft Assert that a string contains another string (case-insensitive) with a message
        public void assertContainsIgnoreCase(String actual, String expected, String message) {
            softAssert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), message);
            logsUtils.info(Colors.BLUE + "Soft Assert Contains Ignore Case - Actual: " + actual + " contains Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Overloaded: Soft Assert that a list contains a specific value with a message
        public <T> void assertContains(List<T> actual, T value, String message) {
            softAssert.assertTrue(actual.contains(value), message);
            logsUtils.info(Colors.BLUE + "Soft Assert Contains - List: " + actual + " contains Value: " + value + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Overloaded: Soft Assert that two integer arrays are equal with a message
        public void assertArrayEquals(int[] actual, int[] expected, String message) {
            softAssert.assertEquals(actual, expected, message);
            logsUtils.info(Colors.BLUE + "Soft Assert Array Equals (int[]) - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Overloaded: Soft Assert that two double arrays are equal within a tolerance with a message
        public void assertArrayEquals(double[] actual, double[] expected, double delta, String message) {
            if (actual.length != expected.length) {
                softAssert.fail("Arrays are not of the same length");
                logsUtils.info(Colors.RED + "Soft Assert Fail - Arrays have different lengths. Expected: " + expected.length + " - Actual: " + actual.length + Colors.RESET);
                Allure.step("Soft Assert: Arrays have different lengths", Status.FAILED);
                return;
            }
            for (int i = 0; i < actual.length; i++) {
                softAssert.assertTrue(Math.abs(actual[i] - expected[i]) <= delta,
                        message + " Array elements at index " + i + " differ by more than " + delta + ". Expected: " + expected[i] + ", Actual: " + actual[i]);
                logsUtils.info(Colors.BLUE + "Soft Assert - Comparing element at index " + i + ": Expected: " + expected[i] + " - Actual: " + actual[i] + Colors.RESET);
                Allure.step("Soft Assert: " + message + " Array element at index " + i + " is within tolerance: " + delta, Status.PASSED);
            }
        }
    }
}