package Ellithium.Utilities.assertion;

import Ellithium.Utilities.assertion.fluent.SoftAssertScope;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static Ellithium.Utilities.assertion.AssertionExecutor.assertThat;

public class FluentAssertTest {

    @Test
    public void testStringAssertSuccess() {
        assertThat("Ellithium Framework")
                .isNotNull()
                .startsWith("Elli")
                .endsWith("Framework")
                .contains("ith")
                .doesNotContain("Selenium")
                .hasSize(19)
                .isNotBlank()
                .matches(".*Framework");
    }

    @Test
    public void testStringAssertHardFailure() {
        Assert.assertThrows(AssertionError.class, () ->
                assertThat("Ellithium")
                        .as("Framework brand check")
                        .contains("Shaft"));
    }

    @Test
    public void testComparableAssertSuccess() {
        assertThat(42)
                .isGreaterThan(40)
                .isGreaterThanOrEqualTo(42)
                .isLessThan(50)
                .isLessThanOrEqualTo(42)
                .isBetween(40, 45);
    }

    @Test
    public void testComparableAssertFailure() {
        Assert.assertThrows(AssertionError.class, () ->
                assertThat(10).isGreaterThan(20));
    }

    @Test
    public void testCollectionAssertSuccess() {
        List<String> items = List.of("apple", "banana", "cherry");
        assertThat(items)
                .isNotEmpty()
                .hasSize(3)
                .contains("banana")
                .doesNotContain("orange")
                .containsAll(List.of("apple", "cherry"))
                .hasSizeGreaterThan(2)
                .hasSizeLessThan(5);
    }

    @Test
    public void testCollectionAssertFailure() {
        List<String> items = List.of("alpha", "beta");
        Assert.assertThrows(AssertionError.class, () ->
                assertThat(items).contains("gamma"));
    }

    @Test
    public void testMapAssertSuccess() {
        Map<String, Integer> map = Map.of("key1", 100, "key2", 200);
        assertThat(map)
                .isNotEmpty()
                .hasSize(2)
                .containsKey("key1")
                .doesNotContainKey("key3")
                .containsValue(200)
                .containsEntry("key1", 100);
    }

    @Test
    public void testMapAssertFailure() {
        Map<String, String> map = Map.of("browser", "chrome");
        Assert.assertThrows(AssertionError.class, () ->
                assertThat(map).containsEntry("browser", "firefox"));
    }

    @Test
    public void testBooleanAssert() {
        assertThat(true).isTrue();
        assertThat(false).isFalse();

        Assert.assertThrows(AssertionError.class, () -> assertThat(false).isTrue());
        Assert.assertThrows(AssertionError.class, () -> assertThat(true).isFalse());
    }

    @Test
    public void testSoftAssertScopeSuccess() {
        try (SoftAssertScope scope = SoftAssertScope.open()) {
            assertThat("test").startsWith("t");
            assertThat(100).isGreaterThan(50);
            assertThat(List.of(1, 2)).hasSize(2);
        }
    }

    @Test
    public void testSoftAssertScopeFailureAccumulation() {
        AssertionError error = Assert.expectThrows(AssertionError.class, () -> {
            try (SoftAssertScope scope = SoftAssertScope.open()) {
                assertThat("Ellithium").as("Check 1").isEqualTo("Other");
                assertThat(10).as("Check 2").isGreaterThan(100);
                assertThat(List.of("a")).as("Check 3").hasSize(5);
            }
        });

        String msg = error.getMessage();
        Assert.assertTrue(msg.contains("The following 3 soft assertion(s) failed:"));
        Assert.assertTrue(msg.contains("[Check 1]"));
        Assert.assertTrue(msg.contains("[Check 2]"));
        Assert.assertTrue(msg.contains("[Check 3]"));
    }

    @Test
    public void testSoftAssertScopeRunConsumer() {
        Assert.assertThrows(AssertionError.class, () ->
                SoftAssertScope.run(scope -> {
                    assertThat("A").isEqualTo("B");
                })
        );
    }
}
