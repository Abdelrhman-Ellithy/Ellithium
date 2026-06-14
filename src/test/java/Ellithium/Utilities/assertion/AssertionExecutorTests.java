package Ellithium.Utilities.assertion;

import org.testng.annotations.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AssertionExecutorTests {

    // ── hard.assertTrue ──────────────────────────────────────────────────────

    @Test
    public void hard_assertTrue_passes_onTrue() {
        AssertionExecutor.hard.assertTrue(true);
    }

    @Test
    public void hard_assertTrue_withMessage_passes_onTrue() {
        AssertionExecutor.hard.assertTrue(true, "should be true");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertTrue_throws_onFalse() {
        AssertionExecutor.hard.assertTrue(false, "expected true but got false");
    }

    // ── hard.assertFalse ─────────────────────────────────────────────────────

    @Test
    public void hard_assertFalse_passes_onFalse() {
        AssertionExecutor.hard.assertFalse(false);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertFalse_throws_onTrue() {
        AssertionExecutor.hard.assertFalse(true, "expected false but got true");
    }

    // ── hard.assertNull / assertNotNull ──────────────────────────────────────

    @Test
    public void hard_assertNull_passes_onNull() {
        AssertionExecutor.hard.assertNull(null);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertNull_throws_onNonNull() {
        AssertionExecutor.hard.assertNull("not-null");
    }

    @Test
    public void hard_assertNotNull_passes_onNonNull() {
        AssertionExecutor.hard.assertNotNull("value");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertNotNull_throws_onNull() {
        AssertionExecutor.hard.assertNotNull(null, "must not be null");
    }

    // ── hard.assertEquals ────────────────────────────────────────────────────

    @Test
    public void hard_assertEquals_objects_passes_whenEqual() {
        AssertionExecutor.hard.assertEquals("hello", "hello");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertEquals_objects_throws_whenNotEqual() {
        AssertionExecutor.hard.assertEquals("hello", "world", "strings differ");
    }

    @Test
    public void hard_assertEquals_long_passes() {
        AssertionExecutor.hard.assertEquals(42L, 42L, "longs should match");
    }

    @Test
    public void hard_assertEquals_boolean_passes() {
        AssertionExecutor.hard.assertEquals(true, true, "booleans should match");
    }

    @Test
    public void hard_assertEquals_double_withinDelta_passes() {
        AssertionExecutor.hard.assertEquals(3.14, 3.141, 0.01, "within delta");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertEquals_double_outsideDelta_throws() {
        AssertionExecutor.hard.assertEquals(1.0, 2.0, 0.5, "outside delta");
    }

    // ── hard.assertSame / assertNotSame ──────────────────────────────────────

    @Test
    public void hard_assertSame_passes_sameReference() {
        String s = "shared";
        AssertionExecutor.hard.assertSame(s, s);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertSame_throws_differentReferences() {
        AssertionExecutor.hard.assertSame(new Object(), new Object(), "different instances");
    }

    @Test
    public void hard_assertNotSame_passes_differentReferences() {
        AssertionExecutor.hard.assertNotSame(new Object(), new Object());
    }

    // ── hard.assertContains ──────────────────────────────────────────────────

    @Test
    public void hard_assertContains_string_passes_whenContained() {
        AssertionExecutor.hard.assertContains("hello world", "world");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertContains_string_throws_whenAbsent() {
        AssertionExecutor.hard.assertContains("hello world", "xyz");
    }

    @Test
    public void hard_assertContains_list_passes_whenPresent() {
        AssertionExecutor.hard.assertContains(Arrays.asList("a", "b", "c"), "b");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertContains_list_throws_whenAbsent() {
        AssertionExecutor.hard.assertContains(Arrays.asList("a", "b"), "z");
    }

    // ── hard.assertContainsIgnoreCase ────────────────────────────────────────

    @Test
    public void hard_assertContainsIgnoreCase_passes() {
        AssertionExecutor.hard.assertContainsIgnoreCase("Hello World", "hello");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertContainsIgnoreCase_throws_whenAbsent() {
        AssertionExecutor.hard.assertContainsIgnoreCase("Hello World", "xyz");
    }

    // ── hard.assertListEquals ────────────────────────────────────────────────

    @Test
    public void hard_assertListEquals_passes_equalLists() {
        AssertionExecutor.hard.assertListEquals(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3));
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertListEquals_throws_differentLists() {
        AssertionExecutor.hard.assertListEquals(Arrays.asList(1, 2), Arrays.asList(1, 3));
    }

    @Test
    public void hard_assertListContainsAll_passes() {
        AssertionExecutor.hard.assertListContainsAll(Arrays.asList("a", "b", "c"), Arrays.asList("a", "c"));
    }

    @Test
    public void hard_assertSameSize_passes_equalSizeLists() {
        AssertionExecutor.hard.assertSameSize(Arrays.asList(1, 2), Arrays.asList("x", "y"));
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertSameSize_throws_differentSizes() {
        AssertionExecutor.hard.assertSameSize(Arrays.asList(1), Arrays.asList(1, 2));
    }

    // ── hard.assertGreaterThan / assertLessThan / assertBetween ──────────────

    @Test
    public void hard_assertGreaterThan_double_passes() {
        AssertionExecutor.hard.assertGreaterThan(5.0, 3.0);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertGreaterThan_double_throws_whenNotGreater() {
        AssertionExecutor.hard.assertGreaterThan(2.0, 3.0);
    }

    @Test
    public void hard_assertGreaterThan_int_passes() {
        AssertionExecutor.hard.assertGreaterThan(10, 5);
    }

    @Test
    public void hard_assertLessThan_double_passes() {
        AssertionExecutor.hard.assertLessThan(1.0, 5.0);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertLessThan_double_throws_whenNotLess() {
        AssertionExecutor.hard.assertLessThan(5.0, 2.0);
    }

    @Test
    public void hard_assertBetween_passes_inRange() {
        AssertionExecutor.hard.assertBetween(5, 1, 10);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertBetween_throws_outsideRange() {
        AssertionExecutor.hard.assertBetween(15, 1, 10);
    }

    @Test
    public void hard_assertBetween_passes_atLowerBound() {
        AssertionExecutor.hard.assertBetween(1, 1, 10);
    }

    @Test
    public void hard_assertBetween_passes_atUpperBound() {
        AssertionExecutor.hard.assertBetween(10, 1, 10);
    }

    // ── hard.assertEmpty / assertNotEmpty ────────────────────────────────────

    @Test
    public void hard_assertEmpty_string_passes_onEmpty() {
        AssertionExecutor.hard.assertEmpty("");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertEmpty_string_throws_onNonEmpty() {
        AssertionExecutor.hard.assertEmpty("not empty");
    }

    @Test
    public void hard_assertEmpty_collection_passes_onEmpty() {
        AssertionExecutor.hard.assertEmpty(Collections.emptyList());
    }

    @Test
    public void hard_assertNotEmpty_string_passes_onNonEmpty() {
        AssertionExecutor.hard.assertNotEmpty("hello");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertNotEmpty_string_throws_onEmpty() {
        AssertionExecutor.hard.assertNotEmpty("");
    }

    @Test
    public void hard_assertNotEmpty_collection_passes_onNonEmpty() {
        AssertionExecutor.hard.assertNotEmpty(Arrays.asList("x"));
    }

    // ── hard.assertStartsWith / assertEndsWith ───────────────────────────────

    @Test
    public void hard_assertStartsWith_passes() {
        AssertionExecutor.hard.assertStartsWith("hello world", "hello");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertStartsWith_throws_whenMissing() {
        AssertionExecutor.hard.assertStartsWith("hello world", "world");
    }

    @Test
    public void hard_assertEndsWith_passes() {
        AssertionExecutor.hard.assertEndsWith("hello world", "world");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertEndsWith_throws_whenMissing() {
        AssertionExecutor.hard.assertEndsWith("hello world", "hello");
    }

    // ── hard.assertMatches ───────────────────────────────────────────────────

    @Test
    public void hard_assertMatches_passes_onMatch() {
        AssertionExecutor.hard.assertMatches("abc123", "[a-z]+\\d+");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertMatches_throws_onMismatch() {
        AssertionExecutor.hard.assertMatches("abc", "\\d+");
    }

    // ── hard.assertInstanceOf ────────────────────────────────────────────────

    @Test
    public void hard_assertInstanceOf_passes_correctType() {
        AssertionExecutor.hard.assertInstanceOf("hello", String.class);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertInstanceOf_throws_wrongType() {
        AssertionExecutor.hard.assertInstanceOf("hello", Integer.class);
    }

    // ── hard.assertArrayEquals ───────────────────────────────────────────────

    @Test
    public void hard_assertArrayEquals_int_passes() {
        AssertionExecutor.hard.assertArrayEquals(new int[]{1, 2, 3}, new int[]{1, 2, 3});
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertArrayEquals_int_throws_whenDifferent() {
        AssertionExecutor.hard.assertArrayEquals(new int[]{1, 2}, new int[]{1, 3});
    }

    @Test
    public void hard_assertArrayEquals_double_withinDelta_passes() {
        AssertionExecutor.hard.assertArrayEquals(new double[]{1.0, 2.0}, new double[]{1.0, 2.001}, 0.01);
    }

    // ── hard.assertMapEmpty / assertMapNotEmpty ──────────────────────────────

    @Test
    public void hard_assertMapEmpty_passes_onEmptyMap() {
        AssertionExecutor.hard.assertMapEmpty(new HashMap<>());
    }

    @Test(expectedExceptions = AssertionError.class)
    public void hard_assertMapEmpty_throws_onNonEmptyMap() {
        Map<String, String> m = new HashMap<>();
        m.put("k", "v");
        AssertionExecutor.hard.assertMapEmpty(m);
    }

    @Test
    public void hard_assertMapNotEmpty_passes_onNonEmptyMap() {
        Map<String, String> m = new HashMap<>();
        m.put("k", "v");
        AssertionExecutor.hard.assertMapNotEmpty(m);
    }

    // ── hard.assertEqualsNoOrder ─────────────────────────────────────────────

    @Test
    public void hard_assertEqualsNoOrder_passes_sameElements() {
        AssertionExecutor.hard.assertEqualsNoOrder(new Object[]{"a", "b"}, new Object[]{"b", "a"}, "order-independent");
    }

    // ── hard.fail ────────────────────────────────────────────────────────────

    @Test(expectedExceptions = AssertionError.class)
    public void hard_fail_alwaysThrows() {
        AssertionExecutor.hard.fail("forced failure");
    }

    // ── soft class ───────────────────────────────────────────────────────────

    @Test
    public void soft_collectsMultipleFailures_thenThrowsOnAssertAll() {
        AssertionExecutor.soft s = new AssertionExecutor.soft();
        s.assertTrue(false, "failure 1");
        s.assertEquals("a", "b", "failure 2");
        try {
            s.assertAll();
            throw new RuntimeException("assertAll should have thrown");
        } catch (AssertionError e) {
            // expected: both failures are collected before throwing
        }
    }

    @Test
    public void soft_assertAll_passes_whenNoFailures() {
        AssertionExecutor.soft s = new AssertionExecutor.soft();
        s.assertTrue(true, "ok");
        s.assertEquals("a", "a", "equal");
        s.assertNotNull("value");
        s.assertAll();
    }

    @Test
    public void soft_eachInstanceHasOwnSoftAssert() {
        AssertionExecutor.soft s1 = new AssertionExecutor.soft();
        AssertionExecutor.soft s2 = new AssertionExecutor.soft();
        s1.assertTrue(false, "s1 fail");
        s2.assertTrue(true, "s2 ok");
        s2.assertAll();
        try {
            s1.assertAll();
            throw new RuntimeException("s1.assertAll should have thrown");
        } catch (AssertionError e) {
            // expected
        }
    }

    @Test
    public void soft_assertNull_doesNotThrowImmediately() {
        AssertionExecutor.soft s = new AssertionExecutor.soft();
        s.assertNull("not null");
        try {
            s.assertAll();
        } catch (AssertionError e) {
            // expected at assertAll, not during assertNull call
        }
    }
}
