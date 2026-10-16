package Ellithium.Utilities.assertion.fluent;

import java.util.Map;
import java.util.Objects;

/**
 * Fluent assertions for {@link Map}.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MapAssert<K, V> extends AbstractAssert<MapAssert<K, V>, Map<K, V>> {

    public MapAssert(Map<K, V> actual) {
        super(actual);
    }

    public MapAssert<K, V> containsKey(K key) {
        if (actual == null || !actual.containsKey(key)) {
            fail("Expected map to contain key <" + key + ">");
        } else {
            pass("Map contains key <" + key + ">");
        }
        return this;
    }

    public MapAssert<K, V> doesNotContainKey(K key) {
        if (actual != null && actual.containsKey(key)) {
            fail("Expected map not to contain key <" + key + ">");
        } else {
            pass("Map does not contain key <" + key + ">");
        }
        return this;
    }

    public MapAssert<K, V> containsValue(V value) {
        if (actual == null || !actual.containsValue(value)) {
            fail("Expected map to contain value <" + value + ">");
        } else {
            pass("Map contains value <" + value + ">");
        }
        return this;
    }

    public MapAssert<K, V> containsEntry(K key, V expectedValue) {
        if (actual == null || !actual.containsKey(key)) {
            fail("Expected map to contain key <" + key + "> with value <" + expectedValue + "> but key was absent");
        } else {
            V actualValue = actual.get(key);
            if (!Objects.equals(actualValue, expectedValue)) {
                fail("Expected map key <" + key + "> to have value <" + expectedValue + "> but was <" + actualValue + ">");
            } else {
                pass("Map contains entry <" + key + "=" + expectedValue + ">");
            }
        }
        return this;
    }

    public MapAssert<K, V> hasSize(int expectedSize) {
        int size = actual != null ? actual.size() : -1;
        if (size != expectedSize) {
            fail("Expected map size <" + expectedSize + "> but was <" + size + ">");
        } else {
            pass("Map has size <" + expectedSize + ">");
        }
        return this;
    }

    public MapAssert<K, V> isEmpty() {
        if (actual == null || !actual.isEmpty()) {
            fail("Expected map to be empty but size was " + (actual != null ? actual.size() : -1));
        } else {
            pass("Map is empty");
        }
        return this;
    }

    public MapAssert<K, V> isNotEmpty() {
        if (actual == null || actual.isEmpty()) {
            fail("Expected map not to be empty");
        } else {
            pass("Map is not empty (size: " + actual.size() + ")");
        }
        return this;
    }
}
