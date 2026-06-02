package Ellithium.core.ai.scoring;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ElementVectorCacheTest {

    private ElementVectorCache cache;

    @BeforeMethod
    public void setUp() {
        cache = ElementVectorCache.getInstance();
        cache.invalidate();
    }

    @Test
    public void get_nullKey_returnsNull() {
        Assert.assertNull(cache.get(null));
    }

    @Test
    public void get_emptyKey_returnsNull() {
        Assert.assertNull(cache.get(""));
    }

    @Test
    public void put_thenGet_returnsSameVector() {
        float[] v = {0.1f, 0.2f, 0.3f};
        cache.put("el1", v);
        Assert.assertEquals(cache.get("el1"), v);
    }

    @Test
    public void put_nullKey_doesNotStore() {
        cache.put(null, new float[]{0.5f});
        Assert.assertEquals(cache.size(), 0);
    }

    @Test
    public void put_nullVector_doesNotStore() {
        cache.put("el1", null);
        Assert.assertEquals(cache.size(), 0);
    }

    @Test
    public void get_afterDomMutated_returnsNull() {
        float[] v = {0.5f};
        cache.put("el1", v);
        cache.markDomMutated();
        Assert.assertNull(cache.get("el1"));
    }

    @Test
    public void get_afterClearMutationFlag_returnsCachedValue() {
        float[] v = {0.5f};
        cache.put("el1", v);
        cache.markDomMutated();
        cache.clearMutationFlag();
        Assert.assertEquals(cache.get("el1"), v);
    }

    @Test
    public void isDomMutated_falseInitially() {
        Assert.assertFalse(cache.isDomMutated());
    }

    @Test
    public void markDomMutated_setsFlagTrue() {
        cache.markDomMutated();
        Assert.assertTrue(cache.isDomMutated());
    }

    @Test
    public void clearMutationFlag_resetsFlagToFalse() {
        cache.markDomMutated();
        cache.clearMutationFlag();
        Assert.assertFalse(cache.isDomMutated());
    }

    @Test
    public void invalidate_clearsCacheAndResetsMutationFlag() {
        cache.put("el1", new float[]{0.1f});
        cache.markDomMutated();
        cache.invalidate();
        Assert.assertEquals(cache.size(), 0);
        Assert.assertFalse(cache.isDomMutated());
    }

    @Test
    public void size_reflectsNumberOfEntries() {
        Assert.assertEquals(cache.size(), 0);
        cache.put("el1", new float[]{0.1f});
        cache.put("el2", new float[]{0.2f});
        Assert.assertEquals(cache.size(), 2);
    }

    @Test
    public void isEmpty_trueWhenEmpty() {
        Assert.assertTrue(cache.isEmpty());
    }

    @Test
    public void isEmpty_falseAfterPut() {
        cache.put("el1", new float[]{0.1f});
        Assert.assertFalse(cache.isEmpty());
    }

    @Test
    public void overwriteKey_replacesVector() {
        float[] v1 = {0.1f};
        float[] v2 = {0.9f};
        cache.put("el1", v1);
        cache.put("el1", v2);
        Assert.assertEquals(cache.get("el1"), v2);
        Assert.assertEquals(cache.size(), 1);
    }
}
