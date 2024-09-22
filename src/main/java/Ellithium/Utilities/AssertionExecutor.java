package Ellithium.Utilities;

import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;

import java.util.Collection;
import java.util.List;

public class AssertionUtils {

    public static class hard {

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
    }

    public static class soft {
        private final SoftAssert softAssert = new SoftAssert();

        // Soft Assert that two objects are equal
        public void assertEquals(Object actual, Object expected) {
            softAssert.assertEquals(actual, expected, "Expected: " + expected + " but got: " + actual);
            logsUtils.info(Colors.GREEN + "Soft Assert Equals - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Soft Assert that a collection or string contains a specific value
        public void assertContains(Object container, Object value) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).contains((String) value), "Container does not contain value: " + value);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).contains(value), "Container does not contain value: " + value);
            }
            logsUtils.info(Colors.GREEN + "Soft Assert Contains - Container: " + container + " contains Value: " + value + Colors.RESET);
            Allure.step("Soft Assert: Container contains value: " + value, Status.PASSED);
        }

        // Soft Assert that two lists are equal
        public void assertListEquals(List<?> actual, List<?> expected) {
            softAssert.assertEquals(actual, expected, "Expected list: " + expected + " but got: " + actual);
            logsUtils.info(Colors.GREEN + "Soft Assert List Equals - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: List equals - Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Soft Assert that all elements of one list are present in another list
        public void assertListContainsAll(List<?> actual, List<?> expected) {
            softAssert.assertTrue(actual.containsAll(expected), "Actual list does not contain all expected elements.");
            logsUtils.info(Colors.GREEN + "Soft Assert List Contains All - Actual: " + actual + " - Expected Elements: " + expected + Colors.RESET);
            Allure.step("Soft Assert: List contains all elements - Expected: " + expected + " - Actual: " + actual, Status.PASSED);
        }

        // Soft Assert that a value is greater than another value
        public void assertGreaterThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            logsUtils.info(Colors.GREEN + "Soft Assert Greater Than - Actual: " + actual + " > Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Actual: " + actual + " > Expected: " + expected, Status.PASSED);
        }

        // Soft Assert that a value is less than another value
        public void assertLessThan(double actual, double expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            logsUtils.info(Colors.GREEN + "Soft Assert Less Than - Actual: " + actual + " < Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Actual: " + actual + " < Expected: " + expected, Status.PASSED);
        }

        // Soft Assert that a collection or string is empty
        public void assertEmpty(Object container, String message) {
            if (container instanceof String) {
                softAssert.assertTrue(((String) container).isEmpty(), message);
            } else if (container instanceof Collection) {
                softAssert.assertTrue(((Collection<?>) container).isEmpty(), message);
            }
            logsUtils.info(Colors.GREEN + "Soft Assert Empty - Container is empty: " + container + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Soft Assert that a collection or string is not empty
        public void assertNotEmpty(Object container, String message) {
            if (container instanceof String) {
                softAssert.assertFalse(((String) container).isEmpty(), message);
            } else if (container instanceof Collection) {
                softAssert.assertFalse(((Collection<?>) container).isEmpty(), message);
            }
            logsUtils.info(Colors.GREEN + "Soft Assert Not Empty - Container is not empty: " + container + Colors.RESET);
            Allure.step("Soft Assert: " + message, Status.PASSED);
        }

        // Soft Assert that an object is of a specific type
        public void assertInstanceOf(Object object, Class<?> clazz, String message) {
            softAssert.assertTrue(clazz.isInstance(object), message);
            logsUtils.info(Colors.GREEN + "Soft Assert Instance Of - Object: " + object + " is an instance of: " + clazz.getName() + Colors.RESET);
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
            logsUtils.info(Colors.GREEN + "Soft Assert Contains Ignore Case - Actual: " + actual + " contains Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: Actual string contains expected string (ignore case): " + expected, Status.PASSED);
        }

        // Overload: Soft Assert that a list contains a specific value
        public <T> void assertContains(List<T> actual, T value) {
            softAssert.assertTrue(actual.contains(value), "List does not contain expected value: " + value);
            logsUtils.info(Colors.GREEN + "Soft Assert Contains - List: " + actual + " contains Value: " + value + Colors.RESET);
            Allure.step("Soft Assert: List contains value: " + value, Status.PASSED);
        }

        // Soft Assert that a value is greater than another value (int)
        public void assertGreaterThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual > expected, message);
            logsUtils.info(Colors.GREEN + "Soft Assert Greater Than (int) - Actual: " + actual + " > Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Actual: " + actual + " > Expected: " + expected, Status.PASSED);
        }

        // Soft Assert that a value is less than another value (int)
        public void assertLessThan(int actual, int expected, String message) {
            softAssert.assertTrue(actual < expected, message);
            logsUtils.info(Colors.GREEN + "Soft Assert Less Than (int) - Actual: " + actual + " < Expected: " + expected + Colors.RESET);
            Allure.step("Soft Assert: " + message + " - Actual: " + actual + " < Expected: " + expected, Status.PASSED);
        }

        // Soft Assert that two integer arrays are equal
        public void assertArrayEquals(int[] actual, int[] expected) {
            softAssert.assertEquals(actual, expected, "Arrays are not equal");
            logsUtils.info(Colors.GREEN + "Soft Assert Array Equals (int[]) - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: Integer arrays are equal", Status.PASSED);
        }

        // Soft Assert that two double arrays are equal within a tolerance
        public void assertArrayEquals(double[] actual, double[] expected, double delta) {
            softAssert.assertEquals(actual, expected, delta, "Double arrays are not equal within tolerance");
            logsUtils.info(Colors.GREEN + "Soft Assert Array Equals (double[]) with delta - Expected: " + expected + " - Actual: " + actual + Colors.RESET);
            Allure.step("Soft Assert: Double arrays are equal within tolerance: " + delta, Status.PASSED);
        }

    }
}